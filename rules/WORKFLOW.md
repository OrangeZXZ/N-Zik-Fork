# Workflow Rules

**Version:** 1.1.0 | **Last updated:** 2026-07-11

## Session Startup Sequence

1. Read AGENTS.md entirely
2. Read all @referenced rules files (BMAD.md, CODE.md, BUILD.md, WORKFLOW.md, SECURITY.md, RECOVERY.md, BMAD-TOOLS.md)
3. Match user's language (from question tool prompt or BMAD `config.user.toml`)
4. Announce critical rules (simplified list below)
5. Ask user using question tool: "Bug, feature, or something else?"
6. Wait for user input
7. If bug or feature: ASK which IDE/tool (ONE at a time), ASK which skill to use

## Announcement Template

Use this format every session — keep it SHORT:

```
📋 Rules loaded:

[CRITICAL]
1. Code → app.n_zik.android.* only (legacy packages READ-ONLY)
2. Timber with tags ONLY (no println/Log.d)
3. Build: ./gradlew :ComposeN-Zik:assembleDebug
4. Version catalog refs only (libs.versions.toml)
5. NEVER commit without human approval
6. NEVER skip BMAD workflow — complete FULL workflow before coding
7. User suggestions ≠ shortcut (still complete workflow)
8. Ask IDE ONE at a time (path depends on IDE)
9. NEVER edit values-*/strings.xml (only values/)
10. NEVER edit DB schema without explicit instruction

See rules/*.md for full details.
```

## Step-by-Step Workflow

### Step 1: Understand

- Read request carefully
- Ask clarifying questions (each as separate question tool call)
- Identify scope (modules, files, packages affected)

### Step 2: Explore

- Search existing implementations
- Read neighboring files for conventions
- Check imports and dependencies
- Consult reference projects in `docs/` if needed

### Step 3: Execute BMAD Skill (MANDATORY)

NEVER write code or create implementation plans without completing this step.

**Loading a skill ≠ Completing the workflow.** You MUST complete ALL sub-steps below.

- **ASK FIRST:** Which IDE/tool they are using (before loading any skill) — **ask ONE IDE at a time** (skill path depends on IDE, see BMAD-TOOLS.md)
- **ASK FIRST:** Which skill to use (propose recommended, let user choose)
- Identify appropriate skill (analyze skills directory first)
- For bugs: `bmad-cis-problem-solving`, then `bmad-code-review`
- For additions: `bmad-quick-dev` (still requires minimal planning at step-02)
- Locate skills on disk (check BOTH `_bmad/` AND `../_bmad/`)
- Read the first step file (e.g. `step-01-clarify-and-route.md`)
- Follow workflow step by step — NEVER skip to implementation
- HALT at every checkpoint

**Before writing ANY code:** verify you have completed EVERY step of the loaded BMAD skill's workflow. Read the skill's step files in order — if any step is incomplete → HALT, do NOT write code.

**User suggestions are input to the workflow, NOT a shortcut to skip it.** Even if the user suggests a specific fix, complete the skill's full workflow before implementing.

**If user declines BMAD skill:** HALT and explain that BMAD workflow is mandatory per AGENTS.md rules. Ask user to confirm they want to proceed without BMAD.

**If skill not found:**

1. Search both `_bmad/` and `../_bmad/`
2. If still not found → HALT, inform user, suggest re-running BMAD installer
3. If SKILL.md is malformed → HALT, report error, suggest `bmad-module-builder` to rebuild

### Step 4: Implement

- Write clean code following all guidelines
- Follow existing patterns
- Handle errors appropriately
- Remove dead code

### Step 5: Verify

- Build: `./gradlew :ComposeN-Zik:assembleDebug`
- Run tests if available
- Review changes for quality

### Step 6: Report

- Summarize what was done and why
- Note files modified or created
- Do NOT commit unless explicitly asked

## Multi-Module Changes

When changes span multiple modules (`extensions/`, `modules/`, `ComposeN-Zik/`):

1. Identify all affected modules before starting
2. Build each module individually if possible
3. Test cross-module interactions
4. Verify no circular dependencies introduced
5. Report which modules were affected

## Announce Steps

Before ANY file edit, command, or tool call, output a short plan:

- Detailed plan when starting new task or deviating
- One-line tag `[Step: <name>] [Rule: <#>]` between actions
- NEVER skip this rule, even for "obvious" fixes

## BMAD Dual Enforcement

Follow BOTH AGENTS.md AND BMAD rules IN PARALLEL — at EVERY step of the workflow.

- AGENTS.md wins on: code quality, security, commits, logging, database, build
- BMAD wins on: workflow ordering, templates, checkpoints
- **AGENTS.md rules apply DURING the BMAD workflow, not just after**

**Conflict resolution example:**

```
CONFLICT:
AGENTS.md says: "Never commit without human approval"
BMAD workflow says: "Mark story complete and commit"
RESOLUTION: AGENTS.md wins — HALT, ask user for commit approval
```

**NEVER use "I'm following BMAD" as an excuse to skip AGENTS.md rules.**
