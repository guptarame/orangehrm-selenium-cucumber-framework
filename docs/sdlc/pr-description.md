# Pull Request: Fix login loading-indicator race and credential hardcoding

## Summary
This cycle of the Agentic SDLC pipeline reviewed the OrangeHRM login automation framework against PRD **US-AUTH-001** (Confluence: `MFS` space, page 13271042) and closed the two **Should-fix** conditions raised by design review that had concrete code-level evidence, plus two smaller hardening/cleanup items.

**PRD:** https://epam-team-en32bjvm.atlassian.net/wiki/spaces/MFS/pages/13271042/Login+Functionality+for+OrangeHRM+Portal
**Traceability:** Full SDLC artifacts in `docs/sdlc/` (requirements → architecture → design-review → impl-plan → verification)

---

## Changes Made

### Bug fixes (design-review Should-fix)
- **Loading-indicator race (`@TS_LOG_009`, FR-4):** `LoginPage.isLoadingIndicatorDisplayed()` used to be polled from a separate `Then` step *after* the login click had already returned in the `When` step, so on a fast auth response the spinner could appear and vanish before the assertion started polling — a genuine false-failure race, not just "timing-sensitive." Added `LoginPage.submitAndCaptureLoadingIndicator(username, password)`, which enters credentials, clicks login, and polls for the indicator synchronously within the same call. `LoginSteps` now stores that result and the `Then` step asserts the stored value instead of re-polling. No `Thread.sleep` introduced. Verified with 5 consecutive re-runs of `@TS_LOG_009` — 0 flaky failures.
- **Credential hardcoding (`@TS_LOG_002/005/006/007`):** these scenarios hardcoded the literal valid username/password directly in Gherkin step text, duplicating the values already centralized in `config.properties`. Added ConfigReader-backed step definitions (mirroring the existing pattern for other scenarios) and reworded the four scenarios to route through them. Genuinely-invalid literals (e.g. wrong password, empty strings) were left as-is, since those are intentionally not real credentials.

### Hardening / cleanup
- `BasePage.isDisplayed()` now catches `TimeoutException` specifically instead of generic `Exception`, so real driver/session errors are no longer silently masked as "element not found."
- Removed the unused `pom.xml` `<env>qa</env>` property and its Surefire `systemPropertyVariables` passthrough — no code ever read it.
- Added a comment above `valid.username`/`valid.password` in `config.properties` documenting that these are OrangeHRM's public demo values and must be overridden via `-D` system properties / CI secrets before pointing this framework at any non-public environment.
- `LoginSteps` now fetches `WebDriver` once via `DriverManager.getDriver()` and shares it across the `LoginPage`/`DashboardPage`/`ResetPasswordPage` constructors instead of re-fetching per call.

### Repo hygiene (carried forward, required for `main` to build)
- `pom.xml` on `main` was pointing at an unrelated, incompatible project scaffold (wrong `groupId`/`artifactId`, JUnit Jupiter, Selenium 4.25.0, no Cucumber at all — `main` never received the fix that was already pushed to `feature/login-coverage-improvements`). This PR carries that corrected `pom.xml` (Java 11, Selenium 4.27.0, Cucumber 7.20.1, JUnit 4.13.2, WebDriverManager 5.9.2, SLF4J 2.0.16) so `main` actually builds this framework.

### Deferred
- CI Firefox-leg / OWASP Dependency-Check plugin work (impl-plan T7): infra work, not framework code — out of scope for this pass, left as a follow-up.

### SDLC Artifacts
- `docs/sdlc/requirements.md` — extracted from Confluence PRD (US-AUTH-001)
- `docs/sdlc/architecture.md` — framework architecture, gaps explicitly marked
- `docs/sdlc/design-review.md` — verdict: APPROVED WITH CONDITIONS (0 must-fix, 4 should-fix, 5 nice-to-have)
- `docs/sdlc/impl-plan.md` — 8-task breakdown, ~4h estimate
- `docs/sdlc/verification-report.md` — verdict: PASS WITH LIMITATIONS

---

## Test Evidence

Independently re-run by the verification stage (not reused from implementation):

| Command | Result |
|---|---|
| `mvn clean compile test-compile` | BUILD SUCCESS |
| `mvn clean test` (Chrome, default) | 10/10 scenarios, 32/32 steps passed |
| `mvn clean test -Dbrowser=firefox` | 10/10 scenarios, 32/32 steps passed |
| `@TS_LOG_009` × 5 consecutive runs | 5/5 passed, 0 flaky failures |
| `mvn clean compile test-compile` (this branch, rebased onto corrected `main`) | BUILD SUCCESS |

No leaked browser/driver processes observed after either run. No credential values found in console output or Cucumber JSON report.

**Not verified** (explicitly, not assumed passing): Edge browser, network throttling, cross-browser matrix beyond Chrome/Firefox, load/performance testing.

---

## Requirements Traceability

| Requirement | Status |
|---|---|
| FR-1..FR-8 (login UI, masking, auth, loading indicator, invalid creds, required-field validation, forgot-password nav) | Covered — FR-4's flakiness fixed this cycle |
| NFR-1..NFR-5 (compatibility, availability, performance, security beyond masking, accessibility) | Not covered — explicitly out of scope per `requirements.md`, no dedicated test/check exists |

Full detail in `docs/sdlc/architecture.md` §7 and `docs/sdlc/verification-report.md`.

---

## Known Limitations
- No Edge/Safari/mobile browser coverage.
- No parallel execution (thread-local driver storage makes it *safe*, but nothing configures it).
- NFR-1..NFR-5 have no dedicated automated coverage (unchanged from requirements/design review — not part of this cycle's scope).
- Credentials remain a plaintext, checked-in demo value — acceptable for the public OrangeHRM demo target, flagged for externalization if this framework ever points at a non-public environment.

---

## Reviewer Checklist
- [ ] `@TS_LOG_009` fix genuinely closes the step-boundary race (see `LoginPage.submitAndCaptureLoadingIndicator`)
- [ ] No literal credentials remain in `@TS_LOG_002/005/006/007` step text
- [ ] `BasePage.isDisplayed()` exception narrowing doesn't change intended negative-path behavior
- [ ] `pom.xml` correction is acceptable as part of this PR (see "Repo hygiene" above)
- [ ] SDLC artifacts in `docs/sdlc/` are clear and traceable

---

## Related
- **PRD:** Confluence page 13271042 (`MFS` space) — linked in `docs/sdlc/requirements.md`
- **Pipeline:** `sdlc_orchestrator` → requirements → architecture → design-review → planning → implementation → verification → this PR


🤖 Generated with [Claude Code](https://claude.com/claude-code)
