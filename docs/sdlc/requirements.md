# Requirements Document

**Feature:** Login Functionality for OrangeHRM Portal
**Story ID:** US-AUTH-001
**Epic:** Authentication & Access Management
**Source:** Pasted PRD/User Story (provided directly by the user; Confluence source page is authentication-gated and was not reachable in this session)
**Target Application:** `https://opensource-demo.orangehrmlive.com/web/index.php/auth/login`
**Date:** 2026-09-27

---

## Overview

This feature covers the login/authentication flow for the OrangeHRM demo portal. A registered user (Admin or Employee) must be able to authenticate with valid credentials to reach their dashboard, be blocked from authenticating with invalid or missing credentials with clear feedback, and be able to reach the password-reset request page from the login screen. The requirement set below is derived from user story US-AUTH-001 and its acceptance criteria (AC1–AC4), cross-referenced against the existing Selenium/Cucumber automation already present in this repository.

---

## Functional Requirements

| ID | Requirement | Source |
|----|-------------|--------|
| FR-1 | The login page must display all required controls: company/product logo, username input field, password input field, a "Login" button, and a "Forgot your password?" link. | AC1.1 |
| FR-2 | The password input field must mask entered characters (rendered as a password-type field, not plain text). | AC1.2 |
| FR-3 | Submitting the login form with a valid, registered username and its correct password must authenticate the user and redirect them to their dashboard/home page. | AC2.1, AC2.2 |
| FR-4 | While an authentication request is in progress, the UI must present a loading/processing indicator to the user. | AC2.3 |
| FR-5 | Submitting the login form with an incorrect username and/or incorrect password must keep the user on the login page and display a generic "Invalid credentials" error message (the system must not indicate whether the username or the password was the incorrect part). | AC3.1 |
| FR-6 | Submitting the login form with the username field, the password field, or both left empty must display inline "Required" validation messages under each empty field, and must not submit the authentication request. | AC3.2 |
| FR-7 | Selecting the "Forgot your password?" link from the login page must navigate the user to the password-reset request page. | AC4.1 |

---

## Non-Functional Requirements

| ID | Category | Requirement | Source |
|----|----------|-------------|--------|
| NFR-1 | Compatibility | The login page must render and function correctly in a modern web browser with JavaScript enabled. | Preconditions |
| NFR-2 | Security | Password characters must never be exposed in plain text in the UI (see FR-2); error messaging on failed login must not disclose which credential (username vs. password) was incorrect (see FR-5). | AC1.2, AC3.1 |
| NFR-3 | Performance | Not specified in the source (no explicit response-time target beyond showing a loading indicator per FR-4). | — |
| NFR-4 | Reliability/Availability | Not specified in the source. | — |
| NFR-5 | Scalability | Not specified in the source. | — |

---

## User Stories

**US-AUTH-001 — Login Functionality for OrangeHRM Portal**

> As a registered OrangeHRM user (Admin or Employee), I want to log in to the portal using my username and password, so that I can securely access my dashboard and perform my role-specific tasks.

**Preconditions:**
- The user has a valid, registered account on the OrangeHRM application.
- The OrangeHRM application server and its underlying database are running and reachable.
- The user has access to a modern web browser with JavaScript enabled and an active internet connection.

---

## Out of Scope

Not specified in the source. The PRD covers only the login page's display, credential validation, success/failure feedback, and navigation to the password-reset request page — it does not describe the reset flow itself, session/logout behavior, "Remember Me" persistence, or multi-factor authentication. These are not explicitly excluded either; treat them as open questions (see below) rather than confirmed out-of-scope items.

---

## Dependencies

- A running, reachable instance of the OrangeHRM demo application at the target URL.
- At least one valid, registered user account (existing demo account) for positive-path testing.
- A modern web browser (Chrome/Firefox, per this repository's existing WebDriver support) with JavaScript enabled.
- Network/internet access to the target application host.

---

## Assumptions and Open Questions

- **Assumption:** The demo credentials already configured in `src/test/resources/config.properties` represent a valid registered account for positive-path testing. Actual credential values are intentionally not reproduced in this document.
- **Open Question:** The PRD does not specify the exact wording of the "Required" inline validation messages or the invalid-credentials banner beyond the phrases "Invalid credentials" and "Required." The existing implementation's exact string matching (see Existing Implementation Coverage) is assumed to satisfy this until a stricter text contract is provided.
- **Open Question:** The PRD does not describe what happens after the "Forgot your password?" link is followed (i.e., the reset-request flow itself) — only that navigation must occur. Full reset-flow behavior is not covered by this requirements set.
- **Open Question:** The PRD does not mention "Remember Me," session timeout, logout, or account lockout after repeated failures. Not specified in the source.
- **Open Question:** No explicit non-functional targets (e.g., max load time, concurrent user count) were provided; NFR-3 through NFR-5 are marked "Not specified in the source" pending clarification.

---

## Existing Implementation Coverage

The repository already contains a Selenium + Cucumber automation suite targeting this exact login page (`src/test/resources/features/login.feature`, tags `@TS_LOG_001`–`@TS_LOG_007`), backed by `LoginPage.java`, `DashboardPage.java`, and `LoginSteps.java`. Coverage against the requirements above:

| Requirement | Existing Evidence | Coverage Status |
|-------------|--------------------|------------------|
| FR-1 (required controls displayed) | `LoginPage.isLoginPageDisplayed()`; Background step "Given the user is on the OrangeHRM login page" | Partial — page-open/login-button visibility is covered; no scenario individually asserts the logo, username field, password field, and "Forgot your password?" link are all present. |
| FR-2 (password masking) | `@TS_LOG_007`; `LoginPage.getPasswordFieldType()` | Covered |
| FR-3 (valid login → dashboard) | `@TS_LOG_001` | Covered |
| FR-4 (loading indicator during auth) | None found | Gap — no scenario or page-object method asserts a loading/processing indicator. |
| FR-5 (invalid credentials → generic error) | `@TS_LOG_002`, `@TS_LOG_003`; `LoginPage.isErrorBannerDisplayed()` / `getErrorBannerText()` | Covered |
| FR-6 (blank-field required validation) | `@TS_LOG_004`, `@TS_LOG_005`, `@TS_LOG_006`; `LoginPage.isUsernameRequiredErrorDisplayed()` / `isPasswordRequiredErrorDisplayed()` | Covered |
| FR-7 (forgot-password navigation) | None found — `LoginPage.java` has no locator or method for the "Forgot your password?" link | Gap — not automated. |

**Summary:** 4 of 7 functional requirements are fully covered by existing automation, 1 is partially covered, and 2 (FR-4 loading indicator, FR-7 forgot-password navigation) have no existing coverage. These gaps are carried forward into architecture and planning.

---

## Success Criteria

- All functional requirements (FR-1–FR-7) have either passing automated coverage or a documented, justified reason why they are not automated (e.g., environment limitation).
- No credential values are hardcoded in new documentation or committed in plaintext beyond the existing demo configuration.
- Identified coverage gaps (FR-4, FR-7, and the FR-1 partial gap) are explicitly addressed or consciously deferred in the implementation plan.

---

## Traceability

- **Source:** Pasted PRD, User Story US-AUTH-001 (Epic: Authentication & Access Management)
- **Existing Implementation:** `src/test/resources/features/login.feature`, `src/test/java/com/orangehrm/pages/LoginPage.java`, `src/test/java/com/orangehrm/stepdefinitions/LoginSteps.java`, `src/test/resources/config.properties`
- **Next Stage:** `docs/sdlc/architecture.md` (Stage 2 — Architecture)
