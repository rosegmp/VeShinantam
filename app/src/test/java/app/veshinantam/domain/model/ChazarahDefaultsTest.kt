package app.veshinantam.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChazarahDefaultsTest {
    @Test
    fun `built in chazarah gaps use the corrected pattern`() {
        assertEquals(listOf(1, 7, 30, 90), ChazarahPattern().dayOffsets)
    }

    @Test
    fun `custom default parser accepts unique positive comma separated gaps`() {
        assertEquals(listOf(2, 8, 40), ChazarahDefaults.parse("2, 8, 40"))
    }

    @Test
    fun `custom default parser rejects blanks duplicates and invalid gaps`() {
        assertNull(ChazarahDefaults.parse(""))
        assertNull(ChazarahDefaults.parse("1, 1"))
        assertNull(ChazarahDefaults.parse("1, 0, 30"))
        assertNull(ChazarahDefaults.parse("1, seven, 30"))
    }
}
