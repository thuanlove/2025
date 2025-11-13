package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.ExcelLoader;
import utils.Helper;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SFBasePage {
    public static WebDriver driver;
    protected static WebDriverWait wait;
    protected static JavascriptExecutor js;
    protected static Actions actions;
    protected Helper helper;

    public SFBasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.js = (JavascriptExecutor) driver;
        this.actions = new Actions(driver);
        this.helper = new Helper(driver);
    }

    public static void selectFromSearchbar(String labelKey, String value) {
        String label = ExcelLoader.getFieldLabel(labelKey);
        WebElement input = driver.findElement(
                By.xpath("//label[contains(text(), '" + label + "')]/following-sibling::div//input[@type='text']"));
        input.sendKeys(value);
//        WebElement firstOption = wait.until(ExpectedConditions.elementToBeClickable(
//                By.xpath("(//lightning-base-combobox-item[contains(., '" + value + "')])[2]"))
//        );
//        actions.click(firstOption).perform();

        List<WebElement> options = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.xpath("//lightning-base-combobox-item[contains(., '" + value + "')]")));

        if (!options.isEmpty()) {
            actions.moveToElement(options.get(1)).click().perform();
            pause(); // optional delay
        } else {
            throw new NoSuchElementException("No matching option found for value: " + value);
        }
        pause();
    }

    public static void fillTextField(String labelKey, String value) {
        String label = ExcelLoader.getFieldLabel(labelKey);
        WebElement field = driver.findElement(
                By.xpath("//label[contains(text(), '" + label + "')]/following-sibling::div//input[@type='text']")
        );
        js.executeScript("arguments[0].scrollIntoView(true);", field);
        field.clear();
        field.sendKeys(value);
        pause();
    }

     //hàm dùng cho 2 textfield trùng tên như 'Đường Phố'
    public static void fillTextField2(String labelKey, int order, String value) {
        String label = ExcelLoader.getFieldLabel(labelKey);
        String xpath = String.format(
                "(//label[normalize-space()='%s']/following-sibling::*//input | //label[normalize-space()='%s']/following-sibling::*//textarea)[%d]",
                label, label, order
        );
        WebElement field = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
        field.clear();
        field.sendKeys(value);
        pause();
    }

    //trùng 'Họ Tên'-tên session với 'Họ'-textbox
    public static void fillTextField1(String labelKey, String value) {
        String labels = ExcelLoader.getFieldLabel(labelKey); // "Họ|Last Name"
        String[] labelArr = labels.split("\\|");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement field = null;

        for (String label : labelArr) {
            try {
                String xpath = String.format("//label[contains(text(),'%s')]/following-sibling::div//input[@type='text']", label.trim());
                field = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
                if (field != null) break;  // tìm được thì dừng
            } catch (TimeoutException e) {// không tìm thấy với label này, thử tiếp label khác
            }
        }
        if (field == null) {
            throw new NoSuchElementException("Không tìm thấy field với label " + labels);
        }

        js.executeScript("arguments[0].scrollIntoView(true);", field);
        field.clear();
        field.sendKeys(value);
        pause();
    }

    public void fillAreaField(String labelKey, String value) {
        String label = ExcelLoader.getFieldLabel(labelKey);
        WebElement field = driver.findElement(
                By.xpath("//label[contains(text(), '" + label + "')]/following-sibling::div//textarea")
        );
        js.executeScript("arguments[0].scrollIntoView(true);", field);
        field.clear();
        field.sendKeys(value);
        pause();
    }

    protected void fillDateField(String xpath, LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");//sửa theo project vd: dd/MM/yyyy
        String formattedDate = date.format(formatter);
        WebElement field = driver.findElement(By.xpath(xpath));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", field);
        field.sendKeys(formattedDate);
        pause();
    }

    public void selectFromCombobox(String labelKey, String value) {
        //System.out.println("Label key truyền vào: [" + labelKey + "]"); //debug ko xoa
        String label = ExcelLoader.getFieldLabel(labelKey);
        //System.out.println("Label lấy từ Excel: [" + label + "]"); //debug ko xoa

        WebElement combobox = driver.findElement(By.xpath("//button[@aria-label='" + label + "']"));
        js.executeScript("arguments[0].click();", combobox);
        WebElement option = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//lightning-base-combobox-item[contains(., '" + value + "')]")));
        js.executeScript("arguments[0].click();", option);
        pause();
    }
    // hàm dùng cho 2 combobox field trùng tên như 'Quốc Gia'
    public void selectFromCombobox1(String labelKey, int order, String value) {
        String label = ExcelLoader.getFieldLabel(labelKey);
        // 1. Tìm combobox input theo label + thứ tự
        String xpath = String.format(
                "(//div[@part='combobox']//label[normalize-space()='%s']/following::input[@role='combobox'])[%d]",
                label, order
        );
        WebElement combobox = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
        js.executeScript("arguments[0].click();", combobox);

        // 2. Chờ dropdown xuất hiện và chọn option
        String optionXpath = String.format("//lightning-base-combobox-item[normalize-space()='%s']", value);
        WebElement option = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(optionXpath)));
        js.executeScript("arguments[0].click();", option);
        pause();
    }
    //Nhấn nút Lưu
    public void submitForm(String labelKey) {
        String label = ExcelLoader.getCommonValue(labelKey);
        WebElement saveButton = driver.findElement(By.xpath("//button[text()='" + label + "']"));
        js.executeScript("arguments[0].scrollIntoView(true);", saveButton);
        wait.until(ExpectedConditions.elementToBeClickable(saveButton)).click();
        pause();
    }

    // ✅ Hàm toast dùng chung
    protected String getToast() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        WebElement toast = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class, 'toastContent')]")
        ));
        return (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].innerText;", toast);
    }

    // ✅ So sánh toast
    protected boolean compareToast(String expected, String actual) {
        System.out.println("📌 Captured: " + actual);
        System.out.println("📌 Expected: " + expected);
        boolean result = expected.equals(actual);
        System.out.println(result ? "✅ Toast đúng" : "❌ Toast sai");
        return result;
    }

    protected boolean verifyToast(String fieldXpath, String prefix, String labelKey) {
        WebElement element = driver.findElement(By.xpath(fieldXpath));
        String actualText = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].innerText;", element);
        String label = ExcelLoader.getFieldLabel(labelKey);
        String expected = MessageFormat.format(label, prefix, actualText);
        String actualToast = getToast().split("\n")[0].trim();
        return compareToast(expected, actualToast);
    }

    protected boolean verifyCreatedMessage(String fieldXpath, String labelKey) {
        try {
            WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(fieldXpath)));

            String actualText = (String) js.executeScript("return arguments[0].innerText;", element);
            String label = ExcelLoader.getCommonValue(labelKey);
            String expected = MessageFormat.format(label, actualText);
            String actualToast = getToast().split("\n")[0].trim();
            return compareToast(expected, actualToast);
        } catch (Exception e) {
            System.out.println("❌ Could not locate field element for toast verification: " + e.getMessage());
            return false;
        }
    }

    private static void pause() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
