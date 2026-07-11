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

**Invocation:**

```
python3 {project-root}/_bmad/scripts/resolve_customization.py --skill {skill-root} --key agent
python3 {project-root}/_bmad/scripts/resolve_customization.py --skill {skill-root} --key workflow
```

**If script fails** → manually read 3 files in order and merge:

1. `{skill-root}/customize.toml` (defaults)
2. `{project-root}/_bmad/custom/{skill-name}.toml` (team)
3. `{project-root}/_bmad/custom/{skill-name}.user.toml` (personal)

**Merge rules:** Scalars override, tables deep-merge, keyed arrays merge by `code` or `id`, other arrays append. **No removal mechanism** — to suppress a default, override by `code` with no-op.

**Key files:**

- `persistent_facts` — Rules that travel with the agent into every workflow (file refs loaded, literal text kept verbatim)
- `activation_steps_prepend` — Runs BEFORE greeting
- `activation_steps_append` — Runs AFTER greeting, BEFORE menu

**Critical rules:**

- NEVER edit `customize.toml` — it is overwritten on every update. All customization goes in `_bmad/custom/`.
- Override files must be sparse — only include fields being changed.
- `agent.name` and `agent.title` are read-only — overrides have no effect.
- File references use `{project-root}` prefix.
- Present output in `{communication_language}` from resolved config.
- Prefix all messages with `{agent.icon}` throughout session.

**Full IDE skill directories table:** See `rules/BMAD-TOOLS.md`

## memlog.py — Session Memory System

Some skills use `memlog.py` for append-only session memory.

**Invocation:**

```
python3 {project-root}/_bmad/scripts/memlog.py init --workspace {doc_workspace} --field topic="<topic>"
python3 {project-root}/_bmad/scripts/memlog.py append --workspace {doc_workspace} --type <type> --text "<text>"
python3 {project-root}/_bmad/scripts/memlog.py set --workspace {doc_workspace} --key status --value complete
```

**Types:** decision, constraint, capability, assumption, question, direction, note, event

**Rules:**

- NEVER write memlog files by hand — use the script only
- All writes are atomic and append-only
- The `.memlog.md` file is the run's canonical memory and audit trail

## Agent Icon Prefix

For agent skills, prefix ALL messages with `{agent.icon}` throughout the ENTIRE session — not just the greeting.

**Example:** If icon is "🎯", every message starts with "🎯 ..."

## resolve_config.py — Central Config Resolution

Some skills (bmad-help, bmad-advanced-elicitation) use `resolve_config.py` for project-wide configuration.

**Invocation:**

```
python3 {project-root}/_bmad/scripts/resolve_config.py
```

**This is different from `resolve_customization.py`:**

- `resolve_customization.py` → per-skill config (3-layer merge)
- `resolve_config.py` → central project config (4-layer merge)

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
