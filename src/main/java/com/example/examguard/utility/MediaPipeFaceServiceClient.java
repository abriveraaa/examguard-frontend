package com.example.examguard.utility;

import com.example.examguard.model.ai.AiRulesConfig;
import com.example.examguard.model.ai.MediaPipeFaceResult;
import com.example.examguard.service.AiRulesService;
import com.google.gson.Gson;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MediaPipeFaceServiceClient {

    private final Gson gson = new Gson();

    public LobbyCheckResult checkFaceSetup(LobbyCameraPreviewService previewService) {
        try {
            AiRulesConfig.MediaPipeFace config =
                    AiRulesService.getRules().getMediapipeFace();

            if (!config.isEnabled()) {
                return LobbyCheckResult.pass("MediaPipe face detection is disabled.");
            }

            Mat frame = previewService.getLatestFrame();

            if (frame == null || frame.empty()) {
                return LobbyCheckResult.fail("No camera frame available.");
            }

            File temp = File.createTempFile("examguard-mediapipe-frame", ".jpg");
            Imgcodecs.imwrite(temp.getAbsolutePath(), frame);

            String endpoint =
                    "http://" + config.getServiceHost() + ":" + config.getServicePort()
                            + config.getAnalyzeEndpoint();

            HttpURLConnection conn =
                    (HttpURLConnection) new URL(endpoint).openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(config.getTimeoutMs());
            conn.setReadTimeout(config.getTimeoutMs());

            String boundary = "----ExamGuardMediaPipeBoundary";

            conn.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=" + boundary
            );

            try (
                    OutputStream output = conn.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output), true);
                    FileInputStream fis = new FileInputStream(temp)
            ) {
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"frame.jpg\"\r\n");
                writer.append("Content-Type: image/jpeg\r\n\r\n");
                writer.flush();

                fis.transferTo(output);
                output.flush();

                writer.append("\r\n").flush();
                writer.append("--").append(boundary).append("--\r\n");
                writer.flush();
            }

            int status = conn.getResponseCode();

            InputStream stream =
                    status >= 200 && status < 300
                            ? conn.getInputStream()
                            : conn.getErrorStream();

            String json = new String(stream.readAllBytes());

            System.out.println("MEDIAPIPE RESPONSE: " + json);

            temp.delete();

            if (status < 200 || status >= 300) {
                return LobbyCheckResult.fail("MediaPipe service error: HTTP " + status);
            }

            MediaPipeFaceResult result =
                    gson.fromJson(json, MediaPipeFaceResult.class);

            MediaPipeOverlayStore.setLatestResult(result);

            if (result == null || !result.isAvailable()) {
                return LobbyCheckResult.fail("MediaPipe face analysis unavailable.");
            }

            if (!result.isFacePresent()) {
                return LobbyCheckResult.fail("No examinee detected. Please face the camera.");
            }

            if (result.getFaceCount() > config.getMaxFaces()) {
                return LobbyCheckResult.fail("Multiple faces detected. Only one examinee is allowed.");
            }

            if (Math.abs(result.getYaw()) > config.getYawThreshold()) {
                return LobbyCheckResult.fail("Please face the camera. Looking away was detected.");
            }

            if (result.getPitch() > config.getPitchDownThreshold()) {
                return LobbyCheckResult.fail("Please keep your face and eyes visible. Looking down was detected.");
            }

            return LobbyCheckResult.pass(
                    "Face setup passed using MediaPipe. Yaw: "
                            + String.format("%.3f", result.getYaw())
                            + ", Pitch: "
                            + String.format("%.3f", result.getPitch())
            );

        } catch (Exception e) {
            e.printStackTrace();
            return LobbyCheckResult.fail("MediaPipe face service unavailable.");
        }
    }

    public MediaPipeFaceResult analyzeOnly(LobbyCameraPreviewService previewService) {
        try {
            AiRulesConfig.MediaPipeFace config =
                    AiRulesService.getRules().getMediapipeFace();

            Mat frame = previewService.getLatestFrame();

            if (frame == null || frame.empty()) {
                return null;
            }

            File temp = File.createTempFile("examguard-mediapipe-preview", ".jpg");
            Imgcodecs.imwrite(temp.getAbsolutePath(), frame);

            String endpoint =
                    "http://" + config.getServiceHost() + ":" + config.getServicePort()
                            + config.getAnalyzeEndpoint();

            HttpURLConnection conn =
                    (HttpURLConnection) new URL(endpoint).openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(config.getTimeoutMs());
            conn.setReadTimeout(config.getTimeoutMs());

            String boundary = "----ExamGuardMediaPipeBoundary";

            conn.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=" + boundary
            );

            try (
                    OutputStream output = conn.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output), true);
                    FileInputStream fis = new FileInputStream(temp)
            ) {
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"frame.jpg\"\r\n");
                writer.append("Content-Type: image/jpeg\r\n\r\n");
                writer.flush();

                fis.transferTo(output);
                output.flush();

                writer.append("\r\n").flush();
                writer.append("--").append(boundary).append("--\r\n");
                writer.flush();
            }

            int status = conn.getResponseCode();

            InputStream stream =
                    status >= 200 && status < 300
                            ? conn.getInputStream()
                            : conn.getErrorStream();

            String json = new String(stream.readAllBytes());

            temp.delete();

            if (status < 200 || status >= 300) {
                return null;
            }

            MediaPipeFaceResult result =
                    gson.fromJson(json, MediaPipeFaceResult.class);

            MediaPipeOverlayStore.setLatestResult(result);

            return result;

        } catch (Exception e) {
            return null;
        }
    }
}