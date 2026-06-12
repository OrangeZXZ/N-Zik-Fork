package app.n_zik.android.updater.models

import app.n_zik.android.updater.services.*
import app.n_zik.android.updater.models.*
import app.n_zik.android.updater.ui.*

object UpdaterConstants {
    const val SUFFIX_FULL = "-f"
    const val SUFFIX_BETA = "-b"
    const val SUFFIX_MINIFIED = "-m"

    const val SUFFIX_CHAR_FULL = "f"
    const val SUFFIX_CHAR_BETA = "b"
    const val SUFFIX_CHAR_MINIFIED = "m"

    const val TYPE_FULL = "full"
    const val TYPE_BETA = "beta"
    const val TYPE_MINIFIED = "minified"
    const val TYPE_STABLE = "stable"

    const val PREFIX_VERSION = "v"

    const val CHANGELOG_ADDED = "added"
    const val CHANGELOG_CHANGED = "changed"
    const val CHANGELOG_IMPROVED = "improved"
    const val CHANGELOG_FIXED = "fixed"
    const val CHANGELOG_REFACTOR = "refactor"
    const val CHANGELOG_OTHER = "other"
    const val CHANGELOG_REMOVED = "removed"
    const val CHANGELOG_DEPRECATED = "deprecated"
}
