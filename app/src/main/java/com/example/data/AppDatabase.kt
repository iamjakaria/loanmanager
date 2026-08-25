package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [LoanEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "loan_manager_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.loanDao())
                    }
                }
            }

            suspend fun populateInitialData(loanDao: LoanDao) {
                if (loanDao.getCount() == 0) {
                    val initialLoans = listOf(
                        LoanEntity(
                            name = "John Doe",
                            date = "2026-08-24",
                            amount = 25000.00,
                            status = "Repaid",
                            repaidDate = "2026-08-25"
                        ),
                        LoanEntity(
                            name = "Jane Smith",
                            date = "2026-08-22",
                            amount = 120500.50,
                            status = "Not-Paid",
                            repaidDate = ""
                        ),
                        LoanEntity(
                            name = "Robert Johnson",
                            date = "2026-08-19",
                            amount = 4500.00,
                            status = "Repaid",
                            repaidDate = "2026-08-20"
                        )
                    )
                    loanDao.insertLoans(initialLoans)
                }
            }
        }
    }
}
