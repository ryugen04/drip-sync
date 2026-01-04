package com.dripsync.shared.sync

/**
 * Data Layer同期で使用するパス定義
 */
object DataLayerPaths {
    // データアイテムのパス
    const val HYDRATION_RECORD_PATH = "/hydration/record"
    const val HYDRATION_SYNC_PATH = "/hydration/sync"
    const val PREFERENCES_PATH = "/preferences"

    // メッセージのパス
    const val SYNC_REQUEST_PATH = "/sync/request"
    const val SYNC_COMPLETE_PATH = "/sync/complete"

    // キー
    const val KEY_RECORD_ID = "record_id"
    const val KEY_AMOUNT_ML = "amount_ml"
    const val KEY_BEVERAGE_TYPE = "beverage_type"
    const val KEY_RECORDED_AT = "recorded_at"
    const val KEY_SOURCE_DEVICE = "source_device"
    const val KEY_DAILY_GOAL_ML = "daily_goal_ml"
    const val KEY_PRESET_1 = "preset_1"
    const val KEY_PRESET_2 = "preset_2"
    const val KEY_PRESET_3 = "preset_3"
    const val KEY_SYNC_TIMESTAMP = "sync_timestamp"
}
