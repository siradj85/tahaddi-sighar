package com.saidcharoun.tahaddisighar

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * أصوات ومؤثرات اللعبة باستخدام نغمات النظام (بدون ملفات صوت خارجية)
 * + اهتزاز عند الخطأ. يمكن إيقاف الصوت من زر في الواجهة.
 */
object SoundManager {

    private var tone: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    var enabled: Boolean = true

    fun init(context: Context) {
        try {
            tone = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
        } catch (_: Exception) {
        }
        @Suppress("DEPRECATION")
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun play(type: Int, durationMs: Int) {
        if (!enabled) return
        try {
            tone?.startTone(type, durationMs)
        } catch (_: Exception) {
        }
    }

    fun click() = play(ToneGenerator.TONE_PROP_BEEP, 60)

    fun correct() = play(ToneGenerator.TONE_PROP_BEEP2, 160)

    fun wrong() {
        play(ToneGenerator.TONE_SUP_ERROR, 250)
        vibrate(220)
    }

    fun lifeLost() = play(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)

    /** لحن فرح قصير عند الفوز / اجتياز المرحلة. */
    fun win() {
        if (!enabled) return
        val melody = listOf(
            ToneGenerator.TONE_PROP_BEEP,
            ToneGenerator.TONE_PROP_BEEP2,
            ToneGenerator.TONE_PROP_ACK,
            ToneGenerator.TONE_PROP_BEEP2
        )
        melody.forEachIndexed { i, t ->
            handler.postDelayed({ play(t, 180) }, i * 200L)
        }
    }

    private fun vibrate(ms: Long) {
        if (!enabled) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (_: Exception) {
        }
    }
}
