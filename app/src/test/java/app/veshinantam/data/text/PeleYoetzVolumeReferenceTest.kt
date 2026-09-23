package app.veshinantam.data.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeleYoetzVolumeReferenceTest {
    @Test
    fun parsesBothScheduledVolumes() {
        assertEquals(
            1,
            PeleYoetzVolumeReference.parse(
                "Pele Yoetz, Volume 1, Day 1, Ot Alef - אהבה להקדוש ברוך הוא",
            )?.volume,
        )
        assertEquals(
            2,
            PeleYoetzVolumeReference.parse(
                "Pele Yoetz, Volume 2, Day 357, Ot Tet - וכן יש לזהר מלגע בנבלה",
            )?.volume,
        )
        assertEquals(
            357,
            PeleYoetzVolumeReference.parse(
                "Pele Yoetz, Volume 2, Day 357, Ot Tet - וכן יש לזהר מלגע בנבלה",
            )?.day,
        )
        assertEquals(
            357,
            PeleYoetzVolumeReference.parse(
                "Pele Yoetz, Volume 2, Day 103, Ot Tet - legacy label",
            )?.day,
        )
    }

    @Test
    fun rejectsLegacyAndUnrelatedLabels() {
        assertNull(PeleYoetzVolumeReference.parse("Pele Yoetz, Day 103"))
        assertNull(PeleYoetzVolumeReference.parse("Kitzur Shulchan Aruch 1:1-4"))
    }
}
