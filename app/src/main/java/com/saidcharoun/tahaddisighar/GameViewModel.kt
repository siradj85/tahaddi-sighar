package com.saidcharoun.tahaddisighar

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

enum class Screen { HOME, AGE, QUIZ, STAGE_CLEAR, GAME_OVER, FINISHED, DAILY_CHALLENGE }

data class Stage(val number: Int, val title: String, val questions: List<Question>)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("tahaddi", Application.MODE_PRIVATE)

    companion object {
        const val QUESTIONS_PER_STAGE = 5
        const val MAX_LIVES = 3
        const val SAMPLE_PER_TIER = 40
        const val DAILY_QUESTION_COUNT = 10
        val STAGE_TITLES = listOf(
            "الانطلاق 🚀", "المغامرة 🌟", "الاستكشاف 🔍", "التحدّي 💪", "العباقرة 🧠",
            "الأبطال 🦸", "الصاعقة ⚡", "الكنز 💎", "النجوم ✨", "البطولة 👑"
        )
    }

    val secondsPerQuestion: Int
        get() = when (ageGroup) {
            AgeGroup.YOUNG -> 18
            AgeGroup.OLDER -> 14
            AgeGroup.NINE_TWELVE -> 12
            AgeGroup.FAMILY -> 10
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

    var coinBalance by mutableIntStateOf(CoinManager.getBalance(app)); private set
    var isDailyChallenge by mutableStateOf(false); private set
    var dailyCompleted by mutableStateOf(false); private set

    init {
        SoundManager.enabled = soundOn
    }

    val currentStage: Stage? get() = stages.getOrNull(currentStageIndex)
    val current: Question? get() = currentStage?.questions?.getOrNull(qIndex)
    val totalStages: Int get() = stages.size
    val totalQuestions: Int get() = stages.sumOf { it.questions.size }

    fun hasSavedGame(): Boolean = prefs.getInt("save_stage", -1) >= 0

    fun savedStageNumber(): Int = prefs.getInt("save_stage", 0) + 1

    // ---------- العملات ----------
    private fun refreshCoinBalance() {
        coinBalance = CoinManager.getBalance(getApplication())
    }

    fun earnCorrectCoin() {
        CoinManager.addCoins(getApplication(), 1)
        refreshCoinBalance()
    }

    fun earnAdCoin() {
        CoinManager.addCoins(getApplication(), 5)
        refreshCoinBalance()
    }

    fun buyExtraLife(): Boolean {
        if (lives >= MAX_LIVES) return false
        if (!CoinManager.spendCoins(getApplication(), 10)) return false
        lives += 1
        refreshCoinBalance()
        return true
    }

    fun buyHint(): Boolean {
        if (fiftyUsed || answered) return false
        if (!CoinManager.spendCoins(getApplication(), 5)) return false
        val q = current ?: return false
        val toRemove = if (q.options.size <= 3) 1 else 2
        val wrong = q.options.indices.filter { it != q.correctIndex }.shuffled().take(toRemove)
        hiddenOptions = wrong.toSet()
        fiftyUsed = true
        return true
    }

    // ---------- التحدّي اليومي ----------
    fun canPlayDaily(): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return CoinManager.getDailyDate(getApplication()) != today
    }

    fun startDailyChallenge() {
        isDailyChallenge = true
        dailyCompleted = false
        ageGroup = AgeGroup.FAMILY
        val pool = QuestionRepository.load(getApplication())
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val seed = today.toLongOrNull() ?: 20250101L
        val rng = Random(seed)

        // Pick questions distributed across age groups and difficulties
        val dailyQuestions = mutableListOf<Question>()
        val ages = listOf(1, 2, 3, 4)
        val difficulties = listOf(1, 2, 3)
        val perBucket = DAILY_QUESTION_COUNT / (ages.size * difficulties.size) + 1

        for (age in ages) {
            for (d in difficulties) {
                val candidates = pool.filter { it.age == age && it.difficulty == d }
                val shuffled = candidates.shuffled(rng)
                dailyQuestions.addAll(shuffled.take(perBucket))
            }
        }

        val finalQuestions = dailyQuestions.shuffled(rng).take(DAILY_QUESTION_COUNT)
            .map { it.withShuffledOptions() }

        stages = listOf(
            Stage(number = 1, title = "التحدّي اليومي 🌟", questions = finalQuestions)
        )
        currentStageIndex = 0
        totalScore = 0
        startStage()
        screen = Screen.QUIZ
    }

    fun completeDailyChallenge() {
        dailyCompleted = true
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        CoinManager.setDailyDate(getApplication(), today)
        // Bonus 3 coins for completing the daily challenge
        CoinManager.addCoins(getApplication(), 3)
        refreshCoinBalance()
    }

    // ---------- التنقل ----------
    fun openAgeSelect() { screen = Screen.AGE }

    fun goHome() {
        screen = Screen.HOME
        isDailyChallenge = false
        dailyCompleted = false
    }

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
        isDailyChallenge = false
        startStage()
    }

    fun continueGame() {
        val savedAge = prefs.getInt("save_age", AgeGroup.YOUNG.code)
        ageGroup = AgeGroup.entries.firstOrNull { it.code == savedAge } ?: AgeGroup.YOUNG
        stages = buildStages(ageGroup.code)
        currentStageIndex = prefs.getInt("save_stage", 0).coerceIn(0, (stages.size - 1).coerceAtLeast(0))
        totalScore = prefs.getInt("save_score", 0)
        isDailyChallenge = false
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
        if (!isDailyChallenge) {
            prefs.edit()
                .putInt("save_age", ageGroup.code)
                .putInt("save_stage", currentStageIndex)
                .putInt("save_score", totalScore)
                .apply()
        }
        screen = Screen.QUIZ
    }

    fun answer(optionIndex: Int) {
        if (answered) return
        selectedIndex = optionIndex
        answered = true
        if (optionIndex == current?.correctIndex) {
            stageScore += 1
            totalScore += 1
            earnCorrectCoin()
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

    fun timeUp() {
        if (answered) return
        answered = true
        selectedIndex = null
        lives -= 1
        if (lives <= 0) {
            stageFailed = true
            SoundManager.wrong()
        } else {
            SoundManager.wrong()
        }
    }

    fun next() {
        val stage = currentStage ?: return
        if (stageFailed) {
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
            saveBestIfNeeded()
            SoundManager.win()
            if (isDailyChallenge) {
                completeDailyChallenge()
                screen = Screen.FINISHED
            } else if (currentStageIndex + 1 >= stages.size) {
                prefs.edit().putInt("save_stage", -1).apply()
                screen = Screen.FINISHED
            } else {
                screen = Screen.STAGE_CLEAR
            }
        }
    }

    fun nextStage() {
        currentStageIndex += 1
        startStage()
    }

    fun retryStage() {
        totalScore -= stageScore
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
        if (isDailyChallenge) return
        if (totalScore > bestScore) {
            bestScore = totalScore
            prefs.edit().putInt("best", totalScore).apply()
        }
    }

    private fun Question.withShuffledOptions(): Question {
        val idx = options.indices.shuffled()
        return copy(
            options = idx.map { options[it] },
            correctIndex = idx.indexOf(correctIndex)
        )
    }

    private fun buildStages(age: Int): List<Stage> {
        val pool = QuestionRepository.load(getApplication()).filter { it.age == age }
        fun pick(d: Int) = pool.filter { it.difficulty == d }.shuffled().take(SAMPLE_PER_TIER)
        val ordered = (pick(1) + pick(2) + pick(3)).map { it.withShuffledOptions() }

        return ordered.chunked(QUESTIONS_PER_STAGE)
            .filter { it.size >= 3 }
            .mapIndexed { i, qs ->
                Stage(number = i + 1, title = STAGE_TITLES[i % STAGE_TITLES.size], questions = qs)
            }
    }
}
