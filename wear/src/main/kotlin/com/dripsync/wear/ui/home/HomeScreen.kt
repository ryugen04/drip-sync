package com.dripsync.wear.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.min
import kotlin.math.sqrt
import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.wear.widget.ConfirmationOverlay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Text
import com.dripsync.wear.R

// カラーパレット
private val CyanBright = Color(0xFF00E5FF)
private val CyanMid = Color(0xFF00B8D4)
private val BluePurple = Color(0xFF7C4DFF)
private val BackgroundDark = Color(0xFF0D1520)
private val ButtonBackground = Color(0xFF1A2535)
private val TextGray = Color(0xFF5A6678)
private val TextLightGray = Color(0xFF8A9AAA)
private val RingBackground = Color(0xFF2A3545)

// 外周リングの設定
private val RING_STROKE_WIDTH = 12.dp
private val RING_PADDING = 4.dp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // フォアグラウンド復帰時にMobileと同期
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.syncWithMobile()
    }

    // 記録イベントを監視してConfirmationOverlayを表示
    LaunchedEffect(Unit) {
        viewModel.recordEvent.collect { event ->
            val activity = context as? Activity ?: return@collect
            val (type, message) = when (event) {
                is RecordEvent.Success -> {
                    ConfirmationOverlay.SUCCESS_ANIMATION to "+${event.amountMl}ml"
                }
                is RecordEvent.Failure -> {
                    ConfirmationOverlay.FAILURE_ANIMATION to "Failed"
                }
            }
            ConfirmationOverlay()
                .setType(type)
                .setMessage(message)
                .showOn(activity)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current

        // 画面サイズから内側の使用可能領域を計算
        val screenSize = with(density) { min(maxWidth.toPx(), maxHeight.toPx()) }
        val ringTotal = with(density) { (RING_STROKE_WIDTH + RING_PADDING).toPx() * 2 }
        val innerDiameter = screenSize - ringTotal
        // 円に内接する正方形のサイズ（円の直径 / √2）
        val innerSquareSize = (innerDiameter / sqrt(2f))
        val innerPadding = with(density) { ((screenSize - innerSquareSize) / 2).toDp() }

        // 円形プログレスインジケーター（12時から時計回り）
        CircularProgressArc(
            progress = uiState.progressPercent / 100f,
            modifier = Modifier.fillMaxSize()
        )

        // メインコンテンツ（リングの内側の円に内接する正方形内に収める）
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ロゴとタイトル（灰色・小さめ）
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_dripsync_logo),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    colorFilter = ColorFilter.tint(CyanBright)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "DRIPSYNC",
                    color = TextGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }

            // 摂取量表示
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.W300
                        )
                    ) {
                        append("${uiState.todayTotalMl}")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = TextLightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W300
                        )
                    ) {
                        append("/${uiState.dailyGoalMl}ml")
                    }
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )

            // プリセットボタン
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PresetButton(
                    amountMl = uiState.presets.preset1Ml,
                    dailyGoalMl = uiState.dailyGoalMl,
                    iconRes = R.drawable.ic_coffee,
                    onClick = { viewModel.recordHydration(uiState.presets.preset1Ml) }
                )
                PresetButton(
                    amountMl = uiState.presets.preset2Ml,
                    dailyGoalMl = uiState.dailyGoalMl,
                    iconRes = R.drawable.ic_glass,
                    onClick = { viewModel.recordHydration(uiState.presets.preset2Ml) }
                )
                PresetButton(
                    amountMl = uiState.presets.preset3Ml,
                    dailyGoalMl = uiState.dailyGoalMl,
                    iconRes = R.drawable.ic_bottle,
                    onClick = { viewModel.recordHydration(uiState.presets.preset3Ml) }
                )
            }
        }
    }
}

@Composable
private fun CircularProgressArc(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(RING_PADDING)) {
        val strokeWidth = RING_STROKE_WIDTH.toPx()
        val diameter = minOf(size.width, size.height) - strokeWidth
        val topLeft = Offset(
            (size.width - diameter) / 2,
            (size.height - diameter) / 2
        )
        val arcSize = Size(diameter, diameter)

        // 背景リング（全周）
        drawArc(
            color = RingBackground,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // プログレス円弧（12時 = -90°から時計回り）
        val progressSweep = 360f * progress.coerceIn(0f, 1f)
        if (progressSweep > 0) {
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to CyanBright,
                    0.25f to CyanMid,
                    0.5f to BluePurple,
                    0.75f to BluePurple,
                    1.0f to CyanBright,
                    center = Offset(size.width / 2, size.height / 2)
                ),
                startAngle = -90f,
                sweepAngle = progressSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun PresetButton(
    amountMl: Int,
    dailyGoalMl: Int,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    val presetProgress = if (dailyGoalMl > 0) {
        (amountMl.toFloat() / dailyGoalMl).coerceIn(0f, 1f)
    } else 0f

    // ボタン + アイコンを重ねて配置
    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        // ボタン本体
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(ButtonBackground)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // プログレスリング
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 2.dp.toPx()
                val padding = 2.dp.toPx()
                val diameter = minOf(size.width, size.height) - strokeWidth - padding * 2
                val topLeft = Offset(
                    (size.width - diameter) / 2,
                    (size.height - diameter) / 2
                )
                val arcSize = Size(diameter, diameter)

                // 背景リング
                drawArc(
                    color = RingBackground,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // プログレス
                val progressSweep = 360f * presetProgress
                if (progressSweep > 0) {
                    drawArc(
                        color = CyanBright,
                        startAngle = -90f,
                        sweepAngle = progressSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // 数字（中央）
            Text(
                text = "$amountMl",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // アイコン（6時の位置、円に被せる）
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(9.dp)
                .align(Alignment.BottomCenter),
            colorFilter = ColorFilter.tint(TextGray)
        )
    }
}

