package ru.lab6;

import ru.lab6.benchmark.BenchmarkRunner;
import ru.lab6.generator.DataGenerator;
import ru.lab6.model.AnalysisResult;
import ru.lab6.parser.sequential.SequentialParser;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || !args[0].equals("--mode")) {
            printUsage();
            return;
        }

        String mode = args[1];
        switch (mode) {
            case "generate" -> handleGenerate(args);
            case "sequential" -> handleSequential(args);
            case "benchmark" -> BenchmarkRunner.main(new String[0]);
            default -> {
                System.err.println("Unknown mode: " + mode);
                printUsage();
            }
        }
    }

    private static void handleGenerate(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("Usage: --mode generate <count> <output.json>");
            return;
        }
        int count = Integer.parseInt(args[2]);
        String outputPath = args[3];
        DataGenerator.generateJson(count, outputPath);
        System.out.println("✅ Generated " + count + " users → " + outputPath);
    }

    private static void handleSequential(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: --mode sequential <input.json>");
            return;
        }
        String inputPath = args[2];
        SequentialParser parser = new SequentialParser();
        AnalysisResult result = parser.parse(new File(inputPath));

        System.out.println("=== Последовательный парсер ===");
        System.out.println("Города: " + result.cityDistribution);
        System.out.printf("Среднее лайков на пользователя: %.2f%n", result.avgLikesPerUser);
        System.out.println("Топ-10 тегов:");
        for (int i = 0; i < result.top10Tags.size(); i++) {
            var tc = result.top10Tags.get(i);
            System.out.printf("  %2d. %-8s → %d%n", i + 1, tc.tag, tc.count);
        }
    }

    private static void printUsage() {
        System.out.println("""
                Usage: Main --mode <mode> [args...]
                
                Modes:
                  generate <count> <output.json>   — generate test data
                  sequential <input.json>          — run sequential parser
                  benchmark                        — run performance comparison
                """);
    }
}