package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Page Object for the OrangeHRM login page
 * (/web/index.php/auth/login).
 */
public class LoginPage extends BasePage {

    // ---- Locators -----------------------------------------------------
    private static final By USERNAME_INPUT = By.name("username");
    private static final By PASSWORD_INPUT = By.name("password");
    private static final By LOGIN_BUTTON = By.cssSelector("button[type='submit']");
    private static final By LOGO_IMAGE = By.cssSelector(".orangehrm-login-branding img");
    private static final By FORGOT_PASSWORD_LINK = By.cssSelector(".orangehrm-login-forgot-header");
    // Brief loader scoped to the login form / submit button while the auth
    // request is in flight (not the global spinner used by dashboard widgets).
    private static final By LOADING_INDICATOR =
            By.cssSelector("form .oxd-form-loader, form button[type='submit'] .oxd-loading-spinner");
    private static final Duration LOADING_INDICATOR_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration LOADING_INDICATOR_POLLING = Duration.ofMillis(50);

    // Footer locators, scoped to the footer container and keyed on stable,
    // hand-authored class fragments (not generated oxd-* chains) confirmed
    // against the live DOM, per FR-001/AD-008.
    private static final By FOOTER_COPYRIGHT =
            By.cssSelector(".orangehrm-login-footer .orangehrm-copyright-wrapper");
    private static final By FOOTER_SOCIAL_LINK =
            By.cssSelector(".orangehrm-login-footer .orangehrm-login-footer-sm a");
    // Single short timeout for the whole missing-controls group (DR-003):
    // open() already waits for the username field, so the page is confirmed
    // loaded before any of these checks run.
    private static final int MISSING_CONTROLS_TIMEOUT_SECONDS = 3;

    // Top-level red banner shown for wrong username/password combinations.
    private static final By ERROR_ALERT_MESSAGE = By.cssSelector(".oxd-alert-content-text");

    // Inline "Required" messages sit inside the same oxd-input-group
    // ancestor as their corresponding input, so we scope the lookup with a
    // relative XPath rather than relying on a shared class name alone.
    private static final By USERNAME_REQUIRED_ERROR =
            By.xpath("//input[@name='username']/ancestor::div[contains(@class,'oxd-input-group')][1]" +
                    "//span[contains(@class,'oxd-input-field-error-message')]");
    private static final By PASSWORD_REQUIRED_ERROR =
            By.xpath("//input[@name='password']/ancestor::div[contains(@class,'oxd-input-group')][1]" +
                    "//span[contains(@class,'oxd-input-field-error-message')]");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // ---- Navigation -----------------------------------------------------

    public LoginPage open(String baseUrl) {
        navigateTo(baseUrl);
        waitForVisible(USERNAME_INPUT);
        return this;
    }

    // ---- Actions -----------------------------------------------------

    public LoginPage enterUsername(String username) {
        if (username != null && !username.isEmpty()) {
            type(USERNAME_INPUT, username);
        }
        return this;
    }

    public LoginPage enterPassword(String password) {
        if (password != null && !password.isEmpty()) {
            type(PASSWORD_INPUT, password);
        }
        return this;
    }

    public void clickLogin() {
        click(LOGIN_BUTTON);
    }

    /** Submits the login form by pressing Enter in the password field. */
    public void submitWithEnterKey() {
        waitForVisible(PASSWORD_INPUT).sendKeys(Keys.ENTER);
    }

    public void clickForgotPassword() {
        click(FORGOT_PASSWORD_LINK);
    }

    /**
     * Convenience method that performs a full login attempt in one call.
     * Empty strings are treated as "leave the field blank".
     */
    public void login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
    }

    /**
     * Performs a full login attempt and, in the same call (i.e. before any
     * Cucumber step-boundary return), begins polling for the transient
     * loading indicator. This avoids the race where a separate later
     * {@code Then} step starts a fresh wait only after the spinner may have
     * already appeared and disappeared on a fast auth response.
     *
     * @return {@code true} if the loading indicator was observed during the
     *         poll window started immediately after the login click.
     */
    public boolean submitAndCaptureLoadingIndicator(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        return isLoadingIndicatorDisplayed();
    }

    // ---- Assertions / State readers -----------------------------------

    public boolean isErrorBannerDisplayed() {
        return isDisplayed(ERROR_ALERT_MESSAGE, 10);
    }

    public String getErrorBannerText() {
        return getText(ERROR_ALERT_MESSAGE);
    }

    public boolean isUsernameRequiredErrorDisplayed() {
        return isDisplayed(USERNAME_REQUIRED_ERROR, 10);
    }

    public String getUsernameRequiredErrorText() {
        return getText(USERNAME_REQUIRED_ERROR);
    }

    public boolean isPasswordRequiredErrorDisplayed() {
        return isDisplayed(PASSWORD_REQUIRED_ERROR, 10);
    }

    public String getPasswordRequiredErrorText() {
        return getText(PASSWORD_REQUIRED_ERROR);
    }

    public boolean isLoginPageDisplayed() {
        return isDisplayed(LOGIN_BUTTON, 5);
    }

    /**
     * Evaluates every required login control independently (FR-001) and
     * returns the names of the ones not displayed, so a failure is
     * diagnosable from the report alone instead of behind one short-circuited
     * boolean (DR-002, DR-003).
     */
    public List<String> findMissingLoginControls() {
        List<String> missingControls = new ArrayList<>();
        addIfNotDisplayed(missingControls, "logo", LOGO_IMAGE);
        addIfNotDisplayed(missingControls, "username field", USERNAME_INPUT);
        addIfNotDisplayed(missingControls, "password field", PASSWORD_INPUT);
        addIfNotDisplayed(missingControls, "Login button", LOGIN_BUTTON);
        addIfNotDisplayed(missingControls, "forgot-password link", FORGOT_PASSWORD_LINK);
        addIfNotDisplayed(missingControls, "footer copyright", FOOTER_COPYRIGHT);
        addIfNotDisplayed(missingControls, "footer social-media link", FOOTER_SOCIAL_LINK);
        return missingControls;
    }

    private void addIfNotDisplayed(List<String> missingControls, String controlName, By locator) {
        if (!isDisplayed(locator, MISSING_CONTROLS_TIMEOUT_SECONDS)) {
            missingControls.add(controlName);
        }
    }

    /** Returns the Username field's placeholder attribute value, for LoginSteps to compare (FR-001). */
    public String getUsernamePlaceholder() {
        return waitForVisible(USERNAME_INPUT).getDomAttribute("placeholder");
    }

    /** Returns the Password field's placeholder attribute value, for LoginSteps to compare (FR-001). */
    public String getPasswordPlaceholder() {
        return waitForVisible(PASSWORD_INPUT).getDomAttribute("placeholder");
    }

    /**
     * Checks for the transient loading indicator shown on the submit button
     * while an authentication request is in flight. Uses a short timeout
     * since the indicator is expected to appear (and often disappear)
     * quickly; a false result only means it was not observed in that window.
     */
    public boolean isLoadingIndicatorDisplayed() {
        return isPresent(LOADING_INDICATOR, LOADING_INDICATOR_TIMEOUT, LOADING_INDICATOR_POLLING);
    }

    /**
     * Returns the "type" attribute of the password input so tests can
     * confirm characters are masked (type="password") rather than shown in
     * plain text (type="text").
     */
    public String getPasswordFieldType() {
        WebElement passwordField = waitForVisible(PASSWORD_INPUT);
        return passwordField.getDomAttribute("type");
    }
}
