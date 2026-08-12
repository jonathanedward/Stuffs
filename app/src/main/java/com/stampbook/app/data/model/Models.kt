package com.stampbook.app.data.model

import com.stampbook.app.data.country.Countries
import com.stampbook.app.data.country.Country
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** One mark in the passport: a place, on a date. */
data class Stamp(
    val id: Long = 0L,
    val tripId: Long? = null,
    val countryCode: String,
    val city: String? = null,
    val date: LocalDate,
    val note: String? = null,
    val seed: Int,
) {
    val country: Country? get() = Countries[countryCode]

    /** City when we have one, otherwise the country. What the stamp shouts. */
    val headline: String
        get() = city?.takeIf { it.isNotBlank() } ?: country?.name ?: countryCode

    /** The smaller line under the headline; null when it would just repeat the headline. */
    val subhead: String?
        get() = country?.name?.takeIf { it != headline }
}

/** A journey, holding the stamps collected on it. */
data class Trip(
    val id: Long = 0L,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val notes: String? = null,
) {
    val nights: Long?
        get() = endDate?.let { ChronoUnit.DAYS.between(startDate, it) }
}

data class TripWithStamps(val trip: Trip, val stamps: List<Stamp>) {
    val countryCodes: Set<String> get() = stamps.map { it.countryCode }.toSet()
}

data class ContinentProgress(
    val continentName: String,
    val visited: Int,
    val total: Int,
) {
    val fraction: Float get() = if (total == 0) 0f else visited.toFloat() / total
}

data class PassportStats(
    val countriesVisited: Int,
    val territoriesVisited: Int,
    val citiesVisited: Int,
    val stampCount: Int,
    val tripCount: Int,
    val continentsVisited: Int,
    val worldFraction: Float,
    val firstStampDate: LocalDate?,
    val latestStampDate: LocalDate?,
    val perContinent: List<ContinentProgress>,
) {
    val worldPercentLabel: String get() = "${(worldFraction * 100).toInt()}%"

    companion object {
        /** Zero state, but still carrying the continent totals so the map screen has scales. */
        val EMPTY: PassportStats = from(emptyList(), tripCount = 0)

        fun from(stamps: List<Stamp>, tripCount: Int): PassportStats {
            val visitedCountries = stamps.mapNotNull { it.country }.distinctBy { it.code }
            val sovereign = visitedCountries.filter { it.sovereign }

            return PassportStats(
                countriesVisited = sovereign.size,
                territoriesVisited = visitedCountries.size - sovereign.size,
                // The same city name in two countries counts twice, which is the honest reading.
                citiesVisited = stamps.mapNotNull { stamp ->
                    stamp.city?.trim()?.takeIf { it.isNotEmpty() }
                        ?.let { stamp.countryCode to it.lowercase() }
                }.distinct().size,
                stampCount = stamps.size,
                tripCount = tripCount,
                continentsVisited = visitedCountries.map { it.continent }.distinct().size,
                worldFraction = sovereign.size.toFloat() / Countries.sovereignCount,
                firstStampDate = stamps.minOfOrNull { it.date },
                latestStampDate = stamps.maxOfOrNull { it.date },
                perContinent = Countries.byContinent().map { (continent, countries) ->
                    ContinentProgress(
                        continentName = continent.displayName,
                        visited = visitedCountries.count { it.continent == continent },
                        total = countries.size,
                    )
                },
            )
        }
    }
}
