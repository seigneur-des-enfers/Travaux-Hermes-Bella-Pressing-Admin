package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour la gestion de la file d'attente locale d'actions hors-ligne.
 */
@Dao
interface PendingActionDao {

    @Query("SELECT * FROM pending_actions ORDER BY timestamp ASC")
    fun getPendingActionsFlow(): Flow<List<PendingActionEntity>>

    @Query("SELECT * FROM pending_actions ORDER BY timestamp ASC")
    suspend fun getAllPendingActions(): List<PendingActionEntity>

    @Query("SELECT COUNT(*) FROM pending_actions")
    fun getPendingActionCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_actions")
    suspend fun getPendingActionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingAction(action: PendingActionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingActions(actions: List<PendingActionEntity>)

    @Update
    suspend fun updatePendingAction(action: PendingActionEntity)

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun deletePendingAction(id: String)

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun deletePendingActionById(id: String)

    @Query("DELETE FROM pending_actions")
    suspend fun clearAllPendingActions()
}
