package pages;

import org.openqa.selenium.WebDriver;
import utils.ExcelLoader;

public class SFAccountPage extends SFBasePage{
    public SFAccountPage(WebDriver driver) {
        super(driver);
    }

    public void fillName1(String name1) {
        fillTextField("Name1Label", name1);
    }

    public void fillTenTaiKhoan(String tenTaiKhoan) {
        fillTextField("AccountNameLabel", tenTaiKhoan);
    }

    public void fillSalesManager(String salesManager) {
        selectFromSearchbar("SalesManagerLabel", salesManager);
    }

    public void fillGrouping(String grouping) {
        selectFromCombobox("GroupingLabel", grouping);
    }

    public void fillBusinessEmail(String businessEmail) {
        fillTextField("BusinessEmailLabel", businessEmail);
    }

    public void fillPaymentTerms(String paymentTerms) {
        selectFromCombobox("PaymentTermsLabel", paymentTerms);
    }

    public void fillDistributionChannel(String distributionChannel) {
        selectFromCombobox("DistributionChannelLabel", distributionChannel);
    }

    public void fillSalesOffice(String salesOffice) {
        selectFromCombobox("SalesOfficeLabel", salesOffice);
    }
    public void fillSalesGroup(String salesGroup) {
        selectFromCombobox("SalesGroupLabel", salesGroup);
    }

    public void fillCustomerGroup(String customerGroup) {
        selectFromCombobox("CustomerGroupLabel", customerGroup);
    }

    public void fillQuocGia(String quocGia) {
        selectFromCombobox1("CountryLabel",1, quocGia);
    }

    public void fillDuongPho(String duongPho) {
        fillTextField2("StreetLabel",1, duongPho);
    }

    public void submit() {
        submitForm("Common.buttonSave");
    }

    public boolean verifyAccountNewToast() {
        String newMessage = ExcelLoader.getCommonValue("Account.AccountName");//Dùng Value của Field?
        String xpath = "//span[text()='"+newMessage+"']/ancestor::div[contains(@class,'slds-form-element')]//lightning-formatted-text";
        return verifyCreatedMessage(xpath, "Account.ToastMessage.Created");
    }
}
