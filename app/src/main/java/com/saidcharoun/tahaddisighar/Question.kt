package com.saidcharoun.tahaddisighar

/**
 * سؤال واحد في اللعبة.
 * @param emoji رمز تعبيري يظهر فوق السؤال لجذب الطفل.
 * @param text نص السؤال.
 * @param options الخيارات الأربعة.
 * @param correctIndex رقم الخيار الصحيح (يبدأ من 0).
 * @param category تصنيف السؤال (حيوانات، ألوان...).
 */
data class Question(
    val emoji: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val category: String
)
