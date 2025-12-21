package com.dripsync.wear.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.wear.MainActivity
import com.dripsync.wear.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 水分摂取量を表示するコンプリケーションサービス
 *
 * 対応タイプ:
 * - SHORT_TEXT: 摂取量のテキスト表示（例: "1.2L"）
 * - RANGED_VALUE: 進捗バー付き表示
 */
class HydrationComplicationService : ComplicationDataSourceService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HydrationComplicationEntryPoint {
        fun hydrationRepository(): HydrationRepository
        fun userPreferencesRepository(): UserPreferencesRepository
    }

    private fun getEntryPoint(): HydrationComplicationEntryPoint {
        return EntryPointAccessors.fromApplication(
            applicationContext,
            HydrationComplicationEntryPoint::class.java
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> createShortTextPreview()
            ComplicationType.RANGED_VALUE -> createRangedValuePreview()
            else -> null
        }
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val entryPoint = getEntryPoint()

        // runBlockingを使用してコルーチンを実行
        val (todayTotal, dailyGoal) = runBlocking {
            val total = entryPoint.hydrationRepository().getTodayTotal()
            val preferences = entryPoint.userPreferencesRepository().observePreferences().first()
            Pair(total, preferences.dailyGoalMl)
        }

        val complicationData = when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> createShortTextData(todayTotal, dailyGoal)
            ComplicationType.RANGED_VALUE -> createRangedValueData(todayTotal, dailyGoal)
            else -> null
        }

        listener.onComplicationData(complicationData)
    }

    // SHORT_TEXT: 摂取量テキスト表示
    private fun createShortTextData(todayTotalMl: Int, dailyGoalMl: Int): ComplicationData {
        val displayText = if (todayTotalMl >= 1000) {
            String.format("%.1fL", todayTotalMl / 1000f)
        } else {
            "${todayTotalMl}ml"
        }
        val percentText = if (dailyGoalMl > 0) {
            "${(todayTotalMl * 100 / dailyGoalMl)}%"
        } else {
            ""
        }

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(displayText).build(),
            contentDescription = PlainComplicationText.Builder(
                "水分摂取量: $todayTotalMl ml"
            ).build()
        )
            .setTitle(PlainComplicationText.Builder(percentText).build())
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.drop)
                ).build()
            )
            .setTapAction(createTapAction())
            .build()
    }

    // RANGED_VALUE: 進捗バー付き表示
    private fun createRangedValueData(todayTotalMl: Int, dailyGoalMl: Int): ComplicationData {
        val displayText = if (todayTotalMl >= 1000) {
            String.format("%.1fL", todayTotalMl / 1000f)
        } else {
            "${todayTotalMl}ml"
        }

        return RangedValueComplicationData.Builder(
            value = todayTotalMl.toFloat(),
            min = 0f,
            max = dailyGoalMl.toFloat().coerceAtLeast(1f),
            contentDescription = PlainComplicationText.Builder(
                "水分摂取量: $todayTotalMl / $dailyGoalMl ml"
            ).build()
        )
            .setText(PlainComplicationText.Builder(displayText).build())
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.drop)
                ).build()
            )
            .setTapAction(createTapAction())
            .build()
    }

    // プレビュー用データ
    private fun createShortTextPreview(): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("1.2L").build(),
            contentDescription = PlainComplicationText.Builder("水分摂取量プレビュー").build()
        )
            .setTitle(PlainComplicationText.Builder("60%").build())
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.drop)
                ).build()
            )
            .build()
    }

    private fun createRangedValuePreview(): ComplicationData {
        return RangedValueComplicationData.Builder(
            value = 1200f,
            min = 0f,
            max = 2000f,
            contentDescription = PlainComplicationText.Builder("水分摂取量プレビュー").build()
        )
            .setText(PlainComplicationText.Builder("1.2L").build())
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.drop)
                ).build()
            )
            .build()
    }

    // タップ時にアプリを起動するPendingIntent
    private fun createTapAction(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        /**
         * コンプリケーションの更新をリクエストする
         * 水分記録後などに呼び出す
         */
        fun requestUpdate(context: Context) {
            val componentName = ComponentName(context, HydrationComplicationService::class.java)
            val requester = ComplicationDataSourceUpdateRequester.create(context, componentName)
            requester.requestUpdateAll()
        }
    }
}
