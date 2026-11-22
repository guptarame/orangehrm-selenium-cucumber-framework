# Implementation Plan

**Story:** `US-AUTH-002` — Customer Login & Authentication
**Upstream artifacts:** `artifacts/architecture.md` (approved, `AD-001`–`AD-013`),
`artifacts/design-review.md` (approved, `DR-001`–`DR-012`, all accepted)
**Planner:** Planner Agent (Step 4)
**Date:** 2026-11-21

## Overview

The approved architecture is **confirmed as-is**: no new layer, module, runner, or
dependency is introduced. All planned work is additive and lands inside existing
components, closing the two coverage gaps the architecture booked (`AD-008`, `AD-009`)
plus the one `High` design-review finding (`DR-001`).

Nine tasks across four workstreams:

| Epic | Tasks | Closes | Theme |
| --- | --- | --- | --- |
| **A — Transport security (`NFR-002`)** | `TASK-001`–`TASK-004` | `DR-001`, `AD-009`, `AD-010` | Make the HTTPS control preventive (fail closed before the browser navigates), with the executed assertion retained as defence in depth |
| **B — `FR-001` completeness** | `TASK-005`, `TASK-006` | `DR-002`, `DR-003`, `AD-008` | Assert the footer copyright, ≥1 social link, and both placeholder *values*; replace the short-circuiting boolean chain with a named missing-controls set |
| **C — Review and verification gates** | `TASK-007`, `TASK-008` | `DR-011`, `DR-012` | The mandatory credential-logging review check and the full-suite regression run |
| **D — Coverage-claim truthfulness** | `TASK-009` | `DR-002`, `DR-004` | Flip `FR-001`/`NFR-002` coverage claims only after the suite proves them |

**Delivery sequence.** Epic A and Epic B share no files and run concurrently.
Epic A is sequenced first because it is the only `High`-severity security finding and
because `TASK-002` carries an open scope question (`OQ-P001`) that the plan-approval
gate must settle. Epics C and D are gates that consume the output of A and B, so they
close the plan rather than open it.

**Recommended first task:** `TASK-001` — it is self-contained, closes the `High`
finding, and depends on nothing.

## Source References

- Architecture: `artifacts/architecture.md` (`AD-008`, `AD-009`, `AD-010` are the
  decisions that generate code; `AD-001`–`AD-007`, `AD-011`–`AD-013` describe the
  existing design and generate no task)
- Requirements: `artifacts/requirements.md` (`FR-001`–`FR-006`, `NFR-001`–`NFR-002`)
- Design review: `artifacts/design-review.md` (`DR-001`–`DR-012`; §Readiness
  Recommendation seeded the four candidates expanded below)
- Rules in force: `.claude/rules/code-quality.md` (`**/*.java`),
  `.claude/rules/tests.md` (auto-applies via `**/*.java`, per `DR-005`),
  `.claude/rules/sdlc-artifacts.md` (`artifacts/**/*.md`)

### Design-review candidates → tasks

| Candidate (`design-review.md` §Readiness) | Task(s) |
| --- | --- |
| Preventive HTTPS scheme guard in `ConfigReader.getBaseUrl()` + negative coverage | `TASK-001`, `TASK-002`, `TASK-003` |
| Executed HTTPS scheme assertion on login and post-submit URLs | `TASK-004` |
| Footer, social-link, and placeholder-value assertions extending `@TS_LOG_008` | `TASK-005`, `TASK-006` |
| Documentation-only corrections to `artifacts/architecture.md` | **No task — already applied** by the Step 3 review (see §Explicitly Not Tasked) |

## Assumptions and Constraints

- **No renumbering.** `FR-###`, `NFR-###`, `AD-###`, and `DR-###` IDs are consumed
  as-is. `TASK-001`–`TASK-009` are assigned once here and never reused.
- **The existing `getValidPassword()` demo-host guard is untouchable.** No task may
  remove, narrow, or widen it; the new scheme guard is strictly additive
  (`AD-010`, `.claude/CLAUDE.md`, `config.properties`, `design-review.md` closing line).
- **Layer contract holds for every task** (`architecture.md` §Recommended
  Architecture): expected literals live in Gherkin, never in a Page Object; step
  definitions may hold a `WebDriver` only to construct Page Objects (`DR-009`);
  assertions live only in `LoginSteps`.
- **No unit-test layer exists today.** `tests.md`'s unit/integration *structure*
  clause is satisfied by the Gherkin + Page Object layering and introduces no unit
  tests of its own (`DR-006`). `TASK-002`/`TASK-003` are the narrow, approved
  exception the architecture anticipated ("unless an approved `TASK-###` introduces
  one") — scoped to `ConfigReader`'s security guard only, not a general unit-test
  initiative.
- **Surefire currently executes `**/TestRunner.java` only** (`pom.xml` lines 96–98).
  A new JUnit test class is compiled but **never executed** until that include list
  changes. This is why `TASK-002` exists as a distinct prerequisite.
- **Explicit waits only** (`AD-007`). No task may introduce an implicit wait.
- **No parallel execution and no CI workflow file** are in scope — `OQ-A004` and
  `OQ-A003` were closed as out of scope by user decision and are **not** gaps.
- **Exact OrangeHRM footer selectors are `Not Found`** in any approved artifact. The
  Implementation Agent must derive them from the live DOM during `TASK-005`; this plan
  deliberately does not invent them.

## Implementation Progress (Step 5)

All nine tasks are implemented, tested, and committed. See
`artifacts/verification-report.md` for the full-suite run (`TASK-008`).

| Task | Status | Outcome |
| --- | --- | --- |
| `TASK-001` | Done | Preventive fail-closed HTTPS scheme guard added to `ConfigReader.getBaseUrl()`; `getValidPassword()`/`isDemoHost` left byte-for-byte unchanged. |
| `TASK-002` | Done | Surefire `<includes>` extended to `**/*Test.java`; confirmed no double-count of the 12 Cucumber scenarios (`REC-002`). |
| `TASK-003` | Done | `ConfigReaderTest` added (6 cases); all pass; properties saved/restored per test. |
| `TASK-004` | Done | Executed HTTPS scheme assertion added to `@TS_LOG_001` and `@TS_LOG_008`. |
| `TASK-005` | Done | `LoginPage.findMissingLoginControls()`, footer/placeholder readers added; `areAllLoginControlsDisplayed()` removed. Footer locators derived from the live rendered DOM. |
| `TASK-006` | Done | `@TS_LOG_008` extended with missing-controls and placeholder-value assertions; negative check performed and reverted (confirmed real coverage). |
| `TASK-007` | Done | Credential-logging review check performed over the `TASK-001`–`TASK-006` diff and the generated reports; no finding. Recorded in `artifacts/verification-report.md`. |
| `TASK-008` | Done | Full-suite run completed; see `artifacts/verification-report.md` for the classification of the one pre-existing `@TS_LOG_009` failure. |
| `TASK-009` | Done | `FR-001`/`NFR-002` coverage notes flipped to Complete in `artifacts/architecture.md` and `artifacts/requirements.md`, backed by the passing tests above. |

## Task List

### TASK-001: Preventive fail-closed HTTPS scheme guard in `ConfigReader.getBaseUrl()`

- **Priority:** High
- **FR/NFR:** `NFR-002`
- **Architecture section:** §Technology Choices `AD-010`; §Security Considerations
  (Transport bullet); §Requirement-to-Component Traceability (`NFR-002` row)
- **Design review:** `DR-001` (the single `High` finding); resolves `OQ-D002`
- **Depends on:** None
- **Blocked by:** None
- **Target agent:** Implementation Agent
- **Description:** Add scheme validation to `ConfigReader.getBaseUrl()` so the suite
  fails closed before the browser ever navigates:
  1. Resolve `base.url` as today, then parse it and require the scheme to equal
     `https`, case-insensitively.
  2. Throw `IllegalStateException` with a message that names the offending scheme and
     the opt-out key, and **never** interpolates a credential value (`AD-011`).
  3. Fail closed on a malformed or scheme-less URL too — the existing `isDemoHost`
     helper swallows `IllegalArgumentException` from `URI.create` and returns `false`;
     the new guard must **throw** rather than silently pass in that case.
  4. Gate the guard behind a single opt-out resolved via
     `getBoolean("allow.insecure.base.url", false)` — **the chosen key name, which
     resolves `OQ-D002`** (selected over `insecure.base.url.allowed` / `allow.http`
     for consistency with the existing dotted, lower-case key style). The default must
     be `false`, obtained from the `getBoolean` default so that an absent key is a
     secure default rather than a parsed value.
  5. Document the key in `src/test/resources/config.properties` as a **commented-out**
     entry with a one-line justification (local non-TLS sandbox only). Do **not** add
     it as a live `false` line and never as `true` — a commented entry cannot be
     flipped by accident and keeps the secure default in code.
  6. Leave `getValidPassword()`'s demo-host guard and `isDemoHost` byte-for-byte
     unchanged.
  7. Add a `why`-style comment on the guard only: that it is preventive because
     `AD-009`'s assertion necessarily runs after the password has been POSTed.
- **Expected output:** Modified
  `src/test/java/com/orangehrm/config/ConfigReader.java`; modified
  `src/test/resources/config.properties` (comment block only).
- **Validation:** `mvn test` still passes unchanged against the committed HTTPS
  `base.url` (guard is transparent on the happy path); a manual
  `mvn test -Dbase.url=http://opensource-demo.orangehrmlive.com/web/index.php/auth/login -Dcucumber.filter.tags="@TS_LOG_001"`
  fails in the `Given` step with the scheme error and **no browser navigation to the
  login form**. Automated coverage follows in `TASK-003`.
- **Note for Step 7:** a deliberate scheme-guard failure also surfaces as a failing
  `Given` step, which superficially matches `DR-011`'s *environmental* classification
  pattern. The two are distinguished by the exception type and message
  (`IllegalStateException` naming the scheme vs. a Selenium timeout with a blank
  screenshot). `TASK-008` must apply that distinction.

### TASK-002: Enable JUnit unit-test execution so the config guard can be covered

- **Priority:** High
- **FR/NFR:** `NFR-002` (enabler)
- **Architecture section:** §Technology Choices `AD-004` (Maven/Surefire contract);
  §SDLC Pipeline Components (`tests.md` precedence note)
- **Design review:** `DR-001` (negative coverage is part of the accepted fix);
  `DR-006` (the approved-task exception to the no-unit-test-layer precedence)
- **Depends on:** None
- **Blocked by:** `OQ-P001` — scope confirmation that a `pom.xml` Surefire change and
  the repository's first unit-test class are in scope for `US-AUTH-002`. Approving
  this plan resolves it; see §Blocked Tasks.
- **Target agent:** Implementation Agent
- **Description:** Extend the `maven-surefire-plugin` `<includes>` block from
  `**/TestRunner.java` alone to also include `**/*Test.java`, so a JUnit 4 test class
  under `src/test/java` is executed by `mvn test`. Change **only** the `<includes>`
  list — leave `testFailureIgnore=false` and the plugin version untouched, and do not
  touch `maven.compiler.source`/`target` (`DR-010` deferred that as housekeeping
  outside this story). Record in the `pom.xml` comment that the include exists for
  `ConfigReader`'s security-guard coverage and is not an invitation to add a broad
  unit-test layer (`code-quality.md` scope discipline).
- **Expected output:** Modified `pom.xml` (Surefire `<includes>` + one comment).
- **Validation:** `mvn test` discovers and runs both `TestRunner` and any
  `*Test.java` class; the 12 existing Cucumber scenarios still run exactly once each
  (confirm the Surefire summary does not double-count scenarios via a second entry
  point) and `target/surefire-reports/` gains the new test class.

### TASK-003: Negative-path coverage for the fail-closed scheme guard

- **Priority:** High
- **FR/NFR:** `NFR-002`
- **Architecture section:** §Technology Choices `AD-010`; §Security Considerations
  (Transport, Secret management)
- **Design review:** `DR-001` ("+ negative coverage for the fail-closed path")
- **Depends on:** `TASK-001` (the guard), `TASK-002` (test execution)
- **Blocked by:** `TASK-002`, and transitively `OQ-P001`
- **Target agent:** Implementation Agent
- **Description:** Add `src/test/java/com/orangehrm/config/ConfigReaderTest.java`
  covering the guard's branches. `ConfigReader` resolves overrides on **every call**
  (`resolveOverride` reads `System.getProperty`/`System.getenv` per invocation), so the
  singleton needs no new seam and no mocking — set the system property, call
  `getBaseUrl()`, assert. Required cases, each naming `NFR-002` in the test name or a
  one-line comment (`tests.md` coverage clause):
  1. `https` `base.url` → returns the URL unchanged (happy path).
  2. `http` `base.url` → throws `IllegalStateException` (the `DR-001` fail-closed
     path, including the public demo host over `http`, which the host guard alone lets
     through).
  3. `http` `base.url` **plus** `allow.insecure.base.url=true` → returns the URL
     (opt-out honoured).
  4. Absent `allow.insecure.base.url` → treated as `false` (secure default).
  5. Malformed / scheme-less `base.url` → throws rather than passing.
  6. **Regression guard:** `getValidPassword()` still throws for a non-demo host with
     no override, proving `TASK-001` did not weaken the existing control.
- **Expected output:** New `ConfigReaderTest.java` (JUnit 4, matching the declared
  stack per `AD-003`).
- **Validation:** All cases pass under `mvn test`. **Determinism requirement
  (`tests.md`):** Surefire runs one JVM by default, so every test must capture the
  prior value of each property it sets and restore or clear it in an `@After`/`finally`
  — a leaked `base.url` or `allow.insecure.base.url` would silently retarget or weaken
  the Cucumber scenarios running in the same JVM. Verify by running the full suite and
  confirming the 12 scenarios still execute against the committed HTTPS demo URL.

### TASK-004: Executed HTTPS scheme assertion on the login and post-submit URLs

- **Priority:** Medium
- **FR/NFR:** `NFR-002`
- **Architecture section:** §Technology Choices `AD-009` (defence in depth);
  §Security Considerations (Transport bullet)
- **Design review:** `DR-001` (`AD-009` retained, re-scoped); closes `OQ-A002`
- **Depends on:** None
- **Blocked by:** None
- **Target agent:** Implementation Agent
- **Description:** Add one reusable `Then` step asserting the current URL uses the
  `https` scheme, read through the existing public `BasePage.getCurrentUrl()` — **no
  new Page Object method is required**. Attach it to two existing scenarios so both
  points `AD-009` names are covered without adding a tag:
  - `@TS_LOG_008` — the loaded login page (pre-submit).
  - `@TS_LOG_001` — after the successful redirect (post-submit), where a mid-session
    downgrade would surface.
  Assert on the scheme specifically (a `https://` prefix check, case-insensitive), not
  a substring `contains("https")`, which a URL carrying `https` in a query parameter
  would satisfy. Keep the assertion in `LoginSteps`; do not re-implement
  `getCurrentUrl`. **Do not** introduce a new `@TS_LOG_013` — `OQ-A001`'s accepted
  default (no new tags) keeps the `requirements.md` scenario mapping stable.
- **Expected output:** Modified `src/test/resources/features/login.feature` (two
  added steps); modified
  `src/test/java/com/orangehrm/stepdefinitions/LoginSteps.java` (one new `@Then`).
- **Validation:** `mvn test -Dcucumber.filter.tags="@TS_LOG_001 or @TS_LOG_008"`
  passes; both scenarios show the new step in `cucumber-html-report.html`. Confirm the
  step is genuinely executed (visible in the report) rather than reported as undefined
  or skipped.

### TASK-005: `LoginPage` — missing-controls collection, footer readers, placeholder readers

- **Priority:** High
- **FR/NFR:** `FR-001`
- **Architecture section:** §Technology Choices `AD-008`;
  §Requirement-to-Component Traceability (`FR-001` row); §Risks (footer-locator
  brittleness row)
- **Design review:** `DR-002` (coverage must be real), `DR-003` (diagnosability and
  stacked timeouts); closes `OQ-A001`
- **Depends on:** None
- **Blocked by:** None
- **Target agent:** Implementation Agent
- **Description:** Replace `areAllLoginControlsDisplayed()` — which `&&`-chains five
  `isDisplayed(…, 10)` calls behind one boolean — with diagnosable readers:
  1. Add a method returning the **collection of missing required control names**
     (e.g. `List<String> findMissingLoginControls()`), evaluating every control
     independently so no missing control masks another. Cover the five existing
     controls (logo, username field, password field, Login button, forgot-password
     link) plus two new ones: the **footer copyright element** and **at least one
     footer social-link anchor**, as distinct entries.
  2. Add two separate readers returning the `placeholder` DOM attribute of the
     username and password inputs. Use `getDomAttribute("placeholder")`, consistent
     with the `FR-002` masking check. These return **values** for `LoginSteps` to
     compare — the Page Object holds no expected literal (layer contract).
  3. Derive footer locators from the live DOM; scope them to the footer container and
     prefer stable class fragments over generated `oxd-*` chains. Assert the social
     link as **anchor presence only, never `href` targets** (§Risks). Exact selectors
     are `Not Found` in the approved artifacts — confirm against the running page, do
     not guess.
  4. Per `DR-003`, use **one short named timeout constant** for the group (a few
     seconds, in the style of the existing `LOADING_INDICATOR_TIMEOUT`), not `10` per
     control: `open()` already waits for the username field, so the page is confirmed
     loaded before these checks run. Seven controls at 10s each would cost ~70s on the
     most likely failure mode (demo site down).
  5. Remove `areAllLoginControlsDisplayed()` rather than leaving it dead alongside the
     replacement (DRY, `code-quality.md`).
- **Expected output:** Modified `src/test/java/com/orangehrm/pages/LoginPage.java`
  (two new locators, one missing-controls method, two placeholder readers, one removed
  method).
- **Validation:** Compiles; `LoginSteps` is updated in `TASK-006` in the same change
  set so the build is never left broken. Confirm the short timeout holds by observing
  `@TS_LOG_008`'s duration in the report.

### TASK-006: `@TS_LOG_008` + `LoginSteps` — assert named missing controls and both placeholder values

- **Priority:** High
- **FR/NFR:** `FR-001`
- **Architecture section:** §Technology Choices `AD-008`;
  §Requirement-to-Component Traceability (`FR-001` row)
- **Design review:** `DR-002`, `DR-003`
- **Depends on:** `TASK-005`
- **Blocked by:** None
- **Target agent:** Implementation Agent
- **Description:** Wire the `TASK-005` readers into the existing scenario — **extend
  `@TS_LOG_008`, do not add a tag** (`OQ-A001`, confirmed):
  1. Rewrite the existing `all required login controls should be displayed` step to
     assert the missing-controls collection is **empty**, with the failure message
     listing the missing control names so an `FR-001` failure is diagnosable from the
     report alone (`DR-003`). Replace the current single `assertTrue` with its long
     prose message.
  2. Add two Gherkin steps carrying the **expected placeholder literals as step
     parameters**, following the existing `"Invalid credentials"` / `"Required"`
     pattern, and assert them with `assertEquals` in `LoginSteps`. Expected text lives
     in the feature file only (`DR-002`, layer contract).
  3. Reference `FR-001` in a one-line comment on the new steps, alongside the
     `@TS_LOG_008` tag (`sdlc-traceability`, `tests.md` coverage clause).
- **Expected output:** Modified `src/test/resources/features/login.feature`
  (`@TS_LOG_008` gains two steps); modified `LoginSteps.java` (one rewritten `@Then`,
  two new `@Then`).
- **Validation:** `mvn test -Dcucumber.filter.tags="@TS_LOG_008"` passes with all
  seven controls and both placeholder values asserted. **Negative check (prove the
  assertions bite):** temporarily point one footer locator at a non-existent selector
  and confirm the failure message names *that* control — then revert. This is the
  check that distinguishes real coverage from `DR-002`'s overstated claim.

### TASK-007: Mandatory no-credential-logging review check

- **Priority:** High
- **FR/NFR:** `NFR-002`
- **Architecture section:** §Technology Choices `AD-011`; §Security Considerations
  (No plaintext in logs — Control ownership); §SDLC Pipeline Components (hooks row)
- **Design review:** `DR-012`
- **Depends on:** `TASK-001`, `TASK-003`, `TASK-004`, `TASK-006`
- **Blocked by:** None
- **Target agent:** Review Agent
- **Description:** During Step 6, verify that no line added by `TASK-001`–`TASK-006`
  logs, prints, or embeds a credential, a field value, or a resolved password — and
  that no new exception message interpolates one (the scheme-guard message in
  `TASK-001` and the unit-test assertions in `TASK-003` are the two places where this
  is most likely to slip in). Also confirm the `getValidPassword()` demo-host guard is
  present and unmodified. **This check is non-skippable:** `hook_scan_secrets` is
  unimplemented and silently no-ops, making this review the *sole active control* for
  `AD-011` (`DR-012`). Do not treat the intended hook as if it were operating.
- **Expected output:** Review findings recorded for Step 6, with this check explicitly
  marked performed (pass or fail), plus any fixes.
- **Validation:** A diff review of every `TASK-001`–`TASK-006` change against
  `code-quality.md`'s "never hard-code, log, or return secrets" clause; any credential
  interpolation is a review-blocking defect.

### TASK-008: Full-suite verification run with environmental-failure classification

- **Priority:** High
- **FR/NFR:** `FR-001`–`FR-006`, `NFR-001`–`NFR-002`
- **Architecture section:** §Reliability and Observability; §Risks and Mitigations
  (live-site row, including the Step 7 classification rule)
- **Design review:** `DR-011`; `DR-004` (record the presence-vs-visibility proxy, do
  not silently report it as full fidelity)
- **Depends on:** `TASK-003`, `TASK-004`, `TASK-006`, `TASK-007`
- **Blocked by:** None
- **Target agent:** Verify Agent
- **Description:** Run `mvn test` (optionally `-Dheadless=true`) and reconcile
  `target/cucumber-reports/` against every `FR`/`NFR`. Apply three conventions this
  plan's upstream artifacts already fixed:
  1. **`DR-011` classification** — a run where the login page never renders (the
     `Given` step or the login-page URL assertion fails, with a blank or error
     screenshot) is an **environmental** failure, reported as such, never as an
     `FR`/`NFR` verification failure.
  2. **Distinguish a deliberate fail-closed from an outage** — a `Given`-step failure
     carrying `TASK-001`'s `IllegalStateException` about the URL scheme is a
     *configuration* failure (the guard working as designed), not an environmental one.
  3. **`DR-004`** — report `NFR-001` as covered via DOM-presence polling, an
     explicitly accepted proxy for `AC-007`'s "visible", rather than implying
     visibility was asserted.
  Also confirm the suite still runs 12 Cucumber scenarios plus the `TASK-003` unit
  tests, and that no scenario was silently skipped.
- **Expected output:** `artifacts/verification-report.md`.
- **Validation:** Every `FR`/`NFR` row maps to a passing scenario or unit test, or is
  reported honestly as a gap (`tests.md`: "report coverage honestly"). Zero
  `undefined`/`skipped` steps in `cucumber.json`.

### TASK-009: Update `FR-001` and `NFR-002` coverage claims in the upstream artifacts

- **Priority:** Medium
- **FR/NFR:** `FR-001`, `NFR-002`
- **Architecture section:** §Requirement-to-Component Traceability (`FR-001` and
  `NFR-002` rows, both currently **Partial**)
- **Design review:** `DR-002` (no "Complete" claim before the sub-elements are
  verified), `DR-001`
- **Depends on:** `TASK-008`
- **Blocked by:** `OQ-P002` — amending the approved `artifacts/requirements.md`
  needs human sign-off; see §Blocked Tasks
- **Target agent:** Implementation Agent (documentation only), then human review
- **Description:** Once `TASK-008` shows the new assertions passing, flip the two
  overstated-until-now coverage claims — and only then:
  1. `artifacts/architecture.md` — `FR-001` row **Partial → Complete** (the condition
     it already states: copyright element, ≥1 social-link anchor, and both placeholder
     *values* asserted); `NFR-002` row **Partial → Complete**, citing the `TASK-001`
     preventive guard and the `TASK-004` executed assertion.
  2. `artifacts/requirements.md` — `AC-008`'s note that "no dedicated Cucumber
     scenario exists for the HTTPS/no-logging aspects" is now **partly stale**: the
     HTTPS clause is executed (`@TS_LOG_001`, `@TS_LOG_008`) and guarded preventively,
     while the **no-logging clause remains code-review-only** (`TASK-007`). Update the
     `NFR-002` traceability row to cite the two scenarios. Preserve the distinction —
     do not let the HTTPS fix imply the logging clause became automated.
  3. Change coverage notes and status only. Do **not** renumber any ID, restate
     requirement text, or alter an `FR`/`NFR` statement (`sdlc-artifacts.md`,
     `sdlc-traceability`).
- **Expected output:** Modified `artifacts/architecture.md` and
  `artifacts/requirements.md` (traceability/coverage rows only).
- **Validation:** Every claim flipped to "Complete" is backed by a named passing
  scenario or test in `artifacts/verification-report.md`. A reviewer can trace each
  changed row to a `TASK-###` and a test result.

## Parallelization Opportunities

- **Epic A and Epic B are fully independent and can run concurrently from the start.**
  `TASK-001`/`TASK-003` touch `ConfigReader.java`, `config.properties`, and a new
  `ConfigReaderTest.java`; `TASK-005`/`TASK-006` touch `LoginPage.java`,
  `login.feature`, and `LoginSteps.java`. No file overlap, no shared prerequisite.
- `TASK-002` (pom.xml) is independent of `TASK-001` and can be done first, in
  parallel, or while `OQ-P001` is being confirmed — `TASK-001` does not wait on it.
- **File-contention warning:** `TASK-004` and `TASK-006` both edit
  `src/test/resources/features/login.feature` and `LoginSteps.java`, and both touch
  `@TS_LOG_008`. They are logically independent but **must not be implemented
  concurrently** — sequence them (`TASK-006` then `TASK-004` is marginally safer,
  since `TASK-006` rewrites an existing step while `TASK-004` only appends) or accept
  a merge reconciliation.
- `TASK-007` (review) can begin on Epic A's diff before Epic B lands, provided it is
  re-run over the complete change set before Step 6 is signed off.
- `TASK-008` and `TASK-009` are strictly serial tail tasks: the suite must pass before
  any coverage claim is flipped.

### Critical path

`TASK-002` → `TASK-003` → `TASK-007` → `TASK-008` → `TASK-009`

`TASK-002` sits at the head of the critical path **only** because of `OQ-P001`.
Resolving that question at the plan-approval gate removes the longest wait in the plan.

## Blocked Tasks

| Task | Blocked by | Unblock criteria |
| --- | --- | --- |
| `TASK-002` | `OQ-P001` — scope confirmation: this task edits `pom.xml` (Surefire `<includes>`) and `TASK-003` adds the repository's **first unit-test class**. `architecture.md` §SDLC Pipeline Components permits exactly this ("no unit/mock layer is in scope for `US-AUTH-002` **unless an approved `TASK-###` introduces one**"), and `DR-001`'s accepted fix explicitly requires negative coverage — but the Planner will not expand the build contract on an inference. | User confirms at the plan-approval gate that `TASK-002` + `TASK-003` are in scope. **Planner recommendation: approve** — without them, `DR-001`'s fail-closed path has no automated coverage and `tests.md`'s "cover the error branches" clause is unmet. |
| `TASK-003` | `TASK-002` (transitively `OQ-P001`) — the test class compiles but never executes under the current Surefire include list, so writing it first would produce silent false confidence. | `TASK-002` merged and verified to execute the new class. |
| `TASK-009` | `OQ-P002` — amending the **approved** `artifacts/requirements.md` (`AC-008` note + `NFR-002` traceability row). `OQ-A002` was confirmed with the design review noting this "would update `AC-008`'s coverage note", so the intent is approved; the edit to a signed-off artifact still warrants explicit sign-off. Also gated on `TASK-008`. | User confirms the `requirements.md` coverage-note edit, and `TASK-008` reports the new assertions passing. The `architecture.md` half of `TASK-009` is unaffected and can proceed on `TASK-008` alone. |

No task is blocked on an unresolved **design** question: `OQ-A001`–`OQ-A004` are
closed, `OQ-D002` is resolved by `TASK-001` (key name chosen), and `OQ-D001` is a
recommendation to the user rather than a task (see below). The two blockers above are
**scope/approval** questions, both answerable at the plan-approval gate.

## Explicitly Not Tasked

Recorded so no downstream agent re-opens settled ground:

| Item | Why there is no task |
| --- | --- |
| `DR-004`–`DR-012` documentation corrections to `artifacts/architecture.md` | **Already applied** by the Step 3 review (`design-review.md` §Required Architecture Updates, items 1–9). The fourth design-review candidate needs **no further doc task** beyond `TASK-009`'s post-verification coverage flip. |
| CI/CD pipeline definition (`OQ-A003`) | Out of scope by user decision; its absence is **not a gap** and must not be raised as a finding. |
| Parallel execution (`OQ-A004`) | Out of scope by user decision; `AD-012`'s `ThreadLocal` driver is parallel-*ready* only. **Not a gap.** |
| `maven.compiler.release` vs `source`/`target` (`DR-010`) | Accepted as documentation-only; the `pom.xml` change is deferred housekeeping outside `US-AUTH-002`. `TASK-002` must not bundle it. |
| Full transport inspection — certificate validity, mixed content | Deferred; needs a proxy for no requirement-backed benefit. `TASK-001` + `TASK-004` cover the credible risk. |
| Unit tests for `BasePage`, `DriverManager`, Page Objects | Out of scope per `DR-006`. `TASK-002`/`TASK-003` are scoped to `ConfigReader`'s security guard **only** and are not a precedent for a general unit-test layer (`code-quality.md` scope discipline). |
| `.claude/settings.json` stale `permissions.allow` entries; the six unimplemented hooks | Deferred repository housekeeping. `DR-012`'s one `NFR-002` consequence is covered by `TASK-007`. |
| Removing plaintext demo credentials from `config.properties` | A documented, scoped exception, valid while the target is the public demo host. `TASK-001` must not "fix" it. |

## Recommendations for the User (not tasks)

- **`REC-001` (`OQ-D001`, from `DR-005`/`DR-007`) — two inaccuracies in
  `.claude/CLAUDE.md`, for the user to apply directly.** `.claude/CLAUDE.md` is
  **project configuration, not an `artifacts/**` deliverable**, so it falls outside
  this skill's scope and outside what any agent in this pipeline should edit on an
  agent's instruction. Raised here as a recommendation, consistent with the Step 3
  review leaving it to the user. Two corrections:
  1. §Optional runtime overrides documents `-DbaseUrl=…`. The key `ConfigReader`
     resolves is **`base.url`**, so `-DbaseUrl=` is silently ignored and the run
     continues against the committed demo URL (`DR-007`). Correct it to
     `-Dbase.url=…`. This matters more after `TASK-001`: an operator who believes they
     retargeted the suite would also believe they had exercised the new scheme guard.
  2. §Rules claims `tests.md`'s path patterns "don't actually match this repo's test
     sources". The rule's real frontmatter includes **`**/*.java`**, so it **does**
     auto-apply to every Java file here (`DR-005`). `artifacts/architecture.md` has
     been corrected; `CLAUDE.md` still carries the stale note — and the repository
     contains two mutually inconsistent copies of its pattern list
     (`**/*.test.py`/`**/*.spec.py` in one, `**/*.test.java` in another), neither of
     which matches the rule. Worth reconciling to one accurate list.
  Both are documentation-only and block nothing in this plan.
- **`REC-002` — after `TASK-002`, confirm Surefire reports no duplicate scenario
  execution.** Broadening `<includes>` is the one change here that could alter how the
  existing 12 scenarios are discovered. `TASK-002`'s validation covers it; flagged
  because a double-counted suite would quietly corrupt `TASK-008`'s evidence.

## Risks and Open Questions

| # | Risk / question | Impact | Mitigation |
| --- | --- | --- | --- |
| 1 | **`OQ-P001`** — is the `pom.xml` + first-unit-test scope expansion approved? | Blocks `TASK-002`/`TASK-003`; without them `DR-001`'s fail-closed path has no automated coverage | Resolve at the plan-approval gate; Planner recommends approval (see §Blocked Tasks) |
| 2 | **`OQ-P002`** — is amending the approved `requirements.md` `AC-008` note approved? | Blocks half of `TASK-009`; leaving it stale means the artifact set under-reports verified `NFR-002` coverage | Resolve at the plan-approval gate; the `architecture.md` half proceeds regardless |
| 3 | Unit tests in `TASK-003` mutate system properties in the **same Surefire JVM** as the Cucumber scenarios; a leaked `base.url` or `allow.insecure.base.url` would retarget or weaken the whole suite | High — a leaked opt-out would silently disable the very guard being added | Mandatory save/restore in `@After`/`finally` (`TASK-003` validation); `TASK-008` confirms the 12 scenarios still run against the committed HTTPS URL |
| 4 | Footer and social-link selectors are third-party `oxd-*` markup, possibly lazy-loaded; exact selectors are `Not Found` in the approved artifacts | Medium — the most brittle locators in the suite | Derive from the live DOM in `TASK-005`; scope to the footer container; anchor **presence** only, never `href`; keep them in `LoginPage` so a repair is one file (§Risks) |
| 5 | `getValidPassword()` calls `getBaseUrl()`, so after `TASK-001` an `http` target raises the **scheme** error before the existing host-guard error — the exception message for an existing failure path changes | Low | Intended (fail closed, earliest possible point). `TASK-003` case 6 proves the host guard still fires on its own path; `TASK-007` confirms the guard is unmodified |
| 6 | Placeholder literals couple the suite to the demo site's English UI copy | Low — localisation is out of scope upstream | Accepted (`AD-008` trade-off); literals live in Gherkin, so a copy change is a one-line feature-file edit |
| 7 | Live demo site down, slow, or data-reset during `TASK-008` | High likelihood, high impact | `DR-011` classification rule, applied in `TASK-008` convention 1, with convention 2 distinguishing a deliberate fail-closed from an outage |
| 8 | `TASK-004` and `TASK-006` edit the same two files and the same scenario | Medium — merge conflicts or a lost assertion | Sequence them; do not implement concurrently (§Parallelization) |

## Recommended First Task for Implementation Agent

`TASK-001` — Preventive fail-closed HTTPS scheme guard in `ConfigReader.getBaseUrl()`

It closes the single `High` design-review finding (`DR-001`), depends on nothing, is
blocked by nothing, is confined to one class plus a comment block, and resolves
`OQ-D002` by fixing the opt-out key name that `TASK-003` and `config.properties` both
depend on. Start `TASK-005` in parallel if a second thread is available.

## Traceability Matrix

| FR/NFR | Architecture | DR | TASK | Test | Status |
| --- | --- | --- | --- | --- | --- |
| `FR-001` | `AD-008`, `LoginPage` | `DR-002`, `DR-003` | `TASK-005`, `TASK-006`, `TASK-009` | `@TS_LOG_008` (extended) | Done |
| `FR-002` | `LoginPage.getPasswordFieldType` | — | *No task — Complete* | `@TS_LOG_007` | Covered |
| `FR-003` | `LoginPage`, `DashboardPage` | — | *No task — Complete* | `@TS_LOG_001`, `@TS_LOG_012` | Covered |
| `FR-004` | `LoginPage.getErrorBannerText` | — | *No task — Complete* | `@TS_LOG_002`, `@TS_LOG_003`, `@TS_LOG_011` | Covered |
| `FR-005` | Field-scoped error locators | — | *No task — Complete* | `@TS_LOG_004`, `@TS_LOG_005`, `@TS_LOG_006` | Covered |
| `FR-006` | `ResetPasswordPage` | — | *No task — Complete* | `@TS_LOG_010` | Covered |
| `NFR-001` | `submitAndCaptureLoadingIndicator` | `DR-004` | `TASK-008` (report the proxy) | `@TS_LOG_009` | Covered (documented proxy) |
| `NFR-002` | `AD-009`, `AD-010`, `AD-011` | `DR-001`, `DR-008`, `DR-012` | `TASK-001`, `TASK-002`, `TASK-003`, `TASK-004`, `TASK-007`, `TASK-009` | `ConfigReaderTest`, `@TS_LOG_001`, `@TS_LOG_008` | Done |

**No orphans.** Every `TASK-001`–`TASK-009` cites at least one `FR`/`NFR`, an
architecture section, and (except the two enabler/gate tasks) a `DR-###`. Every
`FR`/`NFR` is either already covered or has a task. `FR-002`–`FR-006` are verified
complete by the existing suite and correctly generate **no** task — the plan adds no
work that is not traceable to a gap.
