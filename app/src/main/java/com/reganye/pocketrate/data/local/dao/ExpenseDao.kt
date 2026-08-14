package com.reganye.pocketrate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.reganye.pocketrate.data.local.entity.ExpenseEntity
import com.reganye.pocketrate.data.local.entity.TripTotal

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC")
    suspend fun getExpensesForTrip(tripId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: String): ExpenseEntity?

    @Query("SELECT COALESCE(SUM(convertedAmount), 0.0) FROM expenses WHERE tripId = :tripId")
    suspend fun getTotalSpent(tripId: String): Double

    @Query("SELECT tripId, COALESCE(SUM(convertedAmount), 0.0) AS total FROM expenses GROUP BY tripId")
    suspend fun getTotalsByTrip(): List<TripTotal>

    @Query("SELECT COUNT(*) FROM expenses WHERE payerId = :companionId")
    suspend fun countExpensesPaidBy(companionId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    /**
     * Atomically deletes an expense and all of its splits.
     * If either step fails, the transaction is rolled back.
     */
    @Transaction
    suspend fun deleteExpenseWithSplits(expenseId: String) {
        deleteSplitsForExpense(expenseId)
        deleteExpense(expenseId)
    }
}
