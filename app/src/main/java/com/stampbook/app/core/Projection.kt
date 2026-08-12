package com.stampbook.app.core

import kotlin.math.abs

/**
 * Equirectangular (plate carree) projection. Returns 0..1 fractions of the map
 * rectangle so the caller can scale to whatever canvas it has.
 */
object Projection {

    fun xFraction(longitude: Double): Float =
        (((longitude + 180.0) / 360.0)).toFloat().coerceIn(0f, 1f)

    fun yFraction(latitude: Double): Float =
        (((90.0 - latitude) / 180.0)).toFloat().coerceIn(0f, 1f)

    /**
     * Signed longitude difference along the shorter way round, in -180..180.
     * Keeps a Tokyo -> Los Angeles route crossing the Pacific instead of
     * stretching back across all of Eurasia.
     */
    fun shortestLongitudeDelta(from: Double, to: Double): Double {
        var delta = (to - from) % 360.0
        if (delta > 180.0) delta -= 360.0
        if (delta < -180.0) delta += 360.0
        return delta
    }

    /** True when the shorter route between two longitudes crosses the antimeridian. */
    fun crossesAntimeridian(fromLongitude: Double, toLongitude: Double): Boolean =
        abs(toLongitude - fromLongitude) > 180.0
}
