package com.example.examguard.utility;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import java.util.List;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class LobbyCameraPreviewService {

    private static boolean openCvLoaded = false;

    private volatile boolean running = false;
    private Thread previewThread;
    private VideoCapture camera;

    private final ImageView imageView;
    private final Label placeholderLabel;
    private final LobbyObjectDetector objectDetector;

    public LobbyCameraPreviewService(
            ImageView imageView,
            Label placeholderLabel, LobbyObjectDetector objectDetector
    ) {
        this.imageView = imageView;
        this.placeholderLabel = placeholderLabel;
        this.objectDetector = objectDetector;
        loadOpenCv();
    }

    private static synchronized void loadOpenCv() {
        if (!openCvLoaded) {
            OpenCV.loadLocally();
            openCvLoaded = true;
        }
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;

        previewThread = new Thread(() -> {
            try {
                camera = new VideoCapture(0);

                camera.set(3, 1280);
                camera.set(4, 720);

                if (!camera.isOpened()) {
                    Platform.runLater(() -> {
                        if (placeholderLabel != null) {
                            placeholderLabel.setText("Camera unavailable or permission denied.");
                            placeholderLabel.setVisible(true);
                            placeholderLabel.setManaged(true);
                        }
                    });
                    running = false;
                    return;
                }

                Platform.runLater(() -> {
                    if (placeholderLabel != null) {
                        placeholderLabel.setVisible(false);
                        placeholderLabel.setManaged(false);
                    }
                });

                Mat frame = new Mat();

                while (running) {
                    boolean captured = camera.read(frame);

                    if (captured && !frame.empty()) {

                        List<DetectedObject> objects =
                                objectDetector.detect(frame);

                        for (DetectedObject object : objects) {

                            String name = object.getClassName();

                            if (!name.equals("cell phone")
                                    && !name.equals("book")) {
                                continue;
                            }

                            Point topLeft = new Point(
                                    object.getX(),
                                    object.getY()
                            );

                            Point bottomRight = new Point(
                                    object.getX() + object.getWidth(),
                                    object.getY() + object.getHeight()
                            );

                            Imgproc.rectangle(
                                    frame,
                                    topLeft,
                                    bottomRight,
                                    new Scalar(0, 0, 255),
                                    2
                            );

                            String label =
                                    name + " "
                                            + Math.round(object.getConfidence() * 100)
                                            + "%";

                            Imgproc.putText(
                                    frame,
                                    label,
                                    new Point(
                                            object.getX(),
                                            Math.max(20, object.getY() - 8)
                                    ),
                                    Imgproc.FONT_HERSHEY_SIMPLEX,
                                    0.6,
                                    new Scalar(0, 0, 255),
                                    2
                            );
                        }

                        BufferedImage bufferedImage =
                                matToBufferedImage(frame);

                        Platform.runLater(() -> {
                            if (imageView != null) {
                                imageView.setImage(
                                        SwingFXUtils.toFXImage(bufferedImage, null)
                                );
                            }
                        });
                    }

                    sleepQuietly(33);
                }

            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    if (placeholderLabel != null) {
                        placeholderLabel.setText("Camera preview failed.");
                        placeholderLabel.setVisible(true);
                        placeholderLabel.setManaged(true);
                    }
                });

            } finally {
                releaseCamera();
            }
        }, "lobby-camera-preview-thread");

        previewThread.setDaemon(true);
        previewThread.start();
    }

    public void stop() {
        running = false;

        if (previewThread != null) {
            try {
                previewThread.join(700);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            previewThread = null;
        }

        Platform.runLater(() -> {
            if (imageView != null) {
                imageView.setImage(null);
            }

            if (placeholderLabel != null) {
                placeholderLabel.setText("Camera preview will appear here");
                placeholderLabel.setVisible(true);
                placeholderLabel.setManaged(true);
            }
        });
    }

    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_3BYTE_BGR;

        BufferedImage image = new BufferedImage(
                mat.width(),
                mat.height(),
                type
        );

        byte[] data =
                ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        mat.get(0, 0, data);

        return image;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}