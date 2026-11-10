package com.lifeos.app

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val bg = Color(0xFF071329)
private val panel = Color(0xFF173B70)
private val violet = Color(0xFF0865F5)
private val mint = Color(0xFFB8F52A)
private val muted = Color(0xFFA9B8D2)
private val paleCard = Color(0xFFF0F1FF)
private val deepBlue = Color(0xFF073B91)
private val softBlue = Color(0xFF31588D)

data class Topic(val id: Long, val category: String, val title: String, val done: Boolean = false)
data class SetLog(val exercise: String, val sets: Int, val reps: Int, val weight: Double, val day: String)

private val categories = listOf(
    "DSA" to 27, "LLD & Code Architecture" to 19, "Backend" to 29,
    "HLD & System Design" to 24, "AI & Agentic Engineering" to 25,
    "Cyber Security & AppSec" to 23, "DevOps / Cloud / Infrastructure" to 23
)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var unlocked by remember { mutableStateOf(false) }
            var splash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) { delay(1400); splash = false }
            when {
                splash -> SplashScreen()
                unlocked -> LifeOS(this@MainActivity)
                else -> LockScreen { authenticate { unlocked = true } }
            }
        }
    }

    private fun authenticate(onSuccess: () -> Unit) {
        val manager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) return
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock LIFEOS")
            .setSubtitle("Use your fingerprint or phone screen lock")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }
}

private fun loadTopics(ctx: Context): MutableList<Topic> {
    val dao = LifeOSDatabase.get(ctx).lifeOSDao()
    val stored = dao.getTopics()
    if (stored.isNotEmpty()) return stored.map { Topic(it.id, it.category, it.title, it.completed) }.toMutableList()
    // One-time migration from the previous SharedPreferences-based build.
    val legacy = ctx.getSharedPreferences("lifeos", 0).getString("topics", null)
    val imported = if (legacy != null) try {
        val a = JSONArray(legacy); MutableList(a.length()) { i ->
            val o = a.getJSONObject(i); Topic(o.getLong("id"), o.getString("category"), o.getString("title"), o.getBoolean("done"))
        }
    } catch (_: Exception) { seedTopics() } else seedTopics()
    dao.putTopics(imported.map { TopicEntity(it.id, it.category, it.title, it.done) })
    return imported
}
private fun seedTopics(): MutableList<Topic> {
    val result = mutableListOf<Topic>(); var id = 1L
    categories.forEach { (name, count) -> repeat(count) { i -> result.add(Topic(id++, name, "$name · Topic ${i + 1}")) } }
    return result
}
private fun saveTopics(ctx: Context, topics: List<Topic>) {
    LifeOSDatabase.get(ctx).lifeOSDao().putTopics(topics.map { TopicEntity(it.id, it.category, it.title, it.done) })
}
private fun loadLogs(ctx: Context): MutableList<SetLog> {
    val dao = LifeOSDatabase.get(ctx).lifeOSDao()
    val stored = dao.getWorkoutLogs()
    if (stored.isNotEmpty()) return stored.map { SetLog(it.exercise, it.sets, it.reps, it.weight, it.day) }.toMutableList()
    val raw = ctx.getSharedPreferences("lifeos", 0).getString("logs", null) ?: return mutableListOf()
    return try {
        val a = JSONArray(raw); MutableList(a.length()) { i ->
            val o = a.getJSONObject(i); SetLog(o.getString("exercise"), o.getInt("sets"), o.getInt("reps"), o.getDouble("weight"), o.getString("day"))
        }.also { saveLogs(ctx, it) }
    } catch (_: Exception) { mutableListOf() }
}
private fun saveLogs(ctx: Context, logs: List<SetLog>) {
    val dao = LifeOSDatabase.get(ctx).lifeOSDao()
    dao.clearWorkoutLogs()
    logs.forEach { dao.insertWorkout(WorkoutEntity(exercise = it.exercise, sets = it.sets, reps = it.reps, weight = it.weight, day = it.day)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LifeOS(context: Context) {
    val topics = remember { mutableStateListOf<Topic>().also { it.addAll(loadTopics(context)) } }
    val logs = remember { mutableStateListOf<SetLog>().also { it.addAll(loadLogs(context)) } }
    var tab by remember { mutableStateOf("Home") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showTopicDialog by remember { mutableStateOf(false) }
    var showSetDialog by remember { mutableStateOf(false) }
    var customTitle by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("DSA") }
    var exercise by remember { mutableStateOf("Bench Press") }
    var setCount by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var weight by remember { mutableStateOf("20") }
    var seconds by remember { mutableIntStateOf(25 * 60) }
    var isBreak by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { photo = it }

    LaunchedEffect(running) {
        while (running && seconds > 0) { delay(1000); seconds-- }
        if (seconds == 0) { running = false; isBreak = !isBreak; seconds = if (isBreak) 5 * 60 else 25 * 60 }
    }
    val done = topics.count { it.done }
    val total = topics.size
    val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

    MaterialTheme(colorScheme = darkColorScheme(primary = violet, secondary = mint, background = bg, surface = panel)) {
        Scaffold(
            containerColor = bg,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).background(softBlue, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Bolt, null, tint = mint, modifier = Modifier.size(25.dp))
                            }
                            Spacer(Modifier.width(11.dp))
                            Column {
                                Text("Good morning!", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                                Text("Your progress, one day at a time.", color = muted, fontSize = 11.sp)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.NotificationsNone, "Notifications", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bg, titleContentColor = Color.White)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF102B55)) {
                    listOf("Home", "Study", "Gym", "Journal", "Progress").forEach { name ->
                        NavigationBarItem(selected = tab == name, onClick = { tab = name }, colors = NavigationBarItemDefaults.colors(selectedIconColor = violet, selectedTextColor = Color.White, indicatorColor = Color(0xFFDAE5FF), unselectedIconColor = Color(0xFFB2C3DF), unselectedTextColor = Color(0xFFB2C3DF)),
                            icon = { Icon(when(name) { "Home" -> Icons.Default.Home; "Study" -> Icons.Default.MenuBook; "Gym" -> Icons.Default.FitnessCenter; "Journal" -> Icons.Default.PhotoCamera; else -> Icons.Default.Insights }, null) },
                            label = { Text(name, fontSize = 10.sp) })
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                when (tab) {
                    "Home" -> {
                        LazyColumn(modifier = Modifier.testTag("home-list"), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text("YOUR DAILY OVERVIEW", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                                Text("Total Progress", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    colors = CardDefaults.cardColors(containerColor = paleCard),
                                    shape = RoundedCornerShape(25.dp)
                                ) {
                                    Column(Modifier.padding(19.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("LIFEOS SCORE", color = Color(0xFF5E6680), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            Icon(Icons.Default.AutoAwesome, null, tint = violet)
                                        }
                                        Text("$done / $total", color = Color(0xFF101A32), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                                        Text("Study topics completed", color = Color(0xFF69728A), fontSize = 12.sp)
                                        Spacer(Modifier.height(14.dp))
                                        LinearProgressIndicator(
                                            progress = { if (total == 0) 0f else done.toFloat() / total },
                                            modifier = Modifier.fillMaxWidth().height(8.dp),
                                            color = violet,
                                            trackColor = Color(0xFFD4D9EB)
                                        )
                                        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Keep building momentum", color = Color(0xFF69728A), fontSize = 10.sp)
                                            Text("${if (total == 0) 0 else done * 100 / total}%", color = Color(0xFF101A32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = softBlue),
                                    shape = RoundedCornerShape(23.dp)
                                ) {
                                    Column(Modifier.padding(17.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text("YOUR ROUTINE", color = Color(0xFFC7D5EF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                                Text("Small steps. Big change.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Icon(Icons.Default.Bolt, null, tint = mint, modifier = Modifier.size(28.dp))
                                        }
                                        Spacer(Modifier.height(14.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Column(Modifier.weight(1f)) {
                                                Text("STUDY", color = Color(0xFFC7D5EF), fontSize = 10.sp)
                                                Text("$done / $total", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                                Text("topics done", color = Color(0xFFC7D5EF), fontSize = 10.sp)
                                            }
                                            Column(Modifier.weight(1f)) {
                                                Text("GYM", color = Color(0xFFC7D5EF), fontSize = 10.sp)
                                                Text("${logs.size}", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                                Text("workout logs", color = Color(0xFFC7D5EF), fontSize = 10.sp)
                                            }
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { tab = "Study" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = violet), shape = RoundedCornerShape(14.dp)) { Text("Open Study") }
                                            OutlinedButton(onClick = { tab = "Gym" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), shape = RoundedCornerShape(14.dp)) { Text("Open Gym") }
                                        }
                                    }
                                }
                            }
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Focus session", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("POMODORO", color = mint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                TimerPanel(seconds, running, isBreak, { running = !running }, { running = false; isBreak = false; seconds = 25 * 60 }, { running = false; isBreak = true; seconds = 5 * 60 })
                            }
                        }
                    }
                    "Study" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text("STUDY TRACKER", color = muted, fontSize = 11.sp); Text("$done / $total completed", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                                Button(onClick = { showTopicDialog = true }) { Icon(Icons.Default.Add, null); Text("Topic") }
                            }
                            LinearProgressIndicator(progress = { if (total == 0) 0f else done.toFloat() / total }, modifier = Modifier.fillMaxWidth(), color = violet)
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item { FilterChip(selected = selectedCategory == "All", onClick = { selectedCategory = "All" }, label = { Text("All") }) }
                                items(categories) { c -> FilterChip(selected = selectedCategory == c.first, onClick = { selectedCategory = c.first }, label = { Text(c.first) }) }
                            }
                            val visible = topics.filter { selectedCategory == "All" || it.category == selectedCategory }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                                items(visible, key = { it.id }) { topic ->
                                    Card(colors = CardDefaults.cardColors(containerColor = softBlue), shape = RoundedCornerShape(16.dp)) {
                                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) { Text(topic.title, color = Color.White, fontWeight = FontWeight.Medium); Text(topic.category, color = muted, fontSize = 10.sp) }
                                            Checkbox(modifier = Modifier.testTag("topic-checkbox-${topic.id}"), checked = topic.done, onCheckedChange = { checked ->
                                                val idx = topics.indexOfFirst { it.id == topic.id }; if (idx >= 0) topics[idx] = topic.copy(done = checked); saveTopics(context, topics)
                                            }, colors = CheckboxDefaults.colors(checkedColor = mint))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Gym" -> {
                        Column(Modifier.fillMaxSize()) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text("TRAINING LOG", color = muted, fontSize = 11.sp); Text("Push · Pull · Legs", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                                Button(onClick = { showSetDialog = true }) { Icon(Icons.Default.Add, null); Text("Log set") }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("PUSH", color = mint, fontWeight = FontWeight.Bold)
                            ExerciseRow("Bench Press") { exercise = it; showSetDialog = true }
                            ExerciseRow("Shoulder Press") { exercise = it; showSetDialog = true }
                            ExerciseRow("Incline Dumbbell Press") { exercise = it; showSetDialog = true }
                            ExerciseRow("Lateral Raises") { exercise = it; showSetDialog = true }
                            Text("PULL", color = mint, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                            ExerciseRow("Lat Pulldown") { exercise = it; showSetDialog = true }
                            ExerciseRow("Seated Cable Row") { exercise = it; showSetDialog = true }
                            ExerciseRow("Bicep Curl") { exercise = it; showSetDialog = true }
                            Text("LEGS", color = mint, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                            ExerciseRow("Squat") { exercise = it; showSetDialog = true }
                            ExerciseRow("Leg Press") { exercise = it; showSetDialog = true }
                            ExerciseRow("Leg Curl") { exercise = it; showSetDialog = true }
                            Text("RECENT SETS", color = muted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(logs.reversed()) { log ->
                                    Card(colors = CardDefaults.cardColors(containerColor = softBlue), shape = RoundedCornerShape(16.dp)) {
                                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) { Text(log.exercise, color = Color.White); Text("${log.sets} sets × ${log.reps} reps · ${log.weight} kg · ${log.day}", color = muted, fontSize = 11.sp) }
                                            IconButton(onClick = { logs.remove(log); saveLogs(context, logs) }) { Icon(Icons.Default.Delete, null, tint = Color.Gray) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Journal" -> {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(12.dp))
                            Text("PHOTO JOURNAL", color = muted, fontSize = 11.sp)
                            Text("Capture your progress.", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { camera.launch(null) }) { Icon(Icons.Default.CameraAlt, null); Text("Capture progress") }
                            photo?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().weight(1f)) }
                                ?: Box(Modifier.fillMaxWidth().weight(1f).background(panel, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Photo, null, tint = muted, modifier = Modifier.size(72.dp)) }
                            Text("Camera preview is kept for this session. Save/export journal storage can be added in a later version.", color = muted, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Spacer(Modifier.height(12.dp)); Text("YOUR PROGRESS", color = muted, fontSize = 11.sp); Text("Consistency over intensity.", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            StatCard("STUDY COMPLETION", "$done / $total", "topics", violet, Modifier.fillMaxWidth())
                            StatCard("GYM HISTORY", "${logs.size}", "logged sessions / exercises", mint, Modifier.fillMaxWidth())
                            Text("Data is stored locally on this device.", color = muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showTopicDialog) AlertDialog(
        onDismissRequest = { showTopicDialog = false },
        title = { Text("Add study topic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(customTitle, { customTitle = it }, label = { Text("Topic name") }, singleLine = true)
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(customCategory, {}, readOnly = true, label = { Text("Category") }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        (categories.map { it.first } + "Custom").forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = { customCategory = name; expanded = false }) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (customTitle.isNotBlank()) { topics.add(Topic((topics.maxOfOrNull { it.id } ?: 0L) + 1, customCategory, customTitle.trim())); saveTopics(context, topics); customTitle = ""; showTopicDialog = false } }) { Text("Add") } },
        dismissButton = { TextButton(onClick = { showTopicDialog = false }) { Text("Cancel") } }
    )

    if (showSetDialog) AlertDialog(
        onDismissRequest = { showSetDialog = false },
        title = { Text("Log workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(exercise, { exercise = it }, label = { Text("Exercise") }, modifier = Modifier.testTag("workout-exercise"), singleLine = true)
                OutlinedTextField(setCount, { setCount = it }, label = { Text("Sets") }, modifier = Modifier.testTag("workout-sets"), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(reps, { reps = it }, label = { Text("Reps per set") }, modifier = Modifier.testTag("workout-reps"), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(weight, { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.testTag("workout-weight"), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = {
            val s = setCount.toIntOrNull(); val r = reps.toIntOrNull(); val w = weight.toDoubleOrNull()
            if (exercise.isNotBlank() && s != null && s > 0 && r != null && r > 0 && w != null && w >= 0) { logs.add(SetLog(exercise.trim(), s, r, w, date)); saveLogs(context, logs); showSetDialog = false }
        }) { Text("Save set") } },
        dismissButton = { TextButton(onClick = { showSetDialog = false }) { Text("Cancel") } }
    )
}


@Composable
private fun SplashScreen() {
    MaterialTheme(colorScheme = darkColorScheme(primary = violet, background = bg, surface = panel)) {
        Box(Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(82.dp).background(violet, RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Bolt, null, tint = bg, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("LIFEOS", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text("YOUR LIFE. YOUR PROGRESS.", color = muted, fontSize = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(26.dp))
                CircularProgressIndicator(color = mint, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = violet, background = bg, surface = panel)) {
        Box(Modifier.fillMaxSize().background(bg).padding(28.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Fingerprint, null, tint = violet, modifier = Modifier.size(82.dp))
                Spacer(Modifier.height(22.dp))
                Text("Welcome back", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("Unlock your personal workspace", color = muted, fontSize = 13.sp)
                Spacer(Modifier.height(28.dp))
                Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Fingerprint, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock with fingerprint")
                }
                Text("Your phone PIN, pattern or password can be used as a fallback when supported.", color = muted, fontSize = 11.sp, modifier = Modifier.padding(top = 14.dp))
            }
        }
    }
}

@Composable private fun StatCard(title: String, value: String, detail: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = softBlue), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(15.dp)) { Text(title, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(detail, color = muted, fontSize = 11.sp) }
    }
}
@Composable private fun ModuleCard(title: String, subtitle: String, detail: String, accent: Color, click: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { click() }, colors = CardDefaults.cardColors(containerColor = panel), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) { Text(title, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(detail, color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
    }
}
@Composable private fun ExerciseRow(name: String, click: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(bottom = 5.dp).clickable { click(name) }, colors = CardDefaults.cardColors(containerColor = softBlue), shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(name, color = Color.White, modifier = Modifier.weight(1f)); Icon(Icons.Default.AddCircle, null, tint = mint) }
    }
}
@Composable private fun TimerPanel(seconds: Int, running: Boolean, isBreak: Boolean, toggle: () -> Unit, reset: () -> Unit, breakMode: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = softBlue), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(if (isBreak) "BREAK TIMER" else "POMODORO FOCUS", color = muted, fontSize = 10.sp, modifier = Modifier.weight(1f)); Text(if (running) "RUNNING" else "PAUSED", color = mint, fontSize = 10.sp) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("%02d:%02d".format(seconds / 60, seconds % 60), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = toggle) { Text(if (running) "Pause" else "Start") }
                IconButton(onClick = reset) { Icon(Icons.Default.Refresh, null) }
            }
            Row { TextButton(onClick = reset) { Text("25 min focus") }; TextButton(onClick = breakMode) { Text("5 min break") } }
        }
    }
}
