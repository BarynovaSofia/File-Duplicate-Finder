package ua.pro.baynova.duplicatefinder.concurrent;

import ua.pro.baynova.duplicatefinder.model.FileInfo;
import ua.pro.baynova.duplicatefinder.scanner.FileScanner;
import java.util.List;

public class MultiThreadHashTest {

    public static void main(String[] args) {
        System.out.println("=== Тест многопоточного хеширования ===\n");

        try {
            testBasicMultithreading();
            testPerformanceComparison();

            System.out.println("\n✅ Многопоточное хеширование работает!");

        } catch (Exception e) {
            System.err.println("❌ Ошибка теста: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testBasicMultithreading() throws Exception {
        System.out.println("--- Тест 1: Базовая многопоточная обработка ---");

        FileScanner scanner = new FileScanner(
                FileScanner.Filters.byExtensions(".java", ".xml"),
                false,
                5
        );

        List<FileScanner.FileTask> fileTasks = scanner.scanDirectory(".");
        System.out.println("Найдено файлов: " + fileTasks.size());

        if (fileTasks.size() < 3) {
            System.out.println("Слишком мало файлов для демонстрации многопоточности");
            return;
        }

        MultiThreadHashCalculator calculator = new MultiThreadHashCalculator(3, "MD5");

        List<FileInfo> results = calculator.processFiles(fileTasks);

        System.out.println("\nРезультаты обработки:");
        results.stream()
                .limit(5)
                .forEach(fileInfo -> {
                    System.out.printf("  %s -> %s...%n",
                            fileInfo.getFileName(),
                            fileInfo.getHash().substring(0, 8));
                });

        if (results.size() > 5) {
            System.out.println("  ... и еще " + (results.size() - 5) + " файлов");
        }

        System.out.println();
    }

    private static void testPerformanceComparison() throws Exception {
        System.out.println("--- Тест 2: Сравнение производительности ---");

        FileScanner scanner = new FileScanner();
        List<FileScanner.FileTask> allFiles = scanner.scanDirectory(".");

        List<FileScanner.FileTask> testFiles = allFiles.subList(0, Math.min(15, allFiles.size()));

        System.out.println("Тестируем на " + testFiles.size() + " файлах");
        System.out.println("Общий размер: " + calculateTotalSize(testFiles) + " байт");

        int[] threadCounts = {1, 2, 4, Runtime.getRuntime().availableProcessors()};

        for (int threads : threadCounts) {
            System.out.println("\n--- " + threads + " поток(ов) ---");

            MultiThreadHashCalculator calculator = new MultiThreadHashCalculator(threads, "MD5");

            long startTime = System.currentTimeMillis();
            List<FileInfo> results = calculator.processFiles(testFiles);
            long duration = System.currentTimeMillis() - startTime;

            System.out.printf("Результат: %d файлов за %.2f сек%n",
                    results.size(), duration / 1000.0);
        }
    }

    private static long calculateTotalSize(List<FileScanner.FileTask> files) {
        return files.stream().mapToLong(FileScanner.FileTask::getSize).sum();
    }
}