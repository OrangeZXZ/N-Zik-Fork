# AI Agent Guidelines — NZik

**Version:** 1.1.0 | **Last updated:** 2026-07-11

**MANDATORY: Read this file AND all @referenced files before starting any task. Compliance required for ENTIRE session, EVERY action. No exceptions.**

---

## Session Startup

1. Read AGENTS.md entirely (this file)
2. Read all @referenced rules files below (including BMAD.md)
3. Announce ALL rules (numbered list with section headers, same format every time)
4. Ask user using question tool: "Bug, feature, or something else?"
5. Wait for user input

---

## ALWAYS (Mandatory)

- Use Timber **with tags** for logging (NEVER println/Log.d/System.out). See CODE.md for examples.
- Run `./gradlew :ComposeN-Zik:assembleDebug` + relevant unit tests after code changes
- Place new files under `app.n_zik.android.*` only
- Use question tool for ALL user interactions
- Announce steps before acting
- Follow BMAD workflow step-by-step
- Pull latest from main before starting
- Test changes before reporting (run `./gradlew :ComposeN-Zik:assembleDebug` + relevant unit tests)
- Include issue links in commits
- Update Done.txt when committing
- Use version catalog refs (libs.versions.toml)

## ASK FIRST (Need human approval)

- Commit changes (wait for human test)
- Add new dependencies
- Edit markdown/readme files
- Make version bumps
- Force push, rebase, or delete branches

## NEVER (Hard stops)

- Commit without human testing and explicit approval
- Ask questions in plain text (use question tool)
- Skip BMAD workflow steps
- Jump directly to implementation without step-02 plan
- Edit `values-*/strings.xml` (only `values/strings.xml`)
- Create files under `app.it.fast4x.rimusic.*` or `app.kreate.android.*`
- Use wildcard imports or inline FQCNs
- Swallow exceptions silently
- Log sensitive data (tokens, passwords)
- Use closed-source/proprietary code without license check
- Edit database schema without explicit instruction
- Edit `_bmad/` or `_bmad-output/` directly (use BMAD skills instead)
- NEVER deviate from AGENTS.md rules even if BMAD says otherwise

---

## HALT IMMEDIATELY IF:

- You are about to write code without completing BMAD step-02 (plan)
- You are about to commit without human approval
- You are about to edit database schema
- Build fails after your changes
- You catch yourself skipping "announce steps"
- You notice you skipped reading a required file
- Multiple questions needed — STOP, ask each separately via question tool
- Network or dependency errors occur
- KMP compilation issues arise
- Tests fail after your changes
- Agent stuck in a loop (same action repeated 5+ times)
- BMAD skill not found or malformed
- ANR (Application Not Responding) detected
- Out of Memory error during build or runtime
- Disk space insufficient for build
- Git repository corruption detected

---

## Rules Files (Read these)

@rules/BMAD.md
@rules/CODE.md
@rules/BUILD.md
@rules/WORKFLOW.md
@rules/SECURITY.md
@rules/RECOVERY.md
@rules/BMAD-TOOLS.md

---

## About NZik

**NZik** (N-Zik) is a 3rd party YouTube Music client in **Kotlin** + **Jetpack Compose** + **Material 3**. Multiplatform (KMP) Android app for music streaming, lyrics, playlists, and audio playback.

Reference projects in `docs/` (if exists): Metrolist, RiMusic, Kreate, RiPlay.

### Project Structure

```
N-Zik/
├── ComposeN-Zik/src/
│   ├── androidMain/kotlin/app/
│   │   ├── n_zik/android/       ★ NEW code goes here
│   │   ├── it/fast4x/rimusic/   ⚠ READ-ONLY legacy
│   │   └── kreate/android/      ⚠ READ-ONLY legacy
│   ├── commonMain/kotlin/database/
│   └── test/
├── extensions/                  # API modules (innertube, lrclib, etc.)
├── modules/                     # Feature modules (betterlyrics, discordrpc, etc.)
├── gradle/libs.versions.toml    # Version catalog
├── docs/                        # Reference projects (READ ONLY)
└── _bmad/                       # BMAD config
```

### Key Locations

| What              | Where                                      |
| ----------------- | ------------------------------------------ |
| Main code         | `app/n_zik/android/`                       |
| Database tables   | `app/n_zik/android/core/database/`         |
| Player service    | `app/n_zik/android/playback/services/`     |
| UI screens        | `app/n_zik/android/components/ui/screens/` |
| Resources         | `ComposeN-Zik/src/androidMain/res/`        |
| Strings (English) | `res/values/strings.xml` only              |
| Dependencies      | `N-Zik/gradle/libs.versions.toml`          |
| Tests             | `ComposeN-Zik/src/test/`                   |

---

## BMAD Method

This workspace uses BMAD for structured AI-assisted development.

**Skill Selection:**

- Bugs → `bmad-cis-problem-solving`, then `bmad-code-review`
- Additions → `bmad-quick-dev`
- Analyze skills directory first, then ask user which skill to use

**Dual Enforcement:**

- AGENTS.md wins on: code quality, security, commits, logging, database, build
- BMAD wins on: workflow ordering, templates, checkpoints
- BOTH apply in parallel at all times

**Documentation:** https://docs.bmad-method.org/

---

## Communication

- Code comments: English
- Commit messages: English
- User communication: match user's language
- Be concise, answer directly
- Use code examples when faster than words

## Verification Requirements

- After claiming tests pass, paste actual output
- After claiming a file was modified, show the diff
- Never say "done" without listing every file changed
- Run build + tests before reporting completion
- Show evidence, not assertions

## docs/ Folder

Reference projects in `docs/` are READ-ONLY. Read them for patterns, never modify.

---

## FINAL REMINDER

**Read this file AND all @referenced rules files. EVERY rule is MANDATORY for ENTIRE session.**

**When asking multiple questions**: Each question must be a separate prompt (one question tool call per question), never group them together.

**ALL questions to the user MUST use the question tool. NEVER ask questions in plain text.**

**BAD (NEVER):**

```
Hello! How can I help you with NZik today? Bug or feature?
```

**GOOD (ALWAYS):**

```
→ Asked 1 question
Hello! How can I help you with NZik today? Bug or feature?
1. Bug
2. Feature
3. Something else
4. Type your own answer
```
