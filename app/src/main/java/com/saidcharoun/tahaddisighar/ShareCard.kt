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

    fun share(activity: Activity, score: Int, total: Int, ageLabel: String) {
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

        // النتيجة
        p.color = Color.WHITE
        p.textSize = 130f
        c.drawText("نتيجتي: $score / $total", size / 2f, 650f, p)

        // رسالة
        p.color = Color.parseColor("#FFD54F")
        p.textSize = 70f
        p.isFakeBoldText = false
        val msg = if (score >= total) "أنهيت كل المراحل! 🏆" else "هل تستطيع التغلّب علي؟"
        c.drawText(msg, size / 2f, 780f, p)

        // العمر + دعوة
        p.color = Color.WHITE
        p.textSize = 52f
        c.drawText("الفئة: $ageLabel", size / 2f, 880f, p)
        p.color = Color.parseColor("#FFE082")
        c.drawText("حمّلها والعب معي! 🎮", size / 2f, 970f, p)

        // حفظ ومشاركة
        try {
            val dir = File(activity.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, "result.png")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

            val uri = FileProvider.getUriForFile(
                activity, "${activity.packageName}.fileprovider", file
            )
            val text = "🌟 حصلت على $score من $total في لعبة (تحدي الصغار)!\n" +
                "هل تستطيع التغلّب علي؟ 😄\n$PLAY_LINK\n#تحدي_الصغار #ألعاب_أطفال"

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
