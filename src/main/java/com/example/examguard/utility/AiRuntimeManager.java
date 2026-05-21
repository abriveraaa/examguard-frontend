package com.example.examguard.utility;

import com.example.examguard.model.ai.AiRulesConfig;
import com.example.examguard.service.AiRulesService;

import java.net.HttpURLConnection;
import java.net.URL;

public class AiRuntimeManager {

    public boolean isMediaPipeFaceRuntimeReady() {
        try {
            AiRulesConfig.MediaPipeFace config =
                    AiRulesService.getRules().getMediapipeFace();

            if (!config.isEnabled()) {
                return false;
            }

            URL url = new URL(
                    "http://" + config.getServiceHost()
                            + ":" + config.getServicePort()
                            + config.getHealthEndpoint()
            );

            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(config.getTimeoutMs());
            conn.setReadTimeout(config.getTimeoutMs());

            int status = conn.getResponseCode();

            return status >= 200 && status < 300;

        } catch (Exception e) {
            return false;
        }
    }
}