# [Feature] Complete Selenium login automation framework

## Summary

This PR adds a reusable Selenium test automation framework for the My Account login flow. It includes page-object abstractions, browser lifecycle setup, configuration-driven execution, structured logging, screenshots on failure, and JUnit lifecycle diagnostics.

The implementation is traceable to the SDLC artifacts under `docs/sdlc/`, including requirements, architecture, design review, implementation planning, code review, and verification evidence.

## Changes Made

- Added browser/test lifecycle support in `BaseTest`
- Added a reusable `BasePage` abstraction for bounded explicit waits
- Added/updated `LoginPage` for login, validation, lost-password, and authenticated-state interactions
- Added configuration support in `TestConfig`, including browser, headless mode, target URL, credentials, and page-load timeout handling
- Added shared test data in `TestData`
- Added structured logging through `LogUtil`
- Added screenshot capture through `ScreenshotUtil`
- Added JUnit diagnostics through `TestLifecycleListener`
- Added login scenario coverage in `LoginPageTests`
- Added/updated SDLC artifacts under `docs/sdlc/`

## Test Evidence

Primary verification evidence is recorded in `docs/sdlc/verification-report.md`.

### Observed command results

| Command | Result |
|---|---|
| `mvn clean test` | `BUILD SUCCESS`; 7 tests, 0 failures, 0 errors, 3 skipped |
| `mvn clean test -Dbrowser=chrome -Dheadless=true` | `BUILD SUCCESS`; 7 tests, 0 failures, 0 errors, 3 skipped |
| `mvn test -Dheadless=true` | `BUILD SUCCESS`; 7 tests, 0 failures, 0 errors, 3 skipped |
| `mvn clean test -Dbrowser=firefox -Dheadless=true` | `BUILD FAILURE`; 7 tests, 0 failures, 4 errors, 3 skipped |
| `mvn test -Dheadless=true -DpageLoadTimeout=0` | `BUILD SUCCESS`; invalid timeout value fell back safely |
| `mvn dependency:tree` | `BUILD SUCCESS`; dependency tree resolved; no CVE/SCA scan was run |

### Chrome headless accounting

- **Tests run:** 7
- **Passed:** 4
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 3

Passing non-credential scenarios:
- TS-LOG-003 unknown user
- TS-LOG-004 blank fields
- TS-LOG-006 lost-password navigation
- AC-UI-001 required controls

Skipped credential-dependent scenarios:
- TS-LOG-001 valid login
- TS-LOG-002 invalid password
- TS-LOG-005 Remember Me

### Firefox headless accounting

- **Tests run:** 7
- **Failures:** 0
- **Errors:** 4
- **Skipped:** 3

Firefox did not meet the cross-browser baseline in this environment. The observed errors were 30-second navigation timeouts while opening the live target page.

## Known Limitations

- Stage 6 verification is **not a full acceptance pass** because Firefox execution failed in this environment.
- Credential-dependent scenarios were skipped because valid credentials were not supplied through environment or system-property configuration.
- Remember Me persistence is implemented by source inspection but was not exercised end-to-end without credentials.
- Slow-network validation was not run.
- A dependency tree was generated, but no vulnerability/SCA scan was run.
- Screenshot retention/redaction policy is not configured in Maven/CI.

## Reviewer Checklist

- [ ] Framework architecture is clear and matches `docs/sdlc/architecture.md`
- [ ] Implementation follows the approved plan in `docs/sdlc/impl-plan.md`
- [ ] Page Object Model usage is consistent
- [ ] Browser lifecycle and cleanup are reliable
- [ ] Timeouts and explicit waits are bounded and maintainable
- [ ] Credential-dependent tests remain externalized and do not log secrets
- [ ] Chrome non-credential execution evidence is acceptable
- [ ] Firefox navigation timeout blocker is understood before claiming cross-browser support
- [ ] Verification evidence in `docs/sdlc/verification-report.md` is reviewed
- [ ] Follow-up plan exists for credentials, Firefox stability, slow-network testing, and dependency scanning

## Merge Guidance

Treat this PR as reviewable framework work, but do not claim full cross-browser or credential-flow acceptance until:

1. Valid non-production credentials are supplied securely and TS-LOG-001, TS-LOG-002, and TS-LOG-005 are rerun.
2. Firefox navigation timeouts are investigated and the Firefox suite is rerun successfully.
3. Slow-network validation and an approved dependency vulnerability scan are completed if required for release.
