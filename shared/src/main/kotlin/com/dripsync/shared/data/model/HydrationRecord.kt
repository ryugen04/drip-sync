package com.dripsync.shared.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * 水分摂取記録のローカルデータベースエンティティ
 */
@Entity(tableName = "hydration_records")
data class HydrationRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // 水分摂取量（ミリリットル）
    val amountMl: Int,

    // 飲料の種類
    val beverageType: BeverageType = BeverageType.WATER,

    // 記録日時（UTC）
    val recordedAt: Instant = Instant.now(),

    // 記録デバイス
    val sourceDevice: SourceDevice = SourceDevice.UNKNOWN,

    // Health Connect への同期状態
    val syncedToHealthConnect: Boolean = false,

    // Health Connect のレコード ID（同期済みの場合）
    val healthConnectId: String? = null,

    // 作成日時
    val createdAt: Instant = Instant.now(),

    // 更新日時
    val updatedAt: Instant = Instant.now()
)

/**
 * 飲料の種類
 */
enum class BeverageType {
    WATER,           // 水
    TEA,             // お茶
    COFFEE,          // コーヒー
    JUICE,           // ジュース
    SPORTS_DRINK,    // スポーツドリンク
    MILK,            // 牛乳
    SODA,            // 炭酸飲料
    ALCOHOL,         // アルコール
    OTHER            // その他
}

/**
 * 記録元デバイス
 */
enum class SourceDevice {
    MOBILE,          // スマートフォン
    WEAR,            // ウォッチ
    HEALTH_CONNECT,  // Health Connect からのインポート
    UNKNOWN          // 不明
}
