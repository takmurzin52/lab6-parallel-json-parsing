package com.parser.async;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class BufferedReaderWrapper {

    private BufferedReader reader;

    public BufferedReaderWrapper(String filePath) {
        try {
            reader = new BufferedReader(new FileReader(filePath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String readNextBatch() {
        // Реализация чтения порции данных
        return "";
    }

    public boolean hasNext() {
        // Проверка наличия данных
        return false;
    }
}
