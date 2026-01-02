package com.dripsync.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dripsync.mobile.R
import com.dripsync.shared.domain.model.Hydration
import java.time.format.DateTimeFormatter

// カラーパレット
private val CyanBright = Color(0xFF00E5FF)
private val IconBackground = Color(0xFF1A2535)

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * 水分摂取記録の1アイテム
 *
 * @param record 記録データ
 * @param onDelete 削除ボタンクリック時のコールバック
 * @param showDeleteButton 削除ボタンを表示するか
 */
@Composable
fun RecordItem(
    record: Hydration,
    onDelete: (() -> Unit)? = null,
    showDeleteButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // アイコン
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(IconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = getIconPainterForAmount(record.amountMl),
                    contentDescription = null,
                    tint = CyanBright,
                    modifier = Modifier.size(24.dp)
                )
            }

            // テキスト情報
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = record.displayAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = record.recordedAt
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime()
                        .format(timeFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // 削除ボタン
            if (showDeleteButton && onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "削除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun getIconPainterForAmount(amountMl: Int): Painter {
    return when {
        amountMl <= 200 -> rememberVectorPainter(Icons.Filled.LocalCafe)
        amountMl <= 400 -> rememberVectorPainter(Icons.Filled.LocalDrink)
        else -> painterResource(R.drawable.ic_dripsync_logo)
    }
}
