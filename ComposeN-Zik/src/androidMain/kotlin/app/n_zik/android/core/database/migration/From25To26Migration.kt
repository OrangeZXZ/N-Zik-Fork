package app.n_zik.android.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

class From25To26Migration : Migration(25, 26) {

    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE Playlist ADD COLUMN isYoutubePlaylist INTEGER NOT NULL DEFAULT 0;")
        } catch (e: Exception) {
            Timber.e("Database From25To26Migration error ${e.stackTraceToString()}")
        }

    }
}
