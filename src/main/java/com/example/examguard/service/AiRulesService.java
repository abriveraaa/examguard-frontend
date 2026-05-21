package com.example.examguard.service;

import com.example.examguard.model.ai.AiRulesConfig;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class AiRulesService {

    private static AiRulesConfig cachedConfig;

    public static synchronized AiRulesConfig getRules() {
        if (cachedConfig != null) {
            return cachedConfig;
        }

        try {
            Optional<Path> pathOpt = AiAssetSyncService.getOptionalAssetPath("ai-rules");

            if (pathOpt.isEmpty()) {
                cachedConfig = new AiRulesConfig();
                return cachedConfig;
            }

            String json = Files.readString(pathOpt.get(), StandardCharsets.UTF_8);

            AiRulesConfig parsed = new Gson().fromJson(json, AiRulesConfig.class);

            cachedConfig = parsed == null ? new AiRulesConfig() : parsed;

            return cachedConfig;

        } catch (Exception e) {
            e.printStackTrace();
            cachedConfig = new AiRulesConfig();
            return cachedConfig;
        }
    }

    public static synchronized void reload() {
        cachedConfig = null;
    }
}