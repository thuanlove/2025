package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.ExcelLoader;

import java.time.Duration;
import java.time.LocalDate;

public class SFOppPage extends SFBasePage{
    public SFOppPage(WebDriver driver) {
        super(driver);
    }
    // Nhập liệu
    public void fillOppName(String oppName) {
        fillTextField("OpportunityNameLabel", oppName);
    }
    public void fillCloseDate(LocalDate date) {
        fillDateField("//input[@name='CloseDate']", date);
    }

    public void fillAccountName(String accountName) {
        selectFromSearchbar("AccountId", accountName);
    }
    public void selectStage(String stage) {
        selectFromCombobox("StageName", stage);
    }
    public boolean verifyOppNewToast() {
      String newMessage = ExcelLoader.getCommonValue("Opp.OpportunityName");//Nội dung thông báo chứa Value của Field?

        String xpath = "//span[text()='"+newMessage+"']/ancestor::div[contains(@class,'slds-form-element')]/descendant::lightning-formatted-text";
        return verifyCreatedMessage(xpath, "Opp.ToastMessage.Created");
    }

    //Click nút Save
    public void submit() {
        submitForm("Common.buttonSave");
    }

}
