# Design Review

**Feature:** OrangeHRM Login Automation Framework
**Reviewed artifacts:** `docs/sdlc/architecture.md` (this cycle), `docs/sdlc/requirements.md` (this cycle), real codebase (`src/test/java/com/orangehrm/**`, `pom.xml`, `src/test/resources/config.properties`, `src/test/resources/features/login.feature`)
**Date:** 2026-09-28
**Agent:** design-review-agent (senior QA reviewer)
**Verdict:** **APPROVED WITH CONDITIONS**

> Scope note: static review of documents and source code only. No `mvn test` run, no live browser/network/app interaction, and no dependency-CVE database lookup was performed — every statement below is traceable to a specific file read in this session.

---

## 1. Executive Summary

This is a re-validation cycle, not a greenfield review. Every FR (FR-1..FR-8) traces to a real, tagged Cucumber scenario and a real Page Object method; every claim in `architecture.md` §7's traceability table was independently re-checked against `login.feature`, `LoginSteps.java`, and the `pages/` classes and found accurate.

Compared against the stale `design-review.md` this overwrites (dated 2026-09-27), the codebase has materially improved this cycle — the prior review's own findings are now resolved in source, not just claimed:
- **Resolved:** the FR-4 loading-indicator race (prior R1/Should-fix #1) — `LoginPage.submitAndCaptureLoadingIndicator()` now polls for the spinner synchronously inside the same `When` step, immediately after the click, instead of a separate `Then` step starting a fresh wait after a step-boundary return.
- **Resolved:** `BasePage.isDisplayed()` now catches only `TimeoutException` (prior R4/Should-fix #4), not blanket `Exception`.
- **Resolved:** the dead `<env>qa</env>` Maven property is gone from `pom.xml` (prior Nice-to-have #1) — only `<browser>` remains.
- **Resolved:** `LoginSteps` now fetches `DriverManager.getDriver()` once and passes the same reference to all three Page Objects (prior Nice-to-have #5), instead of three independent calls.
- **Resolved:** the credential-duplication finding (prior S2, literal `"Admin"`/`"admin123"` in `login.feature`) — verified by direct read and grep: no valid-credential literals remain in the feature file or step definitions; only deliberately-invalid (`"wrongpass"`, `"InvalidUser"`) and deliberately-empty (`""`) literals appear.

One item from the prior cycle remains open by design choice, not oversight: `config.properties` still ships plaintext demo credentials (`Admin`/`admin123`), now with an explicit in-file comment explaining the override mechanism and the precondition for removing them before non-demo use. This is accepted as appropriate for the current public OrangeHRM demo target but is tracked as a condition below.

No Must-fix defect was found. Two Should-fix items and five Nice-to-have items are identified below.

**Counts:** 0 critical (Must-fix) · 2 warnings (Should-fix) · 5 recommendations (Nice-to-have) · 6 risks logged.

---

## 2. Requirements Coverage

| Req | Status | Evidence |
|---|---|---|
| FR-1 (core UI controls) | Covered | `login.feature:49-51` `@TS_LOG_008` → `LoginPage.areAllLoginControlsDisplayed()` (`LoginPage.java:132-138`) |
| FR-2 (password masking) | Covered | `login.feature:44-47` `@TS_LOG_007` → `LoginPage.getPasswordFieldType()` (`LoginPage.java:155-158`) asserts DOM `type="password"` |
| FR-3 (valid login → dashboard) | Covered | `login.feature:11-14` `@TS_LOG_001` → `DashboardPage.isDashboardPageDisplayed()` + header assert (`LoginSteps.java:105-110`) |
| FR-4 (loading indicator) | Covered (race fixed; still an inherently short 3s window) | `login.feature:53-56` `@TS_LOG_009` → `submitAndCaptureLoadingIndicator()` captured synchronously in the `When` step (`LoginSteps.java:87-92`) |
| FR-5 (invalid credentials rejected) | Covered | `login.feature:16-26` `@TS_LOG_002`/`@TS_LOG_003` → `isErrorBannerDisplayed()`/`getErrorBannerText()` |
| FR-6 (empty username validation) | Covered | `login.feature:28-37` `@TS_LOG_004`/`@TS_LOG_005` → `isUsernameRequiredErrorDisplayed()` |
| FR-7 (empty password validation) | Covered | `login.feature:28-42` `@TS_LOG_004`/`@TS_LOG_006` → `isPasswordRequiredErrorDisplayed()` |
| FR-8 (forgot-password navigation) | Covered | `login.feature:58-61` `@TS_LOG_010` → `ResetPasswordPage.isResetPasswordPageDisplayed()` |
| NFR-1 (browser compatibility) | Partially covered | Real Chrome/Firefox/Edge engines via `DriverManager.initDriver()`; no cross-browser CI matrix, only a single `-Dbrowser` choice per run |
| NFR-2 (JS required) | Partially covered | No headless/no-JS code path exists; no scenario explicitly asserts "JS is required" |
| NFR-3 (availability) | Gap (correctly out of scope) | No health-check/precondition step; a down server surfaces as a generic `TimeoutException` — requirements.md gives no SLA to test against |
| NFR-4 (performance) | Gap, correctly out of scope | requirements.md: "not specified in the source PRD"; no timing assertions, none expected |
| NFR-5 (security beyond masking) | Gap, correctly out of scope | Only FR-2 masking covered; no lockout/rate-limit/session/transport coverage — requirements.md confirms none specified |
| NFR-6 (accessibility) | Gap, correctly out of scope | No ARIA/contrast/keyboard-nav assertions anywhere in `pages/`/`stepdefinitions/`; requirements.md confirms none specified |

All eight FRs have independent, order-agnostic scenario coverage (fresh browser session per scenario via `Hooks`). NFR-3/4/5/6 gaps are honestly self-reported in `architecture.md` §7 and match requirements.md's own "not specified in the source PRD" language — these are correctly treated as out of scope, not silently dropped.

---

## 3. Findings by Category

### 3.1 Security / Credential Handling
- `config.properties:28-29` still commits plaintext `valid.username=Admin`/`valid.password=admin123`. This is OrangeHRM's own public opensource-demo credential (not a real secret), and the file now carries an explicit comment (lines 20-27) documenting that `ConfigReader.get()` already gives a non-blank `-D` system property priority, so externalizing requires zero code change. No secrets-scanning or pre-commit guard exists in the repo to prevent a *real* credential from being committed the same way if this framework is ever forked against a non-public target.
- Verified by grep: no credential value (`Admin`, `admin123`, or the literal string `password`) is logged anywhere in `Hooks`/`DriverManager`/`ConfigReader` — only key names appear in log/exception messages, never values.
- No credential literals remain in `login.feature` or `LoginSteps.java` — confirmed by direct read; only intentionally-invalid/empty literals are hardcoded, consistent with the "credentials via config/env, never hardcoded" principle.

### 3.2 Reliability / Waits / Flakiness
- No `Thread.sleep()` anywhere in `src/` (verified by grep) — all synchronization goes through `WebDriverWait` (`BasePage.java`).
- `BasePage.isDisplayed(By, int)` (`BasePage.java:63-73`) correctly catches only `TimeoutException`, treating "not visible within window" as a first-class outcome for negative/validation assertions without masking other exception types (e.g., `StaleElementReferenceException`, session errors) as false negatives.
- FR-4's `LoginPage.isLoadingIndicatorDisplayed()` (`LoginPage.java:146-148`) is now invoked synchronously inside `submitAndCaptureLoadingIndicator()` immediately after the click — the previous cross-step-boundary race is fixed. The check still uses a short, fixed 3-second poll window, which remains inherently timing-sensitive (though no longer racing against Cucumber step dispatch) — see Risk R3.
- Implicit wait (5s, `DriverManager.java:79`) and explicit `WebDriverWait`s (15s default, `BasePage.java`) are cleanly separated — the implicit wait acts as a floor, not mixed into per-assertion timeouts.

### 3.3 Performance / Scalability
- Suite is single-threaded/sequential by design: no `<parallel>`/`<forkCount>` in `pom.xml`'s Surefire config, no JUnit-Platform config in `TestRunner.java`, no `RemoteWebDriver`/Grid usage in `DriverManager`. This matches the stated architecture principle ("sequential now, parallel/remote documented as future work only") and is not a defect at the current 10-scenario, single-module scope.
- `DriverManager`'s `ThreadLocal<WebDriver>` is already safe for future parallelism without further code change — a genuine forward-compatible design choice, not dead code.

### 3.4 Error Handling / Logging / Screenshots / Reports
- `ConfigReader.get()` fails fast with a clear `RuntimeException` naming the missing key (`ConfigReader.java:49-59`) — no credential value ever appears in that message.
- `Hooks.tearDown` (`Hooks.java:32-42`) gates screenshot capture on `scenario.isFailed()` and attaches PNG bytes directly to the Cucumber report (no loose file on disk) — correct per architecture.md §5.7.
- **Gap:** the screenshot capture itself (`((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)`, `Hooks.java:37`) is not wrapped in try/catch. If the driver session is already invalid/crashed at teardown time (e.g., browser crashed mid-scenario), this call can throw and propagate out of `@After` before `DriverManager.quitDriver()` runs, leaving the session unreleased and potentially obscuring the original failure in the report. See Should-fix #2.
- Three report formats (pretty/summary console, HTML, JSON, JUnit-XML) all configured in `TestRunner.java:17-23`, matching architecture.md §5.6 exactly.

### 3.5 Page Object Model Maintainability / Extensibility
- Clean separation maintained: all locators are private-static in their owning Page class; all wait/action primitives live in `BasePage`; `LoginSteps.java` contains zero raw Selenium calls, only Page Object method calls and JUnit asserts (verified by read).
- `USERNAME_REQUIRED_ERROR`/`PASSWORD_REQUIRED_ERROR` (`LoginPage.java:28-33`) use ancestor-scoped relative XPath rather than a bare class selector, reducing brittleness against OrangeHRM's repeated `oxd-input-group` markup structure.
- `LoginSteps` now shares a single `driver` field across `LoginPage`/`DashboardPage`/`ResetPasswordPage` (`LoginSteps.java:26-29`) rather than three independent `DriverManager.getDriver()` calls — cleaner than the prior cycle.

### 3.6 Cross-Browser Compatibility
`DriverManager.java:45-77` implements real Chrome/Firefox/Edge branches with WebDriverManager auto-resolved binaries and browser-appropriate headless flags for each. Code-level support is genuine and consistent across all three. No CI job or scenario currently exercises Firefox/Edge — `chrome` is the only default in both `config.properties:10` and `pom.xml`'s `<browser>` property. This is a coverage/verification gap for the non-default browsers, not a code defect, and not mandated by requirements.md (NFR-1 gives no specific browser matrix).

### 3.7 Maven Integration
- Surefire correctly restricts execution to `**/TestRunner.java` and sets `testFailureIgnore=false` (`pom.xml:96-106`), matching the "exits non-zero if any scenario fails" success criterion.
- `<browser>` is the only Maven-level runtime property; it flows to Surefire's `<systemPropertyVariables>` and is read via `ConfigReader.getBrowser()`. No dead/unused Maven properties remain (the prior cycle's `<env>qa</env>` gap has been removed, not just documented).
- **Confirmed gap (documented, not fixed):** `ConfigReader.get()` reads only `System.getProperty(...)`, never `System.getenv(...)` — a CI system that sets OS environment variables rather than passing `-D` flags to `mvn test` has no effect on this framework. Correctly flagged as `[PROPOSED]` in architecture.md §5.4; carried forward here as Nice-to-have #1.

### 3.8 Dependencies
Verified directly in `pom.xml`: Selenium Java `4.27.0`, Cucumber Java/JUnit `7.20.1`, JUnit `4.13.2`, WebDriverManager `5.9.2`, SLF4J Simple `2.0.16`, Maven Compiler Plugin `3.13.0`, Maven Surefire Plugin `3.5.2`. All are recent, actively-maintained lines; JUnit `4.13.2` in particular is the release that already carries the fix for the older `TemporaryFolder` insecure-permissions issue (CVE-2020-15250), so no known regression there. No CVE database lookup was performed in this review (out of scope for a static review) — a dependency-vulnerability scan should be added to CI rather than assumed clean. No unexpected/orphaned dependencies found.

---

## 4. Risk Assessment

| Risk | Likelihood | Impact | Severity | Mitigation |
|---|---|---|---|---|
| R1: Unguarded screenshot capture in `Hooks.tearDown` throws on a crashed/invalid driver session, potentially leaving the session unreleased and obscuring the original failure | Low | Medium (misleading failure diagnostics, resource leak) | **Medium** | Wrap the `getScreenshotAs` call in try/catch, log a warning on failure, and always proceed to `DriverManager.quitDriver()` |
| R2: Plaintext demo credential (`Admin`/`admin123`) pattern is copy-pasted into a fork/reuse against a non-public environment | Low today, High if reused | High (credential exposure) | **Medium** (current target is a public demo) | Track as an explicit, time-boxed exception; require credential externalization via `-D`/CI secrets before any non-demo reuse; consider a pre-commit/CI secrets-scan guard |
| R3: FR-4's 3-second loading-indicator poll window is still short even though the step-boundary race is fixed; a slow CI runner or slow spinner transition could produce a false failure | Low-Medium | Medium (false-fail noise on `@TS_LOG_009`) | **Low-Medium** | Monitor in CI; if flakiness is observed, add a secondary signal (e.g., login-button `disabled` attribute) alongside the spinner check |
| R4: No CI job exercises Firefox/Edge, so a latent bug in those `DriverManager` branches would go undetected | Medium | Low-Medium | **Low** | Add a scheduled or matrix CI leg running `-Dbrowser=firefox`/`edge` |
| R5: No automated dependency-vulnerability scanning configured | Low (versions currently look current) | Medium (a future CVE could land unnoticed) | **Low** | Add OWASP Dependency-Check (or equivalent) to the Maven build |
| R6: No health-check/availability precondition (NFR-3); a down app server surfaces only as a generic `TimeoutException` in the first step | Low | Low (diagnostic clarity only) | **Informational** | Out of scope per requirements.md; revisit only if a specific SLA is defined |

---

## 5. Gaps and Recommendations

### Must fix (blocks implementation) — none
No correctness defect blocks this cycle's design or code. All FR-1..FR-8 acceptance criteria have real, passing-by-design, independent scenario coverage, and the prior cycle's flakiness/race findings are already resolved in source.

### Should fix (required before merge)
| # | Recommendation | Rationale |
|---|---|---|
| 1 | Wrap `Hooks.tearDown`'s `((TakesScreenshot) driver).getScreenshotAs(...)` call in try/catch, logging a warning and still calling `DriverManager.quitDriver()` on failure (R1) | Prevents a crashed driver session at teardown from throwing out of `@After`, which could leak the session and muddy failure reporting |
| 2 | Explicitly track the plaintext demo-credential exception (`config.properties`) as a time-boxed, documented condition rather than a permanent state — e.g., an ADR/README note stating it is only valid while the target remains the public OrangeHRM demo instance (R2) | The override mechanism already supports zero-code externalization; the remaining risk is process/governance, not code, and should not be silently carried forward indefinitely |

### Nice to have (future)
| # | Recommendation |
|---|---|
| 1 | Add a `System.getenv(key)` lookup in `ConfigReader.get()` (between the `-D` system-property check and the properties-file fallback) so CI systems using OS env vars, not just `-D` flags, can override config |
| 2 | Add a dependency-vulnerability scan (OWASP Dependency-Check or equivalent) to the Maven build |
| 3 | Add a CI matrix leg or scheduled job exercising `-Dbrowser=firefox` and `-Dbrowser=edge`, not just the default `chrome`, so the existing multi-browser code paths in `DriverManager` are actually verified |
| 4 | Add a lightweight retry for a single transient network/server hiccup, for NFR-3 resilience — not required by requirements.md today |
| 5 | If FR-4 flakiness is observed in real CI runs, strengthen `isLoadingIndicatorDisplayed()` with a secondary signal (e.g., submit-button `disabled` state) alongside the spinner-visibility check |

---

## 6. Approval Conditions

**Verdict: APPROVED WITH CONDITIONS**

The architecture accurately describes the codebase, requirements traceability is complete and honestly self-reported (including the framework's own gaps), and no Must-fix defect blocks proceeding. This cycle also confirms that every Should-fix/Nice-to-have item raised in the prior (2026-09-27) design review has already been implemented in source, except the credential-externalization item, which remains an explicitly accepted, documented exception.

Conditions to close before this cycle's work is considered fully mergeable:
1. Resolve **Should-fix #1** (unguarded screenshot capture in `Hooks.tearDown`) — low-cost robustness hardening, recommended before merge.
2. Acknowledge and track **Should-fix #2** (plaintext demo credentials) as an explicit, scoped exception — acceptable to carry forward only while the target remains the public OrangeHRM demo instance; must not be silently assumed permanent.

No condition requires re-architecting the framework, adding parallel execution, adding a cross-browser CI matrix, or adding new report formats — none of those are mandated by requirements.md, and their absence is correctly treated as out of scope, not a gap.

---

## 7. Reviewer Comments and Sign-off

- This review is a static document/code review only. No test execution, browser session, network call, or dependency-CVE lookup was performed — every finding above is traceable to a specific file and line read directly from the repository in this session.
- All "[IMPLEMENTED]" claims in `architecture.md` that were spot-checked against source (component table, wait strategy, configuration precedence, credential-handling narrative, reporting/screenshot behavior, dependency version table, requirements traceability table) matched the real code with no discrepancies found.
- Independently confirmed that four issues raised in the stale prior-cycle `design-review.md` (FR-4 step-boundary race, `isDisplayed()` catching overly broad `Exception`, dead `<env>qa</env>` Maven property, per-Page-Object independent driver fetches, and literal valid-credential strings in `login.feature`) are all resolved in the current codebase — this is genuine progress, not re-stated debt.
- One new implementation-level finding not previously flagged: unguarded screenshot capture in `Hooks.tearDown` (§3.4, §5 Should-fix #1).

**Counts:** 0 critical (Must-fix) · 2 warnings (Should-fix) · 5 recommendations (Nice-to-have) · 6 risks logged.

**Sign-off:** design-review-agent, 2026-09-28.

---

## 8. Traceability

- **Architecture reviewed:** `docs/sdlc/architecture.md` (this cycle, dated 2026-09-28)
- **Requirements reviewed:** `docs/sdlc/requirements.md` (this cycle, dated 2026-09-28; FR-1..FR-8, NFR-1..NFR-6, US-AUTH-001)
- **Code paths verified:** `src/test/resources/features/login.feature`; `src/test/resources/config.properties`; `pom.xml`; `src/test/java/com/orangehrm/runner/TestRunner.java`; `src/test/java/com/orangehrm/stepdefinitions/LoginSteps.java`; `src/test/java/com/orangehrm/hooks/Hooks.java`; `src/test/java/com/orangehrm/driver/DriverManager.java`; `src/test/java/com/orangehrm/config/ConfigReader.java`; `src/test/java/com/orangehrm/pages/{BasePage,LoginPage,DashboardPage,ResetPasswordPage}.java`
- **Prior-cycle review superseded:** stale `docs/sdlc/design-review.md` dated 2026-09-27 (overwritten by this document)
- **Next stage:** Implementation of the two Should-fix items above (if taken up this cycle), then verification per `docs/sdlc/verification-report.md` conventions for this repo.
