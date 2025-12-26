package com.parser.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncDataProcessor {

    private ExecutorService executor = Executors.newFixedThreadPool(4);

    public CompletableFuture<AnalysisResult> processData(String data) {
        return CompletableFuture.supplyAsync(() -> {
            // Ваша логика обработки данных
            return new AnalysisResult();
        }, executor);
    }

    public void shutdown() {
        executor.shutdown();
    }
}