package ua.pro.baynova.duplicatefinder.cli;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

@DisplayName("CommandLineParser Tests")
class CommandLineParserTest {

    private CommandLineParser parser;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        parser = new CommandLineParser();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Без параметров используются значения по умолчанию")
    void testDefaultValues() {
        parser.parse(new String[]{});

        assertNotNull(parser.getDirectory(), "Directory не должна быть null");
        assertTrue(parser.getThreads() > 0, "Threads должен быть > 0");
        assertTrue(parser.getMinSize() >= 0, "MinSize должен быть >= 0");
        assertNotNull(parser.getExtensions(), "Extensions не должны быть null");
        assertNotNull(parser.getAlgorithm(), "Algorithm не должен быть null");
        assertFalse(parser.isShowHelp(), "ShowHelp должен быть false");
    }

    @Test
    @DisplayName("--directory устанавливает директорию")
    void testDirectory_LongForm() {
        parser.parse(new String[]{"--directory", "/tmp/test"});

        assertEquals("/tmp/test", parser.getDirectory());
    }

    @Test
    @DisplayName("-d устанавливает директорию (короткая форма)")
    void testDirectory_ShortForm() {
        parser.parse(new String[]{"-d", "/home/user/documents"});

        assertEquals("/home/user/documents", parser.getDirectory());
    }

    @Test
    @DisplayName("--directory без значения выбрасывает исключение")
    void testDirectory_MissingValue() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[]{"--directory"}),
                "Должно выбрасываться исключение если не указано значение");
    }

    @Test
    @DisplayName("--threads устанавливает количество потоков")
    void testThreads_LongForm() {
        parser.parse(new String[]{"--threads", "8"});

        assertEquals(8, parser.getThreads());
    }

    @Test
    @DisplayName("-t устанавливает количество потоков (короткая форма)")
    void testThreads_ShortForm() {
        parser.parse(new String[]{"-t", "4"});

        assertEquals(4, parser.getThreads());
    }

    @Test
    @DisplayName("--threads с некорректным числом выбрасывает исключение")
    void testThreads_InvalidNumber() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[]{"--threads", "abc"}),
                "Должно выбрасываться исключение для некорректного числа");
    }

    @Test
    @DisplayName("--threads с отрицательным значением выбрасывает исключение")
    void testThreads_NegativeValue() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[]{"--threads", "-5"}),
                "Должно выбрасываться исключение для отрицательного значения");
    }

    @Test
    @DisplayName("--threads с нулевым значением выбрасывает исключение")
    void testThreads_ZeroValue() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[]{"--threads", "0"}),
                "Должно выбрасываться исключение для нулевого значения");
    }

    @Test
    @DisplayName("--min-size устанавливает минимальный размер")
    void testMinSize_LongForm() {
        parser.parse(new String[]{"--min-size", "1048576"});

        assertEquals(1048576, parser.getMinSize());
    }

    @Test
    @DisplayName("-s устанавливает минимальный размер (короткая форма)")
    void testMinSize_ShortForm() {
        parser.parse(new String[]{"-s", "10240"});

        assertEquals(10240, parser.getMinSize());
    }

    @Test
    @DisplayName("--min-size с некорректным значением выбрасывает исключение")
    void testMinSize_InvalidValue() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[]{"--min-size", "not_a_number"}),
                "Должно выбрасываться исключение для некорректного значения");
    }

    @Test
    @DisplayName("--min-size с отрицательным значением выбрасывает исключение")
    void testMinSize_NegativeValue() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[]{"--min-size", "-100"}),
                "Должно выбрасываться исключение для отрицательного значения");
    }

    @Test
    @DisplayName("--extensions парсит список расширений")
    void testExtensions_LongForm() {
        parser.parse(new String[]{"--extensions", "txt,pdf,doc"});

        var extensions = parser.getExtensions();
        assertEquals(3, extensions.size());
        assertTrue(extensions.contains(".txt"));
        assertTrue(extensions.contains(".pdf"));
        assertTrue(extensions.contains(".doc"));
    }

    @Test
    @DisplayName("-e парсит список расширений (короткая форма)")
    void testExtensions_ShortForm() {
        parser.parse(new String[]{"-e", "jpg,png"});

        var extensions = parser.getExtensions();
        assertEquals(2, extensions.size());
        assertTrue(extensions.contains(".jpg"));
        assertTrue(extensions.contains(".png"));
    }

    @Test
    @DisplayName("Расширения автоматически получают точку")
    void testExtensions_AutomaticallyAddsDot() {
        parser.parse(new String[]{"--extensions", "txt,.pdf,doc"});

        var extensions = parser.getExtensions();
        assertTrue(extensions.contains(".txt"), "txt должно стать .txt");
        assertTrue(extensions.contains(".pdf"), ".pdf должно остаться .pdf");
        assertTrue(extensions.contains(".doc"), "doc должно стать .doc");
    }

    @Test
    @DisplayName("--algorithm устанавливает алгоритм хеширования")
    void testAlgorithm_LongForm() {
        parser.parse(new String[]{"--algorithm", "SHA-256"});

        assertEquals("SHA-256", parser.getAlgorithm());
    }

    @Test
    @DisplayName("-a устанавливает алгоритм (короткая форма)")
    void testAlgorithm_ShortForm() {
        parser.parse(new String[]{"-a", "SHA-1"});

        assertEquals("SHA-1", parser.getAlgorithm());
    }

    @Test
    @DisplayName("Алгоритм приводится к верхнему регистру")
    void testAlgorithm_UpperCase() {
        parser.parse(new String[]{"--algorithm", "md5"});

        assertEquals("MD5", parser.getAlgorithm());
    }

    @Test
    @DisplayName("Некорректный алгоритм выбрасывает исключение")
    void testAlgorithm_Invalid() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[]{"--algorithm", "INVALID"}),
                "Должно выбрасываться исключение для некорректного алгоритма");
    }

    @Test
    @DisplayName("--help устанавливает флаг справки")
    void testHelp_LongForm() {
        parser.parse(new String[]{"--help"});

        assertTrue(parser.isShowHelp());
    }

    @Test
    @DisplayName("-h устанавливает флаг справки (короткая форма)")
    void testHelp_ShortForm() {
        parser.parse(new String[]{"-h"});

        assertTrue(parser.isShowHelp());
    }

    @Test
    @DisplayName("--verbose устанавливает флаг подробного вывода")
    void testVerbose_LongForm() {
        parser.parse(new String[]{"--verbose"});

        assertTrue(parser.isVerbose());
    }

    @Test
    @DisplayName("-v устанавливает флаг подробного вывода (короткая форма)")
    void testVerbose_ShortForm() {
        parser.parse(new String[]{"-v"});

        assertTrue(parser.isVerbose());
    }

    @Test
    @DisplayName("Несколько параметров парсятся корректно")
    void testMultipleParameters() {
        parser.parse(new String[]{
                "--directory", "/tmp",
                "--threads", "8",
                "--min-size", "1024",
                "--extensions", "txt,pdf",
                "--algorithm", "SHA-256",
                "--verbose"
        });

        assertAll("Все параметры должны быть установлены",
                () -> assertEquals("/tmp", parser.getDirectory()),
                () -> assertEquals(8, parser.getThreads()),
                () -> assertEquals(1024, parser.getMinSize()),
                () -> assertEquals(2, parser.getExtensions().size()),
                () -> assertEquals("SHA-256", parser.getAlgorithm()),
                () -> assertTrue(parser.isVerbose())
        );
    }

    @Test
    @DisplayName("Короткие и длинные формы")
    void testMixedShortAndLongForms() {
        parser.parse(new String[]{
                "-d", "/tmp",
                "--threads", "4",
                "-s", "1024",
                "--verbose"
        });

        assertAll("Смешанные формы должны работать",
                () -> assertEquals("/tmp", parser.getDirectory()),
                () -> assertEquals(4, parser.getThreads()),
                () -> assertEquals(1024, parser.getMinSize()),
                () -> assertTrue(parser.isVerbose())
        );
    }

    @Test
    @DisplayName("Неизвестный параметр выбрасывает исключение")
    void testUnknownParameter() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[]{"--unknown-param"}),
                "Должно выбрасываться исключение для неизвестного параметра");
    }

    @Test
    @DisplayName("Параметр без значения выбрасывает исключение")
    void testParameterWithoutValue() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[]{"--directory"}),
                "Должно выбрасываться исключение если параметр требует значение");
    }

    @Test
    @DisplayName("printHelp() выводит текст справки")
    void testPrintHelp() {
        parser.printHelp();
        String output = outContent.toString();

        assertTrue(output.contains("FILE DUPLICATE FINDER"), "Справка должна содержать название программы");
        assertTrue(output.contains("ИСПОЛЬЗОВАНИЕ"), "Справка должна содержать раздел использования");
        assertTrue(output.contains("--directory"), "Справка должна описывать параметры");
        assertTrue(output.contains("--threads"), "Справка должна описывать параметры");
        assertTrue(output.contains("ПРИМЕРЫ"), "Справка должна содержать примеры");
    }

    @Test
    @DisplayName("printConfiguration() выводит текущую конфигурацию")
    void testPrintConfiguration() {
        parser.parse(new String[]{"--directory", "/tmp", "--threads", "8"});

        parser.printConfiguration();
        String output = outContent.toString();

        assertTrue(output.contains("КОНФИГУРАЦИЯ"), "Должен быть заголовок конфигурации");
        assertTrue(output.contains("/tmp"), "Должна быть указана директория");
        assertTrue(output.contains("8"), "Должно быть указано количество потоков");
    }

    @Test
    @DisplayName("CLI параметры переопределяют значения из properties")
    void testCLIOverridesProperties() {
        CommandLineParser freshParser = new CommandLineParser();
        int defaultThreads = freshParser.getThreads();

        parser.parse(new String[]{"--threads", "16"});

        assertEquals(16, parser.getThreads(), "CLI должен переопределить значение из properties");
        assertNotEquals(defaultThreads, parser.getThreads(), "Значение должно измениться");
    }

    @Test
    @DisplayName("Без CLI параметров используются значения из properties")
    void testDefaultsFromProperties() {
        parser.parse(new String[]{});

        assertNotNull(parser.getAppConfig(), "AppConfig должен быть загружен");
        assertNotNull(parser.getDirectory());
        assertNotNull(parser.getAlgorithm());
    }
}