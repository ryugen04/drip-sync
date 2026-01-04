---
name: android-guidelines
description: Use when writing or reviewing Android code. Covers Jetpack Compose, ViewModel, Hilt DI, Room Database, Health Connect, and Data Layer API patterns.
---

# Android Development Guidelines

DripSyncプロジェクトにおけるAndroid開発のベストプラクティス。

## Jetpack Compose

### State Management

```kotlin
// GOOD: State hoisting で状態を上位に持ち上げ
@Composable
fun HydrationCounter(
    currentAmount: Int,
    onAddWater: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("${currentAmount}ml")
        Button(onClick = { onAddWater(200) }) {
            Text("200ml 追加")
        }
    }
}

// BAD: Composable内でrememberで状態を保持（テストしにくい）
@Composable
fun HydrationCounter() {
    var currentAmount by remember { mutableStateOf(0) }
    // ...
}
```

### ViewModel との接続

```kotlin
// GOOD: collectAsStateWithLifecycle で安全に収集
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        onAddHydration = viewModel::addHydration
    )
}

// BAD: collectAsState は lifecycle に対応しない
val uiState by viewModel.uiState.collectAsState()
```

### Preview

```kotlin
// GOOD: PreviewParameterProvider で複数パターンをプレビュー
class HomeUiStateProvider : PreviewParameterProvider<HomeUiState> {
    override val values = sequenceOf(
        HomeUiState(currentAmount = 0, dailyGoal = 2000),
        HomeUiState(currentAmount = 1500, dailyGoal = 2000),
        HomeUiState(currentAmount = 2200, dailyGoal = 2000)
    )
}

@Preview
@Composable
fun HomeContentPreview(
    @PreviewParameter(HomeUiStateProvider::class) uiState: HomeUiState
) {
    HomeContent(uiState = uiState, onAddHydration = {})
}
```

## ViewModel

### UiState パターン

```kotlin
// GOOD: sealed class で状態を表現
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val currentAmount: Int,
        val dailyGoal: Int,
        val records: List<HydrationRecord>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

// ViewModel
class HomeViewModel @Inject constructor(
    private val repository: HydrationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getTodayRecords()
                .catch { e -> _uiState.value = HomeUiState.Error(e.message ?: "Unknown error") }
                .collect { records ->
                    _uiState.value = HomeUiState.Success(
                        currentAmount = records.sumOf { it.amount },
                        dailyGoal = 2000,
                        records = records
                    )
                }
        }
    }
}
```

### イベント処理

```kotlin
// GOOD: 単発イベントは Channel で処理
class HomeViewModel : ViewModel() {
    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

    fun addHydration(amount: Int) {
        viewModelScope.launch {
            repository.addRecord(amount)
            _events.send(HomeEvent.HydrationAdded(amount))
        }
    }
}

sealed class HomeEvent {
    data class HydrationAdded(val amount: Int) : HomeEvent()
    data class Error(val message: String) : HomeEvent()
}
```

## Hilt DI

### Module 定義

```kotlin
// GOOD: インターフェースにバインド
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindHydrationRepository(
        impl: HydrationRepositoryImpl
    ): HydrationRepository
}

// GOOD: 外部ライブラリのインスタンスは @Provides
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): DripSyncDatabase {
        return Room.databaseBuilder(
            context,
            DripSyncDatabase::class.java,
            "dripsync.db"
        ).build()
    }

    @Provides
    fun provideHydrationDao(database: DripSyncDatabase): HydrationDao {
        return database.hydrationDao()
    }
}
```

## Room Database

### Entity 定義

```kotlin
@Entity(tableName = "hydration_records")
data class HydrationEntity(
    @PrimaryKey
    val id: String,
    val amount: Int,
    val drinkType: String,
    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long,
    @ColumnInfo(name = "synced_to_mobile")
    val syncedToMobile: Boolean = false
)
```

### DAO

```kotlin
@Dao
interface HydrationDao {
    @Query("SELECT * FROM hydration_records WHERE recorded_at >= :startOfDay ORDER BY recorded_at DESC")
    fun getTodayRecords(startOfDay: Long): Flow<List<HydrationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: HydrationEntity)

    @Query("DELETE FROM hydration_records WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

### Flow での監視

```kotlin
// GOOD: Repository で Flow を返し、ViewModel で collect
class HydrationRepositoryImpl @Inject constructor(
    private val dao: HydrationDao
) : HydrationRepository {
    override fun getTodayRecords(): Flow<List<HydrationRecord>> {
        val startOfDay = DateTimeUtils.getStartOfDayMillis()
        return dao.getTodayRecords(startOfDay).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
}
```

## Health Connect

### 権限チェック

```kotlin
// GOOD: 権限チェックを必ず行う
class HealthConnectManager @Inject constructor(
    private val context: Context
) {
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    suspend fun checkPermissionsAndRead(): Result<List<HydrationRecord>> {
        val granted = healthConnectClient.permissionController
            .getGrantedPermissions()

        if (HYDRATION_PERMISSION !in granted) {
            return Result.failure(PermissionNotGrantedException())
        }

        return try {
            val records = readHydrationRecords()
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        val HYDRATION_PERMISSION = HealthPermission.getReadPermission(
            HydrationRecord::class
        )
    }
}
```

## Data Layer API

### Watch ↔ Mobile 同期

```kotlin
// shared/sync/DataLayerPaths.kt で統一管理
object DataLayerPaths {
    const val HYDRATION_RECORDS = "/hydration/records"
    const val SETTINGS = "/settings"
    const val SYNC_REQUEST = "/sync/request"
}

// MessageClient: 一時的なアクション用
suspend fun sendHydrationRecord(record: HydrationRecord) {
    val data = record.toByteArray()
    messageClient.sendMessage(
        nodeId,
        DataLayerPaths.HYDRATION_RECORDS,
        data
    ).await()
}

// DataClient: 永続的な状態同期用
suspend fun syncSettings(settings: UserSettings) {
    val request = PutDataMapRequest.create(DataLayerPaths.SETTINGS).apply {
        dataMap.putInt("daily_goal", settings.dailyGoal)
        dataMap.putLong("updated_at", System.currentTimeMillis())
    }
    dataClient.putDataItem(request.asPutDataRequest()).await()
}
```

### ListenerService

```kotlin
class DataLayerListenerService : WearableListenerService() {
    @Inject
    lateinit var repository: HydrationRepository

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            DataLayerPaths.HYDRATION_RECORDS -> {
                val record = HydrationRecord.fromByteArray(event.data)
                CoroutineScope(Dispatchers.IO).launch {
                    repository.insertOrUpdate(record)
                }
            }
        }
    }
}
```

## テスト

### ViewModel テスト

```kotlin
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<HydrationRepository>()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        every { repository.getTodayRecords() } returns flowOf(emptyList())
        viewModel = HomeViewModel(repository)
    }

    @Test
    fun `addHydration updates state correctly`() = runTest {
        coEvery { repository.addRecord(any()) } just Runs

        viewModel.uiState.test {
            viewModel.addHydration(200)

            // Loading -> Success の遷移を確認
            val state = awaitItem()
            assertThat(state).isInstanceOf(HomeUiState.Success::class.java)
        }
    }
}
```

## 参考資料

- [Jetpack Compose State](https://developer.android.com/develop/ui/compose/state)
- [ViewModel Best Practices](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Room](https://developer.android.com/training/data-storage/room)
- [Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect)
- [Wear Data Layer](https://developer.android.com/training/wearables/data/data-layer)
