package app.n_zik.android.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class From27To28Migration: Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Song ADD COLUMN position INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE Album ADD COLUMN position INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE Artist ADD COLUMN position INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE Playlist ADD COLUMN position INTEGER NOT NULL DEFAULT -1")
    }
}
