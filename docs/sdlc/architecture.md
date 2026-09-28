# Architecture Document

**Feature:** OrangeHRM Login Automation Framework
**Source Requirements:** `docs/sdlc/requirements.md` (FR-1..FR-8, NFR-1..NFR-5, US-AUTH-001)
**Date:** 2026-09-27
**Agent:** architecture-agent

> Legend: **[IMPLEMENTED]** = verified present in the current codebase. **[PROPOSED]** = recommended improvement, not yet built. Everything not tagged PROPOSED is IMPLEMENTED.

---

## 1. Project Context and Overview

This is a Selenium WebDriver + Cucumber (Gherkin) + JUnit 4 + Maven test automation framework, written in Java 11, that exercises the OrangeHRM demo application's login module (`https://opensource-demo.orangehrmlive.com/web/index.php/auth/login`). It implements the Page Object Model (POM):

- `login.feature` (Gherkin) expresses 10 independent scenarios (`@TS_LOG_001`–`@TS_LOG_010`) against a shared `Background` (navigate to the login page).
- `LoginSteps` binds each Gherkin step to Page Object calls and JUnit assertions — no Selenium calls or business logic live in the step class.
- `LoginPage`, `DashboardPage`, `ResetPasswordPage` encapsulate locators/actions/state for their respective pages, all extending `BasePage`, which centralizes explicit-wait helpers.
- `DriverManager` owns WebDriver creation/teardown (Chrome) behind a `ThreadLocal`.
- `Hooks` runs `@Before`/`@After` per scenario: driver bootstrap, failure screenshot capture, driver teardown.
- `ConfigReader` is a singleton that loads `src/test/resources/config.properties` and lets any key be overridden by a non-empty JVM `-D` system property.
- `TestRunner` is the JUnit4 `@RunWith(Cucumber.class)` entry point Maven Surefire executes, and it configures Cucumber's reporting plugins.

The framework currently automates all 10 scenarios enumerated in `login.feature`, giving scenario-level coverage for FR-1 through FR-8. NFR-1..NFR-5 are largely **not** exercised by named test scenarios (see §7). This document describes the framework as it stands and separately calls out proposed, not-yet-built improvements.

---

## 2. Components

| Component | Responsibility | Inputs | Outputs | Dependencies | Key Class(es) |
|---|---|---|---|---|---|
| Test Runner | JUnit entry point; wires Cucumber to feature files and glue packages; configures report plugins | `src/test/resources/features/*.feature`, glue packages | Console (`pretty`, `summary`) + HTML/JSON/JUnit-XML reports under `target/cucumber-reports/` | cucumber-junit, junit | `runner.TestRunner` |
| Feature/Scenarios | Behavior specification in Gherkin, one scenario per acceptance case | Requirements (FR-1..FR-8) | Executable, tagged scenarios | Cucumber Gherkin parser | `src/test/resources/features/login.feature` |
| Step Definitions | Bind Gherkin step text to Page Object calls; assert outcomes | Parsed step text/parameters, `DriverManager.getDriver()` | Pass/fail JUnit assertions | cucumber-java, junit `Assert`, Page Objects | `stepdefinitions.LoginSteps` |
| Lifecycle Hooks | Per-scenario browser setup/teardown; failure screenshot capture; logging | Cucumber `Scenario` object | Fresh `WebDriver` per scenario; PNG attached to the scenario **only on failure** | `DriverManager`, SLF4J | `hooks.Hooks` |
| Driver Management | Creates/returns/quits a thread-local `WebDriver` for the configured browser; applies timeouts | `ConfigReader` (browser, headless, waits) | A live, timeout-configured `WebDriver` session | selenium-java, webdrivermanager | `driver.DriverManager` |
| Configuration | Loads `config.properties` from the classpath once (singleton); resolves `-D` overrides; typed getters | `config.properties`, JVM system properties | Typed config values (String/boolean/int) | java.util.Properties | `config.ConfigReader` |
| Page Objects | Encapsulate locators and page-specific actions/state readers | `WebDriver` (via `BasePage`), `ConfigReader` (explicit wait) | Fluent action methods; boolean/string state readers | selenium-java | `pages.BasePage`, `pages.LoginPage`, `pages.DashboardPage`, `pages.ResetPasswordPage` |

**Verified dependency versions (`pom.xml`, all match the recommended stack exactly):** Java 11 (`maven.compiler.source/target`), Selenium Java `4.27.0`, Cucumber Java/JUnit `7.20.1`, JUnit `4.13.2`, WebDriverManager `5.9.2`, Maven Compiler Plugin `3.13.0`, Maven Surefire Plugin `3.5.2`, SLF4J Simple `2.0.16`. No unexpected additions or mismatches found.

---

## 3. Directory Structure

```
orangehrm-selenium-cucumber-framework/
├── pom.xml
├── src/test/java/com/orangehrm/
│   ├── config/ConfigReader.java
│   ├── driver/DriverManager.java
│   ├── hooks/Hooks.java
│   ├── pages/
│   │   ├── BasePage.java
│   │   ├── LoginPage.java
│   │   ├── DashboardPage.java
│   │   └── ResetPasswordPage.java
│   ├── runner/TestRunner.java
│   └── stepdefinitions/LoginSteps.java
└── src/test/resources/
    ├── config.properties
    └── features/login.feature
```

Reports are generated at runtime under `target/cucumber-reports/` (not checked in).

---

## 4. Test Execution Flow

```
mvn test
  └─ Surefire (includes only **/TestRunner.java; passes -Dbrowser / -Denv as system properties)
       └─ TestRunner  [@RunWith(Cucumber.class), @CucumberOptions]
            ├─ loads features:  src/test/resources/features/**
            ├─ loads glue:      com.orangehrm.stepdefinitions, com.orangehrm.hooks
            └─ per scenario (independent, isolated):
                 1. Hooks.setUp (@Before)      → DriverManager.getDriver() lazily launches a new
                                                   browser session on the current thread
                 2. Gherkin steps execute       → LoginSteps → LoginPage/DashboardPage/
                                                   ResetPasswordPage (via BasePage explicit waits)
                 3. JUnit Assert in LoginSteps  → scenario pass/fail
                 4. Hooks.tearDown (@After)     → if scenario.isFailed(): capture PNG via
                                                   TakesScreenshot, scenario.attach(...)
                                                 → DriverManager.quitDriver() (closes browser,
                                                   clears the ThreadLocal)
       └─ Cucumber plugins emit: pretty + summary (console), cucumber-html-report.html,
            cucumber.json, cucumber-junit.xml → target/cucumber-reports/
```

Each scenario gets its own browser session (fresh `WebDriver` per `@Before`/`@After` pair), so scenarios are independent and order-agnostic — important for the negative/validation scenarios where leftover form state could mask a bug.

---

## 5. Technology and Configuration Decisions

### 5.1 Browser support
`DriverManager.initDriver()` switches on `ConfigReader.getBrowser()` (`chrome` | `firefox` | `edge`, defaulting to `chrome`), using WebDriverManager to resolve driver binaries automatically for each. Chrome gets `--remote-allow-origins=*`, `--start-maximized`, `--disable-notifications` (plus `--headless=new` + fixed window size when headless); Firefox gets `-headless` when headless; Edge gets `--headless=new` when headless. All three currently-supported browsers are preserved as-is.

### 5.2 Thread-local driver vs. parallel execution
`DriverManager` stores the `WebDriver` in a `ThreadLocal`, which makes it **safe** to run under parallel execution — but **[IMPLEMENTED]** only covers the safety mechanism. Parallel/remote execution itself is **not** configured: `pom.xml`'s Surefire plugin has no `<parallel>`/`<forkCount>` settings, and `TestRunner`'s `@CucumberOptions` has no parallel/JUnit-Platform configuration or `RemoteWebDriver`/Selenium Grid usage anywhere in `DriverManager`. Today's suite runs single-threaded, sequentially, locally. **[PROPOSED]** Enabling real parallelism would require moving to the Cucumber JUnit Platform engine (or Surefire fork/thread config) plus a CI/Grid-aware `RemoteWebDriver` path in `DriverManager`.

### 5.3 Waits
- Implicit wait: `driver.manage().timeouts().implicitlyWait(...)`, configured from `implicit.wait` (default 5s if key absent, but `config.properties` sets it explicitly to `5`).
- Page load timeout: from `page.load.timeout` (default 30s; configured to `30`).
- Explicit waits: `BasePage` builds one `WebDriverWait` per page object from `explicit.wait` (default 15s; configured to `15`) and exposes `waitForVisible`/`waitForAllVisible`/`waitForClickable`/`waitForUrlContains` helpers; `isDisplayed(locator, timeoutSeconds)` uses a short, call-site-specific timeout wrapped in try/catch to return `false` on timeout instead of throwing.
- These two mechanisms coexist without conflicting: a small implicit wait acts as a safety floor for the driver overall, while all real assertions/synchronization go through `BasePage`'s explicit `WebDriverWait`s. No additional wait strategy is recommended — the existing split is sufficient and should not be duplicated with, e.g., `Thread.sleep`.

### 5.4 Configuration precedence (verified in `ConfigReader.java`)
```java
public String get(String key) {
    String systemProperty = System.getProperty(key);
    if (systemProperty != null && !systemProperty.trim().isEmpty()) {
        return systemProperty.trim();     // -Dkey=value wins if non-empty
    }
    String value = properties.getProperty(key);   // else config.properties
    if (value == null) throw new RuntimeException("Missing required configuration property: " + key);
    return value.trim();
}
```
- **Precedence:** a non-empty JVM system property (`-Dkey=value`) always overrides the value in `config.properties`. An empty/blank `-D` value is ignored (falls through to the file).
- **No environment-variable support exists.** `ConfigReader` reads only `System.getProperty`, never `System.getenv`. The `README`/comments describing `-D` overrides are accurate; there is no `.env` or OS-environment-variable mechanism.
- **Typed getters and their defaults:** `getBrowser()` → `"chrome"`; `isHeadless()` → `false`; `getImplicitWait()` → `5`; `getExplicitWait()` → `15`; `getPageLoadTimeout()` → `30`. These defaults only apply if the key is *also absent* from `config.properties` — today `config.properties` sets all five explicitly, so the getter defaults are a documented fallback, not the active values.
- **No-default (required) getters:** `getBaseUrl()`, `getValidUsername()`, `getValidPassword()` call `get(key)` with no fallback and throw `RuntimeException` if the key is missing from both sources.
- **Gap:** `pom.xml` defines a Maven property `<env>qa</env>` and passes it to Surefire as system property `env`, but no class under `src/test/java` reads `env` (`System.getProperty("env")` or a `ConfigReader.get("env", ...)` call was not found). This is dead configuration today — **[PROPOSED]** either wire it into environment-specific `base.url` resolution or remove it to avoid implying multi-environment support that doesn't exist.

### 5.5 Credential handling and safety
- `config.properties` exposes `valid.username` / `valid.password` as plaintext properties, read only via `ConfigReader.getValidUsername()`/`getValidPassword()`. These are used by the positive-path steps (`the user logs in with valid credentials`, `the user submits valid credentials`, and the mixed valid/invalid steps) so the "known-good" credential is centralized in one file rather than hardcoded in step code.
- `login.feature` itself hardcodes literal credential values directly in Gherkin step text for several scenarios (`@TS_LOG_002`, `@TS_LOG_005`, `@TS_LOG_006`, `@TS_LOG_007` all pass an explicit username/password string in the step, rather than routing through `ConfigReader`). This means the same logical "valid password" value exists in two places (the properties file and the feature file), which is a **gap**: a future credential rotation must be applied in both, and the feature file is the less centralized of the two.
- **Externalization gap:** `config.properties` is a checked-in, plaintext file under source control with the demo credential in it. This is acceptable for OrangeHRM's public opensource-demo instance but would not be acceptable for a real secured environment. **[PROPOSED]** externalize `valid.username`/`valid.password` to CI secrets injected as `-D` system properties at runtime (already supported by `ConfigReader`'s override mechanism, so no code change would be needed — only removing the plaintext defaults from the committed file and requiring the `-D` values at run time).
- No credential value is reproduced in this document.

### 5.6 Reporting
`TestRunner`'s `@CucumberOptions.plugin` configures exactly: `pretty` (console), `summary` (console), `html:target/cucumber-reports/cucumber-html-report.html`, `json:target/cucumber-reports/cucumber.json`, `junit:target/cucumber-reports/cucumber-junit.xml`, with `monochrome = true`. No other report formats (e.g., Allure, ExtentReports) are configured or should be assumed.

### 5.7 Failure screenshots
`Hooks.tearDown` checks `scenario.isFailed()` and, only in that case, casts the driver to `TakesScreenshot`, captures `OutputType.BYTES`, and calls `scenario.attach(bytes, "image/png", <name>-failure)`. The screenshot is embedded into the Cucumber HTML/JSON report via the scenario attachment mechanism — it is **not** additionally written to a standalone file on disk.

---

## 6. Error Handling, Credential Safety, Scalability Boundaries, Success Criteria

**Error handling**
- Missing/blank required config (`base.url`, `valid.username`, `valid.password`) fails fast with a `RuntimeException` from `ConfigReader.get()`, surfaced at first use (typically in `Hooks.setUp` → `LoginSteps`).
- Element-not-found/timeout during a step surfaces as a Selenium `TimeoutException` (from `BasePage`'s `WebDriverWait`) or an `AssertionError` (from `LoginSteps`' JUnit assertions), both of which mark the scenario failed and trigger the screenshot-on-failure path in `Hooks`.
- `isDisplayed(locator, timeoutSeconds)` deliberately swallows the wait exception and returns `false`, so "element absent" is a first-class outcome for negative/validation assertions rather than a thrown error.

**Credential safety:** see §5.5. Summary: centralized `ConfigReader`-mediated credentials for positive-path scenarios; some scenarios use literal Gherkin-level values; plaintext checked-in file is an externalization gap for anything beyond a public demo app.

**Scalability boundaries**
- Single JVM, single browser session per scenario, sequential execution — verified, not parallel (§5.2).
- Three local browsers only; no Selenium Grid/`RemoteWebDriver`/cloud-grid (BrowserStack/Sauce Labs) integration exists.
- Suite scope is the login module only (`login.feature`); `DashboardPage`/`ResetPasswordPage` exist only as landing-page verifiers for login scenarios, not as entry points for broader HR-module test coverage.
- **[PROPOSED]** boundaries to revisit if scope grows: CI parallel fork/thread configuration, a `RemoteWebDriver`/Grid branch in `DriverManager`, and splitting `config.properties` per environment (using the currently-unused `env` property, §5.4).

**Success criteria** (mirrors requirements.md §9, restated architecturally):
- `mvn test` runs all 10 scenarios in `login.feature` to completion, each in an isolated browser session, and exits non-zero if any scenario fails (`testFailureIgnore=false`).
- Every scenario failure produces an attached screenshot and appears in all three report formats.
- Switching `browser`/`headless`/wait values via `-D` system properties changes framework behavior without editing `config.properties` or code.

---

## 7. Traceability: Requirements → Implementation

| Requirement | login.feature Scenario(s) | Implementing Step(s) / Page Object Method(s) | Status |
|---|---|---|---|
| FR-1 (core UI controls displayed) | `@TS_LOG_008` | `all_required_login_controls_should_be_displayed` → `LoginPage.areAllLoginControlsDisplayed()` | Covered |
| FR-2 (password masking) | `@TS_LOG_007` | `the_password_field_should_mask_the_entered_characters` → `LoginPage.getPasswordFieldType()` | Covered |
| FR-3 (valid login → dashboard redirect) | `@TS_LOG_001` | `the_user_should_be_redirected_to_the_dashboard_page` → `DashboardPage.isDashboardPageDisplayed()` / `getPageHeaderText()` | Covered |
| FR-4 (loading indicator during auth) | `@TS_LOG_009` | `a_loading_indicator_should_be_displayed_during_authentication` → `LoginPage.isLoadingIndicatorDisplayed()` (3s window) | Covered — note: short 3s check window, inherently timing-sensitive |
| FR-5 (invalid credentials rejected) | `@TS_LOG_002`, `@TS_LOG_003` | `the_user_should_remain_on_the_login_page` / `the_error_banner_should_be_displayed` → `LoginPage.isErrorBannerDisplayed()`/`getErrorBannerText()` | Covered |
| FR-6 (empty username validation) | `@TS_LOG_004`, `@TS_LOG_005` | `LoginPage.isUsernameRequiredErrorDisplayed()`/`getUsernameRequiredErrorText()` | Covered |
| FR-7 (empty password validation) | `@TS_LOG_004`, `@TS_LOG_006` | `LoginPage.isPasswordRequiredErrorDisplayed()`/`getPasswordRequiredErrorText()` | Covered |
| FR-8 (forgot-password navigation) | `@TS_LOG_010` | `the_user_clicks_the_link` → `LoginPage.clickForgotPassword()`; `ResetPasswordPage.isResetPasswordPageDisplayed()` | Covered |
| NFR-1 (compatibility: modern browser + JS) | — | Framework supports Chrome (`DriverManager`), which exercises real browser JS engines, but no scenario explicitly targets "JS enabled" or cross-browser-matrix execution | **Gap** — no dedicated test; browser choice is a run parameter, not multi-browser CI coverage |
| NFR-2 (availability of server/DB) | — | No health-check/precondition step exists; a down server would simply fail scenario steps with a timeout | **Gap** — not architected as a distinct check |
| NFR-3 (performance) | — | Not specified in requirements; no timing assertions in the framework | **Gap** — explicitly out of scope per requirements.md |
| NFR-4 (security, beyond FR-2) | `@TS_LOG_007` covers password masking only | — | **Gap** — no lockout/rate-limit/session/encryption coverage; requirements.md notes none were specified |
| NFR-5 (accessibility) | — | No accessibility assertions (e.g., ARIA, contrast, keyboard nav) anywhere in `pages/` or `stepdefinitions/` | **Gap** — not specified in requirements, not implemented |

**Extra automation beyond the PRD's own test-ID table:** `@TS_LOG_008`–`@TS_LOG_010` extend past the PRD's `TS_LOG_001`–`TS_LOG_007` range but map to PRD acceptance criteria (AC1.1, AC2.3, AC4.1) per requirements.md §8 — carried through unchanged here, no new gap introduced by the architecture.

---

**Next Stage:** Design/Implementation review of the above against `docs/sdlc/impl-plan.md` (if scope changes) or straight to verification if no code changes are required this cycle.
