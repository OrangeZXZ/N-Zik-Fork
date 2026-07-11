# Security Rules

**Version:** 1.1.0 | **Last updated:** 2026-07-11

## Secrets & API Keys

- NEVER commit secrets, API keys, or tokens
- Use `local.properties` for local secrets (gitignored)
- Use `BuildConfig` fields for build-time secrets
- NEVER log sensitive data (tokens, passwords, user data)

## Input Validation

- Validate all user input before processing
- Sanitize data before displaying in UI
- Room handles parameterized queries automatically
- Validate URLs before opening in browser/webview

## Question Tool Input Validation

- All user input arrives via question tool responses
- Validate URLs before opening (never auto-open)
- Validate file paths (prevent path traversal)
- Reject empty/whitespace-only responses for required fields
- Trim and normalize text inputs

## Sensitive Data Storage

- Use `EncryptedSharedPreferences` for sensitive local storage
- Clear sensitive data when user logs out
- Use HTTPS for all network communications
- Do not store credentials in plain text

## License Checks

When using code from external sources (web, GitHub, StackOverflow, AI):

1. Verify the license before using it
2. Open-source (MIT, Apache, GPL) = acceptable
3. Closed-source/proprietary = NEVER acceptable
4. Always cite source and license in a comment

## Security Scenarios

| Scenario                   | Action                                                              |
| -------------------------- | ------------------------------------------------------------------- |
| Secrets found in code      | HALT immediately, remove secrets, add to .gitignore, report to user |
| License violation detected | HALT, remove code, report to user with violation details            |
| SQL injection risk         | HALT, verify Room parameterized queries, report                     |
| Hardcoded credentials      | HALT, remove credentials, use BuildConfig or local.properties       |
| Insecure network call      | HALT, switch to HTTPS, verify certificate pinning                   |
| User data leak             | HALT, identify leak source, report to user, fix immediately         |

## HALT IMMEDIATELY IF:

- You discover secrets, API keys, or tokens in code
- You detect a license violation
- You find SQL injection or XSS vulnerabilities
- You find hardcoded credentials
- You detect insecure network communication
- You find user data being leaked or logged
