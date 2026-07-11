# Error Recovery & Rollback Rules

**Version:** 1.1.0 | **Last updated:** 2026-07-11

## Build Failure Recovery

1. Read error messages carefully
2. Fix the first error (often cascading)
3. Rebuild
4. **HALT after 3 failed attempts** — report to user with full error log

```
BUILD FAILURE ESCALATION:
Attempt 1 → Fix obvious issue → Rebuild
Attempt 2 → Research error → Fix → Rebuild
Attempt 3 → HALT → Report to user with error log
```

## BMAD Skill Failure

If a BMAD skill fails or gets stuck:

1. **Skill not found** → Search `_bmad/` and `../_bmad/`
2. If still not found → HALT, inform user, suggest re-running BMAD installer
3. **SKILL.md malformed** → HALT, report error, suggest `bmad-module-builder` to rebuild
4. **Skill execution error** → Fallback to `bmad-quick-dev` for implementation tasks
5. **Agent stuck in loop** → HALT after 5 iterations, ask user

## Database Migration Failure

1. **NEVER** modify an already-deployed migration
2. If migration fails → HALT immediately
3. Do NOT commit migration code
4. Report error to user
5. Create new migration to fix (never edit old one)

```
MIGRATION FAILURE:
1. HALT — stop all DB operations
2. Log the exact error
3. Report to user
4. If data corruption suspected → do NOT touch DB
5. Create new migration to reverse changes if needed
```

## Code Changes Break Existing Features

1. Revert changes using `git stash` or `git checkout`
2. Identify what broke
3. Fix incrementally, testing after each change
4. If unable to fix → HALT, report to user with diagnosis

## Network / Dependency Errors

1. Check internet connection
2. Verify Maven/Gradle repositories are accessible
3. Try `./gradlew --refresh-dependencies`
4. If proxy issue → HALT, inform user
5. If repository down → HALT, suggest using cached dependencies

## KMP Compilation Issues

1. Check `commonMain` for Android-specific imports
2. Verify `expect/actual` declarations match
3. Check source set configuration
4. If unresolved → HALT, report with full compilation output

## Corrupted \_bmad/ Directory

1. **NEVER** manually edit `_bmad/` internals
2. Delete `_bmad/` and re-run installer
3. Verify with `bmad-bmb-setup` skill

## Loop Detection

If you notice yourself repeating the same action:

1. Stop immediately
2. Count iterations — HALT at 5
3. Report to user: "I'm stuck in a loop doing [X]. Please advise."
4. Wait for user input before continuing

## General Rollback

- Use `git stash` to save uncommitted changes
- Use `git checkout -- <file>` to discard changes to a file
- Use `git reset --soft HEAD~1` to undo last commit but keep changes staged
- Use `git log --oneline -5` to find safe rollback point
- **NEVER** force push without explicit user instruction
- **NEVER** delete committed history
