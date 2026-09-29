package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.components.CelebrationDialog
import com.example.ui.components.CoinBadge
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TypingRewardsApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingRewardsApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val wallet by viewModel.walletState.collectAsStateWithLifecycle()
    val transactions by viewModel.transactionsState.collectAsStateWithLifecycle()
    val withdrawals by viewModel.withdrawalsState.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val typingState by viewModel.typingState.collectAsStateWithLifecycle()

    // Success / Celebration Dialog
    var celebrationDialogData by remember { mutableStateOf<Pair<String, String>?>(null) }
    var celebrationCoins by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.ShowSuccessDialog -> {
                    celebrationCoins = event.coins
                    celebrationDialogData = Pair(event.title, event.message)
                }
            }
        }
    }

    // Handle back button when not on Home tab
    if (currentTab != "home") {
        BackHandler {
            viewModel.selectTab("home")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Typing Rewards",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                    }
                },
                actions = {
                    CoinBadge(
                        amount = wallet.coins,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { viewModel.selectTab("wallet") }
                            .testTag("appbar_coin_badge")
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { viewModel.selectTab("home") },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == "typing",
                    onClick = { viewModel.selectTab("typing") },
                    icon = { Icon(Icons.Default.Keyboard, contentDescription = "Typing") },
                    label = { Text("Typing") },
                    modifier = Modifier.testTag("nav_typing")
                )
                NavigationBarItem(
                    selected = currentTab == "earn",
                    onClick = { viewModel.selectTab("earn") },
                    icon = { Icon(Icons.Default.CardGiftcard, contentDescription = "Earn") },
                    label = { Text("Earn") },
                    modifier = Modifier.testTag("nav_earn")
                )
                NavigationBarItem(
                    selected = currentTab == "wallet",
                    onClick = { viewModel.selectTab("wallet") },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Redeem") },
                    label = { Text("Redeem") },
                    modifier = Modifier.testTag("nav_wallet")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "home" -> HomeScreen(
                    wallet = wallet,
                    recentTransactions = transactions,
                    canClaimDaily = viewModel.canClaimDailyReward(),
                    onStartTyping = { viewModel.selectTab("typing") },
                    onClaimDaily = { viewModel.claimDailyReward() },
                    onWatchAd = { (context as? Activity)?.let { viewModel.showRewardedAd(it) } },
                    onWithdraw = { viewModel.selectTab("wallet") },
                    onViewAllHistory = { viewModel.selectTab("wallet") }
                )
                "typing" -> TypingChallengeScreen(
                    state = typingState,
                    onType = { viewModel.onUserType(it) },
                    onSkipSentence = { viewModel.loadNewSentence() },
                    onSelectCategory = { viewModel.setCategory(it) }
                )
                "earn" -> EarnScreen(
                    wallet = wallet,
                    canClaimDaily = viewModel.canClaimDailyReward(),
                    onClaimDaily = { viewModel.claimDailyReward() },
                    onWatchAd = { (context as? Activity)?.let { viewModel.showRewardedAd(it) } }
                )
                "wallet" -> RedeemScreen(
                    wallet = wallet,
                    withdrawals = withdrawals,
                    transactions = transactions,
                    onSubmitWithdrawal = { method, detail, amount ->
                        viewModel.submitWithdrawal(method, detail, amount)
                    }
                )
            }
        }
    }

    // Celebration / Success Alert Dialog
    celebrationDialogData?.let { (title, msg) ->
        CelebrationDialog(
            title = title,
            message = msg,
            coinsEarned = celebrationCoins,
            onDismiss = {
                celebrationDialogData = null
                celebrationCoins = 0
                if (typingState.isCompleted) {
                    viewModel.dismissResultDialog()
                }
            }
        )
    }
}
