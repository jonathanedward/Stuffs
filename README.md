# Stampbook

An offline travel passport for Android. Stamp the countries and cities you have
been to, group them into trips, and watch the world fill in.

Everything lives on the device. No account, no network calls, no analytics —
the app works on a plane and in a country where your SIM does not.

## What it does

**Passport** — the front page counts your countries and how much of the world
that is, then every stamp you have collected, newest year first. Every stamp is
drawn at run time, and what makes it that country's stamp is taken from the
country: the ink is its flag's dominant hue muted to something a rubber stamp
could leave, the country names itself in its own language and script (日本,
ΕΛΛΑΔΑ, المغرب), the wording is what a border post there would print (上陸許可,
دخول, ΕΙΣΟΔΟΣ), and the shape follows regional convention. The seed stored with each stamp decides only how that pressing came
out: the angle it was banged down at, how much ink was on the pad, where the ink
failed, and its serial.

**Trips** — a trip groups the stamps from one journey and gives them a name, a
date range and notes. Stamps can also stand alone; those show up under "Not in
a trip" so nothing gets lost.

**Map** — the world with its borders drawn, every country you have stamped
filled in, and each trip traced as an arc through its stops in order. Pinch to
zoom, drag to pan. Underneath: your share of the world, and progress per
continent.

## Building

Open in Android Studio, or from the command line with an Android SDK installed:

```
./gradlew assembleDebug     # or: gradle assembleDebug
./gradlew test              # unit tests for the catalogue, projection and stats
```

- minSdk 26, compileSdk 35
- Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01), Material 3
- Room 2.6.1 for storage, Navigation Compose for routing
- No dependency injection framework: the object graph is two DAOs and a
  repository, wired in `StampbookApplication`

## How it is put together

```
core/            pure logic, no Android imports — projection, stamp styling
data/country/    the offline place catalogue (238 places, 196 sovereign) and
                 the generated per-country ink, wording and design family
data/world/      reader for the packed country outlines
data/local/      Room entities, DAOs, database
data/model/      domain types and the stats calculation
ui/stamp/        the stamp renderer (Canvas + native text-on-path)
ui/passport/     the book
ui/trips/        trip list, detail, editor
ui/map/          the world map
ui/add/          the stamping flow
nav/             routes and the app shell

assets/world.sbw               packed country outlines, 68 KB
tools/build_world_asset.py     rebuilds that asset from Natural Earth
tools/build_country_details.mjs  rebuilds the per-country traits from flag artwork
```

Flag emoji are derived from ISO 3166-1 alpha-2 codes as regional indicator
pairs rather than stored, so the catalogue stays a plain table of code, name,
continent and centroid.

Country counts use 196 sovereign states as the denominator (193 UN members plus
Palestine, Vatican City and Kosovo). The other 42 entries — Hong Kong, Puerto
Rico, Greenland, the Falklands and the like — can be stamped but are counted
separately, so visiting Guam does not claim a country you have not been to.

## Design notes

**Why the outlines are packed by hand.** Filling in countries needs boundary
data, and the obvious routes to it are both bad here: a map tile service breaks
offline-first, and shipping GeoJSON means parsing a megabyte of JSON at startup.
Natural Earth 1:50m is only about 80,000 points before simplification, so
`tools/build_world_asset.py` reduces it with Douglas-Peucker, quantises to a
hundredth of a degree, delta-encodes with varints, and writes a flat binary.
The result is 238 outlines and roughly 27,000 points in 68 KB, read once on a
background thread with no JSON parser involved.

Monaco and Singapore do have outlines at this resolution; the Vatican has none
at any resolution Natural Earth ships, so it falls back to a dot at its
centroid and stays stampable. Two disputed boundaries in the source data
(Northern Cyprus, Somaliland) carry no ISO code; they are drawn but never
highlighted.

The catalogue's 42 territories are stampable but excluded from the country
count, so a trip to the Falklands or the Isle of Man shows on the map without
claiming a country. Their centroids were computed from the boundary outlines
rather than typed in.

**Why stamps are generated, not drawn.** Shipping artwork for 238 places is not
realistic, and a list of rows is not something anyone screenshots. Generating
them keeps the data a plain table while the passport still feels collected.

Splitting the country's design from the pressing is what makes that work.
Deriving everything from the stamp's own seed made all 238 look like variations
on one rubber stamp. Keying the design to the country instead gives each
authority its own, and the traits that matter are real rather than invented:
flag-derived ink, an entry word and the country's own name for itself in one of
its official languages, its alpha-3 code, and a design family that follows what
that part of the world actually prints. Only the border treatment, device and ornaments come from
hashing the code. Over 90% of countries end up with a look no one else has, and
a test holds that line.

Design families are stored as whole shape-and-layout pairs rather than two
independent lists, so a country can never draw an arched heading on a rectangle.

The city keeps whatever script you typed it in, so every stamp has one line that
is readable at a glance; with no city, that line falls back to the country's
English name. Headings in a script that joins its letters or runs right to left
are set straight instead of bent, because the arc places one character at a time
and would pull Arabic apart — `core/Scripts.kt` decides which is which.

Text is fitted against the outline rather than against fixed margins — each
layout asks the shape how much room there is at that height, and the shape
answers for its own geometry, including the innermost line of the border. That
is what keeps a long name off the taper of a shield and an arced date clear of a
double ring.

Text is fitted against the outline rather than against fixed margins — each
layout asks the shape how much room there is at that height, and the shape
answers for its own geometry, including the innermost line of the border. That
is what keeps a long name off the taper of a shield and an arced date clear of a
double ring.

## Data

Country outlines are Natural Earth 1:50m, in the public domain, taken from the
`world-atlas` package; the numeric-to-ISO mapping comes from `world-countries`.
Neither is a runtime dependency — they are inputs to
`tools/build_world_asset.py`, which is run by hand when the source data changes:

```
npm pack world-atlas@2.0.2 world-countries@5.0.0 flag-icons@7.2.3
python3 tools/build_world_asset.py <extracted-dir>
node tools/build_country_details.mjs <extracted-dir>
```

Stamp inks are sampled from the flag artwork in `flag-icons` (MIT); entry
wording and ISO alpha-3 codes come from `world-countries` (MIT). Rasterising
the flags needs a browser, so that generator runs under Playwright. Neither
package is a runtime dependency — both feed a generated Kotlin table.

## Not built yet

- Photos and journal entries attached to stamps
- "Stamp where I am now" using GPS
- Export and backup to a file, and sync across devices
- Sharing a stamp or the map as an image
