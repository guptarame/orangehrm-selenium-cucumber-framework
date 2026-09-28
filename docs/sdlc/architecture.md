# Architecture Document

**Feature:** OrangeHRM Login Automation Framework
**Source Requirements:** `docs/sdlc/requirements.md` (FR-1..FR-8, NFR-1..NFR-6, US-1 / US-AUTH-001)
**Date:** 2026-09-28
**Agent:** architecture-agent

> Legend: **[IMPLEMENTED]** = verified present in the current codebase. **[PROPOSED]** = recommended improvement, not yet built. Everything not tagged PROPOSED is IMPLEMENTED. This document re-validates the existing, already-working framework against the freshly regenerated `requirements.md` — it is not a greenfield design.

---

## 1. Project Context and Overview

This is a Selenium WebDriver + Cucumber (Gherkin) + JUnit 4 + Maven test automation framework, written in Java 11, that exercises the OrangeHRM demo application's login module (`https://opensource-demo.orangehrmlive.com/web/index.php/auth/login`). It follows the Page Object Model (POM):

- `login.feature` (Gherkin) expresses 10 independent scenarios (`@TS_LOG_001`–`@TS_LOG_010`) against a shared `Background` (navigate to the login page).
- `LoginSteps` binds each Gherkin step to Page Object calls and JUnit assertions — no raw Selenium calls or business logic live in the step class.
- `LoginPage`, `DashboardPage`, `ResetPasswordPage` encapsulate locators/actions/state for their respective pages, all extending `BasePage`, which centralizes the WebDriver reference and every explicit-wait helper.
- `DriverManager` owns WebDriver creation/teardown (Chrome/Firefox/Edge) behind a `ThreadLocal`.
- `Hooks` runs `@Before`/`@After` per scenario: driver bootstrap, failure screenshot capture, driver teardown.
- `ConfigReader` is a thread-safe singleton that loads `src/test/resources/config.properties` once and lets any key be overridden by a non-empty JVM `-D` system property.
- `TestRunner` is the JUnit 4 `@RunWith(Cucumber.class)` entry point Maven Surefire executes, and it configures Cucumber's reporting plugins.

The framework automates all 10 scenarios in `login.feature`, giving scenario-level coverage for FR-1 through FR-8. NFR-1 (browser compatibility) and NFR-2 (JavaScript requirement) are partially covered by construction (the suite always drives a real, JS-enabled browser); NFR-3..NFR-6 (availability, performance, security beyond masking, accessibility) are **not** exercised by named test scenarios — see §7. This document describes the framework as it stands today and separately calls out proposed, not-yet-built improvements.

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
| Page Objects | Encapsulate locators and page-specific actions/state readers; no assertions | `WebDriver` (via `BasePage`), `ConfigReader` (explicit wait) | Fluent action methods; boolean/string state readers | selenium-java | `pages.BasePage`, `pages.LoginPage`, `pages.DashboardPage`, `pages.ResetPasswordPage` |

**Verified dependency versions (`pom.xml`):** Java 11 (`maven.compiler.source/target`), Selenium Java `4.27.0`, Cucumber Java/JUnit `7.20.1`, JUnit `4.13.2`, WebDriverManager `5.9.2`, Maven Compiler Plugin `3.13.0`, Maven Surefire Plugin `3.5.2`, SLF4J Simple `2.0.16`. No unexpected additions found. No new technology is introduced by this cycle — the existing stack fully covers the (unchanged) FR set.

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
  └─ Surefire (includes only **/TestRunner.java; passes -Dbrowser as a system property,
                default "chrome" from the pom.xml <browser> property)
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

Each scenario gets its own browser session (fresh `WebDriver` per `@Before`/`@After` pair), so scenarios are independent and order-agnostic — important for the negative/validation scenarios (FR-5, FR-6, FR-7) where leftover form state from a previous scenario could mask a bug.

---

## 5. Technology and Configuration Decisions

### 5.1 Browser support (NFR-1)
`DriverManager.initDriver()` switches on `ConfigReader.getBrowser()` (`chrome` | `firefox` | `edge`, defaulting to `chrome`), using WebDriverManager to auto-resolve driver binaries for each. Chrome gets `--remote-allow-origins=*`, `--start-maximized`, `--disable-notifications` (plus `--headless=new` + fixed window size when headless); Firefox gets `-headless` when headless; Edge gets `--headless=new` when headless. All three are real, JS-capable browser engines, which is how the framework structurally satisfies "modern web browser with internet access" (NFR-1) and "JavaScript enabled" (NFR-2) — there is no headless/no-JS execution path. Cross-browser *matrix* execution (running all three in one CI pass) is not configured; browser choice is a single run-time parameter (`-Dbrowser=...`).

### 5.2 Thread-local driver vs. parallel execution
`DriverManager` stores the `WebDriver` in a `ThreadLocal`, which makes it **safe** to run under parallel execution — but this is only the safety mechanism, not actual parallelism. `pom.xml`'s Surefire plugin has no `<parallel>`/`<forkCount>` settings, and `TestRunner`'s `@CucumberOptions` has no JUnit-Platform/parallel configuration, nor is `RemoteWebDriver`/Selenium Grid used anywhere in `DriverManager`. Today's suite runs single-threaded, sequentially, locally — consistent with the required architecture principle ("sequential execution now; parallel/remote documented as future work only"). **[PROPOSED — future work only]** Real parallelism would require moving to the Cucumber JUnit Platform engine (or Surefire fork/thread config) plus a CI/Grid-aware `RemoteWebDriver` branch in `DriverManager`. Not part of this cycle.

### 5.3 Waits
- Implicit wait: `driver.manage().timeouts().implicitlyWait(...)`, from `implicit.wait` (default 5s if the key is absent from `config.properties`; the file currently sets it to `5`).
- Page load timeout: from `page.load.timeout` (default 30s; configured to `30`).
- Explicit waits: `BasePage` builds one `WebDriverWait` per page object from `explicit.wait` (default 15s; configured to `15`) and exposes `waitForVisible`/`waitForAllVisible`/`waitForClickable`/`waitForUrlContains` helpers; `isDisplayed(locator, timeoutSeconds)` uses a short, call-site-specific timeout wrapped in try/catch and returns `false` on timeout instead of throwing — used deliberately so "element genuinely absent" (e.g. no `Invalid credentials` banner) is a first-class outcome for negative assertions, not a masked failure.
- No `Thread.sleep()` exists anywhere in the codebase (verified) — all synchronization goes through `WebDriverWait`. The one inherently timing-sensitive check is `LoginPage.isLoadingIndicatorDisplayed()` (FR-4), which polls for a transient spinner with only a 3-second window immediately after the login click, invoked synchronously inside the same `When` step (`submitAndCaptureLoadingIndicator`) rather than in a later `Then` step, specifically to avoid a race where the spinner appears and disappears before a separate wait begins.

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
**Actual configuration flow (system property → environment variable → default) as implemented today:**
1. **JVM system property** (`-Dkey=value` on the `mvn test` command line, or a `<systemPropertyVariables>` entry in `pom.xml`'s Surefire config, currently only `browser`) — wins whenever the value is non-blank.
2. **`config.properties`** (`src/test/resources/config.properties`, loaded once by the `ConfigReader` singleton) — used when no non-blank system property exists for that key.
3. **Hard-coded getter default** (`getBrowser()`→`"chrome"`, `isHeadless()`→`false`, `getImplicitWait()`→`5`, `getExplicitWait()`→`15`, `getPageLoadTimeout()`→`30`) — used only if the key is *also* absent from `config.properties`. Since the checked-in file sets all five explicitly, these defaults are a documented fallback, not the active values today.
4. **No-default (required) getters** — `getBaseUrl()`, `getValidUsername()`, `getValidPassword()` call `get(key)` with no fallback and throw `RuntimeException` if the key is missing from both the system property and the properties file.

**Gap — no true OS environment-variable step exists.** `ConfigReader` reads only `System.getProperty(...)`; it never calls `System.getenv(...)`. So step 1 above is a JVM `-D` flag, not an OS environment variable — a CI system that only sets OS env vars (rather than passing `-D...` to Maven) would have no effect on this framework as written. **[PROPOSED]** if genuine environment-variable support is desired (e.g. for CI secret injection without shell-escaping `-D` flags), add an `System.getenv(key)` lookup in `ConfigReader.get()` between steps 1 and 2.

**Note on prior cycle's documented gap:** an earlier architecture draft flagged an unused Maven `<env>` property passed to Surefire. The current `pom.xml` defines no `<env>` property at all (only `<browser>`) — that dead configuration has been removed since the last cycle and is no longer a gap.

### 5.5 Credential handling and safety
- `config.properties` exposes `valid.username`/`valid.password` as plaintext properties, read only via `ConfigReader.getValidUsername()`/`getValidPassword()`. All `login.feature` scenarios that need a "valid" username or password (`@TS_LOG_001`–`@TS_LOG_003`, `@TS_LOG_005`–`@TS_LOG_007`, `@TS_LOG_009`) resolve it through these getters in `LoginSteps` — verified no scenario step or step-definition method hardcodes a literal valid-credential string; only the deliberately-invalid values (`"wrongpass"`, `"InvalidUser"`) and deliberately-empty values (`""`) appear as literals in the feature file, which is correct per the "credentials via config/env, never hardcoded" principle.
- **Externalization gap (unchanged from prior cycle):** `config.properties` is a checked-in, plaintext file with the demo credential (`Admin`/`admin123`) committed to source control. This is acceptable for OrangeHRM's public opensource-demo instance but would not be acceptable for a real secured environment. **[PROPOSED]** externalize `valid.username`/`valid.password` to CI secrets injected as `-D` system properties at runtime — `ConfigReader`'s override mechanism already supports this with zero code change; only the plaintext defaults need to be removed from the committed file and required at run time instead.
- No credential value is reproduced in this document.

### 5.6 Reporting
`TestRunner`'s `@CucumberOptions.plugin` configures exactly: `pretty` (console), `summary` (console), `html:target/cucumber-reports/cucumber-html-report.html`, `json:target/cucumber-reports/cucumber.json`, `junit:target/cucumber-reports/cucumber-junit.xml`, with `monochrome = true`. No other report formats (e.g., Allure, ExtentReports) are configured or should be assumed.

### 5.7 Failure screenshots
`Hooks.tearDown` checks `scenario.isFailed()` and, only in that case, casts the driver to `TakesScreenshot`, captures `OutputType.BYTES`, and calls `scenario.attach(bytes, "image/png", <scenario-name>-failure)`. The screenshot is embedded into the Cucumber HTML/JSON report via the scenario-attachment mechanism — it is **not** additionally written to a standalone file on disk.

---

## 6. Error Handling, Security, Scalability, Success Criteria

**Error handling**
- Missing/blank required config (`base.url`, `valid.username`, `valid.password`) fails fast with a `RuntimeException` from `ConfigReader.get()`, surfaced at first use (`Hooks.setUp` → first `LoginSteps` step that needs it).
- Element-not-found/timeout during a step surfaces as a Selenium `TimeoutException` (from `BasePage`'s `WebDriverWait`) or an `AssertionError` (from `LoginSteps`' JUnit assertions), both of which mark the scenario failed and trigger the screenshot-on-failure path in `Hooks`.
- `isDisplayed(locator, timeoutSeconds)` deliberately swallows the wait exception and returns `false`, so "element absent" is a first-class outcome for negative/validation assertions rather than a thrown error.

**Security (NFR-5):** Password masking is architecturally verified via `LoginPage.getPasswordFieldType()` asserting `type="password"` (FR-2/§7). Beyond masking, requirements.md explicitly states NFR-5 is "not specified in the source PRD" (no transport-encryption, session-timeout, or account-lockout requirement) — the framework has no coverage here because none is required yet. Credential handling gap is documented in §5.5.

**Scalability boundaries**
- Single JVM, single browser session per scenario, sequential execution — verified, not parallel (§5.2). This matches the required principle that parallel/remote execution is future work only.
- Three local browsers only; no Selenium Grid/`RemoteWebDriver`/cloud-grid (BrowserStack/Sauce Labs) integration exists.
- Suite scope is the login module only (`login.feature`); `DashboardPage`/`ResetPasswordPage` exist only as landing-page verifiers for login scenarios, not as entry points for broader HR-module test coverage.
- **[PROPOSED — future work only]** boundaries to revisit if scope grows: CI parallel fork/thread configuration, a `RemoteWebDriver`/Grid branch in `DriverManager`, and a genuine environment-variable/multi-environment configuration path (§5.4).

**Success criteria** (mirrors requirements.md "Success Criteria" section, restated architecturally):
- `mvn test` runs all 10 scenarios in `login.feature` to completion, each in an isolated browser session, and exits non-zero if any scenario fails (`testFailureIgnore=false`).
- Every scenario failure produces an attached screenshot and appears in all three report formats (§5.6).
- Switching `browser`/`headless`/wait values via `-D` system properties changes framework behavior without editing `config.properties` or code (§5.4).
- All FR-1..FR-8 acceptance criteria have at least one passing, independent Cucumber scenario (§7).

---

## 7. Traceability: Requirements → Implementation

| Requirement | login.feature Scenario(s) | Implementing Step(s) / Page Object Method(s) | Status |
|---|---|---|---|
| FR-1 (core UI controls displayed) | `@TS_LOG_008` | `all_required_login_controls_should_be_displayed` → `LoginPage.areAllLoginControlsDisplayed()` | Covered |
| FR-2 (password masking) | `@TS_LOG_007` | `the_password_field_should_mask_the_entered_characters` → `LoginPage.getPasswordFieldType()` | Covered |
| FR-3 (valid login → dashboard redirect) | `@TS_LOG_001` | `the_user_should_be_redirected_to_the_dashboard_page` → `DashboardPage.isDashboardPageDisplayed()` / `getPageHeaderText()` | Covered |
| FR-4 (loading indicator during auth) | `@TS_LOG_009` | `a_loading_indicator_should_be_displayed_during_authentication` → `LoginPage.submitAndCaptureLoadingIndicator()` / `isLoadingIndicatorDisplayed()` (3s poll window) | Covered — note: short 3s check window, inherently timing-sensitive |
| FR-5 (invalid credentials rejected) | `@TS_LOG_002`, `@TS_LOG_003` | `the_user_should_remain_on_the_login_page` / `the_error_banner_should_be_displayed` → `LoginPage.isErrorBannerDisplayed()`/`getErrorBannerText()` | Covered |
| FR-6 (empty username validation) | `@TS_LOG_004`, `@TS_LOG_005` | `LoginPage.isUsernameRequiredErrorDisplayed()`/`getUsernameRequiredErrorText()` | Covered |
| FR-7 (empty password validation) | `@TS_LOG_004`, `@TS_LOG_006` | `LoginPage.isPasswordRequiredErrorDisplayed()`/`getPasswordRequiredErrorText()` | Covered |
| FR-8 (forgot-password navigation) | `@TS_LOG_010` | `the_user_clicks_the_link` → `LoginPage.clickForgotPassword()`; `ResetPasswordPage.isResetPasswordPageDisplayed()` | Covered |
| NFR-1 (browser compatibility) | All scenarios (implicitly) | `DriverManager.initDriver()` drives a real Chrome/Firefox/Edge engine per run | Partially covered — real-browser execution satisfies the requirement structurally, but there is no cross-browser CI matrix (only a single `-Dbrowser` choice per run); no dedicated scenario asserts compatibility itself |
| NFR-2 (JavaScript required) | All scenarios (implicitly) | Same as NFR-1 — no headless/no-JS path exists in `DriverManager` | Partially covered — the suite can only run with JS enabled (no code path disables it), but no scenario explicitly asserts "JS is required for X to work" |
| NFR-3 (server/DB availability) | — | No health-check/precondition step exists; a down server would simply fail scenario steps with a `TimeoutException` | **Gap** — not architected as a distinct check; requirements.md gives no specific SLA to test against |
| NFR-4 (performance) | — | Not specified in requirements; no timing assertions in the framework | **Gap** — explicitly out of scope per requirements.md ("not specified in the source PRD") |
| NFR-5 (security, beyond FR-2) | `@TS_LOG_007` covers password masking only | — | **Gap** — no lockout/rate-limit/session/transport-encryption coverage; requirements.md notes none were specified |
| NFR-6 (accessibility) | — | No accessibility assertions (e.g., ARIA, contrast, keyboard nav) anywhere in `pages/` or `stepdefinitions/` | **Gap** — not specified in requirements, not implemented |

**Extra automation beyond the PRD's own test-ID table:** `@TS_LOG_008`–`@TS_LOG_010` extend past the PRD's `TS_LOG_001`–`TS_LOG_007` range but map to acceptance criteria explicitly called out in requirements.md's Overview/User Stories (full UI display FR-1, loading indicator FR-4, forgot-password navigation FR-8) — no new gap introduced by this architecture.

---

**Next Stage:** Design/implementation review of the above against `docs/sdlc/impl-plan.md` (if scope changes) or straight to verification if no code changes are required this cycle.
