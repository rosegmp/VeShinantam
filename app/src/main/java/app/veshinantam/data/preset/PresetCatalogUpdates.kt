package app.veshinantam.data.preset

import android.content.Context
import android.system.Os
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.veshinantam.BuildConfig
import app.veshinantam.data.readAtMost
import app.veshinantam.domain.material.PresetCatalog
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class VerifiedPresetCatalog(
    val version: String,
    val sequence: Long,
    val positionAsOf: LocalDate,
    val currentReferences: Map<String, String>,
)

enum class PresetUpdateResult { UPDATED, UP_TO_DATE, NOT_CONFIGURED, NETWORK_ERROR, INVALID_SIGNATURE, INVALID_CATALOG }

data class PresetUpdateState(
    val automaticUpdates: Boolean,
    val lastCheckedAt: Instant?,
    val lastResult: PresetUpdateResult?,
)

class PresetUpdateSettings(context: Context) {
    private val preferences = context.getSharedPreferences("preset_updates", Context.MODE_PRIVATE)

    fun read(): PresetUpdateState = PresetUpdateState(
        automaticUpdates = preferences.getBoolean("automatic", false),
        lastCheckedAt = preferences.getLong("last_checked", 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli),
        lastResult = preferences.getString("last_result", null)?.let { runCatching { PresetUpdateResult.valueOf(it) }.getOrNull() },
    )

    fun saveAutomatic(enabled: Boolean) {
        preferences.edit().putBoolean("automatic", enabled).apply()
    }

    fun saveResult(result: PresetUpdateResult, checkedAt: Instant) {
        preferences.edit().putString("last_result", result.name).putLong("last_checked", checkedAt.toEpochMilli()).apply()
    }
}

object PresetCatalogEnvelope {
    const val KEY_ID = "veshinantam-preset-v1"
    private const val FORMAT = "app.veshinantam.preset-catalog"
    private const val SCHEMA_VERSION = 1

    fun verifyAndDecode(envelopeText: String, publicKeyBase64: String): VerifiedPresetCatalog {
        val envelope = JSONObject(envelopeText)
        require(envelope.getString("format") == FORMAT) { "Unexpected catalog format" }
        require(envelope.getString("keyId") == KEY_ID) { "Unknown catalog signing key" }
        val payloadBytes = Base64.getDecoder().decode(envelope.getString("payload"))
        val signatureBytes = Base64.getDecoder().decode(envelope.getString("signature"))
        require(payloadBytes.size <= PresetCatalogUpdateClient.MAX_DOWNLOAD_BYTES) { "Catalog payload is too large" }
        val publicKey = KeyFactory.getInstance("EC").generatePublic(
            X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)),
        )
        val valid = Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey)
            update(payloadBytes)
            verify(signatureBytes)
        }
        require(valid) { "Catalog signature is invalid" }
        val payload = JSONObject(payloadBytes.toString(StandardCharsets.UTF_8))
        require(payload.getInt("schemaVersion") == SCHEMA_VERSION) { "Unsupported catalog schema" }
        val positionsObject = payload.getJSONObject("positions")
        val positions = positionsObject.keys().asSequence().associateWith { positionsObject.getString(it) }
        return VerifiedPresetCatalog(
            version = payload.getString("catalogVersion").also { require(it.isNotBlank()) },
            sequence = payload.getLong("sequence").also { require(it > 0) },
            positionAsOf = LocalDate.parse(payload.getString("positionAsOf")),
            currentReferences = positions,
        )
    }
}

class PresetCatalogUpdateClient(
    private val context: Context,
    private val clock: Clock = Clock.systemUTC(),
    private val endpoint: String = BuildConfig.PRESET_CATALOG_UPDATE_URL,
    private val publicKeyBase64: String = BuildConfig.PRESET_CATALOG_PUBLIC_KEY,
) {
    fun checkForUpdate(): PresetUpdateResult {
        if (endpoint.isBlank()) return save(PresetUpdateResult.NOT_CONFIGURED)
        val url = runCatching { URL(endpoint) }.getOrElse { return save(PresetUpdateResult.NETWORK_ERROR) }
        if (url.protocol != "https") return save(PresetUpdateResult.NETWORK_ERROR)
        val envelope = try {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return save(PresetUpdateResult.NETWORK_ERROR)
                val declaredLength = connection.contentLengthLong
                if (declaredLength > MAX_DOWNLOAD_BYTES) return save(PresetUpdateResult.INVALID_CATALOG)
                connection.inputStream.use { input ->
                    val bytes = input.readAtMost(MAX_DOWNLOAD_BYTES + 1)
                    if (bytes.size > MAX_DOWNLOAD_BYTES) return save(PresetUpdateResult.INVALID_CATALOG)
                    bytes.toString(StandardCharsets.UTF_8)
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            return save(PresetUpdateResult.NETWORK_ERROR)
        }
        val catalog = try {
            PresetCatalogEnvelope.verifyAndDecode(envelope, publicKeyBase64)
        } catch (error: IllegalArgumentException) {
            return save(if (error.message?.contains("signature", ignoreCase = true) == true) PresetUpdateResult.INVALID_SIGNATURE else PresetUpdateResult.INVALID_CATALOG)
        } catch (_: Exception) {
            return save(PresetUpdateResult.INVALID_CATALOG)
        }
        if (catalog.sequence <= PresetCatalog.activeSequence) return save(PresetUpdateResult.UP_TO_DATE)
        if (catalog.positionAsOf.isAfter(LocalDate.now(clock))) return save(PresetUpdateResult.INVALID_CATALOG)
        if (runCatching {
                PresetCatalog.validateVerifiedUpdate(catalog.version, catalog.sequence, catalog.positionAsOf, catalog.currentReferences)
            }.isFailure
        ) return save(PresetUpdateResult.INVALID_CATALOG)
        return try {
            PresetCatalogUpdateStore(context, publicKeyBase64).saveVerified(envelope)
            PresetCatalog.applyVerifiedUpdate(catalog.version, catalog.sequence, catalog.positionAsOf, catalog.currentReferences)
            save(PresetUpdateResult.UPDATED)
        } catch (_: Exception) {
            save(PresetUpdateResult.INVALID_CATALOG)
        }
    }

    private fun save(result: PresetUpdateResult): PresetUpdateResult {
        PresetUpdateSettings(context).saveResult(result, clock.instant())
        return result
    }

    companion object {
        const val MAX_DOWNLOAD_BYTES = 1024 * 1024
    }
}

class PresetCatalogUpdateStore(
    private val context: Context,
    private val publicKeyBase64: String = BuildConfig.PRESET_CATALOG_PUBLIC_KEY,
) {
    private val file get() = File(context.filesDir, FILE_NAME)

    fun loadCached(): Boolean {
        val source = file.takeIf { it.isFile && it.length() <= PresetCatalogUpdateClient.MAX_DOWNLOAD_BYTES } ?: return false
        return try {
            val catalog = PresetCatalogEnvelope.verifyAndDecode(source.readText(), publicKeyBase64)
            if (catalog.sequence <= PresetCatalog.activeSequence) return false
            PresetCatalog.validateVerifiedUpdate(catalog.version, catalog.sequence, catalog.positionAsOf, catalog.currentReferences)
            PresetCatalog.applyVerifiedUpdate(catalog.version, catalog.sequence, catalog.positionAsOf, catalog.currentReferences)
            true
        } catch (_: Exception) {
            source.delete()
            false
        }
    }

    fun saveVerified(envelopeText: String) {
        val temp = File(context.filesDir, "$FILE_NAME.tmp")
        FileOutputStream(temp).use { output ->
            output.write(envelopeText.toByteArray(StandardCharsets.UTF_8))
            output.fd.sync()
        }
        try {
            Os.rename(temp.absolutePath, file.absolutePath)
        } catch (error: Exception) {
            temp.delete()
            throw error
        }
    }

    private companion object { const val FILE_NAME = "preset-catalog-update.json" }
}

class PresetCatalogUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        when (PresetCatalogUpdateClient(applicationContext).checkForUpdate()) {
            PresetUpdateResult.NETWORK_ERROR -> Result.retry()
            else -> Result.success()
        }
    }
}

object PresetCatalogUpdateScheduler {
    private const val WORK_NAME = "preset_catalog_update"

    fun sync(context: Context, enabled: Boolean = PresetUpdateSettings(context).read().automaticUpdates) {
        val manager = WorkManager.getInstance(context)
        if (!enabled || BuildConfig.PRESET_CATALOG_UPDATE_URL.isBlank()) {
            manager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<PresetCatalogUpdateWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
