package com.xbit.proton.engine

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Log-linear KCET rank predictor that mirrors the original JS implementation.
 * Converts raw KCET marks (0–180) to an estimated rank using an anchor table
 * and log-linear interpolation between anchor points.
 */
object KcetRankPredictor {

    // Anchor table: marks -> typical rank (Karnataka state, general engineering)
    // Source: historical KCET data
    private val anchors = listOf(
        180 to 1,
        175 to 50,
        170 to 150,
        165 to 350,
        160 to 700,
        155 to 1_200,
        150 to 2_000,
        145 to 3_100,
        140 to 4_600,
        135 to 6_500,
        130 to 9_000,
        125 to 12_000,
        120 to 16_000,
        115 to 21_000,
        110 to 27_000,
        105 to 34_000,
        100 to 42_000,
        95  to 51_000,
        90  to 61_000,
        85  to 72_000,
        80  to 84_000,
        75  to 97_000,
        70  to 110_000,
        60  to 135_000,
        50  to 155_000,
        40  to 168_000,
        0   to 180_000
    )

    /**
     * Estimate rank from KCET total marks (Physics + Chemistry + Mathematics, out of 180).
     * Returns a rounded integer rank.
     */
    fun estimateRank(marks: Double): Int {
        if (marks >= 180) return 1
        if (marks <= 0) return 180_000

        // Find surrounding anchor pair
        for (i in 0 until anchors.size - 1) {
            val (m1, r1) = anchors[i]
            val (m2, r2) = anchors[i + 1]
            if (marks <= m1 && marks >= m2) {
                // Log-linear interpolation
                val t = (m1 - marks) / (m1 - m2).toDouble()
                val logR = ln(r1.toDouble()) + t * (ln(r2.toDouble()) - ln(r1.toDouble()))
                return exp(logR).roundToInt().coerceIn(1, 180_000)
            }
        }
        return 180_000
    }

    /**
     * Convert board percentage + board name to equivalent marks for inclusion
     * in the KCET rank formula (KCET uses only KCET marks for rank, but
     * 10th/12th % is used for tie-breaking; this helper normalises it).
     */
    fun boardPercentToKcetEquiv(percent: Double, board: String): Double {
        // Karnataka board: direct scale to /180 equivalent
        // CBSE/ISC: apply a slight deflation factor (empirical)
        val factor = when {
            board.contains("cbse", ignoreCase = true) -> 0.95
            board.contains("isc", ignoreCase = true)  -> 0.93
            else -> 1.0
        }
        return (percent / 100.0) * 180.0 * factor
    }
}
