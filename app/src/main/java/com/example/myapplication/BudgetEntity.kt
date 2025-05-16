package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_table")
data class BudgetEntity(
    @PrimaryKey val id: Int = 1,           // Always use id = 1 (only one budget row)
    val budgetAmount: Float
)
