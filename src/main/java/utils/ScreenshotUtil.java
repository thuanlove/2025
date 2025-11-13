package utils;

import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.apache.commons.io.FileUtils;
import java.io.*;

public class ScreenshotUtil {
    public static void captureScreenshot(WebDriver driver, String folderPath, String fileName) {
        try {
            File folder = new File(folderPath);
            if (!folder.exists()) folder.mkdirs();

            File destFile = new File(folderPath + "/" + fileName + ".png");
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(srcFile, destFile);

            System.out.println("✅ Screenshot saved: " + destFile.getAbsolutePath());

            ScreenshotTracker.addScreenshot(destFile); // ✅ lưu lại để sau attach vào bug

            try (InputStream is = new FileInputStream(destFile)) {
                Allure.addAttachment("📸 " + fileName, "image/png", is, ".png");
            }

        } catch (IOException e) {
            System.out.println("❌ Failed to save screenshot: " + fileName);
            e.printStackTrace();
        }
    }
}

