# Pull Request: [Feature] Resilient teardown, wait fixes and config hardening for the login automation framework

**Date:** 2026-11-11
**Branch:** `feature/login-functionality-full-rerun` -> `main`
**PR status:** PR #4 is already open for this branch (earlier commits up to `f5b3e17`, which is what `origin` currently has). The 10 newer commits (`9612d23`..`a07491f`, plus this description commit) update PR #4 once pushed; no new PR is needed.

## Summary
Third SDLC run on the OrangeHRM login framework (PRD US-AUTH-001, Selenium + Cucumber + JUnit/TestNG runner, page objects). The framework already exists; this run hardens it. It makes driver teardown resilient, removes the implicit-wait / explicit-wait interaction that slowed negative checks, tightens the loading-indicator wait, makes config precedence predictable, adds a credential guard for non-demo hosts, and adds two scenarios (TS_LOG_011 both-invalid, TS_LOG_012 Enter-key login), taking `login.feature` to 12 scenarios.

Verification result: **PASS with live-run caveat.** Compile, Cucumber dry-run and non-browser JVM checks only. No browser was available, so nothing was run against the live OrangeHRM demo.

**Not re-read this run:** the Confluence PRD. `docs/sdlc/requirements.md` was re-derived from the prior extract, not from a fresh read of the page.

## Changes Made
Code delta for this run's commits (net of line-ending-only noise): 10 files, about 136 insertions / 27 deletions. Whole branch vs `main` (including PR #4 content) is 155 insertions / 27 deletions across the same 10 files.

- **Driver lifecycle (T-01, T-04; a0caf19)** `DriverManager`: `quitDriver()` clears the ThreadLocal in a `finally`, logs and swallows a failing `quit()`; init failure after browser creation quits the browser and rethrows; new `peekDriver()` returns the current driver without creating one. `Hooks.tearDown` uses `peekDriver()`, is null-safe, keeps screenshot errors from masking the scenario failure, and always calls `quitDriver()` in `finally`.
- **Wait fixes (T-02, T-03; a0caf19, ce5b4e5)** Implicit wait removed (key and getter deleted; no non-zero implicit wait remains). `BasePage.isPresent` polls at 50 ms for 3 s and returns false instead of throwing. `LoginPage` loading-indicator locator scoped to the form / submit button. `DashboardPage` URL check tightened to `dashboard/index`.
- **Config hardening (T-05, T-06; 4a2ac57)** `pom.xml` no longer defaults or passes through `browser`; `config.properties` supplies `chrome`. `ConfigReader` precedence is `-D` > environment variable > file; blank values are ignored. A non-demo `base.url` without a supplied password throws `IllegalStateException` (message never contains the password). Demo host is unaffected.
- **Scenarios (T-03, T-07; ce5b4e5)** TS_LOG_011 and TS_LOG_012 added; Enter-key step implemented through the page object (no locators in step definitions); TS_LOG_009 tagged `@timing` as best-effort. `-Dcucumber.filter.tags="not @timing"` gives 11 scenarios.
- **Docs (T-08; 0e7b76b)** Architecture addendum A.1-A.6 (including `-Dbrowser` and tag-filter usage, headless note).
- **SDLC artifacts** `requirements.md`, `architecture.md`, `design-review.md`, `impl-plan.md`, `verification-report.md` regenerated for run 3.

Earlier commits already in PR #4 (guarded teardown screenshot capture, documented demo-credential exception, code-review draft) remain part of the branch.

Not part of this PR: uncommitted working-tree edits to `.github/agents/*`, `.gitignore`, `.idea/*`, `README.md`, `ResetPasswordPage.java`, and untracked `.claude/`, `.mcp.json`. Most are line-ending noise; they are not committed and should not be reviewed here.

## Test Evidence
Source: `docs/sdlc/verification-report.md` (a07491f), verdict PASS with live-run caveat.

| Check | Result |
|---|---|
| `mvn -o -q -B test-compile -DskipTests` | Success |
| Cucumber `--dry-run` (all) | 12 scenarios, 39 steps, all matched |
| Cucumber `--dry-run --tags "not @timing"` | 11 scenarios, 36 steps matched |
| Ad hoc JVM checks (stubbed WebDriver, ConfigReader precedence, credential guard) | 23 checks OK; script not committed |
| grep for `Thread.sleep` / implicit-wait use; locators in step definitions | None found |
| Live browser scenarios (Chrome, headless, Firefox/Edge, slow network) | **Not run** |
| T-09 browser matrix and T-10 full `mvn clean test` | **Not executed** |

No claim is made that any scenario passes against the live site. Pass rate and timings are not measured. No defects were found in the changes.

## Known Limitations
- No live run: T-09 and T-10 are open; locator scoping (T-03), the tightened dashboard URL, new TS_LOG_011/012, failure screenshots, orphan-process behaviour and implicit-wait timing gains are unverified.
- TS_LOG_009 (loading indicator) is best-effort and may be timing-dependent.
- Cross-browser (Firefox/Edge) not verified; no parallel execution; no mobile coverage; Remember Me is not in the PRD.
- Host `BROWSER` / `HEADLESS` environment variables now override the file (by design; note for CI).
- `impl-plan.md` T-03 says 9 scenarios for the `@timing` exclusion; the correct count is 11 (doc-only inconsistency).
- Demo credentials remain in `config.properties` as a documented exception, guarded for non-demo hosts.

## Reviewer Checklist
- [ ] Teardown always quits the driver and never masks the original failure
- [ ] No implicit wait remains; negative checks use the explicit poll
- [ ] Config precedence and credential guard behave as described
- [ ] Step definitions contain no locators
- [ ] Verification caveat understood: run `mvn clean test` (headed and `-Dheadless=true`), `-Dcucumber.filter.tags="not @timing"` and `-Dbrowser=edge` on a machine with Chrome before merge
- [ ] Verification report and addendum are complete
