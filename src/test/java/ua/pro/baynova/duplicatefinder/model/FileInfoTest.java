package ua.pro.baynova.duplicatefinder.model;

import java.time.LocalDateTime;

public class FileInfoTest {

    public static void main(String[] args) {
        System.out.println("=== Тестируем FileInfo ===\n");

        testBasicCreation();
        testValidation();
        testNeedsReindexing();
        testFileNameExtraction();

        System.out.println("\n✅ Все тесты FileInfo пройдены!");
    }

    private static void testBasicCreation() {
        System.out.println("--- Тест 1: Создание FileInfo ---");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);

        FileInfo fileInfo = new FileInfo(
                "/home/user/documents/test.txt",
                "abc123def456",
                1024,
                yesterday,
                now
        );

        System.out.println("Путь: " + fileInfo.getPath());
        System.out.println("Хеш: " + fileInfo.getHash());
        System.out.println("Размер: " + fileInfo.getSize() + " байт");
        System.out.println("Имя файла: " + fileInfo.getFileName());
        System.out.println("toString(): " + fileInfo);

        assert fileInfo.getPath().equals("/home/user/documents/test.txt");
        assert fileInfo.getSize() == 1024;
        assert fileInfo.getFileName().equals("test.txt");

        System.out.println("✅ Базовое создание работает\n");
    }

    private static void testValidation() {
        System.out.println("--- Тест 2: Валидация данных ---");

        LocalDateTime now = LocalDateTime.now();

        // Тест пустого пути
        try {
            new FileInfo("", "hash", 100, now, now);
            System.out.println("❌ Должна была быть ошибка для пустого пути");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Пустой путь правильно отклонен: " + e.getMessage());
        }

        // Тест отрицательного размера
        try {
            new FileInfo("/test.txt", "hash", -1, now, now);
            System.out.println("❌ Должна была быть ошибка для отрицательного размера");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Отрицательный размер правильно отклонен: " + e.getMessage());
        }

        System.out.println("✅ Валидация работает\n");
    }

    private static void testNeedsReindexing() {
        System.out.println("--- Тест 3: Проверка необходимости переиндексации ---");

        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime laterTime = baseTime.plusHours(1);
        LocalDateTime earlierTime = baseTime.minusHours(1);

        FileInfo fileInfo = new FileInfo(
                "/test.txt", "hash", 100, baseTime, baseTime
        );

        // Файл изменился позже - нужна переиндексация
        boolean needsReindex1 = fileInfo.needsReindexing(laterTime);
        System.out.println("Файл изменен позже, нужна переиндексация: " + needsReindex1);

        // Файл не изменился - переиндексация не нужна
        boolean needsReindex2 = fileInfo.needsReindexing(earlierTime);
        System.out.println("Файл не изменен, переиндексация не нужна: " + needsReindex2);

        assert needsReindex1 == true;
        assert needsReindex2 == false;

        System.out.println("✅ Логика переиндексации работает\n");
    }

    private static void testFileNameExtraction() {
        System.out.println("--- Тест 4: Извлечение имени файла ---");

        LocalDateTime now = LocalDateTime.now();

        // Тест Unix пути
        FileInfo unixFile = new FileInfo(
                "/home/user/docs/report.pdf", "hash", 100, now, now
        );
        System.out.println("Unix путь: " + unixFile.getPath());
        System.out.println("Имя файла: " + unixFile.getFileName());
        assert unixFile.getFileName().equals("report.pdf");

        // Тест Windows пути
        FileInfo windowsFile = new FileInfo(
                "C:\\Users\\User\\Documents\\photo.jpg", "hash", 100, now, now
        );
        System.out.println("Windows путь: " + windowsFile.getPath());
        System.out.println("Имя файла: " + windowsFile.getFileName());
        assert windowsFile.getFileName().equals("photo.jpg");

        // Тест файла без пути
        FileInfo simpleFile = new FileInfo(
                "simple.txt", "hash", 100, now, now
        );
        System.out.println("Простое имя: " + simpleFile.getPath());
        System.out.println("Имя файла: " + simpleFile.getFileName());
        assert simpleFile.getFileName().equals("simple.txt");

        System.out.println("✅ Извлечение имени файла работает\n");
    }
}