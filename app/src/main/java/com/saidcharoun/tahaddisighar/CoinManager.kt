package com.saidcharoun.tahaddisighar

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object CoinManager {

    private const val PREFS = "coins"
    private const val KEY_BALANCE = "balance"
    private const val KEY_DAILY_DATE = "daily_date"
    private const val KEY_STREAK = "streak"
    private const val KEY_LAST_STREAK_DATE = "last_streak_date"
    private const val KEY_TOTAL_DAILY = "total_daily"

    fun getBalance(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_BALANCE, 0)
    }

    fun addCoins(context: Context, amount: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_BALANCE, 0)
        prefs.edit().putInt(KEY_BALANCE, current + amount).apply()
    }

    fun spendCoins(context: Context, amount: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_BALANCE, 0)
        if (current < amount) return false
        prefs.edit().putInt(KEY_BALANCE, current - amount).apply()
        return true
    }

    fun getDailyDate(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DAILY_DATE, "") ?: ""
    }

    fun setDailyDate(context: Context, date: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DAILY_DATE, date).apply()
    }

    fun getStreak(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_STREAK, 0)
    }

    fun getLastStreakDate(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_STREAK_DATE, "") ?: ""
    }

    fun recordDailyPlay(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val lastDate = prefs.getString(KEY_LAST_STREAK_DATE, "") ?: ""
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val currentStreak = prefs.getInt(KEY_STREAK, 0)

        val newStreak = if (lastDate == yesterday) currentStreak + 1 else 1
        prefs.edit()
            .putInt(KEY_STREAK, newStreak)
            .putString(KEY_LAST_STREAK_DATE, today)
            .putInt(KEY_TOTAL_DAILY, prefs.getInt(KEY_TOTAL_DAILY, 0) + 1)
            .apply()
    }

    fun getTotalDailyCompleted(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_TOTAL_DAILY, 0)
    }

    fun streakBonusCoins(context: Context): Int {
        val streak = getStreak(context)
        return when {
            streak >= 30 -> 15
            streak >= 14 -> 10
            streak >= 7 -> 7
            streak >= 3 -> 5
            else -> 3
        }
    }
}
