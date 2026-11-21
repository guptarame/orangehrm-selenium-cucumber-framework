# Verification Report

**Story:** `US-AUTH-002` — Customer Login & Authentication
**Task:** `TASK-008` — Full-suite verification run with environmental-failure
classification
**Upstream artifacts:** `artifacts/impl-plan.md` (`TASK-001`–`TASK-009`),
`artifacts/design-review.md` (`DR-001`–`DR-012`)
**Run date:** 2026-11-21
**Command:** `mvn test -Dheadless=true`
**Environment:** Local workstation, Chrome 158 (headless), the live public
`opensource-demo.orangehrmlive.com` target.

## Summary

| Metric | Result |
| --- | --- |
| Surefire tests run | 18 (`TestRunner` aggregating 12 Cucumber scenarios + `ConfigReaderTest`'s 6 unit tests) |
| Cucumber scenarios | 12 total — 11 passed, 1 failed |
| Cucumber steps | 43 total — 42 passed, 1 failed, **0 undefined, 0 skipped** |
| `ConfigReaderTest` unit tests | 6 total — 6 passed |
| Scenario double-count check (`REC-002`) | Confirmed none — 12 scenarios run exactly once each after the `TASK-002` `<includes>` change |

## Failing scenario

`@TS_LOG_009` — *Loading indicator is shown while authentication is in
progress* (`NFR-001`) failed with:

```
java.lang.AssertionError: Expected a loading indicator to be displayed while authentication was in progress
	at com.orangehrm.stepdefinitions.LoginSteps.a_loading_indicator_should_be_displayed_during_authentication(LoginSteps.java:178)
```

**Classification: pre-existing, accepted proxy risk — not a regression from
`TASK-001`–`TASK-006`.** This scenario and its step definitions were not
touched by any task in this plan. `artifacts/architecture.md` §Risks already
records this exact failure mode ("Transient `NFR-001` loading indicator is
missed on a fast response, causing intermittent `@TS_LOG_009` failures") as a
known, accepted limitation of the DOM-presence proxy (`DR-004`): the demo
site's auth response is fast enough in this environment that the transient
indicator is not observed within the poll window. It is **not** the `DR-011`
environmental pattern (login page failing to render, blank/error screenshot)
and **not** `TASK-001`'s configuration-guard failure (no `IllegalStateException`
is involved) — it is a timing-sensitive assertion on an already-documented
proxy. Re-running `@TS_LOG_009` alone reproduced the same failure
consistently in this environment, so it is reported as a known limitation
rather than transient flakiness. No action is taken per `code-quality.md`
scope discipline (fixing it is not a `TASK-001`–`TASK-009` item).

## Classification conventions applied

1. **`DR-011` — environmental failures.** No scenario failed because the
   login page failed to render (no blank/error screenshot from a page-load
   timeout was produced in this run). Had one occurred, it would be reported
   as environmental, not as an `FR`/`NFR` defect.
2. **Deliberate fail-closed vs. outage.** Manually triggering
   `mvn test -Dbase.url=http://opensource-demo.orangehrmlive.com/web/index.php/auth/login -Dcucumber.filter.tags="@TS_LOG_001"`
   produced:
   ```
   java.lang.IllegalStateException: base.url must use the https scheme, but its scheme is http;
   set allow.insecure.base.url=true to opt out for a local, non-TLS sandbox only
   	at com.orangehrm.config.ConfigReader.getBaseUrl(ConfigReader.java:111)
   	at com.orangehrm.stepdefinitions.LoginSteps.the_user_is_on_the_orange_hrm_login_page(LoginSteps.java:42)
   ```
   This is classified as a **configuration failure** (the `TASK-001` guard
   working as designed) and is clearly distinguishable from a Selenium
   timeout: it is an `IllegalStateException` naming the offending scheme,
   raised in the `Given` step before any browser navigation, never a
   `TimeoutException` with a blank screenshot.
3. **`DR-004` proxy disclosure.** `NFR-001` (`@TS_LOG_009`) is reported as
   covered via DOM-**presence** polling (`BasePage.isPresent`), an accepted
   proxy for `AC-007`'s "visible" — not as full visibility fidelity. This
   run's failure is itself evidence of that proxy's known limitation, recorded
   above rather than hidden.

## Requirement-to-test reconciliation

| ID | Scenario(s) / test(s) | Result |
| --- | --- | --- |
| `FR-001` | `@TS_LOG_008` (missing-controls collection + both placeholder values) | Passed |
| `FR-002` | `@TS_LOG_007` | Passed |
| `FR-003` | `@TS_LOG_001`, `@TS_LOG_012` | Passed |
| `FR-004` | `@TS_LOG_002`, `@TS_LOG_003`, `@TS_LOG_011` | Passed |
| `FR-005` | `@TS_LOG_004`, `@TS_LOG_005`, `@TS_LOG_006` | Passed |
| `FR-006` | `@TS_LOG_010` | Passed |
| `NFR-001` | `@TS_LOG_009` | **Failed** — pre-existing proxy limitation, see above; not a `TASK-001`–`TASK-009` regression |
| `NFR-002` | `@TS_LOG_001`, `@TS_LOG_002`, `@TS_LOG_003`, `@TS_LOG_007`, `@TS_LOG_008`, `@TS_LOG_011`, `ConfigReaderTest` (6 cases) | Passed — HTTPS clause now both preventively guarded (`TASK-001`) and executed (`TASK-004`); non-enumeration and no-plaintext-on-screen verified; no-logging clause remains code-review-only (`TASK-007`, honestly reported, not claimed as automated) |

Every `FR`/`NFR` maps to at least one passing scenario or unit test, except
the pre-existing `NFR-001` limitation, which is reported honestly rather than
hidden (`tests.md`: "report coverage honestly").

## `TASK-007` credential-logging review check (performed)

Since `hook_scan_secrets` is unimplemented and silently no-ops, this review
is the sole active control for `AD-011`/`NFR-002`'s no-logging clause
(`DR-012`):

- Reviewed the full diff for `TASK-001`–`TASK-006` (`ConfigReader.java`,
  `config.properties`, `pom.xml`, `LoginPage.java`, `LoginSteps.java`,
  `login.feature`, `ConfigReaderTest.java`). No line logs, prints, or embeds a
  credential, a field value, or a resolved password; no exception message
  interpolates one — `ConfigReader.getBaseUrl()`'s new message names only the
  scheme and the opt-out key.
- Confirmed `ConfigReader.getValidPassword()` and `isDemoHost()` are
  byte-for-byte unchanged in the diff (`git diff` produced no hunk touching
  either method).
- Scanned `target/surefire-reports/` and `target/cucumber-reports/` (JSON and
  HTML) for the committed demo password value (`admin123`): zero matches.

**Result: Pass.** No credential-logging defect found.

## Conclusion

`TASK-001`–`TASK-007` are verified. `NFR-001`'s `@TS_LOG_009` failure is a
pre-existing, documented proxy limitation unrelated to this plan's scope and
is not blocking. `TASK-009`'s coverage-claim flip for `FR-001` and `NFR-002`
is backed by the passing scenarios and unit tests listed above.
