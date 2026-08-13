package com.stampbook.app

import com.stampbook.app.core.Projection
import com.stampbook.app.core.StampStyles
import com.stampbook.app.data.country.Countries
import com.stampbook.app.data.model.PassportStats
import com.stampbook.app.data.model.Stamp
import java.time.LocalDate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CountryCatalogueTest {

    @Test fun codesAreUniqueAndWellFormed() {
        val codes = Countries.all.map { it.code }
        assertEquals(codes.size, codes.toSet().size, "duplicate country codes: " +
            codes.groupingBy { it }.eachCount().filter { it.value > 1 })
        codes.forEach { assertTrue(it.length == 2 && it.all { ch -> ch.isUpperCase() }, "bad code $it") }
    }

    @Test fun namesAreUnique() {
        val names = Countries.all.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test fun coordinatesAreInRange() {
        Countries.all.forEach {
            assertTrue(it.latitude in -90.0..90.0, "${it.name} latitude")
            assertTrue(it.longitude in -180.0..180.0, "${it.name} longitude")
        }
    }

    @Test fun sovereignCountCoversTheWorld() {
        // 193 UN members plus Palestine, Vatican City and Kosovo.
        assertEquals(196, Countries.sovereignCount)
        assertEquals(238, Countries.all.size)
    }

    @Test fun flagsDeriveFromCodes() {
        assertEquals("🇫🇷", Countries["FR"]!!.flag)
        assertEquals("🇺🇸", Countries["US"]!!.flag)
        Countries.all.forEach { assertEquals(4, it.flag.length, "${it.name} flag") }
    }

    @Test fun lookupIsCaseInsensitiveAndNullSafe() {
        assertEquals("Japan", Countries["jp"]?.name)
        assertEquals("Japan", Countries["JP"]?.name)
        assertEquals(null, Countries[null])
        assertEquals(null, Countries["ZZ"])
    }

    @Test fun searchRanksPrefixMatchesFirst() {
        assertEquals("Japan", Countries.search("jap").first().name)
        assertTrue(Countries.search("uni").take(3).any { it.name == "United Kingdom" })
        // Word-prefix beats a mid-word hit: "Tunisia" contains "uni" but does not start a word with it.
        assertTrue(
            Countries.search("uni").indexOfFirst { it.name == "United States" } <
                Countries.search("uni").indexOfFirst { it.name == "Tunisia" },
        )
        assertEquals("South Korea", Countries.search("kr").first().name)
    }

    @Test fun searchIgnoresAccentsAndCase() {
        assertEquals("Cote d'Ivoire", Countries.search("COTE").first().name)
        assertEquals("Cote d'Ivoire", Countries.search("  cote ").first().name)
    }

    @Test fun emptySearchReturnsEverything() {
        assertEquals(Countries.all.size, Countries.search("").size)
        assertEquals(0, Countries.search("qqqzz").size)
    }

    @Test fun everyContinentIsPopulated() {
        val byContinent = Countries.byContinent()
        assertEquals(7, byContinent.size)
        byContinent.forEach { (continent, list) -> assertTrue(list.isNotEmpty(), "$continent empty") }
    }
}

class ProjectionTest {

    @Test fun longitudeMapsAcrossTheFullWidth() {
        assertEquals(0f, Projection.xFraction(-180.0))
        assertEquals(0.5f, Projection.xFraction(0.0))
        assertEquals(1f, Projection.xFraction(180.0))
    }

    @Test fun latitudeIsFlippedForScreenCoordinates() {
        assertEquals(0f, Projection.yFraction(90.0))
        assertEquals(0.5f, Projection.yFraction(0.0))
        assertEquals(1f, Projection.yFraction(-90.0))
    }

    @Test fun shortestDeltaCrossesTheAntimeridian() {
        assertEquals(20.0, Projection.shortestLongitudeDelta(170.0, -170.0), 1e-9)
        assertEquals(-20.0, Projection.shortestLongitudeDelta(-170.0, 170.0), 1e-9)
        assertEquals(10.0, Projection.shortestLongitudeDelta(0.0, 10.0), 1e-9)
        assertEquals(-90.0, Projection.shortestLongitudeDelta(45.0, -45.0), 1e-9)
    }

    @Test fun shortestDeltaIsNeverTheLongWayRound() {
        Countries.all.forEach { a ->
            Countries.all.forEach { b ->
                val delta = Projection.shortestLongitudeDelta(a.longitude, b.longitude)
                assertTrue(abs(delta) <= 180.0, "${a.code}->${b.code} delta $delta")
            }
        }
    }
}

class StampStyleTest {

    @Test fun styleIsStableForASeed() {
        repeat(200) { seed ->
            assertEquals(StampStyles.forSeed(seed), StampStyles.forSeed(seed))
        }
    }

    @Test fun stylesVaryAcrossSeeds() {
        val styles = (0 until 500).map { StampStyles.forSeed(it) }
        assertTrue(styles.map { it.shape }.distinct().size >= 4)
        assertTrue(styles.map { it.inkArgb }.distinct().size >= 5)
        assertTrue(styles.map { it.label }.distinct().size >= 5)
    }

    @Test fun rotationAndAlphaStayInPrintableBounds() {
        (Int.MIN_VALUE / 2 until Int.MIN_VALUE / 2 + 300).forEach { seed ->
            val style = StampStyles.forSeed(seed)
            assertTrue(abs(style.rotationDegrees) <= 11f, "rotation ${style.rotationDegrees}")
            assertTrue(style.alpha in 0.72f..0.95f, "alpha ${style.alpha}")
            assertTrue(style.starCount in 0..3)
        }
    }
}

class PassportStatsTest {

    private fun stamp(code: String, city: String? = null, day: Int = 1, trip: Long? = null) =
        Stamp(
            id = day.toLong(),
            tripId = trip,
            countryCode = code,
            city = city,
            date = LocalDate.of(2026, 1, 1).plusDays(day.toLong()),
            seed = day,
        )

    @Test fun emptyPassportStillKnowsTheWorld() {
        val stats = PassportStats.EMPTY
        assertEquals(0, stats.countriesVisited)
        assertEquals("0%", stats.worldPercentLabel)
        assertEquals(7, stats.perContinent.size)
        assertEquals(Countries.all.size, stats.perContinent.sumOf { it.total })
        assertEquals(0, stats.perContinent.sumOf { it.visited })
        assertEquals(null, stats.firstStampDate)
    }

    @Test fun countsCountriesOnceAndSeparatesTerritories() {
        val stats = PassportStats.from(
            listOf(
                stamp("FR", "Paris", 1),
                stamp("FR", "Lyon", 2),
                stamp("HK", "Hong Kong", 3),
                stamp("JP", "Tokyo", 4),
            ),
            tripCount = 1,
        )
        assertEquals(2, stats.countriesVisited)
        assertEquals(1, stats.territoriesVisited)
        assertEquals(4, stats.citiesVisited)
        assertEquals(4, stats.stampCount)
        assertEquals(2, stats.continentsVisited)
    }

    @Test fun citiesDeduplicateWithinACountryButNotAcrossThem() {
        val stats = PassportStats.from(
            listOf(
                stamp("FR", "Paris", 1),
                stamp("FR", "paris", 2),
                stamp("FR", " PARIS ", 3),
                stamp("US", "Paris", 4),
                stamp("US", null, 5),
                stamp("US", "  ", 6),
            ),
            tripCount = 0,
        )
        assertEquals(2, stats.citiesVisited)
        assertEquals(2, stats.countriesVisited)
    }

    @Test fun unknownCountryCodesAreIgnoredNotCounted() {
        val stats = PassportStats.from(listOf(stamp("ZZ", "Atlantis", 1)), tripCount = 0)
        assertEquals(0, stats.countriesVisited)
        assertEquals(1, stats.stampCount)
    }

    @Test fun worldFractionUsesSovereignDenominator() {
        val stats = PassportStats.from(listOf(stamp("FR"), stamp("JP")), tripCount = 0)
        assertEquals(2f / 196f, stats.worldFraction, 1e-6f)
    }

    @Test fun datesSpanFirstToLatest() {
        val stats = PassportStats.from(
            listOf(stamp("FR", day = 10), stamp("JP", day = 1), stamp("US", day = 5)),
            tripCount = 0,
        )
        assertEquals(LocalDate.of(2026, 1, 2), stats.firstStampDate)
        assertEquals(LocalDate.of(2026, 1, 11), stats.latestStampDate)
    }

    @Test fun continentTotalsMatchTheCatalogue() {
        val stats = PassportStats.from(listOf(stamp("FR", "Paris")), tripCount = 0)
        val europe = stats.perContinent.single { it.continentName == "Europe" }
        assertEquals(1, europe.visited)
        assertEquals(Countries.all.count { it.continent.displayName == "Europe" }, europe.total)
        assertTrue(europe.fraction > 0f)
    }

    @Test fun stampHeadlineFallsBackToCountry() {
        assertEquals("Paris", stamp("FR", "Paris").headline)
        assertEquals("France", stamp("FR", "Paris").subhead)
        assertEquals("France", stamp("FR", null).headline)
        assertEquals(null, stamp("FR", null).subhead)
        assertEquals("France", stamp("FR", "   ").headline)
        assertNotNull(stamp("FR").country)
    }
}
