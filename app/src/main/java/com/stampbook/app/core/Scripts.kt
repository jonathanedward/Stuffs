package com.stampbook.app.core

/**
 * The stamp renderer bends a heading around an arc by placing one character at a
 * time, rotating each to the tangent. That is fine for scripts whose characters
 * stand alone, and wrong for scripts where glyphs join up, reorder, or stack
 * marks: drawn character by character, Arabic loses its joins and its direction.
 */
object Scripts {

    private val SHAPED = listOf(
        0x0590..0x05FF, // Hebrew
        0x0600..0x06FF, // Arabic
        0x0700..0x074F, // Syriac
        0x0750..0x077F, // Arabic Supplement
        0x0900..0x097F, // Devanagari
        0x0980..0x09FF, // Bengali
        0x0A00..0x0A7F, // Gurmukhi
        0x0A80..0x0AFF, // Gujarati
        0x0B00..0x0B7F, // Oriya
        0x0B80..0x0BFF, // Tamil
        0x0C00..0x0C7F, // Telugu
        0x0C80..0x0CFF, // Kannada
        0x0D00..0x0D7F, // Malayalam
        0x0D80..0x0DFF, // Sinhala
        0x0E00..0x0E7F, // Thai
        0x0E80..0x0EFF, // Lao
        0x0F00..0x0FFF, // Tibetan
        0x1000..0x109F, // Myanmar
        0x1780..0x17FF, // Khmer
        0xFB1D..0xFDFF, // Hebrew and Arabic presentation forms
        0xFE70..0xFEFF, // Arabic presentation forms B
    )

    /** True when this text must be laid out as a run rather than character by character. */
    fun needsShaping(text: String): Boolean =
        text.any { char -> SHAPED.any { char.code in it } }
}
