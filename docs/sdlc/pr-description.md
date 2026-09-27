## Summary
This PR implements a complete Selenium test automation framework for the customer login authentication flow. The framework covers successful login, failed authentication, validation errors, session persistence, and password recovery navigation, using a reusable Page Object Model and centralized configuration.

**PRD:** Confluence PRD — https://epam-team-en32bjvm.atlassian.net/wiki/spaces/MFS/pages/11796481/Customer+Login+Authentication
**Traceability:** Full SDLC artifacts are in `docs/sdlc/`

---

## Changes Made

### Core framework
- `BaseTest.java` for WebDriver lifecycle setup and teardown
- `BasePage.java` for common Selenium utilities, waits, and interactions
- `LoginPage.java` for login page behavior and validation logic
- `TestConfig.java` for system/env configuration and browser defaults
- `TestData.java` for reusable assertions and tokens

### Test coverage
- Valid login success flow
- Invalid password rejection
- Unknown user rejection
- Blank-field validation
- Remember Me persistence
- Lost password navigation
- Required login element verification

### Reporting / diagnostics
- `TestLifecycleListener` for lifecycle reporting
- `ExtentReportExtension` for enhanced test reporting
- Logging and cleanup hooks for browser sessions

### SDLC artifacts
- `docs/sdlc/requirements.md`
- `docs/sdlc/architecture.md`
- `docs/sdlc/design-review.md`
- `docs/sdlc/impl-plan.md`
- `docs/sdlc/verification-report.md`

---

## Test Evidence

```text
mvn test -q
```

Result: 9/9 tests passed, 0 failed, 0 skipped.

---

## Known Limitations
- This is a V1 sequential framework; parallel execution is out of scope.
- Browser-version compatibility requires regular validation when Chrome/Firefox updates.
- Real credentials must remain supplied through environment variables when credential-dependent testing is required.

---

## Reviewer Checklist
- [ ] Framework architecture is clear and maintainable
- [ ] Login scenarios are covered comprehensively
- [ ] Browser configuration and teardown are safe
- [ ] Verification evidence is documented
- [ ] Security expectations for credentials are respected
