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

    private val GREEK_OR_CYRILLIC = listOf(
        0x0370..0x03FF, // Greek
        0x0400..0x04FF, // Cyrillic
        0x0500..0x052F, // Cyrillic Supplement
        0x1F00..0x1FFF, // Greek Extended
    )

    /**
     * Which of the bundled faces can set this text. Latin covers the Vietnamese
     * and Turkish accents too; anything past Greek and Cyrillic — CJK, Arabic,
     * Thai, Devanagari — is left to the device, whose own serif lands on the
     * formal register of each of those scripts.
     */
    fun scriptOf(text: String): TextScript {
        var script = TextScript.LATIN
        text.forEach { char ->
            val code = char.code
            when {
                code < 0x0370 || code in 0x1E00..0x1EFF || code in 0x2000..0x206F -> Unit
                GREEK_OR_CYRILLIC.any { code in it } ->
                    if (script == TextScript.LATIN) script = TextScript.GREEK_OR_CYRILLIC
                else -> return TextScript.OTHER
            }
        }
        return script
    }
}

enum class TextScript { LATIN, GREEK_OR_CYRILLIC, OTHER }
