package com.etozhesandy.redpanda.core.storage.db

import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens the database ahead of the first screen that reads it, so that a pending migration starts at
 * once and [DatabaseMigrationState] covers exactly the time it runs. Blocking: call it off the main thread.
 */
@Singleton
class DatabaseOpener @Inject constructor(
    private val database: Lazy<AppDatabase>,
) {
    fun open() {
        database.get().openHelper.writableDatabase
    }
}
