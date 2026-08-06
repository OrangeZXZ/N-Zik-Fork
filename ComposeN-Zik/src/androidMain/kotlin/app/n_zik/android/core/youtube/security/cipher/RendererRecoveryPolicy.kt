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
 *
 * @param maxConsecutiveFailures consecutive deaths before entering the backoff window.
 * @param backoffMs backoff window duration in milliseconds.
 */
class RendererRecoveryPolicy(
    private val maxConsecutiveFailures: Int = DEFAULT_MAX_CONSECUTIVE_FAILURES,
    private val backoffMs: Long = DEFAULT_BACKOFF_MS,
) {
    var consecutiveFailures: Int = 0
        private set

    /** Timestamp (elapsedRealtime) when the current backoff window ends, or 0 if not in backoff. */
    var backoffUntilMs: Long = 0L
        private set

    /**
     * Should we attempt to create a cipher WebView right now?
     * Returns false during the backoff window after repeated renderer deaths.
     */
    fun shouldAttempt(nowMs: Long): Boolean =
        consecutiveFailures < maxConsecutiveFailures || nowMs >= backoffUntilMs

    /** Called when a cipher operation (deobfuscate, n-transform, generatePoToken) succeeds. */
    fun onSuccess() {
        consecutiveFailures = 0
        backoffUntilMs = 0L
    }

    /** Called when the renderer dies. Records the failure and opens the backoff window if threshold reached. */
    fun onFailure(nowMs: Long) {
        consecutiveFailures++
        if (consecutiveFailures >= maxConsecutiveFailures) {
            backoffUntilMs = nowMs + backoffMs
        }
    }

    companion object {
        const val DEFAULT_MAX_CONSECUTIVE_FAILURES = 3
        const val DEFAULT_BACKOFF_MS = 60_000L
    }
}
