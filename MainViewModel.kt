package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.RewardedAdManager
import com.example.data.AppDatabase
import com.example.data.WalletRepository
import com.example.data.model.CoinTransaction
import com.example.data.model.WalletEntity
import com.example.data.model.WithdrawalRequest
import com.example.typing.TypingChallengeRepository
import com.example.typing.TypingSentence
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TypingSessionState(
    val currentSentence: TypingSentence = TypingChallengeRepository.getRandomSentence(),
    val userInput: String = "",
    val startTimeMillis: Long = 0L,
    val elapsedMillis: Long = 0L,
    val currentWpm: Int = 0,
    val accuracy: Int = 100,
    val totalKeystrokes: Int = 0,
    val errorCount: Int = 0,
    val isCompleted: Boolean = false,
    val showResultDialog: Boolean = false,
    val lastRewardEarned: Int = 0,
    val selectedCategory: String = "All"
)

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowSuccessDialog(val title: String, val message: String, val coins: Int = 0) : UiEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WalletRepository

    init {
        val database = AppDatabase.getInstance(application)
        repository = WalletRepository(database.walletDao())
    }

    val walletState: StateFlow<WalletEntity> = repository.walletFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WalletEntity()
        )

    val transactionsState: StateFlow<List<CoinTransaction>> = repository.transactionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val withdrawalsState: StateFlow<List<WithdrawalRequest>> = repository.withdrawalsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Screen Tab
    private val _currentTab = MutableStateFlow("home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    // Event Flow for toasts / alerts
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    // ----------------------------------------------------
    // TYPING CHALLENGE ENGINE
    // ----------------------------------------------------
    private val _typingState = MutableStateFlow(TypingSessionState())
    val typingState: StateFlow<TypingSessionState> = _typingState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadNewSentence()
    }

    fun setCategory(category: String) {
        _typingState.value = _typingState.value.copy(selectedCategory = category)
        loadNewSentence()
    }

    fun loadNewSentence() {
        timerJob?.cancel()
        val sentence = TypingChallengeRepository.getRandomSentence(_typingState.value.selectedCategory)
        _typingState.value = _typingState.value.copy(
            currentSentence = sentence,
            userInput = "",
            startTimeMillis = 0L,
            elapsedMillis = 0L,
            currentWpm = 0,
            accuracy = 100,
            totalKeystrokes = 0,
            errorCount = 0,
            isCompleted = false,
            showResultDialog = false
        )
    }

    fun onUserType(newInput: String) {
        val current = _typingState.value
        if (current.isCompleted) return

        val target = current.currentSentence.text
        if (newInput.length > target.length + 10) return

        val now = System.currentTimeMillis()
        val startTime = if (current.startTimeMillis == 0L) {
            startTimer()
            now
        } else {
            current.startTimeMillis
        }

        // Keystroke metrics
        val isAdding = newInput.length > current.userInput.length
        val newKeystrokes = if (isAdding) current.totalKeystrokes + 1 else current.totalKeystrokes

        // Error detection
        var errors = 0
        for (i in newInput.indices) {
            if (i < target.length && newInput[i] != target[i]) {
                errors++
            } else if (i >= target.length) {
                errors++
            }
        }

        val elapsed = (now - startTime).coerceAtLeast(1)
        val wpm = TypingChallengeRepository.calculateWpm(newInput.length, elapsed)
        val accuracy = TypingChallengeRepository.calculateAccuracy(newKeystrokes, errors)

        val isTargetReached = newInput.trim() == target.trim()

        _typingState.value = current.copy(
            userInput = newInput,
            startTimeMillis = startTime,
            elapsedMillis = elapsed,
            totalKeystrokes = newKeystrokes,
            errorCount = errors,
            currentWpm = wpm,
            accuracy = accuracy,
            isCompleted = isTargetReached
        )

        if (isTargetReached) {
            timerJob?.cancel()
            completeTypingChallenge(target, wpm, accuracy)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(200)
                val current = _typingState.value
                if (current.startTimeMillis > 0L && !current.isCompleted) {
                    val elapsed = System.currentTimeMillis() - current.startTimeMillis
                    val wpm = TypingChallengeRepository.calculateWpm(current.userInput.length, elapsed)
                    _typingState.value = current.copy(
                        elapsedMillis = elapsed,
                        currentWpm = wpm
                    )
                } else if (current.isCompleted) {
                    break
                }
            }
        }
    }

    private fun completeTypingChallenge(sentence: String, wpm: Int, accuracy: Int) {
        viewModelScope.launch {
            val baseReward = _typingState.value.currentSentence.rewardCoins
            // Bonus for 100% accuracy or fast speed!
            val bonus = if (accuracy == 100) 2 else if (wpm >= 40) 2 else 0
            val totalReward = baseReward + bonus

            repository.addTypingReward(sentence, wpm, accuracy, totalReward)

            _typingState.value = _typingState.value.copy(
                isCompleted = true,
                showResultDialog = true,
                lastRewardEarned = totalReward
            )

            _eventFlow.emit(
                UiEvent.ShowSuccessDialog(
                    title = "Challenge Completed! 🎉",
                    message = "Great typing! You earned +$totalReward coins ($wpm WPM, $accuracy% accuracy).",
                    coins = totalReward
                )
            )
        }
    }

    fun dismissResultDialog() {
        _typingState.value = _typingState.value.copy(showResultDialog = false)
        loadNewSentence()
    }

    // ----------------------------------------------------
    // DAILY REWARD LOGIC
    // ----------------------------------------------------
    fun canClaimDailyReward(): Boolean {
        val wallet = walletState.value
        val todayEpochDay = System.currentTimeMillis() / 86400000L
        return wallet.lastDailyClaimDay != todayEpochDay
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val result = repository.claimDailyReward(rewardAmount = 20)
            result.onSuccess { (coins, streak) ->
                _eventFlow.emit(
                    UiEvent.ShowSuccessDialog(
                        title = "Daily Reward Claimed! 🎁",
                        message = "You received +$coins Coins! Current login streak: Day $streak 🔥",
                        coins = coins
                    )
                )
            }.onFailure { err ->
                _eventFlow.emit(UiEvent.ShowToast(err.message ?: "Failed to claim reward"))
            }
        }
    }

    // ----------------------------------------------------
    // REWARDED VIDEO ADS
    // ----------------------------------------------------
    val isAdLoaded: StateFlow<Boolean> = RewardedAdManager.isAdLoaded

    init {
        RewardedAdManager.initialize(application)
    }

    fun showRewardedAd(activity: Activity) {
        RewardedAdManager.showAd(
            activity = activity,
            onRewarded = {
                val coins = RewardedAdManager.AD_REWARD
                viewModelScope.launch {
                    repository.addAdReward(coins)
                    _eventFlow.emit(
                        UiEvent.ShowSuccessDialog(
                            title = "Ad Reward Received! \uD83D\uDCFA",
                            message = "+$coins coins added to your wallet! Keep watching to earn more.",
                            coins = coins
                        )
                    )
                }
            },
            onFailed = { msg ->
                viewModelScope.launch { _eventFlow.emit(UiEvent.ShowToast(msg)) }
            }
        )
    }

    // ----------------------------------------------------
    // WITHDRAWAL / REDEEM LOGIC
    // ----------------------------------------------------
    fun submitWithdrawal(
        method: String,
        accountDetail: String,
        coinAmount: Int
    ) {
        viewModelScope.launch {
            if (accountDetail.isBlank()) {
                _eventFlow.emit(UiEvent.ShowToast("Please enter your $method account details."))
                return@launch
            }

            val fiatStr = when (coinAmount) {
                100 -> "₹10.00 / $0.12"
                250 -> "₹25.00 / $0.30"
                500 -> "₹50.00 / $0.60"
                1000 -> "₹100.00 / $1.20"
                else -> "₹${coinAmount / 10}.00"
            }

            val result = repository.submitWithdrawal(
                method = method,
                accountDetail = accountDetail.trim(),
                coinAmount = coinAmount,
                fiatValue = fiatStr
            )

            result.onSuccess { req ->
                _eventFlow.emit(
                    UiEvent.ShowSuccessDialog(
                        title = "Withdrawal Submitted! 💰",
                        message = "Your request for $fiatStr via ${req.method} (${req.accountDetail}) is pending review. Ref: ${req.referenceId}."
                    )
                )
            }.onFailure { err ->
                _eventFlow.emit(UiEvent.ShowToast(err.message ?: "Withdrawal failed"))
            }
        }
    }
}
