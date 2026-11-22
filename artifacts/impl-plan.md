# Implementation Plan

**Story:** `US-AUTH-002` — Customer Login & Authentication
**Upstream artifacts:** `artifacts/architecture.md` (approved, `AD-001`–`AD-014`, commit
`341c185`, revised by Step 3 to `e678b44`), `artifacts/design-review.md` (approved,
`DR-001`–`DR-018`, commit `e678b44`, gate **PASS**, 0 `Critical`, 0 unresolved `High`)
**Planner:** Planner Agent (Step 4)
**Date:** 2026-11-22
**Cycle:** Full pipeline restart over an **unchanged `src/`**

## Overview

This is a **re-issue of the plan for an already-implemented story**, not a plan to build
a system from zero. `TASK-001`–`TASK-009` were delivered, reviewed, verified, and merged
in the prior cycle; this plan re-confirms each one as `Done` **against the code and git
history rather than against the prior plan's own claim** (see
[Carried-forward tasks](#carried-forward-tasks-task-001task-009--confirmed-done)), and
then scopes only what this cycle's fresh findings genuinely warrant.

**What is actually new this cycle.** Step 3 produced six new findings
(`DR-013`–`DR-018`), and its Readiness Recommendation states that *no new `TASK-###` is
strictly required*. Every new finding is documentation, pipeline configuration, or a
forward-looking constraint — none requires a change to the test framework's product
behaviour. Against that, four tasks are opened, and two of the four exist because Step 3
agreed to controls that **no agent currently carries**:

| Epic | Tasks | Traces to | Theme |
| --- | --- | --- | --- |
| **E — Security-control delivery mechanism** | `TASK-010` | `DR-015`, `OQ-D004`, `NFR-002` | Terminate `code-quality.md`'s YAML frontmatter so the rule under the sole active `NFR-002` logging control is scoped by design, not by accident |
| **F — Security-control observability** | `TASK-011` | `DR-017`, `OQ-D005`, `AD-010`, `NFR-002` | Make a bypass of the primary `NFR-002` transport guard visible in run evidence |
| **G — This cycle's gates** | `TASK-012`, `TASK-013` | `DR-012`, `DR-016`, `DR-017`, `DR-011` | The mandatory Step 6 credential-logging/guard-integrity check over this cycle's diff, and a current Step 7 run that confirms the opt-out was unset |

**Both new code/config tasks are deliberately `Blocked` pending an explicit human
decision.** Step 3 routed `OQ-D004` and `OQ-D005` to the Planner/user rather than
resolving them, and neither traces to a mandatory `FR`/`NFR` clause. `TASK-010` in
particular edits **agent configuration** under `.claude/`, which no agent may change on
another agent's instruction — it needs the user's own approval. They are recorded as
tasks with stable IDs, rather than as loose recommendations, precisely so they do not
evaporate the way `DR-014` showed an unowned mitigation does.

**Delivery sequence.** `TASK-010` and `TASK-011` are independent and may run
concurrently once approved. `TASK-012` (Step 6) consumes whatever diff they produce, and
`TASK-013` (Step 7) closes the cycle. `TASK-013` runs **even if both `TASK-010` and
`TASK-011` are declined**, because the current `artifacts/verification-report.md`
predates the re-issued architecture and the Step 3 opt-out check.

**Recommended first task:** `TASK-013` if both new tasks are declined; otherwise
`TASK-010` — one line, lowest risk, and it sits under a security control.

## Source References

- **Architecture:** `artifacts/architecture.md` — `AD-010` (opt-out exposure trade-off
  column), `AD-011` (boundary-only logging), `AD-014` (unit-test layer and its mandatory
  capture/restore pattern), §Security Considerations (Transport, No plaintext in logs),
  §SDLC Pipeline Components (`code-quality.md` row), §Risks and Mitigations.
  `AD-001`–`AD-009`, `AD-012`, `AD-013` describe the existing, shipped design and
  generate **no task** this cycle.
- **Requirements:** `artifacts/requirements.md` — `FR-001`–`FR-006`,
  `NFR-001`–`NFR-002`. All eight are already covered by the shipped suite; no new
  requirement appeared this cycle, so **no task adds product behaviour**.
- **Design review:** `artifacts/design-review.md` — `DR-001`–`DR-018`;
  §Readiness Recommendation's four optional candidates are triaged below;
  §Agreed Design Decisions supply the standing constraints this plan inherits.
- **Rules in force:** `.claude/rules/code-quality.md` (scoping defective — `TASK-010`),
  `.claude/rules/tests.md` (auto-applies via `**/*.java`, per `DR-005`),
  `.claude/rules/sdlc-artifacts.md` (`artifacts/**/*.md`).

### Step 3 candidates → planning decision

Step 3 listed four optional, user-gated candidates. Each is resolved here explicitly so
no downstream agent re-opens it:

| Candidate (`design-review.md` §Readiness) | Traces to | Planning decision |
| --- | --- | --- |
| Add the closing `---` to `.claude/rules/code-quality.md` | `DR-015`, `OQ-D004`, `NFR-002` | **Tasked — `TASK-010`**, `High`, blocked on `OQ-P003` (human approval; it is agent configuration) |
| `WARN` log when `allow.insecure.base.url` is engaged | `DR-017`, `OQ-D005`, `AD-010`, `NFR-002` | **Tasked — `TASK-011`**, `Medium`, blocked on `OQ-P004` (human approval; it touches a security-guarded method) |
| Correct `.claude/CLAUDE.md` and `.claude/hooks/hook_implementation_task_gate.md` | `DR-005`, `DR-007`, `DR-013`, `OQ-D001`, `OQ-D003` | **No task.** Traces to no `FR`/`NFR`; it is agent configuration. Carried as `REC-001`/`REC-002` for the user |
| Implement `hook_implementation_task_gate.java` **and** its `settings.json` entry, or add the commit-message check to `code-review`/`create-pr` | `DR-013`, `DR-014`, `OQ-A006` | **No task — out of scope, confirmed by independent analysis.** See [Explicitly Not Tasked](#explicitly-not-tasked) for the reasoning and the compensating instruction placed on Step 5 instead |

## Assumptions and Constraints

- **No renumbering, no reuse.** `FR-###`, `NFR-###`, `AD-###`, and `DR-###` are consumed
  as-is. `TASK-001`–`TASK-009` keep their original IDs and meanings; `TASK-010`–`TASK-013`
  are the only new IDs assigned in this cycle (`sdlc-traceability`). The **full original
  task definitions** for `TASK-001`–`TASK-009` remain in git history at commit `f1009e1`
  (`docs: add implementation plan for US-AUTH-002 login authentication`); this document
  summarises them with completion evidence rather than restating them, per
  `sdlc-artifacts.md`'s single-source-of-truth rule.
- **The shipped product code is the baseline and is not re-implemented.** `src/` is
  unchanged since the prior cycle. No task in this plan re-does `TASK-001`–`TASK-009`
  work, and Step 5 must not "re-implement" anything it finds already present.
- **Both security guards are untouchable.** No task may remove, narrow, or widen
  `ConfigReader.getValidPassword()`'s demo-host guard or `getBaseUrl()`'s HTTPS scheme
  guard, and `allow.insecure.base.url` stays default-`false` and stays shipped commented
  out (`AD-010`, §Security Considerations, `design-review.md` closing constraints,
  `.claude/CLAUDE.md`). `TASK-011` adds a log line **beside** the guard and changes no
  control-flow decision.
- **Layer contract holds** (`architecture.md` §Recommended Architecture): expected
  literals live in Gherkin, never in a Page Object; step definitions may hold a
  `WebDriver` only to construct Page Objects (`DR-009`); assertions live only in
  `LoginSteps`; unit tests touch one class and start no browser (`AD-014`).
- **The `AD-014` unit-test layer stays narrow** — `ConfigReader` guard logic only, never
  a second home for behaviour already covered by Gherkin. The `@Before`/`@After`
  system-property capture-and-restore pattern is **mandatory** for any class matched by
  the broad `**/*Test.java` Surefire include (`DR-016`); `TASK-011`'s added test case
  inherits that obligation.
- **Agent configuration is user territory.** `.claude/**` holds the rules, skills,
  hooks, and settings that govern the agents themselves. This plan may *record* a needed
  change there, but execution requires the user's own approval — not another agent's
  instruction. This follows the `DR-005`/`DR-007`/`DR-015` precedent in which Step 3
  corrected `artifacts/*.md` and flagged `.claude/**`.
- **`.claude/` is currently untracked in git** — `git ls-files .claude` returns **0
  files**, and it is not `.gitignore`d. This is a newly observed fact and it directly
  constrains `TASK-010`: the fix cannot be committed as a one-file change until the user
  decides whether `.claude/` should be version-controlled. It also contradicts
  `.claude/CLAUDE.md`'s self-description as "checked into the codebase" (folded into
  `REC-001`).
- **Explicit waits only** (`AD-007`); no task may introduce an implicit wait.
- **Out of scope by standing user decision, and not gaps:** a CI/CD pipeline definition
  (`OQ-A003`), parallel execution (`OQ-A004`), `maven.compiler.release` (`DR-010`),
  Surefire `<runOrder>`/fork isolation (`DR-016`), and full transport inspection. No
  downstream agent may raise any of these as a finding.

## Carried-forward tasks: `TASK-001`–`TASK-009` — confirmed `Done`

Each task below was re-verified **this cycle** against the working tree and git history,
not accepted from the prior plan's status table. Step 3 independently reached the same
conclusion for the findings these tasks closed
(`design-review.md` §Carried-forward findings: verification).

| Task | Priority | FR/NFR | Arch / DR | Commit | Verified evidence in the working tree | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `TASK-001` Preventive fail-closed HTTPS scheme guard | High | `NFR-002` | `AD-010` / `DR-001` | `a950438` | `ConfigReader.getBaseUrl()` resolves `ALLOW_INSECURE_BASE_URL_KEY` via `getBoolean(…, false)`, then throws `IllegalStateException` naming the scheme and the opt-out key unless the scheme equals `https`; `resolveScheme()` returns `null` for a malformed URL so that also fails closed; opt-out shipped **commented out** in `config.properties`; `getValidPassword()`/`isDemoHost()` unchanged | **Done** |
| `TASK-002` Surefire executes `**/*Test.java` | High | `NFR-002` (enabler) | `AD-004`, `AD-014` / `DR-001`, `DR-006` | `f395aff` | `pom.xml` lines 99–102 carry both includes (`**/TestRunner.java`, `**/*Test.java`) with `testFailureIgnore=false`; the scope-discipline comment is present at lines 96–98 | **Done** |
| `TASK-003` Negative-path coverage for the guard | High | `NFR-002` | `AD-010`, `AD-014` / `DR-001` | `51ce3b2` | `ConfigReaderTest` has all **6** required cases (https pass-through, `http` throws, opt-out honoured, absent opt-out defaults secure, malformed URL throws, `getValidPassword()` host-guard regression) plus `@Before`/`@After` capture-and-restore | **Done** |
| `TASK-004` Executed HTTPS scheme assertion | Medium | `NFR-002` | `AD-009` / `DR-001` | `eed474f` | `LoginSteps.the_current_page_should_be_served_over_https()` asserts an `https://` **prefix** via `regionMatches(true, 0, …)` — not a `contains`; attached to `@TS_LOG_001` and `@TS_LOG_008` | **Done** |
| `TASK-005` `LoginPage` missing-controls + footer/placeholder readers | High | `FR-001` | `AD-008` / `DR-002`, `DR-003` | `eed474f` | `findMissingLoginControls()` returns `List<String>` over **seven** controls including `FOOTER_COPYRIGHT` and `FOOTER_SOCIAL_LINK`; `MISSING_CONTROLS_TIMEOUT_SECONDS = 3`; `getUsernamePlaceholder()`/`getPasswordPlaceholder()` present; the short-circuiting `areAllLoginControlsDisplayed()` is **gone**, not left dead | **Done** |
| `TASK-006` `@TS_LOG_008` asserts named controls + placeholder values | High | `FR-001` | `AD-008` / `DR-002`, `DR-003` | `eed474f` | `@TS_LOG_008` present in `login.feature`; placeholder literals supplied as Gherkin step parameters and compared with `assertEquals`; failure message names the missing controls | **Done** |
| `TASK-007` Mandatory no-credential-logging review check | High | `NFR-002` | `AD-011` / `DR-012` | `f630027` | Performed over the `TASK-001`–`TASK-006` diff and the generated reports; **no finding**; recorded in `artifacts/verification-report.md` | **Done** (re-run for this cycle's diff as `TASK-012`) |
| `TASK-008` Full-suite verification run | High | all `FR`/`NFR` | §Reliability / `DR-011`, `DR-004` | `f630027`, addendum `ea289d4` | `artifacts/verification-report.md`: 18 Surefire tests (12 scenarios + 6 unit tests), 11/12 scenarios passed, **0 undefined / 0 skipped**, no scenario double-count (`REC-002` of the prior plan). The single `@TS_LOG_009` failure is classified as the pre-existing, documented `NFR-001` presence-proxy timing limitation — **not** a regression and **not** the `DR-011` environmental pattern | **Done** (superseded for this cycle by `TASK-013`) |
| `TASK-009` Flip `FR-001`/`NFR-002` coverage claims | Medium | `FR-001`, `NFR-002` | §Traceability / `DR-002`, `DR-001` | `8ad4e1d` | Both rows read **Complete** in the re-issued `architecture.md`, each citing the task and scenario that earns it; the `NFR-002` no-logging clause is still correctly recorded as code-review-only, not implied to be automated | **Done** |

**No carried-forward task is re-opened.** Prior-cycle blockers `OQ-P001` (pom.xml +
first unit-test class) and `OQ-P002` (amending the approved `requirements.md`) were both
approved at that cycle's gate and are **closed by implementation**; they must not be
re-litigated.

## Task List

### TASK-010: Terminate the YAML frontmatter in `.claude/rules/code-quality.md`

- **Priority:** High
- **FR/NFR:** `NFR-002` (indirect — this rule is the delivery mechanism for the clause's
  sole active control)
- **Architecture section:** §SDLC Pipeline Components (`.claude/rules/code-quality.md`
  row, which carries the `DR-015` caveat); §Security Considerations (No plaintext in
  logs — Control ownership); §Recommended Architecture (layer contract, enforced against
  this rule)
- **Design review:** `DR-015` (Medium), `OQ-D004`; depends on `DR-012`'s control-ownership
  finding for its severity
- **Depends on:** None
- **Blocked by:** **`OQ-P003`** — explicit human approval, on two counts: (a) this edits
  **agent configuration** under `.claude/`, which no agent may change on another agent's
  instruction, and (b) `.claude/` is currently untracked in git, so the commit path must
  be decided. See [Blocked Tasks](#blocked-tasks).
- **Target agent:** Implementation Agent **only after the user approves `OQ-P003` in
  their own message**; otherwise the user applies the one-line edit directly and this
  task closes as `Done (user-applied)`.
- **Description:** `.claude/rules/code-quality.md` opens with a single `---` delimiter and
  never closes the block, so its `description` and `paths: - "**/*.java"` declaration do
  not sit inside well-formed YAML frontmatter — unlike `tests.md` and `sdlc-artifacts.md`,
  each of which has two delimiters (verified byte-for-byte this cycle). Add the missing
  closing `---` immediately after the `paths` list, before the blank lines preceding
  `# Code Quality Standards`. Change **nothing else**: not the description, not the path
  list, not a single standard in the body. The rule's content is already correct and
  load-bearing; only its header is malformed.
- **Expected output:** `.claude/rules/code-quality.md` with properly terminated
  frontmatter — a one-line insertion, no body change.
- **Validation:** The file's first lines parse as closed YAML frontmatter and match the
  shape of `tests.md`/`sdlc-artifacts.md`. Confirm empirically that the rule applies when
  a `**/*.java` file is read in a fresh session, and that **nothing else about its
  application changes**.
- **Evidence that refines `DR-015`'s risk, recorded for Step 6:** in this Step 4 session
  the contents of `code-quality.md` were injected as project instructions **at session
  start, before any `**/*.java` file was touched** — consistent with the unterminated
  header being unparsed and the file treated as always-on instructions rather than
  path-scoped. The practical consequence today is therefore **benign, arguably
  over-broad, not under-broad**: the rule's text *is* reaching agents, so `DR-012`'s sole
  active `NFR-002` logging control is currently being delivered. That lowers the urgency
  from "must fix before Step 5" to "should fix": the behaviour is **undesigned and
  tooling-dependent**, and a future change in how unterminated frontmatter is handled
  could silently flip it to never-loaded, removing the control with no signal. This
  observation is a single-session datapoint and should be confirmed, not assumed.
- **Standing constraint, independent of this task:** the Step 6 no-credential-logging
  check runs **unconditionally**, whether or not this rule auto-loads
  (`design-review.md` §Agreed Design Decisions). `TASK-010` must never be cited as a
  precondition for that check.

### TASK-011: `WARN` when the `allow.insecure.base.url` opt-out is engaged

- **Priority:** Medium
- **FR/NFR:** `NFR-002`
- **Architecture section:** §Technology Choices `AD-010` (trade-off column, "Opt-out
  exposure (`DR-017`)"), `AD-011` (boundary-only logging, never a credential);
  §Security Considerations (Transport bullet)
- **Design review:** `DR-017` (Low), `OQ-D005`; enables the Step 7 half of
  `design-review.md` §Agreed Design Decisions ("Step 7 should confirm it was unset for
  the run it reports")
- **Depends on:** None
- **Blocked by:** **`OQ-P004`** — explicit human approval. Step 3 accepted `DR-017` as
  documentation and deferred the code change to the user because it touches a
  security-guarded method and no `FR`/`NFR` clause mandates it. See
  [Blocked Tasks](#blocked-tasks).
- **Target agent:** Implementation Agent
- **Description:** Engaging the opt-out currently leaves **no trace**: it resolves through
  the same `-D` → environment-variable → `config.properties` precedence as any other key,
  so an `ALLOW_INSECURE_BASE_URL=true` inherited from a CI environment silently disables
  the only *preventive* `NFR-002` transport control. Make that visible:
  1. Add an SLF4J logger to `ConfigReader` (the class has **none** today; `slf4j-simple`
     2.0.16 is already a dependency, so no `pom.xml` change is needed) and emit a single
     `WARN` on the branch where `getBaseUrl()` returns early because the opt-out is `true`.
  2. The message must name the key `allow.insecure.base.url`, state that the HTTPS scheme
     guard is bypassed, and **must not interpolate the resolved `base.url`, a username, a
     password, or any other credential** (`AD-011`; this is exactly the line Step 6's
     mandatory check exists to catch). Naming the key alone is sufficient and leaks
     nothing.
  3. **Log once per JVM, not once per call.** `getBaseUrl()` is invoked on every `Given`
     step and again transitively from `getValidPassword()`, so an unguarded statement
     would emit the warning many times per run and train readers to ignore it. Use a
     single `static final AtomicBoolean` (or equivalent) latch so the warning appears
     exactly once and is unmissable.
  4. **Change no control flow.** The scheme check, the `getBoolean(…, false)` secure
     default, the exception message, `resolveScheme()`, `isDemoHost()`, and
     `getValidPassword()` must all remain behaviourally identical. This task adds an
     observation, not a decision.
  5. Extend `ConfigReaderTest` with one case asserting the opt-out branch still returns
     the URL unchanged with the logger present, so the new code path is covered rather
     than merely compiled. Honour `AD-014`/`DR-016`: capture and restore every system
     property the new case touches in `@Before`/`@After`. Do **not** assert on log output
     — that would couple a unit test to the SLF4J binding for no requirement-backed gain.
- **Expected output:** Modified `src/test/java/com/orangehrm/config/ConfigReader.java`
  (one logger field, one latched `WARN`, one `why`-style comment); modified
  `src/test/java/com/orangehrm/config/ConfigReaderTest.java` (one added case).
- **Validation:** `mvn test` passes unchanged on the committed HTTPS `base.url` with **no
  warning emitted** (the opt-out is not engaged, so the latch never fires — this is the
  important negative check). A manual
  `mvn test -Dbase.url=http://opensource-demo.orangehrmlive.com/web/index.php/auth/login -Dallow.insecure.base.url=true -Dcucumber.filter.tags="@TS_LOG_001"`
  emits the `WARN` **exactly once** in console and Surefire output, and the message
  contains no credential. All seven `ConfigReaderTest` cases pass, and the 12 Cucumber
  scenarios still run against the committed HTTPS URL afterwards (proving no property
  leaked).

### TASK-012: Mandatory no-credential-logging and guard-integrity review of this cycle's diff

- **Priority:** High
- **FR/NFR:** `NFR-002`
- **Architecture section:** §Technology Choices `AD-011`, `AD-010`, `AD-014`;
  §Security Considerations (No plaintext in logs — Control ownership; Secret management)
- **Design review:** `DR-012` (sole active control, mandatory and non-skippable),
  `DR-015` (its delivery mechanism), `DR-016` (capture/restore obligation)
- **Depends on:** `TASK-010`, `TASK-011` — whichever of the two the user approves and
  Step 5 lands. If **both** are declined, this task still runs over an empty diff (see
  Description).
- **Blocked by:** None
- **Target agent:** Review Agent
- **Description:** Re-run the mandatory `AD-011` check over **this cycle's** diff.
  `TASK-007` discharged it for the `TASK-001`–`TASK-006` diff and is `Done`; a new diff
  requires a new instance of the check, and `TASK-011` is the single highest-risk change
  this pipeline has ever scoped for that rule — it adds the **first log statement in
  `ConfigReader`**, inside the method that resolves `base.url` and is called by
  `getValidPassword()`. Specifically:
  1. Confirm no line added by `TASK-010`/`TASK-011` logs, prints, or embeds a credential,
     a field value, or a resolved password, and that no exception or log message
     interpolates one. The `TASK-011` `WARN` and any new `ConfigReaderTest` assertion
     message are the two places this can slip in.
  2. Confirm `getValidPassword()`'s demo-host guard and `getBaseUrl()`'s scheme guard are
     present and **behaviourally unmodified** — the opt-out still defaults to `false` via
     `getBoolean(…, false)`, `resolveScheme()` still fails closed on a malformed URL, and
     the early-return branch still returns only when the opt-out is explicitly `true`.
  3. Confirm `allow.insecure.base.url` is still shipped **commented out** in
     `config.properties` and was not converted to a live `false` line.
  4. Confirm any `ConfigReaderTest` case added by `TASK-011` captures and restores every
     system property it touches (`AD-014`, `DR-016`) — a leak would retarget the browser
     scenarios that follow it in the same JVM.
  5. Confirm every `src/` commit in this cycle cites its `TASK-###`. The commit gate is
     **self-policed by Step 5 and by nothing else** (`DR-014`): the hook is neither wired
     nor implemented, and `code-review`'s own skill carries no such check. This line item
     is the one compensating control this plan can place without expanding scope, and it
     is a one-line `git log` inspection — not a request to implement the hook.
  6. **If both `TASK-010` and `TASK-011` are declined**, record the check as performed
     over a no-op diff, with items 2–3 still verified against the unchanged working tree.
     The check is non-skippable regardless (`DR-012`).
- **Expected output:** Step 6 review findings with this check explicitly marked
  performed — pass or fail, never silently omitted — plus any fixes.
- **Validation:** A line-by-line diff review against `code-quality.md`'s "never
  hard-code, log, or return secrets" clause. Any credential interpolation, any weakening
  of either guard, and any uncaptured system-property mutation is a **review-blocking
  defect**. This review must not assume `hook_scan_secrets` ran — it is neither wired nor
  implemented (`DR-013`).

### TASK-013: Current-cycle verification run with opt-out confirmation

- **Priority:** High
- **FR/NFR:** `FR-001`–`FR-006`, `NFR-001`–`NFR-002`
- **Architecture section:** §Reliability and Observability (timeout table, 7-control
  figure); §Deployment (the two Surefire includes); §Risks and Mitigations (live-site row
  and its Step 7 classification rule)
- **Design review:** `DR-011` (environmental classification), `DR-004` (report the
  presence-vs-visibility proxy honestly), `DR-017` (confirm the opt-out was unset),
  `DR-018` (7 controls, not 8), `AD-014` (confirm the unit tests actually ran)
- **Depends on:** `TASK-012`
- **Blocked by:** None — **this task runs even if `TASK-010` and `TASK-011` are both
  declined.** The committed `artifacts/verification-report.md` is dated 2026-11-21 and
  cites `DR-001`–`DR-012` only; it predates the re-issued architecture (`AD-014`,
  `DR-013`–`DR-018`) and predates the Step 3 opt-out check, so this cycle has no current
  verification evidence either way.
- **Target agent:** Verify Agent
- **Description:** Run `mvn test` (optionally `-Dheadless=true`) and reconcile
  `target/cucumber-reports/` and `target/surefire-reports/` against every `FR`/`NFR`,
  applying the conventions the upstream artifacts already fixed:
  1. **Confirm `allow.insecure.base.url` was unset for the recorded run**, and state so
     explicitly in the report. Step 3 agreed this check and assigned it to Step 7
     (§Agreed Design Decisions), but **nothing currently carries it** — this line is that
     assignment. If `TASK-011` landed, the absence of its `WARN` is the positive evidence;
     if it did not, the report must say the confirmation rests on inspecting the
     invocation and environment, which is weaker. That asymmetry is the practical argument
     for approving `OQ-P004`.
  2. **Confirm both Surefire includes still execute** — `target/surefire-reports/` must
     contain a `ConfigReaderTest` result (6 cases, or 7 if `TASK-011` landed), not just
     the Cucumber suite. A silently narrowed include would drop all `NFR-002` guard
     coverage (`AD-014`, §Risks).
  3. **`DR-011` classification** — a run in which the login page never renders (the
     `Given` step or the login-page URL assertion fails, with a blank or error screenshot)
     is an **environmental** failure and must be reported as such, never as an `FR`/`NFR`
     verification failure.
  4. **Distinguish a deliberate fail-closed from an outage** — a `Given`-step failure
     carrying an `IllegalStateException` about the URL scheme is the `TASK-001` guard
     working as designed: a *configuration* failure, not an environmental one.
  5. **`DR-004`** — report `NFR-001` as covered via DOM-**presence** polling, an
     explicitly accepted proxy for `AC-007`'s "visible". If `@TS_LOG_009` fails again,
     classify it as the prior cycle did: the documented timing limitation of that proxy,
     **not** a regression and **not** environmental. Do not silently upgrade the claim to
     visibility.
  6. Confirm 12 Cucumber scenarios run exactly once each with **0 undefined and 0
     skipped** steps, and that no scenario was double-counted by the broad include.
- **Expected output:** Refreshed `artifacts/verification-report.md`, superseding the
  2026-11-21 edition and citing `TASK-010`–`TASK-013` plus `DR-001`–`DR-018`.
- **Validation:** Every `FR`/`NFR` row maps to a passing scenario or unit test, or is
  reported honestly as a gap (`tests.md`: report coverage honestly). The opt-out
  confirmation and the `ConfigReaderTest` presence check both appear explicitly. No
  "Complete" claim is made that the run's own evidence does not support.

## Parallelization Opportunities

- `TASK-010` and `TASK-011` are **fully independent** and can run concurrently once
  approved: `TASK-010` touches only `.claude/rules/code-quality.md`; `TASK-011` touches
  only `ConfigReader.java` and `ConfigReaderTest.java`. No file overlap, no shared
  prerequisite, and their approvals (`OQ-P003`, `OQ-P004`) are independent decisions —
  either can be approved without the other.
- `TASK-012` and `TASK-013` are **strictly serial tail tasks**: the diff must be reviewed
  before the suite result is published as this cycle's evidence.
- **No file contention exists in this cycle.** Unlike the prior cycle (where `TASK-004`
  and `TASK-006` both edited `login.feature` and `LoginSteps.java`), no two tasks here
  touch the same file. `login.feature`, `LoginSteps.java`, `LoginPage.java`,
  `BasePage.java`, `DriverManager.java`, `Hooks.java`, `TestRunner.java`, `pom.xml`, and
  `config.properties` are **not modified by any task in this plan**.

### Critical path

`OQ-P003` / `OQ-P004` (approval gate) → `TASK-010` ∥ `TASK-011` → `TASK-012` → `TASK-013`

If both approvals are declined, the critical path collapses to
`TASK-012` (no-op diff) → `TASK-013`, and the cycle closes with refreshed verification
evidence and no code change — a legitimate and complete outcome for a
documentation-regeneration cycle.

## Blocked Tasks

| Task | Blocked by | Unblock criteria |
| --- | --- | --- |
| `TASK-010` | **`OQ-P003`** — (a) `.claude/rules/code-quality.md` is **agent configuration**: it governs how every agent in this pipeline behaves. An agent must not edit it on another agent's instruction, so this needs the user's own approval. (b) `.claude/` is **untracked in git** (`git ls-files .claude` → 0 files, not `.gitignore`d), so there is no one-file commit path; the user must decide whether `.claude/` becomes version-controlled or the fix stays an uncommitted local edit. | User approves the edit in their own message **and** states the commit path (track `.claude/`, or apply the edit without committing). **Planner recommendation: approve the edit; treat git tracking of `.claude/` as a separate decision** — the fix is valuable whether or not it is committed, and the one-line change is independently verifiable by re-reading the file. The user may equally apply it directly in seconds, which is the lowest-friction route and is why this is also carried as `REC-003`. |
| `TASK-011` | **`OQ-P004`** — Step 3 accepted `DR-017` as documentation and explicitly deferred the code change: "the `WARN` is a recommendation, not a mandate… the documentation update alone satisfies this finding", because it touches a security-guarded method and no `FR`/`NFR` clause compels it. `code-quality.md`'s scope discipline ("implement only what an approved task requires") means the Planner will not slip a change into `ConfigReader` on an inference. | User approves at the plan-approval gate. **Planner recommendation: approve.** Three reasons, in order of weight: (1) Step 3 *already agreed* that Step 7 must confirm the opt-out was unset, and without the `WARN` that confirmation rests on inspecting the environment rather than on run evidence — the `WARN` is what makes an agreed check actually verifiable; (2) it is the only item this cycle that measurably improves a **security** control, converting a silent bypass of the primary `NFR-002` transport guard into a visible one, and it leaks nothing; (3) it is additive and behaviour-preserving, with the guard's own regression coverage already in place (`ConfigReaderTest`) to prove nothing shifted. The counter-argument — any edit to a security-guarded method carries regression risk — is real and is why the task forbids control-flow changes, requires the once-per-JVM latch, and routes the diff through `TASK-012`. **Declining is also fully acceptable**: `DR-017` is `Low`, the documentation in `AD-010` already discharges it, and `TASK-013` then records the weaker confirmation honestly. |

**No task is blocked on an unresolved design question.** `OQ-A001`–`OQ-A004` are closed;
`OQ-D002` is closed by implementation; `OQ-A006`, `OQ-D001`, and `OQ-D003` are
non-blocking items routed to the user, not tasks. `OQ-P001` and `OQ-P002` from the prior
cycle are closed by implementation. The two blockers above are **scope/approval**
questions answerable in one message at the plan-approval gate.

## Explicitly Not Tasked

Recorded so no downstream agent re-opens settled ground:

| Item | Why there is no task |
| --- | --- |
| **`DR-013`/`DR-014` — hook wiring and the self-policed `TASK-###` commit gate** | **Out of scope, confirmed by independent analysis, not merely inherited.** Both the architecture and Step 3 judged this acceptable to carry forward, and that judgement holds on its own merits: it is **pipeline tooling, not product verification**; it traces to **no `FR`/`NFR**`, so `code-quality.md`'s scope discipline forbids pulling it into `US-AUTH-002`; activating the hook is a **two-part** change (script **plus** a `settings.json` entry) in `.claude/`, which is agent configuration and untracked in git; `implement-task` already carries the gate in its own quality-gate table; and the convention is **demonstrably holding without the tool** — every prior-cycle `src/` commit cites a task id (`a950438`, `f395aff`, `51ce3b2`, `eed474f`, `f630027`, `8ad4e1d`). The PR Agent's own output gate (`hook_pr_gate.java`) is wired and implemented, and enforces changelog/PR-body/verification-report completeness. **Nothing mandatory is missing.** Rather than open a task, this plan places the cheapest possible compensating control inside existing scope: `TASK-012` item 5 makes Step 6 run a one-line `git log` check that this cycle's `src/` commits cite their `TASK-###`. Residual risk — traceability drift on a future `src/` commit — remains **explicitly accepted** (`OQ-A006`). |
| `.claude/CLAUDE.md` and `.claude/hooks/hook_implementation_task_gate.md` inaccuracies (`OQ-D001`, `OQ-D003`) | Documentation housekeeping in **agent configuration**; traces to no `FR`/`NFR`. Carried as `REC-001`/`REC-002` for the user, following the `DR-005`/`DR-007`/`DR-015` precedent. |
| Re-implementing any part of `TASK-001`–`TASK-009` | All nine are `Done` and verified present in the working tree this cycle (see the carried-forward table). Step 5 must **not** rewrite, "improve", or re-derive shipped code — `code-quality.md` scope discipline. |
| New Cucumber scenarios or `@TS_LOG_013`+ | No new `FR`/`NFR` appeared this cycle. `OQ-A001`'s accepted default (extend existing scenarios, add no tags) keeps the `requirements.md` scenario mapping stable. |
| Broader unit tests (`BasePage`, `DriverManager`, Page Objects) | Out of scope per `DR-006`/`AD-014`. The unit-test layer stays scoped to `ConfigReader`'s guard logic; `TASK-011`'s single added case is within that scope, not a precedent to widen it. |
| Surefire `<runOrder>` / isolated fork for unit tests (`DR-016`) | Deferred housekeeping. The current `@After` restore is correct, so there is no live defect; the `pom.xml` change traces to no `FR`/`NFR`. Recorded as a **constraint on future tests** instead, and enforced by `TASK-012` item 4. |
| `maven.compiler.release` vs `source`/`target` (`DR-010`) | Accepted as documentation-only; the `pom.xml` change is deferred housekeeping outside `US-AUTH-002`. |
| CI/CD pipeline definition (`OQ-A003`); parallel execution (`OQ-A004`) | Out of scope by standing user decision. Their absence is **not a gap** and must not be raised as a finding. |
| Full transport inspection (certificate validity, mixed content) | Deferred; needs a proxy for no requirement-backed benefit. `TASK-001` + `TASK-004` cover the credible risk. |
| Removing the plaintext demo credentials from `config.properties` | A documented, scoped exception, valid while the target is the public demo host. No task may "fix" it; `getValidPassword()`'s host guard is the control. |
| `DashboardPage.isUserLoggedIn()` being unreferenced | Below the threshold for a task (Step 3 declined to raise it). Step 6 may take or leave it as dead-code housekeeping. |

## Recommendations for the User (not tasks)

- **`REC-001` (`OQ-D001`; `DR-005`, `DR-007`, `DR-013`) — four inaccuracies in
  `.claude/CLAUDE.md`.** It is agent configuration, outside what any agent in this
  pipeline should edit on an agent's instruction. (a) §Optional runtime overrides
  documents `-DbaseUrl=…`; the resolved key is **`base.url`**, so `-DbaseUrl=` is
  silently ignored and the run continues against the committed demo URL — which matters
  because an operator would also believe they had exercised the `TASK-001` scheme guard.
  (b) §Rules claims `tests.md` does not match this repo's test sources; its real
  frontmatter includes `**/*.java` and it **does** auto-apply. (c) Its Hooks section
  states every hook command is guarded with `[ -f … ] && java …`; only
  `hook_pr_gate.java` is wired. (d) **New this cycle:** it describes itself as "checked
  into the codebase", but `.claude/` is entirely **untracked** in git (0 tracked files,
  not ignored).
- **`REC-002` (`OQ-D003`; `DR-013`) — `.claude/hooks/hook_implementation_task_gate.md`
  asserts its own `settings.json` wiring, which does not exist.** If the hook is ever
  implemented, add the `settings.json` entry in the **same** change — a script alone
  leaves the gate inert.
- **`REC-003` — the `TASK-010` edit is a seconds-long change the user can apply
  directly.** If you would rather not route an agent at `.claude/`, apply the closing
  `---` yourself and tell Step 5 to close `TASK-010` as `Done (user-applied)`. The task
  exists to keep the item owned and traceable, not to insist an agent perform it.
- **`REC-004` — decide whether `.claude/` should be version-controlled.** It currently
  holds the agents, skills, rules, hooks, and settings that define this pipeline, all
  untracked. Nothing in this plan depends on the answer, but every future change to
  pipeline configuration is unreviewable and unrevertable until it is settled.

## Risks and Open Questions

| # | Risk / question | Impact | Mitigation |
| --- | --- | --- | --- |
| 1 | **`OQ-P003`** — is the `.claude/rules/code-quality.md` frontmatter fix approved, and by which commit path? | Blocks `TASK-010`. Residual while unfixed: the rule under `DR-012`'s sole active `NFR-002` logging control is scoped by accident rather than by design | Resolve at the plan-approval gate. Mitigated today by the session evidence in `TASK-010` (the rule's text **is** reaching agents, over-broadly rather than not at all) and by the standing rule that the Step 6 check runs unconditionally. Planner recommends approving the edit |
| 2 | **`OQ-P004`** — is the `allow.insecure.base.url` `WARN` approved? | Blocks `TASK-011`. If declined, `TASK-013`'s opt-out confirmation rests on inspecting the environment rather than on run evidence | Resolve at the plan-approval gate; Planner recommends approval. Either way `TASK-013` item 1 reports the confirmation's actual strength honestly |
| 3 | `TASK-011` edits `ConfigReader.getBaseUrl()` — the single most security-sensitive method in the repository | High if the guard is weakened; the opt-out branch is exactly where a regression would be invisible on the happy path | Task forbids any control-flow change; `ConfigReaderTest`'s existing 6 cases already pin both guards' positive **and** negative paths and will fail on a regression; `TASK-012` items 2–3 re-verify the guards by inspection; `TASK-013` item 2 confirms the unit tests actually executed |
| 4 | `TASK-011`'s `WARN` is the **first log statement in `ConfigReader`**, in a method that resolves `base.url` and is called by `getValidPassword()` | High — a credential interpolated here would breach `NFR-002`'s no-plaintext-in-logs clause at its most sensitive point | The task mandates naming only the key, never a value; `AD-011` makes credential logging a review-blocking defect; `TASK-012` item 1 targets this line specifically. Note `hook_scan_secrets` is **neither wired nor implemented** and must not be assumed to help (`DR-012`, `DR-013`) |
| 5 | A naive `WARN` fires on every `getBaseUrl()` call, producing many lines per run and training readers to ignore it | Medium — alert fatigue would defeat the purpose of the task | Once-per-JVM latch required by `TASK-011` item 3; `TASK-011`'s validation asserts "exactly once" |
| 6 | Live demo site down, slow, or data-reset during `TASK-013` | High likelihood, high impact | `DR-011` classification rule applied as `TASK-013` item 3, with item 4 distinguishing a deliberate fail-closed from an outage. The prior cycle already exercised this path once (`ea289d4`) |
| 7 | `@TS_LOG_009` fails again on a fast auth response (the documented `NFR-001` presence-proxy limitation) | Medium — risks being misreported as a regression caused by this cycle | No task touches `@TS_LOG_009`, `LoginPage`, or `LoginSteps`, so a failure **cannot** be caused by this plan. `TASK-013` item 5 mandates the same classification the prior cycle recorded |
| 8 | Agents treat this restart as a licence to re-implement shipped code | Medium — churn, regression risk, and lost verified behaviour for zero requirement gain | The carried-forward table states all nine tasks are `Done` **with working-tree evidence**; [Explicitly Not Tasked](#explicitly-not-tasked) forbids re-implementation; `code-quality.md` scope discipline binds Step 5 |
| 9 | A future `**/*Test.java` class mutates shared system properties without capture/restore, leaking a retargeted `base.url` or an engaged opt-out into the browser scenarios that follow (`DR-016`) | Medium, forward-looking — no live defect today | The pattern is **mandatory** for any class matched by the include (`AD-014`); `TASK-011` item 5 binds its new case; `TASK-012` item 4 verifies it; `TASK-013` item 6 confirms the 12 scenarios still ran against the committed HTTPS URL |
| 10 | The Step 5 `TASK-###` commit gate is self-policed, with no wired hook and no downstream skill check (`DR-014`) | Medium — traceability drift on a future `src/` commit | **Explicitly accepted** (`OQ-A006`), with the cheapest in-scope compensating control added as `TASK-012` item 5. Evidence the convention holds unaided: all six prior-cycle `src/` commits cite a task id |

## Recommended First Task for Implementation Agent

**`TASK-010`** — Terminate the YAML frontmatter in `.claude/rules/code-quality.md` —
**but only once the user has approved `OQ-P003` in their own message.** It is a one-line
change, depends on nothing, carries no regression risk to any test, and sits under the
sole active control for an `NFR-002` clause.

Start `TASK-011` in parallel if `OQ-P004` is also approved.

**If both approvals are declined, the first task is `TASK-012`** (Step 6 check over a
no-op diff), followed by `TASK-013`. The cycle then closes with refreshed verification
evidence and zero code change — a complete and legitimate outcome, since this restart is
regenerating pipeline documentation over an already-verified codebase.

## Traceability Matrix

| FR/NFR | Architecture | DR | TASK | Test / evidence | Status |
| --- | --- | --- | --- | --- | --- |
| `FR-001` | `AD-008`, `LoginPage.findMissingLoginControls` | `DR-002`, `DR-003`, `DR-018` | `TASK-005`, `TASK-006`, `TASK-009` (all `Done`); re-verified by `TASK-013` | `@TS_LOG_008` — 7 controls + both placeholder values | **Complete** |
| `FR-002` | `LoginPage.getPasswordFieldType` | — | *No task — shipped* | `@TS_LOG_007` | Complete |
| `FR-003` | `LoginPage.login` / `submitWithEnterKey`, `DashboardPage` | — | *No task — shipped* | `@TS_LOG_001`, `@TS_LOG_012` | Complete |
| `FR-004` | `LoginPage.getErrorBannerText` | — | *No task — shipped* | `@TS_LOG_002`, `@TS_LOG_003`, `@TS_LOG_011` | Complete |
| `FR-005` | Field-scoped required-error locators | — | *No task — shipped* | `@TS_LOG_004`, `@TS_LOG_005`, `@TS_LOG_006` | Complete |
| `FR-006` | `ResetPasswordPage` | — | *No task — shipped* | `@TS_LOG_010` | Complete |
| `NFR-001` | `LoginPage.submitAndCaptureLoadingIndicator` → `BasePage.isPresent` | `DR-004` | `TASK-013` (report the proxy honestly) | `@TS_LOG_009` | Complete via a **documented presence-for-visibility proxy**; known timing-sensitive failure |
| `NFR-002` | `AD-009`, `AD-010`, `AD-011`, `AD-014` | `DR-001`, `DR-008`, `DR-012`, `DR-015`, `DR-016`, `DR-017` | `TASK-001`–`TASK-004`, `TASK-007`, `TASK-009` (`Done`); **`TASK-010`**, **`TASK-011`**, `TASK-012`, `TASK-013` (this cycle) | `ConfigReaderTest` (6 cases), `@TS_LOG_001`, `@TS_LOG_002`, `@TS_LOG_003`, `@TS_LOG_007`, `@TS_LOG_008`, `@TS_LOG_011` | **Complete**; `TASK-010`/`TASK-011` harden the clause's **delivery mechanism and observability**, not its coverage. The no-logging clause remains **code-review-only** (`TASK-012`) — not automated |

**No orphans, in either direction.**

- Every `TASK-010`–`TASK-013` cites at least one `FR`/`NFR`, an architecture section, and
  a `DR-###`.
- Every `FR`/`NFR` is already covered by shipped, verified code; **no requirement is
  waiting on a task**, which is why this cycle opens only four and why Step 3 correctly
  concluded no new `TASK-###` was *strictly* required.
- Every new finding in this cycle (`DR-013`–`DR-018`) is accounted for: `DR-015` →
  `TASK-010`; `DR-017` → `TASK-011`; `DR-016` → `TASK-011` item 5 and `TASK-012` item 4;
  `DR-011`/`DR-018` → `TASK-013`; `DR-013`/`DR-014` → explicitly not tasked, with
  `TASK-012` item 5 as the in-scope compensating control.
- `FR-002`–`FR-006` and `NFR-001` correctly generate **no** implementation task. This
  plan adds no work that is not traceable to a finding.
