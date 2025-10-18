package ua.pro.baynova.duplicatefinder.index;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.pro.baynova.duplicatefinder.model.FileInfo;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SimpleFileIndex Tests")
class SimpleFileIndexTest {

    private SimpleFileIndex index;

    @BeforeEach
    void setUp() {
        index = new SimpleFileIndex();
    }


    // БАЗОВЫЕ ОПЕРАЦИИ
    @Test
    @DisplayName("Новый индекс пустой")
    void testNewIndexIsEmpty() {

        assertEquals(0, index.size(), "Новый индекс должен быть пустым");
        assertTrue(index.getAllFiles().isEmpty(), "Список файлов должен быть пустым");
    }

    @Test
    @DisplayName("Добавление файла увеличивает размер индекса")
    void testAddFile_IncreasesSize() {

        FileInfo file = createFileInfo("/path/file.txt", "hash1", 100);
        index.addOrUpdate(file);

        assertEquals(1, index.size(), "Размер индекса должен быть 1");
    }

    @Test
    @DisplayName("Добавленный файл можно получить по пути")
    void testAddAndGet() {

        FileInfo file = createFileInfo("/path/file.txt", "hash1", 100);
        index.addOrUpdate(file);

        FileInfo retrieved = index.getByPath("/path/file.txt");

        assertNotNull(retrieved, "Файл должен быть найден");
        assertEquals(file.getPath(), retrieved.getPath());
        assertEquals(file.getHash(), retrieved.getHash());
    }

    @Test
    @DisplayName("contains() возвращает true для существующего файла")
    void testContains_ExistingFile() {

        FileInfo file = createFileInfo("/path/file.txt", "hash1", 100);
        index.addOrUpdate(file);

        assertTrue(index.contains("/path/file.txt"), "Индекс должен содержать файл");
    }

    @Test
    @DisplayName("contains() возвращает false для несуществующего файла")
    void testContains_NonExistingFile() {

        assertFalse(index.contains("/path/nonexistent.txt"), "Индекс не должен содержать файл");
    }

    @Test
    @DisplayName("Удаление файла уменьшает размер индекса")
    void testRemove_DecreasesSize() {

        FileInfo file = createFileInfo("/path/file.txt", "hash1", 100);
        index.addOrUpdate(file);

        boolean removed = index.remove("/path/file.txt");

        assertTrue(removed, "remove() должен вернуть true");
        assertEquals(0, index.size(), "Размер индекса должен быть 0");
        assertFalse(index.contains("/path/file.txt"), "Файл не должен быть в индексе");
    }

    @Test
    @DisplayName("Удаление несуществующего файла возвращает false")
    void testRemove_NonExistingFile() {

        boolean removed = index.remove("/path/nonexistent.txt");

        assertFalse(removed, "remove() должен вернуть false для несуществующего файла");
    }

    @Test
    @DisplayName("clear() очищает весь индекс")
    void testClear() {

        index.addOrUpdate(createFileInfo("/path/file1.txt", "hash1", 100));
        index.addOrUpdate(createFileInfo("/path/file2.txt", "hash2", 200));

        index.clear();

        assertEquals(0, index.size(), "Индекс должен быть пустым после clear()");
        assertTrue(index.getAllFiles().isEmpty(), "getAllFiles() должен вернуть пустой список");
    }


    // ОБНОВЛЕНИЕ ФАЙЛОВ
    @Test
    @DisplayName("Обновление файла заменяет старую информацию")
    void testUpdate_ReplacesOldInfo() {

        FileInfo oldFile = createFileInfo("/path/file.txt", "oldHash", 100);
        FileInfo newFile = createFileInfo("/path/file.txt", "newHash", 200);

        index.addOrUpdate(oldFile);

        index.addOrUpdate(newFile);

        assertEquals(1, index.size(), "Размер не должен измениться");
        FileInfo retrieved = index.getByPath("/path/file.txt");
        assertEquals("newHash", retrieved.getHash(), "Хеш должен обновиться");
        assertEquals(200, retrieved.getSize(), "Размер должен обновиться");
    }


    // ПОИСК ДУБЛИКАТОВ
    @Test
    @DisplayName("Пустой индекс не содержит дубликатов")
    void testFindDuplicates_EmptyIndex() {

        List<List<FileInfo>> duplicates = index.findDuplicates();

        assertTrue(duplicates.isEmpty(), "Пустой индекс не должен иметь дубликатов");
    }

    @Test
    @DisplayName("Один файл не является дубликатом")
    void testFindDuplicates_SingleFile() {

        index.addOrUpdate(createFileInfo("/path/file.txt", "hash1", 100));

        List<List<FileInfo>> duplicates = index.findDuplicates();

        assertTrue(duplicates.isEmpty(), "Один файл не может быть дубликатом");
    }

    @Test
    @DisplayName("Два файла с одинаковым хешом - дубликаты")
    void testFindDuplicates_TwoFiles() {

        index.addOrUpdate(createFileInfo("/path/file1.txt", "sameHash", 100));
        index.addOrUpdate(createFileInfo("/path/file2.txt", "sameHash", 100));

        List<List<FileInfo>> duplicates = index.findDuplicates();

        assertEquals(1, duplicates.size(), "Должна быть одна группа дубликатов");
        assertEquals(2, duplicates.get(0).size(), "В группе должно быть 2 файла");
    }

    @Test
    @DisplayName("Три файла с одинаковым хешом образуют одну группу")
    void testFindDuplicates_ThreeFiles() {

        index.addOrUpdate(createFileInfo("/path/file1.txt", "sameHash", 100));
        index.addOrUpdate(createFileInfo("/path/file2.txt", "sameHash", 100));
        index.addOrUpdate(createFileInfo("/path/file3.txt", "sameHash", 100));

        List<List<FileInfo>> duplicates = index.findDuplicates();

        assertEquals(1, duplicates.size(), "Должна быть одна группа дубликатов");
        assertEquals(3, duplicates.get(0).size(), "В группе должно быть 3 файла");
    }

    @Test
    @DisplayName("Две группы дубликатов определяются корректно")
    void testFindDuplicates_TwoGroups() {

        index.addOrUpdate(createFileInfo("/path/file1.txt", "hash1", 100));
        index.addOrUpdate(createFileInfo("/path/file2.txt", "hash1", 100));

        index.addOrUpdate(createFileInfo("/path/file3.txt", "hash2", 200));
        index.addOrUpdate(createFileInfo("/path/file4.txt", "hash2", 200));

        index.addOrUpdate(createFileInfo("/path/unique.txt", "hash3", 300));

        List<List<FileInfo>> duplicates = index.findDuplicates();

        assertEquals(2, duplicates.size(), "Должно быть 2 группы дубликатов");

        for (List<FileInfo> group : duplicates) {
            assertEquals(2, group.size(), "Каждая группа должна содержать 2 файла");
        }
    }

    @Test
    @DisplayName("Группы дубликатов сортируются по размеру (большие первыми)")
    void testFindDuplicates_SortedBySize() {

        index.addOrUpdate(createFileInfo("/path/file1.txt", "hash1", 100));
        index.addOrUpdate(createFileInfo("/path/file2.txt", "hash1", 100));

        index.addOrUpdate(createFileInfo("/path/file3.txt", "hash2", 200));
        index.addOrUpdate(createFileInfo("/path/file4.txt", "hash2", 200));
        index.addOrUpdate(createFileInfo("/path/file5.txt", "hash2", 200));

        List<List<FileInfo>> duplicates = index.findDuplicates();

        assertEquals(2, duplicates.size());
        assertEquals(3, duplicates.get(0).size(), "Первая группа должна быть самой большой");
        assertEquals(2, duplicates.get(1).size(), "Вторая группа должна быть меньше");
    }

    @Test
    @DisplayName("findDuplicatesOf() находит дубликаты конкретного файла")
    void testFindDuplicatesOf() {

        index.addOrUpdate(createFileInfo("/path/file1.txt", "hash1", 100));
        index.addOrUpdate(createFileInfo("/path/file2.txt", "hash1", 100));
        index.addOrUpdate(createFileInfo("/path/file3.txt", "hash1", 100));
        index.addOrUpdate(createFileInfo("/path/other.txt", "hash2", 200));

        List<FileInfo> duplicates = index.findDuplicatesOf("/path/file1.txt");

        assertEquals(2, duplicates.size(), "Должно быть найдено 2 дубликата");
        assertFalse(duplicates.stream().anyMatch(f -> f.getPath().equals("/path/file1.txt")),
                "Сам файл не должен быть в списке дубликатов");
    }

    @Test
    @DisplayName("findDuplicatesOf() возвращает пустой список для уникального файла")
    void testFindDuplicatesOf_UniqueFile() {

        index.addOrUpdate(createFileInfo("/path/unique.txt", "hash1", 100));
        index.addOrUpdate(createFileInfo("/path/other.txt", "hash2", 200));

        List<FileInfo> duplicates = index.findDuplicatesOf("/path/unique.txt");

        assertTrue(duplicates.isEmpty(), "Уникальный файл не должен иметь дубликатов");
    }

    @Test
    @DisplayName("findDuplicatesOf() возвращает пустой список для несуществующего файла")
    void testFindDuplicatesOf_NonExistentFile() {

        List<FileInfo> duplicates = index.findDuplicatesOf("/path/nonexistent.txt");

        assertTrue(duplicates.isEmpty(), "Несуществующий файл не имеет дубликатов");
    }


    // СТАТИСТИКА
    @Test
    @DisplayName("Статистика пустого индекса корректна")
    void testStatistics_EmptyIndex() {

        SimpleFileIndex.IndexStatistics stats = index.getStatistics();

        assertEquals(0, stats.getTotalFiles());
        assertEquals(0, stats.getTotalSize());
        assertEquals(0, stats.getDuplicateGroups());
        assertEquals(0, stats.getDuplicateFiles());
        assertEquals(0, stats.getDuplicateSize());
        assertEquals(0, stats.getPotentialSavings());
    }

    @Test
    @DisplayName("Статистика без дубликатов корректна")
    void testStatistics_NoDuplicates() {

        index.addOrUpdate(createFileInfo("/path/file1.txt", "hash1", 100));
        index.addOrUpdate(createFileInfo("/path/file2.txt", "hash2", 200));
        index.addOrUpdate(createFileInfo("/path/file3.txt", "hash3", 300));

        SimpleFileIndex.IndexStatistics stats = index.getStatistics();

        assertEquals(3, stats.getTotalFiles());
        assertEquals(600, stats.getTotalSize());
        assertEquals(0, stats.getDuplicateGroups());
        assertEquals(0, stats.getDuplicateFiles());
        assertEquals(0, stats.getPotentialSavings());
    }

    @Test
    @DisplayName("Статистика с дубликатами рассчитывается правильно")
    void testStatistics_WithDuplicates() {

        index.addOrUpdate(createFileInfo("/path/file1.txt", "hash1", 100));
        index.addOrUpdate(createFileInfo("/path/file2.txt", "hash1", 100));
        index.addOrUpdate(createFileInfo("/path/file3.txt", "hash1", 100));

        index.addOrUpdate(createFileInfo("/path/unique.txt", "hash2", 200));

        SimpleFileIndex.IndexStatistics stats = index.getStatistics();

        assertAll("Статистика должна быть корректной",
                () -> assertEquals(4, stats.getTotalFiles(), "Всего файлов"),
                () -> assertEquals(500, stats.getTotalSize(), "Общий размер"),
                () -> assertEquals(1, stats.getDuplicateGroups(), "Групп дубликатов"),
                () -> assertEquals(2, stats.getDuplicateFiles(), "Файлов-дубликатов (3-1)"),
                () -> assertEquals(200, stats.getDuplicateSize(), "Размер дубликатов (2*100)"),
                () -> assertEquals(200, stats.getPotentialSavings(), "Можно освободить")
        );
    }


    // ПОТОКОБЕЗОПАСНОСТЬ (проверка ConcurrentHashMap)
    @Test
    @DisplayName("Параллельное добавление файлов работает корректно")
    void testConcurrentAdd() throws InterruptedException {
        int threadCount = 10;
        int filesPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < filesPerThread; j++) {
                    String path = String.format("/path/thread%d_file%d.txt", threadId, j);
                    String hash = String.format("hash%d_%d", threadId, j);
                    index.addOrUpdate(createFileInfo(path, hash, 100));
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(threadCount * filesPerThread, index.size(),
                "Все файлы должны быть добавлены без потерь");
    }


    // ОШИБОЧНЫЕ СИТУАЦИИ
    @Test
    @DisplayName("Добавление null файла выбрасывает исключение")
    void testAddNull_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> index.addOrUpdate(null),
                "Должно выбрасываться IllegalArgumentException для null");
    }


    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    private FileInfo createFileInfo(String path, String hash, long size) {
        return new FileInfo(
                path,
                hash,
                size,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
