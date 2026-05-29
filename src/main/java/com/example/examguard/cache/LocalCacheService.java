package com.example.examguard.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import com.example.examguard.utility.OffsetDateTimeAdapter;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class LocalCacheService {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .setPrettyPrinting()
            .create();

    private final Path cacheDir;

    public LocalCacheService() {
        this.cacheDir = Paths.get(
                System.getProperty("user.home"),
                ".examguard",
                "cache"
        );

        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public <T> void save(String key, String version, T data) {
        try {
            LocalCacheEntry<T> entry = new LocalCacheEntry<>();
            entry.version = version;
            entry.savedAt = OffsetDateTime.now().toString();
            entry.data = data;

            Path file = cacheDir.resolve(key + ".json");

            Path parent = file.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    file,
                    gson.toJson(entry),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> T loadData(String key, Class<T> dataClass) {
        try {
            Path file = cacheDir.resolve(key + ".json");

            if (!Files.exists(file)) {
                return null;
            }

            String json = Files.readString(file);

            LocalCacheEntry<?> entry =
                    gson.fromJson(json, LocalCacheEntry.class);

            String dataJson = gson.toJson(entry.data);

            return gson.fromJson(dataJson, dataClass);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public <T> List<T> loadList(String key, Class<T> itemClass) {
        try {
            Path file = cacheDir.resolve(key + ".json");

            if (!Files.exists(file)) {
                return null;
            }

            String json = Files.readString(file);

            LocalCacheEntry<?> entry =
                    gson.fromJson(json, LocalCacheEntry.class);

            String dataJson = gson.toJson(entry.data);

            Type listType = TypeToken.getParameterized(List.class, itemClass).getType();

            return gson.fromJson(dataJson, listType);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String loadVersion(String key) {
        try {
            Path file = cacheDir.resolve(key + ".json");

            if (!Files.exists(file)) {
                return null;
            }

            String json = Files.readString(file);
            LocalCacheEntry<?> entry =
                    gson.fromJson(json, LocalCacheEntry.class);

            return entry.version;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void delete(String key) {
        try {
            Path file = cacheDir.resolve(key + ".json");

            Files.deleteIfExists(file);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class LocalCacheEntry<T> {
        public String version;
        public String savedAt;
        public T data;
    }
}