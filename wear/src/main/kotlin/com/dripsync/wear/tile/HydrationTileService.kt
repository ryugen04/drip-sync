package com.dripsync.wear.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.ArcLine
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId
import androidx.wear.protolayout.ResourceBuilders.ImageResource
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.protolayout.TypeBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import com.dripsync.shared.data.preferences.PresetSettings
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.wear.R
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

// HomeScreen と完全に同じカラーパレット
private const val COLOR_CYAN_BRIGHT = 0xFF00E5FF.toInt()
private const val COLOR_CYAN_MID = 0xFF00B8D4.toInt()
private const val COLOR_BLUE_PURPLE = 0xFF7C4DFF.toInt()
private const val COLOR_BACKGROUND_DARK = 0xFF0D1520.toInt()
private const val COLOR_BUTTON_BACKGROUND = 0xFF1A2535.toInt()
private const val COLOR_TEXT_GRAY = 0xFF5A6678.toInt()
private const val COLOR_TEXT_LIGHT_GRAY = 0xFF8A9AAA.toInt()
private const val COLOR_RING_BACKGROUND = 0xFF2A3545.toInt()
private const val COLOR_WHITE = 0xFFFFFFFF.toInt()

// HomeScreen と同じサイズ設定
private const val RING_STROKE_WIDTH = 12f
private const val BUTTON_OUTER_SIZE = 36f
private const val BUTTON_INNER_SIZE = 30f
private const val BUTTON_RING_STROKE = 2f

// リソースID
private const val ID_DROP_ICON = "drop_icon"
private const val ID_CUP_ICON = "cup_icon"
private const val ID_GLASS_ICON = "glass_icon"
private const val ID_BOTTLE_ICON = "bottle_icon"

@OptIn(ExperimentalHorologistApi::class)
class HydrationTileService : SuspendingTileService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HydrationTileServiceEntryPoint {
        fun hydrationRepository(): HydrationRepository
        fun userPreferencesRepository(): UserPreferencesRepository
    }

    private fun getEntryPoint(context: Context): HydrationTileServiceEntryPoint {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            HydrationTileServiceEntryPoint::class.java
        )
    }

    companion object {
        private const val RESOURCES_VERSION = "1"
        const val EXTRA_AMOUNT_ML = "amount_ml"
    }

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): Tile {
        val entryPoint = getEntryPoint(this)
        val todayTotal = entryPoint.hydrationRepository().getTodayTotal()
        val preferences = entryPoint.userPreferencesRepository().observePreferences().first()
        val presets = preferences.presets
        val dailyGoal = preferences.dailyGoalMl

        return Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(60_000)
            .setTileTimeline(
                Timeline.Builder()
                    .addTimelineEntry(
                        TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(createTileLayout(todayTotal, dailyGoal, presets))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .addIdToImageMapping(ID_DROP_ICON, createImageResource(R.drawable.ic_dripsync_logo))
            .addIdToImageMapping(ID_CUP_ICON, createImageResource(R.drawable.ic_coffee))
            .addIdToImageMapping(ID_GLASS_ICON, createImageResource(R.drawable.ic_glass))
            .addIdToImageMapping(ID_BOTTLE_ICON, createImageResource(R.drawable.ic_bottle))
            .build()
    }

    private fun createImageResource(resId: Int): ImageResource {
        return ImageResource.Builder()
            .setAndroidResourceByResId(
                AndroidImageResourceByResId.Builder()
                    .setResourceId(resId)
                    .build()
            )
            .build()
    }

    private fun createTileLayout(
        todayTotalMl: Int,
        dailyGoalMl: Int,
        presets: PresetSettings
    ): LayoutElementBuilders.LayoutElement {
        val progressPercent = if (dailyGoalMl > 0) {
            (todayTotalMl.toFloat() / dailyGoalMl).coerceIn(0f, 1f)
        } else 0f

        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(
                        Background.Builder()
                            .setColor(argb(COLOR_BACKGROUND_DARK))
                            .build()
                    )
                    .build()
            )
            // 外周プログレスリング（背景）- HomeScreenと同じ12dp幅
            .addContent(
                Arc.Builder()
                    .setAnchorAngle(degrees(0f))
                    .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_CENTER)
                    .addContent(
                        ArcLine.Builder()
                            .setLength(degrees(360f))
                            .setThickness(dp(RING_STROKE_WIDTH))
                            .setColor(argb(COLOR_RING_BACKGROUND))
                            .build()
                    )
                    .build()
            )
            // 外周プログレスリング（プログレス）- 12時から時計回り、グラデーション
            .addContent(
                Arc.Builder()
                    .setAnchorAngle(degrees(-90f))
                    .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                    .addContent(
                        ArcLine.Builder()
                            .setLength(degrees(360f * progressPercent))
                            .setThickness(dp(RING_STROKE_WIDTH))
                            .setBrush(createProgressGradient())
                            .build()
                    )
                    .build()
            )
            // メインコンテンツ
            .addContent(
                Box.Builder()
                    .setWidth(expand())
                    .setHeight(expand())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .addContent(
                        Column.Builder()
                            .setWidth(wrap())
                            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                            // タイトル（水滴アイコン + DRIPSYNC）
                            .addContent(createTitle())
                            .addContent(Spacer.Builder().setHeight(dp(16f)).build())
                            // 摂取量表示
                            .addContent(createIntakeDisplay(todayTotalMl, dailyGoalMl))
                            .addContent(Spacer.Builder().setHeight(dp(16f)).build())
                            // プリセットボタン
                            .addContent(createPresetButtons(presets, dailyGoalMl))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    // HomeScreenと同じタイトル（水滴アイコン12dp + DRIPSYNC 9sp）
    private fun createTitle(): LayoutElementBuilders.LayoutElement {
        return Row.Builder()
            .setWidth(wrap())
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(
                Image.Builder()
                    .setResourceId(ID_DROP_ICON)
                    .setWidth(dp(12f))
                    .setHeight(dp(12f))
                    .setColorFilter(
                        LayoutElementBuilders.ColorFilter.Builder()
                            .setTint(argb(COLOR_CYAN_BRIGHT))
                            .build()
                    )
                    .build()
            )
            .addContent(Spacer.Builder().setWidth(dp(4f)).build())
            .addContent(
                Text.Builder()
                    .setText("DRIPSYNC")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(9f))
                            .setColor(argb(COLOR_TEXT_GRAY))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    // HomeScreenと同じ摂取量表示（28sp白 + 12sp灰色）
    private fun createIntakeDisplay(
        todayTotalMl: Int,
        dailyGoalMl: Int
    ): LayoutElementBuilders.LayoutElement {
        return Row.Builder()
            .setWidth(wrap())
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
            .addContent(
                Text.Builder()
                    .setText("$todayTotalMl")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(28f))
                            .setColor(argb(COLOR_WHITE))
                            .build()
                    )
                    .build()
            )
            .addContent(
                Text.Builder()
                    .setText("/${dailyGoalMl}ml")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(12f))
                            .setColor(argb(COLOR_TEXT_LIGHT_GRAY))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    // HomeScreenと同じプリセットボタン（36dp外枠、30dpボタン、リング付き）
    private fun createPresetButtons(
        presets: PresetSettings,
        dailyGoalMl: Int
    ): LayoutElementBuilders.LayoutElement {
        return Row.Builder()
            .setWidth(wrap())
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(createPresetButton(presets.preset1Ml, dailyGoalMl))
            .addContent(Spacer.Builder().setWidth(dp(6f)).build())
            .addContent(createPresetButton(presets.preset2Ml, dailyGoalMl))
            .addContent(Spacer.Builder().setWidth(dp(6f)).build())
            .addContent(createPresetButton(presets.preset3Ml, dailyGoalMl))
            .build()
    }

    private fun createPresetButton(
        amountMl: Int,
        dailyGoalMl: Int
    ): LayoutElementBuilders.LayoutElement {
        val presetProgress = if (dailyGoalMl > 0) {
            (amountMl.toFloat() / dailyGoalMl).coerceIn(0f, 1f)
        } else 0f

        val iconId = when {
            amountMl <= 200 -> ID_CUP_ICON
            amountMl <= 500 -> ID_GLASS_ICON
            else -> ID_BOTTLE_ICON
        }

        return Box.Builder()
            .setWidth(dp(BUTTON_OUTER_SIZE))
            .setHeight(dp(BUTTON_OUTER_SIZE))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            // ボタン本体（30dp）
            .addContent(
                Box.Builder()
                    .setWidth(dp(BUTTON_INNER_SIZE))
                    .setHeight(dp(BUTTON_INNER_SIZE))
                    .setModifiers(
                        Modifiers.Builder()
                            .setBackground(
                                Background.Builder()
                                    .setColor(argb(COLOR_BUTTON_BACKGROUND))
                                    .setCorner(
                                        Corner.Builder()
                                            .setRadius(dp(BUTTON_INNER_SIZE / 2))
                                            .build()
                                    )
                                    .build()
                            )
                            .setClickable(
                                Clickable.Builder()
                                    .setId("record_$amountMl")
                                    .setOnClick(
                                        ActionBuilders.LaunchAction.Builder()
                                            .setAndroidActivity(
                                                ActionBuilders.AndroidActivity.Builder()
                                                    .setPackageName(packageName)
                                                    .setClassName("com.dripsync.wear.tile.RecordHydrationActivity")
                                                    .addKeyToExtraMapping(
                                                        EXTRA_AMOUNT_ML,
                                                        ActionBuilders.AndroidIntExtra.Builder()
                                                            .setValue(amountMl)
                                                            .build()
                                                    )
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    // プログレスリング（背景）
                    .addContent(
                        Arc.Builder()
                            .setAnchorAngle(degrees(0f))
                            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_CENTER)
                            .addContent(
                                ArcLine.Builder()
                                    .setLength(degrees(360f))
                                    .setThickness(dp(BUTTON_RING_STROKE))
                                    .setColor(argb(COLOR_RING_BACKGROUND))
                                    .build()
                            )
                            .build()
                    )
                    // プログレスリング（プログレス）- 12時から開始
                    .addContent(
                        Arc.Builder()
                            .setAnchorAngle(degrees(-90f))
                            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                            .addContent(
                                ArcLine.Builder()
                                    .setLength(degrees(360f * presetProgress))
                                    .setThickness(dp(BUTTON_RING_STROKE))
                                    .setColor(argb(COLOR_CYAN_BRIGHT))
                                    .build()
                            )
                            .build()
                    )
                    // 数字（9sp）
                    .addContent(
                        Text.Builder()
                            .setText("$amountMl")
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(sp(9f))
                                    .setColor(argb(COLOR_WHITE))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            // アイコン（6時の位置、9dp）
            .addContent(
                Box.Builder()
                    .setWidth(dp(BUTTON_OUTER_SIZE))
                    .setHeight(dp(BUTTON_OUTER_SIZE))
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .addContent(
                        Image.Builder()
                            .setResourceId(iconId)
                            .setWidth(dp(9f))
                            .setHeight(dp(9f))
                            .setColorFilter(
                                LayoutElementBuilders.ColorFilter.Builder()
                                    .setTint(argb(COLOR_TEXT_GRAY))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    // HomeScreenと同じグラデーション
    // 0度=12時から時計回りに CyanBright → CyanMid → BluePurple
    private fun createProgressGradient(): ColorBuilders.Brush {
        return ColorBuilders.SweepGradient.Builder(
            ColorBuilders.ColorStop.Builder(
                argb(COLOR_CYAN_BRIGHT),
                TypeBuilders.FloatProp.Builder(0.0f).build()
            ).build(),
            ColorBuilders.ColorStop.Builder(
                argb(COLOR_CYAN_MID),
                TypeBuilders.FloatProp.Builder(0.25f).build()
            ).build(),
            ColorBuilders.ColorStop.Builder(
                argb(COLOR_BLUE_PURPLE),
                TypeBuilders.FloatProp.Builder(0.5f).build()
            ).build(),
            ColorBuilders.ColorStop.Builder(
                argb(COLOR_BLUE_PURPLE),
                TypeBuilders.FloatProp.Builder(0.75f).build()
            ).build(),
            ColorBuilders.ColorStop.Builder(
                argb(COLOR_CYAN_BRIGHT),
                TypeBuilders.FloatProp.Builder(1.0f).build()
            ).build()
        ).build()
    }
}
