package tests;

import base.BaseTest;
import io.qameta.allure.*;
import models.CheckResult;
import models.UiCheckResult;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import utils.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static utils.UiCheck.*;

@Listeners(JiraBugReporter.class)
@Feature("Account Module")
@Story("Basic Case")
public class SFAccount extends BaseTest {
    @BeforeClass
    public void loadLanguageProperties() throws IOException {
        String language = getLanguage(); // "en" or "vn"
        String filePath = "src/test/resources/DataTest.xlsx";
        String dataSheetName = "Account";      // Sheet chứa label theo section
        ExcelLoader.load(filePath, language, dataSheetName);
    }

    @Test(priority = 2)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Check labels theo từng section")
    public void checkUI() throws InterruptedException {
        String url = ExcelLoader.getUrl("Account", null, "View");
        StepLogger.step("1. 🔗 Opening URL: " + url);
        PageNavigator navigator = new PageNavigator(driver);
        navigator.openPage(url);
        new Helper(driver).scrollPageDown();

        StepLogger.step("2. Đọc labels từ Excel, skip field đánh dấu 'x' cột D");
        Map<String, List<String>> expectedMap = ExcelLoader.readLabelsBySection();

        StepLogger.step("3. So sánh với dữ liệu UI");
        Map<String, List<String>> actualLabels = UiCheck.getLabelsBySection(driver);
        UiCheckResult result = UiCheck.compareSectionLabels(actualLabels, expectedMap);
        UiCheck.printFieldsBySection(driver, result, true);

        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        UiCheck.captureErrorSections(driver, result, folderPath, timestamp, methodName);

        Assert.assertEquals(result.getMissingSections().size(), 0, "❌ Có section bị thiếu: ");
        Assert.assertEquals(result.getExtraSections().size(), 0, "❌ Có section thừa: ");
        Assert.assertEquals(result.getMissingLabelsPerSection().size(), 0, "❌ Có label bị thiếu: ");
        Assert.assertEquals(result.getExtraLabelsPerSection().size(), 0, "❌ Có label thừa: ");

    }

    @Test(priority = 3)
    @Description("Check DataType Custom Fields Only")
    public void checkDataTypeAndAPIName() throws InterruptedException {
        String language = getLanguage();
        if (!"en".equalsIgnoreCase(language)) {
            Assert.fail("❌ Giao diện: tiếng Việt bỏ qua check DataType");
        }
        String url = ExcelLoader.getUrl("Account", null,"DataType");
        StepLogger.step("1. Opening URL: " + url);
        PageNavigator navigator = new PageNavigator(driver);
        navigator.openPage(url);
        new Helper(driver).scrollToLoadAllFields();

        StepLogger.step("2. Kiểm tra API Name và Data Type của các field cần thiết. ");
        // Đọc dữ liệu từ Excel
        Map<String, List<String>> expectedMap = ExcelLoader.readFieldInfoFromExcel();
        // Lấy UI (bản mới là List<List<String>> để xử lý trùng label)
        Map<String, List<List<String>>> actualMap = UiCheck.getFieldInfoFromUI(driver);
        // So sánh
        UiCheck.compareFieldInfoByLabel(expectedMap, actualMap);
    }

    @Test(priority = 4)
    @Description("Check history tracking")
    public void checkHistoryTracking() throws Exception {
        String url = ExcelLoader.getUrl("Account", null, "History");
        StepLogger.step("1. 🔗 Opening URL: " + url);
        PageNavigator navigator = new PageNavigator(driver);
        navigator.openPage(url);

        StepLogger.step("2. Đọc data test từ Excel và so sánh với UI");
        Map<String, List<String>> expectedMap = ExcelLoader.readFieldHistory();
        Map<String, List<String>> actualMap = UiCheck.getFieldHistory(driver);
        CheckResult result = UiCheck.compareFieldHistory(actualMap, expectedMap);

        // Locate iframe using 'contains' in title
        WebElement iframe = driver.findElement(By.xpath("//iframe[contains(@title, 'Field History')]"));
        driver.switchTo().frame(iframe);
        highlightMatchedFields(driver, result);
        highlightMissingFields(driver, result);
        highlightExtraFields(driver, result);

        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        UiCheck.captureHistoryTracking(driver, result, folderPath, timestamp, methodName);
        driver.switchTo().defaultContent();

        new Helper(driver).scrollHistoryTrackingPage();

        Assert.assertTrue(result.getMissingInformation().isEmpty(), "❌ Có label bị thiếu");
        Assert.assertTrue(result.getExtraInformation().isEmpty(), "❌ Có label bị thừa");
        int expectedCount = expectedMap.values().stream().mapToInt(List::size).sum();
        int actualMatched = result.getMatchedInformation().size();
        Assert.assertEquals(actualMatched, expectedCount, "❌ Số lượng label khớp không đúng");
    }

    @Test(priority = 5)
    @Description("Check required field(s)")
    public void checkRequired() throws Exception {
        String url = ExcelLoader.getUrl("Account", null, "New");
        StepLogger.step("1. Sau khi đăng nhập thành công, truy cập object Account sau đó bấm 'New'");
        PageNavigator navigator = new PageNavigator(driver);
        navigator.openPage(url);
        new Helper(driver).scrollPageDown();

        StepLogger.step("2. Kiểm tra các field Required");
        Map<String, List<String>> expectedMap = ExcelLoader.readFieldRequired();
        Map<String, List<String>> actualMap = UiCheck.getFieldRequired(driver);
        CheckResult result = UiCheck.compareFieldRequired(actualMap, expectedMap);
        highlightMatchedFields(driver, result);
        highlightMissingFields(driver, result);
        highlightExtraFields(driver, result);
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        UiCheck.captureRequiredFieldScreenshots(driver, result, folderPath, timestamp, methodName);
        Assert.assertEquals(result.getMissingInformation().size(), 0, "Có label bị thiếu so với Excel");
        Assert.assertEquals(result.getExtraInformation().size(), 0);
        Assert.assertEquals(result.getMatchedInformation().size(), expectedMap.values().stream().mapToInt(List::size).sum(), "Số lượng khớp không đúng");

    }

    @Test(priority = 6)
    @Description("Check picklist values & default picklist value")
    public void checkPicklist() throws Exception {
        String url = ExcelLoader.getUrl("Account", null, "New");
        StepLogger.step("1. Opening URL: " + url);
        new PageNavigator(driver).openPage(url);
        new Helper(driver).scrollPageDown();

        String objectName = "Account"; // Sheet 'PicklistValue', cột A - Object?
        StepLogger.step("2. Kiểm tra danh sách values của các field picklist.");
        Map<String, List<List<String>>> picklistMap = getAndCheckFieldPicklist(driver, objectName);
        checkAllPicklistFields(picklistMap);
    }
/*
    @Test(priority = 6, description = "Tạo mới")
    void NewAccount() throws InterruptedException {
        SFAccountFlow flow = new SFAccountFlow(driver);
        // lấy counter từ Flow
        String generatedName1 = flow.getGeneratedName1("Account Test");      // -> "4 Account Test"
        String generatedTenTaiKhoan = flow.getGeneratedTenTaiKhoan("Account Test DN");  // -> "4 Tài Khoản DN"

        flow.newAccountDN(
                generatedName1,
                generatedTenTaiKhoan,
                "Nguyễn Văn Thông",
                "Đối tác nước ngoài",
                "auto@gmail.com",
                "L/C AT SIGHT",
                "Export Distributor",
                "Miền Bắc",
                "Tỉnh Vĩnh Phúc",
                "B2B",
                "Viet Nam",
                "khu dân cư Vĩnh Lộc"
        );
        flow.clickSaveButton();
        boolean result = flow.verifyAccountNewToast();
        Assert.assertTrue(result);
    }
*/
    @AfterMethod
    public void delayAfterEachTest() throws InterruptedException {
        Thread.sleep(2000); // delay between tests
    }
    @AfterMethod
    public void attachJiraBug(ITestResult result) {
        String url = (String) result.getAttribute("jiraBugUrl");
        if (url != null) {
            Allure.addAttachment("🪲 Jira Bug", "text/uri-list", url); // ✅ đính kèm url trong Allure
            System.out.println("URL Attach Allure: " + url);
        }
        StepLogger.clear();
    }
}
