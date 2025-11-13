package tests;

import base.BaseTest;
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
import static utils.ExcelLoader.readPicklistDependencyFromExcel;
import static utils.UiCheck.*;
import io.qameta.allure.*;

@Listeners(JiraBugReporter.class)
@Feature("Lead Module")
@Story("Basic Case")
public class SFLead extends BaseTest {
    @BeforeClass
    public void loadLanguageProperties() throws IOException {
        String language = getLanguage(); // "en" or "vn"
        String filePath = "src/test/resources/DataTest.xlsx";
        String dataSheetName = "Lead";      // Sheet chứa label theo section
        ExcelLoader.load(filePath, language, dataSheetName);
    }

    @Test(priority = 2)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Check labels theo từng section")
    public void checkUI() throws InterruptedException {
        String url = ExcelLoader.getUrl("Lead", null, "View");
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
    public void checkDataType() throws InterruptedException {
        String language = getLanguage();
        if (!"en".equalsIgnoreCase(language)) {
            Assert.fail("❌ Giao diện: tiếng Việt bỏ qua check DataType");
        }

        String url = ExcelLoader.getUrl("Lead", null, "DataType");
        StepLogger.step("1. 🔗 Opening URL: " + url);
        new PageNavigator(driver).openPage(url);
        new Helper(driver).scrollToLoadAllFields();
        StepLogger.step("2. 🔗 Kiểm tra thông tin Data Type và API Name của field.");
        Map<String, String> expectedMap = ExcelLoader.readApiNameAndType();
        Map<String, String> actualLabels = UiCheck.getApiNameAndType(driver);
        expectedMap.entrySet().removeIf(e -> !e.getKey().endsWith("__c"));
        actualLabels.entrySet().removeIf(e -> !e.getKey().endsWith("__c"));

        Map<String, List<String>> result = UiCheck.compareApiNameAndType(expectedMap, actualLabels);
        List<String> actualOrder = getApiNameOrderFromUI(driver);

        captureDataType(driver, actualOrder, result, BaseTest.folderPath, BaseTest.timestamp, "checkDataType");

        Assert.assertTrue(result.get("MissingInUI").isEmpty(), "❌ Có field thiếu trên UI");
        Assert.assertTrue(result.get("ExtraInUI").isEmpty(), "❌ Có field dư trên UI");
        Assert.assertTrue(result.get("TypeMismatch").isEmpty(), "❌ Có field sai kiểu dữ liệu");
    }

    @Test(priority = 4)
    @Description("Check history tracking")
    public void checkHistoryTracking() throws Exception {

        String url = ExcelLoader.getUrl("Lead", null, "History");
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

        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UiCheck.captureHistoryTracking(driver, result, folderPath, timestamp, methodName);
        driver.switchTo().defaultContent();
        //System.out.println("Result:\n" + result); //mở ra khi cần track
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
        String url = ExcelLoader.getUrl("Lead", null, "New");
        StepLogger.step("1. 🔗 Opening URL: " + url);
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
        //System.out.println("Result:\n" + result);

        Assert.assertEquals(result.getMissingInformation().size(), 0, "Có label bị thiếu so với Excel");
        Assert.assertEquals(result.getExtraInformation().size(), 0);
        Assert.assertEquals(result.getMatchedInformation().size(), expectedMap.values().stream().mapToInt(List::size).sum(), "Số lượng khớp không đúng");

    }

    @Test(priority = 5)
    @Description("Check picklist values & default picklist value")
    public void checkPicklist() throws Exception {
        String url = ExcelLoader.getUrl("Lead", null, "New");
        StepLogger.step("1. 🔗 Opening URL: " + url);
        new PageNavigator(driver).openPage(url);
        new Helper(driver).scrollPageDown();
        StepLogger.step("2. 🔗 Kiểm tra các field picklist.");
        String objectName = "Lead"; // Sheet 'PicklistValue', cột A - Object?
        Map<String, List<List<String>>> picklistMap = getAndCheckFieldPicklist(driver, objectName);
        checkAllPicklistFields(picklistMap);
    }

    @Test(priority = 6)
    @Description ("Check Picklist Dependency")
    public void checkPicklistDependency() throws Exception {
    String objectName = "Lead";
    String url = ExcelLoader.getUrl("Lead", null, "New");
    StepLogger.step("1. 🔗 Opening URL: " + url);
    PageNavigator navigator = new PageNavigator(driver);
    navigator.openPage(url);
    StepLogger.step("2. 🔗 Kiểm tra danh sách giá trị của field Picklist Dependency.");
    Map<String, Map<String, List<String>>> actualUIData = getPicklistDependencyFromUI(driver, objectName);
    Map<String, Map<String, List<String>>> expectedExcelData =
            readPicklistDependencyFromExcel(objectName);
    comparePicklistDependencies (expectedExcelData, actualUIData);

}

    @Test(priority = 7)
    @Description("Check Multi Picklist Values")
    public void checkMultiPicklistValues() throws Exception {
        String objectName = "Lead"; //MultiPicklist/Object Name - cột A
        String url = ExcelLoader.getUrl(objectName, null, "New");
        StepLogger.step("1. 🔗 Opening URL: " + url);

        PageNavigator navigator = new PageNavigator(driver);
        navigator.openPage(url);
        new Helper(driver).scrollPageDown();
        StepLogger.step("2. Kiểm tra giá trị MultiPicklist");
        runMultiPicklistCheck(driver, objectName);
    }

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
