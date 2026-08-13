package com.stampbook.app.data.world

/**
 * One country's outline in unit space: x runs 0..1 across the map, y runs 0..1
 * from the north pole down. Rings are flat [x0, y0, x1, y1, ...] arrays because
 * this is walked on every frame.
 */
class CountryShape(
    val code: String,
    val rings: List<FloatArray>,
    val minX: Float,
    val maxX: Float,
) {
    /** True for Russia, Fiji and Antarctica, whose outlines run past the map edge. */
    val wraps: Boolean get() = minX < 0f || maxX > 1f
}

/**
 * Decoder for world.sbw, the packed Natural Earth 1:50m boundaries built by
 * tools/build_world_asset.py. Deliberately free of Android types so the format
 * can be tested on the JVM against the very asset the app ships.
 */
object WorldAsset {

    const val MAGIC = "SBW1"
    private const val X_RANGE = 36000f
    private const val Y_RANGE = 18000f

    fun parse(bytes: ByteArray): List<CountryShape> {
        val reader = Reader(bytes)
        require(reader.ascii(4) == MAGIC) { "not a Stampbook world asset" }

        return List(reader.u16()) {
            val code = reader.ascii(2)
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            val rings = List(reader.u16()) {
                val count = reader.u16()
                val ring = FloatArray(count * 2)
                var x = reader.zigzag()
                var y = reader.zigzag()
                for (i in 0 until count) {
                    if (i > 0) {
                        x += reader.zigzag()
                        y += reader.zigzag()
                    }
                    val fx = x / X_RANGE
                    ring[i * 2] = fx
                    ring[i * 2 + 1] = y / Y_RANGE
                    if (fx < minX) minX = fx
                    if (fx > maxX) maxX = fx
                }
                ring
            }
            CountryShape(code, rings, minX, maxX)
        }
    }

    private class Reader(private val bytes: ByteArray) {
        private var at = 0

        fun ascii(length: Int): String =
            String(bytes, at, length, Charsets.US_ASCII).also { at += length }

        fun u16(): Int = (bytes[at].toInt() and 0xFF or ((bytes[at + 1].toInt() and 0xFF) shl 8))
            .also { at += 2 }

        private fun varint(): Int {
            var shift = 0
            var value = 0
            while (true) {
                val byte = bytes[at++].toInt() and 0xFF
                value = value or ((byte and 0x7F) shl shift)
                if (byte and 0x80 == 0) return value
                shift += 7
            }
        }

        fun zigzag(): Int = varint().let { (it ushr 1) xor -(it and 1) }
    }
}
