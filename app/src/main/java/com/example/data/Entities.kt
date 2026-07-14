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
    val isLoggedIn: Boolean = true,
    val isAdmin: Boolean = false
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

data class GlobalSettings(
    val upiId: String = "pay.arenaesports@upi",
    val waUrl: String = "https://wa.me/919999999999",
    val tgUrl: String = "https://t.me/arenaesportssupport",
    val referralReward: Double = 50.0,
    val referralMinDeposit: Double = 20.0,
    val minesHouseEdge: Double = 97.0
)

data class CasinoGame(
    val id: Int = 0,
    val name: String,
    val posterUrl: String,
    val isActive: Boolean = true
)

data class AdminStatsResponse(
    val success: Boolean,
    val stats: AdminStats
)

data class AdminStats(
    val totalUsers: Int,
    val dailyUsers: Int,
    val weeklyUsers: Int,
    val monthlyUsers: Int,
    val totalSpent: Double,
    val dailySpent: Double,
    val weeklySpent: Double,
    val monthlySpent: Double,
    val graphData: List<GraphDataPoint>
)

data class GraphDataPoint(
    val date: String,
    val users: Int,
    val spent: Double
)

data class ReferredUser(
    val maskedWhatsapp: String,
    val name: String,
    val totalDeposited: Double,
    val createdAt: String
)

data class ReferredUsersResponse(
    val success: Boolean,
    val total: Int,
    val users: List<ReferredUser>
)

data class MinesGame(
    val id: String,
    val betAmount: Double,
    val minesCount: Int,
    val revealed: List<Int>,
    val multiplier: Double,
    val nextMultiplier: Double,
    val status: String,
    val board: List<Boolean>? = null
)

data class MinesStartResponse(
    val success: Boolean,
    val game: MinesGame,
    val updatedBalances: UserBalances? = null,
    val error: String? = null
)

data class MinesRevealResponse(
    val success: Boolean,
    val status: String,
    val revealed: List<Int>? = null,
    val multiplier: Double? = null,
    val nextMultiplier: Double? = null,
    val mineIndex: Int? = null,
    val board: List<Boolean>? = null,
    val prizeWon: Double? = null,
    val error: String? = null
)

data class MinesCashoutResponse(
    val success: Boolean,
    val status: String,
    val prizeWon: Double,
    val multiplier: Double,
    val board: List<Boolean>,
    val updatedBalances: UserBalances? = null,
    val error: String? = null
)

data class MinesActiveResponse(
    val success: Boolean,
    val active: Boolean,
    val game: MinesGame? = null,
    val error: String? = null
)

data class UserBalances(
    val deposit: Double,
    val withdrawal: Double
)
data class InAppNotification(val title: String, val content: String, val timestamp: Long, val isRead: Boolean = false)
