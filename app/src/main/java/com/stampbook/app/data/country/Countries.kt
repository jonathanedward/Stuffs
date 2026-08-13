package com.stampbook.app.data.country

import com.stampbook.app.data.country.Continent.AFRICA
import com.stampbook.app.data.country.Continent.ANTARCTICA
import com.stampbook.app.data.country.Continent.ASIA
import com.stampbook.app.data.country.Continent.EUROPE
import com.stampbook.app.data.country.Continent.NORTH_AMERICA
import com.stampbook.app.data.country.Continent.OCEANIA
import com.stampbook.app.data.country.Continent.SOUTH_AMERICA
import java.text.Normalizer
import java.util.Locale

/**
 * The offline place catalogue. Everything the app knows about the world lives here:
 * no network call is ever made to resolve a country.
 */
object Countries {

    private fun c(
        code: String,
        name: String,
        continent: Continent,
        lat: Double,
        lon: Double,
        sovereign: Boolean = true,
    ) = Country(code, name, continent, lat, lon, sovereign)

    val all: List<Country> = listOf(
        // ---- Africa ----
        c("DZ", "Algeria", AFRICA, 28.03, 1.66),
        c("AO", "Angola", AFRICA, -11.20, 17.87),
        c("BJ", "Benin", AFRICA, 9.31, 2.32),
        c("BW", "Botswana", AFRICA, -22.33, 24.68),
        c("BF", "Burkina Faso", AFRICA, 12.24, -1.56),
        c("BI", "Burundi", AFRICA, -3.37, 29.92),
        c("CV", "Cabo Verde", AFRICA, 16.00, -24.01),
        c("CM", "Cameroon", AFRICA, 7.37, 12.35),
        c("CF", "Central African Republic", AFRICA, 6.61, 20.94),
        c("TD", "Chad", AFRICA, 15.45, 18.73),
        c("KM", "Comoros", AFRICA, -11.88, 43.87),
        c("CG", "Congo", AFRICA, -0.23, 15.83),
        c("CD", "DR Congo", AFRICA, -4.04, 21.76),
        c("CI", "Cote d'Ivoire", AFRICA, 7.54, -5.55),
        c("DJ", "Djibouti", AFRICA, 11.83, 42.59),
        c("EG", "Egypt", AFRICA, 26.82, 30.80),
        c("GQ", "Equatorial Guinea", AFRICA, 1.65, 10.27),
        c("ER", "Eritrea", AFRICA, 15.18, 39.78),
        c("SZ", "Eswatini", AFRICA, -26.52, 31.47),
        c("ET", "Ethiopia", AFRICA, 9.15, 40.49),
        c("GA", "Gabon", AFRICA, -0.80, 11.61),
        c("GM", "Gambia", AFRICA, 13.44, -15.31),
        c("GH", "Ghana", AFRICA, 7.95, -1.02),
        c("GN", "Guinea", AFRICA, 9.95, -9.70),
        c("GW", "Guinea-Bissau", AFRICA, 11.80, -15.18),
        c("KE", "Kenya", AFRICA, 0.02, 37.91),
        c("LS", "Lesotho", AFRICA, -29.61, 28.23),
        c("LR", "Liberia", AFRICA, 6.43, -9.43),
        c("LY", "Libya", AFRICA, 26.34, 17.23),
        c("MG", "Madagascar", AFRICA, -18.77, 46.87),
        c("MW", "Malawi", AFRICA, -13.25, 34.30),
        c("ML", "Mali", AFRICA, 17.57, -4.00),
        c("MR", "Mauritania", AFRICA, 21.01, -10.94),
        c("MU", "Mauritius", AFRICA, -20.35, 57.55),
        c("MA", "Morocco", AFRICA, 31.79, -7.09),
        c("MZ", "Mozambique", AFRICA, -18.67, 35.53),
        c("NA", "Namibia", AFRICA, -22.96, 18.49),
        c("NE", "Niger", AFRICA, 17.61, 8.08),
        c("NG", "Nigeria", AFRICA, 9.08, 8.68),
        c("RW", "Rwanda", AFRICA, -1.94, 29.87),
        c("ST", "Sao Tome and Principe", AFRICA, 0.19, 6.61),
        c("SN", "Senegal", AFRICA, 14.50, -14.45),
        c("SC", "Seychelles", AFRICA, -4.68, 55.49),
        c("SL", "Sierra Leone", AFRICA, 8.46, -11.78),
        c("SO", "Somalia", AFRICA, 5.15, 46.20),
        c("ZA", "South Africa", AFRICA, -30.56, 22.94),
        c("SS", "South Sudan", AFRICA, 6.88, 31.31),
        c("SD", "Sudan", AFRICA, 12.86, 30.22),
        c("TZ", "Tanzania", AFRICA, -6.37, 34.89),
        c("TG", "Togo", AFRICA, 8.62, 0.82),
        c("TN", "Tunisia", AFRICA, 33.89, 9.54),
        c("UG", "Uganda", AFRICA, 1.37, 32.29),
        c("ZM", "Zambia", AFRICA, -13.13, 27.85),
        c("ZW", "Zimbabwe", AFRICA, -19.02, 29.15),

        // ---- Asia ----
        c("AF", "Afghanistan", ASIA, 33.94, 67.71),
        c("AM", "Armenia", ASIA, 40.07, 45.04),
        c("AZ", "Azerbaijan", ASIA, 40.14, 47.58),
        c("BH", "Bahrain", ASIA, 26.07, 50.56),
        c("BD", "Bangladesh", ASIA, 23.68, 90.36),
        c("BT", "Bhutan", ASIA, 27.51, 90.43),
        c("BN", "Brunei", ASIA, 4.54, 114.73),
        c("KH", "Cambodia", ASIA, 12.57, 104.99),
        c("CN", "China", ASIA, 35.86, 104.20),
        c("CY", "Cyprus", ASIA, 35.13, 33.43),
        c("GE", "Georgia", ASIA, 42.32, 43.36),
        c("IN", "India", ASIA, 20.59, 78.96),
        c("ID", "Indonesia", ASIA, -0.79, 113.92),
        c("IR", "Iran", ASIA, 32.43, 53.69),
        c("IQ", "Iraq", ASIA, 33.22, 43.68),
        c("IL", "Israel", ASIA, 31.05, 34.85),
        c("JP", "Japan", ASIA, 36.20, 138.25),
        c("JO", "Jordan", ASIA, 30.59, 36.24),
        c("KZ", "Kazakhstan", ASIA, 48.02, 66.92),
        c("KW", "Kuwait", ASIA, 29.31, 47.48),
        c("KG", "Kyrgyzstan", ASIA, 41.20, 74.77),
        c("LA", "Laos", ASIA, 19.86, 102.50),
        c("LB", "Lebanon", ASIA, 33.85, 35.86),
        c("MY", "Malaysia", ASIA, 4.21, 101.98),
        c("MV", "Maldives", ASIA, 3.20, 73.22),
        c("MN", "Mongolia", ASIA, 46.86, 103.85),
        c("MM", "Myanmar", ASIA, 21.91, 95.96),
        c("NP", "Nepal", ASIA, 28.39, 84.12),
        c("KP", "North Korea", ASIA, 40.34, 127.51),
        c("OM", "Oman", ASIA, 21.51, 55.92),
        c("PK", "Pakistan", ASIA, 30.38, 69.35),
        c("PS", "Palestine", ASIA, 31.95, 35.23),
        c("PH", "Philippines", ASIA, 12.88, 121.77),
        c("QA", "Qatar", ASIA, 25.35, 51.18),
        c("SA", "Saudi Arabia", ASIA, 23.89, 45.08),
        c("SG", "Singapore", ASIA, 1.35, 103.82),
        c("KR", "South Korea", ASIA, 35.91, 127.77),
        c("LK", "Sri Lanka", ASIA, 7.87, 80.77),
        c("SY", "Syria", ASIA, 34.80, 38.997),
        c("TJ", "Tajikistan", ASIA, 38.86, 71.28),
        c("TH", "Thailand", ASIA, 15.87, 100.99),
        c("TL", "Timor-Leste", ASIA, -8.87, 125.73),
        c("TR", "Turkiye", ASIA, 38.96, 35.24),
        c("TM", "Turkmenistan", ASIA, 38.97, 59.56),
        c("AE", "United Arab Emirates", ASIA, 23.42, 53.85),
        c("UZ", "Uzbekistan", ASIA, 41.38, 64.59),
        c("VN", "Vietnam", ASIA, 14.06, 108.28),
        c("YE", "Yemen", ASIA, 15.55, 48.52),

        // ---- Europe ----
        c("AL", "Albania", EUROPE, 41.15, 20.17),
        c("AD", "Andorra", EUROPE, 42.55, 1.60),
        c("AT", "Austria", EUROPE, 47.52, 14.55),
        c("BY", "Belarus", EUROPE, 53.71, 27.95),
        c("BE", "Belgium", EUROPE, 50.50, 4.47),
        c("BA", "Bosnia and Herzegovina", EUROPE, 43.92, 17.68),
        c("BG", "Bulgaria", EUROPE, 42.73, 25.49),
        c("HR", "Croatia", EUROPE, 45.10, 15.20),
        c("CZ", "Czechia", EUROPE, 49.82, 15.47),
        c("DK", "Denmark", EUROPE, 56.26, 9.50),
        c("EE", "Estonia", EUROPE, 58.60, 25.01),
        c("FI", "Finland", EUROPE, 61.92, 25.75),
        c("FR", "France", EUROPE, 46.23, 2.21),
        c("DE", "Germany", EUROPE, 51.17, 10.45),
        c("GR", "Greece", EUROPE, 39.07, 21.82),
        c("HU", "Hungary", EUROPE, 47.16, 19.50),
        c("IS", "Iceland", EUROPE, 64.96, -19.02),
        c("IE", "Ireland", EUROPE, 53.41, -8.24),
        c("IT", "Italy", EUROPE, 41.87, 12.57),
        c("XK", "Kosovo", EUROPE, 42.60, 20.90),
        c("LV", "Latvia", EUROPE, 56.88, 24.60),
        c("LI", "Liechtenstein", EUROPE, 47.17, 9.56),
        c("LT", "Lithuania", EUROPE, 55.17, 23.88),
        c("LU", "Luxembourg", EUROPE, 49.82, 6.13),
        c("MT", "Malta", EUROPE, 35.94, 14.38),
        c("MD", "Moldova", EUROPE, 47.41, 28.37),
        c("MC", "Monaco", EUROPE, 43.75, 7.41),
        c("ME", "Montenegro", EUROPE, 42.71, 19.37),
        c("NL", "Netherlands", EUROPE, 52.13, 5.29),
        c("MK", "North Macedonia", EUROPE, 41.61, 21.75),
        c("NO", "Norway", EUROPE, 60.47, 8.47),
        c("PL", "Poland", EUROPE, 51.92, 19.15),
        c("PT", "Portugal", EUROPE, 39.40, -8.22),
        c("RO", "Romania", EUROPE, 45.94, 24.97),
        c("RU", "Russia", EUROPE, 61.52, 105.32),
        c("SM", "San Marino", EUROPE, 43.94, 12.46),
        c("RS", "Serbia", EUROPE, 44.02, 21.01),
        c("SK", "Slovakia", EUROPE, 48.67, 19.70),
        c("SI", "Slovenia", EUROPE, 46.15, 14.99),
        c("ES", "Spain", EUROPE, 40.46, -3.75),
        c("SE", "Sweden", EUROPE, 60.13, 18.64),
        c("CH", "Switzerland", EUROPE, 46.82, 8.23),
        c("UA", "Ukraine", EUROPE, 48.38, 31.17),
        c("GB", "United Kingdom", EUROPE, 55.38, -3.44),
        c("VA", "Vatican City", EUROPE, 41.90, 12.45),

        // ---- North America ----
        c("AG", "Antigua and Barbuda", NORTH_AMERICA, 17.06, -61.80),
        c("BS", "Bahamas", NORTH_AMERICA, 25.03, -77.40),
        c("BB", "Barbados", NORTH_AMERICA, 13.19, -59.54),
        c("BZ", "Belize", NORTH_AMERICA, 17.19, -88.50),
        c("CA", "Canada", NORTH_AMERICA, 56.13, -106.35),
        c("CR", "Costa Rica", NORTH_AMERICA, 9.75, -83.75),
        c("CU", "Cuba", NORTH_AMERICA, 21.52, -77.78),
        c("DM", "Dominica", NORTH_AMERICA, 15.41, -61.37),
        c("DO", "Dominican Republic", NORTH_AMERICA, 18.74, -70.16),
        c("SV", "El Salvador", NORTH_AMERICA, 13.79, -88.90),
        c("GD", "Grenada", NORTH_AMERICA, 12.12, -61.68),
        c("GT", "Guatemala", NORTH_AMERICA, 15.78, -90.23),
        c("HT", "Haiti", NORTH_AMERICA, 18.97, -72.29),
        c("HN", "Honduras", NORTH_AMERICA, 15.20, -86.24),
        c("JM", "Jamaica", NORTH_AMERICA, 18.11, -77.30),
        c("MX", "Mexico", NORTH_AMERICA, 23.63, -102.55),
        c("NI", "Nicaragua", NORTH_AMERICA, 12.87, -85.21),
        c("PA", "Panama", NORTH_AMERICA, 8.54, -80.78),
        c("KN", "Saint Kitts and Nevis", NORTH_AMERICA, 17.36, -62.78),
        c("LC", "Saint Lucia", NORTH_AMERICA, 13.91, -60.98),
        c("VC", "Saint Vincent and the Grenadines", NORTH_AMERICA, 12.98, -61.29),
        c("TT", "Trinidad and Tobago", NORTH_AMERICA, 10.69, -61.22),
        c("US", "United States", NORTH_AMERICA, 39.83, -98.58),

        // ---- South America ----
        c("AR", "Argentina", SOUTH_AMERICA, -38.42, -63.62),
        c("BO", "Bolivia", SOUTH_AMERICA, -16.29, -63.59),
        c("BR", "Brazil", SOUTH_AMERICA, -14.24, -51.93),
        c("CL", "Chile", SOUTH_AMERICA, -35.68, -71.54),
        c("CO", "Colombia", SOUTH_AMERICA, 4.57, -74.30),
        c("EC", "Ecuador", SOUTH_AMERICA, -1.83, -78.18),
        c("GY", "Guyana", SOUTH_AMERICA, 4.86, -58.93),
        c("PY", "Paraguay", SOUTH_AMERICA, -23.44, -58.44),
        c("PE", "Peru", SOUTH_AMERICA, -9.19, -75.02),
        c("SR", "Suriname", SOUTH_AMERICA, 3.92, -56.03),
        c("UY", "Uruguay", SOUTH_AMERICA, -32.52, -55.77),
        c("VE", "Venezuela", SOUTH_AMERICA, 6.42, -66.59),

        // ---- Oceania ----
        c("AU", "Australia", OCEANIA, -25.27, 133.78),
        c("FJ", "Fiji", OCEANIA, -17.71, 178.07),
        c("KI", "Kiribati", OCEANIA, 1.87, -157.36),
        c("MH", "Marshall Islands", OCEANIA, 7.13, 171.18),
        c("FM", "Micronesia", OCEANIA, 7.43, 150.55),
        c("NR", "Nauru", OCEANIA, -0.52, 166.93),
        c("NZ", "New Zealand", OCEANIA, -40.90, 174.89),
        c("PW", "Palau", OCEANIA, 7.51, 134.58),
        c("PG", "Papua New Guinea", OCEANIA, -6.31, 143.96),
        c("WS", "Samoa", OCEANIA, -13.76, -172.10),
        c("SB", "Solomon Islands", OCEANIA, -9.65, 160.16),
        c("TO", "Tonga", OCEANIA, -21.18, -175.20),
        c("TV", "Tuvalu", OCEANIA, -7.11, 177.65),
        c("VU", "Vanuatu", OCEANIA, -15.38, 166.96),

        // ---- Territories and dependencies (stampable, but not counted as countries) ----
        // Centroids below the Antarctica line were computed from the boundary
        // outlines in world.sbw rather than typed in by hand.
        c("AW", "Aruba", NORTH_AMERICA, 12.52, -69.97, sovereign = false),
        c("BM", "Bermuda", NORTH_AMERICA, 32.32, -64.75, sovereign = false),
        c("KY", "Cayman Islands", NORTH_AMERICA, 19.31, -81.25, sovereign = false),
        c("CW", "Curacao", NORTH_AMERICA, 12.17, -68.99, sovereign = false),
        c("GL", "Greenland", NORTH_AMERICA, 71.71, -42.60, sovereign = false),
        c("PR", "Puerto Rico", NORTH_AMERICA, 18.22, -66.59, sovereign = false),
        c("VI", "U.S. Virgin Islands", NORTH_AMERICA, 18.34, -64.90, sovereign = false),
        c("FO", "Faroe Islands", EUROPE, 61.89, -6.91, sovereign = false),
        c("GI", "Gibraltar", EUROPE, 36.14, -5.35, sovereign = false),
        c("HK", "Hong Kong", ASIA, 22.32, 114.17, sovereign = false),
        c("MO", "Macau", ASIA, 22.20, 113.54, sovereign = false),
        c("TW", "Taiwan", ASIA, 23.70, 120.96, sovereign = false),
        c("PF", "French Polynesia", OCEANIA, -17.68, -149.41, sovereign = false),
        c("GU", "Guam", OCEANIA, 13.44, 144.79, sovereign = false),
        c("NC", "New Caledonia", OCEANIA, -20.90, 165.62, sovereign = false),
        c("AQ", "Antarctica", ANTARCTICA, -75.25, 0.07, sovereign = false),
        c("AS", "American Samoa", OCEANIA, -14.31, -170.71, sovereign = false),
        c("AI", "Anguilla", NORTH_AMERICA, 18.20, -63.08, sovereign = false),
        c("IO", "British Indian Ocean Territory", AFRICA, -7.35, 72.44, sovereign = false),
        c("VG", "British Virgin Islands", NORTH_AMERICA, 18.41, -64.64, sovereign = false),
        c("CK", "Cook Islands", OCEANIA, -21.24, -159.79, sovereign = false),
        c("FK", "Falkland Islands", SOUTH_AMERICA, -51.74, -58.78, sovereign = false),
        c("TF", "French Southern and Antarctic Lands", ANTARCTICA, -49.30, 69.52, sovereign = false),
        c("GG", "Guernsey", EUROPE, 49.48, -2.58, sovereign = false),
        c("HM", "Heard Island and McDonald Islands", ANTARCTICA, -53.09, 73.52, sovereign = false),
        c("IM", "Isle of Man", EUROPE, 54.21, -4.53, sovereign = false),
        c("JE", "Jersey", EUROPE, 49.23, -2.16, sovereign = false),
        c("MS", "Montserrat", NORTH_AMERICA, 16.75, -62.19, sovereign = false),
        c("NU", "Niue", OCEANIA, -19.06, -169.87, sovereign = false),
        c("NF", "Norfolk Island", OCEANIA, -29.06, 167.95, sovereign = false),
        c("MP", "Northern Mariana Islands", OCEANIA, 15.19, 145.74, sovereign = false),
        c("PN", "Pitcairn Islands", OCEANIA, -24.36, -128.31, sovereign = false),
        c("BL", "Saint Barthelemy", NORTH_AMERICA, 17.90, -62.85, sovereign = false),
        c("SH", "Saint Helena, Ascension and Tristan da Cunha", AFRICA, -15.97, -5.73, sovereign = false),
        c("MF", "Saint Martin", NORTH_AMERICA, 18.07, -63.07, sovereign = false),
        c("PM", "Saint Pierre and Miquelon", NORTH_AMERICA, 46.95, -56.33, sovereign = false),
        c("SX", "Sint Maarten", NORTH_AMERICA, 18.07, -63.07, sovereign = false),
        c("GS", "South Georgia", ANTARCTICA, -54.35, -36.71, sovereign = false),
        c("TC", "Turks and Caicos Islands", NORTH_AMERICA, 21.80, -72.27, sovereign = false),
        c("WF", "Wallis and Futuna", OCEANIA, -14.29, -178.12, sovereign = false),
        c("EH", "Western Sahara", AFRICA, 24.22, -12.21, sovereign = false),
        c("AX", "Aland Islands", EUROPE, 60.23, 19.94, sovereign = false),
    ).sortedBy { it.name }

    private val byCode: Map<String, Country> = all.associateBy { it.code }

    /** Number of sovereign countries in the catalogue; the denominator for "% of the world". */
    val sovereignCount: Int = all.count { it.sovereign }

    operator fun get(code: String?): Country? = code?.let { byCode[it.uppercase(Locale.ROOT)] }

    fun byContinent(): Map<Continent, List<Country>> =
        all.groupBy { it.continent }.toSortedMap(compareBy { it.ordinal })

    /**
     * Matches on name prefix first, then on any word prefix, then on the country code,
     * so typing "uni" surfaces the United Kingdom before Tunisia and "kr" finds South Korea.
     */
    fun search(query: String): List<Country> {
        val q = query.fold()
        if (q.isEmpty()) return all
        return all.mapNotNull { country ->
            val name = country.name.fold()
            val rank = when {
                name.startsWith(q) -> 0
                name.split(' ').any { it.startsWith(q) } -> 1
                country.code.lowercase(Locale.ROOT) == q -> 2
                name.contains(q) -> 3
                else -> null
            }
            rank?.let { country to it }
        }.sortedWith(compareBy({ it.second }, { it.first.name })).map { it.first }
    }

    /** Lowercases and strips accents so "cote" matches "Cote d'Ivoire". */
    private fun String.fold(): String =
        Normalizer.normalize(trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
}
