package app.veshinantam.data

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class LimitedInputStreamTest {
    @Test
    fun returnsTheWholeStreamWhenItFits() {
        val source = byteArrayOf(1, 2, 3)

        assertArrayEquals(source, ByteArrayInputStream(source).readAtMost(4))
    }

    @Test
    fun stopsAtTheRequestedLimit() {
        val source = byteArrayOf(1, 2, 3, 4)

        assertArrayEquals(byteArrayOf(1, 2, 3), ByteArrayInputStream(source).readAtMost(3))
    }
}
