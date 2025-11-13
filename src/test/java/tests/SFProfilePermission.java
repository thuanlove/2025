
package tests;

import base.BaseTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import utils.*;

import java.io.IOException;
import java.util.*;
import static utils.UiCheck.*;

@Listeners(JiraBugReporter.class)
public class SFProfilePermission extends BaseTest {
    @BeforeClass
    public void loadLanguageProperties() throws IOException {
        String language = getLanguage(); // "en" or "vn"
        String filePath = "src/test/resources/DataTest.xlsx";
        String dataSheetName = "ProfilePermission";      // Sheet chứa label theo section
        ExcelLoader.load(filePath, language, dataSheetName);
    }
    @Test(priority = 2, description = "Check profile permission.")
    public void checkProfilesLeader() throws Exception {
        String profileName = "Leader";
        StepLogger.step("1. Truy cập màn hình phân quyền profile "+ profileName);
        if (!"Leader".equalsIgnoreCase(profileName)) {
            System.out.println("⛔ Bỏ qua kiểm tra vì profile không phải Leader.");
            return;
        }
        String url = ExcelLoader.getUrl("Profile", null, profileName);
        PageNavigator navigator = new PageNavigator(driver);
        navigator.openPage(url);
        new Helper(driver).scrollPageDown();
        StepLogger.step("2. Kiểm tra phân quyền của các object");
        // Đọc dữ liệu expected
        Map<String, Map<String, Map<String, Boolean>>> allExpectedPermissions = ExcelLoader.readExpectedPermissions();
        if (!allExpectedPermissions.containsKey(profileName)) {
            System.out.println("⚠️ Không tìm thấy dữ liệu expect cho profile: " + profileName);
            return;
        }
        Map<String, Map<String, Boolean>> objectPermissionMap = allExpectedPermissions.get(profileName);
        Map<String, Map<String, Boolean>> actual = UiCheck.readActualPermissions(driver);
        compareProfilePermissions(profileName, objectPermissionMap, actual);
    }
    @Test(priority = 3, description = "Check profile permission.")
    public void checkProfilesSalesReps() throws Exception {
        String profileName = "Sales Rep";
        StepLogger.step("1. Truy cập màn hình phân quyền profile "+ profileName);
        if (!"Leader".equalsIgnoreCase(profileName)) {
            System.out.println("⛔ Bỏ qua kiểm tra vì profile không phải Leader.");
            return;
        }
        String url = ExcelLoader.getUrl("Profile", null, profileName);
        PageNavigator navigator = new PageNavigator(driver);
        navigator.openPage(url);
        new Helper(driver).scrollPageDown();
        StepLogger.step("2. Kiểm tra phân quyền của các object");
        // Đọc dữ liệu expected
        Map<String, Map<String, Map<String, Boolean>>> allExpectedPermissions = ExcelLoader.readExpectedPermissions();
        if (!allExpectedPermissions.containsKey(profileName)) {
            System.out.println("⚠️ Không tìm thấy dữ liệu expect cho profile: " + profileName);
            return;
        }
        Map<String, Map<String, Boolean>> objectPermissionMap = allExpectedPermissions.get(profileName);
        Map<String, Map<String, Boolean>> actual = UiCheck.readActualPermissions(driver);
        compareProfilePermissions(profileName, objectPermissionMap, actual);
    }


    @AfterMethod
    public void delayAfterEachTest() throws InterruptedException {
        Thread.sleep(2000); // delay between tests
    }
}
