package com.stampbook.app.core

import kotlin.random.Random

enum class StampShape {
    CIRCLE, OVAL, SCALLOP, OCTAGON, RECTANGLE, ROUNDED_RECT, HEXAGON, SHIELD;

    /** Round enough to carry text bent around its edge. */
    val isRound: Boolean
        get() = this == CIRCLE || this == OVAL || this == SCALLOP || this == OCTAGON
}

enum class BorderStyle {
    SINGLE, DOUBLE, DASHED, BEADED, HAIRLINE_PAIR;

    /**
     * How far in the innermost line of this border sits, as a fraction of the
     * radius. Text has to clear that line, not just the outer edge.
     */
    val contentInset: Float
        get() = when (this) {
            SINGLE, DASHED -> 1f
            DOUBLE, BEADED -> 0.86f
            HAIRLINE_PAIR -> 0.91f
        }
}

enum class StampLayout {
    /** Country bent over the top, place across the middle. */
    ARCH,

    /** Date over the top, country bent along the bottom instead. */
    DATE_ARCH,

    /** Everything on straight lines, separated by a rule. */
    STACKED,

    /** Place knocked out of a solid band of ink. */
    BAND,

    /** A travel device on the left, the details on the right. */
    SPLIT,

    /** Ruled like a form, the way a printed visa is. */
    FORM,
}

/** The little travel glyph an authority puts on its stamp. */
enum class StampDevice { NONE, PLANE, SHIP, TRAIN, CAR, GLOBE }

/**
 * How one country's stamp looks. Derived from the country, so every stamp from
 * Japan shares a design and none of them look like France's.
 */
data class StampDesign(
    val shape: StampShape,
    val border: BorderStyle,
    val layout: StampLayout,
    val inkArgb: Int,
    val device: StampDevice,
    /** What a border post in this country prints, in its own language. */
    val label: String,
    /** ISO 3166-1 alpha-3, which is what a real stamp carries. */
    val alpha3: String,
    val stars: Int,
    val cornerTicks: Boolean,
)

/**
 * How one pressing of that stamp came out: the angle it was banged down at, how
 * much ink was on the pad, where the ink failed, and the serial it was given.
 */
data class StampImpression(
    val rotationDegrees: Float,
    val alpha: Float,
    val wearSeed: Int,
    val serial: String,
)

object StampStyles {

    /**
     * The design an authority uses. Ink, wording and the family of shapes come
     * from the country itself; the code decides only which look inside that
     * family, plus the border, device and small ornaments.
     */
    fun forCountry(traits: CountryTraits): StampDesign {
        val pick = Pick(fnv1a(traits.code.uppercase()))
        val (shape, layout) = traits.region.looks[pick.next(traits.region.looks.size)]
        return StampDesign(
            shape = shape,
            border = BorderStyle.entries[pick.next(BorderStyle.entries.size)],
            layout = layout,
            inkArgb = traits.inkArgb,
            // Only the layouts that keep a clear slot for it carry a device;
            // the split panel is built around one, so it always gets one.
            device = when (layout) {
                StampLayout.SPLIT -> StampDevice.entries[1 + pick.next(StampDevice.entries.size - 1)]
                StampLayout.ARCH, StampLayout.DATE_ARCH ->
                    StampDevice.entries[pick.next(StampDevice.entries.size)]
                else -> StampDevice.NONE
            },
            label = traits.entryWord,
            alpha3 = traits.alpha3,
            stars = pick.next(4),
            // A shield has no bottom corners to tick, and the marks would land on
            // its taper.
            cornerTicks = !shape.isRound && shape != StampShape.SHIELD && pick.next(2) == 0,
        )
    }

    fun forSeed(seed: Int): StampImpression {
        val random = Random(seed)
        return StampImpression(
            // Hand-stamped, not machine-placed: a few degrees off true.
            rotationDegrees = random.nextDouble(-11.0, 11.0).toFloat(),
            // Uneven ink coverage.
            alpha = random.nextDouble(0.72, 0.95).toFloat(),
            wearSeed = random.nextInt(),
            serial = random.nextInt(1000, 99999).toString().padStart(5, '0'),
        )
    }

    /** A fresh seed for a newly created stamp. */
    fun newSeed(): Int = Random.nextInt()

    /** FNV-1a, so a country's design never shifts with a platform's hashCode. */
    private fun fnv1a(text: String): Int {
        var hash = -0x7EE3623B // 2166136261
        text.forEach { char ->
            hash = hash xor char.code
            hash *= 16777619
        }
        return hash
    }

    /** xorshift32, for pulling several independent choices out of one hash. */
    private class Pick(seed: Int) {
        private var state = if (seed == 0) 0x6D2B79F5 else seed

        fun next(bound: Int): Int {
            state = state xor (state shl 13)
            state = state xor (state ushr 17)
            state = state xor (state shl 5)
            return ((state.toLong() and 0xFFFFFFFFL) % bound).toInt()
        }
    }
}
