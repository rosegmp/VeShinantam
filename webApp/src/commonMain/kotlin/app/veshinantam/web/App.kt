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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.veshinantam.shared.GregorianCalendar
import app.veshinantam.shared.IsoDate
import app.veshinantam.shared.LearningPlanner
import app.veshinantam.shared.LearningTask
import app.veshinantam.shared.LearningTaskType
import app.veshinantam.shared.SharedMaterialUnit
import app.veshinantam.shared.SharedScheduleEngine
import app.veshinantam.shared.SharedScheduleRules
import app.veshinantam.web.generated.resources.NotoSansHebrew
import app.veshinantam.web.generated.resources.Res
import org.jetbrains.compose.resources.Font
import kotlinx.coroutines.delay

private val DeepBlue = Color(0xFF173B67)
private val DeepBlueContainer = Color(0xFFDCE9FF)
private val WarmGold = Color(0xFFC59636)
private val AppBackground = Color(0xFFF7F8FC)
private val MutedInk = Color(0xFF5C6370)
private val SuccessGreen = Color(0xFF2E6E55)

@Composable
private fun appTypography(): Typography {
    val fontFamily = FontFamily(
        Font(Res.font.NotoSansHebrew, FontWeight.Normal),
        Font(Res.font.NotoSansHebrew, FontWeight.Medium),
        Font(Res.font.NotoSansHebrew, FontWeight.SemiBold),
        Font(Res.font.NotoSansHebrew, FontWeight.Bold),
    )
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
    )
}

private enum class Destination(val en: String, val he: String, val icon: ImageVector) {
    TODAY("Today", "היום", Icons.Default.Today),
    CALENDAR("Calendar", "לוח שנה", Icons.Default.CalendarMonth),
    SCHEDULES("Schedules", "תוכניות", Icons.AutoMirrored.Filled.EventNote),
    PROGRESS("Progress", "התקדמות", Icons.Default.Insights),
}

private enum class CalendarTaskFilter { ALL, LEARNING, CHAZARAH }

private data class ScheduleDraft(
    val name: String,
    val material: String,
    val referenceEnglish: String,
    val referenceHebrew: String,
    val pace: Int,
    val weekdays: Set<Int>,
    val chazarahOffsets: List<Int>,
)

@Composable
fun WebApp(store: BrowserStore, cloudAccount: CloudAccount) {
    val initialImportResult = remember { store.consumeBackupImport() }
    var appState by remember { mutableStateOf(store.load()) }
    var todayIso by remember { mutableStateOf(store.currentLocalDate()) }
    var destination by remember { mutableStateOf(Destination.TODAY) }
    var showCreate by remember { mutableStateOf(false) }
    var schedulePendingDelete by remember { mutableStateOf<StoredSchedule?>(null) }
    var pendingRestore by remember { mutableStateOf((initialImportResult as? BackupImportResult.Ready)?.state) }
    var showInvalidBackup by remember { mutableStateOf(initialImportResult == BackupImportResult.Invalid) }
    var accountState by remember { mutableStateOf(cloudAccount.state()) }
    var showAccount by remember { mutableStateOf(accountState.status != null || accountState.conflict) }
    val hebrew = appState.language == "he"

    LaunchedEffect(store) {
        while (true) {
            delay(30_000)
            todayIso = store.currentLocalDate()
        }
    }

    LaunchedEffect(showAccount) {
        while (showAccount) {
            accountState = cloudAccount.state()
            delay(250)
        }
    }

    fun update(transform: (WebAppState) -> WebAppState) {
        appState = transform(appState)
        store.save(appState)
    }

    fun toggleTask(state: WebAppState, taskId: String): WebAppState {
        val now = store.currentInstant()
        val zone = store.currentZoneId()
        return state.copy(tasks = state.tasks.map { task ->
            if (task.id != taskId) task else {
                val complete = !task.completed
                task.copy(
                    completed = complete,
                    completedAt = now.takeIf { complete },
                    completionLocalDate = todayIso.takeIf { complete },
                    completionZoneId = zone.takeIf { complete },
                    updatedAt = now,
                )
            }
        })
    }

    CompositionLocalProvider(LocalLayoutDirection provides if (hebrew) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        MaterialTheme(typography = appTypography()) {
            Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
                BoxWithConstraints {
                    val desktop = maxWidth >= 880.dp
                    if (desktop) {
                        Row(Modifier.fillMaxSize()) {
                            DesktopNavigation(destination, hebrew, accountState.email != null, onDestination = { destination = it })
                            AppContent(
                                modifier = Modifier.weight(1f),
                                destination = destination,
                                state = appState,
                                todayIso = todayIso,
                                hebrew = hebrew,
                                onLanguage = { update { it.copy(language = if (hebrew) "en" else "he") } },
                                onSefarimLanguage = { value -> update { it.copy(sefarimLanguage = value) } },
                                onPrimaryCalendar = { value -> update { it.copy(primaryCalendar = value) } },
                                onTodaySortOrder = { value -> update { it.copy(todaySortOrder = value) } },
                                onToggle = { taskId -> update { state -> toggleTask(state, taskId) } },
                                onCreate = { showCreate = true },
                                onSetScheduleActive = { scheduleId, active ->
                                    update { state -> state.copy(schedules = state.schedules.map { if (it.id == scheduleId) it.copy(active = active) else it }) }
                                },
                                onArchiveSchedule = { scheduleId ->
                                    update { state -> state.copy(schedules = state.schedules.map { if (it.id == scheduleId) it.copy(active = false, archived = true) else it }) }
                                },
                                onRestoreSchedule = { scheduleId ->
                                    update { state -> state.copy(schedules = state.schedules.map { if (it.id == scheduleId) it.copy(active = true, archived = false) else it }) }
                                },
                                onRequestDelete = { scheduleId -> schedulePendingDelete = appState.schedules.firstOrNull { it.id == scheduleId } },
                                onExportBackup = { store.exportBackup(appState) },
                                onImportBackup = store::requestBackupImport,
                                onAccount = { accountState = cloudAccount.state(); showAccount = true },
                                hebrewDateLabel = { date -> store.hebrewDateLabel(date, hebrew) },
                                hebrewDayLabel = { date -> store.hebrewDayLabel(date, hebrew) },
                                hebrewCalendarPeriod = { date -> store.hebrewCalendarPeriod(date, hebrew) },
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
                                onSefarimLanguage = { value -> update { it.copy(sefarimLanguage = value) } },
                                onPrimaryCalendar = { value -> update { it.copy(primaryCalendar = value) } },
                                onTodaySortOrder = { value -> update { it.copy(todaySortOrder = value) } },
                                onToggle = { taskId -> update { state -> toggleTask(state, taskId) } },
                                onCreate = { showCreate = true },
                                onSetScheduleActive = { scheduleId, active ->
                                    update { state -> state.copy(schedules = state.schedules.map { if (it.id == scheduleId) it.copy(active = active) else it }) }
                                },
                                onArchiveSchedule = { scheduleId ->
                                    update { state -> state.copy(schedules = state.schedules.map { if (it.id == scheduleId) it.copy(active = false, archived = true) else it }) }
                                },
                                onRestoreSchedule = { scheduleId ->
                                    update { state -> state.copy(schedules = state.schedules.map { if (it.id == scheduleId) it.copy(active = true, archived = false) else it }) }
                                },
                                onRequestDelete = { scheduleId -> schedulePendingDelete = appState.schedules.firstOrNull { it.id == scheduleId } },
                                onExportBackup = { store.exportBackup(appState) },
                                onImportBackup = store::requestBackupImport,
                                onAccount = { accountState = cloudAccount.state(); showAccount = true },
                                hebrewDateLabel = { date -> store.hebrewDateLabel(date, hebrew) },
                                hebrewDayLabel = { date -> store.hebrewDayLabel(date, hebrew) },
                                hebrewCalendarPeriod = { date -> store.hebrewCalendarPeriod(date, hebrew) },
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
            onCreate = { draft ->
                val id = nextScheduleId(appState.schedules)
                val schedule = StoredSchedule(
                    id = id,
                    name = draft.name,
                    material = draft.material,
                    pace = draft.pace,
                    weekdays = draft.weekdays,
                    chazarahOffsets = draft.chazarahOffsets,
                )
                val engine = SharedScheduleEngine()
                val rules = SharedScheduleRules(schedule.weekdays)
                val units = (1..(14 * schedule.pace)).map { ordinal ->
                    SharedMaterialUnit(
                        id = "$id-unit-$ordinal",
                        ordinal = ordinal,
                        labelEnglish = "${draft.referenceEnglish} $ordinal".trim(),
                        labelHebrew = "${draft.referenceHebrew} $ordinal".trim(),
                    )
                }
                val learningTasks = engine.generateByDailyQuantity(
                    units = units,
                    startDate = requireNotNull(IsoDate.parse(todayIso)),
                    unitsPerDay = schedule.pace,
                    rules = rules,
                )
                val plannedTasks = learningTasks + engine.generateChazarah(
                    learningTasks = learningTasks,
                    dayOffsets = schedule.chazarahOffsets,
                    repeatsAnnually = false,
                    rules = rules,
                    annualReviewsThroughYear = requireNotNull(IsoDate.parse(todayIso)).year,
                )
                val generatedTasks = plannedTasks.mapIndexed { index, task ->
                    StoredTask(
                        id = "$id-${task.type.name.lowercase()}-${index + 1}",
                        scheduleId = id,
                        referenceEnglish = task.material.labelEnglish,
                        referenceHebrew = task.material.labelHebrew,
                        dueDate = task.plannedDate.toString(),
                        type = task.type.name,
                        stableKey = task.stableKey,
                        originalLearningDate = task.originalLearningDate.toString(),
                        reviewIdentity = task.reviewIdentity,
                    )
                }
                update { state ->
                    state.copy(
                        schedules = state.schedules + schedule,
                        tasks = state.tasks + generatedTasks,
                    )
                }
                destination = Destination.TODAY
                showCreate = false
            },
        )
    }

    schedulePendingDelete?.let { schedule ->
        AlertDialog(
            onDismissRequest = { schedulePendingDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = Color(0xFF9B2C2C)) },
            title = { Text(if (hebrew) "למחוק את התוכנית?" else "Delete this schedule?") },
            text = {
                Text(
                    if (hebrew) "התוכנית ${schedule.name} וכל משימות הלימוד והחזרה שלה יימחקו לצמיתות מהמכשיר הזה."
                    else "${schedule.name} and all of its learning and chazarah tasks will be permanently removed from this device.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        update { state ->
                            state.copy(
                                schedules = state.schedules.filterNot { it.id == schedule.id },
                                tasks = state.tasks.filterNot { it.scheduleId == schedule.id },
                            )
                        }
                        schedulePendingDelete = null
                    },
                ) { Text(if (hebrew) "מחק לצמיתות" else "Delete permanently") }
            },
            dismissButton = {
                OutlinedButton(onClick = { schedulePendingDelete = null }) { Text(if (hebrew) "ביטול" else "Cancel") }
            },
        )
    }

    pendingRestore?.let { restored ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            icon = { Icon(Icons.Default.UploadFile, null, tint = DeepBlue) },
            title = { Text(if (hebrew) "לשחזר את הגיבוי?" else "Restore this backup?") },
            text = {
                Text(
                    if (hebrew) "הגיבוי מכיל ${restored.schedules.size} תוכניות ו־${restored.tasks.size} משימות. הנתונים הנוכחיים במכשיר זה יוחלפו."
                    else "This backup contains ${restored.schedules.size} schedules and ${restored.tasks.size} tasks. It will replace the current data on this device.",
                )
            },
            confirmButton = {
                Button(onClick = { update { restored }; pendingRestore = null }) {
                    Text(if (hebrew) "שחזר גיבוי" else "Restore backup")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingRestore = null }) { Text(if (hebrew) "ביטול" else "Cancel") }
            },
        )
    }

    if (showInvalidBackup) {
        AlertDialog(
            onDismissRequest = { showInvalidBackup = false },
            title = { Text(if (hebrew) "לא ניתן לקרוא את הגיבוי" else "Backup couldn’t be read") },
            text = {
                Text(
                    if (hebrew) "בחר קובץ גיבוי תקין של VeShinantam בגודל של עד 2MB. הנתונים הנוכחיים לא השתנו."
                    else "Choose a valid VeShinantam backup file up to 2 MB. Your current data was not changed.",
                )
            },
            confirmButton = { Button(onClick = { showInvalidBackup = false }) { Text(if (hebrew) "אישור" else "OK") } },
        )
    }


    if (showAccount) {
        AccountDialog(
            state = accountState,
            hebrew = hebrew,
            onDismiss = { showAccount = false },
            onSignIn = cloudAccount::signIn,
            onCreateAccount = cloudAccount::createAccount,
            onSync = { cloudAccount.sync(appState) },
            onUseCloud = cloudAccount::useCloudCopy,
            onUseDevice = { cloudAccount.replaceCloudCopy(appState) },
            onSignOut = cloudAccount::signOut,
        )
    }
}

@Composable
private fun DesktopNavigation(selected: Destination, hebrew: Boolean, signedIn: Boolean, onDestination: (Destination) -> Unit) {
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
            Icon(if (signedIn) Icons.Default.CloudSync else Icons.Default.CloudOff, null, tint = Color.White.copy(alpha = .68f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (signedIn) { if (hebrew) "סנכרון מופעל" else "Account sync enabled" }
                else { if (hebrew) "נשמר במכשיר" else "Saved on this device" },
                color = Color.White.copy(alpha = .72f), fontSize = 13.sp,
            )
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
    onSefarimLanguage: (String) -> Unit,
    onPrimaryCalendar: (String) -> Unit,
    onTodaySortOrder: (String) -> Unit,
    onToggle: (String) -> Unit,
    onCreate: () -> Unit,
    onSetScheduleActive: (String, Boolean) -> Unit,
    onArchiveSchedule: (String) -> Unit,
    onRestoreSchedule: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onAccount: () -> Unit,
    hebrewDateLabel: (String) -> String,
    hebrewDayLabel: (String) -> String,
    hebrewCalendarPeriod: (String) -> WebCalendarPeriod,
) {
    var showDataMenu by remember { mutableStateOf(false) }
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
            Box {
                IconButton(
                    onClick = { showDataMenu = true },
                    modifier = Modifier.semantics { contentDescription = if (hebrew) "גיבוי ושחזור" else "Backup and restore" },
                ) { Icon(Icons.Default.MoreVert, null, tint = DeepBlue) }
                DropdownMenu(expanded = showDataMenu, onDismissRequest = { showDataMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (hebrew) "שפת מראי המקומות" else "Reference language", fontWeight = FontWeight.Bold) },
                        enabled = false,
                        onClick = {},
                    )
                    listOf(
                        "ENGLISH" to (if (hebrew) "אנגלית" else "English"),
                        "HEBREW" to (if (hebrew) "עברית" else "Hebrew"),
                        "BOTH" to (if (hebrew) "שתיהן" else "Both"),
                    ).forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text((if (state.sefarimLanguage == value) "✓  " else "    ") + label) },
                            onClick = { onSefarimLanguage(value); showDataMenu = false },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(if (hebrew) "לוח שנה ראשי" else "Primary calendar", fontWeight = FontWeight.Bold) },
                        enabled = false,
                        onClick = {},
                    )
                    listOf(
                        "GREGORIAN" to (if (hebrew) "לועזי" else "Gregorian"),
                        "HEBREW" to (if (hebrew) "עברי" else "Hebrew"),
                    ).forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text((if (state.primaryCalendar == value) "✓  " else "    ") + label) },
                            onClick = { onPrimaryCalendar(value); showDataMenu = false },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(if (hebrew) "חשבון וסנכרון" else "Account and sync") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        onClick = { showDataMenu = false; onAccount() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (hebrew) "ייצוא גיבוי" else "Export backup") },
                        leadingIcon = { Icon(Icons.Default.Download, null) },
                        onClick = { showDataMenu = false; onExportBackup() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (hebrew) "שחזור מגיבוי" else "Restore from backup") },
                        leadingIcon = { Icon(Icons.Default.UploadFile, null) },
                        onClick = { showDataMenu = false; onImportBackup() },
                    )
                }
            }
        }
        HorizontalDivider(color = Color(0xFFE4E7EC))
        when (destination) {
            Destination.TODAY -> TodayScreen(state, todayIso, hebrew, onToggle, onCreate, onTodaySortOrder)
            Destination.CALENDAR -> CalendarScreen(
                state = state,
                today = todayIso,
                hebrew = hebrew,
                onToggle = onToggle,
                hebrewDateLabel = hebrewDateLabel,
                hebrewDayLabel = hebrewDayLabel,
                hebrewCalendarPeriod = hebrewCalendarPeriod,
            )
            Destination.SCHEDULES -> SchedulesScreen(
                state, hebrew, onCreate, onSetScheduleActive, onArchiveSchedule, onRestoreSchedule, onRequestDelete,
            )
            Destination.PROGRESS -> ProgressScreen(state, todayIso, hebrew)
        }
    }
}

@Composable
private fun AccountDialog(
    state: CloudAccountState,
    hebrew: Boolean,
    onDismiss: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onCreateAccount: (String, String) -> Unit,
    onSync: () -> Unit,
    onUseCloud: () -> Unit,
    onUseDevice: () -> Unit,
    onSignOut: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val credentialsValid = '@' in email && '.' in email.substringAfterLast('@', "") && password.length >= 6
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (state.email != null) Icons.Default.CloudSync else Icons.Default.Person, null, tint = DeepBlue) },
        title = { Text(if (hebrew) "חשבון וסנכרון" else "Account and sync", color = DeepBlue) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    !state.configured -> Text(
                        if (hebrew) "החיבור לחשבון עדיין לא הוגדר." else "Account sync is ready, but this build is not connected to a Supabase project yet.",
                        color = MutedInk,
                    )
                    state.email == null -> {
                        Text(
                            if (hebrew) "היכנס באמצעות דוא״ל וסיסמה. הנתונים יישארו זמינים גם ללא חיבור." else "Sign in with your email and password. Your data remains available offline.",
                            color = MutedInk,
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            label = { Text(if (hebrew) "דוא״ל" else "Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(if (hebrew) "סיסמה" else "Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(if (hebrew) "יש להשתמש ב־6 תווים לפחות." else "Use at least 6 characters.", color = MutedInk, fontSize = 13.sp)
                        OutlinedButton(
                            onClick = { onCreateAccount(email, password) },
                            enabled = credentialsValid,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (hebrew) "יצירת חשבון" else "Create account") }
                    }
                    state.conflict -> {
                        Text(
                            if (hebrew) "נמצאו שינויים גם בענן וגם במכשיר. בחר איזה עותק לשמור." else "Changes exist in both the cloud and this device. Choose which copy to keep.",
                            color = MutedInk,
                        )
                        OutlinedButton(onClick = onUseCloud, modifier = Modifier.fillMaxWidth()) {
                            Text(if (hebrew) "השתמש בעותק מהענן" else "Use cloud copy")
                        }
                        OutlinedButton(onClick = onUseDevice, modifier = Modifier.fillMaxWidth()) {
                            Text(if (hebrew) "השתמש בעותק מהמכשיר" else "Use this device’s copy")
                        }
                    }
                    else -> {
                        Text(state.email, fontWeight = FontWeight.Bold)
                        Text(
                            if (hebrew) "הנתונים נשמרים קודם במכשיר ומסתנכרנים לפי דרישה." else "Changes are saved on this device first and synchronized on demand.",
                            color = MutedInk,
                        )
                        Button(onClick = onSync, enabled = !state.syncing, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.CloudSync, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (hebrew) "סנכרן עכשיו" else "Sync now")
                        }
                        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                            Text(if (hebrew) "יציאה" else "Sign out")
                        }
                    }
                }
                state.status?.let {
                    Text(
                        it,
                        color = if (it.startsWith("Error") || it.startsWith("Sync failed")) Color(0xFF9B2C2C) else SuccessGreen,
                        fontSize = 14.sp,
                    )
                }
            }
        },
        confirmButton = {
            if (state.configured && state.email == null) {
                Button(onClick = { onSignIn(email, password) }, enabled = credentialsValid) {
                    Text(if (hebrew) "כניסה" else "Sign in")
                }
            } else {
                TextButton(onClick = onDismiss) { Text(if (hebrew) "סגור" else "Close") }
            }
        },
        dismissButton = {
            if (state.configured && state.email == null) TextButton(onClick = onDismiss) { Text(if (hebrew) "ביטול" else "Cancel") }
        },
    )
}

@Composable
private fun TodayScreen(
    state: WebAppState,
    today: String,
    hebrew: Boolean,
    onToggle: (String) -> Unit,
    onCreate: () -> Unit,
    onSortOrder: (String) -> Unit,
) {
    val activeScheduleIds = state.schedules.filter { it.active && !it.archived }.map { it.id }.toSet()
    val sectionedTasks = todayTasks(
        tasks = state.tasks.filter { it.scheduleId in activeScheduleIds },
        today = today,
        sortOrder = state.todaySortOrder,
        preferHebrew = state.sefarimLanguage == "HEBREW" || state.sefarimLanguage == "BOTH" && hebrew,
    )
    val tasks = sectionedTasks.map { it.task.domain() }
    val progress = LearningPlanner.progress(tasks)
    var collapsedSections by remember { mutableStateOf(emptySet<String>()) }
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
            var sortMenuExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { sortMenuExpanded = true }) {
                    Text(todaySortLabel(state.todaySortOrder, hebrew))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                    WebTodaySortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text((if (state.todaySortOrder == order.name) "✓  " else "    ") + todaySortLabel(order.name, hebrew)) },
                            onClick = { onSortOrder(order.name); sortMenuExpanded = false },
                        )
                    }
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
            state.schedules.filter { it.active && !it.archived }.forEach { schedule ->
                val scheduleTasks = sectionedTasks.filter { it.task.scheduleId == schedule.id }
                if (scheduleTasks.isNotEmpty()) {
                    item {
                        Text(schedule.name, color = DeepBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 6.dp).semantics { heading() })
                    }
                    WebTodaySection.entries.forEach { section ->
                        val sectionTasks = scheduleTasks.filter { it.section == section }.map { it.task.domain() }
                        if (sectionTasks.isNotEmpty()) {
                            val sectionKey = "${schedule.id}-${section.name}"
                            val expanded = sectionKey !in collapsedSections
                            item {
                                TaskGroup(
                                    title = todaySectionLabel(section, hebrew),
                                    tasks = sectionTasks,
                                    hebrew = hebrew,
                                    sefarimLanguage = state.sefarimLanguage,
                                    onToggle = onToggle,
                                    expanded = expanded,
                                    onExpandToggle = {
                                        if (expanded) {
                                            collapsedSections = collapsedSections + sectionKey
                                        } else {
                                            collapsedSections = collapsedSections - sectionKey
                                        }
                                    },
                                    showDueDate = section == WebTodaySection.OVERDUE_LEARNING || section == WebTodaySection.OVERDUE_CHAZARAH,
                                )
                            }
                        }
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
private fun TaskGroup(
    title: String,
    tasks: List<LearningTask>,
    hebrew: Boolean,
    sefarimLanguage: String,
    onToggle: (String) -> Unit,
    expanded: Boolean = true,
    onExpandToggle: (() -> Unit)? = null,
    showDueDate: Boolean = false,
) {
    if (tasks.isEmpty()) return
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().then(if (onExpandToggle != null) Modifier.clickable(onClick = onExpandToggle) else Modifier).padding(horizontal = 18.dp, vertical = 14.dp).semantics { heading() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$title (${tasks.size})", modifier = Modifier.weight(1f), color = MutedInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (onExpandToggle != null) Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = DeepBlue)
            }
            if (!expanded) return@Column
            HorizontalDivider(color = Color(0xFFEEF0F4))
            tasks.forEachIndexed { index, task ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggle(task.id) }.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = task.completed, onCheckedChange = { onToggle(task.id) })
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        val primary = when (sefarimLanguage) {
                            "ENGLISH" -> task.referenceEnglish
                            "HEBREW" -> task.referenceHebrew
                            else -> if (hebrew) task.referenceHebrew else task.referenceEnglish
                        }
                        val secondary = if (sefarimLanguage == "BOTH") {
                            if (hebrew) task.referenceEnglish else task.referenceHebrew
                        } else null
                        Text(primary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (task.completed) MutedInk else Color(0xFF22262D))
                        secondary?.takeIf { it.isNotBlank() }?.let { Text(it, color = MutedInk, fontSize = 14.sp) }
                        if (showDueDate) Text(
                            if (hebrew) "לתאריך ${friendlyDate(task.dueDate, true)}" else "Due ${friendlyDate(task.dueDate, false)}",
                            color = Color(0xFF9B2C2C),
                            fontSize = 13.sp,
                        )
                    }
                    if (task.completed) Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(21.dp))
                }
                if (index != tasks.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = Color(0xFFEEF0F4))
            }
        }
    }
}

private fun todaySectionLabel(section: WebTodaySection, hebrew: Boolean): String = when (section) {
    WebTodaySection.NEW_LEARNING -> if (hebrew) "לימוד חדש" else "New learning"
    WebTodaySection.CHAZARAH_TODAY -> if (hebrew) "חזרה להיום" else "Chazarah due today"
    WebTodaySection.OVERDUE_LEARNING -> if (hebrew) "לימוד באיחור" else "Overdue learning"
    WebTodaySection.OVERDUE_CHAZARAH -> if (hebrew) "חזרה באיחור" else "Overdue chazarah"
    WebTodaySection.COMPLETED_TODAY -> if (hebrew) "הושלם היום" else "Completed today"
}

private fun todaySortLabel(value: String, hebrew: Boolean): String = when (value) {
    WebTodaySortOrder.NEWEST_DUE_FIRST.name -> if (hebrew) "החדש ביותר קודם" else "Newest due first"
    WebTodaySortOrder.REFERENCE_ASCENDING.name -> if (hebrew) "מראה מקום עולה" else "Reference A–Z"
    WebTodaySortOrder.REFERENCE_DESCENDING.name -> if (hebrew) "מראה מקום יורד" else "Reference Z–A"
    else -> if (hebrew) "לפי התאריך המתוכנן" else "Scheduled first"
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
private fun CalendarScreen(
    state: WebAppState,
    today: String,
    hebrew: Boolean,
    onToggle: (String) -> Unit,
    hebrewDateLabel: (String) -> String,
    hebrewDayLabel: (String) -> String,
    hebrewCalendarPeriod: (String) -> WebCalendarPeriod,
) {
    val todayParts = today.split("-").mapNotNull { it.toIntOrNull() }
    var periodAnchor by remember { mutableStateOf(today) }
    var selectedDate by remember { mutableStateOf(today) }
    var followsToday by remember { mutableStateOf(true) }
    var selectedScheduleId by remember { mutableStateOf<String?>(null) }
    var taskFilter by remember { mutableStateOf(CalendarTaskFilter.ALL) }
    val period = if (state.primaryCalendar == "HEBREW") {
        hebrewCalendarPeriod(periodAnchor)
    } else {
        val anchor = IsoDate.parse(periodAnchor) ?: IsoDate(todayParts.getOrElse(0) { 2026 }, todayParts.getOrElse(1) { 9 }, 1)
        val start = IsoDate(anchor.year, anchor.month, 1)
        WebCalendarPeriod(
            startDate = start.toString(),
            endDate = IsoDate(anchor.year, anchor.month, GregorianCalendar.daysInMonth(anchor.year, anchor.month)).toString(),
            title = monthName(anchor.month, hebrew) + " ${anchor.year}",
        )
    }
    val cells = calendarRangeCells(period.startDate, period.endDate)
    val filteredTaskType = when (taskFilter) {
        CalendarTaskFilter.ALL -> null
        CalendarTaskFilter.LEARNING -> LearningTaskType.LEARNING.name
        CalendarTaskFilter.CHAZARAH -> LearningTaskType.CHAZARAH.name
    }
    val daySummaries = calendarDaySummaries(state.tasks, selectedScheduleId, filteredTaskType)
    LaunchedEffect(today) {
        if (followsToday) {
            val updatedParts = today.split("-").mapNotNull { it.toIntOrNull() }
            periodAnchor = today
            selectedDate = today
        }
    }
    LaunchedEffect(state.primaryCalendar) {
        periodAnchor = selectedDate
    }
    val selectedTasks = state.tasks.asSequence()
        .filter { it.dueDate == selectedDate }
        .filter { selectedScheduleId == null || it.scheduleId == selectedScheduleId }
        .filter {
            taskFilter == CalendarTaskFilter.ALL ||
                (taskFilter == CalendarTaskFilter.LEARNING && it.type == LearningTaskType.LEARNING.name) ||
                (taskFilter == CalendarTaskFilter.CHAZARAH && it.type == LearningTaskType.CHAZARAH.name)
        }
        .map { it.domain() }
        .toList()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val previous = requireNotNull(IsoDate.parse(period.startDate)).minusDays(1).toString()
                        periodAnchor = previous
                        selectedDate = previous
                        followsToday = false
                    }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, if (hebrew) "החודש הקודם" else "Previous month") }
                    Text(period.title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = DeepBlue, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    IconButton(onClick = {
                        val next = requireNotNull(IsoDate.parse(period.endDate)).plusDays(1).toString()
                        periodAnchor = next
                        selectedDate = next
                        followsToday = false
                    }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, if (hebrew) "החודש הבא" else "Next month") }
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
                            val summary = cell.isoDate?.let(daySummaries::get)
                            val isToday = cell.isoDate == today
                            val isSelected = cell.isoDate == selectedDate
                            Box(
                                Modifier.weight(1f).height(70.dp).padding(3.dp)
                                    .background(
                                        when {
                                            isSelected -> DeepBlue
                                            summary?.status == WebCalendarDayStatus.COMPLETE -> Color(0xFFDFF3E8)
                                            summary?.status == WebCalendarDayStatus.PARTIAL -> Color(0xFFFFF0CE)
                                            summary?.status == WebCalendarDayStatus.INCOMPLETE -> Color(0xFFFFE8E8)
                                            isToday -> DeepBlueContainer
                                            else -> Color.Transparent
                                        },
                                        RoundedCornerShape(12.dp),
                                    )
                                    .then(if (isToday && !isSelected) Modifier.border(2.dp, DeepBlue, RoundedCornerShape(12.dp)) else Modifier)
                                    .semantics {
                                        cell.isoDate?.let { date ->
                                            contentDescription = calendarDayDescription(date, summary, hebrew)
                                        }
                                    }
                                    .clickable(enabled = cell.gregorianDay != null) {
                                        cell.isoDate?.let {
                                            selectedDate = it
                                            followsToday = it == today
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (cell.gregorianDay != null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        if (state.primaryCalendar == "HEBREW") hebrewDayLabel(requireNotNull(cell.isoDate)) else cell.gregorianDay.toString(),
                                        color = if (isSelected) Color.White else Color(0xFF333841),
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    cell.isoDate?.let { date ->
                                        Text(
                                            if (state.primaryCalendar == "HEBREW") cell.gregorianDay.toString() else hebrewDayLabel(date),
                                            color = if (isSelected) Color.White.copy(alpha = .8f) else MutedInk,
                                            fontSize = 11.sp,
                                        )
                                    }
                                    summary?.let {
                                        Text("${it.completedCount}/${it.taskCount}", color = if (isSelected) Color.White else DeepBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CalendarLegend(Color(0xFFFFE8E8), if (hebrew) "לא הושלם" else "Incomplete")
            CalendarLegend(Color(0xFFFFF0CE), if (hebrew) "הושלם חלקית" else "Partial")
            CalendarLegend(Color(0xFFDFF3E8), if (hebrew) "הושלם" else "Complete")
        }
        Spacer(Modifier.height(20.dp))
        val primaryDate = if (state.primaryCalendar == "HEBREW") hebrewDateLabel(selectedDate) else friendlyDate(selectedDate, hebrew)
        val alternateDate = if (state.primaryCalendar == "HEBREW") friendlyDate(selectedDate, hebrew) else hebrewDateLabel(selectedDate)
        Text(primaryDate, color = DeepBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Text(alternateDate, color = MutedInk, fontSize = 15.sp)
        Spacer(Modifier.height(12.dp))
        Text(if (hebrew) "תוכנית" else "Schedule", color = MutedInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            FilterChip(
                selected = selectedScheduleId == null,
                onClick = { selectedScheduleId = null },
                label = { Text(if (hebrew) "הכול" else "All") },
            )
            state.schedules.forEach { schedule ->
                FilterChip(
                    selected = selectedScheduleId == schedule.id,
                    onClick = { selectedScheduleId = schedule.id },
                    label = { Text(schedule.name) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            CalendarTaskFilter.entries.forEach { filter ->
                val label = when (filter) {
                    CalendarTaskFilter.ALL -> if (hebrew) "כל המשימות" else "All tasks"
                    CalendarTaskFilter.LEARNING -> if (hebrew) "לימוד חדש" else "New learning"
                    CalendarTaskFilter.CHAZARAH -> if (hebrew) "חזרה" else "Chazarah"
                }
                FilterChip(selected = taskFilter == filter, onClick = { taskFilter = filter }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(14.dp))
        if (selectedTasks.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Text(
                    if (hebrew) "אין משימות מתוכננות ליום זה." else "No tasks are scheduled for this day.",
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    color = MutedInk,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            state.schedules.forEach { schedule ->
                val scheduleTasks = selectedTasks.filter { it.scheduleId == schedule.id }
                if (scheduleTasks.isNotEmpty()) {
                    Text(schedule.name, color = DeepBlue, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp, bottom = 7.dp))
                    TaskGroup(
                        title = if (hebrew) "משימות היום" else "Day’s tasks",
                        tasks = scheduleTasks,
                        hebrew = hebrew,
                        sefarimLanguage = state.sefarimLanguage,
                        onToggle = onToggle,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, CircleShape).border(1.dp, Color(0xFFD3D7DE), CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(label, color = MutedInk, fontSize = 13.sp)
    }
}

private fun calendarDayDescription(date: String, summary: WebCalendarDaySummary?, hebrew: Boolean): String {
    if (summary == null) return if (hebrew) "$date, אין משימות" else "$date, no tasks"
    val status = when (summary.status) {
        WebCalendarDayStatus.NONE -> if (hebrew) "אין משימות" else "no tasks"
        WebCalendarDayStatus.INCOMPLETE -> if (hebrew) "לא הושלם" else "incomplete"
        WebCalendarDayStatus.PARTIAL -> if (hebrew) "הושלם חלקית" else "partially complete"
        WebCalendarDayStatus.COMPLETE -> if (hebrew) "הושלם" else "complete"
    }
    return if (hebrew) "$date, $status, ${summary.completedCount} מתוך ${summary.taskCount}" else "$date, $status, ${summary.completedCount} of ${summary.taskCount}"
}

private fun nextScheduleId(schedules: List<StoredSchedule>): String {
    val largest = schedules.mapNotNull { it.id.removePrefix("schedule-").toIntOrNull() }.maxOrNull() ?: 0
    return "schedule-${largest + 1}"
}

@Composable
private fun SchedulesScreen(
    state: WebAppState,
    hebrew: Boolean,
    onCreate: () -> Unit,
    onSetActive: (String, Boolean) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (hebrew) "תוכניות הלימוד שלך" else "Your learning plans", color = DeepBlue, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                val activeCount = state.schedules.count { it.active && !it.archived }
                Text(if (hebrew) "$activeCount תוכניות פעילות" else "$activeCount active schedules", color = MutedInk)
            }
            Button(onClick = onCreate) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(7.dp)); Text(if (hebrew) "הוסף" else "Add") }
        }
        Spacer(Modifier.height(20.dp))
        if (state.schedules.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.EventNote, null, tint = DeepBlue, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(if (hebrew) "עדיין אין תוכניות לימוד" else "No learning schedules yet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    TextButton(onClick = onCreate) { Text(if (hebrew) "צור תוכנית" else "Create a schedule") }
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.schedules, key = { it.id }) { schedule ->
                val scheduleTasks = state.tasks.filter { it.scheduleId == schedule.id }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = if (schedule.archived) Color(0xFFE8E9ED) else DeepBlueContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(48.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.EventNote, null, tint = if (schedule.archived) MutedInk else DeepBlue) }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(schedule.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                val stateLabel = when {
                                    schedule.archived -> if (hebrew) "בארכיון" else "Archived"
                                    !schedule.active -> if (hebrew) "מושהה" else "Paused"
                                    else -> if (hebrew) "פעיל" else "Active"
                                }
                                Text("${schedule.material} · ${if (hebrew) "${schedule.pace} ליום" else "${schedule.pace} per day"} · $stateLabel", color = MutedInk, fontSize = 14.sp)
                            }
                            Text("${scheduleTasks.count { it.completed }}/${scheduleTasks.size}", color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                        if (!schedule.archived) {
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFFEEF0F4))
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onSetActive(schedule.id, !schedule.active) }) {
                                    Icon(if (schedule.active) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(19.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (schedule.active) { if (hebrew) "השהה" else "Pause" } else { if (hebrew) "המשך" else "Resume" })
                                }
                                TextButton(onClick = { onArchive(schedule.id) }) {
                                    Icon(Icons.Default.Archive, null, modifier = Modifier.size(19.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (hebrew) "העבר לארכיון" else "Archive")
                                }
                            }
                        } else {
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFFEEF0F4))
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onRestore(schedule.id) }) {
                                    Icon(Icons.Default.Unarchive, null, modifier = Modifier.size(19.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (hebrew) "שחזר" else "Restore")
                                }
                                TextButton(onClick = { onRequestDelete(schedule.id) }) {
                                    Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(19.dp), tint = Color(0xFF9B2C2C))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (hebrew) "מחק" else "Delete", color = Color(0xFF9B2C2C))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressScreen(state: WebAppState, today: String, hebrew: Boolean) {
    val domainTasks = state.tasks.map { it.domain() }
    val progress = LearningPlanner.progress(domainTasks)
    val streak = LearningPlanner.scheduledDayStreak(domainTasks, today)
    val workload = LearningPlanner.upcomingWorkload(domainTasks, today)
    val maximumWorkload = workload.maxOfOrNull { it.total }?.coerceAtLeast(1) ?: 1
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
            ProgressMetric(if (hebrew) "רצף ימי לימוד" else "Scheduled-day streak", streak.toString())
        }
        Spacer(Modifier.height(24.dp))
        Text(if (hebrew) "עומס החזרות הקרוב" else "Upcoming workload", color = DeepBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                workload.forEach { day ->
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(friendlyDate(day.date, hebrew), modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (hebrew) "${day.learning} לימוד · ${day.chazarah} חזרה" else "${day.learning} learning · ${day.chazarah} chazarah",
                                color = MutedInk,
                                fontSize = 13.sp,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { day.total.toFloat() / maximumWorkload },
                            modifier = Modifier.fillMaxWidth().height(7.dp),
                            color = if (day.chazarah > day.learning) WarmGold else DeepBlue,
                            trackColor = Color(0xFFE8EBF1),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(if (hebrew) "לפי תוכנית" else "By schedule", color = DeepBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.schedules.forEach { schedule ->
                val scheduleProgress = LearningPlanner.progress(domainTasks.filter { it.scheduleId == schedule.id })
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(schedule.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(schedule.material, color = MutedInk, fontSize = 14.sp)
                            }
                            Text("${scheduleProgress.completed}/${scheduleProgress.total}", color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { scheduleProgress.fraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = WarmGold,
                            trackColor = Color(0xFFE8EBF1),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
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
private fun CreateScheduleDialog(hebrew: Boolean, onDismiss: () -> Unit, onCreate: (ScheduleDraft) -> Unit) {
    val presets = listOf(
        listOf("Daf Yomi Bavli", "Gemara", "Berachos", "ברכות", "1"),
        listOf("Mishnah Yomis", "Mishnah", "Peah", "פאה", "2"),
        listOf("Rambam Yomi", "Mishneh Torah", "Mishneh Torah", "משנה תורה", "1"),
        listOf(if (hebrew) "מותאם אישית" else "Custom", "", "", "", "1"),
    )
    var selectedPreset by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf(presets[0][0]) }
    var material by remember { mutableStateOf(presets[0][1]) }
    var english by remember { mutableStateOf(presets[0][2]) }
    var hebrewReference by remember { mutableStateOf(presets[0][3]) }
    var paceText by remember { mutableStateOf(presets[0][4]) }
    var weekdays by remember { mutableStateOf((0..6).toSet()) }
    var chazarahEnabled by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hebrew) "תוכנית לימוד חדשה" else "New learning schedule", color = DeepBlue) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (hebrew) "בחר מחזור או בנה תוכנית אישית. ייווצרו 14 ימי לימוד קרובים." else "Choose a cycle or build a custom plan. We’ll create the next 14 learning days.", color = MutedInk)
                Text(if (hebrew) "תוכנית" else "Program", fontWeight = FontWeight.Bold, color = DeepBlue)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    presets.forEachIndexed { index, preset ->
                        FilterChip(
                            selected = selectedPreset == index,
                            onClick = {
                                selectedPreset = index
                                name = preset[0]
                                material = preset[1]
                                english = preset[2]
                                hebrewReference = preset[3]
                                paceText = preset[4]
                            },
                            label = { Text(preset[0]) },
                        )
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text(if (hebrew) "שם התוכנית" else "Schedule name") }, singleLine = true)
                OutlinedTextField(material, { material = it }, label = { Text(if (hebrew) "ספר או נושא" else "Sefer or topic") }, singleLine = true)
                OutlinedTextField(english, { english = it }, label = { Text(if (hebrew) "נקודת התחלה באנגלית" else "English starting reference") }, singleLine = true)
                OutlinedTextField(hebrewReference, { hebrewReference = it }, label = { Text(if (hebrew) "נקודת התחלה בעברית" else "Hebrew starting reference") }, singleLine = true)
                OutlinedTextField(paceText, { value -> paceText = value.filter(Char::isDigit).take(2) }, label = { Text(if (hebrew) "יחידות ליום" else "Units per learning day") }, singleLine = true)
                Text(if (hebrew) "ימי לימוד" else "Learning days", fontWeight = FontWeight.Bold, color = DeepBlue)
                val dayLabels = if (hebrew) listOf("א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳") else listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayLabels.forEachIndexed { index, label ->
                        FilterChip(
                            selected = index in weekdays,
                            onClick = {
                                weekdays = if (index in weekdays && weekdays.size > 1) weekdays - index else weekdays + index
                            },
                            label = { Text(label) },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (hebrew) "חזרה מובנית" else "Built-in chazarah", fontWeight = FontWeight.Bold)
                        Text(if (hebrew) "אחרי יום ואחרי שבעה ימים" else "Review after 1 and 7 days", color = MutedInk, fontSize = 13.sp)
                    }
                    Switch(checked = chazarahEnabled, onCheckedChange = { chazarahEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        ScheduleDraft(
                            name.trim(), material.trim(), english.trim(), hebrewReference.trim(),
                            paceText.toIntOrNull()?.coerceIn(1, 20) ?: 1,
                            weekdays,
                            if (chazarahEnabled) listOf(1, 7) else emptyList(),
                        ),
                    )
                },
                enabled = name.isNotBlank() && material.isNotBlank() && english.isNotBlank() && paceText.toIntOrNull() != null,
            ) {
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
