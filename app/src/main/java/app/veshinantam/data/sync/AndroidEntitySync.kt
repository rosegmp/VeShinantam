package app.veshinantam.data.sync

import android.content.Context
import app.veshinantam.data.ScheduleDefaultsSettings
import app.veshinantam.data.local.ScheduleDao
import app.veshinantam.data.local.SyncMetadataEntity
import app.veshinantam.data.local.SyncOutboxEntity
import app.veshinantam.data.local.SyncShadowEntity
import app.veshinantam.data.preset.PresetUpdateSettings
import app.veshinantam.data.preset.PresetCatalogUpdateScheduler
import app.veshinantam.localization.AppLanguage
import app.veshinantam.localization.LanguageSettings
import app.veshinantam.localization.PrimaryCalendar
import app.veshinantam.localization.SefarimLanguage
import app.veshinantam.notifications.ReminderPreference
import app.veshinantam.notifications.ReminderScheduler
import app.veshinantam.notifications.ReminderSettings
import app.veshinantam.shared.CanonicalDataCodec
import app.veshinantam.shared.CanonicalExclusion
import app.veshinantam.shared.CanonicalGoal
import app.veshinantam.shared.CanonicalPreferences
import app.veshinantam.shared.CanonicalPrimaryCalendar
import app.veshinantam.shared.CanonicalSchedule
import app.veshinantam.shared.CanonicalSefarimLanguage
import app.veshinantam.shared.CanonicalTask
import app.veshinantam.shared.SyncEntityType
import app.veshinantam.ui.today.TodayDisplaySettings
import app.veshinantam.ui.today.TodaySortOrder
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

internal data class EntitySyncResult(
    val pushed: Int,
    val pulled: Int,
    val conflicts: Int,
)

internal class AndroidEntitySync(
    context: Context,
    private val dao: ScheduleDao,
    private val request: (path: String, method: String, body: String?) -> String,
) {
    private val appContext = context.applicationContext

    suspend fun synchronize(): EntitySyncResult = processMutex.withLock {
        syncPhase("Preparing device changes") { captureLocalChanges() }
        val push = syncPhase("Uploading device changes") { pushOutbox() }
        val pulled = syncPhase("Downloading cloud changes") { pullRemoteChanges() }
        EntitySyncResult(push.first, pulled, push.second)
    }

    suspend fun resetForAccount() = processMutex.withLock {
        dao.clearSyncState()
    }

    private suspend fun captureLocalChanges() {
        val now = Instant.now()
        val preferences = readPreferences(now)
        val stableData = CanonicalAndroidMapper.export(
            schedules = dao.getAllSchedules(),
            exclusions = dao.getAllExclusions(),
            tasks = emptyList(),
            goals = dao.getAllProgressGoals(),
            preferences = preferences,
            now = now,
            syncCursor = dao.getSyncMetadata(CURSOR_KEY) ?: 0,
        )
        captureEntities(buildList {
            stableData.schedules.forEach { add(LocalEntity(SyncEntityType.SCHEDULE, it.id, encode(CanonicalSchedule.serializer(), it))) }
            stableData.exclusions.forEach { add(LocalEntity(SyncEntityType.EXCLUSION, it.id, encode(CanonicalExclusion.serializer(), it))) }
            stableData.goals.forEach { add(LocalEntity(SyncEntityType.GOAL, it.id, encode(CanonicalGoal.serializer(), it))) }
            add(LocalEntity(SyncEntityType.PREFERENCES, PREFERENCES_ID, encode(CanonicalPreferences.serializer(), stableData.preferences)))
        })

        var afterTaskId = ""
        do {
            val taskPage = dao.getTasksPage(afterTaskId, LOCAL_TASK_PAGE_SIZE)
            val canonicalTasks = CanonicalAndroidMapper.export(
                schedules = emptyList(),
                exclusions = emptyList(),
                tasks = taskPage,
                goals = emptyList(),
                preferences = preferences,
                now = now,
            ).tasks
            captureEntities(canonicalTasks.map {
                LocalEntity(SyncEntityType.TASK, it.id, encode(CanonicalTask.serializer(), it))
            })
            afterTaskId = taskPage.lastOrNull()?.id ?: afterTaskId
        } while (taskPage.size == LOCAL_TASK_PAGE_SIZE)

        dao.getMissingLocalSyncShadows().forEach { shadow ->
            enqueue(SyncEntityType.valueOf(shadow.entityType), shadow.entityId, null, deleted = true, shadow.revision)
        }
    }

    private suspend fun captureEntities(entities: List<LocalEntity>) {
        if (entities.isEmpty()) return
        val shadows = mutableMapOf<Pair<String, String>, SyncShadowEntity>()
        val pending = mutableMapOf<Pair<String, String>, SyncOutboxEntity>()
        entities.groupBy { it.type.name }.forEach { (entityType, values) ->
            val ids = values.map { it.id }
            dao.getSyncShadows(entityType, ids).forEach { shadows[it.entityType to it.entityId] = it }
            dao.getSyncOutbox(entityType, ids).forEach { pending[it.entityType to it.entityId] = it }
        }
        val redundantMutations = mutableListOf<String>()
        entities.forEach { entity ->
            val key = entity.type.name to entity.id
            val shadow = shadows[key]
            if (shadow != null && samePayload(shadow.payload, entity.payload) && !shadow.deleted) {
                pending[key]?.takeIf { !it.deleted && samePayload(it.payload, entity.payload) }
                    ?.let { redundantMutations += it.mutationId }
            } else {
                enqueue(entity.type, entity.id, entity.payload, deleted = false, shadow?.revision ?: 0)
            }
        }
        redundantMutations.chunked(OUTBOX_DELETE_BATCH_SIZE).forEach { dao.deleteSyncOutboxMutations(it) }
    }

    private suspend fun enqueue(type: SyncEntityType, id: String, payload: String?, deleted: Boolean, baseRevision: Long) {
        val existing = dao.getSyncOutbox(type.name, id)
        if (existing != null && existing.deleted == deleted && samePayload(existing.payload, payload)) return
        dao.upsertSyncOutbox(
            SyncOutboxEntity(
                entityType = type.name,
                entityId = id,
                mutationId = UUID.randomUUID().toString(),
                baseRevision = existing?.baseRevision ?: baseRevision,
                payload = payload,
                deleted = deleted,
                createdAt = Instant.now().toString(),
            ),
        )
    }

    private suspend fun pushOutbox(): Pair<Int, Int> {
        var pushed = 0
        var conflicts = 0
        for (batch in dao.getSyncOutbox().chunked(ENTITY_SYNC_PUSH_BATCH_SIZE)) {
            val rows = JSONArray(
                request(
                    "/rest/v1/rpc/apply_learning_mutations",
                    "POST",
                    mutationBatchRequestBody(batch),
                ),
            )
            val results = (0 until rows.length()).associate { index ->
                val row = rows.getJSONObject(index)
                row.getString("mutation_id") to row
            }
            check(results.size == batch.size) { "The sync server returned an incomplete mutation batch." }

            for (mutation in batch) {
                val result = checkNotNull(results[mutation.mutationId]) {
                    "The sync server omitted mutation ${mutation.mutationId}."
                }
                val revision = result.getLong("revision")
                val current = dao.getSyncOutbox(mutation.entityType, mutation.entityId)
                if (result.optBoolean("conflict")) {
                    conflicts++
                    val remote = RemoteEntity(
                        type = SyncEntityType.valueOf(mutation.entityType),
                        id = mutation.entityId,
                        payload = result.optJSONObject("remote_payload")?.toString(),
                        deleted = result.optBoolean("remote_deleted"),
                        revision = revision,
                    )
                    if (current?.mutationId == mutation.mutationId) {
                        applyRemote(remote)
                        dao.deleteSyncOutbox(mutation.entityType, mutation.entityId, mutation.mutationId)
                    } else {
                        updateShadow(remote)
                        dao.updateSyncOutboxBaseRevision(mutation.entityType, mutation.entityId, revision)
                    }
                } else {
                    pushed++
                    dao.upsertSyncShadow(
                        SyncShadowEntity(mutation.entityType, mutation.entityId, mutation.payload, revision, mutation.deleted),
                    )
                    if (current?.mutationId == mutation.mutationId) {
                        dao.deleteSyncOutbox(mutation.entityType, mutation.entityId, mutation.mutationId)
                    } else {
                        dao.updateSyncOutboxBaseRevision(mutation.entityType, mutation.entityId, revision)
                    }
                }
            }
        }
        return pushed to conflicts
    }

    private suspend fun pullRemoteChanges(): Int {
        var pageCursor = dao.getSyncMetadata(CURSOR_KEY) ?: 0
        var finalCursor = pageCursor
        var pulled = 0
        val deferredChildren = mutableListOf<RemoteEntity>()
        do {
            val rows = JSONArray(
                request(
                    "/rest/v1/learning_entities?select=entity_type,entity_id,payload,deleted,revision,updated_at&revision=gt.$pageCursor&order=revision.asc&limit=$PULL_PAGE_SIZE",
                    "GET",
                    null,
                ),
            )
            val records = List(rows.length()) { index ->
                val row = rows.getJSONObject(index)
                RemoteEntity(
                    type = SyncEntityType.valueOf(row.getString("entity_type")),
                    id = row.getString("entity_id"),
                    payload = row.optJSONObject("payload")?.toString(),
                    deleted = row.getBoolean("deleted"),
                    revision = row.getLong("revision"),
                )
            }
            val ordered = records.sortedWith(compareBy<RemoteEntity>({ applyOrder(it) }, { it.revision }))
            for (record in ordered) {
                if (hasMissingParent(record)) deferredChildren += record else applyPulledRecord(record)
            }
            records.maxOfOrNull { it.revision }?.let {
                pageCursor = it
                finalCursor = it
            }
            pulled += records.size
        } while (rows.length() == PULL_PAGE_SIZE)

        deferredChildren.forEach { record ->
            if (hasMissingParent(record)) {
                // Old sync versions could leave a task or exclusion behind after its schedule was removed.
                // Retain a shadow so the orphan is not retried, but do not violate the local database FK.
                updateShadow(record)
            } else {
                applyPulledRecord(record)
            }
        }
        if (finalCursor > (dao.getSyncMetadata(CURSOR_KEY) ?: 0)) {
            dao.upsertSyncMetadata(SyncMetadataEntity(CURSOR_KEY, finalCursor))
        }
        return pulled
    }

    private suspend fun applyPulledRecord(record: RemoteEntity) {
        val pending = dao.getSyncOutbox(record.type.name, record.id)
        try {
            if (pending == null) applyRemote(record) else {
                updateShadow(record)
                dao.updateSyncOutboxBaseRevision(record.type.name, record.id, record.revision)
            }
        } catch (error: Exception) {
            throw IllegalStateException(
                "Could not apply ${record.type}:${record.id} at revision ${record.revision}: " +
                    (error.message ?: error.javaClass.simpleName),
                error,
            )
        }
    }

    private suspend fun hasMissingParent(record: RemoteEntity): Boolean {
        if (record.deleted || (record.type != SyncEntityType.TASK && record.type != SyncEntityType.EXCLUSION)) {
            return false
        }
        val scheduleId = record.payload?.let(::JSONObject)?.optString("scheduleId").orEmpty()
        return scheduleId.isNotBlank() && dao.getSchedule(scheduleId) == null
    }

    private suspend fun applyRemote(record: RemoteEntity) {
        if (record.deleted) {
            when (record.type) {
                SyncEntityType.SCHEDULE -> dao.deleteSchedule(record.id)
                SyncEntityType.TASK -> dao.deleteSyncedTask(record.id)
                SyncEntityType.EXCLUSION -> record.id.split(':', limit = 2).takeIf { it.size == 2 }?.let {
                    dao.deleteSyncedExclusion(it[0], LocalDate.parse(it[1]))
                }
                SyncEntityType.GOAL -> dao.deleteSyncedGoal(record.id)
                SyncEntityType.MATERIAL_UNIT, SyncEntityType.PREFERENCES -> Unit
            }
            updateShadow(record)
            return
        }
        val payload = requireNotNull(record.payload) { "Missing sync payload for ${record.type}:${record.id}" }
        when (record.type) {
            SyncEntityType.SCHEDULE -> dao.upsertSyncedSchedule(
                CanonicalAndroidMapper.schedule(decode(CanonicalSchedule.serializer(), payload).copy(revision = record.revision)),
            )
            SyncEntityType.TASK -> dao.upsertSyncedTask(
                CanonicalAndroidMapper.task(decode(CanonicalTask.serializer(), payload).copy(revision = record.revision)),
            )
            SyncEntityType.EXCLUSION -> dao.upsertSyncedExclusion(
                CanonicalAndroidMapper.exclusion(decode(CanonicalExclusion.serializer(), payload).copy(revision = record.revision)),
            )
            SyncEntityType.GOAL -> dao.upsertSyncedGoal(
                CanonicalAndroidMapper.goal(decode(CanonicalGoal.serializer(), payload).copy(revision = record.revision)),
            )
            SyncEntityType.PREFERENCES -> applyPreferences(
                decode(CanonicalPreferences.serializer(), payload).copy(revision = record.revision),
            )
            SyncEntityType.MATERIAL_UNIT -> Unit
        }
        updateShadow(record)
    }

    private suspend fun updateShadow(record: RemoteEntity) {
        dao.upsertSyncShadow(
            SyncShadowEntity(record.type.name, record.id, record.payload, record.revision, record.deleted),
        )
    }

    private fun readPreferences(now: Instant): CanonicalPreferences {
        val languages = LanguageSettings(appContext)
        val reminder = ReminderSettings(appContext).read()
        return CanonicalPreferences(
            appLanguage = languages.read().languageTag,
            sefarimLanguage = CanonicalSefarimLanguage.valueOf(languages.readSefarimLanguage().name),
            primaryCalendar = CanonicalPrimaryCalendar.valueOf(languages.readPrimaryCalendar().name),
            defaultChazarahOffsets = ScheduleDefaultsSettings(appContext).readChazarahOffsets(),
            reminderEnabled = reminder.enabled,
            reminderHour = reminder.hour,
            reminderMinute = reminder.minute,
            todaySortOrder = TodayDisplaySettings(appContext).readSortOrder().name,
            automaticPresetUpdates = PresetUpdateSettings(appContext).read().automaticUpdates,
            updatedAt = now.toString(),
        )
    }

    private fun applyPreferences(value: CanonicalPreferences) {
        val languages = LanguageSettings(appContext)
        languages.save(AppLanguage.fromTag(value.appLanguage))
        languages.saveSefarimLanguage(SefarimLanguage.valueOf(value.sefarimLanguage.name))
        languages.savePrimaryCalendar(PrimaryCalendar.valueOf(value.primaryCalendar.name))
        ScheduleDefaultsSettings(appContext).saveChazarahOffsets(value.defaultChazarahOffsets)
        ReminderSettings(appContext).save(ReminderPreference(value.reminderEnabled, value.reminderHour, value.reminderMinute))
        TodayDisplaySettings(appContext).saveSortOrder(
            runCatching { TodaySortOrder.valueOf(value.todaySortOrder) }.getOrDefault(TodaySortOrder.SCHEDULED_FIRST),
        )
        PresetUpdateSettings(appContext).saveAutomatic(value.automaticPresetUpdates)
        PresetCatalogUpdateScheduler.sync(appContext, value.automaticPresetUpdates)
        ReminderScheduler.sync(appContext)
    }

    private fun samePayload(left: String?, right: String?): Boolean {
        return syncPayloadsEquivalent(left, right)
    }

    private fun <T> encode(serializer: kotlinx.serialization.KSerializer<T>, value: T): String =
        CanonicalDataCodec.json.encodeToString(serializer, value)

    private fun <T> decode(serializer: kotlinx.serialization.KSerializer<T>, value: String): T =
        CanonicalDataCodec.json.decodeFromString(serializer, value)

    private fun applyOrder(record: RemoteEntity): Int = when {
        !record.deleted && record.type == SyncEntityType.SCHEDULE -> 0
        !record.deleted -> 1
        record.type == SyncEntityType.SCHEDULE -> 3
        else -> 2
    }

    private suspend fun <T> syncPhase(name: String, block: suspend () -> T): T = try {
        block()
    } catch (error: Exception) {
        throw IllegalStateException("$name failed: ${error.message ?: error.javaClass.simpleName}", error)
    }

    private data class LocalEntity(val type: SyncEntityType, val id: String, val payload: String)
    private data class RemoteEntity(
        val type: SyncEntityType,
        val id: String,
        val payload: String?,
        val deleted: Boolean,
        val revision: Long,
    )

    private companion object {
        val processMutex = Mutex()
        const val CURSOR_KEY = "entity_cursor"
        const val PREFERENCES_ID = "preferences"
        const val PULL_PAGE_SIZE = 1_000
        const val LOCAL_TASK_PAGE_SIZE = 500
        const val OUTBOX_DELETE_BATCH_SIZE = 500
    }
}

internal const val ENTITY_SYNC_PUSH_BATCH_SIZE = 100

internal fun syncPayloadsEquivalent(left: String?, right: String?): Boolean {
    if (left == null || right == null) return left == right
    return syncJsonEquivalent(normalizeSyncPayload(left), normalizeSyncPayload(right))
}

private fun normalizeSyncPayload(value: String): JSONObject = JSONObject(value).apply {
    remove("updatedAt")
    remove("revision")
}

private fun syncJsonEquivalent(left: Any?, right: Any?): Boolean = when {
    left === right -> true
    left == null || right == null -> false
    left is JSONObject && right is JSONObject -> {
        val leftKeys = left.keys().asSequence().toSet()
        val rightKeys = right.keys().asSequence().toSet()
        leftKeys == rightKeys && leftKeys.all { syncJsonEquivalent(left.get(it), right.get(it)) }
    }
    left is JSONArray && right is JSONArray ->
        left.length() == right.length() && (0 until left.length()).all { syncJsonEquivalent(left.get(it), right.get(it)) }
    left is Number && right is Number ->
        left.toString().toBigDecimal().compareTo(right.toString().toBigDecimal()) == 0
    else -> left == right
}

internal fun mutationBatchRequestBody(mutations: List<SyncOutboxEntity>): String = JSONObject()
    .put("p_mutations", JSONArray().apply {
        mutations.forEach { mutation ->
            put(
                JSONObject()
                    .put("mutation_id", mutation.mutationId)
                    .put("entity_type", mutation.entityType)
                    .put("entity_id", mutation.entityId)
                    .put("base_revision", mutation.baseRevision)
                    .put("payload", mutation.payload?.let(::JSONObject) ?: JSONObject.NULL)
                    .put("deleted", mutation.deleted),
            )
        }
    })
    .toString()
