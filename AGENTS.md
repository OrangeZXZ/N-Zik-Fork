# AI Agent Guidelines — NZik

**MANDATORY: Read this file AND rules/\*.md before any task. Compliance for ENTIRE session.**

## Session Startup

1. Read this file entirely
2. Read all rules/\*.md files
3. Ask user via question tool: "Bug, feature, or something else?"

---

## Critical Rules (Agents Consistently Miss These)

### Code Placement

- **NEW code** → `app.n_zik.android.*` ONLY
- **NEVER** create files under `app.it.fast4x.rimusic.*` or `app.kreate.android.*` (legacy, read-only)
- **NEVER** edit `values-*/strings.xml` — only `values/strings.xml`

→ See rules/CODE.md for full file placement table

### Build & Test

- `./gradlew :ComposeN-Zik:assembleDebug` — primary dev build
- `./gradlew :ComposeN-Zik:testDebugUnitTest --tests "app.n_zik.android.SomeTest"` — single test
- `./gradlew :ComposeN-Zik:test` — all tests
- ALWAYS verify build passes after changes
- HALT after 3 failed build attempts
- New features/bug fixes should include at least one test

→ See rules/BUILD.md for build types, commit convention, version catalog, testing patterns

### NEVER (Hard Stops)

- Commit without human testing and explicit approval
- Skip BMAD workflow steps
- **Write code before completing full BMAD workflow (even if user suggests a fix)**
- Edit database schema without explicit instruction

→ See rules/WORKFLOW.md for full NEVER/HALT lists, rules/SECURITY.md for security rules

### BMAD Workflow — Mandatory

**Loading a BMAD skill ≠ Completing the workflow.** You MUST follow the skill's workflow from first step to last step, in order, without skipping.

**Before writing ANY code:** verify you have completed EVERY step of the loaded BMAD skill's workflow. If any step is incomplete → HALT, do NOT write code.

User suggestions are input to the workflow, NOT a shortcut to skip it.

→ See rules/WORKFLOW.md for full BMAD workflow enforcement

### IDE Selection — One at a Time

When asking which IDE/tool the user is using, **ask ONE option at a time** using the question tool. Skill path depends on the IDE.

→ See rules/BMAD-TOOLS.md for IDE skill directories

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

| What              | Where                                      |
| ----------------- | ------------------------------------------ |
| Main code         | `app/n_zik/android/`                       |
| Database          | `app/n_zik/android/core/database/`         |
| Player service    | `app/n_zik/android/playback/services/`     |
| UI screens        | `app/n_zik/android/components/ui/screens/` |
| Strings (English) | `res/values/strings.xml` only              |
| Tests             | `ComposeN-Zik/src/test/`                   |

---

## BMAD Method

This project uses BMAD for structured AI-assisted development.

- **Bugs** → `bmad-cis-problem-solving`, then `bmad-code-review`
- **Additions** → `bmad-quick-dev`
- AGENTS.md wins on: code quality, security, commits, logging, database, build
- BMAD wins on: workflow ordering, templates, checkpoints
- BOTH apply in parallel — if conflict, AGENTS.md wins

→ See rules/BMAD.md for installation, config resolution. See rules/WORKFLOW.md for step-by-step workflow.

---

## Communication

- Code comments & commits: English
- User communication: match user's language
- ALL user questions via question tool (never plain text)
- Show evidence (diffs, test output) — never just claim "done"
