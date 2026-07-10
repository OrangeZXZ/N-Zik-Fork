# BMAD Technical Reference

This file contains the complete technical structure of BMAD extracted from the installer source (`tools/installer/`). **Consult this when unsure about file locations, naming, or config resolution.**

**IMPORTANT: Read this file when executing BMAD skills.**

---

## 0. Installation Location Check

Before creating or writing to `_bmad/` or `_bmad-out/`, **ALWAYS verify** where BMAD is installed:

1. Check if `_bmad/` exists in the current project root
2. Check if `_bmad/` exists in the parent directory
3. **NEVER create a new `_bmad/` or `_bmad-out/` folder** if one already exists elsewhere
4. Use the existing installation path for all BMAD operations

**Why:** The installer may have been run from the parent directory. Creating duplicate folders causes conflicts and breaks the BMAD workflow.

---

## 1. Installation Structure

The installer creates `_bmad/` in the project root. Every file and directory below is managed by the installer — do NOT create or rename them manually.

```
_bmad/                                 # BMAD_FOLDER_NAME (centralized constant)
│
├── _config/                           # Installer-owned metadata
│   ├── manifest.yaml                  # Installation metadata: version, modules, dates, IDEs
│   ├── files-manifest.csv             # SHA256 hash of every installed file (detects user modifications)
│   ├── skill-manifest.csv             # Skills copied to IDEs: canonicalId, name, description, module, path
│   ├── bmad-help.csv                  # Merged help catalog from all module-help.csv files
│   └── agents/                        # Generated agent .customize.yaml files
│
├── config.toml                        # Central config — TEAM layer (committed, installer-owned)
├── config.user.toml                   # Central config — USER layer (committed, installer-owned)
│
├── custom/                            # Human-authored config overrides (version-controlled)
│   ├── .gitignore                     # Contains: *.user.toml
│   ├── config.toml                    # Team overrides (committed)
│   └── config.user.toml               # User overrides (gitignored by *.user.toml pattern)
│
├── scripts/                           # Shared Python scripts (copied from src/scripts/)
│   ├── resolve_config.py              # 4-layer TOML config resolution
│   ├── resolve_customization.py       # Customization resolution
│   └── memlog.py                      # Memory logging
│
├── core/                              # Core module (always installed)
│   └── config.yaml                    # Core config: user_name, project_name, etc.
│
├── <module>/                          # Each installed module (bmm, cis, wds, etc.)
│   └── config.yaml                    # Module config with core values merged in
│
└── memory/                            # Agent runtime state (NOT installer-managed)
```

---

## 2. Config Resolution (4-Layer TOML Merge)

`resolve_config.py` merges 4 TOML layers (highest priority last):

| Priority    | Path                            | Owner     | Committed?         |
| ----------- | ------------------------------- | --------- | ------------------ |
| 1 (lowest)  | `_bmad/config.toml`             | Installer | Yes                |
| 2           | `_bmad/config.user.toml`        | Installer | Yes                |
| 3           | `_bmad/custom/config.toml`      | Human     | Yes                |
| 4 (highest) | `_bmad/custom/config.user.toml` | Human     | No (\*.gitignored) |

**Merge rules:**

- Scalars: override wins
- Tables: deep merge
- Arrays of tables where every item shares `code` or `id`: merge by that key
- All other arrays: append

**Run manually:**

```bash
uv run _bmad/scripts/resolve_config.py --project-root /path/to/project
uv run _bmad/scripts/resolve_config.py --project-root ... --key core
uv run _bmad/scripts/resolve_config.py --project-root ... --key agents
```

Requires Python 3.11+ (`tomllib` stdlib).

---

## 3. IDE Skill Directories (Full Reference)

From `tools/installer/ide/platform-codes.yaml`:

**Legend:**

- **Skills dir** = `{target_dir}` — where skill directories are installed (project/workspace)
- **Global dir** = `{global_target_dir}` — user-home directory for global install
- **Commands dir** = `{commands_target_dir}` — where command pointer files are generated
- **Commands ext** = `{commands_extension}` — file extension for command pointers
- **Filter** = `{commands_filter}` — only surface certain artifact types in picker

### Preferred tools (recommended during install)

| Tool                  | Skills dir        | Global dir          | Commands dir          | Commands ext | Filter        |
| --------------------- | ----------------- | ------------------- | --------------------- | ------------ | ------------- |
| **Claude Code** ⭐    | `.claude/skills/` | `~/.claude/skills/` | —                     | —            | —             |
| **Cursor** ⭐         | `.agents/skills/` | `~/.agents/skills/` | —                     | —            | —             |
| **GitHub Copilot** ⭐ | `.agents/skills/` | `~/.agents/skills/` | `.github/agents/`     | `.agent.md`  | `agents-only` |
| **Codex** ⭐          | `.agents/skills/` | `~/.codex/skills/`  | —                     | —            | —             |
| OpenCode              | `.agents/skills/` | `~/.agents/skills/` | `.opencode/commands/` | —            | —             |

### All other tools

| Tool               | Skills dir            | Global dir                      | Commands dir | Commands ext | Filter |
| ------------------ | --------------------- | ------------------------------- | ------------ | ------------ | ------ |
| AdaL               | `.adal/skills/`       | `~/.adal/skills/`               | —            | —            | —      |
| Sourcegraph Amp    | `.agents/skills/`     | `~/.config/agents/skills/`      | —            | —            | —      |
| Google Antigravity | `.agent/skills/`      | `~/.gemini/antigravity/skills/` | —            | —            | —      |
| Auggie             | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| IBM Bob            | `.bob/skills/`        | `~/.bob/skills/`                | —            | —            | —      |
| Cline              | `.cline/skills/`      | `~/.cline/skills/`              | —            | —            | —      |
| CodeWhale          | `.codewhale/skills/`  | `~/.codewhale/skills/`          | —            | —            | —      |
| CodeBuddy          | `.codebuddy/skills/`  | `~/.codebuddy/skills/`          | —            | —            | —      |
| Command Code       | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| Snowflake Cortex   | `.cortex/skills/`     | `~/.snowflake/cortex/skills/`   | —            | —            | —      |
| Crush              | `.agents/skills/`     | `~/.config/agents/skills/`      | —            | —            | —      |
| Factory Droid      | `.factory/skills/`    | `~/.factory/skills/`            | —            | —            | —      |
| Firebender         | `.firebender/skills/` | `~/.agents/skills/`             | —            | —            | —      |
| Gemini CLI         | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| Block Goose        | `.agents/skills/`     | `~/.config/agents/skills/`      | —            | —            | —      |
| Hermes Agent       | `.agents/skills/`     | `~/.hermes/skills/`             | —            | —            | —      |
| iFlow              | `.iflow/skills/`      | `~/.iflow/skills/`              | —            | —            | —      |
| Junie              | `.junie/skills/`      | `~/.junie/skills/`              | —            | —            | —      |
| KiloCoder          | `.agents/skills/`     | `~/.kilocode/skills/`           | —            | —            | —      |
| Kimi Code          | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| Kiro               | `.kiro/skills/`       | `~/.kiro/skills/`               | —            | —            | —      |
| Kode               | `.kode/skills/`       | `~/.kode/skills/`               | —            | —            | —      |
| Mistral Vibe       | `.agents/skills/`     | `~/.vibe/skills/`               | —            | —            | —      |
| Mux                | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| Neovate            | `.neovate/skills/`    | `~/.neovate/skills/`            | —            | —            | —      |
| Ona                | `.ona/skills/`        | —                               | —            | —            | —      |
| OpenClaw           | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| OpenHands          | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| Pi                 | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| Pochi              | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| Qoder              | `.qoder/skills/`      | `~/.qoder/skills/`              | —            | —            | —      |
| QwenCoder          | `.qwen/skills/`       | `~/.qwen/skills/`               | —            | —            | —      |
| Replit Agent       | `.agents/skills/`     | —                               | —            | —            | —      |
| Roo Code           | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| Rovo Dev           | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| Trae               | `.trae/skills/`       | —                               | —            | —            | —      |
| Warp               | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| Windsurf           | `.agents/skills/`     | `~/.agents/skills/`             | —            | —            | —      |
| Zencoder           | `.zencoder/skills/`   | `~/.zencoder/skills/`           | —            | —            | —      |

⭐ = Preferred (shown first during install)

**Shared directories:** Many tools share `.agents/skills/` (Cursor, Copilot, Codex, Gemini CLI, OpenHands, Warp, Windsurf, Roo Code, etc.). The installer checks for ancestor conflicts before writing to shared directories.

**Source:** `tools/installer/ide/platform-codes.yaml` from BMAD-METHOD repo.
**Doc:** https://docs.bmad-method.org/reference/commands/

---

## 4. Command Pointer Files

For tools that support command pointers (OpenCode, Copilot), the installer generates `.md` files in the commands directory.

**OpenCode pointer format** (`.opencode/commands/`):

```markdown
---
description: <skill description from manifest>
---

@skills/{canonicalId}
```

**GitHub Copilot pointer format** (`.github/agents/`):

```markdown
---
description: <skill description from manifest>
---

LOAD the FULL {project-root}/{target_dir}/{canonicalId}/SKILL.md, READ its entire contents and follow its directions exactly!
```

**Naming:** `toDashPath()` converts `bmm/agents/pm.md` → `bmad-agent-bmm-pm.md`

**Reserved commands** (skipped to avoid shadowing built-ins): `review`, `commit`, `init`, `help`, `skills`, `fast`, `compact`, `clear`, `undo`, `redo`, `edit`, `editor`, `exit`, `quit`, `theme`, `config`, `model`, `session`

---

## 5. Skill Naming Conventions

From `path-utils.js` — the installer flattens hierarchical paths to dash-separated names:

| Source path                       | Generated filename                                     |
| --------------------------------- | ------------------------------------------------------ |
| `bmm/agents/pm.md`                | `bmad-agent-bmm-pm.md`                                 |
| `bmm/workflows/correct-course.md` | `bmad-bmm-correct-course.md`                           |
| `core/agents/brainstorming.md`    | `bmad-agent-brainstorming.md` (core skips module name) |
| `standalone/agents/fred.md`       | `bmad-agent-standalone-fred.md`                        |
| `cis/agents/storymaster.md`       | `bmad-agent-cis-storymaster.md`                        |

**Rules:**

- Agents get `bmad-agent-` prefix
- Core module skips its name: `bmad-agent-{name}.md`
- Standalone includes `standalone`: `bmad-agent-standalone-{name}.md`
- Other modules: `bmad-agent-{module}-{name}.md`
- Non-agents (workflows/tasks/tools): `bmad-{module}-{name}.md`

---

## 6. Manifest Tracking

**`_config/manifest.yaml`** — Installation state:

```yaml
installation:
  version: '6.10.0'
  installDate: '2026-07-08T...'
  lastUpdated: '2026-07-08T...'
modules:
  - name: core
    version: '6.10.0'
    installDate: '...'
    lastUpdated: '...'
    source: built-in
  - name: bmm
    version: '1.7.0'
    source: external
ides:
  - opencode
```

**`_config/files-manifest.csv`** — Installed file tracking with SHA256 hashes for detecting user modifications.

**`_config/skill-manifest.csv`** — Skills copied to IDEs:

```
canonicalId,name,description,module,path
```

**`_config/bmad-help.csv`** — Merged help catalog:

```
module,skill,display-name,menu-code,description,action,args,phase,preceded-by,followed-by,required,output-location,outputs
```

---

## 7. Shared Python Scripts

Copied from `src/scripts/` to `_bmad/scripts/` during install. Excludes tests, `__pycache__`, `.pytest_cache`.

| Script                     | Purpose                           |
| -------------------------- | --------------------------------- |
| `resolve_config.py`        | 4-layer TOML config merge (see 2) |
| `resolve_customization.py` | Customization file resolution     |
| `memlog.py`                | Memory logging                    |

**Execution:** `uv run _bmad/scripts/resolve_config.py --project-root /path/to/project`

---

## 8. Update Behavior

During updates, the installer:

1. Detects custom files (not in files-manifest.csv) → preserves them
2. Detects modified files (SHA256 changed vs manifest) → backs up as `.bak`
3. Backs up to `_bmad-custom-backup-temp/` and `_bmad-modified-backup-temp/` (cleaned up after)
4. Preserves `_bmad/custom/` directory and `*.user.toml` files
5. Preserves `memory/` and `_memory/` directories (agent runtime state)
6. Does NOT treat `config.yaml` files as modified (they're regenerated each install)
