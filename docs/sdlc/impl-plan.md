# Implementation Plan

**Source Documents:** `docs/sdlc/architecture.md`, `docs/sdlc/design-review.md` (APPROVED WITH CONDITIONS)
**Date:** 2026-09-27
**Task Count:** 8
**Complexity Summary:** 5 Low, 3 Medium
**Total Effort Estimate:** ~4.5 hours

---

## Task Breakdown

| ID | Task | Priority | Complexity | Effort | Dependencies |
|----|------|----------|------------|--------|---------------|
| T1 | Route the `@TS_LOG_001` valid-login scenario's credentials through `ConfigReader.getValidUsername()`/`getValidPassword()` instead of the literal in `login.feature` | Should-fix (S1) | Low | 20 min | None |
| T2 | Add basic SLF4J lifecycle logging (scenario start/end, driver init/quit, failure-screenshot capture) to `Hooks.java` and `DriverManager.java` | Should-fix (M1) | Low | 30 min | None |
| T3 | Add an "all required login controls visible" check (logo, username field, password field, login button, forgot-password link) to `LoginPage`; add scenario `@TS_LOG_008` covering FR-1 fully | Must-fix (FR-1 gap) | Low | 30 min | None |
| T4 | Add a loading-indicator locator/method to `LoginPage`; add scenario `@TS_LOG_009` asserting the indicator is shown during authentication (FR-4) | Must-fix (FR-4 gap) | Medium — timing-sensitive assertion against a transient UI state | 60 min | None |
| T5 | Add a forgot-password link locator/click method to `LoginPage`; create a minimal `ResetPasswordPage` page object; add scenario `@TS_LOG_010` asserting navigation to the reset-request page (FR-7) | Must-fix (FR-7 gap) | Medium — new page object plus navigation assertion | 60 min | T3 (shares `LoginPage` edits; sequence to avoid merge conflicts) |
| T6 | Reconcile `login.feature` tag numbering/ordering after T3–T5 land; confirm Background and existing `@TS_LOG_001`–`007` scenarios are unaffected | Should-fix | Low | 15 min | T3, T4, T5 |
| T7 | Attempt an Edge browser verification run (`mvn test -Dbrowser=edge`) in addition to the existing Chrome/Firefox scope; document pass/fail/skip honestly if Edge is unavailable in this environment (C1) | Should-fix | Low | 15 min | T1–T6 (needs final scenario set) |
| T8 | Full regression run of all scenarios (`@TS_LOG_001`–`010`) on Chrome to confirm no existing behavior broke | Verification (existing features, not new implementation) | Low | 20 min | T1–T6 |

---

## Dependency Table

```
T1 ─┐
T2 ─┤
T3 ─┼─▶ T6 ─▶ T7
T4 ─┤        └─▶ T8
T5 ─┘ (depends on T3)
```

T1, T2, T3, T4 have no dependencies and can be implemented in any order. T5 should follow T3 since both edit `LoginPage`. T6 reconciles the feature file only after T3–T5 exist. T7 and T8 both need the final scenario set from T6.

---

## Phased Execution Order

1. **Foundation (config/logging hardening):** T1, T2 — low-risk, independent of new scenarios.
2. **Framework extension (new page-object capability):** T3, T4, T5 — new locators/methods on `LoginPage`, new `ResetPasswordPage`.
3. **Test integration:** T6 — reconcile `login.feature` tags and Background compatibility.
4. **Validation:** T7 (Edge attempt), T8 (full regression) — matches the architecture's/verification-agent's existing Chrome/Firefox scope plus the C1 follow-up.

---

## Design-Review Condition Coverage

| Design-Review Item | Plan Task |
|---|---|
| S1 — credential/config routing | T1 |
| M1 — unused SLF4J dependency | T2 |
| C1 — Edge verification gap | T7 |
| FR-1 partial coverage | T3 |
| FR-4 missing coverage | T4 |
| FR-7 missing coverage | T5 |
| R1 (implicit+explicit wait mixing) | Not planned — Nice to Have, no observed flakiness; deferred |
| P1 (no parallel execution) | Not planned — Nice to Have, suite size does not justify it yet |

---

## Risk Mitigation Mapping

| Risk (from design review) | Mitigation Task |
|---|---|
| FR-4/FR-7 shipped without coverage | T4, T5 |
| Credential/config drift (S1) | T1 |
| Wait-mixing flakiness (R1) | Deferred — monitor via T8 regression runs; no task needed unless flakiness appears |
| Edge unverified (C1) | T7 |

---

## Success Criteria

- All 8 tasks completed with their stated deliverables.
- `mvn clean compile test-compile` succeeds after each phase.
- `@TS_LOG_001`–`@TS_LOG_010` all pass on Chrome; Firefox pass/fail documented; Edge result documented honestly (pass, fail, or skipped-with-reason).
- No credential value appears in any SDLC document or commit message.
- No must-fix design-review condition remains unaddressed; all should-fix items (S1, M1, C1) are implemented or explicitly deferred with reason.

---

## Traceability

- **Architecture:** `docs/sdlc/architecture.md`
- **Design Review:** `docs/sdlc/design-review.md`
- **Requirements:** `docs/sdlc/requirements.md`
- **Next Stage:** Implementation (Stage 5) — no approval gate; proceeds directly.
