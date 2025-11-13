package utils;

import io.qameta.allure.Allure;

import java.util.ArrayList;
import java.util.List;

public class StepLogger {
    private static final ThreadLocal<List<String>> steps = ThreadLocal.withInitial(ArrayList::new);

    public static void step(String message) {
        Allure.step(message);
        steps.get().add(message);
    }

    public static List<String> getSteps() {
        return new ArrayList<>(steps.get());
    }

    public static void clear() {
        steps.get().clear();
    }
}
