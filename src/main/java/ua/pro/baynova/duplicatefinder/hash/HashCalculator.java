package ua.pro.baynova.duplicatefinder.hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashCalculator {

    private static final String DEFAULT_ALGORITHM = "MD5";
    private static final int BUFFER_SIZE = 8192;

    private final String algorithm;

    public HashCalculator() {
        this(DEFAULT_ALGORITHM);
    }

    /**
     * Создает калькулятор с указанным алгоритмом
     * @param algorithm алгоритм хеширования (MD5, SHA-1, SHA-256)
     */
    public HashCalculator(String algorithm) {
        this.algorithm = algorithm;

        try {
            MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Неподдерживаемый алгоритм: " + algorithm, e);
        }
    }

    /**
     * Вычисляет хеш файла
     * @param file файл для хеширования
     * @return хеш в виде hex-строки
     * @throws IOException если ошибка чтения файла
     */
    public String calculateHash(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Файл не может быть null");
        }

        if (!file.exists()) {
            throw new IOException("Файл не существует: " + file.getAbsolutePath());
        }

        if (!file.isFile()) {
            throw new IOException("Указанный путь не является файлом: " + file.getAbsolutePath());
        }

        if (!file.canRead()) {
            throw new IOException("Нет прав на чтение файла: " + file.getAbsolutePath());
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            try (FileInputStream fis = new FileInputStream(file)) {
                return calculateHash(fis, digest);
            }

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Алгоритм неожиданно стал недоступен: " + algorithm, e);
        }
    }

    /**
     * Вычисляет хеш файла по пути
     * @param filePath путь к файлу
     * @return хеш в виде hex-строки
     */
    public String calculateHash(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Путь к файлу не может быть пустым");
        }

        return calculateHash(new File(filePath));
    }

    private String calculateHash(FileInputStream fis, MessageDigest digest) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        while ((bytesRead = fis.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }

        byte[] hashBytes = digest.digest();

        return bytesToHex(hashBytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public boolean areFilesIdentical(File file1, File file2) throws IOException {
        if (file1 == null || file2 == null) {
            return false;
        }

        if (file1.equals(file2)) {
            return true;
        }

        if (file1.length() != file2.length()) {
            return false;
        }

        String hash1 = calculateHash(file1);
        String hash2 = calculateHash(file2);

        return hash1.equals(hash2);
    }

    public FileHashInfo getFileHashInfo(File file) throws IOException {
        long startTime = System.currentTimeMillis();
        String hash = calculateHash(file);
        long duration = System.currentTimeMillis() - startTime;

        return new FileHashInfo(
                file.getAbsolutePath(),
                file.length(),
                hash,
                duration,
                algorithm
        );
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public static class FileHashInfo {
        private final String filePath;
        private final long fileSize;
        private final String hash;
        private final long processingTimeMs;
        private final String algorithm;

        public FileHashInfo(String filePath, long fileSize, String hash, long processingTimeMs, String algorithm) {
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.hash = hash;
            this.processingTimeMs = processingTimeMs;
            this.algorithm = algorithm;
        }

        public String getFilePath() { return filePath; }
        public long getFileSize() { return fileSize; }
        public String getHash() {return hash; }
        public long getProcessingTimeMs() {return processingTimeMs; }
        public String getAlgorithm() {return algorithm; }

        @Override
        public String toString() {
            return String.format("FileHashInfo{file='%s', size=%d bytes, hash='%s...', time=%dms, algo='%s'}",
                    getFileName(), fileSize, hash.substring(0, Math.min(8, hash.length())),
                    processingTimeMs, algorithm);
        }

        private String getFileName() {
            int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
            return lastSeparator > 0 ? filePath.substring(lastSeparator + 1) : filePath;
        }
    }
}
