package app.veshinantam.data.backup

import android.content.Context
import android.net.Uri
import app.veshinantam.data.ScheduleDefaultsSettings
import app.veshinantam.data.local.ProgressGoalEntity
import app.veshinantam.data.local.ScheduleDao
import app.veshinantam.data.local.ScheduleEntity
import app.veshinantam.data.local.ScheduleExclusionEntity
import app.veshinantam.data.local.TaskEntity
import app.veshinantam.domain.model.MaterialType
import app.veshinantam.domain.model.MissedWorkBehavior
import app.veshinantam.domain.model.ScheduleKind
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import app.veshinantam.localization.AppLanguage
import app.veshinantam.localization.LanguageSettings
import app.veshinantam.localization.PrimaryCalendar
import app.veshinantam.localization.SefarimLanguage
import app.veshinantam.notifications.ReminderPreference
import app.veshinantam.notifications.ReminderSettings
import app.veshinantam.data.preset.PresetCatalogUpdateScheduler
import app.veshinantam.data.preset.PresetUpdateSettings
import app.veshinantam.ui.today.TodayDisplaySettings
import app.veshinantam.ui.today.TodaySortOrder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

data class BackupSummary(
    val scheduleCount: Int,
    val taskCount: Int,
)

class BackupException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

class BackupService(
    private val context: Context,
    private val dao: ScheduleDao,
) {
    suspend fun write(uri: Uri): BackupSummary {
        val payload = BackupPayload(
            schedules = dao.getAllSchedules(),
            exclusions = dao.getAllExclusions(),
            tasks = dao.getAllTasks(),
            goals = dao.getAllProgressGoals(),
            preferences = readPreferences(),
        )
        val bytes = BackupJson.encode(payload).toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: throw BackupException("The selected backup file could not be opened.")
        return BackupSummary(payload.schedules.size, payload.tasks.size)
    }

    suspend fun restore(uri: Uri): BackupSummary {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readNBytes(MAX_BACKUP_BYTES + 1)
            if (bytes.size > MAX_BACKUP_BYTES) throw BackupException("The backup file is too large.")
            bytes.toString(Charsets.UTF_8)
        } ?: throw BackupException("The selected backup file could not be opened.")
        val payload = BackupJson.decode(text)
        validate(payload)
        dao.replaceAllData(payload.schedules, payload.exclusions, payload.tasks, payload.goals)
        savePreferences(payload.preferences)
        return BackupSummary(payload.schedules.size, payload.tasks.size)
    }

    private fun readPreferences() = BackupPreferences(
        appLanguage = LanguageSettings(context).read(),
        sefarimLanguage = LanguageSettings(context).readSefarimLanguage(),
        primaryCalendar = LanguageSettings(context).readPrimaryCalendar(),
        defaultChazarahOffsets = ScheduleDefaultsSettings(context).readChazarahOffsets(),
        reminder = ReminderSettings(context).read(),
        todaySortOrder = TodayDisplaySettings(context).readSortOrder(),
        automaticPresetUpdates = PresetUpdateSettings(context).read().automaticUpdates,
    )

    private fun savePreferences(value: BackupPreferences) {
        LanguageSettings(context).apply {
            save(value.appLanguage)
            saveSefarimLanguage(value.sefarimLanguage)
            savePrimaryCalendar(value.primaryCalendar)
        }
        ScheduleDefaultsSettings(context).saveChazarahOffsets(value.defaultChazarahOffsets)
        ReminderSettings(context).save(value.reminder)
        TodayDisplaySettings(context).saveSortOrder(value.todaySortOrder)
        PresetUpdateSettings(context).saveAutomatic(value.automaticPresetUpdates)
        PresetCatalogUpdateScheduler.sync(context, value.automaticPresetUpdates)
    }

    private fun validate(payload: BackupPayload) {
        if (payload.schedules.size > MAX_ROWS || payload.tasks.size > MAX_ROWS || payload.exclusions.size > MAX_ROWS) {
            throw BackupException("The backup contains too many records.")
        }
        val scheduleIds = payload.schedules.map { it.id }
        if (scheduleIds.any(String::isBlank) || scheduleIds.distinct().size != scheduleIds.size) {
            throw BackupException("The backup contains invalid or duplicate schedule IDs.")
        }
        val knownSchedules = scheduleIds.toSet()
        if (payload.tasks.any { it.scheduleId !in knownSchedules } || payload.exclusions.any { it.scheduleId !in knownSchedules }) {
            throw BackupException("The backup contains records for a missing schedule.")
        }
        if (payload.tasks.map { it.id }.let { it.any(String::isBlank) || it.distinct().size != it.size }) {
            throw BackupException("The backup contains invalid or duplicate task IDs.")
        }
        if (payload.tasks.groupBy { it.scheduleId }.values.any { tasks -> tasks.map { it.stableKey }.distinct().size != tasks.size }) {
            throw BackupException("The backup contains duplicate task identities.")
        }
        if (payload.exclusions.distinctBy { it.scheduleId to it.date }.size != payload.exclusions.size ||
            payload.goals.map { it.kind }.distinct().size != payload.goals.size
        ) {
            throw BackupException("The backup contains duplicate exclusions or goals.")
        }
        if (payload.tasks.any { (it.completedAt == null) != (it.completionLocalDate == null) || (it.completedAt == null) != (it.completionZoneId == null) }) {
            throw BackupException("The backup contains incomplete completion history.")
        }
        if (payload.schedules.any { it.dailyQuantity < 0 || it.generationRevision < 1 } ||
            payload.tasks.any { it.quantity <= 0.0 || it.generationRevision < 1 } ||
            payload.goals.any { it.kind.isBlank() || !it.target.isFinite() || it.target <= 0.0 }
        ) {
            throw BackupException("The backup contains invalid numeric values.")
        }
        payload.schedules.forEach { schedule ->
            val weekdays = schedule.selectedWeekdays.split(',').filter(String::isNotBlank)
            if (weekdays.isEmpty() || weekdays.any { runCatching { DayOfWeek.valueOf(it.trim()) }.isFailure }) {
                throw BackupException("The backup contains invalid learning weekdays.")
            }
            val offsets = schedule.chazarahDayOffsets.split(',').filter(String::isNotBlank).map { it.trim().toIntOrNull() }
            if (offsets.any { it == null || it <= 0 } || offsets.filterNotNull().distinct().size != offsets.size) {
                throw BackupException("The backup contains invalid chazarah offsets.")
            }
        }
        if (payload.tasks.any { task -> task.completionZoneId?.let { runCatching { ZoneId.of(it) }.isFailure } == true }) {
            throw BackupException("The backup contains an invalid completion time zone.")
        }
        if (payload.preferences.defaultChazarahOffsets.isEmpty() ||
            payload.preferences.defaultChazarahOffsets.any { it <= 0 } ||
            payload.preferences.defaultChazarahOffsets.distinct().size != payload.preferences.defaultChazarahOffsets.size ||
            payload.preferences.reminder.hour !in 0..23 || payload.preferences.reminder.minute !in 0..59
        ) {
            throw BackupException("The backup contains invalid preferences.")
        }
    }

    private companion object {
        const val MAX_BACKUP_BYTES = 64 * 1024 * 1024
        const val MAX_ROWS = 1_000_000
    }
}

internal data class BackupPayload(
    val schedules: List<ScheduleEntity>,
    val exclusions: List<ScheduleExclusionEntity>,
    val tasks: List<TaskEntity>,
    val goals: List<ProgressGoalEntity>,
    val preferences: BackupPreferences,
)

internal data class BackupPreferences(
    val appLanguage: AppLanguage,
    val sefarimLanguage: SefarimLanguage,
    val primaryCalendar: PrimaryCalendar,
    val defaultChazarahOffsets: List<Int>,
    val reminder: ReminderPreference,
    val todaySortOrder: TodaySortOrder,
    val automaticPresetUpdates: Boolean = false,
)

internal object BackupJson {
    private const val FORMAT = "app.veshinantam.backup"
    private const val VERSION = 2

    fun encode(payload: BackupPayload): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("createdAt", Instant.now().toString())
        put("schedules", JSONArray().apply { payload.schedules.forEach { put(scheduleToJson(it)) } })
        put("exclusions", JSONArray().apply { payload.exclusions.forEach { put(exclusionToJson(it)) } })
        put("tasks", JSONArray().apply { payload.tasks.forEach { put(taskToJson(it)) } })
        put("goals", JSONArray().apply { payload.goals.forEach { put(goalToJson(it)) } })
        put("preferences", preferencesToJson(payload.preferences))
    }.toString(2)

    fun decode(text: String): BackupPayload = try {
        val root = JSONObject(text)
        if (root.requiredString("format") != FORMAT) throw BackupException("This is not a VeShinantam backup.")
        val version = root.requiredInt("version")
        if (version !in 1..VERSION) throw BackupException("Backup version $version is not supported by this app version.")
        BackupPayload(
            schedules = root.requiredArray("schedules").objects(::scheduleFromJson),
            exclusions = root.requiredArray("exclusions").objects(::exclusionFromJson),
            tasks = root.requiredArray("tasks").objects(::taskFromJson),
            goals = root.requiredArray("goals").objects(::goalFromJson),
            preferences = preferencesFromJson(root.requiredObject("preferences"), version),
        )
    } catch (error: BackupException) {
        throw error
    } catch (error: Exception) {
        throw BackupException("The backup is damaged or incomplete.", error)
    }

    private fun scheduleToJson(value: ScheduleEntity) = JSONObject().apply {
        put("id", value.id); put("nameEnglish", value.nameEnglish); put("nameHebrew", value.nameHebrew)
        put("kind", value.kind.name); put("sourceType", value.sourceType); put("materialType", value.materialType.name)
        putNullable("presetId", value.presetId); put("startDate", value.startDate.toString()); putNullable("targetDate", value.targetDate?.toString())
        put("dailyQuantity", value.dailyQuantity); put("selectedWeekdays", value.selectedWeekdays); put("chazarahDayOffsets", value.chazarahDayOffsets)
        put("repeatsAnnually", value.repeatsAnnually); put("officialOraysaChazarah", value.officialOraysaChazarah)
        put("missedWorkBehavior", value.missedWorkBehavior.name); put("state", value.state.name)
        put("generationRevision", value.generationRevision); put("createdAt", value.createdAt.toString())
    }

    private fun scheduleFromJson(value: JSONObject) = ScheduleEntity(
        id = value.requiredString("id"), nameEnglish = value.requiredString("nameEnglish"), nameHebrew = value.requiredString("nameHebrew"),
        kind = value.enum("kind"), sourceType = value.requiredString("sourceType"), materialType = value.enum("materialType"),
        presetId = value.optionalString("presetId"), startDate = value.date("startDate"), targetDate = value.optionalString("targetDate")?.let(LocalDate::parse),
        dailyQuantity = value.requiredInt("dailyQuantity"), selectedWeekdays = value.requiredString("selectedWeekdays"),
        chazarahDayOffsets = value.requiredString("chazarahDayOffsets"), repeatsAnnually = value.requiredBoolean("repeatsAnnually"),
        officialOraysaChazarah = value.requiredBoolean("officialOraysaChazarah"), missedWorkBehavior = value.enum("missedWorkBehavior"),
        state = value.enum("state"), generationRevision = value.requiredInt("generationRevision"), createdAt = value.instant("createdAt"),
    )

    private fun exclusionToJson(value: ScheduleExclusionEntity) = JSONObject().apply {
        put("scheduleId", value.scheduleId); put("date", value.date.toString())
    }
    private fun exclusionFromJson(value: JSONObject) = ScheduleExclusionEntity(value.requiredString("scheduleId"), value.date("date"))

    private fun taskToJson(value: TaskEntity) = JSONObject().apply {
        put("id", value.id); put("stableKey", value.stableKey); put("scheduleId", value.scheduleId); put("type", value.type.name)
        put("labelEnglish", value.labelEnglish); put("labelHebrew", value.labelHebrew); put("materialType", value.materialType.name)
        put("quantity", value.quantity); put("plannedDate", value.plannedDate.toString()); put("originalLearningDate", value.originalLearningDate.toString())
        putNullable("reviewIdentity", value.reviewIdentity); put("generationRevision", value.generationRevision)
        putNullable("completedAt", value.completedAt?.toString()); putNullable("completionLocalDate", value.completionLocalDate?.toString())
        putNullable("completionZoneId", value.completionZoneId)
    }

    private fun taskFromJson(value: JSONObject) = TaskEntity(
        id = value.requiredString("id"), stableKey = value.requiredString("stableKey"), scheduleId = value.requiredString("scheduleId"), type = value.enum("type"),
        labelEnglish = value.requiredString("labelEnglish"), labelHebrew = value.requiredString("labelHebrew"), materialType = value.enum("materialType"),
        quantity = value.requiredDouble("quantity"), plannedDate = value.date("plannedDate"), originalLearningDate = value.date("originalLearningDate"),
        reviewIdentity = value.optionalString("reviewIdentity"), generationRevision = value.requiredInt("generationRevision"),
        completedAt = value.optionalString("completedAt")?.let(Instant::parse), completionLocalDate = value.optionalString("completionLocalDate")?.let(LocalDate::parse),
        completionZoneId = value.optionalString("completionZoneId"),
    )

    private fun goalToJson(value: ProgressGoalEntity) = JSONObject().apply { put("kind", value.kind); put("target", value.target) }
    private fun goalFromJson(value: JSONObject) = ProgressGoalEntity(value.requiredString("kind"), value.requiredDouble("target"))

    private fun preferencesToJson(value: BackupPreferences) = JSONObject().apply {
        put("appLanguage", value.appLanguage.name); put("sefarimLanguage", value.sefarimLanguage.name); put("primaryCalendar", value.primaryCalendar.name)
        put("defaultChazarahOffsets", JSONArray(value.defaultChazarahOffsets)); put("reminderEnabled", value.reminder.enabled)
        put("reminderHour", value.reminder.hour); put("reminderMinute", value.reminder.minute); put("todaySortOrder", value.todaySortOrder.name)
        put("automaticPresetUpdates", value.automaticPresetUpdates)
    }

    private fun preferencesFromJson(value: JSONObject, version: Int) = BackupPreferences(
        appLanguage = value.enum("appLanguage"), sefarimLanguage = value.enum("sefarimLanguage"), primaryCalendar = value.enum("primaryCalendar"),
        defaultChazarahOffsets = value.requiredArray("defaultChazarahOffsets").ints(),
        reminder = ReminderPreference(value.requiredBoolean("reminderEnabled"), value.requiredInt("reminderHour"), value.requiredInt("reminderMinute")),
        todaySortOrder = value.enum("todaySortOrder"),
        automaticPresetUpdates = if (version >= 2) value.requiredBoolean("automaticPresetUpdates") else false,
    )

    private fun JSONObject.putNullable(key: String, value: String?) { put(key, value ?: JSONObject.NULL) }
    private fun JSONObject.requiredString(key: String) = getString(key)
    private fun JSONObject.requiredInt(key: String) = getInt(key)
    private fun JSONObject.requiredDouble(key: String) = getDouble(key)
    private fun JSONObject.requiredBoolean(key: String) = getBoolean(key)
    private fun JSONObject.requiredArray(key: String) = getJSONArray(key)
    private fun JSONObject.requiredObject(key: String) = getJSONObject(key)
    private fun JSONObject.optionalString(key: String) = if (isNull(key)) null else getString(key)
    private fun JSONObject.date(key: String) = LocalDate.parse(requiredString(key))
    private fun JSONObject.instant(key: String) = Instant.parse(requiredString(key))
    private inline fun <reified T : Enum<T>> JSONObject.enum(key: String): T = enumValueOf(requiredString(key))
    private fun <T> JSONArray.objects(transform: (JSONObject) -> T) = List(length()) { transform(getJSONObject(it)) }
    private fun JSONArray.ints() = List(length()) { getInt(it) }
}
