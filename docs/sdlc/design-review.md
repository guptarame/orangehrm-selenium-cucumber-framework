# Design Review

**Feature:** Login Functionality for OrangeHRM Portal (US-AUTH-001)
**Date:** 2026-11-11
**Agent:** design-review-agent (SDLC Stage 3, run 3)
**Reviewed:** `docs/sdlc/architecture.md` (approved by human, commit 8f83691)
**Against:** `docs/sdlc/requirements.md`, `pom.xml`, `src/test/java/com/orangehrm/**`, `src/test/resources/**`
**Method:** static review of documents and source. No browser, network, dependency resolution or live-site behaviour was executed; claims about runtime behaviour are marked "unverified".

## 1. Executive Summary

**Verdict: APPROVED WITH CONDITIONS**

The architecture is proportionate: it keeps the existing Selenium 4 / Cucumber 7 / JUnit 4 / Maven POM stack, keeps responsibilities separate (steps assert, pages act, hooks manage lifecycle), and honestly records its own gaps (A-1, R-1..R-5). No requirement is structurally unaddressed. However, two claims in the document are not true of the code as written, and four traceability or configuration items are overstated.

| Category | Count |
|---|---|
| Must fix (blocks implementation planning) | 2 |
| Should fix (before merge) | 4 |
| Consider (nice to have) | 6 |
| Positive confirmations | 7 |

Blocking conditions: M-1 and M-2 must be carried into the Stage 4 plan as explicit tasks, or explicitly accepted by the human reviewer.

## 2. Requirements Coverage

| Req | Architecture coverage | Code / scenario check | Status |
|---|---|---|---|
| FR-1 UI elements | `LoginPage` visibility checks | `LoginPage.java:132-138` checks logo, username, password, button, forgot link only. Placeholders and footer copyright/social links (FR-1 ACs 2, 3, 6) are not checked. | Partial (S-2) |
| FR-2 Masking | `type` attribute | `LoginPage.java:155-158`, TS_LOG_007 | Covered |
| FR-3 Success | `login` -> `DashboardPage` | TS_LOG_001. "Pressing Enter" has no scenario. Dashboard URL checked as `"dashboard"` (`DashboardPage.java:12`), not `dashboard/index`. | Partial (S-2) |
| FR-4 Loading indicator | Short-lived element capture | TS_LOG_009; design is unsound under 5s implicit wait (M-2) | At risk |
| FR-5 Invalid credentials | Error banner | TS_LOG_002, 003. The "both invalid" case in the AC has no scenario. | Partial (S-2) |
| FR-6 / FR-7 Required | Field-level accessors | TS_LOG_004-006 (XPath `LoginPage.java:28-33`) | Covered |
| FR-8 Forgot password | `clickForgotPassword` | TS_LOG_010, `ResetPasswordPage.java:20-23` | Covered |
| NFR-1 Browsers | `DriverManager` switch | Chrome/Firefox/Edge branches `DriverManager.java:45-77`; only Chrome is a success criterion in practice | Covered (config), unverified at runtime |
| NFR-2 JavaScript | Not tested | Honest in architecture section 7 | Accepted gap |
| NFR-3 Availability | Precondition | No pre-flight check; failures surface as 15s timeouts | Accepted (R-1) |
| NFR-4/5/6 | Unspecified | Architecture correctly scopes out | Accepted |

All 10 scenarios in `login.feature` were checked against the FR/NFR list; TS_LOG_008-010 extend beyond the PRD IDs (R-4), which is acceptable and correctly flagged.

## 3. Findings

### Must fix

**M-1. Driver lifecycle contract is weaker than the architecture claims (reliability).**
Architecture sections 4.5 and 6 state the driver is "always quit" and the failure path is safe. `Hooks.java:49-50` does use `finally`, but the guarantee depends on `DriverManager`, which has two holes:
- `DriverManager.java:88-95`: `driver.quit()` (line 92) is not in a `try/finally`. If `quit()` throws (crashed browser, lost session), `DRIVER_THREAD_LOCAL.remove()` (line 93) is skipped. The dead driver stays in the ThreadLocal, and because execution is sequential on one thread, every later scenario receives the dead session and fails.
- `DriverManager.java:79-85`: the driver is stored in the ThreadLocal only at line 85, after `implicitlyWait`, `pageLoadTimeout` and `maximize` run. If any of those throws, a live browser process is created but never recorded, so it can never be quit (orphan browser).

Recommendation: add a "DriverManager contract" to the architecture: `quitDriver()` removes the ThreadLocal in a `finally`; `initDriver()` quits the local driver if post-creation configuration fails.

**M-2. Implicit + explicit wait mixing makes negative checks and the FR-4 check unreliable (flakiness).**
Architecture R-2 treats this as "consider later". Evidence says it should be decided now:
- `DriverManager.java:79` sets a 5s implicit wait for every `findElement`. `BasePage.isDisplayed` (`BasePage.java:63-73`) polls with `visibilityOfElementLocated`, so each absent-element poll blocks up to 5s inside the call.
- `LoginPage.isLoadingIndicatorDisplayed` (`LoginPage.java:146-148`) uses a 3s window. With an absent spinner, the first lookup alone blocks 5s, exceeding the stated window, and by then a brief spinner has gone. Timeout behaviour is therefore effectively undefined (unverified live).
- The spinner selector `.oxd-form-loader, .oxd-loading-spinner` (`LoginPage.java:20`) may also match loaders on the dashboard that appear after redirect, so TS_LOG_009 could pass for the wrong reason (unverified live).
- The Selenium project documents that mixing the two waits can produce unpredictable timeouts.

Recommendation: set implicit wait to 0 (remove the `implicit.wait` use in `DriverManager`, keep explicit waits only), and define in the architecture how FR-4 is observed (for example, assert the spinner is attached to the submit button and scope the locator to the login form, or accept the check as best-effort and tag it so it can be excluded).

### Should fix

**S-1. Teardown can start a new browser (reliability).** `Hooks.java:36` calls `DriverManager.getDriver()`, which lazily creates a driver if none exists (`DriverManager.java:33-35`). If `@Before` failed during `initDriver`, `@After` will try to launch a browser again just to take a screenshot, producing a second failure. Add a non-creating accessor (for example `peekDriver()`) for hooks. The architecture should state that hooks never create drivers in teardown.

**S-2. Traceability overstates coverage (completeness).** Section 7 of the architecture marks FR-1, FR-3 and FR-5 as covered. See the coverage table: placeholders and footer (FR-1), Enter-key submission and `dashboard/index` URL (FR-3), and the both-invalid case (FR-5) are not asserted. Either add scenarios (a small `Scenario Outline` for FR-5 and an Enter-key scenario) or record these as explicit accepted gaps.

**S-3. `browser` default in `pom.xml` silently overrides `config.properties` (configuration).** `pom.xml:30` defines `<browser>chrome</browser>` and `pom.xml:103` passes it as a system property to Surefire. Because `ConfigReader.get` gives any non-empty system property priority (`ConfigReader.java:50-53`), `browser=` in `config.properties:10` is dead configuration under `mvn test`. Only `-Dbrowser=` works. Document this precedence in the architecture, or remove the pom property and pass-through.

**S-4. Credential handling and env-var resolution need a recorded decision (security).**
- `config.properties:36-37` holds `Admin/admin123` in plaintext, committed. These are public OrangeHRM demo values, so the exception is reasonable. However, the comment at `config.properties:29` cites "design-review.md, this cycle" as authority. This review is that record: **accepted exception, valid only while `base.url` is the public opensource-demo host**. No password is logged (grep of `src` found no logging of credentials; only `Hooks` and `DriverManager` use the logger).
- Architecture A-1 (no env-var support, `ConfigReader.java:49-59` reads only `-D` and the file) is a real gap against the agent guide's precedence order. It is not required by an FR/NFR, so it is not blocking, but it should be implemented before merge or consciously deferred in the Stage 4 plan. Add a guard so that a non-demo `base.url` with a password still loaded from the file fails fast.

### Consider (nice to have)

- **C-1.** `--remote-allow-origins=*` (`DriverManager.java:68`) is a workaround for older chromedriver/CDP and widens the origin policy for the debug port; test without it on current Chrome.
- **C-2.** Dependency currency: Selenium 4.27.0 and WebDriverManager 5.9.2 are dated relative to current Chrome releases (unverified). Selenium 4.6+ ships Selenium Manager, which makes WebDriverManager largely redundant. Confirm a Chrome run in Stage 6, then consider upgrading and dropping WebDriverManager. No known-vulnerable version was verified here (no dependency scan was run).
- **C-3.** `slf4j-simple` and other dependencies in `pom.xml:33-73` have no `<scope>test</scope>`. Harmless today since everything is under `src/test`, but conventional.
- **C-4.** `ConfigReader.getInt` (`ConfigReader.java:73-75`) throws a bare `NumberFormatException` without naming the key; wrap with a message. Also a blank `-D` value is silently ignored (`ConfigReader.java:51`).
- **C-5.** Failure diagnostics are screenshot-only (`Hooks.java:37-41`). Page source and URL attached on failure would help diagnose demo-site issues. No CI workflow exists in the repo (`.github/` has only `agents` and `.vscode`); a short headless CI note (`-Dheadless=true`) in the architecture would complete the CI scope item.
- **C-6.** `DashboardPage.isDashboardPageDisplayed` (`DashboardPage.java:21`) throws `TimeoutException` on failed login rather than returning false, so the assertion message in `LoginSteps.java:107` is never reached; fine, but note for readability.

### Positive confirmations

1. Hooks structure is sound: screenshot capture is isolated in its own try/catch for `WebDriverException` (`Hooks.java:38-47`) and `quitDriver()` is in an outer `finally` (`Hooks.java:49-50`).
2. `ThreadLocal` driver is correctly keyed and removed, so future parallelism is plausible once M-1 is fixed.
3. `ConfigReader` precedence matches the document (`-D` then file then default), is thread-safe (double-checked locking, `volatile`, `ConfigReader.java:19,34-43`) and fails fast on a missing required key (`ConfigReader.java:55-57`).
4. No `Thread.sleep` anywhere in `src`; no locators in step definitions; page objects contain no assertions.
5. Locators are semantic (`By.name`, scoped XPath) rather than positional; maintainability is good.
6. Feature file contains no credentials; the valid password comes from `ConfigReader` at runtime (`LoginSteps.java:54`).
7. Surefire is configured with `testFailureIgnore=false` and a single `TestRunner` include (`pom.xml:98-101`), so failures fail the build.

## 4. Risk Assessment

| ID | Risk | Likelihood | Impact | Severity | Mitigation |
|---|---|---|---|---|---|
| RK-1 | Failed `quit()` leaves a dead driver in ThreadLocal; cascade failure (M-1) | Low | High | High | `finally` around remove; quit-on-init-failure |
| RK-2 | Orphan browser process when init partially fails (M-1) | Low | Medium | Medium | Same fix; CI runner cleanup |
| RK-3 | Implicit/explicit wait mixing causes slow or flaky negatives and a false FR-4 result (M-2) | High | Medium | High | Implicit wait 0; scope spinner locator |
| RK-4 | Public demo site unavailable, reset or rate-limited (R-1) | Medium | High | Medium | Accepted; clear timeout messages; rerun policy outside the framework |
| RK-5 | Teardown relaunches a browser after setup failure (S-1) | Low | Medium | Medium | Non-creating accessor |
| RK-6 | Credentials committed and framework later pointed at a non-demo host | Low | High | Medium | Env/`-D` injection; fail-fast guard (S-4) |
| RK-7 | Coverage claim exceeds actual assertions (S-2) | High | Low | Medium | Add scenarios or record gaps |
| RK-8 | Tool versions lag current Chrome (C-2) | Medium | Medium | Medium | Verify in Stage 6; upgrade if failing |
| RK-9 | UI selectors change on the demo site (OrangeHRM class names) | Medium | Medium | Medium | Locators centralised in page objects |

## 5. Gaps and Prioritised Recommendations

| Priority | Item | Type | Action |
|---|---|---|---|
| 1 | M-1 DriverManager lifecycle | Architecture + implementation | Document contract; `finally` in `quitDriver`; guard init |
| 2 | M-2 Wait strategy | Architecture + implementation | Implicit wait 0; define FR-4 observation approach |
| 3 | S-1 Teardown driver access | Implementation | Add `peekDriver()`; use in `Hooks` |
| 4 | S-2 Coverage gaps | Architecture + tests | Add or formally accept scenarios |
| 5 | S-3 Browser precedence | Architecture + config | Document or remove pom pass-through |
| 6 | S-4 Credentials / env vars | Architecture + implementation | Record exception; implement A-1 or defer explicitly |
| 7 | C-1..C-6 | Implementation | Backlog; none blocks merge |

Counts: critical (must fix) 2, warnings (should fix) 4, recommendations (consider) 6.

## 6. Approval Conditions, Comments and Sign-off

**Conditions for proceeding to Stage 4 (Planning):**
1. The plan includes M-1 and M-2 as tasks (or the human reviewer explicitly accepts the risk).
2. The plan lists S-1..S-4 as pre-merge tasks, or records each as an accepted deferral with reason.
3. The architecture addendum (or plan) states that the credential exception applies only to the public demo host.

**Reviewer comments:** The design is deliberately small and mostly correct. The two blocking items are narrow and low-effort; they are about stating and enforcing a driver and wait contract that the document currently assumes. Distinction kept between architecture gaps (M-1 contract, M-2 decision, S-2, S-3) and implementation gaps (S-1, `quitDriver` code, A-1).

**Not verified:** live-site behaviour, selector validity, spinner timing, browser/driver compatibility, dependency vulnerabilities, Firefox/Edge execution.

**Sign-off:** design-review-agent, 2026-11-11. Verdict: APPROVED WITH CONDITIONS. Human approval required before Stage 4.

## 7. Traceability

- Architecture: `docs/sdlc/architecture.md` sections 2, 4, 5, 6, 7, 8 (A-1, R-1..R-5)
- Requirements: `docs/sdlc/requirements.md` FR-1..FR-8, NFR-1..NFR-6
- Code: `Hooks.java:33-53`; `DriverManager.java:32-95`; `ConfigReader.java:49-75`; `BasePage.java:63-73`; `LoginPage.java:20,132-148`; `DashboardPage.java:12,20-23`; `LoginSteps.java:26,54,107`; `login.feature` (TS_LOG_001-010); `config.properties:29-37`; `pom.xml:30,98-103`
- Next stage: Implementation Planning
