package app.veshinantam.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppUpdateClientTest {
    @Test
    fun `latest release selects matching APK and checksum`() {
        val release = parseLatestAppRelease(releaseJson("v1.0.8"))

        assertEquals("1.0.8", release.versionName)
        assertEquals(25L, release.versionCode)
        assertEquals("VeShinantam-1.0.8-25-release.apk", release.apkName)
        assertEquals(
            "https://github.com/rosegmp/VeShinantam/releases/download/v1.0.8/VeShinantam-1.0.8-25-release.apk",
            release.apkUrl,
        )
        assertEquals("a".repeat(64), release.githubDigest)
    }

    @Test
    fun `rejects release metadata that names a different APK version`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseLatestAppRelease(releaseJson("v1.0.9"))
        }
    }

    @Test
    fun `checksum must name the downloaded APK`() {
        val checksum = "A".repeat(64)
        assertEquals(checksum, parseApkChecksum("${checksum.lowercase()} *VeShinantam-1.0.8-25-release.apk\n", "VeShinantam-1.0.8-25-release.apk"))
        assertThrows(IllegalArgumentException::class.java) {
            parseApkChecksum("$checksum *other.apk", "VeShinantam-1.0.8-25-release.apk")
        }
    }

    private fun releaseJson(tag: String) = """
        {
          "tag_name": "$tag",
          "assets": [
            {
              "name": "VeShinantam-1.0.8-25-release.apk",
              "browser_download_url": "https://github.com/rosegmp/VeShinantam/releases/download/v1.0.8/VeShinantam-1.0.8-25-release.apk",
              "digest": "sha256:${"a".repeat(64)}"
            },
            {
              "name": "VeShinantam-1.0.8-25-release.apk.sha256",
              "browser_download_url": "https://github.com/rosegmp/VeShinantam/releases/download/v1.0.8/VeShinantam-1.0.8-25-release.apk.sha256"
            }
          ]
        }
    """.trimIndent()
}
