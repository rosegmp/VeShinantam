package app.veshinantam.data.preset

import app.veshinantam.domain.material.PresetCatalog
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.Signature
import java.time.LocalDate
import java.util.Base64
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PresetCatalogEnvelopeTest {
    @After
    fun resetCatalog() {
        PresetCatalog.clearVerifiedUpdate()
    }

    @Test
    fun `signed catalog verifies and applies known positions`() {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val positions = PresetCatalog.programs.associate { program ->
            program.id to program.units[(program.currentIndex + 1).coerceAtMost(program.units.lastIndex)].english
        }
        val envelope = signedEnvelope(keyPair, 8, "2026.09.10-8", LocalDate.parse("2026-09-09"), positions)

        val verified = PresetCatalogEnvelope.verifyAndDecode(
            envelope,
            Base64.getEncoder().encodeToString(keyPair.public.encoded),
        )
        PresetCatalog.applyVerifiedUpdate(verified.version, verified.sequence, verified.positionAsOf, verified.currentReferences)

        assertEquals(8, PresetCatalog.activeSequence)
        assertEquals("2026.09.10-8", PresetCatalog.VERSION)
        assertEquals(LocalDate.parse("2026-09-09"), PresetCatalog.positionAsOf)
        assertEquals(positions, PresetCatalog.programs.associate { it.id to it.currentReference.english })
    }

    @Test
    fun `tampered payload is rejected before parsing`() {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val positions = PresetCatalog.programs.associate { it.id to it.currentReference.english }
        val valid = JSONObject(signedEnvelope(keyPair, 8, "version-8", LocalDate.parse("2026-09-09"), positions))
        val payload = String(Base64.getDecoder().decode(valid.getString("payload")), StandardCharsets.UTF_8)
            .replace("version-8", "version-9")
        valid.put("payload", Base64.getEncoder().encodeToString(payload.toByteArray(StandardCharsets.UTF_8)))

        assertThrows(IllegalArgumentException::class.java) {
            PresetCatalogEnvelope.verifyAndDecode(valid.toString(), Base64.getEncoder().encodeToString(keyPair.public.encoded))
        }
        assertEquals(PresetCatalog.BUNDLED_VERSION, PresetCatalog.VERSION)
    }

    @Test
    fun `catalog missing a bundled program is rejected`() {
        val positions = PresetCatalog.programs.dropLast(1).associate { it.id to it.currentReference.english }

        assertThrows(IllegalArgumentException::class.java) {
            PresetCatalog.validateVerifiedUpdate("version-8", 8, LocalDate.parse("2026-09-09"), positions)
        }
    }

    @Test
    fun `catalog sequence cannot roll back active data`() {
        val positions = PresetCatalog.programs.associate { it.id to it.currentReference.english }
        PresetCatalog.applyVerifiedUpdate("version-8", 8, LocalDate.parse("2026-09-09"), positions)

        assertThrows(IllegalArgumentException::class.java) {
            PresetCatalog.applyVerifiedUpdate("older", 7, LocalDate.parse("2026-09-08"), positions)
        }
        assertEquals(8, PresetCatalog.activeSequence)
    }

    private fun signedEnvelope(
        keyPair: java.security.KeyPair,
        sequence: Long,
        version: String,
        asOf: LocalDate,
        positions: Map<String, String>,
    ): String {
        val payload = JSONObject().apply {
            put("schemaVersion", 1)
            put("catalogVersion", version)
            put("sequence", sequence)
            put("positionAsOf", asOf.toString())
            put("positions", JSONObject(positions))
        }.toString().toByteArray(StandardCharsets.UTF_8)
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(payload)
            sign()
        }
        return JSONObject().apply {
            put("format", "app.veshinantam.preset-catalog")
            put("keyId", PresetCatalogEnvelope.KEY_ID)
            put("payload", Base64.getEncoder().encodeToString(payload))
            put("signature", Base64.getEncoder().encodeToString(signature))
        }.toString()
    }
}
