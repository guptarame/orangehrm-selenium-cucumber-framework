package com.orangehrm.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads test configuration from src/test/resources/config.properties.
 * <p>
 * Any property can be overridden at runtime without touching the file, e.g.:
 * {@code mvn test -Dbrowser=firefox -Dheadless=true}
 * <p>
 * Implemented as a simple thread-safe singleton so the properties file is
 * parsed exactly once per test run.
 */
public final class ConfigReader {

    private static final String CONFIG_FILE = "config.properties";
    private static volatile ConfigReader instance;
    private final Properties properties;

    private ConfigReader() {
        properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new RuntimeException("Unable to locate " + CONFIG_FILE + " on the classpath");
            }
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file: " + CONFIG_FILE, e);
        }
    }

    public static ConfigReader getInstance() {
        if (instance == null) {
            synchronized (ConfigReader.class) {
                if (instance == null) {
                    instance = new ConfigReader();
                }
            }
        }
        return instance;
    }

    /**
     * Returns the property value, giving priority to a matching JVM system
     * property (-Dkey=value) over the value stored in config.properties.
     */
    public String get(String key) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null && !systemProperty.trim().isEmpty()) {
            return systemProperty.trim();
        }
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Missing required configuration property: " + key);
        }
        return value.trim();
    }

    public String get(String key, String defaultValue) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null && !systemProperty.trim().isEmpty()) {
            return systemProperty.trim();
        }
        return properties.getProperty(key, defaultValue).trim();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public int getInt(String key, int defaultValue) {
        return Integer.parseInt(get(key, String.valueOf(defaultValue)));
    }

    public String getBaseUrl() {
        return get("base.url");
    }

    public String getBrowser() {
        return get("browser", "chrome");
    }

    public boolean isHeadless() {
        return getBoolean("headless", false);
    }

    public int getImplicitWait() {
        return getInt("implicit.wait", 5);
    }

    public int getExplicitWait() {
        return getInt("explicit.wait", 15);
    }

    public int getPageLoadTimeout() {
        return getInt("page.load.timeout", 30);
    }

    public String getValidUsername() {
        return get("valid.username");
    }

    public String getValidPassword() {
        return get("valid.password");
    }
}
