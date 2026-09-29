package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoinTransaction
import com.example.data.model.WalletEntity
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.CoinBadge
import com.example.ui.components.TransactionRow
import com.example.ui.components.WithdrawalRow
import com.example.ui.theme.*

@Composable
fun RedeemScreen(
    wallet: WalletEntity,
    withdrawals: List<WithdrawalRequest>,
    transactions: List<CoinTransaction>,
    onSubmitWithdrawal: (method: String, accountDetail: String, coinAmount: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMethod by remember { mutableStateOf("UPI") }
    var accountDetail by remember { mutableStateOf("") }
    var selectedAmount by remember { mutableIntStateOf(100) }
    var activeTab by remember { mutableStateOf("redeem") } // "redeem", "history", "transactions"

    val minRequired = 100
    val hasEnoughCoins = wallet.coins >= minRequired

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "💰 Redeem & Withdrawal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Convert your earned coins into cash or digital gift vouchers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Balance & Threshold Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasEnoughCoins) EmeraldContainer.copy(alpha = 0.5f) else GoldContainer.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (hasEnoughCoins) EmeraldSuccess else GoldAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (hasEnoughCoins) EmeraldSuccess else OnGoldContainer
                            )
                            Text(
                                text = "${wallet.coins} 🪙",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasEnoughCoins) EmeraldSuccess else GoldAccent
                        ) {
                            Text(
                                text = if (hasEnoughCoins) "Eligible to Withdraw" else "Need ${minRequired - wallet.coins} more 🪙",
                                color = if (hasEnoughCoins) Color.White else Slate900,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val progress = (wallet.coins.toFloat() / minRequired.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (hasEnoughCoins) EmeraldSuccess else GoldAccent,
                        trackColor = Color.White.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Minimum withdrawal threshold is 100 coins (₹10.00).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Tab Selector: Redeem Form vs Withdrawal History vs Transaction Ledger
        item {
            TabRow(
                selectedTabIndex = when (activeTab) {
                    "redeem" -> 0
                    "history" -> 1
                    else -> 2
                },
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = activeTab == "redeem",
                    onClick = { activeTab = "redeem" },
                    text = { Text("Redeem Form", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == "history",
                    onClick = { activeTab = "history" },
                    text = { Text("Requests (${withdrawals.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == "transactions",
                    onClick = { activeTab = "transactions" },
                    text = { Text("All Ledger", fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (activeTab == "redeem") {
            // Method Selection (UPI, Amazon Gift Card, Redeem Code)
            item {
                Text(
                    text = "1. Select Payment Method",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MethodCard(
                        title = "UPI",
                        subtitle = "Instant Bank",
                        icon = Icons.Default.AccountBalance,
                        isSelected = selectedMethod == "UPI",
                        onClick = {
                            selectedMethod = "UPI"
                            accountDetail = ""
                        },
                        modifier = Modifier.weight(1f)
                    )

                    MethodCard(
                        title = "Amazon",
                        subtitle = "Gift Voucher",
                        icon = Icons.Default.ShoppingBag,
                        isSelected = selectedMethod == "Amazon Gift Card",
                        onClick = {
                            selectedMethod = "Amazon Gift Card"
                            accountDetail = ""
                        },
                        modifier = Modifier.weight(1f)
                    )

                    MethodCard(
                        title = "Play Code",
                        subtitle = "Redeem Code",
                        icon = Icons.Default.SportsEsports,
                        isSelected = selectedMethod == "Redeem Code",
                        onClick = {
                            selectedMethod = "Redeem Code"
                            accountDetail = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Coin Amount Tiers
            item {
                Text(
                    text = "2. Select Coin Tier",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(100 to "₹10", 250 to "₹25", 500 to "₹50", 1000 to "₹100").forEach { (tierCoins, fiat) ->
                        val isSelected = selectedAmount == tierCoins
                        val canAfford = wallet.coins >= tierCoins

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedAmount = tierCoins },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) IndigoContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$tierCoins 🪙",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (canAfford) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = fiat,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Account Details Input
            item {
                Text(
                    text = "3. Enter Payout Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                val hint = when (selectedMethod) {
                    "UPI" -> "Enter UPI ID (e.g. mobile@upi or name@okaxis)"
                    "Amazon Gift Card" -> "Enter email address for gift voucher delivery"
                    else -> "Enter mobile number or email for redeem code"
                }

                OutlinedTextField(
                    value = accountDetail,
                    onValueChange = { accountDetail = it },
                    label = { Text(selectedMethod) },
                    placeholder = { Text(hint) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_details_input"),
                    leadingIcon = {
                        Icon(
                            imageVector = when (selectedMethod) {
                                "UPI" -> Icons.Default.AlternateEmail
                                "Amazon Gift Card" -> Icons.Default.Email
                                else -> Icons.Default.ConfirmationNumber
                            },
                            contentDescription = null
                        )
                    },
                    singleLine = true
                )
            }

            // Submit Button
            item {
                val canSubmit = wallet.coins >= selectedAmount && accountDetail.isNotBlank()

                Button(
                    onClick = {
                        onSubmitWithdrawal(selectedMethod, accountDetail, selectedAmount)
                        accountDetail = ""
                    },
                    enabled = canSubmit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_withdrawal_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (wallet.coins < selectedAmount) "Insufficient Coins (${wallet.coins}/$selectedAmount 🪙)"
                        else "Submit $selectedMethod Redemption",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚡ Requests are reviewed and verified within 24-48 business hours.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (activeTab == "history") {
            // Withdrawal History List
            if (withdrawals.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📬", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Withdrawal Requests Yet",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Earn at least 100 coins and submit your first payout request!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(withdrawals) { req ->
                    WithdrawalRow(request = req)
                }
            }
        } else {
            // Complete Transaction Ledger
            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "No coin transactions logged yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(transactions) { tx ->
                    TransactionRow(tx = tx)
                }
            }
        }
    }
}

@Composable
fun MethodCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) IndigoContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
