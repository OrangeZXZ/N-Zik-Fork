package app.n_zik.android.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

class From23To24Migration : Migration(23, 24) {

    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE SongPlaylistMap ADD COLUMN setVideoId TEXT;")
        } catch (e: Exception) {
            Timber.tag("From23To24Migration").e("Database error ${e.stackTraceToString()}")
        }

    }
}
