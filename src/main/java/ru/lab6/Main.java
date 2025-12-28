package ru.lab6;

import ru.lab6.generator.DataGenerator;
import ru.lab6.model.AnalysisResult;
import ru.lab6.parser.sequential.SequentialParser;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || !args[0].equals("--mode")) {
            System.err.println("Usage: Main --mode <sequential|generate> [args...]");
            return;
        }

        String mode = args[1];
        if ("generate".equals(mode)) {
            if (args.length < 4) {
                System.err.println("Usage: --mode generate <count> <output.json>");
                return;
            }
            int count = Integer.parseInt(args[2]);
            String outputPath = args[3];
            DataGenerator.generateJson(count, outputPath);
            System.out.println("✅ Generated " + count + " users → " + outputPath);

        } else if ("sequential".equals(mode)) {
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
                AnalysisResult.TagCount tc = result.top10Tags.get(i);
                System.out.printf("  %2d. %s → %d%n", i + 1, tc.tag, tc.count);
            }

        } else {
            System.err.println("Unknown mode: " + mode);
        }
    }
}