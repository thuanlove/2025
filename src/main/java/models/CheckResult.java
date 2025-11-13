//thông báo khác History tracking 10.06 nvthong
package models;

import java.util.ArrayList;
import java.util.List;

// Lưu kết quả so sánh giữa dữ liệu Excel và thực tế (UI)
public class CheckResult {
    private final List<String> missingInformation = new ArrayList<>();
    private final List<String> extraInformation = new ArrayList<>();
    private final List<String> matchedInformation = new ArrayList<>();

    public void addMissingInformation(String info) {
        missingInformation.add(info);
    }

    public void addExtraInformation(String info) {
        extraInformation.add(info);
    }

    public void addMatchedInformation(String info) {
        matchedInformation.add(info);
    }

    public List<String> getMissingInformation() {
        return new ArrayList<>(missingInformation);
    }

    public List<String> getExtraInformation() {
        return new ArrayList<>(extraInformation);
    }

    public List<String> getMatchedInformation() {
        return new ArrayList<>(matchedInformation);
    }

    @Override
    public String toString() {
        return "Missing Information: " + getMissingInformation() + "\n" +
                "Extra Information: " + getExtraInformation() + "\n" +
                "Matched Information: " + getMatchedInformation();
    }
}
