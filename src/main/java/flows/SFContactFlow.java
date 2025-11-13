package flows;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pages.SFContactPage;
import utils.ExcelLoader;
import utils.PageNavigator;

import java.time.Duration;

public class SFContactFlow {
    private WebDriver driver;
    private SFContactPage newContactPage;
    private PageNavigator navigator;

    public SFContactFlow(WebDriver driver) {
        this.driver = driver;
        this.navigator = new PageNavigator(driver);
        this.newContactPage = new SFContactPage(driver);
    }

    public void newContact(String lastName, String accountName) throws InterruptedException {
        String url = ExcelLoader.getUrl("Contact", null, "New");
        navigator.openPage(url);

        String contactNameValue = ExcelLoader.getCommonValue("Common.Contact.NewMessage");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement brandNameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[contains(text(), '"+contactNameValue+"')]/following-sibling::div//input[@type='text']")
        ));

        newContactPage = new SFContactPage(driver);
        SFContactPage.fillLastName(lastName);
        SFContactPage.fillAccountName(accountName);
    }
    public boolean verifyContactNewToast() {
        return newContactPage.verifyContactNewToast();
    }
    public void clickSaveButton(){
        newContactPage.submit();
    }
}
