package app.veshinantam.data.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MishnahBerurahPageReferenceTest {
    @Test
    fun parsesPageReferencesAndComputesStableOrdinals() {
        assertEquals(0, MishnahBerurahPageReference.parse("Mishnah Berurah, chelek 1 page 4a")?.ordinal)
        assertEquals(1, MishnahBerurahPageReference.parse("Mishnah Berurah, chelek 1 page 4b")?.ordinal)
        assertEquals(2, MishnahBerurahPageReference.parse("Mishnah Berurah, chelek 1 page 5a")?.ordinal)
        assertEquals(0, MishnahBerurahPageReference.parse("Mishnah Berurah, chelek 4 page 196a")?.ordinal)
        assertEquals(3, MishnahBerurahPageReference.parse("Mishnah Berurah chelek 1 page 5b")?.ordinal)
        assertEquals(10, MishnahBerurahPageReference.parse("Mishnah Berurah, chelek 5 page 7a – Mishnah Berurah, chelek 5 page 7b")?.ordinal)
    }

    @Test
    fun rejectsOtherReferencesAndPagesBeforeTheChelekStart() {
        assertNull(MishnahBerurahPageReference.parse("Mishnah Berurah 1:1"))
        assertNull(MishnahBerurahPageReference.parse("Mishnah Berurah, chelek 1 page 3b"))
    }
}
