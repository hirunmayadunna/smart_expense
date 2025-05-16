package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // ✅ Every Room table needs a primary key
    val title: String,
    val date: String,
    val amount: String, // You can later convert this to Double if needed
    val type: String, // "Income" or "Expense"
    val category: String,
    val userEmail: String  // ✅ NEW field to link transactions to users
)
