package utils;

import java.io.*;

public class Counter {
    private static final String COUNTER_FILE = "src/test/resources/counter.txt";

    public static int incrementCounter() {
        int current = getCurrentCounter();
        current++;
        saveCounter(current);
        return current;
    }

    public static int getCurrentCounter() {
        try (BufferedReader reader = new BufferedReader(new FileReader(COUNTER_FILE))) {
            return Integer.parseInt(reader.readLine());
        } catch (IOException | NumberFormatException e) {
            return 0; // Default if file not found or empty
        }
    }

    private static void saveCounter(int value) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(COUNTER_FILE))) {
            writer.write(String.valueOf(value));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
