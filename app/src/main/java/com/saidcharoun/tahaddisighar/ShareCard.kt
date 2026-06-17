package com.saidcharoun.tahaddisighar

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * ينشئ صورة نتيجة جميلة ويشاركها عبر تطبيقات التواصل (Sharesheet).
 * الهدف: يفتخر الطفل/الأهل بالنتيجة → دعاية مجانية للعبة.
 */
object ShareCard {

    // ⚠️ بعد نشر اللعبة على Google Play استبدل هذا الرابط برابط متجرك
    private const val PLAY_LINK =
        "https://play.google.com/store/apps/details?id=com.saidcharoun.tahaddisighar"

    /** مشاركة التقدّم الحالي (وصلت إلى المرحلة كذا) في أي وقت. */
    fun shareProgress(activity: Activity, stageReached: Int, totalStages: Int, score: Int) {
        val title = "وصلت إلى المرحلة $stageReached من $totalStages! 🚀"
        val sub = "نقاطي: $score"
        renderAndSend(activity, title, sub, "تحدّني في لعبة تحدي الصغار!")
    }

    fun share(activity: Activity, score: Int, total: Int, ageLabel: String) {
        val title = if (score >= total) "أنهيت كل المراحل! 🏆" else "نتيجتي: $score / $total"
        renderAndSend(activity, title, "الفئة: $ageLabel", "هل تستطيع التغلّب علي؟ 😄")
    }

    private fun renderAndSend(activity: Activity, line1: String, line2: String, line3: String) {
        val size = 1080
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // خلفية متدرّجة بنفسجية
        p.shader = LinearGradient(
            0f, 0f, 0f, size.toFloat(),
            Color.parseColor("#7B1FA2"), Color.parseColor("#4A148C"),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
        p.shader = null

        // إطار ذهبي
        p.style = Paint.Style.STROKE
        p.strokeWidth = 14f
        p.color = Color.parseColor("#FFC107")
        c.drawRoundRect(40f, 40f, size - 40f, size - 40f, 40f, 40f, p)
        p.style = Paint.Style.FILL

        p.textAlign = Paint.Align.CENTER

        // النجمة
        p.color = Color.WHITE
        p.textSize = 250f
        c.drawText("⭐", size / 2f, 330f, p)

        // اسم اللعبة
        p.color = Color.parseColor("#FFE082")
        p.textSize = 96f
        p.isFakeBoldText = true
        c.drawText("تحدي الصغار", size / 2f, 470f, p)

        // السطر الأول (العنوان الرئيسي)
        p.color = Color.WHITE
        p.textSize = 96f
        c.drawText(line1, size / 2f, 650f, p)

        // السطر الثاني
        p.color = Color.parseColor("#FFD54F")
        p.textSize = 64f
        p.isFakeBoldText = false
        c.drawText(line2, size / 2f, 770f, p)

        // السطر الثالث (دعوة)
        p.color = Color.WHITE
        p.textSize = 56f
        c.drawText(line3, size / 2f, 880f, p)
        p.color = Color.parseColor("#FFE082")
        p.textSize = 48f
        c.drawText("حمّلها والعب معي! 🎮", size / 2f, 970f, p)

        // حفظ ومشاركة
        try {
            val dir = File(activity.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, "result.png")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

            val uri = FileProvider.getUriForFile(
                activity, "${activity.packageName}.fileprovider", file
            )
            val text = "🌟 $line1 في لعبة (تحدي الصغار)!\n" +
                "$line3\n$PLAY_LINK\n#تحدي_الصغار #ألعاب_أطفال #تعليم"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, "مشاركة النتيجة"))
        } catch (_: Exception) {
        }
    }
}
