package app.veshinantam.data

import java.io.InputStream

internal fun InputStream.readAtMost(maxBytes: Int): ByteArray {
    require(maxBytes >= 0)
    val buffer = ByteArray(maxBytes)
    var offset = 0
    while (offset < maxBytes) {
        val read = read(buffer, offset, maxBytes - offset)
        if (read <= 0) break
        offset += read
    }
    return if (offset == buffer.size) buffer else buffer.copyOf(offset)
}
