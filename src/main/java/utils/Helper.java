package utils;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;

//Helper-BasePage-xPage-xTest: tách biệt thao tác UI khỏi Page/Flow/Test.
public class Helper {
    private WebDriver driver;

    public Helper(WebDriver driver) {
        this.driver = driver;
    }

    public void waitForElement(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void scrollAndWait(By locator, int seconds) {
        WebElement element = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    //Cuộn trang xuống đảm bảo phần tử có thể nhìn thấy
    public void scrollPageDown() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            By scrollableContainer = By.className("viewport");
            WebElement scrollableDiv = driver.findElement(scrollableContainer);

            long scrollHeight = (long) js.executeScript("return arguments[0].scrollHeight", scrollableDiv);
            long heightCounter = 0;
            while (heightCounter <= scrollHeight) {
                js.executeScript("window.scrollBy(0, 300);");
                Thread.sleep(500);
                scrollHeight = (long) js.executeScript("return arguments[0].scrollHeight", scrollableDiv);
                heightCounter += 300;
            }

        } catch (Exception e) {
            System.out.println("⚠️ Skipping label due to error: " + e.getMessage());
        }
    }

    public String getFieldErrorMessage(String partialFieldLabel) {
        try {
            By errorLocator = By.xpath("//div[contains(@class, 'slds-form-element__help') and contains(., '" + partialFieldLabel + "')]");
            WebElement errorElement = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOfElementLocated(errorLocator));
            //String actualError = errorElement.getText();
            //System.out.println("❗ Validation Message for '" + partialFieldLabel + "': " + actualError);
            return errorElement.getText();
        } catch (TimeoutException e) {
            //System.out.println("✅ No validation message found for: " + partialFieldLabel);
            return "";
        }
    }

    //Scroll dành cho DataType page
    public void scrollToLoadAllFields() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebElement scrollContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.scroller.uiScroller.scroller-wrapper")
        ));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        long lastHeight = (long) js.executeScript("return arguments[0].scrollHeight", scrollContainer);

        while (true) {
            js.executeScript("arguments[0].scrollTop = arguments[0].scrollHeight", scrollContainer);

            try {
                Thread.sleep(1000); // chờ load thêm dữ liệu
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            long newHeight = (long) js.executeScript("return arguments[0].scrollHeight", scrollContainer);

            if (newHeight == lastHeight) {
                break; // không load thêm được nữa
            }
            lastHeight = newHeight;
        }
    }

    //Camdtt 2606 update (By.xpath("//iframe[contains(@title, 'Lead Field History')]"));
    public void scrollHistoryTrackingPage() {
        // Locate iframe using 'contains' in title
        WebElement iframe = driver.findElement(By.xpath("//iframe[contains(@title, 'Field History')]"));
        driver.switchTo().frame(iframe);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            while (true) {
                // Scroll down by 300px
                js.executeScript("window.scrollBy(0, 50);");

                // Wait 500 milliseconds
                Thread.sleep(500);

                // Check if reached the bottom of the page
                boolean atBottom = (Boolean) js.executeScript(
                        "return window.innerHeight + window.pageYOffset >= document.body.scrollHeight;"
                );

                if (atBottom) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Không tìm thấy hoặc không thể cuộn .scrollableContainer: " + e.getMessage());
        }
        driver.switchTo().defaultContent();
    }

    public WebDriver getDriver() {
        return driver;
    }

}