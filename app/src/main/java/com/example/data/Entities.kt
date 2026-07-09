package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val whatsappNumber: String,
    val name: String = "",
    val referralCodeUsed: String?,
    val ownReferralCode: String,
    val depositBalance: Double = 0.0,
    val withdrawalBalance: Double = 0.0,
    val referredCount: Int = 0,
    val isLoggedIn: Boolean = true
)

@Entity(tableName = "tournaments")
data class Tournament(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val game: String, // "BGMI", "PUBG", "FREEFIRE"
    val title: String,
    val posterRes: String, // helper key for falls/styles
    val entryFee: Double,
    val prizePool: Double,
    val prize1st: Double,
    val prize2nd: Double,
    val prize3rd: Double,
    val prize4th: Double,
    val rules: String,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val startTime: String,
    val isJoined: Boolean = false
)

@Entity(tableName = "registrations")
data class Registration(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val whatsappNumber: String,
    val tournamentId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val whatsappNumber: String,
    val type: String, // "DEPOSIT", "WITHDRAWAL"
    val amount: Double,
    val upiId: String, // UPI ID for copy / request
    val referenceNumber: String?, // 12-digit number for deposit, or null for withdrawal
    val status: String, // "PENDING", "APPROVED", "REJECTED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "game_histories")
data class GameHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val whatsappNumber: String,
    val gameName: String,
    val prizeWon: Double?, // null for PENDING, otherwise amount
    val status: String, // "PENDING", "COMPLETED"
    val timestamp: Long = System.currentTimeMillis()
)
