package com.dripsync.shared.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * プリセット設定（3つの水分量ボタン）
 */
data class PresetSettings(
    val preset1Ml: Int = 200,
    val preset2Ml: Int = 350,
    val preset3Ml: Int = 500
) {
    fun toList(): List<Int> = listOf(preset1Ml, preset2Ml, preset3Ml)
}

/**
 * ユーザー設定（目標摂取量など）
 */
data class UserPreferences(
    val dailyGoalMl: Int = 2000,
    val presets: PresetSettings = PresetSettings()
)

interface UserPreferencesRepository {
    fun observePreferences(): Flow<UserPreferences>
    fun observePresets(): Flow<PresetSettings>
    fun observeDailyGoal(): Flow<Int>

    suspend fun getPreferences(): UserPreferences
    suspend fun getPresets(): PresetSettings

    suspend fun updatePresets(preset1Ml: Int, preset2Ml: Int, preset3Ml: Int)
    suspend fun updatePreset(index: Int, amountMl: Int)
    suspend fun updateDailyGoal(goalMl: Int)
}

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal_ml")
        val PRESET_1 = intPreferencesKey("preset_1_ml")
        val PRESET_2 = intPreferencesKey("preset_2_ml")
        val PRESET_3 = intPreferencesKey("preset_3_ml")
    }

    override fun observePreferences(): Flow<UserPreferences> {
        return context.dataStore.data.map { prefs ->
            UserPreferences(
                dailyGoalMl = prefs[Keys.DAILY_GOAL] ?: 2000,
                presets = PresetSettings(
                    preset1Ml = prefs[Keys.PRESET_1] ?: 200,
                    preset2Ml = prefs[Keys.PRESET_2] ?: 350,
                    preset3Ml = prefs[Keys.PRESET_3] ?: 500
                )
            )
        }
    }

    override fun observePresets(): Flow<PresetSettings> {
        return context.dataStore.data.map { prefs ->
            PresetSettings(
                preset1Ml = prefs[Keys.PRESET_1] ?: 200,
                preset2Ml = prefs[Keys.PRESET_2] ?: 350,
                preset3Ml = prefs[Keys.PRESET_3] ?: 500
            )
        }
    }

    override fun observeDailyGoal(): Flow<Int> {
        return context.dataStore.data.map { prefs ->
            prefs[Keys.DAILY_GOAL] ?: 2000
        }
    }

    override suspend fun getPreferences(): UserPreferences {
        var result = UserPreferences()
        context.dataStore.data.collect { prefs ->
            result = UserPreferences(
                dailyGoalMl = prefs[Keys.DAILY_GOAL] ?: 2000,
                presets = PresetSettings(
                    preset1Ml = prefs[Keys.PRESET_1] ?: 200,
                    preset2Ml = prefs[Keys.PRESET_2] ?: 350,
                    preset3Ml = prefs[Keys.PRESET_3] ?: 500
                )
            )
            return@collect
        }
        return result
    }

    override suspend fun getPresets(): PresetSettings {
        var result = PresetSettings()
        context.dataStore.data.collect { prefs ->
            result = PresetSettings(
                preset1Ml = prefs[Keys.PRESET_1] ?: 200,
                preset2Ml = prefs[Keys.PRESET_2] ?: 350,
                preset3Ml = prefs[Keys.PRESET_3] ?: 500
            )
            return@collect
        }
        return result
    }

    override suspend fun updatePresets(preset1Ml: Int, preset2Ml: Int, preset3Ml: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRESET_1] = preset1Ml
            prefs[Keys.PRESET_2] = preset2Ml
            prefs[Keys.PRESET_3] = preset3Ml
        }
    }

    override suspend fun updatePreset(index: Int, amountMl: Int) {
        context.dataStore.edit { prefs ->
            when (index) {
                0 -> prefs[Keys.PRESET_1] = amountMl
                1 -> prefs[Keys.PRESET_2] = amountMl
                2 -> prefs[Keys.PRESET_3] = amountMl
            }
        }
    }

    override suspend fun updateDailyGoal(goalMl: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_GOAL] = goalMl
        }
    }
}
