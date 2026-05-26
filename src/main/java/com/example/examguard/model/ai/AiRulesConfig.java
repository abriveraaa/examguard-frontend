package com.example.examguard.model.ai;

public class AiRulesConfig {

    private boolean enabled = true;
    private CameraQuality cameraQuality = new CameraQuality();
    private OpenCvFace opencvFace = new OpenCvFace();
    private MediaPipeFace mediaPipeFace = new MediaPipeFace();
    private Yolo yolo = new Yolo();
    private LobbySetup lobbySetup = new LobbySetup();
    private ObjectDetection objectDetection = new ObjectDetection();
    private RfDetr rfdetr = new RfDetr();

    public boolean isEnabled() {
        return enabled;
    }

    public CameraQuality getCameraQuality() {
        return cameraQuality == null ? new CameraQuality() : cameraQuality;
    }

    public OpenCvFace getOpencvFace() {
        return opencvFace == null ? new OpenCvFace() : opencvFace;
    }

    public MediaPipeFace getMediapipeFace() {
        return mediaPipeFace == null ? new MediaPipeFace() : mediaPipeFace;
    }

    public Yolo getYolo() {
        return yolo == null ? new Yolo() : yolo;
    }

    public LobbySetup getLobbySetup() {
        return lobbySetup == null ? new LobbySetup() : lobbySetup;
    }

    public static class CameraQuality {
        private boolean enabled = true;
        private double minBrightness = 35;
        private double maxBrightness = 230;
        private double failBlurVariance = 7;
        private double warningBlurVariance = 12;
        private int sampleCount = 8;
        private int minimumValidFrames = 4;

        public boolean isEnabled() {
            return enabled;
        }

        public double getMinBrightness() {
            return minBrightness;
        }

        public double getMaxBrightness() {
            return maxBrightness;
        }

        public double getFailBlurVariance() {
            return failBlurVariance;
        }

        public double getWarningBlurVariance() {
            return warningBlurVariance;
        }

        public int getSampleCount() {
            return sampleCount;
        }

        public int getMinimumValidFrames() {
            return minimumValidFrames;
        }
    }

    public static class OpenCvFace {
        private boolean enabled = true;
        private int requiredOneFaceFrames = 4;
        private int multipleFaceFramesToFail = 2;
        private double minFaceAreaRatio = 0.015;
        private double maxFaceAreaRatio = 0.45;
        private double minCenterXRatio = 0.18;
        private double maxCenterXRatio = 0.82;
        private double minCenterYRatio = 0.12;
        private double maxCenterYRatio = 0.72;

        public boolean isEnabled() {
            return enabled;
        }

        public int getRequiredOneFaceFrames() {
            return requiredOneFaceFrames;
        }

        public int getMultipleFaceFramesToFail() {
            return multipleFaceFramesToFail;
        }

        public double getMinFaceAreaRatio() {
            return minFaceAreaRatio;
        }

        public double getMaxFaceAreaRatio() {
            return maxFaceAreaRatio;
        }

        public double getMinCenterXRatio() {
            return minCenterXRatio;
        }

        public double getMaxCenterXRatio() {
            return maxCenterXRatio;
        }

        public double getMinCenterYRatio() {
            return minCenterYRatio;
        }

        public double getMaxCenterYRatio() {
            return maxCenterYRatio;
        }
    }

    public static class MediaPipeFace {
        private boolean enabled = true;
        private boolean required = true;
        private String serviceHost = "127.0.0.1";
        private int servicePort = 5005;
        private String requiredVersion = "1.0.0";
        private String healthEndpoint = "/health";
        private String analyzeEndpoint = "/analyze";
        private int timeoutMs = 3000;
        private int sampleCount = 5;
        private int requiredValidFaceFrames = 3;
        private int maxFaces = 1;
        private double yawThreshold = 0.08;
        private double pitchDownThreshold = 0.12;
        private String startupCommandKey = "mediapipe-face-start-command";

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isRequired() {
            return required;
        }

        public String getServiceHost() {
            return serviceHost;
        }

        public int getServicePort() {
            return servicePort;
        }

        public String getRequiredVersion() {
            return requiredVersion;
        }

        public String getHealthEndpoint() {
            return healthEndpoint;
        }

        public String getAnalyzeEndpoint() {
            return analyzeEndpoint;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public int getSampleCount() {
            return sampleCount;
        }

        public int getRequiredValidFaceFrames() {
            return requiredValidFaceFrames;
        }

        public int getMaxFaces() {
            return maxFaces;
        }

        public double getYawThreshold() {
            return yawThreshold;
        }

        public double getPitchDownThreshold() {
            return pitchDownThreshold;
        }

        public String getStartupCommandKey() {
            return startupCommandKey;
        }
    }

    public static class Yolo {
        private boolean enabled = true;
        private String modelKey = "object-detector";
        private String labelsKey = "object-labels";
        private int inputSize = 640;
        private float confidenceThreshold = 0.25f;
        private float nmsThreshold = 0.45f;

        private float phoneConfidence = 0.78f;
        private int requiredPhoneHits = 4;

        private float personConfidence = 0.45f;
        private int requiredPersonHits = 3;
        private int sideViewSampleCount = 5;

        private double maxPhoneAreaRatio = 0.18;

        private boolean detectBook = false;
        private boolean detectPaper = false;

        public boolean isEnabled() {
            return enabled;
        }

        public String getModelKey() {
            return modelKey;
        }

        public String getLabelsKey() {
            return labelsKey;
        }

        public int getInputSize() {
            return inputSize;
        }

        public float getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public float getNmsThreshold() {
            return nmsThreshold;
        }

        public float getPhoneConfidence() {
            return phoneConfidence;
        }

        public int getRequiredPhoneHits() {
            return requiredPhoneHits;
        }

        public float getPersonConfidence() {
            return personConfidence;
        }

        public int getRequiredPersonHits() {
            return requiredPersonHits;
        }

        public int getSideViewSampleCount() {
            return sideViewSampleCount;
        }

        public double getMaxPhoneAreaRatio() {
            return maxPhoneAreaRatio;
        }

        public boolean isDetectBook() {
            return detectBook;
        }

        public boolean isDetectPaper() {
            return detectPaper;
        }
    }

    public static class LobbySetup {
        private boolean enabled = true;
        private boolean requireSinglePerson = true;
        private boolean requireFaceVisible = true;
        private boolean requireCameraNotBlurred = true;
        private boolean requireGoodLighting = true;
        private boolean requireNoPhoneDetected = true;
        private boolean requireNoReviewerDetected = false;

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isRequireSinglePerson() {
            return requireSinglePerson;
        }

        public boolean isRequireFaceVisible() {
            return requireFaceVisible;
        }

        public boolean isRequireCameraNotBlurred() {
            return requireCameraNotBlurred;
        }

        public boolean isRequireGoodLighting() {
            return requireGoodLighting;
        }

        public boolean isRequireNoPhoneDetected() {
            return requireNoPhoneDetected;
        }

        public boolean isRequireNoReviewerDetected() {
            return requireNoReviewerDetected;
        }
    }

    public ObjectDetection getObjectDetection() {
        return objectDetection == null
                ? new ObjectDetection()
                : objectDetection;
    }

    public RfDetr getRfdetr() {
        return rfdetr == null
                ? new RfDetr()
                : rfdetr;
    }

    public static class ObjectDetection {

        private String provider = "YOLO";

        public String getProvider() {
            return provider == null || provider.isBlank()
                    ? "YOLO"
                    : provider;
        }
    }

    public static class RfDetr {

        private boolean enabled = true;

        private String modelKey = "rfdetr-object-detector";
        private String labelsKey = "rfdetr-object-labels";

        private int inputSize = 640;

        private float confidenceThreshold = 0.45f;
        private float nmsThreshold = 0.50f;

        private float personConfidence = 0.55f;
        private float phoneConfidence = 0.55f;
        private float tabletConfidence = 0.55f;
        private float paperConfidence = 0.50f;
        private float bookConfidence = 0.50f;
        private float notesConfidence = 0.50f;
        private float laptopConfidence = 0.60f;
        private float monitorConfidence = 0.60f;
        private float keyboardConfidence = 0.45f;
        private float mouseConfidence = 0.45f;
        private float calculatorConfidence = 0.50f;

        private int sampleCount = 5;

        private int requiredPersonHits = 3;
        private int requiredSuspiciousHits = 2;

        private int maxPersons = 1;
        private int maxLaptops = 1;
        private int maxMonitors = 1;

        private boolean hardFailPhone = true;
        private boolean hardFailTablet = true;
        private boolean hardFailPaper = true;
        private boolean hardFailBook = true;
        private boolean hardFailNotes = true;
        private boolean hardFailCalculator = true;
        private boolean hardFailExtraLaptop = true;
        private boolean hardFailExtraMonitor = true;

        public boolean isEnabled() {
            return enabled;
        }

        public String getModelKey() {
            return modelKey;
        }

        public String getLabelsKey() {
            return labelsKey;
        }

        public int getInputSize() {
            return inputSize;
        }

        public float getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public float getNmsThreshold() {
            return nmsThreshold;
        }

        public float getPersonConfidence() {
            return personConfidence;
        }

        public float getPhoneConfidence() {
            return phoneConfidence;
        }

        public float getTabletConfidence() {
            return tabletConfidence;
        }

        public float getPaperConfidence() {
            return paperConfidence;
        }

        public float getBookConfidence() {
            return bookConfidence;
        }

        public float getNotesConfidence() {
            return notesConfidence;
        }

        public float getLaptopConfidence() {
            return laptopConfidence;
        }

        public float getMonitorConfidence() {
            return monitorConfidence;
        }

        public float getKeyboardConfidence() {
            return keyboardConfidence;
        }

        public float getMouseConfidence() {
            return mouseConfidence;
        }

        public float getCalculatorConfidence() {
            return calculatorConfidence;
        }

        public int getSampleCount() {
            return sampleCount;
        }

        public int getRequiredPersonHits() {
            return requiredPersonHits;
        }

        public int getRequiredSuspiciousHits() {
            return requiredSuspiciousHits;
        }

        public int getMaxPersons() {
            return maxPersons;
        }

        public int getMaxLaptops() {
            return maxLaptops;
        }

        public int getMaxMonitors() {
            return maxMonitors;
        }

        public boolean isHardFailPhone() {
            return hardFailPhone;
        }

        public boolean isHardFailTablet() {
            return hardFailTablet;
        }

        public boolean isHardFailPaper() {
            return hardFailPaper;
        }

        public boolean isHardFailBook() {
            return hardFailBook;
        }

        public boolean isHardFailNotes() {
            return hardFailNotes;
        }

        public boolean isHardFailCalculator() {
            return hardFailCalculator;
        }

        public boolean isHardFailExtraLaptop() {
            return hardFailExtraLaptop;
        }

        public boolean isHardFailExtraMonitor() {
            return hardFailExtraMonitor;
        }
    }
}