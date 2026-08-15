package com.stampbook.app.core

import com.stampbook.app.core.StampLayout.ARCH
import com.stampbook.app.core.StampLayout.BAND
import com.stampbook.app.core.StampLayout.DATE_ARCH
import com.stampbook.app.core.StampLayout.FORM
import com.stampbook.app.core.StampLayout.SPLIT
import com.stampbook.app.core.StampLayout.STACKED
import com.stampbook.app.core.StampShape.CIRCLE
import com.stampbook.app.core.StampShape.HEXAGON
import com.stampbook.app.core.StampShape.OCTAGON
import com.stampbook.app.core.StampShape.OVAL
import com.stampbook.app.core.StampShape.RECTANGLE
import com.stampbook.app.core.StampShape.ROUNDED_RECT
import com.stampbook.app.core.StampShape.SCALLOP
import com.stampbook.app.core.StampShape.SHIELD

/**
 * Stamp design families, drawn loosely from what each part of the world actually
 * prints in passports: Schengen posts use a rectangle carrying a transport
 * pictogram, Gulf and North African posts favour arched ovals, much of Africa
 * uses circles, and the United States a wide oval.
 *
 * Each entry is a list of whole shape-and-layout pairs rather than two separate
 * lists, so a country can never draw an arched heading on a rectangle.
 */
enum class DesignRegion(val looks: List<Pair<StampShape, StampLayout>>) {

    EUROPE(
        listOf(
            RECTANGLE to SPLIT,
            ROUNDED_RECT to SPLIT,
            RECTANGLE to FORM,
            ROUNDED_RECT to FORM,
            RECTANGLE to STACKED,
            HEXAGON to STACKED,
        ),
    ),

    EAST_ASIA(
        listOf(
            ROUNDED_RECT to STACKED,
            RECTANGLE to STACKED,
            ROUNDED_RECT to FORM,
            CIRCLE to ARCH,
            RECTANGLE to BAND,
            CIRCLE to BAND,
        ),
    ),

    LEVANT(
        listOf(
            OVAL to ARCH,
            CIRCLE to ARCH,
            OCTAGON to DATE_ARCH,
            OVAL to DATE_ARCH,
            CIRCLE to BAND,
            OCTAGON to ARCH,
        ),
    ),

    MONSOON(
        listOf(
            CIRCLE to ARCH,
            OVAL to BAND,
            ROUNDED_RECT to STACKED,
            CIRCLE to DATE_ARCH,
            HEXAGON to FORM,
            OVAL to ARCH,
        ),
    ),

    AFRICA(
        listOf(
            CIRCLE to ARCH,
            SCALLOP to ARCH,
            OVAL to ARCH,
            CIRCLE to DATE_ARCH,
            SCALLOP to BAND,
            OCTAGON to ARCH,
        ),
    ),

    AMERICAS(
        listOf(
            OVAL to BAND,
            OVAL to ARCH,
            ROUNDED_RECT to FORM,
            SHIELD to STACKED,
            CIRCLE to ARCH,
            RECTANGLE to FORM,
        ),
    ),

    PACIFIC(
        listOf(
            HEXAGON to STACKED,
            OCTAGON to ARCH,
            OVAL to ARCH,
            SHIELD to FORM,
            ROUNDED_RECT to SPLIT,
            OCTAGON to BAND,
        ),
    ),
}

/**
 * What the stamp renderer needs to know about a country. The ink is its flag's
 * dominant hue muted to something a rubber stamp could leave, and the wording is
 * what a border post there would actually print.
 */
data class CountryTraits(
    val code: String,
    val alpha3: String,
    val inkArgb: Int,
    val entryWord: String,
    /** What the country calls itself, in the same language as the wording. */
    val nativeName: String,
    val region: DesignRegion,
) {
    companion object {
        /** For a code the catalogue does not carry. */
        fun unknown(code: String) = CountryTraits(
            code = code,
            alpha3 = code,
            inkArgb = 0xFF2B2F36.toInt(),
            entryWord = "ENTRY",
            nativeName = code,
            region = DesignRegion.AMERICAS,
        )
    }
}
