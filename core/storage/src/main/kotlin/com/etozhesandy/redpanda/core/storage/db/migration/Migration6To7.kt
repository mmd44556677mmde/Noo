package com.etozhesandy.redpanda.core.storage.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Adds the reader's own name for a profile and their notes on it, both empty until they write one. */
internal val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `profiles` ADD COLUMN `customName` TEXT")
        connection.execSQL("ALTER TABLE `profiles` ADD COLUMN `notes` TEXT NOT NULL DEFAULT ''")
    }
}
