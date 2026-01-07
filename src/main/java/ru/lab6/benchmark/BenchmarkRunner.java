package ru.lab6.benchmark;

import ru.lab6.model.AnalysisResult;
import ru.lab6.parser.sequential.SequentialParser;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class BenchmarkRunner {

    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    public static void main(String[] args) throws Exception {
        String[] files = {"data/test_10k.json", "data/test_100k.json"};
        for (String file : files) {
            System.out.println("\n=== Benchmark: " + file + " ===");
            warmup(file); // Прогрев JVM
            runBenchmarks(file);
        }
    }

    private static void warmup(String filePath) throws Exception {
        System.out.println("[W] Warming up...");
        for (int i = 0; i < 3; i++) {
            new SequentialParser().parse(new File(filePath));
        }
    }

    private static void runBenchmarks(String filePath) throws Exception {
        List<BenchmarkResult> results = new ArrayList<>();

        // Добавьте сюда новые реализации по мере готовности:
        results.add(benchmark("Sequential", () -> {
            try {
                return new SequentialParser().parse(new File(filePath));
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse file: " + filePath, e);
            }
        }));

        // TODO: Раскомментировать позже:
        // results.add(benchmark("ForkJoin",     () -> new ru.lab6.parser.forkjoin.ForkJoinParser().parse(new File(filePath))));
        // results.add(benchmark("CompletableFuture", () -> new ru.lab6.parser.async.CompletableFutureParser().parse(new File(filePath))));
        // results.add(benchmark("ProducerConsumer", () -> new ru.lab6.parser.producer.ProducerConsumerParser().parse(new File(filePath))));

        // Вывод таблицы
        System.out.printf("%-20s | %8s | %10s | %6s%n", "Mode", "Time, ms", "Peak Mem, MB", "Speedup");
        System.out.println("-".repeat(55));
        double baseline = results.get(0).timeMs;
        for (BenchmarkResult r : results) {
            double speedup = baseline / r.timeMs;
            System.out.printf("%-20s | %8.0f | %10.1f | %6.2fx%n", r.name, r.timeMs, r.peakMemoryMB, speedup);
        }
    }

    private static BenchmarkResult benchmark(String name, Supplier<AnalysisResult> parser) {
        System.gc(); // Подсказка GC (не гарантирует, но помогает)
        long startMem = getUsedMemoryMB();
        long start = System.nanoTime();

        AnalysisResult result = parser.get();

        long timeMs = (System.nanoTime() - start) / 1_000_000;
        long peakMem = getUsedMemoryMB() - startMem;

        return new BenchmarkResult(name, timeMs, peakMem, result);
    }

    private static long getUsedMemoryMB() {
        memoryBean.gc(); // попытка собрать мусор для точности
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        return heap.getUsed() / (1024 * 1024);
    }

    private static class BenchmarkResult {
        final String name;
        final double timeMs;
        final double peakMemoryMB;
        final AnalysisResult result;

        BenchmarkResult(String name, double timeMs, double peakMemoryMB, AnalysisResult result) {
            this.name = name;
            this.timeMs = timeMs;
            this.peakMemoryMB = peakMemoryMB;
            this.result = result;
        }
    }
}