package ua.pro.baynova.duplicatefinder.cli;

import ua.pro.baynova.duplicatefinder.config.AppConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandLineParser {

    private final AppConfig config;
    private String directory;
    private int threads;
    private long minSize;
    private List<String> extensions;
    private String algorithm;
    private boolean showHelp = false;
    private boolean verbose;

    public CommandLineParser() {
        this.config = new AppConfig();
        loadDefaults();
    }

    private void loadDefaults() {
        this.directory = config.getDirectory();
        this.threads = config.getThreads();
        this.minSize = config.getMinSize();
        this.extensions = new ArrayList<>(config.getExtensions());
        this.algorithm = config.getHashAlgorithm();
        this.verbose = config.isVerbose();
    }

    /**
     * @param args аргументы из main()
     * @throws IllegalArgumentException
     */
    public void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--directory", "-d" -> {
                    directory = getNextValue(args, i, "directory");
                    i++;
                }
                case "--threads", "-t" -> {
                    threads = parseInteger(getNextValue(args, i, "threads"), "threads");
                    if (threads < 1) {
                        throw new IllegalArgumentException("Количество потоков >= 1");
                    }
                    i++;
                }
                case "--min-size", "-s" -> {
                    minSize = parseLong(getNextValue(args, i, "min-size"), "min-size");
                    if (minSize < 0) {
                        throw new IllegalArgumentException("Не может быть отрицательным");
                    }
                    i++;
                }
                case "--extensions", "-e" -> {
                    String extensionsStr = getNextValue(args, i, "extensions");
                    extensions = Arrays.asList(extensionsStr.split(","));

                    extensions = extensions.stream()
                            .map(ext -> ext.startsWith(".") ? ext : "." + ext)
                            .toList();
                    i++;
                }
                case "--algorithm", "-a" -> {
                    algorithm = getNextValue(args, i, "algorithm").toUpperCase();
                    if (!isValidAlgorithm(algorithm)) {
                        throw new IllegalArgumentException(
                                "Неподдерживаемый алгоритм: " + algorithm +
                                        ". Доступны: MD5, SHA-1, SHA-256");
                    }
                    i++;
                }
                case "--verbose", "-v" -> verbose = true;
                case "--help", "-h" -> showHelp = true;
                default -> throw new IllegalArgumentException("Неизвестный параметр: " + arg);
            }
        }
    }

    private String getNextValue(String[] args, int currentIndex, String paramName) {
        if (currentIndex + 1 >= args.length) {
            throw new IllegalArgumentException(
                    "Параметр --" + paramName + " требует значение");
        }
        return args[currentIndex + 1];
    }

    private int parseInteger(String value, String paramName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Параметр --" + paramName + " должен быть целым числом: " + value);
        }
    }

    private long parseLong(String value, String paramName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Параметр --" + paramName + " должен быть числом: " + value);
        }
    }

    private boolean isValidAlgorithm(String algorithm) {
        return algorithm.equals("MD5") ||
                algorithm.equals("SHA-1") ||
                algorithm.equals("SHA-256");
    }

    public void printHelp() {
        System.out.println("""
                --------FILE DUPLICATE FINDER - Поиск дубликатов файлов--------
                
                ИСПОЛЬЗОВАНИЕ:
                  java -jar file-duplicate-finder.jar [ОПЦИИ]
                
                ОПЦИИ:
                  -d, --directory <путь>
                      Директория для сканирования (по умолчанию: текущая папка)
                      Пример: --directory /home/user/documents
                
                  -t, --threads <число>
                      Количество потоков для обработки (по умолчанию: количество ядер CPU)
                      Пример: --threads 8
                
                  -s, --min-size <байты>
                      Минимальный размер файла для обработки (по умолчанию: 0)
                      Пример: --min-size 1048576  (1 MB)
                
                  -e, --extensions <список>
                      Расширения файлов через запятую (по умолчанию: все файлы)
                      Пример: --extensions txt,jpg,png,pdf
                
                  -a, --algorithm <алгоритм>
                      Алгоритм хеширования: MD5, SHA-1, SHA-256 (по умолчанию: MD5)
                      Пример: --algorithm SHA-256
                
                  -v, --verbose
                      Подробный вывод (детальная информация о процессе)
                
                  -h, --help
                      Показать эту справку
                
                ПРИМЕРЫ:
                
                  1. Базовое использование (текущая папка, авто-потоки):
                     java -jar file-duplicate-finder.jar
                
                  2. Сканирование конкретной папки:
                     java -jar file-duplicate-finder.jar --directory /home/user/photos
                
                  3. С настройками производительности:
                     java -jar file-duplicate-finder.jar -d /home/user -t 8 -s 10240
                
                  4. Только изображения:
                     java -jar file-duplicate-finder.jar -d . -e jpg,png,gif,bmp
                
                  5. С SHA-256 для большей надёжности:
                     java -jar file-duplicate-finder.jar -d /tmp -a SHA-256 -v
                
                ИНФОРМАЦИЯ:
                  Проект: File Duplicate Finder
                  Версия: 1.0.0
                  Автор: Barynova
                  GitHub: https://github.com/BarynovaSofia/File-Duplicate-Finder
                
                """);
    }

    public void printConfiguration() {
        System.out.println("\n  КОНФИГУРАЦИЯ  ");
        System.out.println(" Директория:       " + directory);
        System.out.println(" Потоков:          " + threads);
        System.out.println(" Мин. размер:      " + formatSize(minSize));
        System.out.println(" Расширения:       " + (extensions.isEmpty() ? "все файлы" : String.join(", ", extensions)));
        System.out.println(" Алгоритм:         " + algorithm);
        System.out.println(" Подробный вывод:  " + (verbose ? "да" : "нет"));
        System.out.println(" Источник:         " + (hasCliOverrides() ? "CLI параметры + application.properties" : "application.properties"));
        System.out.println("----------------------\n");
    }

    private boolean hasCliOverrides() {
        return !directory.equals(config.getDirectory()) ||
                threads != config.getThreads() ||
                minSize != config.getMinSize() ||
                !extensions.equals(config.getExtensions()) ||
                !algorithm.equals(config.getHashAlgorithm()) ||
                verbose != config.isVerbose();
    }

    private String formatSize(long bytes) {
        if (bytes == 0) return "не ограничен";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public String getDirectory() { return directory; }
    public int getThreads() { return threads; }
    public long getMinSize() { return minSize; }
    public List<String> getExtensions() { return extensions; }
    public String getAlgorithm() { return algorithm; }
    public boolean isShowHelp() { return showHelp; }
    public boolean isVerbose() { return verbose; }
    public AppConfig getAppConfig() { return config; }
}
