package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the OrangeHRM Dashboard page that the user lands on after
 * a successful login (/web/index.php/dashboard/index).
 */
public class DashboardPage extends BasePage {

    private static final String DASHBOARD_URL_FRAGMENT = "dashboard";
    private static final By PAGE_HEADER = By.cssSelector("h6.oxd-topbar-header-breadcrumb-module");
    private static final By USER_DROPDOWN = By.cssSelector(".oxd-userdropdown-tab");

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public boolean isDashboardPageDisplayed() {
        waitForUrlContains(DASHBOARD_URL_FRAGMENT);
        return isDisplayed(PAGE_HEADER, 10) && getCurrentUrl().contains(DASHBOARD_URL_FRAGMENT);
    }

    public String getPageHeaderText() {
        return getText(PAGE_HEADER);
    }

    public boolean isUserLoggedIn() {
        return isDisplayed(USER_DROPDOWN, 10);
    }
}
