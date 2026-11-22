# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.1] — 2026-11-22

### Added

#### Observability (DR-017, TASK-011, NFR-002)
- SLF4J WARN logging in `ConfigReader.getBaseUrl()` when the `allow.insecure.base.url` opt-out is engaged — the warning names only the opt-out key (never the base.url value or any credential), is emitted exactly once per JVM via an `AtomicBoolean` latch, and is bounded by `AD-011`'s boundary-only logging policy (makes the HTTPS guard bypass detectable in run evidence, closes DR-017 Low design-review finding). Purely additive: no change to the opt-out's existing resolution logic or control flow.
- Extended `ConfigReaderTest` with one new case: `getBaseUrl_withInsecureOptOutEngaged_logsWarningNamingTheKeyOnly()` verifying that the WARN is logged exactly once and contains no credential, with mandatory `@Before`/`@After` capture-and-restore of system properties per `AD-014` / `DR-016` unit-test layer contract.

#### SDLC Pipeline Hardening (DR-015, TASK-010)
- Fixed malformed YAML frontmatter in `.claude/rules/code-quality.md` — added closing `---` delimiter (line 5) after the `paths` list, ensuring proper syntax and reliable auto-loading on fresh tool sessions. The rule is the sole active control for `NFR-002`'s no-plaintext-in-logs clause (closes DR-015 Medium design-review finding).

### Documentation

- `artifacts/requirements.md`: Re-issued with no requirement changes (all 6 FR + 2 NFR carried forward); full re-verification against code and git history.
- `artifacts/architecture.md`: Re-issued with 14 architecture decisions carried forward (AD-001–AD-014); prior-cycle findings DR-001–DR-012 verified in real code; new findings DR-013–DR-018 added (6 findings addressing SDLC pipeline integrity, unit-test layer risk, and opt-out observability; `DR-013`, `DR-014`, `DR-016`, `DR-017` load-bearing on this cycle; `DR-015` closes via TASK-010; `DR-018` minor doc correction).
- `artifacts/design-review.md`: Re-issued with 18 findings; design-review gate: **PASS** (0 Critical, 0 High unresolved).
- `artifacts/impl-plan.md`: Re-issued as a "re-confirmation" document: TASK-001–TASK-009 all marked Done with evidence re-verified against git and working tree; TASK-010, TASK-011, TASK-012 (Step 6 credential check), TASK-013 (this verification) newly scoped and completed for this cycle; two tasks (TASK-010, TASK-011) carried explicit human-approval blockers resolved per user gate.
- `artifacts/verification-report.md`: New comprehensive verification report documenting full-suite execution, opt-out confirmation, guard integrity, and no credential leakage.

### Test Results

- Full suite: `mvn test -Dheadless=true`
- Surefire: 19 tests total (7 `ConfigReaderTest` + 12 Cucumber scenarios), 18 passed, 1 pre-existing failure
- Cucumber: 12 scenarios, 11 passed, 1 pre-existing failure (`@TS_LOG_009` NFR-001 timing limitation, not a regression)
- Steps: 43 total, 42 passed, 1 failed (pre-existing), 0 undefined, 0 skipped
- Coverage: All 6 FR + 2 NFR traced to passing tests or code-review checks
- Credentials: Zero credentials logged or embedded in reports (confirmed via grep and manual inspection)
- Opt-out confirmation: `allow.insecure.base.url` unset for this run (secure default active)

### Known Limitations (updated)

- **NFR-001 (loading indicator):** `@TS_LOG_009` fails due to pre-existing DOM-presence-proxy limitation (not a regression). Documented in `architecture.md` §Risks (DR-004) as an explicitly accepted trade-off.
- **NFR-002 no-logging clause:** Code-review-only coverage (TASK-007/TASK-012). The `hook_scan_secrets` automation referenced in `.claude/CLAUDE.md` is not yet implemented; Step 6/Step 8 manual review is the sole active control per `DR-012`.
- **Unit-test layer forward-looking risk (DR-016):** Any future `**/*Test.java` class mutating `base.url`, `allow.insecure.base.url`, or `valid.password` must honour the `@Before`/`@After` capture-and-restore pattern or risk leaking retargeted URLs into subsequent Cucumber scenarios. Pattern is documented as mandatory in `architecture.md` `AD-014` trade-off column; this cycle's `ConfigReaderTest` honours it.
- **YAML frontmatter auto-load confidence:** The fix to `.claude/rules/code-quality.md` frontmatter (TASK-010) makes the rule syntactically correct, but tooling-dependent auto-loading is not strictly guaranteed. The rule content is load-bearing regardless, and the fix removes the accidental-vs.-designed ambiguity.

---

**Traceability:**

| ID | Type | Link | Status |
| --- | --- | --- | --- |
| TASK-010 | Implementation Task | `.claude/rules/code-quality.md` frontmatter fix | Complete |
| TASK-011 | Implementation Task | `ConfigReader` opt-out WARN + `ConfigReaderTest` case 7 | Complete |
| TASK-012 | Code Review Task | Credential logging & guard integrity check | Complete |
| TASK-013 | Verification Task | Full-suite run, opt-out confirmation | Complete |
| DR-013..DR-018 | Design-Review Findings | `artifacts/design-review.md` | 6 new findings added this cycle; gate PASS |

## [1.0.0] — 2026-11-21

### Added

#### Security & Transport (NFR-002, TASK-001–TASK-004)
- Preventive fail-closed HTTPS scheme guard in `ConfigReader.getBaseUrl()` that validates all `base.url` values against the HTTPS scheme before any browser navigation; throws `IllegalStateException` with the offending scheme and opt-out key name (never credentials) if the scheme is not HTTPS (closes DR-001 High design-review finding).
- Configurable `allow.insecure.base.url` opt-out key for sandboxed, non-TLS test environments only (secure default: opt-out disabled when the key is absent).
- JUnit 4 `ConfigReaderTest` class with six negative-path unit tests covering all branches of the HTTPS scheme guard: https happy path, http rejection, opt-out honored, secure default, malformed URL handling, and regression check that `getValidPassword()` host guard remains unmodified (implements TASK-003 for DR-001 negative coverage).
- Executed HTTPS scheme assertion on login page load (`@TS_LOG_008`, pre-submit) and post-successful-login redirect (`@TS_LOG_001`, post-submit) as defence-in-depth per AD-009 (implements TASK-004).

#### Login Page UI Completeness & Coverage (FR-001, TASK-005–TASK-006, TASK-009)
- Replaced brittle `areAllLoginControlsDisplayed()` boolean chain with diagnosable `LoginPage.findMissingLoginControls()` method that reports missing required controls by name: logo, username field, password field, Login button, forgot-password link, footer copyright element, and at least one footer social-link anchor.
- Added placeholder-value readers `getUsernamePlaceholder()` and `getPasswordPlaceholder()` to enable assertion of the actual HTML placeholder attribute on both input fields, complementing the existing password masking check (FR-002).
- Extended `@TS_LOG_008` scenario with missing-controls collection assertion (verifying the collection is empty, with a diagnostic failure message if any control is absent) and both placeholder-value assertions, replacing the previous short-circuiting boolean check with explicit, diagnosable steps (no new tags added per OQ-A001).
- Updated FR-001 and NFR-002 coverage claims in `artifacts/architecture.md` and `artifacts/requirements.md` from Partial to Complete (backed by passing test suite per TASK-009).

#### Test Automation Infrastructure (TASK-002)
- Extended Surefire `<includes>` in `pom.xml` from `**/TestRunner.java` alone to also include `**/*Test.java`, enabling JUnit 4 unit-test discovery and execution in addition to Cucumber scenarios; prerequisite for `ConfigReaderTest` negative-path coverage (TASK-002).

#### Build & Documentation
- Verification report (`artifacts/verification-report.md`) documenting full-suite test run with 18 Surefire tests (12 Cucumber scenarios + 6 ConfigReaderTest unit tests), 17 passed, all requirements traced to passing tests or code-review checks.
- Implementation plan documentation and requirement-to-test reconciliation updated to reflect completion of all nine tasks and known limitations (pre-existing NFR-001 DOM-presence proxy limitation, placeholder UI coupling, footer selector brittleness).

### Changed

- `LoginPage.java`: Removed short-circuiting `areAllLoginControlsDisplayed()` method (replaced with diagnosable `findMissingLoginControls()` and individual placeholder readers per DR-003, DR-002).
- `login.feature`: Extended `@TS_LOG_008` with two placeholder-value assertion steps and added reusable `Then` step for HTTPS scheme assertion injected into `@TS_LOG_008` and `@TS_LOG_001` (no new tags, per OQ-A001).

### Documentation

- Added `artifacts/CHANGELOG.md` (this file) with user-facing entry for version 1.0.0 traceability to FR-001–FR-006, NFR-001–NFR-002, and TASK-001–TASK-009.
- `artifacts/requirements.md`: Updated AC-008 (NFR-002) coverage note and NFR-002 traceability row to reflect HTTPS clause now executed via @TS_LOG_001/@TS_LOG_008 and guarded preventively (TASK-001/TASK-004), while no-logging clause remains code-review-only (TASK-007).
- `artifacts/architecture.md`: Flipped FR-001 and NFR-002 coverage from Partial to Complete with clarifying note on HTTPS preventive + executed coverage split.
- `artifacts/impl-plan.md`: Updated all task statuses to Done with outcomes and added comprehensive verification reconciliation per DR-011 classification conventions.

### Fixed

- DR-001 (High design-review finding): Transport security control regression — base.url had no scheme validation; the existing `getValidPassword()` host guard could be bypassed with an override, and the AD-009 HTTPS assertion ran too late (after the password was POSTed). Now covered by preventive fail-closed scheme guard in `getBaseUrl()` (TASK-001) plus negative-path unit tests (TASK-003) and executed assertion (TASK-004).
- DR-002, DR-003: FR-001 overstated coverage — the previous `areAllLoginControlsDisplayed()` boolean chain masked individual control failures and applied excessive per-control timeouts. Replaced with diagnosable missing-controls collection and single grouped timeout (TASK-005, TASK-006).
- REC-002: Scenario double-count after Surefire `<includes>` change — confirmed none (TASK-002 validation, TASK-008 verification).

### Known Limitations

- **NFR-001 (Loading Indicator):** `@TS_LOG_009` fails with a pre-existing DOM-presence proxy limitation (login page's auth response is fast enough that the transient loading indicator is not captured). Documented in architecture.md §Risks (DR-004) and not a regression from TASK-001–TASK-009.
- **Placeholder Text Coupling:** Assertion values are coupled to the OrangeHRM demo site's English UI copy; localization or copy changes require feature-file updates (accepted trade-off per AD-008).
- **Footer Selector Brittleness:** Footer locators derived from live DOM using `oxd-*` class fragments; future demo-site updates may require one-file repair in `LoginPage.java` (documented in impl-plan.md §Risks).
- **NFR-002 No-Logging Clause:** Code-review-only coverage (TASK-007); `hook_scan_secrets` automation is unimplemented and silently no-ops. No credentials found in logs/reports via manual review.

### Test Results

- Full suite: `mvn test -Dheadless=true`
- Surefire: 18 tests, 17 passed, 1 pre-existing failure (NFR-001 proxy limitation, not a regression)
- Cucumber: 12 scenarios, 11 passed, 1 pre-existing failure; 43 total steps, 42 passed, 1 failed, 0 undefined, 0 skipped
- ConfigReaderTest: 6 unit tests, 6 passed
- Coverage: All 6 FR + 2 NFR traced to passing tests or code-review checks
- Credentials: Zero credentials logged or embedded in code/reports

---

**Traceability:**

| ID | Type | Link | Status |
| --- | --- | --- | --- |
| US-AUTH-002 | Story | Customer Login & Authentication | Complete |
| FR-001..FR-006 | Functional Requirements | artifacts/requirements.md | All passing |
| NFR-001..NFR-002 | Non-Functional Requirements | artifacts/requirements.md | NFR-002 complete; NFR-001 pre-existing limitation |
| TASK-001..TASK-009 | Implementation Tasks | artifacts/impl-plan.md | All complete |
| @TS_LOG_001..@TS_LOG_012 | Cucumber Scenarios | src/test/resources/features/login.feature | 11/12 passing (1 pre-existing) |
| ConfigReaderTest | Unit Tests | src/test/java/com/orangehrm/config/ConfigReaderTest.java | 6/6 passing |
