# N-Zik Changelog

## [4.0.0] - 2026-05-30

### Major Changes & Refactoring 🚀
- **Network Architecture Overhaul:** Fully centralized all HTTP and OkHttp network calls to go through a single `NetworkClientFactory`. This ensures proxy settings and timeout policies are consistently applied globally.
- **Improved Player Resiliency:** Completely refactored the ExoPlayer error handling in `PlayerServiceModern`. The service now elegantly catches HTTP 403 server restrictions without getting stuck in endless retry loops.
- **Deciphering Pipeline Upgrade:** Replaced the legacy `newpipe-extractor` dependency with modern `PoTokenWebView`, `PlayerJsFetcher`, and `CipherWebView` handling for robust YouTube playback and streaming.

### Features ✨
- **Enhanced Playback Error UI:** If a song cannot be played due to server restrictions (HTTP 403), the UI (`Thumbnail` and `PlaybackError` screens) now accurately displays localized, user-friendly warnings instead of cryptic networking errors.
- **Actionable Background Notifications:** When encountering playback errors while the app is in the background, a `SmartMessage` Toast will explicitly warn the user why a track was skipped.

### Fixes 🐛
- Removed unused and conflicting dependencies like `protobuf-java` and the obsolete `NewPipeExtractor` library to resolve build/duplicate class issues.
- Restored missing `R.string.error_this_song_cannot_be_played_due_to_server_restrictions` and `R.string.login_required_to_play_this_media` mapping to the UI properly.
- Added comprehensive `PLAYER_STATUS` logs making debugging playback/network issues significantly easier via logcat.

