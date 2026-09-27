# Architecture Document

**Feature:** OrangeHRM Login Automation Framework
**Source Requirements:** `docs/sdlc/requirements.md` (US-AUTH-001)
**Date:** 2026-09-27

---

## 1. Project Context and Overview

This is a Selenium + Cucumber + JUnit + Maven test automation framework, written in Java, that exercises the OrangeHRM demo application's login page (`/web/index.php/auth/login`). It follows the Page Object Model (POM): Gherkin scenarios in `login.feature` drive step definitions in `LoginSteps.java`, which delegate all browser interaction to `LoginPage`/`DashboardPage` page objects built on a shared `BasePage`. Browser lifecycle is centralized in `DriverManager`, scenario setup/teardown in Cucumber `Hooks`, and all tunable settings in `ConfigReader`. The framework already implements 7 of the login scenarios required by US-AUTH-001 (`@TS_LOG_001`–`@TS_LOG_007`); this document describes that existing implementation as-is and calls out where it does not yet cover the full requirement set (see §7 Traceability).

---

## 2. Components

| Component | Responsibility | Inputs | Outputs | Key Class(es) |
|-----------|-----------------|--------|---------|----------------|
| Test Runner | JUnit entry point; wires Cucumber to the feature files and glue code; configures reporting plugins | `src/test/resources/features/*.feature`, glue packages | Cucumber pretty/summary console output, HTML/JSON/JUnit-XML reports under `target/cucumber-reports/` | `runner.TestRunner` |
| Feature/Scenarios | Behavior specification in Gherkin | Business requirements | Executable scenarios | `src/test/resources/features/login.feature` |
| Step Definitions | Bind Gherkin steps to page-object calls and assertions | Parsed step text/parameters | Pass/fail assertions | `stepdefinitions.LoginSteps` |
| Lifecycle Hooks | Per-scenario browser setup/teardown, failure screenshot capture | Cucumber `Scenario` object | Fresh `WebDriver` per scenario; PNG attached to the scenario on failure | `hooks.Hooks` |
| Driver Management | Creates and tears down a thread-local WebDriver instance per configured browser | `ConfigReader` (browser, headless, timeouts) | A live `WebDriver` session | `driver.DriverManager` |
| Configuration | Loads `config.properties` from the classpath; resolves runtime overrides | `src/test/resources/config.properties`, JVM `-D` system properties | Typed config values (String/boolean/int) | `config.ConfigReader` |
| Page Objects | Encapsulate locators and UI actions/assertions for a single page | `WebDriver`, `ConfigReader` (via `BasePage`) | Fluent action methods, boolean/string state readers | `pages.BasePage`, `pages.LoginPage`, `pages.DashboardPage` |

**Dependencies (from `pom.xml`):** Selenium Java 4.27.0, WebDriverManager 5.9.2, Cucumber Java/JUnit 7.20.1, JUnit 4.13.2, SLF4J Simple 2.0.16, Maven Compiler Plugin 3.13.0, Maven Surefire Plugin 3.5.2. Java source/target level 11.

---

## 3. Directory Structure

```
src/test/java/com/orangehrm/
├── config/ConfigReader.java
├── driver/DriverManager.java
├── hooks/Hooks.java
├── pages/BasePage.java
├── pages/LoginPage.java
├── pages/DashboardPage.java
├── runner/TestRunner.java
└── stepdefinitions/LoginSteps.java
src/test/resources/
├── config.properties
└── features/login.feature
docs/sdlc/
└── (this pipeline's generated artifacts)
```

---

## 4. Test Execution Flow

1. `mvn test` invokes Surefire, which is configured to include only `**/TestRunner.java` (`pom.xml` `<includes>`), passing `browser`/`env` as system properties.
2. `TestRunner` (`@RunWith(Cucumber.class)`, `@CucumberOptions`) loads scenarios from `src/test/resources/features` and glue from `com.orangehrm.stepdefinitions` and `com.orangehrm.hooks`.
3. For each scenario, Cucumber's `@Before` hook (`Hooks.setUp`) calls `DriverManager.getDriver()`, which lazily creates a new browser session for that thread if one does not already exist.
4. Gherkin steps execute in order, delegating to `LoginSteps`, which constructs `LoginPage`/`DashboardPage` against the current thread's driver and calls their action/assertion methods.
5. On scenario completion, Cucumber's `@After` hook (`Hooks.tearDown`) captures a PNG screenshot via `TakesScreenshot` and attaches it to the scenario **only if the scenario failed**, then calls `DriverManager.quitDriver()` to close the browser and clear the thread-local reference.
6. Cucumber emits `pretty`/`summary` console output plus HTML, JSON, and JUnit-XML reports to `target/cucumber-reports/`.

Each scenario gets an isolated browser session (fresh driver per `@Before`/`@After` pair) — there is no cross-scenario browser reuse. Driver storage is `ThreadLocal`, which makes the framework parallel-safe if a parallel runner/forked Surefire config is introduced later, but **no parallel execution is currently configured** in `pom.xml` or `TestRunner` — this is a possible future capability, not an implemented one.

---

## 5. Technology and Configuration Decisions

Stack matches `pom.xml` exactly: Java 11, Selenium Java 4.27.0, Cucumber Java/JUnit 7.20.1, JUnit 4.13.2, WebDriverManager 5.9.2, SLF4J Simple 2.0.16, Maven Compiler Plugin 3.13.0, Maven Surefire Plugin 3.5.2.

**Configuration precedence (`ConfigReader`):**
1. A non-empty JVM system property (`-Dkey=value`) always wins, for both `get(key)` and `get(key, default)`.
2. Otherwise, the value loaded from `src/test/resources/config.properties` on the classpath is used.
3. Otherwise (for `get(key, default)` overloads only — `getBrowser`, `getBoolean`/`getInt`-backed getters), the caller-supplied default applies. `get(key)` alone (used by `getBaseUrl`, `getValidUsername`, `getValidPassword`) throws a `RuntimeException` if the key is absent from both a system property and the properties file — there is no silent default for required keys.

**No environment-variable support exists.** Only JVM system properties and the classpath properties file are read; this document does not claim otherwise.

**Browser support:** Chrome (default), Firefox, and Edge are all implemented in `DriverManager.initDriver()` via `WebDriverManager`-managed driver binaries, selected by the `browser` config key (case-insensitive; unrecognized values fall back to Chrome). Headless mode is supported for all three via browser-specific flags (`--headless=new` for Chrome/Edge, `-headless` for Firefox).

**Waits:** `BasePage` uses only explicit `WebDriverWait` (`explicit.wait`, default 15s) for all element-visibility/clickability/URL waits; `DriverManager` additionally sets an implicit wait (`implicit.wait`, default 5s) and a page-load timeout (`page.load.timeout`, default 30s) on the driver itself. Mixing an implicit wait with explicit waits is the current, working state of the code — this document does not recommend changing it, only records it as-is.

---

## 6. Error Handling, Credential Safety, and Scalability Boundaries

- **Failure diagnostics:** `Hooks.tearDown` attaches a screenshot to the Cucumber report only on scenario failure, without altering or suppressing the original assertion failure.
- **Credential handling — identified gap:** Login credentials for all seven scenarios (`Admin`/`admin123`, `wrongpass`, `InvalidUser`) are hardcoded as literal strings directly in `login.feature`'s `When`/`Then` steps. Separately, `ConfigReader.getValidUsername()`/`getValidPassword()` expose `valid.username`/`valid.password` from `config.properties`, but **no step definition or page object currently calls these getters** — the two credential sources are disconnected, and the properties-file getters are effectively dead code for the current scenario set. This is a real gap: if credentials ever need to change or be sourced securely (e.g., a CI secret), today it requires editing the feature file text directly. No code in this document reproduces the actual credential values beyond referencing where they live.
- **Scalability boundary:** `ThreadLocal` driver storage makes the code safe to run under a parallel Surefire/Cucumber configuration, but the project does not currently configure parallel forks or a parallel Cucumber runner, so today's execution is strictly sequential, one scenario/browser session at a time.
- **Success criteria for this architecture:** all seven existing scenarios continue to pass against Chrome/Firefox/Edge (as configured); configuration precedence remains exactly as described above; no credential value is written into any SDLC document generated by this pipeline.

---

## 7. Traceability and Coverage Gaps

| Requirement (from `docs/sdlc/requirements.md`) | Architectural Coverage | Gap? |
|---|---|---|
| FR-1 UI controls displayed | `LoginPage.isLoginPageDisplayed()`, Background step | Partial — no single check for logo + all fields + forgot-password link together |
| FR-2 password masking | `LoginPage.getPasswordFieldType()`, `@TS_LOG_007` | Covered |
| FR-3 valid login → dashboard | `LoginSteps`/`DashboardPage.isDashboardPageDisplayed()`, `@TS_LOG_001` | Covered |
| FR-4 loading indicator during auth | No locator/method exists in `LoginPage` or elsewhere | **Gap — no architecture element covers this today** |
| FR-5 invalid credentials → generic error | `LoginPage.isErrorBannerDisplayed()`/`getErrorBannerText()`, `@TS_LOG_002`/`@TS_LOG_003` | Covered |
| FR-6 blank-field required validation | `LoginPage.is*RequiredErrorDisplayed()`, `@TS_LOG_004`–`@TS_LOG_006` | Covered |
| FR-7 forgot-password navigation | No locator/method exists in `LoginPage` | **Gap — no architecture element covers this today** |

The two identified gaps (FR-4, FR-7) and the FR-1 partial gap require new locators/methods on `LoginPage` (and possibly a new page object for the password-reset request page for FR-7) plus new Cucumber scenarios. These are carried forward into Stage 3 (Design Review) and Stage 4 (Planning) rather than resolved here.

---

## Traceability

- **Requirements:** `docs/sdlc/requirements.md`
- **Implementation:** `src/test/java/com/orangehrm/`, `src/test/resources/`
- **Next Stage:** `docs/sdlc/design-review.md` (Stage 3 — Design Review, human approval gate required)
