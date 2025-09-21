package ua.pro.baynova.duplicatefinder.index;

import ua.pro.baynova.duplicatefinder.model.FileInfo;
import java.time.LocalDateTime;
import java.util.List;

public class SimpleFileIndexTest {

    public static void main(String[] args) {
        System.out.println("=== Тестируем SimpleFileIndex ===\n");

        testBasicOperations();
        testDuplicateDetection();
        testStatistics();
        testEdgeCases();

        System.out.println("\n✅ Все тесты SimpleFileIndex пройдены!");
    }

    private static void testBasicOperations() {
        System.out.println("--- Тест 1: Базовые операции ---");

        SimpleFileIndex index = new SimpleFileIndex();
        LocalDateTime now = LocalDateTime.now();

        FileInfo file1 = new FileInfo(
                "/test/document1.txt",
                "hash123",
                1000,
                now.minusDays(1),
                now
        );

        FileInfo file2 = new FileInfo(
                "/test/photo.jpg",
                "hash456",
                2000000,
                now.minusDays(2),
                now
        );

        index.addOrUpdate(file1);
        index.addOrUpdate(file2);

        System.out.println("Размер индекса: " + index.size());

        FileInfo found = index.getByPath("/test/document1.txt");
        if (found != null && found.equals(file1)) {
            System.out.println("✅ Поиск по пути работает");
        } else {
            System.out.println("❌ Проблема с поиском по пути");
        }

        if (index.contains("/test/document1.txt") && !index.contains("/nonexistent")) {
            System.out.println("✅ Метод contains работает");
        } else {
            System.out.println("❌ Проблема с методом contains");
        }

        List<FileInfo> allFiles = index.getAllFiles();
        if (allFiles.size() == 2) {
            System.out.println("✅ Получение всех файлов работает");
        } else {
            System.out.println("❌ Проблема с получением всех файлов");
        }

        System.out.println();
    }

    private static void testDuplicateDetection() {
        System.out.println("--- Тест 2: Обнаружение дубликатов ---");

        SimpleFileIndex index = new SimpleFileIndex();
        LocalDateTime now = LocalDateTime.now();

        FileInfo original = new FileInfo(
                "/folder1/original.txt",
                "duplicate_hash",
                5000,
                now,
                now
        );

        FileInfo duplicate1 = new FileInfo(
                "/folder2/copy1.txt",
                "duplicate_hash",
                5000,
                now,
                now
        );

        FileInfo duplicate2 = new FileInfo(
                "/folder3/copy2.txt",
                "duplicate_hash",
                5000,
                now,
                now
        );

        FileInfo unique = new FileInfo(
                "/folder4/unique.txt",
                "unique_hash",
                3000,
                now,
                now
        );

        FileInfo another1 = new FileInfo(
                "/folder5/another1.txt",
                "another_hash",
                1000,
                now,
                now
        );

        FileInfo another2 = new FileInfo(
                "/folder6/another2.txt",
                "another_hash",
                1000,
                now,
                now
        );

        index.addOrUpdate(original);
        index.addOrUpdate(duplicate1);
        index.addOrUpdate(duplicate2);
        index.addOrUpdate(unique);
        index.addOrUpdate(another1);
        index.addOrUpdate(another2);

        System.out.println("Добавлено файлов: " + index.size());

        List<List<FileInfo>> duplicateGroups = index.findDuplicates();

        System.out.println("Найдено групп дубликатов: " + duplicateGroups.size());

        if (duplicateGroups.size() == 2) {
            System.out.println("✅ Правильное количество групп дубликатов");

            for (int i = 0; i < duplicateGroups.size(); i++) {
                List<FileInfo> group = duplicateGroups.get(i);
                System.out.println("Группа " + (i + 1) + ": " + group.size() + " файлов");

                for (FileInfo file : group) {
                    String hashPreview = file.getHash().length() >= 8 ?
                            file.getHash().substring(0, 8) + "..." :
                            file.getHash() + "...";
                    System.out.println("  - " + file.getFileName() + " (" + hashPreview + ")");
                }
            }

        } else {
            System.out.println("❌ Неправильное количество групп дубликатов");
        }

        List<FileInfo> duplicatesOfOriginal = index.findDuplicatesOf("/folder1/original.txt");
        if (duplicatesOfOriginal.size() == 2) {
            System.out.println("✅ Поиск дубликатов конкретного файла работает");
        } else {
            System.out.println("❌ Проблема с поиском дубликатов конкретного файла: " + duplicatesOfOriginal.size());
        }

        System.out.println();
    }

    private static void testStatistics() {
        System.out.println("--- Тест 3: Статистика ---");

        SimpleFileIndex index = new SimpleFileIndex();
        LocalDateTime now = LocalDateTime.now();

        index.addOrUpdate(new FileInfo("/big1.bin", "big_hash", 10_000_000, now, now));
        index.addOrUpdate(new FileInfo("/big2.bin", "big_hash", 10_000_000, now, now));

        index.addOrUpdate(new FileInfo("/small1.txt", "small_hash", 1000, now, now));
        index.addOrUpdate(new FileInfo("/small2.txt", "small_hash", 1000, now, now));
        index.addOrUpdate(new FileInfo("/small3.txt", "small_hash", 1000, now, now));

        index.addOrUpdate(new FileInfo("/unique.doc", "unique_hash", 5_000_000, now, now));

        SimpleFileIndex.IndexStatistics stats = index.getStatistics();

        System.out.println("=== СТАТИСТИКА ===");
        System.out.println(stats);

        if (stats.getTotalFiles() == 6) {
            System.out.println("✅ Общее количество файлов правильное");
        } else {
            System.out.println("❌ Неправильное общее количество файлов: " + stats.getTotalFiles());
        }

        if (stats.getDuplicateGroups() == 2) {
            System.out.println("✅ Количество групп дубликатов правильное");
        } else {
            System.out.println("❌ Неправильное количество групп дубликатов: " + stats.getDuplicateGroups());
        }

        if (stats.getDuplicateFiles() == 3) {
            System.out.println("✅ Количество дубликатов правильное");
        } else {
            System.out.println("❌ Неправильное количество дубликатов: " + stats.getDuplicateFiles());
        }

        long expectedDuplicateSize = 10_000_000 + 2 * 1000;
        if (stats.getDuplicateSize() == expectedDuplicateSize) {
            System.out.println("✅ Размер дубликатов правильный");
        } else {
            System.out.println("❌ Неправильный размер дубликатов: " + stats.getDuplicateSize() +
                    ", ожидался: " + expectedDuplicateSize);
        }

        System.out.println();
    }

    private static void testEdgeCases() {
        System.out.println("--- Тест 4: Граничные случаи ---");

        SimpleFileIndex index = new SimpleFileIndex();
        LocalDateTime now = LocalDateTime.now();

        List<List<FileInfo>> emptyDuplicates = index.findDuplicates();
        if (emptyDuplicates.isEmpty()) {
            System.out.println("✅ Пустой индекс корректно обрабатывается");
        }

        FileInfo file1v1 = new FileInfo("/test.txt", "hash1", 1000, now, now);
        FileInfo file1v2 = new FileInfo("/test.txt", "hash2", 2000, now.plusMinutes(1), now);

        index.addOrUpdate(file1v1);
        index.addOrUpdate(file1v2);

        if (index.size() == 1) {
            FileInfo updated = index.getByPath("/test.txt");
            if (updated.getHash().equals("hash2")) {
                System.out.println("✅ Обновление файла работает правильно");
            }
        }

        boolean removed = index.remove("/test.txt");
        if (removed && index.size() == 0) {
            System.out.println("✅ Удаление файла работает");
        }

        try {
            index.addOrUpdate(null);
            System.out.println("❌ Должна была быть ошибка для null FileInfo");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Null FileInfo правильно отклонен: " + e.getMessage());
        }

        System.out.println();
    }
}
