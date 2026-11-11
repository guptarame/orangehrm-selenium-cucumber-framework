# Implementation Plan

**Feature:** OrangeHRM Login Automation Framework
**Source Documents:** `docs/sdlc/architecture.md` (this cycle, 2026-09-28), `docs/sdlc/design-review.md` (this cycle, 2026-09-28 — **Verdict: APPROVED WITH CONDITIONS**, 0 Must-fix, 2 Should-fix, 5 Nice-to-have), `docs/sdlc/requirements.md` (this cycle, 2026-09-28)
**Date:** 2026-09-28
**Agent:** planning-agent
**Task Count:** 3
**Complexity Summary:** 2 Low, 1 Low (verification)
**Total Effort Estimate:** ~1 hour (30 min + 15 min + 15 min)

> Scope note: this is a re-validation cycle, not greenfield work. `docs/sdlc/design-review.md` (this cycle) confirms **FR-1..FR-8 are already fully implemented and covered** by the 10 tagged scenarios in `login.feature` (`@TS_LOG_001`–`@TS_LOG_010`) — see its §2 Requirements Coverage table and `architecture.md` §7 traceability table, both independently verified against source this cycle. Four issues raised in the *prior* (2026-09-27) design-review cycle — the FR-4 step-boundary race, `BasePage.isDisplayed()` catching overly broad `Exception`, the dead `<env>qa</env>` Maven property, and per-Page-Object independent driver fetches — are **already resolved in the current codebase** (design-review.md §1, §7) and are **not** re-planned here. This plan's actual scope is narrow:
> 1. Implement the 2 Should-fix items from this cycle's design-review (Hooks.tearDown screenshot guard; documented credential exception).
> 2. Task a Stage 6 verification pass confirming FR-1..FR-8 coverage holds (no new feature code — verification only).
> 3. Log the 5 Nice-to-have items as backlog, explicitly out of scope this cycle.
>
> No new page objects, drivers, scenarios, or net-new feature work is planned. No source code is modified by this planning stage itself — code changes described below are deliverables for the *next* (implementation) stage to execute against this plan.

---

## 1. Task Breakdown

| ID | Description | Priority | Complexity | Effort Estimate | Dependencies | Deliverables | Acceptance Criteria |
|----|---|---|---|---|---|---|---|
| **T1** | Guard `Hooks.tearDown`'s failure-screenshot capture with try/catch so a crashed/invalid driver session at teardown cannot throw out of `@After`; log a warning on capture failure; `DriverManager.quitDriver()` must still run unconditionally in all cases (success, capture failure, or no failure) | Should-fix (design-review §5 Should-fix #1 / Risk R1, **Medium** severity) | Low | 20 min | None | Modified `src/test/java/com/orangehrm/hooks/Hooks.java`: `((TakesScreenshot) driver).getScreenshotAs(...)` call wrapped in try/catch inside `tearDown`; catch block logs a warning (scenario name + exception) via the existing SLF4J `LOGGER`; `DriverManager.quitDriver()` moved so it is reached regardless of whether the screenshot try block succeeds, throws, or is skipped (e.g. via `finally`, or by placing it after the guarded block unconditionally) | (1) Code review confirms no exception path inside `tearDown` can return/propagate without `DriverManager.quitDriver()` having executed. (2) A forced-throw unit/manual check (e.g. temporarily stub `getScreenshotAs` to throw, or crash the driver mid-scenario) shows the scenario still completes teardown and the driver's `ThreadLocal` is cleared, with a warning logged instead of an uncaught exception. (3) Existing passing behavior unchanged: `@TS_LOG_002`/`@TS_LOG_004` (or any other scenario forced to fail) still attach a screenshot on a healthy driver session exactly as before. |
| **T2** | Document the plaintext demo-credential exception in `config.properties` as an explicit, time-boxed, scoped condition (not a silent/permanent gap) — record that it is accepted only while the target remains OrangeHRM's public demo instance, and that `ConfigReader.get()`'s existing `-D` override already supports zero-code externalization | Should-fix (design-review §5 Should-fix #2 / Finding §3.1, Risk R2) | Low | 15 min | None | A short, explicit note added to either `README.md` or as an expanded comment block in `src/test/resources/config.properties` (in addition to the existing lines 20-27 comment) stating: (a) this is an accepted, time-boxed exception scoped to the public OrangeHRM demo target, (b) it must be revisited (credentials removed from the file, supplied only via `-Dvalid.username=...`/`-Dvalid.password=...` or CI secret injection) before this framework is ever pointed at a non-public/non-demo environment, (c) no code change is required to make that switch, since `ConfigReader.get()` already gives a non-blank `-D` value priority over the properties file | (1) The note is present, readable, and locatable (grep for "demo" or "exception" in `README.md`/`config.properties` returns the new text). (2) The note explicitly names the scoping condition ("public demo instance only") and the required action before non-demo reuse — not a vague disclaimer. (3) No real/non-demo credential value is introduced anywhere in the note. (4) `config.properties`'s existing `valid.username=Admin`/`valid.password=admin123` values are otherwise left unchanged (this task documents the exception; it does not remove the demo credentials, since the demo target is still in use). |
| **T3** | Verify FR-1..FR-8 coverage is intact and unregressed after T1/T2 — **verification only, no new feature scenarios**; this is Stage 6's execution, planned here as this cycle's only "coverage" task | Verification (maps to design-review.md §2 Requirements Coverage, already "Covered" for all 8 FRs pre-change) | Low | 15 min | T1, T2 | A recorded `mvn test` run (default Chrome, all 10 scenarios `@TS_LOG_001`–`@TS_LOG_010`) executed after T1/T2 land, with its console/HTML/JSON/JUnit-XML report retained under `target/cucumber-reports/`, confirming no regression against the design-review.md §2 coverage table | (1) `mvn test` exits 0 (per `pom.xml`'s `testFailureIgnore=false`). (2) All 10 scenarios pass, preserving FR-1..FR-8's "Covered" status from design-review.md §2 — this task adds no new scenario and changes no `login.feature` content. (3) `@TS_LOG_002`/any scenario exercising a failure path still produces an attached failure screenshot in the report (confirms T1's guard didn't silently suppress legitimate screenshot capture on a healthy driver). (4) No credential value appears in the report output. |

**Explicitly out of scope this cycle (backlog, from design-review.md §5 Nice-to-have — not tasked, not built now):**
| # | Nice-to-have (design-review.md §5) | Disposition |
|---|---|---|
| N1 | Add `System.getenv(key)` lookup in `ConfigReader.get()` for OS-env-var CI override support | Backlog — no CI env-var-only system currently in use; revisit if one is adopted |
| N2 | Add a dependency-vulnerability scan (OWASP Dependency-Check or equivalent) to the Maven build | Backlog — current dependency versions verified current in design-review.md §3.8; no known CVE flagged |
| N3 | Add a CI matrix leg or scheduled job exercising `-Dbrowser=firefox`/`-Dbrowser=edge` | Backlog — `DriverManager` code paths exist and are structurally sound; only the CI *verification* of non-default browsers is missing (Risk R4, Low severity) |
| N4 | Add a lightweight retry for a single transient network/server hiccup (NFR-3 resilience) | Backlog — not required by requirements.md; NFR-3 is explicitly out of scope per requirements.md |
| N5 | Strengthen `isLoadingIndicatorDisplayed()` with a secondary signal (e.g. submit-button `disabled` state) if FR-4 flakiness is observed in real CI | Backlog — conditional on observed flakiness (Risk R3, Low-Medium); no such flakiness reported yet since this cycle's step-boundary-race fix |

---

## 2. Dependency Table / Order

| Task | Depends On | Reason |
|---|---|---|
| T1 | None | Isolated change to `Hooks.java` only |
| T2 | None | Isolated change to `README.md`/`config.properties` comment only; no shared file with T1 |
| T3 | T1, T2 | Final regression/verification must run against the completed change set from both Should-fix tasks |

```
T1 ──┐
     ├──▶ T3
T2 ──┘
```

No cycles. T1 and T2 touch disjoint files (`Hooks.java` vs. `README.md`/`config.properties`) and can proceed in parallel; T3 is the single convergence point.

---

## 3. Phased Execution Order

1. **Phase 1 — Should-fix remediation (parallel-safe):** T1 (screenshot-capture guard in `Hooks.tearDown`) and T2 (documented credential exception) — independent files, no merge conflicts, can be implemented in either order or concurrently.
2. **Phase 2 — Verification (Stage 6):** T3 — full `mvn test` regression across all 10 `login.feature` scenarios, confirming FR-1..FR-8 remain "Covered" per design-review.md §2 and that neither Should-fix change introduced a regression.

No further phases are planned this cycle. Nice-to-have items N1–N5 remain backlog per §1 above and are not scheduled into any phase.

---

## 4. Design-Review Condition Coverage

| Design-Review Item | Type | Plan Task | Disposition |
|---|---|---|---|
| Should-fix #1 — unguarded screenshot capture in `Hooks.tearDown` (design-review.md §3.4, §5, Risk R1) | Should-fix | **T1** | Fixed — try/catch guard added; `DriverManager.quitDriver()` guaranteed to run |
| Should-fix #2 — plaintext demo credentials in `config.properties` with no tracked exception (design-review.md §3.1, §5, Risk R2) | Should-fix | **T2** | Documented/tracked as an explicit, time-boxed, scoped exception — per design-review.md §6, this is acceptable to close via documentation, not a code fix, while the target remains the public OrangeHRM demo instance |
| FR-1..FR-8 requirements coverage (design-review.md §2 — all "Covered") | Verification | **T3** | Re-verified, not re-implemented — no Must-fix or coverage gap exists against any FR |
| Nice-to-have #1–#5 (design-review.md §5) | Nice-to-have | *(none — backlog)* | Explicitly deferred; see §1 backlog table above |

Both Should-fix items from this cycle's `design-review.md` are accounted for above; none dropped silently. No Must-fix items exist this cycle (design-review.md §5: "0 critical").

---

## 5. Risk Mitigation Mapping

(Source: `design-review.md` §4 Risk Assessment table)

| Risk | Severity | Mitigation Task | Notes |
|---|---|---|---|
| R1: Unguarded screenshot capture throws on a crashed driver session, potentially leaking the session and masking the original failure | Medium | **T1** | Direct fix — try/catch + logged warning + guaranteed `quitDriver()` |
| R2: Plaintext demo credential pattern copy-pasted into a fork/reuse against a non-public environment | Medium (Low today, High if reused) | **T2** | Mitigated via explicit documentation of scope/precondition; does not eliminate the risk of future misuse but ensures it is not a *silent* gap, per design-review.md §6 |
| R3: FR-4's 3-second loading-indicator poll window remains inherently timing-sensitive on a slow CI runner | Low-Medium | Not planned (backlog N5) | No observed flakiness this cycle; monitor per design-review.md's own recommendation — revisit only if flakiness is observed |
| R4: No CI job exercises Firefox/Edge, so a latent bug in those `DriverManager` branches would go undetected | Low | Not planned (backlog N3) | Code paths exist and are structurally verified by design-review.md §3.6; CI verification gap only |
| R5: No automated dependency-vulnerability scanning configured | Low | Not planned (backlog N2) | Current dependency versions independently verified current (design-review.md §3.8) |
| R6: No health-check/availability precondition (NFR-3); a down app server surfaces only as a generic `TimeoutException` | Informational | Not planned | Explicitly out of scope per requirements.md (NFR-3 has no stated SLA) |

Only R1 and R2 (the two Medium-severity, Should-fix-linked risks) are actively mitigated this cycle, matching design-review.md's own approval conditions. R3–R6 are Low/Informational and correctly left as backlog or out-of-scope, consistent with design-review.md §6: "No condition requires re-architecting the framework... none of those are mandated by requirements.md."

---

## 6. Success Criteria and Traceability

**Success criteria this cycle:**
- `Hooks.tearDown` cannot throw an uncaught exception originating from screenshot capture; `DriverManager.quitDriver()` executes on every code path through `tearDown` (T1).
- The plaintext demo-credential exception in `config.properties` is documented as an explicit, scoped, time-boxed condition — no longer a silent/undocumented gap (T2).
- `mvn test` runs all 10 scenarios in `login.feature` (`@TS_LOG_001`–`@TS_LOG_010`) to completion with exit code 0 after T1/T2 land, with no regression to any FR-1..FR-8 scenario (T3).
- Both design-review.md Should-fix conditions are closed (T1 fixed in code, T2 closed via documentation per design-review.md §6's own allowance) — zero Should-fix items carried forward silently.
- No new page objects, drivers, scenarios, or `login.feature` content are introduced — this cycle's scope is strictly the 2 Should-fix items plus verification, per the planning-agent instruction for this cycle.

**Traceability:**
- **Requirements:** `docs/sdlc/requirements.md` (FR-1..FR-8, NFR-1..NFR-6, US-AUTH-001) — all FR-1..FR-8 already "Covered" per architecture.md §7 and design-review.md §2; unaffected by T1/T2; reconfirmed by T3.
- **Architecture:** `docs/sdlc/architecture.md` (this cycle, 2026-09-28) — §3.4/§5.7 describe the current (pre-fix) `Hooks.tearDown` screenshot behavior that T1 hardens; §5.5 describes the current (pre-documentation) credential-handling behavior that T2 documents.
- **Design Review:** `docs/sdlc/design-review.md` (this cycle, 2026-09-28 — APPROVED WITH CONDITIONS) — §5 Should-fix #1 → T1; §5 Should-fix #2 → T2; §2 Requirements Coverage table → T3; §5 Nice-to-have #1-5 → backlog (§1 of this document).
- **Code paths this plan touches:** `src/test/java/com/orangehrm/hooks/Hooks.java` (T1); `README.md` and/or `src/test/resources/config.properties` (T2, comment/doc only — no credential value changes). No other source file is in scope this cycle.
- **Next Stage:** Implementation of T1/T2 against this plan, then Stage 6 verification (T3) per this repo's `docs/sdlc/verification-report.md` conventions.
