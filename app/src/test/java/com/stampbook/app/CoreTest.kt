package com.stampbook.app

import com.stampbook.app.core.BorderStyle
import com.stampbook.app.core.DesignRegion
import com.stampbook.app.core.Projection
import com.stampbook.app.core.Scripts
import com.stampbook.app.core.StampDesign
import com.stampbook.app.core.StampDevice
import com.stampbook.app.core.StampLayout
import com.stampbook.app.core.StampShape
import com.stampbook.app.core.StampStyles
import com.stampbook.app.data.country.Countries
import com.stampbook.app.data.country.CountryDetails
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

class StampDesignTest {

    private val designs = Countries.all.map { it.code to StampStyles.forCountry(CountryDetails[it.code]) }

    @Test fun aCountryAlwaysGetsTheSameDesign() {
        Countries.all.forEach {
            assertEquals(
                StampStyles.forCountry(CountryDetails[it.code]),
                StampStyles.forCountry(CountryDetails[it.code]),
            )
        }
        assertEquals(
            StampStyles.forCountry(CountryDetails["JP"]),
            StampStyles.forCountry(CountryDetails["jp"]),
        )
    }

    @Test fun everyCountryHasItsOwnTraits() {
        Countries.all.forEach {
            val traits = CountryDetails[it.code]
            assertEquals(it.code, traits.code)
            assertEquals(3, traits.alpha3.length, "${it.code} alpha-3")
            assertTrue(traits.entryWord.isNotBlank(), "${it.code} has no wording")
            // Opaque, and dark enough to read as ink on paper.
            assertEquals(0xFF, traits.inkArgb ushr 24 and 0xFF, "${it.code} ink not opaque")
        }
    }

    @Test fun inkComesFromTheFlagNotAPalette() {
        // A shared palette would repeat heavily across 238 countries; flags do not.
        val inks = Countries.all.map { CountryDetails[it.code].inkArgb }.distinct()
        assertTrue(inks.size > 150, "only ${inks.size} distinct inks")
    }

    @Test fun countriesAreNamedAsTheyNameThemselves() {
        assertEquals("日本", CountryDetails["JP"].nativeName)
        assertEquals("DEUTSCHLAND", CountryDetails["DE"].nativeName)
        assertEquals("ΕΛΛΑΔΑ", CountryDetails["GR"].nativeName, "Greek drops accents in capitals")
        assertEquals("РОССИЯ", CountryDetails["RU"].nativeName)
        assertEquals("مصر", CountryDetails["EG"].nativeName)
        assertEquals("TÜRKİYE", CountryDetails["TR"].nativeName, "Turkish capitalises i with its dot")
        assertEquals("SUOMI", CountryDetails["FI"].nativeName)
        Countries.all.forEach {
            assertTrue(CountryDetails[it.code].nativeName.isNotBlank(), "${it.code} unnamed")
        }
    }

    @Test fun headingsThatCannotBeBentAreRecognised() {
        // Arabic joins its letters and runs right to left; drawn one character at
        // a time around an arc it comes out disjointed and backwards.
        listOf("مصر", "ประเทศไทย", "ភ្នំពេញ", "प्रवेश").forEach {
            assertTrue(Scripts.needsShaping(it), "$it should be set straight")
        }
        // These stand alone well enough to bend one character at a time.
        listOf("日本", "한국", "ΕΛΛΑΔΑ", "РОССИЯ", "TÜRKİYE", "PARIS", "VIỆT NAM").forEach {
            assertTrue(!Scripts.needsShaping(it), "$it can follow an arc")
        }
    }

    @Test fun wordingIsInTheCountrysOwnLanguage() {
        assertEquals("上陸許可", CountryDetails["JP"].entryWord)
        assertEquals("ENTRÉE", CountryDetails["FR"].entryWord)
        assertEquals("ВЪЕЗД", CountryDetails["RU"].entryWord)
        assertEquals("دخول", CountryDetails["SA"].entryWord)
        assertEquals("ΕΙΣΟΔΟΣ", CountryDetails["GR"].entryWord)
        assertEquals("입국", CountryDetails["KR"].entryWord)
    }

    @Test fun unknownCodesStillGetAStamp() {
        val traits = CountryDetails["ZZ"]
        assertEquals("ZZ", traits.alpha3)
        assertEquals("ENTRY", traits.entryWord)
    }

    @Test fun neighboursInARegionShareAFamilyWithoutRepeating() {
        // A page of European stamps should read as European, but Germany and
        // France must not print the same stamp.
        val europe = Countries.all
            .filter { CountryDetails[it.code].region == DesignRegion.EUROPE }
            .map { StampStyles.forCountry(CountryDetails[it.code]) }
        assertTrue(europe.all { !it.shape.isRound || it.shape == StampShape.HEXAGON })
        assertTrue(europe.distinct().size > europe.size * 0.85)
    }

    @Test fun neighboursDoNotShareADesign() {
        // The whole point of keying off the country: two stamps side by side in the
        // passport should not look like the same rubber stamp.
        val distinct = designs.map { it.second }.distinct()
        assertTrue(distinct.size > Countries.all.size * 0.9, "only ${distinct.size} distinct designs")
    }

    @Test fun everyShapeAndLayoutGetsUsed() {
        assertEquals(StampShape.entries.toSet(), designs.map { it.second.shape }.toSet())
        assertEquals(StampLayout.entries.toSet(), designs.map { it.second.layout }.toSet())
        assertEquals(BorderStyle.entries.toSet(), designs.map { it.second.border }.toSet())
        assertEquals(StampDevice.entries.toSet(), designs.map { it.second.device }.toSet())
    }

    @Test fun noSingleLookSwallowsTheCatalogue() {
        listOf<(StampDesign) -> Any>({ it.shape }, { it.layout }, { it.border }, { it.inkArgb })
            .forEach { property ->
                val biggest = designs.groupingBy { property(it.second) }.eachCount().values.max()
                assertTrue(biggest < Countries.all.size / 2, "one value covers $biggest countries")
            }
    }

    @Test fun everyRegionOffersOnlyWorkableCombinations() {
        DesignRegion.entries.forEach { region ->
            assertTrue(region.looks.isNotEmpty(), "$region has no looks")
            region.looks.forEach { (shape, layout) ->
                val needsRound = layout == StampLayout.ARCH || layout == StampLayout.DATE_ARCH
                val needsAngular = layout == StampLayout.SPLIT || layout == StampLayout.FORM
                if (needsRound) assertTrue(shape.isRound, "$region bends text around $shape")
                if (needsAngular) assertTrue(!shape.isRound, "$region rules a $shape")
            }
        }
    }

    @Test fun layoutsMatchTheOutlineTheySitIn() {
        val roundOnly = setOf(StampLayout.ARCH, StampLayout.DATE_ARCH)
        val angularOnly = setOf(StampLayout.SPLIT, StampLayout.FORM)
        designs.forEach { (code, design) ->
            if (design.layout in roundOnly) {
                assertTrue(design.shape.isRound, "$code bends text around ${design.shape}")
            }
            if (design.layout in angularOnly) {
                assertTrue(!design.shape.isRound, "$code rules a ${design.shape}")
            }
        }
    }

    @Test fun devicesAppearOnlyWhereThereIsRoom() {
        designs.forEach { (code, design) ->
            when (design.layout) {
                // The split panel is built around its device.
                StampLayout.SPLIT -> assertTrue(design.device != StampDevice.NONE, "$code has no device")
                // Anything else would collide with the knocked-out band or a rule.
                StampLayout.ARCH, StampLayout.DATE_ARCH -> Unit
                else -> assertEquals(StampDevice.NONE, design.device, "$code carries a device")
            }
        }
    }

    @Test fun cornerTicksNeverLandOnACurve() {
        designs.forEach { (code, design) ->
            if (design.cornerTicks) assertTrue(!design.shape.isRound, "$code ticks a ${design.shape}")
        }
    }
}

class StampImpressionTest {

    @Test fun impressionIsStableForASeed() {
        repeat(200) { seed -> assertEquals(StampStyles.forSeed(seed), StampStyles.forSeed(seed)) }
    }

    @Test fun impressionsVaryAcrossSeeds() {
        val pressings = (0 until 500).map { StampStyles.forSeed(it) }
        assertTrue(pressings.map { it.rotationDegrees }.distinct().size > 400)
        assertTrue(pressings.map { it.serial }.distinct().size > 400)
    }

    @Test fun rotationAndAlphaStayInPrintableBounds() {
        (Int.MIN_VALUE / 2 until Int.MIN_VALUE / 2 + 300).forEach { seed ->
            val impression = StampStyles.forSeed(seed)
            assertTrue(abs(impression.rotationDegrees) <= 11f, "rotation ${impression.rotationDegrees}")
            assertTrue(impression.alpha in 0.72f..0.95f, "alpha ${impression.alpha}")
            assertEquals(5, impression.serial.length)
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
