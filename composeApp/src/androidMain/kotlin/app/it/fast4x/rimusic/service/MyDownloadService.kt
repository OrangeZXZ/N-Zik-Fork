package app.it.fast4x.rimusic.service

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import app.kreate.android.R
import app.it.fast4x.rimusic.service.MyDownloadHelper.DOWNLOAD_NOTIFICATION_CHANNEL_ID

private const val JOB_ID = 8888
private const val FOREGROUND_NOTIFICATION_ID = 8989

@UnstableApi
class MyDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.download, 0
) {

    override fun getDownloadManager(): DownloadManager {

        // This will only happen once, because getDownloadManager is guaranteed to be called only once
        // in the life cycle of the process.
        val downloadManager: DownloadManager = MyDownloadHelper.getDownloadManager(this)
        val downloadNotificationHelper: DownloadNotificationHelper =
            MyDownloadHelper.getDownloadNotificationHelper(this)
        downloadManager.addListener(
            TerminalStateNotificationHelper(
                this,
                downloadNotificationHelper,
                FOREGROUND_NOTIFICATION_ID + 1
            )
        )
        return downloadManager
    }

    override fun getScheduler(): PlatformScheduler? {
        return if(Util.SDK_INT >= 21) PlatformScheduler(this, JOB_ID) else null
    }

    private var maxActiveDownloads = 0

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        if (downloads.size > maxActiveDownloads) {
            maxActiveDownloads = downloads.size
        } else if (downloads.isEmpty()) {
            maxActiveDownloads = 0
        }
        val downloaded = maxActiveDownloads - downloads.size
        val message = if (maxActiveDownloads > 1) {
            getString(R.string.download_progress, downloaded, maxActiveDownloads)
        } else {
            getString(R.string.download_in_progress, downloads.size)
        }

        val activeDownload = downloads.firstOrNull { it.state == Download.STATE_DOWNLOADING } ?: downloads.firstOrNull()
        val currentDownloadName = activeDownload?.request?.data?.let { Util.fromUtf8Bytes(it) }

        return NotificationCompat.Builder(this, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.download_progress)
            .setContentTitle(getString(R.string.download))
            .setContentText(currentDownloadName ?: message)
            .setSubText(message) // Always show the progress like "11 / 20" in subtext
            .setProgress(maxActiveDownloads, downloaded, maxActiveDownloads == 0)
            .setOngoing(true)
            .setShowWhen(false)
            .build()    }

    /**
     * Creates and displays notifications for downloads when they complete or fail.
     *
     *     * This helper will outlive the lifespan of a single instance of [MyDownloadService].
     * It is static to avoid leaking the first [MyDownloadService] instance.
     */
    private class TerminalStateNotificationHelper(
        private val context: Context,
        private val notificationHelper: DownloadNotificationHelper,
        private val notificationId: Int
    ) : DownloadManager.Listener {
        private var completedCount = 0
        private var failedCount = 0
        private var lastName = ""

        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            if (download.state == Download.STATE_COMPLETED) {
                completedCount++
                lastName = Util.fromUtf8Bytes(download.request.data)
            } else if (download.state == Download.STATE_FAILED) {
                failedCount++
                lastName = Util.fromUtf8Bytes(download.request.data)
            }
        }

        override fun onIdle(downloadManager: DownloadManager) {
            if (completedCount == 0 && failedCount == 0) {
                return
            }

            val title = if (failedCount > 0) {
                context.getString(R.string.download_completed_with_failed, completedCount, failedCount)
            } else {
                context.getString(R.string.download_completed, completedCount)  
            }

            val notification = NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(if (failedCount == 0) R.drawable.downloaded else R.drawable.alert_circle_not_filled)
                .setContentTitle(context.getString(R.string.download))
                .setContentText(title)
                .setSubText(lastName)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()

            // Use the defined static ID (+1) to prevent deletion when the service stops
            NotificationUtil.setNotification(context, notificationId, notification)

            completedCount = 0
            failedCount = 0
            lastName = ""
        }
    }

}
