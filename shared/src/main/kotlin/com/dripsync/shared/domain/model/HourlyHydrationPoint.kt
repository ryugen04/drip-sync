package com.dripsync.shared.domain.model

import java.time.LocalTime

/**
 * 時間ごとの水分摂取データポイント
 *
 * グラフ描画用に理想値と実績値を持つ
 */
data class HourlyHydrationPoint(
    val time: LocalTime,
    val actualCumulative: Int,
    val idealCumulative: Int
) {
    /**
     * 理想値に対する実績の割合（0.0〜1.0+）
     */
    val ratio: Float
        get() = if (idealCumulative > 0) {
            actualCumulative.toFloat() / idealCumulative
        } else {
            0f
        }

    /**
     * 理想値との差分（正: 超過、負: 不足）
     */
    val difference: Int
        get() = actualCumulative - idealCumulative

    /**
     * 理想を上回っているか
     */
    val isAboveIdeal: Boolean
        get() = actualCumulative >= idealCumulative
}
