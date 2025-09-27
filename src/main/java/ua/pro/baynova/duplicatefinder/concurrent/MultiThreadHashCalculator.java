package ua.pro.baynova.duplicatefinder.concurrent;

import ua.pro.baynova.duplicatefinder.hash.HashCalculator;
import ua.pro.baynova.duplicatefinder.model.FileInfo;
import ua.pro.baynova.duplicatefinder.scanner.FileScanner;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MultiThreadHashCalculator {

    private final int numberOfThreads;
    private final String algorithm;

    private final AtomicInteger processedFiles = new AtomicInteger(0);
    private final AtomicInteger failedFiles = new AtomicInteger(0);
    private final AtomicLong totalBytes = new AtomicLong(0);

    public MultiThreadHashCalculator() {
        this(Runtime.getRuntime().availableProcessors(), "MD5");
    }

    public MultiThreadHashCalculator(int numberOfThreads, String algorithm) {
        this.numberOfThreads = numberOfThreads;
        this.algorithm = algorithm;

        System.out.println("MultiThreadHashCalculator создан:");
        System.out.println("  Потоков: " + numberOfThreads);
        System.out.println("  Алгоритм: " + algorithm);
        System.out.println("  Доступно ядер процессора: " + Runtime.getRuntime().availableProcessors());
    }

    /**
     * Обрабатывает список файлов параллельно
     * @param fileTasks список файлов для обработки
     * @return список FileInfo с вычисленными хешами
     */
    public List<FileInfo> processFiles(List<FileScanner.FileTask> fileTasks)
            throws InterruptedException {

        if (fileTasks == null || fileTasks.isEmpty()) {
            return new ArrayList<>();
        }

        System.out.println("\nНачинаем параллельную обработку " + fileTasks.size() + " файлов...");
        long startTime = System.currentTimeMillis();

        processedFiles.set(0);
        failedFiles.set(0);
        totalBytes.set(0);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<FileInfo>> futures = new ArrayList<>();

        try {
            for (FileScanner.FileTask task : fileTasks) {
                Future<FileInfo> future = executor.submit(new HashingTask(task));
                futures.add(future);
            }

            List<FileInfo> results = collectResults(futures, startTime);

            return results;

        } finally {
            shutdownExecutor(executor);
        }
    }

    private List<FileInfo> collectResults(List<Future<FileInfo>> futures, long startTime) {
        List<FileInfo> results = new ArrayList<>();
        int completed = 0;

        for (Future<FileInfo> future : futures) {
            try {
                FileInfo result = future.get();
                if (result != null) {
                    results.add(result);
                }

                completed++;

                if (completed % Math.max(1, futures.size() / 4) == 0) {
                    double progress = (100.0 * completed) / futures.size();
                    long elapsed = System.currentTimeMillis() - startTime;
                    System.out.printf("Прогресс: %.1f%% (%d/%d файлов, %d сек)%n",
                            progress, completed, futures.size(), elapsed / 1000);
                }

            } catch (ExecutionException e) {
                failedFiles.incrementAndGet();
                System.err.println("Ошибка обработки файла: " + e.getCause().getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Обработка прервана");
                break;
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        printStatistics(totalTime, results.size());

        return results;
    }

    private void shutdownExecutor(ExecutorService executor) throws InterruptedException {
        executor.shutdown();

        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            System.out.println("Принудительно останавливаем потоки...");
            executor.shutdownNow();

            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Не удалось корректно остановить все потоки");
            }
        }
    }

    private void printStatistics(long totalTimeMs, int successCount) {
        System.out.println("\n=== СТАТИСТИКА МНОГОПОТОЧНОЙ ОБРАБОТКИ ===");
        System.out.printf("Успешно обработано: %d файлов%n", successCount);
        System.out.printf("Ошибок: %d%n", failedFiles.get());
        System.out.printf("Общий размер: %s%n", formatBytes(totalBytes.get()));
        System.out.printf("Время выполнения: %.2f сек%n", totalTimeMs / 1000.0);

        if (totalTimeMs > 0) {
            double filesPerSecond = (double) successCount * 1000 / totalTimeMs;
            double mbPerSecond = (double) totalBytes.get() / (1024 * 1024) * 1000 / totalTimeMs;
            System.out.printf("Производительность: %.1f файлов/сек, %.1f MB/сек%n",
                    filesPerSecond, mbPerSecond);
        }

        System.out.printf("Использовано потоков: %d%n", numberOfThreads);
    }

    private class HashingTask implements Callable<FileInfo> {
        private final FileScanner.FileTask fileTask;

        public HashingTask(FileScanner.FileTask fileTask) {
            this.fileTask = fileTask;
        }

        @Override
        public FileInfo call() throws Exception {
            String threadName = Thread.currentThread().getName();
            File file = new File(fileTask.getPath());

            try {
                HashCalculator calculator = new HashCalculator(algorithm);
                String hash = calculator.calculateHash(file);

                FileInfo fileInfo = new FileInfo(
                        fileTask.getPath(),
                        hash,
                        fileTask.getSize(),
                        fileTask.getLastModified(),
                        LocalDateTime.now()
                );

                processedFiles.incrementAndGet();
                totalBytes.addAndGet(fileTask.getSize());

                if (fileTask.getSize() > 1024 * 1024) {
                    System.out.printf("[%s] Обработан большой файл: %s (%.1f MB)%n",
                            threadName, file.getName(), fileTask.getSize() / (1024.0 * 1024));
                }

                return fileInfo;

            } catch (Exception e) {
                failedFiles.incrementAndGet();
                System.err.printf("[%s] Ошибка обработки %s: %s%n",
                        threadName, file.getName(), e.getMessage());
                throw e;
            }
        }
    }

    public ComparisonResult comparePerformance(List<FileScanner.FileTask> fileTasks)
            throws InterruptedException {

        System.out.println("\n=== СРАВНЕНИЕ ПРОИЗВОДИТЕЛЬНОСТИ ===");

        System.out.println("Тест 1: Однопоточная обработка...");
        long singleThreadTime = measureSingleThreadPerformance(fileTasks);

        System.out.println("\nТест 2: Многопоточная обработка...");
        long multiThreadTime = measureMultiThreadPerformance(fileTasks);

        double speedup = (double) singleThreadTime / multiThreadTime;
        double efficiency = speedup / numberOfThreads * 100;

        return new ComparisonResult(singleThreadTime, multiThreadTime, speedup, efficiency);
    }

    private long measureSingleThreadPerformance(List<FileScanner.FileTask> fileTasks) {
        long startTime = System.currentTimeMillis();

        HashCalculator calculator = new HashCalculator(algorithm);
        int processed = 0;

        for (FileScanner.FileTask task : fileTasks) {
            try {
                calculator.calculateHash(task.getPath());
                processed++;
            } catch (Exception e) {
                // Игнорируем ошибки для чистоты эксперимента
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("Однопоточно: %d файлов за %.2f сек%n", processed, duration / 1000.0);

        return duration;
    }

    private long measureMultiThreadPerformance(List<FileScanner.FileTask> fileTasks)
            throws InterruptedException {
        long startTime = System.currentTimeMillis();

        List<FileInfo> results = processFiles(fileTasks);

        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("Многопоточно: %d файлов за %.2f сек%n", results.size(), duration / 1000.0);

        return duration;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static class ComparisonResult {
        private final long singleThreadTimeMs;
        private final long multiThreadTimeMs;
        private final double speedup;
        private final double efficiency;

        public ComparisonResult(long singleThreadTimeMs, long multiThreadTimeMs,
                                double speedup, double efficiency) {
            this.singleThreadTimeMs = singleThreadTimeMs;
            this.multiThreadTimeMs = multiThreadTimeMs;
            this.speedup = speedup;
            this.efficiency = efficiency;
        }

        public long getSingleThreadTimeMs() { return singleThreadTimeMs; }
        public long getMultiThreadTimeMs() { return multiThreadTimeMs; }
        public double getSpeedup() { return speedup; }
        public double getEfficiency() { return efficiency; }

        @Override
        public String toString() {
            return String.format(
                    "Результаты сравнения:\n" +
                            "  Однопоточно: %.2f сек\n" +
                            "  Многопоточно: %.2f сек\n" +
                            "  Ускорение: %.2fx\n" +
                            "  Эффективность: %.1f%%",
                    singleThreadTimeMs / 1000.0,
                    multiThreadTimeMs / 1000.0,
                    speedup,
                    efficiency
            );
        }
    }
}
