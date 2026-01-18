package com.dripsync.mobile.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dripsync.shared.domain.model.HourlyHydrationPoint
import java.time.LocalDate
import java.time.LocalTime

// カラーパレット
private val CyanBright = Color(0xFF06B6D4)
private val PurpleBright = Color(0xFF8B5CF6)
private val OrangeBright = Color(0xFFF97316)
private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val GrayText = Color(0xFF64748B)
private val IdealLineColor = Color(0xFF4A5568)

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
            Spacer(modifier = Modifier.height(16.dp))

            // タブ切り替え
            TabSelector(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.selectedTab) {
                StatsTab.DAILY -> DailyContent(
                    uiState = uiState,
                    modifier = Modifier.weight(1f)
                )
                StatsTab.WEEKLY -> WeeklyContent(
                    uiState = uiState,
                    daysCompleted = daysCompleted,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TabSelector(
    selectedTab: StatsTab,
    onTabSelected: (StatsTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TabItem(
            text = "今日",
            isSelected = selectedTab == StatsTab.DAILY,
            onClick = { onTabSelected(StatsTab.DAILY) },
            modifier = Modifier.weight(1f)
        )
        TabItem(
            text = "今週",
            isSelected = selectedTab == StatsTab.WEEKLY,
            onClick = { onTabSelected(StatsTab.WEEKLY) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CyanBright else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) Color.Black else GrayText,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DailyContent(
    uiState: StatsUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 今日の進捗
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${uiState.todayTotal}",
                    fontSize = 48.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " / ${uiState.dailyGoalMl}ml",
                    fontSize = 18.sp,
                    color = GrayText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (uiState.isGoalAchieved) "目標達成" else "残り ${uiState.dailyGoalMl - uiState.todayTotal}ml",
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.isGoalAchieved) CyanBright else GrayText
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 累積グラフ
        Text(
            text = "時間別推移",
            style = MaterialTheme.typography.labelLarge,
            color = GrayText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        key(
            uiState.hourlyData.size,
            uiState.hourlyData.lastOrNull()?.actualCumulative ?: 0,
            uiState.dailyGoalMl
        ) {
            DailyCumulativeChart(
                data = uiState.hourlyData,
                goalMl = uiState.dailyGoalMl,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // 凡例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp, 3.dp)
                    .background(CyanBright, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "実績",
                style = MaterialTheme.typography.labelSmall,
                color = GrayText
            )
            Spacer(modifier = Modifier.width(24.dp))
            Box(
                modifier = Modifier
                    .size(12.dp, 3.dp)
                    .background(IdealLineColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "理想",
                style = MaterialTheme.typography.labelSmall,
                color = GrayText
            )
        }
    }
}

@Composable
private fun WeeklyContent(
    uiState: StatsUiState,
    daysCompleted: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // メイン: 連続達成日数
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

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(32.dp))

        // 週間グラフ
        key(uiState.weeklyData.hashCode()) {
            WeeklyBarChart(
                data = uiState.weeklyData,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun DailyCumulativeChart(
    data: List<HourlyHydrationPoint>,
    goalMl: Int,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = "データがありません", color = GrayText)
        }
        return
    }

    // データが変更されたときに現在時刻を再取得
    val now = remember(data) { LocalTime.now() }

    // maxValueを動的に計算（目標値と実績の最大値の大きい方）
    val maxActualValue = data.maxOfOrNull { it.actualCumulative } ?: 0
    val maxDisplayValue = maxOf(goalMl, maxActualValue)

    Row(modifier = modifier) {
        // Y軸ラベル
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${maxDisplayValue / 1000f}L",
                style = MaterialTheme.typography.labelSmall,
                color = GrayText
            )
            Text(
                text = "${maxDisplayValue / 2000f}L",
                style = MaterialTheme.typography.labelSmall,
                color = GrayText
            )
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmall,
                color = GrayText
            )
        }

        // グラフエリア
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val chartHeight = size.height
                val chartWidth = size.width
                val maxValue = maxDisplayValue.toFloat()

                // 現在時刻までのインデックスを計算
                val currentPointIndex = data.indexOfLast { !it.time.isAfter(now) }
                    .coerceAtLeast(0)

                // 理想カーブ（破線）を描画
                val idealPoints = data.mapIndexed { index, point ->
                    val x = if (data.size > 1) {
                        index * (chartWidth / (data.size - 1))
                    } else {
                        chartWidth / 2
                    }
                    val y = chartHeight - (point.idealCumulative.toFloat() / maxValue * chartHeight)
                        .coerceIn(0f, chartHeight)
                    Offset(x, y)
                }

                if (idealPoints.size >= 2) {
                    val idealPath = Path().apply {
                        moveTo(idealPoints.first().x, idealPoints.first().y)
                        for (i in 1 until idealPoints.size) {
                            lineTo(idealPoints[i].x, idealPoints[i].y)
                        }
                    }
                    drawPath(
                        path = idealPath,
                        color = IdealLineColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    )
                }

                // 実績カーブ（現在時刻まで）を描画
                val actualPoints = data.take(currentPointIndex + 1).mapIndexed { index, point ->
                    val x = if (data.size > 1) {
                        index * (chartWidth / (data.size - 1))
                    } else {
                        chartWidth / 2
                    }
                    val y = chartHeight - (point.actualCumulative.toFloat() / maxValue * chartHeight)
                        .coerceIn(0f, chartHeight)
                    Offset(x, y)
                }

                // グラデーション塗りつぶし
                if (actualPoints.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(actualPoints.first().x, chartHeight)
                        actualPoints.forEach { lineTo(it.x, it.y) }
                        lineTo(actualPoints.last().x, chartHeight)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CyanBright.copy(alpha = 0.3f),
                                PurpleBright.copy(alpha = 0.1f)
                            )
                        )
                    )

                    // 実績線
                    val linePath = Path().apply {
                        moveTo(actualPoints.first().x, actualPoints.first().y)
                        for (i in 1 until actualPoints.size) {
                            lineTo(actualPoints[i].x, actualPoints[i].y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(PurpleBright, CyanBright)
                        ),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                // 現在位置のポイント
                if (actualPoints.isNotEmpty()) {
                    val currentPoint = actualPoints.last()
                    drawCircle(
                        color = CyanBright,
                        radius = 6.dp.toPx(),
                        center = currentPoint
                    )
                    drawCircle(
                        color = DarkBackground,
                        radius = 3.dp.toPx(),
                        center = currentPoint
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 時刻ラベル
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(6, 9, 12, 15, 18, 21).forEach { hour ->
                    Text(
                        text = "$hour",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayText
                    )
                }
            }
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
                goalMl = 1500
            )
        }
    } else {
        data
    }

    val maxAmount = displayData.maxOfOrNull { maxOf(it.amountMl, it.goalMl) }?.coerceAtLeast(1) ?: 1500
    val goalMl = displayData.firstOrNull()?.goalMl ?: 1500
    val goalLineRatio = (goalMl.toFloat() / maxAmount).coerceIn(0f, 1f)

    // 選択されたバーのインデックス（-1は選択なし）
    var selectedBarIndex by remember { mutableIntStateOf(-1) }

    BoxWithConstraints(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // グラフエリア（バー + 目標線）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // 目標値の点線
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val lineY = size.height * (1f - goalLineRatio)
                    drawLine(
                        color = CyanBright.copy(alpha = 0.6f),
                        start = Offset(0f, lineY),
                        end = Offset(size.width, lineY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }

                // バーを描画
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    displayData.forEachIndexed { index, dayStats ->
                        val barHeightRatio = (dayStats.amountMl.toFloat() / maxAmount).coerceIn(0.02f, 1f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            selectedBarIndex = if (selectedBarIndex == index) -1 else index
                                        }
                                    )
                                },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .fillMaxHeight(barHeightRatio)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        if (dayStats.isCompleted) {
                                            Brush.verticalGradient(listOf(CyanBright, PurpleBright))
                                        } else if (dayStats.amountMl > 0) {
                                            Brush.verticalGradient(listOf(CardBackground, CardBackground))
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(
                                                    CardBackground.copy(alpha = 0.5f),
                                                    CardBackground.copy(alpha = 0.5f)
                                                )
                                            )
                                        }
                                    )
                            )

                            // 選択時のツールチップ
                            if (selectedBarIndex == index) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = (-8).dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${dayStats.amountMl}ml",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 日付ラベル
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayData.forEachIndexed { index, dayStats ->
                    val isToday = dayStats.date == LocalDate.now()
                    Text(
                        text = dayStats.dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) CyanBright else GrayText,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f).wrapContentSize(Alignment.Center)
                    )
                }
            }
        }
    }
}
