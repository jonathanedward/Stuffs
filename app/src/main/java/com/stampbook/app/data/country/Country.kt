package com.stampbook.app.data.country

/**
 * A place that can be stamped. Coordinates are approximate country centroids,
 * good enough for plotting a dot on a world map.
 */
data class Country(
    val code: String,
    val name: String,
    val continent: Continent,
    val latitude: Double,
    val longitude: Double,
    /** False for territories and dependencies, which do not count toward the world total. */
    val sovereign: Boolean = true,
) {
    /**
     * Regional indicator pair, e.g. "FR" -> the French flag. Derived rather than
     * stored so the dataset stays a plain table.
     */
    val flag: String
        get() = code.uppercase().map { Character.toChars(FLAG_OFFSET + (it - 'A')).concatToString() }
            .joinToString("")

    private companion object {
        /** U+1F1E6 REGIONAL INDICATOR SYMBOL LETTER A. */
        const val FLAG_OFFSET = 0x1F1E6
    }
}

enum class Continent(val displayName: String) {
    AFRICA("Africa"),
    ASIA("Asia"),
    EUROPE("Europe"),
    NORTH_AMERICA("North America"),
    SOUTH_AMERICA("South America"),
    OCEANIA("Oceania"),
    ANTARCTICA("Antarctica"),
}
