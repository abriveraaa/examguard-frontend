package com.example.examguard.ai;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MediaPipeFaceRuntime {

    private static Process process;

    private MediaPipeFaceRuntime() {}

    public static synchronized void startIfNeeded() {
        if (isRunning()) {
            return;
        }

        try {
            File runtimeDir = new File("ai-runtime/mediapipe-face");

            String pythonPath = new File(
                    runtimeDir,
                    ".venv/bin/python"
            ).getAbsolutePath();

            File script = new File(
                    runtimeDir,
                    "mediapipe_face_service.py"
            );

            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath,
                    script.getAbsolutePath()
            );

            pb.directory(runtimeDir);
            pb.redirectErrorStream(true);

            process = pb.start();

            Runtime.getRuntime().addShutdownHook(
                    new Thread(MediaPipeFaceRuntime::stop)
            );

            waitUntilReady();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Failed to start MediaPipe face service.",
                    e
            );
        }
    }

    public static synchronized void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();
            process = null;
        }
    }

    private static boolean isRunning() {
        try {
            URL url = new URL("http://127.0.0.1:5005/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(500);
            conn.setReadTimeout(500);
            conn.setRequestMethod("GET");

            return conn.getResponseCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }

    private static void waitUntilReady() throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            if (isRunning()) {
                return;
            }

            Thread.sleep(500);
        }

        throw new RuntimeException(
                "MediaPipe face service did not start on port 5005."
        );
    }
}