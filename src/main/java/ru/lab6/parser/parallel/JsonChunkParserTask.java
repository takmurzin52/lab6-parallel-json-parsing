package ru.lab6.parser.parallel;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.lab6.model.AnalysisResult;
import ru.lab6.model.Post;
import ru.lab6.model.User;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.RecursiveTask;

public class JsonChunkParserTask extends RecursiveTask<AnalysisResult> {

    private final File file;
    private final long start;
    private final long end;

    private static final long THRESHOLD = 500 * 1024;
    private static final ObjectMapper mapper = new ObjectMapper();

    public JsonChunkParserTask(File file, long start, long end) {
        this.file = file;
        this.start = start;
        this.end = end;
    }

    @Override
    protected AnalysisResult compute() {
        long length = end - start;
        String threadName = Thread.currentThread().getName();

        if (length <= THRESHOLD) {
            return parseSequential();
        }

        long splitPos = start + length / 2;
        try {
            long oldSplit = splitPos;
            splitPos = findObjectBoundary(splitPos);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (splitPos >= end) {
            return parseSequential();
        }

        JsonChunkParserTask left = new JsonChunkParserTask(file, start, splitPos);
        JsonChunkParserTask right = new JsonChunkParserTask(file, splitPos, end);

        left.fork();
        AnalysisResult rightResult = right.compute();
        AnalysisResult leftResult = left.join();


        rightResult.merge(leftResult);
        return rightResult;
    }


    private AnalysisResult parseSequential() {
        AnalysisResult result = new AnalysisResult();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            int size = (int) (end - start);
            byte[] buffer = new byte[size];
            raf.seek(start);
            raf.readFully(buffer);

            String chunk = new String(buffer, java.nio.charset.StandardCharsets.UTF_8).trim();

            // Убираем лишние запятые и скобки в начале и конце
            if (chunk.startsWith("[")) chunk = chunk.substring(1);
            if (chunk.endsWith("]")) chunk = chunk.substring(0, chunk.length() - 1);

            chunk = chunk.trim();
            if (chunk.startsWith(",")) chunk = chunk.substring(1).trim();
            if (chunk.endsWith(",")) chunk = chunk.substring(0, chunk.length() - 1).trim();

            if (chunk.isEmpty()) return result;

            // Оборачиваем в массив, чтобы Jackson корректно распарсил список
            String jsonToParse = "[" + chunk + "]";

            User[] users = mapper.readValue(jsonToParse, User[].class);

            for (User user : users) {
                if (user == null) continue;
                result.userCount++;
                result.cityDistribution.merge(user.city, 1L, Long::sum);
                if (user.posts != null) {
                    for (Post post : user.posts) {
                        result.totalLikes += post.likes;
                        if (post.tags != null) {
                            for (String tag : post.tags) {
                                result.tagCounts.merge(tag, 1L, Long::sum);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Если кусок совсем битый, не падаем, а просто логируем (для отладки)
            // System.err.println("Пропуск битого куска: " + e.getMessage());
        }
        return result;
    }


    private long findObjectBoundary(long position) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(position);

            // Ищем строку "id" (без скобок и двоеточий, так надежнее)
            byte[] pattern = "\"id\"".getBytes();
            int matchIndex = 0;

            while (true) {
                int c = raf.read();
                if (c == -1) return raf.getFilePointer(); // Конец файла

                if (c == pattern[matchIndex]) {
                    matchIndex++;
                    if (matchIndex == pattern.length) {
                        // Мы нашли "id". Теперь нужно отмотать назад до начала этого объекта '{'
                        long foundIdPos = raf.getFilePointer();
                        for (long p = foundIdPos - 1; p >= 0; p--) {
                            raf.seek(p);
                            if (raf.read() == '{') {
                                return p; // Нашли начало объекта!
                            }
                        }
                        return foundIdPos;
                    }
                } else {
                    matchIndex = (c == pattern[0]) ? 1 : 0;
                }
            }
        }
    }
}