package com.lifeos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val bg = Color(0xFF090B10)
private val panel = Color(0xFF171B26)
private val violet = Color(0xFF9B8CFF)
private val mint = Color(0xFF55D6A5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LifeOS() }
    }
}

@Composable
private fun LifeOS() {
    var tab by remember { mutableStateOf("Home") }
    var completed by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }
    var sets by remember { mutableIntStateOf(0) }
    LaunchedEffect(running) {
        while (running && seconds > 0) { delay(1000); seconds-- }
        if (seconds == 0) running = false
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = violet, secondary = mint, background = bg, surface = panel)) {
        Scaffold(
            containerColor = bg,
            bottomBar = {
                NavigationBar(containerColor = panel) {
                    listOf("Home", "Study", "Gym", "Progress").forEach { name ->
                        NavigationBarItem(
                            selected = tab == name,
                            onClick = { tab = name },
                            icon = { Icon(when(name) {
                                "Home" -> Icons.Default.Home
                                "Study" -> Icons.Default.MenuBook
                                "Gym" -> Icons.Default.FitnessCenter
                                else -> Icons.Default.Insights
                            }, contentDescription = name) },
                            label = { Text(name) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp).padding(top = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LIFEOS", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("Build your consistency.", color = Color.Gray, fontSize = 12.sp)
                    }
                    Text("● LIVE", color = mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(22.dp))
                when(tab) {
                    "Home" -> {
                        Text("YOUR SPACE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Module("STUDY", "170-topic learning roadmap", "$completed topics completed", violet) { tab = "Study" }
                        Spacer(Modifier.height(12.dp))
                        Module("GYM", "Push · Pull · Legs", "Track sets, reps and training", mint) { tab = "Gym" }
                        Spacer(Modifier.height(18.dp))
                        Text("FOCUS TIMER", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TimerCard(seconds, running, { running = !running }, { seconds = 25 * 60; running = false })
                    }
                    "Study" -> {
                        Text("STUDY TRACKER", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Your 170-topic roadmap", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { completed / 170f }, modifier = Modifier.fillMaxWidth(), color = violet)
                        Text("$completed / 170 completed", color = Color.Gray, modifier = Modifier.padding(vertical = 10.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("DSA · 27 topics", "LLD & Code Architecture · 19 topics", "Backend · 29 topics", "HLD & System Design · 24 topics", "AI & Agentic Engineering · 25 topics", "Cyber Security & AppSec · 23 topics", "DevOps / Cloud / Infrastructure · 23 topics")) { item ->
                                Card(colors = CardDefaults.cardColors(containerColor = panel)) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(item, color = Color.White, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { completed = (completed + 1).coerceAtMost(170) }) { Icon(Icons.Default.CheckCircle, null, tint = mint) }
                                    }
                                }
                            }
                        }
                    }
                    "Gym" -> {
                        Text("TRAINING", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Push · Pull · Legs", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        listOf("PUSH DAY" to listOf("Bench Press", "Shoulder Press", "Incline Dumbbell Press", "Lateral Raises", "Triceps Pushdown"), "PULL DAY" to listOf("Lat Pulldown", "Seated Cable Row", "Face Pull", "Bicep Curl"), "LEGS DAY" to listOf("Squats", "Leg Press", "Leg Curl", "Calf Raises")).forEach { (day, exercises) ->
                            Text(day, color = mint, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                            exercises.forEach { ex ->
                                Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.padding(bottom = 6.dp)) {
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(ex, color = Color.White, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { sets++ }) { Icon(Icons.Default.Add, null, tint = mint) }
                                    }
                                }
                            }
                        }
                        Text("Sets logged this session: $sets", color = Color.Gray)
                    }
                    else -> {
                        Text("YOUR PROGRESS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Consistency over intensity.", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Module("STUDY", "Curriculum progress", "$completed / 170 topics", violet) {}
                        Spacer(Modifier.height(12.dp))
                        Module("GYM", "Workout activity", "$sets sets logged this session", mint) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun Module(title: String, subtitle: String, detail: String, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = panel)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun TimerCard(seconds: Int, running: Boolean, toggle: () -> Unit, reset: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = panel), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val time = "%02d:%02d".format(seconds / 60, seconds % 60)
            Text(time, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = toggle) { Text(if (running) "Pause" else "Start") }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = reset) { Icon(Icons.Default.Refresh, null, tint = Color.Gray) }
        }
    }
}
