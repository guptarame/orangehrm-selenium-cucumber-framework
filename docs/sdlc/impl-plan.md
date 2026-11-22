# Implementation Plan

**Feature:** Login Functionality for OrangeHRM Portal (US-AUTH-001)
**Date:** 2026-11-11
**Agent:** planning-agent (SDLC Stage 4, run 3)
**Sources:** `docs/sdlc/architecture.md` (human-approved), `docs/sdlc/design-review.md` (APPROVED WITH CONDITIONS, accepted by human)
**Existing code reviewed:** `DriverManager`, `Hooks`, `ConfigReader`, `BasePage`, `LoginPage`, `DashboardPage`, `ResetPasswordPage`, `LoginSteps`, `TestRunner`, `login.feature`, `config.properties`, `pom.xml` (all under `src/test/**` and root).

## 1. Context and Summary

The framework (Selenium 4.27 / Cucumber 7.20 / JUnit 4 / Maven, POM) already exists with 10 scenarios (TS_LOG_001..010), `pageLoadTimeout` wired in `DriverManager`, explicit waits in `BasePage`, failure screenshots in `Hooks`, reports via the runner plugins, and test data in `config.properties`. Per the review this plan therefore schedules **corrective and verification work only**: no new framework, no POM rewrite, no stack change (JUnit 5 migration is not planned).

| Metric | Value |
|---|---|
| Tasks | 10 (3 must-fix, 4 should-fix, 1 docs, 2 validation/gate) |
| Complexity | 5 Low, 5 Medium, 0 High |
| Total effort | about 11.5 hours |

## 2. Task Breakdown

Effort in hours. Priority: P1 must-fix, P2 should-fix (pre-merge), P3 supporting.

| ID | Task | Source | Pri | Cplx | Effort | Depends on |
|---|---|---|---|---|---|---|
| T-01 | Harden `DriverManager` lifecycle | M-1 | P1 | Med | 1.5 | none |
| T-02 | Set implicit wait to 0; keep explicit waits only | M-2 | P1 | Low | 1.0 | none |
| T-03 | Make FR-4 loading-indicator check deterministic and scoped | M-2 | P1 | Med | 1.5 | T-02 |
| T-04 | Add non-creating `peekDriver()`; use in `Hooks.tearDown` | S-1 | P2 | Low | 0.5 | T-01 |
| T-05 | Resolve `browser` precedence conflict (pom vs properties) | S-3 | P2 | Low | 0.5 | none |
| T-06 | `ConfigReader` env-var resolution and non-demo credential guard | S-4 | P2 | Med | 1.5 | none |
| T-07 | Close FR-3/FR-5 coverage gaps; record FR-1 gap | S-2 | P2 | Med | 1.5 | T-02 |
| T-08 | Architecture addendum and doc updates | M-1, M-2, S-1..S-4 | P3 | Low | 0.75 | T-01..T-07 |
| T-09 | Validate on real app, Chrome, headless and slow network | all | P1 | Med | 2.0 | T-01..T-07 |
| T-10 | Regression gate: static checks and full-suite run | all | P1 | Low | 0.75 | T-09 |

Existing features handled as verification, not new work: `pageLoadTimeout` (`DriverManager.java:80`, `ConfigReader.getPageLoadTimeout`) is verified in T-09; BasePage explicit waits and LoginPage synchronization are kept and only the FR-4 locator is changed (T-03); screenshots/lifecycle logging in `Hooks` are kept, only the driver accessor changes (T-04); test data stays in `config.properties` and the feature file (no new data layer); the reset-link flow (TS_LOG_010) is existing and verified in T-09. Remember Me is not in the PRD or architecture and is not planned.

### Task details

**T-01 Harden DriverManager lifecycle (M-1)**
Files: `src/test/java/com/orangehrm/driver/DriverManager.java`.
- `quitDriver()` (lines 88-95): wrap `driver.quit()` in `try { } finally { DRIVER_THREAD_LOCAL.remove(); }`; log (do not rethrow) a `WebDriverException` from quit.
- `initDriver()`: after the browser object is created (line 75 and equivalents), wrap lines 79-83 in `try/catch (RuntimeException e)`; on failure call `driver.quit()` (guarded) and rethrow; set the ThreadLocal only on success.
Acceptance: (a) code review shows `remove()` in a `finally`; (b) a temporary run with an invalid `-Dpage.load.timeout=abc` or forced exception after creation leaves no `chromedriver`/`chrome` process from that run (check with process list) and the next scenario gets a fresh driver; (c) `mvn test-compile` passes.

**T-02 Implicit wait to 0 (M-2)**
Files: `DriverManager.java:79`, `src/test/resources/config.properties` (`implicit.wait=0`), `ConfigReader` (keep `getImplicitWait()`, default 0).
- Prefer removing the `implicitlyWait` call entirely and deleting the `implicit.wait` key and getter if unused elsewhere (grep first); otherwise keep the key at 0 with a comment.
Acceptance: no non-zero implicit wait is applied (grep shows no `implicitlyWait(` with a configurable positive default); TS_LOG_002/003/004-006 negative checks (`isDisplayed` false path) complete in no more than explicit-wait time (record before/after durations in T-09).

**T-03 Deterministic FR-4 check (M-2)**
Files: `src/test/java/com/orangehrm/pages/LoginPage.java` (locator line 20, `isLoadingIndicatorDisplayed` ~146-148, `submitAndCaptureLoadingIndicator`), `LoginSteps.java` (TS_LOG_009 step), `login.feature` (TS_LOG_009 tag).
- Scope the loader locator to the login form / submit button (for example `.oxd-form-loader` inside `form`/`button[type=submit]`), not the global `.oxd-loading-spinner`.
- Poll for presence with a short explicit wait (FluentWait with small polling interval) started immediately after click; return false instead of throwing.
- Tag TS_LOG_009 `@timing` (best-effort) so it can be excluded with `-Dcucumber.filter.tags="not @timing"`; the architecture records this as the accepted FR-4 approach.
Acceptance: locator no longer matches dashboard-level loaders (inspect DOM on app in T-09); TS_LOG_009 passes in 3 consecutive live runs or is documented as best-effort with the tag exclusion working (`mvn test -Dcucumber.filter.tags="not @timing"` runs 9 scenarios).

**T-04 Non-creating driver accessor (S-1)**
Files: `DriverManager.java` (add `public static WebDriver peekDriver()` returning `DRIVER_THREAD_LOCAL.get()` without init), `Hooks.java:132` (use `peekDriver()`; null-safe).
Acceptance: when `initDriver` is forced to fail, `@After` does not start a second browser (log shows exactly one "Initializing WebDriver"); `tearDown` still calls `quitDriver()` in `finally`; original failure is not masked.

**T-05 Browser precedence (S-3)**
Files: `pom.xml` (property `<browser>` ~line 30, Surefire `systemPropertyVariables` ~line 103), `config.properties`.
- Remove the pom default and pass-through of `browser` so `config.properties` is the default and `-Dbrowser=` (forwarded by Maven to Surefire) still overrides. Leave the other pom properties untouched.
Acceptance: setting `browser=` in `config.properties` changes the browser under `mvn test`; `-Dbrowser=edge` overrides it (verify via the "Initializing WebDriver for browser" log line).

**T-06 Env-var resolution and credential guard (S-4, A-1)**
Files: `src/test/java/com/orangehrm/config/ConfigReader.java:49-59`, `config.properties` (comment only), `ConfigReaderTest` is not added (no unit test layer exists; verify via run).
- Resolution order in `get`: non-empty `-Dkey`, then env var `KEY` upper-cased with `.` replaced by `_` (`VALID_PASSWORD`), then file, then default. Non-empty only.
- Guard: if `valid.password` resolves from the file and `base.url` host is not `opensource-demo.orangehrmlive.com`, throw `IllegalStateException` with a clear message.
- C-4 (wrapping `getInt` NumberFormatException) is deferred, see section 6.
Acceptance: `VALID_PASSWORD=x` env var overrides the file value; `-Dbase.url=https://example.test/...` without a supplied password fails fast with the guard message; default run on the demo host is unaffected; no password is logged.

**T-07 Coverage gaps (S-2)**
Files: `login.feature`, `LoginSteps.java`, `LoginPage.java` (add `submitWithEnterKey()` only), `DashboardPage.java:12`.
- FR-5 both-invalid: add a Scenario Outline row or new scenario tagged `@TS_LOG_011 @negative` (username and password both invalid, banner "Invalid credentials").
- FR-3: add `@TS_LOG_012 @positive` for login submitted with Enter; tighten dashboard URL check to `dashboard/index`.
- FR-1 placeholders and footer: recorded as an **accepted gap** (low value, selector risk on demo site); documented in T-08.
- Reuse existing steps; no new page objects.
Acceptance: `login.feature` has 12 scenarios; new ones pass on Chrome against the live app; the traceability table in the architecture addendum names scenario IDs; step definitions contain no locators.

**T-08 Documentation (supports S-2, S-3, S-4, M-1, M-2, S-1)**
Files: `docs/sdlc/architecture.md` (append "Addendum" only), `config.properties` comments.
- State the DriverManager contract (quit in `finally`, init cleanup, hooks never create drivers), wait strategy (implicit 0, explicit only, FR-4 best-effort and tag), config precedence (including pom/`-D`/env), credential exception valid only while `base.url` is the public demo host, corrected traceability, and a headless CI note (`-Dheadless=true`; C-5 CI portion).
Acceptance: addendum contains each of the six points; a reader can find the `-Dcucumber.filter.tags` and `-Dbrowser` usage.

**T-09 Validation on the real app (all)**
No code changes except defects found (raise as new tasks).
- Chrome headed: full suite against the live demo; record pass/fail and durations.
- Chrome headless: `mvn test -Dheadless=true`.
- Slow network: Chrome DevTools throttling (or OS throttle, for example Slow 3G profile) to confirm `pageLoadTimeout` (30s) and explicit wait (15s) produce a clear `TimeoutException`, not a hang; and that a throttled run still tears down cleanly (no orphan processes).
- Firefox/Edge: one smoke run each, results recorded; not blocking (NFR-1, unverified in review).
- Confirm C-2 relevance: note any Chrome/driver incompatibility.
Acceptance: results table (browser, mode, scenarios passed/total, notes) is produced for Stage 6; zero orphan browser processes after all runs.

**T-10 Regression gate**
- `grep` shows no `Thread.sleep` and no locators in `stepdefinitions`; `mvn clean test` green; cucumber html/json/junit reports present under `target/cucumber-reports`; a deliberately failed scenario still attaches a screenshot.
Acceptance: all five checks recorded.

## 3. Dependency Table

| Task | Depends on |
|---|---|
| T-01, T-02, T-05, T-06 | none |
| T-03 | T-02 |
| T-04 | T-01 |
| T-07 | T-02 |
| T-08 | T-01..T-07 |
| T-09 | T-01..T-07 |
| T-10 | T-09 |

No cycles (graph is a DAG; critical path T-01 -> T-04 -> T-09 -> T-10 or T-02 -> T-03/T-07 -> T-09 -> T-10).

## 4. Phased Execution Order

1. **Foundation (config and driver):** T-01, T-02, T-05, T-06 (must-fix first: T-01, T-02).
2. **Framework:** T-04, T-03.
3. **Tests:** T-07.
4. **Integration and docs:** T-08.
5. **Validation:** T-09, T-10.

## 5. Design-Review Condition Coverage and Risk Mapping

| Review item | Task | Risk |
|---|---|---|
| M-1 quit/ThreadLocal in finally; init-failure orphan | T-01 | RK-1, RK-2 |
| M-2 implicit vs explicit; FR-4 observation | T-02, T-03 | RK-3 |
| S-1 teardown relaunch | T-04 | RK-5 |
| S-2 coverage overstated | T-07 (add), T-08 (record FR-1 gap) | RK-7 |
| S-3 browser precedence | T-05, T-08 | n/a |
| S-4 credentials exception and env vars (A-1) | T-06, T-08 | RK-6 |
| Condition 3: credential exception only for public demo host | T-06 (guard), T-08 | RK-6 |
| R-1 demo site availability (accepted) | T-09 clear timeouts; no retries added | RK-4 |
| RK-8 tool versions | T-09 Chrome check | RK-8 |
| RK-9 selector change | locators stay centralised in pages (T-03 scoped) | RK-9 |

## 6. Deferred (Consider items, not scheduled; none blocks merge)

| ID | Item | Reason |
|---|---|---|
| C-1 | Remove `--remote-allow-origins=*` | Revisit only if T-09 shows current Chrome works without it |
| C-2 | Upgrade Selenium/WebDriverManager, drop WDM | Do only if T-09 finds a Chrome incompatibility |
| C-3 | `<scope>test</scope>` for pom dependencies | Cosmetic |
| C-4 | `ConfigReader.getInt` message, blank `-D` handling | Low value |
| C-5 | Page source/URL on failure; CI workflow file | Headless CI note is in T-08; workflow itself out of scope |
| C-6 | `DashboardPage` TimeoutException readability | Cosmetic |

## 7. Success Criteria and Traceability

- Both must-fix items implemented and verified (T-01, T-02, T-03, T-09).
- S-1..S-4 implemented or recorded (T-04..T-08).
- Full suite (12 scenarios, or 11 when `@timing` is excluded) passes on Chrome; reports generated; failures carry a screenshot.
- No `Thread.sleep`, no selectors in steps, no new dependencies, no non-demo secrets.

| Requirement | Tasks |
|---|---|
| FR-1 | T-07 (gap recorded), T-09 |
| FR-2, FR-6, FR-7, FR-8 | existing scenarios, verified in T-09 |
| FR-3 | T-07 |
| FR-4 | T-03 |
| FR-5 | T-07 |
| NFR-1 | T-05, T-09 |
| NFR-3 | T-09 (timeouts) |

**Validation performed:** every architecture component is either covered by a task or marked existing/verified; all must-fix and should-fix items are mapped; the dependency graph is acyclic; order is foundation, framework, tests, integration, validation; acceptance criteria are observable; effort total 11.5 hours is proportionate to the scope.
