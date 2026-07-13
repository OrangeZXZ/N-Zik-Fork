package app.kreate.android.me.knighthat.utils

import app.n_zik.android.BuildConfig

object Repository {

    const val GITHUB = "https://github.com"
    const val GITHUB_API = "https://api.github.com"
    const val GITHUB_RAW = "https://raw.githubusercontent.com"

    const val OWNER = "N-Zik-Group"
    const val REAL_OWNER = "NEVARLeVrai"
    const val REPO = "$OWNER/${BuildConfig.APP_NAME}"
    const val REPO_URL = "$GITHUB/$REPO"
    const val RAW_REPO_URL = "$GITHUB_RAW/$REPO/main"

    const val LATEST_TAG_URL = "$REPO/releases/latest"
    const val RELEASE_DOWNLOAD_URL = "$REPO_URL/releases/download/v"
    const val CHANGELOGS_PATH = "Updater/changelogs"
    const val CHANGELOGS_URL = "$RAW_REPO_URL/$CHANGELOGS_PATH"
}


