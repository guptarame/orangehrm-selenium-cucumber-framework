# Selenium Login Automation - Capstone

This project implements the full capstone SDLC artifacts and a Selenium Java Maven test suite for user story `US-AUTH-002` (Customer Login & Authentication).

## Prerequisites
- Java 21+
- Maven 3.9+
- Internet access to `https://askomdch.com/account/`
- Chrome (default) or Firefox installed locally

## Environment Variables (recommended)
Set these only when you want to run credential-dependent tests:

- `LOGIN_VALID_USERNAME`
- `LOGIN_VALID_PASSWORD`
- `LOGIN_INVALID_PASSWORD` (optional, default is `invalid-password`)
- `LOGIN_UNKNOWN_USERNAME` (optional)

## Run Tests
```powershell
mvn test
```

## Optional Runtime Overrides
```powershell
mvn test -Dbrowser=firefox -Dheadless=true -DbaseUrl=https://askomdch.com/account/
```

## SDLC Artifacts
- `docs/sdlc/requirements.md`
- `docs/sdlc/architecture.md`
- `docs/sdlc/design-review.md`
- `docs/sdlc/impl-plan.md`
- `docs/sdlc/code-review-report.md`
- `docs/sdlc/verify.md`
- `docs/sdlc/pr-description.md`

