package app.n_zik.android.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.n_zik.android.enums.lyrics.LyricsType

class From29To30Migration : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create new table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `Lyrics_new` (
                `songId` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `data` TEXT, 
                PRIMARY KEY(`songId`, `type`), 
                FOREIGN KEY(`songId`) REFERENCES `Song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Migrate fixed lyrics to Unsynced
        db.execSQL(
            """
            INSERT INTO `Lyrics_new` (`songId`, `type`, `data`)
            SELECT `songId`, '${LyricsType.Unsynced.name}', `fixed` FROM `Lyrics` WHERE `fixed` IS NOT NULL
            """.trimIndent()
        )

        // Migrate synced lyrics to Synced
        db.execSQL(
            """
            INSERT INTO `Lyrics_new` (`songId`, `type`, `data`)
            SELECT `songId`, '${LyricsType.Synced.name}', `synced` FROM `Lyrics` WHERE `synced` IS NOT NULL
            """.trimIndent()
        )

        // Drop old table
        db.execSQL("DROP TABLE `Lyrics`")

        // Rename new table to old table
        db.execSQL("ALTER TABLE `Lyrics_new` RENAME TO `Lyrics`")
    }
}
