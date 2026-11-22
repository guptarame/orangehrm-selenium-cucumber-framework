package com.orangehrm.config;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * NFR-002: negative-path coverage for ConfigReader.getBaseUrl()'s fail-closed
 * HTTPS scheme guard (TASK-001).
 * <p>
 * Runs in the same Surefire JVM as the Cucumber scenarios (TASK-002), so
 * every system property mutated here is captured before each test and
 * restored afterwards - a leaked base.url or allow.insecure.base.url would
 * otherwise silently retarget or weaken the scenarios that run after it.
 */
public class ConfigReaderTest {

    private static final String BASE_URL_KEY = "base.url";
    private static final String ALLOW_INSECURE_KEY = "allow.insecure.base.url";
    private static final String VALID_PASSWORD_KEY = "valid.password";
    private static final String DEMO_BASE_URL = "https://opensource-demo.orangehrmlive.com/web/index.php/auth/login";

    private String originalBaseUrl;
    private String originalAllowInsecure;
    private String originalValidPassword;

    @Before
    public void captureOverrides() {
        originalBaseUrl = System.getProperty(BASE_URL_KEY);
        originalAllowInsecure = System.getProperty(ALLOW_INSECURE_KEY);
        originalValidPassword = System.getProperty(VALID_PASSWORD_KEY);
    }

    @After
    public void restoreOverrides() {
        restoreProperty(BASE_URL_KEY, originalBaseUrl);
        restoreProperty(ALLOW_INSECURE_KEY, originalAllowInsecure);
        restoreProperty(VALID_PASSWORD_KEY, originalValidPassword);
    }

    private static void restoreProperty(String key, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, originalValue);
        }
    }

    // NFR-002: happy path - an https base.url is returned unchanged.
    @Test
    public void getBaseUrl_withHttpsScheme_returnsUrlUnchanged() {
        System.setProperty(BASE_URL_KEY, DEMO_BASE_URL);
        System.clearProperty(ALLOW_INSECURE_KEY);

        assertEquals(DEMO_BASE_URL, ConfigReader.getInstance().getBaseUrl());
    }

    // NFR-002 (DR-001 fail-closed path): http base.url throws, even for the public demo host.
    @Test
    public void getBaseUrl_withHttpScheme_throwsIllegalStateException() {
        System.setProperty(BASE_URL_KEY, "http://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        System.clearProperty(ALLOW_INSECURE_KEY);

        assertThrows(IllegalStateException.class, () -> ConfigReader.getInstance().getBaseUrl());
    }

    // NFR-002: the opt-out is honoured once explicitly enabled.
    @Test
    public void getBaseUrl_withHttpSchemeAndInsecureOptOut_returnsUrlUnchanged() {
        String httpUrl = "http://opensource-demo.orangehrmlive.com/web/index.php/auth/login";
        System.setProperty(BASE_URL_KEY, httpUrl);
        System.setProperty(ALLOW_INSECURE_KEY, "true");

        assertEquals(httpUrl, ConfigReader.getInstance().getBaseUrl());
    }

    // NFR-002: an absent allow.insecure.base.url key is treated as false (secure default).
    @Test
    public void getBaseUrl_withoutInsecureOptOutProperty_defaultsToSecureAndThrows() {
        System.setProperty(BASE_URL_KEY, "http://opensource-demo.orangehrmlive.com/web/index.php/auth/login");
        System.clearProperty(ALLOW_INSECURE_KEY);

        assertThrows(IllegalStateException.class, () -> ConfigReader.getInstance().getBaseUrl());
    }

    // NFR-002: a malformed / scheme-less base.url fails closed rather than silently passing.
    @Test
    public void getBaseUrl_withMalformedUrl_throwsIllegalStateException() {
        System.setProperty(BASE_URL_KEY, "not a valid url");
        System.clearProperty(ALLOW_INSECURE_KEY);

        assertThrows(IllegalStateException.class, () -> ConfigReader.getInstance().getBaseUrl());
    }

    // NFR-002 regression guard: getValidPassword()'s demo-host guard still fires unmodified
    // by TASK-001, proving the new scheme guard was strictly additive.
    @Test
    public void getValidPassword_withNonDemoHostAndNoOverride_throwsIllegalStateException() {
        System.clearProperty(VALID_PASSWORD_KEY);
        System.setProperty(BASE_URL_KEY, "https://example.com/auth/login");
        System.clearProperty(ALLOW_INSECURE_KEY);

        assertThrows(IllegalStateException.class, () -> ConfigReader.getInstance().getValidPassword());
    }

    // DR-017: the HTTPS scheme guard's opt-out path must leave a detectable trace.
    // The warning is logged once per JVM, so the "logged" flag is reset via reflection
    // before this test to make the assertion independent of execution order relative to
    // the other opt-out tests above in this class.
    @Test
    public void getBaseUrl_withInsecureOptOutEngaged_logsWarningNamingTheKeyOnly() throws ReflectiveOperationException {
        String httpUrl = "http://opensource-demo.orangehrmlive.com/web/index.php/auth/login";
        System.setProperty(BASE_URL_KEY, httpUrl);
        System.setProperty(ALLOW_INSECURE_KEY, "true");
        resetInsecureBaseUrlWarningLoggedFlag();

        PrintStream originalErr = System.err;
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8));
            ConfigReader.getInstance().getBaseUrl();
        } finally {
            System.setErr(originalErr);
        }

        String logOutput = capturedErr.toString(StandardCharsets.UTF_8);
        assertTrue("Expected a WARN log naming " + ALLOW_INSECURE_KEY + " when its opt-out is engaged",
                logOutput.contains(ALLOW_INSECURE_KEY));
        assertTrue("WARN log must not contain the base.url value", !logOutput.contains(httpUrl));
    }

    private static void resetInsecureBaseUrlWarningLoggedFlag() throws ReflectiveOperationException {
        Field flagField = ConfigReader.class.getDeclaredField("INSECURE_BASE_URL_WARNING_LOGGED");
        flagField.setAccessible(true);
        ((AtomicBoolean) flagField.get(null)).set(false);
    }
}
