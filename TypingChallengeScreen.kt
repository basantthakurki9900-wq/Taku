package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CoinBadge
import com.example.ui.theme.*

@Composable
fun TypingChallengeScreen(
    state: TypingSessionState,
    onType: (String) -> Unit,
    onSkipSentence: () -> Unit,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.currentSentence.id) {
        // Request focus when a new sentence is loaded
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "⌨️ Typing Challenge",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Type exactly to earn coins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CoinBadge(amount = state.currentSentence.rewardCoins, large = true)
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Easy", "Medium", "Hard").forEach { category ->
                val selected = state.selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("chip_$category")
                )
            }
        }

        // Live Performance Metrics HUD
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricItem(
                    label = "SPEED",
                    value = "${state.currentWpm}",
                    unit = "WPM",
                    color = IndigoPrimary
                )
                VerticalDivider(modifier = Modifier.height(36.dp))
                MetricItem(
                    label = "ACCURACY",
                    value = "${state.accuracy}%",
                    unit = if (state.errorCount > 0) "${state.errorCount} err" else "Clean",
                    color = if (state.accuracy >= 90) EmeraldSuccess else RoseError
                )
                VerticalDivider(modifier = Modifier.height(36.dp))
                val seconds = (state.elapsedMillis / 1000).toInt()
                MetricItem(
                    label = "TIME",
                    value = "${seconds}s",
                    unit = "Elapsed",
                    color = GoldDark
                )
            }
        }

        // Sentence Target Card with Real-time Character Highlighting
        val targetText = state.currentSentence.text
        val input = state.userInput

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TARGET SENTENCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${state.currentSentence.category} • ${targetText.length} chars",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Build styled annotated string
                val annotatedString = buildAnnotatedString {
                    for (i in targetText.indices) {
                        if (i < input.length) {
                            if (input[i] == targetText[i]) {
                                // Correct character: Emerald green
                                withStyle(
                                    SpanStyle(
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(targetText[i])
                                }
                            } else {
                                // Error character: Red with strike/highlight
                                withStyle(
                                    SpanStyle(
                                        color = RoseError,
                                        background = RoseContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(targetText[i])
                                }
                            }
                        } else if (i == input.length) {
                            // Current cursor character: Highlighted
                            withStyle(
                                SpanStyle(
                                    color = IndigoPrimary,
                                    background = IndigoContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(targetText[i])
                            }
                        } else {
                            // Remaining text
                            withStyle(
                                SpanStyle(
                                    color = Slate700,
                                    fontWeight = FontWeight.Normal
                                )
                            ) {
                                append(targetText[i])
                            }
                        }
                    }
                }

                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 32.sp
                    )
                )

                // Extra characters typed beyond length
                if (input.length > targetText.length) {
                    val extra = input.substring(targetText.length)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Extra input: \"$extra\"",
                        color = RoseError,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Typing Input Field
        OutlinedTextField(
            value = state.userInput,
            onValueChange = onType,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .testTag("typing_input_field"),
            placeholder = { Text("Type here...") },
            label = { Text("Your Typing Input") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IndigoPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            trailingIcon = {
                if (state.userInput.isNotEmpty()) {
                    IconButton(onClick = { onType("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Input"
                        )
                    }
                }
            },
            singleLine = false,
            maxLines = 3
        )

        // Quick Input Assistance / Virtual Helpers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSkipSentence,
                colors = ButtonDefaults.outlinedButtonColors(),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("skip_sentence_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Sentence")
            }

            Button(
                onClick = {
                    // Quick-fill one next correct word to assist user
                    val target = state.currentSentence.text
                    val curr = state.userInput
                    if (curr.length < target.length) {
                        val nextSpace = target.indexOf(' ', curr.length)
                        val nextTarget = if (nextSpace != -1) target.substring(0, nextSpace + 1) else target
                        onType(nextTarget)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier
                    .weight(1f)
                    .testTag("assist_word_button")
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Quick Word")
            }
        }

        // Instructions Tip Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "💡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Tip: Type the sentence with 100% accuracy or speed over 40 WPM to earn bonus coins!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
