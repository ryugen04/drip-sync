package com.dripsync.mobile.ui.settings

import android.widget.Toast
import com.dripsync.mobile.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.dripsync.mobile.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dripsync.mobile.health.HealthConnectManager
import com.dripsync.mobile.health.SyncResult
import com.dripsync.shared.data.preferences.ReminderSettings
import kotlinx.coroutines.flow.collectLatest

// カラーパレット（他の画面と統一）
private val CyanBright = Color(0xFF06B6D4)
private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val GrayText = Color(0xFF94A3B8)
private val ButtonBackground = Color(0xFF374151)

private val GOAL_PRESETS = listOf(1000, 1500, 2000, 2500)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val healthConnectManager = viewModel.getHealthConnectManager()
    var showGoalInfoDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = healthConnectManager.createPermissionRequestContract()
    ) { grantedPermissions ->
        val allGranted = HealthConnectManager.REQUIRED_PERMISSIONS.all { it in grantedPermissions }
        viewModel.onPermissionResult(allGranted)
    }

    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is SettingsEvent.SaveSuccess -> { }
                is SettingsEvent.SaveFailure -> {
                    Toast.makeText(context, "保存に失敗しました", Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.SyncComplete -> {
                    val message = when (val result = event.result) {
                        is SyncResult.Success -> "同期完了: ${result.synced}件"
                        is SyncResult.PartialSuccess -> "同期完了: ${result.synced}件（一部エラー）"
                        is SyncResult.NotAvailable -> "Health Connectが利用できません"
                        is SyncResult.PermissionRequired -> "権限が必要です"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkHealthConnectStatus()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.syncWithWear()
    }

    if (showGoalInfoDialog) {
        GoalInfoDialog(onDismiss = { showGoalInfoDialog = false })
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = CyanBright,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "設定",
                    style = MaterialTheme.typography.titleSmall,
                    color = GrayText,
                    letterSpacing = 4.sp
                )
            }

            // Daily Goal Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.GpsFixed),
                    contentDescription = null,
                    tint = CyanBright,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "一日の目標",
                    style = MaterialTheme.typography.labelSmall,
                    color = GrayText,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { showGoalInfoDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "目標について",
                        tint = GrayText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = "${uiState.dailyGoalMl}",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "mL",
                            style = MaterialTheme.typography.titleMedium,
                            color = GrayText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GOAL_PRESETS.forEach { preset ->
                            Button(
                                onClick = { viewModel.updateDailyGoal(preset) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.dailyGoalMl == preset) CyanBright else ButtonBackground,
                                    contentColor = if (uiState.dailyGoalMl == preset) Color.Black else GrayText
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${preset / 1000f}L",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "飲料水として1日1.2〜1.5L程度の摂取が推奨されています。",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Reminder Section
            SectionHeader(
                icon = rememberVectorPainter(Icons.Filled.Notifications),
                title = "リマインダー"
            )

            ReminderSettingsCard(
                settings = uiState.reminderSettings,
                onToggle = { viewModel.toggleReminder() },
                onStartHourChange = { viewModel.updateReminderStartHour(it) },
                onEndHourChange = { viewModel.updateReminderEndHour(it) },
                onIntervalChange = { viewModel.updateReminderInterval(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Quick Record Section
            SectionHeader(
                icon = painterResource(R.drawable.ic_dripsync_logo),
                title = "クイック記録"
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PresetItem(
                    label = "プリセット 1",
                    amountMl = uiState.presets.preset1Ml,
                    iconRes = R.drawable.ic_coffee,
                    onDecrease = { viewModel.updatePreset(0, maxOf(50, uiState.presets.preset1Ml - 50)) },
                    onIncrease = { viewModel.updatePreset(0, minOf(2000, uiState.presets.preset1Ml + 50)) }
                )

                PresetItem(
                    label = "プリセット 2",
                    amountMl = uiState.presets.preset2Ml,
                    iconRes = R.drawable.ic_glass,
                    onDecrease = { viewModel.updatePreset(1, maxOf(50, uiState.presets.preset2Ml - 50)) },
                    onIncrease = { viewModel.updatePreset(1, minOf(2000, uiState.presets.preset2Ml + 50)) }
                )

                PresetItem(
                    label = "プリセット 3",
                    amountMl = uiState.presets.preset3Ml,
                    iconRes = R.drawable.ic_bottle,
                    onDecrease = { viewModel.updatePreset(2, maxOf(50, uiState.presets.preset3Ml - 50)) },
                    onIncrease = { viewModel.updatePreset(2, minOf(2000, uiState.presets.preset3Ml + 50)) }
                )
            }

            Text(
                text = "よく飲む量をカスタマイズして、素早く記録できます。",
                style = MaterialTheme.typography.bodySmall,
                color = GrayText,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Health Connect Section
            SectionHeader(
                icon = rememberVectorPainter(Icons.Filled.Sync),
                title = "Health Connect"
            )

            HealthConnectCard(
                status = uiState.healthConnectStatus,
                onRequestPermission = {
                    permissionLauncher.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                },
                onInstall = {
                    context.startActivity(healthConnectManager.getInstallIntent())
                },
                onSync = { viewModel.syncToHealthConnect() },
                onOpenSettings = {
                    context.startActivity(healthConnectManager.getSettingsIntent())
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App Info Section
            SectionHeader(
                icon = rememberVectorPainter(Icons.Filled.Info),
                title = "アプリについて"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "DRIPSYNC",
                        style = MaterialTheme.typography.bodyLarge,
                        color = CyanBright,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "バージョン ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(GrayText.copy(alpha = 0.3f))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Pixel WatchとPixelスマホで水分管理を同期。健康的な水分補給習慣をサポートします。",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GoalInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Text(
                text = "推奨水分摂取量について",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = """厚生労働省の「健康のため水を飲もう」推進運動によると、成人が1日に必要な水分量は約2.5Lです。

このうち食事から約1L、体内で生成される水分が約0.3Lとされており、飲料水として約1.2L〜1.5Lの摂取が推奨されています。

1回100〜200mLをこまめに摂取するのが基本で、起床直後と入浴前後の水分補給が特に重要です。

個人の体格や活動量、気温などによって必要量は変わりますので、ご自身の状況に合わせて目標を調整してください。""",
                color = GrayText,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる", color = CyanBright)
            }
        }
    )
}

@Composable
private fun ReminderSettingsCard(
    settings: ReminderSettings,
    onToggle: () -> Unit,
    onStartHourChange: (Int) -> Unit,
    onEndHourChange: (Int) -> Unit,
    onIntervalChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // オン/オフ切り替え
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "リマインダー通知",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanBright,
                        checkedTrackColor = CyanBright.copy(alpha = 0.5f),
                        uncheckedThumbColor = GrayText,
                        uncheckedTrackColor = ButtonBackground
                    )
                )
            }

            if (settings.isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GrayText.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 時間範囲
                Text(
                    text = "通知時間",
                    style = MaterialTheme.typography.labelSmall,
                    color = GrayText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeSelector(
                        label = "開始",
                        hour = settings.startHour,
                        onHourChange = onStartHourChange,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "〜",
                        color = GrayText,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    TimeSelector(
                        label = "終了",
                        hour = settings.endHour,
                        onHourChange = onEndHourChange,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 通知間隔
                Text(
                    text = "通知間隔",
                    style = MaterialTheme.typography.labelSmall,
                    color = GrayText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReminderSettings.INTERVAL_OPTIONS.forEach { interval ->
                        Button(
                            onClick = { onIntervalChange(interval) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (settings.intervalHours == interval) CyanBright else ButtonBackground,
                                contentColor = if (settings.intervalHours == interval) Color.Black else GrayText
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${interval}時間",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (settings.isEnabled) {
                    "目標未達成の場合、${settings.startHour}時〜${settings.endHour}時の間、${settings.intervalHours}時間ごとに通知します。"
                } else {
                    "リマインダーを有効にすると、目標未達成時に通知でお知らせします。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = GrayText
            )
        }
    }
}

@Composable
private fun TimeSelector(
    label: String,
    hour: Int,
    onHourChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ButtonBackground)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilledIconButton(
            onClick = { if (hour > 0) onHourChange(hour - 1) },
            modifier = Modifier.size(28.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = CardBackground
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "減らす",
                tint = GrayText,
                modifier = Modifier.size(14.dp)
            )
        }

        Text(
            text = "${hour}:00",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        FilledIconButton(
            onClick = { if (hour < 23) onHourChange(hour + 1) },
            modifier = Modifier.size(28.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = CardBackground
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "増やす",
                tint = GrayText,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.painter.Painter,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = CyanBright,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = GrayText,
            letterSpacing = 2.sp
        )
    }
}


@Composable
private fun HealthConnectCard(
    status: HealthConnectStatus,
    onRequestPermission: () -> Unit,
    onInstall: () -> Unit,
    onSync: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (status) {
                        HealthConnectStatus.CONNECTED -> Icons.Filled.CheckCircle
                        else -> Icons.Filled.Warning
                    },
                    contentDescription = null,
                    tint = when (status) {
                        HealthConnectStatus.CONNECTED -> Color(0xFF4CAF50)
                        else -> Color(0xFFFF9800)
                    },
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = when (status) {
                        HealthConnectStatus.NOT_INSTALLED -> "インストールされていません"
                        HealthConnectStatus.NOT_PERMITTED -> "権限が許可されていません"
                        HealthConnectStatus.CONNECTED -> "接続済み"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (status) {
                HealthConnectStatus.NOT_INSTALLED -> {
                    Button(
                        onClick = onInstall,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanBright,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("インストール")
                    }
                }
                HealthConnectStatus.NOT_PERMITTED -> {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanBright,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("権限を許可")
                    }
                }
                HealthConnectStatus.CONNECTED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSync,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = GrayText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("同期", color = GrayText)
                        }
                        OutlinedButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("設定", color = GrayText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetItem(
    label: String,
    amountMl: Int,
    @DrawableRes iconRes: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
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
                        .background(ButtonBackground, RoundedCornerShape(8.dp)),
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
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${amountMl}mL",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ButtonBackground
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "減らす",
                        tint = GrayText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                FilledIconButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ButtonBackground
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "増やす",
                        tint = GrayText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
