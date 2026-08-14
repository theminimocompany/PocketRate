package com.reganye.pocketrate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.reganye.pocketrate.data.local.entity.ExpenseSplitEntity

@Dao
interface ExpenseSplitDao {
    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpense(expenseId: String): List<ExpenseSplitEntity>

    @Query("SELECT * FROM expense_splits WHERE expenseId IN (:expenseIds)")
    suspend fun getSplitsForExpenses(expenseIds: List<String>): List<ExpenseSplitEntity>

    @Query("SELECT COUNT(*) FROM expense_splits WHERE companionId = :companionId")
    suspend fun countSplitsFor(companionId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplit(split: ExpenseSplitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    /**
     * Atomically replaces all splits for an expense.
     * If the insert fails, the deletes are rolled back.
     */
    @Transaction
    suspend fun replaceSplitsForExpense(expenseId: String, splits: List<ExpenseSplitEntity>) {
        deleteSplitsForExpense(expenseId)
        insertSplits(splits)
    }
}
