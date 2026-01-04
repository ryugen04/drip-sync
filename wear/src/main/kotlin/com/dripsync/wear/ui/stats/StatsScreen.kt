package com.dripsync.wear.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.wear.compose.material.Text
import com.dripsync.shared.domain.model.HourlyHydrationPoint
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// カラーパレット（HomeScreenと共通）
private val CyanBright = Color(0xFF00E5FF)
private val BluePurple = Color(0xFF7C4DFF)
private val BackgroundDark = Color(0xFF0D1520)
private val TextGray = Color(0xFF5A6678)
private val IdealLineColor = Color(0xFF4A5568)

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // タイトル
        Text(
            text = "Today",
            color = CyanBright,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(2.dp))

        // サブタイトル（現在の累計）
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${uiState.todayTotalMl}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " / ${uiState.dailyGoalMl}ml",
                color = TextGray,
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 累積グラフ
        DailyCumulativeChart(
            data = uiState.hourlyData,
            goalMl = uiState.dailyGoalMl,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))
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
            Text(text = "No data", color = TextGray, fontSize = 10.sp)
        }
        return
    }

    val timeFormatter = DateTimeFormatter.ofPattern("H")
    val now = LocalTime.now()

    Row(modifier = modifier) {
        // Y軸ラベル
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${goalMl / 1000f}L",
                color = TextGray,
                fontSize = 7.sp
            )
            Text(
                text = "${goalMl / 2000f}L",
                color = TextGray,
                fontSize = 7.sp
            )
            Text(
                text = "0",
                color = TextGray,
                fontSize = 7.sp
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
                val maxValue = goalMl.toFloat()

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
                            width = 1.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    )
                }

                // 実績カーブ（現在時刻まで）を描画
                val actualPoints = data.mapIndexedNotNull { index, point ->
                    if (index > currentPointIndex) {
                        return@mapIndexedNotNull null
                    }
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
                                BluePurple.copy(alpha = 0.1f)
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
                            colors = listOf(BluePurple, CyanBright)
                        ),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                // 現在位置のポイント
                if (actualPoints.isNotEmpty()) {
                    val currentPoint = actualPoints.last()
                    drawCircle(
                        color = CyanBright,
                        radius = 4.dp.toPx(),
                        center = currentPoint
                    )
                    drawCircle(
                        color = BackgroundDark,
                        radius = 2.dp.toPx(),
                        center = currentPoint
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 時刻ラベル
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 6時、12時、18時、22時のラベルを表示
                listOf(6, 12, 18, 22).forEach { hour ->
                    Text(
                        text = hour.toString(),
                        color = TextGray,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}
