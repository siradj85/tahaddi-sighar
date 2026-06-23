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

enum class Screen { SPLASH, HOME, AGE, QUIZ, STAGE_CLEAR, GAME_OVER, FINISHED, DAILY_CHALLENGE, MODE_SELECT, CATEGORY_SELECT, ACHIEVEMENTS, LEADERBOARD, FAMILY_RESULT, SETTINGS }

enum class GameMode { NORMAL, TIME_ATTACK, SURVIVAL, FAMILY, SINGLE_CATEGORY }

data class Stage(val number: Int, val title: String, val questions: List<Question>)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("tahaddi", Application.MODE_PRIVATE)

    companion object {
        const val QUESTIONS_PER_STAGE = 5
        const val MAX_LIVES = 3
        const val SAMPLE_PER_TIER = 40
        const val DAILY_QUESTION_COUNT = 10
        const val TIME_ATTACK_DURATION = 60
        const val SURVIVAL_QUESTIONS = 30
        const val FAMILY_QUESTIONS_PER_PLAYER = 10
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
    var screen by mutableStateOf(Screen.SPLASH); private set
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

    // ---------- Game Mode state ----------
    var gameMode by mutableStateOf(GameMode.NORMAL); private set
    var timeAttackTimeLeft by mutableIntStateOf(TIME_ATTACK_DURATION); private set
    var survivalCorrectStreak by mutableIntStateOf(0); private set
    var survivalBestStreak by mutableIntStateOf(0); private set
    var familyPlayer1Score by mutableIntStateOf(0); private set
    var familyPlayer2Score by mutableIntStateOf(0); private set
    var familyCurrentPlayer by mutableIntStateOf(1); private set
    var familyTotalQuestions by mutableIntStateOf(0); private set
    var selectedCategory by mutableStateOf(""); private set
    var availableCategories by mutableStateOf<List<String>>(emptyList()); private set
    var streakCount by mutableIntStateOf(CoinManager.getStreak(app)); private set

    // Leaderboard data
    var leaderboardEntries by mutableStateOf<List<LeaderboardEntry>>(emptyList()); private set
    var leaderboardMode by mutableStateOf("normal"); private set

    // Achievements
    var achievementList by mutableStateOf(AchievementManager.achievements); private set
    var unlockedAchievements by mutableStateOf<Set<String>>(emptySet()); private set

    // ---------- Initialization ----------
    init {
        SoundManager.enabled = soundOn
        AchievementManager.init(app)
        refreshUnlockedAchievements()
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
        AchievementManager.checkAndUnlock(getApplication(), "coins_collected", 1)
        refreshCoinBalance()
    }

    fun earnAdCoin() {
        CoinManager.addCoins(getApplication(), 5)
        AchievementManager.checkAndUnlock(getApplication(), "coins_collected", 5)
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
        gameMode = GameMode.NORMAL
        ageGroup = AgeGroup.FAMILY
        val pool = QuestionRepository.load(getApplication())
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val seed = today.toLongOrNull() ?: 20250101L
        val rng = Random(seed)

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
        CoinManager.recordDailyPlay(getApplication())
        streakCount = CoinManager.getStreak(getApplication())
        val bonus = CoinManager.streakBonusCoins(getApplication())
        CoinManager.addCoins(getApplication(), 3 + bonus)
        refreshCoinBalance()
        AchievementManager.checkAndUnlock(getApplication(), "daily_count", 1)
        AchievementManager.checkAndUnlockSet(getApplication(), "streak_count", CoinManager.getStreak(getApplication()))
    }

    // ---------- Game Mode methods ----------
    fun openModeSelect() {
        screen = Screen.MODE_SELECT
    }

    fun selectMode(mode: GameMode) {
        gameMode = mode
        when (mode) {
            GameMode.NORMAL -> openAgeSelect()
            GameMode.SINGLE_CATEGORY -> {
                val pool = QuestionRepository.load(getApplication())
                availableCategories = extractCategories(pool)
                screen = Screen.CATEGORY_SELECT
            }
            GameMode.TIME_ATTACK -> startTimeAttack()
            GameMode.SURVIVAL -> startSurvival()
            GameMode.FAMILY -> startFamily()
        }
    }

    fun selectCategory(category: String) {
        selectedCategory = category
        val pool = QuestionRepository.load(getApplication())
        val filtered = pool.filter { it.category == category }
        if (filtered.isEmpty()) {
            openAgeSelect()
            return
        }
        ageGroup = AgeGroup.FAMILY
        val questions = filtered.shuffled().take(20).map { it.withShuffledOptions() }
        stages = listOf(Stage(number = 1, title = category, questions = questions))
        currentStageIndex = 0
        totalScore = 0
        startStage()
    }

    private fun startTimeAttack() {
        isDailyChallenge = false
        ageGroup = AgeGroup.FAMILY
        val pool = QuestionRepository.load(getApplication())
        val questions = pool.shuffled().take(30).map { it.withShuffledOptions() }
        stages = listOf(Stage(number = 1, title = "هجوم الوقت ⏱️", questions = questions))
        currentStageIndex = 0
        totalScore = 0
        timeAttackTimeLeft = TIME_ATTACK_DURATION
        startStage()
    }

    fun tickTimeAttack() {
        if (gameMode != GameMode.TIME_ATTACK || answered) return
        if (timeAttackTimeLeft > 0) {
            timeAttackTimeLeft -= 1
        }
    }

    fun endTimeAttack() {
        if (gameMode != GameMode.TIME_ATTACK) return
        val ctx = getApplication<Application>()
        LeaderboardManager.addScore(ctx, "time_attack", totalScore)
        AchievementManager.checkAndUnlock(ctx, "time_attack_plays", 1)
        screen = Screen.FINISHED
    }

    private fun startSurvival() {
        isDailyChallenge = false
        ageGroup = AgeGroup.FAMILY
        val pool = QuestionRepository.load(getApplication())
        val questions = pool.shuffled().take(SURVIVAL_QUESTIONS).map { it.withShuffledOptions() }
        stages = listOf(Stage(number = 1, title = "البقاء 🛡️", questions = questions))
        currentStageIndex = 0
        totalScore = 0
        lives = MAX_LIVES
        survivalCorrectStreak = 0
        survivalBestStreak = 0
        startStage()
    }

    private fun startFamily() {
        isDailyChallenge = false
        ageGroup = AgeGroup.FAMILY
        val pool = QuestionRepository.load(getApplication())
        val questions = pool.shuffled().take(FAMILY_QUESTIONS_PER_PLAYER * 2).map { it.withShuffledOptions() }
        stages = listOf(Stage(number = 1, title = "لعبة العائلة 👨‍👩‍👧‍👦", questions = questions))
        currentStageIndex = 0
        totalScore = 0
        familyPlayer1Score = 0
        familyPlayer2Score = 0
        familyCurrentPlayer = 1
        familyTotalQuestions = 0
        startStage()
    }

    fun openAchievements() {
        refreshUnlockedAchievements()
        screen = Screen.ACHIEVEMENTS
    }

    fun openLeaderboard(mode: String = "normal") {
        leaderboardMode = mode
        leaderboardEntries = LeaderboardManager.getScores(getApplication(), mode)
        screen = Screen.LEADERBOARD
    }

    private fun refreshUnlockedAchievements() {
        val ctx = getApplication<Application>()
        unlockedAchievements = AchievementManager.achievements.filter {
            AchievementManager.isUnlocked(ctx, it.id)
        }.map { it.id }.toSet()
    }

    // ---------- التنقل ----------
    fun finishSplash() { if (screen == Screen.SPLASH) screen = Screen.HOME }

    fun openSettings() { screen = Screen.SETTINGS }

    fun openAgeSelect() { screen = Screen.AGE }

    fun goHome() {
        screen = Screen.HOME
        isDailyChallenge = false
        dailyCompleted = false
        gameMode = GameMode.NORMAL
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
        gameMode = GameMode.NORMAL
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
        if (!isDailyChallenge && gameMode == GameMode.NORMAL) {
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
        val correct = optionIndex == current?.correctIndex
        if (correct) {
            stageScore += 1
            totalScore += 1
            if (gameMode == GameMode.SURVIVAL) {
                survivalCorrectStreak += 1
                if (survivalCorrectStreak > survivalBestStreak) {
                    survivalBestStreak = survivalCorrectStreak
                }
            }
            earnCorrectCoin()
            if (gameMode == GameMode.FAMILY) {
                if (familyCurrentPlayer == 1) familyPlayer1Score += 1
                else familyPlayer2Score += 1
            }
            SoundManager.correct()
        } else {
            if (gameMode == GameMode.SURVIVAL) {
                survivalCorrectStreak = 0
            }
            if (gameMode == GameMode.TIME_ATTACK) {
                timeAttackTimeLeft = (timeAttackTimeLeft - 5).coerceAtLeast(0)
            }
            lives -= 1
            if (lives <= 0) {
                stageFailed = true
                SoundManager.wrong()
            } else {
                SoundManager.lifeLost()
            }
        }
        if (gameMode == GameMode.FAMILY) {
            familyCurrentPlayer = if (familyCurrentPlayer == 1) 2 else 1
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
            if (gameMode == GameMode.NORMAL) {
                screen = Screen.GAME_OVER
            } else {
                endGameMode()
            }
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
            } else if (gameMode == GameMode.SURVIVAL) {
                endGameMode()
            } else if (gameMode == GameMode.FAMILY) {
                endGameMode()
            } else if (gameMode == GameMode.TIME_ATTACK) {
                endGameMode()
            } else if (currentStageIndex + 1 >= stages.size) {
                prefs.edit().putInt("save_stage", -1).apply()
                screen = Screen.FINISHED
            } else {
                screen = Screen.STAGE_CLEAR
            }
        }
    }

    private fun endGameMode() {
        val ctx = getApplication<Application>()
        when (gameMode) {
            GameMode.TIME_ATTACK -> {
                LeaderboardManager.addScore(ctx, "time_attack", totalScore)
                AchievementManager.checkAndUnlock(ctx, "time_attack_plays", 1)
            }
            GameMode.SURVIVAL -> {
                LeaderboardManager.addScore(ctx, "survival", survivalBestStreak)
                AchievementManager.checkAndUnlockSet(ctx, "survival_streak", survivalBestStreak)
            }
            GameMode.FAMILY -> {
                LeaderboardManager.addScore(ctx, "family", maxOf(familyPlayer1Score, familyPlayer2Score))
                if (familyPlayer1Score != familyPlayer2Score) {
                    AchievementManager.checkAndUnlock(ctx, "family_wins", 1)
                }
            }
            GameMode.SINGLE_CATEGORY -> {
                AchievementManager.checkAndUnlock(ctx, "categories_played", 1)
            }
            GameMode.NORMAL -> {}
        }
        refreshUnlockedAchievements()
        screen = Screen.FINISHED
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
        if (isDailyChallenge || gameMode != GameMode.NORMAL) return
        val ctx = getApplication<Application>()
        if (totalScore > bestScore) {
            bestScore = totalScore
            prefs.edit().putInt("best", totalScore).apply()
        }
        AchievementManager.checkAndUnlock(ctx, "stages_completed", 1)
        AchievementManager.checkAndUnlock(ctx, "total_score", stageScore)
        if (stageScore >= QUESTIONS_PER_STAGE) {
            AchievementManager.checkAndUnlock(ctx, "perfect_stages", 1)
        }
        LeaderboardManager.addScore(ctx, "normal", totalScore)
        refreshUnlockedAchievements()
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
