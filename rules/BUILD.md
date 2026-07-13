# Build & Test Rules

**Version:** 1.1.0 | **Last updated:** 2026-07-11

## Gradle Version Catalog

Use `N-Zik/gradle/libs.versions.toml` references. NEVER hardcode versions.

```kotlin
// GOOD
implementation(libs.timber)
implementation(libs.room)

// BAD
implementation("com.jakewharton.timber:timber:5.0.1")
```

If a needed library isn't in the catalog → HALT and ask user before adding to `libs.versions.toml`.

## Build Commands

```bash
./gradlew :ComposeN-Zik:assembleDebug          # Debug build (primary)
./gradlew :ComposeN-Zik:assembleFoss           # FOSS build
./gradlew :ComposeN-Zik:assembleBeta           # Beta build
./gradlew :ComposeN-Zik:test                   # All tests
./gradlew :ComposeN-Zik:testDebugUnitTest --tests "app.n_zik.android.playback.utils.ShufflerTest"
./gradlew clean :ComposeN-Zik:assembleDebug    # Clean + debug
```

## Verification

ALWAYS verify changes compile before reporting. If build fails:

1. Read error messages
2. Fix issues
3. Rebuild until successful
4. Run relevant tests

HALT after 3 failed build attempts — report to user with full error log (see RECOVERY.md).

### Done.txt Format

File: `N-Zik/assets/notes/Done.txt`

When committing, update `Done.txt` with:

```
## 2026-07-11
- Modified: app/n_zik/android/components/player/LyricsScreen.kt
- Modified: app/n_zik/android/core/database/SongDao.kt
- Issue: https://github.com/user/repo/issues/42
```

## Build Types

| Type    | Command         | Notes                         |
| ------- | --------------- | ----------------------------- |
| `debug` | `assembleDebug` | Primary development build     |
| `foss`  | `assembleFoss`  | No proprietary dependencies   |
| `beta`  | `assembleBeta`  | Beta build with debug signing |

## Proguard/R8

- `minified` build uses R8 shrinkResources
- Do NOT add Proguard rules unless explicitly asked
- Test minified build if modifying serialization or reflection-heavy code

## Commit Convention

Format: `type(scope): short description`

| Type       | When to use                                |
| ---------- | ------------------------------------------ |
| `feat`     | New feature or capability                  |
| `fix`      | Bug fix                                    |
| `refactor` | Code restructuring without behavior change |
| `chore`    | Build, CI, dependency, or tooling changes  |
| `docs`     | Documentation changes only                 |
| `test`     | Adding or updating tests                   |
| `perf`     | Performance improvement                    |

Examples:

```
feat(lyrics): add synced lyrics display
fix(player): handle seek to end of track
refactor(database): extract playlist DAO logic
chore(deps): update room to 2.6.0
```

Rules:

- Under 72 characters
- Imperative mood ("add" not "added")
- Scope optional but recommended
- No period at end
- Include GitHub issue URL when applicable — use `issue https://...` (avoid keywords that auto-close issues like "fixes" or "closes")

## Branching

- Work on `main` unless user specifies a branch
- Branch naming: `feat/<name>`, `fix/<name>`, `chore/<name>`
- If merge conflict → HALT, report to user

## Testing — JUnit 5 + MockK

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

Test files: `ComposeN-Zik/src/test/kotlin/` — mirror source package structure.

New features/bug fixes should include at least one test. If no test framework is available → HALT and note it.

## CI Expectations

- Pre-commit hooks may run lint and build checks
- If pre-commit hook fails → fix before committing
- If CI pipeline fails after push → HALT, investigate, fix

## Code Formatting

- Run ktlint/detekt if configured in the project
- Use Android Studio auto-format for consistent style
- Follow existing file formatting patterns
- No trailing whitespace, newline at end of file
- If ktlint/detekt fails → HALT, fix formatting before continuing
