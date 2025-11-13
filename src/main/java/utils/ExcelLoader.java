// Refactored ExcelLoader with method to read API Name and Type
package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ExcelLoader {
    public static Workbook workbook;
    private static String _language;
    private static String _sheetName;

    public static void load(String path, String language, String sheetName) {
        try (FileInputStream file = new FileInputStream(path)) {
            workbook = new XSSFWorkbook(file);
            _language = language;
            _sheetName = sheetName;
        } catch (IOException e) {
            throw new RuntimeException("❌ Configuration file not found or invalid: " + path, e);
        }
    }

    public static String getLanguage() {
        return _language;
    }

    public static String getSheetName() {
        return _sheetName;
    }

    public static boolean isWorkbookLoaded() {
        return workbook != null;
    }

    public static String getUrl(String objectName, String recordTypeId, String actionName) {
        try {
            Sheet sheet = workbook.getSheet("URLs");
            if (sheet == null) throw new RuntimeException("❌ Sheet 'URLs' not found");
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    Cell cellObject = row.getCell(0);         // A - object
                    Cell cellRecordType = row.getCell(1);     // B - recordTypeId
                    Cell cellAction = row.getCell(2);         // C - action
                    if (cellObject == null || cellAction == null) continue;
                    boolean matchObject = objectName.equals(cellObject.getStringCellValue());
                    boolean matchAction = actionName.equals(cellAction.getStringCellValue());

                    // Cho phép recordTypeId = null => bỏ qua check recordType
                    boolean matchRecordType = (recordTypeId == null ||
                            (cellRecordType != null && recordTypeId.equals(cellRecordType.getStringCellValue())));

                    if (matchObject && matchAction && matchRecordType) {
                        Cell resultCell = row.getCell(3);     // D - URL
                        return resultCell != null ? resultCell.getStringCellValue() : "";
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error reading URL: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public static String getCommonValue(String labelKey) {
        try {
            Sheet sheet = workbook.getSheet("Common");
            if (sheet == null) throw new RuntimeException("❌ Sheet 'Common' not found");

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    Cell cell = row.getCell(0);
                    if (cell != null && labelKey.equals(cell.getStringCellValue())) {
                        return _language.equals("vn") ? row.getCell(2).getStringCellValue() : row.getCell(1).getStringCellValue();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error reading Common sheet: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public static Map<String, List<String>> readLabelsBySection() {
        Map<String, List<String>> sectionMap = new LinkedHashMap<>();
        try {
            Sheet sheet = workbook.getSheet(_sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet '" + _sheetName + "' not found");
            int columnIndex = _language.equals("vn") ? 2 : 1;
            String currentSection = "No Section";
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    // Nếu cột D (index 3) là 'x' thì bỏ qua
                    Cell skipCell = row.getCell(3);
                    if (skipCell != null && "x".equalsIgnoreCase(skipCell.getStringCellValue().trim())) {
                        continue;
                    }
                    Cell cell = row.getCell(columnIndex);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String label = cell.getStringCellValue().trim();
                        if (label.isEmpty()) continue;

                        boolean isBold = workbook.getFontAt(cell.getCellStyle().getFontIndex()).getBold();
                        if (isBold) {
                            currentSection = label;
                            sectionMap.putIfAbsent(currentSection, new ArrayList<>());
                        } else {
                            sectionMap.computeIfAbsent(currentSection, k -> new ArrayList<>()).add(label);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error reading Excel: " + e.getMessage());
        }
        return sectionMap;
    }

    public static Map<String, List<String>> readHistoryTrackingLabelsBySection() {
        return readLabelsBySection();
    }

    public static Map<String, List<String>> readFieldInfoFromExcel() {
        Map<String, List<String>> fieldMap = new LinkedHashMap<>();
        try {
            Sheet sheet = workbook.getSheet(_sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet '" + _sheetName + "' not found");
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    Cell apiCell = row.getCell(0);      // A - API Name
                    Cell labelCell = row.getCell(1);    // B - fields.en
                    Cell typeCell = row.getCell(5);     // F - Type

                    boolean valid = apiCell != null && labelCell != null && typeCell != null &&
                            apiCell.getCellType() == CellType.STRING &&
                            labelCell.getCellType() == CellType.STRING &&
                            typeCell.getCellType() == CellType.STRING;

                    if (!valid) continue;

                    String label = labelCell.getStringCellValue().trim();
                    String apiName = apiCell.getStringCellValue().trim();
                    String type = typeCell.getStringCellValue().trim();

                    // ✅ Chỉ lấy nếu có label và API là custom (__c)
                    if (!label.isEmpty() && !apiName.isEmpty() && apiName.endsWith("__c")) {
                        fieldMap.put(label, List.of(apiName, type));
                    }
                }
            }

            /*
            // ✅ In ra console kết quả đọc được
            System.out.println("📄 Danh sách field đọc từ Excel:");
            for (Map.Entry<String, List<String>> entry : fieldMap.entrySet()) {
                String label = entry.getKey();
                String apiName = entry.getValue().get(0);
                String type = entry.getValue().get(1);
                System.out.printf("  - Label: %-30s | API: %-25s | Type: %s%n", label, apiName, type);
            }
*/
        } catch (Exception e) {
            System.err.println("⚠️ Error reading Excel field info: " + e.getMessage());
            e.printStackTrace();
        }
        return fieldMap;
    }

    // ✅ New method to read API Name and Type
    public static Map<String, String> readApiNameAndType() {
        Map<String, String> apiMap = new LinkedHashMap<>();
        try {
            Sheet sheet = workbook.getSheet(_sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet '" + _sheetName + "' not found");
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    Cell apiCell = row.getCell(0);
                    Cell typeCell = row.getCell(5);
                    if (apiCell != null && typeCell != null &&
                            apiCell.getCellType() == CellType.STRING &&
                            typeCell.getCellType() == CellType.STRING &&
                            !apiCell.getStringCellValue().trim().isEmpty()) {

                        apiMap.put(apiCell.getStringCellValue().trim(), typeCell.getStringCellValue().trim());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error reading API Name and Type: " + e.getMessage());
            e.printStackTrace();
        }
        return apiMap;
    }

    //getFieldLabel 1006 nvthong
    public static String getFieldLabel(String labelKey) {
        if (ExcelLoader._sheetName == null) {
            throw new RuntimeException("❌ Sheet chưa được set. Gọi setCurrentSheet(...) trước khi gọi getExcelLabelField.");
        }
        try {
            Sheet sheet = workbook.getSheet(ExcelLoader._sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet '" + ExcelLoader._sheetName + "' not found");

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    // Nếu cột D (index 3) là 'x' thì bỏ qua
                    Cell skipCell = row.getCell(3);
                    if (skipCell != null && "x".equalsIgnoreCase(skipCell.getStringCellValue().trim())) {
                        continue;
                    }
                    Cell cell = row.getCell(0);
                    if (cell != null && labelKey.equals(cell.getStringCellValue())) {
                        return _language.equals("vn") ? row.getCell(2).getStringCellValue() : row.getCell(1).getStringCellValue();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error reading sheet '" + ExcelLoader._sheetName + "': " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    // đọc history 1006 nvthong
    public static Map<String, List<String>> readFieldHistory() {
        Map<String, List<String>> sectionMap = new LinkedHashMap<>();
        List<String> fieldHistoryLabels = new ArrayList<>();
        try {
            Sheet sheet = workbook.getSheet(ExcelLoader._sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet '" + ExcelLoader._sheetName + "' not found");
            int columnIndex = ExcelLoader._language.equals("vn") ? 2 : 1;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) { // đọc từ dòng 2
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    // Kiểm tra cột H (index 7) có 'x' hay không
                    Cell flagCell = row.getCell(7);
                    if (flagCell != null && "x".equalsIgnoreCase(flagCell.getStringCellValue().trim())) {
                        Cell cell = row.getCell(columnIndex);
                        if (cell != null && cell.getCellType() == CellType.STRING) {
                            String label = cell.getStringCellValue().trim();
                            if (!label.isEmpty()) {
                                fieldHistoryLabels.add(label);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error reading Excel: " + e.getMessage());
        }
        sectionMap.put("Field History", fieldHistoryLabels);
        return sectionMap;
    }

    //2006 camdtt update tách readpicklistvalue, default value ra riêng 2 hàm
    public static List<String> readPicklistValues(String sheetName, String objectName, String field) {
        List<String> values = new ArrayList<>();
        try {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet '" + sheetName + "' not found");

            int valueColumnIndex = ExcelLoader._language.equalsIgnoreCase("vn") ? 4 : 3;
            int fieldColumnIndex = ExcelLoader._language.equalsIgnoreCase("vn") ? 2 : 1;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                Cell objectCell = row.getCell(0);
                Cell fieldCell = row.getCell(fieldColumnIndex);
                Cell valueCell = row.getCell(valueColumnIndex);

                if (objectCell == null || fieldCell == null || valueCell == null) continue;

                String objectCellValue = objectCell.getStringCellValue().trim();
                String fieldCellValue = fieldCell.getStringCellValue().trim();

                if (objectName.equalsIgnoreCase(objectCellValue) && field.equalsIgnoreCase(fieldCellValue)) {
                    String picklistValue = valueCell.toString().trim();
                    if (!picklistValue.isEmpty()) {
                        values.add(picklistValue);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error reading Excel: " + e.getMessage());
        }
        return values;
    }

    //get danh sách field picklist từ file excel 1006 nvthong
    public static List<String> readPicklistField(String sheetName, String objectName) {
        List<String> fieldsFromExcel = new ArrayList<>();
        try {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet '" + sheetName + "' not found");

            int fieldIndex = ExcelLoader._language.equalsIgnoreCase("vn") ? 2 : 1;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                Cell objectCell = row.getCell(0);
                Cell fieldCell = row.getCell(fieldIndex);

                if (objectCell == null || fieldCell == null) continue;

                String objectCellValue = objectCell.getStringCellValue().trim();
                String fieldCellValue = fieldCell.getStringCellValue().trim();

                if (objectName.equalsIgnoreCase(objectCellValue) && !fieldCellValue.isEmpty()) {
                    fieldsFromExcel.add(fieldCellValue);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error reading Excel field list: " + e.getMessage());
        }
        return fieldsFromExcel;
    }

    //2306 cam update trùng giá trị default
    public static String readDefaultPicklistValue(String sheetName, String objectName, String field) {
        String foundDefault = null;
        int defaultCount = 0;
        StringBuilder log = AllureReporter.getBuilder();

        try {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException(String.format("❌ Sheet '%s' not found", sheetName));
            }

            int fieldCol = _language.equalsIgnoreCase("vn") ? 2 : 1;
            int defaultCol = _language.equalsIgnoreCase("vn") ? 6 : 5;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell objectCell = row.getCell(0);
                Cell fieldCell = row.getCell(fieldCol);
                Cell defaultCell = row.getCell(defaultCol);

                if (objectCell == null || fieldCell == null || defaultCell == null) continue;

                String objectVal = objectCell.toString().trim();
                String fieldVal = fieldCell.toString().trim();
                String defaultVal = defaultCell.toString().trim();

                if (!objectName.equalsIgnoreCase(objectVal)
                        || !field.equalsIgnoreCase(fieldVal)
                        || defaultVal.isEmpty()) {
                    continue;
                }

                defaultCount++;
                if (foundDefault == null) {
                    foundDefault = defaultVal;
                } else if (!foundDefault.equals(defaultVal)) {
                    log.append(String.format(
                            "❌ Field '%s' on data test has multiple default values: '%s' and '%s'\n",
                            field, foundDefault, defaultVal
                    ));
                }
            }

        } catch (Exception e) {
            System.out.println("⚠️ " + e.getMessage());
        }

        return foundDefault != null ? foundDefault : "";
    }

    // read picklist dependency 1006 nvthong
    public static Map<String, Map<String, List<String>>> readPicklistDependencyFromExcel(String objectName) {
        Map<String, Map<String, List<String>>> dependencyMap = new HashMap<>();
        // 👉 Tự động chọn sheet theo ngôn ngữ
        String sheetName = _language.equalsIgnoreCase("vn") ? "PicklistDependencyVN" : "PicklistDependencyEN";
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) throw new RuntimeException("❌ Sheet '" + sheetName + "' not found");

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String objName = getCellString(row.getCell(0));
            String fieldLevel1 = getCellString(row.getCell(1));
            String valueLevel1 = getCellString(row.getCell(2));
            String fieldLevel2 = getCellString(row.getCell(3));
            String valueLevel2 = getCellString(row.getCell(4));

            if (!objectName.equalsIgnoreCase(objName)) continue;
            if (fieldLevel1.isEmpty() || valueLevel1.isEmpty()) continue;
            // Tạo map con nếu chưa có
            dependencyMap.putIfAbsent(fieldLevel1, new HashMap<>());
            Map<String, List<String>> valueMap = dependencyMap.get(fieldLevel1);

            valueMap.putIfAbsent(valueLevel1, new ArrayList<>());

            // Chỉ thêm value level 2 nếu có
            if (!valueLevel2.isEmpty()) {
                valueMap.get(valueLevel1).add(valueLevel2);
            }
        }
        return dependencyMap;
    }
    // Helper 1006 nvthong
private static String getCellString(Cell cell) {
    return (cell == null) ? "" : cell.getStringCellValue().trim();
}
    public static Map<String, String> buildFieldLevel2MapFromExcel(String objectName) {
        Map<String, String> fieldLevel2Map = new HashMap<>();
// 👉 Tự động chọn sheet theo ngôn ngữ
        String sheetName = _language.equalsIgnoreCase("vn") ? "PicklistDependencyVN" : "PicklistDependencyEN";
        System.out.println("sheet đọc: "+ sheetName);
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) throw new RuntimeException("❌ Sheet '" + sheetName + "' not found");

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String objName = getCellString(row.getCell(0));
            String fieldLevel1 = getCellString(row.getCell(1));
            String valueLevel1 = getCellString(row.getCell(2));
            String fieldLevel2 = getCellString(row.getCell(3));

            if (!objectName.equalsIgnoreCase(objName)) continue;
            if (fieldLevel1.isEmpty() || valueLevel1.isEmpty() || fieldLevel2.isEmpty()) continue;

            String key = objectName + "|" + fieldLevel1 + "|" + valueLevel1;
            fieldLevel2Map.putIfAbsent(key, fieldLevel2);
        }

        return fieldLevel2Map;
    }

    // đọc field required
    public static Map<String, List<String>> readFieldRequired() {
        Map<String, List<String>> sectionMap = new LinkedHashMap<>();
        List<String> labelFieldRequired = new ArrayList<>();
        try {
            Sheet sheet = workbook.getSheet(_sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet '" + _sheetName + "' not found");
            int columnIndex = _language.equals("vn") ? 2 : 1;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) { // đọc từ dòng 2
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    // Kiểm tra cột H (index 6) có 'x' hay không
                    Cell flagCell = row.getCell(6);
                    if (flagCell != null && "x".equalsIgnoreCase(flagCell.getStringCellValue().trim())) {
                        Cell cell = row.getCell(columnIndex);
                        if (cell != null && cell.getCellType() == CellType.STRING) {
                            String label = cell.getStringCellValue().trim();
                            if (!label.isEmpty()) {
                                labelFieldRequired.add(label);
                                //System.out.println("Field Required: "+ label);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error reading Excel: " + e.getMessage());
        }
        sectionMap.put("Field Required", labelFieldRequired);
        return sectionMap;
    }
    //get danh sách field multi picklist từ file excel
    public static List<String> readMultiPicklist(String sheetName, String objectName) {
        List<String> fieldsFromExcel = new ArrayList<>();
        try {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet '" + sheetName + "' not found");

            int fieldIndex = _language.equalsIgnoreCase("vn") ? 2 : 1;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                Cell objectCell = row.getCell(0);
                Cell fieldCell = row.getCell(fieldIndex);

                if (objectCell == null || fieldCell == null) continue;

                String objectCellValue = objectCell.getStringCellValue().trim();
                String fieldCellValue = fieldCell.getStringCellValue().trim();

                if (objectName.equalsIgnoreCase(objectCellValue) && !fieldCellValue.isEmpty()) {
                    fieldsFromExcel.add(fieldCellValue);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error reading Excel field list: " + e.getMessage());
        }
        return fieldsFromExcel;
    }
    // read field multi picklist- nvthong 16062025
    public static List<String> readMultiPicklistField(String sheetName, String objectName) {
        List<String> fieldsFromExcel = new ArrayList<>();
        try {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet '" + sheetName + "' not found");

            int fieldIndex = _language.equalsIgnoreCase("vn") ? 2 : 1;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                Cell objectCell = row.getCell(0);
                Cell fieldCell = row.getCell(fieldIndex);

                if (objectCell == null || fieldCell == null) continue;

                String objectCellValue = objectCell.getStringCellValue().trim();
                String fieldCellValue = fieldCell.getStringCellValue().trim();

                if (objectName.equalsIgnoreCase(objectCellValue) && !fieldCellValue.isEmpty()) {
                    fieldsFromExcel.add(fieldCellValue);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error reading Excel field list: " + e.getMessage());
        }
        return fieldsFromExcel;
    }
    //get danh sách field multi picklist từ file excel _ nvthong 16062025
    public static List<String> readMultiPicklistValues(String sheetName, String objectName, String fieldName) {
        List<String> values = new ArrayList<>();
        try {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet '" + sheetName + "' not found");

            int fieldIndex = _language.equalsIgnoreCase("vn") ? 2 : 1;
            int valueIndex =  _language.equalsIgnoreCase("vn") ? 4 : 3;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                Cell objectCell = row.getCell(0);
                Cell fieldCell = row.getCell(fieldIndex);
                Cell valueCell = row.getCell(valueIndex);

                if (objectCell == null || fieldCell == null || valueCell == null) continue;

                String objectCellValue = objectCell.getStringCellValue().trim();
                String fieldCellValue = fieldCell.getStringCellValue().trim();
                String value = valueCell.getStringCellValue().trim();

                if (objectName.equalsIgnoreCase(objectCellValue)
                        && fieldName.equalsIgnoreCase(fieldCellValue)
                        && !value.isEmpty()) {
                    values.add(value);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error reading values for field " + fieldName + ": " + e.getMessage());
        }
        return values;
    }

    // get danh sách profile permission từ file excel
    public static Map<String, Map<String, Map<String, Boolean>>> readExpectedPermissions() {
        Map<String, Map<String, Map<String, Boolean>>> profilePermissionMap = new LinkedHashMap<>();

        try {
            Sheet sheet = workbook.getSheet(ExcelLoader._sheetName);
            if (sheet == null) throw new RuntimeException("❌ Sheet '" + ExcelLoader._sheetName + "' not found");

            // Đọc dòng tiêu đề
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new RuntimeException("❌ Header row not found");

            // Xác định vị trí cột
            Map<String, Integer> columnIndexMap = new HashMap<>();
            for (Cell cell : headerRow) {
                String col = cell.getStringCellValue().trim().toLowerCase();
                switch (col) {
                    case "profile name":
                        columnIndexMap.put("Profile Name", cell.getColumnIndex());
                        break;
                    case "object name":
                        columnIndexMap.put("Object Name", cell.getColumnIndex());
                        break;
                    case "read":
                        columnIndexMap.put("Read", cell.getColumnIndex());
                        break;
                    case "created":
                        columnIndexMap.put("Create", cell.getColumnIndex());
                        break;
                    case "update":
                        columnIndexMap.put("Edit", cell.getColumnIndex());
                        break;
                    case "delete":
                        columnIndexMap.put("Delete", cell.getColumnIndex());
                        break;
                }
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                // Lấy tên profile & object
                Cell profileCell = row.getCell(columnIndexMap.get("Profile Name"));
                Cell objectCell = row.getCell(columnIndexMap.get("Object Name"));
                if (profileCell == null || objectCell == null) continue;

                String profileName = profileCell.getStringCellValue().trim();
                String objectName = objectCell.getStringCellValue().trim();
                if (profileName.isEmpty() || objectName.isEmpty()) continue;

                // Lấy permissions
                Map<String, Boolean> permissions = new LinkedHashMap<>();
                for (String action : List.of("Read", "Create", "Edit", "Delete")) {
                    Integer colIdx = columnIndexMap.get(action);
                    if (colIdx == null) {
                        permissions.put(action, false);
                        continue;
                    }

                    Cell cell = row.getCell(colIdx);
                    boolean hasPermission = cell != null &&
                            cell.getCellType() == CellType.STRING &&
                            "x".equalsIgnoreCase(cell.getStringCellValue().trim());
                    permissions.put(action, hasPermission);
                }

                // Gộp vào map
                profilePermissionMap
                        .computeIfAbsent(profileName, k -> new LinkedHashMap<>())
                        .put(objectName, permissions);
            }

        } catch (Exception e) {
            System.out.println("⚠️ Error reading expected permissions: " + e.getMessage());
        }

        return profilePermissionMap;
    }


}
