# Design Review

**Feature:** OrangeHRM Login Automation Framework
**Reviewed artifacts:** `docs/sdlc/architecture.md`, `docs/sdlc/requirements.md`, current codebase (`src/test/**`, `pom.xml`, `config.properties`)
**Date:** 2026-09-27
**Agent:** design-review-agent (senior QA reviewer)
**Verdict:** **APPROVED WITH CONDITIONS**

> Scope note: this is a static review of documents and source code. No `mvn test` run, no live browser/network/app interaction, and no dependency-CVE database lookup was performed in this review — all statements below are based on reading the files listed above, not on execution.

---

## 1. Executive Summary

The architecture accurately describes the codebase: every "[IMPLEMENTED]" claim I spot-checked against source matches, and the flagged gaps (no parallel execution, credential duplication, unused `<env>qa</env>`, no env-var support, plaintext checked-in credentials) are all real and correctly attributed as **design-level** gaps, not implementation bugs. FR-1..FR-8 all trace to a real scenario and a real, working Page Object method. NFR-1..NFR-5 are honestly reported as not exercised by named tests, which is appropriate given requirements.md does not mandate NFR test coverage.

Beyond confirming the architecture's own gap list, this review adds one **implementation-level** finding the architecture doc did not call out: `@TS_LOG_009`'s loading-indicator check (`LoginPage.isLoadingIndicatorDisplayed()`, 3s window) is invoked *after* `loginPage.login()` has already returned control to the step method — meaning the spinner can legitimately appear and vanish before the assertion begins polling, on a fast connection. This is a race condition in the test itself, distinct from the architecture's "timing-sensitive" note about the window size.

No blocking defects were found in the Page Object logic, wait strategy, or Maven/Surefire wiring. Conditions below are about hardening credential handling and the flaky FR-4 check before merge, not about redesigning the framework.

**Counts:** 2 Must fix · 4 Should fix · 5 Nice to have.

---

## 2. Requirements Coverage

| Req | Status | Evidence |
|---|---|---|
| FR-1 | Covered | `login.feature:49-51` `@TS_LOG_008` → `LoginPage.areAllLoginControlsDisplayed()` (`LoginPage.java:115-121`) checks logo, username, password, login button, forgot-password link |
| FR-2 | Covered | `login.feature:44-47` `@TS_LOG_007` → `LoginPage.getPasswordFieldType()` (`LoginPage.java:138-141`) asserts DOM `type="password"` |
| FR-3 | Covered | `login.feature:11-14` `@TS_LOG_001` → `DashboardPage.isDashboardPageDisplayed()` + header text assert (`LoginSteps.java:80-85`) |
| FR-4 | Covered, but flaky (see §3.2) | `login.feature:53-56` `@TS_LOG_009` → `isLoadingIndicatorDisplayed()` (3s window, checked post-click) |
| FR-5 | Covered | `login.feature:16-26` `@TS_LOG_002`/`@TS_LOG_003` → error-banner text assert against literal `"Invalid credentials"` |
| FR-6 | Covered | `login.feature:28-37` `@TS_LOG_004`/`@TS_LOG_005` → `isUsernameRequiredErrorDisplayed()` |
| FR-7 | Covered | `login.feature:28-42` `@TS_LOG_004`/`@TS_LOG_006` → `isPasswordRequiredErrorDisplayed()` |
| FR-8 | Covered | `login.feature:58-61` `@TS_LOG_010` → `ResetPasswordPage.isResetPasswordPageDisplayed()` |
| NFR-1 (compatibility/JS) | Gap (design-level, honestly reported) | Chrome supported as a run parameter (`DriverManager.java`), but no scenario or CI matrix asserts cross-browser behavior |
| NFR-2 (availability) | Gap (design-level) | No health-check/precondition step; a down server surfaces as a generic `TimeoutException`, not a distinct diagnostic |
| NFR-3 (performance) | Gap, correctly out of scope | requirements.md states "not specified"; no timing assertions exist, none expected |
| NFR-4 (security beyond FR-2) | Gap, correctly out of scope | No lockout/rate-limit/session coverage; requirements.md confirms none specified |
| NFR-5 (accessibility) | Gap, correctly out of scope | No ARIA/contrast/keyboard-nav assertions; requirements.md confirms none specified |

All FR traceability claims in `architecture.md` §7 were re-verified directly against `login.feature` and the Page Object methods — no discrepancies found.

---

## 3. Findings

### 3.1 Security / Credential Handling
| # | Finding | Evidence |
|---|---|---|
| S1 | Plaintext, checked-in real credentials | `config.properties:21-22` (`valid.username=Admin`, `valid.password=admin123`), not gitignored |
| S2 | Credential value duplicated between config and Gherkin literals | `login.feature:18` (`@TS_LOG_002`, username `"Admin"` literal), `:36` (`@TS_LOG_005`, password `"admin123"` literal), `:41` (`@TS_LOG_006`, username `"Admin"` literal), `:46` (`@TS_LOG_007`, password `"admin123"` literal) — confirmed by direct grep, exactly matches architecture.md's claim |
| S3 | No secrets-scanning/CI guard preventing a real credential from ever being committed this way | `.gitignore` excludes `target/` only; no rule for property files |

Given the target is OrangeHRM's own public opensource-demo instance, S1–S3 are **acceptable today** but become blocking the moment this framework is pointed at a real, non-public environment. Treated as Should-fix now (harden before that day comes), not Must-fix against current scope.

### 3.2 Reliability / Waits / Flakiness
| # | Finding | Evidence |
|---|---|---|
| R1 | **New finding, not in architecture.md:** FR-4 loading-indicator check is a race, not just "timing-sensitive" | `LoginSteps.java` step `the_user_submits_valid_credentials` (calls `loginPage.login(...)`) returns fully before the separate `Then` step calls `isLoadingIndicatorDisplayed()` (`LoginPage.java:129-131`, 3s wait). Between click and assertion there is an unbounded, uncontrolled gap (Cucumber step dispatch); on a fast auth response the spinner can already be gone. This can produce a **false failure** on a healthy app, not just occasional slowness. |
| R2 | Implicit + explicit wait combination is correctly separated (no anti-pattern found) | `DriverManager.java:79-80` sets a 5s implicit wait as a floor; `BasePage.java` explicit `WebDriverWait`s do the real synchronization. No `Thread.sleep` anywhere in `src/` (verified by grep). |
| R3 | No retry for a single transient network/server hiccup | Confirmed no `@Retry`/retry-plugin config in `pom.xml` or `TestRunner.java`. Not required by requirements.md; Nice-to-have only. |
| R4 | `isDisplayed()` swallowing all `Exception`, not just `TimeoutException` | `BasePage.java:62-70` — a genuine `StaleElementReferenceException` or driver-session error would also silently resolve to `false`, masking a real infrastructure failure as "element not found." |

### 3.3 Performance / Scalability
No timing assertions exist and none are required (NFR-3 out of scope). Suite is single-threaded/sequential by design (`architecture.md` §5.2, confirmed: no `<parallel>` in `pom.xml`, no JUnit-Platform config in `TestRunner.java`). This is an accepted, documented architecture-level boundary for a 10-scenario, single-module suite — not a defect at current scope.

### 3.4 Error Handling / Logging / Screenshots / Reports
- `ConfigReader.get()` fails fast with a clear `RuntimeException` message naming the missing key (`ConfigReader.java:49-59`) — good.
- `Hooks.tearDown` correctly gates screenshot capture on `scenario.isFailed()` and attaches PNG bytes to the Cucumber report rather than writing loose files (`Hooks.java:33-40`) — matches architecture.md, verified.
- SLF4J logging present in `Hooks` and `DriverManager` at reasonable INFO/WARN levels; no credential values are ever logged (verified by grep — no `password`/`valid.password` logging calls exist).
- Three report formats (pretty/summary console, HTML, JSON, JUnit-XML) all configured in `TestRunner.java:17-23` — matches architecture.md exactly.

### 3.5 Page Object Model Maintainability/Extensibility
- Clean separation: locators private-static in each Page class, all wait/action primitives centralized in `BasePage` — no Selenium calls leak into `LoginSteps.java` (verified: only Page Object method calls and JUnit asserts present).
- `LoginSteps` constructs three Page Objects per scenario instance (`LoginSteps.java:22-24`), each independently calling `DriverManager.getDriver()` — harmless today (driver already initialized by `Hooks.setUp`) but slightly redundant; a single shared driver reference passed once would be marginally cleaner. Not a defect.
- `USERNAME_REQUIRED_ERROR`/`PASSWORD_REQUIRED_ERROR` XPath locators are appropriately scoped via ancestor-relative XPath rather than a bare class selector (`LoginPage.java:28-33`) — reduces brittleness against OrangeHRM's repeated `oxd-input-group` structure.

### 3.6 Cross-Browser Support
`DriverManager.java:45-77` implements Chrome/Firefox/Edge branches with WebDriverManager auto-resolution and browser-appropriate headless flags — code-level support is real and consistent for all three, as architecture.md claims. No CI job or scenario currently exercises Firefox/Edge (only `chrome` is the default in both `config.properties:10` and `pom.xml:30`) — this is a coverage/CI gap, not a code defect, and not mandated by requirements.md.

### 3.7 Maven Integration
- `pom.xml` Surefire config correctly restricts execution to `**/TestRunner.java` and sets `testFailureIgnore=false` (`pom.xml:98-107`) — matches the success-criteria claim in architecture.md §6.
- `<env>qa</env>` (`pom.xml:31`) is passed to Surefire as `-Denv=qa` (`pom.xml:105`) but no class reads `System.getProperty("env")` anywhere in `src/test/java` (confirmed by grep across the codebase) — dead configuration, matches architecture.md's flagged gap exactly.

### 3.8 Dependencies
Versions match `architecture.md`'s table exactly (Selenium 4.27.0, Cucumber 7.20.1, JUnit 4.13.2, WebDriverManager 5.9.2, SLF4J 2.0.16, Compiler 3.13.0, Surefire 3.5.2) — verified directly in `pom.xml:23-27, 87, 97`. No CVE database lookup was performed in this review; a dependency-vulnerability scan (e.g., OWASP Dependency-Check) should be run as part of CI rather than assumed clean here.

---

## 4. Risk Assessment

| Risk | Likelihood | Impact | Severity | Mitigation |
|---|---|---|---|---|
| FR-4 loading-indicator assertion fails intermittently on healthy app (R1) | Medium | Medium (false-fail noise erodes trust in suite) | **High** | Check indicator presence immediately around the click (e.g., wrap click+poll in one page-object method) instead of a fresh 3s wait dispatched after a separate step boundary |
| Real credentials committed to a fork/reuse of this framework against a non-public app (S1) | Low (today), High (if reused) | High (credential exposure) | **High** | Externalize via `-D`/CI secrets before pointing at any non-demo environment; document this as a precondition for reuse |
| Credential drift between `config.properties` and hardcoded Gherkin literals (S2) | Medium (next credential rotation) | Medium (silent scenario failures for @TS_LOG_002/005/006/007) | **Medium** | Route all literals through `ConfigReader`/scenario outline parameters |
| `isDisplayed()` masking a real driver/session error as a normal "not found" (R4) | Low | Medium (misdiagnosis during failure triage) | **Low** | Narrow the caught exception type to `TimeoutException` |
| Dead `env` Maven property implies unsupported multi-environment capability (from architecture.md) | Low | Low (developer confusion, no functional break) | **Low** | Wire it into `base.url` resolution or remove it |
| No parallel/Grid execution (from architecture.md, explicitly not required) | N/A | N/A | **Informational** | Revisit only if suite size or CI time budget grows |

---

## 5. Gaps and Recommendations

### Must fix (blocks implementation — none found)
None. No correctness defect blocks merging this cycle's architecture/code as-is.

*(Table kept empty deliberately — see §6 conditions below for the two items that must be resolved before final sign-off, tracked as conditions rather than blocking defects since they don't break current test correctness.)*

### Should fix (required before merge)
| # | Recommendation | Rationale |
|---|---|---|
| 1 | Fix the FR-4 race in `@TS_LOG_009` (R1): make the loading-indicator check start polling at or before the click, not after step-boundary return | Prevents false failures against a healthy app; this is a real flakiness bug, not a documentation gap |
| 2 | Route `@TS_LOG_002`, `@TS_LOG_005`, `@TS_LOG_006`, `@TS_LOG_007` literal credential values through `ConfigReader`/Scenario Outline examples instead of inline Gherkin strings | Removes the duplication the architecture flagged; a future rotation currently requires editing two places |
| 3 | Externalize `valid.username`/`valid.password` off the checked-in `config.properties` (blank defaults + required `-D`/CI secret injection) before this framework is ever pointed at a non-public environment | `ConfigReader`'s override mechanism already supports this with no code change — only a config/process change |
| 4 | Narrow `BasePage.isDisplayed()`'s caught exception from `Exception` to `TimeoutException` (R4) | Avoids silently converting real driver/session errors into a "not displayed" false negative |

### Nice to have (future)
| # | Recommendation |
|---|---|
| 1 | Wire `pom.xml`'s `<env>qa</env>` into environment-specific `base.url` resolution, or remove it |
| 2 | Add a lightweight retry (e.g., one re-run of a transient network failure) for NFR-2 resilience — not required by requirements.md today |
| 3 | Add a CI matrix leg (or scheduled job) exercising Firefox/Edge, not just the default `chrome`, to make the existing multi-browser code path actually verified in CI |
| 4 | Add a dependency-vulnerability scan (OWASP Dependency-Check or equivalent) to the build; this review did not check CVEs |
| 5 | Consider a single shared driver reference passed into all three Page Objects in `LoginSteps` rather than three independent `DriverManager.getDriver()` calls, for minor clarity |

---

## 6. Approval Conditions

**Verdict: APPROVED WITH CONDITIONS**

The architecture is sound, requirements traceability is accurate and honestly self-reported (including its own gaps), and no Must-fix defect blocks proceeding. Conditions to close before this cycle's work is considered fully mergeable:

1. Resolve **Should-fix #1** (FR-4 flaky loading-indicator check) — this is the one finding in this review that represents an actual latent bug rather than a documented, accepted gap.
2. Resolve **Should-fix #2 and #3** (credential duplication and externalization) before this framework is reused against any non-public target; acceptable to defer only if scope permanently stays on the public OrangeHRM demo instance, but should be tracked explicitly rather than silently carried forward.
3. Should-fix #4 (narrow the caught exception type) is a low-cost correctness hardening — recommended before merge but not release-blocking.

No condition requires re-architecting the framework, adding parallel execution, adding browsers, or adding HTML reporting — none of those are mandated by requirements.md, and their absence is correctly treated as out of scope, not a gap.

---

## 7. Reviewer Comments and Sign-off

- This review is a static document/code review only. No test execution, browser session, network call, or dependency-CVE lookup was performed — all findings above are traceable to specific lines read directly from the repository.
- The architecture document's self-reported gaps (parallel execution, `env` dead config, credential duplication, plaintext credentials) were independently re-verified against source and found accurate in every instance checked.
- One new implementation-level finding (R1, the FR-4 race condition) was identified that the architecture document did not surface, because it is a runtime-behavior consequence of step-boundary timing rather than a structural design gap.

**Counts:** 0 Must fix (none blocking) · 4 Should fix · 5 Nice to have · 6 risks logged.

**Sign-off:** design-review-agent, 2026-09-27.

---

## 8. Traceability

- **Architecture reviewed:** `docs/sdlc/architecture.md` (this cycle)
- **Requirements reviewed:** `docs/sdlc/requirements.md` (FR-1..FR-8, NFR-1..NFR-5, US-AUTH-001)
- **Code paths verified:** `src/test/resources/features/login.feature`; `src/test/java/com/orangehrm/runner/TestRunner.java`; `src/test/java/com/orangehrm/pages/{LoginPage,DashboardPage,ResetPasswordPage,BasePage}.java`; `src/test/java/com/orangehrm/stepdefinitions/LoginSteps.java`; `src/test/java/com/orangehrm/hooks/Hooks.java`; `src/test/java/com/orangehrm/driver/DriverManager.java`; `src/test/java/com/orangehrm/config/ConfigReader.java`; `pom.xml`; `src/test/resources/config.properties`; `.gitignore`
- **Next stage:** Implementation of Should-fix items above, then re-verification against `docs/sdlc/verification-report.md` conventions for this repo.
