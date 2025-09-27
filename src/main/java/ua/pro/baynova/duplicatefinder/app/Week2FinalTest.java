package ua.pro.baynova.duplicatefinder.app;

import ua.pro.baynova.duplicatefinder.concurrent.MultiThreadFileIndexer;

public class Week2FinalTest {

    public static void main(String[] args) {
        System.out.println("=== ИТОГОВЫЙ ТЕСТ НЕДЕЛИ 2 ===");
        System.out.println("Многопоточная обработка файлов");
        System.out.println();

        try {
            testMultiThreadVersion();

            compareWithSingleThread();

            System.out.println("\n🚀 НЕДЕЛЯ 2 ЗАВЕРШЕНА УСПЕШНО!");
            System.out.println("Многопоточная обработка реализована и работает");

        } catch (Exception e) {
            System.err.println("❌ Ошибка теста: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testMultiThreadVersion() throws Exception {
        System.out.println("--- Тест многопоточной версии ---");

        int[] threadCounts = {1, 2, 4, Runtime.getRuntime().availableProcessors()};
        String testDirectory = ".";

        for (int threads : threadCounts) {
            System.out.printf("\n=== ТЕСТ С %d ПОТОКАМИ ===%n", threads);

            MultiThreadFileIndexer indexer = new MultiThreadFileIndexer(threads);
            long startTime = System.currentTimeMillis();

            MultiThreadFileIndexer.IndexingResult result = indexer.indexDirectory(testDirectory);

            long duration = System.currentTimeMillis() - startTime;

            System.out.printf("\nРЕЗУЛЬТАТ (%d потоков):%n", threads);
            System.out.printf("  Файлов обработано: %d%n", result.getProcessedCount());
            System.out.printf("  Групп дубликатов: %d%n", result.getDuplicateGroupsCount());
            System.out.printf("  Общее время: %.2f сек%n", duration / 1000.0);
            System.out.printf("  Производительность: %.1f файлов/сек%n", result.getOverallPerformance());
            System.out.println("─".repeat(50));
        }
    }

    private static void compareWithSingleThread() throws Exception {
        System.out.println("\n--- Сравнение производительности ---");
        System.out.println("Однопоточная vs Многопоточная обработка");

        String testDirectory = ".";

        System.out.println("\nОднопоточная версия:");
        MultiThreadFileIndexer singleThread = new MultiThreadFileIndexer(1);
        long startTime = System.currentTimeMillis();
        MultiThreadFileIndexer.IndexingResult singleResult = singleThread.indexDirectory(testDirectory);
        long singleTime = System.currentTimeMillis() - startTime;

        System.out.println("\nМногопоточная версия:");
        int optimalThreads = Runtime.getRuntime().availableProcessors();
        MultiThreadFileIndexer multiThread = new MultiThreadFileIndexer(optimalThreads);
        startTime = System.currentTimeMillis();
        MultiThreadFileIndexer.IndexingResult multiResult = multiThread.indexDirectory(testDirectory);
        long multiTime = System.currentTimeMillis() - startTime;

        System.out.println("\n" + "=".repeat(60));
        System.out.println("СРАВНЕНИЕ ПРОИЗВОДИТЕЛЬНОСТИ");
        System.out.println("=".repeat(60));

        System.out.printf("Однопоточная версия:  %.2f сек%n", singleTime / 1000.0);
        System.out.printf("Многопоточная версия: %.2f сек (%d потоков)%n", multiTime / 1000.0, optimalThreads);

        if (singleTime > multiTime) {
            double speedup = (double) singleTime / multiTime;
            double efficiency = speedup / optimalThreads * 100;

            System.out.printf("Ускорение: %.2fx%n", speedup);
            System.out.printf("Эффективность: %.1f%%%n", efficiency);

            if (speedup > 1.5) {
                System.out.println("✅ Многопоточность дает заметное ускорение!");
            } else {
                System.out.println("⚠️ Небольшое ускорение - возможно файлы слишком маленькие");
            }
        } else {
            System.out.println("⚠️ Многопоточная версия работает медленнее");
            System.out.println("   Это может происходить при обработке очень маленьких файлов");
        }

        System.out.println("\nОба результата должны быть идентичными по содержанию:");
        System.out.printf("  Файлов: %d vs %d%n",
                singleResult.getProcessedCount(), multiResult.getProcessedCount());
        System.out.printf("  Дубликатов: %d vs %d%n",
                singleResult.getDuplicateGroupsCount(), multiResult.getDuplicateGroupsCount());

        boolean resultsMatch = singleResult.getProcessedCount() == multiResult.getProcessedCount()
                && singleResult.getDuplicateGroupsCount() == multiResult.getDuplicateGroupsCount();

        if (resultsMatch) {
            System.out.println("✅ Результаты идентичны - многопоточность работает корректно");
        } else {
            System.out.println("❌ Результаты различаются - возможна ошибка в многопоточном коде");
        }
    }
}
