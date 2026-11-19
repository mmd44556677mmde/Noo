package com.etozhesandy.redpanda.core.storage.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the database is being brought up to the current schema.
 *
 * Room migrates when it first opens the file, which for an imported archive of gigabytes can take
 * minutes, and the screens that wait on it would otherwise show nothing but a spinner. The need is
 * known beforehand — the file records the version it was last written at — so it is read here
 * when the process starts, and cleared as soon as the database has opened.
 */
@Singleton
class DatabaseMigrationState @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val migrating = MutableStateFlow(hasPendingMigration(context))

    val isMigrating: StateFlow<Boolean> = migrating.asStateFlow()

    internal fun markUpToDate() {
        migrating.value = false
    }

    private fun hasPendingMigration(context: Context): Boolean {
        val file = context.getDatabasePath(DATABASE_NAME)
        if (!file.exists()) return false
        // Opened read-only and only to read the version: this does not migrate anything, and a
        // file that cannot be read this way is left for Room to report.
        return runCatching {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                database.version in 1 until DATABASE_VERSION
            }
        }.getOrDefault(false)
    }
}
