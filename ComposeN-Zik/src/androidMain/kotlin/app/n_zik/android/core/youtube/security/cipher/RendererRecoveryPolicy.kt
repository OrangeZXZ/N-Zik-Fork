package app.n_zik.android.core.security.cipher

/**
 * Thrown when the cipher WebView's renderer process dies (OOM kill under memory pressure).
 * Transient — NOT a BadWebViewException which would permanently disable poTokens.
 */
class CipherRendererGoneException(message: String) : Exception(message)

/**
 * Tracks consecutive cipher WebView renderer deaths and implements a short backoff window
 * so the current song fails over fast instead of stalling on a doomed ~2.8 MB player.js parse.
 *
 * The window must stay short/half-open: the non-WebView fallbacks are unreliable, so the
 * cipher WebView is the primary path and we retry it as soon as pressure may have eased.
 */
class RendererRecoveryPolicy {
    companion object {
        /** After this many consecutive renderer deaths, open the backoff window. */
        private const val FAILURE_THRESHOLD = 3
        /** Backoff window duration in milliseconds. */
        private const val BACKOFF_WINDOW_MS = 60_000L
    }

    var consecutiveFailures: Int = 0
        private set

    private var backoffWindowOpenedAt: Long = 0L

    /** Timestamp (elapsedRealtime) when the current backoff window ends, or 0 if not in backoff. */
    val backoffUntilMs: Long
        get() = if (consecutiveFailures >= FAILURE_THRESHOLD) backoffWindowOpenedAt + BACKOFF_WINDOW_MS else 0L

    /**
     * Should we attempt to create a cipher WebView right now?
     * Returns false during the backoff window after repeated renderer deaths.
     */
    fun shouldAttempt(nowMs: Long): Boolean {
        if (consecutiveFailures < FAILURE_THRESHOLD) return true
        // Backoff window: skip WebView creation so the current song fails over fast.
        val elapsed = nowMs - backoffWindowOpenedAt
        return elapsed >= BACKOFF_WINDOW_MS
    }

    /** Called when a cipher operation (deobfuscate, n-transform, generatePoToken) succeeds. */
    fun onSuccess() {
        consecutiveFailures = 0
        backoffWindowOpenedAt = 0L
    }

    /** Called when the renderer dies. Records the failure and opens the backoff window if threshold reached. */
    fun onFailure(nowMs: Long) {
        consecutiveFailures++
        if (consecutiveFailures >= FAILURE_THRESHOLD) {
            backoffWindowOpenedAt = nowMs
        }
    }
}
