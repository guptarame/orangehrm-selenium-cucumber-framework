# Requirements Document

**Feature:** Customer Login & Authentication  
**Story ID:** US-AUTH-002  
**Source:** Confluence PRD — https://epam-team-en32bjvm.atlassian.net/wiki/spaces/MFS/pages/11796481/Customer+Login+Authentication  
**Source Access Status:** Live Confluence content could not be fetched in this execution environment because no Confluence MCP tool/session is exposed to the agent. This document structures the requirements from the repository's existing SDLC requirements artifact for the same PRD feature and URL context.  
**Date:** 2026-09-22  
**Agent:** requirements-agent

---

## Overview

The feature covers the customer login experience for the My Account portal at `https://askomdch.com/account/`. Registered customers shall be able to authenticate with a username or email address and password, optionally request a persistent session with **Remember me**, navigate to password recovery, and receive clear feedback for successful and unsuccessful login attempts.

---

## Functional Requirements

### FR-01: Display customer login form
**Description:** The system shall present a login form for returning customers on the My Account page.

**Acceptance Criteria:**
- The My Account page displays a login form for returning customers.
- The login form includes a username or email address field.
- The login form includes a password field.
- The login form includes a **Remember me** option.
- The login form includes a **Log in** action.
- The login form includes a **Lost your password?** link.

### FR-02: Validate required credentials
**Description:** The system shall prevent login submission when required credential fields are blank and shall inform the user which required information is missing.

**Acceptance Criteria:**
- Submitting the login form with a blank username or email address does not authenticate the user.
- Submitting the login form with a blank password does not authenticate the user.
- Submitting the login form with both credential fields blank does not authenticate the user.
- The system displays validation feedback for each missing required credential.
- The user remains in a state where the missing credentials can be entered and the login retried.

### FR-03: Authenticate registered customers
**Description:** The system shall authenticate a registered customer when valid credentials are submitted.

**Acceptance Criteria:**
- A customer with valid username or email address and valid password can submit the login form successfully.
- After successful authentication, the customer is redirected to the My Account dashboard or equivalent authenticated account home view.
- The authenticated view indicates that the customer is signed in.
- The authenticated view provides a logout option.

### FR-04: Handle invalid authentication attempts
**Description:** The system shall reject invalid login attempts and provide clear failure feedback without authenticating the user.

**Acceptance Criteria:**
- A login attempt with a registered username or email address and an incorrect password is rejected.
- A login attempt with an unregistered username or email address is rejected.
- Failed authentication displays an error notice that is understandable to the customer.
- Failed authentication keeps the customer on, or returns the customer to, the login experience.
- Failed authentication does not expose sensitive account or credential information.

### FR-05: Support Remember me session persistence
**Description:** The system shall allow a customer to request session persistence by selecting the **Remember me** option during login.

**Acceptance Criteria:**
- The customer can select and clear the **Remember me** option before submitting the login form.
- When **Remember me** is selected with valid credentials, the authenticated session persists across browser restarts according to the application's session policy.
- When session persistence is active, returning to the My Account page shows the authenticated account view without requiring immediate re-entry of credentials, subject to the application's session expiration rules.

### FR-06: Navigate to password recovery
**Description:** The system shall provide a path for customers who have forgotten their password to access the password recovery or reset flow.

**Acceptance Criteria:**
- Selecting **Lost your password?** navigates the customer to the password recovery or reset experience.
- The password recovery destination allows the customer to begin account recovery.
- Navigation to password recovery does not require the customer to be authenticated.

---

## Non-Functional Requirements

### NFR-01: Security
**Requirement:** Customer credentials shall be protected during authentication and shall not be exposed in application feedback, logs, source control, or test artifacts.

**Acceptance Criteria:** 
- The customer login page and credential submission use HTTPS.
- Authentication failure feedback does not reveal whether a specific account exists beyond necessary user-facing error handling.
- Real customer or test account passwords are not stored in repository files.
- Password values are not printed in logs, reports, screenshots, or failure messages.

### NFR-02: Reliability
**Requirement:** The login flow shall produce consistent outcomes for successful, invalid, and blank-credential scenarios during normal site availability.

**Acceptance Criteria:**
- Valid credentials result in successful authentication when the application and dependent services are available.
- Invalid credentials consistently result in failed authentication.
- Blank required fields consistently result in validation feedback.
- Each login attempt reaches a terminal success, failure, or validation state within 10 seconds under normal network conditions.

### NFR-03: Compatibility
**Requirement:** The login experience shall be usable and verifiable on supported desktop browsers.

**Acceptance Criteria:**
- The My Account login flow is accessible on the live URL `https://askomdch.com/account/`.
- The login, validation, error, Remember me, and password recovery behaviors can be verified in the latest stable versions of Chrome and Firefox on desktop.

### NFR-04: Usability
**Requirement:** The login experience shall provide clear controls and feedback for customers attempting to access their account.

**Acceptance Criteria:**
- Required login fields and primary login action are visible without ambiguity on the My Account page.
- Validation and authentication feedback are displayed in proximity to the login experience.
- Error and validation messages are understandable to a typical customer and indicate the next corrective action.

### NFR-05: Observability and testability
**Requirement:** The authentication outcomes shall be observable through page state, navigation state, and user-facing messages so that the flow can be verified by automated and manual tests.

**Acceptance Criteria:**
- Successful authentication can be verified through authenticated account state, dashboard navigation, or logout availability.
- Failed authentication can be verified through an error notice and unauthenticated state.
- Blank-field validation can be verified through validation feedback and unauthenticated state.
- Password recovery navigation can be verified through the recovery/reset destination.

---

## User Stories

**US-1:** As a registered customer, I want to log in with my username or email address and password, so that I can access my account dashboard.

**Acceptance Criteria:**
- Given the customer is on the My Account login page, when valid credentials are submitted, then the customer is authenticated.
- Given authentication succeeds, when the customer is redirected, then the My Account dashboard or authenticated account home view is displayed.
- Given the authenticated account view is displayed, then a logout option is available.

**US-2:** As a returning customer, I want clear feedback when my login attempt fails, so that I can correct my credentials and try again.

**Acceptance Criteria:**
- Given an incorrect password is submitted, then the system rejects the login and displays an error notice.
- Given an unregistered username or email address is submitted, then the system rejects the login and displays an error notice.
- Given a failed login attempt, then the customer remains unauthenticated and can retry login.

**US-3:** As a customer, I want required login fields to be validated, so that I know which information is needed before I can sign in.

**Acceptance Criteria:**
- Given the username or email address is blank, when the login form is submitted, then the system displays required-field validation feedback.
- Given the password is blank, when the login form is submitted, then the system displays required-field validation feedback.
- Given required-field validation fails, then the user is not authenticated.

**US-4:** As a customer on a trusted device, I want to select Remember me, so that I can stay signed in according to the site's session policy.

**Acceptance Criteria:**
- Given the customer selects **Remember me** and submits valid credentials, then the system creates a persistent authenticated session according to policy.
- Given the persistent session remains valid, when the customer returns after a browser restart, then the customer is still authenticated.

**US-5:** As a customer who forgot my password, I want to navigate to password recovery, so that I can regain access to my account.

**Acceptance Criteria:**
- Given the customer is on the login page, when **Lost your password?** is selected, then the password recovery or reset flow is opened.
- Given the recovery flow is opened, then the customer can begin password recovery without being authenticated.

---

## Acceptance Criteria

1. The My Account page displays all required login controls: username or email field, password field, Remember me option, login action, and password recovery link.
2. Blank username/email and/or password submissions are blocked and show required-field validation feedback.
3. Valid credentials authenticate the customer and display the authenticated My Account experience.
4. Incorrect passwords are rejected with a clear error notice and do not authenticate the customer.
5. Unregistered usernames or email addresses are rejected with a clear error notice and do not authenticate the customer.
6. Selecting **Remember me** with valid credentials persists the authenticated session according to the application policy.
7. Selecting **Lost your password?** opens the password recovery or reset flow.
8. Credentials are transmitted over HTTPS and are not stored or exposed by the test assets.

---

## Out of Scope

- Customer account registration.
- Multi-factor authentication.
- Social login providers.
- Administrative account access.
- Checkout, order history, and profile-management capabilities beyond confirming authenticated account access.
- Implementation of the password reset process after the user reaches the recovery flow.
- Changes to authentication provider, session policy, or credential storage mechanisms.

---

## Dependencies

- Live access to `https://askomdch.com/account/`.
- Existing registered customer test account with valid username or email address and password.
- Application authentication services and session-management services.
- Supported desktop browser for verification, including Chrome or Firefox.
- Network connectivity to the target application.
- Secure external storage or runtime injection for test credentials.

---

## Assumptions

- A registered customer account already exists for successful-login validation.
- Test credentials are provided securely at execution time and are not committed to the repository.
- The live My Account page is reachable during verification.
- The site supports a Remember me capability and defines the duration and expiration behavior for persistent sessions.
- The exact wording of user-facing validation and error messages may vary, but equivalent customer-meaningful feedback satisfies the requirement.
- The password recovery flow is owned by the same customer account experience and is reachable from the login page.

---

## Success Criteria

The feature is considered done when customers can access the My Account login page, authenticate successfully with valid credentials, receive clear validation or error feedback for invalid attempts, request session persistence via **Remember me**, navigate to password recovery, and satisfy the defined security, reliability, compatibility, usability, and testability requirements.

---

## Traceability

| Source item | Requirement coverage | Verification focus |
| --- | --- | --- |
| Login form display | FR-01, NFR-03, NFR-04 | Required controls are visible and usable |
| Required-field validation | FR-02, NFR-04, NFR-05 | Blank submissions show validation and remain unauthenticated |
| Valid login | FR-03, NFR-01, NFR-02, NFR-05 | Valid credentials reach authenticated account state |
| Invalid password | FR-04, NFR-01, NFR-02, NFR-05 | Incorrect password is rejected with safe feedback |
| Unknown account | FR-04, NFR-01, NFR-02, NFR-05 | Unregistered username/email is rejected with safe feedback |
| Remember me | FR-05, NFR-01, NFR-02 | Persistent session follows application policy |
| Password recovery navigation | FR-06, NFR-04, NFR-05 | Recovery/reset flow opens from login page |

- Source: Confluence PRD page — https://epam-team-en32bjvm.atlassian.net/wiki/spaces/MFS/pages/11796481/Customer+Login+Authentication
- Access limitation: live Confluence read was not possible because no Confluence MCP tool/session is available in this run.
- Next Stage: Architecture
