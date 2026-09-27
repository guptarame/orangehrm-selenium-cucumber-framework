# Test Automation Framework Design Review

**Framework Version:** Architecture dated 2026-09-22
**Project:** Selenium Login Automation Capstone  
**Reviewed By:** design-review-agent  
**Date:** 2026-09-22
**Status:** APPROVED WITH CONDITIONS

## Executive Summary

The JUnit 5/Selenium design has a sensible Page Object Model, centralized configuration,
explicit waits, lifecycle diagnostics, and traceability to the six login scenarios.
However, the Remember me test does not reuse the profile in which login occurred, and
several outcome reads are not synchronized after submission. Live browser/application
behavior and dependency vulnerability status were not tested.

**Verdict:** APPROVED WITH CONDITIONS
**Critical Issues:** 2
**Warnings:** 7
**Recommendations:** 11

## Requirements Coverage

| Requirement | Scenario / capability | Addressed | Notes |
|---|---|---:|---|
| FR-01 / AC-1 | Login controls and selectors | Partial | Visibility is checked; labels and exact semantics are not independently asserted. |
| FR-02 / AC-2 | Blank-field validation | Partial | Both fields are blank in one test; username-only/password-only cases are absent. |
| FR-03 / AC-3 | Valid login and dashboard state | Yes, conditional | Requires supplied valid credentials; dashboard/logout tokens are checked. |
| FR-04 / AC-4 | Invalid password/unknown user remain on login | Partial | Error tokens are checked, but login-page state/URL is not explicitly asserted. |
| FR-05 / AC-5 | Remember me across restart | No | Initial driver uses no profile; the restarted driver uses a newly created profile. |
| FR-06 / AC-6 | Lost-password navigation | Yes | URL token is checked; destination page controls are not asserted. |
| NFR-01 | HTTPS and credential safety | Partial | HTTPS default and secret inputs are designed; artifact/command-line exposure remains. |
| NFR-02 | Deterministic reliability | Partial | Fresh drivers help, but post-submit reads can race the UI. |
| NFR-03 | Chrome/Firefox compatibility | Partial | Both factories exist; Remember me is Chrome-only and neither browser was run in review. |
| NFR-04 | POM and stable locators | Yes | `BasePage`/`LoginPage` centralize interactions and use id/name selectors. |
| NFR-05 | Assertions and observability | Yes | JUnit assertions, logs, Surefire XML, and failure screenshots exist. |

**Summary:** 5 fully addressed, 7 partial, and 1 not addressed. Credential-dependent
tests are skipped without credentials; this must remain visible in CI.

## Security Review

**Strengths**

- Credentials come from system properties or environment variables, not source defaults.
- The documented default URL uses HTTPS; passwords are not deliberately logged.
- Unknown-user data is synthetic and screenshot names are sanitized.

**Concerns**

- **High:** Screenshots may contain account data, and `-DvalidPassword=...` may expose a
  secret through shell history/process inspection. Use CI secret stores, avoid password
  command-line arguments, and define artifact retention/redaction rules.
- **Medium:** Chrome uses `--no-sandbox`; restrict this to isolated CI containers or make
  it opt-in rather than a general default.
- **Low:** Review exception logging to ensure application messages cannot contain secrets.
  No real credentials were inspected.

## Reliability and Flakiness

**Strengths:** New driver per test, teardown, explicit element waits, configurable
30-second page-load timeout, and failure screenshots preserving the original failure.

**Findings**

- **High:** `submitLogin().getFeedbackMessage()` reads immediately. Add outcome-specific
  waits for error banner, dashboard/logout, login-page state, and reset URL.
- **High:** Remember me creates `profileDir` after the first driver exists. Create the
  profile before the initial driver and pass the same profile to both drivers; clean it
  up afterward.
- **Medium:** Combining a one-second implicit wait with explicit waits can be inconsistent.
  Prefer zero implicit wait or document one standard policy.
- **Medium:** `isVisible` performs separate lookups and can encounter stale elements.
  Use one guarded lookup or an explicit condition.
- **Low:** Avoid `maximize()` in headless mode; use consistent fixed window sizing.
- Network failures and live wording changes have no classification strategy. Keep bounded
  timeouts and actionable diagnostics; do not add broad retries.

## Performance and Scalability

Per-test browser startup is reasonable for this small suite, but the two-second target
was not benchmarked and may be unrealistic for a live remote site. The one-second
implicit wait plus 12-second page wait may add latency. Parallel execution is correctly
deferred until driver/profile isolation and CI capacity are verified. Record a CI timing
baseline rather than weakening synchronization.

## Maintainability and POM

`BasePage`/`LoginPage`, `TestConfig`, `TestData`, listener, logging, and screenshot
utilities have clear responsibilities. Make the page wait duration configurable,
centralize business-outcome waits, validate browser/URL/positive timeout values, and
document public page methods. Keeping assertions in tests and state/actions in pages is
appropriate. No unnecessary abstraction is required for this capstone.

## Cross-Browser Support

Chrome and Firefox creation is isolated and configurable. The general tests are intended
for both, but this review did not launch either browser. Remember me is skipped on
Firefox and therefore lacks cross-browser coverage. Document a browser/version/OS
matrix and either implement Firefox persistence or obtain an explicit exception. Safari
and Edge are not required.

## Maven Integration and Reporting

Surefire 3.5.0 discovers `*Tests.java`; JUnit 5 provides lifecycle and assumptions;
XML reports and exact dependency versions are configured. Parallel execution, HTML
reporting, CI workflow, and toolchain enforcement are absent but not mandatory for the
current scope. Monitor skipped credential tests so a green build does not conceal
missing coverage.

## Testability

Configuration and page helpers are unit-testable; scenarios are live integration tests
dependent on site, browsers, credentials, and network. Add unit tests for configuration
precedence/invalid values and pure utility helpers. Add a credentialed integration smoke
job. No live, browser, or network testing was performed, and mocked tests would not
validate the live login flow.

## Dependency Safety

Selenium 4.25.0, WebDriverManager 6.1.0, JUnit Jupiter 5.11.3, and Surefire 3.5.0 are
explicitly versioned established libraries. This review did not run Maven or an SCA/CVE
scan, so no vulnerability-free claim is made. Run `mvn dependency:tree` and an approved
dependency scan in CI; review updates periodically and enforce Java 21/Maven versions.

## Risk Assessment

| Risk | Likelihood | Impact | Severity | Mitigation |
|---|---|---|---|---|
| Remember me uses the wrong profile | High | High | **Critical** | Share one temporary profile across both Chrome drivers and clean it up. |
| Post-submit state is read too early | High | High | **Critical** | Wait for mutually exclusive error/dashboard/login outcomes and reset URL. |
| Live site/network changes cause failures | Medium | High | **High** | Keep bounded page-load timeout, diagnostics, and environment-failure classification. |
| Secrets appear in artifacts/process history | Medium | High | **High** | CI secret store, no password CLI args, artifact review and retention policy. |
| Wording/selectors change | Medium | Medium | **Medium** | Stable attributes plus state and tolerant token assertions. |
| Firefox/headless behavior differs | Medium | Medium | **Medium** | Execute and record a browser matrix; isolate options. |
| Skips hide missing credential coverage | Medium | Medium | **Medium** | Separate visible credentialed CI job and fail protected runs when expected secrets are absent. |
| Combined waits increase execution time | Low | Low | **Low** | Standardize explicit waits and benchmark. |

## Identified Gaps

1. **Blocking:** Incorrect Remember me profile lifecycle.
2. **Blocking:** Missing post-action synchronization.
3. Add username-only and password-only blank cases if FR-02 is interpreted literally.
4. Assert failed login remains on the login page.
5. Define secret, screenshot, log, and CI artifact handling.
6. Execute and record a live Chrome/Firefox baseline.
7. Add dependency scanning and a CI definition before using this as a gate.

## Prioritized Recommendations

### Must fix (blocks implementation approval)

1. Correct and verify the shared Remember me profile lifecycle.
2. Add deterministic waits after login submission and reset navigation.
3. Run the suite against the real application with a non-production account and verify
   Chrome and Firefox; do not claim success for skipped credential tests.

### Should fix (before merge)

4. Assert failed-login location and add missing blank-field cases.
5. Standardize/remove the implicit wait and configure page wait duration.
6. Validate configuration and document browser/OS/version support.
7. Define secret, screenshot, log, and CI artifact policy.
8. Add unit tests for configuration/utilities and expose skipped scenarios in CI.

### Nice to have (future)

9. Add HTML report aggregation and screenshot links.
10. Add controlled parallel execution after isolation testing.
11. Add Edge/Safari if product support expands.

## Approval Conditions

**Conditional approval:** Planning may proceed only after a human reviewer accepts the
live-site dependency and the three must-fix items are completed and recorded:

- shared Remember me profile is fixed;
- outcome-specific waits are implemented;
- Chrome/Firefox execution against the target application is run and documented.

## Reviewer Comments and Sign-Off

The foundation is appropriate for a learning project and is substantially more
maintainable than a single-class Selenium suite. The two implementation defects must
be resolved before the suite is treated as evidence for authentication behavior.

**Design Review:** COMPLETE
**Recommendation:** APPROVED WITH CONDITIONS
**Next Step:** Resolve must-fix conditions, then proceed to planning and verification.

## Traceability

- Architecture: `docs/sdlc/architecture.md`
- Requirements: `docs/sdlc/requirements.md`
- Code: `src/test/java/Github_Copilot/base/BaseTest.java`,
  `config/TestConfig.java`, `pages/BasePage.java`, `pages/LoginPage.java`,
  `tests/LoginPageTests.java`
- Supporting code: `data/TestData.java`, `listeners/TestLifecycleListener.java`,
  `utils/LogUtil.java`, `utils/ScreenshotUtil.java`
- Build: `pom.xml`
- Output: `docs/sdlc/design-review.md`
