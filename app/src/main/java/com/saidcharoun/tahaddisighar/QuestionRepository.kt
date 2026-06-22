package com.saidcharoun.tahaddisighar

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * مصدر الأسئلة: يحمّلها عن بُعد من GitHub Pages (مجاني) ويخزّنها محلياً.
 * - load(): يُعيد الأسئلة فوراً من الكاش، وإلا المدمجة في [QuestionBank] (يعمل بلا إنترنت).
 * - refreshInBackground(): يجلب أحدث نسخة ويحدّث الكاش إذا تغيّر رقم الإصدار.
 *
 * لإضافة أسئلة جديدة لكل المستخدمين: عدّل docs/questions.json وارفع رقم "version"
 * ثم ادفعه إلى GitHub — يصل التحديث دون تحديث التطبيق ودون أي تكلفة.
 */
object QuestionRepository {

    private const val REMOTE_URL =
        "https://siradj85.github.io/tahaddi-sighar/questions.json"
    private const val PREFS = "remote_questions"
    private const val KEY_JSON = "json"
    private const val KEY_VERSION = "version"

    private val exec = Executors.newSingleThreadExecutor()

    @Volatile
    private var cached: List<Question>? = null

    /** يُعيد الأسئلة فوراً (كاش محلي أو المدمجة احتياطياً). */
    fun load(context: Context): List<Question> {
        cached?.let { return it }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_JSON, null)
        val parsed = if (json != null) parse(json) else emptyList()
        val result = parsed.ifEmpty { QuestionBank.all }
        cached = result
        return result
    }

    /** تحديث في الخلفية بدون تعطيل الواجهة. */
    fun refreshInBackground(context: Context) {
        exec.execute {
            try {
                val text = fetch() ?: return@execute
                val remoteVer = JSONObject(text).optInt("version", 0)
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                if (remoteVer > prefs.getInt(KEY_VERSION, 0)) {
                    val parsed = parse(text)
                    if (parsed.isNotEmpty()) {
                        prefs.edit()
                            .putString(KEY_JSON, text)
                            .putInt(KEY_VERSION, remoteVer)
                            .apply()
                        cached = parsed
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun fetch(): String? {
        return try {
            val conn = (URL(REMOTE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept-Charset", "utf-8")
                setRequestProperty("Accept-Encoding", "identity")
            }
            if (conn.responseCode == 200) {
                // فك ترميز صريح UTF-8 (يمنع أي تشويه للنص العربي)
                conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parse(json: String): List<Question> {
        return try {
            val arr = JSONObject(json).getJSONArray("questions")
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val optsArr = o.getJSONArray("o")
                val opts = (0 until optsArr.length()).map { optsArr.getString(it) }
                val c = o.getInt("c")
                if (opts.size < 2 || c !in opts.indices) return@mapNotNull null
                Question(
                    emoji = o.optString("e", "❓"),
                    text = o.getString("t"),
                    options = opts,
                    correctIndex = c,
                    age = o.getInt("age"),
                    difficulty = o.getInt("d")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
