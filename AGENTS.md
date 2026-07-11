# AI Agent Guidelines — NZik

**MANDATORY: Read this file AND rules/*.md before any task. Compliance for ENTIRE session.**

## Session Startup

1. Read this file entirely
2. Read all rules/*.md files
3. Ask user via question tool: "Bug, feature, or something else?"

---

## Critical Rules

### Code Placement
- **NEW code** → `app.n_zik.android.*` ONLY
- **NEVER** create files under `app.it.fast4x.rimusic.*` or `app.kreate.android.*` (legacy, read-only)
- **NEVER** edit `values-*/strings.xml` — only `values/strings.xml`

### Build & Test
- `./gradlew :ComposeN-Zik:assembleDebug` — primary dev build
- `./gradlew :ComposeN-Zik:testDebugUnitTest --tests "app.n_zik.android.SomeTest"` — single test
- `./gradlew :ComposeN-Zik:test` — all tests
- ALWAYS verify build passes after changes
- HALT after 3 failed build attempts

### Dependencies
- Use version catalog refs from `gradle/libs.versions.toml` — NEVER hardcode versions
- If library not in catalog → HALT, ask user before adding

### Logging
- Timber with tags ONLY — NEVER `println`, `Log.d`, `System.out`, `e.printStackTrace()`
- See CODE.md for examples and log levels

### Database
- NEVER edit schema without explicit instruction
- Never modify deployed migrations — create new ones
- Migration failure → HALT immediately

### Git & Commits
- Format: `type(scope): short description` (under 72 chars, imperative mood)
- Types: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `perf`
- Include GitHub issue URL when applicable
- Update `assets/notes/Done.txt` when committing
- NEVER commit without human testing and explicit approval

### KMP Source Sets
- `commonMain` → shared logic (NO Android imports)
- `androidMain` → Android-specific code
- Use `expect/actual` in correct source sets

---

## ASK FIRST (Human Approval Required)
- Commit changes
- Add new dependencies
- Edit markdown/readme files
- Version bumps
- Force push, rebase, delete branches

## NEVER (Hard Stops)
- Commit without human approval
- Ask questions in plain text (use question tool)
- Skip BMAD workflow steps
- Use wildcard imports or inline FQCNs
- Swallow exceptions silently
- Log sensitive data (tokens, passwords)
- Edit `_bmad/` or `_bmad-output/` directly (use BMAD skills)
- Use closed-source code without license check

## HALT IMMEDIATELY IF
- Build or tests fail after changes
- DB schema edit without instruction
- Agent stuck in loop (5+ same actions)
- Network/dependency errors
- KMP compilation issues
- Multiple questions needed — ask each separately

---

## Project Structure

```
N-Zik/
├── ComposeN-Zik/src/
│   ├── androidMain/kotlin/app/
│   │   ├── n_zik/android/       ★ NEW code here
│   │   ├── it/fast4x/rimusic/   ⚠ READ-ONLY legacy
│   │   └── kreate/android/      ⚠ READ-ONLY legacy
│   ├── commonMain/kotlin/database/
│   └── test/
├── extensions/                  API modules (innertube, lrclib, piped, etc.)
├── modules/                     Feature submodules (betterlyrics, discordrpc, nextvisualizer)
├── gradle/libs.versions.toml    Version catalog
└── docs/                        Reference projects (READ-ONLY)
```

| What | Where |
|---|---|
| Main code | `app/n_zik/android/` |
| Database | `app/n_zik/android/core/database/` |
| Player service | `app/n_zik/android/playback/services/` |
| UI screens | `app/n_zik/android/components/ui/screens/` |
| Strings (English) | `res/values/strings.xml` only |
| Tests | `ComposeN-Zik/src/test/` |

---

## BMAD Method

This project uses BMAD for structured AI-assisted development.

- **Bugs** → `bmad-cis-problem-solving`, then `bmad-code-review`
- **Additions** → `bmad-quick-dev`
- AGENTS.md wins on: code quality, security, commits, logging, database, build
- BMAD wins on: workflow ordering, templates, checkpoints
- BOTH apply in parallel — if conflict, AGENTS.md wins

---

## Communication
- Code comments & commits: English
- User communication: match user's language
- ALL user questions via question tool (never plain text)
- Show evidence (diffs, test output) — never just claim "done"
