package ua.pro.baynova.duplicatefinder.hash;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HashCalculator Tests")
class HashCalculatorTest {

    private HashCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new HashCalculator("MD5");
    }

    // БАЗОВЫЕ ТЕСТЫ
    @Test
    @DisplayName("Одинаковое содержимое → одинаковый хеш")
    void testSameContent_SameHash(@TempDir Path tempDir) throws IOException {

        File file1 = createFileWithContent(tempDir, "file1.txt", "Hello World");
        File file2 = createFileWithContent(tempDir, "file2.txt", "Hello World");

        String hash1 = calculator.calculateHash(file1);
        String hash2 = calculator.calculateHash(file2);

        assertEquals(hash1, hash2, "Файлы с одинаковым содержимым должны иметь одинаковый хеш");
    }

    @Test
    @DisplayName("Разное содержимое → разный хеш")
    void testDifferentContent_DifferentHash(@TempDir Path tempDir) throws IOException {

        File file1 = createFileWithContent(tempDir, "file1.txt", "Hello World");
        File file2 = createFileWithContent(tempDir, "file2.txt", "Different Content");

        String hash1 = calculator.calculateHash(file1);
        String hash2 = calculator.calculateHash(file2);

        assertNotEquals(hash1, hash2, "Файлы с разным содержимым должны иметь разный хеш");
    }

    @Test
    @DisplayName("Пустой файл вычисляется корректно")
    void testEmptyFile(@TempDir Path tempDir) throws IOException {
        File emptyFile = createFileWithContent(tempDir, "empty.txt", "");

        assertDoesNotThrow(() -> {
            String hash = calculator.calculateHash(emptyFile);
            assertNotNull(hash, "Хеш пустого файла не должен быть null");
            assertFalse(hash.isEmpty(), "Хеш пустого файла не должен быть пустой строкой");
        });
    }

    @Test
    @DisplayName("MD5 хеш имеет правильную длину (32 символа)")
    void testMD5HashLength(@TempDir Path tempDir) throws IOException {

        File file = createFileWithContent(tempDir, "test.txt", "Test content");

        String hash = calculator.calculateHash(file);

        assertEquals(32, hash.length(), "MD5 хеш должен быть 32 символа (128 бит в hex)");
    }

    @Test
    @DisplayName("Хеш состоит только из hex символов")
    void testHashIsHexadecimal(@TempDir Path tempDir) throws IOException {

        File file = createFileWithContent(tempDir, "test.txt", "Test content");

        String hash = calculator.calculateHash(file);

        assertTrue(hash.matches("[0-9a-f]+"), "Хеш должен содержать только hex символы (0-9, a-f)");
    }

    // ТЕСТЫ РАЗНЫХ АЛГОРИТМОВ
    @Test
    @DisplayName("SHA-1 хеш имеет длину 40 символов")
    void testSHA1HashLength(@TempDir Path tempDir) throws IOException {

        HashCalculator sha1Calculator = new HashCalculator("SHA-1");
        File file = createFileWithContent(tempDir, "test.txt", "Test content");

        String hash = sha1Calculator.calculateHash(file);

        assertEquals(40, hash.length(), "SHA-1 хеш должен быть 40 символов (160 бит в hex)");
    }

    @Test
    @DisplayName("SHA-256 хеш имеет длину 64 символа")
    void testSHA256HashLength(@TempDir Path tempDir) throws IOException {

        HashCalculator sha256Calculator = new HashCalculator("SHA-256");
        File file = createFileWithContent(tempDir, "test.txt", "Test content");

        String hash = sha256Calculator.calculateHash(file);

        assertEquals(64, hash.length(), "SHA-256 хеш должен быть 64 символа (256 бит в hex)");
    }

    @Test
    @DisplayName("Разные алгоритмы → разные хеши")
    void testDifferentAlgorithms_DifferentHashes(@TempDir Path tempDir) throws IOException {

        File file = createFileWithContent(tempDir, "test.txt", "Test content");
        HashCalculator md5 = new HashCalculator("MD5");
        HashCalculator sha1 = new HashCalculator("SHA-1");
        HashCalculator sha256 = new HashCalculator("SHA-256");

        String hashMD5 = md5.calculateHash(file);
        String hashSHA1 = sha1.calculateHash(file);
        String hashSHA256 = sha256.calculateHash(file);

        assertNotEquals(hashMD5, hashSHA1, "MD5 и SHA-1 должны давать разные хеши");
        assertNotEquals(hashMD5, hashSHA256, "MD5 и SHA-256 должны давать разные хеши");
        assertNotEquals(hashSHA1, hashSHA256, "SHA-1 и SHA-256 должны давать разные хеши");
    }

    // ТЕСТЫ ОШИБОЧНЫХ СИТУАЦИЙ
    @Test
    @DisplayName("Несуществующий файл → IOException")
    void testNonExistentFile() {

        File nonExistent = new File("non_existent_file_12345.txt");

        assertThrows(IOException.class, () -> calculator.calculateHash(nonExistent),
                "Должно выбрасываться IOException для несуществующего файла");
    }

    @Test
    @DisplayName("null файл → IllegalArgumentException")
    void testNullFile() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateHash((File) null),
                "Должно выбрасываться IllegalArgumentException для null");
    }

    @Test
    @DisplayName("Директория вместо файла → IOException")
    void testDirectory(@TempDir Path tempDir) {

        File directory = tempDir.toFile();

        assertThrows(IOException.class, () -> calculator.calculateHash(directory),
                "Должно выбрасываться IOException для директории");
    }

    @Test
    @DisplayName("Некорректный алгоритм → IllegalArgumentException")
    void testInvalidAlgorithm() {

        assertThrows(IllegalArgumentException.class,
                () -> new HashCalculator("INVALID_ALGORITHM"),
                "Должно выбрасываться IllegalArgumentException для некорректного алгоритма");
    }

    // ТЕСТЫ МЕТОДА areFilesIdentical
    @Test
    @DisplayName("Одинаковые файлы идентичны")
    void testAreFilesIdentical_SameContent(@TempDir Path tempDir) throws IOException {

        File file1 = createFileWithContent(tempDir, "file1.txt", "Same content");
        File file2 = createFileWithContent(tempDir, "file2.txt", "Same content");

        boolean identical = calculator.areFilesIdentical(file1, file2);

        assertTrue(identical, "Файлы с одинаковым содержимым должны быть идентичны");
    }

    @Test
    @DisplayName("Разные файлы не идентичны")
    void testAreFilesIdentical_DifferentContent(@TempDir Path tempDir) throws IOException {

        File file1 = createFileWithContent(tempDir, "file1.txt", "Content 1");
        File file2 = createFileWithContent(tempDir, "file2.txt", "Content 2");

        boolean identical = calculator.areFilesIdentical(file1, file2);

        assertFalse(identical, "Файлы с разным содержимым не должны быть идентичны");
    }

    @Test
    @DisplayName("Файл идентичен самому себе")
    void testAreFilesIdentical_SameFile(@TempDir Path tempDir) throws IOException {

        File file = createFileWithContent(tempDir, "file.txt", "Content");

        boolean identical = calculator.areFilesIdentical(file, file);

        assertTrue(identical, "Файл должен быть идентичен самому себе");
    }

    @Test
    @DisplayName("Файлы разного размера не идентичны (быстрая проверка)")
    void testAreFilesIdentical_DifferentSize(@TempDir Path tempDir) throws IOException {

        File file1 = createFileWithContent(tempDir, "file1.txt", "Short");
        File file2 = createFileWithContent(tempDir, "file2.txt", "Much longer content");

        boolean identical = calculator.areFilesIdentical(file1, file2);

        assertFalse(identical, "Файлы разного размера не могут быть идентичны");
    }

    // ТЕСТЫ ПРОИЗВОДИТЕЛЬНОСТИ
    @Test
    @DisplayName("Хеширование большого файла завершается за нормальное время")
    @Timeout(5)
    void testLargeFilePerformance(@TempDir Path tempDir) throws IOException {

        File largeFile = tempDir.resolve("large.bin").toFile();
        byte[] data = new byte[10 * 1024 * 1024];
        Files.write(largeFile.toPath(), data);

        assertDoesNotThrow(() -> {
            String hash = calculator.calculateHash(largeFile);
            assertNotNull(hash);
        });
    }

    // ТЕСТЫ getFileHashInfo
    @Test
    @DisplayName("getFileHashInfo возвращает корректную информацию")
    void testGetFileHashInfo(@TempDir Path tempDir) throws IOException {

        File file = createFileWithContent(tempDir, "test.txt", "Test content");

        HashCalculator.FileHashInfo info = calculator.getFileHashInfo(file);

        assertAll("FileHashInfo должна быть полностью заполнена",
                () -> assertNotNull(info, "FileHashInfo не должна быть null"),
                () -> assertTrue(info.filePath().contains("test.txt"), "Путь должен содержать имя файла"),
                () -> assertTrue(info.fileSize() > 0, "Размер должен быть больше 0"),
                () -> assertNotNull(info.hash(), "Хеш не должен быть null"),
                () -> assertTrue(info.processingTimeMs() >= 0, "Время обработки должно быть >= 0"),
                () -> assertEquals("MD5", info.algorithm(), "Алгоритм должен быть MD5")
        );
    }

    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    private File createFileWithContent(Path tempDir, String filename, String content) throws IOException {
        File file = tempDir.resolve(filename).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }
}
