package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,       // "caterogious"
    val date: String,           // e.g. "10/27/2023"
    val day: String,            // e.g. "Sunday" or format
    val amount: Double,         // Income is positive, Expense is negative
    val comment: String,        // Comment / notes
    val isSynced: Boolean = false
) : Serializable
