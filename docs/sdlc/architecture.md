# Selenium Test Automation Architecture

**SDLC Stage:** 2 — Architecture  
**Project:** Selenium Login Automation Capstone  
**Feature:** US-AUTH-002 — Customer Login & Authentication  
**Primary requirement source:** `docs/sdlc/requirements.md`  
**Existing implementation inspected:** `pom.xml`, `BaseTest.java`, `TestConfig.java`, `BasePage.java`, `LoginPage.java`, `LoginPageTests.java`, `TestData.java`, `TestLifecycleListener.java`, `LogUtil.java`, `ScreenshotUtil.java`  
**Date:** 2026-09-22  
**Agent:** architecture-agent  

> Note: live Confluence access was not available in this execution environment. This architecture is based on the repository SDLC requirements artifact and the existing Selenium/JUnit framework.

---

## 1. Project Context and Architecture Overview

The repository verifies the live customer login experience at `https://askomdch.com/account/` for returning customers. The automation scope covers login form display, blank required-field validation, valid login, invalid login, **Remember me**, password recovery navigation, and security/testability expectations from `requirements.md`.

The architecture remains intentionally small:

```text
Maven Surefire
  → JUnit 5 test class
  → BaseTest creates one local browser per test
  → LoginPage exposes user-level login actions and observations
  → TestConfig resolves runtime configuration and credentials
  → JUnit assertions validate page state, feedback, and navigation
  → Listener logs result and captures screenshots on failures
```

### Scope

| In scope | Out of scope / deferred |
| --- | --- |
| Desktop web automation for customer login/authentication | Product code changes |
| Chrome and Firefox local execution | Registration, MFA, social login, admin login |
| Sequential test execution through Maven/JUnit | Password reset process after reaching recovery page |
| Runtime-injected credentials through system properties or environment variables | Committed credentials or external secret retrieval code |
| Maven Surefire XML reports and failure screenshots | Parallel/remote grid execution, advanced reporting portals |

---

## 2. Component Responsibilities

| Component | Responsibility | Inputs | Outputs | Dependencies | Key classes/files |
| --- | --- | --- | --- | --- | --- |
| Build and test runner | Compile tests, resolve dependencies, discover `**/*Tests.java`, produce Surefire reports | `pom.xml`, `mvn test`, `-D` system properties | JUnit execution result, `target/surefire-reports` | Maven 3.9+, Surefire 3.5.0 | `pom.xml` |
| Test lifecycle and driver management | Create, configure, register, and quit one `WebDriver` per test; handle Remember-me Chrome profile | Browser/headless config, test display name | Isolated browser session; temporary profile for Remember-me test | Selenium, WebDriverManager, JUnit lifecycle | `BaseTest.java` |
| Configuration | Resolve base URL, browser, headless flag, timeout, and credential/test-data overrides | System properties, environment variables, safe defaults | Typed runtime values | Java standard library, `TestData` | `TestConfig.java` |
| Test data constants | Hold non-secret defaults, timeout constants, and message-token sets | Static values and safe defaults | Reusable tokens and default timeout values | Java collections | `TestData.java` |
| Page base utilities | Centralize navigation, explicit waits, click/type helpers, visibility checks, current URL | `WebDriver`, Selenium locators | Reusable page-level operations | Selenium `WebDriverWait`, expected conditions | `BasePage.java` |
| Login page object | Own login selectors and customer-login actions/observations | Account URL, credentials, Remember-me state | Page actions, feedback text, authenticated state, reset navigation | `BasePage`, Selenium | `LoginPage.java` |
| Scenario tests | Map requirements to independent readable tests with assertions and assumptions | Config, page object, test data | Pass/fail/skip outcomes | JUnit 5 assertions/assumptions | `LoginPageTests.java` |
| Diagnostics | Log test start/result; capture failure screenshots without changing result | JUnit extension context, registered driver | Console logs, `target/screenshots/*.png` | JUnit extension API, Selenium screenshots | `TestLifecycleListener.java`, `LogUtil.java`, `ScreenshotUtil.java` |

### Single-responsibility boundaries

- Tests describe **what** behavior is verified; they should not contain selectors or low-level waits.
- Page objects describe **how** the UI is operated and observed; they should not contain test assertions.
- `BaseTest` owns browser lifecycle only; it should not know business assertions.
- `TestConfig` owns runtime configuration only; it must not log or persist secrets.
- Diagnostic utilities may record test names and screenshots, but must not print credential values.

---

## 3. Directory Structure

Current project structure to preserve:

```text
.
├── pom.xml
├── README.md
├── docs/
│   └── sdlc/
│       ├── requirements.md
│       └── architecture.md
├── src/
│   ├── main/java/Github_Copilot/
│   │   └── Main.java
│   └── test/java/Github_Copilot/
│       ├── base/
│       │   └── BaseTest.java
│       ├── config/
│       │   └── TestConfig.java
│       ├── data/
│       │   └── TestData.java
│       ├── listeners/
│       │   └── TestLifecycleListener.java
│       ├── pages/
│       │   ├── BasePage.java
│       │   └── LoginPage.java
│       ├── tests/
│       │   └── LoginPageTests.java
│       └── utils/
│           ├── LogUtil.java
│           └── ScreenshotUtil.java
└── target/
    ├── screenshots/          # generated on failures
    └── surefire-reports/     # generated by Maven Surefire
```

No additional framework layer is required for the current feature. Add a new page object only when a future scenario crosses a distinct page boundary, for example a full reset-password flow.

---

## 4. Test Execution Flow

```text
1. Developer/CI runs:
     mvn test [-Dbrowser=chrome|firefox] [-Dheadless=true|false] [...]

2. Maven Surefire:
     - uses Java 21 test compilation
     - discovers classes matching **/*Tests.java
     - starts JUnit Jupiter

3. JUnit lifecycle:
     - TestLifecycleListener logs test start
     - BaseTest @BeforeEach resolves TestConfig values
     - BaseTest creates ChromeDriver or FirefoxDriver through WebDriverManager
     - Driver page-load timeout is applied
     - Implicit wait is set to zero so synchronization is explicit
     - Browser is maximized locally or set to 1920x1080 in headless mode

4. Test scenario:
     - LoginPage opens the configured account URL
     - Page object waits for visible/clickable elements
     - Test performs actions through LoginPage
     - Test asserts UI controls, validation, error feedback, login state, or URL

5. Failure handling:
     - Listener logs failures
     - ScreenshotUtil writes a timestamped screenshot to target/screenshots
     - The original JUnit failure remains authoritative

6. Teardown and reports:
     - BaseTest @AfterEach quits the browser
     - Temporary Chrome profile for Remember-me is deleted best-effort
     - Surefire writes XML/text reports under target/surefire-reports
```

### Execution examples

```powershell
# Default local Chrome, headed
mvn test

# Firefox, headless
mvn test -Dbrowser=firefox -Dheadless=true

# Explicit target URL
mvn test -DbaseUrl=https://askomdch.com/account/

# Credential-dependent tests through environment variables are preferred
$env:LOGIN_VALID_USERNAME="test.user@example.com"
$env:LOGIN_VALID_PASSWORD="<provided by secure secret source>"
mvn test
```

---

## 5. Technology and Framework Decisions

| Concern | Decision | Rationale |
| --- | --- | --- |
| Language/runtime | Java 21+ | Matches `pom.xml`; current LTS; familiar Selenium/JUnit ecosystem |
| Automation library | Selenium 4.25.0 | Standard WebDriver API, Chrome/Firefox support, explicit waits |
| Driver binaries | WebDriverManager 6.1.0 | Avoids committing or manually managing driver executables |
| Test framework | JUnit Jupiter 5.11.3 | Lifecycle annotations, extensions, assumptions, assertions |
| Build/reporting | Maven 3.9+ with Surefire 3.5.0 | Reproducible CLI/CI execution and standard XML reports |
| Assertions | JUnit 5 native assertions | Sufficient for current scenario checks; avoids extra dependencies |
| Reporting | Console logs, Surefire reports, screenshots | Necessary and lightweight for the project size |
| Execution model | Sequential local execution | Reduces flakiness and profile/session conflicts; parallel/grid deferred |

The architecture avoids speculative complexity such as Cucumber, remote WebDriver, custom HTML reporting, service virtualization, or external secret clients until requirements demand them.

---

## 6. Configuration and Test Data Strategy

### Configuration precedence

`TestConfig` resolves every configurable value in this order:

```text
1. Java system property supplied with -D
2. Environment variable
3. Safe default
```

| Purpose | System property | Environment variable | Default | Secret? |
| --- | --- | --- | --- | --- |
| Account login URL | `baseUrl` | `BASE_URL` | `https://askomdch.com/account/` | No |
| Browser | `browser` | `BROWSER` | `chrome` | No |
| Headless mode | `headless` | `HEADLESS` | `false` | No |
| Page-load timeout | `pageLoadTimeout` | `PAGE_LOAD_TIMEOUT_SECONDS` | `30` | No |
| Valid username/email | `validUsername` | `LOGIN_VALID_USERNAME` | empty string | Sensitive identifier |
| Valid password | `validPassword` | `LOGIN_VALID_PASSWORD` | empty string | Yes |
| Invalid password | `invalidPassword` | `LOGIN_INVALID_PASSWORD` | `invalid-password` | No |
| Unknown username/email | `unknownUsername` | `LOGIN_UNKNOWN_USERNAME` | `unknown_user_not_registered@example.com` | No |

### Credential handling

- Valid credentials are never stored in repository files.
- CI must inject valid credentials using secret variables.
- Tests that require valid credentials use JUnit assumptions and are reported as skipped when values are missing.
- Passwords must not be logged, included in assertion messages, or intentionally captured in screenshots.

### Test data

- Deterministic negative values are safe to keep in `TestData`/defaults because they are not secrets.
- Message assertions use token sets because live WooCommerce/WordPress wording can vary.
- Valid-login and Remember-me tests depend on a real registered test account supplied at runtime.

---

## 7. Page Objects, Synchronization, and Utilities

### Page Object Model

`LoginPage` is the single page object for the current scope. It owns locators and exposes business-level methods:

| Requirement area | Page-object support |
| --- | --- |
| Login form display | `isUsernameVisible`, `isPasswordVisible`, `isRememberMeVisible`, `isLoginButtonVisible`, `isLostPasswordVisible` |
| Credential entry | `enterUsername`, `enterPassword`, `submitLogin` |
| Remember me | `setRememberMe`, `isRememberMeSelected` |
| Successful login | `isLoggedIn`, `waitForDashboard`, `getDashboardText` |
| Validation/error feedback | `getFeedbackMessage` / `readFeedbackMessage` |
| Password recovery | `clickLostPassword`, `getCurrentUrl` |

Preferred selectors are stable `id`, `name`, CSS, and link text values already used by the current login page. If the application markup changes, selectors should be updated only in `LoginPage`.

### Synchronization

- Use explicit waits through `BasePage` (`WebDriverWait`) for visibility, clickability, URL changes, and post-submit outcomes.
- Keep implicit wait at zero to avoid compounded wait behavior.
- Do not use `Thread.sleep()`.
- Default explicit wait is centralized in `TestData.DEFAULT_WAIT_TIMEOUT_SECONDS` and is currently 12 seconds, satisfying the requirement that normal flows reach a terminal state in approximately 10 seconds while allowing small live-site variance.

### Reusable utilities

- `LogUtil`: lightweight timestamped console logging.
- `ScreenshotUtil`: failure screenshot capture under `target/screenshots`.
- `TestLifecycleListener`: JUnit extension binding logging/screenshots to test execution.

---

## 8. Browser Coverage and CI Execution

### Browser coverage

| Browser | Status | Notes |
| --- | --- | --- |
| Chrome latest stable desktop | Primary/default | Supports all scenarios, including Remember-me profile restart |
| Firefox latest stable desktop | Supported | Covers form, validation, valid/invalid login, and recovery navigation |

The current Remember-me persistence test is Chrome-specific because it uses a temporary Chrome user-data directory to simulate browser restart with a persistent profile. Firefox can still verify that the Remember-me control is visible/selectable, but cross-restart persistence should be treated as Chrome-only unless Firefox profile support is added in a later stage.

### CI guidance

CI should run sequential Maven jobs:

```text
Job 1: mvn test -Dheadless=true -Dbrowser=chrome
Job 2: mvn test -Dheadless=true -Dbrowser=firefox
```

CI requirements:

- Java 21 and Maven 3.9+ installed.
- Chrome and Firefox installed, or available through the CI image.
- Outbound network access to `https://askomdch.com/account/`.
- Credentials injected through CI secrets for valid-login coverage.
- Archive `target/surefire-reports` and, on failures, `target/screenshots`.

Parallel execution and Selenium Grid/remote browser execution are future work only. They should not be introduced until driver isolation, credential use, profile storage, and CI capacity are reviewed.

---

## 9. Reporting, Error Handling, and Security

### Reporting and observability

| Artifact | Location | Purpose |
| --- | --- | --- |
| Surefire XML/text reports | `target/surefire-reports` | CI-readable pass/fail/skip results |
| Console logs | Maven output | Test start/end and failure diagnostics |
| Screenshots | `target/screenshots` | Visual debugging for failed UI tests |

### Error handling

- Unsupported browser values fail fast with `IllegalArgumentException`.
- Missing valid credentials skip only credential-dependent tests via JUnit assumptions.
- Explicit-wait timeouts surface as normal test failures and trigger screenshots.
- Teardown always attempts to quit the browser and clean temporary profiles.
- Diagnostic failures must not mask the original test failure.

### Security controls

- Default target URL uses HTTPS.
- Valid credentials come only from runtime configuration.
- Passwords are not committed, printed, or embedded in test names/assertion messages.
- Authentication failure assertions must not require sensitive account disclosure.
- Generated screenshots and reports are build artifacts; CI retention should follow the project security policy.

---

## 10. Requirement Traceability

| Requirement | Automated coverage | Architecture elements |
| --- | --- | --- |
| FR-01: Display customer login form | `AC-UI-001: Login page displays all required elements` | `LoginPage` selectors and visibility methods; explicit wait for login form |
| FR-02: Validate required credentials | `TS-LOG-004: Validation appears for blank username and password` | `submitLogin`, post-submit outcome wait, feedback/native validation observation |
| FR-03: Authenticate registered customers | `TS-LOG-001: Successful login with valid username/email and password` | Runtime credentials, `isLoggedIn`, dashboard/logout observation |
| FR-04: Handle invalid authentication attempts | `TS-LOG-002`, `TS-LOG-003` | Negative data, error-banner feedback, login-form state assertions |
| FR-05: Support Remember me session persistence | `TS-LOG-005` | Remember-me checkbox methods, temporary Chrome profile restart flow |
| FR-06: Navigate to password recovery | `TS-LOG-006` | Lost-password link method and URL assertion |
| NFR-01: Security | All credential-sensitive tests | HTTPS default, secret injection, no committed passwords, no password logging |
| NFR-02: Reliability | All tests | Fresh browser per test, explicit waits, deterministic negative data, sequential execution |
| NFR-03: Compatibility | Chrome/Firefox Maven runs | `BaseTest` browser switch, WebDriverManager, headless option |
| NFR-04: Usability | UI/feedback scenarios | Visibility and understandable-feedback checks |
| NFR-05: Observability/testability | All scenarios | Page state, URL state, user-facing messages, reports, screenshots |

Every functional requirement in `requirements.md` has corresponding page-object support, test scenario coverage, and diagnostic/reporting support.

---

## 11. Risks, Assumptions, and Open Decisions

### Risks

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Live site unavailable or slow | False failures/timeouts | Explicit waits, page-load timeout, rerun only after confirming site health |
| UI text or WooCommerce markup changes | Selector/message assertion failures | Keep selectors isolated in `LoginPage`; use meaningful message tokens |
| Missing valid test account in CI | Successful-login and Remember-me tests skipped | Require CI secrets for full regression coverage |
| Remember-me policy changes or cookie expiration | Persistence test may fail despite framework health | Confirm application session policy with product owner |
| Captured screenshots may show account page details | Artifact sensitivity | Secure CI artifacts and avoid using real customer accounts |

### Assumptions

- A registered non-production or approved test customer account exists.
- The live My Account URL remains accessible over HTTPS.
- Chrome and Firefox latest stable desktop are the supported browsers.
- The exact wording of messages may vary, but equivalent customer-facing feedback is acceptable.
- Sequential execution is sufficient for the current capstone scope.

### Open decisions for later stages

| Decision | Owner/input needed | Current recommendation |
| --- | --- | --- |
| Official CI secret names and storage | DevOps/project owner | Use the environment variable names already supported by `TestConfig` |
| Whether Firefox must validate Remember-me across restart | Product/QA owner | Keep Chrome-only persistence unless explicitly required |
| Test account lifecycle and reset process | Product/QA owner | Use a stable dedicated test account; avoid real customer data |
| Artifact retention duration for screenshots | Security/DevOps | Retain only as long as needed for debugging |
| Remote/parallel execution | QA/DevOps future planning | Defer until local sequential suite is stable |

---

## 12. Validation Checklist

- [x] Every functional requirement has architectural coverage.
- [x] Component responsibilities are clear and single-purpose.
- [x] Configuration/data flow from Maven/JUnit to page objects is complete.
- [x] Technology choices match `pom.xml` and are justified.
- [x] Explicit waits, failure handling, screenshots, and reporting are covered.
- [x] Credential safety and HTTPS expectations are addressed.
- [x] Design is simple, maintainable, and ready for implementation/review.

**Next SDLC stage:** Stage 3 — Architecture/design review.  
**Suggested commit message:** `[Architecture] Propose Selenium test automation architecture`
