package com.etozhesandy.redpanda.core.storage.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE video_transfer (`key` TEXT NOT NULL PRIMARY KEY, bytes INTEGER NOT NULL, total INTEGER NOT NULL, validator TEXT, complete INTEGER NOT NULL)")
    }
}
