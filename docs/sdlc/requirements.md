# Requirements: Login Functionality for OrangeHRM Portal

- **Feature:** OrangeHRM Login Functionality
- **Source:** [Login Functionality for OrangeHRM Portal (Confluence, page 13271042)](https://epam-team-en32bjvm.atlassian.net/wiki/spaces/MFS/pages/13271042/Login+Functionality+for+OrangeHRM+Portal)
- **Date:** 2026-09-27
- **Agent:** requirements-agent

---

## 1. Overview

This feature covers the login functionality of the OrangeHRM portal (`https://opensource-demo.orangehrmlive.com/web/index.php/auth/login`), allowing a registered user (Admin/Employee) to authenticate and reach their dashboard. The PRD (User Story US-AUTH-001, Epic "Authentication & Access Management") specifies the login page's required UI elements, successful and unsuccessful login flows, inline field validation, and the "Forgot your password?" navigation link.

---

## 2. Functional Requirements

| ID | Requirement | Acceptance Criteria | Source |
|----|---|---|---|
| FR-1 | The login page shall display all core UI controls needed for authentication. | The page loads with: company logo/branding banner, Username input field (with placeholder), Password input field (with placeholder), "Login" submit button, "Forgot your password?" link, and a footer with copyright/social links, all visible. | PRD AC1.1 |
| FR-2 | The password input field shall mask entered characters. | When a user types into the Password field, the characters are displayed as dots/asterisks rather than plain text. | PRD AC1.2 |
| FR-3 | A user entering a valid registered username and matching password shall be authenticated and redirected to their dashboard. | Submitting valid credentials (via clicking "Login" or pressing Enter) authenticates the user and redirects to the dashboard page (`.../dashboard/index`). | PRD AC2.1, AC2.2 |
| FR-4 | The system shall show a loading indicator while an authentication request is in progress. | A loading spinner/indicator is visibly displayed between form submission and the resulting page response. | PRD AC2.3 |
| FR-5 | A login attempt with an invalid username or incorrect password shall be rejected without authenticating the user. | The system remains on the login page and displays the error message "Invalid credentials" when either the username or password (or both) is incorrect. | PRD AC3.1 |
| FR-6 | Submitting the login form with an empty Username field shall trigger inline validation. | A "Required" validation message is displayed directly below the Username field when the form is submitted with that field blank. | PRD AC3.2 |
| FR-7 | Submitting the login form with an empty Password field shall trigger inline validation. | A "Required" validation message is displayed directly below the Password field when the form is submitted with that field blank. | PRD AC3.2 |
| FR-8 | The "Forgot your password?" link shall navigate the user to the password reset request page. | Clicking the link navigates to the Reset Password request page (`.../auth/requestPasswordResetCode`). | PRD AC4.1 |

---

## 3. Non-Functional Requirements

| ID | Category | Requirement and Acceptance Criteria | Source |
|----|---|---|---|
| NFR-1 | Compatibility | The user must access the login page using a modern web browser with internet access and JavaScript enabled for the application to function correctly. Acceptance: login page loads and behaves correctly under these client conditions. | PRD Preconditions #3, #4 |
| NFR-2 | Availability | The OrangeHRM application server and database must be up and running for authentication to succeed. No specific uptime/SLA metric is given. | PRD Preconditions #2 |
| NFR-3 | Performance | Not specified in the source (no response-time, load, or throughput targets given for login/authentication). | Not specified in the source |
| NFR-4 | Security | Not specified in the source beyond password masking, which is captured as a functional requirement (FR-2). No requirements on encryption, session timeout, lockout after failed attempts, or credential storage are given. | Not specified in the source |
| NFR-5 | Accessibility | Not specified in the source. | Not specified in the source |

---

## 4. User Stories

**US-AUTH-001 — Login Functionality for OrangeHRM Portal** (Epic: Authentication & Access Management)

> As a registered user (Admin/Employee) of the OrangeHRM system, I want to securely log into the application using my credentials, so that I can access my designated dashboard and perform role-based HR management tasks.

**Preconditions (from source):**
1. The user has a valid registered account.
2. The application server and database are up and running.
3. The user has a modern web browser with internet access and successfully navigates to the login URL.
4. JavaScript is enabled in the browser.

**Acceptance Criteria** (see Functional Requirements FR-1 through FR-8 above for the testable breakdown):
- AC1: UI elements and display (FR-1, FR-2)
- AC2: Successful login / positive flow (FR-3, FR-4)
- AC3: Unsuccessful login / negative flows and empty-field validation (FR-5, FR-6, FR-7)
- AC4: Password recovery link navigation (FR-8)

---

## 5. Out of Scope

Not specified in the source. The PRD does not include an explicit "Out of Scope" section; no exclusions (e.g., multi-factor authentication, account lockout, "remember me", session management, password reset flow beyond the link itself) are stated one way or the other.

---

## 6. Dependencies

- A valid, pre-registered user account must exist in the OrangeHRM system (e.g., an Admin or Employee account) — PRD Precondition #1.
- The OrangeHRM application server and its underlying database must be operational — PRD Precondition #2.
- Client must have a modern web browser with internet access — PRD Precondition #3.
- JavaScript must be enabled in the client browser — PRD Precondition #4.
- The login page and its downstream pages (Dashboard, Reset Password request page) must be reachable at their documented URLs.

---

## 7. Assumptions and Open Questions

### Assumptions (inferred, not explicitly stated in the source)
- "Modern web browser" is assumed to mean current versions of standard desktop browsers (e.g., Chrome, Firefox, Edge); the PRD does not enumerate a supported-browser list.
- The dashboard redirect target (`.../dashboard/index`) is assumed to be the single success destination referenced for both Admin and Employee roles, since the PRD does not describe role-differentiated landing pages beyond mentioning a "designated dashboard."
- It is assumed "Invalid credentials" is the single error message covering both the wrong-username and wrong-password negative cases, since the PRD uses the same message text for both (AC3.1).

### Open Questions (unresolved — require follow-up with product/business owner)
- The PRD's own "Test Scenarios" table only enumerates TS_LOG_001 through TS_LOG_007, yet its Acceptance Criteria describe additional behaviors (AC1.1 full UI-controls display, AC2.3 loading indicator, AC4.1 forgot-password navigation) that are not given corresponding test IDs in that table. It is unclear whether this is an oversight in the source document or an intentional scoping decision.
- No requirement is stated for account lockout, rate limiting, or CAPTCHA after repeated invalid login attempts — is this in scope for a future iteration?
- No requirement is stated for session timeout, "remember me," or multi-factor authentication — are these intentionally excluded, or simply not yet documented?
- No specific error-handling requirement is given for malformed input (e.g., whitespace-only username, special characters) beyond the basic "Required" and "Invalid credentials" messages.

---

## 8. Existing Implementation Coverage

*(Implementation evidence only — presence of a scenario tag indicates automated test coverage exists in `src/test/resources/features/login.feature`; it does not by itself certify that the requirement is fully or correctly satisfied.)*

| Requirement | login.feature Scenario Tag(s) | Coverage Status |
|---|---|---|
| FR-1 (core UI controls displayed) | `@TS_LOG_008` (`@ui`) | Covered |
| FR-2 (password masking) | `@TS_LOG_007` (`@security @ui`) | Covered |
| FR-3 (successful login → dashboard redirect) | `@TS_LOG_001` (`@positive`) | Covered |
| FR-4 (loading indicator during authentication) | `@TS_LOG_009` (`@ui`) | Covered |
| FR-5 (invalid credentials rejected, error shown) | `@TS_LOG_002`, `@TS_LOG_003` (`@negative`) | Covered |
| FR-6 (empty username validation) | `@TS_LOG_004`, `@TS_LOG_005` (`@validation`) | Covered |
| FR-7 (empty password validation) | `@TS_LOG_004`, `@TS_LOG_006` (`@validation`) | Covered |
| FR-8 (forgot password link navigation) | `@TS_LOG_010` (`@navigation`) | Covered |
| NFR-1 to NFR-5 (compatibility, availability, performance, security, accessibility) | None | Not covered (no NFRs are exercised by named scenario tags; framework-level browser setup exists but is implementation detail, not a requirement test) |

Note: the automated suite's tag numbering (`TS_LOG_001`–`TS_LOG_010`) extends three scenarios beyond the PRD's own Test Scenarios table (`TS_LOG_001`–`TS_LOG_007`); those extra scenarios (`TS_LOG_008`–`TS_LOG_010`) map to acceptance criteria the PRD does describe (AC1.1, AC2.3, AC4.1) even though the PRD's table did not assign them test IDs — see the related Open Question in Section 7.

---

## 9. Success Criteria

Not specified as a distinct section in the source. The following is derived from the PRD's Acceptance Criteria (AC1–AC4) for traceability purposes and should be validated with the product owner:
- All core login page UI controls render as specified (FR-1) and the password field masks input (FR-2).
- A user with valid credentials can always reach the dashboard, with a loading indicator shown during the request (FR-3, FR-4).
- A user with invalid credentials is always kept on the login page with the "Invalid credentials" message shown (FR-5).
- Submitting the form with either field empty always produces the corresponding "Required" inline validation message(s) (FR-6, FR-7).
- The "Forgot your password?" link always navigates to the password reset request page (FR-8).

---

## 10. Traceability

- **Confluence source:** [Login Functionality for OrangeHRM Portal](https://epam-team-en32bjvm.atlassian.net/wiki/spaces/MFS/pages/13271042/Login+Functionality+for+OrangeHRM+Portal) (page ID 13271042, Story ID US-AUTH-001)
- **Relevant `login.feature` tags:** `@TS_LOG_001`, `@TS_LOG_002`, `@TS_LOG_003`, `@TS_LOG_004`, `@TS_LOG_005`, `@TS_LOG_006`, `@TS_LOG_007`, `@TS_LOG_008`, `@TS_LOG_009`, `@TS_LOG_010` (plus `@login`, `@positive`, `@negative`, `@validation`, `@security`, `@ui`, `@navigation`)
- **Next Stage:** Architecture
