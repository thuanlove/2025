package pages;

import org.openqa.selenium.WebDriver;
import utils.ExcelLoader;

public class SFContactPage extends SFBasePage {
    public SFContactPage(WebDriver driver) {
        super(driver);
    }

    // Nhập liệu
    public static void fillLastName(String lastName) {
      //  fillTextField1("LastNameLabel", lastName);//key
    }

    public static void fillAccountName(String accountName) {
        selectFromSearchbar("AccountNameLabel", accountName);//key
    }

    public boolean verifyContactNewToast() {
        String newMessage = ExcelLoader.getCommonValue("Contact.LastName");//Nội dung thông báo chứa Value của Field?
        String xpath = "//span[@class='test-id__field-label' and normalize-space()='" + newMessage + "']" +
                "/ancestor::div[contains(@class,'slds-form-element')]" +
                "//lightning-formatted-name";
        return verifyCreatedMessage(xpath,  "Contact.ToastMessage.Created");
    }

    //Click nút Save
    public void submit() {
        submitForm("Common.buttonSave");
    }

}
