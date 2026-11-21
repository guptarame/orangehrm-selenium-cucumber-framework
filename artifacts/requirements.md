# Requirements

## User Story Reference

**Story ID (authoritative):** US-AUTH-002 — confirmed by the user against
`.claude/CLAUDE.md` (line 10) and the repo's established convention. The
source Confluence PRD page (13271042) self-identifies internally as
`US-AUTH-001`; see **Open Question OQ-001 (Resolved)** below for provenance
and reconciliation of that discrepancy.
**Epic:** Authentication & Access Management
**Target URL:** `https://opensource-demo.orangehrmlive.com/web/index.php/auth/login`
**Source:** Confluence page 13271042, "Login Functionality for OrangeHRM Portal"

**As a** registered user (Admin/Employee) of the OrangeHRM system,
**I want to** securely log into the application using my credentials,
**so that** I can access my designated dashboard and perform role-based HR
management tasks.

## Business Objective

Give registered OrangeHRM users a reliable, secure login gate — correct
credentials grant dashboard access, incorrect or incomplete input is rejected
with clear, non-revealing feedback — so downstream HR features are only
reachable by authenticated users.

## Preconditions (from PRD)

1. The user has a valid registered account (demo credentials: `Admin` /
   `admin123`).
2. The OrangeHRM application server and database are up and running.
3. The user has a modern browser with internet access and has navigated to
   the login URL.
4. JavaScript is enabled in the browser.

## Functional Requirements

### FR-001: Login page displays all required controls
**Statement:** The system shall render the login page with the company
logo/branding, a Username field (with placeholder text), a Password field
(with placeholder text), a "Login" submit button, a "Forgot your password?"
link, and a footer containing the OrangeHRM copyright and social links.
**Source:** PRD AC1.1
**Priority:** High

### FR-002: Password input masks entered characters
**Statement:** The system shall mask every character typed into the Password
field (e.g. as dots or asterisks) so the plaintext value is never visible
on screen.
**Source:** PRD AC1.2
**Priority:** High

### FR-003: Successful authentication redirects to the dashboard
**Statement:** When the user submits a valid, registered username and its
correct matching password — via the "Login" button or the Enter key — the
system shall authenticate the user and redirect to the dashboard page
(`.../dashboard/index`).
**Source:** PRD AC2.1, AC2.2
**Priority:** High

### FR-004: Invalid credentials are rejected with a generic error
**Statement:** When the user submits a username/password combination that is
not a valid, matching pair (wrong password, unknown username, or both), the
system shall remain on the login page and display the generic error message
"Invalid credentials", without indicating which field was incorrect.
**Source:** PRD AC3.1
**Priority:** High

### FR-005: Empty required fields are rejected with inline validation
**Statement:** When the user submits the login form with the Username field
blank, the Password field blank, or both blank, the system shall block
submission and display the inline validation message "Required" directly
below each blank field.
**Source:** PRD AC3.2
**Priority:** High

### FR-006: Forgot-password link navigates to the reset-password page
**Statement:** When the user clicks the "Forgot your password?" link, the
system shall navigate to the reset-password request page
(`.../auth/requestPasswordResetCode`).
**Source:** PRD AC4.1
**Priority:** Medium

## Non-Functional Requirements

### NFR-001: Authentication-in-progress feedback
**Statement:** The system shall display a loading spinner or indicator while
an authentication request is in flight, from submission until the login page
either redirects or returns an error/validation result.
**Source:** PRD AC2.3
**Priority:** Medium

### NFR-002: Credential confidentiality and non-enumeration
**Statement:** The system shall never expose a submitted password in plaintext
(on screen, in logs, or over the network — login traffic shall be served over
HTTPS) and shall never disclose through its error response whether the
username or the password was the incorrect element of a failed login attempt.
**Source:** PRD story intent ("securely log into the application"), AC1.2,
AC3.1; reinforced by `.claude/rules/code-quality.md` login-specific guidance.
**Priority:** High

## Acceptance Criteria

### AC-001 (FR-001)
**Given** the user has navigated to the OrangeHRM login URL
**When** the login page finishes loading
**Then** the logo, Username field (with placeholder), Password field (with
placeholder), Login button, "Forgot your password?" link, and footer
(copyright + social links) are all visible on the page
*(traces to `@TS_LOG_008`)*

### AC-002 (FR-002)
**Given** the user is on the login page
**When** the user types any value into the Password field
**Then** the field displays masked characters (dots/asterisks), never the
plaintext value
*(traces to `@TS_LOG_007`)*

### AC-003 (FR-003)
**Given** the user is on the login page
**When** the user enters the valid username `Admin` and the valid password
`admin123` and either clicks "Login" or presses Enter
**Then** the user is redirected to `.../dashboard/index`
*(traces to `@TS_LOG_001`, `@TS_LOG_012`)*

### AC-004 (FR-004)
**Given** the user is on the login page
**When** the user submits a username and password where at least one value
does not match a registered account (e.g. valid username/wrong password,
invalid username/valid password, or both invalid)
**Then** the user remains on the login page and the error message
"Invalid credentials" is displayed, with no indication of which field was
wrong
*(traces to `@TS_LOG_002`, `@TS_LOG_003`, `@TS_LOG_011`)*

### AC-005 (FR-005)
**Given** the user is on the login page
**When** the user submits the form with the Username field blank, the
Password field blank, or both blank
**Then** the message "Required" is displayed directly below each field that
was left blank
*(traces to `@TS_LOG_004`, `@TS_LOG_005`, `@TS_LOG_006`)*

### AC-006 (FR-006)
**Given** the user is on the login page
**When** the user clicks the "Forgot your password?" link
**Then** the browser navigates to `.../auth/requestPasswordResetCode`
*(traces to `@TS_LOG_010`)*

### AC-007 (NFR-001)
**Given** the user has submitted valid or invalid login credentials
**When** the authentication request is in progress
**Then** a loading spinner/indicator is visible until the request completes
*(traces to `@TS_LOG_009`)*

### AC-008 (NFR-002)
**Given** the user submits the login form
**When** the request is sent and a response (success or failure) is returned
**Then** the connection uses HTTPS, the password value is never rendered in
plaintext or written to logs, and a failed attempt's error message does not
reveal whether the username or the password was incorrect
*(traces to PRD AC3.1 generic-error wording; validated functionally via
`@TS_LOG_002`, `@TS_LOG_003`, `@TS_LOG_011`, and operationally via code review
of logging/transport handling — no dedicated Cucumber scenario exists for the
HTTPS/no-logging aspects)*

## Traceability

| ID | Source | Test Scenario(s) | Status |
| --- | --- | --- | --- |
| FR-001 | PRD AC1.1 | `@TS_LOG_008` | Approved |
| FR-002 | PRD AC1.2 | `@TS_LOG_007` | Approved |
| FR-003 | PRD AC2.1, AC2.2 | `@TS_LOG_001`, `@TS_LOG_012` | Approved |
| FR-004 | PRD AC3.1 | `@TS_LOG_002`, `@TS_LOG_003`, `@TS_LOG_011` | Approved |
| FR-005 | PRD AC3.2 | `@TS_LOG_004`, `@TS_LOG_005`, `@TS_LOG_006` | Approved |
| FR-006 | PRD AC4.1 | `@TS_LOG_010` | Approved |
| NFR-001 | PRD AC2.3 | `@TS_LOG_009` | Approved |
| NFR-002 | PRD story intent, AC1.2/AC3.1, code-quality.md | *(no dedicated scenario — partially covered by `@TS_LOG_002/003/011`)* | Approved |

## Out of Scope

- Account lockout/throttling after repeated failed login attempts (not
  mentioned in the PRD).
- Password reset flow steps beyond navigating to the request page (i.e. the
  content and behavior of `.../auth/requestPasswordResetCode` itself).
- "Remember me" / persistent session functionality (not present in the PRD
  or the current login page).
- Multi-factor authentication (not mentioned in the PRD).
- Role-based dashboard content differences between Admin and Employee logins
  (the PRD only specifies redirect-to-dashboard, not role-specific content).
- Session timeout/expiry behavior after a successful login.
- Browser/OS compatibility matrix beyond "a modern web browser" (precondition
  #3 is not broken into testable cross-browser requirements).
- Accessibility (WCAG) requirements — not mentioned in the PRD.
- Internationalization/localization of login page text and error messages.
- Server-side password storage/hashing mechanics — out of scope for a
  UI-driven test framework with no backend code under test.

## Open Questions

### OQ-001: Story ID discrepancy (US-AUTH-001 vs US-AUTH-002) — Resolved
**Resolution:** `US-AUTH-002` is the authoritative story ID for this epic,
confirmed by the user, citing `.claude/CLAUDE.md` (line 10: "Built as
capstone SDLC artifacts for user story `US-AUTH-002`") as the source of
truth. This document and all downstream artifacts (architecture, impl-plan,
etc.) use `US-AUTH-002` when citing the story.

**Provenance (for traceability, not an open discrepancy):** The Confluence
PRD (page 13271042) self-identifies internally as **`US-AUTH-001`**, while
`.claude/CLAUDE.md` and the existing codebase (this repo's established
convention, predating this pipeline run) refer to this same login capability
as **`US-AUTH-002`** ("Customer Login & Authentication"). The two IDs refer to
the same underlying login story; `US-AUTH-001` is retained here only as a
record of the PRD's self-identification, not as an alternative authoritative
ID.

### OQ-002: Scope beyond the PRD's 7 test scenarios
The existing `src/test/resources/features/login.feature` already implements
`@TS_LOG_001`–`@TS_LOG_012`, five more scenarios than the PRD's 7
(`TS_LOG_008` UI controls display, `TS_LOG_009` loading indicator,
`TS_LOG_010` forgot-password navigation, `TS_LOG_011` both-invalid credentials,
`TS_LOG_012` Enter-key submission). These extra scenarios map cleanly onto
AC1.1, AC2.3, and AC4.1, which the PRD states but did not list as standalone
test IDs — this document's FR-001, FR-006, and NFR-001 close that gap. No
scenario exists for AC3.1's "both invalid" combination beyond what
`TS_LOG_011` already covers, so no new requirement was needed there beyond
FR-004. Flagging this as confirmed-covered rather than a gap, so the next
agent does not duplicate this analysis.
