package models;

import java.util.*;

// Lưu kết quả so sánh giữa dữ liệu Excel và thực tế (UI)
public class UiCheckResult {

    // lưu các section bị thiếu khi so sánh UI và Excel
    private final List<String> missingSections = new ArrayList<>();
    // lưu các section thừa khi so sánh UI và Excel
    private final List<String> extraSections = new ArrayList<>();
    // lưu các section khớp giữa UI và Excel
    private final List<String> matchedSections = new ArrayList<>();

    // Trả về danh sách các section thiếu (so sánh UI và Excel)
    public List<String> getMissingSections() {
        return new ArrayList<>(missingSections);
    }

    // Trả về danh sách các section thừa (so sánh UI và Excel)
    public List<String> getExtraSections() {
        return new ArrayList<>(extraSections);
    }

    // Trả về danh sách các section đã khớp (so sánh UI và Excel)
    public List<String> getMatchedSections() {
        return new ArrayList<>(matchedSections);
    }

    // Thêm một section thiếu vào danh sách
    public void addMissingSection(String section) {
        missingSections.add(section);
    }

    // Thêm một section thừa vào danh sách
    public void addExtraSection(String section) {
        extraSections.add(section);
    }

    // Thêm một section đã khớp vào danh sách
    public void addMatchedSection(String section) {
        matchedSections.add(section);
    }

    // Dữ liệu lưu các label thiếu, thừa, khớp cho từng section
    private final Map<String, List<String>> missingLabelsPerSection = new LinkedHashMap<>();
    private final Map<String, List<String>> extraLabelsPerSection = new LinkedHashMap<>();
    private final Map<String, List<String>> matchedLabelsPerSection = new LinkedHashMap<>();

    // Trả về các label thiếu theo từng section
    public Map<String, List<String>> getMissingLabelsPerSection() {
        return missingLabelsPerSection;
    }

    // Trả về các label thừa theo từng section
    public Map<String, List<String>> getExtraLabelsPerSection() {
        return extraLabelsPerSection;
    }

    // Trả về các label khớp theo từng section
    public Map<String, List<String>> getMatchedLabelsPerSection() {
        return matchedLabelsPerSection;
    }
//2606 cam them

    @Override
    public String toString() {
        // Định dạng kết quả trả về dưới dạng chuỗi
        return "Matched Sections: " + getMatchedSections() + "\n" +
                "Missing Sections (Thiếu Section bỏ qua kiểm tra label): " + getMissingSections() + "\n" +
                "Extra Sections (Thừa Section bỏ qua kiểm tra label): " + getExtraSections() + "\n" +
                "Missing Labels: " + formatLabelsPerSection(getMissingLabelsPerSection()) + "\n" +
                "Extra Labels: " + formatLabelsPerSection(getExtraLabelsPerSection());
    }

    // Phương thức hỗ trợ để định dạng các label theo từng section cho dễ đọc
    private String formatLabelsPerSection(Map<String, List<String>> labelsPerSection) {
        StringBuilder sb = new StringBuilder();
        labelsPerSection.forEach((section, labels) -> {
            sb.append("\n  Section: ").append(section).append("\n");
            labels.forEach(label -> sb.append("    - ").append(label).append("\n"));
        });
        return sb.toString();
    }
}
