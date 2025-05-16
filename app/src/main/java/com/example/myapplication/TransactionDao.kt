package com.example.myapplication

import androidx.room.*

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // ✅ Filter only transactions for the logged-in user
    @Query("SELECT * FROM transactions WHERE userEmail = :email ORDER BY date DESC")
    suspend fun getTransactionsForUser(email: String): List<Transaction>
}
