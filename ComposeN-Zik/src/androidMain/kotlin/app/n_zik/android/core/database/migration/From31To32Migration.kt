package app.n_zik.android.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class From31To32Migration : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `Song` ADD COLUMN `playCount` INTEGER NOT NULL DEFAULT 0")
    }
}
