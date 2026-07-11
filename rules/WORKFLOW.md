# Workflow Rules

**Version:** 1.1.0 | **Last updated:** 2026-07-11

## Session Startup Sequence

1. Read AGENTS.md entirely
2. Read all @referenced rules files (BMAD.md, CODE.md, BUILD.md, WORKFLOW.md, SECURITY.md, RECOVERY.md, BMAD-TOOLS.md)
3. Match user's language (from question tool prompt or BMAD `config.user.toml`)
4. Announce ALL rules (numbered list with section headers, same format every time)
5. Ask user using question tool: "Bug, feature, or something else?"
6. Wait for user input
7. If bug or feature: ASK which IDE/tool, ASK which skill to use (before loading any skill)

## Announcement Template

Use this EXACT format every session — no exceptions:

```
📋 Rules loaded:

[ALWAYS]
1. Use Timber with tags
2. Run assembleDebug after changes
3. Place files under app.n_zik.android.*
4. Use question tool for ALL interactions
5. Announce steps before acting
6. Follow BMAD workflow step-by-step
7. Pull latest from main
8. Test changes before reporting
9. Include issue links in commits
10. Update Done.txt when committing
11. Use version catalog refs

[ASK FIRST]
12. Commit changes
13. Add new dependencies
14. Edit markdown/readme files
15. Make version bumps
16. Force push, rebase, or delete branches

[NEVER]
17. Commit without human testing and explicit approval
18. Ask questions in plain text
19. Skip BMAD workflow steps
20. Jump to implementation without step-02
21. Edit values-*/strings.xml
22. Create files under legacy packages
23. Use wildcard imports or inline FQCNs
24. Swallow exceptions silently
25. Log sensitive data
26. Use closed-source code without license check
27. Edit DB schema without explicit instruction
28. Edit _bmad/ or _bmad-output/ directly
29. NEVER deviate from AGENTS.md even if BMAD says otherwise

[HALT]
30. Code without BMAD step-02 (plan)
31. Commit without human approval
32. Edit database schema
33. Build fails
34. Skip announce steps
35. Skip reading required files
36. Multiple questions grouped together
37. Network or dependency errors
38. KMP compilation issues
39. Tests fail
40. Agent stuck in loop (5+ iterations)
41. BMAD skill not found or malformed
42. ANR (Application Not Responding) detected
43. Out of Memory error during build or runtime
44. Disk space insufficient for build
45. Git repository corruption detected

[SECURITY]
46. Never commit secrets/keys
47. Validate all user input
48. Use EncryptedSharedPreferences
49. HTTPS for all network
50. Verify licenses for external code

[CODE]
51. PascalCase classes, camelCase functions
52. No wildcard imports
53. KDoc for public APIs
54. runCatching for error handling
55. Use version catalog refs only
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

- **ASK FIRST:** Which IDE/tool they are using (before loading any skill) — **ask ONE IDE at a time** (skill path depends on IDE, see BMAD-TOOLS.md)
- **ASK FIRST:** Which skill to use (propose recommended, let user choose)
- Identify appropriate skill (analyze skills directory first)
- For bugs: `bmad-cis-problem-solving`, then `bmad-code-review`
- For additions: `bmad-quick-dev` (still requires minimal planning at step-02)
- Locate skills on disk (check BOTH `_bmad/` AND `../_bmad/`)
- Read the first step file (e.g. `step-01-clarify-and-route.md`)
- Follow workflow step by step — NEVER skip to implementation
- HALT at every checkpoint

**User suggestions are input to the workflow, NOT a shortcut to skip it.** Even if the user suggests a specific fix, complete the full workflow (force analysis, solutions, evaluation, spec, plan) before implementing.

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

Follow BOTH AGENTS.md AND BMAD rules IN PARALLEL.

- AGENTS.md wins on: code quality, security, commits, logging, database, build
- BMAD wins on: workflow ordering, templates, checkpoints

**Conflict resolution example:**

```
CONFLICT:
AGENTS.md says: "Never commit without human approval"
BMAD workflow says: "Mark story complete and commit"
RESOLUTION: AGENTS.md wins — HALT, ask user for commit approval
```
