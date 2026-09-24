package app.veshinantam.shared.preset

import java.io.File
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PublishedPresetCatalogTest {
    @Test
    fun signedCatalogPayloadKeepsBundledPresetIdsAndCanonicalPositions() {
        val path = generateSequence(File(".").canonicalFile) { it.parentFile }
            .map { it.resolve("catalog/preset-catalog.payload.json") }
            .first(File::isFile)
        val update = Json.decodeFromString<RemotePresetCatalog>(path.readText())
        val programs = RemotePresetCatalogValidator.programs(update)

        val mishnah = programs.single { it.id == "mishnah-yomis" }
        val oraysa = programs.single { it.id == "oraysa" }
        assertEquals(4192, mishnah.units.size)
        assertEquals(5365, oraysa.units.size)
        assertEquals("Berakhot 2a", oraysa.units.first().english)
        assertEquals("Oholot 5:1", mishnah.currentReference.english)
        assertEquals("Yevamot 110a", oraysa.currentReference.english)
    }
}
