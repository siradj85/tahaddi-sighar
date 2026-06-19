package com.saidcharoun.tahaddisighar

import android.content.Context

object CoinManager {

    private const val PREFS = "coins"
    private const val KEY_BALANCE = "balance"
    private const val KEY_DAILY_DATE = "daily_date"

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
}
