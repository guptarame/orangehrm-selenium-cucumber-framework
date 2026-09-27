package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
    // Brief loader shown on the submit button while the auth request is in flight.
    private static final By LOADING_INDICATOR = By.cssSelector(".oxd-form-loader, .oxd-loading-spinner");

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
     * Confirms every required login control (logo, username field, password
     * field, login button, forgot-password link) is present on the page.
     */
    public boolean areAllLoginControlsDisplayed() {
        return isDisplayed(LOGO_IMAGE, 10)
                && isDisplayed(USERNAME_INPUT, 10)
                && isDisplayed(PASSWORD_INPUT, 10)
                && isDisplayed(LOGIN_BUTTON, 10)
                && isDisplayed(FORGOT_PASSWORD_LINK, 10);
    }

    /**
     * Checks for the transient loading indicator shown on the submit button
     * while an authentication request is in flight. Uses a short timeout
     * since the indicator is expected to appear (and often disappear)
     * quickly; a false result only means it was not observed in that window.
     */
    public boolean isLoadingIndicatorDisplayed() {
        return isDisplayed(LOADING_INDICATOR, 3);
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
