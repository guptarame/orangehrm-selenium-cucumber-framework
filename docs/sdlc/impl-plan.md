# Test Automation Implementation Plan

**Project:** Selenium Login Automation Capstone (US-AUTH-002)
**Based on:** `docs/sdlc/architecture.md`, `docs/sdlc/design-review.md`
**Date:** 2026-09-22
**Agent:** planning-agent
**Scope:** Framework hardening, scenario verification, and live-execution evidence. No
application/product code changes are planned.

## Overview

The repository already contains the main framework components: `TestConfig`,
`BaseTest`, `BasePage`, `LoginPage`, `TestData`, the lifecycle listener, logging,
screenshots, and seven login/UI tests. The plan therefore treats those items as
verification or targeted improvement work rather than greenfield implementation.

This plan contains **14 tasks**. Estimated effort is **approximately 3 hours 10
minutes** (execution time for live-browser and throttled-network tasks may vary).

| Complexity | Count |
|---|---:|
| High | 3 |
| Medium | 7 |
| Low | 4 |

## Existing Baseline

Already present and to be verified/improved:

- `TestConfig.pageLoadTimeout()` exists with system-property/environment fallback.
- `BaseTest` applies page-load and implicit waits and supports Chrome/Firefox.
- `BasePage` is abstract and `LoginPage` extends it.
- `LogUtil`, `ScreenshotUtil`, `TestLifecycleListener`, and `TestData` exist.
- Tests cover valid login, invalid password, unknown user, blank fields,
  Remember Me, lost-password navigation, and required-control visibility.

The design-review defects remain actionable: the Remember Me test does not create
the shared profile before the first driver, and outcome reads are not synchronized
after submit/navigation.

## Task Breakdown

### TASK-001: Verify and harden timeout configuration (MUST FIX condition)

**Priority:** Critical | **Complexity:** Low | **Effort:** 10 minutes
**Dependencies:** None | **Blocks:** TASK-002, TASK-013

**Deliverables**

- Verify `TestConfig` precedence: system property, environment variable, default.
- Keep the existing 30-second default and `-DpageLoadTimeout` support.
- Add positive-value validation (or a documented fallback) for malformed/zero
  timeout values; align the environment variable name with the architecture
  (`PAGE_LOAD_TIMEOUT_SECONDS`) or document the current `PAGE_LOAD_TIMEOUT`.

**Acceptance criteria**

- `pageLoadTimeout()` returns a positive integer for default and valid overrides.
- Invalid values do not create a zero/negative Selenium timeout.
- A focused configuration check demonstrates precedence and fallback behavior.

### TASK-002: Verify BaseTest driver and lifecycle integration

**Priority:** Critical | **Complexity:** Medium | **Effort:** 10 minutes
**Dependencies:** TASK-001 | **Blocks:** TASK-013, TASK-014

**Deliverables**

- Confirm `BaseTest` applies the configured page-load timeout before tests run.
- Confirm driver registration/cleanup works for normal and failure paths.
- Standardize the implicit-wait policy (prefer zero, or document why the current
  one-second value is retained); avoid `maximize()` in headless mode.
- Preserve unsupported-browser failure and Chrome/Firefox option behavior.

**Acceptance criteria**

- Each test receives a fresh driver and teardown quits it.
- Configured page-load timeout is observable in code and a smoke execution.
- Headless runs use a deterministic window size without an unnecessary maximize.

### TASK-003: Verify and improve BasePage synchronization helpers

**Priority:** High | **Complexity:** Medium | **Effort:** 10 minutes
**Dependencies:** None | **Blocks:** TASK-004

**Deliverables**

- Retain the existing abstract `BasePage` API and explicit waits.
- Make the page wait duration configurable through `TestConfig` if practical.
- Harden `isVisible` against separate lookups/stale elements.
- Add reusable URL and outcome wait helpers without sleeps or broad retries.

**Acceptance criteria**

- Common navigation, click, type, visibility, text, and URL operations remain
  centralized in `BasePage`.
- Waits are bounded and produce useful timeout failures.
- No page object introduces `Thread.sleep()` or an unbounded retry loop.

### TASK-004: Add deterministic post-action waits to LoginPage (MUST FIX condition)

**Priority:** Critical | **Complexity:** High | **Effort:** 20 minutes
**Dependencies:** TASK-003 | **Blocks:** TASK-009, TASK-010, TASK-011, TASK-012, TASK-013

**Deliverables**

- Add outcome-specific waits for error banner, dashboard/logout, login-page
  state, and lost-password/reset URL.
- Make `submitLogin()` and `clickLostPassword()` usable without immediate,
  race-prone reads.
- Keep assertions in tests and business actions/state in the page object.

**Acceptance criteria**

- Invalid/blank submissions wait for an error or validation state.
- Successful login waits for dashboard/logout state.
- Reset-link test waits for the destination URL and does not rely on timing luck.
- A failed wait remains bounded and is diagnosable.

### TASK-005: Correct shared Remember Me profile lifecycle (MUST FIX condition)

**Priority:** Critical | **Complexity:** High | **Effort:** 25 minutes
**Dependencies:** TASK-002, TASK-003 | **Blocks:** TASK-011, TASK-013

**Deliverables**

- Create the temporary Chrome profile before the initial driver.
- Pass the same profile to both the login driver and restarted driver.
- Register the replacement driver and clean up the temporary profile after the
  test, including failure/teardown paths.
- Keep the Firefox skip explicit unless Firefox persistence is implemented.

**Acceptance criteria**

- The initial and restarted Chrome drivers use the identical profile directory.
- The test proves authentication persists after restart, not merely that the
  second login page loads.
- Profile directories are not left behind on successful or failed execution.

### TASK-006: Verify logging and lifecycle reporting

**Priority:** High | **Complexity:** Medium | **Effort:** 10 minutes
**Dependencies:** TASK-002 | **Blocks:** TASK-007, TASK-013

**Deliverables**

- Verify `LogUtil` emits timestamped, non-secret lifecycle/step/result messages.
- Verify `TestLifecycleListener` logs start, pass/fail, and completion and
  preserves the original test failure.
- Add any missing test-name or exception context without logging passwords.

**Acceptance criteria**

- Console/Surefire output identifies each test and its result.
- A listener failure cannot turn a passing test into a false failure or hide the
  original exception.
- No valid password is written by framework logging.

### TASK-007: Verify failure screenshots and artifact policy

**Priority:** High | **Complexity:** Medium | **Effort:** 10 minutes
**Dependencies:** TASK-006 | **Blocks:** TASK-013, TASK-014

**Deliverables**

- Verify `ScreenshotUtil` creates `target/screenshots` and sanitized,
  timestamped PNG names.
- Exercise a controlled failing test and confirm capture before teardown.
- Document CI retention/redaction handling because screenshots may contain
  account data; do not expose secrets in filenames or logs.

**Acceptance criteria**

- A failing browser test produces one readable screenshot and retains the
  original assertion failure.
- A non-screenshot-capable driver or filesystem error is logged without masking
  the test result.
- Artifact retention and access expectations are documented.

### TASK-008: Verify and extend centralized test data/config checks

**Priority:** Medium | **Complexity:** Low | **Effort:** 10 minutes
**Dependencies:** TASK-001 | **Blocks:** TASK-009, TASK-010, TASK-011, TASK-012

**Deliverables**

- Verify `TestData` owns shared message tokens and wait constants.
- Add focused checks for configuration precedence and safe credential defaults.
- Ensure synthetic unknown-user and invalid-password data remain deterministic.

**Acceptance criteria**

- Tests contain no hardcoded valid credentials.
- Missing credentials cause visible skips for credential-dependent scenarios.
- Configuration/helper checks pass without starting a browser.

### TASK-009: Verify valid and invalid-password scenarios

**Priority:** Critical | **Complexity:** Medium | **Effort:** 10 minutes
**Dependencies:** TASK-004, TASK-008 | **Blocks:** TASK-013

**Deliverables**

- Verify `TS-LOG-001` with supplied non-production credentials.
- Verify `TS-LOG-002` with a known username and wrong password.
- Add explicit post-login dashboard/logout and failed-login-page assertions.

**Acceptance criteria**

- Valid login asserts authenticated state and dashboard/welcome evidence.
- Invalid password asserts error feedback and remains on the login page.
- Credential-dependent tests skip with a clear reason when credentials are absent.

### TASK-010: Verify unknown-user and blank-field coverage

**Priority:** High | **Complexity:** Medium | **Effort:** 15 minutes
**Dependencies:** TASK-004, TASK-008 | **Blocks:** TASK-013

**Deliverables**

- Verify `TS-LOG-003` for an unregistered user and login-page retention.
- Verify `TS-LOG-004` for both fields blank.
- Add username-only and password-only cases if FR-02 is interpreted literally;
  otherwise record the accepted scope in the test documentation.

**Acceptance criteria**

- Unknown-user feedback is asserted using tolerant tokens plus non-authenticated
  page state.
- Blank validation is deterministic and checks meaningful field/error feedback.
- Any additional field-level cases have independent, readable assertions.

### TASK-011: Verify Remember Me scenario

**Priority:** High | **Complexity:** Medium | **Effort:** 10 minutes
**Dependencies:** TASK-005, TASK-009 | **Blocks:** TASK-013

**Deliverables**

- Execute `TS-LOG-005` on Chrome with valid non-production credentials.
- Verify checkbox selection, authenticated state, restart, and persisted session.
- Record Firefox skip and rationale, or implement/document an approved exception.

**Acceptance criteria**

- The test passes only when the same-profile restart retains authentication.
- Chrome/headless behavior is recorded.
- A missing credential or unsupported browser is reported as an explicit skip.

### TASK-012: Verify reset-link and required-control flows

**Priority:** Medium | **Complexity:** Low | **Effort:** 5 minutes
**Dependencies:** TASK-004 | **Blocks:** TASK-013

**Deliverables**

- Verify `TS-LOG-006` waits for and asserts the reset/lost-password URL.
- Verify `AC-UI-001` controls and URL; add destination-page controls if required
  by the accepted requirement interpretation.

**Acceptance criteria**

- Reset navigation is asserted after a bounded URL wait.
- Username, password, Remember Me, login, and lost-password controls are found
  through the page object and visibly asserted.

### TASK-013: Execute live Chrome/Firefox integration baseline (MUST FIX condition)

**Priority:** Critical | **Complexity:** High | **Effort:** 25 minutes
**Dependencies:** TASK-001, TASK-002, TASK-004, TASK-005, TASK-006, TASK-007,
TASK-009, TASK-010, TASK-011, TASK-012 | **Blocks:** TASK-014

**Deliverables**

- Run the suite against `https://askomdch.com/account/` in headed and/or
  headless Chrome and Firefox.
- Use a non-production account from CI/local secret storage; never commit or
  print credentials.
- Record browser/version/OS, pass/fail/skip results, timing, screenshots, and
  live-site/network classifications.
- Run `mvn dependency:tree` and the approved dependency/CVE scan, recording
  findings separately from functional results.

**Acceptance criteria**

- Both required browser factories execute and results are documented.
- All six `TS-LOG` scenarios and `AC-UI-001` are accounted for; credential
  skips are not represented as successful credential coverage.
- Failures are classified as product/test/environment issues with evidence.

### TASK-014: Slow-network validation, CI/reporting, and documentation

**Priority:** High | **Complexity:** Medium | **Effort:** 20 minutes
**Dependencies:** TASK-013 | **Blocks:** None

**Deliverables**

- Run a bounded throttled-network test (for example, 3G/400 ms latency) and
  record timeout/wait behavior.
- Document browser/OS/version matrix, secret handling, screenshot retention,
  skip policy, Maven commands, and dependency-scan results.
- Define a credentialed CI smoke job/reporting expectation; defer parallelism
  until driver/profile isolation is proven.

**Acceptance criteria**

- Slow-network tests complete deterministically or produce actionable bounded
  failures; no broad retries are added.
- `pageLoadTimeout` and explicit waits are demonstrably sufficient or have a
  justified revised value.
- Documentation enables another engineer to reproduce headed/headless and
  credentialed/non-credentialed runs.

## Dependency Graph

```text
TASK-001 ──> TASK-002 ───────────────┐
    │            │                   │
    └────────> TASK-008              │
TASK-003 ──> TASK-004 ──> TASK-009 ──┤
    │            │       TASK-010 ───┤
    └────────> TASK-005 ─> TASK-011 ─┤
                 │                   │
TASK-006 ──> TASK-007 ───────────────┤
TASK-004 ─────────────────> TASK-012 ┤
                                    ▼
                              TASK-013 ──> TASK-014
```

There are no circular dependencies. Tasks 001/003/006/008 can begin in parallel;
the execution order below is the recommended critical-path order.

## Phased Execution Order

1. **Foundation:** TASK-001, TASK-002.
2. **Framework synchronization and state isolation:** TASK-003, TASK-004,
   TASK-005.
3. **Diagnostics:** TASK-006, TASK-007.
4. **Data/config verification:** TASK-008.
5. **Scenario verification:** TASK-009, TASK-010, TASK-011, TASK-012.
6. **Integration and resilience:** TASK-013, TASK-014.

## Design-Review Condition Mapping

| Condition/recommendation | Task(s) | Coverage |
|---|---|---|
| Configurable positive `pageLoadTimeout` | TASK-001, TASK-002 | MUST FIX |
| Shared Remember Me profile | TASK-005, TASK-011 | MUST FIX |
| Deterministic post-submit/reset waits | TASK-003, TASK-004 | MUST FIX |
| Live application plus Chrome/Firefox execution | TASK-013 | MUST FIX |
| Failed-login page state and blank-field breadth | TASK-009, TASK-010 | Should fix |
| Standardize implicit/page waits | TASK-001, TASK-002, TASK-003 | Should fix |
| Browser/version and configuration validation | TASK-001, TASK-002, TASK-013, TASK-014 | Should fix |
| Logging and screenshot diagnostics | TASK-006, TASK-007 | Should fix |
| Secret/artifact policy and visible skips | TASK-007, TASK-008, TASK-013, TASK-014 | Should fix |
| Unit utility/config checks and dependency scan | TASK-008, TASK-013 | Should fix |
| Slow-network evidence | TASK-014 | MUST FIX |

## Risk Mitigation

| Risk | Mitigation task(s) | Evidence |
|---|---|---|
| Remember Me uses a different profile | TASK-005, TASK-011 | Same profile path and restart assertion |
| UI outcome is read before it updates | TASK-003, TASK-004 | Outcome-specific bounded waits |
| Live site/network changes | TASK-001, TASK-013, TASK-014 | Classified failures and diagnostics |
| Secrets in logs/screenshots/process history | TASK-006, TASK-007, TASK-013, TASK-014 | Redaction/retention policy and CI secret store |
| Chrome/Firefox differences | TASK-002, TASK-011, TASK-013 | Browser matrix and explicit Firefox scope |
| Skips conceal missing credential coverage | TASK-008, TASK-013, TASK-014 | Separate credentialed job and visible skip report |
| Wait-policy latency/flakiness | TASK-002, TASK-003, TASK-014 | One documented wait policy and timing baseline |
| Dependency vulnerability | TASK-013, TASK-014 | Dependency tree and approved SCA result |

## Success Criteria

- All architecture components are verified or have a named improvement task.
- Three must-fix review conditions are implemented and evidenced: shared
  Remember Me profile, outcome-specific waits, and live Chrome/Firefox runs.
- `pageLoadTimeout` is positive, configurable, and applied by `BaseTest`.
- The POM remains responsible for actions/state while tests retain assertions.
- Valid, invalid, unknown-user, blank, Remember Me, reset-link, and UI scenarios
  have measurable coverage; credential skips remain visible.
- Failure logs/screenshots are useful without exposing passwords.
- Slow-network, browser matrix, dependency, CI, and artifact policies are
  documented and reproducible.
- No circular task dependencies exist, and all acceptance criteria can be
  verified from code, test output, or recorded execution evidence.

## Traceability

- **Architecture:** `docs/sdlc/architecture.md`
- **Design review:** `docs/sdlc/design-review.md`
- **Requirements:** `docs/sdlc/requirements.md`
- **Existing code reviewed:** `src/test/java/Github_Copilot/base/BaseTest.java`,
  `config/TestConfig.java`, `pages/BasePage.java`, `pages/LoginPage.java`,
  `tests/LoginPageTests.java`, `data/TestData.java`,
  `listeners/TestLifecycleListener.java`, `utils/LogUtil.java`,
  `utils/ScreenshotUtil.java`
- **Build/reporting:** `pom.xml` (Java 21, Selenium 4.25.0, JUnit Jupiter 5.11.3,
  Surefire 3.5.0)
- **Next stage:** Stage 5 implementation and recorded validation
- **Planned commit message:** `[Planning] Create Selenium test framework implementation plan`
