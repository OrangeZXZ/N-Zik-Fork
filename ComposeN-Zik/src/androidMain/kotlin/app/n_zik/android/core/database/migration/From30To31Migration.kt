package app.n_zik.android.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class From30To31Migration : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add codecs, sampleRate, perceptualLoudnessDb, audioChannels, playbackUrl columns to Format table
        db.execSQL("ALTER TABLE `Format` ADD COLUMN `codecs` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `Format` ADD COLUMN `sampleRate` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `Format` ADD COLUMN `perceptualLoudnessDb` REAL DEFAULT NULL")
        db.execSQL("ALTER TABLE `Format` ADD COLUMN `audioChannels` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `Format` ADD COLUMN `playbackUrl` TEXT DEFAULT NULL")
    }
}
