package utils;

import org.testng.ITestResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScreenshotTracker {
    private static final ThreadLocal<List<File>> screenshotFiles = ThreadLocal.withInitial(ArrayList::new);

    public static void addScreenshot(File file) {
        screenshotFiles.get().add(file);
    }

    public static List<File> getScreenshots() {
        return screenshotFiles.get();
    }

    public static void clear() {
        screenshotFiles.remove();
    }
}

