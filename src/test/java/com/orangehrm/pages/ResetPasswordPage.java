package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the OrangeHRM "Reset Password" request page reached from
 * the login page's "Forgot your password?" link
 * (/web/index.php/auth/requestPasswordResetCode).
 */
public class ResetPasswordPage extends BasePage {

    private static final String RESET_PASSWORD_URL_FRAGMENT = "requestPasswordResetCode";
    private static final By PAGE_TITLE = By.cssSelector(".orangehrm-forgot-password-title");

    public ResetPasswordPage(WebDriver driver) {
        super(driver);
    }

    public boolean isResetPasswordPageDisplayed() {
        waitForUrlContains(RESET_PASSWORD_URL_FRAGMENT);
        return isDisplayed(PAGE_TITLE, 10) && getCurrentUrl().contains(RESET_PASSWORD_URL_FRAGMENT);
    }
}
