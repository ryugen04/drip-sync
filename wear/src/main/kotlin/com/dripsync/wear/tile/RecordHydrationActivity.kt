package com.dripsync.wear.tile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.wear.activity.ConfirmationActivity
import androidx.wear.tiles.TileService
import com.dripsync.shared.data.model.SourceDevice
import com.dripsync.shared.data.repository.HydrationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * タイルからの水分記録を処理するActivity
 * 記録完了後すぐに終了する
 */
@AndroidEntryPoint
class RecordHydrationActivity : ComponentActivity() {

    @Inject
    lateinit var hydrationRepository: HydrationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val amountMl = intent.getIntExtra(HydrationTileService.EXTRA_AMOUNT_ML, 0)

        if (amountMl > 0) {
            lifecycleScope.launch {
                try {
                    hydrationRepository.recordHydration(
                        amountMl = amountMl,
                        sourceDevice = SourceDevice.WEAR
                    )
                    // タイルを更新
                    TileService.getUpdater(this@RecordHydrationActivity)
                        .requestUpdate(HydrationTileService::class.java)

                    showConfirmation(
                        ConfirmationActivity.SUCCESS_ANIMATION,
                        "+${amountMl}ml"
                    )
                } catch (e: Exception) {
                    showConfirmation(
                        ConfirmationActivity.FAILURE_ANIMATION,
                        "記録失敗"
                    )
                }
            }
        } else {
            finish()
        }
    }

    private fun showConfirmation(animationType: Int, message: String) {
        val intent = Intent(this, ConfirmationActivity::class.java).apply {
            putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, animationType)
            putExtra(ConfirmationActivity.EXTRA_MESSAGE, message)
        }
        startActivity(intent)
        finish()
    }
}
