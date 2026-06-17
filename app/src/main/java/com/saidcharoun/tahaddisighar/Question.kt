package com.saidcharoun.tahaddisighar

/**
 * سؤال واحد في اللعبة.
 * @param emoji رمز تعبيري يظهر فوق السؤال.
 * @param text نص السؤال.
 * @param options الخيارات (٣ أو ٤).
 * @param correctIndex رقم الخيار الصحيح (يبدأ من 0).
 * @param age الفئة العمرية: 1 = صغار (٣-٥)، 2 = كبار (٦-٨).
 * @param difficulty الصعوبة: 1 سهل، 2 متوسط، 3 صعب.
 */
data class Question(
    val emoji: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val age: Int,
    val difficulty: Int
)

enum class AgeGroup(val code: Int, val label: String, val emoji: String) {
    YOUNG(1, "٣ - ٥ سنوات", "🧸"),
    OLDER(2, "٦ - ٨ سنوات", "🎒")
}
