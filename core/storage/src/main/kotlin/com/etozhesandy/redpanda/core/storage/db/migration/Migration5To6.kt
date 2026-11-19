package com.etozhesandy.redpanda.core.storage.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Trims and reshapes the indices of the two big tables.
 *
 * The single-column `dialogId` and `profileId` indices on `attachments`, and `dialogId` on
 * `messages`, were each the prefix of a composite one, so they cost a B-tree write per imported row
 * and answered nothing the composite ones could not. The remaining ones gain the `orderInMessage`
 * column the screens sort by, which removes the temporary sort a media grid and a page of
 * attachments used to pay for.
 */
internal val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        listOf(
            "index_attachments_messageId",
            "index_attachments_dialogId",
            "index_attachments_profileId",
            "index_attachments_dialogId_timestampEpoch",
            "index_attachments_dialogId_type_timestampEpoch",
            "index_messages_dialogId",
        ).forEach { name -> connection.execSQL("DROP INDEX IF EXISTS `$name`") }

        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_attachments_messageId_orderInMessage` " +
                "ON `attachments` (`messageId`, `orderInMessage`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_attachments_dialogId_timestampEpoch_orderInMessage` " +
                "ON `attachments` (`dialogId`, `timestampEpoch`, `orderInMessage`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_attachments_dialogId_type_timestampEpoch_orderInMessage` " +
                "ON `attachments` (`dialogId`, `type`, `timestampEpoch`, `orderInMessage`)",
        )
    }
}
