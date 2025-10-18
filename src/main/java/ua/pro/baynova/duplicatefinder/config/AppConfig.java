package ua.pro.baynova.duplicatefinder.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class AppConfig {
    private static final String CONFIG_FILE = "application.properties";

    private final Properties properties;

    private static final String DEFAULT_THREADS = String.valueOf(Runtime.getRuntime().availableProcessors());
    private static final String DEFAULT_ALGORITHM = "MD5";
    private static final String DEFAULT_MIN_SIZE = "0";
    private static final String DEFAULT_DIRECTORY = ".";
    private static final String DEFAULT_VERBOSE = "false";
    private static final String DEFAULT_EXTENSIONS = "";

    public AppConfig() {
        this.properties = new Properties();
        loadProperties();
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.out.println(" Файл " + CONFIG_FILE + " не найден, используются значения по умолчанию");
                return;
            }

            properties.load(input);
            System.out.println(" Конфигурация загружена из " + CONFIG_FILE);

        } catch (IOException e) {
            System.err.println("!! Ошибка чтения " + CONFIG_FILE + ": " + e.getMessage());
            System.err.println(" Используются значения по умолчанию");
        }
    }

    public int getThreads() {
        String value = properties.getProperty("threads", DEFAULT_THREADS);
        try {
            int threads = Integer.parseInt(value);
            if (threads < 1) {
                System.err.println("!! Некорректное значение threads в конфигурации, используется: " + DEFAULT_THREADS);
                return Integer.parseInt(DEFAULT_THREADS);
            }
            return threads;
        } catch (NumberFormatException e) {
            System.err.println("!! Некорректное значение threads в конфигурации, используется: " + DEFAULT_THREADS);
            return Integer.parseInt(DEFAULT_THREADS);
        }
    }

    public String getHashAlgorithm() {
        String algorithm = properties.getProperty("hash.algorithm", DEFAULT_ALGORITHM).toUpperCase();

        if (!algorithm.equals("MD5") && !algorithm.equals("SHA-1") && !algorithm.equals("SHA-256")) {
            System.err.println(" Некорректный алгоритм в конфигурации: " + algorithm);
            System.err.println(" Используется: " + DEFAULT_ALGORITHM);
            return DEFAULT_ALGORITHM;
        }

        return algorithm;
    }

    public long getMinSize() {
        String value = properties.getProperty("file.min-size", DEFAULT_MIN_SIZE);
        try {
            long size = Long.parseLong(value);
            if (size < 0) {
                System.err.println(" Некорректное значение min-size в конфигурации, используется: 0");
                return 0;
            }
            return size;
        } catch (NumberFormatException e) {
            System.err.println(" Некорректное значение min-size в конфигурации, используется: 0");
            return 0;
        }
    }

    public List<String> getExtensions() {
        String value = properties.getProperty("file.extensions", DEFAULT_EXTENSIONS).trim();

        if (value.isEmpty()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String :: trim)
                .filter(ext -> !ext.isEmpty())
                .map(ext -> ext.startsWith(".") ? ext : "." + ext)
                .toList();
    }

    public String getDirectory() {
        return properties.getProperty("directory", DEFAULT_DIRECTORY);
    }

    public boolean isVerbose() {
        String value = properties.getProperty("verbose", DEFAULT_VERBOSE);
        return Boolean.getBoolean(value);
    }

    public void printConfig() {
        System.out.println("\n КОНФИГУРАЦИЯ ИЗ application.properties:");
        System.out.println("   threads = " + getThreads());
        System.out.println("   hash.algorithm = " + getHashAlgorithm());
        System.out.println("   file.min-size = " + getMinSize() + " байт");
        System.out.println("   file.extensions = " + (getExtensions().isEmpty() ? "все файлы" : String.join(", ", getExtensions())));
        System.out.println("   directory = " + getDirectory());
        System.out.println("   verbose = " + isVerbose());
        System.out.println();
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
