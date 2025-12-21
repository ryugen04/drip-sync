package com.dripsync.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// wearモジュールと同じカラーパレット
private val CyanBright = Color(0xFF00E5FF)
private val CyanMid = Color(0xFF00B8D4)
private val BluePurple = Color(0xFF7C4DFF)
private val RingBackground = Color(0xFF2A3545)

/**
 * 円形プログレスインジケーター
 *
 * @param progress 進捗率（0.0f ~ 1.0f）
 * @param currentValue 現在の値（表示用）
 * @param targetValue 目標値（表示用）
 * @param size コンポーネントのサイズ
 * @param strokeWidth リングの太さ
 */
@Composable
fun CircularProgress(
    progress: Float,
    currentValue: String,
    targetValue: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 16.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )

    val sweepAngle = animatedProgress * 360f

    // グラデーションブラシ
    val gradientBrush = Brush.sweepGradient(
        0f to CyanBright,
        0.25f to CyanMid,
        0.5f to BluePurple,
        0.75f to BluePurple,
        1f to CyanBright
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()

            // 背景リング
            drawArc(
                color = RingBackground,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // プログレスリング（グラデーション）
            drawArc(
                brush = gradientBrush,
                startAngle = -90f, // 12時位置から開始
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        // 中央のテキスト
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentValue,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "/ $targetValue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
