package app.kreate.android.me.knighthat.utils

import app.kreate.android.BuildConfig

object Repository {

    const val GITHUB = "https://github.com"
    const val GITHUB_API = "https://api.github.com"
    const val GITHUB_RAW = "https://raw.githubusercontent.com"

    const val OWNER = "NEVARLeVrai"
    const val REPO = "$OWNER/${BuildConfig.APP_NAME}"
    const val REPO_URL = "$GITHUB/$REPO"
    const val RAW_REPO_URL = "$GITHUB_RAW/$REPO/main"

    const val LATEST_TAG_URL = "$REPO/releases/latest"
    const val RELEASE_DOWNLOAD_URL = "$REPO_URL/releases/download/v"
    
    const val FASTLANE_CHANGELOGS_PATH = "fastlane/metadata/android/en-US/changelogs"
    const val FASTLANE_CHANGELOGS_URL = "$RAW_REPO_URL/$FASTLANE_CHANGELOGS_PATH"
}


