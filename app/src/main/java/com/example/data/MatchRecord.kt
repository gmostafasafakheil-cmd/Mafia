package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerX: String,
    val playerO: String,
    val winner: String?, // "X", "O", or null (for Draw)
    val mode: String,    // "تک‌نفره - آسان", "تک‌نفره - متوسط", "تک‌نفره - سخت", "دونفره"
    val timestamp: Long = System.currentTimeMillis()
)
