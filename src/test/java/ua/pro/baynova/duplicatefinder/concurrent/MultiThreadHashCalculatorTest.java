package ua.pro.baynova.duplicatefinder.concurrent;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import ua.pro.baynova.duplicatefinder.hash.HashCalculator;
import ua.pro.baynova.duplicatefinder.model.FileInfo;
import ua.pro.baynova.duplicatefinder.scanner.FileScanner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MultiThreadHashCalculator Tests")
public class MultiThreadHashCalculatorTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Калькулятор с разным количеством потоков")
    void testCreation() {
        assertDoesNotThrow(() -> new MultiThreadHashCalculator(1, "MD5"));
        assertDoesNotThrow(() -> new MultiThreadHashCalculator(4, "MD5"));
        assertDoesNotThrow(() -> new MultiThreadHashCalculator(8, "SHA-256"));
    }

    @Test
    @DisplayName("Создание с дефолтными параметрами")
    void testDefaultConstructor() {
        assertDoesNotThrow(() -> new MultiThreadHashCalculator());
    }

    @Test
    @DisplayName("Многопоточный хеш совпадает с однопоточным")
    void testMultiThreadMatchesSingleThread() throws Exception {

        List<FileScanner.FileTask> tasks = createTestFiles(10);

        HashCalculator singleThreads = new HashCalculator("MD5");
        MultiThreadHashCalculator multiThread = new MultiThreadHashCalculator(4, "MD5");

        List<String> singleThreadHashes = new ArrayList<>();
        for (FileScanner.FileTask task : tasks) {
            String hash = singleThreads.calculateHash(task.getPath());
            singleThreadHashes.add(hash);
        }

        List<FileInfo> multiThreadResults = multiThread.processFiles(tasks);

        assertEquals(tasks.size(), multiThreadResults.size(),
                "Количество обработанных файлов должно совпадать");

        for (int i = 0; i < tasks.size(); i++) {
            String path = tasks.get(i).getPath();
            String expectedHash = singleThreadHashes.get(i);

            FileInfo result = multiThreadResults.stream()
                    .filter(f -> f.getPath().equals(path))
                    .findFirst()
                    .orElse(null);

            assertNotNull(result, "Файл должен быть обработан: " + path);
            assertEquals(expectedHash, result.getHash(),
                    "Хеш должен совпадать с однопоточной версией");
        }
    }

    @Test
    @DisplayName("Обработка пустого списка не вызывает ошибок")
    void testEmptyList() throws Exception {
        MultiThreadHashCalculator calculator = new MultiThreadHashCalculator(4, "MD5");
        List<FileScanner.FileTask> emptyList = new ArrayList<>();

        List<FileInfo> results = calculator.processFiles(emptyList);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Нет потерянных файлов при параллельной обработке")
    void testNoLostFiles() throws Exception {
        List<FileScanner.FileTask> tasks = createTestFiles(100);
        MultiThreadHashCalculator calculator = new MultiThreadHashCalculator(8, "MD5");

        List<FileInfo> results = calculator.processFiles(tasks);

        assertEquals(tasks.size(), results.size(),
                "Все файлы должны быть обработаны без потерь");

        Set<String> uniquePaths = ConcurrentHashMap.newKeySet();
        for (FileInfo info : results) {
            assertTrue(uniquePaths.add(info.getPath()),
                    "Пути должны быть уникальны: " + info.getPath());
        }
    }

    @Test
    @DisplayName("Все файлы обработаны ровно один раз")
    void testEachFileProcessedOnce() throws Exception {
        List<FileScanner.FileTask> tasks = createTestFiles(50);
        MultiThreadHashCalculator calculator = new MultiThreadHashCalculator(4, "MD5");

        List<FileInfo> results = calculator.processFiles(tasks);

        for (FileScanner.FileTask task : tasks) {
            long count = results.stream()
                    .filter(f -> f.getPath().equals(task.getPath()))
                    .count();

            assertEquals(1, count,
                    "Файл должен быть обработан один раз: " + task.getPath());
        }
    }

    @Test
    @DisplayName("Многопоточная обработка быстрее однопоточной")
    @Timeout(30)
    void testMultiThreadFasterThanSingle() throws Exception {
        List<FileScanner.FileTask> tasks = createTestFiles(50);

        long singleThreadStart = System.currentTimeMillis();
        HashCalculator singleThread = new HashCalculator("MD5");
        for (FileScanner.FileTask task : tasks) {
            singleThread.calculateHash(task.getPath());
        }
        long singleThreadTime = System.currentTimeMillis() - singleThreadStart;

        long multiThreadStart = System.currentTimeMillis();
        MultiThreadHashCalculator multiThread = new MultiThreadHashCalculator(4, "MD5");
        multiThread.processFiles(tasks);
        long multiThreadTime = System.currentTimeMillis() - multiThreadStart;

        System.out.println("Однопоточно: " + singleThreadTime + " мс");
        System.out.println("Многопоточно: " + multiThreadTime + " мс");
        System.out.println("Ускорение: " + (double) singleThreadTime / multiThreadTime + "x");

        assertTrue(multiThreadTime < singleThreadTime * 2,
                "Многопоточная версия не должна быть медленнее более чем в 2 раза");
    }

    @Test
    @DisplayName("Производительность растёт с увеличением потоков")
    @Timeout(60)
    void testPerformanceScalesWithThreads() throws Exception {
        List<FileScanner.FileTask> tasks = createTestFiles(100);

        long time1Thread = measureProcessingTime(tasks, 1);
        long time4Threads = measureProcessingTime(tasks, 4);

        System.out.println("1 поток: " + time1Thread + " мс");
        System.out.println("4 потока: " + time4Threads + " мс");
        System.out.println("Ускорение: " + (double) time1Thread / time4Threads + "x");

        assertTrue(time4Threads < time1Thread,
                "4 потока должны работать быстрее чем 1");
    }

    @Test
    @DisplayName("Работа с разными алгоритмами хеширования")
    void testDifferentAlgorithms() throws Exception {
        List<FileScanner.FileTask> tasks = createTestFiles(10);

        MultiThreadHashCalculator md5 = new MultiThreadHashCalculator(4, "MD5");
        List<FileInfo> resultsMD5 = md5.processFiles(tasks);
        assertEquals(tasks.size(), resultsMD5.size());
        resultsMD5.forEach(r -> assertEquals(32, r.getHash().length()));

        MultiThreadHashCalculator sha1 = new MultiThreadHashCalculator(4, "SHA-1");
        List<FileInfo> resultsSHA1 = sha1.processFiles(tasks);
        assertEquals(tasks.size(), resultsSHA1.size());
        resultsSHA1.forEach(r -> assertEquals(40, r.getHash().length()));

        MultiThreadHashCalculator sha256 = new MultiThreadHashCalculator(4, "SHA-256");
        List<FileInfo> resultsSHA256 = sha256.processFiles(tasks);
        assertEquals(tasks.size(), resultsSHA256.size());
        resultsSHA256.forEach(r -> assertEquals(64, r.getHash().length()));
    }

    @Test
    @DisplayName("Обработка большого количества файлов")
    @Timeout(60)
    void testLargeNumberOfFiles() throws Exception {
        List<FileScanner.FileTask> tasks = createTestFiles(500);
        MultiThreadHashCalculator calculator = new MultiThreadHashCalculator(8, "MD5");

        List<FileInfo> results = calculator.processFiles(tasks);
        assertEquals(tasks.size(), results.size(),
                "Все файлы должны быть обработаны");
    }

    @Test
    @DisplayName("Работа с максимальным количеством потоков")
    void testMaxThreads() throws Exception {
        List<FileScanner.FileTask> tasks = createTestFiles(20);
        int maxThreads = Runtime.getRuntime().availableProcessors() * 2;
        MultiThreadHashCalculator calculator = new MultiThreadHashCalculator(maxThreads, "MD5");

        List<FileInfo> results = calculator.processFiles(tasks);
        assertEquals(tasks.size(), results.size());
    }

    private List<FileScanner.FileTask> createTestFiles(int count) throws IOException {
        List<FileScanner.FileTask> tasks = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            File file = tempDir.resolve("testfile_" + i + ".txt").toFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Test content for file " + i + "\n");
                writer.write("Some additional data to make files different\n");
                writer.write("File number: " + i);
            }

            FileScanner.FileTask task = new FileScanner.FileTask(
                    file.getAbsolutePath(),
                    file.length(),
                    LocalDateTime.now()
            );
            tasks.add(task);
        }

        return tasks;
    }

    private long measureProcessingTime(List<FileScanner.FileTask> tasks, int threads)
            throws InterruptedException {
        MultiThreadHashCalculator calculator = new MultiThreadHashCalculator(threads, "MD5");

        long start = System.currentTimeMillis();
        calculator.processFiles(tasks);
        long duration = System.currentTimeMillis() - start;

        return duration;
    }
}
