package app.n_zik.android.playback.exceptions

import app.n_zik.android.playback.services.*
import app.n_zik.android.playback.models.*
import app.n_zik.android.playback.exceptions.*
import app.n_zik.android.playback.utils.*

import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi

@UnstableApi
class PlayableFormatNotFoundException(
    message: String? = null,
    cause: Throwable? = null
) : PlaybackException(message, cause, ERROR_CODE_REMOTE_ERROR)

@UnstableApi
class UnplayableException(
    message: String? = null,
    cause: Throwable? = null
) : PlaybackException(message, cause, ERROR_CODE_REMOTE_ERROR)

@UnstableApi
class LoginRequiredException(
    message: String? = null,
    cause: Throwable? = null
) : PlaybackException(message, cause, ERROR_CODE_REMOTE_ERROR)

@UnstableApi
class VideoIdMismatchException(
    message: String? = null,
    cause: Throwable? = null
) : PlaybackException(message, cause, ERROR_CODE_REMOTE_ERROR)

@UnstableApi
class PlayableFormatNonSupported(
    message: String? = null,
    cause: Throwable? = null
) : PlaybackException(message, cause, ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED)

@UnstableApi
class NoInternetException(
    message: String? = null,
    cause: Throwable? = null
) : PlaybackException(message, cause, ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)

@UnstableApi
class TimeoutException(
    message: String? = null,
    cause: Throwable? = null
) : PlaybackException(message, cause, ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)

@UnstableApi
class UnknownException(
    message: String? = null,
    cause: Throwable? = null
) : PlaybackException(message, cause, ERROR_CODE_REMOTE_ERROR)

@UnstableApi
class FakeException(
    message: String? = null,
    cause: Throwable? = null
) : PlaybackException(message, cause, ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
