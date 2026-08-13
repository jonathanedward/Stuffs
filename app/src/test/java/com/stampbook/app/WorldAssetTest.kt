package com.stampbook.app

import com.stampbook.app.data.country.Countries
import com.stampbook.app.data.world.CountryShape
import com.stampbook.app.data.world.WorldAsset
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Runs against the very world.sbw the app ships. */
class WorldAssetTest {

    private val shapes: List<CountryShape> by lazy {
        val asset = sequenceOf(
            File("src/main/assets/world.sbw"),
            File("app/src/main/assets/world.sbw"),
        ).firstOrNull { it.exists() }
        assertNotNull(asset, "world.sbw not found")
        WorldAsset.parse(asset.readBytes())
    }

    private val byCode by lazy { shapes.associateBy { it.code } }

    @Test fun assetDecodes() {
        assertEquals(238, shapes.size)
        assertEquals(27_648, shapes.sumOf { shape -> shape.rings.sumOf { it.size / 2 } })
    }

    @Test fun everyRingIsAClosablePolygon() {
        shapes.forEach { shape ->
            assertTrue(shape.rings.isNotEmpty(), "${shape.code} has no rings")
            shape.rings.forEach { ring ->
                assertTrue(ring.size % 2 == 0, "${shape.code} ring is not coordinate pairs")
                assertTrue(ring.size / 2 >= 3, "${shape.code} ring has fewer than 3 points")
            }
        }
    }

    @Test fun coordinatesStayOnTheMap() {
        shapes.forEach { shape ->
            shape.rings.forEach { ring ->
                for (i in 0 until ring.size / 2) {
                    val x = ring[i * 2]
                    val y = ring[i * 2 + 1]
                    // x may run just past an edge: outlines that cross the
                    // antimeridian are unwrapped rather than split.
                    assertTrue(x > -1.05f && x < 2.05f, "${shape.code} x=$x")
                    assertTrue(y in 0f..1f, "${shape.code} y=$y")
                }
            }
        }
    }

    @Test fun onlyTheAntimeridianCountriesWrap() {
        // Antarctica spans every longitude but its outline, once unwrapped, still
        // lands inside the map; only these two genuinely run off an edge.
        assertEquals(setOf("RU", "FJ"), shapes.filter { it.wraps }.map { it.code }.toSet())
    }

    @Test fun smallCountriesSurvivedSimplification() {
        listOf("SG", "MC", "MT", "MV", "BH", "BB", "LI", "AD", "SM").forEach {
            assertNotNull(byCode[it], "$it has no outline")
        }
    }

    @Test fun countriesWithoutAnOutlineFallBackToTheCatalogue() {
        // Natural Earth has no polygon for the Vatican at any resolution it ships,
        // so the map draws it as a dot; it still has to be stampable.
        assertEquals(null, byCode["VA"])
        assertNotNull(Countries["VA"])
    }

    @Test fun codesAreKnownToTheCatalogue() {
        // "??" marks a boundary with no ISO code: drawn, but never highlighted.
        val unknown = shapes.map { it.code }.filter { it != "??" && Countries[it] == null }
        assertEquals(emptyList(), unknown)
    }

    @Test fun capitalsFallInsideTheirOwnCountry() {
        // Catches a flipped axis or an off-by-one in the delta decoding far more
        // sharply than a bounds check does.
        listOf(
            Triple("FR", 2.35, 48.86),
            Triple("JP", 139.69, 35.69),
            Triple("US", -77.04, 38.91),
            Triple("BR", -47.93, -15.78),
            Triple("AU", 149.13, -35.28),
            Triple("ZA", 28.19, -25.75),
            Triple("IN", 77.21, 28.61),
            Triple("PE", -77.04, -12.05),
            Triple("IS", -21.94, 64.15),
            Triple("NZ", 174.78, -41.29),
        ).forEach { (code, longitude, latitude) ->
            val shape = byCode[code]
            assertNotNull(shape, "$code missing")
            val x = ((longitude + 180.0) / 360.0).toFloat()
            val y = ((90.0 - latitude) / 180.0).toFloat()
            assertTrue(shape.contains(x, y), "$code does not contain its capital")
        }
    }

    @Test fun capitalsDoNotFallInTheWrongCountry() {
        val paris = ((2.35 + 180.0) / 360.0).toFloat() to ((90.0 - 48.86) / 180.0).toFloat()
        listOf("DE", "ES", "IT", "GB").forEach {
            assertTrue(byCode.getValue(it).contains(paris.first, paris.second).not(), "$it contains Paris")
        }
    }

    /** Even-odd ray cast across every ring, matching how the map fills a country. */
    private fun CountryShape.contains(x: Float, y: Float): Boolean {
        var inside = false
        rings.forEach { ring ->
            val count = ring.size / 2
            var j = count - 1
            for (i in 0 until count) {
                val xi = ring[i * 2]
                val yi = ring[i * 2 + 1]
                val xj = ring[j * 2]
                val yj = ring[j * 2 + 1]
                if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                    inside = !inside
                }
                j = i
            }
        }
        return inside
    }
}
