package com.saidcharoun.tahaddisighar

import android.content.Context

object AchievementManager {

    private const val PREFS = "achievements"

    val achievements = listOf(
        Achievement("first_win", "أول انتصار 🏆", "أكمل أول مرحلة", { s -> s.getInt("stages_completed", 0) >= 1 }),
        Achievement("perfect", "مثالي 💯", "اجب على كل أسئلة مرحلة بدون خطأ", { s -> s.getInt("perfect_stages", 0) >= 1 }),
        Achievement("coins_100", "جامع العملات 🪙", "اجمع 100 قطعة نقدية", { s -> s.getInt("coins_collected", 0) >= 100 }),
        Achievement("streak_7", "سلسلة أسبوع 🔥", "حقق سلسلة 7 أيام متتالية", { s -> s.getInt("streak_count", 0) >= 7 }),
        Achievement("time_10", "متسابق الوقت ⏱️", "العب 10 جولات من وضع الوقت", { s -> s.getInt("time_attack_plays", 0) >= 10 }),
        Achievement("survivor", "الناجي 🛡️", "اجب على 30 سؤالاً متتالياً في وضع البقاء", { s -> s.getInt("survival_streak", 0) >= 30 }),
        Achievement("family_champ", "بطل العائلة 👨‍👩‍👧‍👦", "افزع 5 مرات في وضع العائلة", { s -> s.getInt("family_wins", 0) >= 5 }),
        Achievement("categories_3", "مستكشف 📚", "العب في 3 فئات مختلفة", { s -> s.getInt("categories_played", 0) >= 3 }),
        Achievement("total_500", "أسطورة النقاط 💎", "اجمع 500 نقطة إجمالية", { s -> s.getInt("total_score", 0) >= 500 }),
        Achievement("daily_30", "المواظب 📆", "أكمل 30 تحدياً يومياً", { s -> s.getInt("daily_count", 0) >= 30 })
    )

    fun init(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun isUnlocked(context: Context, id: String): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(id, false)
    }

    fun markAchievement(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(id, true).apply()
    }

    fun checkAndUnlock(context: Context, key: String, value: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(key, prefs.getInt(key, 0) + value).apply()
        for (a in achievements) {
            if (!prefs.getBoolean(a.id, false) && a.condition(prefs)) {
                prefs.edit().putBoolean(a.id, true).apply()
            }
        }
    }

    fun checkAndUnlockSet(context: Context, key: String, value: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt(key, 0)
        if (value > current) {
            prefs.edit().putInt(key, value).apply()
        }
        for (a in achievements) {
            if (!prefs.getBoolean(a.id, false) && a.condition(prefs)) {
                prefs.edit().putBoolean(a.id, true).apply()
            }
        }
    }

    fun markForced(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(id, true).apply()
    }
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val condition: (android.content.SharedPreferences) -> Boolean
)
