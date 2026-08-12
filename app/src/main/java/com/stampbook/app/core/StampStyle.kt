package com.stampbook.app.core

import kotlin.random.Random

enum class StampShape { CIRCLE, RECTANGLE, HEXAGON, SCALLOP }

/**
 * The look of a single stamp. Derived entirely from a stored seed so a stamp
 * renders identically forever, but no two stamps look quite alike.
 */
data class StampStyle(
    val shape: StampShape,
    val inkArgb: Int,
    val rotationDegrees: Float,
    val alpha: Float,
    val doubleBorder: Boolean,
    val starCount: Int,
    val label: String,
)

object StampStyles {

    /** Passport-ink colours: no bright hues, everything looks rubber-stamped. */
    private val INKS = intArrayOf(
        0xFF1B3A6B.toInt(), // navy
        0xFF7B2233.toInt(), // oxblood
        0xFF1F5140.toInt(), // forest
        0xFF4A2A6B.toInt(), // violet
        0xFF2B2F36.toInt(), // near black
        0xFF14555A.toInt(), // teal
        0xFF8A4B12.toInt(), // sepia
    )

    private val LABELS = arrayOf("ARRIVAL", "ENTRY", "ADMITTED", "IMMIGRATION", "CONTROLE", "VISITED")

    fun forSeed(seed: Int): StampStyle {
        val random = Random(seed)
        return StampStyle(
            shape = StampShape.entries[random.nextInt(StampShape.entries.size)],
            inkArgb = INKS[random.nextInt(INKS.size)],
            // Hand-stamped, not machine-placed: a few degrees off true.
            rotationDegrees = random.nextDouble(-11.0, 11.0).toFloat(),
            // Uneven ink coverage.
            alpha = random.nextDouble(0.72, 0.95).toFloat(),
            doubleBorder = random.nextBoolean(),
            starCount = random.nextInt(0, 4),
            label = LABELS[random.nextInt(LABELS.size)],
        )
    }

    /** A fresh seed for a newly created stamp. */
    fun newSeed(): Int = Random.nextInt()
}
