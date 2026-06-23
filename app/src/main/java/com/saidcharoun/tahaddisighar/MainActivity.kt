package com.saidcharoun.tahaddisighar

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdManager.initialize(applicationContext)
        SoundManager.init(applicationContext)
        QuestionRepository.refreshInBackground(applicationContext)
        ReminderManager.scheduleDaily(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
            Screen.SPLASH -> SplashScreen(vm)
            Screen.HOME -> HomeScreen(vm)
            Screen.AGE -> AgeScreen(vm)
            Screen.QUIZ -> QuizScreen(vm)
            Screen.STAGE_CLEAR -> StageClearScreen(vm)
            Screen.GAME_OVER -> GameOverScreen(vm)
            Screen.FINISHED -> FinishedScreen(vm)
            Screen.DAILY_CHALLENGE -> QuizScreen(vm)
            Screen.MODE_SELECT -> ModeSelectScreen(vm)
            Screen.CATEGORY_SELECT -> CategorySelectScreen(vm)
            Screen.ACHIEVEMENTS -> AchievementsScreen(vm)
            Screen.LEADERBOARD -> LeaderboardScreen(vm)
            Screen.FAMILY_RESULT -> FamilyResultScreen(vm)
            Screen.SETTINGS -> SettingsScreen(vm)
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
private fun CoinBalance(vm: GameViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(4.dp))
        Text("${vm.coinBalance}", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

@Composable
private fun MusicToggle(vm: GameViewModel) {
    IconButton(onClick = { SoundManager.toggleMusic() }) {
        Icon(
            if (SoundManager.musicOn) Icons.Default.MusicNote else Icons.Default.MusicOff,
            contentDescription = "الموسيقى",
            tint = Color.White
        )
    }
}

// ===================== اختيار وضع اللعب =====================
@Composable
fun ModeSelectScreen(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().background(bg())) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("اختر وضع اللعب", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(20.dp))
            ModeButton("🎯 الوضع العادي", "مراحل متدرّجة حسب العمر", Color(0xFF9C27B0)) { vm.selectMode(GameMode.NORMAL) }
            ModeButton("⏱️ هجوم الوقت", "أجب على أكبر عدد من الأسئلة في 60 ثانية", Color(0xFFFF6F00)) { vm.selectMode(GameMode.TIME_ATTACK) }
            ModeButton("🛡️ البقاء", "أجب على 30 سؤالاً متتالياً دون خطأ", Color(0xFFD32F2F)) { vm.selectMode(GameMode.SURVIVAL) }
            ModeButton("👨‍👩‍👧‍👦 العائلة", "تنافس مع فرد من عائلتك", Color(0xFF1976D2)) { vm.selectMode(GameMode.FAMILY) }
            ModeButton("📚 فئة واحدة", "اختر فئة واحصل على أسئلة عنها فقط", Color(0xFF388E3C)) { vm.selectMode(GameMode.SINGLE_CATEGORY) }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { vm.goHome() }) { Text("رجوع", color = Color.White, fontSize = 18.sp) }
        }
    }
}

@Composable
private fun ModeButton(title: String, desc: String, color: Color, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { SoundManager.click(); onClick() }
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(desc, fontSize = 14.sp, color = Color(0xFFFFE082))
        }
    }
}

// ===================== اختيار الفئة =====================
@Composable
fun CategorySelectScreen(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().background(bg())) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("اختر فئة الأسئلة", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(20.dp))
            vm.availableCategories.forEach { category ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF9C27B0)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { SoundManager.click(); vm.selectCategory(category) }
                ) {
                    Text(category, Modifier.padding(18.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { vm.goHome() }) { Text("رجوع", color = Color.White, fontSize = 18.sp) }
        }
    }
}

// ===================== الإنجازات =====================
@Composable
fun AchievementsScreen(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().background(bg())) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("الإنجازات 🏆", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))
            vm.achievementList.forEach { a ->
                val unlocked = a.id in vm.unlockedAchievements
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (unlocked) Color(0xFF2E7D32) else Color(0xFF424242)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (unlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (unlocked) Gold else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(a.description, fontSize = 13.sp, color = Color(0xFFFFE082))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { vm.goHome() }) { Text("رجوع", color = Color.White, fontSize = 18.sp) }
        }
    }
}

// ===================== لوحة المتصدّرين =====================
@Composable
fun LeaderboardScreen(vm: GameViewModel) {
    val modes = listOf("normal" to "العادي", "time_attack" to "هجوم الوقت", "survival" to "البقاء", "family" to "العائلة")
    Box(Modifier.fillMaxSize().background(bg())) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("لوحة المتصدّرين 🏅", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEach { (key, label) ->
                    FilterChip(
                        selected = vm.leaderboardMode == key,
                        onClick = { vm.openLeaderboard(key) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold,
                            selectedLabelColor = DeepPurple
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (vm.leaderboardEntries.isEmpty()) {
                Text("لا توجد نتائج بعد", fontSize = 18.sp, color = Color(0xFFFFE082))
            } else {
                vm.leaderboardEntries.forEachIndexed { i, entry ->
                    val medal = when (i) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "$i."
                    }
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(medal, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Text("${entry.score}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.weight(1f))
                            Text(entry.date, fontSize = 12.sp, color = Color(0xFFFFE082))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { vm.goHome() }) { Text("رجوع", color = Color.White, fontSize = 18.sp) }
        }
    }
}

// ===================== نتيجة العائلة =====================
@Composable
fun FamilyResultScreen(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().background(bg()), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Text("👨‍👩‍👧‍👦", fontSize = 92.sp)
            Text("نتيجة لعبة العائلة", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(20.dp))
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF))) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👤 اللاعب الأول: ${vm.familyPlayer1Score}", fontSize = 22.sp, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("👤 اللاعب الثاني: ${vm.familyPlayer2Score}", fontSize = 22.sp, color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    val winner = when {
                        vm.familyPlayer1Score > vm.familyPlayer2Score -> "اللاعب الأول 🏆"
                        vm.familyPlayer2Score > vm.familyPlayer1Score -> "اللاعب الثاني 🏆"
                        else -> "تعادل! 👏"
                    }
                    Text("الفائز: $winner", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Gold)
                }
            }
            Spacer(Modifier.height(28.dp))
            BigButton("الرئيسية") { vm.goHome() }
        }
    }
}

// ===================== الإعدادات =====================
@Composable
fun SettingsScreen(vm: GameViewModel) {
    val ctx = LocalContext.current
    var music by remember { mutableStateOf(SoundManager.musicOn) }

    fun open(url: String) {
        try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
    }

    Box(Modifier.fillMaxSize().background(bg())) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("الإعدادات ⚙️", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(20.dp))

            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    SettingSwitchRow("المؤثرات الصوتية", Icons.Default.VolumeUp, vm.soundOn) { vm.toggleSound() }
                    SettingSwitchRow("الموسيقى الخلفية", Icons.Default.MusicNote, music) {
                        SoundManager.toggleMusic(); music = SoundManager.musicOn
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    SettingActionRow("قيّم التطبيق", Icons.Default.Star) {
                        open("https://play.google.com/store/apps/details?id=com.saidcharoun.tahaddisighar")
                    }
                    SettingActionRow("شارك التطبيق", Icons.Default.Share) {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "العب تحدّي الصغار — لعبة أسئلة تعليمية ممتعة للأطفال!\nhttps://play.google.com/store/apps/details?id=com.saidcharoun.tahaddisighar"
                            )
                        }
                        try { ctx.startActivity(Intent.createChooser(share, "مشاركة")) } catch (_: Exception) {}
                    }
                    SettingActionRow("سياسة الخصوصية", Icons.Default.Lock) {
                        open("https://siradj85.github.io/tahaddi-sighar/privacy.html")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("الإصدار 7.0", fontSize = 14.sp, color = Color(0xFFFFE082))
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { vm.goHome() }) { Text("رجوع", color = Color.White, fontSize = 18.sp) }
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, icon: ImageVector, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Gold)
        Spacer(Modifier.width(12.dp))
        Text(title, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SettingActionRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Gold)
        Spacer(Modifier.width(12.dp))
        Text(title, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color(0x88FFFFFF))
    }
}

// ===================== شاشة البداية =====================
@Composable
fun SplashScreen(vm: GameViewModel) {
    var started by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (started) 1f else 0.55f, tween(750), label = "scale")
    val fade by animateFloatAsState(if (started) 1f else 0f, tween(750), label = "fade")

    LaunchedEffect(Unit) {
        started = true
        delay(1900)
        vm.finishSplash()
    }

    Box(Modifier.fillMaxSize().background(bg()), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(fade)
        ) {
            // الشعار — استبدله بأيقونة التطبيق الجديدة لاحقاً (painterResource)
            Box(
                Modifier.size(150.dp).scale(scale).background(Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("⭐", fontSize = 86.sp)
            }
            Spacer(Modifier.height(22.dp))
            Text("تحدي الصغار", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("تعلّم وأنت تلعب", fontSize = 17.sp, color = Color(0xFFFFE082))
            Spacer(Modifier.height(30.dp))
            CircularProgressIndicator(color = Gold, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
        }
    }
}

// ===================== الرئيسية =====================
@Composable
fun HomeScreen(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().background(bg())) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // الشريط العلوي: العملات + أزرار الصوت
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoinBalance(vm)
                Row {
                    MusicToggle(vm)
                    SoundToggle(vm)
                    IconButton(onClick = { SoundManager.click(); vm.openSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // الشعار والعنوان
            Box(
                Modifier.size(96.dp).background(Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("⭐", fontSize = 56.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text("تحدي الصغار", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text("تعلّم وأنت تلعب عبر مراحل ممتعة!", fontSize = 15.sp, color = Color(0xFFFFE082), textAlign = TextAlign.Center)

            // السلسلة اليومية
            val streak = CoinManager.getStreak(LocalContext.current)
            if (streak > 0) {
                Spacer(Modifier.height(12.dp))
                Surface(color = Color(0x33FFFFFF), shape = RoundedCornerShape(20.dp)) {
                    Text(
                        "🔥 سلسلة $streak يوم",
                        Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontSize = 16.sp, color = Gold, fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            if (vm.hasSavedGame()) {
                BigButton("▶ متابعة (مرحلة ${vm.savedStageNumber()})") { vm.continueGame() }
                Spacer(Modifier.height(12.dp))
            }

            BigButton("🎮 أوضاع اللعب") { vm.openModeSelect() }
            Spacer(Modifier.height(12.dp))

            if (vm.canPlayDaily()) {
                BigButton("🌟 التحدّي اليومي", container = Color(0xFFFF6F00), textColor = Color.White) {
                    vm.startDailyChallenge()
                }
            } else {
                BigButton("✅ أكملت التحدّي اليومي", container = Color(0xFF388E3C), textColor = Color.White) { }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { SoundManager.click(); vm.openAchievements() },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Gold)
                    Spacer(Modifier.width(6.dp))
                    Text("الإنجازات", color = Color.White)
                }
                OutlinedButton(
                    onClick = { SoundManager.click(); vm.openLeaderboard() },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Default.Leaderboard, contentDescription = null, tint = Gold)
                    Spacer(Modifier.width(6.dp))
                    Text("المتصدّرون", color = Color.White)
                }
            }

            Spacer(Modifier.height(20.dp))

            // بطاقة أفضل نتيجة
            Surface(color = Color(0x22FFFFFF), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Gold)
                    Spacer(Modifier.width(8.dp))
                    Text("أفضل نتيجة: ${vm.bestScore}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

// ===================== اختيار العمر =====================
@Composable
fun AgeScreen(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().background(bg())) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("اختر الفئة العمرية", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(20.dp))
            AgeGroup.entries.forEach { group ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(96.dp)
                        .clickable { SoundManager.click(); vm.selectAge(group) }
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(group.emoji, fontSize = 48.sp)
                        Spacer(Modifier.width(20.dp))
                        Text(group.label, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepPurple)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
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

    // Timer logic
    val totalTime = if (vm.gameMode == GameMode.TIME_ATTACK) GameViewModel.TIME_ATTACK_DURATION else vm.secondsPerQuestion
    var timeLeft by remember(vm.qIndex, vm.currentStageIndex, vm.screen) { mutableIntStateOf(totalTime) }

    LaunchedEffect(vm.qIndex, vm.currentStageIndex, vm.screen, vm.answered) {
        if (!vm.answered) {
            timeLeft = totalTime
            while (timeLeft > 0 && !vm.answered) {
                delay(1000)
                if (!vm.answered) {
                    timeLeft -= 1
                    if (vm.gameMode == GameMode.TIME_ATTACK) {
                        vm.tickTimeAttack()
                        if (timeLeft <= 0) vm.endTimeAttack()
                    }
                }
            }
            if (timeLeft <= 0 && !vm.answered && vm.gameMode != GameMode.TIME_ATTACK) vm.timeUp()
        }
    }

    val timeColor = if (timeLeft <= 5) Color(0xFFFF5252) else Gold

    var showShop by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(bg()).padding(20.dp)) {
        Column(Modifier.fillMaxSize()) {
            // الشريط العلوي
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    when {
                        vm.isDailyChallenge -> {
                            Text("التحدّي اليومي 🌟", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("السؤال ${vm.qIndex + 1} / ${stage.questions.size}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        vm.gameMode == GameMode.TIME_ATTACK -> {
                            Text("هجوم الوقت ⏱️", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("${vm.timeAttackTimeLeft} ث", color = if (vm.timeAttackTimeLeft <= 10) Color(0xFFFF5252) else Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        vm.gameMode == GameMode.SURVIVAL -> {
                            Text("البقاء 🛡️", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("السؤال ${vm.qIndex + 1} / ${GameViewModel.SURVIVAL_QUESTIONS}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        vm.gameMode == GameMode.FAMILY -> {
                            Text("لعبة العائلة 👨‍👩‍👧‍👦", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("دور اللاعب ${vm.familyCurrentPlayer}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Text("المرحلة ${stage.number} / ${vm.totalStages}", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(stage.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinBalance(vm)
                    Spacer(Modifier.width(12.dp))
                    if (vm.gameMode != GameMode.TIME_ATTACK) {
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
            }

            Spacer(Modifier.height(10.dp))

            // Timer bar
            if (vm.gameMode != GameMode.TIME_ATTACK) {
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
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("سؤال ${vm.qIndex + 1} / ${stage.questions.size}", color = Color.White, fontSize = 15.sp)
                Text("النقاط: ${vm.totalScore}", color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (vm.gameMode == GameMode.SURVIVAL) {
                    Text("👍 ${vm.survivalCorrectStreak}", color = Color(0xFFFFE082), fontSize = 15.sp)
                }
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { SoundManager.click(); AdManager.showRewardedAd(activity) {
                            if (vm.isDailyChallenge || vm.gameMode != GameMode.NORMAL) vm.earnAdCoin()
                            else vm.useFiftyFifty()
                        } },
                        enabled = if (vm.isDailyChallenge || vm.gameMode != GameMode.NORMAL) true else !vm.fiftyUsed,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(if (vm.fiftyUsed) "تم الاستخدام" else "🆘 مساعدة", color = Color.White, fontSize = 15.sp)
                    }
                    OutlinedButton(
                        onClick = { SoundManager.click(); showShop = true },
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "المتجر", tint = Gold, modifier = Modifier.size(22.dp))
                    }
                }
            } else {
                val isLast = vm.qIndex + 1 >= stage.questions.size
                val btnText = when {
                    isLast && vm.gameMode == GameMode.SURVIVAL -> "عرض النتيجة"
                    isLast && vm.gameMode == GameMode.FAMILY -> "عرض النتيجة"
                    isLast && vm.gameMode == GameMode.TIME_ATTACK -> "عرض النتيجة"
                    isLast -> "إنهاء المرحلة"
                    else -> "السؤال التالي ←"
                }
                BigButton(btnText) { vm.next() }
            }
        }

        // Shop dialog
        if (showShop) {
            AlertDialog(
                onDismissRequest = { showShop = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Gold)
                        Spacer(Modifier.width(8.dp))
                        Text("المتجر 🪙", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) { CoinBalance(vm) }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (vm.buyExtraLife()) { SoundManager.correct(); showShop = false }
                            },
                            enabled = vm.lives < GameViewModel.MAX_LIVES,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("❤️ قلب إضافي — 🪙 10", color = Color.White)
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (vm.buyHint()) { SoundManager.correct(); showShop = false }
                            },
                            enabled = !vm.fiftyUsed && !vm.answered,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Gold)
                            Spacer(Modifier.width(8.dp))
                            Text("💡 تلميح (حذف خيارين) — 🪙 5", color = Color.White)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showShop = false }) { Text("إغلاق") } }
            )
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
            if (vm.lives < GameViewModel.MAX_LIVES && vm.coinBalance >= 10) {
                Text("يمكنك شراء قلب إضافي بـ 🪙 10", fontSize = 16.sp, color = Color(0xFFFFE082), textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                BigButton("❤️ اشتري قلباً (🪙 10)", container = Color(0xFFFF5252), textColor = Color.White) {
                    if (vm.buyExtraLife()) vm.retryStage()
                }
                Spacer(Modifier.height(10.dp))
            }
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
            when {
                vm.isDailyChallenge -> {
                    Text("🌙", fontSize = 100.sp)
                    Text("أكملت التحدّي اليومي!", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("نقاطك: ${vm.totalScore} / $total", fontSize = 22.sp, color = Gold)
                    Spacer(Modifier.height(8.dp))
                    val streak = CoinManager.getStreak(LocalContext.current)
                    Text("+ 🪙 3 مكافأة إكمال التحدّي!", fontSize = 18.sp, color = Color(0xFFFFE082))
                    if (streak > 1) {
                        Text("🔥 سلسلة $streak يوم — مكافأة إضافية!", fontSize = 16.sp, color = Color(0xFFFFE082))
                    }
                }
                vm.gameMode == GameMode.TIME_ATTACK -> {
                    Text("⏱️", fontSize = 100.sp)
                    Text("انتهى هجوم الوقت!", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("نقاطك: ${vm.totalScore}", fontSize = 28.sp, color = Gold, fontWeight = FontWeight.Bold)
                }
                vm.gameMode == GameMode.SURVIVAL -> {
                    Text("🛡️", fontSize = 100.sp)
                    Text("انتهى وضع البقاء!", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("أفضل سلسلة: ${vm.survivalBestStreak}", fontSize = 28.sp, color = Gold, fontWeight = FontWeight.Bold)
                }
                vm.gameMode == GameMode.FAMILY -> {
                    Text("👨‍👩‍👧‍👦", fontSize = 100.sp)
                    Text("انتهت لعبة العائلة!", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("اللاعب الأول: ${vm.familyPlayer1Score}", fontSize = 20.sp, color = Color(0xFFFFE082))
                    Text("اللاعب الثاني: ${vm.familyPlayer2Score}", fontSize = 20.sp, color = Color(0xFFFFE082))
                    Spacer(Modifier.height(8.dp))
                    val winner = when {
                        vm.familyPlayer1Score > vm.familyPlayer2Score -> "اللاعب الأول 🏆"
                        vm.familyPlayer2Score > vm.familyPlayer1Score -> "اللاعب الثاني 🏆"
                        else -> "تعادل! 👏"
                    }
                    Text("الفائز: $winner", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Gold)
                }
                else -> {
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
                }
            }
            Spacer(Modifier.height(28.dp))
            BigButton("📤 شارك فوزك مع أصدقائك") {
                ShareCard.share(activity, vm.totalScore, total, vm.ageGroup.label)
            }
            Spacer(Modifier.height(12.dp))
            if (!vm.isDailyChallenge && vm.gameMode == GameMode.NORMAL) {
                BigButton("العب من جديد", container = Color(0xFF9C27B0), textColor = Color.White) { vm.openAgeSelect() }
                Spacer(Modifier.height(8.dp))
            }
            TextButton(onClick = { vm.goHome() }) { Text("الرئيسية", color = Color.White, fontSize = 17.sp) }
        }
        if (!vm.isDailyChallenge && vm.gameMode == GameMode.NORMAL) {
            Confetti(Modifier.fillMaxSize(), pieceCount = 140)
        }
    }
}
