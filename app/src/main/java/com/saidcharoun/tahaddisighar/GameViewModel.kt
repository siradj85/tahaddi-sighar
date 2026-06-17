package com.saidcharoun.tahaddisighar

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

enum class Screen { HOME, AGE, QUIZ, STAGE_CLEAR, GAME_OVER, FINISHED }

data class Stage(val number: Int, val title: String, val questions: List<Question>)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("tahaddi", Application.MODE_PRIVATE)

    companion object {
        const val QUESTIONS_PER_STAGE = 5
        const val MAX_LIVES = 3
        val STAGE_TITLES = listOf(
            "الانطلاق 🚀", "الإحماء 🔥", "التحدّي 💪",
            "المهارة ⚡", "الخبراء 🧠", "البطولة 👑"
        )
    }

    // ---------- الحالة ----------
    var screen by mutableStateOf(Screen.HOME); private set
    var ageGroup by mutableStateOf(AgeGroup.YOUNG); private set
    var stages by mutableStateOf<List<Stage>>(emptyList()); private set
    var currentStageIndex by mutableIntStateOf(0); private set

    var qIndex by mutableIntStateOf(0); private set
    var lives by mutableIntStateOf(MAX_LIVES); private set
    var totalScore by mutableIntStateOf(0); private set
    var stageScore by mutableIntStateOf(0); private set

    var answered by mutableStateOf(false); private set
    var selectedIndex by mutableStateOf<Int?>(null); private set
    var hiddenOptions by mutableStateOf<Set<Int>>(emptySet()); private set
    var fiftyUsed by mutableStateOf(false); private set
    private var stageFailed = false

    var bestScore by mutableIntStateOf(prefs.getInt("best", 0)); private set
    var soundOn by mutableStateOf(prefs.getBoolean("sound", true)); private set

    init {
        SoundManager.enabled = soundOn
    }

    val currentStage: Stage? get() = stages.getOrNull(currentStageIndex)
    val current: Question? get() = currentStage?.questions?.getOrNull(qIndex)
    val totalStages: Int get() = stages.size

    fun hasSavedGame(): Boolean = prefs.getInt("save_stage", -1) >= 0

    fun savedStageNumber(): Int = prefs.getInt("save_stage", 0) + 1

    // ---------- التنقل ----------
    fun openAgeSelect() { screen = Screen.AGE }

    fun goHome() { screen = Screen.HOME }

    fun toggleSound() {
        soundOn = !soundOn
        SoundManager.enabled = soundOn
        prefs.edit().putBoolean("sound", soundOn).apply()
    }

    fun selectAge(group: AgeGroup) {
        ageGroup = group
        stages = buildStages(group.code)
        currentStageIndex = 0
        totalScore = 0
        startStage()
    }

    fun continueGame() {
        val savedAge = prefs.getInt("save_age", AgeGroup.YOUNG.code)
        ageGroup = AgeGroup.entries.firstOrNull { it.code == savedAge } ?: AgeGroup.YOUNG
        stages = buildStages(ageGroup.code)
        currentStageIndex = prefs.getInt("save_stage", 0).coerceIn(0, (stages.size - 1).coerceAtLeast(0))
        totalScore = prefs.getInt("save_score", 0)
        startStage()
    }

    private fun startStage() {
        qIndex = 0
        lives = MAX_LIVES
        stageScore = 0
        answered = false
        selectedIndex = null
        hiddenOptions = emptySet()
        fiftyUsed = false
        stageFailed = false
        // حفظ نقطة الاستئناف
        prefs.edit()
            .putInt("save_age", ageGroup.code)
            .putInt("save_stage", currentStageIndex)
            .putInt("save_score", totalScore)
            .apply()
        screen = Screen.QUIZ
    }

    fun answer(optionIndex: Int) {
        if (answered) return
        selectedIndex = optionIndex
        answered = true
        if (optionIndex == current?.correctIndex) {
            stageScore += 1
            totalScore += 1
            SoundManager.correct()
        } else {
            lives -= 1
            if (lives <= 0) {
                stageFailed = true
                SoundManager.wrong()
            } else {
                SoundManager.lifeLost()
            }
        }
    }

    fun next() {
        val stage = currentStage ?: return
        if (stageFailed) {
            // نفدت القلوب → إعادة المرحلة
            screen = Screen.GAME_OVER
            return
        }
        if (qIndex + 1 < stage.questions.size) {
            qIndex += 1
            answered = false
            selectedIndex = null
            hiddenOptions = emptySet()
            fiftyUsed = false
        } else {
            // اكتملت المرحلة بنجاح
            saveBestIfNeeded()
            SoundManager.win()
            if (currentStageIndex + 1 >= stages.size) {
                prefs.edit().putInt("save_stage", -1).apply() // أنهى اللعبة
                screen = Screen.FINISHED
            } else {
                screen = Screen.STAGE_CLEAR
            }
        }
    }

    /** الانتقال للمرحلة التالية بعد شاشة النجاح. */
    fun nextStage() {
        currentStageIndex += 1
        startStage()
    }

    /** إعادة المرحلة الحالية بعد نفاد القلوب. */
    fun retryStage() {
        totalScore -= stageScore // إلغاء نقاط هذه المرحلة الفاشلة
        if (totalScore < 0) totalScore = 0
        startStage()
    }

    fun useFiftyFifty() {
        val q = current ?: return
        if (fiftyUsed || answered) return
        val toRemove = if (q.options.size <= 3) 1 else 2
        val wrong = q.options.indices.filter { it != q.correctIndex }.shuffled().take(toRemove)
        hiddenOptions = wrong.toSet()
        fiftyUsed = true
    }

    private fun saveBestIfNeeded() {
        if (totalScore > bestScore) {
            bestScore = totalScore
            prefs.edit().putInt("best", totalScore).apply()
        }
    }

    /**
     * يبني ٦ مراحل متدرّجة الصعوبة: مرحلتان سهلتان، ثم متوسطتان، ثم صعبتان.
     */
    private fun buildStages(age: Int): List<Stage> {
        val pool = QuestionBank.all.filter { it.age == age }
        val ordered = pool.filter { it.difficulty == 1 }.shuffled() +
            pool.filter { it.difficulty == 2 }.shuffled() +
            pool.filter { it.difficulty == 3 }.shuffled()

        return ordered.chunked(QUESTIONS_PER_STAGE)
            .filter { it.size >= 3 } // تجاهل مرحلة ناقصة جداً
            .mapIndexed { i, qs ->
                val title = STAGE_TITLES.getOrElse(i) { "مرحلة ${i + 1}" }
                Stage(number = i + 1, title = title, questions = qs)
            }
    }
}
