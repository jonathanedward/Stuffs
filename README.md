# Stampbook

An offline travel passport for Android. Stamp the countries and cities you have
been to, group them into trips, and watch the world fill in.

Everything lives on the device. No account, no network calls, no analytics —
the app works on a plane and in a country where your SIM does not.

## What it does

**Passport** — the front page counts your countries and how much of the world
that is, then every stamp you have collected, newest year first. Each stamp is
drawn at run time from a seed stored with it: shape, ink colour, rotation, wear
and the wording all vary, so no two look alike and any one stamp looks the same
forever.

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
data/country/    the offline place catalogue (238 places, 196 sovereign)
data/world/      reader for the packed country outlines
data/local/      Room entities, DAOs, database
data/model/      domain types and the stats calculation
ui/stamp/        the stamp renderer (Canvas + native text-on-path)
ui/passport/     the book
ui/trips/        trip list, detail, editor
ui/map/          the world map
ui/add/          the stamping flow
nav/             routes and the app shell

assets/world.sbw            packed country outlines, 68 KB
tools/build_world_asset.py  rebuilds that asset from Natural Earth
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

**Why stamps are generated, not drawn.** Shipping artwork for 200 countries is
not realistic, and a list of rows is not something anyone screenshots. Deriving
the look from a stored seed means the passport feels hand-collected while the
data stays a plain table.

## Data

Country outlines are Natural Earth 1:50m, in the public domain, taken from the
`world-atlas` package; the numeric-to-ISO mapping comes from `world-countries`.
Neither is a runtime dependency — they are inputs to
`tools/build_world_asset.py`, which is run by hand when the source data changes:

```
npm pack world-atlas@2.0.2 world-countries@5.0.0
python3 tools/build_world_asset.py <extracted-dir>
```

## Not built yet

- Photos and journal entries attached to stamps
- "Stamp where I am now" using GPS
- Export and backup to a file, and sync across devices
- Sharing a stamp or the map as an image
