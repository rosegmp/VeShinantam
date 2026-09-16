package app.veshinantam.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.Switch
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.veshinantam.R
import app.veshinantam.BuildConfig
import app.veshinantam.VeShinantamApplication
import app.veshinantam.data.CustomScheduleDraft
import app.veshinantam.data.FutureScheduleEditDraft
import app.veshinantam.data.PendingSchedulePlan
import app.veshinantam.data.OnboardingSettings
import app.veshinantam.data.PresetScheduleDraft
import app.veshinantam.data.ScheduleUnitInput
import app.veshinantam.data.ScheduleDefaultsSettings
import app.veshinantam.data.backup.BackupService
import app.veshinantam.data.backup.BackupSummary
import app.veshinantam.data.pdf.PrintableScheduleService
import app.veshinantam.data.pdf.PrintableScheduleSummary
import app.veshinantam.data.preset.PresetCatalogUpdateClient
import app.veshinantam.data.preset.PresetCatalogUpdateScheduler
import app.veshinantam.data.preset.PresetUpdateResult
import app.veshinantam.data.preset.PresetUpdateSettings
import app.veshinantam.data.local.ScheduleEntity
import app.veshinantam.data.local.ScheduleExclusionEntity
import app.veshinantam.data.local.TodayTaskRow
import app.veshinantam.data.sync.AccountSyncState
import app.veshinantam.domain.model.ChazarahPattern
import app.veshinantam.domain.model.ChazarahDefaults
import app.veshinantam.domain.model.MissedWorkBehavior
import app.veshinantam.domain.model.MaterialType
import app.veshinantam.domain.model.ScheduleKind
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import app.veshinantam.domain.material.GemaraUnit
import app.veshinantam.domain.material.MishnahUnit
import app.veshinantam.domain.material.MishnahBerurahUnit
import app.veshinantam.domain.material.MaterialCatalog
import app.veshinantam.domain.material.Masechta
import app.veshinantam.domain.material.SeferChoice
import app.veshinantam.domain.material.UnitReference
import app.veshinantam.domain.material.PresetCatalog
import app.veshinantam.shared.preset.SharedPresetCatalog
import app.veshinantam.domain.material.PresetProgram
import app.veshinantam.localization.AppLanguage
import app.veshinantam.localization.BidiText
import app.veshinantam.localization.AppLocale
import app.veshinantam.localization.LanguageSettings
import app.veshinantam.localization.PrimaryCalendar
import app.veshinantam.localization.SefarimDisplay
import app.veshinantam.localization.SefarimLanguage
import app.veshinantam.localization.HebrewNumerals
import app.veshinantam.notifications.ReminderPreference
import app.veshinantam.notifications.ReminderScheduler
import app.veshinantam.notifications.ReminderSettings
import app.veshinantam.ui.calendar.CalendarViewModel
import app.veshinantam.ui.calendar.CalendarDayStatus
import app.veshinantam.ui.calendar.CalendarMonthCalculator
import app.veshinantam.ui.calendar.CalendarTaskStatus
import app.veshinantam.ui.calendar.HebrewDateFormatter
import app.veshinantam.ui.progress.ProgressUiState
import app.veshinantam.ui.progress.ProgressViewModel
import app.veshinantam.ui.progress.MilestoneKind
import app.veshinantam.ui.progress.ProgressMilestone
import app.veshinantam.ui.progress.ProgressGoal
import app.veshinantam.ui.progress.ProgressGoalKind
import app.veshinantam.ui.schedules.SchedulesViewModel
import app.veshinantam.ui.today.TodayScheduleUi
import app.veshinantam.ui.today.TodaySection
import app.veshinantam.ui.today.TodayTaskUi
import app.veshinantam.ui.today.TodayUiState
import app.veshinantam.ui.today.TodayViewModel
import app.veshinantam.ui.today.TodayDisplaySettings
import app.veshinantam.ui.today.TodaySortOrder
import app.veshinantam.ui.today.sortTodayTasks
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import java.text.NumberFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Destination(@param:StringRes val label: Int, val icon: ImageVector) {
    TODAY(R.string.today, Icons.Default.Today),
    CALENDAR(R.string.calendar, Icons.Default.CalendarMonth),
    SCHEDULES(R.string.schedules, Icons.AutoMirrored.Filled.EventNote),
    PROGRESS(R.string.progress, Icons.Default.Insights),
}

private enum class PlanningMode { DAILY_PACE, FINISH_BY }
private enum class ScheduleCreatorMode { PRESET, CUSTOM, WIZARD }
private enum class WizardScheduleKind { PRESET, CUSTOM }
private enum class PastCompletionKind { LEARNING, CHAZARAH }
private data class BackupNotice(val restored: Boolean, val summary: BackupSummary?, val error: String? = null)
private data class PrintNotice(val summary: PrintableScheduleSummary?, val failed: Boolean = false)
private val LocalSefarimLanguage = staticCompositionLocalOf { SefarimLanguage.BOTH }
private val LocalPrimaryCalendar = staticCompositionLocalOf { PrimaryCalendar.GREGORIAN }
private val LocalDefaultChazarahOffsets = staticCompositionLocalOf { ChazarahDefaults.BUILT_IN_OFFSETS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeShinantamApp(openTodayRequest: Int = 0, openAccountRequest: Int = 0) {
    val context = LocalContext.current
    var destination by remember { mutableStateOf(Destination.TODAY) }
    val reminderSettings = remember(context) { ReminderSettings(context.applicationContext) }
    val languageSettings = remember(context) { LanguageSettings(context.applicationContext) }
    val scheduleDefaultsSettings = remember(context) { ScheduleDefaultsSettings(context.applicationContext) }
    val onboardingSettings = remember(context) { OnboardingSettings(context.applicationContext) }
    val application = context.applicationContext as VeShinantamApplication
    val backupService = remember(application) { BackupService(application, application.database.scheduleDao()) }
    val printableScheduleService = remember(application) { PrintableScheduleService(application, application.database.scheduleDao()) }
    val presetUpdateSettings = remember(application) { PresetUpdateSettings(application) }
    val presetUpdateClient = remember(application) { PresetCatalogUpdateClient(application) }
    val accountService = remember(application) { application.supabaseSyncService }
    val coroutineScope = rememberCoroutineScope()
    var reminderPreference by remember { mutableStateOf(reminderSettings.read()) }
    var appLanguage by remember { mutableStateOf(languageSettings.read()) }
    var sefarimLanguage by remember { mutableStateOf(languageSettings.readSefarimLanguage()) }
    var primaryCalendar by remember { mutableStateOf(languageSettings.readPrimaryCalendar()) }
    var defaultChazarahOffsets by remember { mutableStateOf(scheduleDefaultsSettings.readChazarahOffsets()) }
    var showReminderSettings by remember { mutableStateOf(false) }
    var showFirstLaunch by remember { mutableStateOf(!onboardingSettings.isComplete()) }
    var scheduleWizardRequest by remember { mutableStateOf(0) }
    var languageAfterPermission by remember { mutableStateOf<AppLanguage?>(null) }
    var backupNotice by remember { mutableStateOf<BackupNotice?>(null) }
    var printNotice by remember { mutableStateOf<PrintNotice?>(null) }
    var pendingPrintDayCount by remember { mutableStateOf(30) }
    var presetUpdateState by remember { mutableStateOf(presetUpdateSettings.read()) }
    var presetUpdateInProgress by remember { mutableStateOf(false) }
    var accountState by remember { mutableStateOf(accountService.state()) }
    var showAccount by remember { mutableStateOf(false) }
    var accountBusy by remember { mutableStateOf(false) }
    val localizedContext = remember(context, appLanguage) { AppLocale.wrap(context, appLanguage) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        languageAfterPermission?.let { pending ->
            AppLocale.apply(context, pending)
        }
        languageAfterPermission = null
    }
    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) coroutineScope.launch {
            backupNotice = runCatching { withContext(Dispatchers.IO) { backupService.write(uri) } }
                .fold(
                    onSuccess = { BackupNotice(restored = false, summary = it) },
                    onFailure = { BackupNotice(restored = false, summary = null, error = it.message) },
                )
        }
    }
    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) coroutineScope.launch {
            val oldLanguage = appLanguage
            backupNotice = runCatching { withContext(Dispatchers.IO) { backupService.restore(uri) } }
                .fold(
                    onSuccess = { summary ->
                        reminderPreference = reminderSettings.read()
                        appLanguage = languageSettings.read()
                        sefarimLanguage = languageSettings.readSefarimLanguage()
                        primaryCalendar = languageSettings.readPrimaryCalendar()
                        defaultChazarahOffsets = scheduleDefaultsSettings.readChazarahOffsets()
                        presetUpdateState = presetUpdateSettings.read()
                        ReminderScheduler.sync(context.applicationContext, reminderPreference)
                        app.veshinantam.widget.WidgetUpdater.enqueueImmediate(context.applicationContext)
                        accountService.scheduleAutomaticSync()
                        if (appLanguage != oldLanguage) AppLocale.apply(context, appLanguage)
                        BackupNotice(restored = true, summary = summary)
                    },
                    onFailure = { BackupNotice(restored = true, summary = null, error = it.message) },
                )
        }
    }
    val createPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) coroutineScope.launch {
            printNotice = runCatching {
                withContext(Dispatchers.IO) {
                    printableScheduleService.write(uri, LocalDate.now(), pendingPrintDayCount)
                }
            }.fold(
                onSuccess = { PrintNotice(summary = it) },
                onFailure = { PrintNotice(summary = null, failed = true) },
            )
        }
    }
    LaunchedEffect(openTodayRequest) {
        if (openTodayRequest > 0) destination = Destination.TODAY
    }
    LaunchedEffect(openAccountRequest) {
        if (openAccountRequest > 0) {
            accountState = accountService.state()
            showAccount = true
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalResources provides localizedContext.resources,
    ) {
        if (showFirstLaunch) {
            FirstLaunchWelcome(
                selectedLanguage = appLanguage,
                onLanguageSelected = { language ->
                    languageSettings.save(language)
                    accountService.scheduleAutomaticSync()
                    appLanguage = language
                    AppLocale.apply(context, language)
                },
                onUseWizard = {
                    onboardingSettings.markComplete()
                    showFirstLaunch = false
                    destination = Destination.SCHEDULES
                    scheduleWizardRequest++
                },
                onCreateDirectly = {
                    onboardingSettings.markComplete()
                    showFirstLaunch = false
                    destination = Destination.SCHEDULES
                },
                onSkip = {
                    onboardingSettings.markComplete()
                    showFirstLaunch = false
                },
            )
        } else {
            if (showAccount) {
                AccountDialog(
                    state = accountState,
                    busy = accountBusy,
                    onDismiss = { if (!accountBusy) showAccount = false },
                    onSignIn = { email, password ->
                        accountBusy = true
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) { accountService.signInWithPassword(email, password) }
                            accountState = accountService.state()
                            accountBusy = false
                        }
                    },
                    onCreateAccount = { email, password ->
                        accountBusy = true
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) { accountService.createPasswordAccount(email, password) }
                            accountState = accountService.state()
                            accountBusy = false
                        }
                    },
                    onSync = {
                        accountBusy = true
                        coroutineScope.launch {
                            val oldLanguage = appLanguage
                            withContext(Dispatchers.IO) { accountService.sync() }
                            reminderPreference = reminderSettings.read()
                            appLanguage = languageSettings.read()
                            sefarimLanguage = languageSettings.readSefarimLanguage()
                            primaryCalendar = languageSettings.readPrimaryCalendar()
                            defaultChazarahOffsets = scheduleDefaultsSettings.readChazarahOffsets()
                            presetUpdateState = presetUpdateSettings.read()
                            accountState = accountService.state()
                            accountBusy = false
                            if (appLanguage != oldLanguage) AppLocale.apply(context, appLanguage)
                        }
                    },
                    onUseCloud = {
                        accountBusy = true
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) { accountService.useCloudCopy() }
                            accountState = accountService.state()
                            accountBusy = false
                        }
                    },
                    onUseDevice = {
                        accountBusy = true
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) { accountService.replaceCloudCopy() }
                            accountState = accountService.state()
                            accountBusy = false
                        }
                    },
                    onSignOut = {
                        accountService.signOut()
                        accountState = accountService.state()
                    },
                )
            }
            if (showReminderSettings) {
                SettingsDialog(
            initial = reminderPreference,
            initialLanguage = appLanguage,
            initialSefarimLanguage = sefarimLanguage,
            initialPrimaryCalendar = primaryCalendar,
            initialDefaultChazarahOffsets = defaultChazarahOffsets,
            backupNotice = backupNotice,
            printNotice = printNotice,
            presetUpdateState = presetUpdateState,
            presetUpdateInProgress = presetUpdateInProgress,
            presetUpdatesConfigured = BuildConfig.PRESET_CATALOG_UPDATE_URL.isNotBlank(),
            onBackupRequested = {
                backupNotice = null
                createBackupLauncher.launch("VeShinantam-backup-${LocalDate.now()}.json")
            },
            onRestoreRequested = {
                backupNotice = null
                openBackupLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            },
            onPrintRequested = { dayCount ->
                pendingPrintDayCount = dayCount
                printNotice = null
                createPdfLauncher.launch("VeShinantam-schedule-${LocalDate.now()}-$dayCount-days.pdf")
            },
            onCheckPresetUpdates = {
                presetUpdateInProgress = true
                coroutineScope.launch {
                    withContext(Dispatchers.IO) { presetUpdateClient.checkForUpdate() }
                    presetUpdateState = presetUpdateSettings.read()
                    presetUpdateInProgress = false
                }
            },
            onDismiss = { showReminderSettings = false },
            onSave = { preference, language, materialLanguage, calendar, chazarahOffsets, automaticPresetUpdates ->
                val languageChanged = language != appLanguage
                reminderPreference = preference
                appLanguage = language
                sefarimLanguage = materialLanguage
                primaryCalendar = calendar
                defaultChazarahOffsets = chazarahOffsets
                reminderSettings.save(preference)
                languageSettings.save(language)
                languageSettings.saveSefarimLanguage(materialLanguage)
                languageSettings.savePrimaryCalendar(calendar)
                scheduleDefaultsSettings.saveChazarahOffsets(chazarahOffsets)
                presetUpdateSettings.saveAutomatic(automaticPresetUpdates)
                presetUpdateState = presetUpdateSettings.read()
                PresetCatalogUpdateScheduler.sync(context.applicationContext, automaticPresetUpdates)
                app.veshinantam.notifications.ReminderNotifications.createChannel(AppLocale.wrap(context.applicationContext))
                ReminderScheduler.sync(context.applicationContext, preference)
                app.veshinantam.widget.WidgetUpdater.enqueueImmediate(context.applicationContext)
                accountService.scheduleAutomaticSync()
                val needsPermission = preference.enabled &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    languageAfterPermission = language.takeIf { languageChanged }
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else if (languageChanged) {
                    AppLocale.apply(context, language)
                }
                showReminderSettings = false
            },
                )
            }
            Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (destination == Destination.TODAY) stringResource(R.string.app_name) else stringResource(destination.label)) },
                actions = {
                    IconButton(onClick = { accountState = accountService.state(); showAccount = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = stringResource(R.string.account_and_sync))
                    }
                    IconButton(onClick = { showReminderSettings = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.label)) },
                    )
                }
            }
        },
            ) { padding ->
                CompositionLocalProvider(
                    LocalSefarimLanguage provides sefarimLanguage,
                    LocalPrimaryCalendar provides primaryCalendar,
                    LocalDefaultChazarahOffsets provides defaultChazarahOffsets,
                ) {
                    when (destination) {
                        Destination.TODAY -> TodayRoute(Modifier.padding(padding))
                        Destination.CALENDAR -> CalendarRoute(Modifier.padding(padding))
                        Destination.SCHEDULES -> SchedulesRoute(Modifier.padding(padding), scheduleWizardRequest)
                        Destination.PROGRESS -> ProgressRoute(Modifier.padding(padding))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountDialog(
    state: AccountSyncState,
    busy: Boolean,
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
        icon = { Icon(if (state.email == null) Icons.Default.AccountCircle else Icons.Default.CloudSync, null) },
        title = { Text(stringResource(R.string.account_and_sync)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    !state.configured -> Text(stringResource(R.string.account_not_configured))
                    state.email == null -> {
                        Text(stringResource(R.string.account_sign_in_explanation))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            label = { Text(stringResource(R.string.account_email)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.account_password)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            stringResource(R.string.account_password_requirement),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedButton(
                            onClick = { onCreateAccount(email, password) },
                            enabled = !busy && credentialsValid,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.create_account)) }
                    }
                    state.conflict -> {
                        Text(stringResource(R.string.sync_conflict_explanation))
                        OutlinedButton(onClick = onUseCloud, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.use_cloud_copy))
                        }
                        OutlinedButton(onClick = onUseDevice, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.use_device_copy))
                        }
                    }
                    else -> {
                        Text(state.email, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.sync_offline_explanation))
                        Button(onClick = onSync, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            if (busy) CircularProgressIndicator(Modifier.width(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.CloudSync, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.sync_now))
                        }
                        TextButton(onClick = onSignOut, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.sign_out))
                        }
                    }
                }
                state.status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = {
            if (state.configured && state.email == null) {
                Button(
                    onClick = { onSignIn(email, password) },
                    enabled = !busy && credentialsValid,
                ) { Text(stringResource(R.string.sign_in)) }
            } else TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.close)) }
        },
        dismissButton = {
            if (state.configured && state.email == null) TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun FirstLaunchWelcome(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onUseWizard: () -> Unit,
    onCreateDirectly: () -> Unit,
    onSkip: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.welcome_tagline),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.welcome_offline))
                    Text(stringResource(R.string.welcome_bilingual))
                    Text(stringResource(R.string.welcome_private))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedLanguage == AppLanguage.ENGLISH,
                    onClick = { onLanguageSelected(AppLanguage.ENGLISH) },
                    label = { Text("English") },
                )
                FilterChip(
                    selected = selectedLanguage == AppLanguage.HEBREW,
                    onClick = { onLanguageSelected(AppLanguage.HEBREW) },
                    label = { Text("עברית") },
                )
            }
            Button(onClick = onUseWizard, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.use_setup_wizard))
            }
            OutlinedButton(onClick = onCreateDirectly, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.set_up_directly))
            }
            TextButton(onClick = onSkip) { Text(stringResource(R.string.not_now)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    initial: ReminderPreference,
    initialLanguage: AppLanguage,
    initialSefarimLanguage: SefarimLanguage,
    initialPrimaryCalendar: PrimaryCalendar,
    initialDefaultChazarahOffsets: List<Int>,
    backupNotice: BackupNotice?,
    printNotice: PrintNotice?,
    presetUpdateState: app.veshinantam.data.preset.PresetUpdateState,
    presetUpdateInProgress: Boolean,
    presetUpdatesConfigured: Boolean,
    onBackupRequested: () -> Unit,
    onRestoreRequested: () -> Unit,
    onPrintRequested: (Int) -> Unit,
    onCheckPresetUpdates: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (ReminderPreference, AppLanguage, SefarimLanguage, PrimaryCalendar, List<Int>, Boolean) -> Unit,
) {
    var enabled by remember(initial) { mutableStateOf(initial.enabled) }
    var hour by remember(initial) { mutableStateOf(initial.hour) }
    var minute by remember(initial) { mutableStateOf(initial.minute) }
    var language by remember(initialLanguage) { mutableStateOf(initialLanguage) }
    var sefarimLanguage by remember(initialSefarimLanguage) { mutableStateOf(initialSefarimLanguage) }
    var primaryCalendar by remember(initialPrimaryCalendar) { mutableStateOf(initialPrimaryCalendar) }
    var defaultChazarahText by remember(initialDefaultChazarahOffsets) {
        mutableStateOf(ChazarahDefaults.format(initialDefaultChazarahOffsets))
    }
    var choosingTime by remember { mutableStateOf(false) }
    var confirmingRestore by remember { mutableStateOf(false) }
    var printDayCount by remember { mutableStateOf(30) }
    var automaticPresetUpdates by remember(presetUpdateState.automaticUpdates) { mutableStateOf(presetUpdateState.automaticUpdates) }
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val formattedTime = remember(hour, minute, locale) {
        LocalTime.of(hour, minute).format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale),
        )
    }
    val parsedDefaultChazarah = ChazarahDefaults.parse(defaultChazarahText)

    if (choosingTime) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = { choosingTime = false },
            title = { Text(stringResource(R.string.reminder_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hour = timePickerState.hour
                        minute = timePickerState.minute
                        choosingTime = false
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { choosingTime = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
        return
    }

    if (confirmingRestore) {
        AlertDialog(
            onDismissRequest = { confirmingRestore = false },
            title = { Text(stringResource(R.string.restore_backup_title)) },
            text = { Text(stringResource(R.string.restore_backup_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingRestore = false
                    onRestoreRequested()
                }) { Text(stringResource(R.string.choose_backup_file)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingRestore = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.interface_language), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = language == AppLanguage.ENGLISH,
                            onClick = { language = AppLanguage.ENGLISH },
                            label = { Text(stringResource(R.string.english)) },
                        )
                        FilterChip(
                            selected = language == AppLanguage.HEBREW,
                            onClick = { language = AppLanguage.HEBREW },
                            label = { Text(stringResource(R.string.hebrew)) },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.sefarim_language), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = sefarimLanguage == SefarimLanguage.ENGLISH,
                            onClick = { sefarimLanguage = SefarimLanguage.ENGLISH },
                            label = { Text(stringResource(R.string.english)) },
                        )
                        FilterChip(
                            selected = sefarimLanguage == SefarimLanguage.HEBREW,
                            onClick = { sefarimLanguage = SefarimLanguage.HEBREW },
                            label = { Text(stringResource(R.string.hebrew)) },
                        )
                        FilterChip(
                            selected = sefarimLanguage == SefarimLanguage.BOTH,
                            onClick = { sefarimLanguage = SefarimLanguage.BOTH },
                            label = { Text(stringResource(R.string.both_languages)) },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.primary_calendar), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = primaryCalendar == PrimaryCalendar.GREGORIAN,
                            onClick = { primaryCalendar = PrimaryCalendar.GREGORIAN },
                            label = { Text(stringResource(R.string.gregorian_calendar)) },
                        )
                        FilterChip(
                            selected = primaryCalendar == PrimaryCalendar.HEBREW,
                            onClick = { primaryCalendar = PrimaryCalendar.HEBREW },
                            label = { Text(stringResource(R.string.hebrew_calendar)) },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.default_chazarah), style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = defaultChazarahText,
                        onValueChange = { defaultChazarahText = it },
                        label = { Text(stringResource(R.string.default_chazarah_offsets)) },
                        supportingText = {
                            Text(
                                stringResource(
                                    if (parsedDefaultChazarah == null) {
                                        R.string.default_chazarah_invalid
                                    } else {
                                        R.string.default_chazarah_help
                                    },
                                ),
                            )
                        },
                        isError = parsedDefaultChazarah == null,
                        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().toggleable(
                        value = enabled,
                        onValueChange = { enabled = it },
                        role = Role.Switch,
                    ).heightIn(min = 48.dp).semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.daily_reminder))
                        Text(
                            stringResource(if (enabled) R.string.reminder_enabled else R.string.reminder_disabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = null)
                }
                OutlinedButton(
                    onClick = { choosingTime = true },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.reminder_time_value, formattedTime))
                }
                Text(
                    stringResource(R.string.reminder_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_and_restore), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.backup_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onBackupRequested, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.create_backup))
                        }
                        OutlinedButton(onClick = { confirmingRestore = true }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.restore_backup))
                        }
                    }
                    backupNotice?.let { notice ->
                        val message = if (notice.error != null) {
                            stringResource(if (notice.restored) R.string.restore_failed else R.string.backup_failed)
                        } else notice.summary?.let { summary ->
                            stringResource(
                                if (notice.restored) R.string.restore_complete else R.string.backup_complete,
                                summary.scheduleCount,
                                summary.taskCount,
                            )
                        }.orEmpty()
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (notice.error == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.printable_schedule), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.printable_schedule_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrintableScheduleService.ALLOWED_DAY_COUNTS.forEach { count ->
                            FilterChip(
                                selected = printDayCount == count,
                                onClick = { printDayCount = count },
                                label = { Text(stringResource(R.string.print_day_count, count)) },
                            )
                        }
                    }
                    OutlinedButton(onClick = { onPrintRequested(printDayCount) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.create_pdf))
                    }
                    printNotice?.let { notice ->
                        Text(
                            if (notice.failed) {
                                stringResource(R.string.print_pdf_failed)
                            } else {
                                stringResource(
                                    R.string.print_pdf_complete,
                                    notice.summary?.taskCount ?: 0,
                                    notice.summary?.pageCount ?: 0,
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (notice.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.preset_catalog_updates), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.preset_catalog_version, PresetCatalog.VERSION),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!presetUpdatesConfigured) {
                        Text(
                            stringResource(R.string.preset_updates_not_configured),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().toggleable(
                            value = automaticPresetUpdates,
                            enabled = presetUpdatesConfigured,
                            onValueChange = { automaticPresetUpdates = it },
                            role = Role.Switch,
                        ).heightIn(min = 48.dp).semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(R.string.automatic_preset_updates), modifier = Modifier.weight(1f))
                        Switch(
                            checked = automaticPresetUpdates,
                            onCheckedChange = null,
                            enabled = presetUpdatesConfigured,
                        )
                    }
                    OutlinedButton(
                        onClick = onCheckPresetUpdates,
                        enabled = presetUpdatesConfigured && !presetUpdateInProgress,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(if (presetUpdateInProgress) R.string.checking_for_updates else R.string.check_for_updates))
                    }
                    presetUpdateState.lastResult?.let { result ->
                        Text(
                            stringResource(
                                when (result) {
                                    PresetUpdateResult.UPDATED -> R.string.preset_update_installed
                                    PresetUpdateResult.UP_TO_DATE -> R.string.preset_catalog_up_to_date
                                    PresetUpdateResult.NOT_CONFIGURED -> R.string.preset_updates_not_configured
                                    PresetUpdateResult.NETWORK_ERROR -> R.string.preset_update_network_error
                                    PresetUpdateResult.INVALID_SIGNATURE -> R.string.preset_update_invalid_signature
                                    PresetUpdateResult.INVALID_CATALOG -> R.string.preset_update_invalid_catalog
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (result == PresetUpdateResult.INVALID_SIGNATURE || result == PresetUpdateResult.INVALID_CATALOG || result == PresetUpdateResult.NETWORK_ERROR) {
                                MaterialTheme.colorScheme.error
                            } else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        ReminderPreference(enabled, hour, minute),
                        language,
                        sefarimLanguage,
                        primaryCalendar,
                        requireNotNull(parsedDefaultChazarah),
                        automaticPresetUpdates,
                    )
                },
                enabled = parsedDefaultChazarah != null,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ProgressRoute(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as VeShinantamApplication
    val progressViewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory(application.scheduleRepository))
    val state by progressViewModel.state.collectAsStateWithLifecycle()
    ProgressScreen(state, progressViewModel::saveGoals, modifier)
}

@Composable
private fun ProgressScreen(
    state: ProgressUiState,
    onSaveGoals: (Double?, Double?, Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val primaryCalendar = LocalPrimaryCalendar.current
    val resources = LocalContext.current.resources
    val numberFormat = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 } }
    var editingGoals by remember { mutableStateOf(false) }
    if (editingGoals) {
        ProgressGoalsDialog(
            goals = state.goals,
            onSave = { completion, streak, units ->
                onSaveGoals(completion, streak, units)
                editingGoals = false
            },
            onDismiss = { editingGoals = false },
        )
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.progress_overview), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.completion_percent, state.completionPercent), style = MaterialTheme.typography.headlineMedium)
                    LinearProgressIndicator(
                        progress = { state.completionPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.completed_due_tasks, state.completedDueCount, state.dueCount),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProgressMetricCard(
                    value = resources.getQuantityString(R.plurals.scheduled_day_count, state.currentStreak, state.currentStreak),
                    label = stringResource(R.string.current_streak),
                    modifier = Modifier.weight(1f),
                )
                ProgressMetricCard(
                    value = numberFormat.format(state.completedLearningUnits),
                    label = stringResource(R.string.learning_units_completed),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            ProgressMetricCard(
                value = state.completedReviews.toString(),
                label = stringResource(R.string.reviews_completed),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.personal_goals),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                TextButton(onClick = { editingGoals = true }) {
                    Text(stringResource(if (state.goals.isEmpty()) R.string.set_goals else R.string.edit_goals))
                }
            }
        }
        if (state.goals.isEmpty()) {
            item { MessageCard(R.string.no_personal_goals) }
        } else {
            items(state.goals, key = { it.kind }) { goal -> GoalCard(goal, numberFormat) }
        }
        item {
            Text(
                stringResource(R.string.milestones),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp).semantics { heading() },
            )
        }
        val visibleMilestones = state.milestones.filter { it.unlocked } +
            MilestoneKind.entries.mapNotNull { kind -> state.milestones.firstOrNull { it.kind == kind && !it.unlocked } }
        if (visibleMilestones.isEmpty()) {
            item { MessageCard(R.string.no_milestones) }
        } else {
            items(visibleMilestones.distinct(), key = { "${it.kind}-${it.target}" }) { milestone ->
                MilestoneCard(milestone, numberFormat)
            }
        }
        item {
            Text(
                stringResource(R.string.upcoming_reviews_30),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp).semantics { heading() },
            )
        }
        if (state.upcomingReviews.isEmpty()) {
            item { MessageCard(R.string.no_upcoming_reviews) }
        } else {
            items(state.upcomingReviews, key = { it.date }) { workload ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(displayDate(workload.date, primaryCalendar, locale))
                        Text(
                            resources.getQuantityString(R.plurals.review_count, workload.count, workload.count),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressGoalsDialog(
    goals: List<ProgressGoal>,
    onSave: (Double?, Double?, Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    fun value(kind: ProgressGoalKind): String = goals.firstOrNull { it.kind == kind }?.target?.let {
        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
    }.orEmpty()
    var completionText by remember(goals) { mutableStateOf(value(ProgressGoalKind.COMPLETION)) }
    var streakText by remember(goals) { mutableStateOf(value(ProgressGoalKind.STREAK)) }
    var unitsText by remember(goals) { mutableStateOf(value(ProgressGoalKind.LEARNING_UNITS)) }
    var invalid by remember { mutableStateOf(false) }
    fun parsed(text: String): Double? = text.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.personal_goals)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.goals_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = completionText,
                    onValueChange = { completionText = it; invalid = false },
                    label = { Text(stringResource(R.string.completion_goal_percent)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left),
                )
                OutlinedTextField(
                    value = streakText,
                    onValueChange = { streakText = it; invalid = false },
                    label = { Text(stringResource(R.string.streak_goal_days)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left),
                )
                OutlinedTextField(
                    value = unitsText,
                    onValueChange = { unitsText = it; invalid = false },
                    label = { Text(stringResource(R.string.learning_units_goal)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left),
                )
                if (invalid) Text(stringResource(R.string.goal_validation_error), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val completion = parsed(completionText)
                val streak = parsed(streakText)
                val units = parsed(unitsText)
                val valuesValid = listOf(completionText to completion, streakText to streak, unitsText to units)
                    .all { (text, number) -> text.isBlank() || number != null && number > 0 }
                if (valuesValid && (completion == null || completion <= 100)) onSave(completion, streak, units) else invalid = true
            }) { Text(stringResource(R.string.save_goals)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun GoalCard(goal: ProgressGoal, numberFormat: NumberFormat) {
    val label = when (goal.kind) {
        ProgressGoalKind.COMPLETION -> stringResource(R.string.completion_goal)
        ProgressGoalKind.STREAK -> stringResource(R.string.streak_goal)
        ProgressGoalKind.LEARNING_UNITS -> stringResource(R.string.learning_units_goal_label)
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.goal_progress, numberFormat.format(goal.current), numberFormat.format(goal.target)))
            }
            LinearProgressIndicator(
                progress = { (goal.current / goal.target).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MilestoneCard(milestone: ProgressMilestone, numberFormat: NumberFormat) {
    val label = when (milestone.kind) {
        MilestoneKind.STREAK -> stringResource(R.string.streak_milestone)
        MilestoneKind.LEARNING_UNITS -> stringResource(R.string.learning_milestone)
        MilestoneKind.REVIEWS -> stringResource(R.string.review_milestone)
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (milestone.unlocked) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.milestone_target, label, milestone.target), fontWeight = FontWeight.Bold)
                Text(stringResource(if (milestone.unlocked) R.string.milestone_earned else R.string.milestone_next))
            }
            LinearProgressIndicator(
                progress = { (milestone.current / milestone.target).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.milestone_progress, numberFormat.format(milestone.current), milestone.target),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressMetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class CalendarDisplayPeriod(val start: LocalDate, val endInclusive: LocalDate)

private fun calendarPeriodContaining(date: LocalDate, calendar: PrimaryCalendar): CalendarDisplayPeriod =
    if (calendar == PrimaryCalendar.HEBREW) {
        HebrewDateFormatter.monthContaining(date).let { CalendarDisplayPeriod(it.start, it.endInclusive) }
    } else {
        YearMonth.from(date).let { CalendarDisplayPeriod(it.atDay(1), it.atEndOfMonth()) }
    }

private fun previousCalendarPeriod(period: CalendarDisplayPeriod, calendar: PrimaryCalendar): CalendarDisplayPeriod =
    calendarPeriodContaining(period.start.minusDays(1), calendar)

private fun nextCalendarPeriod(period: CalendarDisplayPeriod, calendar: PrimaryCalendar): CalendarDisplayPeriod =
    calendarPeriodContaining(period.endInclusive.plusDays(1), calendar)

private fun calendarPeriodTitle(period: CalendarDisplayPeriod, calendar: PrimaryCalendar, locale: Locale): String =
    if (calendar == PrimaryCalendar.HEBREW) {
        HebrewDateFormatter.monthTitle(HebrewDateFormatter.monthContaining(period.start), locale)
    } else {
        period.start.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    }

private fun calendarDayLabel(date: LocalDate, calendar: PrimaryCalendar, locale: Locale): String =
    if (calendar == PrimaryCalendar.HEBREW) HebrewDateFormatter.dayLabel(date, locale) else date.dayOfMonth.toString()

private fun weekdayLabel(day: DayOfWeek, locale: Locale): String = if (locale.language == "he") {
    mapOf(
        DayOfWeek.SUNDAY to "א׳", DayOfWeek.MONDAY to "ב׳", DayOfWeek.TUESDAY to "ג׳",
        DayOfWeek.WEDNESDAY to "ד׳", DayOfWeek.THURSDAY to "ה׳", DayOfWeek.FRIDAY to "ו׳",
        DayOfWeek.SATURDAY to "ש׳",
    ).getValue(day)
} else {
    day.getDisplayName(TextStyle.NARROW, locale)
}

private fun alternateCalendar(calendar: PrimaryCalendar): PrimaryCalendar =
    if (calendar == PrimaryCalendar.HEBREW) PrimaryCalendar.GREGORIAN else PrimaryCalendar.HEBREW

private fun displayDate(
    date: LocalDate,
    calendar: PrimaryCalendar,
    locale: Locale,
    style: FormatStyle = FormatStyle.MEDIUM,
): String = if (calendar == PrimaryCalendar.HEBREW) {
    HebrewDateFormatter.format(date, locale)
} else {
    date.format(DateTimeFormatter.ofLocalizedDate(style).withLocale(locale))
}

@Composable
private fun CalendarRoute(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as VeShinantamApplication
    val calendarViewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory(application.scheduleRepository))
    val tasks by calendarViewModel.tasks.collectAsStateWithLifecycle()
    val primaryCalendar = LocalPrimaryCalendar.current
    var period by remember(primaryCalendar) { mutableStateOf(calendarPeriodContaining(LocalDate.now(), primaryCalendar)) }
    var scheduleFilter by remember { mutableStateOf<String?>(null) }
    var typeFilter by remember { mutableStateOf<TaskType?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val resources = LocalContext.current.resources
    LaunchedEffect(period) { calendarViewModel.showRange(period.start, period.endInclusive) }
    val schedules = tasks.distinctBy { it.scheduleId }
    val filtered = tasks.filter { row ->
        (scheduleFilter == null || row.scheduleId == scheduleFilter) &&
            (typeFilter == null || row.type == typeFilter)
    }
    val grid = CalendarMonthCalculator.build(
        period.start,
        period.endInclusive,
        filtered.map { CalendarTaskStatus(it.plannedDate, it.completedAt != null) },
    )
    val selectedTasks = filtered.filter { it.plannedDate == selectedDate }
    val weekDays = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
    )

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    period = previousCalendarPeriod(period, primaryCalendar)
                    selectedDate = period.start
                    scheduleFilter = null
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
                }
                Text(
                    calendarPeriodTitle(period, primaryCalendar, locale),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                IconButton(onClick = {
                    period = nextCalendarPeriod(period, primaryCalendar)
                    selectedDate = period.start
                    scheduleFilter = null
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
                }
            }
        }
        item {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    weekDays.forEach { day ->
                        Text(
                            weekdayLabel(day, locale),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        )
                    }
                }
                grid.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            if (day == null) {
                                Spacer(Modifier.weight(1f).height(80.dp))
                            } else {
                                val selected = day.date == selectedDate
                                val containerColor = when (day.status) {
                                    CalendarDayStatus.NONE -> MaterialTheme.colorScheme.surfaceVariant
                                    CalendarDayStatus.INCOMPLETE -> MaterialTheme.colorScheme.errorContainer
                                    CalendarDayStatus.PARTIAL -> MaterialTheme.colorScheme.secondaryContainer
                                    CalendarDayStatus.COMPLETE -> MaterialTheme.colorScheme.tertiaryContainer
                                }
                                Card(
                                    onClick = { selectedDate = day.date },
                                    colors = CardDefaults.cardColors(containerColor = containerColor),
                                    modifier = Modifier.weight(1f).height(80.dp).padding(2.dp).semantics {
                                        contentDescription = resources.getString(
                                            R.string.calendar_day_status,
                                            calendarDayLabel(day.date, primaryCalendar, locale),
                                            day.completedCount,
                                            day.taskCount,
                                        )
                                    },
                                ) {
                                    Column(
                                        Modifier.fillMaxSize().padding(5.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            calendarDayLabel(day.date, primaryCalendar, locale),
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                                        )
                                        if (day.taskCount > 0) {
                                            Text(
                                                "${day.completedCount}/${day.taskCount}",
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.filter_schedule), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = scheduleFilter == null,
                    onClick = { scheduleFilter = null },
                    label = { Text(stringResource(R.string.all_schedules)) },
                )
                schedules.forEach { schedule ->
                    FilterChip(
                        selected = scheduleFilter == schedule.scheduleId,
                        onClick = { scheduleFilter = schedule.scheduleId },
                        label = { Text(localizedName(schedule.scheduleNameEnglish, schedule.scheduleNameHebrew, locale)) },
                    )
                }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = typeFilter == null,
                    onClick = { typeFilter = null },
                    label = { Text(stringResource(R.string.all_types)) },
                )
                FilterChip(
                    selected = typeFilter == TaskType.LEARNING,
                    onClick = { typeFilter = TaskType.LEARNING },
                    label = { Text(stringResource(R.string.new_learning)) },
                )
                FilterChip(
                    selected = typeFilter == TaskType.CHAZARAH,
                    onClick = { typeFilter = TaskType.CHAZARAH },
                    label = { Text(stringResource(R.string.chazarah)) },
                )
            }
        }
        item(key = "date-$selectedDate") {
            Column(Modifier.padding(top = 8.dp)) {
                Text(
                    displayDate(selectedDate, primaryCalendar, locale, FormatStyle.FULL),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    displayDate(selectedDate, alternateCalendar(primaryCalendar), locale, FormatStyle.FULL),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        if (selectedTasks.isEmpty()) item { MessageCard(R.string.no_tasks_selected_day) }
        items(selectedTasks, key = { it.taskId }) { task ->
            CalendarTaskCard(task, locale) { checked -> calendarViewModel.setCompleted(task.taskId, checked) }
        }
    }
}

@Composable
private fun CalendarTaskCard(task: TodayTaskRow, locale: Locale, onCheckedChange: (Boolean) -> Unit) {
    val reference = referenceLabel(task.labelEnglish, task.labelHebrew, locale)
    val schedule = localizedName(task.scheduleNameEnglish, task.scheduleNameHebrew, locale)
    val type = stringResource(if (task.type == TaskType.LEARNING) R.string.new_learning else R.string.chazarah)
    val checkboxDescription = stringResource(R.string.calendar_task_description, type, reference, schedule)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.completedAt != null,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.semantics {
                    contentDescription = checkboxDescription
                },
            )
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(reference, style = MaterialTheme.typography.bodyLarge)
                Text("$schedule • $type", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SchedulesRoute(modifier: Modifier = Modifier, openWizardRequest: Int = 0) {
    val application = LocalContext.current.applicationContext as VeShinantamApplication
    val schedulesViewModel: SchedulesViewModel = viewModel(factory = SchedulesViewModel.Factory(application.scheduleRepository))
    val schedules by schedulesViewModel.schedules.collectAsStateWithLifecycle()
    val exclusions by schedulesViewModel.exclusions.collectAsStateWithLifecycle()
    var creatorMode by remember { mutableStateOf<ScheduleCreatorMode?>(null) }
    LaunchedEffect(openWizardRequest) {
        if (openWizardRequest > 0) creatorMode = ScheduleCreatorMode.WIZARD
    }
    when (creatorMode) {
        ScheduleCreatorMode.PRESET -> PresetScheduleCreator(
            previewPlan = schedulesViewModel::previewPreset,
            savePlan = { schedulesViewModel.save(it); creatorMode = null },
            onCancel = { creatorMode = null },
            modifier = modifier,
        )
        ScheduleCreatorMode.CUSTOM -> CustomScheduleCreator(
            previewPlan = schedulesViewModel::preview,
            savePlan = { schedulesViewModel.save(it); creatorMode = null },
            onCancel = { creatorMode = null },
            modifier = modifier,
        )
        ScheduleCreatorMode.WIZARD -> ScheduleSetupWizard(
            previewPreset = schedulesViewModel::previewPreset,
            previewCustom = schedulesViewModel::preview,
            savePlan = { schedulesViewModel.save(it); creatorMode = null },
            onCancel = { creatorMode = null },
            modifier = modifier,
        )
        null ->
        ScheduleList(
            schedules = schedules,
            exclusions = exclusions,
            onCreateWizard = { creatorMode = ScheduleCreatorMode.WIZARD },
            onCreatePreset = { creatorMode = ScheduleCreatorMode.PRESET },
            onCreateCustom = { creatorMode = ScheduleCreatorMode.CUSTOM },
            onStateChange = schedulesViewModel::setState,
            onEditFuture = schedulesViewModel::editFuture,
            onCompletePastLearning = schedulesViewModel::completePastLearning,
            onCompletePastChazarah = schedulesViewModel::completePastChazarah,
            onAddExclusion = schedulesViewModel::addExclusion,
            onRemoveExclusion = schedulesViewModel::removeExclusion,
            onDelete = schedulesViewModel::delete,
            modifier = modifier,
        )
    }
}

@Composable
private fun ScheduleList(
    schedules: List<ScheduleEntity>,
    exclusions: List<ScheduleExclusionEntity>,
    onCreateWizard: () -> Unit,
    onCreatePreset: () -> Unit,
    onCreateCustom: () -> Unit,
    onStateChange: (String, ScheduleState) -> Unit,
    onEditFuture: (String, FutureScheduleEditDraft) -> Unit,
    onCompletePastLearning: (String) -> Unit,
    onCompletePastChazarah: (String) -> Unit,
    onAddExclusion: (String, LocalDate) -> Unit,
    onRemoveExclusion: (String, LocalDate) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val primaryCalendar = LocalPrimaryCalendar.current
    var pendingDelete by remember { mutableStateOf<ScheduleEntity?>(null) }
    var pendingCompletePast by remember { mutableStateOf<Pair<ScheduleEntity, PastCompletionKind>?>(null) }
    var selectedDetails by remember { mutableStateOf<ScheduleEntity?>(null) }
    var selectedEditing by remember { mutableStateOf<ScheduleEntity?>(null) }
    selectedEditing?.let { schedule ->
        EditFutureScheduleDialog(
            schedule = schedule,
            onSave = { draft ->
                onEditFuture(schedule.id, draft)
                selectedEditing = null
            },
            onDismiss = { selectedEditing = null },
        )
    }
    selectedDetails?.let { schedule ->
        ScheduleDetailsDialog(
            schedule = schedule,
            exclusions = exclusions.filter { it.scheduleId == schedule.id },
            onAddExclusion = { onAddExclusion(schedule.id, it) },
            onRemoveExclusion = { onRemoveExclusion(schedule.id, it) },
            onDismiss = { selectedDetails = null },
        )
    }
    pendingDelete?.let { schedule ->
        val name = localizedName(schedule.nameEnglish, schedule.nameHebrew, locale)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_schedule_title)) },
            text = { Text(stringResource(R.string.delete_schedule_message, name)) },
            confirmButton = {
                TextButton(onClick = { onDelete(schedule.id); pendingDelete = null }) {
                    Text(stringResource(R.string.delete_permanently))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    pendingCompletePast?.let { (schedule, kind) ->
        val name = localizedName(schedule.nameEnglish, schedule.nameHebrew, locale)
        AlertDialog(
            onDismissRequest = { pendingCompletePast = null },
            title = {
                Text(stringResource(if (kind == PastCompletionKind.LEARNING) R.string.complete_past_learning_title else R.string.complete_past_chazarah_title))
            },
            text = {
                Text(
                    stringResource(
                        if (kind == PastCompletionKind.LEARNING) R.string.complete_past_learning_message else R.string.complete_past_chazarah_message,
                        name,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (kind == PastCompletionKind.LEARNING) onCompletePastLearning(schedule.id) else onCompletePastChazarah(schedule.id)
                    pendingCompletePast = null
                }) {
                    Text(stringResource(R.string.mark_complete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCompletePast = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(onClick = onCreateWizard, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.setup_wizard))
            }
            Text(
                stringResource(R.string.setup_wizard_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        item {
            Text(stringResource(R.string.or_create_directly), style = MaterialTheme.typography.labelLarge)
        }
        item {
            Button(onClick = onCreatePreset, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_preset_program))
            }
        }
        item {
            OutlinedButton(onClick = onCreateCustom, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.create_custom_schedule))
            }
        }
        if (schedules.isEmpty()) item { MessageCard(R.string.no_schedules_yet) }
        items(schedules, key = { it.id }) { schedule ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        localizedName(schedule.nameEnglish, schedule.nameHebrew, locale),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (schedule.dailyQuantity > 0) {
                            stringResource(
                                R.string.schedule_pace_summary,
                                schedule.dailyQuantity,
                                schedule.targetDate?.let { displayDate(it, primaryCalendar, locale) }.orEmpty(),
                            )
                        } else {
                            stringResource(
                                R.string.schedule_finish_summary,
                                schedule.targetDate?.let { displayDate(it, primaryCalendar, locale) }.orEmpty(),
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(when (schedule.state) {
                            ScheduleState.ACTIVE -> R.string.schedule_active
                            ScheduleState.PAUSED -> R.string.schedule_paused
                            ScheduleState.ARCHIVED -> R.string.schedule_archived
                        }),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick = { selectedDetails = schedule }) {
                            Text(stringResource(R.string.view_schedule_details))
                        }
                        when (schedule.state) {
                            ScheduleState.ACTIVE -> TextButton(onClick = { onStateChange(schedule.id, ScheduleState.PAUSED) }) {
                                Text(stringResource(R.string.pause_schedule))
                            }
                            ScheduleState.PAUSED -> TextButton(onClick = { onStateChange(schedule.id, ScheduleState.ACTIVE) }) {
                                Text(stringResource(R.string.resume_schedule))
                            }
                            ScheduleState.ARCHIVED -> TextButton(onClick = { onStateChange(schedule.id, ScheduleState.PAUSED) }) {
                                Text(stringResource(R.string.restore_schedule))
                            }
                        }
                        if (schedule.state != ScheduleState.ARCHIVED) {
                            TextButton(onClick = { selectedEditing = schedule }) {
                                Text(stringResource(R.string.edit_schedule))
                            }
                            TextButton(onClick = { pendingCompletePast = schedule to PastCompletionKind.LEARNING }) {
                                Text(stringResource(R.string.complete_past_learning))
                            }
                            TextButton(onClick = { pendingCompletePast = schedule to PastCompletionKind.CHAZARAH }) {
                                Text(stringResource(R.string.complete_past_chazarah))
                            }
                            TextButton(onClick = { onStateChange(schedule.id, ScheduleState.ARCHIVED) }) {
                                Text(stringResource(R.string.archive_schedule))
                            }
                        }
                        TextButton(onClick = { pendingDelete = schedule }) {
                            Text(stringResource(R.string.delete_schedule))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditFutureScheduleDialog(
    schedule: ScheduleEntity,
    onSave: (FutureScheduleEditDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val primaryCalendar = LocalPrimaryCalendar.current
    val today = remember { LocalDate.now() }
    var startDate by remember(schedule.id) { mutableStateOf(schedule.startDate.coerceAtLeast(today)) }
    var planningMode by remember(schedule.id) {
        mutableStateOf(if (schedule.dailyQuantity > 0) PlanningMode.DAILY_PACE else PlanningMode.FINISH_BY)
    }
    var paceText by remember(schedule.id) {
        mutableStateOf(schedule.dailyQuantity.takeIf { it > 0 }?.toString() ?: "1")
    }
    var targetDate by remember(schedule.id) {
        mutableStateOf(schedule.targetDate?.coerceAtLeast(startDate) ?: startDate)
    }
    val initialWeekdays = remember(schedule.id) {
        schedule.selectedWeekdays.split(',')
            .mapNotNull { value -> runCatching { DayOfWeek.valueOf(value.trim()) }.getOrNull() }
            .ifEmpty { DayOfWeek.entries }
    }
    val weekdays = remember(schedule.id) { mutableStateListOf(*initialWeekdays.toTypedArray()) }
    var weekendChazarah by remember(schedule.id) { mutableStateOf(schedule.officialOraysaChazarah) }
    var choosingDate by remember(schedule.id) { mutableStateOf(false) }
    var choosingTargetDate by remember(schedule.id) { mutableStateOf(false) }
    val pace = paceText.toIntOrNull()
    val isValid = weekdays.isNotEmpty() && !startDate.isBefore(today) && when (planningMode) {
        PlanningMode.DAILY_PACE -> pace != null && pace > 0
        PlanningMode.FINISH_BY -> !targetDate.isBefore(startDate)
    }

    if (choosingDate) {
        LocalDatePickerDialog(
            initialDate = startDate,
            onDateSelected = { selected ->
                if (!selected.isBefore(today)) startDate = selected
                choosingDate = false
            },
            onDismiss = { choosingDate = false },
        )
    }
    if (choosingTargetDate) {
        LocalDatePickerDialog(
            initialDate = targetDate,
            onDateSelected = { selected ->
                if (!selected.isBefore(startDate)) targetDate = selected
                choosingTargetDate = false
            },
            onDismiss = { choosingTargetDate = false },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_schedule_title)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(R.string.edit_future_explanation),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { choosingDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.future_start_date, displayDate(startDate, primaryCalendar, locale)))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = planningMode == PlanningMode.DAILY_PACE,
                        onClick = { planningMode = PlanningMode.DAILY_PACE },
                        label = { Text(stringResource(R.string.daily_pace)) },
                    )
                    FilterChip(
                        selected = planningMode == PlanningMode.FINISH_BY,
                        onClick = { planningMode = PlanningMode.FINISH_BY },
                        label = { Text(stringResource(R.string.finish_by)) },
                    )
                }
                if (planningMode == PlanningMode.DAILY_PACE) {
                    OutlinedTextField(
                        value = paceText,
                        onValueChange = { value -> if (value.all(Char::isDigit)) paceText = value },
                        label = { Text(stringResource(R.string.units_per_day)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    TextButton(onClick = { choosingTargetDate = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.finish_date_value, displayDate(targetDate, primaryCalendar, locale)))
                    }
                }
                Text(stringResource(R.string.learning_days), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in weekdays,
                            onClick = {
                                if (day in weekdays && weekdays.size > 1) weekdays.remove(day)
                                else if (day !in weekdays) weekdays.add(day)
                            },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                        )
                    }
                }
                LabeledCheckbox(
                    checked = weekendChazarah,
                    onCheckedChange = { weekendChazarah = it },
                    label = stringResource(
                        if (schedule.presetId == "oraysa") R.string.official_oraysa_chazarah
                        else R.string.weekend_chazarah,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onSave(
                        FutureScheduleEditDraft(
                            startDate = startDate,
                            dailyQuantity = pace ?: 1,
                            targetCompletionDate = targetDate.takeIf { planningMode == PlanningMode.FINISH_BY },
                            selectedWeekdays = weekdays.toSet(),
                            includeWeekendChazarah = weekendChazarah,
                        ),
                    )
                },
            ) { Text(stringResource(R.string.save_changes)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ScheduleDetailsDialog(
    schedule: ScheduleEntity,
    exclusions: List<ScheduleExclusionEntity>,
    onAddExclusion: (LocalDate) -> Unit,
    onRemoveExclusion: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val primaryCalendar = LocalPrimaryCalendar.current
    val name = localizedName(schedule.nameEnglish, schedule.nameHebrew, locale)
    val weekdays = schedule.selectedWeekdays
        .split(',')
        .mapNotNull { value -> runCatching { DayOfWeek.valueOf(value.trim()) }.getOrNull() }
        .sortedBy { it.value }
        .joinToString(", ") { it.getDisplayName(TextStyle.FULL, locale) }
    val learningDays = if (weekdays.isBlank()) stringResource(R.string.every_day) else weekdays
    val configuredChazarah = when {
        schedule.chazarahDayOffsets.isBlank() && !schedule.repeatsAnnually ->
            stringResource(R.string.schedule_detail_no_chazarah)
        schedule.chazarahDayOffsets.isBlank() ->
            stringResource(R.string.schedule_detail_annual_only)
        schedule.repeatsAnnually ->
            stringResource(R.string.schedule_detail_chazarah_annual, schedule.chazarahDayOffsets)
        else -> stringResource(R.string.schedule_detail_chazarah_values, schedule.chazarahDayOffsets)
    }
    val chazarah = when {
        !schedule.officialOraysaChazarah -> configuredChazarah
        schedule.presetId == "oraysa" && schedule.chazarahDayOffsets.isBlank() && !schedule.repeatsAnnually ->
            stringResource(R.string.official_oraysa_chazarah)
        schedule.presetId == "oraysa" ->
            stringResource(R.string.official_oraysa_with_additional, configuredChazarah)
        schedule.chazarahDayOffsets.isBlank() && !schedule.repeatsAnnually ->
            stringResource(R.string.weekend_chazarah)
        else -> stringResource(R.string.weekend_with_additional, configuredChazarah)
    }
    val status = stringResource(
        when (schedule.state) {
            ScheduleState.ACTIVE -> R.string.schedule_active
            ScheduleState.PAUSED -> R.string.schedule_paused
            ScheduleState.ARCHIVED -> R.string.schedule_archived
        },
    )
    val missedWork = stringResource(
        if (schedule.missedWorkBehavior == MissedWorkBehavior.SHIFT_FORWARD) {
            R.string.shift_forward
        } else {
            R.string.keep_overdue
        },
    )
    var choosingExclusion by remember(schedule.id) { mutableStateOf(false) }
    if (choosingExclusion) {
        LocalDatePickerDialog(
            initialDate = LocalDate.now(),
            onDateSelected = { date ->
                if (!date.isBefore(LocalDate.now())) onAddExclusion(date)
                choosingExclusion = false
            },
            onDismiss = { choosingExclusion = false },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name)
                Text(
                    stringResource(R.string.schedule_details),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScheduleDetailRow(stringResource(R.string.schedule_detail_status), status)
                ScheduleDetailRow(
                    stringResource(R.string.schedule_detail_type),
                    stringResource(
                        if (schedule.kind == ScheduleKind.PRESET) {
                            R.string.schedule_type_preset
                        } else {
                            R.string.schedule_type_custom
                        },
                    ),
                )
                ScheduleDetailRow(
                    stringResource(R.string.schedule_detail_unit),
                    stringResource(schedule.materialType.detailUnitResource),
                )
                ScheduleDetailRow(
                    stringResource(R.string.schedule_detail_start),
                    displayDate(schedule.startDate, primaryCalendar, locale),
                )
                schedule.targetDate?.let { targetDate ->
                    ScheduleDetailRow(
                        stringResource(R.string.schedule_detail_completion),
                        displayDate(targetDate, primaryCalendar, locale),
                    )
                }
                ScheduleDetailRow(
                    stringResource(R.string.schedule_detail_pace),
                    if (schedule.dailyQuantity > 0) {
                        stringResource(R.string.schedule_detail_daily_pace, schedule.dailyQuantity)
                    } else {
                        stringResource(R.string.schedule_detail_finish_by)
                    },
                )
                ScheduleDetailRow(
                    stringResource(R.string.schedule_detail_days),
                    learningDays,
                )
                ScheduleDetailRow(stringResource(R.string.schedule_detail_chazarah), chazarah)
                ScheduleDetailRow(stringResource(R.string.schedule_detail_missed), missedWork)
                Text(stringResource(R.string.excluded_dates), style = MaterialTheme.typography.labelMedium)
                if (exclusions.isEmpty()) {
                    Text(
                        stringResource(R.string.no_excluded_dates),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    exclusions.sortedBy { it.date }.forEach { exclusion ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(displayDate(exclusion.date, primaryCalendar, locale))
                            TextButton(onClick = { onRemoveExclusion(exclusion.date) }) {
                                Text(stringResource(R.string.remove))
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { choosingExclusion = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.add_excluded_date))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun ScheduleDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleSetupWizard(
    previewPreset: (PresetScheduleDraft) -> PendingSchedulePlan,
    previewCustom: (CustomScheduleDraft) -> PendingSchedulePlan,
    savePlan: (PendingSchedulePlan) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val programs = PresetCatalog.programs
    var kind by remember { mutableStateOf<WizardScheduleKind?>(null) }
    var programIndex by remember { mutableStateOf(0) }
    var seferChoice by remember { mutableStateOf(SeferChoice.GEMARA) }
    var showingForm by remember { mutableStateOf(false) }

    if (showingForm) {
        when (kind) {
            WizardScheduleKind.PRESET -> PresetScheduleCreator(
                previewPlan = previewPreset,
                savePlan = savePlan,
                onCancel = { showingForm = false },
                initialProgramIndex = programIndex,
                guided = true,
                modifier = modifier,
            )
            WizardScheduleKind.CUSTOM -> CustomScheduleCreator(
                previewPlan = previewCustom,
                savePlan = savePlan,
                onCancel = { showingForm = false },
                initialSeferChoice = seferChoice,
                guided = true,
                modifier = modifier,
            )
            null -> Unit
        }
        return
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                stringResource(R.string.setup_wizard),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.wizard_step, if (kind == null) 1 else 2, 7),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (kind == null) {
            item {
                Text(stringResource(R.string.wizard_choose_schedule_type), style = MaterialTheme.typography.titleMedium)
            }
            item {
                WizardChoiceCard(
                    title = stringResource(R.string.add_preset_program),
                    description = stringResource(R.string.wizard_preset_description),
                    onClick = { kind = WizardScheduleKind.PRESET },
                )
            }
            item {
                WizardChoiceCard(
                    title = stringResource(R.string.create_custom_schedule),
                    description = stringResource(R.string.wizard_custom_description),
                    onClick = { kind = WizardScheduleKind.CUSTOM },
                )
            }
            item {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            }
        } else {
            item {
                Text(
                    stringResource(
                        if (kind == WizardScheduleKind.PRESET) R.string.wizard_choose_program else R.string.choose_sefer,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (kind == WizardScheduleKind.PRESET) {
                item {
                    SelectionDropdown(
                        label = stringResource(R.string.choose_program),
                        options = programs.map { localizedName(it.nameEnglish, it.nameHebrew, locale) },
                        selectedIndex = programIndex,
                        onSelected = { programIndex = it },
                    )
                }
                item {
                    Text(
                        stringResource(R.string.wizard_program_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SeferChoice.entries.forEach { choice ->
                            FilterChip(
                                selected = seferChoice == choice,
                                onClick = { seferChoice = choice },
                                label = { Text(stringResource(choice.labelResource)) },
                            )
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.wizard_sefer_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { kind = null }) { Text(stringResource(R.string.back)) }
                    Button(onClick = { showingForm = true }) { Text(stringResource(R.string.continue_label)) }
                }
            }
        }
    }
}

@Composable
private fun WizardChoiceCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WizardStepHeader(step: Int, title: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.wizard_step, step, 7), style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun WizardNavigation(
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextEnabled: Boolean = true,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Button(onClick = onNext, enabled = nextEnabled) { Text(stringResource(R.string.continue_label)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetScheduleCreator(
    previewPlan: (PresetScheduleDraft) -> PendingSchedulePlan,
    savePlan: (PendingSchedulePlan) -> Unit,
    onCancel: () -> Unit,
    initialProgramIndex: Int = 0,
    guided: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val primaryCalendar = LocalPrimaryCalendar.current
    val defaultChazarahOffsets = LocalDefaultChazarahOffsets.current
    val programs = PresetCatalog.programs
    var programIndex by remember(initialProgramIndex) { mutableStateOf(initialProgramIndex.coerceIn(programs.indices)) }
    val catalogPositionDate = remember { LocalDate.now() }
    val baseProgram = programs[programIndex]
    val program = remember(baseProgram, catalogPositionDate) { PresetCatalog.programAtDate(baseProgram, catalogPositionDate) }
    var startIndex by remember(programIndex) { mutableStateOf(program.currentIndex) }
    var startDate by remember { mutableStateOf(catalogPositionDate) }
    val currentMasechtaStartIndex = remember(program.id, program.currentIndex) { PresetCatalog.currentMasechtaStartIndex(program) }
    val currentMasechtaStartDate = currentMasechtaStartIndex?.let {
        PresetCatalog.scheduledDate(program, it, catalogPositionDate)
    }
    val startsWithCurrentMasechta = startIndex == currentMasechtaStartIndex && startDate == currentMasechtaStartDate
    var choosingPosition by remember { mutableStateOf(false) }
    var choosingDate by remember { mutableStateOf(false) }
    var weekendChazarah by remember(programIndex) { mutableStateOf(program.id == "oraysa") }
    var chazarahEnabled by remember(programIndex) {
        mutableStateOf(SharedPresetCatalog.defaultAdditionalChazarahOffsets(program.id).isNotEmpty())
    }
    var offsetsText by remember(defaultChazarahOffsets) {
        mutableStateOf(ChazarahDefaults.format(defaultChazarahOffsets))
    }
    var annualReviews by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf(false) }
    var preview by remember(programIndex, startIndex, startDate, weekendChazarah, chazarahEnabled, offsetsText, annualReviews) {
        mutableStateOf<PendingSchedulePlan?>(null)
    }
    var guidedStep by remember { mutableStateOf(3) }

    fun generatePreview(): Boolean {
        val offsets = ChazarahDefaults.parse(offsetsText)
        val validOffsets = !chazarahEnabled || offsets != null
        preview = if (validOffsets) runCatching {
            previewPlan(
                PresetScheduleDraft(
                    program = program,
                    startIndex = startIndex,
                    startDate = startDate,
                    chazarahPattern = if (chazarahEnabled) {
                        ChazarahPattern(requireNotNull(offsets), annualReviews)
                    } else {
                        ChazarahPattern(emptyList(), repeatsAnnually = false)
                    },
                    includeWeekendChazarah = weekendChazarah,
                ),
            )
        }.getOrNull() else null
        validationError = preview == null
        return preview != null
    }

    if (choosingPosition) {
        PresetPositionDialog(
            program = program,
            anchorDate = catalogPositionDate,
            selectedIndex = startIndex,
            locale = locale,
            onSelected = { startIndex = it; choosingPosition = false },
            onDismiss = { choosingPosition = false },
        )
    }
    if (choosingDate) {
        LocalDatePickerDialog(
            initialDate = startDate,
            onDateSelected = { startDate = it; choosingDate = false },
            onDismiss = { choosingDate = false },
        )
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (guided) item {
            WizardStepHeader(
                step = guidedStep,
                title = stringResource(
                    when (guidedStep) {
                        3 -> R.string.wizard_starting_position_step
                        4 -> R.string.wizard_timing_step
                        5 -> R.string.wizard_chazarah_step
                        6 -> R.string.wizard_chazarah_details_step
                        else -> R.string.wizard_review_step
                    },
                ),
            )
        }
        if (!guided) item {
            Text(
                stringResource(R.string.preset_programs),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (!guided) item { Text(stringResource(R.string.preset_programs_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (!guided) item {
            SelectionDropdown(
                label = stringResource(R.string.choose_program),
                options = programs.map { localizedName(it.nameEnglish, it.nameHebrew, locale) },
                selectedIndex = programIndex,
                onSelected = { programIndex = it; startDate = LocalDate.now() },
            )
        }
        if (!guided || guidedStep == 3) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(
                            R.string.catalog_position_as_of,
                            displayDate(catalogPositionDate, primaryCalendar, locale),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        referenceLabel(program.currentReference.english, program.currentReference.hebrew, locale),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        if (!guided || guidedStep == 3) item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = startIndex == program.currentIndex,
                    onClick = { startIndex = program.currentIndex; startDate = LocalDate.now() },
                    label = { Text(stringResource(R.string.join_current_position)) },
                )
                if (currentMasechtaStartIndex != null && currentMasechtaStartIndex < program.currentIndex) {
                    FilterChip(
                        selected = startsWithCurrentMasechta,
                        onClick = {
                            startIndex = currentMasechtaStartIndex
                            startDate = requireNotNull(currentMasechtaStartDate)
                        },
                        label = { Text(stringResource(R.string.start_current_masechta)) },
                    )
                }
                FilterChip(
                    selected = startIndex != program.currentIndex && !startsWithCurrentMasechta,
                    onClick = { choosingPosition = true },
                    label = { Text(stringResource(R.string.choose_earlier_position)) },
                )
            }
        }
        if (!guided || guidedStep == 3) item {
            OutlinedButton(onClick = { choosingPosition = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${stringResource(R.string.starting_position)}: " +
                        referenceLabel(program.units[startIndex].english, program.units[startIndex].hebrew, locale),
                )
            }
            if (startIndex != program.currentIndex) {
                Text(
                    stringResource(
                        R.string.originally_scheduled,
                        displayDate(PresetCatalog.scheduledDate(program, startIndex, catalogPositionDate), primaryCalendar, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        if (!guided || guidedStep == 4) item {
            OutlinedButton(onClick = { choosingDate = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.start_date_value, displayDate(startDate, primaryCalendar, locale)))
            }
        }
        if (!guided || guidedStep == 4) item {
            val cadence = if (program.selectedWeekdays.size == DayOfWeek.entries.size) {
                stringResource(R.string.every_day)
            } else {
                stringResource(R.string.sunday_through_thursday)
            }
            Text(
                stringResource(
                    R.string.preset_cadence,
                    program.dailyQuantity,
                    stringResource(program.materialType.presetUnitResource),
                    cadence,
                ),
            )
        }
        if (!guided || guidedStep == 5) item {
            Text(
                stringResource(R.string.chazarah_pattern),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (!guided || guidedStep == 5) item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(if (program.id == "oraysa") R.string.official_oraysa_chazarah else R.string.weekend_chazarah),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(if (program.id == "oraysa") R.string.official_oraysa_chazarah_help else R.string.weekend_chazarah_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = weekendChazarah,
                        onClick = { weekendChazarah = true; validationError = false },
                        label = { Text(stringResource(R.string.include_weekend_chazarah)) },
                    )
                    FilterChip(
                        selected = !weekendChazarah,
                        onClick = { weekendChazarah = false; validationError = false },
                        label = { Text(stringResource(R.string.no_weekend_chazarah)) },
                    )
                }
            }
        }
        if ((!guided || guidedStep == 5) && weekendChazarah) item {
            Text(stringResource(R.string.additional_chazarah), style = MaterialTheme.typography.titleMedium)
        }
        if (!guided || guidedStep == 5) item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = chazarahEnabled,
                    onClick = { chazarahEnabled = true; validationError = false },
                    label = { Text(stringResource(if (weekendChazarah) R.string.add_additional_chazarah else R.string.add_chazarah)) },
                )
                FilterChip(
                    selected = !chazarahEnabled,
                    onClick = { chazarahEnabled = false; validationError = false },
                    label = { Text(stringResource(if (weekendChazarah) R.string.no_additional_chazarah else R.string.no_chazarah)) },
                )
            }
        }
        if ((!guided || guidedStep == 6) && chazarahEnabled) item {
            Text(
                stringResource(if (weekendChazarah) R.string.additional_chazarah_help else R.string.preset_default_chazarah),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!guided || guidedStep == 6) item {
            OutlinedTextField(
                value = offsetsText,
                onValueChange = { offsetsText = it; validationError = false },
                enabled = chazarahEnabled,
                label = { Text(stringResource(R.string.chazarah_offsets)) },
                supportingText = { Text(stringResource(R.string.chazarah_offsets_help)) },
                textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!guided || guidedStep == 6) item {
            LabeledCheckbox(
                checked = annualReviews && chazarahEnabled,
                enabled = chazarahEnabled,
                onCheckedChange = { annualReviews = it },
                label = stringResource(R.string.repeat_annually),
            )
        }
        if ((!guided || guidedStep >= 6) && validationError) item {
            Text(stringResource(R.string.schedule_validation_error), color = MaterialTheme.colorScheme.error)
        }
        if (!guided) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                Button(onClick = {
                    generatePreview()
                }) { Text(stringResource(R.string.preview_schedule)) }
            }
        }
        if (guided && guidedStep < 7) item {
            WizardNavigation(
                onBack = { if (guidedStep == 3) onCancel() else guidedStep-- },
                onNext = {
                    if (guidedStep == 6) {
                        if (generatePreview()) guidedStep = 7
                    } else {
                        guidedStep++
                    }
                },
            )
        }
        if (guided && guidedStep == 7) item {
            TextButton(onClick = { guidedStep = 6 }) { Text(stringResource(R.string.back)) }
        }
        if (!guided || guidedStep == 7) preview?.let { plan ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.preview), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.learning_task_count, plan.learningCount))
                        Text(stringResource(R.string.chazarah_task_count, plan.reviewCount))
                        Text(stringResource(R.string.completion_date, plan.completionDate?.let { displayDate(it, primaryCalendar, locale) }.orEmpty()))
                        Button(onClick = { savePlan(plan) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.save_schedule))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetPositionDialog(
    program: PresetProgram,
    anchorDate: LocalDate,
    selectedIndex: Int,
    locale: Locale,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val primaryCalendar = LocalPrimaryCalendar.current
    var query by remember(program.id) { mutableStateOf("") }
    val matches = remember(program.id, query) {
        val normalized = query.trim().lowercase()
        program.selectableStartingUnits.withIndex().asSequence()
            .filter { normalized.isEmpty() || it.value.english.lowercase().contains(normalized) || it.value.hebrew.contains(normalized) }
            .take(100)
            .toList()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_earlier_position)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_positions)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (matches.isEmpty()) {
                    Text(stringResource(R.string.no_matching_positions))
                } else {
                    LazyColumn(Modifier.height(320.dp)) {
                        items(matches, key = { it.index }) { indexed ->
                            Row(
                                Modifier.fillMaxWidth().selectable(
                                    selected = indexed.index == selectedIndex,
                                    onClick = { onSelected(indexed.index) },
                                    role = Role.RadioButton,
                                ).padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = indexed.index == selectedIndex,
                                    onClick = null,
                                )
                                Column {
                                    Text(referenceLabel(indexed.value.english, indexed.value.hebrew, locale))
                                    Text(
                                        displayDate(PresetCatalog.scheduledDate(program, indexed.index, anchorDate), primaryCalendar, locale),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomScheduleCreator(
    previewPlan: (CustomScheduleDraft) -> PendingSchedulePlan,
    savePlan: (PendingSchedulePlan) -> Unit,
    onCancel: () -> Unit,
    initialSeferChoice: SeferChoice = SeferChoice.OTHER,
    guided: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val primaryCalendar = LocalPrimaryCalendar.current
    val defaultChazarahOffsets = LocalDefaultChazarahOffsets.current
    var scheduleName by remember { mutableStateOf("") }
    var seferChoice by remember(initialSeferChoice) { mutableStateOf(initialSeferChoice) }
    var unitLabel by remember { mutableStateOf("") }
    var paceText by remember { mutableStateOf("1") }
    var planningMode by remember { mutableStateOf(PlanningMode.DAILY_PACE) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var targetDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) }
    var missedBehavior by remember { mutableStateOf(MissedWorkBehavior.KEEP_FIXED_OVERDUE) }
    var chazarahEnabled by remember { mutableStateOf(true) }
    var weekendChazarah by remember { mutableStateOf(false) }
    var offsetsText by remember(defaultChazarahOffsets) {
        mutableStateOf(ChazarahDefaults.format(defaultChazarahOffsets))
    }
    var annualReviews by remember { mutableStateOf(true) }
    var choosingStartDate by remember { mutableStateOf(false) }
    var choosingTargetDate by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf(false) }
    val units = remember { mutableStateListOf<String>() }
    var fromMasechtaIndex by remember { mutableStateOf(0) }
    var toMasechtaIndex by remember { mutableStateOf(0) }
    var startUnitIndex by remember { mutableStateOf(0) }
    var endUnitIndex by remember { mutableStateOf(0) }
    var gemaraUnit by remember { mutableStateOf(GemaraUnit.DAF) }
    var mishnahUnit by remember { mutableStateOf(MishnahUnit.MISHNAH) }
    var mishnahBerurahUnit by remember { mutableStateOf(MishnahBerurahUnit.SIMAN) }
    var selectedChelek by remember { mutableStateOf(1) }
    val weekdays = remember { mutableStateListOf(*DayOfWeek.entries.toTypedArray()) }
    var preview by remember { mutableStateOf<PendingSchedulePlan?>(null) }
    val masechtaCatalog = when (seferChoice) {
        SeferChoice.GEMARA -> MaterialCatalog.gemara
        SeferChoice.YERUSHALMI -> MaterialCatalog.yerushalmi
        SeferChoice.MISHNAH -> MaterialCatalog.mishnah
        SeferChoice.RAMBAM -> MaterialCatalog.rambam
        else -> emptyList()
    }
    val fromMasechta = masechtaCatalog.getOrNull(fromMasechtaIndex)
    val toMasechta = masechtaCatalog.getOrNull(toMasechtaIndex)
    val startUnitOptions = materialUnitOptions(seferChoice, fromMasechta, gemaraUnit, mishnahUnit, selectedChelek, mishnahBerurahUnit)
    val rawEndUnitOptions = materialUnitOptions(seferChoice, toMasechta, gemaraUnit, mishnahUnit, selectedChelek, mishnahBerurahUnit)
    val sectionedSefarim = setOf(SeferChoice.GEMARA, SeferChoice.YERUSHALMI, SeferChoice.MISHNAH, SeferChoice.RAMBAM)
    val sameSection = seferChoice !in sectionedSefarim || fromMasechtaIndex == toMasechtaIndex
    val boundedStartIndex = startUnitIndex.coerceIn(0, (startUnitOptions.size - 1).coerceAtLeast(0))
    val endUnitOptions = rawEndUnitOptions.drop(if (sameSection) boundedStartIndex else 0)
    val boundedEndIndex = endUnitIndex.coerceIn(0, (endUnitOptions.size - 1).coerceAtLeast(0))
    val selectedStartUnit = startUnitOptions.getOrNull(boundedStartIndex)
    val selectedEndUnit = endUnitOptions.getOrNull(boundedEndIndex)
    var guidedStep by remember { mutableStateOf(3) }

    fun generatePreview(): Boolean {
        val offsets = ChazarahDefaults.parse(offsetsText)
        val validOffsets = !chazarahEnabled || offsets != null
        val validDates = planningMode != PlanningMode.FINISH_BY || !targetDate.isBefore(startDate)
        val structuredUnits = runCatching {
            selectedUnitReferences(
                seferChoice, fromMasechtaIndex, toMasechtaIndex, selectedStartUnit, selectedEndUnit,
                gemaraUnit, mishnahUnit, selectedChelek, mishnahBerurahUnit,
            )
        }.getOrNull()
        val unitInputs = if (seferChoice == SeferChoice.OTHER) {
            units.map(::ScheduleUnitInput)
        } else {
            structuredUnits?.map { ScheduleUnitInput(it.english, it.hebrew) }.orEmpty()
        }
        preview = if (validOffsets && validDates && unitInputs.isNotEmpty()) runCatching {
            previewPlan(
                CustomScheduleDraft(
                    name = scheduleName,
                    units = unitInputs,
                    startDate = startDate,
                    dailyQuantity = paceText.toIntOrNull() ?: 1,
                    targetCompletionDate = targetDate.takeIf { planningMode == PlanningMode.FINISH_BY },
                    selectedWeekdays = weekdays.toSet(),
                    chazarahPattern = if (chazarahEnabled) {
                        ChazarahPattern(requireNotNull(offsets), annualReviews)
                    } else {
                        ChazarahPattern(emptyList(), repeatsAnnually = false)
                    },
                    includeWeekendChazarah = weekendChazarah,
                    missedWorkBehavior = missedBehavior,
                    materialType = when (seferChoice) {
                        SeferChoice.GEMARA -> if (gemaraUnit == GemaraUnit.DAF) MaterialType.DAF else MaterialType.AMUD
                        SeferChoice.YERUSHALMI -> MaterialType.DAF
                        SeferChoice.MISHNAH -> if (mishnahUnit == MishnahUnit.MISHNAH) MaterialType.MISHNAH else MaterialType.PEREK
                        SeferChoice.MISHNAH_BERURAH -> when (mishnahBerurahUnit) {
                            MishnahBerurahUnit.PAGE -> MaterialType.PAGE
                            MishnahBerurahUnit.SEIF -> MaterialType.SEIF
                            MishnahBerurahUnit.SIMAN -> MaterialType.SIMAN
                        }
                        SeferChoice.KITZUR -> MaterialType.SIMAN
                        SeferChoice.RAMBAM, SeferChoice.TEHILLIM -> MaterialType.PEREK
                        SeferChoice.CHOFETZ_CHAIM, SeferChoice.OTHER -> MaterialType.CUSTOM_UNIT
                    },
                    sourceType = seferChoice.name,
                ),
            )
        }.getOrNull() else null
        validationError = preview == null
        return preview != null
    }

    LaunchedEffect(
        seferChoice,
        toMasechtaIndex,
        boundedStartIndex,
        gemaraUnit,
        mishnahUnit,
        selectedChelek,
        mishnahBerurahUnit,
        endUnitOptions.size,
    ) {
        if (endUnitOptions.isNotEmpty()) endUnitIndex = endUnitOptions.lastIndex
    }

    if (choosingStartDate) {
        LocalDatePickerDialog(
            initialDate = startDate,
            onDateSelected = { startDate = it; if (targetDate < it) targetDate = it; preview = null },
            onDismiss = { choosingStartDate = false },
        )
    }
    if (choosingTargetDate) {
        LocalDatePickerDialog(
            initialDate = targetDate,
            onDateSelected = { targetDate = it; preview = null },
            onDismiss = { choosingTargetDate = false },
        )
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (guided) item {
            WizardStepHeader(
                step = guidedStep,
                title = stringResource(
                    when (guidedStep) {
                        3 -> R.string.wizard_material_step
                        4 -> R.string.wizard_timing_step
                        5 -> R.string.wizard_learning_days_step
                        6 -> R.string.wizard_chazarah_step
                        else -> R.string.wizard_review_step
                    },
                ),
            )
        }
        if (!guided) item {
            Text(stringResource(R.string.custom_schedule), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.custom_schedule_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!guided || guidedStep == 3) item {
            OutlinedTextField(
                value = scheduleName,
                onValueChange = { scheduleName = it; preview = null },
                label = { Text(stringResource(R.string.schedule_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!guided) item { Text(stringResource(R.string.choose_sefer), style = MaterialTheme.typography.titleMedium) }
        if (!guided) item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SeferChoice.entries.forEach { choice ->
                    FilterChip(
                        selected = seferChoice == choice,
                        onClick = {
                            seferChoice = choice
                            fromMasechtaIndex = 0
                            toMasechtaIndex = 0
                            startUnitIndex = 0
                            endUnitIndex = 0
                            preview = null
                        },
                        label = { Text(stringResource(choice.labelResource)) },
                    )
                }
            }
        }
        if ((!guided || guidedStep == 3) && seferChoice in sectionedSefarim) {
            item { Text(stringResource(R.string.choose_range), style = MaterialTheme.typography.titleMedium) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectionDropdown(
                        label = stringResource(if (seferChoice == SeferChoice.RAMBAM) R.string.from_section else R.string.from_sefer),
                        options = masechtaCatalog.map { referenceLabel(it.english, it.hebrew, locale) },
                        selectedIndex = fromMasechtaIndex,
                        onSelected = { index ->
                            fromMasechtaIndex = index
                            if (toMasechtaIndex < index) toMasechtaIndex = index
                            startUnitIndex = 0
                            endUnitIndex = 0
                            preview = null
                        },
                        modifier = Modifier.weight(1f),
                    )
                    val toCatalog = masechtaCatalog.drop(fromMasechtaIndex)
                    SelectionDropdown(
                        label = stringResource(if (seferChoice == SeferChoice.RAMBAM) R.string.to_section else R.string.to_sefer),
                        options = toCatalog.map { referenceLabel(it.english, it.hebrew, locale) },
                        selectedIndex = (toMasechtaIndex - fromMasechtaIndex).coerceAtLeast(0),
                        onSelected = { relativeIndex ->
                            toMasechtaIndex = fromMasechtaIndex + relativeIndex
                            val chosen = masechtaCatalog[toMasechtaIndex]
                            endUnitIndex = materialUnitOptions(
                                seferChoice, chosen, gemaraUnit, mishnahUnit, selectedChelek, mishnahBerurahUnit,
                            ).lastIndex.coerceAtLeast(0)
                            preview = null
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if ((!guided || guidedStep == 3) && seferChoice == SeferChoice.MISHNAH_BERURAH) {
            item { Text(stringResource(R.string.choose_chelek), style = MaterialTheme.typography.titleMedium) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..6).forEach { chelek ->
                        FilterChip(
                            selected = selectedChelek == chelek,
                            onClick = {
                                selectedChelek = chelek
                                startUnitIndex = 0
                                endUnitIndex = materialUnitOptions(
                                    SeferChoice.MISHNAH_BERURAH, null, gemaraUnit, mishnahUnit, chelek, mishnahBerurahUnit,
                                ).lastIndex.coerceAtLeast(0)
                                preview = null
                            },
                            label = {
                                Text(referenceLabel("Chelek $chelek", "חלק ${HebrewNumerals.format(chelek)}", locale))
                            },
                        )
                    }
                }
            }
            item {
                Text(stringResource(R.string.choose_unit), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MishnahBerurahUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = mishnahBerurahUnit == unit,
                            onClick = {
                                mishnahBerurahUnit = unit
                                startUnitIndex = 0
                                endUnitIndex = materialUnitOptions(
                                    SeferChoice.MISHNAH_BERURAH, null, gemaraUnit, mishnahUnit, selectedChelek, unit,
                                ).lastIndex.coerceAtLeast(0)
                                preview = null
                            },
                            label = {
                                Text(stringResource(when (unit) {
                                    MishnahBerurahUnit.PAGE -> R.string.page
                                    MishnahBerurahUnit.SEIF -> R.string.seif
                                    MishnahBerurahUnit.SIMAN -> R.string.siman
                                }))
                            },
                        )
                    }
                }
            }
        }
        if ((!guided || guidedStep == 3) && seferChoice == SeferChoice.GEMARA) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GemaraUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = gemaraUnit == unit,
                            onClick = { gemaraUnit = unit; startUnitIndex = 0; endUnitIndex = 0; preview = null },
                            label = { Text(stringResource(if (unit == GemaraUnit.DAF) R.string.daf else R.string.amud)) },
                        )
                    }
                }
            }
        }
        if ((!guided || guidedStep == 3) && seferChoice == SeferChoice.MISHNAH) {
            item {
                Text(stringResource(R.string.choose_unit), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MishnahUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = mishnahUnit == unit,
                            onClick = { mishnahUnit = unit; startUnitIndex = 0; endUnitIndex = 0; preview = null },
                            label = { Text(stringResource(if (unit == MishnahUnit.MISHNAH) R.string.mishnah_unit else R.string.perek)) },
                        )
                    }
                }
            }
        }
        if ((!guided || guidedStep == 3) && seferChoice != SeferChoice.OTHER) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectionDropdown(
                        label = stringResource(R.string.from_unit),
                        options = startUnitOptions.map { referenceLabel(it.english, it.hebrew, locale) },
                        selectedIndex = boundedStartIndex,
                        onSelected = { startUnitIndex = it; endUnitIndex = 0; preview = null },
                        modifier = Modifier.weight(1f),
                    )
                    SelectionDropdown(
                        label = stringResource(R.string.to_unit),
                        options = endUnitOptions.map { referenceLabel(it.english, it.hebrew, locale) },
                        selectedIndex = boundedEndIndex,
                        onSelected = { endUnitIndex = it; preview = null },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else if (!guided || guidedStep == 3) {
            item { Text(stringResource(R.string.add_units_individually), style = MaterialTheme.typography.titleMedium) }
            item {
                OutlinedTextField(
                    value = unitLabel,
                    onValueChange = { unitLabel = it },
                    label = { Text(stringResource(R.string.unit_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(
                    onClick = { units += unitLabel.trim(); unitLabel = ""; preview = null },
                    enabled = unitLabel.isNotBlank(),
                ) { Text(stringResource(R.string.add_unit)) }
            }
            if (units.isNotEmpty()) {
                items(units) { unit ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(unit, modifier = Modifier.weight(1f))
                        TextButton(onClick = { units.remove(unit); preview = null }) { Text(stringResource(R.string.remove)) }
                    }
                }
            }
        }
        if (!guided || guidedStep == 4) item { Text(stringResource(R.string.plan_dates), style = MaterialTheme.typography.titleMedium) }
        if (!guided || guidedStep == 4) item {
            TextButton(onClick = { choosingStartDate = true }) {
                Text(stringResource(R.string.start_date_value, displayDate(startDate, primaryCalendar, locale)))
            }
        }
        if (!guided || guidedStep == 4) item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = planningMode == PlanningMode.DAILY_PACE,
                    onClick = { planningMode = PlanningMode.DAILY_PACE; preview = null },
                    label = { Text(stringResource(R.string.daily_pace)) },
                )
                FilterChip(
                    selected = planningMode == PlanningMode.FINISH_BY,
                    onClick = { planningMode = PlanningMode.FINISH_BY; preview = null },
                    label = { Text(stringResource(R.string.finish_by)) },
                )
            }
        }
        if ((!guided || guidedStep == 4) && planningMode == PlanningMode.DAILY_PACE) {
            item {
                OutlinedTextField(
                    value = paceText,
                    onValueChange = { value -> if (value.all(Char::isDigit)) paceText = value; preview = null },
                    label = { Text(stringResource(R.string.units_per_day)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else if (!guided || guidedStep == 4) {
            item {
                TextButton(onClick = { choosingTargetDate = true }) {
                    Text(stringResource(R.string.finish_date_value, displayDate(targetDate, primaryCalendar, locale)))
                }
            }
        }
        if (!guided || guidedStep == 5) item { Text(stringResource(R.string.learning_days), style = MaterialTheme.typography.titleMedium) }
        if (!guided || guidedStep == 5) item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = day in weekdays,
                        onClick = {
                            if (day in weekdays && weekdays.size > 1) weekdays.remove(day) else if (day !in weekdays) weekdays.add(day)
                            preview = null
                        },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                    )
                }
            }
        }
        if (!guided || guidedStep == 5) item { Text(stringResource(R.string.missed_learning), style = MaterialTheme.typography.titleMedium) }
        if (!guided || guidedStep == 5) item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = missedBehavior == MissedWorkBehavior.SHIFT_FORWARD,
                    onClick = { missedBehavior = MissedWorkBehavior.SHIFT_FORWARD; preview = null },
                    label = { Text(stringResource(R.string.shift_forward)) },
                )
                FilterChip(
                    selected = missedBehavior == MissedWorkBehavior.KEEP_FIXED_OVERDUE,
                    onClick = { missedBehavior = MissedWorkBehavior.KEEP_FIXED_OVERDUE; preview = null },
                    label = { Text(stringResource(R.string.keep_overdue)) },
                )
            }
        }
        if (!guided || guidedStep == 6) item { Text(stringResource(R.string.chazarah_pattern), style = MaterialTheme.typography.titleMedium) }
        if (!guided || guidedStep == 6) item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LabeledCheckbox(
                    checked = weekendChazarah,
                    onCheckedChange = { weekendChazarah = it; preview = null },
                    label = stringResource(R.string.weekend_chazarah),
                )
                Text(
                    stringResource(R.string.weekend_chazarah_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!guided || guidedStep == 6) item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = chazarahEnabled,
                    onClick = { chazarahEnabled = true; preview = null; validationError = false },
                    label = { Text(stringResource(R.string.add_chazarah)) },
                )
                FilterChip(
                    selected = !chazarahEnabled,
                    onClick = { chazarahEnabled = false; preview = null; validationError = false },
                    label = { Text(stringResource(R.string.no_chazarah)) },
                )
            }
        }
        if (!guided || guidedStep == 6) item {
            OutlinedTextField(
                value = offsetsText,
                onValueChange = { offsetsText = it; preview = null; validationError = false },
                enabled = chazarahEnabled,
                label = { Text(stringResource(R.string.chazarah_offsets)) },
                supportingText = { Text(stringResource(R.string.chazarah_offsets_help)) },
                textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!guided || guidedStep == 6) item {
            LabeledCheckbox(
                checked = annualReviews && chazarahEnabled,
                enabled = chazarahEnabled,
                onCheckedChange = { annualReviews = it; preview = null },
                label = stringResource(R.string.repeat_annually),
            )
        }
        if ((!guided || guidedStep >= 6) && validationError) item {
            Text(stringResource(R.string.schedule_validation_error), color = MaterialTheme.colorScheme.error)
        }
        if (!guided) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = { generatePreview() },
                    enabled = scheduleName.isNotBlank() && (seferChoice != SeferChoice.OTHER || units.isNotEmpty()) &&
                        (planningMode == PlanningMode.FINISH_BY || (paceText.toIntOrNull() ?: 0) > 0),
                ) { Text(stringResource(R.string.preview_schedule)) }
            }
        }
        if (guided && guidedStep < 7) item {
            val validMaterial = scheduleName.isNotBlank() && (seferChoice != SeferChoice.OTHER || units.isNotEmpty())
            val validTiming = planningMode == PlanningMode.FINISH_BY || (paceText.toIntOrNull() ?: 0) > 0
            WizardNavigation(
                onBack = { if (guidedStep == 3) onCancel() else guidedStep-- },
                onNext = {
                    if (guidedStep == 6) {
                        if (generatePreview()) guidedStep = 7
                    } else {
                        guidedStep++
                    }
                },
                nextEnabled = when (guidedStep) {
                    3 -> validMaterial
                    4 -> validTiming
                    else -> true
                },
            )
        }
        if (guided && guidedStep == 7) item {
            TextButton(onClick = { guidedStep = 6 }) { Text(stringResource(R.string.back)) }
        }
        if (!guided || guidedStep == 7) preview?.let { plan ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.preview), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.learning_task_count, plan.learningCount))
                        Text(stringResource(R.string.chazarah_task_count, plan.reviewCount))
                        Text(stringResource(R.string.completion_date, plan.completionDate?.let { displayDate(it, primaryCalendar, locale) }.orEmpty()))
                        Button(onClick = { savePlan(plan) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.save_schedule))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    if (LocalPrimaryCalendar.current == PrimaryCalendar.HEBREW) {
        HebrewDatePickerDialog(initialDate, onDateSelected, onDismiss)
        return
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(state = state) }
}

@Composable
private fun HebrewDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    var period by remember(initialDate) { mutableStateOf(calendarPeriodContaining(initialDate, PrimaryCalendar.HEBREW)) }
    val dates = remember(period) {
        generateSequence(period.start) { current -> current.plusDays(1).takeIf { !it.isAfter(period.endInclusive) } }.toList()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_hebrew_date)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        period = previousCalendarPeriod(period, PrimaryCalendar.HEBREW)
                        selectedDate = period.start
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
                    }
                    Text(calendarPeriodTitle(period, PrimaryCalendar.HEBREW, locale), fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        period = nextCalendarPeriod(period, PrimaryCalendar.HEBREW)
                        selectedDate = period.start
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
                    }
                }
                SelectionDropdown(
                    label = stringResource(R.string.day_of_month),
                    options = dates.map { date ->
                        "${HebrewDateFormatter.dayLabel(date, locale)} · ${date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)}"
                    },
                    selectedIndex = dates.indexOf(selectedDate).coerceAtLeast(0),
                    onSelected = { selectedDate = dates[it] },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    displayDate(selectedDate, PrimaryCalendar.GREGORIAN, locale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDateSelected(selectedDate); onDismiss() }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun MessageCard(@StringRes message: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(message), modifier = Modifier.padding(24.dp))
    }
}

@Composable
private fun TodayRoute(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as VeShinantamApplication
    val todayViewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory(application.scheduleRepository))
    val state by todayViewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { todayViewModel.refreshDate() }

    when (val value = state) {
        TodayUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is TodayUiState.Error -> MessageScreen(R.string.today_load_error, modifier)
        is TodayUiState.Ready -> TodayScreen(value, todayViewModel::setCompleted, modifier)
    }
}

@Composable
private fun TodayScreen(
    state: TodayUiState.Ready,
    onCheckedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val primaryCalendar = LocalPrimaryCalendar.current
    val sefarimLanguage = LocalSefarimLanguage.current
    val preferHebrewReference = sefarimLanguage == SefarimLanguage.HEBREW ||
        (sefarimLanguage == SefarimLanguage.BOTH && AppLanguage.fromTag(locale.language) == AppLanguage.HEBREW)
    val context = LocalContext.current
    val displaySettings = remember(context) { TodayDisplaySettings(context.applicationContext) }
    var sortOrder by remember { mutableStateOf(displaySettings.readSortOrder()) }
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }
    val expandedSchedules = remember { mutableStateMapOf<String, Boolean>() }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = displayDate(state.today, primaryCalendar, locale, FormatStyle.FULL),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            SelectionDropdown(
                label = stringResource(R.string.sort_items),
                options = listOf(
                    stringResource(R.string.sort_scheduled_first),
                    stringResource(R.string.sort_newest_due_first),
                    stringResource(R.string.sort_reference_ascending),
                    stringResource(R.string.sort_reference_descending),
                ),
                selectedIndex = sortOrder.ordinal,
                onSelected = { index ->
                    sortOrder = TodaySortOrder.entries[index]
                    displaySettings.saveSortOrder(sortOrder)
                    (context.applicationContext as VeShinantamApplication).supabaseSyncService.scheduleAutomaticSync()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.schedules.isEmpty()) item { EmptyTodayCard() }
        state.schedules.forEach { schedule ->
            val scheduleExpanded = expandedSchedules[schedule.id] ?: true
            item(key = "header-${schedule.id}") {
                ScheduleHeader(schedule, locale, scheduleExpanded) {
                    expandedSchedules[schedule.id] = !scheduleExpanded
                }
            }
            if (scheduleExpanded) {
                TodaySection.entries.forEach { section ->
                    val tasks = sortTodayTasks(
                        schedule.tasks.filter { it.section == section },
                        sortOrder,
                        preferHebrewReference,
                    )
                    if (tasks.isNotEmpty()) {
                        val sectionKey = "${schedule.id}-$section"
                        val expanded = expandedSections[sectionKey] ?: true
                        item(key = "section-$sectionKey") {
                            SectionHeader(section, tasks.size, expanded) {
                                expandedSections[sectionKey] = !expanded
                            }
                        }
                        if (expanded) {
                            items(tasks, key = { it.id }) { task ->
                                TaskRow(task, locale) { checked -> onCheckedChange(task.id, checked) }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ScheduleHeader(
    schedule: TodayScheduleUi,
    locale: Locale,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).semantics(mergeDescendants = true) {},
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Text(
                localizedName(schedule.nameEnglish, schedule.nameHebrew, locale),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${schedule.completedCount}/${schedule.tasks.size}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.collapse_schedule else R.string.expand_schedule),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SectionHeader(section: TodaySection, taskCount: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .heightIn(min = 48.dp)
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${stringResource(section.stringResource)} ($taskCount)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = stringResource(if (expanded) R.string.collapse_section else R.string.expand_section),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TaskRow(task: TodayTaskUi, locale: Locale, onCheckedChange: (Boolean) -> Unit) {
    val reference = referenceLabel(task.labelEnglish, task.labelHebrew, locale)
    val section = stringResource(task.section.stringResource)
    val dueDate = displayDate(task.plannedDate, LocalPrimaryCalendar.current, locale)
    val description = stringResource(R.string.task_checkbox_description, section, reference)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.semantics { contentDescription = description },
            )
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(reference, style = MaterialTheme.typography.bodyLarge)
                if (task.section == TodaySection.OVERDUE_LEARNING || task.section == TodaySection.OVERDUE_CHAZARAH) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.due_date, dueDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTodayCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Text(
            stringResource(R.string.all_done),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        )
    }
}

@Composable
private fun LabeledCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(
            value = checked,
            enabled = enabled,
            onValueChange = onCheckedChange,
            role = Role.Checkbox,
        ).heightIn(min = 48.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(label)
    }
}

@Composable
private fun MessageScreen(@StringRes message: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(message), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlaceholderScreen(
    @StringRes title: Int,
    @StringRes description: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(description), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.coming_in_increment), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun localizedName(english: String, hebrew: String, locale: Locale): String {
    val uiLanguage = AppLanguage.fromTag(locale.language)
    val selected = if (uiLanguage == AppLanguage.HEBREW) hebrew.ifBlank { english } else english.ifBlank { hebrew }
    return BidiText.isolateForUi(selected, uiLanguage)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val safeIndex = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (options.isNotEmpty()) expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = options.getOrNull(safeIndex).orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = options.isNotEmpty(),
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelected(index); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun referenceLabel(english: String, hebrew: String, locale: Locale): String =
    SefarimDisplay.label(english, hebrew, LocalSefarimLanguage.current, AppLanguage.fromTag(locale.language))

private val MaterialType.presetUnitResource: Int
    get() = when (this) {
        MaterialType.DAF -> R.string.preset_unit_daf
        MaterialType.AMUD -> R.string.preset_unit_amud
        MaterialType.MISHNAH -> R.string.preset_unit_mishnayos
        MaterialType.PAGE -> R.string.preset_unit_page
        MaterialType.SEIF -> R.string.preset_unit_seifim
        MaterialType.PEREK -> R.string.preset_unit_chapters
        MaterialType.CUSTOM_UNIT -> R.string.preset_unit_portions
        else -> error("Unsupported preset material type: $this")
    }

private val MaterialType.detailUnitResource: Int
    get() = when (this) {
        MaterialType.DAF -> R.string.daf
        MaterialType.AMUD -> R.string.amud
        MaterialType.MISHNAH -> R.string.mishnah
        MaterialType.PEREK -> R.string.perek
        MaterialType.PAGE -> R.string.page
        MaterialType.SEIF -> R.string.seif
        MaterialType.SIMAN -> R.string.siman
        MaterialType.CUSTOM_UNIT -> R.string.custom_unit
    }

private val TodaySection.stringResource: Int
    get() = when (this) {
        TodaySection.NEW_LEARNING -> R.string.new_learning
        TodaySection.CHAZARAH_TODAY -> R.string.chazarah_due_today
        TodaySection.OVERDUE_LEARNING -> R.string.overdue_learning
        TodaySection.OVERDUE_CHAZARAH -> R.string.overdue_chazarah
        TodaySection.COMPLETED_TODAY -> R.string.completed_today
    }

private val SeferChoice.labelResource: Int
    get() = when (this) {
        SeferChoice.GEMARA -> R.string.gemara
        SeferChoice.YERUSHALMI -> R.string.yerushalmi
        SeferChoice.MISHNAH -> R.string.mishnah
        SeferChoice.MISHNAH_BERURAH -> R.string.mishnah_berurah
        SeferChoice.RAMBAM -> R.string.rambam
        SeferChoice.CHOFETZ_CHAIM -> R.string.chofetz_chaim
        SeferChoice.TEHILLIM -> R.string.tehillim
        SeferChoice.KITZUR -> R.string.kitzur
        SeferChoice.OTHER -> R.string.other
    }

private fun selectedUnitReferences(
    choice: SeferChoice,
    fromMasechtaIndex: Int,
    toMasechtaIndex: Int,
    start: UnitReference?,
    end: UnitReference?,
    gemaraUnit: GemaraUnit,
    mishnahUnit: MishnahUnit,
    chelek: Int,
    mishnahBerurahUnit: MishnahBerurahUnit,
): List<UnitReference> {
    requireNotNull(start)
    requireNotNull(end)
    val fullRange = when (choice) {
        SeferChoice.GEMARA, SeferChoice.YERUSHALMI, SeferChoice.MISHNAH, SeferChoice.RAMBAM -> {
            val catalog = when (choice) {
                SeferChoice.GEMARA -> MaterialCatalog.gemara
                SeferChoice.YERUSHALMI -> MaterialCatalog.yerushalmi
                SeferChoice.MISHNAH -> MaterialCatalog.mishnah
                SeferChoice.RAMBAM -> MaterialCatalog.rambam
                else -> error("Not a sectioned sefer")
            }
            require(fromMasechtaIndex in catalog.indices && toMasechtaIndex in fromMasechtaIndex..catalog.lastIndex)
            catalog.subList(fromMasechtaIndex, toMasechtaIndex + 1).flatMap {
                materialUnitOptions(choice, it, gemaraUnit, mishnahUnit, chelek, mishnahBerurahUnit)
            }
        }
        SeferChoice.MISHNAH_BERURAH, SeferChoice.CHOFETZ_CHAIM, SeferChoice.TEHILLIM, SeferChoice.KITZUR ->
            materialUnitOptions(choice, null, gemaraUnit, mishnahUnit, chelek, mishnahBerurahUnit)
        SeferChoice.OTHER -> emptyList()
    }
    val startIndex = fullRange.indexOf(start)
    val endIndex = fullRange.indexOf(end)
    require(startIndex >= 0 && endIndex >= startIndex)
    return fullRange.subList(startIndex, endIndex + 1)
}

private fun materialUnitOptions(
    choice: SeferChoice,
    masechta: Masechta?,
    gemaraUnit: GemaraUnit,
    mishnahUnit: MishnahUnit,
    chelek: Int,
    mishnahBerurahUnit: MishnahBerurahUnit,
): List<UnitReference> = when (choice) {
    SeferChoice.GEMARA -> requireNotNull(masechta).let {
        MaterialCatalog.gemaraUnits(listOf(it), 2, it.lastLocation, gemaraUnit)
    }
    SeferChoice.YERUSHALMI -> MaterialCatalog.yerushalmiUnits(requireNotNull(masechta))
    SeferChoice.MISHNAH -> MaterialCatalog.mishnahUnits(requireNotNull(masechta), mishnahUnit)
    SeferChoice.MISHNAH_BERURAH -> MaterialCatalog.mishnahBerurahUnitOptions(chelek, mishnahBerurahUnit)
    SeferChoice.RAMBAM -> MaterialCatalog.rambamUnits(requireNotNull(masechta))
    SeferChoice.CHOFETZ_CHAIM -> MaterialCatalog.chofetzChaimUnits
    SeferChoice.TEHILLIM -> MaterialCatalog.tehillimUnits
    SeferChoice.KITZUR -> MaterialCatalog.simanim("Kitzur Shulchan Aruch", "קיצור שולחן ערוך", 1, 221)
    SeferChoice.OTHER -> emptyList()
}
