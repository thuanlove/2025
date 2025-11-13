package flows;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pages.SFAccountPage;
import utils.Counter;
import utils.ExcelLoader;
import utils.PageNavigator;

import java.time.Duration;

public class SFAccountFlow {
    private WebDriver driver;
    private SFAccountPage newAccountDNPage;
    private PageNavigator navigator;

    public SFAccountFlow(WebDriver driver) {
        this.driver = driver;
        this.navigator = new PageNavigator(driver);
        this.newAccountDNPage = new SFAccountPage(driver);
    }

    public void newAccountDN(String name1, String tenTaiKhoan, String salesManager, String grouping, String businessEmail,
                             String paymentTerms, String distributionChannel, String salesOffice, String salesGroup, String customerGroup,
                             String quocGia, String duongPho) throws InterruptedException {
        String url = ExcelLoader.getUrl("Account", null, "New");
        // Đọc URL từ Excel và mở trang
        navigator.openPage(url);

        String message = ExcelLoader.getCommonValue("Common.Account.NewMessage");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement brandNameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[contains(text(), '" + message + "')]/following-sibling::div//input[@type='text']")
        ));

        newAccountDNPage.fillName1(name1);
        newAccountDNPage.fillTenTaiKhoan(tenTaiKhoan);
        newAccountDNPage.fillSalesManager(salesManager);
        newAccountDNPage.fillGrouping(grouping);
        newAccountDNPage.fillBusinessEmail(businessEmail);
        newAccountDNPage.fillPaymentTerms(paymentTerms);
        newAccountDNPage.fillDistributionChannel(distributionChannel);
        newAccountDNPage.fillSalesOffice(salesOffice);
        newAccountDNPage.fillSalesGroup(salesGroup);
        newAccountDNPage.fillCustomerGroup(customerGroup);
        newAccountDNPage.fillQuocGia(quocGia);
        newAccountDNPage.fillDuongPho(duongPho);
    }

    public void clickSaveButton() {
        newAccountDNPage.submit();
    }

    public boolean verifyAccountNewToast() {
        return newAccountDNPage.verifyAccountNewToast();
    }

}
