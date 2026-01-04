---
name: wear-os-guidelines
description: Use when writing or reviewing Wear OS code. Covers Compose for Wear OS, Tiles, Complications, Horologist library, and battery optimization.
---

# Wear OS Development Guidelines

DripSyncプロジェクトにおけるWear OS開発のベストプラクティス。

## Compose for Wear OS

### 基本構成

```kotlin
// GOOD: Horologist の ScalingLazyColumn を使用
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddHydration: (Int) -> Unit
) {
    val listState = rememberResponsiveColumnState(
        contentPadding = ScalingLazyColumnDefaults.padding(
            first = ScalingLazyColumnDefaults.ItemType.Text,
            last = ScalingLazyColumnDefaults.ItemType.Chip
        )
    )

    ScreenScaffold(
        scrollState = listState,
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            columnState = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text("今日の摂取量")
            }
            item {
                CircularProgressIndicator(
                    progress = uiState.progress,
                    modifier = Modifier.size(120.dp)
                )
            }
            items(presets) { preset ->
                Chip(
                    label = { Text("${preset}ml") },
                    onClick = { onAddHydration(preset) }
                )
            }
        }
    }
}
```

### 丸型画面対応

```kotlin
// GOOD: 画面端の切れ込みを考慮したパディング
@Composable
fun WearContent(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 8.dp,
                vertical = 8.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// BAD: 画面端ギリギリまで使用
Box(modifier = Modifier.fillMaxSize()) {
    // 丸型画面で端が切れる
}
```

### ナビゲーション

```kotlin
// GOOD: Horologist の Navigation を使用
@Composable
fun WearNavHost(
    navController: NavHostController = rememberSwipeDismissableNavController()
) {
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("history") {
            HistoryScreen()
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}
```

## Tiles

### TileService 実装

```kotlin
class HydrationTileService : SuspendingTileService() {
    @Inject
    lateinit var repository: HydrationRepository

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .addIdToImageMapping(
                "water_icon",
                ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.ic_water)
                            .build()
                    )
                    .build()
            )
            .build()
    }

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val todayAmount = repository.getTodayTotalAmount()
        val dailyGoal = 2000

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                createLayout(todayAmount, dailyGoal)
                            )
                            .build()
                    )
                    .build()
            )
            .setFreshnessIntervalMillis(FRESHNESS_INTERVAL)
            .build()
    }

    private fun createLayout(current: Int, goal: Int): LayoutElementBuilders.LayoutElement {
        // Tiles Material Components を使用
        return PrimaryLayout.Builder(deviceParameters)
            .setContent(
                CircularProgressIndicator.Builder()
                    .setProgress((current.toFloat() / goal).coerceIn(0f, 1f))
                    .build()
            )
            .setPrimaryLabelTextContent(
                Text.Builder(context, "${current}ml / ${goal}ml")
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .build()
            )
            .setPrimaryChipContent(
                CompactChip.Builder(
                    context,
                    Clickable.Builder()
                        .setOnClick(
                            ActionBuilders.LoadAction.Builder().build()
                        )
                        .build(),
                    deviceParameters
                )
                    .setTextContent("記録する")
                    .build()
            )
            .build()
    }

    companion object {
        private const val RESOURCES_VERSION = "1"
        private const val FRESHNESS_INTERVAL = 15 * 60 * 1000L // 15分
    }
}
```

### タイル更新

```kotlin
// GOOD: タイルを更新する際は TileService.getUpdater() を使用
fun requestTileUpdate(context: Context) {
    TileService.getUpdater(context)
        .requestUpdate(HydrationTileService::class.java)
}

// 水分記録後に更新をリクエスト
class HydrationRepositoryImpl : HydrationRepository {
    override suspend fun addRecord(amount: Int) {
        dao.insert(createRecord(amount))
        requestTileUpdate(context)
    }
}
```

## Complications

### ComplicationDataSourceService

```kotlin
class HydrationComplicationService : SuspendingComplicationDataSourceService() {
    @Inject
    lateinit var repository: HydrationRepository

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("1500ml").build(),
                    contentDescription = PlainComplicationText.Builder("水分摂取量").build()
                ).build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 0.75f,
                    min = 0f,
                    max = 1f,
                    contentDescription = PlainComplicationText.Builder("75%").build()
                )
                    .setText(PlainComplicationText.Builder("1500ml").build())
                    .build()
            }
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val current = repository.getTodayTotalAmount()
        val goal = 2000
        val progress = (current.toFloat() / goal).coerceIn(0f, 1f)

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("${current}ml").build(),
                    contentDescription = PlainComplicationText.Builder("今日の水分摂取量").build()
                )
                    .setTapAction(createTapPendingIntent())
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = progress,
                    min = 0f,
                    max = 1f,
                    contentDescription = PlainComplicationText.Builder("${(progress * 100).toInt()}%").build()
                )
                    .setText(PlainComplicationText.Builder("${current}ml").build())
                    .setTapAction(createTapPendingIntent())
                    .build()
            }
            else -> null
        }
    }

    override fun createUpdateRequest(): ProviderUpdateRequester? {
        return ProviderUpdateRequester(
            this,
            ComponentName(this, HydrationComplicationService::class.java)
        )
    }
}
```

## Horologist

### 推奨ライブラリ

```kotlin
// build.gradle.kts
dependencies {
    // Horologist Compose Layout - ScalingLazyColumn, TimeText など
    implementation("com.google.android.horologist:horologist-compose-layout:0.6.22")

    // Horologist Compose Material - Chip, Button の拡張
    implementation("com.google.android.horologist:horologist-compose-material:0.6.22")

    // Horologist Tiles - タイル作成ヘルパー
    implementation("com.google.android.horologist:horologist-tiles:0.6.22")
}
```

### ScalingLazyColumn の使用

```kotlin
// GOOD: Horologist の ResponsiveColumnState を使用
@Composable
fun HistoryScreen(records: List<HydrationRecord>) {
    val columnState = rememberResponsiveColumnState(
        contentPadding = ScalingLazyColumnDefaults.padding(
            first = ScalingLazyColumnDefaults.ItemType.Text,
            last = ScalingLazyColumnDefaults.ItemType.Card
        )
    )

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(columnState = columnState) {
            item {
                ResponsiveListHeader {
                    Text("履歴")
                }
            }
            items(records) { record ->
                Card(
                    onClick = { /* 詳細表示 */ }
                ) {
                    Text("${record.amount}ml - ${record.formattedTime}")
                }
            }
        }
    }
}
```

## バッテリー最適化

### 原則

1. **同期頻度を最小限に**: 15分間隔以上が推奨
2. **バッチ処理**: 複数の更新をまとめて処理
3. **Ambient Mode対応**: 画面オフ時の更新を控える

### WorkManager での定期同期

```kotlin
// GOOD: WorkManager で効率的にバックグラウンド同期
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            syncWithMobile()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES,   // 最小間隔
                5, TimeUnit.MINUTES     // フレックス間隔
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "sync_hydration",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
```

### Always-on Display 対応

```kotlin
// GOOD: Ambient Mode を考慮したUI
@Composable
fun HomeScreen(
    ambientState: AmbientState,
    uiState: HomeUiState
) {
    val isAmbient = ambientState.isAmbient

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAmbient) Color.Black else MaterialTheme.colors.background)
    ) {
        // Ambient Mode では更新頻度を下げ、シンプルな表示に
        if (isAmbient) {
            // 白黒のシンプルな表示
            AmbientContent(uiState)
        } else {
            // 通常のカラー表示
            InteractiveContent(uiState)
        }
    }
}
```

## Input Methods

### ロータリー入力対応

```kotlin
// GOOD: RotaryScrollableDefaults でスクロール
@Composable
fun PresetListScreen(
    presets: List<Int>,
    onSelect: (Int) -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = rememberActiveFocusRequester()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .rotaryWithScroll(
                scrollableState = listState,
                focusRequester = focusRequester
            )
    ) {
        items(presets) { preset ->
            Chip(
                label = { Text("${preset}ml") },
                onClick = { onSelect(preset) }
            )
        }
    }
}
```

### 物理ボタン対応

```kotlin
// GOOD: ハードウェアボタンのイベントを処理
@Composable
fun RecordScreen(onRecord: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.key == Key.Stem1 && event.type == KeyEventType.KeyUp) {
                    onRecord()
                    true
                } else {
                    false
                }
            }
    ) {
        // UI
    }
}
```

## 画面サイズ対応

### デバイスパラメータの取得

```kotlin
// GOOD: 画面サイズに応じたレイアウト調整
@Composable
fun AdaptiveLayout() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    val chipSize = when {
        screenWidth < 192 -> ChipDefaults.SmallChipDefaults
        screenWidth < 225 -> ChipDefaults.DefaultChipDefaults
        else -> ChipDefaults.LargeChipDefaults
    }

    Chip(
        modifier = Modifier.size(chipSize),
        // ...
    )
}
```

## 参考資料

- [Compose for Wear OS](https://developer.android.com/training/wearables/compose)
- [Horologist](https://github.com/google/horologist)
- [Wear Tiles](https://developer.android.com/training/wearables/tiles)
- [Complications](https://developer.android.com/training/wearables/watch-faces/complications)
- [Battery Best Practices](https://developer.android.com/training/wearables/performance/battery)
- [Wear OS Design Principles](https://developer.android.com/design/ui/wear)
