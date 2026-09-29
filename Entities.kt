package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet")
data class WalletEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 25,
    val totalEarned: Int = 25,
    val totalRedeemed: Int = 0,
    val lastDailyClaimDay: Long = -1L,
    val dailyStreak: Int = 1,
    val sentencesTyped: Int = 0,
    val highestWpm: Int = 0,
    val totalCharsTyped: Int = 0,
    val adsWatched: Int = 0
)

@Entity(tableName = "coin_transactions")
data class CoinTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Int,
    val type: String, // "TYPING", "DAILY", "AD", "WITHDRAWAL"
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "withdrawal_requests")
data class WithdrawalRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val referenceId: String,
    val method: String, // "UPI", "Amazon Gift Card", "Redeem Code"
    val accountDetail: String,
    val coinAmount: Int,
    val fiatValue: String,
    val status: String = "PENDING", // "PENDING", "APPROVED", "PROCESSING"
    val timestamp: Long = System.currentTimeMillis()
)
