# AI Agent Guidelines for NZik-Folder Workspace

**This document is MANDATORY. Every rule, workflow, and convention must be followed strictly without deviation. No exceptions.**

This document provides comprehensive guidelines for AI agents working on Android/Kotlin projects in this workspace. It covers code standards, build processes, architectural patterns, and workflows to ensure consistent, high-quality contributions.

---

## 0. About NZik

**NZik** (N-Zik) is a 3rd party YouTube Music client written in **Kotlin** with **Jetpack Compose** UI following **Material 3** design guidelines. It is a multiplatform (KMP) Android application that provides music streaming, lyrics, playlist management, and audio playback features.

The project is built on top of/reference implementations from:

- **Metrolist** -- YouTube Music client reference
- **RiMusic** -- Multiplatform music player
- **Kreate** -- Similar architecture reference
- **RiPlay** -- Additional patterns

### Project Structure

```
NZik-Folder/
  N-Zik/                         # Main NZik project
    ComposeN-Zik/                # Main app module (KMP)
      src/
        androidMain/             # Android-specific code
          kotlin/app/n_zik/android/
        commonMain/              # Shared multiplatform code
          kotlin/database/
        test/                    # Unit tests
      build.gradle.kts
      schemas/                   # Room database schemas
    extensions/                  # API extension modules
      innertube/                 # YouTube Music API client
      lrclib/                    # Lyrics provider
      kugou/                     # Lyrics provider
      piped/                     # Piped API client
      invidious/                 # Invidious API client
      ktor-client-brotli/        # Brotli compression
    modules/                     # Feature modules
      betterlyrics/              # Enhanced lyrics
      discordrpc/                # Discord Rich Presence
      nextvisualizer/            # Audio visualizer
    gradle/libs.versions.toml    # Version catalog
  docs/                          # Reference projects
    Metrolist-main/
    RiMusic-master/
    Kreate-main/
    RiPlay-main/
  _bmad/                         # BMAD method configuration
  _bmad-out/                     # BMAD method output
  .bmad-loop/                    # BMAD method loop
```

### Key Locations

| What                  | Where to look                                                  |
| --------------------- | -------------------------------------------------------------- |
| Main application code | `N-Zik/ComposeN-Zik/src/androidMain/kotlin/app/n_zik/android/` |
| Shared database code  | `N-Zik/ComposeN-Zik/src/commonMain/kotlin/database/`           |
| Database entities     | `database/entities/` (commonMain)                              |
| Database DAO          | `MusicDatabase.kt` (commonMain)                                |
| Compose UI screens    | Look for `@Composable` functions in screen files               |
| Player service        | `playback/services/PlayerServiceModern.kt`                     |
| Extensions (API)      | `N-Zik/extensions/`                                            |
| Resources             | `N-Zik/ComposeN-Zik/src/androidMain/res/`                      |
| Strings               | `res/values/strings.xml` (default English only)                |
| Build config          | `N-Zik/ComposeN-Zik/build.gradle.kts`                          |
| Dependencies          | `N-Zik/gradle/libs.versions.toml`                              |
| Tests                 | `N-Zik/ComposeN-Zik/src/test/`                                 |

---

## 1. Core Rules (Absolute)

These rules are non-negotiable and override all other instructions:

1. **Always pull the latest changes** from `main` before starting your work to minimize merge conflicts.
2. **No commits/pushes** unless explicitly requested by a human contributor. **NEVER commit until the human has tested the changes and explicitly asked you to commit.** Present the changes, wait for test confirmation, then commit only when asked.
3. **No version bumps** -- version numbers are managed exclusively by the core development team after manual review.
4. **No markdown/readme edits** unless explicitly asked.
5. **Ask when uncertain** -- never assume requirements or implementation details without clarification from a human contributor.
6. **License check** -- When using code from any external source (web, documentation, GitHub, StackOverflow, AI generated, etc.), you MUST verify the license before using it. Open-source licenses (MIT, Apache, GPL, etc.) are acceptable. Closed-source or proprietary code is NEVER acceptable. Always cite the source and license in a comment when using external code.
7. **Use Timber** for all logging. Never use `println`, `Log.d`, `System.out`, or any other logging mechanism, use tags for different modules with timber tags like "Timber.tag("TAG").d("message")". Example of a good logging implementation in a Kotlin file:

```kotlin
import timber.log.Timber

// File-level tag is automatically created from the file name
class MyClass {
    fun doSomething() {
        Timber.tag("MyClass").d("Doing something")
    }
}
```

8. **Prioritize** performance, battery efficiency, and maintainability in all code contributions.
9. **No force pushes, rebases, or branch deletions** without explicit instructions from a human.
10. **Follow existing patterns** -- always examine neighboring files and existing code before introducing new patterns.
11. **Test your changes** -- if you do not test your changes before reporting, you will face reprimands and may be asked to redo your work. Always verify thoroughly.
12. **Imports** -- the `import` declarations must ALWAYS be placed at the top of the files. It is strictly forbidden to use fully qualified class names (ex: `java.util.List`) in the middle of the code (inline) unless there is an absolute naming conflict impossible to resolve otherwise.
13. **Feature request or bug report** -- if you finish a feature request or a bug always provide the link to the feature request or bug, in the git commit message with the actual bug/feature commit. Example:

```
feat(audiobar): Refresh Fake Audio Bar (gh-605)
  - Added option to refresh the fake audio bar in the player menu (SeekBarStaticAudioWaves)
  - Added validation to prevent refreshing if the song is not fully downloaded or cached, with a Toaster error
  - Removed inline import paths for cleaner code in PlayerItemMenu.kt

Issue: Link to the issue

```

14. **Done.txt** -- When the user asks to commit, update Done.txt located in assets/notes/Done.txt in the same commit as the changes.

---

## 2. BMAD Method

This workspace uses the **BMAD Method** (Breakthrough Method of Agile AI-Driven Development) for structured AI-assisted development. BMAD is a framework that breaks down software development into specialized phases, each handled by dedicated AI agents with specific skills. The goal is to ensure consistent, high-quality output by following structured workflows rather than ad-hoc coding.

**Documentation:** https://docs.bmad-method.org/
**Repository:** https://github.com/bmad-code-org/BMAD-METHOD

### How BMAD Works

BMAD organizes development into these phases:

1. **Planning** -- Create PRDs (Product Requirements Documents), architecture decisions, and UX designs
2. **Stories** -- Break features into epics and user stories with full context
3. **Sprint** -- Plan and track sprint progress from epics
4. **Development** -- Implement stories following context-filled specs
5. **Review** -- Code review with adversarial analysis and human-in-the-loop checkpoints
6. **Retrospective** -- Post-epic review to extract lessons and improve

Each phase has dedicated skills that provide step-by-step workflows, templates, and quality gates. The AI agent loads the appropriate skill based on the user's request.

### Available Skills

Skills are located in the project's skill directories. Consult the BMAD documentation (links above) or browse the skill directories to find the appropriate skill for your task.

### Using Skills

Load a skill when a task matches its description. Skills provide step-by-step workflows, templates, and quality gates.

### Reference Projects

The `docs/` folder contains reference implementations from similar projects (Metrolist, RiMusic, Kreate, RiPlay). Consult these for patterns and best practices when implementing features.

---

## 3. Code Quality Standards

### Naming Conventions

- **Classes/PascalCase**: `MusicDatabase`, `PlayerService`, `LyricsScreen`
- **Functions/camelCase**: `getSongById`, `updatePlaylist`, `handlePlaybackError`
- **Constants/UPPER_SNAKE_CASE**: `LOCAL_KEY_PREFIX`, `MAX_RETRY_COUNT`
- **Variables/camelCase**: `songList`, `isPlaying`, `currentPosition`
- **Packages/lowercase**: `app.n_zik.android.playback`, `database.entities`

### Code Organization

- Group related functionality together (imports, then properties, then functions)
- Keep files focused -- one primary class/composable per file
- Place composables in appropriate screen/component packages
- Use `internal` visibility for implementation details
- Prefer `private` over `internal` when scope allows

### Imports

- No wildcard imports (`import com.example.*`)
- Remove unused imports before committing
- Group imports: stdlib, third-party, project-internal
- Prefer explicit imports for clarity

### Dead Code

- Remove commented-out code blocks
- Remove unused functions, classes, and variables
- Remove unused parameters from functions
- If code is kept for reference, add a clear `// TODO: reason` comment

### Comments

- Add comments only for complex or non-obvious logic
- Do NOT restate what the code already says
- Use KDoc for public APIs that need documentation
- Mark TODOs with `// TODO(author): description`

```kotlin
// Bad - restates the code
// Increment counter by one
counter++

// Good - explains WHY
// Offset by 1 because Room IDs are 1-indexed but list indices are 0-indexed
val adjustedIndex = roomIndex - 1
```

---

## 4. Commit Convention

### Format

```
type(scope): short description
```

### Types

| Type       | When to use                                |
| ---------- | ------------------------------------------ |
| `feat`     | New feature or capability                  |
| `fix`      | Bug fix                                    |
| `refactor` | Code restructuring without behavior change |
| `chore`    | Build, CI, dependency, or tooling changes  |
| `docs`     | Documentation changes only                 |
| `test`     | Adding or updating tests                   |
| `perf`     | Performance improvement                    |

### Examples

- `feat(lyrics): add karaoke wave effect`
- `fix(player): resolve crash on seek`
- `refactor(database): simplify queries`
- `chore(deps): update media3 to 1.10.1`
- `perf(cache): implement LRU bitmap cache`

### Rules

- Keep description under 72 characters
- Use imperative mood ("add" not "added")
- Scope is optional but recommended
- No period at the end

---

## 5. Build System

### Gradle Version Catalog

Dependencies are managed via `gradle/libs.versions.toml`. Always use version catalog references:

```kotlin
// Good
implementation(libs.timber)
implementation(libs.room)
add("kspAndroid", libs.room.compiler)

// Bad - hardcoded versions
implementation("com.jakewharton.timber:timber:5.0.1")
```

### Build Commands

```bash
# Debug build (primary development build)
./gradlew :ComposeN-Zik:assembleDebug

# FOSS build (no proprietary dependencies)
./gradlew :ComposeN-Zik:assembleFoss

# Beta build
./gradlew :ComposeN-Zik:assembleBeta

# Run all tests
./gradlew :ComposeN-Zik:test

# Run specific test class
./gradlew :ComposeN-Zik:testDebugUnitTest --tests "app.n_zik.android.playback.utils.ShufflerTest"

# Clean build
./gradlew clean :ComposeN-Zik:assembleDebug
```

### Build Types

| Type       | Suffix   | Notes                                   |
| ---------- | -------- | --------------------------------------- |
| `debug`    | `.debug` | Development build, auto-update disabled |
| `full`     | `-f`     | Full release build                      |
| `minified` | `-m`     | R8 minified, shrinkResources enabled    |
| `beta`     | `-b`     | Beta build with debug signing           |
| `foss`     | (none)   | FOSS build, auto-update disabled        |

### Verification

Always verify your changes compile successfully before reporting completion. If the build fails:

1. Read the error messages carefully
2. Fix the issues in your code
3. Rebuild until successful
4. Run relevant tests if available

---

## 6. Database Rules

### Absolute Rule

**NEVER edit the database schema** without explicit instruction from a human contributor. This includes:

- Adding/removing/renaming columns
- Adding/removing/renaming tables
- Changing column types
- Changing constraints or indices

### Room Architecture

The project uses Room (KMP) with a single DAO interface:

```kotlin
@Database(entities = [...], version = 23, exportSchema = true)
@ConstructedBy(MusicDatabaseConstructor::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun getDao(): MusicDatabaseDao
}

@Dao
interface MusicDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: Song): Long

    @Upsert
    suspend fun upsert(item: Song): Long

    @Query("SELECT * FROM Song WHERE id = :id")
    fun song(id: String): Flow<Song?>

    @Delete
    suspend fun delete(item: Song)
}
```

### DAO Patterns

- Use `@Insert(onConflict = OnConflictStrategy.IGNORE)` for insert-or-ignore
- Use `@Upsert` for insert-or-update semantics
- Use `@Query` for read operations, return `Flow<T>` for reactive queries
- Use `@Transaction` for multi-step operations
- All DAO methods should be `suspend` functions (except Flow-returning queries)

### Migrations

- Migrations must be tested thoroughly
- Schema files are exported to `ComposeN-Zik/schemas/`
- Never modify existing migration files
- New migrations must handle all edge cases (null values, defaults)

### Repository Pattern

Access database through repository/table objects, not directly from UI code. See section 16 for the full pattern and examples.

---

## 7. UI Guidelines

### Jetpack Compose

- All UI must be written in Jetpack Compose (no XML layouts)
- Use Material 3 components and design tokens
- Follow the existing theming system (colorPalette, typography)
- Use `Modifier` for styling, chain modifiers for multiple effects
- Keep composables small and focused (single responsibility)

### Theming

```kotlin
// Use the existing theme system
val colorPalette = LocalColorPalette.current
val typography = LocalTypography.current

// Access theme colors
Text(
    text = "Hello",
    color = colorPalette.textPrimary,
    style = typography.bodyLarge
)
```

### Animations

- Use `tween` with easing for smooth transitions
- Use `animateFloatAsState` for single value animations
- Use `AnimatedVisibility` for show/hide transitions
- Use `Crossfade` for screen transitions
- Keep animations under 300ms for snappy feel

```kotlin
val alpha by animateFloatAsState(
    targetValue = if (isVisible) 1f else 0f,
    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
)
```

### State Management

- Use `remember` for UI-only state
- Use `rememberSaveable` for state that survives configuration changes
- Use `mutableStateOf` for observable state
- Use `derivedStateOf` for computed state
- Hoist state to ViewModel for business logic

### Settings and Preferences

- Respect user settings and preferences
- Use the existing preference system (SharedPreferences with keys)
- Provide sensible defaults for all settings

---

## 8. Logging

### Timber Usage

All logging must use Timber. The project initializes Timber in `MainApplication`.

Use **tags** to differentiate modules and components. Tags help filter logs by feature area.

```kotlin
import timber.log.Timber

// File-level tag is automatically created from the file name
class MyClass {
    fun doSomething() {
        Timber.tag("MyClass").d("Doing something")
    }
}
```

### Tag Convention

Use the class name or module name as the tag:

```kotlin
// Debug logging with tag
Timber.tag("PlayerService").d("Loading playlist: %s", playlistId)

// Warning with tag
Timber.tag("NetworkHelper").w("Request timed out, retrying...")

// Error with tag and exception
Timber.tag("YtMusic").e(exception, "Failed to load song: %s", songId)

// Info with tag
Timber.tag("Playback").i("Playback started for: %s", song.title)
```

### What to Log

- Entry points of important operations
- Error conditions and recoverable failures
- Performance-critical timing information
- User-facing error states

### What NOT to Log

- Sensitive data (tokens, passwords, API keys)
- High-frequency events in hot paths (every frame, every buffer)
- Redundant information already captured by the system
- User personal data

### Prohibited

```kotlin
println("...")           // NEVER
Log.d("TAG", "...")      // NEVER
System.out.print(...)     // NEVER
e.printStackTrace()       // NEVER - use Timber.tag("TAG").e(e, "message")
```

---

## 9. Error Handling

### runCatching Pattern

Use Kotlin's `runCatching` for operations that may fail:

```kotlin
// Basic usage
runCatching {
    riskyOperation()
}.onFailure { e ->
    Timber.tag("MyClass").e(e, "Operation failed")
}

// With transformation
val result = runCatching {
    parseJson(rawData)
}.getOrElse { e ->
    Timber.tag("Parser").e(e, "JSON parse failed")
    defaultValue
}

// With chaining
runCatching {
    fetchData()
}.mapCatching { data ->
    transformData(data)
}.onSuccess { transformed ->
    updateUI(transformed)
}.onFailure { e ->
    Timber.tag("DataLoader").e(e, "Pipeline failed")
    showError(e.message)
}
```

### Error Reporting

- Always log errors with Timber, including the exception object
- Provide user-friendly error messages (not stack traces)
- Handle network errors gracefully with retry logic
- Use `PlaybackExceptions` for player-specific errors

### Prohibited Patterns

```kotlin
// BAD - swallows exception silently
try {
    riskyOperation()
} catch (e: Exception) {
    // empty catch
}

// BAD - prints stack trace to logcat
try {
    riskyOperation()
} catch (e: Exception) {
    e.printStackTrace()
}
```

---

## 10. Performance

### Coroutines

- Use `Dispatchers.IO` for network and disk operations
- Use `Dispatchers.Default` for CPU-intensive work
- Use `Dispatchers.Main` only for UI updates
- Use `withContext` to switch dispatchers, not `launch`
- Cancel coroutines properly in `onCleared()` or `DisposableEffect`

```kotlin
// Good
viewModelScope.launch {
    val data = withContext(Dispatchers.IO) {
        repository.fetchData()
    }
    _uiState.value = UiState.Success(data)
}

// Bad - blocks main thread
viewModelScope.launch {
    val data = repository.fetchData() // network call on Main!
}
```

### Memory

- Avoid holding references to Activity/Context in long-lived objects
- Use `WeakReference` or `ApplicationContext` when Context is needed
- Release resources in `onCleared()` or `DisposableEffect`
- Use Coil for image loading (handles memory caching automatically)
- Avoid creating objects in hot paths (recomposition loops)

### Battery

- Minimize background work
- Use `WorkManager` for deferrable background tasks
- Batch network requests when possible
- Release wake locks promptly
- Use efficient data structures (avoid unnecessary copies)

### Caching

- Use in-memory caching for frequently accessed data
- Use disk caching for network responses
- Implement LRU eviction for bounded caches
- Cache decoded bitmaps, not raw bytes

---

## 11. Testing

### Framework

The project uses JUnit 5 with MockK for unit testing:

```kotlin
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShufflerTest {
    @Test
    fun `shuffle should return list of same size`() {
        val shuffler = Shuffler()
        val input = listOf(1, 2, 3, 4, 5)
        val result = shuffler.shuffle(input)
        assertEquals(input.size, result.size)
    }
}
```

### Where to Write Tests

- Unit tests: `ComposeN-Zik/src/test/kotlin/`
- Mirror the source package structure
- Test files: `*Test.kt` (e.g., `ShufflerTest.kt`, `NZikRadioTest.kt`)

### Naming Conventions

- Test class: `{ClassName}Test` (e.g., `ShufflerTest`)
- Test method: backtick-quoted descriptive name (e.g., `` `shuffle should return list of same size` ``)
- Or standard camelCase: `testShuffleReturnsSameSize`

### What to Test

- Utility functions and pure logic
- Data transformations
- Edge cases and boundary conditions
- Error handling paths

### Running Tests

```bash
# Run all tests
./gradlew :ComposeN-Zik:test

# Run specific test
./gradlew :ComposeN-Zik:testDebugUnitTest --tests "app.n_zik.android.playback.utils.ShufflerTest"
```

---

## 12. Security

### Secrets and API Keys

- **Never commit secrets, API keys, or tokens** to the repository
- Use `local.properties` for local secrets (gitignored)
- Use `BuildConfig` fields for build-time secrets
- Never log sensitive data (tokens, passwords, user data)

### Input Validation

- Validate all user input before processing
- Sanitize data before displaying in UI
- Use parameterized queries (Room handles this automatically)
- Validate URLs before opening in browser/webview

### Sensitive Data

- Use `EncryptedSharedPreferences` for sensitive local storage
- Clear sensitive data when user logs out
- Use HTTPS for all network communications
- Do not store credentials in plain text

---

## 13. Workflow (Step-by-Step)

When implementing a feature or fixing a bug, follow this process:

### Step 1: Understand

- Read the request carefully and completely
- Ask clarifying questions if anything is ambiguous
- Identify the scope (which modules, files, packages are affected)

### Step 2: Explore

- Search for existing implementations of similar features
- Read neighboring files to understand conventions
- Check imports and dependencies used in related code
- Consult reference projects in `docs/` if needed

### Step 3: Execute BMAD Skill (MANDATORY)

**After understanding and exploring, you MUST execute the appropriate BMAD skill.** Read section 2 (BMAD Method) to identify which skill matches the task, then execute it.

BMAD is primordial and must never be deviated from once its workflow is started.

**Rules for BMAD execution:**

- **Identify your tool.** ASK the human which IDE/tool they are using. Do NOT assume. Skill directories differ between tools — guessing wrong means loading from the wrong path.
- **Locate skills on disk.** Verify the skill file exists before loading. Check BOTH the project directory AND the parent directory — the installer may have been run from the parent. Full reference from BMAD installer `platform-codes.yaml`:

### Preferred tools (recommended during install)

| Tool                  | Skills dir        | Global dir                      | Extra                                                        |
| --------------------- | ----------------- | ------------------------------- | ------------------------------------------------------------ |
| OpenCode ⭐           | `.agents/skills/` | `~/.agents/skills/`             | Commands: `.opencode/commands/`                              |
| Google Antigravity ⭐ | `.agent/skills/`  | `~/.gemini/antigravity/skills/` |                                                              |
| Claude Code           | `.claude/skills/` | `~/.claude/skills/`             |                                                              |
| Cursor                | `.agents/skills/` | `~/.agents/skills/`             |                                                              |
| GitHub Copilot        | `.agents/skills/` | `~/.agents/skills/`             | Commands: `.github/agents/` (.agent.md), filter: agents-only |
| Codex                 | `.agents/skills/` | `~/.codex/skills/`              |                                                              |

### All other tools

| Tool             | Skills dir            | Global dir                    | Extra |
| ---------------- | --------------------- | ----------------------------- | ----- |
| AdaL             | `.adal/skills/`       | `~/.adal/skills/`             |       |
| Sourcegraph Amp  | `.agents/skills/`     | `~/.config/agents/skills/`    |       |
| Auggie           | `.agents/skills/`     | `~/.agents/skills/`           |       |
| IBM Bob          | `.bob/skills/`        | `~/.bob/skills/`              |       |
| Cline            | `.cline/skills/`      | `~/.cline/skills/`            |       |
| CodeWhale        | `.codewhale/skills/`  | `~/.codewhale/skills/`        |       |
| CodeBuddy        | `.codebuddy/skills/`  | `~/.codebuddy/skills/`        |       |
| Command Code     | `.agents/skills/`     | `~/.agents/skills/`           |       |
| Snowflake Cortex | `.cortex/skills/`     | `~/.snowflake/cortex/skills/` |       |
| Crush            | `.agents/skills/`     | `~/.config/agents/skills/`    |       |
| Factory Droid    | `.factory/skills/`    | `~/.factory/skills/`          |       |
| Firebender       | `.firebender/skills/` | `~/.agents/skills/`           |       |
| Gemini CLI       | `.agents/skills/`     | `~/.agents/skills/`           |       |
| Block Goose      | `.agents/skills/`     | `~/.config/agents/skills/`    |       |
| Hermes Agent     | `.agents/skills/`     | `~/.hermes/skills/`           |       |
| iFlow            | `.iflow/skills/`      | `~/.iflow/skills/`            |       |
| Junie            | `.junie/skills/`      | `~/.junie/skills/`            |       |
| KiloCoder        | `.agents/skills/`     | `~/.kilocode/skills/`         |       |
| Kimi Code        | `.agents/skills/`     | `~/.agents/skills/`           |       |
| Kiro             | `.kiro/skills/`       | `~/.kiro/skills/`             |       |
| Kode             | `.kode/skills/`       | `~/.kode/skills/`             |       |
| Mistral Vibe     | `.agents/skills/`     | `~/.vibe/skills/`             |       |
| Mux              | `.agents/skills/`     | `~/.agents/skills/`           |       |
| Neovate          | `.neovate/skills/`    | `~/.neovate/skills/`          |       |
| Ona              | `.ona/skills/`        | —                             |       |
| OpenClaw         | `.agents/skills/`     | `~/.agents/skills/`           |       |
| OpenHands        | `.agents/skills/`     | `~/.agents/skills/`           |       |
| Pi               | `.agents/skills/`     | `~/.agents/skills/`           |       |
| Pochi            | `.agents/skills/`     | `~/.agents/skills/`           |       |
| Qoder            | `.qoder/skills/`      | `~/.qoder/skills/`            |       |
| QwenCoder        | `.qwen/skills/`       | `~/.qwen/skills/`             |       |
| Replit Agent     | `.agents/skills/`     | —                             |       |
| Roo Code         | `.agents/skills/`     | `~/.agents/skills/`           |       |
| Rovo Dev         | `.agents/skills/`     | `~/.agents/skills/`           |       |
| Trae             | `.trae/skills/`       | —                             |       |
| Warp             | `.agents/skills/`     | `~/.agents/skills/`           |       |
| Windsurf         | `.agents/skills/`     | `~/.agents/skills/`           |       |
| Zencoder         | `.zencoder/skills/`   | `~/.zencoder/skills/`         |       |

**Source:** `tools/installer/ide/platform-codes.yaml` from BMAD-METHOD repo.
**Doc:** https://docs.bmad-method.org/reference/commands/

Never assume a path — always verify with a file search first.

- **Check online documentation.** If the workflow is unclear or you're unsure about a step, consult https://docs.bmad-method.org/ or https://github.com/bmad-code-org/BMAD-METHOD before proceeding.
- **Ask the human** if you're unsure which tool is being used. Do not guess.
- Execute the skill before starting work
- Follow the skill's workflow step-by-step, in order
- **NEVER skip steps or optimize the sequence**
- **NEVER deviate from the skill's instructions once started**
- **HALT at every checkpoint** and wait for human input as directed by the skill. Do not proceed past a checkpoint without explicit human confirmation.
- The skill's workflow takes precedence over this section if there's a conflict

### Step 4: Implement

- Write clean, focused code following all guidelines
- Follow existing patterns and conventions
- Handle errors appropriately
- Add comments only for complex logic
- Remove any dead code

### Step 5: Verify

- Build the project to ensure no compilation errors
- Run relevant tests if available
- Review your changes for quality
- Fix any lint warnings

### Step 6: Report

- Summarize what was done and why
- Note any files modified or created
- Highlight any potential risks or follow-up items
- Do NOT commit unless explicitly asked

---

## 14. BMAD Technical Reference

This section contains the complete technical structure of BMAD extracted from the installer source (`tools/installer/`). **Consult this when unsure about file locations, naming, or config resolution.**

### 14.1 Installation Structure

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

### 14.2 Config Resolution (4-Layer TOML Merge)

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

### 14.3 IDE Skill Directories (Full Reference)

From `tools/installer/ide/platform-codes.yaml`:

**Legend:**

- **Skills dir** = `{target_dir}` — where skill directories are installed (project/workspace)
- **Global dir** = `{global_target_dir}` — user-home directory for global install
- **Commands dir** = `{commands_target_dir}` — where command pointer files are generated
- **Commands ext** = `{commands_extension}` — file extension for command pointers
- **Filter** = `{commands_filter}` — only surface certain artifact types in picker

| Tool                  | Skills dir            | Global dir                      | Commands dir          | Commands ext | Filter        |
| --------------------- | --------------------- | ------------------------------- | --------------------- | ------------ | ------------- |
| **Claude Code** ⭐    | `.claude/skills/`     | `~/.claude/skills/`             | —                     | —            | —             |
| **Cursor** ⭐         | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| **GitHub Copilot** ⭐ | `.agents/skills/`     | `~/.agents/skills/`             | `.github/agents/`     | `.agent.md`  | `agents-only` |
| **Codex** ⭐          | `.agents/skills/`     | `~/.codex/skills/`              | —                     | —            | —             |
| OpenCode              | `.agents/skills/`     | `~/.agents/skills/`             | `.opencode/commands/` | —            | —             |
| AdaL                  | `.adal/skills/`       | `~/.adal/skills/`               | —                     | —            | —             |
| Sourcegraph Amp       | `.agents/skills/`     | `~/.config/agents/skills/`      | —                     | —            | —             |
| Google Antigravity    | `.agent/skills/`      | `~/.gemini/antigravity/skills/` | —                     | —            | —             |
| Auggie                | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| IBM Bob               | `.bob/skills/`        | `~/.bob/skills/`                | —                     | —            | —             |
| Cline                 | `.cline/skills/`      | `~/.cline/skills/`              | —                     | —            | —             |
| CodeWhale             | `.codewhale/skills/`  | `~/.codewhale/skills/`          | —                     | —            | —             |
| CodeBuddy             | `.codebuddy/skills/`  | `~/.codebuddy/skills/`          | —                     | —            | —             |
| Command Code          | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| Snowflake Cortex      | `.cortex/skills/`     | `~/.snowflake/cortex/skills/`   | —                     | —            | —             |
| Crush                 | `.agents/skills/`     | `~/.config/agents/skills/`      | —                     | —            | —             |
| Factory Droid         | `.factory/skills/`    | `~/.factory/skills/`            | —                     | —            | —             |
| Firebender            | `.firebender/skills/` | `~/.agents/skills/`             | —                     | —            | —             |
| Gemini CLI            | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| Block Goose           | `.agents/skills/`     | `~/.config/agents/skills/`      | —                     | —            | —             |
| Hermes Agent          | `.agents/skills/`     | `~/.hermes/skills/`             | —                     | —            | —             |
| iFlow                 | `.iflow/skills/`      | `~/.iflow/skills/`              | —                     | —            | —             |
| Junie                 | `.junie/skills/`      | `~/.junie/skills/`              | —                     | —            | —             |
| KiloCoder             | `.agents/skills/`     | `~/.kilocode/skills/`           | —                     | —            | —             |
| Kimi Code             | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| Kiro                  | `.kiro/skills/`       | `~/.kiro/skills/`               | —                     | —            | —             |
| Kode                  | `.kode/skills/`       | `~/.kode/skills/`               | —                     | —            | —             |
| Mistral Vibe          | `.agents/skills/`     | `~/.vibe/skills/`               | —                     | —            | —             |
| Mux                   | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| Neovate               | `.neovate/skills/`    | `~/.neovate/skills/`            | —                     | —            | —             |
| Ona                   | `.ona/skills/`        | —                               | —                     | —            | —             |
| OpenClaw              | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| OpenHands             | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| Pi                    | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| Pochi                 | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| Qoder                 | `.qoder/skills/`      | `~/.qoder/skills/`              | —                     | —            | —             |
| QwenCoder             | `.qwen/skills/`       | `~/.qwen/skills/`               | —                     | —            | —             |
| Replit Agent          | `.agents/skills/`     | —                               | —                     | —            | —             |
| Roo Code              | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| Rovo Dev              | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| Trae                  | `.trae/skills/`       | —                               | —                     | —            | —             |
| Warp                  | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| Windsurf              | `.agents/skills/`     | `~/.agents/skills/`             | —                     | —            | —             |
| Zencoder              | `.zencoder/skills/`   | `~/.zencoder/skills/`           | —                     | —            | —             |

⭐ = Preferred (shown first during install)

**Shared directories:** Many tools share `.agents/skills/` (Cursor, Copilot, Codex, Gemini CLI, OpenHands, Warp, Windsurf, Roo Code, etc.). The installer checks for ancestor conflicts before writing to shared directories.

### 14.4 Command Pointer Files

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

### 14.5 Skill Naming Conventions

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

### 14.6 Manifest Tracking

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

### 14.7 Shared Python Scripts

Copied from `src/scripts/` to `_bmad/scripts/` during install. Excludes tests, `__pycache__`, `.pytest_cache`.

| Script                     | Purpose                              |
| -------------------------- | ------------------------------------ |
| `resolve_config.py`        | 4-layer TOML config merge (see 14.2) |
| `resolve_customization.py` | Customization file resolution        |
| `memlog.py`                | Memory logging                       |

**Execution:** `uv run _bmad/scripts/resolve_config.py --project-root /path/to/project`

### 14.8 Update Behavior

During updates, the installer:

1. Detects custom files (not in files-manifest.csv) → preserves them
2. Detects modified files (SHA256 changed vs manifest) → backs up as `.bak`
3. Backs up to `_bmad-custom-backup-temp/` and `_bmad-modified-backup-temp/` (cleaned up after)
4. Preserves `_bmad/custom/` directory and `*.user.toml` files
5. Preserves `memory/` and `_memory/` directories (agent runtime state)
6. Does NOT treat `config.yaml` files as modified (they're regenerated each install)

---

## 15. Communication

### Language

- **Code comments**: Always in English
- **Commit messages**: Always in English
- **Communication with user**: Match the user's language

### String Resources

- **ALL text in English** — code, strings, comments, commit messages, documentation
- **Edit ONLY** `N-Zik/ComposeN-Zik/src/androidMain/res/values/strings.xml` (default English)
- **NEVER edit** other language `strings.xml` files (e.g. `values-fr/strings.xml`, `values-de/strings.xml`). Translations are managed via Crowdin.
- When adding new strings, always add the English entry in `values/strings.xml`

### Conciseness

- Keep responses short and focused
- Answer the question directly without unnecessary preamble
- Use code examples when they clarify faster than words
- Avoid restating what is already clear

### When to Ask

- Requirements are ambiguous or incomplete
- Multiple valid approaches exist and the trade-off is significant
- The change could affect existing functionality in unexpected ways
- You are unsure about the scope of a change
- The request conflicts with established patterns

### When NOT to Ask

- The implementation is straightforward and follows existing patterns
- The request is clear and unambiguous
- Standard best practices apply without controversy

---

## 16. Common Patterns

### Repository Pattern

Data access is abstracted through repository/table objects:

```kotlin
object MusicDatabase {
    suspend fun insert(song: Song) = database.getDao().insert(song)
    fun songsByPlaylist(id: String) = database.getDao().songsByPlaylist(id)
}
```

### ViewModel Pattern

Business logic lives in ViewModels, UI observes state:

```kotlin
class PlayerViewModel : ViewModel() {
    private val _uiState = mutableStateOf(PlayerUiState())
    val uiState: State<PlayerUiState> = _uiState

    fun play(song: Song) {
        viewModelScope.launch {
            // business logic
        }
    }
}
```

### Composable Pattern

UI composables are stateless, receiving state and emitting events:

```kotlin
@Composable
fun SongList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(songs) { song ->
            SongItem(song = song, onClick = { onSongClick(song) })
        }
    }
}
```

### Extension Functions

Use extension functions for utility operations on domain types:

```kotlin
fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id)
    .setUri(id.uri)
    .build()
```

---

## 17. Reference

### Documentation

- `docs/Metrolist-main/` -- Reference implementation of a YouTube Music client
- `docs/RiMusic-master/` -- Reference implementation with multiplatform support
- `docs/Kreate-main/` -- Reference implementation with similar architecture
- `docs/RiPlay-main/` -- Reference implementation for patterns

### Key Files to Consult

| Topic             | File                                                                                             |
| ----------------- | ------------------------------------------------------------------------------------------------ |
| Database schema   | `ComposeN-Zik/src/commonMain/kotlin/database/MusicDatabase.kt`                                   |
| Database entities | `ComposeN-Zik/src/commonMain/kotlin/database/entities/`                                          |
| Player service    | `ComposeN-Zik/src/androidMain/kotlin/app/n_zik/android/playback/services/PlayerServiceModern.kt` |
| Main activity     | `ComposeN-Zik/src/androidMain/kotlin/app/n_zik/android/MainActivity.kt`                          |
| Application class | `ComposeN-Zik/src/androidMain/kotlin/app/n_zik/android/MainApplication.kt`                       |
| Dependencies      | `N-Zik/gradle/libs.versions.toml`                                                                |
| Build config      | `N-Zik/ComposeN-Zik/build.gradle.kts`                                                            |
| Settings/Modules  | `N-Zik/settings.gradle.kts`                                                                      |

### When in Doubt

1. Check existing code for similar patterns
2. Consult reference projects in `docs/`
3. Ask a human contributor
4. Never assume requirements

---

## 18. UI & Animations Specifics

### CustomModalBottomSheet and Animations

When dismissing a `CustomModalBottomSheet` manually from an inner component (e.g., clicking a close chevron instead of swiping down), **always** orchestrate the hide animation before changing the visibility state. Modifying the global boolean (`showSheet = false`) immediately will kill the component from the Composition tree and break the transition.

**Correct Pattern:**

```kotlin
coroutineScope.launch {
    if (sheetState.isVisible) sheetState.hide()
    showSheet = false // Only update boolean AFTER animation
}
```

**Incorrect Pattern:**

```kotlin
onDismiss = { showSheet = false } // Brittle: causes sudden disappearance
```
