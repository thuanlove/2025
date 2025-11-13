package utils;

import org.openqa.selenium.WebDriver;

public class PageNavigator {
    private WebDriver driver;

    public PageNavigator(WebDriver driver) {
        this.driver = driver;
    }

    public void openPage(String url) throws InterruptedException {
        driver.get(url); // Mở trang
        Thread.sleep(5000); // Chờ trang tải xong
    }

}



