package com.dripsync.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dripsync.mobile.R

// カラーパレット
private val CyanBright = Color(0xFF00E5FF)
private val ButtonBackground = Color(0xFF1A2535)

/**
 * 水分記録用プリセットボタン
 *
 * @param amountMl 記録する水分量（ml）
 * @param onClick クリック時のコールバック
 */
@Composable
fun PresetButton(
    amountMl: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = getIconPainterForAmount(amountMl)
    val displayText = formatAmount(amountMl)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(ButtonBackground)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = "$amountMl ml",
                tint = CyanBright,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = displayText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun getIconPainterForAmount(amountMl: Int): Painter {
    return when {
        amountMl <= 200 -> rememberVectorPainter(Icons.Filled.LocalCafe)       // カップ
        amountMl <= 400 -> rememberVectorPainter(Icons.Filled.LocalDrink)      // グラス
        else -> painterResource(R.drawable.ic_dripsync_logo)                   // カスタムロゴ
    }
}

private fun formatAmount(amountMl: Int): String {
    return if (amountMl >= 1000) {
        String.format("%.1fL", amountMl / 1000f)
    } else {
        "${amountMl}ml"
    }
}
