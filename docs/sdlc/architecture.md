# Architecture Document

**Feature:** Login Functionality for OrangeHRM Portal (US-AUTH-001)
**Date:** 2026-11-11
**Agent:** architecture-agent (SDLC Stage 2, run 3)
**Input:** `docs/sdlc/requirements.md` (FR-1..FR-8, NFR-1..NFR-6), existing code under `src/test/java`, `src/test/resources`, `pom.xml`

---

## 1. Project Context and Overview

Automated UI tests verify the OrangeHRM login page (`https://opensource-demo.orangehrmlive.com/web/index.php/auth/login`). The existing framework is **Selenium 4 + Cucumber (BDD) + JUnit 4 + Maven** using the **Page Object Model (POM)**. This architecture preserves that stack and its conventions. The "Recommended Stack" in the agent guide (JUnit 5) is deliberately not adopted: Cucumber 7 runs through `cucumber-junit` (JUnit 4), and migrating would be speculative complexity with no requirement driving it.

```
 Maven (surefire) -> JUnit4 TestRunner (@RunWith Cucumber)
        |
        v
 login.feature (Gherkin) -> LoginSteps (glue) -> Page Objects (BasePage <- LoginPage/DashboardPage/ResetPasswordPage)
        ^                                              |
   Hooks (@Before/@After)                              v
        |                                  DriverManager (ThreadLocal WebDriver) -> Browser (Chrome/Firefox/Edge)
        v                                              ^
 ConfigReader (-D > config.properties) -----------------+
        |
        v
 Reports: target/cucumber-reports (html/json/junit xml), surefire-reports, failure screenshots attached to report
```

## 2. Components

| Component | Responsibility | Inputs | Outputs | Depends on | Key classes |
|---|---|---|---|---|---|
| Test runner | Discover features, bind glue, configure report plugins | Feature files, `-Dcucumber.filter.tags` | Cucumber reports | Cucumber-JUnit | `runner.TestRunner` |
| Feature files | Business-readable scenarios and tags (e.g. `@TS_LOG_001`) | Requirements | Gherkin steps | none | `features/login.feature` |
| Step definitions | Translate steps to page-object calls and hold assertions | Gherkin params | Pass/fail | Pages, DriverManager, ConfigReader | `stepdefinitions.LoginSteps` |
| Hooks | Per-scenario setup and teardown; failure screenshot; always quit driver | `Scenario` | Browser session, PNG attachment | DriverManager | `hooks.Hooks` |
| Driver management | Create browser per thread by config, timeouts, headless, quit | browser, headless, waits | `WebDriver` | Selenium, WebDriverManager, ConfigReader | `driver.DriverManager` |
| Page objects | Own locators and UI actions; no assertions | `WebDriver` | State/text/booleans | Selenium | `BasePage`, `LoginPage`, `DashboardPage`, `ResetPasswordPage` |
| Base page utilities | Reusable explicit-wait helpers (visible, clickable, URL, etc.) | locators | `WebElement`s | `WebDriverWait`, ConfigReader | `BasePage` |
| Configuration | Single source of config with override order | `-D` props, `config.properties` | typed getters | JDK | `config.ConfigReader` |

Single-responsibility rules: steps assert, pages act, hooks manage lifecycle, `DriverManager` owns the driver, `ConfigReader` owns configuration.

## 3. Directory Structure

```
pom.xml
docs/sdlc/                       requirements.md, architecture.md, ...
src/test/java/com/orangehrm/
  config/ConfigReader.java
  driver/DriverManager.java
  hooks/Hooks.java
  pages/{BasePage,LoginPage,DashboardPage,ResetPasswordPage}.java
  runner/TestRunner.java
  stepdefinitions/LoginSteps.java
src/test/resources/
  config.properties
  features/login.feature
target/cucumber-reports/         generated: html, json, junit xml
```

New work follows the same packages (new page -> `pages`, new feature -> `features` plus steps in `stepdefinitions`).

## 4. Test Execution Flow

1. `mvn test` starts Surefire (3.5.2), which runs `TestRunner` (JUnit 4 + Cucumber).
2. Cucumber loads features from `src/test/resources/features`, glue `stepdefinitions` + `hooks`; optional tag filter.
3. Per scenario, `Hooks.@Before` calls `DriverManager.getDriver()`: reads config, runs `WebDriverManager.<browser>().setup()`, builds options (headless if set), applies implicit wait and page-load timeout, stores driver in `ThreadLocal`.
4. Steps open `config base.url` via `LoginPage`, perform actions through page objects (explicit waits in `BasePage`), then assert (JUnit assertions) in the step class.
5. `Hooks.@After`: on failure, best-effort screenshot attached to the scenario; `finally` always `quitDriver()` (fresh isolated session per scenario).
6. Reporting: Cucumber `pretty/summary/html/json/junit` under `target/cucumber-reports`; Surefire reports under `target/surefire-reports`.

Execution is sequential. Parallel (Cucumber/Surefire forks; `ThreadLocal` already makes this safe) and remote Selenium Grid are future work only.

## 5. Technology and Configuration Decisions

| Decision | Choice | Justification |
|---|---|---|
| Language | Java (compiler level 11 in pom) | Existing; raising to 17/21 is optional future work |
| Automation | Selenium 4.27.0 | Existing; built-in cross-browser support |
| BDD | Cucumber 7.20.1 (`cucumber-java`, `cucumber-junit`) | Maps requirements/ACs to Gherkin scenarios |
| Test runner | JUnit 4.13.2 | Required by `cucumber-junit`; no benefit in changing |
| Driver binaries | WebDriverManager 5.9.2 | No manual driver management |
| Build | Maven, Surefire 3.5.2 | Existing |
| Logging | slf4j-simple 2.0.16 | Minimal |
| Reporting | Cucumber HTML/JSON/JUnit XML + Surefire | No extra dependency |

**Configuration flow.** Implemented: `-Dkey=value` (non-empty) -> `config.properties` -> in-code default (`get(key, default)`); a missing required key throws. Environment variables are NOT read by the current `ConfigReader`. The guide wants a system property -> environment variable -> default order; this is recorded as a gap (A-1) with a small, recommended extension: resolve `key`, then env var `KEY.UPPER_WITH_UNDERSCORES` (e.g. `VALID_PASSWORD`), then file, then default. CI can alternatively map secrets to `-Dvalid.password`.

Keys: `base.url`, `browser` (chrome|firefox|edge), `headless`, `implicit.wait` (5), `explicit.wait` (15), `page.load.timeout` (30), `valid.username`, `valid.password`.

Waits: explicit `WebDriverWait` helpers in `BasePage`; no `Thread.sleep()`. A 5s implicit wait coexists with explicit waits (risk R-2).

## 6. Error Handling, Security, Scalability, Success Criteria

**Error handling**
- Timeouts surface as Selenium `TimeoutException` from wait helpers (negative checks return booleans).
- Failure screenshot is best-effort and never masks the original failure; driver is always quit in `finally`.
- Missing config key fails fast with a descriptive message.

**Security**
- Demo credentials `Admin/admin123` are public OrangeHRM demo values, currently in `config.properties` (accepted exception documented in that file). For any non-public target they must be removed from the file and supplied via `-D` overrides / env vars / CI secrets. Never log passwords.
- NFR-5 (security) is unspecified in the PRD; tests only cover password masking (FR-2, `type=password`).

**Scalability**
- Today sequential; `ThreadLocal` driver and per-scenario isolation allow later parallel runs; Selenium Grid/remote driver is future work.

**Success criteria**
- All 10 scenarios in `login.feature` pass on Chrome, and on Firefox/Edge via `-Dbrowser=`.
- No `Thread.sleep()`, no selectors in steps, no hardcoded secrets for non-demo targets.
- Reports generated on every run; failures carry a screenshot.

## 7. Traceability

| Requirement | Architectural coverage | Existing implementation |
|---|---|---|
| FR-1 UI elements | `LoginPage` visibility checks | `areAllLoginControlsDisplayed()`, scenario "All required login controls are displayed" |
| FR-2 Password masking | `LoginPage` exposes field type attr | `getPasswordFieldType()`, masking scenario |
| FR-3 Successful login | `LoginPage.login` -> `DashboardPage`; creds from `ConfigReader` | "Successful login with valid Admin credentials" |
| FR-4 Loading indicator | Short-lived element capture on submit | `submitAndCaptureLoadingIndicator()`, `isLoadingIndicatorDisplayed()` |
| FR-5 Invalid credentials | Error banner accessors | 2 failure scenarios; `getErrorBannerText()` |
| FR-6 Required username | Field-level message accessors | `isUsernameRequiredErrorDisplayed()`, validation scenarios |
| FR-7 Required password | Same | `isPasswordRequiredErrorDisplayed()`, validation scenarios |
| FR-8 Forgot password | `LoginPage.clickForgotPassword` -> `ResetPasswordPage` | forgot-password scenario |
| NFR-1 Browser compat | `DriverManager` chrome/firefox/edge via `browser` | config switch |
| NFR-2 JavaScript enabled | Browsers' default; no JS-disabled test | none (not tested) |
| NFR-3 Availability | Precondition; failures show as timeouts | none |
| NFR-4/5/6 Unspecified | Out of design scope | none |

## 8. Assumptions and Risks

- A-1: Env-var resolution is not implemented in `ConfigReader` (see section 5); proposed for Stage 5 as a small change.
- A-2: Java compiler target 11 is retained; no requirement needs 21.
- A-3: Supported browsers are Chrome (default), Firefox, Edge (per requirements assumption on "modern browser").
- R-1: Tests depend on a public, shared demo site (availability, data resets, rate limits) - flakiness is possible; retries are not added (YAGNI).
- R-2: Mixing implicit and explicit waits can add unpredictable delay for negative checks; consider lowering implicit wait to 0 in a later stage.
- R-3: The loading-indicator check (FR-4) is timing-sensitive as the spinner is brief.
- R-4: The PRD lists only TS_LOG_001..007 while scenarios cover more behaviours; tags should be reconciled in review.
- R-5: Confluence PRD was not re-read (see requirements Traceability), so the design reflects the earlier extract.
