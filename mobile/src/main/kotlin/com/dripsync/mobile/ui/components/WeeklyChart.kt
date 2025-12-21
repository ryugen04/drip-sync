package com.dripsync.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// カラーパレット
private val CyanBright = Color(0xFF00E5FF)
private val BluePurple = Color(0xFF7C4DFF)
private val BarBackground = Color(0xFF2A3545)

data class DailyChartData(
    val date: LocalDate,
    val amountMl: Int,
    val goalMl: Int
) {
    val progress: Float
        get() = if (goalMl > 0) (amountMl.toFloat() / goalMl).coerceAtMost(1f) else 0f

    val dayLabel: String
        get() = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    val isToday: Boolean
        get() = date == LocalDate.now()
}

/**
 * 週間の棒グラフ
 *
 * @param data 7日分のデータ
 * @param maxHeight 棒グラフの最大高さ
 * @param barWidth 棒の幅
 */
@Composable
fun WeeklyChart(
    data: List<DailyChartData>,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 120.dp,
    barWidth: Dp = 32.dp
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(CyanBright, BluePurple)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { dayData ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 棒グラフ
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height(maxHeight)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(BarBackground),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(maxHeight * dayData.progress)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(gradientBrush)
                    )
                }

                // 曜日ラベル
                Text(
                    text = dayData.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dayData.isToday) {
                        CyanBright
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
