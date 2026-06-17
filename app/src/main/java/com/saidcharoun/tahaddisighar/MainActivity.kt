package com.saidcharoun.tahaddisighar

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdManager.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            // فرض اتجاه من اليمين لليسار (عربي)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TahaddiTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppRoot()
                    }
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
            Screen.QUIZ -> QuizScreen(vm)
            Screen.RESULT -> ResultScreen(vm)
        }
    }
}

private fun bgBrush() = Brush.verticalGradient(
    listOf(Color(0xFF7B1FA2), Color(0xFF4A148C))
)

@Composable
fun HomeScreen(vm: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("⭐", fontSize = 88.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "تحدي الصغار",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "أسئلة ممتعة تتعلم منها وأنت تلعب!",
                fontSize = 18.sp,
                color = Color(0xFFFFE082),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = { vm.startGame() },
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF4A148C))
                Spacer(Modifier.width(8.dp))
                Text("ابدأ اللعب", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AccentAmber)
                Spacer(Modifier.width(8.dp))
                Text("أفضل نتيجة: ${vm.bestScore}", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun QuizScreen(vm: GameViewModel) {
    val activity = LocalContext.current as Activity
    val q = vm.current ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush())
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // شريط علوي: التقدّم والنتيجة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "سؤال ${vm.index + 1} / ${vm.questions.size}",
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
                Text("النقاط: ${vm.score}", color = AccentAmber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (vm.index + 1f) / vm.questions.size },
                modifier = Modifier.fillMaxWidth(),
                color = AccentAmber,
                trackColor = Color(0x33FFFFFF)
            )

            Spacer(Modifier.height(24.dp))

            // بطاقة السؤال
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(q.emoji, fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        q.text,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF311B92)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // الخيارات
            q.options.forEachIndexed { i, option ->
                if (i !in vm.hiddenOptions) {
                    OptionButton(
                        text = option,
                        state = optionState(vm, i),
                        onClick = { vm.answer(i) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            // أزرار أسفل الشاشة
            if (!vm.answered) {
                OutlinedButton(
                    onClick = {
                        AdManager.showRewardedAd(activity) { vm.useFiftyFifty() }
                    },
                    enabled = !vm.fiftyUsedThisQuestion,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        if (vm.fiftyUsedThisQuestion) "تم استخدام المساعدة" else "🆘 مساعدة (احذف إجابتين)",
                        color = Color.White, fontSize = 18.sp
                    )
                }
            } else {
                Button(
                    onClick = { vm.next() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        if (vm.index + 1 >= vm.questions.size) "إنهاء" else "السؤال التالي ←",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C)
                    )
                }
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
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            disabledContainerColor = container
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().height(60.dp)
    ) {
        Text(
            text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ResultScreen(vm: GameViewModel) {
    val total = vm.questions.size
    val score = vm.score
    val emoji = when {
        score >= total -> "🏆"
        score >= total * 0.6 -> "🎉"
        score >= total * 0.3 -> "👍"
        else -> "💪"
    }
    val message = when {
        score >= total -> "ممتاز! إجابات كاملة!"
        score >= total * 0.6 -> "أحسنت! نتيجة رائعة"
        score >= total * 0.3 -> "جيد! حاول مرة أخرى"
        else -> "لا بأس، التدريب يصنع البطل!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(emoji, fontSize = 96.sp)
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("نتيجتك", fontSize = 18.sp, color = Color.Gray)
                    Text("$score / $total", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
                    Spacer(Modifier.height(8.dp))
                    Text("أفضل نتيجة: ${vm.bestScore}", fontSize = 16.sp, color = Color(0xFF6A1B9A))
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { vm.startGame() },
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) {
                Text("العب مرة أخرى", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { vm.goHome() }) {
                Text("الرئيسية", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}
