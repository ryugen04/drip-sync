package com.dripsync.shared.util

import com.dripsync.shared.data.preferences.PresetSettings

/**
 * 水分量に基づいてアイコン種別を判定するユーティリティ
 */
object IconUtils {
    enum class IconType {
        COFFEE,  // 小（preset1以下）
        GLASS,   // 中（preset1超〜preset2以下）
        BOTTLE   // 大（preset2超）
    }

    /**
     * プリセット設定を閾値として、量に対応するアイコン種別を返す
     */
    fun getIconTypeForAmount(amountMl: Int, presets: PresetSettings): IconType {
        return when {
            amountMl <= presets.preset1Ml -> IconType.COFFEE
            amountMl <= presets.preset2Ml -> IconType.GLASS
            else -> IconType.BOTTLE
        }
    }
}
