package com.dripsync.wear.tile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.wear.tiles.TileService
import androidx.wear.widget.ConfirmationOverlay
import com.dripsync.shared.data.model.SourceDevice
import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.wear.complication.HydrationComplicationService
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

                    // コンプリケーションを更新
                    HydrationComplicationService.requestUpdate(this@RecordHydrationActivity)

                    showConfirmation(
                        ConfirmationOverlay.SUCCESS_ANIMATION,
                        "+${amountMl}ml"
                    )
                } catch (e: Exception) {
                    showConfirmation(
                        ConfirmationOverlay.FAILURE_ANIMATION,
                        "記録失敗"
                    )
                }
            }
        } else {
            finish()
        }
    }

    private fun showConfirmation(type: Int, message: String) {
        ConfirmationOverlay()
            .setType(type)
            .setMessage(message)
            .setOnAnimationFinishedListener { finish() }
            .showOn(this)
    }
}
