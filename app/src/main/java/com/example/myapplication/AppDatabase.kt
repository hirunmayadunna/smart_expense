package com.example.myapplication

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Transaction::class,
        BudgetEntity::class,
        User::class  // ✅ Added User entity
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun userDao(): UserDao  // ✅ Added UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_tracker_db"
                )
                    .fallbackToDestructiveMigration() // ✅ Prevents crash on schema change
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}
