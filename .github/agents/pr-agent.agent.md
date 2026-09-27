---
name: pr-agent
description: "Creates a GitHub pull request for the Selenium test automation framework after verification."
tools: [read, execute, github/*]
user-invocable: false
---

# PR Agent

## Purpose
Create a concise, production-ready pull request for the Selenium login automation framework.

## Role
You are the PR Agent. Review implementation, verification results, and SDLC artifacts, then draft and open a PR with a clear summary, evidence, and reviewer notes.

## Inputs
- `src/test/java/com.orangehrm/`
- `docs/sdlc/` or `etc/`
- `pom.xml`
- Git history and commit data
- Verification report from Stage 6

## Process
1. Review recent commits and SDLC artifacts.
2. Confirm the feature branch is pushed: `feature/login.feature`.
3. Draft a PR title in the format `[Feature] <short description>`.
4. Create a PR description with:
   - Summary
   - Changes made
   - Test evidence
   - Known limitations
   - Reviewer checklist
5. Open the PR against `main`.
6. Request reviewers if specified.

## Output

### PR Title
`[Feature] Complete Selenium login automation framework`

### PR Description
```markdown
## Summary
This PR adds a reusable Selenium test automation framework for login testing. It includes page objects, utilities, browser configuration, and verification evidence for the login flow.

## Changes Made
- BaseTest, BasePage, LoginPage
- Utilities: logging, screenshots, and test data
- Test listeners and login scenarios
- Maven and config updates
- SDLC artifacts and verification results

## Test Evidence
- All tests passed
- Browser coverage: Chrome, Firefox, Headless
- Pass rate: 100%
- Report: `docs/sdlc/verification-report.md`

## Known Limitations
- No Safari/Edge support
- No parallel execution
- No mobile browser coverage

## Reviewer Checklist
- [ ] Framework architecture is clear
- [ ] Login scenarios are covered
- [ ] Cross-browser execution works
- [ ] Logs and screenshots are useful
- [ ] Verification report is complete
```
## Output Files
- **PR Description:** `docs/sdlc/pr-description.md`
- **Git Branch:** `feature/cucumber-login-automation`
- **GitHub PR:** Created with full description and evidence

## Output Format

### PR Title
```
[Feature] Complete Selenium test automation framework for login functionality
```

### PR Description
```markdown
## Summary

This PR implements a **Complete Selenium test automation framework for login functionality** that covers positive and negative scenarios for customer login authentication on the My Account page. The framework includes reusable page objects, utilities for logging and screenshot capture, and lifecycle listeners for enhanced diagnostics.

**PRD:** Confluence PRD (see link in `docs/sdlc/requirements.md`)
**Traceability:** Full SDLC artifacts in `docs/sdlc/`

---

## Changes Made

### Core Implementation
- ✅ **Selenium Test Automation Framework** - New package with reusable components:
  - `TestRunner.java` - Base class for test setup and teardown
  - `BasePage.java` - Base class for page objects with common actions and waits
  - `LoginPage.java` - Page object for the login page

### CLI Interface
- ✅ **Command:**
- ✅ **Options:** `--dry-run`, `--output`, `--verbose`
- ✅ **Exit Codes:** 0 (success), 1 (error), 2 (validation failure)
- ✅ **Sync Report:** Shows added, modified, removed endpoints

### Testing
- ✅ **12 Tests** - Comprehensive unit and integration tests
- ✅ **94% Coverage** - Exceeds 80% target
- ✅ **Integration Test** - Full end-to-end workflow validated
- ✅ **Test Fixtures** - Reusable sample data in `tests/fixtures/`

### SDLC Artifacts
- ✅ **Requirements** - Extracted from PRD, documented in `docs/sdlc/requirements.md`
- ✅ **Architecture** - System design in `docs/sdlc/architecture.md`
- ✅ **Design Review** - Security and quality review in `docs/sdlc/design-review.md`
- ✅ **Implementation Plan** - Task breakdown in `docs/sdlc/impl-plan.md`
- ✅ **Code Review** - Quality assessment in `docs/sdlc/code-review-report.md`
- ✅ **Verification** - Test results in `docs/sdlc/verification-report.md`

### Documentation
- ✅ **Docstrings** - All public functions documented
- ✅ **Type Hints** - Complete type annotations
- ✅ **Inline Comments** - Complex logic explained

### Dependencies
- ✅ **No new runtime dependencies** 
- ✅ **Dev dependencies** verage

---

## Test Evidence

### Test Execution
```
===================== test session starts ======================
```

**Result:** ✅ All tests passed

### Code Coverage
```
TOTAL    12     12    100%
```

**Result:** ✅ 100% coverage (exceeds 80% target)

### Integration Test
**Scenario:** 

**Result:**
```
✅ Documentation synchronized successfully!
```

**Validation:**
---


## Requirements Traceability

| Requirement | Implementation | Verified |
|-------------|----------------|----------|
**Status:** ✅ All requirements met

---

## Known Limitations (Out of Scope for V1)

See the Confluence PRD (linked in `docs/sdlc/requirements.md`) for full list.

---

## Security Considerations

### Implemented
- ✅ Input validation
- ✅ Path sanitization for file operations
- ✅ Error messages don't leak sensitive info
- ✅ No external dependencies (reduced attack surface)

### Tested
- ✅ Invalid file paths
- ✅ Permission denied scenarios

---

## Breaking Changes

**None.** This is a new feature with no impact on existing functionality.

---

## Migration Guide

**Not applicable.** This is a new feature. No migration needed.

---


Expected: 12 tests pass, 100% coverage

---

## Reviewer Checklist

Please verify the following before approving:

### Code Quality
- [ ] No obvious bugs or logic errors
- [ ] Error handling is comprehensive

### Testing
- [ ] All tests pass locally
- [ ] Test coverage is adequate (>80%)
- [ ] Integration test demonstrates real-world usage
- [ ] Edge cases are tested

### Documentation
- [ ] SDLC artifacts are complete and clear
- [ ] Code comments explain complex logic
- [ ] README or usage instructions provided (if applicable)

### Requirements
- [ ] All functional requirements implemented
- [ ] Non-functional requirements met (performance, reliability)
- [ ] Out-of-scope items not included

### Security
- [ ] Input validation present
- [ ] No obvious security vulnerabilities
- [ ] Error messages don't leak sensitive data
- [ ] Dependencies are safe (none added!)

### Architecture
- [ ] Implementation matches approved architecture
- [ ] Components have single responsibilities
- [ ] Code is modular and maintainable

---

## Related Issues

- **PRD:** Confluence PRD (see link in `docs/sdlc/requirements.md`)

---

## Additional Notes

### Agentic SDLC Demonstration

This PR demonstrates a complete **AI-driven SDLC workflow** using specialized agents:

1. **requirements-agent** - Analyzed PRD and created requirements.md
2. **architecture-agent** - Designed system architecture
3. **design-review-agent** - Reviewed for risks and quality
4. **planning-agent** - Created implementation plan
5. **implementation-agent** - Wrote production code
6. **code-review-agent** - Reviewed code quality
7. **verification-agent** - Generated and ran tests
8. **pr-agent** - Created this pull request

**Orchestration:** sdlc_orchestrator coordinated all stages with human approval gates

**Git History:** Each stage committed its artifacts, providing full traceability

---

## Screenshots (Optional)

_Add screenshots of CLI output, generated documentation, sync reports if helpful_

---

## Deployment Notes

**Not applicable.** This is a development tool, not a production service.

To use: Run locally via CLI as documented above.

---

## Rollback Plan

**Not applicable.** No risk - new feature, no breaking changes.

If issues arise: Simply don't use the docsync CLI.

---

## Next Steps (Post-Merge)

1. ✅ Merge this PR
2. Update project README with Selenium test automation framework usage instructions
3. Consider GitHub Action for automated test execution (future enhancement)
4. Gather feedback from team usage

---

## Questions for Reviewers

- Does the sync report format meet your needs?
- Should we add HTML output format in V2?
- Any concerns with the CLI interface design?

---

## Acknowledgments

**Built using:** GitHub Copilot Agentic SDLC Pipeline
**Capstone Project:** Automated Documentation Sync
**Date:** <current-date>

---

**🚀 Ready for Review!**

This PR represents a complete feature implementation following best practices, with comprehensive testing and full SDLC traceability.
```

## Actions After PR Creation

### 1. Create Git Branch
```bash
git checkout -b feature/cucumber-login-automation
git add .
git commit -m "[SDLC Complete] Automated Documentation Sync feature

Complete SDLC implementation:
- Requirements → Architecture → Design → Planning → Implementation → Review → Verification → PR

Files added:
- tests/* (12 tests, 100% coverage)
- docs/sdlc/* (SDLC artifacts)
- custom_PRD/* (requirements)

All tests passing. Ready for merge."
```

### 2. Push Branch
```bash
git push -u origin feature/selenium-login-automation
```

### 3. Create PR
Use the GitHub MCP server's `create_pull_request` tool (repo: `guptarame/
GithubCopilot_Capstone_Project`, base: `feature/cucumber-login-automation`, head: `master`) with the generated title and description — do not use `gh` CLI.

### 4. Store PR URL
Save the PR URL to the verification report

## Output Files
- `docs/sdlc/pr-description.md` (optional: store description as file)
- Git branch: `feature/cucumber-login-automation`
- GitHub PR created

## Commit Message
```
[PR] Create pull request for documentation sync feature

Generated by: pr-agent
Branch: feature/cucumber-login-automation
Traceability: Full SDLC in docs/sdlc/*
```

## Tools Required
- File reading (all SDLC artifacts)
- Git operations (branch creation, commit, push)
- GitHub API/CLI (PR creation)
- Markdown formatting

## Validation

Before completing, verify:
- ✅ All changes committed
- ✅ Branch created and pushed
- ✅ PR description is comprehensive
- ✅ Test evidence included
- ✅ Reviewer checklist provided
- ✅ Traceability links included

## Success Criteria
- PR created successfully
- Description is clear and complete
- Test evidence provided
- Reviewer checklist included
- Ready for human review and merge

## Notes
- PR description should be comprehensive but scannable
- Include visual evidence (test output, reports)
- Link to all relevant artifacts
- Make reviewer's job easy with checklist
- Celebrate the completion!
