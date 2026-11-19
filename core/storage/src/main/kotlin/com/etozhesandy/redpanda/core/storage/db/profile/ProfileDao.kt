package com.etozhesandy.redpanda.core.storage.db.profile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.etozhesandy.redpanda.core.model.ProfileStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY importedAt DESC")
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun observeProfile(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfile(id: String): ProfileEntity?

    /** Inserts the row unless one with the same id exists; returns the new row id, or -1 if it did. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(profile: ProfileEntity): Long

    @Update(entity = ProfileEntity::class)
    suspend fun updateImportFields(fields: ProfileImportFields)

    /** Sets the reader's own name for the profile; null brings back the one from the archive. */
    @Query("UPDATE profiles SET customName = :name WHERE id = :id")
    suspend fun updateCustomName(id: String, name: String?)

    @Query("UPDATE profiles SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String)

    @Query("UPDATE profiles SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: ProfileStatus, updatedAt: Long)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: String)
}
