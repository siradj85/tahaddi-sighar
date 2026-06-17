package com.saidcharoun.tahaddisighar

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdManager.initialize(applicationContext)
        SoundManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TahaddiTheme {
                    Surface(modifier = Modifier.fillMaxSize()) { AppRoot() }
                }
            }
        }
    }
}

@Composable
fun AppRoot(vm: GameViewModel = viewModel()) {
    AnimatedContent(targetState = vm.screen, label = "screen") { screen ->
        when (screen) {
            Screen.HOME -> HomeScreen(vm)
            Screen.AGE -> AgeScreen(vm)
            Screen.QUIZ -> QuizScreen(vm)
            Screen.STAGE_CLEAR -> StageClearScreen(vm)
            Screen.GAME_OVER -> GameOverScreen(vm)
            Screen.FINISHED -> FinishedScreen(vm)
        }
    }
}

private fun bg() = Brush.verticalGradient(listOf(Color(0xFF7B1FA2), Color(0xFF4A148C)))
private val Gold = Color(0xFFFFC107)
private val DeepPurple = Color(0xFF4A148C)

@Composable
private fun SoundToggle(vm: GameViewModel, modifier: Modifier = Modifier) {
    IconButton(onClick = { vm.toggleSound() }, modifier = modifier) {
        Icon(
            if (vm.soundOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = "الصوت",
            tint = Color.White
        )
    }
}

@Composable
private fun BigButton(text: String, container: Color = Gold, textColor: Color = DeepPurple, onClick: () -> Unit) {
    Button(
        onClick = { SoundManager.click(); onClick() },
        colors = ButtonDefaults.buttonColors(containerColor = container),
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth().height(62.dp)
    ) {
        Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

// ===================== الرئيسية =====================
@Composable
fun HomeScreen(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().background(bg())) {
        SoundToggle(vm, Modifier.align(Alignment.TopEnd).padding(8.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center).padding(28.dp)
        ) {
            Text("⭐", fontSize = 92.sp)
            Text("تحدي الصغار", fontSize = 46.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("تعلّم وأنت تلعب عبر مراحل ممتعة!", fontSize = 17.sp, color = Color(0xFFFFE082), textAlign = TextAlign.Center)
            Spacer(Modifier.height(36.dp))

            if (vm.hasSavedGame()) {
                BigButton("▶ متابعة (مرحلة ${vm.savedStageNumber()})") { vm.continueGame() }
                Spacer(Modifier.height(14.dp))
                BigButton("لعبة جديدة", container = Color(0xFF9C27B0), textColor = Color.White) { vm.openAgeSelect() }
            } else {
                BigButton("ابدأ اللعب") { vm.openAgeSelect() }
            }

            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Gold)
                Spacer(Modifier.width(8.dp))
                Text("أفضل نتيجة: ${vm.bestScore}", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

// ===================== اختيار العمر =====================
@Composable
fun AgeScreen(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().background(bg())) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center).padding(28.dp)
        ) {
            Text("كم عمر اللاعب؟", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(32.dp))
            AgeGroup.entries.forEach { group ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).height(110.dp)
                        .clickable { SoundManager.click(); vm.selectAge(group) }
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(group.emoji, fontSize = 56.sp)
                        Spacer(Modifier.width(20.dp))
                        Text(group.label, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = DeepPurple)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = { vm.goHome() }) { Text("رجوع", color = Color.White, fontSize = 18.sp) }
        }
    }
}

// ===================== اللعب =====================
@Composable
fun QuizScreen(vm: GameViewModel) {
    val activity = LocalContext.current as Activity
    val q = vm.current ?: return
    val stage = vm.currentStage ?: return

    // ----- المؤقّت الزمني لكل سؤال -----
    val totalTime = vm.secondsPerQuestion
    var timeLeft by remember(vm.qIndex, vm.currentStageIndex) { mutableIntStateOf(totalTime) }
    LaunchedEffect(vm.qIndex, vm.currentStageIndex, vm.answered) {
        if (!vm.answered) {
            timeLeft = totalTime
            while (timeLeft > 0 && !vm.answered) {
                delay(1000)
                if (!vm.answered) timeLeft -= 1
            }
            if (timeLeft <= 0 && !vm.answered) vm.timeUp()
        }
    }
    val timeColor = if (timeLeft <= 5) Color(0xFFFF5252) else Gold

    Box(Modifier.fillMaxSize().background(bg()).padding(20.dp)) {
        Column(Modifier.fillMaxSize()) {
            // الشريط العلوي: المرحلة + القلوب
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("المرحلة ${stage.number} / ${vm.totalStages}", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(stage.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(GameViewModel.MAX_LIVES) { i ->
                        Icon(
                            if (i < vm.lives) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (i < vm.lives) Color(0xFFFF5252) else Color(0x66FFFFFF),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // المؤقّت الزمني
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = timeColor, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                LinearProgressIndicator(
                    progress = { timeLeft.toFloat() / totalTime },
                    modifier = Modifier.weight(1f).height(10.dp),
                    color = timeColor, trackColor = Color(0x33FFFFFF)
                )
                Spacer(Modifier.width(8.dp))
                Text("$timeLeft ث", color = timeColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("سؤال ${vm.qIndex + 1} / ${stage.questions.size}", color = Color.White, fontSize = 15.sp)
                Text("النقاط: ${vm.totalScore}", color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (vm.qIndex + 1f) / stage.questions.size },
                modifier = Modifier.fillMaxWidth(),
                color = Gold, trackColor = Color(0x33FFFFFF)
            )
            Spacer(Modifier.height(20.dp))

            // بطاقة السؤال
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(q.emoji, fontSize = 60.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(q.text, fontSize = 25.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF311B92))
                }
            }
            Spacer(Modifier.height(20.dp))

            q.options.forEachIndexed { i, option ->
                if (i !in vm.hiddenOptions) {
                    OptionButton(option, optionState(vm, i)) { vm.answer(i) }
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            if (!vm.answered) {
                OutlinedButton(
                    onClick = { SoundManager.click(); AdManager.showRewardedAd(activity) { vm.useFiftyFifty() } },
                    enabled = !vm.fiftyUsed,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(if (vm.fiftyUsed) "تم استخدام المساعدة" else "🆘 مساعدة (احذف إجابات خاطئة)", color = Color.White, fontSize = 17.sp)
                }
            } else {
                BigButton(if (vm.qIndex + 1 >= stage.questions.size) "إنهاء المرحلة" else "السؤال التالي ←") { vm.next() }
            }
        }
    }
}

private enum class OptionVisual { NORMAL, CORRECT, WRONG }

private fun optionState(vm: GameViewModel, i: Int): OptionVisual {
    if (!vm.answered) return OptionVisual.NORMAL
    val correct = vm.current?.correctIndex
    return when {
        i == correct -> OptionVisual.CORRECT
        i == vm.selectedIndex -> OptionVisual.WRONG
        else -> OptionVisual.NORMAL
    }
}

@Composable
private fun OptionButton(text: String, state: OptionVisual, onClick: () -> Unit) {
    val container = when (state) {
        OptionVisual.NORMAL -> Color.White
        OptionVisual.CORRECT -> CorrectGreen
        OptionVisual.WRONG -> WrongRed
    }
    val textColor = if (state == OptionVisual.NORMAL) Color(0xFF311B92) else Color.White
    Button(
        onClick = onClick,
        enabled = state == OptionVisual.NORMAL,
        colors = ButtonDefaults.buttonColors(containerColor = container, disabledContainerColor = container),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().height(58.dp)
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center)
    }
}

// ===================== اجتياز مرحلة =====================
@Composable
fun StageClearScreen(vm: GameViewModel) {
    val activity = LocalContext.current as Activity
    Box(Modifier.fillMaxSize().background(bg()), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Text("🎉", fontSize = 96.sp)
            Text("أحسنت! اجتزت المرحلة", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Row {
                repeat(3) { i ->
                    val earned = vm.stageScore >= (i + 1) * (GameViewModel.QUESTIONS_PER_STAGE / 3f)
                    Icon(Icons.Default.Star, contentDescription = null, tint = if (earned) Gold else Color(0x55FFFFFF), modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("النقاط الكلية: ${vm.totalScore}", fontSize = 20.sp, color = Color(0xFFFFE082))
            Spacer(Modifier.height(32.dp))
            BigButton("المرحلة التالية ←") { vm.nextStage() }
            Spacer(Modifier.height(10.dp))
            BigButton("📤 شارك تقدّمي", container = Color(0xFF9C27B0), textColor = Color.White) {
                ShareCard.shareProgress(activity, vm.currentStageIndex + 1, vm.totalStages, vm.totalScore)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.goHome() }) { Text("الرئيسية", color = Color.White, fontSize = 17.sp) }
        }
        Confetti(Modifier.fillMaxSize())
    }
}

// ===================== خسارة (نفاد القلوب) =====================
@Composable
fun GameOverScreen(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFB71C1C), Color(0xFF4A148C)))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Text("💔", fontSize = 92.sp)
            Text("نفدت القلوب!", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text("لا تقلق، حاول هذه المرحلة مرة أخرى\nأنت تستطيع! 💪", fontSize = 18.sp, color = Color(0xFFFFCDD2), textAlign = TextAlign.Center)
            Spacer(Modifier.height(36.dp))
            BigButton("🔄 إعادة المحاولة") { vm.retryStage() }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { vm.goHome() }) { Text("الرئيسية", color = Color.White, fontSize = 17.sp) }
        }
    }
}

// ===================== إنهاء كل المراحل =====================
@Composable
fun FinishedScreen(vm: GameViewModel) {
    val activity = LocalContext.current as Activity
    val total = vm.totalQuestions
    Box(Modifier.fillMaxSize().background(bg()), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Text("🏆", fontSize = 100.sp)
            Text("بطل تحدي الصغار!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(horizontal = 44.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("نتيجتك النهائية", fontSize = 16.sp, color = Color.Gray)
                    Text("${vm.totalScore} / $total", fontSize = 46.sp, fontWeight = FontWeight.Bold, color = DeepPurple)
                    Text("أفضل نتيجة: ${vm.bestScore}", fontSize = 15.sp, color = Color(0xFF6A1B9A))
                }
            }
            Spacer(Modifier.height(28.dp))
            BigButton("📤 شارك فوزك مع أصدقائك") {
                ShareCard.share(activity, vm.totalScore, total, vm.ageGroup.label)
            }
            Spacer(Modifier.height(12.dp))
            BigButton("العب من جديد", container = Color(0xFF9C27B0), textColor = Color.White) { vm.openAgeSelect() }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.goHome() }) { Text("الرئيسية", color = Color.White, fontSize = 17.sp) }
        }
        Confetti(Modifier.fillMaxSize(), pieceCount = 140)
    }
}
