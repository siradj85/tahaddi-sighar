package com.saidcharoun.tahaddisighar

import android.content.Context

object LeaderboardManager {

    private const val PREFS = "leaderboard"

    private fun getScoresRaw(context: Context, mode: String): MutableList<LeaderboardEntry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(mode, "") ?: ""
        if (raw.isBlank()) return mutableListOf()
        return raw.split("|").mapNotNull { part ->
            val parts = part.split(",")
            if (parts.size == 2) {
                val score = parts[0].toIntOrNull() ?: return@mapNotNull null
                val date = parts[1]
                LeaderboardEntry(score, date)
            } else null
        }.sortedByDescending { it.score }.toMutableList()
    }

    private fun saveScoresRaw(context: Context, mode: String, entries: List<LeaderboardEntry>) {
        val raw = entries.joinToString("|") { "${it.score},${it.date}" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(mode, raw).apply()
    }

    fun addScore(context: Context, mode: String, score: Int) {
        val entries = getScoresRaw(context, mode)
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        entries.add(LeaderboardEntry(score, today))
        val top5 = entries.sortedByDescending { it.score }.take(5)
        saveScoresRaw(context, mode, top5)
    }

    fun getScores(context: Context, mode: String): List<LeaderboardEntry> {
        return getScoresRaw(context, mode).sortedByDescending { it.score }
    }

    fun clearScores(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(mode).apply()
    }
}

data class LeaderboardEntry(val score: Int, val date: String)
