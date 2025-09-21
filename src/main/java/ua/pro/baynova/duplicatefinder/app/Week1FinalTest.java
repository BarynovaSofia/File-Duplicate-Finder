package ua.pro.baynova.duplicatefinder.app;

import ua.pro.baynova.duplicatefinder.hash.HashCalculator;
import ua.pro.baynova.duplicatefinder.index.SimpleFileIndex;
import ua.pro.baynova.duplicatefinder.model.FileInfo;
import ua.pro.baynova.duplicatefinder.scanner.FileScanner;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

public class Week1FinalTest {
    public static void main(String[] args) {
        System.out.println("=== ИТОГОВЫЙ ТЕСТ НЕДЕЛИ 1 ===");
        System.out.println("Полный цикл поиска дубликатов файлов\n");

        try {
            createTestFiles();
            runFullDuplicateSearch(".");

            System.out.println("\n🎉 НЕДЕЛЯ 1 ЗАВЕРШЕНА УСПЕШНО!");
            System.out.println("Базовая функциональность работает в одном потоке");

        } catch (Exception e) {
            System.err.println("❌ Ошибка в итоговом тесте: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createTestFiles(){
        System.out.println("--- Подготовка тестовых данных ---");

        try {
            File testDir = new File("test_duplicates");
            if (!testDir.exists()) {
                testDir.mkdir();
            }

            String duplicateContent = "This is duplicate content for testing duplicate detection";

            createTestFile(new File(testDir, "original.txt"), duplicateContent);
            createTestFile(new File(testDir, "copy1.txt"), duplicateContent);
            createTestFile(new File(testDir, "copy2.txt"), duplicateContent);

            createTestFile(new File(testDir, "unique1.txt"), "Unique content 1");
            createTestFile(new File(testDir, "unique2.txt"), "Unique content 2");

            String anotherDuplicate = "Another set of duplicate files for comprehensive testing";
            createTestFile(new File(testDir, "another1.txt"), anotherDuplicate);
            createTestFile(new File(testDir, "another2.txt"), anotherDuplicate);

            System.out.println("Создано 7 тестовых файлов в папке: " + testDir.getAbsolutePath());
            System.out.println("- 3 файла с одинаковым содержимым (группа 1)");
            System.out.println("- 2 файла с одинаковым содержимым (группа 2)");
            System.out.println("- 2 уникальных файла");
            System.out.println();

        } catch (Exception e) {
            System.err.println("Ошибка создания тестовых файлов: " + e.getMessage());
        }
    }

    private static void createTestFile(File file, String content) throws Exception {
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(content);
        }
        file.deleteOnExit();
    }

    private static void runFullDuplicateSearch(String directoryPath) throws Exception {
        System.out.println("--- ПОЛНЫЙ ЦИКЛ ПОИСКА ДУБЛИКАТОВ ---");
        System.out.println("Директория: " + new File(directoryPath).getAbsolutePath());
        System.out.println();

        long startTime = System.currentTimeMillis();

        System.out.println("ШАГ 1: Сканируем файлы...");
        FileScanner scanner = new FileScanner(
                FileScanner.Filters.byExtensions(".txt", ".java", ".xml"),
                false,
                10
        );

        List<FileScanner.FileTask> fileTasks = scanner.scanDirectory(directoryPath);
        System.out.println("Найдено файлов для обработки: " + fileTasks.size());
        System.out.println();

        System.out.println("ШАГ 2: Вычисляем хеши файлов...");
        HashCalculator hashCalculator = new HashCalculator();
        SimpleFileIndex index = new SimpleFileIndex();

        int processed = 0;
        int errors = 0;

        for (FileScanner.FileTask task : fileTasks) {
            try {
                String hash = hashCalculator.calculateHash(task.getPath());

                FileInfo fileInfo = new FileInfo(
                        task.getPath(),
                        hash,
                        task.getSize(),
                        task.getLastModified(),
                        LocalDateTime.now()
                );

                index.addOrUpdate(fileInfo);
                processed++;

                if (processed % 10 == 0) {
                    System.out.println("Обработано: " + processed + " файлов");
                }

            } catch (Exception e) {
                errors++;
                System.err.println("Ошибка обработки " + task.getPath() + ": " + e.getMessage());
            }
        }

        System.out.println("Обработка завершена. Успешно: " + processed + ", ошибок: " + errors);
        System.out.println();

        System.out.println("ШАГ 3: Ищем дубликаты...");
        List<List<FileInfo>> duplicateGroups = index.findDuplicates();

        if (duplicateGroups.isEmpty()) {
            System.out.println("Дубликатов не найдено");
        } else {
            System.out.println("Найдено групп дубликатов: " + duplicateGroups.size());
            System.out.println();

            for (int i = 0; i < duplicateGroups.size(); i++) {
                List<FileInfo> group = duplicateGroups.get(i);
                System.out.printf("=== ГРУППА ДУБЛИКАТОВ %d (%d файлов) ===%n", i + 1, group.size());

                for (FileInfo file : group) {
                    System.out.printf("  📄 %s%n", file.getFileName());
                    System.out.printf("      Путь: %s%n", file.getPath());
                    System.out.printf("      Размер: %d байт%n", file.getSize());
                    System.out.printf("      Хеш: %s%n", file.getHash());
                    System.out.println();
                }
            }
        }

        System.out.println("--- СТАТИСТИКА ---");
        SimpleFileIndex.IndexStatistics stats = index.getStatistics();
        System.out.println(stats);

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;

        System.out.println();
        System.out.printf("Общее время выполнения: %.2f секунд%n", duration);
        System.out.printf("Производительность: %.0f файлов/секунду%n", processed / Math.max(duration, 0.01));

        if (stats.getPotentialSavings() > 0) {
            System.out.println();
            System.out.println("💾 ПОТЕНЦИАЛЬНАЯ ЭКОНОМИЯ МЕСТА:");
            System.out.println("Можно освободить: " + formatSize(stats.getPotentialSavings()));
            System.out.println("Это " + (100.0 * stats.getPotentialSavings() / stats.getTotalSize()));
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
