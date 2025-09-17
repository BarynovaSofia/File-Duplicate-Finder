package ua.pro.baynova.duplicatefinder.scanner;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FileScanner {

    private final Predicate<Path> fileFilter;
    private final boolean followSymlinks;
    private final int maxDepth;

    /**
     * Создает сканер с настройками по умолчанию
     */
    public FileScanner() {
        this(path -> true, false, Integer.MAX_VALUE);
    }

    /**
     * Создает сканер с настройками
     * @param fileFilter фильтр файлов (какие файлы включать)
     * @param followSymlinks следовать ли символьным ссылкам
     * @param maxDepth максимальная глубина обхода
     */
    public FileScanner(Predicate<Path> fileFilter, boolean followSymlinks, int maxDepth) {
        this.fileFilter = fileFilter != null ? fileFilter : (path -> true);
        this.followSymlinks = followSymlinks;
        this.maxDepth = maxDepth;
    }

    /**
     * Сканирует директорию и возвращает список найденных файлов
     * @param directoryPath путь к директории
     * @return список файлов для обработки
     */
    public List<FileTask> scanDirectory(String directoryPath) throws IOException {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Путь к директории не может быть пустым");
        }

        Path startPath = Paths.get(directoryPath);

        if (!Files.exists(startPath)) {
            throw new IOException("Директория не существует: " + directoryPath);
        }

        if (!Files.isDirectory(startPath)) {
            throw new IOException("Указанный путь не является директорией: " + directoryPath);
        }

        List<FileTask> fileTasks = new ArrayList<>();

        System.out.println("Сканируем директорию: " + startPath.toAbsolutePath());

        try {
            Files.walkFileTree(startPath, getVisitOptions(), maxDepth, new FileVisitor(fileTasks));
        } catch (IOException e) {
            throw new IOException("Ошибка при сканировании директории: " + e.getMessage(), e);
        }

        System.out.println("Сканирование завершено. Найдено файлов: " + fileTasks.size());
        return fileTasks;
    }

    /**
     * Возвращает опции обхода файлового дерева
     */
    private java.util.Set<FileVisitOption> getVisitOptions() {
        if (followSymlinks) {
            return java.util.Set.of(FileVisitOption.FOLLOW_LINKS);
        }
        return java.util.Set.of();
    }

    /**
     * Visitor для обхода файлового дерева
     */
    private class FileVisitor extends SimpleFileVisitor<Path> {
        private final List<FileTask> fileTasks;
        private int visitedFiles = 0;
        private int skippedFiles = 0;

        public FileVisitor(List<FileTask> fileTasks) {
            this.fileTasks = fileTasks;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            visitedFiles++;

            if (visitedFiles % 1000 == 0) {
                System.out.println("Просмотрено файлов: " + visitedFiles);
            }

            if (!attrs.isRegularFile()) {
                return FileVisitResult.CONTINUE;
            }

            if (!fileFilter.test(file)) {
                skippedFiles++;
                return FileVisitResult.CONTINUE;
            }

            try {
                LocalDateTime lastModified = LocalDateTime.ofInstant(
                        attrs.lastModifiedTime().toInstant(),
                        ZoneId.systemDefault()
                );

                FileTask task = new FileTask(
                        file.toAbsolutePath().toString(),
                        attrs.size(),
                        lastModified
                );

                fileTasks.add(task);

            } catch (Exception e) {
                System.err.println("Ошибка обработки файла " + file + ": " + e.getMessage());
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            System.err.println("Не удалось обработать файл: " + file + " - " + exc.getMessage());
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) {
                System.err.println("Ошибка в директории " + dir + ": " + exc.getMessage());
            }
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Информация о файле для обработки
     * Простая структура данных без логики
     */
    public static class FileTask {
        private final String path;
        private final long size;
        private final LocalDateTime lastModified;

        public FileTask(String path, long size, LocalDateTime lastModified) {
            this.path = path;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getPath() {
            return path;
        }

        public long getSize() {
            return size;
        }

        public LocalDateTime getLastModified() {
            return lastModified;
        }

        @Override
        public String toString() {
            return String.format("FileTask{path='%s', size=%d bytes}",
                    getFileName(), size);
        }

        private String getFileName() {
            int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            return lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
        }
    }

    public static class Filters {

        /**
         * Фильтр по расширениям файлов
         */
        public static Predicate<Path> byExtensions(String... extensions) {
            java.util.Set<String> extSet = java.util.Set.of(extensions);
            return path -> {
                String fileName = path.getFileName().toString().toLowerCase();
                return extSet.stream().anyMatch(fileName::endsWith);
            };
        }

        /**
         * Фильтр по минимальному размеру файла
         */
        public static Predicate<Path> minSize(long minBytes) {
            return path -> {
                try {
                    return Files.size(path) >= minBytes;
                } catch (IOException e) {
                    return false;
                }
            };
        }

        /**
         * Исключить скрытые файлы
         */
        public static Predicate<Path> excludeHidden() {
            return path -> !path.getFileName().toString().startsWith(".");
        }

        /**
         * Комбинирование фильтров через AND
         */
        public static Predicate<Path> and(Predicate<Path> first, Predicate<Path> second) {
            return first.and(second);
        }
    }
}