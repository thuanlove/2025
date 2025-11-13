package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {
    private static Properties properties;

    static {
        try {
            properties = new Properties();
            FileInputStream file = new FileInputStream("src/main/resources/config.properties");
            properties.load(file);
        } catch (IOException e) {
            throw new RuntimeException("Configuration file not found!", e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}