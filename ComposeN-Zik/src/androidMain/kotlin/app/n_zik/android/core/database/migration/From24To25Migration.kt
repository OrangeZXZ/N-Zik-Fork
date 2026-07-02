package app.n_zik.android.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

class From24To25Migration : Migration(24, 25) {

    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE Playlist ADD COLUMN isEditable INTEGER NOT NULL DEFAULT 0;")
        } catch (e: Exception) {
            Timber.tag("From24To25Migration").e("Database error ${e.stackTraceToString()}")
        }

    }
}
