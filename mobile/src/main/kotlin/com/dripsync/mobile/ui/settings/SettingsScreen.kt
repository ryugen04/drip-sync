package com.dripsync.mobile.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.dripsync.mobile.R
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dripsync.mobile.health.HealthConnectManager
import com.dripsync.mobile.health.SyncResult
import kotlinx.coroutines.flow.collectLatest

// カラーパレット（他の画面と統一）
private val CyanBright = Color(0xFF06B6D4)
private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val GrayText = Color(0xFF94A3B8)
private val ButtonBackground = Color(0xFF374151)

private val GOAL_PRESETS = listOf(1500, 2000, 2500, 3000)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val healthConnectManager = viewModel.getHealthConnectManager()

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
            SectionHeader(
                icon = rememberVectorPainter(Icons.Filled.GpsFixed),
                title = "一日の目標"
            )

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
                                    text = "$preset",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "一般的な成人の1日の水分摂取推奨量は2000mLです。",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText
                    )
                }
            }

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
                    iconRes = getIconResForAmount(uiState.presets.preset1Ml),
                    onDecrease = { viewModel.updatePreset(0, maxOf(50, uiState.presets.preset1Ml - 50)) },
                    onIncrease = { viewModel.updatePreset(0, minOf(2000, uiState.presets.preset1Ml + 50)) }
                )

                PresetItem(
                    label = "プリセット 2",
                    amountMl = uiState.presets.preset2Ml,
                    iconRes = getIconResForAmount(uiState.presets.preset2Ml),
                    onDecrease = { viewModel.updatePreset(1, maxOf(50, uiState.presets.preset2Ml - 50)) },
                    onIncrease = { viewModel.updatePreset(1, minOf(2000, uiState.presets.preset2Ml + 50)) }
                )

                PresetItem(
                    label = "プリセット 3",
                    amountMl = uiState.presets.preset3Ml,
                    iconRes = getIconResForAmount(uiState.presets.preset3Ml),
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
                        text = "バージョン 1.0.0",
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

@DrawableRes
private fun getIconResForAmount(amountMl: Int): Int {
    return when {
        amountMl <= 250 -> R.drawable.ic_coffee
        amountMl <= 400 -> R.drawable.ic_glass
        else -> R.drawable.ic_bottle
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
