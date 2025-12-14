package src;

import java.io.*;
import java.util.*;

public class ClassicIOCacheWithLimit {

    // Кэш: ключ — абсолютный путь к файлу, значение — объект с данными файла
    private Map<String, FileCacheEntry> cache;

    // Максимальное количество файлов в кэше
    private int maxSize;

    // Внутренний класс для хранения данных файла
    private static class FileCacheEntry {
        String content;              // содержимое файла
        long lastReadTime;           // время последнего чтения
        long lastModifiedTimeAtRead; // время последней модификации файла при чтении

        public FileCacheEntry(String content, long lastReadTime, long lastModifiedTimeAtRead) {
            this.content = content;
            this.lastReadTime = lastReadTime;
            this.lastModifiedTimeAtRead = lastModifiedTimeAtRead;
        }
    }

    // Конструктор с ограничением размера
    public ClassicIOCacheWithLimit(int maxSize) {
        this.cache = new LinkedHashMap<>();
        this.maxSize = maxSize;
    }

    // Конструктор по умолчанию (лимит 100 файлов)
    public ClassicIOCacheWithLimit() {
        this(100);
    }

    // Основной метод чтения файла
    public String readFile(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new FileNotFoundException("Файл не найден: " + filePath);
        }

        String absolutePath = file.getAbsolutePath();
        long currentModifiedTime = file.lastModified();

        // Проверяем есть ли файл в кэше и актуален ли кэш
        if (cache.containsKey(absolutePath)) {
            FileCacheEntry entry = cache.get(absolutePath);
            if (isCacheValid(entry, currentModifiedTime)) {
                entry.lastReadTime = System.currentTimeMillis(); // обновляем время последнего чтения
                return entry.content;
            }
        }

        // Файл не в кэше или устарел -> читаем с диска
        return updateCache(file, absolutePath, currentModifiedTime);
    }

    // Проверка актуальности кэша
    private boolean isCacheValid(FileCacheEntry cachedEntry, long currentModifiedTime) {
        return cachedEntry.lastModifiedTimeAtRead == currentModifiedTime;
    }

    // Обновление кэша
    private String updateCache(File file, String absolutePath, long currentModifiedTime) throws IOException {
        String content = readFileContent(file);

        // Если кэш достиг максимального размера -> удаляем самый старый
        if (cache.size() >= maxSize) {
            removeOldestEntry();
        }

        // Создаем новую запись в кэше
        FileCacheEntry entry = new FileCacheEntry(content, System.currentTimeMillis(), currentModifiedTime);
        cache.put(absolutePath, entry);

        return content;
    }

    // Удаление самого старого файла из кэша
    private void removeOldestEntry() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<String, FileCacheEntry> e : cache.entrySet()) {
            if (e.getValue().lastReadTime < oldestTime) {
                oldestTime = e.getValue().lastReadTime;
                oldestKey = e.getKey();
            }
        }
        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }

    // Чтение содержимого файла с диска
    private String readFileContent(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file), 8192)) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    // Методы управления кэшем
    public void invalidate(String filePath) {
        File file = new File(filePath);
        cache.remove(file.getAbsolutePath());
    }

    public void invalidateAll() {
        cache.clear();
    }

    public boolean isCached(String filePath) {
        File file = new File(filePath);
        return cache.containsKey(file.getAbsolutePath());
    }

    public int getCachedFilesCount() {
        return cache.size();
    }

    // Методы статистики
    public long getCacheSizeInMemory() {
        long size = 0;
        for (FileCacheEntry entry : cache.values()) {
            size += entry.content.length() * 2; // 2 байта на символ
        }
        return size;
    }

    public void printCacheStats() {
        System.out.println("Количество файлов в кэше: " + getCachedFilesCount());
        System.out.println("Размер в памяти (примерно, байт): " + getCacheSizeInMemory());
        System.out.println("Максимальный размер кэша: " + maxSize);
        System.out.println("Файлы в кэше:");
        for (Map.Entry<String, FileCacheEntry> e : cache.entrySet()) {
            System.out.println("Файл: " + e.getKey() + ", размер: " + e.getValue().content.length() * 2
                    + " байт, последнее чтение: " + new Date(e.getValue().lastReadTime));
        }
    }
}
