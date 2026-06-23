package com.saidcharoun.tahaddisighar

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * يدير المؤثرات الصوتية والموسيقى.
 *
 * يبحث أولاً عن ملفات صوت حقيقية في res/raw بالأسماء التالية:
 *   snd_correct, snd_wrong, snd_win, snd_click, snd_life_lost  (مؤثرات قصيرة .ogg/.mp3/.wav)
 *   snd_music  (موسيقى خلفية قابلة للتكرار)
 * فإن لم يجدها، يولّد نغمات بسيطة برمجياً (احتياطي) — فيبقى التطبيق يعمل بلا أي ملفات.
 * يكفي وضع الملفات في app/src/main/res/raw/ بهذه الأسماء لتُستخدم تلقائياً.
 */
object SoundManager {

    var enabled: Boolean = true
    var musicOn: Boolean = false

    private const val SR = 44100
    private val exec = Executors.newSingleThreadExecutor()
    private var vibrator: Vibrator? = null

    // الصوت من ملفات res/raw (إن وُجدت)
    private var appContext: Context? = null
    private var soundPool: SoundPool? = null
    private val sampleIds = mutableMapOf<String, Int>()   // اسم المورد -> معرّف SoundPool
    private var musicResId: Int = 0
    private var musicPlayer: MediaPlayer? = null

    // الموسيقى المولّدة احتياطياً
    private var genMusicPlaying = false
    private var genMusicThread: Thread? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        @Suppress("DEPRECATION")
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()

        val ctx = appContext ?: return
        for (name in listOf("snd_correct", "snd_wrong", "snd_win", "snd_click", "snd_life_lost")) {
            val resId = ctx.resources.getIdentifier(name, "raw", ctx.packageName)
            if (resId != 0) {
                val id = soundPool?.load(ctx, resId, 1) ?: 0
                if (id != 0) sampleIds[name] = id
            }
        }
        musicResId = ctx.resources.getIdentifier("snd_music", "raw", ctx.packageName)
    }

    // ---------- المؤثرات ----------
    fun click() = playSample("snd_click") { melody(880.0 to 40, volume = 0.22) }

    fun correct() = playSample("snd_correct") { melody(784.0 to 90, 1046.0 to 150, volume = 0.45) }

    fun wrong() {
        playSample("snd_wrong") { melody(330.0 to 130, 247.0 to 170, volume = 0.4) }
        vibrate(180)
    }

    fun lifeLost() = playSample("snd_life_lost") { melody(392.0 to 120, volume = 0.35) }

    fun win() = playSample("snd_win") {
        melody(523.0 to 130, 659.0 to 130, 784.0 to 130, 1046.0 to 280, volume = 0.5)
    }

    private fun playSample(name: String, fallback: () -> ShortArray) {
        if (!enabled) return
        val id = sampleIds[name]
        if (id != null) {
            soundPool?.play(id, 1f, 1f, 1, 0, 1f)
        } else {
            playGenerated(fallback())
        }
    }

    // ---------- الموسيقى ----------
    fun toggleMusic() {
        musicOn = !musicOn
        if (musicOn) startMusic() else stopMusic()
    }

    fun startMusic() {
        val ctx = appContext
        if (musicResId != 0 && ctx != null) {
            if (musicPlayer != null) return
            try {
                musicPlayer = MediaPlayer.create(ctx, musicResId)?.apply {
                    isLooping = true
                    setVolume(0.5f, 0.5f)
                    start()
                }
            } catch (_: Exception) {
            }
        } else {
            startGeneratedMusic()
        }
    }

    fun stopMusic() {
        try {
            musicPlayer?.stop()
            musicPlayer?.release()
        } catch (_: Exception) {
        }
        musicPlayer = null
        stopGeneratedMusic()
    }

    // ---------- الموسيقى المولّدة (احتياطي) ----------
    private fun startGeneratedMusic() {
        if (genMusicPlaying) return
        genMusicPlaying = true
        genMusicThread = Thread {
            val melody = generateMelody(30.0)
            val totalSamples = melody.size
            while (genMusicPlaying) {
                try {
                    val track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setSampleRate(SR)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(totalSamples * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                    track.write(melody, 0, totalSamples)
                    track.play()
                    Thread.sleep((totalSamples * 1000L / SR) + 100)
                    track.stop()
                    track.release()
                } catch (_: Exception) {
                }
            }
        }.apply { isDaemon = true; start() }
    }

    private fun stopGeneratedMusic() {
        genMusicPlaying = false
        genMusicThread?.interrupt()
        genMusicThread = null
    }

    private fun generateMelody(durationSeconds: Double): ShortArray {
        val notes = listOf(
            262.0 to 0.3, 294.0 to 0.3, 330.0 to 0.3, 349.0 to 0.3,
            392.0 to 0.3, 349.0 to 0.3, 330.0 to 0.3, 294.0 to 0.3,
            262.0 to 0.6, 330.0 to 0.3, 392.0 to 0.3, 440.0 to 0.3,
            392.0 to 0.3, 330.0 to 0.3, 294.0 to 0.3, 262.0 to 0.6,
            294.0 to 0.3, 330.0 to 0.3, 349.0 to 0.6, 330.0 to 0.3,
            294.0 to 0.3, 262.0 to 0.3, 330.0 to 0.3, 392.0 to 0.6,
            349.0 to 0.3, 330.0 to 0.3, 294.0 to 0.3, 262.0 to 0.3,
            294.0 to 0.3, 330.0 to 0.3, 262.0 to 0.6
        )
        val noteDurationMs = (durationSeconds * 1000 / notes.size).toInt()
        val totalSamples = (SR * durationSeconds).toInt()
        val out = ShortArray(totalSamples)
        val fade = (SR * 0.008).toInt().coerceAtLeast(1)
        var pos = 0
        for ((freq, vol) in notes) {
            val n = SR * noteDurationMs / 1000
            for (i in 0 until n) {
                if (pos >= totalSamples) break
                var amp = vol * 0.08
                if (i < fade) amp *= i.toDouble() / fade
                if (i > n - fade) amp *= (n - i).toDouble() / fade
                val s = sin(2.0 * PI * freq * i / SR) * amp
                out[pos++] = (s * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }

    private fun tone(freq: Double, ms: Int, volume: Double): ShortArray {
        val n = SR * ms / 1000
        val out = ShortArray(n)
        val fade = (SR * 0.010).toInt().coerceAtLeast(1)
        for (i in 0 until n) {
            var amp = volume
            if (i < fade) amp *= i.toDouble() / fade
            if (i > n - fade) amp *= (n - i).toDouble() / fade
            val s = sin(2.0 * PI * freq * i / SR) * amp
            out[i] = (s * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

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

    private fun playGenerated(samples: ShortArray) {
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
