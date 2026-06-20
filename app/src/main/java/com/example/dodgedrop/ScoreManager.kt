package com.example.dodgedrop

import android.content.Context

/**
 * Persists the high score locally. Seeing "NEW BEST!" or "so close to your record"
 * is one of the strongest, cheapest retention hooks in arcade games — it gives the
 * player a personal goal that's always one tap away.
 */
class ScoreManager(context: Context) {

    private val prefs = context.getSharedPreferences("dodge_drop_prefs", Context.MODE_PRIVATE)

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(value) {
            prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()
        }

    var totalRuns: Int
        get() = prefs.getInt(KEY_TOTAL_RUNS, 0)
        set(value) {
            prefs.edit().putInt(KEY_TOTAL_RUNS, value).apply()
        }

    fun incrementRuns() {
        totalRuns += 1
    }

    /** Returns true if this score beat the previous high score. */
    fun submitScore(score: Int): Boolean {
        val isNewBest = score > highScore
        if (isNewBest) {
            highScore = score
        }
        return isNewBest
    }

    companion object {
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_TOTAL_RUNS = "total_runs"
    }
}
