package utils;

import io.qameta.allure.Allure;
import models.UiCheckResult;
import org.openqa.selenium.*;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import models.CheckResult;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;

import static java.lang.Thread.sleep;
import static utils.ExcelLoader.*;

public class UiCheck {

    // Hàm chuẩn hóa label: loại bỏ dấu '*' và trim khoảng trắng
    public static String cleanLabel(String label) {
        return label.replace("*", "").trim();
    }

    private static void compareFieldLabels(List<String> actualList, List<String> expectedList, String section, UiCheckResult result) {
        // Chuẩn hóa cả hai danh sách
        List<String> cleanedActual = actualList.stream()
                .map(UiCheck::cleanLabel)
                .collect(Collectors.toList());

        List<String> cleanedExpected = expectedList.stream()
                .map(UiCheck::cleanLabel)
                .collect(Collectors.toList());

        // Các label trùng khớp
        List<String> matchedLabels = cleanedActual.stream()
                .filter(cleanedExpected::contains)
                .collect(Collectors.toList());
        if (!matchedLabels.isEmpty()) {
            result.getMatchedLabelsPerSection().put(section, matchedLabels);
        }
        // Các label thiếu
        List<String> missingLabels = cleanedExpected.stream()
                .filter(label -> !cleanedActual.contains(label))
                .collect(Collectors.toList());
        if (!missingLabels.isEmpty()) {
            result.getMissingLabelsPerSection().put(section, missingLabels);
        }

        // Các label thừa
        List<String> extraLabels = cleanedActual.stream()
                .filter(label -> !cleanedExpected.contains(label))
                .collect(Collectors.toList());
        if (!extraLabels.isEmpty()) {
            result.getExtraLabelsPerSection().put(section, extraLabels);
        }
    }

    public static void captureErrorSections(WebDriver driver, UiCheckResult result, String folderPath, String timestamp, String methodName) throws InterruptedException {
        AtomicInteger screenshotIndex = new AtomicInteger(1);

        Set<String> allErrorSections = new HashSet<>();
        allErrorSections.addAll(result.getMissingLabelsPerSection().keySet());
        allErrorSections.addAll(result.getExtraLabelsPerSection().keySet());

        for (String section : allErrorSections) {
            try {
                WebElement sectionElement = driver.findElement(By.xpath("//*[text()='" + section + "']"));
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", sectionElement);
                sleep(1000); // wait for scroll

                String safeFileName = section
                        .replaceAll("[\\\\/:*?\"<>|]", "")  // Loại bỏ ký tự cấm
                        .replaceAll("\\s+", "_");           // Thay khoảng trắng thành dấu gạch dưới
                //String fileName = methodName + "_" + safeFileName + "_" + timestamp + "_" + screenshotIndex.getAndIncrement();
                String fileName = methodName + "_" + safeFileName + "_" + timestamp;
                ScreenshotUtil.captureScreenshot(driver, folderPath, fileName);

            } catch (Exception e) {
                System.out.println("⚠️ Cannot capture section: " + section);
                e.printStackTrace();
            }
        }
    }

    // Lấy Map các section và label từ trang web.
    public static Map<String, List<String>> getLabelsBySection(WebDriver driver) {
        String sectionsXPath =
                ".//div[@class='test-id__section slds-section has-header slds-is-open slds-m-vertical_none'] |" +
                        ".//div[@class='section-layout-container slds-section slds-is-open'] |" + ".//div[@class='test-id__section slds-section slds-is-open slds-m-vertical_none']";

        String titleSectionXPath =
                ".//span[contains(@class,'test-id__section-header-title')]" +
                        " | .//button[@class='slds-button slds-section__title-action']/span";

        String labelsXPath =
                ".//records-record-layout-item//span[contains(@class, 'test-id__field-label')]" +
                        " | .//record_flexipage-record-field//span[contains(@class, 'test-id__field-label')]" +
                        " | .//a[contains(@class,'slds-hyphenate') and normalize-space(text())!='']";

        Map<String, List<String>> sectionMap = new LinkedHashMap<>();
        List<WebElement> sectionElements = driver.findElements(By.xpath(sectionsXPath));

        String lastValidSectionTitle = null;

        for (WebElement section : sectionElements) {
            String rawTitle = getSectionTitle(section, titleSectionXPath);
            String sectionTitle = normalizeSectionTitle(rawTitle);

            List<String> fieldLabels = getFieldLabels(section, labelsXPath);
            if (fieldLabels.isEmpty()) continue;

            if (!sectionTitle.isEmpty()) {
                lastValidSectionTitle = sectionTitle;
                sectionMap.putIfAbsent(sectionTitle, new ArrayList<>());
            }

            // Gộp vào section hợp lệ gần nhất nếu tiêu đề trống
            if (sectionTitle.isEmpty() && lastValidSectionTitle != null) {
                sectionMap.get(lastValidSectionTitle).addAll(fieldLabels);
            } else {
                sectionMap.get(sectionTitle).addAll(fieldLabels);
            }
        }
        return sectionMap;
    }

    private static String normalizeSectionTitle(String title) {
        return title == null ? "" : title.replaceAll("\\s+", " ").trim();
    }

    private static String getSectionTitle(WebElement sectionElement, String titleSectionXPath) {
        List<WebElement> titleElements = sectionElement.findElements(By.xpath(titleSectionXPath));
        if (!titleElements.isEmpty()) {
            return titleElements.get(0).getText().trim();
        }
        return ""; // Không có tiêu đề
    }

    // Lấy label trong từng section.
    private static List<String> getFieldLabels(WebElement sectionElement, String labelsXPath) {
        return sectionElement.findElements(By.xpath(labelsXPath)).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(labelText -> !labelText.isEmpty())
                .collect(Collectors.toList());
    }

    public static Map<String, String> getApiNameAndType(WebDriver driver) {
        Map<String, String> fieldMap = new LinkedHashMap<>();

        List<WebElement> rows = driver.findElements(By.xpath("//table//tbody/tr"));
        for (WebElement row : rows) {
            try {
                String apiName = row.findElement(By.xpath(".//td[2]//span")).getText().trim();
                String dataType = row.findElement(By.xpath(".//td[3]//span")).getText().trim();
                fieldMap.put(apiName, dataType);
            } catch (Exception e) {
                // skip headers/invalid rows
            }
        }

        return fieldMap;
    }

    // Hàm lấy danh sách API name theo thứ tự dòng trên UI
    public static List<String> getApiNameOrderFromUI(WebDriver driver) {
        List<String> apiOrder = new ArrayList<>();
        List<WebElement> rows = driver.findElements(By.xpath("//table//tbody/tr"));

        for (WebElement row : rows) {
            // Cột API name giả sử nằm cột thứ 2 (td[2]), tùy UI chỉnh lại cho đúng
            String apiName = row.findElement(By.xpath("./td[2]")).getText().trim();
            apiOrder.add(apiName);
        }
        return apiOrder;
    }

    public static String formatList(List<String> items, int itemsPerLine) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(items.get(i));
            if ((i + 1) % itemsPerLine == 0) {
                sb.append("\n");
            } else if (i < items.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    // So sánh và in kết quả theo từng section
    public static void printFieldsBySection(WebDriver driver, UiCheckResult result, boolean highlightFlag) {
        // Duyệt qua các section đã matched, nếu bật highlight thì mới thực hiện highlight
        if (highlightFlag) {
            result.getMatchedLabelsPerSection().forEach((section, labels) ->
                    labels.forEach(label -> highlightField(driver, label, true))//hàm thông thêm true
            );
            System.out.println("Highlight flag: " + highlightFlag);  // In ra giá trị của highlightFlag

        }
    }

    // Hàm highlight Field
    private static void highlightField(WebDriver driver, String field, boolean isMatch) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<WebElement> elements = driver.findElements(By.xpath(
                "//label[normalize-space(text())='" + field + "']" +
                        " | //span[contains(@class,'label') and normalize-space(text())='" + field + "']" +
                        " | //a[contains(@class,'slds-hyphenate') and normalize-space(text())='" + field + "']" +
                        " | //div//div[normalize-space(text())='" + field + "']"
        ));
        String color = isMatch ? "lightgreen" : "salmon"; // xanh lá nếu đúng, đỏ nếu sai
        // elements.forEach(element -> js.executeScript("arguments[0].style.backgroundColor='" + color + "'", element));
        for (WebElement element : elements) {
            // Scroll đến phần tử
            js.executeScript("arguments[0].scrollIntoView({behavior: 'auto', block: 'center'});", element);
            // Highlight
            js.executeScript("arguments[0].style.backgroundColor='" + color + "'", element);
            // Đợi một chút để người test thấy rõ (nếu muốn)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void highlightMatchedFields(WebDriver driver, CheckResult result) {
        List<String> matchedFields = result.getMatchedInformation();
        for (String field : matchedFields) {
            highlightField(driver, field, true); // sử dụng hàm highlightField có sẵn
        }
    }

    public static void highlightMissingFields(WebDriver driver, CheckResult result) {
        List<String> missingMatchedFields = result.getMissingInformation();
        for (String field : missingMatchedFields) {
            highlightField(driver, field, false); // sử dụng hàm highlightField có sẵn
        }
    }

    public static void highlightExtraFields(WebDriver driver, CheckResult result) {
        List<String> extraMatchedFields = result.getExtraInformation();
        for (String field : extraMatchedFields) {
            highlightField(driver, field, false); // sử dụng hàm highlightField có sẵn
        }
    }

    // get field required -nvthong 12/06/2025
    public static Map<String, List<String>> getFieldRequired(WebDriver driver) throws Exception {
        Map<String, List<String>> sectionMap = new LinkedHashMap<>();
        List<String> requiredFields = new ArrayList<>();

        // Lấy tất cả
        List<WebElement> divFields = driver.findElements(By.xpath(".//abbr[@class='slds-required']/parent::div | .//abbr[@class='slds-required']/parent::label"));
        for (WebElement divField : divFields) {
            String field = divField.getText().trim();
            if (!field.contains("* = Thông Tin Bắt Buộc") && !field.contains("* = Required Information")) {
                requiredFields.add(field);
            }
        }
        if (!requiredFields.isEmpty()) {
            sectionMap.put("Required Fields", requiredFields);
        }
        return sectionMap;
    }

    // compare field required-nvthong 12/06/2025 - Dttcam update 02/07/2025 in allure + in console khi pass + chỉ in phần sai
    public static CheckResult compareFieldRequired(Map<String, List<String>> actualMap, Map<String, List<String>> expectedMap) {
        CheckResult result = new CheckResult();

        // Gom tất cả các value từ map thành 1 Set, sau khi clean và trim
        Set<String> uiSet = actualMap.values().stream()
                .flatMap(List::stream)
                .map(String::trim)
                .map(UiCheck::cleanLabel)
                .collect(Collectors.toSet());

        Set<String> expectedSet = expectedMap.values().stream()
                .flatMap(List::stream)
                .map(String::trim)
                .map(UiCheck::cleanLabel)
                .collect(Collectors.toSet());

        // So sánh thiếu
        for (String value : expectedSet) {
            if (uiSet.contains(value)) {
                result.addMatchedInformation(value);
            } else {
                result.addMissingInformation(value);
            }
        }

        // So sánh dư
        for (String value : uiSet) {
            if (!expectedSet.contains(value)) {
                result.addExtraInformation(value);
            }
        }

        // 👉 Tạo log chi tiết cho console + Allure
        StringBuilder sb = new StringBuilder();

        List<String> missing = result.getMissingInformation();
        List<String> extra = result.getExtraInformation();

        if (!missing.isEmpty()) {
            sb.append("❌ Missing required field").append(missing.size() > 1 ? "s" : "").append(":\n");
            missing.forEach(e -> sb.append("  - ").append(e).append("\n"));
        }

        if (!extra.isEmpty()) {
            sb.append("❌ Extra required field").append(extra.size() > 1 ? "s" : "").append(":\n");
            extra.forEach(e -> sb.append("  - ").append(e).append("\n"));
        }

        if (sb.length() == 0) {
            sb.append("✅ All required fields matched.");
        }
// 👉 Lưu kết quả vào TestNG để log vào bug Jira nếu fail
        ITestResult testResult = Reporter.getCurrentTestResult();
        testResult.setAttribute("checkResult", sb.toString());
        //add steps vào bug jira
        String joinedSteps = String.join("\n", StepLogger.getSteps());
        testResult.setAttribute("steps", joinedSteps);
        String detail = sb.toString();
        System.out.println(detail);
        Allure.addAttachment("Kết quả", "text/plain", detail);

        return result;
    }
//capture required fields
public static void captureRequiredFieldScreenshots(WebDriver driver, CheckResult result, String folderPath, String timestamp, String methodName) throws Exception {
    List<String> allErrorLabels = new ArrayList<>();
    allErrorLabels.addAll(result.getMissingInformation());
    allErrorLabels.addAll(result.getExtraInformation());

    if (allErrorLabels.isEmpty()) return;
    // 🟡 Lấy danh sách các label bắt buộc
    Map<String, List<String>> sectionMap = getFieldRequired(driver);
    List<String> requiredLabels = sectionMap.getOrDefault("Required Fields", new ArrayList<>());

    if (requiredLabels.isEmpty()) return;

    JavascriptExecutor js = (JavascriptExecutor) driver;
    Long viewportHeight = (Long) js.executeScript("return window.innerHeight");

    class LabelInfo {
        String labelText;
        WebElement labelEl;
        int yOffset;

        LabelInfo(String text, WebElement el, int y) {
            labelText = text;
            labelEl = el;
            yOffset = y;
        }
    }

    List<LabelInfo> matchedLabels = new ArrayList<>();

    // 🟡 Tìm element label tương ứng
    List<WebElement> allLabelElements = driver.findElements(By.xpath(
            ".//abbr[@class='slds-required']/parent::div | .//abbr[@class='slds-required']/parent::label"
    ));

    for (WebElement labelEl : allLabelElements) {
        String labelText = labelEl.getText().trim();
        for (String target : requiredLabels) {
            if (labelText.equalsIgnoreCase(target.trim())) {
                int y = ((Number) js.executeScript("return arguments[0].getBoundingClientRect().top;", labelEl)).intValue();
                matchedLabels.add(new LabelInfo(labelText, labelEl, y));
                break;
            }
        }
    }

    // 🟢 Sắp xếp theo vị trí xuất hiện
    matchedLabels.sort(Comparator.comparingInt(l -> l.yOffset));

    // 🟢 Gom nhóm theo vị trí trên màn hình
    List<LabelInfo> currentGroup = new ArrayList<>();
    int baseY = -1;
    int groupIndex = 1;

    for (LabelInfo info : matchedLabels) {
        if (currentGroup.isEmpty()) {
            currentGroup.add(info);
            baseY = info.yOffset;
        } else {
            int deltaY = info.yOffset - baseY;
            if (deltaY <= viewportHeight * 0.8) {
                currentGroup.add(info);
            } else {
                // Scroll và chụp nhóm trước đó
                js.executeScript("arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", currentGroup.get(0).labelEl);
                Thread.sleep(800);
                String fileName = String.format("%s_RequiredField_Group%d_%s", methodName, groupIndex++, timestamp);
                ScreenshotUtil.captureScreenshot(driver, folderPath, fileName);
                currentGroup.clear();
                currentGroup.add(info);
                baseY = info.yOffset;
            }
        }
    }

    // 🟢 Chụp nhóm cuối
    if (!currentGroup.isEmpty()) {
        js.executeScript("arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", currentGroup.get(0).labelEl);
        Thread.sleep(800);
        String fileName = String.format("%s_RequiredField_Group%d_%s", methodName, groupIndex, timestamp);
        ScreenshotUtil.captureScreenshot(driver, folderPath, fileName);
    }

    driver.switchTo().defaultContent();
}

    // compare field required - -nvthong 12/06/2025
    public static CheckResult compareValuesMultiPicklist(Map<String, List<String>> actualMap, Map<String, List<String>> expectedMap) {
        CheckResult result = new CheckResult();

        // Gom tất cả các value (List<String>) từ map thành 1 Set, sau khi clean và trim
        Set<String> uiSet = actualMap.values().stream()
                .flatMap(List::stream)
                .map(String::trim)
                .map(UiCheck::cleanLabel)
                .collect(Collectors.toSet());

        Set<String> expectedSet = expectedMap.values().stream()
                .flatMap(List::stream)
                .map(String::trim)
                .map(UiCheck::cleanLabel)
                .collect(Collectors.toSet());

        // Tìm thông tin khớp và thiếu
        for (String value : expectedSet) {
            if (uiSet.contains(value)) {
                result.addMatchedInformation(value);
            } else {
                result.addMissingInformation(value);
            }
        }

        // Tìm thông tin dư
        for (String value : uiSet) {
            if (!expectedSet.contains(value)) {
                result.addExtraInformation(value);
            }
        }
        return result;
    }


/*    public static List<String> getValuesMultiPicklist(WebDriver driver, String divElementField) {
        List<String> picklistValues = new ArrayList<>();
        try {
            String xpath = String.format(
                    ".//records-record-layout-item[@field-label='%1$s'] | .//div[div[normalize-space(text())='%1$s']]",
                    divElementField
            );
            JavascriptExecutor js = (JavascriptExecutor) driver;
            WebElement fieldElement = driver.findElement(By.xpath(xpath));
            js.executeScript("arguments[0].scrollIntoView({behavior: 'auto', block: 'center'});", fieldElement);
            // Tìm phần tử ul chứa các giá trị
            WebElement scrollContainer = driver.findElement(By.xpath(
                    xpath + "//div[@class='slds-dueling-list__options']"
            ));

            // ✅ Scroll container (nếu có thanh cuộn)
            long lastScroll = -1;
            long currentScroll = 0;
            int attempts = 0;

            while (lastScroll != currentScroll && attempts++ < 10) {
                lastScroll = currentScroll;
                js.executeScript("arguments[0].scrollTop += 80;", scrollContainer);
                Thread.sleep(700);
                currentScroll = ((Number) js.executeScript("return arguments[0].scrollTop;", scrollContainer)).longValue();
            }

            // ✅ Chụp ảnh trước khi xử lý value
            String screenshotName = "Field_" + divElementField.replaceAll("\\s+", "_");
            ScreenshotUtil.captureScreenshot(driver, "./test-screenshots", screenshotName);
            List<WebElement> valueOfField = driver.findElements(By.xpath(
                    xpath + "//div[@class='slds-dueling-list__options']//ul/li"));
            for (WebElement value : valueOfField) {
                String text = value.getText().trim();
                if (!text.isEmpty()) {
                    picklistValues.add(text);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting picklist values: " + e);
            e.printStackTrace(); // để in stacktrace rõ ràng
        }

        return picklistValues;
    }*/
    /*public static List<String> getValuesMultiPicklist(WebDriver driver, String divElementField) {
        List<String> picklistValues = new ArrayList<>();
        try {
            String xpath = String.format(
                    ".//records-record-layout-item[@field-label='%1$s'] | .//div[div[normalize-space(text())='%1$s']]",
                    divElementField
            );
            JavascriptExecutor js = (JavascriptExecutor) driver;
            WebElement scrollContainer = driver.findElement(By.xpath(xpath + "//div[@class='slds-dueling-list__options']"));

            List<WebElement> valueOfField;
            int previousCount = -1;

            // Scroll liên tục đến từng item cuối
            while (true) {
                valueOfField = scrollContainer.findElements(By.xpath(".//ul/li"));
                int currentCount = valueOfField.size();

                if (currentCount == previousCount || currentCount == 0) {
                    break; // Không load thêm nữa hoặc không có dữ liệu
                }
                previousCount = currentCount;

                // Scroll đến từng item 1 từ trên xuống, để nhìn thấy
                for (WebElement element : valueOfField) {
                    js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
                    Thread.sleep(500); // Delay để người dùng nhìn thấy từng item
                }

                // Đợi thêm sau mỗi vòng scroll toàn bộ
                Thread.sleep(1000);
            }

            // Lấy text sau khi đã scroll đủ
            for (WebElement value : valueOfField) {
                String text = value.getText().trim();
                if (!text.isEmpty() && !picklistValues.contains(text)) {
                    picklistValues.add(text);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting picklist values: " + e);
            e.printStackTrace();
        }
        return picklistValues;
    }*/
public static List<String> getValuesMultiPicklist(WebDriver driver, String divElementField) {
    List<String> picklistValues = new ArrayList<>();
    try {
        String xpath = String.format(
                ".//records-record-layout-item[@field-label='%1$s'] | .//div[div[normalize-space(text())='%1$s']]",
                divElementField
        );
        JavascriptExecutor js = (JavascriptExecutor) driver;

        WebElement scrollContainer = driver.findElement(By.xpath(xpath + "//div[@class='slds-dueling-list__options']"));

        // 🟢 Scroll từ từ bằng JavaScript (setInterval)
        js.executeScript(
                "let container = arguments[0];" +
                        "let step = 100;" +                // mỗi lần cuộn 100px
                        "let delay = 50;" +               // mỗi 50ms
                        "let interval = setInterval(() => {" +
                        "  container.scrollTop += step;" +
                        "  if (container.scrollTop + container.clientHeight >= container.scrollHeight) clearInterval(interval);" +
                        "}, delay);",
                scrollContainer
        );

        // 🟢 Đợi một khoảng để scroll mượt hoàn tất
        Thread.sleep(2000); // có thể tăng lên nếu danh sách dài

        // 🟢 Lấy các item sau khi đã scroll xong
        List<WebElement> valueOfField = scrollContainer.findElements(By.xpath(".//ul/li"));
        for (WebElement value : valueOfField) {
            String text = value.getText().trim();
            if (!text.isEmpty() && !picklistValues.contains(text)) {
                picklistValues.add(text);
            }
        }

    } catch (Exception e) {
        System.out.println("⚠️ Error getting picklist values: " + e);
        e.printStackTrace();
    }
    return picklistValues;
}





    // multi picklist nvthong 16062025
    public static Map<String, List<List<String>>> getAndCheckFieldMultiPicklist(WebDriver driver, String objectName) {
        Map<String, List<List<String>>> result = new LinkedHashMap<>();
        String sheetName = "MultiPicklist";
        try {
            List<String> expectedFields = readMultiPicklistField(sheetName, objectName);
            for (String field : expectedFields) {
                sleep(3000);

                List<String> uiPicklistValues = getValuesMultiPicklist(driver, field);
                List<String> excelPicklistValues = readMultiPicklistValues(sheetName, objectName, field);

                List<List<String>> pair = new ArrayList<>();
                pair.add(uiPicklistValues);
                pair.add(excelPicklistValues);

                result.put(field, pair);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error in getAndCheckFieldMultiPicklist: " + e.getMessage());
        }
        return result;
    }

    //2606 update: in phần lable field sai
    public static UiCheckResult compareSectionLabels(Map<String, List<String>> actual, Map<String, List<String>> expected) {
        UiCheckResult result = new UiCheckResult();

        Set<String> actualSections = actual.keySet();
        Set<String> expectedSections = expected.keySet();

        // So sánh section thiếu
        Set<String> missingSections = new LinkedHashSet<>(expectedSections);
        missingSections.removeAll(actualSections);
        result.getMissingSections().addAll(missingSections);

        // So sánh section thừa
        Set<String> extraSections = new LinkedHashSet<>(actualSections);
        extraSections.removeAll(expectedSections);
        result.getExtraSections().addAll(extraSections);

        // So sánh labels từng section (chỉ so với các section có mặt ở cả hai bên)
        for (String section : expectedSections) {
            if (!actual.containsKey(section)) continue;

            List<String> expectedLabels = expected.get(section);
            List<String> actualLabels = actual.get(section);

            // ✅ Gọi lại hàm compareFieldLabels để hỗ trợ đánh dấu highlight các label sai
            compareFieldLabels(actualLabels, expectedLabels, section, result);
        }

        // ✅ Log kết quả chi tiết
        StringBuilder sb = new StringBuilder();
        if (!result.getMissingSections().isEmpty()) {
            sb.append("❌ Missing Sections: ").append(result.getMissingSections()).append("\n");
        }
        if (!result.getExtraSections().isEmpty()) {
            sb.append("❌ Extra Sections: ").append(result.getExtraSections()).append("\n");
        }
        if (!result.getMissingLabelsPerSection().isEmpty()) {
            sb.append("❌ Missing Labels:\n");
            result.getMissingLabelsPerSection().forEach((section, labels) -> {
                sb.append("   - Section: ").append(section).append("\n");
                sb.append("     ➤ Missing: ").append(labels).append("\n");
            });
        }
        if (!result.getExtraLabelsPerSection().isEmpty()) {
            sb.append("❌ Extra Labels:\n");
            result.getExtraLabelsPerSection().forEach((section, labels) -> {
                sb.append("   - Section: ").append(section).append("\n");
                sb.append("     ➤ Extra: ").append(labels).append("\n");
            });
        }
        if (sb.length() == 0) {
            sb.append("✅ No mismatches found.");
        }
// 👉 Lưu kết quả vào TestNG để log vào Jira nếu fail
        ITestResult testResult = Reporter.getCurrentTestResult();
        testResult.setAttribute("checkResult", sb.toString());
//add steps vào jira
        String joinedSteps = String.join("\n", StepLogger.getSteps());
        testResult.setAttribute("steps", joinedSteps);
        String details = sb.toString();
        System.out.println(details); // In console
        Allure.addAttachment("Kết quả", "text/plain", details); // In Allure

        return result;
    }

    //2006 cam update in console, allure, check thêm case excel trùng
    public static CheckResult comparePicklist(String fieldName, List<String> uiList, List<String> expectedList,
                                              Map<String, List<String>> duplicateMap) {
        CheckResult result = new CheckResult();

        Map<String, Long> excelCounts = expectedList.stream()
                .map(String::trim)
                .collect(Collectors.groupingBy(s -> s, LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> uiCounts = uiList.stream()
                .map(String::trim)
                .collect(Collectors.groupingBy(s -> s, LinkedHashMap::new, Collectors.counting()));

        // ✅ Chỉ thêm duplicated value nếu chưa log field này
        if (!duplicateMap.containsKey(fieldName)) {
            for (Map.Entry<String, Long> entry : excelCounts.entrySet()) {
                String value = entry.getKey();
                long count = entry.getValue();
                if (count > 1) {
                    duplicateMap.computeIfAbsent(fieldName, k -> new ArrayList<>())
                            .add("'" + value + "' (Excel x" + count + ")");
                }
            }
        }

        // Thiếu trong UI
        for (String value : excelCounts.keySet()) {
            if (!uiCounts.containsKey(value)) {
                result.addMissingInformation(value);
            }
        }

        // Thừa trong UI
        for (String value : uiCounts.keySet()) {
            if (!excelCounts.containsKey(value)) {
                result.addExtraInformation(value);
            }
        }

        return result;
    }

    //2306 Camdtt update
    public static void checkAllPicklistFields(Map<String, List<List<String>>> picklistMap) {
        Set<String> failedFields = new LinkedHashSet<>();
        StringBuilder logBuilder = AllureReporter.getBuilder(); // dùng builder từ reporter

        for (Map.Entry<String, List<List<String>>> entry : picklistMap.entrySet()) {
            String fieldName = entry.getKey();
            List<List<String>> lists = entry.getValue();

            List<String> uiValues = lists.get(0);
            List<String> excelValues = lists.get(1);
            List<String> defaultValueList = lists.get(2);

            String defaultUI = defaultValueList.size() > 0 ? defaultValueList.get(0).trim() : "";
            String defaultExcel = defaultValueList.size() > 1 ? defaultValueList.get(1).trim() : "";

            boolean failed = false;
            StringBuilder fieldLog = new StringBuilder();
            fieldLog.append("➤ Field '").append(fieldName).append("':\n");

            // So sánh picklist
            CheckResult res = comparePicklist(fieldName, uiValues, excelValues, new LinkedHashMap<>());

            // ✅ Check duplicate values in picklist
            Map<String, Long> excelCounts = excelValues.stream()
                    .map(String::trim)
                    .collect(Collectors.groupingBy(s -> s, LinkedHashMap::new, Collectors.counting()));

            for (Map.Entry<String, Long> e : excelCounts.entrySet()) {
                String value = e.getKey();
                long count = e.getValue();

                if (count > 1) {
                    if (value.equals(defaultExcel)) {
                        fieldLog.append("  ⚠️ Duplicate Default value: ").append(defaultExcel)
                                .append(" (Excel x").append(count).append(")\n");
                    }
                    fieldLog.append("  ⚠️ Duplicate Values: ").append(value)
                            .append(" (Excel x").append(count).append(")\n");
                    failed = true;
                }
            }

            // ❌ Default mismatch
            if (!defaultUI.equals(defaultExcel)) {
                fieldLog.append("  ❌ Default mismatch (Excel/UI): ").append(defaultExcel).append(" / ").append(defaultUI).append("\n");
                failed = true;
            }

            // ❌ Missing / Extra
            if (!res.getMissingInformation().isEmpty() || !res.getExtraInformation().isEmpty()) {
                fieldLog.append("  ❌ Value mismatch:\n");
                if (!res.getMissingInformation().isEmpty()) {
                    fieldLog.append("    ➤ Missing: ").append(res.getMissingInformation()).append("\n");
                }
                if (!res.getExtraInformation().isEmpty()) {
                    fieldLog.append("    ➤ Extra: ").append(res.getExtraInformation()).append("\n");
                }
                failed = true;
            }

            if (failed) {
                logBuilder.append(fieldLog).append("\n");
                failedFields.add(fieldName);
            }

        }
        if (failedFields.isEmpty()){
            logBuilder.append("Case Passed.\n");
        }
        // 👉 Lưu kết quả vào TestNG để log vào bug Jira nếu fail
        ITestResult testResult = Reporter.getCurrentTestResult();
        testResult.setAttribute("checkResult", logBuilder.toString());
        //add steps vào bug jira
        String joinedSteps = String.join("\n", StepLogger.getSteps());
        testResult.setAttribute("steps", joinedSteps);
        System.out.println(logBuilder);
        AllureReporter.flush("Kết quả:");

        if (!failedFields.isEmpty()) {
            Assert.fail("❌ Case failed in field(s): " + failedFields);
        }

    }

    //2606 Camdtt update test run luon pass, old nvthong
    public static Map<String, List<String>> getFieldHistory(WebDriver driver) throws Exception {
        Map<String, List<String>> sectionMap = new LinkedHashMap<>();
        List<String> checkedFields = new ArrayList<>();

        // 👉 Tách phần chuyển vào iframe thành hàm riêng
        WebElement iframe = switchToFirstIframe(driver);

        // ⏳ Chờ các checkbox render xong
        List<WebElement> checkboxes = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//input[@type='checkbox']")));

        boolean skippedFirst = false;
        for (WebElement checkbox : checkboxes) {
            if (checkbox.isSelected()) {
                String id = checkbox.getAttribute("id");
                if (id != null && !id.isEmpty()) {
                    WebElement label = driver.findElement(By.xpath("//label[@for='" + id + "']"));
                    if (label != null) {
                        String labelText = label.getText().trim();
                        if (!skippedFirst) {
                            skippedFirst = true;
                            continue; // Skip first selected checkbox (business logic)
                        }
                        checkedFields.add(labelText);
                    }
                }
            }
        }

        if (!checkedFields.isEmpty()) {
            sectionMap.put("Checked Fields", checkedFields);
        }

        driver.switchTo().defaultContent(); // 🧹 Quay lại context chính
        return sectionMap;
    }

    //ham ho tro cho historytracking
    public static WebElement switchToFirstIframe(WebDriver driver) {
        WebElement iframe = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe | //frame")));
        driver.switchTo().frame(iframe);
        return iframe;
    }

    //compareFieldHistory nvthong, update 1906 CamDTT thêm phần chỉ in sai
    public static CheckResult compareFieldHistory(Map<String, List<String>> actualMap, Map<String, List<String>> expectedMap) {
        CheckResult result = new CheckResult();

        // Gộp tất cả label từ các section
        List<String> actualList = new ArrayList<>();
        actualMap.values().forEach(actualList::addAll);

        List<String> expectedList = new ArrayList<>();
        expectedMap.values().forEach(expectedList::addAll);

        // Dùng Set để so sánh hiệu quả hơn
        Set<String> actualSet = new HashSet<>(actualList);
        Set<String> expectedSet = new HashSet<>(expectedList);

        // Khớp và thiếu
        for (String expected : expectedList) {
            if (actualSet.contains(expected)) {
                result.addMatchedInformation(expected);
            } else {
                result.addMissingInformation(expected);
            }
        }

        // Thừa
        for (String actual : actualList) {
            if (!expectedSet.contains(actual)) {
                result.addExtraInformation(actual);
            }
        }

        // ✅ Log vào console + Allure
        StringBuilder sb = new StringBuilder();
        if (!result.getMissingInformation().isEmpty()) {
            sb.append("❌ Missing fields in UI:\n");
            result.getMissingInformation().forEach(label -> sb.append("  - ").append(label).append("\n"));
        }

        if (!result.getExtraInformation().isEmpty()) {
            sb.append("❌ Extra fields in UI:\n");
            result.getExtraInformation().forEach(label -> sb.append("  - ").append(label).append("\n"));
        }


// 👉 Lưu kết quả vào TestNG để log vào bug Jira nếu fail
        ITestResult testResult = Reporter.getCurrentTestResult();
        testResult.setAttribute("checkResult", sb.toString());
        //add steps vào bug jira
        String joinedSteps = String.join("\n", StepLogger.getSteps());
        testResult.setAttribute("steps", joinedSteps);

        String details = sb.toString();
        System.out.println(details);
        Allure.addAttachment("Kết quả", "text/plain", details);

        return result;
    }

    //dung cho Historytracking, original Thong
    public static void captureHistoryTracking(WebDriver driver, CheckResult result, String folderPath, String timestamp, String methodName) throws InterruptedException {
        List<String> allErrorLabels = new ArrayList<>();
        allErrorLabels.addAll(result.getMissingInformation());
        allErrorLabels.addAll(result.getExtraInformation());

        if (allErrorLabels.isEmpty()) return;

        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Long viewportHeight = (Long) js.executeScript("return window.innerHeight");

            class LabelInfo {
                String labelText;
                WebElement labelEl;
                int yOffset;

                LabelInfo(String text, WebElement el, int y) {
                    labelText = text;
                    labelEl = el;
                    yOffset = y;
                }
            }
            List<LabelInfo> matchedLabels = new ArrayList<>();
            // Lấy tất cả checkbox
            List<WebElement> checkboxes = driver.findElements(By.xpath("//input[@type='checkbox']"));
            for (WebElement checkbox : checkboxes) {
                String id = checkbox.getAttribute("id");
                if (id != null && !id.isEmpty()) {
                    try {
                        WebElement label = driver.findElement(By.xpath("//label[@for='" + id + "']"));
                        String labelText = label.getText().trim();
                        for (String error : allErrorLabels) {
                            if (labelText.equalsIgnoreCase(error.trim())) {
                                int y = ((Number) js.executeScript("return arguments[0].getBoundingClientRect().top;", label)).intValue();
                                matchedLabels.add(new LabelInfo(labelText, label, y));
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            // Sắp xếp theo vị trí Y
            matchedLabels.sort(Comparator.comparingInt(l -> l.yOffset));
            // Từng nhóm đảm bảo nằm trong viewport
            List<LabelInfo> currentGroup = new ArrayList<>();
            int baseY = -1;
            int groupIndex = 1;
            for (LabelInfo info : matchedLabels) {
                if (currentGroup.isEmpty()) {
                    currentGroup.add(info);
                    baseY = info.yOffset;
                } else {
                    int deltaY = info.yOffset - baseY;
                    if (deltaY <= viewportHeight * 0.8) {
                        currentGroup.add(info);
                    } else {
                        // Cuộn tới label đầu nhóm
                        js.executeScript("arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", currentGroup.get(0).labelEl);
                        sleep(800);
                        String fileName = String.format("%s_Group%d_%s", methodName, groupIndex++, timestamp);
                        ScreenshotUtil.captureScreenshot(driver, folderPath, fileName);

                        currentGroup.clear();
                        currentGroup.add(info);
                        baseY = info.yOffset;
                    }
                }
            }

            // Chụp nhóm cuối cùng
            if (!currentGroup.isEmpty()) {
                js.executeScript("arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", currentGroup.get(0).labelEl);
                sleep(800);
                String fileName = String.format("%s_Group%d_%s", methodName, groupIndex, timestamp);
                ScreenshotUtil.captureScreenshot(driver, folderPath, fileName);
            }
            driver.switchTo().defaultContent();
        } catch (Exception e) {
            System.out.println("❌ Không thể xử lý label hoặc lỗi chụp field history.");
            driver.switchTo().defaultContent();
            e.printStackTrace();
        }
    }

    //2706 Xac dinh 1 viewport chua bao nhieu dong du lieu
    public static int detectLinesDataType(WebDriver driver) throws InterruptedException {
        List<WebElement> rows = driver.findElements(By.xpath("//table//tbody/tr"));
        if (rows.isEmpty()) return 0;

        // Cuộn đến dòng đầu
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'start'});", rows.get(0));
        Thread.sleep(600);

        JavascriptExecutor js = (JavascriptExecutor) driver;
        int count = 0;

        for (WebElement row : rows) {
            Boolean isFullyVisible = (Boolean) js.executeScript(
                    "var elem = arguments[0];" +
                            "var rect = elem.getBoundingClientRect();" +
                            "return (rect.top >= 0 && rect.bottom <= window.innerHeight);", row);

            if (Boolean.TRUE.equals(isFullyVisible)) {
                count++;
            } else {
                break; // vì các dòng dưới thường sẽ không visible tiếp nữa
            }
        }
        System.out.println("👁️ Viewport thực tế đang hiển thị chính xác " + count + " dòng (dựa theo visibility).");
        return count;
    }

    //2706 Chup hinh theo viewport tu dong
    public static void captureDataType(
            WebDriver driver,
            List<String> actualOrder,
            Map<String, List<String>> result,
            String folderPath,
            String timestamp,
            String methodName
    ) throws InterruptedException {

        List<WebElement> rows = driver.findElements(By.xpath("//table//tbody/tr"));
        int linesPerViewport = detectLinesDataType(driver);
        int totalRows = rows.size();

        // Bước 1: xác định index dòng lỗi
        Set<Integer> errorIndices = new TreeSet<>();
        for (String line : result.getOrDefault("TypeMismatch", Collections.emptyList())) {
            String fieldName = line.split("=>")[0].trim();
            int idx = actualOrder.indexOf(fieldName);
            if (idx != -1) errorIndices.add(idx);
        }
        for (String line : result.getOrDefault("ExtraInUI", Collections.emptyList())) {
            String fieldName = line.split("=>")[0].trim();
            int idx = actualOrder.indexOf(fieldName);
            if (idx != -1) errorIndices.add(idx);
        }

        // Bước 2: Gom lỗi theo viewport thực tế
        Map<Integer, List<Integer>> viewportGroups = new LinkedHashMap<>();
        for (Integer index : errorIndices) {
            int viewportIndex = index / linesPerViewport;
            viewportGroups.computeIfAbsent(viewportIndex, k -> new ArrayList<>()).add(index);
        }

        // Bước 3: Scroll và chụp theo viewport
        for (Map.Entry<Integer, List<Integer>> entry : viewportGroups.entrySet()) {
            int viewportIndex = entry.getKey();
            List<Integer> group = entry.getValue();
            int startRowIndex = viewportIndex * linesPerViewport;

            if (startRowIndex >= totalRows) continue;

            WebElement startRow = rows.get(startRowIndex);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'start'});", startRow);
            sleep(600);

            String screenshotName = methodName + "_viewport_" + (viewportIndex + 1) + "_" + timestamp;
            //takeScreenshot(driver, folderPath, screenshotName);
            ScreenshotUtil.captureScreenshot(driver, folderPath, screenshotName);


            System.out.println("📸 Chụp viewport #" + (viewportIndex + 1) + ", dòng lỗi: " + group);
        }
    }

    public static Map<String, List<String>> compareApiNameAndType(
            Map<String, String> apiMapFromExcel,
            Map<String, String> apiMapFromUI) {

        List<String> missingInUI = new ArrayList<>();
        List<String> extraInUI = new ArrayList<>();
        List<String> typeMismatch = new ArrayList<>();

        for (Map.Entry<String, String> excelEntry : apiMapFromExcel.entrySet()) {
            String apiName = excelEntry.getKey();
            String typeFromExcel = excelEntry.getValue().trim();

            if (!apiMapFromUI.containsKey(apiName)) {
                missingInUI.add(apiName + " => " + typeFromExcel);
            } else {
                String typeFromUI = apiMapFromUI.get(apiName).trim();
                if (!typeFromExcel.equalsIgnoreCase(typeFromUI)) {
                    typeMismatch.add(apiName + " => Excel: " + typeFromExcel + " | UI: " + typeFromUI);
                }
            }
        }

        for (Map.Entry<String, String> uiEntry : apiMapFromUI.entrySet()) {
            String apiName = uiEntry.getKey();
            if (!apiMapFromExcel.containsKey(apiName)) {
                extraInUI.add(apiName + " => " + uiEntry.getValue().trim());
            }
        }

        // Format chung để vừa in ra console, vừa đẩy lên Allure
        StringBuilder sb = new StringBuilder();
        if (!missingInUI.isEmpty()) {
            sb.append("❌ Missing in UI:\n");
            missingInUI.forEach(e -> sb.append("  - ").append(e).append("\n"));
        }
        if (!extraInUI.isEmpty()) {
            sb.append("❌ Extra in UI:\n");
            extraInUI.forEach(e -> sb.append("  - ").append(e).append("\n"));
        }
        if (!typeMismatch.isEmpty()) {
            sb.append("❌ Type mismatch:\n");
            typeMismatch.forEach(e -> sb.append("  - ").append(e).append("\n"));
        }
        if (sb.length() == 0) {
            sb.append("✅ No mismatches found.");
        }
// 👉 Lưu kết quả vào TestNG để log vào Jira nếu fail
        ITestResult testResult = Reporter.getCurrentTestResult();
        testResult.setAttribute("checkResult", sb.toString());
//add steps vào
        String joinedSteps = String.join("\n", StepLogger.getSteps());
        testResult.setAttribute("steps", joinedSteps);
        // Ghi ra console và Allure 1 lần
        String detail = sb.toString();
        System.out.println(detail);
        Allure.addAttachment("Kết quả", "text/plain", detail);

        // Trả result về test để tiếp tục assert
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put("MissingInUI", missingInUI);
        result.put("ExtraInUI", extraInUI);
        result.put("TypeMismatch", typeMismatch);
        return result;
    }

    // Dttcam 02/07/2025 them ham check runMultiPicklistCheck
    public static void runMultiPicklistCheck(WebDriver driver, String objectName) {
        Map<String, List<List<String>>> valuesMap = getAndCheckFieldMultiPicklist(driver, objectName);
        List<String> failedFields = new ArrayList<>();
        Map<String, List<String>> duplicateMap = new LinkedHashMap<>();

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<List<String>>> entry : valuesMap.entrySet()) {
            String fieldName = entry.getKey();
            List<String> uiValues = entry.getValue().get(0);
            List<String> excelValues = entry.getValue().get(1);

            CheckResult res = comparePicklist(fieldName, uiValues, excelValues, duplicateMap);

            // Nếu có lỗi thì log chi tiết từng phần
            if (!res.getMissingInformation().isEmpty() || !res.getExtraInformation().isEmpty()) {
                sb.append("➤➤➤ Field: ").append(fieldName).append("\n");

                if (!res.getMissingInformation().isEmpty()) {
                    sb.append("  ❌ Missing values:\n");
                    res.getMissingInformation().forEach(v -> sb.append("    - ").append(v).append("\n"));
                }

                if (!res.getExtraInformation().isEmpty()) {
                    sb.append("  ❌ Extra values:\n");
                    res.getExtraInformation().forEach(v -> sb.append("    - ").append(v).append("\n"));
                }

                failedFields.add(fieldName);
            }
        }

        if (!duplicateMap.isEmpty()) {
            sb.append("⚠️ Warning: Duplicate Values Detected\n");
            for (Map.Entry<String, List<String>> entry : duplicateMap.entrySet()) {
                String fieldName = entry.getKey();
                String duplicates = String.join(", ", entry.getValue());
                sb.append("  ➤ Field '").append(fieldName).append("': ").append(duplicates).append("\n");
            }
            failedFields.addAll(duplicateMap.keySet());
        }

        // Nếu không có lỗi, log thông báo thành công
        if (failedFields.isEmpty()) {
            sb.append("✅ All picklist values matched.\n");
        }

        // 👉 Lưu kết quả vào TestNG để log vào Jira nếu fail
        ITestResult testResult = Reporter.getCurrentTestResult();
        testResult.setAttribute("checkResult", sb.toString());
//add steps vào
        String joinedSteps = String.join("\n", StepLogger.getSteps());
        testResult.setAttribute("steps", joinedSteps);

        // In ra console & Allure
        String detail = sb.toString();
        System.out.println(detail);
        Allure.addAttachment("Kết quả", "text/plain", detail);

        // Fail nếu có lỗi
        if (!failedFields.isEmpty()) {
            Assert.fail("❌ Case failed in field(s): " + failedFields);
        }
    }

    // hàm hỗ trợ click field picklist nvthong
    public static void clickFieldPicklist(WebDriver driver, String elementField) {
        // Tạo XPath động với điều kiện OR
        String xpath = String.format(
                ".//button[@aria-label='%s']",
                elementField
        );
        // Tìm phần tử tương ứng
        WebElement fieldPicklist = driver.findElement(By.xpath(xpath));
        // Sử dụng JavascriptExecutor để click phần tử
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        executor.executeScript("arguments[0].click();", fieldPicklist);
    }

    // hàm xác định thẻ div chứa value picklist nvthong
    public static List<String> getValuesPicklist(WebDriver driver, String divElementField) {
        List<String> picklistValues = new ArrayList<>();
        try {
            String xpath = String.format(".//div[@aria-label='%s']", divElementField);
            WebElement fieldPicklist = driver.findElement(By.xpath(xpath));

            JavascriptExecutor js = (JavascriptExecutor) driver;

            int previousItemCount = -1;
            long previousScrollTop = -1;
            int stableTries = 0;
            int maxStableTries = 10; // ✅ tăng để không dừng sớm

            while (stableTries < maxStableTries) {
                List<WebElement> items = fieldPicklist.findElements(By.cssSelector("lightning-base-combobox-item"));
                int currentItemCount = items.size();
                long currentScrollTop = ((Number) js.executeScript("return arguments[0].scrollTop;", fieldPicklist)).longValue();

                // ✅ Nếu không thêm item và không scroll xuống nữa → xem như ổn định
                if (currentItemCount == previousItemCount && currentScrollTop == previousScrollTop) {
                    stableTries++;
                } else {
                    stableTries = 0;
                }

                previousItemCount = currentItemCount;
                previousScrollTop = currentScrollTop;

                js.executeScript("arguments[0].scrollTop += 200;", fieldPicklist);
                Thread.sleep(500);
            }

            // Sau khi scroll hết → lấy toàn bộ
            List<WebElement> finalItems = fieldPicklist.findElements(By.cssSelector("lightning-base-combobox-item"));
            for (WebElement item : finalItems) {
                String text = item.getText().trim();
                if (!text.isEmpty()) {
                    picklistValues.add(text);
                }
            }

        } catch (Exception e) {
            System.out.println("⚠️ Error getting picklist values: " + e.getMessage());
        }
        return picklistValues;
    }


    // picklist value nvthong
    public static Map<String, List<List<String>>> getAndCheckFieldPicklist(WebDriver driver, String objectName) {
        Map<String, List<List<String>>> result = new HashMap<>();
        String sheetName = "PicklistValue";
        try {
            List<String> expectedFields = readPicklistField(sheetName, objectName);
            List<WebElement> elementFieldPicklist = driver.findElements(By.xpath("//div[@part='combobox']/label"));

            for (WebElement field : elementFieldPicklist) {
                String labelText = field.getText().trim();
                String cleanText = cleanLabel(labelText);
                if (!cleanText.isEmpty() && expectedFields.contains(cleanText)) {
                    WebElement comboBoxContainer = field.findElement(By.xpath("ancestor::div[@part='combobox']"));
                    WebElement defaultValueElement = comboBoxContainer.findElement(
                            By.xpath(String.format(
                                    ".//input[@aria-label=\"%s\"] | .//button[@aria-label=\"%s\"]/span"
                                    ,
                                    cleanText, cleanText
                            ))
                    );
                    String defaultValueUI = defaultValueElement.getText().trim();

                    clickFieldPicklist(driver, cleanText);
                    sleep(1500);
                    List<String> uiPicklistValues = getValuesPicklist(driver, cleanText);
                    List<String> excelPicklistValues = readPicklistValues(sheetName, objectName, cleanText);
                    //String defaultValueExcel = readDefaultPicklistValue(sheetName, objectName, cleanText);
                    String defaultValueExcel = readDefaultPicklistValue(sheetName, objectName, cleanText);

                    List<String> defaultList = new ArrayList<>();
                    defaultList.add(defaultValueUI);
                    defaultList.add(defaultValueExcel);

                    List<List<String>> pair = new ArrayList<>();
                    pair.add(uiPicklistValues);
                    pair.add(excelPicklistValues);
                    pair.add(defaultList);

                    result.put(cleanText, pair);
                }
            }

        } catch (Exception e) {
            System.out.println("⚠️ Error: " + e.getMessage());
        }

        return result;
    }

    //picklist dependency nvthong
    private static String findFieldLevel2(String objectName, String fieldLevel1, String valueLevel1,
                                          Map<String, String> fieldLevel2Map) {
        String key = objectName + "|" + fieldLevel1 + "|" + valueLevel1;
        return fieldLevel2Map.getOrDefault(key, null);
    }

    //Dttcam 02/07/2025 update in allure, chỉ in sai
    public static Map<String, Map<String, List<String>>> getPicklistDependencyFromUI(WebDriver driver, String objectName) {
        //String sheetName = "PicklistDependencyEN";
        // 👉 Tự động chọn sheet theo ngôn ngữ
        //String sheetName = _language.equalsIgnoreCase("vn") ? "PicklistDependencyVN" : "PicklistDependencyEN";
        Map<String, Map<String, List<String>>> expectedMap = readPicklistDependencyFromExcel(objectName);
        Map<String, String> fieldLevel2Map = buildFieldLevel2MapFromExcel(objectName);
        Map<String, Map<String, List<String>>> actualMap = new HashMap<>();

        for (String fieldLevel1 : expectedMap.keySet()) {
            Map<String, List<String>> valueMap = expectedMap.get(fieldLevel1);

            for (String valueLevel1 : valueMap.keySet()) {
                try {
                    // MỞ dropdown của field level 1
                    clickFieldPicklist(driver, fieldLevel1);
                    sleep(1000);
                    String xpath = String.format(
                            ".//div[@aria-label='%s']",
                            fieldLevel1
                    );
                    // Tìm phần tử tương ứng
                    WebElement fieldPicklist = driver.findElement(By.xpath(xpath));
                    // Tìm và chọn item tương ứng với valueLevel1
                    List<WebElement> items = fieldPicklist.findElements(By.cssSelector("lightning-base-combobox-item"));
                    boolean selected = false;
                    for (WebElement item : items) {
                        if (item.getText().trim().equalsIgnoreCase(valueLevel1)) {
                            item.click();
                            selected = true;
                            break;
                        }
                    }

                    if (!selected) {
                        System.out.printf("⚠️ Không tìm thấy giá trị '%s' trong field '%s'%n", valueLevel1, fieldLevel1);
                        continue;
                    }

                    sleep(1500); // đợi load field level 2

                    // Xác định field level 2
                    String fieldLevel2 = findFieldLevel2(objectName, fieldLevel1, valueLevel1, fieldLevel2Map);

                    //Dùng actualMap, không phải fieldLevel2Map
                    Map<String, List<String>> actualSubMap = actualMap.computeIfAbsent(fieldLevel1, k -> new HashMap<>());

                    if (fieldLevel2 == null) {
                        actualSubMap.put(valueLevel1, new ArrayList<>());
                        continue;
                    }

                    WebElement field2Button = driver.findElement(By.xpath(String.format(".//button[@aria-label='%s']", fieldLevel2)));

                    //Kiểm tra có bị disable không
                    String isDisabled = field2Button.getAttribute("aria-disabled");

                    if ("true".equalsIgnoreCase(isDisabled)) {
                        //System.out.printf("   ⚠️ '%s' is disabled after selecting '%s'%n", fieldLevel2, valueLevel1);
                        actualSubMap.put(valueLevel1, new ArrayList<>());
                    } else {
                        // Click và lấy giá trị thực tế
                        clickFieldPicklist(driver, fieldLevel2);
                        sleep(1000); // Hoặc WebDriverWait nếu bạn dùng

                        List<String> uiValues = getValuesPicklist(driver, fieldLevel2);

                        if (uiValues.isEmpty()) {
                            System.out.printf("   ⚠️ No values loaded for '%s' after selecting '%s'%n", fieldLevel2, valueLevel1);
                        }

                        actualSubMap.put(valueLevel1, uiValues);
                    }

                } catch (Exception e) {
                    System.out.printf("⚠️ Lỗi khi kiểm tra %s -> %s: %s%n", fieldLevel1, valueLevel1, e.getMessage());
                }
            }
        }

        return actualMap;
    }

    //hàm compare picklist dependency nvthong
    public static void comparePicklistDependencies(
            Map<String, Map<String, List<String>>> expectedMap,
            Map<String, Map<String, List<String>>> actualMap
    ) {
        boolean hasMismatch = false;
        StringBuilder log = new StringBuilder();
        for (String fieldLevel1 : expectedMap.keySet()) {
            Map<String, List<String>> expectedValueMap = expectedMap.get(fieldLevel1);
            Map<String, List<String>> actualValueMap = actualMap.getOrDefault(fieldLevel1, new HashMap<>());

            for (String valueLevel1 : expectedValueMap.keySet()) {
                List<String> expectedValues = expectedValueMap.getOrDefault(valueLevel1, new ArrayList<>());
                List<String> actualValues = actualValueMap.getOrDefault(valueLevel1, new ArrayList<>());

                // Tìm giá trị thiếu và dư
                List<String> missingInActual = new ArrayList<>(expectedValues);
                missingInActual.removeAll(actualValues);

                List<String> unexpectedInActual = new ArrayList<>(actualValues);
                unexpectedInActual.removeAll(expectedValues);

                if (!missingInActual.isEmpty() || !unexpectedInActual.isEmpty()) {
                    hasMismatch = true;
                    log.append(String.format("❌ Field: '%s'\n", fieldLevel1));
                    log.append(String.format("   ➥ Value Level 1: '%s'\n", valueLevel1));

                    if (!missingInActual.isEmpty()) {
                        log.append("      ⚠️ Missing Level 2 values:\n");
                        missingInActual.forEach(v -> log.append("         - ").append(v).append("\n"));
                    }

                    if (!unexpectedInActual.isEmpty()) {
                        log.append("      ⚠️ Extra Level 2 values:\n");
                        unexpectedInActual.forEach(v -> log.append("         - ").append(v).append("\n"));
                    }
                }
            }
        }

        if (!hasMismatch) {
            log.append("✅ All picklist dependencies matched expected values.\n");
        }
        // 👉 Lưu kết quả vào TestNG để log vào bug Jira nếu fail
        ITestResult testResult = Reporter.getCurrentTestResult();
        testResult.setAttribute("checkResult", log.toString());
        //add steps vào bug jira
        String joinedSteps = String.join("\n", StepLogger.getSteps());
        testResult.setAttribute("steps", joinedSteps);

        String detail = log.toString();
        System.out.println(detail);
        Allure.addAttachment("Kết quả", "text/plain", detail);

        if (hasMismatch) {
            Assert.fail("❌ Picklist dependencies mismatched. See details in console or Allure.");
        }
    }

    // read profile permission thực tế
    public static Map<String, Map<String, Boolean>> readActualPermissions(WebDriver driver) {
        Map<String, Map<String, Boolean>> actual = new LinkedHashMap<>();

        try {
            driver.switchTo().frame(0);

            List<WebElement> objectHeaders = driver.findElements(By.xpath("//th[@class='labelCol' and @scope='row']"));

            for (WebElement th : objectHeaders) {
                String obj = th.getText().trim();
                try {
                    WebElement td = th.findElement(By.xpath("following-sibling::td[1]"));
                    WebElement crudTable = td.findElement(By.xpath(".//table[@class='crudTable']"));
                    List<WebElement> crudImgs = crudTable.findElements(By.xpath(".//img"));

                    Map<String, Boolean> permissionMap = new LinkedHashMap<>();

                    for (WebElement img : crudImgs) {
                        String id = img.getAttribute("id"); // ví dụ: crudRead___Account
                        String src = img.getAttribute("src"); // chứa "check" nếu có quyền

                        if (id == null) continue;

                        String[] parts = id.split("___");
                        if (parts.length < 2) continue;

                        String actionKey = parts[0]; // crudRead,...
                        String actionLabel = convertAction(actionKey);
                        if (actionLabel == null) continue;

                        boolean isChecked = !src.toLowerCase().contains("uncheck");
                        permissionMap.put(actionLabel, isChecked);
                    }

                    actual.put(obj, permissionMap);

                } catch (Exception e) {
                    System.out.println("⚠️ Skip object: " + obj + " (no permissions or hidden)");
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Error reading actual permissions: " + e.getMessage());
        }

        return actual;
    }

    // ✅ Convert crudRead, crudUpdate,... → Read, Edit,...
    private static String convertAction(String crudId) {
        switch (crudId) {
            case "crudRead": return "Read";
            case "crudCreate": return "Create";
            case "crudUpdate": return "Edit";
            case "crudDelete": return "Delete";
            default: return null;
        }
    }
    //compare object permission
    public static void compareProfilePermissions(String profileName,
                                                 Map<String, Map<String, Boolean>> expected,
                                                 Map<String, Map<String, Boolean>> actual) {
        Set<String> failedObjects = new LinkedHashSet<>();
        StringBuilder logBuilder = AllureReporter.getBuilder(); // sử dụng Allure builder nếu có

        logBuilder.append("▶️ Profile: ").append(profileName).append("\n");

        for (String objectName : expected.keySet()) {
            Map<String, Boolean> expectedActions = expected.get(objectName);
            Map<String, Boolean> actualActions = actual.getOrDefault(objectName, new LinkedHashMap<>());

            StringBuilder objLog = new StringBuilder();
            boolean failed = false;

            objLog.append("➤ Object '").append(objectName).append("':\n");

            for (String action : List.of("Read", "Create", "Edit", "Delete")) {
                boolean exp = expectedActions.getOrDefault(action, false);
                boolean act = actualActions.getOrDefault(action, false);

                if (exp != act) {
                    objLog.append(String.format("  ❌ %s: Expected = %s | Actual = %s%n", action, exp, act));
                    failed = true;
                } else {
                    objLog.append(String.format("  ✅ %s: Match%n", action));
                }
            }

            if (failed) {
                logBuilder.append(objLog).append("\n");
                failedObjects.add(objectName);
            }
        }

        // Gắn log vào báo cáo Allure/TestNG
        ITestResult testResult = Reporter.getCurrentTestResult();
        testResult.setAttribute("checkResult", logBuilder.toString());

        String joinedSteps = String.join("\n", StepLogger.getSteps());
        testResult.setAttribute("steps", joinedSteps);

        System.out.println(logBuilder); // In ra console nếu cần
        AllureReporter.flush("Kết quả:");

        if (!failedObjects.isEmpty()) {
            Assert.fail("❌ Case failed tại object(s): " + failedObjects);
        }
    }
    // Get Data Type
    public static Map<String, List<List<String>>> getFieldInfoFromUI(WebDriver driver) {
        Map<String, List<List<String>>> uiMap = new LinkedHashMap<>();
        List<WebElement> rows = driver.findElements(By.xpath(".//table//tbody/tr"));
        for (WebElement row : rows) {
            try {
                String label = row.findElement(By.xpath(".//td[1]//span")).getText().trim();
                String apiName = row.findElement(By.xpath(".//td[2]//span")).getText().trim();
                String type = row.findElement(By.xpath(".//td[3]//span")).getText().trim();

                if (!uiMap.containsKey(label)) {
                    uiMap.put(label, new ArrayList<>());
                }
                uiMap.get(label).add(List.of(apiName, type));

            } catch (Exception ignored) {}
        }

        return uiMap;
    }
    // compare Datatype
    public static void compareFieldInfoByLabel(
            Map<String, List<String>> excelMap,                        // Label → [api, type]
            Map<String, List<List<String>>> uiMap                      // Label → List<[api, type]>
    ) {
        Set<String> failedLabels = new LinkedHashSet<>();
        StringBuilder logBuilder = AllureReporter.getBuilder(); // dùng Allure builder nếu có

        for (Map.Entry<String, List<String>> entry : excelMap.entrySet()) {
            String label = entry.getKey();
            List<String> expected = entry.getValue(); // [api, type]
            String expectedApi = expected.get(0);
            String expectedType = expected.get(1);

            if (!uiMap.containsKey(label)) {
                logBuilder.append(String.format("❌ Label '%s' không tìm thấy trên UI (API: %s)%n", label, expectedApi));
                failedLabels.add(label);
                continue;
            }

            List<List<String>> candidates = uiMap.get(label);
            boolean apiMatched = false;

            for (List<String> candidate : candidates) {
                String actualApi = candidate.get(0);
                String actualType = candidate.get(1);

                if (expectedApi.equals(actualApi)) {
                    apiMatched = true;
                    if (!expectedType.equalsIgnoreCase(actualType)) {
                        logBuilder.append(String.format("❌ Label '%s': Kiểu dữ liệu sai → Expect = %s | UI = %s%n", label, expectedType, actualType));
                        failedLabels.add(label);
                    }
                    break;
                }
            }

            if (!apiMatched) {
                String uiApis = candidates.stream().map(c -> c.get(0)).collect(Collectors.joining(", "));
                logBuilder.append(String.format("❌ Label '%s': API mismatch → Expect = %s | UI = [%s]%n", label, expectedApi, uiApis));
                failedLabels.add(label);
            }
        }
        if (failedLabels.isEmpty()){
            logBuilder.append("✅ Case Passed.\n");
        }

        // Gắn log vào báo cáo Allure/TestNG
        ITestResult testResult = Reporter.getCurrentTestResult();
        testResult.setAttribute("checkResult", logBuilder.toString());
        testResult.setAttribute("steps", String.join("\n", StepLogger.getSteps()));

        System.out.println(logBuilder); // In ra console
        AllureReporter.flush("Kết quả:");

        if (!failedLabels.isEmpty()) {
            Assert.fail("❌ Case failed tại field(s): " + failedLabels);
        }
    }


}
