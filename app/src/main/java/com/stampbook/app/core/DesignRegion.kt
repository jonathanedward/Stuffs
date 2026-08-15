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
/**
 * The lettering a stamp is set in. Only three faces ship, because a font per
 * country is not a thing that can exist — Noto's CJK families alone are larger
 * than this whole app. What does hold is the register: which of these traditions
 * a border post's lettering belongs to.
 */
enum class StampFace {
    /** An old-style serif, the lettering of older and colonial-era border posts. */
    DOCUMENTARY,

    /** A grotesque. What a Schengen stamp is actually set in. */
    INSTITUTIONAL,

    /** A slab serif: the office-registry voice, and what US entry stamps favour. */
    REGISTRY;

    /** Only two of the three bundled faces carry Greek and Cyrillic. */
    val coversEuropeanScripts: Boolean get() = this != INSTITUTIONAL
}

enum class DesignRegion(
    val looks: List<Pair<StampShape, StampLayout>>,
    val faces: List<StampFace>,
) {

    EUROPE(
        listOf(
            RECTANGLE to SPLIT,
            ROUNDED_RECT to SPLIT,
            RECTANGLE to FORM,
            ROUNDED_RECT to FORM,
            RECTANGLE to STACKED,
            HEXAGON to STACKED,
        ),
        // Schengen posts print a grotesque; older European posts, a slab.
        faces = listOf(StampFace.INSTITUTIONAL, StampFace.INSTITUTIONAL, StampFace.REGISTRY),
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
        // The Latin lines only: 日本 and 한국 are set by the device, whose serif
        // is Mincho and Myeongjo — the formal register in both.
        faces = listOf(StampFace.DOCUMENTARY, StampFace.REGISTRY),
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
        // Naskh, the documentary Arabic hand, sits with an old-style serif.
        faces = listOf(StampFace.DOCUMENTARY, StampFace.DOCUMENTARY, StampFace.REGISTRY),
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
        faces = listOf(StampFace.DOCUMENTARY, StampFace.REGISTRY),
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
        // The serif officialdom left behind by British and French border posts.
        faces = listOf(StampFace.DOCUMENTARY, StampFace.DOCUMENTARY, StampFace.REGISTRY),
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
        faces = listOf(StampFace.REGISTRY, StampFace.DOCUMENTARY),
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
        faces = listOf(StampFace.INSTITUTIONAL, StampFace.REGISTRY),
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
