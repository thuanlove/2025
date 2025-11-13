package flows;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pages.SFOppPage;
import utils.ExcelLoader;
import utils.PageNavigator;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public class SFOppFLow {
    private WebDriver driver;
    private SFOppPage newOpportunityPage;
    private PageNavigator navigator;

    public SFOppFLow(WebDriver driver) {
        this.driver = driver;
        this.navigator = new PageNavigator(driver);
        this.newOpportunityPage = new SFOppPage(driver);
    }

    public void newOpp_Collagen_Seafood(String oppName, LocalDate closeDate, String accountName, String stage) throws InterruptedException {
        String url = ExcelLoader.getUrl("Opp", "Collagen", "New");
        navigator.openPage(url);

        String oppNameValue = ExcelLoader.getCommonValue("Common.Opp.NewMessage");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement brandNameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[contains(text(), '" + oppNameValue + "')]/following-sibling::div//input[@type='text']")
        ));

        newOpportunityPage = new SFOppPage(driver);

        newOpportunityPage.fillOppName(oppName);
        newOpportunityPage.fillCloseDate(closeDate);
        newOpportunityPage.fillAccountName(accountName);
        newOpportunityPage.selectStage(stage);
    }

    public boolean verifyOppNewToast() {
        return newOpportunityPage.verifyOppNewToast();
    }

    public void clickSaveButton() {
        newOpportunityPage.submit();
    }
    //0506
    public boolean isRequiredFieldErrorDisplayed() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

            // Kiểm tra popup báo lỗi chính (forcePageError)
            WebElement errorDialog = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("div.forcePageError")
            ));

            // Kiểm tra các trường có lỗi hiển thị viền đỏ
            List<WebElement> errorFields = driver.findElements(By.cssSelector("input.slds-has-error, div.slds-has-error"));

            return errorDialog.isDisplayed() && !errorFields.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void closeErrorPopupIfVisible(WebDriver driver) {
        try {
            By closeBtnLocator = By.xpath("//button[@title='Close error dialog']");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement closeBtn = wait.until(ExpectedConditions.elementToBeClickable(closeBtnLocator));
            closeBtn.click();
            System.out.println("✅ Đã đóng popup lỗi.");
        } catch (TimeoutException e) {
            System.out.println("ℹ️ Không tìm thấy popup lỗi.");
        } catch (Exception ex) {
            System.out.println("❌ Lỗi khi đóng popup: " + ex.getMessage());
        }
    }



}
