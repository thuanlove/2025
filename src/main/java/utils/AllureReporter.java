package utils;

import io.qameta.allure.Allure;


public class AllureReporter {
    private static final ThreadLocal<StringBuilder> logBuilder = ThreadLocal.withInitial(StringBuilder::new);

    public static void append(String line) {
        logBuilder.get().append(line).append(System.lineSeparator());
    }

public static void append(String format, Object... args) {
    String msg = String.format(format, args);
    System.out.println(msg); // in ra console
    logBuilder.get().append(msg).append(System.lineSeparator());
}
    public static void flush(String title) {
        Allure.addAttachment(title, logBuilder.get().toString());
        logBuilder.remove(); // clean up for next test
    }

    public static StringBuilder getBuilder() {
        return logBuilder.get();
    }
}

