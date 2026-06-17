package com.saidcharoun.tahaddisighar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Screen { HOME, QUIZ, RESULT }

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("tahaddi", Application.MODE_PRIVATE)

    companion object {
        const val QUESTIONS_PER_ROUND = 10
    }

    var screen by mutableStateOf(Screen.HOME)
        private set

    var questions by mutableStateOf<List<Question>>(emptyList())
        private set

    var index by mutableIntStateOf(0)
        private set

    var score by mutableIntStateOf(0)
        private set

    var answered by mutableStateOf(false)
        private set

    var selectedIndex by mutableStateOf<Int?>(null)
        private set

    var hiddenOptions by mutableStateOf<Set<Int>>(emptySet())
        private set

    var fiftyUsedThisQuestion by mutableStateOf(false)
        private set

    var bestScore by mutableIntStateOf(prefs.getInt("best", 0))
        private set

    val current: Question?
        get() = questions.getOrNull(index)

    fun startGame() {
        questions = QuestionBank.all.shuffled().take(QUESTIONS_PER_ROUND)
        index = 0
        score = 0
        answered = false
        selectedIndex = null
        hiddenOptions = emptySet()
        fiftyUsedThisQuestion = false
        screen = Screen.QUIZ
    }

    fun answer(optionIndex: Int) {
        if (answered) return
        selectedIndex = optionIndex
        answered = true
        if (optionIndex == current?.correctIndex) {
            score += 1
        }
    }

    fun next() {
        if (index + 1 >= questions.size) {
            if (score > bestScore) {
                bestScore = score
                prefs.edit().putInt("best", score).apply()
            }
            screen = Screen.RESULT
        } else {
            index += 1
            answered = false
            selectedIndex = null
            hiddenOptions = emptySet()
            fiftyUsedThisQuestion = false
        }
    }

    /** يحذف خيارين خاطئين (يُستدعى بعد مشاهدة الإعلان المكافئ). */
    fun useFiftyFifty() {
        val q = current ?: return
        if (fiftyUsedThisQuestion || answered) return
        val wrong = q.options.indices.filter { it != q.correctIndex }.shuffled().take(2)
        hiddenOptions = wrong.toSet()
        fiftyUsedThisQuestion = true
    }

    fun goHome() {
        screen = Screen.HOME
    }
}
