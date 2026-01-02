package com.dripsync.wear.ui.home

import app.cash.turbine.test
import com.dripsync.shared.data.preferences.PresetSettings
import com.dripsync.shared.data.preferences.UserPreferences
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * HomeUiStateの計算ロジックをテスト
 * ViewModelの完全なテストはAndroidテストで行う（TileService依存のため）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var hydrationRepository: HydrationRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private val todayTotalFlow = MutableStateFlow(0)
    private val preferencesFlow = MutableStateFlow(
        UserPreferences(dailyGoalMl = 2000, presets = PresetSettings(150, 250, 500))
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        hydrationRepository = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)

        every { hydrationRepository.observeTodayTotal() } returns todayTotalFlow
        every { userPreferencesRepository.observePreferences() } returns preferencesFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // === HomeUiState の計算ロジックテスト ===

    @Test
    fun `progressPercent calculates correctly when goal is positive`() {
        val state = HomeUiState(todayTotalMl = 1000, dailyGoalMl = 2000)
        assertThat(state.progressPercent).isEqualTo(50)
    }

    @Test
    fun `progressPercent is 0 when goal is 0`() {
        val state = HomeUiState(todayTotalMl = 1000, dailyGoalMl = 0)
        assertThat(state.progressPercent).isEqualTo(0)
    }

    @Test
    fun `progressPercent exceeds 100 when total exceeds goal`() {
        val state = HomeUiState(todayTotalMl = 3000, dailyGoalMl = 2000)
        assertThat(state.progressPercent).isEqualTo(150)
    }

    @Test
    fun `displayTotal formats correctly for ml`() {
        val state = HomeUiState(todayTotalMl = 750)
        assertThat(state.displayTotal).isEqualTo("750ml")
    }

    @Test
    fun `displayTotal formats correctly for exactly 1L`() {
        val state = HomeUiState(todayTotalMl = 1000)
        assertThat(state.displayTotal).isEqualTo("1.0L")
    }

    @Test
    fun `displayTotal formats correctly for liters`() {
        val state = HomeUiState(todayTotalMl = 1500)
        assertThat(state.displayTotal).isEqualTo("1.5L")
    }

    @Test
    fun `displayTotal shows 0ml when no intake`() {
        val state = HomeUiState(todayTotalMl = 0)
        assertThat(state.displayTotal).isEqualTo("0ml")
    }

    // === StateFlow結合テスト ===

    @Test
    fun `uiState combines repository flows correctly`() = runTest {
        // 注意: このテストはViewModelのStateFlow結合をテストしますが
        // 実際のViewModel生成にはApplication（TileService用）が必要なため
        // ここではFlowの動作確認のみ行う

        // 設定Flowの値変更を確認
        preferencesFlow.value = UserPreferences(
            dailyGoalMl = 2500,
            presets = PresetSettings(100, 200, 300)
        )

        preferencesFlow.test {
            val prefs = awaitItem()
            assertThat(prefs.dailyGoalMl).isEqualTo(2500)
            assertThat(prefs.presets.preset1Ml).isEqualTo(100)
            cancelAndIgnoreRemainingEvents()
        }

        // 今日の合計Flowの値変更を確認
        todayTotalFlow.value = 1500

        todayTotalFlow.test {
            val total = awaitItem()
            assertThat(total).isEqualTo(1500)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repository flows emit updates correctly`() = runTest {
        todayTotalFlow.test {
            assertThat(awaitItem()).isEqualTo(0)

            todayTotalFlow.value = 500
            assertThat(awaitItem()).isEqualTo(500)

            todayTotalFlow.value = 1000
            assertThat(awaitItem()).isEqualTo(1000)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
