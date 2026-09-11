package app.veshinantam.web

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.veshinantam.shared.GregorianCalendar
import app.veshinantam.shared.LearningPlanner
import app.veshinantam.shared.LearningTask
import app.veshinantam.shared.LearningTaskType

private val DeepBlue = Color(0xFF173B67)
private val DeepBlueContainer = Color(0xFFDCE9FF)
private val WarmGold = Color(0xFFC59636)
private val AppBackground = Color(0xFFF7F8FC)
private val MutedInk = Color(0xFF5C6370)
private val SuccessGreen = Color(0xFF2E6E55)

private enum class Destination(val en: String, val he: String, val icon: ImageVector) {
    TODAY("Today", "היום", Icons.Default.Today),
    CALENDAR("Calendar", "לוח שנה", Icons.Default.CalendarMonth),
    SCHEDULES("Schedules", "תוכניות", Icons.AutoMirrored.Filled.EventNote),
    PROGRESS("Progress", "התקדמות", Icons.Default.Insights),
}

@Composable
fun WebApp(store: BrowserStore, todayIso: String) {
    var appState by remember { mutableStateOf(store.load()) }
    var destination by remember { mutableStateOf(Destination.TODAY) }
    var showCreate by remember { mutableStateOf(false) }
    val hebrew = appState.language == "he"

    fun update(transform: (WebAppState) -> WebAppState) {
        appState = transform(appState)
        store.save(appState)
    }

    CompositionLocalProvider(LocalLayoutDirection provides if (hebrew) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
                BoxWithConstraints {
                    val desktop = maxWidth >= 880.dp
                    if (desktop) {
                        Row(Modifier.fillMaxSize()) {
                            DesktopNavigation(destination, hebrew, onDestination = { destination = it })
                            AppContent(
                                modifier = Modifier.weight(1f),
                                destination = destination,
                                state = appState,
                                todayIso = todayIso,
                                hebrew = hebrew,
                                onLanguage = { update { it.copy(language = if (hebrew) "en" else "he") } },
                                onToggle = { taskId ->
                                    update { state ->
                                        state.copy(tasks = state.tasks.map { task ->
                                            if (task.id == taskId) task.copy(completed = !task.completed) else task
                                        })
                                    }
                                },
                                onCreate = { showCreate = true },
                            )
                        }
                    } else {
                        Scaffold(
                            bottomBar = {
                                NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                                    Destination.entries.forEach { item ->
                                        NavigationBarItem(
                                            selected = destination == item,
                                            onClick = { destination = item },
                                            icon = { Icon(item.icon, null) },
                                            label = { Text(if (hebrew) item.he else item.en, fontSize = 12.sp) },
                                        )
                                    }
                                }
                            },
                        ) { padding ->
                            AppContent(
                                modifier = Modifier.padding(padding),
                                destination = destination,
                                state = appState,
                                todayIso = todayIso,
                                hebrew = hebrew,
                                onLanguage = { update { it.copy(language = if (hebrew) "en" else "he") } },
                                onToggle = { taskId ->
                                    update { state ->
                                        state.copy(tasks = state.tasks.map { task ->
                                            if (task.id == taskId) task.copy(completed = !task.completed) else task
                                        })
                                    }
                                },
                                onCreate = { showCreate = true },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateScheduleDialog(
            hebrew = hebrew,
            onDismiss = { showCreate = false },
            onCreate = { name, material, english, hebrewReference ->
                val id = "schedule-${appState.schedules.size + 1}"
                val task = LearningPlanner.createFirstAssignment(id, material, english, hebrewReference, todayIso)
                update { state ->
                    state.copy(
                        schedules = state.schedules + StoredSchedule(id, name, material, 1),
                        tasks = state.tasks + StoredTask(
                            task.id, task.scheduleId, task.referenceEnglish, task.referenceHebrew,
                            task.dueDate, task.type.name, task.completed,
                        ),
                    )
                }
                destination = Destination.TODAY
                showCreate = false
            },
        )
    }
}

@Composable
private fun DesktopNavigation(selected: Destination, hebrew: Boolean, onDestination: (Destination) -> Unit) {
    Column(
        modifier = Modifier.width(248.dp).fillMaxHeight().background(DeepBlue).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = WarmGold, modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("ו", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("VeShinantam", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("ושננתם", color = Color.White.copy(alpha = .72f), fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(34.dp))
        Destination.entries.forEach { item ->
            val isSelected = selected == item
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .background(if (isSelected) Color.White.copy(alpha = .14f) else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable { onDestination(item) }.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(item.icon, null, tint = if (isSelected) Color.White else Color.White.copy(alpha = .72f))
                Spacer(Modifier.width(14.dp))
                Text(if (hebrew) item.he else item.en, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, null, tint = Color.White.copy(alpha = .68f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (hebrew) "נשמר במכשיר" else "Saved on this device", color = Color.White.copy(alpha = .72f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun AppContent(
    modifier: Modifier,
    destination: Destination,
    state: WebAppState,
    todayIso: String,
    hebrew: Boolean,
    onLanguage: () -> Unit,
    onToggle: (String) -> Unit,
    onCreate: () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (hebrew) destination.he else destination.en, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = DeepBlue)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onLanguage, modifier = Modifier.semantics { contentDescription = if (hebrew) "Switch to English" else "עבור לעברית" }) {
                Icon(Icons.Default.Language, null, tint = DeepBlue)
            }
            Text(if (hebrew) "EN" else "עברית", color = DeepBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        HorizontalDivider(color = Color(0xFFE4E7EC))
        when (destination) {
            Destination.TODAY -> TodayScreen(state, todayIso, hebrew, onToggle, onCreate)
            Destination.CALENDAR -> CalendarScreen(state, todayIso, hebrew)
            Destination.SCHEDULES -> SchedulesScreen(state, hebrew, onCreate)
            Destination.PROGRESS -> ProgressScreen(state, hebrew)
        }
    }
}

@Composable
private fun TodayScreen(state: WebAppState, today: String, hebrew: Boolean, onToggle: (String) -> Unit, onCreate: () -> Unit) {
    val tasks = LearningPlanner.tasksForDate(state.tasks.map { it.domain() }, today)
    val progress = LearningPlanner.progress(tasks)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (hebrew) "יום לימוד טוב" else "A good day for learning", color = DeepBlue, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                    Text(friendlyDate(today, hebrew), color = MutedInk, fontSize = 15.sp)
                }
                Surface(shape = RoundedCornerShape(99.dp), color = DeepBlueContainer) {
                    Text("${progress.completed}/${progress.total}", modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp), color = DeepBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (hebrew) "התקדמות היום" else "Today’s progress", fontWeight = FontWeight.Bold, color = DeepBlue)
                        Spacer(Modifier.weight(1f))
                        Text("${progress.percent}%", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier.fillMaxWidth().height(9.dp),
                        color = WarmGold,
                        trackColor = Color(0xFFE8EBF1),
                    )
                }
            }
        }
        if (tasks.isEmpty()) {
            item { EmptyToday(hebrew, onCreate) }
        } else {
            state.schedules.forEach { schedule ->
                val scheduleTasks = tasks.filter { it.scheduleId == schedule.id }
                if (scheduleTasks.isNotEmpty()) {
                    item {
                        Text(schedule.name, color = DeepBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 6.dp).semantics { heading() })
                    }
                    item {
                        TaskGroup(
                            title = if (hebrew) "לימוד חדש" else "New learning",
                            tasks = scheduleTasks.filter { it.type == LearningTaskType.LEARNING },
                            hebrew = hebrew,
                            onToggle = onToggle,
                        )
                    }
                    item {
                        TaskGroup(
                            title = if (hebrew) "חזרה" else "Chazarah",
                            tasks = scheduleTasks.filter { it.type == LearningTaskType.CHAZARAH },
                            hebrew = hebrew,
                            onToggle = onToggle,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(onClick = onCreate, containerColor = WarmGold, contentColor = Color.White) {
            Icon(Icons.Default.Add, if (hebrew) "הוסף תוכנית" else "Add schedule")
        }
    }
}

@Composable
private fun TaskGroup(title: String, tasks: List<LearningTask>, hebrew: Boolean, onToggle: (String) -> Unit) {
    if (tasks.isEmpty()) return
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column {
            Text(title, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), color = MutedInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = Color(0xFFEEF0F4))
            tasks.forEachIndexed { index, task ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggle(task.id) }.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = task.completed, onCheckedChange = { onToggle(task.id) })
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (hebrew) task.referenceHebrew else task.referenceEnglish, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (task.completed) MutedInk else Color(0xFF22262D))
                        Text(if (hebrew) task.referenceEnglish else task.referenceHebrew, color = MutedInk, fontSize = 14.sp)
                    }
                    if (task.completed) Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(21.dp))
                }
                if (index != tasks.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = Color(0xFFEEF0F4))
            }
        }
    }
}

@Composable
private fun EmptyToday(hebrew: Boolean, onCreate: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(12.dp))
            Text(if (hebrew) "הכול מוכן להיום" else "You’re all caught up", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onCreate) { Text(if (hebrew) "הוסף תוכנית" else "Add a schedule") }
        }
    }
}

@Composable
private fun CalendarScreen(state: WebAppState, today: String, hebrew: Boolean) {
    val todayParts = today.split("-").mapNotNull { it.toIntOrNull() }
    var year by remember { mutableStateOf(todayParts.getOrElse(0) { 2026 }) }
    var month by remember { mutableStateOf(todayParts.getOrElse(1) { 9 }) }
    val cells = GregorianCalendar.monthCells(year, month)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { GregorianCalendar.previous(year, month).also { year = it.first; month = it.second } }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
                    Text(monthName(month, hebrew) + " $year", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = DeepBlue, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    IconButton(onClick = { GregorianCalendar.next(year, month).also { year = it.first; month = it.second } }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    (if (hebrew) listOf("א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳") else listOf("S", "M", "T", "W", "T", "F", "S")).forEach {
                        Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = MutedInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { cell ->
                            val taskCount = state.tasks.count { it.dueDate == cell.isoDate }
                            val isToday = cell.isoDate == today
                            Box(
                                Modifier.weight(1f).height(58.dp).padding(3.dp)
                                    .background(if (isToday) DeepBlueContainer else Color.Transparent, RoundedCornerShape(12.dp))
                                    .then(if (taskCount > 0 && !isToday) Modifier.border(1.dp, Color(0xFFDDE1E8), RoundedCornerShape(12.dp)) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (cell.day != null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(cell.day.toString(), color = if (isToday) DeepBlue else Color(0xFF333841), fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                                    if (taskCount > 0) Box(Modifier.padding(top = 4.dp).size(6.dp).background(WarmGold, CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(if (hebrew) "הנקודה הזהובה מסמנת יום עם לימוד מתוכנן." else "A gold dot marks a day with scheduled learning.", color = MutedInk, fontSize = 14.sp)
    }
}

@Composable
private fun SchedulesScreen(state: WebAppState, hebrew: Boolean, onCreate: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (hebrew) "תוכניות הלימוד שלך" else "Your learning plans", color = DeepBlue, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                Text(if (hebrew) "${state.schedules.size} תוכניות פעילות" else "${state.schedules.size} active schedules", color = MutedInk)
            }
            Button(onClick = onCreate) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(7.dp)); Text(if (hebrew) "הוסף" else "Add") }
        }
        Spacer(Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.schedules, key = { it.id }) { schedule ->
                val scheduleTasks = state.tasks.filter { it.scheduleId == schedule.id }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = DeepBlueContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.EventNote, null, tint = DeepBlue) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(schedule.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("${schedule.material} · ${if (hebrew) "${schedule.pace} ליום" else "${schedule.pace} per day"}", color = MutedInk, fontSize = 14.sp)
                        }
                        Text("${scheduleTasks.count { it.completed }}/${scheduleTasks.size}", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressScreen(state: WebAppState, hebrew: Boolean) {
    val progress = LearningPlanner.progress(state.tasks.map { it.domain() })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(if (hebrew) "ההתקדמות שלך" else "Your progress", color = DeepBlue, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = DeepBlue), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text(if (hebrew) "הושלם בסך הכול" else "Overall completion", color = Color.White.copy(alpha = .72f))
                Text("${progress.percent}%", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth().height(9.dp), color = WarmGold, trackColor = Color.White.copy(alpha = .18f))
            }
        }
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressMetric(if (hebrew) "משימות שהושלמו" else "Tasks completed", progress.completed.toString())
            ProgressMetric(if (hebrew) "לימוד חדש" else "New learning", progress.learningCompleted.toString())
            ProgressMetric(if (hebrew) "חזרות" else "Chazarah", progress.chazarahCompleted.toString())
            ProgressMetric(if (hebrew) "תוכניות פעילות" else "Active schedules", state.schedules.count { it.active }.toString())
        }
    }
}

@Composable
private fun ProgressMetric(label: String, value: String) {
    Card(modifier = Modifier.width(210.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text(value, color = DeepBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, color = MutedInk, fontSize = 14.sp)
        }
    }
}

@Composable
private fun CreateScheduleDialog(hebrew: Boolean, onDismiss: () -> Unit, onCreate: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var material by remember { mutableStateOf("") }
    var english by remember { mutableStateOf("") }
    var hebrewReference by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hebrew) "תוכנית לימוד חדשה" else "New learning schedule", color = DeepBlue) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (hebrew) "המשימה הראשונה תתווסף להיום. ניתן לסמן אותה מיד." else "Your first assignment will be added for today and ready to check off.", color = MutedInk)
                OutlinedTextField(name, { name = it }, label = { Text(if (hebrew) "שם התוכנית" else "Schedule name") }, singleLine = true)
                OutlinedTextField(material, { material = it }, label = { Text(if (hebrew) "ספר או נושא" else "Sefer or topic") }, singleLine = true)
                OutlinedTextField(english, { english = it }, label = { Text(if (hebrew) "מראה מקום באנגלית" else "English reference") }, singleLine = true)
                OutlinedTextField(hebrewReference, { hebrewReference = it }, label = { Text(if (hebrew) "מראה מקום בעברית" else "Hebrew reference") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name.trim(), material.trim(), english.trim(), hebrewReference.trim()) }, enabled = name.isNotBlank() && material.isNotBlank()) {
                Text(if (hebrew) "צור תוכנית" else "Create schedule")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(if (hebrew) "ביטול" else "Cancel") } },
    )
}

private fun friendlyDate(iso: String, hebrew: Boolean): String {
    val parts = iso.split("-").mapNotNull { it.toIntOrNull() }
    if (parts.size != 3) return iso
    return if (hebrew) "${parts[2]} ב${monthName(parts[1], true)} ${parts[0]}" else "${monthName(parts[1], false)} ${parts[2]}, ${parts[0]}"
}

private fun monthName(month: Int, hebrew: Boolean): String {
    val english = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val translated = listOf("ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני", "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר")
    return (if (hebrew) translated else english).getOrElse(month - 1) { month.toString() }
}
