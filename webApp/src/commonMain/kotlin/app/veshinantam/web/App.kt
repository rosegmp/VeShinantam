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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
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
import app.veshinantam.shared.SharedProgressCalculator
import app.veshinantam.shared.SharedProgressGoal
import app.veshinantam.shared.SharedProgressGoalKind
import app.veshinantam.shared.SharedProgressMilestone
import app.veshinantam.shared.SharedProgressMilestoneKind
import app.veshinantam.shared.SharedProgressTask
import app.veshinantam.shared.SharedSavedGoal
import app.veshinantam.shared.preset.SharedPresetCatalog
import app.veshinantam.shared.preset.SharedPresetProgram
import app.veshinantam.shared.preset.GemaraUnit
import app.veshinantam.shared.preset.MaterialCatalog
import app.veshinantam.shared.preset.MishnahBerurahUnit
import app.veshinantam.shared.preset.MishnahUnit
import app.veshinantam.shared.preset.SeferChoice
import app.veshinantam.shared.preset.UnitReference
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
    val nameHebrew: String,
    val material: String,
    val materialType: String,
    val preset: SharedPresetProgram?,
    val startIndex: Int,
    val units: List<UnitReference>,
    val startDate: IsoDate,
    val targetCompletionDate: IsoDate?,
    val pace: Int,
    val weekdays: Set<Int>,
    val excludedDates: Set<IsoDate>,
    val chazarahOffsets: List<Int>,
    val repeatsAnnually: Boolean,
    val includeWeekendChazarah: Boolean,
    val missedWorkBehavior: String,
    val sourceType: String,
)

private data class CustomSchedulePreview(
    val draft: ScheduleDraft,
    val learningCount: Int,
    val reviewCount: Int,
    val completionDate: String,
)

private data class WebPreferencesEdit(
    val language: String,
    val sefarimLanguage: String,
    val primaryCalendar: String,
    val defaultChazarahOffsets: List<Int>,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val todaySortOrder: String,
    val automaticPresetUpdates: Boolean,
)

private data class BulkCompletionRequest(
    val schedule: StoredSchedule,
    val taskType: LearningTaskType,
    val taskCount: Int,
)

@Composable
fun WebApp(store: BrowserStore, cloudAccount: CloudAccount) {
    val initialImportResult = remember { store.consumeBackupImport() }
    var appState by remember { mutableStateOf(store.load()) }
    var todayIso by remember { mutableStateOf(store.currentLocalDate()) }
    var destination by remember { mutableStateOf(Destination.TODAY) }
    var showCreate by remember { mutableStateOf(false) }
    var schedulePendingDelete by remember { mutableStateOf<StoredSchedule?>(null) }
    var schedulePendingEdit by remember { mutableStateOf<StoredSchedule?>(null) }
    var bulkCompletionRequest by remember { mutableStateOf<BulkCompletionRequest?>(null) }
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

    LaunchedEffect(appState, todayIso) {
        store.updateDueBadge(appState, todayIso)
        store.updateBrowserReminder(appState, todayIso)
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

    fun saveGoals(completion: Double?, streak: Double?, learningUnits: Double?) {
        val now = store.currentInstant()
        val existing = appState.goals.associateBy { it.kind }
        fun stored(kind: SharedProgressGoalKind, target: Double): StoredGoal {
            val prior = existing[kind.name]
            return StoredGoal(kind.name, target, now, revision = prior?.revision ?: 0, id = prior?.id ?: kind.name)
        }
        val goals = listOfNotNull(
            completion?.takeIf { it > 0 }?.let { stored(SharedProgressGoalKind.COMPLETION, it.coerceAtMost(100.0)) },
            streak?.takeIf { it > 0 }?.let { stored(SharedProgressGoalKind.STREAK, it) },
            learningUnits?.takeIf { it > 0 }?.let { stored(SharedProgressGoalKind.LEARNING_UNITS, it) },
        )
        update { it.copy(goals = goals) }
    }

    fun savePreferences(edit: WebPreferencesEdit) {
        if (edit.reminderEnabled) store.requestReminderPermission()
        update { state ->
            state.copy(
                language = edit.language,
                sefarimLanguage = edit.sefarimLanguage,
                primaryCalendar = edit.primaryCalendar,
                defaultChazarahOffsets = edit.defaultChazarahOffsets,
                reminderEnabled = edit.reminderEnabled,
                reminderHour = edit.reminderHour,
                reminderMinute = edit.reminderMinute,
                todaySortOrder = edit.todaySortOrder,
                automaticPresetUpdates = edit.automaticPresetUpdates,
            )
        }
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
                                onSaveGoals = ::saveGoals,
                                onSavePreferences = ::savePreferences,
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
                                onRequestEdit = { scheduleId -> schedulePendingEdit = appState.schedules.firstOrNull { it.id == scheduleId } },
                                onRequestBulkCompletion = { scheduleId, taskType ->
                                    val count = appState.tasks.count {
                                        it.scheduleId == scheduleId && it.type == taskType.name && it.dueDate < todayIso && !it.completed
                                    }
                                    appState.schedules.firstOrNull { it.id == scheduleId }?.takeIf { count > 0 }?.let {
                                        bulkCompletionRequest = BulkCompletionRequest(it, taskType, count)
                                    }
                                },
                                onExportBackup = { store.exportBackup(appState) },
                                onImportBackup = store::requestBackupImport,
                                onPrintSchedule = { dayCount -> store.printSchedule(appState, dayCount) },
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
                                onSaveGoals = ::saveGoals,
                                onSavePreferences = ::savePreferences,
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
                                onRequestEdit = { scheduleId -> schedulePendingEdit = appState.schedules.firstOrNull { it.id == scheduleId } },
                                onRequestBulkCompletion = { scheduleId, taskType ->
                                    val count = appState.tasks.count {
                                        it.scheduleId == scheduleId && it.type == taskType.name && it.dueDate < todayIso && !it.completed
                                    }
                                    appState.schedules.firstOrNull { it.id == scheduleId }?.takeIf { count > 0 }?.let {
                                        bulkCompletionRequest = BulkCompletionRequest(it, taskType, count)
                                    }
                                },
                                onExportBackup = { store.exportBackup(appState) },
                                onImportBackup = store::requestBackupImport,
                                onPrintSchedule = { dayCount -> store.printSchedule(appState, dayCount) },
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
            today = todayIso,
            onDismiss = { showCreate = false },
            onCreate = { draft ->
                val id = nextScheduleId(appState.schedules)
                val schedule = StoredSchedule(
                    id = id,
                    name = draft.name,
                    material = draft.material,
                    pace = if (draft.targetCompletionDate == null) draft.pace else 0,
                    weekdays = draft.weekdays,
                    chazarahOffsets = draft.chazarahOffsets,
                    nameHebrew = draft.nameHebrew,
                    presetId = draft.preset?.id,
                    startDate = draft.startDate.toString(),
                    targetDate = draft.targetCompletionDate?.toString(),
                    missedWorkBehavior = draft.missedWorkBehavior,
                    repeatsAnnually = draft.repeatsAnnually,
                    officialOraysaChazarah = draft.includeWeekendChazarah,
                    createdAt = store.currentInstant(),
                    updatedAt = store.currentInstant(),
                    sourceType = draft.preset?.let { "PRESET:${SharedPresetCatalog.VERSION}" } ?: draft.sourceType,
                    materialType = draft.materialType,
                )
                val engine = SharedScheduleEngine()
                val rules = SharedScheduleRules(schedule.weekdays, draft.excludedDates)
                val sourceUnits: List<UnitReference>? = draft.preset?.units?.subList(draft.startIndex, draft.preset.units.size)
                val units = (sourceUnits ?: draft.units).mapIndexed { index, reference ->
                    SharedMaterialUnit(
                        id = "$id-unit-$index",
                        ordinal = index,
                        labelEnglish = reference.english,
                        labelHebrew = reference.hebrew,
                    )
                }
                val learningTasks = draft.targetCompletionDate?.let { target ->
                    engine.generateByCompletionDate(units, draft.startDate, target, rules)
                } ?: engine.generateByDailyQuantity(units, draft.startDate, schedule.pace, rules)
                val additionalReviews = engine.generateChazarah(
                    learningTasks = learningTasks,
                    dayOffsets = schedule.chazarahOffsets,
                    repeatsAnnually = schedule.repeatsAnnually,
                    rules = rules,
                    annualReviewsThroughYear = draft.startDate.year + if (schedule.repeatsAnnually) 10 else 0,
                )
                val weekendReviews = if (draft.includeWeekendChazarah) {
                    if (draft.preset?.id == "oraysa") engine.generateOfficialOraysaChazarah(learningTasks, rules)
                    else engine.generateWeekendChazarah(learningTasks)
                } else {
                    emptyList()
                }
                val plannedTasks = (learningTasks + weekendReviews + additionalReviews).distinctBy { task ->
                    listOf(task.type.name, task.originalLearningDate.toString(), task.plannedDate.toString(), task.material.labelEnglish)
                }
                val generatedTasks = plannedTasks.mapIndexed { index, task ->
                    StoredTask(
                        id = "$id-${task.type.name.lowercase()}-${index + 1}",
                        scheduleId = id,
                        referenceEnglish = task.material.labelEnglish,
                        referenceHebrew = task.material.labelHebrew,
                        dueDate = task.plannedDate.toString(),
                        type = task.type.name,
                        stableKey = task.stableKey,
                        materialType = draft.materialType,
                        originalLearningDate = task.originalLearningDate.toString(),
                        reviewIdentity = task.reviewIdentity,
                    )
                }
                update { state ->
                    state.copy(
                        schedules = state.schedules + schedule,
                        tasks = state.tasks + generatedTasks,
                        exclusions = state.exclusions + draft.excludedDates.map { date ->
                            StoredExclusion(id, date.toString(), updatedAt = store.currentInstant())
                        },
                    )
                }
                destination = Destination.TODAY
                showCreate = false
            },
        )
    }

    schedulePendingEdit?.let { schedule ->
        EditFutureScheduleDialog(
            schedule = schedule,
            today = todayIso,
            hebrew = hebrew,
            onDismiss = { schedulePendingEdit = null },
            onSave = { edit ->
                update { state -> editFutureSchedule(state, schedule.id, todayIso, edit, store.currentInstant()) }
                schedulePendingEdit = null
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
                                exclusions = state.exclusions.filterNot { it.scheduleId == schedule.id },
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

    bulkCompletionRequest?.let { request ->
        val typeLabel = if (request.taskType == LearningTaskType.LEARNING) {
            if (hebrew) "לימוד" else "learning"
        } else {
            if (hebrew) "חזרה" else "chazarah"
        }
        AlertDialog(
            onDismissRequest = { bulkCompletionRequest = null },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen) },
            title = { Text(if (hebrew) "לסמן משימות קודמות כהושלמו?" else "Complete past tasks?") },
            text = {
                Text(
                    if (hebrew) "${request.taskCount} משימות $typeLabel קודמות בתוכנית ${request.schedule.name} יסומנו כהושלמו היום."
                    else "${request.taskCount} past $typeLabel tasks in ${request.schedule.name} will be marked complete today.",
                )
            },
            confirmButton = {
                Button(onClick = {
                    val now = store.currentInstant()
                    val zone = store.currentZoneId()
                    update { state -> completePastTasks(state, request.schedule.id, request.taskType.name, todayIso, now, zone) }
                    bulkCompletionRequest = null
                }) { Text(if (hebrew) "סמן כהושלם" else "Mark complete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { bulkCompletionRequest = null }) { Text(if (hebrew) "ביטול" else "Cancel") }
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
    onSaveGoals: (Double?, Double?, Double?) -> Unit,
    onSavePreferences: (WebPreferencesEdit) -> Unit,
    onToggle: (String) -> Unit,
    onCreate: () -> Unit,
    onSetScheduleActive: (String, Boolean) -> Unit,
    onArchiveSchedule: (String) -> Unit,
    onRestoreSchedule: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onRequestEdit: (String) -> Unit,
    onRequestBulkCompletion: (String, LearningTaskType) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onPrintSchedule: (Int) -> Unit,
    onAccount: () -> Unit,
    hebrewDateLabel: (String) -> String,
    hebrewDayLabel: (String) -> String,
    hebrewCalendarPeriod: (String) -> WebCalendarPeriod,
) {
    var showDataMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPrintSchedule by remember { mutableStateOf(false) }
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
                        text = { Text(if (hebrew) "הגדרות" else "Settings") },
                        leadingIcon = { Icon(Icons.Default.Settings, null) },
                        onClick = { showDataMenu = false; showSettings = true },
                    )
                    DropdownMenuItem(
                        text = { Text(if (hebrew) "חשבון וסנכרון" else "Account and sync") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        onClick = { showDataMenu = false; onAccount() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (hebrew) "סדר להדפסה" else "Printable schedule") },
                        leadingIcon = { Icon(Icons.Default.Print, null) },
                        onClick = { showDataMenu = false; showPrintSchedule = true },
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
                state, todayIso, hebrew, onCreate, onSetScheduleActive, onArchiveSchedule, onRestoreSchedule, onRequestDelete, onRequestEdit, onRequestBulkCompletion,
            )
            Destination.PROGRESS -> ProgressScreen(state, todayIso, hebrew, onSaveGoals)
        }
    }
    if (showSettings) {
        WebSettingsDialog(
            state = state,
            hebrew = hebrew,
            onSave = { edit -> onSavePreferences(edit); showSettings = false },
            onDismiss = { showSettings = false },
        )
    }
    if (showPrintSchedule) {
        PrintableScheduleDialog(
            hebrew = hebrew,
            onPrint = { dayCount -> showPrintSchedule = false; onPrintSchedule(dayCount) },
            onDismiss = { showPrintSchedule = false },
        )
    }
}

@Composable
private fun PrintableScheduleDialog(
    hebrew: Boolean,
    onPrint: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var dayCount by remember { mutableStateOf(30) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Print, null, tint = DeepBlue) },
        title = { Text(if (hebrew) "סדר להדפסה" else "Printable schedule", color = DeepBlue) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (hebrew) "הדפסת משימות הלימוד והחזרה הפעילות מהיום, עם תיבת סימון לכל משימה."
                    else "Print active learning and chazarah tasks beginning today, with one checkbox for every task.",
                    color = MutedInk,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    PrintableScheduleDayCounts.forEach { count ->
                        FilterChip(
                            selected = dayCount == count,
                            onClick = { dayCount = count },
                            label = { Text(if (hebrew) "$count ימים" else "$count days") },
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onPrint(dayCount) }) { Text(if (hebrew) "הדפסה / PDF" else "Print / PDF") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(if (hebrew) "ביטול" else "Cancel") } },
    )
}

@Composable
private fun WebSettingsDialog(
    state: WebAppState,
    hebrew: Boolean,
    onSave: (WebPreferencesEdit) -> Unit,
    onDismiss: () -> Unit,
) {
    var language by remember(state) { mutableStateOf(state.language) }
    var sefarimLanguage by remember(state) { mutableStateOf(state.sefarimLanguage) }
    var primaryCalendar by remember(state) { mutableStateOf(state.primaryCalendar) }
    var chazarahText by remember(state) { mutableStateOf(state.defaultChazarahOffsets.joinToString(", ")) }
    var reminderEnabled by remember(state) { mutableStateOf(state.reminderEnabled) }
    var reminderHourText by remember(state) { mutableStateOf(state.reminderHour.toString().padStart(2, '0')) }
    var reminderMinuteText by remember(state) { mutableStateOf(state.reminderMinute.toString().padStart(2, '0')) }
    var sortOrder by remember(state) { mutableStateOf(state.todaySortOrder) }
    var automaticPresetUpdates by remember(state) { mutableStateOf(state.automaticPresetUpdates) }
    val offsets = chazarahText.split(',').mapNotNull { value -> value.trim().takeIf { it.isNotEmpty() }?.toIntOrNull() }
    val hour = reminderHourText.toIntOrNull()
    val minute = reminderMinuteText.toIntOrNull()
    val valid = offsets.isNotEmpty() && offsets.size == chazarahText.split(',').count { it.trim().isNotEmpty() } &&
        offsets.all { it in 1..3650 } && offsets.distinct().size == offsets.size &&
        hour != null && hour in 0..23 && minute != null && minute in 0..59
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Settings, null, tint = DeepBlue) },
        title = { Text(if (hebrew) "הגדרות" else "Settings", color = DeepBlue) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsLabel(if (hebrew) "שפת הממשק" else "Interface language")
                SettingsChoices(
                    options = listOf("en" to "English", "he" to "עברית"),
                    selected = language,
                    onSelected = { language = it },
                )
                SettingsLabel(if (hebrew) "שפת מראי המקומות" else "Reference language")
                SettingsChoices(
                    options = listOf("ENGLISH" to "English", "HEBREW" to "עברית", "BOTH" to if (hebrew) "שתיהן" else "Both"),
                    selected = sefarimLanguage,
                    onSelected = { sefarimLanguage = it },
                )
                SettingsLabel(if (hebrew) "לוח שנה ראשי" else "Primary calendar")
                SettingsChoices(
                    options = listOf("GREGORIAN" to if (hebrew) "לועזי" else "Gregorian", "HEBREW" to if (hebrew) "עברי" else "Hebrew"),
                    selected = primaryCalendar,
                    onSelected = { primaryCalendar = it },
                )
                OutlinedTextField(
                    value = chazarahText,
                    onValueChange = { chazarahText = it },
                    label = { Text(if (hebrew) "ימי חזרה כברירת מחדל" else "Default chazarah days") },
                    supportingText = { Text(if (hebrew) "ימים מופרדים בפסיקים, לדוגמה 1, 7, 30, 90" else "Comma-separated days, for example 1, 7, 30, 90") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingsLabel(if (hebrew) "מיון מסך היום" else "Today sorting")
                SettingsChoices(
                    options = WebTodaySortOrder.entries.map { it.name to todaySortLabel(it.name, hebrew) },
                    selected = sortOrder,
                    onSelected = { sortOrder = it },
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (hebrew) "תזכורת יומית" else "Daily reminder", fontWeight = FontWeight.Bold)
                        Text(
                            if (hebrew) "כאשר האתר פתוח, הדפדפן יתריע בשעה שנבחרה. התראות כשהאתר סגור דורשות Web Push ואינן זמינות עדיין."
                            else "While the site is open, the browser will notify at this time. Closed-app reminders require Web Push and are not available yet.",
                            color = MutedInk, fontSize = 12.sp,
                        )
                    }
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = reminderHourText,
                        onValueChange = { reminderHourText = it.filter(Char::isDigit).take(2) },
                        label = { Text(if (hebrew) "שעה" else "Hour") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = reminderMinuteText,
                        onValueChange = { reminderMinuteText = it.filter(Char::isDigit).take(2) },
                        label = { Text(if (hebrew) "דקה" else "Minute") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (hebrew) "עדכוני קטלוג אוטומטיים" else "Automatic catalog updates", fontWeight = FontWeight.Bold)
                        Text(
                            if (hebrew) "ההעדפה מסתנכרנת עם Android; אתר האינטרנט משתמש בקטלוג המאומת המצורף."
                            else "This preference syncs with Android; web uses its bundled validated catalog.",
                            color = MutedInk, fontSize = 12.sp,
                        )
                    }
                    Switch(checked = automaticPresetUpdates, onCheckedChange = { automaticPresetUpdates = it })
                }
                if (!valid) Text(
                    if (hebrew) "בדוק את ימי החזרה ואת שעת התזכורת." else "Check the chazarah days and reminder time.",
                    color = Color(0xFF9B2C2C), fontSize = 13.sp,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        WebPreferencesEdit(
                            language, sefarimLanguage, primaryCalendar, offsets,
                            reminderEnabled, requireNotNull(hour), requireNotNull(minute), sortOrder, automaticPresetUpdates,
                        ),
                    )
                },
            ) { Text(if (hebrew) "שמור" else "Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(if (hebrew) "ביטול" else "Cancel") } },
    )
}

@Composable
private fun SettingsLabel(value: String) {
    Text(value, color = DeepBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}

@Composable
private fun SettingsChoices(options: List<Pair<String, String>>, selected: String, onSelected: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelected(value) }, label = { Text(label) })
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
    today: String,
    hebrew: Boolean,
    onCreate: () -> Unit,
    onSetActive: (String, Boolean) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onRequestEdit: (String) -> Unit,
    onRequestBulkCompletion: (String, LearningTaskType) -> Unit,
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
                val overdueLearning = scheduleTasks.count { it.type == LearningTaskType.LEARNING.name && it.dueDate < today && !it.completed }
                val overdueChazarah = scheduleTasks.count { it.type == LearningTaskType.CHAZARAH.name && it.dueDate < today && !it.completed }
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
                                val timing = if (schedule.pace > 0) {
                                    if (hebrew) "${schedule.pace} ליום" else "${schedule.pace} per day"
                                } else {
                                    if (hebrew) "סיום עד ${schedule.targetDate.orEmpty()}" else "finish by ${schedule.targetDate.orEmpty()}"
                                }
                                Text("${schedule.material} · $timing · $stateLabel", color = MutedInk, fontSize = 14.sp)
                            }
                            Text("${scheduleTasks.count { it.completed }}/${scheduleTasks.size}", color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                        if (!schedule.archived) {
                            if (overdueLearning > 0 || overdueChazarah > 0) {
                                Spacer(Modifier.height(12.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (overdueLearning > 0) {
                                        OutlinedButton(onClick = { onRequestBulkCompletion(schedule.id, LearningTaskType.LEARNING) }) {
                                            Text(if (hebrew) "השלם לימוד קודם ($overdueLearning)" else "Complete past learning ($overdueLearning)")
                                        }
                                    }
                                    if (overdueChazarah > 0) {
                                        OutlinedButton(onClick = { onRequestBulkCompletion(schedule.id, LearningTaskType.CHAZARAH) }) {
                                            Text(if (hebrew) "השלם חזרה קודמת ($overdueChazarah)" else "Complete past chazarah ($overdueChazarah)")
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFFEEF0F4))
                            FlowRow(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onRequestEdit(schedule.id) }) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(19.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (hebrew) "ערוך עתיד" else "Edit future")
                                }
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
private fun EditFutureScheduleDialog(
    schedule: StoredSchedule,
    today: String,
    hebrew: Boolean,
    onDismiss: () -> Unit,
    onSave: (WebFutureScheduleEdit) -> Unit,
) {
    var startDateText by remember(schedule.id) { mutableStateOf(maxOf(schedule.startDate ?: today, today)) }
    var finishBy by remember(schedule.id) { mutableStateOf(false) }
    var paceText by remember(schedule.id) { mutableStateOf(schedule.pace.coerceAtLeast(1).toString()) }
    var targetDateText by remember(schedule.id) { mutableStateOf(maxOf(schedule.targetDate ?: today, startDateText)) }
    var weekdays by remember(schedule.id) { mutableStateOf(schedule.weekdays.ifEmpty { (0..6).toSet() }) }
    var weekendChazarah by remember(schedule.id) { mutableStateOf(schedule.officialOraysaChazarah) }
    val startDate = IsoDate.parse(startDateText)
    val targetDate = IsoDate.parse(targetDateText)
    val pace = paceText.toIntOrNull()
    val valid = startDate != null && startDate >= requireNotNull(IsoDate.parse(today)) && weekdays.isNotEmpty() &&
        if (finishBy) targetDate != null && targetDate >= startDate else pace != null && pace > 0
    val dayLabels = if (hebrew) listOf("א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳")
        else listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hebrew) "עריכת המשך התוכנית" else "Edit future schedule", color = DeepBlue) },
        text = {
            Column(
                Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (hebrew) "רק משימות שלא הושלמו מהיום והלאה ייווצרו מחדש. היסטוריה ומשימות שהושלמו לא ישתנו."
                    else "Only unfinished tasks from today forward will be regenerated. History and completed tasks will not change.",
                    color = MutedInk,
                )
                OutlinedTextField(
                    value = startDateText,
                    onValueChange = { startDateText = it.take(10) },
                    label = { Text(if (hebrew) "תאריך התחלה עתידי" else "Future start date") },
                    supportingText = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !finishBy,
                        onClick = { finishBy = false },
                        label = { Text(if (hebrew) "קצב יומי" else "Daily pace") },
                    )
                    FilterChip(
                        selected = finishBy,
                        onClick = { finishBy = true },
                        label = { Text(if (hebrew) "סיום עד" else "Finish by") },
                    )
                }
                if (finishBy) {
                    OutlinedTextField(
                        value = targetDateText,
                        onValueChange = { targetDateText = it.take(10) },
                        label = { Text(if (hebrew) "תאריך סיום" else "Completion date") },
                        supportingText = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = paceText,
                        onValueChange = { paceText = it.filter(Char::isDigit).take(2) },
                        label = { Text(if (hebrew) "יחידות ליום" else "Units per learning day") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(if (hebrew) "ימי לימוד" else "Learning days", fontWeight = FontWeight.Bold, color = DeepBlue)
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
                        Text(
                            if (schedule.presetId == "oraysa") {
                                if (hebrew) "חזרת אורייתא הרשמית" else "Official Oraysa chazarah"
                            } else {
                                if (hebrew) "חזרת סוף שבוע" else "Weekend chazarah"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Switch(checked = weekendChazarah, onCheckedChange = { weekendChazarah = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        WebFutureScheduleEdit(
                            startDate = startDateText,
                            dailyQuantity = pace ?: schedule.pace.coerceAtLeast(1),
                            targetCompletionDate = targetDateText.takeIf { finishBy },
                            selectedWeekdays = weekdays,
                            includeWeekendChazarah = weekendChazarah,
                        ),
                    )
                },
            ) { Text(if (hebrew) "שמור שינויים" else "Save changes") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(if (hebrew) "ביטול" else "Cancel") } },
    )
}

@Composable
private fun ProgressScreen(
    state: WebAppState,
    today: String,
    hebrew: Boolean,
    onSaveGoals: (Double?, Double?, Double?) -> Unit,
) {
    val domainTasks = state.tasks.map { it.domain() }
    val schedulesById = state.schedules.associateBy { it.id }
    val progress = SharedProgressCalculator.calculate(
        tasks = state.tasks.map { task ->
            SharedProgressTask(
                id = task.id,
                plannedDate = task.dueDate,
                type = LearningTaskType.valueOf(task.type),
                quantity = task.quantity,
                completed = task.completed,
                scheduleActive = schedulesById[task.scheduleId]?.let { it.active && !it.archived } == true,
            )
        },
        today = today,
        savedGoals = state.goals.map { SharedSavedGoal(it.kind, it.target) },
    )
    var editingGoals by remember { mutableStateOf(false) }
    if (editingGoals) {
        ProgressGoalsDialog(
            goals = progress.goals,
            hebrew = hebrew,
            onSave = { completion, streak, units ->
                onSaveGoals(completion, streak, units)
                editingGoals = false
            },
            onDismiss = { editingGoals = false },
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(if (hebrew) "ההתקדמות שלך" else "Your progress", color = DeepBlue, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = DeepBlue), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text(if (hebrew) "הושלם בסך הכול" else "Overall completion", color = Color.White.copy(alpha = .72f))
                Text("${progress.completionPercent}%", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { progress.completionPercent / 100f }, modifier = Modifier.fillMaxWidth().height(9.dp), color = WarmGold, trackColor = Color.White.copy(alpha = .18f))
            }
        }
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressMetric(if (hebrew) "משימות שהושלמו" else "Tasks completed", "${progress.completedDueCount}/${progress.dueCount}")
            ProgressMetric(if (hebrew) "יחידות לימוד" else "Learning units", formatProgressNumber(progress.completedLearningUnits))
            ProgressMetric(if (hebrew) "חזרות" else "Reviews", progress.completedReviews.toString())
            ProgressMetric(if (hebrew) "רצף נוכחי" else "Current streak", progress.currentStreak.toString())
            ProgressMetric(if (hebrew) "הרצף הארוך ביותר" else "Longest streak", progress.longestStreak.toString())
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (hebrew) "יעדים" else "Goals", color = DeepBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
            TextButton(onClick = { editingGoals = true }) {
                Icon(Icons.Default.Edit, null)
                Spacer(Modifier.width(5.dp))
                Text(if (hebrew) "עריכה" else "Edit")
            }
        }
        if (progress.goals.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Text(
                    if (hebrew) "הגדר יעדים לאחוז השלמה, רצף ויחידות לימוד." else "Set goals for completion, streak, and learning units.",
                    modifier = Modifier.fillMaxWidth().padding(18.dp), color = MutedInk,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                progress.goals.forEach { ProgressGoalCard(it, hebrew) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(if (hebrew) "אבני דרך" else "Milestones", color = DeepBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val visibleMilestones = progress.milestones.filter { it.unlocked } +
                SharedProgressMilestoneKind.entries.mapNotNull { kind -> progress.milestones.firstOrNull { it.kind == kind && !it.unlocked } }
            visibleMilestones.distinct().forEach { ProgressMilestoneCard(it, hebrew) }
        }
        Spacer(Modifier.height(24.dp))
        Text(if (hebrew) "עומס החזרות ב־30 הימים הקרובים" else "30-day chazarah workload", color = DeepBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                if (progress.upcomingReviews.isEmpty()) Text(if (hebrew) "אין חזרות מתוכננות." else "No upcoming reviews.", color = MutedInk)
                val maximumWorkload = progress.upcomingReviews.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                progress.upcomingReviews.forEach { day ->
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(friendlyDate(day.date, hebrew), modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (hebrew) "${day.count} חזרות" else "${day.count} reviews", color = MutedInk, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { day.count.toFloat() / maximumWorkload },
                            modifier = Modifier.fillMaxWidth().height(7.dp),
                            color = WarmGold,
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
private fun ProgressGoalsDialog(
    goals: List<SharedProgressGoal>,
    hebrew: Boolean,
    onSave: (Double?, Double?, Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    fun value(kind: SharedProgressGoalKind): String = goals.firstOrNull { it.kind == kind }?.target?.let(::formatProgressNumber).orEmpty()
    var completionText by remember(goals) { mutableStateOf(value(SharedProgressGoalKind.COMPLETION)) }
    var streakText by remember(goals) { mutableStateOf(value(SharedProgressGoalKind.STREAK)) }
    var unitsText by remember(goals) { mutableStateOf(value(SharedProgressGoalKind.LEARNING_UNITS)) }
    var invalid by remember { mutableStateOf(false) }
    fun parsed(value: String): Double? = value.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hebrew) "עריכת יעדים" else "Edit goals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(completionText, { completionText = it; invalid = false }, label = { Text(if (hebrew) "אחוז השלמה" else "Completion percent") }, singleLine = true)
                OutlinedTextField(streakText, { streakText = it; invalid = false }, label = { Text(if (hebrew) "ימי רצף" else "Streak days") }, singleLine = true)
                OutlinedTextField(unitsText, { unitsText = it; invalid = false }, label = { Text(if (hebrew) "יחידות לימוד" else "Learning units") }, singleLine = true)
                if (invalid) Text(if (hebrew) "הזן מספרים חיוביים; אחוז ההשלמה לא יעלה על 100." else "Enter positive numbers; completion cannot exceed 100.", color = Color(0xFF9B2C2C), fontSize = 13.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                val completion = parsed(completionText)
                val streak = parsed(streakText)
                val units = parsed(unitsText)
                val valid = listOf(completionText to completion, streakText to streak, unitsText to units).all { (text, number) ->
                    text.isBlank() || number != null && number > 0
                } && (completion == null || completion <= 100)
                if (valid) onSave(completion, streak, units) else invalid = true
            }) { Text(if (hebrew) "שמור" else "Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(if (hebrew) "ביטול" else "Cancel") } },
    )
}

@Composable
private fun ProgressGoalCard(goal: SharedProgressGoal, hebrew: Boolean) {
    val label = when (goal.kind) {
        SharedProgressGoalKind.COMPLETION -> if (hebrew) "יעד השלמה" else "Completion goal"
        SharedProgressGoalKind.STREAK -> if (hebrew) "יעד רצף" else "Streak goal"
        SharedProgressGoalKind.LEARNING_UNITS -> if (hebrew) "יעד יחידות לימוד" else "Learning-units goal"
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("${formatProgressNumber(goal.current)}/${formatProgressNumber(goal.target)}", color = DeepBlue, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (goal.current / goal.target).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(7.dp), color = WarmGold, trackColor = Color(0xFFE8EBF1),
            )
        }
    }
}

@Composable
private fun ProgressMilestoneCard(milestone: SharedProgressMilestone, hebrew: Boolean) {
    val label = when (milestone.kind) {
        SharedProgressMilestoneKind.STREAK -> if (hebrew) "ימי רצף" else "day streak"
        SharedProgressMilestoneKind.LEARNING_UNITS -> if (hebrew) "יחידות לימוד" else "learning units"
        SharedProgressMilestoneKind.REVIEWS -> if (hebrew) "חזרות" else "reviews"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = if (milestone.unlocked) Color(0xFFFFF0CE) else Color.White),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("${milestone.target} $label", fontWeight = FontWeight.Bold)
            Text(if (milestone.unlocked) (if (hebrew) "הושג" else "Earned") else (if (hebrew) "הבא" else "Next"), color = MutedInk, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (milestone.current / milestone.target).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(7.dp), color = if (milestone.unlocked) WarmGold else DeepBlue, trackColor = Color(0xFFE8EBF1),
            )
        }
    }
}

private fun formatProgressNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

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
private fun CreateScheduleDialog(hebrew: Boolean, today: String, onDismiss: () -> Unit, onCreate: (ScheduleDraft) -> Unit) {
    val programs = remember { SharedPresetCatalog.programs }
    val catalogPositionDate = remember(today) { requireNotNull(IsoDate.parse(today)) }
    var selectedProgramIndex by remember { mutableStateOf(0) }
    val baseProgram = programs.getOrNull(selectedProgramIndex)
    val program = remember(baseProgram, catalogPositionDate) {
        baseProgram?.let { SharedPresetCatalog.programAtDate(it, catalogPositionDate) }
    }
    val custom = program == null
    var programMenuExpanded by remember { mutableStateOf(false) }
    var startIndex by remember(selectedProgramIndex) { mutableStateOf(program?.currentIndex ?: 0) }
    var positionQuery by remember(selectedProgramIndex) { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var seferChoice by remember { mutableStateOf(SeferChoice.GEMARA) }
    var fromSectionIndex by remember { mutableStateOf(0) }
    var toSectionIndex by remember { mutableStateOf(0) }
    var startUnitIndex by remember { mutableStateOf(0) }
    var endUnitIndex by remember { mutableStateOf(Int.MAX_VALUE) }
    var gemaraUnit by remember { mutableStateOf(GemaraUnit.DAF) }
    var mishnahUnit by remember { mutableStateOf(MishnahUnit.MISHNAH) }
    var mishnahBerurahUnit by remember { mutableStateOf(MishnahBerurahUnit.SIMAN) }
    var selectedChelek by remember { mutableStateOf(1) }
    var customUnitText by remember { mutableStateOf("") }
    var customUnits by remember { mutableStateOf(emptyList<UnitReference>()) }
    var customStartDate by remember(today) { mutableStateOf(today) }
    var finishBy by remember { mutableStateOf(false) }
    var targetDate by remember(today) { mutableStateOf(catalogPositionDate.plusDays(30).toString()) }
    var excludedDatesText by remember { mutableStateOf("") }
    var missedWorkBehavior by remember { mutableStateOf("KEEP_FIXED_OVERDUE") }
    var offsetsText by remember { mutableStateOf("1,7") }
    var repeatsAnnually by remember { mutableStateOf(true) }
    var customPreview by remember { mutableStateOf<CustomSchedulePreview?>(null) }
    var paceText by remember(selectedProgramIndex) { mutableStateOf((program?.dailyQuantity ?: 1).toString()) }
    var weekdays by remember(selectedProgramIndex) { mutableStateOf(program?.selectedWeekdays ?: (0..6).toSet()) }
    var chazarahEnabled by remember(selectedProgramIndex) {
        mutableStateOf(program?.let { SharedPresetCatalog.defaultAdditionalChazarahOffsets(it.id).isNotEmpty() } ?: true)
    }
    var weekendChazarah by remember(selectedProgramIndex) { mutableStateOf(program?.id == "oraysa") }
    val currentMasechtaStart = program?.let(SharedPresetCatalog::currentMasechtaStartIndex)
    val positionMatches = if (program != null && positionQuery.length >= 2) {
        program.selectableStartingUnits.withIndex().filter { indexed ->
            indexed.value.english.contains(positionQuery, ignoreCase = true) || indexed.value.hebrew.contains(positionQuery)
        }.takeLast(20)
    } else {
        emptyList()
    }
    val sections = MaterialCatalog.sections(seferChoice)
    val fromSection = sections.getOrNull(fromSectionIndex.coerceIn(0, sections.lastIndex.coerceAtLeast(0)))
    val toSection = sections.getOrNull(toSectionIndex.coerceIn(0, sections.lastIndex.coerceAtLeast(0)))
    val startOptions = runCatching {
        MaterialCatalog.unitOptions(seferChoice, fromSection, gemaraUnit, mishnahUnit, selectedChelek, mishnahBerurahUnit)
    }.getOrDefault(emptyList())
    val boundedStartIndex = startUnitIndex.coerceIn(0, (startOptions.size - 1).coerceAtLeast(0))
    val rawEndOptions = runCatching {
        MaterialCatalog.unitOptions(seferChoice, toSection, gemaraUnit, mishnahUnit, selectedChelek, mishnahBerurahUnit)
    }.getOrDefault(emptyList())
    val sameSection = seferChoice !in MaterialCatalog.sectionedChoices || fromSectionIndex == toSectionIndex
    val endOptions = rawEndOptions.drop(if (sameSection) boundedStartIndex else 0)
    val boundedEndIndex = endUnitIndex.coerceIn(0, (endOptions.size - 1).coerceAtLeast(0))
    val structuredUnits = if (seferChoice == SeferChoice.OTHER) {
        customUnits
    } else {
        runCatching {
            MaterialCatalog.selectedUnits(
                choice = seferChoice,
                fromSectionIndex = fromSectionIndex,
                toSectionIndex = toSectionIndex,
                start = requireNotNull(startOptions.getOrNull(boundedStartIndex)),
                end = requireNotNull(endOptions.getOrNull(boundedEndIndex)),
                gemaraUnit = gemaraUnit,
                mishnahUnit = mishnahUnit,
                chelek = selectedChelek,
                mishnahBerurahUnit = mishnahBerurahUnit,
            )
        }.getOrDefault(emptyList())
    }

    fun buildCustomDraft(): ScheduleDraft? {
        val start = IsoDate.parse(customStartDate) ?: return null
        if (start < catalogPositionDate || customName.isBlank() || structuredUnits.isEmpty() || weekdays.isEmpty()) return null
        val target = if (finishBy) IsoDate.parse(targetDate)?.takeIf { it >= start } ?: return null else null
        val pace = if (finishBy) 1 else (paceText.toIntOrNull()?.takeIf { it in 1..20 } ?: return null)
        val exclusions = excludedDatesText.trim().takeIf { it.isNotEmpty() }
            ?.split(Regex("[,\\s]+"))
            ?.map { IsoDate.parse(it) ?: return null }
            ?.toSet()
            .orEmpty()
        val offsets = if (!chazarahEnabled) emptyList() else offsetsText.split(Regex("[,\\s]+"))
            .filter(String::isNotBlank)
            .map { it.toIntOrNull()?.takeIf { value -> value > 0 } ?: return null }
            .distinct()
        return ScheduleDraft(
            name = customName.trim(),
            nameHebrew = "",
            material = seferChoiceLabel(seferChoice, false),
            materialType = customMaterialType(seferChoice, gemaraUnit, mishnahUnit, mishnahBerurahUnit),
            preset = null,
            startIndex = 0,
            units = structuredUnits,
            startDate = start,
            targetCompletionDate = target,
            pace = pace,
            weekdays = weekdays,
            excludedDates = exclusions,
            chazarahOffsets = offsets,
            repeatsAnnually = chazarahEnabled && repeatsAnnually,
            includeWeekendChazarah = weekendChazarah,
            missedWorkBehavior = missedWorkBehavior,
            sourceType = seferChoice.name,
        )
    }

    val customDraft = if (custom) buildCustomDraft() else null
    val visiblePreview = customPreview?.takeIf { it.draft == customDraft }

    fun generateCustomPreview() {
        val draft = customDraft ?: return
        val engine = SharedScheduleEngine()
        val rules = SharedScheduleRules(draft.weekdays, draft.excludedDates)
        val material = draft.units.mapIndexed { index, reference ->
            SharedMaterialUnit("preview-unit-$index", index, reference.english, reference.hebrew)
        }
        val learning = draft.targetCompletionDate?.let { engine.generateByCompletionDate(material, draft.startDate, it, rules) }
            ?: engine.generateByDailyQuantity(material, draft.startDate, draft.pace, rules)
        val additional = engine.generateChazarah(
            learning, draft.chazarahOffsets, draft.repeatsAnnually, rules,
            draft.startDate.year + if (draft.repeatsAnnually) 10 else 0,
        )
        val weekend = if (draft.includeWeekendChazarah) engine.generateWeekendChazarah(learning) else emptyList()
        val reviews = (additional + weekend).distinctBy {
            listOf(it.originalLearningDate.toString(), it.plannedDate.toString(), it.material.labelEnglish)
        }
        customPreview = CustomSchedulePreview(
            draft = draft,
            learningCount = learning.size,
            reviewCount = reviews.size,
            completionDate = learning.maxOfOrNull { it.plannedDate }?.toString().orEmpty(),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hebrew) "תוכנית לימוד חדשה" else "New learning schedule", color = DeepBlue) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (hebrew) "בחר מחזור מלא או בנה תוכנית אישית. תוכנית מחזור כוללת את כל היחידות מנקודת ההתחלה ועד סוף המחזור."
                    else "Choose a complete cycle or build a custom plan. Presets include every unit from the selected position through the end of the cycle.",
                    color = MutedInk,
                )
                Text(if (hebrew) "תוכנית" else "Program", fontWeight = FontWeight.Bold, color = DeepBlue)
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { programMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(program?.let { if (hebrew) it.nameHebrew else it.nameEnglish } ?: if (hebrew) "מותאם אישית" else "Custom")
                    }
                    DropdownMenu(expanded = programMenuExpanded, onDismissRequest = { programMenuExpanded = false }) {
                        programs.forEachIndexed { index, item ->
                            DropdownMenuItem(
                                text = { Text(if (hebrew) item.nameHebrew else item.nameEnglish) },
                                onClick = { selectedProgramIndex = index; programMenuExpanded = false },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (hebrew) "מותאם אישית" else "Custom") },
                            onClick = { selectedProgramIndex = programs.size; programMenuExpanded = false },
                        )
                    }
                }
                if (program != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = DeepBlueContainer), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (hebrew) "מיקום הקטלוג נכון ל־$catalogPositionDate" else "Catalog position as of $catalogPositionDate",
                                color = MutedInk,
                                fontSize = 13.sp,
                            )
                            Text(
                                if (hebrew) program.currentReference.hebrew else program.currentReference.english,
                                color = DeepBlue,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (hebrew) "${program.dailyQuantity} יחידות, ${program.units.size} יחידות במחזור"
                                else "${program.dailyQuantity} per learning day · ${program.units.size} units in cycle",
                                color = MutedInk,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(
                            selected = startIndex == program.currentIndex,
                            onClick = { startIndex = program.currentIndex; positionQuery = "" },
                            label = { Text(if (hebrew) "המיקום הנוכחי" else "Current position") },
                        )
                        if (currentMasechtaStart != null && currentMasechtaStart < program.currentIndex) {
                            FilterChip(
                                selected = startIndex == currentMasechtaStart,
                                onClick = { startIndex = currentMasechtaStart; positionQuery = "" },
                                label = { Text(if (hebrew) "תחילת המסכת הנוכחית" else "Current tractate start") },
                            )
                        }
                    }
                    Text(
                        (if (hebrew) "נקודת התחלה: " else "Starting position: ") +
                            (if (hebrew) program.units[startIndex].hebrew else program.units[startIndex].english),
                        fontWeight = FontWeight.Bold,
                    )
                    if (startIndex != program.currentIndex) {
                        Text(
                            if (hebrew) "נקבע במקור ל־${SharedPresetCatalog.scheduledDate(program, startIndex, catalogPositionDate)}"
                            else "Originally scheduled for ${SharedPresetCatalog.scheduledDate(program, startIndex, catalogPositionDate)}",
                            color = MutedInk,
                            fontSize = 13.sp,
                        )
                    }
                    OutlinedTextField(
                        value = positionQuery,
                        onValueChange = { positionQuery = it },
                        label = { Text(if (hebrew) "חפש מיקום מוקדם יותר" else "Find an earlier position") },
                        supportingText = { Text(if (hebrew) "חפש באנגלית או בעברית" else "Search in English or Hebrew") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    positionMatches.forEach { indexed ->
                        TextButton(
                            onClick = { startIndex = indexed.index; positionQuery = "" },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (hebrew) indexed.value.hebrew else indexed.value.english,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                } else {
                    OutlinedTextField(customName, { customName = it }, label = { Text(if (hebrew) "שם התוכנית" else "Schedule name") }, singleLine = true)
                    Text(if (hebrew) "חומר לימוד" else "Learning material", fontWeight = FontWeight.Bold, color = DeepBlue)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SeferChoice.entries.forEach { choice ->
                            FilterChip(
                                selected = seferChoice == choice,
                                onClick = {
                                    seferChoice = choice
                                    fromSectionIndex = 0
                                    toSectionIndex = 0
                                    startUnitIndex = 0
                                    endUnitIndex = Int.MAX_VALUE
                                },
                                label = { Text(seferChoiceLabel(choice, hebrew)) },
                            )
                        }
                    }
                    if (seferChoice in MaterialCatalog.sectionedChoices && sections.isNotEmpty()) {
                        CompactSelection(
                            label = if (hebrew) "מספר/חלק ראשון" else "From section",
                            options = sections.map { if (hebrew) it.hebrew else it.english },
                            selectedIndex = fromSectionIndex,
                            onSelected = { index ->
                                fromSectionIndex = index
                                if (toSectionIndex < index) toSectionIndex = index
                                startUnitIndex = 0
                                endUnitIndex = Int.MAX_VALUE
                            },
                        )
                        CompactSelection(
                            label = if (hebrew) "מספר/חלק אחרון" else "To section",
                            options = sections.drop(fromSectionIndex).map { if (hebrew) it.hebrew else it.english },
                            selectedIndex = (toSectionIndex - fromSectionIndex).coerceAtLeast(0),
                            onSelected = { relative -> toSectionIndex = fromSectionIndex + relative; endUnitIndex = Int.MAX_VALUE },
                        )
                    }
                    if (seferChoice == SeferChoice.GEMARA) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GemaraUnit.entries.forEach { unit ->
                                FilterChip(
                                    selected = gemaraUnit == unit,
                                    onClick = { gemaraUnit = unit; startUnitIndex = 0; endUnitIndex = Int.MAX_VALUE },
                                    label = { Text(if (unit == GemaraUnit.DAF) { if (hebrew) "דף" else "Daf" } else { if (hebrew) "עמוד" else "Amud" }) },
                                )
                            }
                        }
                    }
                    if (seferChoice == SeferChoice.MISHNAH) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MishnahUnit.entries.forEach { unit ->
                                FilterChip(
                                    selected = mishnahUnit == unit,
                                    onClick = { mishnahUnit = unit; startUnitIndex = 0; endUnitIndex = Int.MAX_VALUE },
                                    label = { Text(if (unit == MishnahUnit.MISHNAH) { if (hebrew) "משנה" else "Mishnah" } else { if (hebrew) "פרק" else "Perek" }) },
                                )
                            }
                        }
                    }
                    if (seferChoice == SeferChoice.MISHNAH_BERURAH) {
                        Text(if (hebrew) "חלק" else "Chelek", fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..6).forEach { chelek ->
                                FilterChip(
                                    selected = selectedChelek == chelek,
                                    onClick = { selectedChelek = chelek; startUnitIndex = 0; endUnitIndex = Int.MAX_VALUE },
                                    label = { Text(chelek.toString()) },
                                )
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MishnahBerurahUnit.entries.forEach { unit ->
                                FilterChip(
                                    selected = mishnahBerurahUnit == unit,
                                    onClick = { mishnahBerurahUnit = unit; startUnitIndex = 0; endUnitIndex = Int.MAX_VALUE },
                                    label = { Text(unit.name.lowercase().replaceFirstChar(Char::uppercase)) },
                                )
                            }
                        }
                    }
                    if (seferChoice == SeferChoice.OTHER) {
                        OutlinedTextField(
                            value = customUnitText,
                            onValueChange = { customUnitText = it },
                            label = { Text(if (hebrew) "שם היחידה" else "Unit label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            enabled = customUnitText.isNotBlank(),
                            onClick = {
                                val value = customUnitText.trim()
                                customUnits = customUnits + UnitReference(value, value)
                                customUnitText = ""
                            },
                        ) { Text(if (hebrew) "הוסף יחידה" else "Add unit") }
                        customUnits.forEachIndexed { index, unit ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(unit.english, Modifier.weight(1f))
                                TextButton(onClick = { customUnits = customUnits.filterIndexed { unitIndex, _ -> unitIndex != index } }) {
                                    Text(if (hebrew) "הסר" else "Remove")
                                }
                            }
                        }
                    } else if (startOptions.isNotEmpty() && endOptions.isNotEmpty()) {
                        ReferenceSearchPicker(
                            label = if (hebrew) "מיחידה" else "From unit",
                            options = startOptions,
                            selectedIndex = boundedStartIndex,
                            hebrew = hebrew,
                            onSelected = { startUnitIndex = it; endUnitIndex = Int.MAX_VALUE },
                        )
                        ReferenceSearchPicker(
                            label = if (hebrew) "עד יחידה" else "To unit",
                            options = endOptions,
                            selectedIndex = boundedEndIndex,
                            hebrew = hebrew,
                            onSelected = { endUnitIndex = it },
                        )
                        Text(
                            if (hebrew) "${structuredUnits.size} יחידות נבחרו" else "${structuredUnits.size} units selected",
                            color = MutedInk,
                        )
                    }
                    OutlinedTextField(
                        customStartDate,
                        { customStartDate = it.take(10) },
                        label = { Text(if (hebrew) "תאריך התחלה" else "Start date") },
                        supportingText = { Text("YYYY-MM-DD") },
                        singleLine = true,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = !finishBy, onClick = { finishBy = false }, label = { Text(if (hebrew) "קצב יומי" else "Daily pace") })
                        FilterChip(selected = finishBy, onClick = { finishBy = true }, label = { Text(if (hebrew) "סיום עד" else "Finish by") })
                    }
                    if (finishBy) {
                        OutlinedTextField(
                            targetDate,
                            { targetDate = it.take(10) },
                            label = { Text(if (hebrew) "תאריך סיום" else "Completion date") },
                            supportingText = { Text("YYYY-MM-DD") },
                            singleLine = true,
                        )
                    } else {
                        OutlinedTextField(paceText, { value -> paceText = value.filter(Char::isDigit).take(2) }, label = { Text(if (hebrew) "יחידות ליום" else "Units per learning day") }, singleLine = true)
                    }
                    Text(if (hebrew) "ימי לימוד" else "Learning days", fontWeight = FontWeight.Bold, color = DeepBlue)
                    val dayLabels = if (hebrew) listOf("א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳") else listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayLabels.forEachIndexed { index, label ->
                            FilterChip(
                                selected = index in weekdays,
                                onClick = { weekdays = if (index in weekdays && weekdays.size > 1) weekdays - index else weekdays + index },
                                label = { Text(label) },
                            )
                        }
                    }
                    OutlinedTextField(
                        excludedDatesText,
                        { excludedDatesText = it },
                        label = { Text(if (hebrew) "תאריכים ללא לימוד" else "Excluded dates") },
                        supportingText = { Text(if (hebrew) "תאריכי YYYY-MM-DD מופרדים בפסיקים" else "Comma-separated YYYY-MM-DD dates") },
                        singleLine = true,
                    )
                    Text(if (hebrew) "לימוד שהוחמץ" else "Missed learning", fontWeight = FontWeight.Bold, color = DeepBlue)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = missedWorkBehavior == "KEEP_FIXED_OVERDUE",
                            onClick = { missedWorkBehavior = "KEEP_FIXED_OVERDUE" },
                            label = { Text(if (hebrew) "השאר באיחור" else "Keep overdue") },
                        )
                        FilterChip(
                            selected = missedWorkBehavior == "SHIFT_FORWARD",
                            onClick = { missedWorkBehavior = "SHIFT_FORWARD" },
                            label = { Text(if (hebrew) "דחה קדימה" else "Shift forward") },
                        )
                    }
                }
                if (program == null || program.id != "oraysa") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (hebrew) "חזרת סוף שבוע" else "Weekend chazarah", fontWeight = FontWeight.Bold)
                            Text(if (hebrew) "ראשון–שני ביום שישי; שלישי–חמישי בשבת" else "Sunday–Monday on Friday; Tuesday–Thursday on Shabbos", color = MutedInk, fontSize = 13.sp)
                        }
                        Switch(checked = weekendChazarah, onCheckedChange = { weekendChazarah = it })
                    }
                }
                if (program?.id == "oraysa") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (hebrew) "חזרת אורייתא הרשמית" else "Official Oraysa chazarah", fontWeight = FontWeight.Bold)
                            Text(if (hebrew) "חזרה יומית וטווחי סוף שבוע" else "Daily review and weekend ranges", color = MutedInk, fontSize = 13.sp)
                        }
                        Switch(checked = weekendChazarah, onCheckedChange = { weekendChazarah = it })
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (hebrew) "חזרה נוספת" else "Additional chazarah", fontWeight = FontWeight.Bold)
                        Text(if (hebrew) "חזרות במרווחים שתבחר" else "Reviews at selected intervals", color = MutedInk, fontSize = 13.sp)
                    }
                    Switch(checked = chazarahEnabled, onCheckedChange = { chazarahEnabled = it })
                }
                if (program == null && chazarahEnabled) {
                    OutlinedTextField(
                        offsetsText,
                        { offsetsText = it },
                        label = { Text(if (hebrew) "מרווחי חזרה בימים" else "Chazarah day offsets") },
                        supportingText = { Text(if (hebrew) "לדוגמה: 1, 7, 30" else "For example: 1, 7, 30") },
                        singleLine = true,
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (hebrew) "חזרה שנתית" else "Repeat annually", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Switch(checked = repeatsAnnually, onCheckedChange = { repeatsAnnually = it })
                    }
                }
                if (program == null) {
                    visiblePreview?.let { preview ->
                        Card(colors = CardDefaults.cardColors(containerColor = DeepBlueContainer), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (hebrew) "תצוגה מקדימה" else "Preview", color = DeepBlue, fontWeight = FontWeight.Bold)
                                Text(if (hebrew) "${preview.learningCount} משימות לימוד" else "${preview.learningCount} learning tasks")
                                Text(if (hebrew) "${preview.reviewCount} משימות חזרה" else "${preview.reviewCount} chazarah tasks")
                                Text(if (hebrew) "סיום: ${preview.completionDate}" else "Completion: ${preview.completionDate}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selected = program
                    if (selected == null) {
                        val preview = visiblePreview
                        if (preview == null) generateCustomPreview() else onCreate(preview.draft)
                    } else onCreate(
                        ScheduleDraft(
                            name = selected.nameEnglish,
                            nameHebrew = selected.nameHebrew,
                            material = selected.materialType.name,
                            materialType = selected.materialType.name,
                            preset = selected,
                            startIndex = startIndex,
                            units = emptyList(),
                            startDate = catalogPositionDate,
                            targetCompletionDate = null,
                            pace = selected.dailyQuantity,
                            weekdays = selected.selectedWeekdays,
                            excludedDates = selected.excludedDates,
                            chazarahOffsets = if (chazarahEnabled) listOf(1, 7) else emptyList(),
                            repeatsAnnually = false,
                            includeWeekendChazarah = weekendChazarah,
                            missedWorkBehavior = "KEEP_FIXED_OVERDUE",
                            sourceType = "PRESET:${SharedPresetCatalog.VERSION}",
                        ),
                    )
                },
                enabled = !custom || customDraft != null,
            ) {
                Text(
                    if (custom && visiblePreview == null) {
                        if (hebrew) "הצג תצוגה מקדימה" else "Preview schedule"
                    } else {
                        if (hebrew) "צור תוכנית" else "Create schedule"
                    },
                )
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(if (hebrew) "ביטול" else "Cancel") } },
    )
}

@Composable
private fun CompactSelection(label: String, options: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
    var expanded by remember(options) { mutableStateOf(false) }
    val bounded = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = MutedInk, fontSize = 13.sp)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), enabled = options.isNotEmpty()) {
                Text(options.getOrNull(bounded).orEmpty())
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(index); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun ReferenceSearchPicker(
    label: String,
    options: List<UnitReference>,
    selectedIndex: Int,
    hebrew: Boolean,
    onSelected: (Int) -> Unit,
) {
    var query by remember(options) { mutableStateOf("") }
    val bounded = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    val selected = options.getOrNull(bounded)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = MutedInk, fontSize = 13.sp)
        Text(if (hebrew) selected?.hebrew.orEmpty() else selected?.english.orEmpty(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(if (hebrew) "חיפוש" else "Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (query.isNotBlank()) {
            options.withIndex().asSequence()
                .filter { it.value.english.contains(query, true) || it.value.hebrew.contains(query) }
                .take(8)
                .forEach { indexed ->
                    TextButton(onClick = { onSelected(indexed.index); query = "" }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (hebrew) indexed.value.hebrew else indexed.value.english,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
        }
    }
}

private fun seferChoiceLabel(choice: SeferChoice, hebrew: Boolean): String = when (choice) {
    SeferChoice.GEMARA -> if (hebrew) "גמרא" else "Gemara"
    SeferChoice.YERUSHALMI -> if (hebrew) "ירושלמי" else "Yerushalmi"
    SeferChoice.MISHNAH -> if (hebrew) "משנה" else "Mishnah"
    SeferChoice.MISHNAH_BERURAH -> if (hebrew) "משנה ברורה" else "Mishnah Berurah"
    SeferChoice.RAMBAM -> if (hebrew) "רמב״ם" else "Rambam"
    SeferChoice.CHOFETZ_CHAIM -> if (hebrew) "חפץ חיים" else "Chofetz Chaim"
    SeferChoice.TEHILLIM -> if (hebrew) "תהילים" else "Tehillim"
    SeferChoice.KITZUR -> if (hebrew) "קיצור שולחן ערוך" else "Kitzur Shulchan Aruch"
    SeferChoice.OTHER -> if (hebrew) "אחר" else "Other"
}

private fun customMaterialType(
    choice: SeferChoice,
    gemaraUnit: GemaraUnit,
    mishnahUnit: MishnahUnit,
    mishnahBerurahUnit: MishnahBerurahUnit,
): String = when (choice) {
    SeferChoice.GEMARA -> if (gemaraUnit == GemaraUnit.DAF) "DAF" else "AMUD"
    SeferChoice.YERUSHALMI -> "DAF"
    SeferChoice.MISHNAH -> if (mishnahUnit == MishnahUnit.MISHNAH) "MISHNAH" else "PEREK"
    SeferChoice.MISHNAH_BERURAH -> mishnahBerurahUnit.name
    SeferChoice.KITZUR -> "SIMAN"
    SeferChoice.RAMBAM, SeferChoice.TEHILLIM -> "PEREK"
    SeferChoice.CHOFETZ_CHAIM, SeferChoice.OTHER -> "CUSTOM_UNIT"
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
