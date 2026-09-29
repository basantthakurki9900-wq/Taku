package com.example.data

import com.example.data.model.CoinTransaction
import com.example.data.model.WalletEntity
import com.example.data.model.WithdrawalRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.math.max

class WalletRepository(private val dao: WalletDao) {

    val walletFlow: Flow<WalletEntity> = dao.getWalletFlow().map { entity ->
        entity ?: WalletEntity()
    }

    val transactionsFlow: Flow<List<CoinTransaction>> = dao.getAllTransactions()
    val withdrawalsFlow: Flow<List<WithdrawalRequest>> = dao.getAllWithdrawals()

    suspend fun getWallet(): WalletEntity {
        val existing = dao.getWalletDirect()
        if (existing != null) return existing
        val defaultWallet = WalletEntity()
        dao.saveWallet(defaultWallet)
        return defaultWallet
    }

    suspend fun addTypingReward(sentence: String, wpm: Int, accuracy: Int, rewardCoins: Int = 5) {
        val current = getWallet()
        val newCoins = current.coins + rewardCoins
        val newEarned = current.totalEarned + rewardCoins
        val newSentences = current.sentencesTyped + 1
        val newHighestWpm = max(current.highestWpm, wpm)
        val newTotalChars = current.totalCharsTyped + sentence.length

        val updated = current.copy(
            coins = newCoins,
            totalEarned = newEarned,
            sentencesTyped = newSentences,
            highestWpm = newHighestWpm,
            totalCharsTyped = newTotalChars
        )
        dao.saveWallet(updated)

        dao.insertTransaction(
            CoinTransaction(
                title = "Typing Challenge",
                amount = rewardCoins,
                type = "TYPING",
                note = "$wpm WPM • $accuracy% Accuracy"
            )
        )
    }

    suspend fun claimDailyReward(rewardAmount: Int = 20): Result<Pair<Int, Int>> {
        val current = getWallet()
        val todayDayEpoch = System.currentTimeMillis() / 86400000L

        if (current.lastDailyClaimDay == todayDayEpoch) {
            return Result.failure(IllegalStateException("You have already claimed today's daily reward. Come back tomorrow!"))
        }

        val isConsecutive = (todayDayEpoch - current.lastDailyClaimDay) == 1L
        val newStreak = if (isConsecutive) (current.dailyStreak % 7) + 1 else 1
        val streakBonus = (newStreak - 1) * 5
        val totalBonus = rewardAmount + streakBonus

        val updated = current.copy(
            coins = current.coins + totalBonus,
            totalEarned = current.totalEarned + totalBonus,
            lastDailyClaimDay = todayDayEpoch,
            dailyStreak = newStreak
        )
        dao.saveWallet(updated)

        dao.insertTransaction(
            CoinTransaction(
                title = "Daily Login Reward",
                amount = totalBonus,
                type = "DAILY",
                note = "Day $newStreak Streak Bonus"
            )
        )

        return Result.success(Pair(totalBonus, newStreak))
    }

    suspend fun addAdReward(amount: Int = 10) {
        val current = getWallet()
        val updated = current.copy(
            coins = current.coins + amount,
            totalEarned = current.totalEarned + amount,
            adsWatched = current.adsWatched + 1
        )
        dao.saveWallet(updated)

        dao.insertTransaction(
            CoinTransaction(
                title = "Sponsored Ad Reward",
                amount = amount,
                type = "AD",
                note = "Unity Ads BP_Rewarded_Android"
            )
        )
    }

    suspend fun submitWithdrawal(
        method: String,
        accountDetail: String,
        coinAmount: Int,
        fiatValue: String
    ): Result<WithdrawalRequest> {
        val current = getWallet()
        if (coinAmount < 100) {
            return Result.failure(IllegalArgumentException("Minimum 100 coins required to withdraw."))
        }
        if (current.coins < coinAmount) {
            return Result.failure(IllegalArgumentException("Insufficient balance. You have ${current.coins} coins."))
        }

        val refId = "TR-" + UUID.randomUUID().toString().substring(0, 6).uppercase()
        val request = WithdrawalRequest(
            referenceId = refId,
            method = method,
            accountDetail = accountDetail,
            coinAmount = coinAmount,
            fiatValue = fiatValue,
            status = "PENDING"
        )

        val updatedWallet = current.copy(
            coins = current.coins - coinAmount,
            totalRedeemed = current.totalRedeemed + coinAmount
        )
        dao.saveWallet(updatedWallet)
        dao.insertWithdrawal(request)
        dao.insertTransaction(
            CoinTransaction(
                title = "$method Redemption",
                amount = -coinAmount,
                type = "WITHDRAWAL",
                note = "Ref: $refId • $fiatValue"
            )
        )

        return Result.success(request)
    }
}
