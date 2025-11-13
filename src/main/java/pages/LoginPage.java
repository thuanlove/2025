// Refactored LoginPage.java
package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.util.List;

public class LoginPage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private static final Logger logger = LoggerFactory.getLogger(LoginPage.class);

    private final By usernameField = By.id("username");
    private final By passwordField = By.id("password");
    private final By loginButtonField = By.id("Login");
    private final By verifyCodeField = By.id("emc");
    private final By verifySaveField = By.id("save");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    public void login(String username, String password) {
        try {
            logger.info("Attempting login for user: {}", username);
            wait.until(ExpectedConditions.visibilityOfElementLocated(usernameField)).sendKeys(username);
            driver.findElement(passwordField).sendKeys(password);
            driver.findElement(loginButtonField).click();

            handleVerificationCode();
            waitForPostLoginConfirmation();
        } catch (Exception e) {
            logger.error("Login failed: {}", e.getMessage());
        }
    }

    public void loginEToken() {
        try {
            String eToken = "MsKcAEYUyeZWt3fYyUuihoGyr";
            String url = driver.getCurrentUrl();
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + eToken);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                logger.info("API Response: {}", response);
            }

            driver.findElement(loginButtonField).click();
            waitForPostLoginConfirmation();
        } catch (Exception e) {
            logger.error("eToken login failed: ", e);
        }
    }

    private void handleVerificationCode() {
        try {
            List<WebElement> codeInputs = driver.findElements(verifyCodeField);
            if (!codeInputs.isEmpty()) {
                String verifyCode = ConfigReader.getProperty("salesforce.verifyCode");
                WebElement verificationCodeInput = codeInputs.get(0);
                verificationCodeInput.clear();
                verificationCodeInput.sendKeys(verifyCode);
                driver.findElement(verifySaveField).click();
                logger.info("Verification code submitted successfully.");
            } else {
                logger.info("No verification code required.");
            }
        } catch (Exception e) {
            logger.warn("Verification code step failed: {}", e.getMessage());
        }
    }

    private void waitForPostLoginConfirmation() {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//p[contains(text(), 'Sandbox')]")),
                    ExpectedConditions.titleContains("Home")
            ));
            logger.info("Login successful. Current title: {}", driver.getTitle());
        } catch (Exception e) {
            logger.error("Login might have failed. Current URL: {}", driver.getCurrentUrl());
        }
    }
}
