package com.example.examguard.utility;

import com.example.examguard.model.ai.MediaPipeFaceResult;

public class MediaPipeOverlayStore {

    private static volatile MediaPipeFaceResult latestResult;

    public static void setLatestResult(MediaPipeFaceResult result) {
        latestResult = result;
    }

    public static MediaPipeFaceResult getLatestResult() {
        return latestResult;
    }

    public static void clear() {
        latestResult = null;
    }
}