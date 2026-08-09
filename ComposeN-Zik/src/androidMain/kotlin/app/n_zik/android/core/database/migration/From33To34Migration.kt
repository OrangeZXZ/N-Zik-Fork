package app.n_zik.android.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class From33To34Migration : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `Artist` ADD COLUMN `lastFetch` INTEGER DEFAULT NULL")
    }
}
