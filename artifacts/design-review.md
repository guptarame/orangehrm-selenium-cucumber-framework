# Design Review

**Story:** `US-AUTH-002` — Customer Login & Authentication
**Document under review:** `artifacts/architecture.md` (13 decisions `AD-001`–`AD-013`)
**Review date:** 2026-11-21
**Reviewer:** Design Review Agent (Step 3)

## Review Summary

The architecture is **sound and approved to proceed**, with one `High` finding that
must be folded into the plan before coding. The layered Gherkin → steps → Page
Object → WebDriver design, the `ThreadLocal` driver, the explicit-wait-only
synchronization policy (`AD-007`), and the override-first `ConfigReader` are all
correctly described and match the code as it stands. Every `FR`/`NFR` has a named
verifying component, and there are no orphan components.

Three themes drove the findings.

**1. `AD-009` closes the `NFR-002` HTTPS gap in the wrong direction.** The
architect correctly identified that the HTTPS clause was code-review-only, but the
proposed fix is a *detective* control: it asserts the scheme of the URL the browser
has already loaded, from inside a scenario that has already typed and submitted the
password. Nothing in the design *prevents* a non-HTTPS `base.url`. `base.url` is a
freely overridable key with no scheme validation, and the `getValidPassword()` host
guard is bypassed entirely whenever an override is supplied — which is the exact
mode the Deployment section recommends for a non-demo target with CI-injected
secrets. The combination "real credential from the CI secret store + `http://`
`base.url`" therefore has zero protection today, and `AD-009` would catch it only
*after* the credential crossed the wire in plaintext. This is `DR-001`, and the
agreed fix makes the control preventive (fail closed in `ConfigReader.getBaseUrl()`)
while keeping `AD-009` as defence in depth.

**2. `AD-008` would mark `FR-001` "Complete" without actually covering it.** The
decision to extend `@TS_LOG_008` (per the accepted `OQ-A001` default) is right, but
the shape of the extension is under-specified in a way that matters. The risk-table
mitigation "assert presence only, not text or link targets" — applied to the footer
and the two placeholders — yields a check that confirms a footer container exists
and that a `placeholder` attribute exists, while `FR-001` names the *copyright*, the
*social links*, and placeholder *text*. Flipping the traceability row to "Complete"
on that basis would overstate coverage (`DR-002`). Separately, folding three more
conditions into the existing short-circuiting `&&` chain behind a single `assertTrue`
makes a failure undiagnosable and stacks up to ~80s of timeouts (`DR-003`).

**3. One stated constraint is factually wrong.** `artifacts/architecture.md` asserts
twice — in the SDLC Pipeline Components table and as a booked risk — that
`.claude/rules/tests.md` does not match `src/test/java/**` and therefore does not
auto-apply. The rule's actual frontmatter includes `**/*.java`, so it *does* apply to
every Java file in this repository. The architecture inherits this from a stale note
in `.claude/CLAUDE.md`. The correction removes a phantom risk but introduces a real
one: `tests.md`'s unit-test/mocking structure mandate now binds Step 5, and conflicts
with an architecture that has no unit-test layer (`DR-005`, `DR-006`).

Verified as correct and requiring no change: the `FR-004` non-enumeration argument
(all three negative scenarios do use `assertEquals` against the literal
`"Invalid credentials"`, so any field-revealing message fails at least one);
`AD-013`'s `peekDriver()` reasoning; `DriverManager`'s half-initialised-browser
cleanup; the `NFR-001` same-call poll design; Surefire's `**/TestRunner.java`-only
include; and `FR-002`'s attribute-based masking check — reading `type` via
`getDomAttribute` is sound here because `type` is a reflected IDL attribute, so a
JavaScript `el.type = 'text'` toggle would update the content attribute and still be
caught.

**Readiness:** no `Critical` findings. The single `High` finding has an agreed,
documented decision and is carried into `Required Architecture Updates`, so the
architecture is ready for the Planner Agent (Step 4).

## Review Inputs

| Input | Version / commit | Status |
| --- | --- | --- |
| `artifacts/architecture.md` | commit `90de31c` "docs: define architecture for US-AUTH-002 login authentication" | Approved by user; `OQ-A001`–`OQ-A004` left at architect's recommended defaults |
| `artifacts/requirements.md` | commit `24b5d2d` (6 `FR`, 2 `NFR`) | Approved |
| `.claude/rules/code-quality.md` | working tree | Review baseline (OWASP / login-specific guidance) |
| `.claude/rules/sdlc-artifacts.md` | working tree | Review baseline (structure, traceability, writing quality) |
| `.claude/rules/tests.md` | working tree | Review baseline; frontmatter inspected directly (see `DR-005`) |
| Source under `src/test/java`, `src/test/resources`, `pom.xml` | working tree | Read to verify the architecture's claims rather than accept them |

Every claim in the architecture about existing code was checked against the code.
Findings below cite the architecture section or `AD-###` under review.

## Findings

| ID | Severity | Area | Finding | Impact | Recommendation | Decision | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `DR-001` | **High** | Security and access control | (§Technology Choices `AD-009`/`AD-010`, §Security Considerations; `NFR-002`) `AD-009` only asserts the scheme of an already-loaded URL. No component validates that `base.url` is HTTPS, and `getValidPassword()`'s host guard is skipped whenever a `valid.password` override is supplied — the sanctioned CI path. A `http://` target plus an injected real credential submits that credential in plaintext before any assertion runs. | `NFR-002`'s "never transmit in plain text" clause and `code-quality.md`'s "use HTTPS" / "fail closed on security-relevant errors" are breached in the exact deployment mode the Deployment section recommends. `AD-009` detects the breach only after disclosure. | Make the control preventive: `ConfigReader.getBaseUrl()` throws unless the resolved URL's scheme is `https`, gated by an explicit, default-`false` opt-out (e.g. `allow.insecure.base.url`) for a deliberate non-TLS sandbox. Keep `AD-009`'s executed assertion as defence in depth against a mid-session downgrade. | **Accepted.** Add preventive scheme validation to `ConfigReader`; `AD-009` is retained and re-scoped as defence in depth. | Resolved — decision agreed |
| `DR-002` | Medium | Requirements alignment | (§Technology Choices `AD-008`, §Risks, §Traceability `FR-001`) `AD-008` plus the risk mitigation "assert presence only, not text or link targets" would assert a footer *container* and the mere existence of a `placeholder` attribute, while `FR-001`/`AC-001` name the copyright, the social links, and placeholder text. | `FR-001` would be reported "Complete" while its named sub-elements are unverified — an overstated coverage claim that `sdlc-artifacts.md` forbids and that Step 7 would propagate. | Assert the footer copyright element and at least one social-link anchor as distinct controls, and compare both `placeholder` attributes to expected literals supplied as Gherkin step parameters (the existing `"Invalid credentials"` pattern). If container-only is accepted for brittleness reasons, `FR-001` must stay "Partial" with the residual named. | **Accepted.** Assert copyright + ≥1 social link + both placeholder values; expected text lives in Gherkin, not in the Page Object. | Resolved — decision agreed |
| `DR-003` | Medium | Testability and maintainability | (§Technology Choices `AD-008`; `LoginPage.areAllLoginControlsDisplayed`) The method already `&&`-chains five `isDisplayed(…, 10)` calls behind one `assertTrue`. `AD-008` adds three more terms. Short-circuiting means the first missing control masks the rest, and a page that fails to load costs up to ~80s of stacked timeouts. | An `FR-001` failure reports only "some control is missing", forcing manual reproduction; suite wall-clock degrades sharply on the most likely failure mode (demo site down). | Have `LoginPage` return the collection of missing controls (or expose per-control readers) and let `LoginSteps` assert on it, so the report names the missing control. Use one short timeout for the group after the page is already confirmed loaded. | **Accepted.** Replace the boolean chain with a missing-controls collection asserted in `LoginSteps`. | Resolved — decision agreed |
| `DR-004` | Medium | Requirements alignment | (§Traceability `NFR-001`, §Reliability timeout table; `BasePage.isPresent`) `AC-007` requires the indicator to be **visible**; `isPresent` polls for DOM *presence* only. The traceability table records `NFR-001` as "Complete". | Presence is weaker than visibility — a hidden-but-present node would satisfy the test. The coverage claim overstates what is verified. | Either poll on visibility with the same fast interval, or keep presence as a deliberate proxy (justified by the indicator's sub-100ms lifetime) and say so in the `NFR-001` coverage note so the trade-off is explicit rather than silent. | **Accepted.** Keep presence-based polling for reliability, and document it as an explicit accepted proxy in the `NFR-001` coverage note. | Resolved — decision agreed |
| `DR-005` | Medium | SDLC pipeline coverage | (§SDLC Pipeline Components, §Risks) The architecture states twice that `.claude/rules/tests.md` does not match `src/test/java/**` and does not auto-apply. Its actual frontmatter is `test/**`, `tests/**`, `**/__tests__/**`, `**/*.java` — the last pattern matches every Java file here, so it **does** auto-apply. | A false constraint tells Steps 5 and 6 to apply test standards manually and books a risk that does not exist. Inherited from a stale `.claude/CLAUDE.md` note, which itself has two mutually inconsistent copies of the pattern list. | Correct both rows in `architecture.md` to state that `tests.md` auto-applies via `**/*.java`, and delete the phantom risk row. `.claude/CLAUDE.md` needs the same correction — flagged for the user; not edited by this agent. | **Accepted.** Correct `architecture.md`; raise the `CLAUDE.md` inaccuracy to the user as `OQ-D001`. | Resolved — decision agreed |
| `DR-006` | Medium | SDLC pipeline coverage | (follows from `DR-005`) Because `tests.md` does auto-apply, its "provide **unit tests** for individual functions/modules with external dependencies mocked" and "provide **integration tests**" clauses now bind the Implementation Agent, but no `FR`/`NFR` and no architecture component defines a unit-test layer. | Step 5 may add out-of-scope unit tests for `ConfigReader`/`BasePage` (or Step 6 may flag their absence as a defect), contradicting `code-quality.md`'s scope discipline and `AD-004`. | State the precedence in `architecture.md`: `tests.md`'s coverage, determinism, and quality clauses apply in full; its structure clause is satisfied by the Gherkin + Page Object layering, and no unit/mock layer is in scope for `US-AUTH-002` unless an approved `TASK-###` introduces one. | **Accepted.** Record the precedence note in the architecture so Steps 5 and 6 read it the same way. | Resolved — decision agreed |
| `DR-007` | Medium | Deployment and CI/CD | (§Deployment) `.claude/CLAUDE.md` documents the override as `-DbaseUrl=…`, but `ConfigReader.getBaseUrl()` resolves the key `base.url`. `-DbaseUrl=` is silently ignored, and the run continues against the committed demo URL. The architecture's Deployment section describes the mechanism correctly but never names the exact key, so it does not catch the error. | An operator retargeting the suite believes they changed the target while the run still hits the public demo — a silent misconfiguration that also hides `DR-001`'s scheme problem. | Name exact keys in the Deployment section (`-Dbase.url=`, `-Dvalid.username=`, `-Dvalid.password=`, `-Dbrowser=`, `-Dheadless=`). Correct `-DbaseUrl` in `.claude/CLAUDE.md` — flagged for the user; not edited by this agent. | **Accepted.** Spell out exact keys in `architecture.md`; `CLAUDE.md` correction raised as `OQ-D001`. | Resolved — decision agreed |
| `DR-008` | Medium | Security and access control | (§Technology Choices `AD-013`, §Security Considerations) The screenshot trade-off reasons only about the password, noting `FR-002` masking makes capture safe. The **username** renders in plaintext and is embedded in `cucumber.json`/HTML and published as a CI build artifact. The Deployment section actively recommends injecting real credentials via `VALID_USERNAME` for a non-demo target. | Harmless for the public `Admin` demo account, but in the recommended non-demo mode a real username is persisted into a published artifact. The security analysis does not name this channel. | Document the exposure as accepted and scoped: for the public demo it is a non-issue; when `base.url` targets a non-demo environment, `target/cucumber-reports/` must be treated as sensitive and not published to an untrusted artifact store. | **Accepted.** Add the disclosure channel and its scoped mitigation to Security Considerations. | Resolved — decision agreed |
| `DR-009` | Low | Components and responsibilities | (§Recommended Architecture, layer contract) The contract says step definitions "must never call Selenium APIs directly", but `LoginSteps` legitimately holds a `WebDriver` field (from `DriverManager.getDriver()`) solely to construct Page Objects. As written, the contract reads as prohibiting the existing, intended design. | Step 6 could false-positive on `LoginSteps`' `WebDriver` field, or conversely dismiss the rule as inaccurate and miss a genuine violation. | Tighten to: step definitions may hold a `WebDriver` reference only to construct Page Objects, and must never invoke `WebDriver`/`WebElement`/`By`/wait APIs on it. | **Accepted.** Reword the layer-contract row. | Resolved — decision agreed |
| `DR-010` | Low | Technology choices | (§Deployment, §Assumptions) The architecture says "sources compile at release 11", but `pom.xml` sets `maven.compiler.source`/`target` to 11, not `release`. `source`/`target` do not restrict the API bootclasspath, so code referencing a post-11 API compiles cleanly and would fail only on an actual Java 11 JVM. | Low practical impact — the documented prerequisite is Java 21+, so nothing runs on 11. The stated guarantee is nonetheless not the one the build provides. | Either switch to `maven.compiler.release=11` to make the claim true, or restate it as "targets 11 bytecode without API-level enforcement". Not required for this story. | **Accepted as documentation-only.** Restate the claim in `architecture.md`; a `pom.xml` change is out of scope for `US-AUTH-002`. | Resolved — decision agreed |
| `DR-011` | Low | Observability and operations | (§Risks row 1, §Reliability) The dominant risk — the live third-party demo site being down, slow, or data-reset — is correctly rated High/High, but no convention tells Step 7 how to classify such a run. | The Verify Agent could report an environmental outage as an `FR`/`NFR` verification failure, corrupting `artifacts/verification-report.md`. | Add the classification rule to the architecture: a failure where the login page itself never renders (the `Given` step or the URL assertion fails, with a blank/error screenshot) is environmental and must be reported as such, not as a requirement failure. | **Accepted.** Add the classification convention for Step 7. | Resolved — decision agreed |
| `DR-012` | Low | Security and access control | (§SDLC Pipeline Components, §Risks; `AD-011`) The mitigation for "a future change logs a credential" leans partly on "the intended `hook_scan_secrets` hook would add automation once implemented". That hook does not exist and silently no-ops, so the only active control is the Step 6 review. | Over time the unimplemented hook can be mistaken for an operating control, leaving `AD-011` effectively unenforced. | State plainly that the Step 6 no-credential-logging check is the sole active control for `AD-011` and is mandatory and non-skippable while the hook is absent. | **Accepted.** Make the control ownership explicit. | Resolved — decision agreed |

### Finding detail — `DR-001` (the one `High`)

The guard in `ConfigReader.getValidPassword()` is:

```java
if (resolveOverride("valid.password") == null && !isDemoHost(getBaseUrl())) {
    throw new IllegalStateException(...);
}
```

Two properties follow, and the architecture documents neither:

1. **`isDemoHost` compares only the host.** `URI.create(url).getHost()` ignores the
   scheme, so `http://opensource-demo.orangehrmlive.com/...` satisfies the guard and
   the committed demo password is submitted over plaintext HTTP. Low impact in
   itself — that password is a public OrangeHRM demo value.
2. **Supplying an override short-circuits the host check entirely.** This is correct
   by design (the override is the sanctioned path for a real environment), but it
   means the combination the Deployment section recommends — `VALID_PASSWORD` from a
   CI secret store plus a retargeted `base.url` — carries **no** transport check at
   all. A `base.url` of `http://internal-hr.example/...` would send a real
   credential in cleartext.

`AD-009` cannot close this because it runs too late: by the time a `Then` step reads
`getCurrentUrl()`, `LoginPage.login()` has already typed and POSTed the password. The
fix must fail closed before the browser navigates, which is why the agreed decision
puts scheme validation in `ConfigReader.getBaseUrl()`. `AD-009` remains valuable for
the case the architecture originally had in mind — an HTTPS target that redirects or
downgrades mid-session — so it is kept, not replaced.

## Agreed Design Decisions

| Decision | Rationale | Owner |
| --- | --- | --- |
| `ConfigReader.getBaseUrl()` fails closed unless the resolved URL scheme is `https`, with an explicit default-`false` `allow.insecure.base.url` opt-out. | Converts `NFR-002`'s transport clause from a post-hoc assertion into a precondition, so no credential can be submitted over cleartext (`DR-001`). Satisfies `code-quality.md`'s "fail closed on security-relevant errors". | Planner (Step 4) → Implementation (Step 5) |
| `AD-009` is retained and re-scoped as defence in depth: it detects a mid-session downgrade, it is not the primary HTTPS control. | Keeps the cheap executed check the architect wanted without relying on it for prevention (`DR-001`). | Architect / Implementation |
| The existing `getValidPassword()` demo-host guard is **not** removed, narrowed, or widened — the new scheme check is additive. | Mandated by `.claude/CLAUDE.md`, `config.properties`, and §Security Considerations. Both guards address different failure modes. | All downstream agents |
| `FR-001` closure extends `@TS_LOG_008` (no new tag) **and** asserts the footer copyright element, at least one social-link anchor, and both placeholder values. | Honours the accepted `OQ-A001` default while making the coverage claim truthful (`DR-002`). | Planner → Implementation |
| Expected placeholder and footer text is supplied as Gherkin step parameters, never embedded in the Page Object. | Preserves the layer contract ("Page Objects must never contain test data") and matches the existing `"Invalid credentials"` pattern. | Implementation |
| `LoginPage` reports *which* required controls are missing instead of returning a single short-circuited boolean. | Makes an `FR-001` failure diagnosable from the report alone and avoids stacked timeouts (`DR-003`). | Implementation |
| `NFR-001` keeps DOM-presence polling, documented as an explicit accepted proxy for `AC-007`'s "visible". | Visibility polling on a sub-100ms indicator would trade a real reliability loss for a marginal fidelity gain; the trade-off is recorded rather than hidden (`DR-004`). | Architect |
| `tests.md` auto-applies via `**/*.java`; its coverage/determinism clauses bind this repo, its unit/integration *structure* clause is satisfied by the Gherkin + Page Object layering, and no unit-test layer is in scope for `US-AUTH-002`. | Removes a factual error and pre-empts out-of-scope unit-test work or a spurious Step 6 finding (`DR-005`, `DR-006`). | Architect / Planner / Review |
| `target/cucumber-reports/` is treated as sensitive whenever `base.url` targets a non-demo environment. | Screenshots and report JSON capture the username in plaintext (`DR-008`). | Deployment / CI owner |
| A run where the login page never renders is classified environmental, not an `FR`/`NFR` failure. | Protects the integrity of `artifacts/verification-report.md` (`DR-011`). | Verify Agent (Step 7) |
| The Step 6 no-credential-logging review check is the sole active control for `AD-011` and is non-skippable. | `hook_scan_secrets` is unimplemented and silently no-ops (`DR-012`). | Review Agent (Step 6) |

## Rejected or Deferred Findings

| ID | Finding | Rationale |
| --- | --- | --- |
| — | Absence of a CI/CD pipeline definition (`OQ-A003`) | **Not a gap — out of scope by user decision.** No pipeline file exists and none traces to any `FR`/`NFR`. The architect's recommendation to leave it out of scope was accepted. Deliberately *not* raised as a finding; downstream agents must not re-raise it. |
| — | Parallel test execution not enabled (`OQ-A004`) | **Not a gap — out of scope by user decision.** `AD-012`'s `ThreadLocal` driver is parallel-*ready* by design; no requirement targets suite duration, and concurrent sessions against a shared public demo instance would add flake. Status quo confirmed. Deliberately *not* raised as a finding. |
| `DR-010` | `maven.compiler.release` vs `source`/`target` | Accepted as a **documentation-only** correction. Changing `pom.xml` is not traceable to any `FR`/`NFR` and is deferred as housekeeping outside `US-AUTH-002`. |
| — | Full transport inspection (certificate validity, mixed content) | Deferred. Would require a proxy (e.g. BrowserMob) for no requirement-backed benefit; `AD-009`'s trade-off note already records this and `DR-001`'s preventive guard covers the credible risk. |
| — | `.claude/settings.json` stale `permissions.allow` entries (`pytest*`, `python*`, `pip*`, `uvicorn*`) and the six unimplemented hooks | Deferred as repository housekeeping, outside this story. `DR-012` captures the one consequence that affects `NFR-002`. Not an architecture defect. |
| — | `JUnit 4` rather than JUnit 5 (`AD-003`) | Rejected as a finding. `AD-003`'s rationale is sound and the stack is a mandated constraint from `.claude/CLAUDE.md`; a runner migration serves no requirement. |

## Required Architecture Updates

Applied to `artifacts/architecture.md` as part of this review (Step 6 of the
`design-review` skill):

1. **`AD-009`** — re-scoped as defence in depth; primary HTTPS control moved to a
   preventive `ConfigReader.getBaseUrl()` scheme check. (`DR-001`)
2. **`AD-010`** — records that a `valid.password` override bypasses the host guard by
   design, and that the new scheme guard is the control for that path. (`DR-001`)
3. **§Security Considerations** — Transport bullet rewritten around the preventive
   guard; new bullet for the screenshot/report username exposure (`DR-008`); credential
   -logging control ownership made explicit (`DR-012`).
4. **`AD-008`** — specifies copyright, social-link, and placeholder-value assertions,
   expected text sourced from Gherkin, and a missing-controls return shape.
   (`DR-002`, `DR-003`)
5. **§Requirement-to-Component Traceability** — `FR-001` row states what "Complete"
   will require; `NFR-001` row records presence-vs-visibility as an accepted proxy;
   `NFR-002` row cites the preventive guard. (`DR-001`, `DR-002`, `DR-004`)
6. **§SDLC Pipeline Components** — `tests.md` row corrected to "auto-applies via
   `**/*.java`" with the structure-clause precedence note. (`DR-005`, `DR-006`)
7. **§Risks and Mitigations** — phantom `tests.md` risk row removed; live-site row
   gains the Step 7 classification rule. (`DR-005`, `DR-011`)
8. **§Recommended Architecture layer contract** — step-definition row reworded to
   permit a constructor-only `WebDriver` reference. (`DR-009`)
9. **§Deployment** — exact override keys named (`-Dbase.url=`, not `-DbaseUrl=`);
   compiler claim restated. (`DR-007`, `DR-010`)

## Open Questions

- **`OQ-D001` (`DR-005`, `DR-007`) — `.claude/CLAUDE.md` contains two inaccuracies
  that this agent did not edit.** (a) It documents the base-URL override as
  `-DbaseUrl=…`, but the resolved key is `base.url`, so the documented command is
  silently ignored. (b) It describes `.claude/rules/tests.md` as not matching this
  repo's test sources, in two mutually inconsistent ways (`**/*.test.py`/`**/*.spec.py`
  in one place, `**/*.test.java` in another), while the rule's real frontmatter is
  `**/*.java` and does match. `artifacts/architecture.md` has been corrected;
  `CLAUDE.md` is project configuration and is left for the user to amend.
  **Non-blocking** for Step 4.
- **`OQ-D002` (`DR-001`) — what should the `allow.insecure.base.url` opt-out be
  named, and should it exist at all?** The agreed decision includes a default-`false`
  escape hatch so a developer can point the suite at a local non-TLS sandbox. A
  stricter alternative is no opt-out whatsoever — HTTPS or nothing. Recommendation:
  keep the opt-out, default `false`, documented as accepted only for a local
  throwaway target; the Planner can fix the final key name in the `TASK-###`.
  **Non-blocking** — either choice satisfies `DR-001`.

`OQ-A001`–`OQ-A004` from `artifacts/architecture.md` are **closed**: all four were
resolved at the architect's recommended defaults by user decision and are recorded
above (`OQ-A001`/`OQ-A002` as agreed design decisions, `OQ-A003`/`OQ-A004` as
explicitly out of scope and not findings).

## Readiness Recommendation

**Ready for planning.**

- `Critical`: 0
- `High`: 1 (`DR-001`) — resolved by an agreed, documented design decision; no
  unresolved `High` item remains.
- `Medium`: 7 (`DR-002`–`DR-008`) — all accepted with decisions.
- `Low`: 4 (`DR-009`–`DR-012`) — all accepted with decisions.

The Planner Agent (Step 4) should convert the following into `TASK-###` entries, each
citing its `FR`/`NFR`, architecture section, and `DR-###`:

| Planned work | Traces to |
| --- | --- |
| Preventive HTTPS scheme guard in `ConfigReader.getBaseUrl()` (+ negative coverage for the fail-closed path) | `NFR-002`, `AD-010`, `DR-001` |
| Executed HTTPS scheme assertion on the login and post-submit URLs | `NFR-002`, `AD-009`, `DR-001` |
| Footer, social-link, and placeholder-value assertions extending `@TS_LOG_008`, with a missing-controls return shape | `FR-001`, `AD-008`, `DR-002`, `DR-003` |
| Documentation-only corrections to `artifacts/architecture.md` | `DR-004`–`DR-012` (already applied by this review) |

No code may be written against `DR-001` that removes, narrows, or widens the existing
`getValidPassword()` demo-host guard.
