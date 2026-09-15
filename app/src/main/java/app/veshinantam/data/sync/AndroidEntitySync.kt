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
    private val mutex = Mutex()

    suspend fun synchronize(): EntitySyncResult = mutex.withLock {
        syncPhase("Preparing device changes") { captureLocalChanges() }
        val push = syncPhase("Uploading device changes") { pushOutbox() }
        val pulled = syncPhase("Downloading cloud changes") { pullRemoteChanges() }
        EntitySyncResult(push.first, pulled, push.second)
    }

    suspend fun resetForAccount() = mutex.withLock {
        dao.clearSyncState()
    }

    private suspend fun captureLocalChanges() {
        val now = Instant.now()
        val data = CanonicalAndroidMapper.export(
            schedules = dao.getAllSchedules(),
            exclusions = dao.getAllExclusions(),
            tasks = dao.getAllTasks(),
            goals = dao.getAllProgressGoals(),
            preferences = readPreferences(now),
            now = now,
            syncCursor = dao.getSyncMetadata(CURSOR_KEY) ?: 0,
        )
        val current = buildList {
            data.schedules.forEach { add(LocalEntity(SyncEntityType.SCHEDULE, it.id, encode(CanonicalSchedule.serializer(), it))) }
            data.materialUnits.forEach { add(LocalEntity(SyncEntityType.MATERIAL_UNIT, it.id, encode(app.veshinantam.shared.CanonicalMaterialUnit.serializer(), it))) }
            data.tasks.forEach { add(LocalEntity(SyncEntityType.TASK, it.id, encode(CanonicalTask.serializer(), it))) }
            data.exclusions.forEach { add(LocalEntity(SyncEntityType.EXCLUSION, it.id, encode(CanonicalExclusion.serializer(), it))) }
            data.goals.forEach { add(LocalEntity(SyncEntityType.GOAL, it.id, encode(CanonicalGoal.serializer(), it))) }
            add(LocalEntity(SyncEntityType.PREFERENCES, PREFERENCES_ID, encode(CanonicalPreferences.serializer(), data.preferences)))
        }
        val currentKeys = current.mapTo(mutableSetOf()) { it.type.name to it.id }
        val shadows = dao.getSyncShadows().associateBy { it.entityType to it.entityId }

        current.forEach { entity ->
            val key = entity.type.name to entity.id
            val shadow = shadows[key]
            if (shadow != null && samePayload(shadow.payload, entity.payload) && !shadow.deleted) return@forEach
            enqueue(entity.type, entity.id, entity.payload, deleted = false, shadow?.revision ?: 0)
        }
        shadows.values.filter {
            it.entityType != SyncEntityType.MATERIAL_UNIT.name &&
                !it.deleted && (it.entityType to it.entityId) !in currentKeys
        }.forEach { shadow ->
            enqueue(SyncEntityType.valueOf(shadow.entityType), shadow.entityId, null, deleted = true, shadow.revision)
        }
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
        var cursor = dao.getSyncMetadata(CURSOR_KEY) ?: 0
        var pulled = 0
        do {
            val rows = JSONArray(
                request(
                    "/rest/v1/learning_entities?select=entity_type,entity_id,payload,deleted,revision,updated_at&revision=gt.$cursor&order=revision.asc&limit=$PULL_PAGE_SIZE",
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
                val pending = dao.getSyncOutbox(record.type.name, record.id)
                if (pending == null) applyRemote(record) else {
                    updateShadow(record)
                    dao.updateSyncOutboxBaseRevision(record.type.name, record.id, record.revision)
                }
            }
            records.maxOfOrNull { it.revision }?.let {
                cursor = it
                dao.upsertSyncMetadata(SyncMetadataEntity(CURSOR_KEY, it))
            }
            pulled += records.size
        } while (rows.length() == PULL_PAGE_SIZE)
        return pulled
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

    private fun samePayload(left: String?, right: String?): Boolean = normalize(left) == normalize(right)

    private fun normalize(value: String?): String? = value?.let {
        JSONObject(it).apply {
            remove("updatedAt")
            remove("revision")
        }.toString()
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
        const val CURSOR_KEY = "entity_cursor"
        const val PREFERENCES_ID = "preferences"
        const val PULL_PAGE_SIZE = 1_000
    }
}

internal const val ENTITY_SYNC_PUSH_BATCH_SIZE = 100

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
