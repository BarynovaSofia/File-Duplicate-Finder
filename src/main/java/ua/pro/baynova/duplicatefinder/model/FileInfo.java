package ua.pro.baynova.duplicatefinder.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class FileInfo {
    private final String path;
    private final String hash;
    private final long size;
    private final LocalDateTime lastModified;
    private final LocalDateTime indexedAt;

    public FileInfo(String path, String hash, long size, LocalDateTime lastModified, LocalDateTime indexedAt){
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Путь к файлу не может быть пустым");
        }
        if (hash == null || hash.trim().isEmpty()) {
            throw new IllegalArgumentException("Хеш файла не может быть пустым");
        }
        if (size < 0) {
            throw new IllegalArgumentException("Размер файла не может быть отрицательным");
        }

        this.path = path;
        this.hash = hash;
        this.size = size;
        this.lastModified = lastModified;
        this.indexedAt = indexedAt;
    }

    public String getPath() {
        return path;
    }

    public String getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public LocalDateTime getIndexedAt() {
        return indexedAt;
    }

    /**
     * Проверяет нужно ли переиндексировать файл
     * @param currentLastModified текущее время изменения файла
     * @return true если файл был изменен и нужно пересчитать хеш
     */
    public boolean needsReindexing(LocalDateTime currentLastModified){
        return currentLastModified.isAfter(this.lastModified);
    }

    /**
     * Получить имя файла без пути
     */
    public String getFileName(){
        int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSeparator > 0 ? path.substring(lastSeparator + 1) : path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return size == fileInfo.size &&
                Objects.equals(path, fileInfo.path) &&
                Objects.equals(hash, fileInfo.hash) &&
                Objects.equals(lastModified, fileInfo.lastModified);
    }

    @Override
    public int hashCode(){
        return Objects.hash(path, hash, size, lastModified);
    }

    @Override
    public String toString(){
        return String.format("FileInfo{fileName='%s', size=%d bytes, hash='%s...'}",
                getFileName(), size, hash.substring(0, Math.min(8, hash.length())));
    }

    public String getDetailedInfo(){
        return String.format("""
                FileInfo:
                  Path: %s
                  Size: %d bytes
                  Hash: %s
                  Last modified: %s
                  Indexed at: %s
                """, path, size, hash, lastModified, indexedAt);
    }
}
