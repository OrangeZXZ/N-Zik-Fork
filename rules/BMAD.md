# BMAD Technical Reference

**Version:** 1.1.0 | **Last updated:** 2026-07-11

**MANDATORY: Read this file before executing any BMAD skill.**

---

## Installation Location

Before creating/writing to `_bmad/` or `_bmad-output/`, verify where BMAD is installed:

1. Check if `_bmad/` exists in current project root
2. Check if `_bmad/` exists in parent directory
3. NEVER create new `_bmad/` folder if one already exists elsewhere
4. Use existing installation path for all operations

---

## Installation Structure

```
_bmad/
├── _config/                    # Installer metadata (manifest.yaml, CSVs)
├── config.toml                 # Central config — TEAM layer
├── config.user.toml            # Central config — USER layer
├── custom/                     # Human-authored overrides
│   ├── config.toml             # Team overrides (committed)
│   └── config.user.toml        # User overrides (gitignored)
├── scripts/                    # resolve_config.py, resolve_customization.py
├── core/config.yaml            # Core module config
├── <module>/config.yaml        # Per-module config (bmm, cis, wds, etc.)
└── memory/                     # Agent runtime state (NOT installer-managed)
```

## Config Resolution (4-Layer TOML Merge)

| Priority    | Path                            | Owner     | Committed?         |
| ----------- | ------------------------------- | --------- | ------------------ |
| 1 (lowest)  | `_bmad/config.toml`             | Installer | Yes                |
| 2           | `_bmad/config.user.toml`        | Installer | Yes                |
| 3           | `_bmad/custom/config.toml`      | Human     | Yes                |
| 4 (highest) | `_bmad/custom/config.user.toml` | Human     | No (\*.gitignored) |

**Merge rules:** Scalars override, tables deep-merge, keyed arrays merge by key, other arrays append.

## Skill Customization (3-Layer TOML Merge)

| Priority    | Path                                  | Owner | Committed?         |
| ----------- | ------------------------------------- | ----- | ------------------ |
| 1 (lowest)  | Skill's `customize.toml`              | Skill | Yes (read-only)    |
| 2           | `_bmad/custom/{skill-name}.toml`      | Team  | Yes                |
| 3 (highest) | `_bmad/custom/{skill-name}.user.toml` | User  | No (\*.gitignored) |

**Key files:**

- `persistent_facts` — Rules that travel with the agent into every workflow
- `activation_steps_prepend` — Runs BEFORE greeting
- `activation_steps_append` — Runs AFTER greeting, BEFORE menu

**Full IDE skill directories table:** See `rules/BMAD-TOOLS.md`

## Command Pointer Files

OpenCode: `.opencode/commands/` with `@skills/{canonicalId}` format.
Copilot: `.github/agents/` with `LOAD the FULL {path}/SKILL.md` format.

## Skill Naming

- Agents: `bmad-agent-{name}.md` (core) or `bmad-agent-{module}-{name}.md`
- Workflows: `bmad-{module}-{name}.md`

---

## Documentation

- https://docs.bmad-method.org/
- https://github.com/bmad-code-org/BMAD-METHOD
