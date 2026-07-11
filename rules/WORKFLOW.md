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

- **ASK FIRST:** Which IDE/tool they are using (before loading any skill) — **ask ONE IDE at a time** (skill path depends on IDE, see BMAD-TOOLS.md). **Order:** OpenCode first, then Google Antigravity, then other preferred tools.
- **ASK FIRST:** Which skill to use (propose recommended, let user choose)
- Identify appropriate skill (analyze skills directory first)
- For bugs: `bmad-cis-problem-solving`, then `bmad-code-review`
- For additions: `bmad-quick-dev` (still requires minimal planning at step-02)
- Locate skills on disk (check BOTH `_bmad/` AND `../_bmad/`)

**BMAD Activation Sequence (MANDATORY for every skill):**

1. Run `resolve_customization.py` to get merged config:

   ```
   python3 {project-root}/_bmad/scripts/resolve_customization.py --skill {skill-root} --key agent
   ```

   (or `--key workflow` for workflow skills)

2. If script fails → manually read 3 files in order and merge:
   - `{skill-root}/customize.toml` (defaults)
   - `{project-root}/_bmad/custom/{skill-name}.toml` (team)
   - `{project-root}/_bmad/custom/{skill-name}.user.toml` (personal)

3. Execute `activation_steps_prepend` (before greeting)

4. Load `persistent_facts` — file refs loaded as context, literal text kept verbatim

5. Load config from `_bmad/bmm/config.yaml` (user_name, languages, paths)

6. Adopt persona (role, identity, communication_style, principles)

7. Greet user using `{user_name}` and `{communication_language}`

8. Execute `activation_steps_append` (after greeting, before workflow)

**After activation, read the ENTIRE SKILL.md file before starting the workflow.** Do NOT just read the step headers — read EVERY line including:

- `<template-output>` tags (what to produce at each step)
- `<energy-checkpoint>` tags (when to ask for breaks)
- Checkpoint instructions (what options to present after each step)
- `<action>` tags (what scripts to run)
- All instructions between step tags

**After activation, follow the skill's workflow step by step — NEVER skip to implementation.**

**HALT at every checkpoint.**

**Spec Production (MANDATORY):**

- If the skill has a `template.md` → you MUST produce a spec document using that template
- After every `<template-output>`, save the artifact to `{default_output_file}`
- Show checkpoint separator, display generated content, present options `[a] Advanced Elicitation`, `[c] Continue`, `[p] Party-Mode`, `[y] YOLO`
- Wait for user response before proceeding to next step
- NEVER skip spec production — the spec IS the workflow output

**on_complete hook (MANDATORY):**

- After workflow completes, run: `python3 {project-root}/_bmad/scripts/resolve_customization.py --skill {skill-root} --key workflow.on_complete`
- If the resolved value is non-empty → follow it as final terminal instruction before exiting
- NEVER skip this hook — it is the skill's official completion action

**Energy checkpoints (MANDATORY):**

- If the skill has `<energy-checkpoint>` tags → pause and ask the user about their energy level
- Present the checkpoint message exactly as written in the skill
- Wait for user response before proceeding
- NEVER skip energy checkpoints — they prevent burnout during long sessions

**Co/Fast path choice (MANDATORY):**

- Some skills (bmad-architecture, bmad-prd, bmad-ux, bmad-product-brief) require offering:
  - **Coaching path** — guided, explains each step
  - **Fast path** — streamlined, skips explanations
- Ask user which path before any drafting begins
- NEVER skip this choice — it affects the entire workflow

**external_handoffs (MANDATORY):**

- If the skill has `{workflow.external_handoffs}` → execute it and surface returned URLs/IDs
- Skip and flag unavailable tools (don't crash)
- This routes artifacts to external systems (Confluence, Notion, Jira, etc.)

**doc_standards (MANDATORY):**

- If the skill has `{workflow.doc_standards}` → apply them in order
- Structural passes before prose — do not polish soon-to-be-cut text

**finalize_reviewers (MANDATORY):**

- If the skill has `{workflow.finalize_reviewers}` → dispatch reviewer lenses as parallel subagents
- Each reviewer lens evaluates the artifact independently
- Surface all reviewer feedback to user before finalizing

**Before writing ANY code:** verify you have completed EVERY step of the loaded BMAD skill's workflow. Read the skill's step files in order — if any step is incomplete → HALT, do NOT write code.

**Enforcement — before starting the workflow:**

1. Read the skill's SKILL.md file
2. Count the total number of steps in the `<workflow>` section
3. List all steps: "Steps: 1. X, 2. Y, 3. Z, ..."
4. Announce: "BMAD workflow has N steps. Starting step 1."

**Enforcement — during the workflow:**

- Before each action, announce: `[BMAD Step X/N: <step name>]`
- Before moving to next step, ask user: "Step X complete. Proceed to step Y?"
- Before implementing, verify: "All N steps complete. Ready to implement?"
- If you cannot name the current step → HALT, you are lost

**User suggestions are input to the workflow, NOT a shortcut to skip it.** Even if the user suggests a specific fix, complete the skill's full workflow before implementing.

**If user declines BMAD skill:** HALT and explain that BMAD workflow is mandatory per AGENTS.md rules. Ask user to confirm they want to proceed without BMAD.

**If skill not found:**

1. Search both `_bmad/` and `../_bmad/`
2. If still not found → HALT, inform user, suggest re-running BMAD installer
3. If SKILL.md is malformed → HALT, report error, suggest `bmad-module-builder` to rebuild

## Step-File Architecture

Some skills use micro-file design where each step is in its own file.

**Rules (NO EXCEPTIONS):**

- NEVER load multiple step files simultaneously
- ALWAYS read entire step file before execution
- NEVER skip steps or optimize the sequence
- ALWAYS follow exact instructions in the step file
- ALWAYS halt at checkpoints and wait for human input
- Load next step file ONLY when directed by current step

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
