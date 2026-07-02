# AI Agent Guidelines for NZik-Folder Workspace

This document provides comprehensive guidelines for AI agents working on Android/Kotlin projects in this workspace. It covers code standards, build processes, architectural patterns, and workflows to ensure consistent, high-quality contributions.

**This AGENTS.md applies to ALL AI tools used in this project:**

- **opencode** -- CLI AI agent (skills in `.opencode/skills/`)
- **Claude Code** -- Anthropic's CLI coding agent (skills in `.claude/skills/`)
- **GitHub Copilot** -- GitHub's AI assistant (uses this AGENTS.md as context)
- **Antigravity** -- AI coding agent (uses this AGENTS.md as context)

All tools share the same rules, BMAD workflows, and code standards. Skills are synchronized across `.opencode/skills/`, `.claude/skills/`, and `.agent/skills/`.

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
  .opencode/skills/              # AI agent skills (142 skills)
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
2. **No commits/pushes** unless explicitly requested by a human contributor.
3. **No version bumps** -- version numbers are managed exclusively by the core development team after manual review.
4. **No markdown/readme edits** unless explicitly asked.
5. **Ask when uncertain** -- never assume requirements or implementation details without clarification from a human contributor.
6. **Use Timber** for all logging. Never use `println`, `Log.d`, `System.out`, or any other logging mechanism.
7. **Prioritize** performance, battery efficiency, and maintainability in all code contributions.
8. **No force pushes, rebases, or branch deletions** without explicit instructions from a human.
9. **Follow existing patterns** -- always examine neighboring files and existing code before introducing new patterns.
10. **Test your changes** -- if you do not test your changes before reporting, you will face reprimands and may be asked to redo your work. Always verify thoroughly.

---

## 2. BMAD Method

This workspace uses the **BMAD Method** (Breakthrough Method of Agile AI-Driven Development) for structured AI-assisted development. BMAD is a framework that breaks down software development into specialized phases, each handled by dedicated AI agents with specific skills. The goal is to ensure consistent, high-quality output by following structured workflows rather than ad-hoc coding.

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

Skills are located in multiple directories (synchronized across tools):

- `.opencode/skills/` -- opencode skills
- `.claude/skills/` -- Claude Code skills
- `.agent/skills/` -- Shared agent skills

All directories contain the same skills. Load from the appropriate directory based on your tool.

#### Core Development Skills

| Skill                           | Purpose                                                |
| ------------------------------- | ------------------------------------------------------ |
| `bmad-quick-dev`                | Implement features, fix bugs, refactor code directly   |
| `bmad-dev-story`                | Implement stories following context-filled specs       |
| `bmad-create-prd`               | Create Product Requirements Documents from scratch     |
| `bmad-edit-prd`                 | Edit existing PRDs                                     |
| `bmad-create-architecture`      | Create architecture solution design decisions          |
| `bmad-create-ux-design`         | Plan UX patterns and design specifications             |
| `bmad-create-epics-and-stories` | Break requirements into epics and user stories         |
| `bmad-create-story`             | Create dedicated story files with full context         |
| `bmad-sprint-planning`          | Generate sprint status tracking from epics             |
| `bmad-code-review`              | Review code changes adversarially                      |
| `bmad-checkpoint-preview`       | Human-in-the-loop review of changes                    |
| `bmad-brainstorming`            | Facilitate interactive brainstorming sessions          |
| `bmad-correct-course`           | Manage significant changes during sprint execution     |
| `bmad-retrospective`            | Post-epic review to extract lessons                    |
| `bmad-distillator`              | Lossless LLM-optimized compression of source documents |

#### Agent Personas

| Skill                    | Purpose                                            |
| ------------------------ | -------------------------------------------------- |
| `bmad-agent-pm`          | Product manager for PRD creation and requirements  |
| `bmad-agent-analyst`     | Strategic business analyst and requirements expert |
| `bmad-agent-architect`   | System architect and technical design leader       |
| `bmad-agent-dev`         | Senior software engineer for story execution       |
| `bmad-agent-tech-writer` | Technical documentation specialist                 |
| `bmad-agent-ux-designer` | UX designer and UI specialist                      |
| `bmad-agent-builder`     | Builds, edits or analyzes Agent Skills             |

#### Advanced Elicitation & Problem Solving

| Skill                                    | Purpose                                        |
| ---------------------------------------- | ---------------------------------------------- |
| `bmad-advanced-elicitation`              | Push LLM to reconsider, refine, improve output |
| `bmad-agent-cis-brainstorming-coach`     | Elite brainstorming specialist                 |
| `bmad-agent-cis-creative-problem-solver` | Master problem solver                          |
| `bmad-agent-cis-design-thinking-coach`   | Design thinking maestro                        |
| `bmad-agent-cis-innovation-strategist`   | Disruptive innovation oracle                   |
| `bmad-agent-cis-presentation-master`     | Visual communication expert                    |
| `bmad-agent-cis-storyteller`             | Master storyteller                             |
| `bmad-cis-design-thinking`               | Guide human-centered design processes          |
| `bmad-cis-innovation-strategy`           | Identify disruption opportunities              |
| `bmad-cis-problem-solving`               | Apply systematic problem-solving methodologies |
| `bmad-cis-storytelling`                  | Craft compelling narratives                    |

#### Quality & Review

| Skill                                 | Purpose                                           |
| ------------------------------------- | ------------------------------------------------- |
| `bmad-code-review`                    | Review code changes adversarially                 |
| `bmad-checkpoint-preview`             | Human-in-the-loop review                          |
| `bmad-check-implementation-readiness` | Validate PRD, UX, Architecture specs are complete |
| `bmad-review-adversarial-general`     | Cynical review and findings report                |
| `bmad-review-edge-case-hunter`        | Exhaustive edge-case analysis                     |
| `bmad-validate-prd`                   | Validate PRD against standards                    |
| `bmad-editorial-review-prose`         | Clinical copy-editor for communication issues     |
| `bmad-editorial-review-structure`     | Structural editor for cuts and reorganization     |

#### Research & Analysis

| Skill                           | Purpose                                              |
| ------------------------------- | ---------------------------------------------------- |
| `bmad-domain-research`          | Conduct domain and industry research                 |
| `bmad-market-research`          | Conduct market research on competition and customers |
| `bmad-technical-research`       | Conduct technical research on technologies           |
| `bmad-product-brief`            | Create or update product briefs                      |
| `bmad-prfaq`                    | Working Backwards PRFAQ challenge                    |
| `bmad-document-project`         | Document brownfield projects for AI context          |
| `bmad-generate-project-context` | Create project-context.md with AI rules              |
| `bmad-index-docs`               | Generate or update index.md for docs folder          |
| `bmad-shard-doc`                | Split large markdown documents into smaller files    |

#### Testing & Quality Assurance

| Skill                        | Purpose                                          |
| ---------------------------- | ------------------------------------------------ |
| `bmad-tea`                   | Master Test Architect and Quality Advisor        |
| `bmad-teach-me-testing`      | Teach testing progressively through sessions     |
| `bmad-qa`                    | QA agent                                         |
| `bmad-qa-generate-e2e-tests` | Generate end-to-end automated tests              |
| `bmad-testarch-atdd`         | Generate red-phase acceptance test scaffolds     |
| `bmad-testarch-automate`     | Expand test automation coverage                  |
| `bmad-testarch-ci`           | Scaffold CI/CD quality pipeline                  |
| `bmad-testarch-framework`    | Initialize test framework                        |
| `bmad-testarch-nfr`          | Assess NFRs (performance, security, reliability) |
| `bmad-testarch-test-design`  | Create system-level test plans                   |
| `bmad-testarch-test-review`  | Review test quality                              |
| `bmad-testarch-trace`        | Generate traceability matrix                     |

#### Workflow & Planning

| Skill                   | Purpose                                          |
| ----------------------- | ------------------------------------------------ |
| `bmad-sprint-planning`  | Generate sprint status tracking                  |
| `bmad-sprint-status`    | Summarize sprint status and surface risks        |
| `bmad-correct-course`   | Manage significant changes during sprint         |
| `bmad-retrospective`    | Post-epic review to extract lessons              |
| `bmad-help`             | Analyze state and recommend next skills          |
| `bmad-party-mode`       | Multi-agent group discussions                    |
| `bmad-customize`        | Author customization overrides for BMAD skills   |
| `bmad-module-builder`   | Plan, create, and validate BMAD modules          |
| `bmad-workflow-builder` | Build, convert, and analyze workflows and skills |
| `bmad-bmb-setup`        | Set up BMad Builder module                       |

#### WDS (Web Design System) Skills

| Skill                                     | Purpose                                       |
| ----------------------------------------- | --------------------------------------------- |
| `wds-0-alignment-signoff`                 | Create alignment around idea before starting  |
| `wds-0-project-setup`                     | Project onboarding and routing                |
| `wds-1-project-brief`                     | Establish project context                     |
| `wds-2-trigger-mapping`                   | Map business goals to user psychology         |
| `wds-3-scenarios`                         | Create UX scenario outlines                   |
| `wds-4-ux-design`                         | Transform ideas into visual specifications    |
| `wds-5-agentic-development`               | AI-assisted development and testing           |
| `wds-6-asset-generation`                  | Generate visual and text assets               |
| `wds-7-design-system`                     | Create and maintain design system             |
| `wds-8-product-evolution`                 | Brownfield improvements                       |
| `wds-agent-freya-ux`                      | Strategic UX designer for WDS                 |
| `wds-agent-saga-analyst`                  | Strategic business analyst for WDS            |
| `wds-agent-mimir-builder`                 | Implementation agent for WDS                  |
| `bmad-wds-acceptance-test`                | Test implementation against specification     |
| `bmad-wds-acceptance-testing`             | Design and run acceptance tests               |
| `bmad-wds-agentic-development`            | AI-assisted development, testing, reverse eng |
| `bmad-wds-alignment-signoff`              | Create alignment before project               |
| `bmad-wds-analysis`                       | Understand existing codebase                  |
| `bmad-wds-analyze-product`                | Understand current product state              |
| `bmad-wds-asset-generation`               | Generate visual and text assets               |
| `bmad-wds-browse-design-system`           | Generate localhost app for tokens/components  |
| `bmad-wds-bugfixing`                      | Fix bugs through structured investigation     |
| `bmad-wds-content-creation`               | Strategic text content generation             |
| `bmad-wds-create-design-system`           | Build new design system                       |
| `bmad-wds-deploy`                         | Create PR and deliver improvement             |
| `bmad-wds-design-solution`                | Sketch and specify update                     |
| `bmad-wds-design-system`                  | Create, import, browse design system          |
| `bmad-wds-development`                    | Write production code from specs              |
| `bmad-wds-edit-components`                | Open components in Figma for editing          |
| `bmad-wds-evolution`                      | Add features to existing products             |
| `bmad-wds-figma-integration`              | Code-to-Figma and Figma-to-code workflows     |
| `bmad-wds-handover`                       | Package testable flows and hand off           |
| `bmad-wds-icons`                          | Generate icon sets                            |
| `bmad-wds-images`                         | Generate photos and illustrations             |
| `bmad-wds-implement`                      | Code the designed improvement                 |
| `bmad-wds-import-design-system`           | Import existing design system                 |
| `bmad-wds-Modular Component Architecture` | Three-tier specification system               |
| `bmad-wds-Object Type Router`             | Intelligent object detection and routing      |
| `bmad-wds-page-designs`                   | Generate full page compositions               |
| `bmad-wds-product-evolution`              | Brownfield improvements pipeline              |
| `bmad-wds-project-brief`                  | Establish project context                     |
| `bmad-wds-project-setup`                  | Project onboarding                            |
| `bmad-wds-prototyping`                    | Build interactive prototypes                  |
| `bmad-wds-reverse-engineering`            | Analyze software to extract specs             |
| `bmad-wds-scenarios`                      | Create UX scenario outlines                   |
| `bmad-wds-scenarios-validate`             | Validate scenario outlines                    |
| `bmad-wds-scope-improvement`              | Create focused scenario for improvement       |
| `bmad-wds-stitch-generation`              | AI-assisted UI design using Google Stitch     |
| `bmad-wds-trigger-mapping`                | Map business goals to user psychology         |
| `bmad-wds-trigger-mapping-validate`       | Validate trigger map documents                |
| `bmad-wds-ui-elements`                    | Generate UI components                        |
| `bmad-wds-ux-design`                      | Transform ideas into visual specs             |
| `bmad-wds-videos`                         | Generate motion content and animations        |
| `bmad-wds-view-components`                | Preview design system components              |
| `bmad-wds-wireframes`                     | Generate outline wireframes                   |
| `bmad-wds-workflow-design-system`         | Define and review design system components    |
| `bmad-wds-workflow-discuss`               | Creative dialog for page design               |
| `bmad-wds-workflow-dream`                 | Autonomous scenario flow creation             |
| `bmad-wds-workflow-sketch`                | Analyze sketches and translate to specs       |
| `bmad-wds-workflow-specify`               | Create implementation-ready page spec         |
| `bmad-wds-workflow-suggest`               | Workflow suggestion                           |
| `bmad-wds-workflow-validate`              | Audit page specifications                     |
| `bmad-wds-workflow-visual`                | Create visual representations of designs      |

### Using Skills

Load a skill when a task matches its description. Skills provide step-by-step workflows, templates, and quality gates. For example:

- User says "create a PRD" -> load `bmad-create-prd`
- User says "implement this story" -> load `bmad-dev-story`
- User says "review this code" -> load `bmad-code-review`
- User says "help me brainstorm" -> load `bmad-brainstorming`
- User says "run a retrospective" -> load `bmad-retrospective`
- User says "let's do design thinking" -> load `bmad-cis-design-thinking`

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

Access database through repository/table objects, not directly from UI code:

```kotlin
// Good - through repository
val songs = MusicDatabase.getSongsByPlaylist(playlistId)

// Bad - direct DAO access from Composable
val songs = database.getDao().songsByPlaylist(playlistId).collectAsState()
```

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

```kotlin
// Debug logging (development only)
Timber.d("Loading playlist: %s", playlistId)

// Warning (recoverable issues)
Timber.w("Network request timed out, retrying...")

// Error (failures with exceptions)
Timber.e(exception, "Failed to load song: %s", songId)

// Info (important events)
Timber.i("Playback started for: %s", song.title)
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
e.printStackTrace()       // NEVER - use Timber.e(e, "message")
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
    Timber.e(e, "Operation failed")
}

// With transformation
val result = runCatching {
    parseJson(rawData)
}.getOrElse { e ->
    Timber.e(e, "JSON parse failed")
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
    Timber.e(e, "Pipeline failed")
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

### Step 3: Plan

- Identify which files need to be created or modified
- Consider the impact on existing functionality
- Plan for error cases and edge cases
- Consider performance implications

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

## 14. Communication

### Language

- **Code comments**: Always in English
- **Commit messages**: Always in English
- **Communication with user**: Match the user's language
- **String resources**: Default English only; translations are managed via Crowdin

### String Resources

All string edits should be made to `N-Zik/ComposeN-Zik/src/androidMain/res/values/strings.xml` (default English only). DO NOT edit other language `strings.xml` files -- translations are managed via Crowdin. Only touch the default English file.

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

## 15. Common Patterns

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

## 16. Reference

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
