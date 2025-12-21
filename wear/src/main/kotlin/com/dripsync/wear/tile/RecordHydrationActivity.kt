package com.dripsync.wear.tile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
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

                    Toast.makeText(
                        this@RecordHydrationActivity,
                        "${amountMl}ml を記録しました",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@RecordHydrationActivity,
                        "記録に失敗しました",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    finish()
                }
            }
        } else {
            finish()
        }
    }
}
