package app.veshinantam.data.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import app.veshinantam.BuildConfig
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AppRelease(
    val versionName: String,
    val versionCode: Long,
    val apkName: String,
    val apkUrl: String,
    val checksumUrl: String,
    val githubDigest: String?,
)

sealed interface AppUpdateCheck {
    data object UpToDate : AppUpdateCheck
    data class Available(val release: AppRelease) : AppUpdateCheck
}

internal fun parseLatestAppRelease(json: String): AppRelease {
    val release = Json.parseToJsonElement(json).jsonObject
    val tag = release.getValue("tag_name").jsonPrimitive.content
    val assets = release.getValue("assets").jsonArray.map { it.jsonObject }
    val apk = assets.firstOrNull { asset ->
        asset["name"]?.jsonPrimitive?.content?.matches(APK_NAME_PATTERN) == true
    } ?: throw IOException("The latest release has no APK")
    val apkName = apk.getValue("name").jsonPrimitive.content
    val match = requireNotNull(APK_NAME_PATTERN.matchEntire(apkName))
    val versionName = match.groupValues[1]
    val versionCode = match.groupValues[2].toLong()
    require(tag == "v$versionName") { "Release tag and APK version differ" }
    val checksumName = "$apkName.sha256"
    val checksum = assets.firstOrNull { it["name"]?.jsonPrimitive?.content == checksumName }
        ?: throw IOException("The latest release has no checksum")
    val digest = apk["digest"]?.jsonPrimitive?.content?.takeIf { it.startsWith("sha256:") }
        ?.removePrefix("sha256:")
    return AppRelease(
        versionName = versionName,
        versionCode = versionCode,
        apkName = apkName,
        apkUrl = checkedAssetUrl(apk.getValue("browser_download_url").jsonPrimitive.content, apkName),
        checksumUrl = checkedAssetUrl(checksum.getValue("browser_download_url").jsonPrimitive.content, checksumName),
        githubDigest = digest,
    )
}

internal fun parseApkChecksum(text: String, apkName: String): String {
    val parts = text.trim().split(Regex("\\s+"), limit = 2)
    require(parts.size == 2 && parts[0].matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid checksum" }
    require(parts[1].removePrefix("*") == apkName) { "Checksum names a different APK" }
    return parts[0].uppercase()
}

private val APK_NAME_PATTERN = Regex("VeShinantam-([0-9]+(?:\\.[0-9]+)+)-([0-9]+)-release\\.apk")
private const val RELEASE_API = "https://api.github.com/repos/rosegmp/VeShinantam/releases/latest"
private const val RELEASE_DOWNLOAD_PREFIX = "https://github.com/rosegmp/VeShinantam/releases/download/"
private const val MAX_APK_BYTES = 100L * 1024 * 1024

private fun checkedAssetUrl(raw: String, assetName: String): String {
    val uri = URI(raw)
    require(raw.startsWith(RELEASE_DOWNLOAD_PREFIX) && uri.path.substringAfterLast('/') == assetName && uri.query == null) {
        "Unexpected release asset URL"
    }
    return raw
}

class AppUpdateClient(private val context: Context) {
    suspend fun checkForUpdate(): AppUpdateCheck = withContext(Dispatchers.IO) {
        val release = parseLatestAppRelease(readBytes(RELEASE_API, 1024 * 1024).decodeToString())
        if (release.versionCode > BuildConfig.VERSION_CODE) AppUpdateCheck.Available(release)
        else AppUpdateCheck.UpToDate
    }

    suspend fun downloadVerified(release: AppRelease): File = withContext(Dispatchers.IO) {
        require(release.versionCode > BuildConfig.VERSION_CODE) { "The release is not newer" }
        val expected = parseApkChecksum(
            readBytes(release.checksumUrl, 4096).decodeToString(), release.apkName,
        )
        release.githubDigest?.let { require(it.equals(expected, ignoreCase = true)) { "Release checksum differs" } }
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(directory, release.apkName)
        val temporary = File(directory, "download.apk")
        temporary.delete()
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            openConnection(release.apkUrl).useConnection { connection ->
                if (connection.contentLengthLong > MAX_APK_BYTES) throw IOException("APK is too large")
                connection.inputStream.use { input ->
                    temporary.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var count = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            count += read
                            if (count > MAX_APK_BYTES) throw IOException("APK is too large")
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                        }
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02X".format(it.toInt() and 0xFF) }
            require(actual == expected) { "APK checksum mismatch" }
            verifyArchive(temporary, release.versionCode)
            if (target.exists() && !target.delete()) throw IOException("Could not replace old APK")
            if (!temporary.renameTo(target)) throw IOException("Could not save APK")
            target
        } catch (error: Exception) {
            temporary.delete()
            throw error
        }
    }

    fun canInstallPackages(): Boolean = Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission() {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun openInstaller(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updatefileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                clipData = ClipData.newUri(context.contentResolver, "VeShinantam update", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private fun verifyArchive(file: File, expectedVersionCode: Long) {
        @Suppress("DEPRECATION")
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        @Suppress("DEPRECATION")
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IOException("Invalid APK")
        @Suppress("DEPRECATION")
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        require(archive.packageName == context.packageName) { "APK package differs" }
        require(versionCode(archive) == expectedVersionCode && expectedVersionCode > versionCode(installed)) {
            "APK version differs"
        }
        require(signers(archive) == signers(installed) && signers(archive).isNotEmpty()) {
            "APK signer differs"
        }
    }

    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else { @Suppress("DEPRECATION") info.versionCode.toLong() }

    private fun signers(info: PackageInfo): Set<String> {
        @Suppress("DEPRECATION")
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return signatures.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") {
                "%02X".format(it.toInt() and 0xFF)
            }
        }.toSet()
    }

    private fun readBytes(url: String, maxBytes: Int): ByteArray = openConnection(url).useConnection { connection ->
        if (connection.contentLengthLong > maxBytes) throw IOException("Response is too large")
        connection.inputStream.use { input ->
            val result = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (result.size() + count > maxBytes) throw IOException("Response is too large")
                result.write(buffer, 0, count)
            }
            result.toByteArray()
        }
    }

    private fun openConnection(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "VeShinantam/${BuildConfig.VERSION_NAME}")
        if (responseCode != HttpURLConnection.HTTP_OK) {
            disconnect()
            throw IOException("HTTP $responseCode")
        }
    }

    private inline fun <T> HttpURLConnection.useConnection(block: (HttpURLConnection) -> T): T =
        try { block(this) } finally { disconnect() }
}
