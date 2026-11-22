# Design Review

**Story:** `US-AUTH-002` — Customer Login & Authentication
**Document under review:** `artifacts/architecture.md` (14 decisions `AD-001`–`AD-014`), commit `341c185`
**Review date:** 2026-11-22
**Reviewer:** Design Review Agent (Step 3)
**Cycle:** Full pipeline restart. `DR-001`–`DR-012` are carried forward unchanged from the
prior cycle per `sdlc-traceability` ("assign IDs once and never reuse or renumber them");
`DR-013`–`DR-018` are new in this review.

## Review Summary

The architecture is **sound and ready for planning**. There are **no `Critical` and no
`High`** findings in this cycle. Every `FR-001`–`FR-006` and `NFR-001`–`NFR-002` has a
named verifying component, every component traces to at least one requirement, and the
four-layer contract (Gherkin → steps → Page Objects → WebDriver, plus `DriverManager` /
`ConfigReader`) is described accurately.

**The prior cycle's findings were verified, not assumed.** The architecture asserts that
`DR-001`–`DR-012` are already resolved and reflected in the current design. That claim was
checked line by line against the real code rather than taken on trust, and it holds for all
twelve — see [Carried-forward findings: verification](#carried-forward-findings-verification).
The load-bearing one, `DR-001`, is genuinely closed: `ConfigReader.getBaseUrl()` really does
fail closed on a non-`https` scheme behind a default-`false` `allow.insecure.base.url`
opt-out, the opt-out really is shipped commented out in `config.properties`, and
`ConfigReaderTest` really does pin the `http`, absent-opt-out, malformed-URL, and
host-guard-regression paths.

Three themes drove the six new findings, and **all three are about the SDLC pipeline's own
integrity rather than about the test framework**, which is a measure of how settled the
framework design now is.

**1. The hook story is still one step wrong, in a new direction.** The re-issue correctly
reversed the prior document's claim that `hook_implementation_task_gate.java` was
implemented. But it then states — twice — that the seven unimplemented hooks are *wired in
`.claude/settings.json`* and no-op only because their scripts are missing. They are not
wired at all: `settings.json` contains exactly one hook entry (`Bash(gh pr create*)` →
`hook_pr_gate.java`), its `PostToolUse` array is empty, its `Stop` entry has an empty
`hooks` array, and there is no `Bash(git commit*)` matcher anywhere. `.claude/hooks/
hook_implementation_task_gate.md` repeats the same false claim about itself (`DR-013`).

**2. The compensating control for the missing commit gate does not exist where it would
have to run.** The architecture's risk row mitigates the stub gate with "the Review (Step 6)
and PR (Step 8) agents must check commit messages for `TASK-###` manually." Neither
`.claude/skills/code-review/SKILL.md` nor `.claude/skills/create-pr/SKILL.md` contains any
such check, and each agent delegates its entire workflow to its skill. The gate therefore
lives only in `implement-task`'s own quality-gate table — self-policing by the agent that
writes the commit, with no independent verification (`DR-014`). This is the substance of
`OQ-A006`: deferring the *hook* is right, but the mitigation as written is aspirational.

**3. A rule file the architecture leans on is malformed.** `.claude/rules/code-quality.md`
has an **unterminated** YAML frontmatter block — one `---` delimiter where `tests.md` and
`sdlc-artifacts.md` each have two — so its declared `**/*.java` path scoping is not in a
well-formed block. The architecture cites that rule as the enforcement mechanism for the
layer contract, for `AD-010`'s fail-closed principle, and (per `DR-012`) as the **sole
active control** for `NFR-002`'s no-plaintext-in-logs clause. A one-line defect sits under
the last remaining control for a security requirement (`DR-015`).

The remaining three new findings are `Low`: a future-regression hazard in the new
`AD-014` unit-test layer (`DR-016`), the silent, unlogged nature of the
`allow.insecure.base.url` bypass (`DR-017`), and one stale number in the timeout table
(`DR-018`).

**`AD-014` is accepted as designed.** Its justification is the strongest kind available:
the negative paths of the only security control implemented *in this repository* are
structurally untestable through Gherkin — no Cucumber scenario can assert that an `http://`
`base.url` *throws*, because the suite would fail to start. It is correctly traceable
(`NFR-002` → `AD-014` → `TASK-002`/`TASK-003`, both present and `Done` in
`artifacts/impl-plan.md`), correctly scoped (6 tests, one class under test, no browser, no
network), and it narrows `DR-006` through that note's own escape clause ("unless an approved
`TASK-###` introduces one") rather than contradicting it. It introduces one risk the
architect already booked (a narrowed Surefire include silently dropping the coverage) and
one he did not (`DR-016`).

**`OQ-A006` is accepted as a carried-forward risk**, with the two corrections above. The
convention is demonstrably holding without the tool: every prior-cycle `src/` commit cites
a task id (for example `eed474f feat(TASK-004,TASK-005,TASK-006): …`).

Verified as correct and requiring no change this cycle: the `NFR-001` same-call poll design
(3s timeout, 50ms interval, started inside `submitAndCaptureLoadingIndicator`); `AD-013`'s
`peekDriver()` reasoning and the `try`/`finally` ordering in `Hooks.tearDown`;
`DriverManager`'s half-initialised-browser cleanup and the deliberate absence of any
implicit wait; the `FR-004` non-enumeration argument (all three negative scenarios
`assertEquals` the literal `"Invalid credentials"`); `FR-002`'s `getDomAttribute("type")`
masking check; `AD-009`'s `https://` **prefix** match via `regionMatches` rather than a
substring; the two Surefire includes with `testFailureIgnore=false`; `DR-005`'s correction
(`tests.md`'s frontmatter does include `**/*.java`, confirmed by direct inspection); and
the `TestRunner` glue/plugin configuration.

## Review Inputs

| Input | Version / commit | Status |
| --- | --- | --- |
| `artifacts/architecture.md` | commit `341c185` "docs: define architecture for US-AUTH-002 login authentication" | Approved by user (Step 2 gate); `AD-001`–`AD-013` carried forward, `AD-014` new |
| `artifacts/requirements.md` | commit `62c4000` "docs: capture requirements for US-AUTH-002 login functionality (pipeline restart)" (6 `FR`, 2 `NFR`) | Approved |
| `artifacts/impl-plan.md` | working tree (prior cycle, `TASK-001`–`TASK-009` all `Done`) | Read only to verify `AD-014`'s `TASK-002`/`TASK-003` citation |
| `artifacts/design-review.md` (prior cycle) | git history (`DR-001`–`DR-012`) | Superseded by this document; IDs preserved |
| `.claude/rules/code-quality.md`, `tests.md`, `sdlc-artifacts.md` | working tree | Review baseline; frontmatter inspected byte-for-byte (`DR-005`, `DR-015`) |
| `.claude/settings.json`, `.claude/hooks/**`, `.claude/skills/**` | working tree | Read to verify hook wiring and the `OQ-A006` compensating control (`DR-013`, `DR-014`) |
| `src/test/java/**`, `src/test/resources/**`, `pom.xml` | working tree (unchanged since the prior cycle) | Read in full to verify the architecture's claims rather than accept them |

Every claim in the architecture about existing code, build configuration, rules, or hooks
was checked against the artefact itself. Findings cite the architecture section or `AD-###`
under review.

## Findings

| ID | Severity | Area | Finding | Impact | Recommendation | Decision | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `DR-001` | **High** | Security and access control | (§Technology Choices `AD-009`/`AD-010`, §Security Considerations; `NFR-002`) `AD-009` only asserts the scheme of an already-loaded URL. No component validated that `base.url` is HTTPS, and `getValidPassword()`'s host guard is skipped whenever a `valid.password` override is supplied — the sanctioned CI path. A `http://` target plus an injected real credential submits that credential in plaintext before any assertion runs. | `NFR-002`'s "never transmit in plain text" clause and `code-quality.md`'s "use HTTPS" / "fail closed on security-relevant errors" are breached in the exact deployment mode the Deployment section recommends. `AD-009` detects the breach only after disclosure. | Make the control preventive: `ConfigReader.getBaseUrl()` throws unless the resolved URL's scheme is `https`, gated by an explicit, default-`false` opt-out (e.g. `allow.insecure.base.url`) for a deliberate non-TLS sandbox. Keep `AD-009`'s executed assertion as defence in depth against a mid-session downgrade. | **Accepted.** Preventive scheme validation added to `ConfigReader`; `AD-009` retained and re-scoped as defence in depth. | **Resolved — verified in code this cycle** (`ConfigReader.getBaseUrl()`, `AD-010`, `TASK-001`) |
| `DR-002` | Medium | Requirements alignment | (§Technology Choices `AD-008`, §Risks, §Traceability `FR-001`) `AD-008` plus the risk mitigation "assert presence only, not text or link targets" would assert a footer *container* and the mere existence of a `placeholder` attribute, while `FR-001`/`AC-001` name the copyright, the social links, and placeholder text. | `FR-001` would be reported "Complete" while its named sub-elements are unverified — an overstated coverage claim that `sdlc-artifacts.md` forbids and that Step 7 would propagate. | Assert the footer copyright element and at least one social-link anchor as distinct controls, and compare both `placeholder` attributes to expected literals supplied as Gherkin step parameters. If container-only is accepted for brittleness reasons, `FR-001` must stay "Partial" with the residual named. | **Accepted.** Assert copyright + ≥1 social link + both placeholder values; expected text lives in Gherkin, not in the Page Object. | **Resolved — verified in code this cycle** (`FOOTER_COPYRIGHT`, `FOOTER_SOCIAL_LINK`, `@TS_LOG_008` placeholder steps) |
| `DR-003` | Medium | Testability and maintainability | (§Technology Choices `AD-008`; the former `LoginPage.areAllLoginControlsDisplayed`) The method `&&`-chained five `isDisplayed(…, 10)` calls behind one `assertTrue`; `AD-008` added three more terms. Short-circuiting means the first missing control masks the rest, and a page that fails to load costs stacked timeouts. | An `FR-001` failure reports only "some control is missing", forcing manual reproduction; suite wall-clock degrades sharply on the most likely failure mode (demo site down). | Have `LoginPage` return the collection of missing controls and let `LoginSteps` assert on it, so the report names the missing control. Use one short timeout for the group after the page is already confirmed loaded. | **Accepted.** Boolean chain replaced with a missing-controls collection asserted in `LoginSteps`. | **Resolved — verified in code this cycle** (`findMissingLoginControls`, `MISSING_CONTROLS_TIMEOUT_SECONDS = 3`) |
| `DR-004` | Medium | Requirements alignment | (§Traceability `NFR-001`, §Reliability timeout table; `BasePage.isPresent`) `AC-007` requires the indicator to be **visible**; `isPresent` polls for DOM *presence* only, while the traceability table records `NFR-001` as "Complete". | Presence is weaker than visibility — a hidden-but-present node would satisfy the test. The coverage claim overstates what is verified. | Either poll on visibility with the same fast interval, or keep presence as a deliberate proxy (justified by the indicator's sub-100ms lifetime) and say so in the `NFR-001` coverage note. | **Accepted.** Presence-based polling kept and documented as an explicit accepted proxy. | **Resolved — verified in doc and code this cycle** (`NFR-001` row states the proxy; `isPresent(…, 3s, 50ms)`) |
| `DR-005` | Medium | SDLC pipeline coverage | (§SDLC Pipeline Components, §Risks) The prior architecture stated twice that `.claude/rules/tests.md` does not match `src/test/java/**` and does not auto-apply. Its actual frontmatter is `test/**`, `tests/**`, `**/__tests__/**`, `**/*.java` — the last pattern matches every Java file here, so it **does** auto-apply. | A false constraint tells Steps 5 and 6 to apply test standards manually and books a risk that does not exist. Inherited from a stale `.claude/CLAUDE.md` note. | Correct both rows in `architecture.md`; delete the phantom risk row. `.claude/CLAUDE.md` needs the same correction — flagged for the user, not edited by this agent. | **Accepted.** `architecture.md` corrected; `CLAUDE.md` inaccuracy raised as `OQ-D001`. | **Resolved — re-verified this cycle** (frontmatter inspected directly; `tests.md` auto-applied in this session after reading a `.java` file) |
| `DR-006` | Medium | SDLC pipeline coverage | (follows from `DR-005`) Because `tests.md` does auto-apply, its "provide **unit tests** … with external dependencies mocked" and "provide **integration tests**" clauses bind the Implementation Agent, but no `FR`/`NFR` and no architecture component defined a unit-test layer. | Step 5 may add out-of-scope unit tests (or Step 6 may flag their absence as a defect), contradicting `code-quality.md`'s scope discipline and `AD-004`. | State the precedence in `architecture.md`: `tests.md`'s coverage, determinism, and quality clauses apply in full; its structure clause is satisfied by the Gherkin + Page Object layering, and no unit/mock layer is in scope for `US-AUTH-002` unless an approved `TASK-###` introduces one. | **Accepted.** Precedence note recorded so Steps 5 and 6 read it the same way. | **Resolved — verified this cycle**, and correctly narrowed by `AD-014` via this note's own escape clause (`TASK-002`/`TASK-003`, approved under `OQ-P001`) |
| `DR-007` | Medium | Deployment and CI/CD | (§Deployment) `.claude/CLAUDE.md` documents the override as `-DbaseUrl=…`, but `ConfigReader` resolves the key `base.url`. `-DbaseUrl=` is silently ignored and the run continues against the committed demo URL; the Deployment section never named the exact keys. | An operator retargeting the suite believes they changed the target while the run still hits the public demo — a silent misconfiguration that also hides `DR-001`'s scheme problem. | Name exact keys in the Deployment section. Correct `-DbaseUrl` in `.claude/CLAUDE.md` — flagged for the user, not edited by this agent. | **Accepted.** Exact keys spelled out in `architecture.md`; `CLAUDE.md` correction raised as `OQ-D001`. | **Resolved in `architecture.md`** — `CLAUDE.md` still wrong; `OQ-D001` remains open |
| `DR-008` | Medium | Security and access control | (§Technology Choices `AD-013`, §Security Considerations) The screenshot trade-off reasoned only about the password. The **username** renders in plaintext and is embedded in `cucumber.json`/HTML and published as a CI build artifact, while Deployment actively recommends injecting real credentials via `VALID_USERNAME` for a non-demo target. | Harmless for the public `Admin` demo account, but in the recommended non-demo mode a real username is persisted into a published artifact. The security analysis did not name this channel. | Document the exposure as accepted and scoped: a non-issue for the public demo; when `base.url` targets a non-demo environment, `target/cucumber-reports/` must be treated as sensitive and not published to an untrusted artifact store. | **Accepted.** Disclosure channel and scoped mitigation added to Security Considerations. | **Resolved — verified in doc this cycle** |
| `DR-009` | Low | Components and responsibilities | (§Recommended Architecture, layer contract) The contract said step definitions "must never call Selenium APIs directly", but `LoginSteps` legitimately holds a `WebDriver` field solely to construct Page Objects. As written, the contract prohibited the existing, intended design. | Step 6 could false-positive on `LoginSteps`' `WebDriver` field, or dismiss the rule as inaccurate and miss a genuine violation. | Tighten to: step definitions may hold a `WebDriver` reference only to construct Page Objects, and must never invoke `WebDriver`/`WebElement`/`By`/wait APIs on it. | **Accepted.** Layer-contract row reworded. | **Resolved — verified in doc and code this cycle** (`LoginSteps.driver` is passed only to the three Page Object constructors) |
| `DR-010` | Low | Technology choices | (§Deployment, §Assumptions) The prior document said "sources compile at release 11", but `pom.xml` sets `maven.compiler.source`/`target` to 11, not `release`, so code referencing a post-11 API compiles cleanly. | Low practical impact — the documented prerequisite is Java 21+ — but the stated guarantee is not the one the build provides. | Either switch to `maven.compiler.release=11` or restate the claim as "targets 11 bytecode without API-level enforcement". | **Accepted as documentation-only.** Claim restated; a `pom.xml` change is out of scope for `US-AUTH-002`. | **Resolved — verified in doc and `pom.xml` this cycle** |
| `DR-011` | Low | Observability and operations | (§Risks row 1, §Reliability) The dominant risk — the live third-party demo site being down, slow, or data-reset — is correctly rated High/High, but no convention told Step 7 how to classify such a run. | The Verify Agent could report an environmental outage as an `FR`/`NFR` verification failure, corrupting `artifacts/verification-report.md`. | Add the classification rule: a failure where the login page itself never renders (the `Given` step or the login-page URL assertion fails, with a blank/error screenshot) is environmental and must be reported as such. | **Accepted.** Step 7 classification convention added. | **Resolved — verified in doc this cycle**; already exercised once in the prior cycle (`ea289d4`) |
| `DR-012` | Low | Security and access control | (§SDLC Pipeline Components, §Risks; `AD-011`) The mitigation for "a future change logs a credential" leaned partly on "the intended `hook_scan_secrets` hook would add automation once implemented". That hook does not exist, so the only active control is the Step 6 review. | Over time the unimplemented hook can be mistaken for an operating control, leaving `AD-011` effectively unenforced. | State plainly that the Step 6 no-credential-logging check is the sole active control for `AD-011` and is mandatory and non-skippable while the hook is absent. | **Accepted.** Control ownership made explicit. | **Resolved — verified in doc this cycle**; note `DR-015`, which weakens the delivery mechanism of this same control |
| `DR-013` | Medium | SDLC pipeline coverage | (§SDLC Pipeline Components hook rows, §Assumptions and Constraints final bullet; new in this cycle) The architecture states that `hook_implementation_task_gate.java` and the six other unimplemented hooks are **"wired in `.claude/settings.json`"** and no-op only because their scripts are missing. `.claude/settings.json` actually contains **one** hook entry — `PreToolUse` / `Bash(gh pr create*)` → `hook_pr_gate.java`. Its `PostToolUse` array is empty, its `Stop` entry holds an empty `hooks` array, and there is no `Bash(git commit*)` matcher at all. `permissions.allow` permits `Bash(git commit*)` unconditionally. `.claude/hooks/hook_implementation_task_gate.md` repeats the same false claim about its own wiring. | Two concrete consequences. (a) Activating the Step 5 gate is a **two-part** change (add the script *and* add the `settings.json` entry), not the one-file drop the document implies — any future task scoped from this description would be incomplete and would silently leave the gate inert. (b) It propagates the wrong mental model of the `[ -f … ] && java …` guard pattern, which exists for exactly one hook. | Correct both hook rows and the Assumptions bullet to "**not wired and not implemented**", and state that only `hook_pr_gate.java` is both wired and implemented. Note that `.claude/hooks/hook_implementation_task_gate.md` carries the same error and is project configuration this agent does not edit. | **Accepted.** `architecture.md` corrected; the `.md` design-note correction raised to the user as `OQ-D003`. | Resolved — architecture updated by this review |
| `DR-014` | Medium | SDLC pipeline coverage | (§Risks row 3, §Open Questions `OQ-A006`; new in this cycle) The mitigation for the stub commit gate reads "the Review (Step 6) and PR (Step 8) agents must check commit messages for `TASK-###` manually." Neither `.claude/skills/code-review/SKILL.md` nor `.claude/skills/create-pr/SKILL.md` contains any commit-message or `TASK-###` check, and per `.claude/CLAUDE.md` each agent file is a thin invoker whose skill is the source of truth. The gate therefore exists only in `implement-task`'s own quality-gate table — self-policing by the agent that writes the commit, with no independent check anywhere. | The architecture books a mitigation that no agent will execute, which is worse than booking none: Step 7 and Step 8 would both assume the check happened. A `src/` commit with no task reference can reach the PR undetected, breaking the `sdlc-traceability` chain at its only unautomated link. | State the residual risk honestly: with the hook absent **and** no downstream skill carrying the check, the `TASK-###` commit convention is self-policed by Step 5 alone. Either accept that explicitly, or raise a separate, scoped change to add the check to `code-review` and `create-pr` — which is pipeline tooling, traces to no `FR`/`NFR`, and should not become a `TASK-###` under this story. | **Accepted — risk carried forward with an honest mitigation.** The risk row and `OQ-A006` are rewritten to say the convention is self-policed by Step 5; adding the check to the two downstream skills is recommended to the user as separate pipeline work (`OQ-A006`). Supporting evidence that the convention holds: every prior-cycle `src/` commit cites a task id (e.g. `eed474f`). | Resolved — architecture updated by this review; residual risk explicitly accepted |
| `DR-015` | Medium | SDLC pipeline coverage / Security and access control | (§SDLC Pipeline Components `code-quality.md` row, §Recommended Architecture layer contract, §Security Considerations; new in this cycle) `.claude/rules/code-quality.md` has an **unterminated** YAML frontmatter block: one `---` delimiter, where `tests.md` and `sdlc-artifacts.md` each have two. Its `paths: - "**/*.java"` declaration is therefore not inside a closed block, so the path scoping the architecture attributes to it is not well-formed. | The architecture treats this rule as load-bearing in three places: it "enforces" the layer contract, it supplies `AD-010`'s "fail closed on security-relevant errors" principle, and — per `DR-012` — the Step 6 review against it is the **sole active control** for `NFR-002`'s no-plaintext-in-logs clause. A malformed header makes the rule's auto-application accidental rather than designed, and it is the one control with no backup (`hook_scan_secrets` does not exist and, per `DR-013`, is not even wired). | Add the missing closing `---` to `.claude/rules/code-quality.md`. Until then, annotate the `code-quality.md` row in `architecture.md` so no downstream agent assumes the rule is reliably auto-applied, and keep the Step 6 credential-logging check mandatory regardless of whether the rule loads. | **Accepted.** `architecture.md` annotated by this review. The rule file is project configuration under `.claude/` and is **not** edited by this agent — the one-line fix is raised to the user as `OQ-D004`, following the `DR-005`/`DR-007` precedent for `CLAUDE.md`. | Resolved in `architecture.md` — rule-file fix pending user action (`OQ-D004`) |
| `DR-016` | Low | Reliability and fault tolerance | (§Technology Choices `AD-014`, §Scalability, §Risks; new in this cycle) `ConfigReaderTest` mutates the `base.url`, `allow.insecure.base.url`, and `valid.password` **system properties** in the same Surefire JVM as the Cucumber suite, relying solely on its own `@Before`/`@After` capture-and-restore. `pom.xml` pins no `runOrder`, `forkCount`, or `reuseForks`, and the include is the broad `**/*Test.java`. | Correct today — the restore is per-method and complete. The hazard is forward-looking: any future class matching `**/*Test.java` that mutates these keys without the same discipline would leak a retargeted `base.url`, or an `allow.insecure.base.url=true`, into every browser scenario that runs afterwards — silently disabling the `AD-010` guard for them. The architecture books the opposite risk (the include being narrowed back) but not this one. | Record in `AD-014`'s trade-off column and a risk row that the capture/restore pattern is **mandatory** for any class matched by the include, and that the include's breadth is a known cost of the approach. Pinning `<runOrder>` or giving unit tests their own fork is the durable fix and is deferred housekeeping, not in scope for `US-AUTH-002`. | **Accepted as documentation.** `AD-014` note and a new risk row added; no `pom.xml` change this cycle. | Resolved — architecture updated by this review |
| `DR-017` | Low | Observability and operations / Security | (§Technology Choices `AD-010`, §Security Considerations Transport bullet; new in this cycle) Engaging `allow.insecure.base.url` is **silent**. It resolves through the same `-D` → environment-variable → `config.properties` precedence as every other key, so an `ALLOW_INSECURE_BASE_URL=true` inherited from a CI environment disables the only *preventive* `NFR-002` transport control with no log line, no report entry, and nothing for the Verify Agent to notice. The architecture documents the opt-out as default-`false` and as an escape hatch, but not that its engagement leaves no trace. | A deliberately scoped escape hatch becomes an undetectable one. `AD-009`'s executed assertion would still fail on an `http://` page, so the breach is not invisible end-to-end — but the *reason* (a bypassed guard rather than a mid-session downgrade) would not be distinguishable from the evidence, which is exactly the confusion `DR-011` exists to prevent. | Document the environment-variable path and the silence in `AD-010` and the Transport bullet. Recommended cheap hardening: have `getBaseUrl()` emit a single SLF4J `WARN` naming the opt-out when it is engaged — consistent with `AD-011`'s boundary-only logging policy and leaking no secret — and have Step 7 confirm the opt-out was unset for the recorded run. | **Accepted as documentation; the `WARN` is a recommendation, not a mandate.** The architecture is updated to record the exposure. Whether the one-line `WARN` becomes a `TASK-###` in this cycle is left to the user (`OQ-D005`) rather than silently added, since it is a code change to a security-guarded method. | Resolved in `architecture.md` — optional code change deferred to `OQ-D005` |
| `DR-018` | Low | Observability and operations | (§Reliability and Observability timeout table; new in this cycle) The table states that `findMissingLoginControls` costs "~8×3s rather than 8×15s". The method probes **seven** controls — logo, username field, password field, Login button, forgot-password link, footer copyright, footer social link — matching the seven named in the `FR-001` traceability row. | Trivial in isolation, but Step 7 quotes these figures when budgeting and explaining run duration, and the document contradicts its own `FR-001` row. | Change to "~7×3s rather than 7×15s". | **Accepted, documentation-only.** | Resolved — architecture updated by this review |

### Carried-forward findings: verification

The architecture asserts `DR-001`–`DR-012` are resolved. Each was re-checked against the
current document *and* the real `src/`, `pom.xml`, `.claude/rules/`, and `.claude/settings.json`:

| ID | Claim | Evidence checked | Verdict |
| --- | --- | --- | --- |
| `DR-001` | `AD-010` adds a fail-closed HTTPS scheme guard | `ConfigReader.getBaseUrl()` resolves `allow.insecure.base.url` (default `false`) then throws `IllegalStateException` unless the scheme equals `https`; `resolveScheme` returns `null` for a malformed URL so that also fails closed; opt-out shipped commented out in `config.properties`; `getValidPassword()`/`isDemoHost` unchanged and still reached through `getBaseUrl()` | **Confirmed resolved** |
| `DR-002` | Footer + placeholder coverage | `FOOTER_COPYRIGHT`, `FOOTER_SOCIAL_LINK` probed as distinct controls; `@TS_LOG_008` compares both placeholders with `assertEquals` against the Gherkin literals `"Username"` / `"Password"` | **Confirmed resolved** |
| `DR-003` | Missing-controls collection, short timeout | `findMissingLoginControls()` returns `List<String>`; `LoginSteps` prints the list in the failure message; `MISSING_CONTROLS_TIMEOUT_SECONDS = 3` | **Confirmed resolved** (count mis-stated in the doc — `DR-018`) |
| `DR-004` | Presence-as-proxy recorded | `NFR-001` traceability row states the accepted proxy explicitly; `isPresent(LOADING_INDICATOR, 3s, 50ms)` | **Confirmed resolved** |
| `DR-005` | `tests.md` auto-applies via `**/*.java` | Frontmatter read directly: `test/**`, `tests/**`, `**/__tests__/**`, `**/*.java`, properly terminated; rule auto-loaded in this session on reading a `.java` file | **Confirmed resolved** |
| `DR-006` | Precedence note present | `tests.md` row in §SDLC Pipeline Components carries it, now explicitly narrowed by `AD-014`; `impl-plan.md` `OQ-P001` shows the escape clause was exercised with user approval | **Confirmed resolved** |
| `DR-007` | Exact override keys named | §Deployment lists `-Dbase.url=`, `-Dvalid.username=`, `-Dvalid.password=`, `-Dbrowser=`, `-Dheadless=`, `-Dexplicit.wait=`, `-Dpage.load.timeout=` and warns that `-DbaseUrl=` is ignored | **Confirmed resolved in the architecture**; `.claude/CLAUDE.md` still documents `-DbaseUrl=` (`OQ-D001` still open) |
| `DR-008` | Username disclosure channel documented | §Security Considerations "Report artifacts as a disclosure channel" bullet | **Confirmed resolved** |
| `DR-009` | Layer contract permits a constructor-only driver reference | Layer-contract row reworded; `LoginSteps` passes `driver` only to the three Page Object constructors and invokes no Selenium API | **Confirmed resolved** |
| `DR-010` | `source`/`target` vs `release` restated | §Deployment and §Assumptions both state the weaker guarantee; `pom.xml` confirms `source`/`target` 11 | **Confirmed resolved** |
| `DR-011` | Step 7 environmental-failure rule | Present in §Risks row 1 | **Confirmed resolved** |
| `DR-012` | Step 6 is the sole active credential-logging control | Stated in §Security Considerations; no component logs a credential (`DriverManager` logs browser/headless, `Hooks` logs scenario name/status, `LoginSteps` logs nothing) | **Confirmed resolved**, with the delivery-mechanism caveat raised as `DR-015` |

## Agreed Design Decisions

| Decision | Rationale | Owner |
| --- | --- | --- |
| `AD-014` is **approved as designed**: one narrow JUnit 4 class, `ConfigReaderTest`, executed by the `**/*Test.java` Surefire include, scoped to `ConfigReader`'s two `NFR-002` guards, browser-free and network-free. | The negative paths of the only security control implemented in this repository cannot be covered through Gherkin at all — an `http://` `base.url` makes the suite fail to start rather than produce an assertion. Traceability is intact (`NFR-002` → `AD-014` → `TASK-002`/`TASK-003`, both `Done`), and it narrows `DR-006` through that note's own escape clause rather than contradicting it. | Architect (confirmed by this review) |
| The `AD-014` unit-test layer **stays narrow**: guard logic only, never a second home for behaviour already covered by Gherkin; and the `@Before`/`@After` system-property capture-and-restore pattern is **mandatory** for any class matched by `**/*Test.java`. | Protects the Gherkin-first design from drift and prevents a future test from leaking a mutated `base.url` into the browser scenarios that follow (`DR-016`). | Planner (Step 4) / Implementation (Step 5) / Review (Step 6) |
| Only `hook_pr_gate.java` is **both wired and implemented**. Every other hook named in this pipeline is **neither wired in `.claude/settings.json` nor implemented**; activating one requires both a script and a `settings.json` entry. | Removes a false mental model of the `[ -f … ] && java …` guard and prevents an incompletely scoped activation task (`DR-013`). | All downstream agents |
| The `TASK-###` commit-traceability gate is **self-policed by the Implementation Agent (Step 5)** and by nothing else: the hook is absent, and neither `code-review` nor `create-pr` carries the check. | Replaces an aspirational mitigation with the true control surface (`DR-014`, `OQ-A006`). Step 5 must not skip its own quality gate, because no one downstream will catch it. | Implementation Agent (Step 5) |
| Implementing `hook_implementation_task_gate.java` (and/or adding the commit-message check to `code-review`/`create-pr`) stays **out of scope for `US-AUTH-002`** and is tracked as separate pipeline work. | It is pipeline tooling, not product verification, and traces to no `FR`/`NFR`; `code-quality.md`'s scope discipline forbids pulling it into this story. Convention is demonstrably holding: every prior-cycle `src/` commit cites a task id. | User / pipeline owner |
| The Step 6 no-credential-logging review check remains **mandatory and non-skippable**, independent of whether `.claude/rules/code-quality.md` auto-applies. | It is the sole active control for `NFR-002`'s logging clause (`DR-012`), and its delivery mechanism is currently malformed (`DR-015`). | Review Agent (Step 6) |
| `allow.insecure.base.url` must stay default-`false`, is settable through the same `-D` → env → file precedence as any other key, and its engagement is currently **unlogged**. Step 7 should confirm it was unset for the run it reports. | Keeps the deliberate escape hatch from becoming an undetectable one (`DR-017`). | Deployment / CI owner; Verify Agent (Step 7) |
| All prior-cycle decisions stand unchanged: the preventive `getBaseUrl()` scheme guard as the primary `NFR-002` transport control; `AD-009` as defence in depth; the untouchable `getValidPassword()` demo-host guard; `FR-001` footer/placeholder assertions sourced from Gherkin; the missing-controls return shape; `NFR-001` presence-as-proxy; the `tests.md` precedence note; `target/cucumber-reports/` as sensitive off-demo; and the Step 7 environmental-failure classification. | Re-verified against the code this cycle (see [Carried-forward findings: verification](#carried-forward-findings-verification)); no evidence contradicts any of them. | All downstream agents |

## Rejected or Deferred Findings

| ID | Finding | Rationale |
| --- | --- | --- |
| — | Absence of a CI/CD pipeline definition (`OQ-A003`) | **Not a gap — out of scope by standing user decision.** No pipeline file exists and none traces to any `FR`/`NFR`. Deliberately *not* raised as a finding; downstream agents must not re-raise it. |
| — | Parallel test execution not enabled (`OQ-A004`) | **Not a gap — out of scope by standing user decision.** `AD-012`'s `ThreadLocal` driver is parallel-*ready* by design; no requirement targets suite duration, and concurrent sessions against a shared public demo instance would add flake. |
| — | `OQ-A005` — `.claude/CLAUDE.md` documentation drift | **Not an architecture defect.** Confirmed still present on disk (the `-DbaseUrl=` spelling, the "`tests.md` doesn't match" note, and a hooks section that both claims every hook is wired and lists `hook_implementation_task_gate` as a `Bash(git commit*)` entry). `CLAUDE.md` is project configuration; this agent does not edit it. Folded into `OQ-D001` and re-raised to the user. |
| `DR-015` | Fixing `.claude/rules/code-quality.md`'s frontmatter | Accepted as a finding, but the **file edit is deferred to the user** (`OQ-D004`), following the `DR-005`/`DR-007` precedent that this agent corrects `artifacts/*.md` and flags `.claude/**` configuration rather than editing it. |
| `DR-017` | SLF4J `WARN` when `allow.insecure.base.url` is engaged | Documentation accepted; the **code change is deferred** to `OQ-D005` for the user to approve, because it touches a security-guarded method and no `FR`/`NFR` compels it. |
| `DR-016` | Pinning Surefire `<runOrder>` / an isolated fork for unit tests | Deferred as housekeeping. The current `@After` restore is correct, so there is no live defect; a `pom.xml` change traces to no `FR`/`NFR`. Documented as a constraint on future tests instead. |
| `DR-010` | `maven.compiler.release` vs `source`/`target` | Remains a **documentation-only** correction. Changing `pom.xml` traces to no `FR`/`NFR` and is deferred as housekeeping outside `US-AUTH-002`. |
| — | `requirements.md` assumption `A-001` (PRD rebuilt from git history, not re-read from live Confluence) | **Not an architecture finding.** It is an upstream, already-disclosed assumption owned by `artifacts/requirements.md`, and the architecture adds no driver absent from that document. Noted here only so Step 7 and Step 8 do not present PRD coverage as live-verified. |
| — | `.claude/settings.json` stale `permissions.allow` entries (`pip*`, `uvicorn*`) | Deferred as repository housekeeping, as in the prior cycle (`pytest*`/`python*` have since been removed). Not an architecture defect. The broad `Bash(git commit*)` allowance is noted under `DR-013`/`DR-014` as context, not as a separate finding. |
| — | `DashboardPage.isUserLoggedIn()` is unreferenced by any step definition | Not raised. A single unused public reader on a Page Object is below the threshold for a design finding and the architecture makes no claim about it; Step 6 can take it or leave it as dead-code housekeeping. |
| — | Full transport inspection (certificate validity, mixed content) | Deferred. Would require a proxy (e.g. BrowserMob) for no requirement-backed benefit; `AD-009`'s trade-off note records this and `AD-010`'s preventive guard covers the credible risk. |
| — | JUnit 4 rather than JUnit 5 (`AD-003`) | Rejected as a finding. The stack is a mandated constraint from `.claude/CLAUDE.md` and a runner migration serves no requirement. `AD-014` deliberately reuses JUnit 4 rather than introducing a second runner. |

## Required Architecture Updates

Applied to `artifacts/architecture.md` as part of this review (Step 6 of the
`design-review` skill):

1. **§SDLC Pipeline Components, hook rows** — `hook_implementation_task_gate.java` and the
   six other hooks restated as **not wired and not implemented**; only `hook_pr_gate.java`
   is wired *and* implemented; activation requires both a script and a `settings.json`
   entry. (`DR-013`)
2. **§Assumptions and Constraints, final bullet** — the "wired in `.claude/settings.json`"
   claim corrected. (`DR-013`)
3. **§Risks and Mitigations, commit-gate row** — mitigation rewritten: the convention is
   self-policed by Step 5 because neither `code-review` nor `create-pr` carries the check.
   (`DR-014`)
4. **§SDLC Pipeline Components, `code-quality.md` row** — annotated with the unterminated
   frontmatter and the instruction not to assume reliable auto-application; the Step 6
   credential-logging check stays mandatory regardless. (`DR-015`)
5. **§Technology Choices `AD-014`** — trade-off column records the mandatory
   capture/restore pattern and the breadth of the `**/*Test.java` include; **new risk row**
   for a future `*Test.java` leaking a mutated `base.url` into the browser suite.
   (`DR-016`)
6. **§Technology Choices `AD-010`** and **§Security Considerations, Transport bullet** —
   record that the opt-out is settable via `-D`, environment variable, *or* file, that its
   engagement is currently unlogged, and the recommended `WARN` plus the Step 7 check.
   (`DR-017`)
7. **§Reliability and Observability, timeout table** — `~8×3s` → `~7×3s`. (`DR-018`)
8. **§Open Questions `OQ-A006`** — rewritten with this review's assessment: deferral of the
   hook accepted, compensating control corrected.
9. **§SDLC Pipeline Components, Design Review Agent row** — finding range updated to
   `DR-001`–`DR-018`.

## Open Questions

- **`OQ-D001` (`DR-005`, `DR-007`, `DR-013`) — `.claude/CLAUDE.md` contains three
  inaccuracies this agent did not edit.** (a) It documents the base-URL override as
  `-DbaseUrl=…`, but the resolved key is `base.url`, so the documented command is silently
  ignored. (b) It describes `.claude/rules/tests.md` as not matching this repo's test
  sources, while the rule's real frontmatter includes `**/*.java` and does match. (c) Its
  Hooks section states that *every* hook command is guarded with `[ -f … ] && java …` and
  lists `hook_implementation_task_gate.java` as a wired `Bash(git commit*)` entry, when
  `settings.json` wires only `hook_pr_gate.java`. `artifacts/architecture.md` is corrected;
  `CLAUDE.md` is project configuration left for the user. **Non-blocking** for Step 4.
- **`OQ-D003` (`DR-013`) — `.claude/hooks/hook_implementation_task_gate.md` asserts its own
  wiring, which does not exist.** Its Status section says the `PreToolUse` /
  `Bash(git commit*)` entry "in `.claude/settings.json` … is guarded with `[ -f … ]`"; there
  is no such entry. Recommend correcting the note and, if the hook is ever implemented,
  adding the `settings.json` entry in the same change. **Non-blocking.**
- **`OQ-D004` (`DR-015`) — `.claude/rules/code-quality.md` needs a closing `---`.** One-line
  fix to project configuration, deliberately not made by this agent. Until it is applied,
  treat the rule's auto-application as unverified and the Step 6 credential-logging check as
  the control that must run regardless. **Non-blocking** for Step 4, but it sits under the
  only active control for an `NFR-002` clause, so it should be fixed before Step 5 writes
  any code.
- **`OQ-D005` (`DR-017`) — should the `allow.insecure.base.url` engagement emit a `WARN`?**
  A one-line SLF4J `WARN` in `ConfigReader.getBaseUrl()` would make a bypass of the primary
  `NFR-002` transport control visible in the console and Surefire output, leaking nothing.
  Recommendation: approve it as a small `TASK-###` citing `NFR-002`/`AD-010`/`DR-017`; it is
  not mandated, and the documentation update alone satisfies this finding.
  **Non-blocking** — either choice is acceptable.
- **`OQ-A006` (`DR-013`, `DR-014`) — reviewer assessment recorded.** Leaving
  `hook_implementation_task_gate.java` unimplemented **is an acceptable risk to carry
  forward**: it is pipeline tooling, it traces to no `FR`/`NFR`, `implement-task` already
  carries the gate in its own quality-gate table, and the prior cycle's commit history shows
  the convention being honoured without it. Two corrections were required rather than
  carried, and both are applied: the hook is *not wired*, not merely script-less
  (`DR-013`), and the "Steps 6 and 8 check manually" mitigation does not exist in those
  agents' skills (`DR-014`). With those recorded, the residual risk is traceability drift on
  a future `src/` commit, self-policed at Step 5. **Not a blocker for Step 4.**

`OQ-D002` from the prior cycle (**what to name the insecure opt-out, and should it exist**)
is now **closed by implementation**: the key is `allow.insecure.base.url`, it defaults to
`false`, and it ships commented out in `config.properties` with an in-file warning.
`OQ-A001`–`OQ-A004` remain **closed** at the architect's recommended defaults and must not
be re-litigated.

## Readiness Recommendation

**Ready for planning.**

- `Critical`: 0
- `High`: 1 (`DR-001`) — carried forward from the prior cycle and **verified resolved in
  code** this cycle; no unresolved `High` item remains.
- `Medium`: 10 — `DR-002`–`DR-008` (carried forward, verified resolved) and `DR-013`,
  `DR-014`, `DR-015` (new, accepted with decisions; `DR-015`'s file fix pending
  `OQ-D004`).
- `Low`: 7 — `DR-009`–`DR-012` (carried forward, verified resolved) and `DR-016`,
  `DR-017`, `DR-018` (new, accepted with decisions).

No finding in this cycle requires production-code change to the test framework. All six new
findings are documentation, pipeline-configuration, or forward-looking-constraint items, and
all are resolved in `artifacts/architecture.md` or raised as non-blocking open questions.

The Planner Agent (Step 4) should note that **no new `TASK-###` is strictly required by this
review**. Optional and user-gated candidates, in priority order:

| Candidate work | Traces to | Status |
| --- | --- | --- |
| Add the closing `---` to `.claude/rules/code-quality.md` | `DR-015`, `OQ-D004`, `NFR-002` | Recommended before Step 5 writes code; project-configuration change for the user |
| `WARN` log when `allow.insecure.base.url` is engaged | `NFR-002`, `AD-010`, `DR-017`, `OQ-D005` | Optional small task, user decision |
| Correct `.claude/CLAUDE.md` and `.claude/hooks/hook_implementation_task_gate.md` | `DR-005`, `DR-007`, `DR-013`, `OQ-D001`, `OQ-D003` | Documentation housekeeping, traces to no `FR`/`NFR`, out of scope for this story |
| Implement `hook_implementation_task_gate.java` **and** its `settings.json` entry, or add the commit-message check to `code-review`/`create-pr` | `DR-013`, `DR-014`, `OQ-A006` | Separate pipeline work, out of scope for `US-AUTH-002` |

Standing constraints for every downstream step: no code may remove, narrow, or widen the
`getValidPassword()` demo-host guard or the `getBaseUrl()` HTTPS scheme guard;
`allow.insecure.base.url` stays default-`false`; the `AD-014` unit-test layer stays scoped
to guard logic; and the Step 6 no-credential-logging check runs unconditionally.
