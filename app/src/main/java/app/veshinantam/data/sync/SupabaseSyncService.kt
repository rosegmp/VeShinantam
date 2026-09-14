package app.veshinantam.data.sync

import android.content.Context
import android.net.Uri
import android.util.Base64
import app.veshinantam.BuildConfig
import app.veshinantam.data.local.ScheduleDao
import app.veshinantam.data.local.ScheduleEntity
import app.veshinantam.data.local.TaskEntity
import app.veshinantam.domain.model.MaterialType
import app.veshinantam.domain.model.MissedWorkBehavior
import app.veshinantam.domain.model.ScheduleKind
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import app.veshinantam.localization.LanguageSettings
import app.veshinantam.widget.WidgetUpdater
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

data class AccountSyncState(
    val configured: Boolean,
    val email: String? = null,
    val status: String? = null,
    val conflict: Boolean = false,
)

class SupabaseSyncService(
    context: Context,
    private val dao: ScheduleDao,
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val deviceSyncPreferences = appContext.getSharedPreferences(DEVICE_SYNC_PREFERENCES, Context.MODE_PRIVATE)
    private val entitySync = AndroidEntitySync(appContext, dao) { path, method, body ->
        request(path, method, body)
    }

    fun state() = AccountSyncState(
        configured = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank(),
        email = preferences.getString(KEY_EMAIL, null),
        status = preferences.getString(KEY_STATUS, null),
        conflict = preferences.contains(KEY_CONFLICT),
    )

    fun handleAuthRedirect(uri: Uri): Boolean {
        val fragment = uri.fragment ?: return false
        val parameters = Uri.parse("https://auth.local/?$fragment")
        val accessToken = parameters.getQueryParameter("access_token") ?: return false
        val refreshToken = parameters.getQueryParameter("refresh_token") ?: return false
        saveSession(accessToken, refreshToken)
        preferences.edit()
            .remove(KEY_REVISION)
            .remove(KEY_CONFLICT)
            .putString(KEY_STATUS, "Signed in. Tap Sync now to connect this device.")
            .apply()
        scheduleAutomaticSync()
        return true
    }

    suspend fun sendMagicLink(email: String): Result<Unit> = runCatching {
        val body = magicLinkRequestBody(email, "app.veshinantam://auth")
        request("/auth/v1/otp", "POST", body, authenticated = false)
        preferences.edit().putString(KEY_STATUS, "Check your email for the secure sign-in link.").apply()
    }.onFailure {
        preferences.edit().putString(KEY_STATUS, "Error sending the sign-in link. Please try again.").apply()
    }

    suspend fun sync(): Result<Unit> = runCatching {
        preferences.edit().putString(KEY_STATUS, "Synchronizing…").apply()
        if (BuildConfig.ENTITY_SYNC_ENABLED) {
            prepareEntityAccount()
            val result = entitySync.synchronize()
            WidgetUpdater.enqueueImmediate(appContext)
            preferences.edit()
                .remove(KEY_CONFLICT)
                .putString(
                    KEY_STATUS,
                    if (result.conflicts > 0) {
                        "Synced with ${result.conflicts} newer cloud change${if (result.conflicts == 1) "" else "s"}."
                    } else {
                        "Synced successfully."
                    },
                )
                .apply()
            return@runCatching
        }
        val local = localPayload()
        val rows = JSONArray(request("/rest/v1/learning_snapshots?select=payload,revision&limit=1"))
        if (rows.length() == 0) {
            push(local, 0)
            return@runCatching
        }
        val remote = rows.getJSONObject(0)
        val remotePayload = remote.getJSONObject("payload")
        val remoteRevision = remote.getLong("revision")
        val localRevision = preferences.getLong(KEY_REVISION, -1)
        if (local.toString() == remotePayload.toString()) {
            preferences.edit().putLong(KEY_REVISION, remoteRevision).remove(KEY_CONFLICT)
                .putString(KEY_STATUS, "Already up to date.").apply()
        } else if (localRevision != remoteRevision) {
            preferences.edit().putString(
                KEY_CONFLICT,
                JSONObject().put("payload", remotePayload).put("revision", remoteRevision).toString(),
            ).putString(KEY_STATUS, "Choose which copy to keep.").apply()
        } else {
            push(local, remoteRevision)
        }
    }.onFailure {
        preferences.edit().putString(KEY_STATUS, "Error synchronizing. Device data is unchanged.").apply()
    }

    suspend fun useCloudCopy(): Result<Unit> = runCatching {
        val conflict = JSONObject(requireNotNull(preferences.getString(KEY_CONFLICT, null)))
        mergeCloudPayload(conflict.getJSONObject("payload"))
        preferences.edit().putLong(KEY_REVISION, conflict.getLong("revision")).remove(KEY_CONFLICT)
            .putString(KEY_STATUS, "Cloud changes merged into this device.").apply()
    }.onFailure {
        preferences.edit().putString(KEY_STATUS, "Error applying cloud changes. Device data is unchanged.").apply()
    }

    suspend fun replaceCloudCopy(): Result<Unit> = runCatching {
        val conflict = JSONObject(requireNotNull(preferences.getString(KEY_CONFLICT, null)))
        push(localPayload(), conflict.getLong("revision"))
    }.onFailure {
        preferences.edit().putString(KEY_STATUS, "Error replacing the cloud copy. Device data is unchanged.").apply()
    }

    fun signOut() {
        EntitySyncScheduler.cancel(appContext)
        preferences.edit().clear().putString(KEY_STATUS, "Signed out. Device data remains available offline.").apply()
    }

    fun scheduleAutomaticSync() {
        if (BuildConfig.ENTITY_SYNC_ENABLED && preferences.contains(KEY_ACCESS_TOKEN)) {
            EntitySyncScheduler.enqueue(appContext)
        }
    }

    suspend fun automaticSync(): Boolean {
        if (!BuildConfig.ENTITY_SYNC_ENABLED || !preferences.contains(KEY_ACCESS_TOKEN)) return true
        return sync().isSuccess
    }

    private suspend fun prepareEntityAccount() {
        val accountId = preferences.getString(KEY_USER_ID, null)
            ?: preferences.getString(KEY_ACCESS_TOKEN, null)?.let(::tokenClaims)?.optString("sub")
                ?.takeIf(String::isNotBlank)
                ?.also { preferences.edit().putString(KEY_USER_ID, it).apply() }
            ?: error("Please sign in again.")
        val boundAccountId = deviceSyncPreferences.getString(KEY_ENTITY_ACCOUNT_ID, null)
        if (boundAccountId == null) {
            entitySync.resetForAccount()
            deviceSyncPreferences.edit().putString(KEY_ENTITY_ACCOUNT_ID, accountId).apply()
        } else if (boundAccountId != accountId) {
            error("This device's offline data is linked to another account. Clear the app's data before linking a different account.")
        }
    }

    private suspend fun localPayload(): JSONObject {
        val schedules = dao.getAllSchedules()
        val tasks = dao.getAllTasks()
        return JSONObject()
            .put("schedules", JSONArray().apply {
                schedules.forEach { schedule ->
                    put(JSONObject()
                        .put("id", schedule.id)
                        .put("name", schedule.nameEnglish.ifBlank { schedule.nameHebrew })
                        .put("material", schedule.materialType.name)
                        .put("pace", schedule.dailyQuantity.coerceAtLeast(1))
                        .put("weekdays", JSONArray(schedule.selectedWeekdays.split(',').mapNotNull { value ->
                            runCatching { DayOfWeek.valueOf(value.trim()).value % 7 }.getOrNull()
                        }))
                        .put("chazarahOffsets", JSONArray(schedule.chazarahDayOffsets.split(',').mapNotNull { it.trim().toIntOrNull() }))
                        .put("active", schedule.state == ScheduleState.ACTIVE)
                        .put("archived", schedule.state == ScheduleState.ARCHIVED))
                }
            })
            .put("tasks", JSONArray().apply {
                tasks.forEach { task ->
                    put(JSONObject()
                        .put("id", task.id)
                        .put("scheduleId", task.scheduleId)
                        .put("referenceEnglish", task.labelEnglish)
                        .put("referenceHebrew", task.labelHebrew)
                        .put("dueDate", task.plannedDate.toString())
                        .put("type", task.type.name)
                        .put("completed", task.completedAt != null))
                }
            })
            .put("language", LanguageSettings(appContext).read().languageTag)
    }

    private suspend fun mergeCloudPayload(payload: JSONObject) {
        val scheduleObjects = payload.getJSONArray("schedules")
        val taskObjects = payload.getJSONArray("tasks")
        val existingSchedules = dao.getAllSchedules().associateBy { it.id }
        val existingTasks = dao.getAllTasks().associateBy { it.id }
        val now = Instant.now()

        for (index in 0 until scheduleObjects.length()) {
            val value = scheduleObjects.getJSONObject(index)
            val id = value.getString("id")
            val archived = value.optBoolean("archived", false)
            val active = value.optBoolean("active", true)
            val state = if (archived) ScheduleState.ARCHIVED else if (active) ScheduleState.ACTIVE else ScheduleState.PAUSED
            existingSchedules[id]?.let { dao.updateSchedule(it.copy(state = state)) } ?: run {
                val dates = (0 until taskObjects.length()).mapNotNull { taskIndex ->
                    taskObjects.getJSONObject(taskIndex).takeIf { it.getString("scheduleId") == id }
                        ?.optString("dueDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                }
                val weekdays = value.optJSONArray("weekdays")?.let { array ->
                    (0 until array.length()).map { day ->
                        val webDay = array.getInt(day)
                        if (webDay == 0) DayOfWeek.SUNDAY else DayOfWeek.of(webDay)
                    }
                }.orEmpty().ifEmpty { DayOfWeek.entries }
                val offsets = value.optJSONArray("chazarahOffsets")?.let { array ->
                    (0 until array.length()).map { array.getInt(it) }
                }.orEmpty()
                dao.insertSchedule(ScheduleEntity(
                    id = id,
                    nameEnglish = value.optString("name"),
                    nameHebrew = value.optString("name"),
                    kind = ScheduleKind.CUSTOM,
                    sourceType = "SYNCED_WEB",
                    materialType = MaterialType.CUSTOM_UNIT,
                    presetId = null,
                    startDate = dates.minOrNull() ?: LocalDate.now(),
                    targetDate = dates.maxOrNull(),
                    dailyQuantity = value.optInt("pace", 1).coerceAtLeast(1),
                    selectedWeekdays = weekdays.joinToString(",") { it.name },
                    chazarahDayOffsets = offsets.joinToString(","),
                    repeatsAnnually = false,
                    officialOraysaChazarah = false,
                    missedWorkBehavior = MissedWorkBehavior.KEEP_FIXED_OVERDUE,
                    state = state,
                    generationRevision = 1,
                    createdAt = now,
                ))
            }
        }

        val newTasks = mutableListOf<TaskEntity>()
        for (index in 0 until taskObjects.length()) {
            val value = taskObjects.getJSONObject(index)
            val id = value.getString("id")
            val complete = value.optBoolean("completed", false)
            existingTasks[id]?.let { task ->
                val completedAt = when {
                    complete && task.completedAt == null -> now
                    complete -> task.completedAt
                    else -> null
                }
                dao.updateCompletion(
                    id,
                    completedAt,
                    completedAt?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate(),
                    completedAt?.let { java.time.ZoneId.systemDefault().id },
                )
            } ?: newTasks.add(TaskEntity(
                id = id,
                stableKey = "sync:$id",
                scheduleId = value.getString("scheduleId"),
                type = runCatching { TaskType.valueOf(value.getString("type")) }.getOrDefault(TaskType.LEARNING),
                labelEnglish = value.optString("referenceEnglish"),
                labelHebrew = value.optString("referenceHebrew"),
                materialType = MaterialType.CUSTOM_UNIT,
                quantity = 1.0,
                plannedDate = LocalDate.parse(value.getString("dueDate")),
                originalLearningDate = LocalDate.parse(value.getString("dueDate")),
                reviewIdentity = null,
                generationRevision = 1,
                completedAt = now.takeIf { complete },
                completionLocalDate = LocalDate.now().takeIf { complete },
                completionZoneId = java.time.ZoneId.systemDefault().id.takeIf { complete },
            ))
        }
        if (newTasks.isNotEmpty()) dao.insertTasks(newTasks)
    }

    private fun push(payload: JSONObject, expectedRevision: Long) {
        val body = JSONObject().put("expected_revision", expectedRevision).put("new_payload", payload).toString()
        val revision = request("/rest/v1/rpc/sync_learning_snapshot", "POST", body).trim().toLong()
        preferences.edit().putLong(KEY_REVISION, revision).remove(KEY_CONFLICT)
            .putString(KEY_STATUS, "Synced successfully.").apply()
    }

    private fun authenticatedToken(): String {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: error("Please sign in again.")
        val expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0)
        if (expiresAt > System.currentTimeMillis() + 60_000) return accessToken
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null) ?: error("Please sign in again.")
        val body = JSONObject().put("refresh_token", refreshToken).toString()
        val response = JSONObject(request("/auth/v1/token?grant_type=refresh_token", "POST", body, authenticated = false))
        saveSession(response.getString("access_token"), response.getString("refresh_token"))
        return preferences.getString(KEY_ACCESS_TOKEN, null) ?: error("Please sign in again.")
    }

    private fun request(path: String, method: String = "GET", body: String? = null, authenticated: Boolean = true): String {
        val connection = URL(BuildConfig.SUPABASE_URL + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Content-Type", "application/json")
            if (authenticated) connection.setRequestProperty("Authorization", "Bearer ${authenticatedToken()}")
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) error(text.ifBlank { "Request failed ($status)." })
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun saveSession(accessToken: String, refreshToken: String) {
        val claims = tokenClaims(accessToken)
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_EMAIL, claims.optString("email"))
            .putString(KEY_USER_ID, claims.optString("sub"))
            .putLong(KEY_EXPIRES_AT, claims.optLong("exp") * 1000)
            .apply()
    }

    private fun tokenClaims(accessToken: String): JSONObject = runCatching {
        val encoded = accessToken.split('.')[1]
        JSONObject(String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8))
    }.getOrDefault(JSONObject())

    private companion object {
        const val PREFERENCES = "supabase_account"
        const val DEVICE_SYNC_PREFERENCES = "supabase_device_sync"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EMAIL = "email"
        const val KEY_USER_ID = "user_id"
        const val KEY_ENTITY_ACCOUNT_ID = "entity_account_id"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_REVISION = "revision"
        const val KEY_STATUS = "status"
        const val KEY_CONFLICT = "conflict"
    }
}

internal fun magicLinkRequestBody(email: String, redirectTo: String): String = JSONObject()
    .put("email", email.trim())
    .put("create_user", true)
    .put("redirect_to", redirectTo)
    .toString()
