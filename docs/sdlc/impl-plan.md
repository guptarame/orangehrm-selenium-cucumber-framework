# Implementation Plan

**Feature:** OrangeHRM Login Automation Framework
**Source Documents:** `docs/sdlc/architecture.md` (this cycle), `docs/sdlc/design-review.md` (this cycle — **Verdict: APPROVED WITH CONDITIONS**, 0 Must-fix, 4 Should-fix, 5 Nice-to-have)
**Date:** 2026-09-27
**Agent:** planning-agent
**Task Count:** 8
**Complexity Summary:** 5 Low, 3 Medium
**Total Effort Estimate:** ~4 hours

> Scope note: FR-1..FR-8 already have full, passing scenario coverage in `login.feature` and are **not** re-planned as new work. Every task below either (a) fixes a design-review Should-fix condition against existing code, (b) closes a design-review Nice-to-have, or (c) validates that (a)/(b) didn't regress the existing 10 scenarios. No new page objects, drivers, or scenarios are needed this cycle.

---

## 1. Task Breakdown

| ID | Task | Priority | Complexity | Effort | Dependencies |
|----|------|----------|------------|--------|---------------|
| T1 | Fix the FR-4 loading-indicator race in `@TS_LOG_009` — poll for the indicator at/around the click, not in a separate `Then` step after step-boundary return | Should-fix (design-review §5 Should-fix #1 / Risk R1, **High** severity) | Medium | 60 min | None |
| T2 | Route the hardcoded `"Admin"`/`"admin123"` literals in `login.feature` (`@TS_LOG_002/005/006/007`) through `ConfigReader.getValidUsername()`/`getValidPassword()`-backed steps | Should-fix (design-review §5 Should-fix #2 / Finding S2) | Medium | 45 min | T1 (shared edits in `LoginSteps.java`/`login.feature` — sequence to avoid merge conflicts) |
| T3 | Narrow `BasePage.isDisplayed()`'s caught exception from `Exception` to `TimeoutException` | Should-fix (design-review §5 Should-fix #4 / Finding R4) | Low | 15 min | None |
| T4 | Document the credential-externalization precondition: annotate `config.properties` (and README, if present) that the checked-in `valid.username`/`valid.password` are OrangeHRM's public-demo values, and that `ConfigReader`'s `-D` override already supports CI-secret injection before any non-public reuse | Should-fix (design-review §5 Should-fix #3 / Finding S1, conditionally deferrable per design-review §6 condition 2) | Low | 15 min | None |
| T5 | Remove the unused `pom.xml` `<env>qa</env>` property and its Surefire `systemPropertyVariables` passthrough | Nice-to-have #1 | Low | 10 min | None |
| T6 | Refactor `LoginSteps` to call `DriverManager.getDriver()` once and share the same `WebDriver` reference across `LoginPage`/`DashboardPage`/`ResetPasswordPage` construction | Nice-to-have #5 | Low | 15 min | T2 (shared edits in `LoginSteps.java`) |
| T7 | Add a Firefox execution leg (Maven profile or CI job) and an OWASP Dependency-Check plugin binding | Nice-to-have #3 and #4 | Medium | 45 min | T5 (shared edits in `pom.xml`) |
| T8 | Full regression validation: run the complete `@TS_LOG_001`–`@TS_LOG_010` suite on Chrome, confirm all should-fix fixes hold and nothing regressed | Validation | Low | 40 min | T1, T2, T3, T4, T5, T6, T7 |

---

## 2. Task Details (complex tasks)

### T1 — Fix FR-4 loading-indicator race (Should-fix #1)
- **Root cause (confirmed in code):** `LoginSteps.the_user_submits_valid_credentials()` (`LoginSteps.java:63-67`) calls `loginPage.login(...)` and returns; a separate `Then` step, `a_loading_indicator_should_be_displayed_during_authentication()` (`LoginSteps.java:127-131`), then calls `loginPage.isLoadingIndicatorDisplayed()` (`LoginPage.java:129-131`, a fresh 3s `WebDriverWait`). Cucumber's step dispatch between the `When` and `Then` is an uncontrolled gap — on a fast auth response the spinner can appear and vanish entirely before the `Then` step starts polling, producing a false failure on a healthy app.
- **Fix approach:** add a `LoginPage` method that clicks login and immediately begins polling for the indicator in the same call (e.g. `submitAndCaptureLoadingIndicator(username, password)` returning `boolean`), so the wait starts at/around the click rather than after a step-boundary return. `LoginSteps` stores the captured boolean in an instance field during the `When` step; the `Then` step asserts the stored value instead of re-polling.
- **Deliverables:** modified `LoginPage.java` (new/changed method, `LOADING_INDICATOR` locator unchanged), modified `LoginSteps.java` (`@TS_LOG_009`'s `When`/`Then` steps), `login.feature` step text unchanged or clarified if needed.
- **Acceptance criteria:**
  - No `Thread.sleep` introduced anywhere in the fix.
  - The loading-indicator check begins polling within the same method call that triggers the click (verifiable by reading the diff — no intervening step-boundary return between click and first poll).
  - `mvn test -Dcucumber.filter.tags="@TS_LOG_009"` passes on 5 consecutive local Chrome runs (0 flaky failures).
  - `@TS_LOG_001` (same `login()`/dashboard-redirect path) still passes unchanged.

### T2 — Route hardcoded credentials through `ConfigReader` (Should-fix #2)
- **Confirmed literals:** `login.feature:18` (`@TS_LOG_002`, username `"Admin"`), `:36` (`@TS_LOG_005`, password `"admin123"`), `:41` (`@TS_LOG_006`, username `"Admin"`), `:46` (`@TS_LOG_007`, password `"admin123"`) — these duplicate `config.properties:21-22`. `@TS_LOG_003` (`login.feature:24`) already demonstrates the target pattern (`"the user logs in with username {string} and a valid password"` → `ConfigReader.getValidPassword()`), so this task extends that existing pattern rather than inventing a new one.
- **Fix approach:** reword the four affected `login.feature` steps to reference "the valid username" / "the valid password" instead of literal values (mirroring `@TS_LOG_003`'s phrasing), and add the corresponding `LoginSteps` step definitions that pull the value from `ConfigReader.getValidUsername()`/`getValidPassword()`. Genuinely invalid test values (`"wrongpass"`, `"InvalidUser"`, `""`) stay as literals — they are not the credential being centralized.
- **Deliverables:** modified `login.feature` (4 scenario steps), modified `LoginSteps.java` (new step definitions following the existing `@TS_LOG_003` pattern).
- **Acceptance criteria:**
  - `grep -c "Admin\|admin123" src/test/resources/features/login.feature` returns `0`.
  - All four scenarios (`@TS_LOG_002/005/006/007`) still pass with the same assertions/behavior as before the change.
  - No new config keys needed beyond the existing `valid.username`/`valid.password`.

### T7 — chrome CI leg + dependency-vulnerability scan (Nice-to-have #3, #4)
- **Deliverables:** a Maven profile (or documented CI job) that runs `mvn test -Dbrowser=chrome`; an `owasp:dependency-check-maven` plugin binding added to `pom.xml` (bound to a non-default phase/profile so it doesn't block every local `mvn test`).
- **Acceptance criteria:**
  - `mvn test -Dbrowser=chrome` (or the added profile invocation) completes and its pass/fail result is documented.
  - Dependency-check plugin is present in `pom.xml` and runs to completion via its bound goal without requiring network access during normal `mvn test`.
  - No credential values appear in any new CI/build config.

---

## 3. Dependency Graph

```
T1 ──▶ T2 ──▶ T6 ─┐
T3 ────────────────┤
T4 ────────────────┼──▶ T8
T5 ──▶ T7 ─────────┘
```

- T1 → T2: both touch `LoginSteps.java`/`login.feature`; sequencing avoids merge conflicts (no logical dependency).
- T2 → T6: both touch `LoginSteps.java`.
- T5 → T7: both touch `pom.xml`.
- T3, T4 are fully independent (different files: `BasePage.java`, `config.properties`/README).
- T8 depends on all of T1–T7 (final regression needs the complete change set).
- No cycles: T1→T2→T6→T8, T3→T8, T4→T8, T5→T7→T8 — a strict DAG.

---

## 4. Phased Execution Order

1. **Foundation (config/documentation hardening):** T3 (`BasePage` exception narrowing), T4 (credential-externalization documentation), T5 (remove unused `env` property) — independent, no shared-file conflicts, safest to land first.
2. **Framework (page-object synchronization fix):** T1 (FR-4 race fix in `LoginPage`/`LoginSteps`).
3. **Test data / test scenarios:** T2 (credential routing in `login.feature`/`LoginSteps`).
4. **Integration (framework cleanup + CI/tooling):** T6 (shared driver reference in `LoginSteps`), T7 (chrome CI leg + dependency scan).
5. **Validation:** T8 (full Chrome + chrome regression of `@TS_LOG_001`–`@TS_LOG_010`, confirms all should-fix items hold).

---

## 5. Design-Review Condition Coverage

| Design-Review Item | Type | Plan Task | Disposition |
|---|---|---|---|
| Should-fix #1 — FR-4 loading-indicator race (R1) | Should-fix | **T1** | Fixed |
| Should-fix #2 — credential literal/config duplication (S2) | Should-fix | **T2** | Fixed |
| Should-fix #3 — externalize `valid.username`/`valid.password` before non-public reuse (S1) | Should-fix | **T4** | Documented/tracked (design-review §6 explicitly allows deferral while scope stays on the public demo instance, provided it's tracked — not silently carried forward) |
| Should-fix #4 — narrow `isDisplayed()` caught exception (R4) | Should-fix | **T3** | Fixed |
| Nice-to-have #1 — unused `<env>qa</env>` | Nice-to-have | **T5** | Removed |
| Nice-to-have #2 — retry for transient network failure | Nice-to-have | *(none)* | **Explicitly deferred** — not required by requirements.md; no observed transient flakiness in current suite; revisit only if CI shows real network-flake evidence |
| Nice-to-have #3 — CI matrix leg for Firefox/Edge | Nice-to-have | **T7** | Addressed (Firefox leg; Edge left for a future cycle per design-review's own scope note) |
| Nice-to-have #4 — dependency-vulnerability scan | Nice-to-have | **T7** | Addressed |
| Nice-to-have #5 — single shared driver reference in `LoginSteps` | Nice-to-have | **T6** | Addressed |

All 4 Should-fix conditions and all 5 Nice-to-have items from `design-review.md` §5 are accounted for above — none dropped silently.

---

## 6. Risk Mitigation Mapping

| Risk (design-review §4) | Severity | Mitigation Task |
|---|---|---|
| FR-4 assertion fails intermittently on a healthy app (R1) | High | T1 |
| Real credentials committed to a fork/reuse against a non-public app (S1) | High | T4 (tracked precondition); no functional change required while scope stays on the public demo |
| Credential drift between `config.properties` and hardcoded Gherkin literals (S2) | Medium | T2 |
| `isDisplayed()` masking a real driver/session error as "not found" (R4) | Low | T3 |
| Dead `env` Maven property implies unsupported multi-environment capability | Low | T5 |
| No parallel/Grid execution | Informational (N/A) | Not planned — explicitly out of scope per design-review, suite size doesn't justify it |

---

## 7. Success Criteria

- All 8 tasks completed with their stated deliverables; no cycles in the dependency graph (verified in §3).
- `mvn clean test-compile` succeeds after every phase.
- `mvn test` (default Chrome) runs `@TS_LOG_001`–`@TS_LOG_010` to completion with exit code 0; `mvn test -Dbrowser=chrome` result documented (T8).
- `@TS_LOG_009` passes on 5 consecutive Chrome runs with the T1 fix in place (no flaky failures).
- `grep` for `"Admin"`/`"admin123"` in `login.feature` returns zero matches after T2.
- `BasePage.isDisplayed()` catches `TimeoutException`, not `Exception`, after T3.
- No credential value appears in any SDLC document, commit message, or new CI config.
- All 4 Should-fix conditions from `design-review.md` are either fixed (T1, T2, T3) or explicitly tracked with documented rationale (T4) — none silently carried forward.

---

## 8. Traceability

- **Architecture:** `docs/sdlc/architecture.md` (this cycle)
- **Design Review:** `docs/sdlc/design-review.md` (this cycle — APPROVED WITH CONDITIONS)
- **Requirements:** `docs/sdlc/requirements.md` (FR-1..FR-8, NFR-1..NFR-5, US-AUTH-001)
- **Code paths this plan touches:** `src/test/resources/features/login.feature`; `src/test/java/com/orangehrm/pages/{LoginPage,BasePage}.java`; `src/test/java/com/orangehrm/stepdefinitions/LoginSteps.java`; `src/test/resources/config.properties`; `pom.xml`
- **Next Stage:** Implementation, then re-verification against `docs/sdlc/verification-report.md` conventions for this repo.
