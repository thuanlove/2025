package tests;

import base.BaseTest;
import flows.SFOppFLow;
import io.qameta.allure.*;
import models.CheckResult;
import models.UiCheckResult;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import utils.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static utils.UiCheck.*;

//@Epic("Salesforce UI Automation")
@Listeners(JiraBugReporter.class)
@Feature("Opp Module")
@Story("Basic Case")
public class SFOpp extends BaseTest {
    @BeforeClass
    public void loadLanguageProperties() throws IOException {
        String language = getLanguage(); // "en" or "vn"
        String filePath = "src/test/resources/DataTest.xlsx";
        String dataSheetName = "Opp";      // Sheet chứa label theo section
        ExcelLoader.load(filePath, language, dataSheetName);
    }

    @Test(priority = 1)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Check labels theo từng section")
    public void checkUI() throws InterruptedException {
        String url = ExcelLoader.getUrl("Opp", null, "View");
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

    @Test(priority = 2)
    @Description("Check DataType Custom Fields Only")
    public void checkDataType() throws InterruptedException {
        String language = getLanguage();
        if (!"en".equalsIgnoreCase(language)) {
            Assert.fail("❌ Giao diện: tiếng Việt bỏ qua check DataType");
        }

        String url = ExcelLoader.getUrl("Opp", null, "DataType");
        StepLogger.step("1. 🔗 Opening URL: " + url);
        new PageNavigator(driver).openPage(url);
        new Helper(driver).scrollToLoadAllFields();
        StepLogger.step("2. Kiểm tra thông tin DataType và API Name.");
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

    @Test(priority = 3)
    @Description("Check history tracking")
    public void checkHistoryTracking() throws Exception {
        String url = ExcelLoader.getUrl("Opp", null, "History");
        StepLogger.step("1. 🔗 Opening URL: " + url);
        PageNavigator navigator = new PageNavigator(driver);
        navigator.openPage(url);
        StepLogger.step("2. Kiểm tra field history được check.");
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

    @Test(priority = 4)
    @Description("Check required field(s)")
    public void checkRequired() throws Exception {
        String url = ExcelLoader.getUrl("Opp", null, "New");
        StepLogger.step("1. 🔗 Opening URL: " + url);
        PageNavigator navigator = new PageNavigator(driver);
        navigator.openPage(url);
        new Helper(driver).scrollPageDown();

        StepLogger.step("2. Kiểm tra các field required.");
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
        String url = ExcelLoader.getUrl("Opp", null, "New");
        StepLogger.step("1. 🔗 Opening URL: " + url);
        new PageNavigator(driver).openPage(url);
        new Helper(driver).scrollPageDown();
        StepLogger.step("2. Kiểm tra danh sách field picklist.");
        String objectName = "Opp"; // Sheet 'PicklistValue', cột A - Object?
        Map<String, List<List<String>>> picklistMap = getAndCheckFieldPicklist(driver, objectName);
        checkAllPicklistFields(picklistMap);
    }

    @Test(priority = 6)
    @Description("Automation Close Date - Collagen")
    public void AutomationCloseDate_Collagen() throws InterruptedException {
        try {
            StepLogger.step("1. Thực hiện tạo mới 1 record Opp với record type = 'Colaggen'. Điền giá trị 'Close Date' = today");
            SFOppFLow flow = new SFOppFLow(driver);
            flow.newOpp_Collagen_Seafood(
                    "Auto test Opp",
                    LocalDate.now(),
                    "10 TÀI KHOẢN DN",
                    "Qualification"
            );
            flow.clickSaveButton();
            boolean result = flow.verifyOppNewToast();
            Assert.assertTrue(result);
            Thread.sleep(1000);
            StepLogger.step("2. Kiểm tra field 'Close Date'. Field 'Close Date' = today + 14 ngày");

            WebElement closeDateEl = driver.findElement(By.xpath(
                    "//records-record-layout-item[@field-label='Close Date']//lightning-formatted-text"
            ));
            // ✅ Scroll tới Close Date
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", closeDateEl);
            Thread.sleep(500);
            String closeDateStr = closeDateEl.getText();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
            LocalDate closeDate = LocalDate.parse(closeDateStr, formatter);
            LocalDate expectedDate = LocalDate.now().plusDays(14);

            if (!closeDate.equals(expectedDate)) {
                Assert.fail(String.format("❌ Close Date mismatch. Expected: %s | Actual: %s", expectedDate, closeDate));
            } else {
                System.out.println("✅ Close Date is correct: " + closeDate);
            }

        } finally {
            // Gắn các bước vào Jira bug thông qua TestNG attribute
            ITestResult testResult = Reporter.getCurrentTestResult();
            String joinedSteps = String.join("\n", StepLogger.getSteps());
            testResult.setAttribute("steps", joinedSteps);
        }
    }
    /*
    @Test(priority = 7)
    @Description("Automation Close Date - Seafood")
    public void AutomationCloseDate_Seafood() throws InterruptedException {
        StepLogger.step("1. Thực hiện tạo mới 1 record Opp với record type = 'Seafood'. Điền giá trị 'Close Date' = today");
        SFOppFLow flow = new SFOppFLow(driver);
        flow.newOpp_Collagen_Seafood(
                "Auto test Opp",
                LocalDate.now(),
                "10 TÀI KHOẢN DN",
                "Qualification"
        );
        flow.clickSaveButton();
        boolean result = flow.verifyOppNewToast();
        Assert.assertTrue(result);
        Thread.sleep(5000);
        StepLogger.step("2. Kiểm tra field 'Close Date'. Field 'Close Date' = today + 14 ngày");
        WebElement closeDateEl = driver.findElement(By.xpath(
                "//records-record-layout-item[@field-label='Close Date']//lightning-formatted-text"
        ));
        String closeDateStr = closeDateEl.getText();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
        LocalDate closeDate = LocalDate.parse(closeDateStr, formatter);
        LocalDate expectedDate = LocalDate.now().plusDays(14);
        if (!closeDate.equals(expectedDate)) {
            Assert.fail(String.format("❌ Close Date mismatch. Expected: %s | Actual: %s", expectedDate, closeDate));
        } else {
            System.out.println("✅ Close Date is correct: " + closeDate);
        }
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
