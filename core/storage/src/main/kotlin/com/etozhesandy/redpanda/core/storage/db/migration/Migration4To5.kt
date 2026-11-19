package com.etozhesandy.redpanda.core.storage.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Adds `chat_bookmarks`, where the reader's saved place in a dialog's tab is kept. */
internal val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `chat_bookmarks` (`dialogId` TEXT NOT NULL, `tab` TEXT NOT NULL, " +
                "`itemId` TEXT NOT NULL, PRIMARY KEY(`dialogId`, `tab`))",
        )
    }
}
