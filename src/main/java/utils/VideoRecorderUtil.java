package utils;

import org.monte.media.Format;
import org.monte.media.math.Rational;
import org.monte.screenrecorder.ScreenRecorder;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.monte.media.AudioFormatKeys.*;
import static org.monte.media.VideoFormatKeys.*;

public class VideoRecorderUtil {
    private static SpecializedScreenRecorder screenRecorder;
    private static File lastVideoFile;
    private static boolean isRecordingEnabled = true;

    public static void startRecording(String methodName) throws Exception {
        if (GraphicsEnvironment.isHeadless() || !isRecordingEnabled()) {
            System.out.println("[INFO] Headless environment detected or recording disabled. Skipping video recording.");
            return;
        }
        try {
            File videoDirectory = new File("test-recordings");
            if (!videoDirectory.exists()) {
                videoDirectory.mkdirs(); // Create the directory if it doesn't exist
            }

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle captureSize = new Rectangle(0, 0, screenSize.width, screenSize.height);

            // Sanitize the method name for use as a valid file name
            String sanitizedMethodName = methodName.replaceAll("[^a-zA-Z0-9_]", "_");

            // Initialize the screen recorder with appropriate settings
            screenRecorder = new SpecializedScreenRecorder(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration(),
                    captureSize,
                    new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_AVI),
                    new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                            CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                            DepthKey, 24, FrameRateKey, Rational.valueOf(15),
                            QualityKey, 1.0f,
                            KeyFrameIntervalKey, 15 * 60),
                    new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, "black",
                            FrameRateKey, Rational.valueOf(30)),
                    null, videoDirectory, sanitizedMethodName);
            screenRecorder.start();
        } catch (IOException | AWTException e) {
            System.err.println("[ERROR] Failed to start video recording: " + e.getMessage());
        }
    }

    public static File stopRecording() {
        if (GraphicsEnvironment.isHeadless() || !isRecordingEnabled()) {
            return null;
        }
        try {
            if (screenRecorder != null) {
                screenRecorder.stop();
                lastVideoFile = screenRecorder.getRecordedFile(); // ✅ Lưu lại file
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to stop video recording: " + e.getMessage());
        }
        System.out.println("📹 Video saved: " + lastVideoFile.getAbsolutePath());
        System.out.println("📦 Size: " + lastVideoFile.length() + " bytes");
        return lastVideoFile;
    }

    public static boolean isRecordingEnabled() {
        return isRecordingEnabled && !Optional.ofNullable(System.getenv("CI"))
                .map("true"::equalsIgnoreCase)
                .orElse(false);
    }

}
