package ua.pro.baynova.duplicatefinder.app;

import ua.pro.baynova.duplicatefinder.cli.CommandLineParser;
import ua.pro.baynova.duplicatefinder.concurrent.MultiThreadFileIndexer;
import ua.pro.baynova.duplicatefinder.scanner.FileScanner;

import java.util.function.Predicate;
import java.nio.file.Path;

public class DuplicateFinderApp {

    public static void main(String[] args) {
        try {
            CommandLineParser parser = new CommandLineParser();
            parser.parse(args);

            if (parser.isShowHelp()) {
                parser.printHelp();
                return;
            }

            printHeader();

            parser.printConfiguration();

            runDuplicateFinder(parser);

            printSuccess();

        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка в параметрах: " + e.getMessage());
            System.err.println("Используйте --help для справки");
            System.exit(1);

        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void runDuplicateFinder(CommandLineParser config) throws Exception {

        Predicate<Path> filter = createFileFilter(config);

        MultiThreadFileIndexer indexer = new MultiThreadFileIndexer(config.getThreads());

        System.out.println("Начинается поиск дубликатов...\n");

        long startTime = System.currentTimeMillis();

        MultiThreadFileIndexer.IndexingResult result =
                indexer.indexDirectory(config.getDirectory());

        long totalTime = System.currentTimeMillis() - startTime;

        printFinalReport(result, totalTime);
    }

    private static Predicate<Path> createFileFilter(CommandLineParser config) {
        Predicate<Path> filter = FileScanner.Filters.excludeHidden();

        if (config.getMinSize() > 0) {
            filter = filter.and(FileScanner.Filters.minSize(config.getMinSize()));
        }

        if (!config.getExtensions().isEmpty()) {
            String[] extensions = config.getExtensions().toArray(new String[0]);
            filter = filter.and(FileScanner.Filters.byExtensions(extensions));
        }

        return filter;
    }

    private static void printHeader() {
        System.out.println("---FILE DUPLICATE FINDER---");
    }

    private static void printFinalReport(MultiThreadFileIndexer.IndexingResult result, long totalTime) {
        System.out.println("\n ИТОГОВЫЙ ОТЧЁТ  \n");

        System.out.printf(" Общее время выполнения: %.2f сек%n", totalTime / 1000.0);
        System.out.printf(" Обработано файлов: %d%n", result.getProcessedCount());
        System.out.printf(" Найдено групп дубликатов: %d%n", result.getDuplicateGroupsCount());

        if (result.indexStatistics != null) {
            System.out.printf("Можно освободить места: %s%n",
                    formatSize(result.indexStatistics.getPotentialSavings()));
        }

        System.out.printf("Производительность: %.1f файлов/сек%n", result.getOverallPerformance());

        System.out.println("\n-------");

        if (!result.duplicateGroups.isEmpty()) {
            System.out.println("\n🔥 ТОП-3 ГРУППЫ ДУБЛИКАТОВ:\n");

            int count = Math.min(3, result.duplicateGroups.size());
            for (int i = 0; i < count; i++) {
                var group = result.duplicateGroups.get(i);
                System.out.printf(" %d. Группа из %d файлов (%s каждый):%n",
                        i + 1, group.size(), formatSize(group.get(0).getSize()));

                int filesToShow = Math.min(3, group.size());
                for (int j = 0; j < filesToShow; j++) {
                    System.out.println(" 📄 " + group.get(j).getFileName());
                }

                if (group.size() > 3) {
                    System.out.printf("...и ещё %d файлов%n", group.size() - 3);
                }
                System.out.println();
            }

            if (result.duplicateGroups.size() > 3) {
                System.out.printf("...и ещё %d групп дубликатов%n",
                        result.duplicateGroups.size() - 3);
            }
        }
    }

    private static void printSuccess() {
        System.out.println(" ✅ УСПЕШНО ЗАВЕРШЕНО ✅ ");
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
