package ru.lab6.parser.parallel.producerconsumer;

import ru.lab6.model.AnalysisResult;
import ru.lab6.model.User;
import ru.lab6.model.Post;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ProducerConsumerParser {
    private static final String POISON_PILL = "__POISON_PILL__";
    private static final int QUEUE_CAPACITY = 10000;
    private static final int NUM_CONSUMERS = Runtime.getRuntime().availableProcessors();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisResult parse(String filePath) throws Exception {
        System.out.println("=== PRODUCER-CONSUMER JSON PARSER ===");
        System.out.println("Stage 2.3: Producer-Consumer with queues pattern");
        System.out.println("File: " + filePath);
        System.out.println("Threads: " + NUM_CONSUMERS + " consumers + 1 producer");
        System.out.println("Queue capacity: " + QUEUE_CAPACITY);
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println();

        long startTime = System.currentTimeMillis();

        // Создаем очереди
        BlockingQueue<String> jsonQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<PartialResult> resultQueue = new LinkedBlockingQueue<>();
        AtomicInteger activeProducers = new AtomicInteger(1);

        // Создаем пул потоков
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CONSUMERS + 1);

        // Запускаем Consumer'ов первыми
        System.out.println("Starting " + NUM_CONSUMERS + " consumers...");
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            executor.submit(new JsonConsumer(i, jsonQueue, resultQueue, activeProducers, POISON_PILL));
        }

        // Даем время Consumer'ам запуститься
        Thread.sleep(100);

        // Запускаем Producer
        System.out.println("Starting producer...");
        executor.submit(new JsonProducer(filePath, jsonQueue, POISON_PILL, activeProducers, NUM_CONSUMERS));

        // Собираем результаты от всех Consumer'ов
        System.out.println("Main thread waiting for results...");
        PartialResult finalPartialResult = new PartialResult();
        int completedConsumers = 0;

        while (completedConsumers < NUM_CONSUMERS) {
            try {
                PartialResult partialResult = resultQueue.poll(60, TimeUnit.SECONDS);
                if (partialResult != null) {
                    finalPartialResult.merge(partialResult);
                    completedConsumers++;
                    System.out.println("Received result from consumer " + completedConsumers + "/" + NUM_CONSUMERS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Преобразуем PartialResult в AnalysisResult
        AnalysisResult finalResult = convertToAnalysisResult(finalPartialResult);

        // Завершаем ExecutorService
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n=== PARSING COMPLETE ===");
        System.out.println("Total time: " + duration + " ms");
        System.out.println("Users processed: " + finalPartialResult.userCount);
        System.out.println("Rate: " + String.format("%.2f", finalPartialResult.userCount / (duration / 1000.0)) + " users/sec");

        return finalResult;
    }

    // Преобразование PartialResult в AnalysisResult
    private AnalysisResult convertToAnalysisResult(PartialResult partial) {
        AnalysisResult result = new AnalysisResult();

        // Копируем распределение по городам
        result.cityDistribution = new HashMap<>(partial.cityDistribution);

        // Вычисляем среднее количество лайков
        result.avgLikesPerUser = partial.userCount > 0 ?
                (double) partial.totalLikes / partial.userCount : 0.0;

        // Вычисляем топ-10 тегов
        result.top10Tags = new ArrayList<>();
        partial.tagFrequency.entrySet().stream()
                .map(entry -> new AnalysisResult.TagCount(entry.getKey(), entry.getValue()))
                .sorted()  // TagCount уже реализует Comparable (по убыванию)
                .limit(10)
                .forEach(result.top10Tags::add);

        return result;
    }

    // Внутренний класс для хранения промежуточных результатов
    private static class PartialResult {
        int userCount = 0;
        long totalLikes = 0;
        Map<String, Long> cityDistribution = new HashMap<>();
        Map<String, Long> tagFrequency = new HashMap<>();

        void merge(PartialResult other) {
            this.userCount += other.userCount;
            this.totalLikes += other.totalLikes;

            other.cityDistribution.forEach((city, count) ->
                    this.cityDistribution.merge(city, count, Long::sum));

            other.tagFrequency.forEach((tag, count) ->
                    this.tagFrequency.merge(tag, count, Long::sum));
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ProducerConsumerParser <json-file>");
            System.out.println("Example: java ProducerConsumerParser data/users.json");
            System.out.println("\nFirst generate test data:");
            System.out.println("  mvn exec:java -Dexec.mainClass=\"ru.lab6.generator.DataGenerator\" \\");
            System.out.println("    -Dexec.args=\"10000 data/users.json\"");
            System.exit(1);
        }

        try {
            ProducerConsumerParser parser = new ProducerConsumerParser();
            AnalysisResult result = parser.parse(args[0]);

            // Выводим результаты анализа
            System.out.println("\n=== ANALYSIS RESULTS ===");
            System.out.println("Total users: " + getTotalUsers(result));
            System.out.println("Total likes: " + getTotalLikes(result));
            System.out.printf("Average likes per user: %.2f%n", result.avgLikesPerUser);
            System.out.println("Unique cities: " + result.cityDistribution.size());

            System.out.println("\nTop 10 cities by users:");
            result.cityDistribution.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .limit(10)
                    .forEach(entry -> System.out.printf("  %s: %d users%n", entry.getKey(), entry.getValue()));

            System.out.println("\nTop 10 popular tags:");
            result.top10Tags.forEach(tag -> System.out.println("  " + tag));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Вспомогательные методы для получения статистики
    private static int getTotalUsers(AnalysisResult result) {
        return result.cityDistribution.values().stream()
                .mapToInt(Long::intValue)
                .sum();
    }

    private static long getTotalLikes(AnalysisResult result) {
        return (long) (result.avgLikesPerUser * getTotalUsers(result));
    }
}
