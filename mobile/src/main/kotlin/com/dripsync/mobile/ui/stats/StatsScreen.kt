package com.dripsync.mobile.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

// カラーパレット
private val CyanBright = Color(0xFF06B6D4)
private val PurpleBright = Color(0xFF8B5CF6)
private val OrangeBright = Color(0xFFF97316)
private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val GrayText = Color(0xFF64748B)

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val daysCompleted = uiState.weeklyData.count { it.isCompleted }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // メイン: 連続達成日数（大きく中央に）
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = OrangeBright,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${uiState.currentStreak}",
                        fontSize = 72.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "日",
                        fontSize = 24.sp,
                        color = GrayText,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )
                }
                Text(
                    text = "連続達成",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GrayText
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // サブ指標: 達成日数 + 1日平均
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 達成日数
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$daysCompleted",
                            fontSize = 32.sp,
                            color = CyanBright,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/7",
                            fontSize = 16.sp,
                            color = GrayText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "達成日数",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayText
                    )
                }

                // 区切り線
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(GrayText.copy(alpha = 0.3f))
                )

                // 1日平均
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${uiState.weeklyAverageMl}",
                            fontSize = 32.sp,
                            color = PurpleBright,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ml",
                            fontSize = 14.sp,
                            color = GrayText,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1日平均",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 週間グラフ（残りのスペースを使用）
            Text(
                text = "今週",
                style = MaterialTheme.typography.labelLarge,
                color = GrayText,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            WeeklyBarChart(
                data = uiState.weeklyData,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WeeklyBarChart(
    data: List<DayStats>,
    modifier: Modifier = Modifier
) {
    val displayData = if (data.isEmpty()) {
        val today = LocalDate.now()
        (0..6).map { index ->
            DayStats(
                date = today.minusDays(6 - index.toLong()),
                amountMl = 0,
                goalMl = 2000
            )
        }
    } else {
        data
    }

    val maxAmount = displayData.maxOfOrNull { maxOf(it.amountMl, it.goalMl) }?.coerceAtLeast(1) ?: 2000

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        displayData.forEach { dayStats ->
            val isToday = dayStats.date == LocalDate.now()
            val barHeightRatio = (dayStats.amountMl.toFloat() / maxAmount).coerceIn(0.02f, 1f)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // バー
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .fillMaxSize(barHeightRatio)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                if (dayStats.isCompleted) {
                                    Brush.verticalGradient(listOf(CyanBright, PurpleBright))
                                } else if (dayStats.amountMl > 0) {
                                    Brush.verticalGradient(listOf(CardBackground, CardBackground))
                                } else {
                                    Brush.verticalGradient(listOf(CardBackground.copy(alpha = 0.5f), CardBackground.copy(alpha = 0.5f)))
                                }
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 日付
                Text(
                    text = dayStats.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) CyanBright else GrayText,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp
                )
            }
        }
    }
}
