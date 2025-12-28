package ru.lab6;

import ru.lab6.model.AnalysisResult;
import ru.lab6.parser.parallel.JsonChunkParserTask;
import ru.lab6.parser.sequential.SequentialParser;

import java.io.File;
import java.util.concurrent.ForkJoinPool;

public class BenchmarkRunner {

    public static void runTests(String filePath) throws Exception {
        File file = new File(filePath);
        System.out.println("\n=== НАГРУЗОЧНОЕ ТЕСТИРОВАНИЕ ===");
        System.out.println("Файл: " + filePath + " (" + (file.length() / 1024 / 1024) + " MB)");

        SequentialParser seqParser = new SequentialParser();
        ForkJoinPool pool = new ForkJoinPool();

        // 1. ПРОГРЕВ (Warm-up) - 3 прогона без замера времени
        System.out.print("Прогрев JVM...");
        for (int i = 0; i < 3; i++) {
            seqParser.parse(file);
            pool.invoke(new JsonChunkParserTask(file, 0, file.length()));
        }
        System.out.println(" Готово.");

        // 2. ТЕСТ ПОСЛЕДОВАТЕЛЬНОЙ РЕАЛИЗАЦИИ
        long seqTotalTime = 0;
        int iterations = 5; // Сделаем 5 замеров для точности
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            seqParser.parse(file);
            seqTotalTime += (System.nanoTime() - start);
        }
        double avgSeqTime = (seqTotalTime / iterations) / 1_000_000.0;
        System.out.printf("Последовательно (среднее): %.2f мс%n", avgSeqTime);

        // 3. ТЕСТ FORKJOINPOOL
        long fjTotalTime = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            JsonChunkParserTask task = new JsonChunkParserTask(file, 0, file.length());
            pool.invoke(task);
            fjTotalTime += (System.nanoTime() - start);
        }
        double avgFjTime = (fjTotalTime / iterations) / 1_000_000.0;
        System.out.printf("ForkJoinPool (среднее):   %.2f мс%n", avgFjTime);

        // 4. РАСЧЕТ УСКОРЕНИЯ
        double speedup = avgSeqTime / avgFjTime;
        int cores = Runtime.getRuntime().availableProcessors();
        double efficiency = (speedup / cores) * 100;

        System.out.println("---------------------------------");
        System.out.printf("Ускорение (Speedup): %.2f x%n", speedup);
        System.out.printf("Доступно ядер: %d%n", cores);
        System.out.printf("Эффективность: %.2f%%%n", efficiency);
    }
}