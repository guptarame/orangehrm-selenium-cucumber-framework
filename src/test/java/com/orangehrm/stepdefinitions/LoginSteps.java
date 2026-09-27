package com.orangehrm.stepdefinitions;

import com.orangehrm.config.ConfigReader;
import com.orangehrm.driver.DriverManager;
import com.orangehrm.pages.DashboardPage;
import com.orangehrm.pages.LoginPage;
import com.orangehrm.pages.ResetPasswordPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Step definitions for src/test/resources/features/login.feature.
 * Business logic is delegated entirely to the LoginPage / DashboardPage
 * Page Objects — this class only orchestrates calls and asserts outcomes.
 */
public class LoginSteps {

    private final LoginPage loginPage = new LoginPage(DriverManager.getDriver());
    private final DashboardPage dashboardPage = new DashboardPage(DriverManager.getDriver());
    private final ResetPasswordPage resetPasswordPage = new ResetPasswordPage(DriverManager.getDriver());

    // ---- Given -----------------------------------------------------

    @Given("the user is on the OrangeHRM login page")
    public void the_user_is_on_the_orange_hrm_login_page() {
        String baseUrl = ConfigReader.getInstance().getBaseUrl();
        loginPage.open(baseUrl);
    }

    // ---- When -----------------------------------------------------

    @When("the user logs in with username {string} and password {string}")
    public void the_user_logs_in_with_username_and_password(String username, String password) {
        loginPage.login(username, password);
    }

    @When("the user submits the login form with username {string} and password {string}")
    public void the_user_submits_the_login_form_with_username_and_password(String username, String password) {
        loginPage.login(username, password);
    }

    @When("the user enters {string} into the password field")
    public void the_user_enters_into_the_password_field(String password) {
        loginPage.enterPassword(password);
    }

    @When("the user logs in with valid credentials")
    public void the_user_logs_in_with_valid_credentials() {
        ConfigReader config = ConfigReader.getInstance();
        loginPage.login(config.getValidUsername(), config.getValidPassword());
    }

    @When("the user logs in with username {string} and a valid password")
    public void the_user_logs_in_with_username_and_a_valid_password(String username) {
        String validPassword = ConfigReader.getInstance().getValidPassword();
        loginPage.login(username, validPassword);
    }

    @When("the user submits valid credentials")
    public void the_user_submits_valid_credentials() {
        ConfigReader config = ConfigReader.getInstance();
        loginPage.login(config.getValidUsername(), config.getValidPassword());
    }

    @When("the user clicks the {string} link")
    public void the_user_clicks_the_link(String linkName) {
        if ("Forgot your password?".equals(linkName)) {
            loginPage.clickForgotPassword();
        } else {
            throw new IllegalArgumentException("Unsupported link: " + linkName);
        }
    }

    // ---- Then -----------------------------------------------------

    @Then("the user should be redirected to the Dashboard page")
    public void the_user_should_be_redirected_to_the_dashboard_page() {
        assertTrue("Expected the Dashboard page to be displayed after a successful login",
                dashboardPage.isDashboardPageDisplayed());
        assertEquals("Dashboard", dashboardPage.getPageHeaderText());
    }

    @Then("the user should remain on the login page")
    public void the_user_should_remain_on_the_login_page() {
        assertTrue("Expected the login form to still be visible", loginPage.isLoginPageDisplayed());
        assertTrue("Expected the URL to still be the login/auth page",
                loginPage.getCurrentUrl().contains("auth/login"));
    }

    @Then("the error banner {string} should be displayed")
    public void the_error_banner_should_be_displayed(String expectedMessage) {
        assertTrue("Expected an error banner to be displayed on the login page",
                loginPage.isErrorBannerDisplayed());
        assertEquals(expectedMessage, loginPage.getErrorBannerText());
    }

    @Then("a {string} validation error should be displayed under the Username field")
    public void a_validation_error_should_be_displayed_under_the_username_field(String expectedMessage) {
        assertTrue("Expected a required validation error under the Username field",
                loginPage.isUsernameRequiredErrorDisplayed());
        assertEquals(expectedMessage, loginPage.getUsernameRequiredErrorText());
    }

    @Then("a {string} validation error should be displayed under the Password field")
    public void a_validation_error_should_be_displayed_under_the_password_field(String expectedMessage) {
        assertTrue("Expected a required validation error under the Password field",
                loginPage.isPasswordRequiredErrorDisplayed());
        assertEquals(expectedMessage, loginPage.getPasswordRequiredErrorText());
    }

    @Then("the password field should mask the entered characters")
    public void the_password_field_should_mask_the_entered_characters() {
        assertEquals("password", loginPage.getPasswordFieldType());
    }

    @Then("all required login controls should be displayed")
    public void all_required_login_controls_should_be_displayed() {
        assertTrue("Expected the logo, username field, password field, login button, "
                        + "and forgot-password link to all be displayed",
                loginPage.areAllLoginControlsDisplayed());
    }

    @Then("a loading indicator should be displayed during authentication")
    public void a_loading_indicator_should_be_displayed_during_authentication() {
        assertTrue("Expected a loading indicator to be displayed while authentication was in progress",
                loginPage.isLoadingIndicatorDisplayed());
    }

    @Then("the user should be redirected to the Reset Password page")
    public void the_user_should_be_redirected_to_the_reset_password_page() {
        assertTrue("Expected the Reset Password page to be displayed",
                resetPasswordPage.isResetPasswordPageDisplayed());
    }
}
