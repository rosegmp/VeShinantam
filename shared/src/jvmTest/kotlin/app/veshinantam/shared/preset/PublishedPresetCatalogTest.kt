package app.veshinantam.shared.preset

import java.io.File
import java.time.LocalDate
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
        assertEquals("2026-10-21", update.positionAsOf)
        assertEquals("Oholot 11:6", mishnah.currentReference.english)
        assertEquals("Yevamot 119b", oraysa.currentReference.english)
        fun assignmentOn(date: LocalDate): String {
            val anchor = LocalDate.parse(update.positionAsOf)
            val learningDays = (1..java.time.temporal.ChronoUnit.DAYS.between(anchor, date).toInt())
                .count { anchor.plusDays(it.toLong()).dayOfWeek.value in 1..4 || anchor.plusDays(it.toLong()).dayOfWeek.value == 7 }
            return oraysa.units[oraysa.currentIndex + learningDays].english
        }
        assertEquals("Yevamot 122b", assignmentOn(LocalDate.of(2026, 10, 29)))
        assertEquals("Kiddushin 2a", assignmentOn(LocalDate.of(2026, 11, 1)))
        assertEquals("Kiddushin 82b", assignmentOn(LocalDate.of(2027, 6, 14)))
        assertEquals("Ketubot 2a", assignmentOn(LocalDate.of(2027, 6, 15)))
        val kiddushinEnd = oraysa.units.indexOfFirst { it.english == "Kiddushin 82b" }
        assertEquals("Ketubot 2a", oraysa.units[kiddushinEnd + 1].english)
    }
}
