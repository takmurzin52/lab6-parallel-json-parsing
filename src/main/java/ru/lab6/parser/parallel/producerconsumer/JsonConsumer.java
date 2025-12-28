package ru.lab6.parser.parallel.producerconsumer;

import ru.lab6.model.User;
import ru.lab6.model.Post;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.Map;

public class JsonConsumer implements Runnable {
    private final int consumerId;
    private final BlockingQueue<String> jsonQueue;
    private final BlockingQueue<ProducerConsumerParser.PartialResult> resultQueue;
    private final AtomicInteger activeProducers;
    private final String poisonPill;
    private final ObjectMapper objectMapper;

    public JsonConsumer(int consumerId, BlockingQueue<String> jsonQueue,
                        BlockingQueue<ProducerConsumerParser.PartialResult> resultQueue,
                        AtomicInteger activeProducers, String poisonPill) {
        this.consumerId = consumerId;
        this.jsonQueue = jsonQueue;
        this.resultQueue = resultQueue;
        this.activeProducers = activeProducers;
        this.poisonPill = poisonPill;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void run() {
        String threadName = "Consumer-" + consumerId;
        System.out.println(threadName + " started");

        ProducerConsumerParser.PartialResult partialResult = new ProducerConsumerParser.PartialResult();
        int processedCount = 0;
        long startTime = System.currentTimeMillis();

        try {
            while (true) {
                String jsonLine = jsonQueue.take();

                // Проверяем poison pill
                if (jsonLine.equals(poisonPill)) {
                    System.out.println(threadName + " received poison pill, processed: " + processedCount);

                    // Передаем poison pill следующему Consumer'у
                    jsonQueue.put(poisonPill);

                    // Если все Producer'ы завершили, отправляем результат
                    if (activeProducers.get() == 0) {
                        resultQueue.put(partialResult);
                        System.out.println(threadName + " sending partial result to main thread");
                    }
                    break;
                }

                try {
                    // Парсим JSON строку в объект User
                    User user = objectMapper.readValue(jsonLine, User.class);

                    // Обрабатываем пользователя
                    processUser(partialResult, user);
                    processedCount++;

                    // Прогресс каждые 5000 объектов
                    if (processedCount % 5000 == 0) {
                        long currentTime = System.currentTimeMillis();
                        double rate = processedCount / ((currentTime - startTime) / 1000.0);
                        System.out.printf("%s processed: %d objects (%.1f objects/sec)%n",
                                threadName, processedCount, rate);
                    }

                } catch (Exception e) {
                    // Игнорируем ошибки парсинга для отдельных объектов
                    System.err.println(threadName + " WARN: Failed to parse JSON: " +
                            e.getMessage().substring(0, Math.min(50, e.getMessage().length())));
                }
            }
        } catch (InterruptedException e) {
            System.out.println(threadName + " interrupted");
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double rate = processedCount / (duration / 1000.0);

        System.out.printf("%s finished. Total: %d objects in %d ms (%.1f objects/sec)%n",
                threadName, processedCount, duration, rate);
    }

    private void processUser(ProducerConsumerParser.PartialResult result, User user) {
        // Увеличиваем счетчик пользователей
        result.userCount++;

        // Обновляем распределение по городам
        if (user.city != null && !user.city.isEmpty()) {
            result.cityDistribution.merge(user.city, 1L, Long::sum);
        }

        // Обрабатываем посты пользователя
        if (user.posts != null) {
            for (Post post : user.posts) {
                // Добавляем лайки
                result.totalLikes += post.likes;

                // Добавляем теги в частотный словарь
                if (post.tags != null) {
                    for (String tag : post.tags) {
                        if (tag != null && !tag.isEmpty()) {
                            result.tagFrequency.merge(tag, 1L, Long::sum);
                        }
                    }
                }
            }
        }
    }
}
