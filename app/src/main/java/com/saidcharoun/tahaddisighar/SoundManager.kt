package com.saidcharoun.tahaddisighar

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * نظام صوت ناعم وممتع — يولّد نغمات موسيقية بموجات جيبية مع تلاشٍ تدريجي
 * (بدلاً من أزيز النظام المزعج). + اهتزاز خفيف عند الخطأ.
 */
object SoundManager {

    var enabled: Boolean = true

    private const val SR = 44100
    private val exec = Executors.newSingleThreadExecutor()
    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        @Suppress("DEPRECATION")
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // توليد نغمة واحدة (تردد، مدة بالملي ثانية) مع تلاشٍ ناعم لمنع الطقطقة
    private fun tone(freq: Double, ms: Int, volume: Double): ShortArray {
        val n = SR * ms / 1000
        val out = ShortArray(n)
        val fade = (SR * 0.010).toInt().coerceAtLeast(1) // 10ms
        for (i in 0 until n) {
            var amp = volume
            if (i < fade) amp *= i.toDouble() / fade
            if (i > n - fade) amp *= (n - i).toDouble() / fade
            val s = sin(2.0 * PI * freq * i / SR) * amp
            out[i] = (s * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    // دمج عدة نغمات في لحن واحد
    private fun melody(vararg notes: Pair<Double, Int>, volume: Double = 0.5): ShortArray {
        val parts = notes.map { tone(it.first, it.second, volume) }
        val total = parts.sumOf { it.size }
        val out = ShortArray(total)
        var p = 0
        for (part in parts) {
            System.arraycopy(part, 0, out, p, part.size)
            p += part.size
        }
        return out
    }

    private fun play(samples: ShortArray) {
        if (!enabled) return
        exec.execute {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SR)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep(samples.size * 1000L / SR + 60)
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }
    }

    /** نقرة خفيفة عند الضغط. */
    fun click() = play(melody(880.0 to 40, volume = 0.22))

    /** نغمة فرح صاعدة عند الإجابة الصحيحة. */
    fun correct() = play(melody(784.0 to 90, 1046.0 to 150, volume = 0.45))

    /** نغمة هابطة ناعمة (غير مزعجة) عند الخطأ + اهتزاز خفيف. */
    fun wrong() {
        play(melody(330.0 to 130, 247.0 to 170, volume = 0.4))
        vibrate(180)
    }

    /** فقدان قلب. */
    fun lifeLost() = play(melody(392.0 to 120, volume = 0.35))

    /** لحن انتصار صاعد عند اجتياز المرحلة / الفوز. */
    fun win() = play(
        melody(523.0 to 130, 659.0 to 130, 784.0 to 130, 1046.0 to 280, volume = 0.5)
    )

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
