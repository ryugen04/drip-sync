package com.dripsync.mobile.ui.dashboard

import app.cash.turbine.test
import com.dripsync.mobile.health.HealthConnectRepository
import com.dripsync.mobile.health.SyncResult
import com.dripsync.mobile.sync.DataLayerRepository
import com.dripsync.shared.data.model.BeverageType
import com.dripsync.shared.data.model.SourceDevice
import com.dripsync.shared.data.preferences.PresetSettings
import com.dripsync.shared.data.preferences.UserPreferences
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.shared.domain.model.Hydration
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var hydrationRepository: HydrationRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var healthConnectRepository: HealthConnectRepository
    private lateinit var dataLayerRepository: DataLayerRepository

    private val todayTotalFlow = MutableStateFlow(0)
    private val todayRecordsFlow = MutableStateFlow<List<Hydration>>(emptyList())
    private val preferencesFlow = MutableStateFlow(
        UserPreferences(dailyGoalMl = 2000, presets = PresetSettings(150, 250, 500))
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        hydrationRepository = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)
        healthConnectRepository = mockk(relaxed = true)
        dataLayerRepository = mockk(relaxed = true)

        every { hydrationRepository.observeTodayTotal() } returns todayTotalFlow
        every { hydrationRepository.observeTodayRecords() } returns todayRecordsFlow
        every { userPreferencesRepository.observePreferences() } returns preferencesFlow
        coEvery { userPreferencesRepository.getPreferences() } returns preferencesFlow.value
        coEvery { hydrationRepository.getRecordsByDateRange(any(), any()) } returns emptyList()
        coEvery { healthConnectRepository.syncToHealthConnect() } returns SyncResult.Success(0)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DashboardViewModel(
        hydrationRepository = hydrationRepository,
        userPreferencesRepository = userPreferencesRepository,
        healthConnectRepository = healthConnectRepository,
        dataLayerRepository = dataLayerRepository
    )

    @Test
    fun `uiState reflects today total from repository`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            // 初期状態
            val initial = awaitItem()
            assertThat(initial.todayTotalMl).isEqualTo(0)

            // 値を更新
            todayTotalFlow.value = 1500
            val updated = awaitItem()
            assertThat(updated.todayTotalMl).isEqualTo(1500)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState reflects daily goal from preferences`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertThat(initial.dailyGoalMl).isEqualTo(2000)

            preferencesFlow.value = UserPreferences(
                dailyGoalMl = 2500,
                presets = PresetSettings(100, 200, 300)
            )
            val updated = awaitItem()
            assertThat(updated.dailyGoalMl).isEqualTo(2500)
            assertThat(updated.presets.preset1Ml).isEqualTo(100)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `progressPercent calculates correctly`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // 初期状態
            todayTotalFlow.value = 1000
            val state = awaitItem()
            assertThat(state.progressPercent).isEqualTo(50)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `progressRatio is capped at 1`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            todayTotalFlow.value = 3000
            val state = awaitItem()
            assertThat(state.progressRatio).isEqualTo(1f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isGoalAchieved is true when total exceeds goal`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            todayTotalFlow.value = 2500
            val state = awaitItem()
            assertThat(state.isGoalAchieved).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `displayTotal formats correctly for ml`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            todayTotalFlow.value = 750
            val state = awaitItem()
            assertThat(state.displayTotal).isEqualTo("750ml")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `displayTotal formats correctly for liters`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            todayTotalFlow.value = 1500
            val state = awaitItem()
            assertThat(state.displayTotal).isEqualTo("1.5L")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recordHydration calls repository and syncs to health connect`() = runTest {
        coEvery {
            hydrationRepository.recordHydration(
                amountMl = any(),
                beverageType = any(),
                sourceDevice = any(),
                recordId = any(),
                recordedAt = any()
            )
        } returns "new-id"

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.recordHydration(250)
        advanceUntilIdle()

        coVerify {
            hydrationRepository.recordHydration(
                amountMl = 250,
                sourceDevice = SourceDevice.MOBILE,
                recordedAt = any(),
                beverageType = any(),
                recordId = any()
            )
        }
        coVerify { healthConnectRepository.syncToHealthConnect() }
    }

    @Test
    fun `recordHydration emits success event`() = runTest {
        coEvery {
            hydrationRepository.recordHydration(
                amountMl = any(),
                beverageType = any(),
                sourceDevice = any(),
                recordId = any(),
                recordedAt = any()
            )
        } returns "new-id"

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.event.test {
            viewModel.recordHydration(250)
            advanceUntilIdle()

            val event = awaitItem()
            assertThat(event).isInstanceOf(DashboardEvent.RecordSuccess::class.java)
            assertThat((event as DashboardEvent.RecordSuccess).amountMl).isEqualTo(250)
        }
    }

    @Test
    fun `recordHydration emits failure event on error`() = runTest {
        coEvery {
            hydrationRepository.recordHydration(
                amountMl = any(),
                beverageType = any(),
                sourceDevice = any(),
                recordId = any(),
                recordedAt = any()
            )
        } throws Exception("DB error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.event.test {
            viewModel.recordHydration(250)
            advanceUntilIdle()

            val event = awaitItem()
            assertThat(event).isEqualTo(DashboardEvent.RecordFailure)
        }
    }

    @Test
    fun `todayRecords are sorted by recordedAt descending`() = runTest {
        val viewModel = createViewModel()

        val records = listOf(
            Hydration(
                id = "1",
                amountMl = 100,
                beverageType = BeverageType.WATER,
                recordedAt = Instant.ofEpochMilli(1000),
                sourceDevice = SourceDevice.MOBILE,
                isSyncedToHealthConnect = false
            ),
            Hydration(
                id = "2",
                amountMl = 200,
                beverageType = BeverageType.WATER,
                recordedAt = Instant.ofEpochMilli(2000),
                sourceDevice = SourceDevice.MOBILE,
                isSyncedToHealthConnect = false
            )
        )

        viewModel.uiState.test {
            awaitItem() // 初期状態
            todayRecordsFlow.value = records
            val state = awaitItem()
            assertThat(state.todayRecords).hasSize(2)
            assertThat(state.todayRecords[0].id).isEqualTo("2") // 新しい記録が先
            assertThat(state.todayRecords[1].id).isEqualTo("1")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
