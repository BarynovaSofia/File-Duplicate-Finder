package ua.pro.baynova.duplicatefinder.concurrent;

import ua.pro.baynova.duplicatefinder.concurrent.MultiThreadHashCalculator;
import ua.pro.baynova.duplicatefinder.index.SimpleFileIndex;
import ua.pro.baynova.duplicatefinder.model.FileInfo;
import ua.pro.baynova.duplicatefinder.scanner.FileScanner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadFileIndexer {

    private final FileScanner fileScanner;
    private final MultiThreadHashCalculator hashCalculator;
    private final SimpleFileIndex fileIndex;
    private final int numberOfThreads;

    private String hashAlgorithm = "MD5";
    private boolean showProgress = true;

    public MultiThreadFileIndexer() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public MultiThreadFileIndexer(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        this.fileScanner = new FileScanner();
        this.hashCalculator = new MultiThreadHashCalculator(numberOfThreads, hashAlgorithm);
        this.fileIndex = new SimpleFileIndex();

        System.out.println("=== MultiThreadFileIndexer ===");
        System.out.println("Потоков: " + numberOfThreads);
        System.out.println("Алгоритм хеширования: " + hashAlgorithm);
        System.out.println("Показ прогресса: " + (showProgress ? "включен" : "выключен"));
    }

    /**
     * Главный метод - полный цикл индексации директории
     * @param directoryPath путь к директории
     * @return результат индексации с подробной статистикой
     */
    public IndexingResult indexDirectory(String directoryPath) throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("МНОГОПОТОЧНАЯ ИНДЕКСАЦИЯ: " + directoryPath);
        System.out.println("=".repeat(60));

        long totalStartTime = System.currentTimeMillis();
        IndexingResult result = new IndexingResult();

        try {
            executeScanningPhase(directoryPath, result);

            executeHashingPhase(result);

            executeIndexingPhase(result);

            result.totalDuration = System.currentTimeMillis() - totalStartTime;
            printFinalStatistics(result);

            return result;

        } catch (Exception e) {
            System.err.println("Критическая ошибка индексации: " + e.getMessage());
            throw e;
        }
    }

    private void executeScanningPhase(String directoryPath, IndexingResult result) throws Exception {
        System.out.println("\n--- ЭТАП 1: СКАНИРОВАНИЕ ---");
        long phaseStart = System.currentTimeMillis();

        FileScanner scanner = new FileScanner(
                FileScanner.Filters.excludeHidden()
                        .and(FileScanner.Filters.minSize(1)),
                false,
                20
        );

        result.fileTasks = scanner.scanDirectory(directoryPath);
        result.scanningDuration = System.currentTimeMillis() - phaseStart;

        System.out.printf("Найдено файлов: %d%n", result.fileTasks.size());
        System.out.printf("Время сканирования: %.2f сек%n", result.scanningDuration / 1000.0);

        if (result.fileTasks.isEmpty()) {
            System.out.println("⚠️ Файлы для обработки не найдены");
            return;
        }

        analyzeFileSizes(result.fileTasks);
    }

    private void executeHashingPhase(IndexingResult result) throws Exception {
        if (result.fileTasks.isEmpty()) return;

        System.out.println("\n--- ЭТАП 2: ВЫЧИСЛЕНИЕ ХЕШЕЙ ---");
        long phaseStart = System.currentTimeMillis();

        ProgressTracker progressTracker = new ProgressTracker(result.fileTasks.size());

        result.processedFiles = hashCalculator.processFiles(result.fileTasks);
        result.hashingDuration = System.currentTimeMillis() - phaseStart;

        System.out.printf("Обработано файлов: %d из %d%n",
                result.processedFiles.size(), result.fileTasks.size());
        System.out.printf("Время хеширования: %.2f сек%n", result.hashingDuration / 1000.0);

        calculateParallelEfficiency(result);
    }

    private void executeIndexingPhase(IndexingResult result) {
        if (result.processedFiles.isEmpty()) return;

        System.out.println("\n--- ЭТАП 3: ПОСТРОЕНИЕ ИНДЕКСА ---");
        long phaseStart = System.currentTimeMillis();

        for (FileInfo fileInfo : result.processedFiles) {
            fileIndex.addOrUpdate(fileInfo);
        }

        result.duplicateGroups = fileIndex.findDuplicates();
        result.indexStatistics = fileIndex.getStatistics();
        result.indexingDuration = System.currentTimeMillis() - phaseStart;

        System.out.printf("Файлов в индексе: %d%n", fileIndex.size());
        System.out.printf("Групп дубликатов: %d%n", result.duplicateGroups.size());
        System.out.printf("Время индексации: %.2f сек%n", result.indexingDuration / 1000.0);

        displayDuplicates(result.duplicateGroups);
    }

    private void analyzeFileSizes(List<FileScanner.FileTask> fileTasks) {
        long totalSize = fileTasks.stream().mapToLong(FileScanner.FileTask::getSize).sum();
        long maxSize = fileTasks.stream().mapToLong(FileScanner.FileTask::getSize).max().orElse(0);
        double avgSize = (double) totalSize / fileTasks.size();

        System.out.printf("Анализ размеров:%n");
        System.out.printf("  Общий размер: %s%n", formatBytes(totalSize));
        System.out.printf("  Средний размер: %s%n", formatBytes((long) avgSize));
        System.out.printf("  Максимальный: %s%n", formatBytes(maxSize));

        long small = fileTasks.stream().mapToLong(t -> t.getSize() < 1024 ? 1 : 0).sum();
        long medium = fileTasks.stream().mapToLong(t -> t.getSize() >= 1024 && t.getSize() < 1024*1024 ? 1 : 0).sum();
        long large = fileTasks.stream().mapToLong(t -> t.getSize() >= 1024*1024 ? 1 : 0).sum();

        System.out.printf("  Маленькие (<1KB): %d файлов%n", small);
        System.out.printf("  Средние (1KB-1MB): %d файлов%n", medium);
        System.out.printf("  Большие (>1MB): %d файлов%n", large);
    }

    private void calculateParallelEfficiency(IndexingResult result) {

        long estimatedSingleThreadTime = result.hashingDuration * numberOfThreads;
        double theoreticalSpeedup = (double) estimatedSingleThreadTime / result.hashingDuration;
        double efficiency = theoreticalSpeedup / numberOfThreads * 100;

        System.out.printf("Оценка эффективности многопоточности:%n");
        System.out.printf("  Теоретическое ускорение: %.1fx%n", theoreticalSpeedup);
        System.out.printf("  Эффективность: %.1f%%%n", efficiency);

        if (efficiency < 50) {
            System.out.println("  ⚠️ Низкая эффективность - возможно файлы слишком маленькие для многопоточности");
        } else if (efficiency > 80) {
            System.out.println("  ✅ Отличная эффективность многопоточной обработки");
        }
    }

    private void displayDuplicates(List<List<FileInfo>> duplicateGroups) {
        if (duplicateGroups.isEmpty()) {
            System.out.println("Дубликаты не обнаружены");
            return;
        }

        System.out.println("\n--- НАЙДЕННЫЕ ДУБЛИКАТЫ ---");

        int groupsToShow = Math.min(3, duplicateGroups.size());

        for (int i = 0; i < groupsToShow; i++) {
            List<FileInfo> group = duplicateGroups.get(i);
            System.out.printf("\nГруппа %d (%d файлов, %s каждый):%n",
                    i + 1, group.size(), formatBytes(group.get(0).getSize()));

            for (FileInfo file : group) {
                System.out.printf("  📄 %s%n", file.getFileName());
                System.out.printf("      %s%n", file.getPath());
            }
        }

        if (duplicateGroups.size() > groupsToShow) {
            System.out.printf("\n... и еще %d групп дубликатов%n",
                    duplicateGroups.size() - groupsToShow);
        }
    }

    private void printFinalStatistics(IndexingResult result) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ИТОГОВАЯ СТАТИСТИКА");
        System.out.println("=".repeat(60));

        System.out.printf("Общее время: %.2f сек%n", result.totalDuration / 1000.0);
        System.out.printf("  └─ Сканирование: %.2f сек (%.1f%%)%n",
                result.scanningDuration / 1000.0,
                100.0 * result.scanningDuration / result.totalDuration);
        System.out.printf("  └─ Хеширование: %.2f сек (%.1f%%)%n",
                result.hashingDuration / 1000.0,
                100.0 * result.hashingDuration / result.totalDuration);
        System.out.printf("  └─ Индексация: %.2f сек (%.1f%%)%n",
                result.indexingDuration / 1000.0,
                100.0 * result.indexingDuration / result.totalDuration);

        if (result.indexStatistics != null) {
            System.out.println("\n" + result.indexStatistics);
        }

        provideOptimizationSuggestions(result);
    }

    private void provideOptimizationSuggestions(IndexingResult result) {
        System.out.println("\n--- РЕКОМЕНДАЦИИ ---");

        double hashingPercentage = 100.0 * result.hashingDuration / result.totalDuration;

        if (hashingPercentage > 70) {
            System.out.println("• Хеширование занимает большую часть времени - рассмотрите увеличение количества потоков");
        }

        if (result.processedFiles.size() < numberOfThreads * 10) {
            System.out.println("• Слишком мало файлов для эффективной многопоточности - для маленьких задач используйте 1-2 потока");
        }

        if (result.duplicateGroups.size() > result.processedFiles.size() * 0.1) {
            System.out.println("• Найдено много дубликатов - рассмотрите очистку или реорганизацию файлов");
        }

        System.out.printf("• Производительность: %.1f файлов/сек%n",
                result.processedFiles.size() * 1000.0 / result.totalDuration);
    }

    private static class ProgressTracker {
        private final int total;
        private final AtomicInteger completed = new AtomicInteger(0);
        private long lastUpdateTime = System.currentTimeMillis();

        public ProgressTracker(int total) {
            this.total = total;
        }

        public void increment() {
            int current = completed.incrementAndGet();
            long now = System.currentTimeMillis();

            if (now - lastUpdateTime > 2000 || current % Math.max(1, total / 4) == 0) {
                double percentage = (100.0 * current) / total;
                System.out.printf("Прогресс: %.1f%% (%d/%d)%n", percentage, current, total);
                lastUpdateTime = now;
            }
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static class IndexingResult {

        public List<FileScanner.FileTask> fileTasks;
        public List<FileInfo> processedFiles;
        public List<List<FileInfo>> duplicateGroups;
        public SimpleFileIndex.IndexStatistics indexStatistics;

        public long scanningDuration;
        public long hashingDuration;
        public long indexingDuration;
        public long totalDuration;

        public int getProcessedCount() {
            return processedFiles != null ? processedFiles.size() : 0;
        }

        public int getDuplicateGroupsCount() {
            return duplicateGroups != null ? duplicateGroups.size() : 0;
        }

        public double getOverallPerformance() {
            return totalDuration > 0 ? getProcessedCount() * 1000.0 / totalDuration : 0;
        }
    }
}
