package ua.pro.baynova.duplicatefinder.hash;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class HashCalculatorTest {

    public static void main(String[] args) {
        System.out.println("=== Тестируем HashCalculator ===\n");

        testBasicHashing();
        testDuplicateDetection();
        testDifferentAlgorithms();
        testErrorHandling();
        testPerformance();

        System.out.println("\n✅ Все тесты HashCalculator пройдены!");
    }

    private static void testBasicHashing() {
        System.out.println("--- Тест 1: Базовое хеширование ---");

        try {
            File tempFile = File.createTempFile("test_hash", ".txt");
            tempFile.deleteOnExit();

            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write("Hello, World!");
            }

            HashCalculator calculator = new HashCalculator();

            String hash = calculator.calculateHash(tempFile);

            System.out.println("Файл: " + tempFile.getName());
            System.out.println("Содержимое: Hello, World!");
            System.out.println("MD5 хеш: " + hash);
            System.out.println("Длина хеша: " + hash.length() + " символов");

            if (hash.length() == 32) {
                System.out.println("✅ Длина хеша правильная (32 символа)");
            } else {
                System.out.println("❌ Неправильная длина хеша");
            }

            if (hash.matches("[0-9a-f]+")) {
                System.out.println("✅ Хеш содержит только hex символы");
            } else {
                System.out.println("❌ Хеш содержит недопустимые символы");
            }

        } catch (IOException e) {
            System.err.println("❌ Ошибка теста: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testDuplicateDetection() {
        System.out.println("--- Тест 2: Обнаружение дубликатов ---");

        try {
            HashCalculator calculator = new HashCalculator();

            File file1 = File.createTempFile("duplicate1", ".txt");
            File file2 = File.createTempFile("duplicate2", ".txt");
            file1.deleteOnExit();
            file2.deleteOnExit();

            String content = "This is duplicate content for testing";

            try (FileWriter writer1 = new FileWriter(file1);
                 FileWriter writer2 = new FileWriter(file2)) {
                writer1.write(content);
                writer2.write(content);
            }

            String hash1 = calculator.calculateHash(file1);
            String hash2 = calculator.calculateHash(file2);

            System.out.println("Файл 1: " + file1.getName() + " -> " + hash1);
            System.out.println("Файл 2: " + file2.getName() + " -> " + hash2);

            if (hash1.equals(hash2)) {
                System.out.println("✅ Одинаковое содержимое дает одинаковые хеши");
            } else {
                System.out.println("❌ Одинаковое содержимое дало разные хеши");
            }

            boolean identical = calculator.areFilesIdentical(file1, file2);
            if (identical) {
                System.out.println("✅ areFilesIdentical правильно определил дубликаты");
            } else {
                System.out.println("❌ areFilesIdentical не определил дубликаты");
            }

            File file3 = File.createTempFile("different", ".txt");
            file3.deleteOnExit();

            try (FileWriter writer3 = new FileWriter(file3)) {
                writer3.write("Different content");
            }

            String hash3 = calculator.calculateHash(file3);
            boolean notIdentical = !calculator.areFilesIdentical(file1, file3);

            System.out.println("Файл 3: " + file3.getName() + " -> " + hash3);

            if (!hash1.equals(hash3) && notIdentical) {
                System.out.println("✅ Разное содержимое дает разные хеши");
            } else {
                System.out.println("❌ Проблема с определением разных файлов");
            }

        } catch (IOException e) {
            System.err.println("❌ Ошибка теста дубликатов: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testDifferentAlgorithms() {
        System.out.println("--- Тест 3: Разные алгоритмы ---");

        try {
            File tempFile = File.createTempFile("algo_test", ".txt");
            tempFile.deleteOnExit();

            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write("Test content for different algorithms");
            }

            String[] algorithms = {"MD5", "SHA-1", "SHA-256"};

            for (String algo : algorithms) {
                try {
                    HashCalculator calculator = new HashCalculator(algo);
                    String hash = calculator.calculateHash(tempFile);

                    System.out.printf("%s: %s (длина: %d)%n", algo, hash, hash.length());

                } catch (Exception e) {
                    System.out.printf("%s: не поддерживается - %s%n", algo, e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Ошибка теста алгоритмов: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testErrorHandling() {
        System.out.println("--- Тест 4: Обработка ошибок ---");

        HashCalculator calculator = new HashCalculator();

        try {
            calculator.calculateHash("nonexistent_file.txt");
            System.out.println("❌ Должна была быть ошибка для несуществующего файла");
        } catch (IOException e) {
            System.out.println("✅ Несуществующий файл: " + e.getMessage());
        }

        try {
            calculator.calculateHash((File) null);
            System.out.println("❌ Должна была быть ошибка для null файла");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Null файл: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("✅ Null файл (IO): " + e.getMessage());
        }

        try {
            new HashCalculator("NONEXISTENT_ALGORITHM");
            System.out.println("❌ Должна была быть ошибка для неподдерживаемого алгоритма");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Неподдерживаемый алгоритм: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testPerformance() {
        System.out.println("--- Тест 5: Производительность ---");

        try {
            File tempFile = File.createTempFile("performance_test", ".txt");
            tempFile.deleteOnExit();

            try (FileWriter writer = new FileWriter(tempFile)) {
                for (int i = 0; i < 10000; i++) {
                    writer.write("This is line " + i + " of performance test data.\n");
                }
            }

            HashCalculator calculator = new HashCalculator();

            HashCalculator.FileHashInfo info = calculator.getFileHashInfo(tempFile);

            System.out.println("Файл размером: " + info.getFileSize() + " байт");
            System.out.println("Время хеширования: " + info.getProcessingTimeMs() + " мс");
            System.out.println("Скорость: " + (info.getFileSize() / Math.max(1, info.getProcessingTimeMs())) + " байт/мс");
            System.out.println("Хеш: " + info.getHash().substring(0, 16) + "...");

            if (info.getProcessingTimeMs() < 5000) {
                System.out.println("✅ Производительность в порядке");
            } else {
                System.out.println("⚠️ Медленное хеширование, возможно файл очень большой");
            }

        } catch (IOException e) {
            System.err.println("❌ Ошибка теста производительности: " + e.getMessage());
        }

        System.out.println();
    }
}
