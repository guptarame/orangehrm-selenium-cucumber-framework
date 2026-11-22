---
description: "Secure, clear, DRY coding standards enforced during implementation and review."
paths:
  - "**/*.java"
---

# Code Quality Standards

Applies to all production source code generated or modified in this repository.
## Security (OWASP Top 10)

- Never hard-code, log, or return secrets, tokens, passwords, or credentials.
  Read them from environment variables or a secrets manager.
- Validate and sanitise all external and user-supplied input at the boundary.
  Guard against injection (SQL, command, XSS) and broken access control.
- Enforce authentication and authorization on every protected operation.
- Use parameterized queries and safe, maintained libraries. Flag and avoid
  known-vulnerable package versions.
- For the login use case specifically: mask passwords, never store or transmit
  them in plain text, use HTTPS, and return generic auth errors that do not
  reveal whether the identifier or the password was wrong.

## Error handling

- Handle API failures, timeouts, missing files, empty inputs, and `Not Found`
  states gracefully with clear messages — no unhandled exceptions.
- Fail closed on security-relevant errors.

## Clarity and DRY

- Use self-explanatory function and variable names; keep control flow readable
  without relying on comments.
- Extract duplicated logic into a single shared, well-named function or module.
- Add a comment only to explain *why* something non-obvious is done — never to
  restate what the code already shows.

## Scope discipline

- Implement only what an approved task in `impl-plan.md` requires.
- Do not add features, refactors, or abstractions beyond the task scope.
- Follow existing project structure, naming, and style conventions.
