package base;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.*;
import pages.LoginPage;
import utils.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Comparator;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.GenerateAllureReportListener; // 👈 đảm bảo import đúng

@Listeners(GenerateAllureReportListener.class) // 👈 THÊM DÒNG NÀY

public class BaseTest {
    protected static String timestamp;
    protected static String folderPath;
    protected WebDriver driver;
    protected WebDriverWait wait;
    private static boolean isRecordingEnabled = true;
    private static final Logger logger = LoggerFactory.getLogger(BaseTest.class);
    protected String language_use;

    public String getLanguage() {
        if (this.language_use == null || this.language_use.equals(""))
            return "en";
        else
            return language_use;
    }

    public void setLanguage(String language_use) {
        this.language_use = language_use;
    }

    @Parameters({"testSuite"})  // note
    @BeforeClass // Pre-test setup
    public void setUp(@Optional("0") String testSuiteValue) throws InterruptedException {
        logger.info("The value of testSuite is: {}", testSuiteValue);
        ChromeOptions options = new ChromeOptions(); // Initialize ChromeOptions
        // Automatically setup ChromeDriver using WebDriverManager
        //WebDriverManager.chromedriver().setup();
        options.addArguments("--disable-notifications");

        if ((testSuiteValue != null && testSuiteValue.equals("1")) || GraphicsEnvironment.isHeadless() || !isRecordingEnabled()) {
            options.addArguments("--headless=new"); // Headless mode
            options.addArguments("--disable-gpu");  // Disable GPU (useful for headless mode)
            //options.addArguments("--disable-notifications");
            options.addArguments("--no-sandbox"); // Use with caution, understand security implications
            options.addArguments("--disable-dev-shm-usage"); // Often useful in Docker/CI environments
            options.addArguments("--window-size=1920,1080"); // Set a window size for consistency
        }

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        //driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20));
        //wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

        driver.get(ConfigReader.getProperty("salesforce.url") + "?EToken=MsKcAEYUyeZWt3fYyUuihoGyr");

        LoginPage loginPage = new LoginPage(driver);
        //loginPage.loginEToken();
        loginPage.login(ConfigReader.getProperty("salesforce.username"), ConfigReader.getProperty("salesforce.password"));
        String language = checkLanguageSetting();
        this.setLanguage(language);
    }

    @BeforeClass
    public void beforeClass() {
        timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        folderPath = "test-screenshots/" + timestamp;
        new File(folderPath).mkdirs();
    }

    @BeforeMethod
    public void startVideoRecording(Method method) throws Exception {
        VideoRecorderUtil.startRecording(method.getName());
    }

    @AfterMethod
    public void tearDown(ITestResult result) {
            File videoFile = VideoRecorderUtil.stopRecording();
            if (result.getStatus() == ITestResult.FAILURE) {
                if (videoFile != null && videoFile.exists()) {
                    result.setAttribute("videoFile", videoFile); // ✅ Gán vào ITestResult
                }

                System.out.println("❌ Test failed: " + result.getMethod().getMethodName());
            } else {
                System.out.println("✅ Test passed: " + result.getMethod().getMethodName());
            }

        if (ITestResult.FAILURE == result.getStatus()) {
            // Không chụp toàn trang vì đã có screenshot từng section trong test
            logger.warn("❌ Test failed: {}, không chụp toàn trang vì đã có hình lỗi từng section.", result.getMethod().getMethodName());
        } else {
            logger.info("Test passed: {}", result.getMethod().getMethodName());
        }
    }
    @AfterMethod
    public void uploadVideoToJira(ITestResult result) {
        String issueUrl = (String) result.getAttribute("jiraBugUrl");
        File videoFile = (File) result.getAttribute("videoFile");
        if (issueUrl != null && videoFile != null && videoFile.exists()) {
            String issueKey = extractIssueKey(issueUrl);
            try {
                new JiraBugReporter().uploadAttachmentToIssue(issueKey, videoFile);
                System.out.println("📹 Uploaded video to Jira: " + videoFile.getName());
            } catch (Exception e) {
                System.err.println("❌ Failed to upload video: " + e.getMessage());
            }
        }
    }
    private String extractIssueKey(String url) {
        if (url == null) return null;
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }
    public String checkLanguageSetting() throws InterruptedException {
        driver.get(ConfigReader.getProperty("salesforce.url") + "lightning/settings/personal/LanguageAndTimeZone/home");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement iframe = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//iframe|//frame")));
        driver.switchTo().frame(iframe);

        // Get the dropdown element
        WebElement languageDropdown = driver.findElement(By.id("LanguageAndTimeZoneSetup:editPage:editPageBlock:languageLocaleKey"));
        // Get the selected option
        WebElement selectedLanguage = languageDropdown.findElement(By.cssSelector("option[selected='selected']"));

        // Output the selected language
        System.out.println("Default Language: " + selectedLanguage.getText());
        return mapLanguageToLocaleCode(selectedLanguage.getText());
    }

    public static String mapLanguageToLocaleCode(String selectedLanguage) {
        switch (selectedLanguage.trim()) {
            case "English":
            case "English (United States)":
                return "en";
            case "Tiếng Việt":
            case "Vietnamese (Vietnam)":
                return "vn";
            default:
                return "en"; // fallback
        }
    }


    @AfterClass //Affter
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    public static boolean isRecordingEnabled() {//record
        return isRecordingEnabled && !java.util.Optional.ofNullable(System.getenv("CI"))
                .map("true"::equalsIgnoreCase)
                .orElse(false);
    }

    public static void setRecordingEnabled(boolean enabled) {//chua sử dụng
        isRecordingEnabled = enabled;
    }
/*    @BeforeSuite
    public void cleanOldAllureResults() {
        try {
            Path resultsDir = Paths.get("target", "allure-results");
            if (Files.exists(resultsDir)) {
                Files.walk(resultsDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                System.out.println("🧹 Deleted old Allure results");
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to clean Allure results:");
            e.printStackTrace();
        }
    }*/
    @BeforeSuite(alwaysRun = true)
    //delete video,image,allure report trước khi run test mới
    protected void deleteReport() throws IOException {
        ReportUtils.DeleteAllureFolder();
    }
}
