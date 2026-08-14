package com.reganye.pocketrate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.reganye.pocketrate.data.local.entity.TripEntity

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    suspend fun getAllTrips(): List<TripEntity>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getTripById(id: String): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    /**
     * Updates an existing trip. Never use insert-with-REPLACE for updates:
     * REPLACE is DELETE+INSERT in SQLite, and the DELETE fires the child
     * tables' ON DELETE CASCADE — wiping the trip's expenses, companions,
     * and splits.
     */
    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTrip(id: String)

    @Query("DELETE FROM expense_splits WHERE expenseId IN (SELECT id FROM expenses WHERE tripId = :tripId)")
    suspend fun deleteSplitsForTrip(tripId: String)

    @Query("DELETE FROM expenses WHERE tripId = :tripId")
    suspend fun deleteExpensesForTrip(tripId: String)

    @Query("DELETE FROM companions WHERE tripId = :tripId")
    suspend fun deleteCompanionsForTrip(tripId: String)

    /**
     * Atomically deletes a trip and all of its expenses, splits, and companions.
     * If any step fails, the transaction is rolled back.
     */
    @Transaction
    suspend fun deleteTripWithDependencies(tripId: String) {
        deleteSplitsForTrip(tripId)
        deleteExpensesForTrip(tripId)
        deleteCompanionsForTrip(tripId)
        deleteTrip(tripId)
    }
}
