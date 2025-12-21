package com.dripsync.wear.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import java.time.format.DateTimeFormatter

// カラーパレット（HomeScreenと共通）
private val CyanBright = Color(0xFF00E5FF)
private val BluePurple = Color(0xFF7C4DFF)
private val BackgroundDark = Color(0xFF0D1520)
private val TextGray = Color(0xFF5A6678)
private val GoalLineColor = Color(0xFF4A5568)

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 画面表示時に最新データを取得
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
            text = "Stats",
            color = CyanBright,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(2.dp))

        // サブタイトル
        Text(
            text = "Last 7 days",
            color = TextGray,
            fontSize = 9.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 線グラフ
        LineChart(
            data = uiState.dailyData,
            goalMl = uiState.dailyGoalMl,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun LineChart(
    data: List<DailyData>,
    goalMl: Int,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = "No data", color = TextGray, fontSize = 10.sp)
        }
        return
    }

    val scrollState = rememberScrollState()
    val dateFormatter = DateTimeFormatter.ofPattern("d")

    // 初期位置を右端（最新）に設定
    LaunchedEffect(data.size) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val pointSpacing = 36.dp
    val chartWidth = pointSpacing * (data.size - 1).coerceAtLeast(1)

    Row(modifier = modifier) {
        // Y軸ラベル
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${goalMl * 2 / 1000}L",
                color = TextGray,
                fontSize = 7.sp
            )
            Text(
                text = "${goalMl / 1000}L",
                color = GoalLineColor,
                fontSize = 7.sp
            )
            Text(
                text = "0",
                color = TextGray,
                fontSize = 7.sp
            )
        }

        // グラフ + 日付ラベル（一緒にスクロール）
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier.width(chartWidth)
            ) {
                // グラフエリア
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val maxValue = (goalMl * 2).toFloat()
                    val chartHeight = size.height
                    val chartWidthPx = size.width

                    // 目標ライン（破線）
                    val goalY = chartHeight - (goalMl.toFloat() / maxValue * chartHeight)
                    drawLine(
                        color = GoalLineColor,
                        start = Offset(0f, goalY),
                        end = Offset(chartWidthPx, goalY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )

                    // データポイントを計算
                    val points = data.mapIndexed { index, dayData ->
                        val x = if (data.size > 1) {
                            index * (chartWidthPx / (data.size - 1))
                        } else {
                            chartWidthPx / 2
                        }
                        val y = chartHeight - (dayData.amountMl.toFloat() / maxValue * chartHeight)
                            .coerceIn(0f, chartHeight)
                        Offset(x, y)
                    }

                    // グラデーション塗りつぶし
                    if (points.size >= 2) {
                        val fillPath = Path().apply {
                            moveTo(points.first().x, chartHeight)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, chartHeight)
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
                    }

                    // 線を描画
                    if (points.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
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

                    // ポイントを描画
                    points.forEach { point ->
                        drawCircle(
                            color = CyanBright,
                            radius = 4.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = BackgroundDark,
                            radius = 2.dp.toPx(),
                            center = point
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 日付ラベル
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    data.forEach { dayData ->
                        Text(
                            text = dayData.date.format(dateFormatter),
                            color = TextGray,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
