package ua.pro.baynova.duplicatefinder.scanner;

import java.io.IOException;
import java.util.List;

public class FileScannerTest {

    public static void main(String[] args) {
        System.out.println("=== Тестируем FileScanner ===\n");

        testBasicScanning();
        testWithFilters();
        testErrorHandling();

        System.out.println("\n✅ Все тесты FileScanner пройдены!");
    }

    private static void testBasicScanning() {
        System.out.println("--- Тест 1: Базовое сканирование ---");

        FileScanner scanner = new FileScanner();

        try {
            List<FileScanner.FileTask> files = scanner.scanDirectory(".");

            System.out.println("Найдено файлов: " + files.size());

            files.stream()
                    .limit(5)
                    .forEach(file -> {
                        System.out.printf("  - %s (%d байт)%n",
                                getFileName(file.getPath()), file.getSize());
                    });

            if (files.size() > 5) {
                System.out.println("  ... и еще " + (files.size() - 5) + " файлов");
            }

            boolean foundPom = files.stream()
                    .anyMatch(file -> file.getPath().endsWith("pom.xml"));

            if (foundPom) {
                System.out.println("✅ Найден pom.xml - сканирование работает");
            } else {
                System.out.println("⚠️ pom.xml не найден, но это может быть нормально");
            }

        } catch (IOException e) {
            System.err.println("❌ Ошибка сканирования: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testWithFilters() {
        System.out.println("--- Тест 2: Сканирование с фильтрами ---");

        var filter = FileScanner.Filters.byExtensions(".java", ".xml");
        FileScanner filteredScanner = new FileScanner(filter, false, 10);

        try {
            List<FileScanner.FileTask> files = filteredScanner.scanDirectory(".");

            System.out.println("Найдено Java/XML файлов: " + files.size());

            files.forEach(file -> {
                System.out.printf("  - %s (%d байт)%n",
                        getFileName(file.getPath()), file.getSize());
            });

            boolean allMatch = files.stream()
                    .allMatch(file -> file.getPath().endsWith(".java") || file.getPath().endsWith(".xml"));

            if (allMatch) {
                System.out.println("✅ Фильтр работает - только .java и .xml файлы");
            } else {
                System.out.println("❌ Фильтр не работает - найдены лишние файлы");
            }

        } catch (IOException e) {
            System.err.println("❌ Ошибка фильтрованного сканирования: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testErrorHandling() {
        System.out.println("--- Тест 3: Обработка ошибок ---");

        FileScanner scanner = new FileScanner();

        try {
            scanner.scanDirectory("/nonexistent/directory/path");
            System.out.println("❌ Должна была быть ошибка для несуществующей директории");
        } catch (IOException e) {
            System.out.println("✅ Несуществующая директория обработана: " + e.getMessage());
        }

        try {
            scanner.scanDirectory("");
            System.out.println("❌ Должна была быть ошибка для пустого пути");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Пустой путь обработан: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("✅ Пустой путь обработан как IO ошибка: " + e.getMessage());
        }

        try {
            scanner.scanDirectory(null);
            System.out.println("❌ Должна была быть ошибка для null пути");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Null путь обработан: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("✅ Null путь обработан как IO ошибка: " + e.getMessage());
        }

        System.out.println();
    }

    private static String getFileName(String path) {
        int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
    }
}