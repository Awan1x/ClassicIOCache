package test;

import src.ClassicIOCacheWithLimit;

import java.io.*;
import java.util.*;

public class TestCache {

    public static void main(String[] args) {
        try {
            // 1. Создаем объект кэша с лимитом 3 файла
            ClassicIOCacheWithLimit cache = new ClassicIOCacheWithLimit(3);

            // 2. Создаем тестовые файлы
            String[] filenames = {"file1.txt", "file2.txt", "file3.txt", "file4.txt"};
            createTestFiles(filenames);

            // 3. Первое чтение - с диска
            System.out.println("=== Первое чтение с диска ===");
            for (String f : filenames) {
                long start = System.nanoTime();
                String content = cache.readFile("test/" + f);
                long end = System.nanoTime();
                System.out.println(f + " прочитан, время: " + (end - start)/1_000_000 + " мс");
            }

            cache.printCacheStats();

            // 4. Повторное чтение - из кэша (если файл не изменялся)
            System.out.println("\n=== Повторное чтение (из кэша) ===");
            for (String f : filenames) {
                long start = System.nanoTime();
                String content = cache.readFile("test/" + f);
                long end = System.nanoTime();
                System.out.println(f + " прочитан, время: " + (end - start)/1_000_000 + " мс");
            }

            cache.printCacheStats();

            // 5. Проверка вытеснения старых файлов
            System.out.println("\n=== Проверка вытеснения при превышении лимита ===");
            System.out.println("Кэш содержит file1.txt? " + cache.isCached("test/file1.txt"));
            System.out.println("Кэш содержит file4.txt? " + cache.isCached("test/file4.txt"));

            // 6. Очистка кэша
            cache.invalidateAll();
            System.out.println("\nПосле очистки:");
            cache.printCacheStats();

            // 7. Удаляем тестовые файлы
            deleteTestFiles(filenames);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для создания тестовых файлов
    private static void createTestFiles(String[] filenames) throws IOException {
        File testDir = new File("test");
        if (!testDir.exists()) testDir.mkdir();

        for (String f : filenames) {
            File file = new File(testDir, f);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("Тестовое содержимое для " + f + "\n".repeat(10));
            }
        }
    }

    // Метод для удаления тестовых файлов
    private static void deleteTestFiles(String[] filenames) {
        for (String f : filenames) {
            File file = new File("test/" + f);
            if (file.exists()) file.delete();
        }
        File testDir = new File("test");
        if (testDir.exists() && testDir.isDirectory()) testDir.delete();
    }
}
