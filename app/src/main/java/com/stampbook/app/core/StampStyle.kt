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
 * How one country's stamp looks. Derived from the country code, so every stamp
 * from Japan shares a design and none of them look like France's.
 */
data class StampDesign(
    val shape: StampShape,
    val border: BorderStyle,
    val layout: StampLayout,
    val inkArgb: Int,
    val device: StampDevice,
    val label: String,
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

    /** Rubber-stamp inks: nothing bright, everything looks pressed rather than printed. */
    private val INKS = intArrayOf(
        0xFF1B3A6B.toInt(), // navy
        0xFF7B2233.toInt(), // oxblood
        0xFF1F5140.toInt(), // forest
        0xFF4A2A6B.toInt(), // violet
        0xFF2B2F36.toInt(), // near black
        0xFF14555A.toInt(), // teal
        0xFF8A4B12.toInt(), // sepia
        0xFF2E3D7A.toInt(), // indigo
        0xFF4A5A20.toInt(), // moss
        0xFF5C2A4A.toInt(), // plum
        0xFF93441A.toInt(), // rust
        0xFF3A4A57.toInt(), // slate
        0xFF17493A.toInt(), // bottle
        0xFF6B1F3A.toInt(), // wine
    )

    private val LABELS = arrayOf(
        "ARRIVAL", "ENTRY", "ADMITTED", "IMMIGRATION", "CONTROLE", "ENTRADA",
        "EINREISE", "INGRESSO", "FRONTIER", "ADMISSION", "CONTROL", "VISITED",
    )

    private val ROUND_LAYOUTS = arrayOf(
        StampLayout.ARCH, StampLayout.ARCH, StampLayout.DATE_ARCH,
        StampLayout.BAND, StampLayout.STACKED,
    )

    private val ANGULAR_LAYOUTS = arrayOf(
        StampLayout.STACKED, StampLayout.BAND, StampLayout.FORM,
        StampLayout.SPLIT, StampLayout.FORM,
    )

    /**
     * The design an authority uses. Two countries can land on the same shape, but
     * shape, border, layout, ink, device and wording are drawn independently, so
     * a collision across all six is vanishingly unlikely.
     */
    fun forCountry(code: String): StampDesign {
        val pick = Pick(fnv1a(code.uppercase()))
        val shape = StampShape.entries[pick.next(StampShape.entries.size)]
        val layout = if (shape.isRound) {
            ROUND_LAYOUTS[pick.next(ROUND_LAYOUTS.size)]
        } else {
            ANGULAR_LAYOUTS[pick.next(ANGULAR_LAYOUTS.size)]
        }
        return StampDesign(
            shape = shape,
            border = BorderStyle.entries[pick.next(BorderStyle.entries.size)],
            layout = layout,
            inkArgb = INKS[pick.next(INKS.size)],
            // Only the layouts that keep a clear slot for it carry a device;
            // the split panel is built around one, so it always gets one.
            device = when (layout) {
                StampLayout.SPLIT -> StampDevice.entries[1 + pick.next(StampDevice.entries.size - 1)]
                StampLayout.ARCH, StampLayout.DATE_ARCH ->
                    StampDevice.entries[pick.next(StampDevice.entries.size)]
                else -> StampDevice.NONE
            },
            label = LABELS[pick.next(LABELS.size)],
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
