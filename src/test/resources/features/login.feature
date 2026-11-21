@login
Feature: OrangeHRM Login
  As a user of the OrangeHRM application
  I want to log in through the authentication page
  So that I can access the system only with valid credentials and receive
  clear feedback when my input is invalid or incomplete

  Background:
    Given the user is on the OrangeHRM login page

  @TS_LOG_001 @positive
  Scenario: Successful login with valid Admin credentials
    When the user logs in with valid credentials
    Then the user should be redirected to the Dashboard page
    # NFR-002: post-submit defence-in-depth check - catches a mid-session downgrade.
    And the current page should be served over HTTPS

  @TS_LOG_002 @negative
  Scenario: Login failure with valid username and invalid password
    When the user logs in with a valid username and password "wrongpass"
    Then the user should remain on the login page
    And the error banner "Invalid credentials" should be displayed

  @TS_LOG_003 @negative
  Scenario: Login failure with invalid username and valid password
    When the user logs in with username "InvalidUser" and a valid password
    Then the user should remain on the login page
    And the error banner "Invalid credentials" should be displayed

  @TS_LOG_004 @validation
  Scenario: Form validation when both Username and Password are empty
    When the user submits the login form with username "" and password ""
    Then a "Required" validation error should be displayed under the Username field
    And a "Required" validation error should be displayed under the Password field

  @TS_LOG_005 @validation
  Scenario: Form validation when Username is left empty
    When the user submits the login form with username "" and a valid password
    Then a "Required" validation error should be displayed under the Username field

  @TS_LOG_006 @validation
  Scenario: Form validation when Password is left empty
    When the user submits the login form with a valid username and password ""
    Then a "Required" validation error should be displayed under the Password field

  @TS_LOG_007 @security @ui
  Scenario: Password field masks entered characters
    When the user enters a valid password into the password field
    Then the password field should mask the entered characters

  @TS_LOG_008 @ui
  Scenario: All required login controls are displayed
    # FR-001: logo, both fields, Login button, forgot-password link, footer
    # copyright, footer social link, and both placeholder values.
    Then all required login controls should be displayed
    And the Username field placeholder should be "Username"
    And the Password field placeholder should be "Password"
    # NFR-002: preventive-guard defence-in-depth check on the pre-submit login page.
    And the current page should be served over HTTPS

  @TS_LOG_009 @ui @timing
  Scenario: Loading indicator is shown while authentication is in progress
    When the user submits valid credentials
    Then a loading indicator should be displayed during authentication

  @TS_LOG_010 @navigation
  Scenario: Forgot password link navigates to the reset password page
    When the user clicks the "Forgot your password?" link
    Then the user should be redirected to the Reset Password page

  @TS_LOG_011 @negative
  Scenario: Login failure with both invalid username and invalid password
    When the user submits the login form with username "InvalidUser" and password "wrongpass"
    Then the user should remain on the login page
    And the error banner "Invalid credentials" should be displayed

  @TS_LOG_012 @positive
  Scenario: Successful login submitted with the Enter key
    When the user submits valid credentials with the Enter key
    Then the user should be redirected to the Dashboard page
