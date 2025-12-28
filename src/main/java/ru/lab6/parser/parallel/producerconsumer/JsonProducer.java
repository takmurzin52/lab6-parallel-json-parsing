package ru.lab6.parser.parallel.producerconsumer;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class JsonProducer implements Runnable {
    private final String filePath;
    private final BlockingQueue<String> jsonQueue;
    private final String poisonPill;
    private final AtomicInteger activeProducers;
    private final int numConsumers;

    public JsonProducer(String filePath, BlockingQueue<String> jsonQueue,
                        String poisonPill, AtomicInteger activeProducers,
                        int numConsumers) {
        this.filePath = filePath;
        this.jsonQueue = jsonQueue;
        this.poisonPill = poisonPill;
        this.activeProducers = activeProducers;
        this.numConsumers = numConsumers;
    }

    @Override
    public void run() {
        String threadName = "Producer-" + Thread.currentThread().getId();
        System.out.println(threadName + " started reading: " + filePath);

        int objectsRead = 0;
        long startTime = System.currentTimeMillis();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder currentObject = new StringBuilder();
            boolean inObject = false;
            int braceCount = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                // Пропускаем пустые строки
                if (trimmedLine.isEmpty()) {
                    continue;
                }

                // Начало массива
                if (trimmedLine.equals("[")) {
                    continue;
                }

                // Конец массива
                if (trimmedLine.equals("]")) {
                    break;
                }

                // Обрабатываем JSON объекты
                for (int i = 0; i < trimmedLine.length(); i++) {
                    char c = trimmedLine.charAt(i);

                    if (c == '{') {
                        if (!inObject) {
                            inObject = true;
                            currentObject = new StringBuilder();
                        }
                        braceCount++;
                    }

                    if (inObject) {
                        currentObject.append(c);
                    }

                    if (c == '}') {
                        braceCount--;
                        if (braceCount == 0 && inObject) {
                            // Нашли полный JSON объект
                            String jsonString = currentObject.toString();

                            // Убираем запятую в конце, если есть
                            if (jsonString.endsWith(",")) {
                                jsonString = jsonString.substring(0, jsonString.length() - 1);
                            }

                            // Кладем в очередь
                            jsonQueue.put(jsonString);
                            objectsRead++;
                            inObject = false;

                            // Прогресс каждые 10000 объектов
                            if (objectsRead % 10000 == 0) {
                                System.out.println(threadName + " queued: " + objectsRead + " objects");
                            }
                        }
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            System.out.println(threadName + " finished. Total objects: " + objectsRead +
                    " (" + (endTime - startTime) + " ms)");

            // Отправляем poison pill для каждого Consumer'а
            System.out.println(threadName + " sending " + numConsumers + " poison pills");
            for (int i = 0; i < numConsumers; i++) {
                jsonQueue.put(poisonPill);
            }

        } catch (FileNotFoundException e) {
            System.err.println(threadName + " ERROR: File not found - " + filePath);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(threadName + " ERROR: IO error - " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println(threadName + " interrupted");
            Thread.currentThread().interrupt();
        } finally {
            activeProducers.decrementAndGet();
            System.out.println(threadName + " completed, active producers: " + activeProducers.get());
        }
    }
}
