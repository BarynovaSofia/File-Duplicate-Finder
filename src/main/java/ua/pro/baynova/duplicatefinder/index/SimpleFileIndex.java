package ua.pro.baynova.duplicatefinder.index;

import ua.pro.baynova.duplicatefinder.model.FileInfo;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleFileIndex {

    private final Map<String, FileInfo> pathIndex = new HashMap<>();
    private final Map<String, Set<String>> hashIndex = new HashMap<>();

    /**
     * Добавляет или обновляет информацию о файле
     * @param fileInfo информация о файле
     */
    public void addOrUpdate(FileInfo fileInfo) {
        if (fileInfo == null) {
            throw new IllegalArgumentException("FileInfo не может быть null");
        }

        String path = fileInfo.getPath();
        String hash = fileInfo.getHash();

        FileInfo existingFile = pathIndex.get(path);
        if (existingFile != null) {
            removeFromHashIndex(existingFile.getHash(), path);
        }

        pathIndex.put(path, fileInfo);
        addToHashIndex(hash, path);

        System.out.println("Добавлен в индекс: " + fileInfo.getFileName() +
                " (хеш: " + hash.substring(0, Math.min(8, hash.length())) + "...)");
    }

    /**
     * Получает информацию о файле по пути
     * @param path путь к файлу
     * @return информация о файле или null если не найден
     */
    public FileInfo getByPath(String path) {
        return pathIndex.get(path);
    }

    /**
     * Проверяет содержится ли файл в индексе
     * @param path путь к файлу
     * @return true если файл есть в индексе
     */
    public boolean contains(String path) {
        return pathIndex.containsKey(path);
    }

    /**
     * Удаляет файл из индекса
     * @param path путь к файлу
     * @return true если файл был удален, false если его не было
     */
    public boolean remove(String path) {
        FileInfo fileInfo = pathIndex.remove(path);
        if (fileInfo != null) {
            removeFromHashIndex(fileInfo.getHash(), path);
            System.out.println("Удален из индекса: " + fileInfo.getFileName());
            return true;
        }
        return false;
    }

    /**
     * Возвращает все файлы в индексе
     * @return список всех файлов
     */
    public List<FileInfo> getAllFiles() {
        return new ArrayList<>(pathIndex.values());
    }

    /**
     * Находит все группы дубликатов
     * @return список групп дубликатов (каждая группа содержит файлы с одинаковым содержимым)
     */
    public List<List<FileInfo>> findDuplicates() {
        List<List<FileInfo>> duplicateGroups = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : hashIndex.entrySet()) {
            Set<String> paths = entry.getValue();

            if (paths.size() > 1) {
                List<FileInfo> duplicateGroup = new ArrayList<>();

                for (String path : paths) {
                    FileInfo fileInfo = pathIndex.get(path);
                    if (fileInfo != null) {
                        duplicateGroup.add(fileInfo);
                    }
                }

                if (duplicateGroup.size() > 1) {
                    duplicateGroups.add(duplicateGroup);
                }
            }
        }

        duplicateGroups.sort((g1, g2) -> Integer.compare(g2.size(), g1.size()));

        return duplicateGroups;
    }

    /**
     * Находит дубликаты конкретного файла
     * @param filePath путь к файлу
     * @return список дубликатов (без самого файла)
     */
    public List<FileInfo> findDuplicatesOf(String filePath) {
        FileInfo targetFile = pathIndex.get(filePath);
        if (targetFile == null) {
            return new ArrayList<>();
        }

        String targetHash = targetFile.getHash();
        Set<String> duplicatePaths = hashIndex.get(targetHash);

        if (duplicatePaths == null || duplicatePaths.size() <= 1) {
            return new ArrayList<>();
        }

        return duplicatePaths.stream()
                .filter(path -> !path.equals(filePath))
                .map(pathIndex::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Получить статистику индекса
     * @return информация о содержимом индекса
     */
    public IndexStatistics getStatistics() {
        int totalFiles = pathIndex.size();

        long totalSize = pathIndex.values().stream()
                .mapToLong(FileInfo::getSize)
                .sum();

        int duplicateGroups = (int) hashIndex.values().stream()
                .filter(paths -> paths.size() > 1)
                .count();

        int duplicateFiles = hashIndex.values().stream()
                .filter(paths -> paths.size() > 1)
                .mapToInt(paths -> paths.size() - 1)
                .sum();

        long duplicateSize = hashIndex.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .mapToLong(entry -> {
                    String firstPath = entry.getValue().iterator().next();
                    FileInfo firstFile = pathIndex.get(firstPath);
                    return firstFile != null ?
                            firstFile.getSize() * (entry.getValue().size() - 1) : 0;
                })
                .sum();

        return new IndexStatistics(totalFiles, totalSize, duplicateGroups, duplicateFiles, duplicateSize);
    }

    public void clear() {
        pathIndex.clear();
        hashIndex.clear();
        System.out.println("Индекс очищен");
    }

    public int size() {
        return pathIndex.size();
    }

    private void addToHashIndex(String hash, String path) {
        hashIndex.computeIfAbsent(hash, k -> new HashSet<>()).add(path);
    }

    private void removeFromHashIndex(String hash, String path) {
        Set<String> paths = hashIndex.get(hash);
        if (paths != null) {
            paths.remove(path);
            if (paths.isEmpty()) {
                hashIndex.remove(hash);
            }
        }
    }

    public static class IndexStatistics {
        private final int totalFiles;
        private final long totalSize;
        private final int duplicateGroups;
        private final int duplicateFiles;
        private final long duplicateSize;

        public IndexStatistics(int totalFiles, long totalSize, int duplicateGroups,
                               int duplicateFiles, long duplicateSize) {
            this.totalFiles = totalFiles;
            this.totalSize = totalSize;
            this.duplicateGroups = duplicateGroups;
            this.duplicateFiles = duplicateFiles;
            this.duplicateSize = duplicateSize;
        }

        public int getTotalFiles() { return totalFiles; }
        public long getTotalSize() { return totalSize; }
        public int getDuplicateGroups() { return duplicateGroups; }
        public int getDuplicateFiles() { return duplicateFiles; }
        public long getDuplicateSize() { return duplicateSize; }

        public long getPotentialSavings() {
            return duplicateSize;
        }

        @Override
        public String toString() {
            return String.format(
                    "Статистика индекса:\n" +
                            "  Всего файлов: %d\n" +
                            "  Общий размер: %s\n" +
                            "  Групп дубликатов: %d\n" +
                            "  Дубликатов: %d файлов\n" +
                            "  Размер дубликатов: %s\n" +
                            "  Можно освободить: %s",
                    totalFiles,
                    formatSize(totalSize),
                    duplicateGroups,
                    duplicateFiles,
                    formatSize(duplicateSize),
                    formatSize(getPotentialSavings())
            );
        }

        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
