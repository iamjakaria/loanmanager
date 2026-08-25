package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val date: String, // Format: YYYY-MM-DD
    val amount: Double,
    val status: String, // "Repaid" or "Not-Paid"
    val repaidDate: String = "", // Format: YYYY-MM-DD
    val createdAt: Long = System.currentTimeMillis()
)
