package com.orangehrm.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
    private static final String DEMO_HOST = "opensource-demo.orangehrmlive.com";
    private static final String HTTPS_SCHEME = "https";
    private static final String ALLOW_INSECURE_BASE_URL_KEY = "allow.insecure.base.url";
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
     * Returns the property value. Resolution order: non-empty JVM system
     * property (-Dkey=value), non-empty environment variable (key upper-cased,
     * dots replaced by underscores, e.g. VALID_PASSWORD), then
     * config.properties.
     */
    public String get(String key) {
        String value = resolveOverride(key);
        if (value == null) {
            value = properties.getProperty(key);
        }
        if (value == null) {
            throw new RuntimeException("Missing required configuration property: " + key);
        }
        return value.trim();
    }

    public String get(String key, String defaultValue) {
        String value = resolveOverride(key);
        if (value == null) {
            value = properties.getProperty(key, defaultValue);
        }
        return value.trim();
    }

    /** Returns the non-empty -D or environment override for the key, else null. */
    private String resolveOverride(String key) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null && !systemProperty.trim().isEmpty()) {
            return systemProperty.trim();
        }
        String envValue = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        return null;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public int getInt(String key, int defaultValue) {
        return Integer.parseInt(get(key, String.valueOf(defaultValue)));
    }

    /**
     * Resolves base.url and fails closed unless its scheme is https (NFR-002).
     * This check is preventive, running before the browser ever navigates -
     * the executed scheme assertion in LoginSteps necessarily runs only after
     * LoginPage.login() has already typed and POSTed the password, so it
     * cannot by itself stop a cleartext submission. Opt out only via
     * allow.insecure.base.url for a local, non-TLS sandbox.
     */
    public String getBaseUrl() {
        String baseUrl = get("base.url");
        if (getBoolean(ALLOW_INSECURE_BASE_URL_KEY, false)) {
            return baseUrl;
        }
        String scheme = resolveScheme(baseUrl);
        if (!HTTPS_SCHEME.equalsIgnoreCase(scheme)) {
            String schemeDescription = scheme == null ? "missing or unparseable" : scheme;
            throw new IllegalStateException("base.url must use the https scheme, but its scheme is "
                    + schemeDescription + "; set " + ALLOW_INSECURE_BASE_URL_KEY
                    + "=true to opt out for a local, non-TLS sandbox only");
        }
        return baseUrl;
    }

    /** Returns the URL's scheme, or null for a malformed or scheme-less URL rather than throwing. */
    private static String resolveScheme(String url) {
        try {
            return URI.create(url).getScheme();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getBrowser() {
        return get("browser", "chrome");
    }

    public boolean isHeadless() {
        return getBoolean("headless", false);
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

    /**
     * The committed file password is a public demo value. It is only allowed
     * while base.url targets the public demo host; otherwise the password must
     * be supplied via -Dvalid.password or the VALID_PASSWORD environment
     * variable.
     */
    public String getValidPassword() {
        if (resolveOverride("valid.password") == null && !isDemoHost(getBaseUrl())) {
            throw new IllegalStateException("base.url is not the public demo host (" + DEMO_HOST
                    + "); supply valid.password via -Dvalid.password or the VALID_PASSWORD "
                    + "environment variable instead of the committed demo value");
        }
        return get("valid.password");
    }

    private static boolean isDemoHost(String url) {
        try {
            return DEMO_HOST.equalsIgnoreCase(URI.create(url).getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
