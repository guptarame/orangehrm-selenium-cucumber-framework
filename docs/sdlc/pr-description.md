# Pull Request: Guard teardown screenshot capture and document demo-credential exception

## Summary
This is a re-validation cycle of the already-implemented and merged OrangeHRM login automation framework (PRD **US-AUTH-001**, Confluence page 13271042; original build merged via PRs #1-#3). This cycle re-checked requirements, architecture, and design against the current codebase and confirmed FR-1..FR-8 remain fully covered by the existing 10 tagged scenarios in `login.feature`. Design review (this cycle) came back **APPROVED WITH CONDITIONS** — 0 Must-fix, 2 Should-fix, 5 Nice-to-have — and this PR closes both Should-fix items. It is a small, targeted hardening/documentation PR, not new feature work: no page objects, drivers, or scenarios were added or changed.

**PRD:** https://epam-team-en32bjvm.atlassian.net/wiki/spaces/MFS/pages/13271042/Login+Functionality+for+OrangeHRM+Portal
**Traceability:** Full SDLC artifacts in `docs/sdlc/` (requirements → architecture → design-review → impl-plan → verification)

---

## Changes Made

### T1 — Guarded teardown screenshot capture (`Hooks.java`)
`Hooks.tearDown`'s failure-screenshot capture (`((TakesScreenshot) driver).getScreenshotAs(...)`) was previously unguarded: if the driver session was already crashed/invalid at teardown time, the call could throw and propagate out of `@After` *before* `DriverManager.quitDriver()` ran — leaking the session and potentially obscuring the original scenario failure in the report.

Fixed by:
- Wrapping the screenshot-capture call in its own try/catch, catching `WebDriverException` specifically, and logging a warning (scenario name + exception) instead of propagating.
- Moving `DriverManager.quitDriver()` into an outer `finally` block, so it now executes unconditionally on every path through `tearDown` — scenario passed, scenario failed with a successful screenshot, or scenario failed with a screenshot-capture exception.

### T2 — Documented demo-credential exception (`config.properties`)
`config.properties` ships plaintext demo credentials (`valid.username=Admin` / `valid.password=admin123`) — OrangeHRM's own public demo values, not a real secret, but previously undocumented as an intentional decision. Added an explicit comment block (in addition to the pre-existing override-mechanism comment) stating this is a deliberate, scoped, time-boxed exception: accepted only while the target remains OrangeHRM's public `opensource-demo` instance, and must be revisited (values removed, supplied only via existing `-D` overrides / CI secret injection) before this framework is ever pointed at a non-public or production environment. No credential values were changed or removed — `ConfigReader.get()` already gives a non-blank `-D` system property priority over the file, so no code change is needed to externalize later.

### SDLC artifacts (this cycle)
- `docs/sdlc/requirements.md` — re-extracted/re-validated against the current Confluence PRD
- `docs/sdlc/architecture.md` — re-validated against current codebase
- `docs/sdlc/design-review.md` — verdict: **APPROVED WITH CONDITIONS** (0 Must-fix, 2 Should-fix, 5 Nice-to-have)
- `docs/sdlc/impl-plan.md` — 3-task plan (T1, T2, T3-verification)
- `docs/sdlc/verification-report.md` — verdict: **PASS**

---

## Test Evidence

Stage 6 verification ran `mvn clean test` against the live OrangeHRM public demo site (`https://opensource-demo.orangehrmlive.com/web/index.php/auth/login`) using a real, headed Chrome browser (WebDriverManager-resolved):

| Command | Result |
|---|---|
| `mvn clean test` (default Chrome, headed, live network) | **BUILD SUCCESS** — 10/10 scenarios passed, 32/32 steps passed, 0 failures, 0 errors, 0 skipped (~53s total Maven wall time) |

- All 8 functional requirements (FR-1..FR-8) have at least one passing tagged scenario (`@TS_LOG_001`–`@TS_LOG_010`) — no regression against `architecture.md` §7's traceability table.
- T1 was statically confirmed correct by code review (try/catch around screenshot capture, `quitDriver()` in `finally`); since all 10 scenarios passed live, the `scenario.isFailed()` branch itself was not exercised in this run — a limitation of an all-passing run, not a defect (see Known Limitations).
- T2 was confirmed present, explicit, and correctly scoped in `config.properties` (lines 20-35).
- No credential value (`admin123`) was found anywhere in console output or in any generated report (`cucumber-html-report.html`, `cucumber.json`, `cucumber-junit.xml`); `Admin` appears only as scenario-title text, never as a logged credential value.

Full detail in `docs/sdlc/verification-report.md`.

---

## Requirements Traceability

| Requirement | Status |
|---|---|
| FR-1..FR-8 (login UI controls, password masking, valid login, loading indicator, invalid credentials, required-field validation, forgot-password nav) | Covered — all 10 tagged scenarios passed this cycle, no regression |
| NFR-1 (browser compatibility), NFR-2 (JS required) | Partially covered (real Chrome/Firefox/Edge engines exist in `DriverManager`; only Chrome exercised this run) |
| NFR-3..NFR-6 (availability, performance, security beyond masking, accessibility) | Not covered — explicitly out of scope per `requirements.md`, unchanged this cycle |

---

## Known Limitations
(Carried forward from `docs/sdlc/design-review.md` §5 Nice-to-have / §4 Risk Assessment — genuinely still open, not affected by this PR)

- No `System.getenv(...)` lookup in `ConfigReader.get()` — only `-D` system properties are read, so a CI system relying solely on OS env vars (not `-D` flags) has no override path today.
- No automated dependency-vulnerability scan (e.g. OWASP Dependency-Check) configured in the Maven build.
- No CI matrix or scheduled job exercising `-Dbrowser=firefox` / `-Dbrowser=edge` — those `DriverManager` code paths exist and are structurally sound but are not verified in CI, only Chrome is.
- FR-4's loading-indicator check still uses a short, fixed 3-second poll window (the previous cross-step-boundary race is already fixed in the current codebase, prior to this cycle) — no flakiness observed this run; would only warrant a secondary signal (e.g. submit-button `disabled` state) if flakiness is observed in real CI.
- No lightweight retry for a single transient network/server hiccup (NFR-3 resilience) — not required by `requirements.md`.
- The failure-screenshot capture path (including this PR's new catch block) was not exercised live this cycle, since all 10 scenarios passed; correctness was established via static code review only.

---

## Reviewer Checklist
- [ ] `Hooks.tearDown` — confirm no code path can exit without `DriverManager.quitDriver()` having run (try/catch + finally structure)
- [ ] `Hooks.tearDown` — confirm the new catch block only swallows `WebDriverException` from screenshot capture, and still logs a warning rather than silently suppressing it
- [ ] `config.properties` — confirm no real/non-demo credential value was introduced, and `valid.username`/`valid.password` values are unchanged
- [ ] `config.properties` — confirm the new comment clearly scopes the exception to the public demo target and names the required action before non-demo reuse
- [ ] Test evidence (10/10 scenarios, 32/32 steps, `mvn clean test`) matches `docs/sdlc/verification-report.md`
- [ ] SDLC artifacts in `docs/sdlc/` are clear, internally consistent, and traceable to this PR's actual diff

---

## Related
- **PRD:** Confluence page 13271042 (`MFS` space) — linked in `docs/sdlc/requirements.md`
- **Prior PRs:** #1, #2, #3 (original framework implementation and merge)
- **Pipeline:** requirements → architecture → design-review → planning → implementation → verification → this PR

🤖 Generated with [Claude Code](https://claude.com/claude-code)
