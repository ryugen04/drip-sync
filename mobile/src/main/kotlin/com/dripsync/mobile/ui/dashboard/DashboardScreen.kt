package com.dripsync.mobile.ui.dashboard

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dripsync.mobile.R
import kotlinx.coroutines.flow.collectLatest
import java.time.format.DateTimeFormatter

// カラーパレット（参考実装に合わせる）
private val CyanBright = Color(0xFF06B6D4)
private val BlueBright = Color(0xFF3B82F6)
private val PurpleBright = Color(0xFF8B5CF6)
private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val GrayText = Color(0xFF94A3B8)

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCustomInput by remember { mutableStateOf(false) }
    var customAmount by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is DashboardEvent.RecordSuccess -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.record_success_format, event.amountMl),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is DashboardEvent.RecordFailure -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.record_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(48.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dripsync_logo),
                        contentDescription = null,
                        tint = CyanBright,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "DRIPSYNC",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanBright,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            // Main Progress Display
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${uiState.todayTotalMl}",
                                fontSize = 64.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Light
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "/ ${uiState.dailyGoalMl}",
                                style = MaterialTheme.typography.titleLarge,
                                color = GrayText,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                        Text(
                            text = "ML",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanBright,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                    }

                    // Circular Progress
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = uiState.progressRatio
                        val sweepAngle = 360f * progress
                        val gradientBrush = Brush.sweepGradient(
                            colors = listOf(CyanBright, BlueBright, PurpleBright, CyanBright)
                        )

                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .drawBehind {
                                    // Background ring
                                    drawArc(
                                        color = CyanBright.copy(alpha = 0.15f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    // Progress ring
                                    drawArc(
                                        brush = gradientBrush,
                                        startAngle = -90f,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                        )

                        Text(
                            text = "${uiState.progressPercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick Add Section
            item {
                Text(
                    text = "クイック記録",
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayText,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PresetButtonWithRing(
                        amountMl = uiState.presets.preset1Ml,
                        dailyGoal = uiState.dailyGoalMl,
                        label = "コップ",
                        iconRes = R.drawable.ic_coffee,
                        onClick = { viewModel.recordHydration(uiState.presets.preset1Ml) },
                        modifier = Modifier.weight(1f)
                    )
                    PresetButtonWithRing(
                        amountMl = uiState.presets.preset2Ml,
                        dailyGoal = uiState.dailyGoalMl,
                        label = "マグ",
                        iconRes = R.drawable.ic_glass,
                        onClick = { viewModel.recordHydration(uiState.presets.preset2Ml) },
                        modifier = Modifier.weight(1f)
                    )
                    PresetButtonWithRing(
                        amountMl = uiState.presets.preset3Ml,
                        dailyGoal = uiState.dailyGoalMl,
                        label = "水筒",
                        iconRes = R.drawable.ic_bottle,
                        onClick = { viewModel.recordHydration(uiState.presets.preset3Ml) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Custom Amount
            item {
                Spacer(modifier = Modifier.height(16.dp))

                if (!showCustomInput) {
                    OutlinedButton(
                        onClick = { showCustomInput = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GrayText.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GrayText
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("カスタム量を追加")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customAmount,
                            onValueChange = { customAmount = it.filter { c -> c.isDigit() } },
                            placeholder = { Text("量 (ml)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = GrayText.copy(alpha = 0.3f),
                                focusedBorderColor = CyanBright,
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                customAmount.toIntOrNull()?.let { amount ->
                                    if (amount > 0) {
                                        viewModel.recordHydration(amount)
                                        customAmount = ""
                                        showCustomInput = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanBright,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("追加")
                        }
                        IconButton(
                            onClick = {
                                showCustomInput = false
                                customAmount = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "閉じる",
                                tint = GrayText
                            )
                        }
                    }
                }
            }

            // Today's Records
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "今日の記録",
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayText,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (uiState.todayRecords.isEmpty()) {
                item {
                    Text(
                        text = "まだ記録がありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrayText.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            } else {
                items(
                    items = uiState.todayRecords,
                    key = { it.id }
                ) { record ->
                    RecordItemCard(
                        amountMl = record.amountMl,
                        time = record.recordedAt.atZone(java.time.ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("HH:mm")),
                        onDelete = { /* TODO: implement delete */ }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PresetButtonWithRing(
    amountMl: Int,
    dailyGoal: Int,
    label: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (amountMl.toFloat() / dailyGoal).coerceIn(0f, 1f)
    val sweepAngle = 360f * progress

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .drawBehind {
                            drawArc(
                                color = GrayText.copy(alpha = 0.2f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = CyanBright,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                )

                Text(
                    text = "$amountMl",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = GrayText,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = GrayText
            )
        }
    }
}

@Composable
private fun RecordItemCard(
    amountMl: Int,
    time: String,
    onDelete: () -> Unit
) {
    val iconRes = getIconResForAmount(amountMl)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(CyanBright.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = CyanBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${amountMl}ml",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = time,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "削除",
                    tint = GrayText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@DrawableRes
private fun getIconResForAmount(amountMl: Int): Int {
    return when {
        amountMl <= 250 -> R.drawable.ic_coffee
        amountMl <= 400 -> R.drawable.ic_glass
        else -> R.drawable.ic_bottle
    }
}
