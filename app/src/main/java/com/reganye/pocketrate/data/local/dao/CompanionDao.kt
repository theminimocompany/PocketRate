package com.reganye.pocketrate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reganye.pocketrate.data.local.entity.CompanionEntity

@Dao
interface CompanionDao {
    @Query("SELECT * FROM companions WHERE tripId = :tripId ORDER BY name ASC")
    suspend fun getCompanionsForTrip(tripId: String): List<CompanionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanion(companion: CompanionEntity)

    @Query("DELETE FROM companions WHERE id = :id")
    suspend fun deleteCompanion(id: String)
}
