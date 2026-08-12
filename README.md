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

**Map** — every country in the catalogue is a dot, the ones you have stamped
burn brighter, and each trip is drawn as an arc through its stops in order.
Pinch to zoom, drag to pan. Underneath: your share of the world, and progress
per continent.

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
data/country/    the offline place catalogue (212 places, 196 sovereign)
data/local/      Room entities, DAOs, database
data/model/      domain types and the stats calculation
ui/stamp/        the stamp renderer (Canvas + native text-on-path)
ui/passport/     the book
ui/trips/        trip list, detail, editor
ui/map/          the constellation map
ui/add/          the stamping flow
nav/             routes and the app shell
```

Flag emoji are derived from ISO 3166-1 alpha-2 codes as regional indicator
pairs rather than stored, so the catalogue stays a plain table of code, name,
continent and centroid.

Country counts use 196 sovereign states as the denominator (193 UN members plus
Palestine, Vatican City and Kosovo). Territories such as Hong Kong, Puerto Rico
and Greenland can be stamped but are counted separately, so visiting Guam does
not claim a country you have not been to.

## Design notes

**Why a dot map and not a filled-in choropleth.** Colouring country shapes needs
country polygons — several megabytes of boundary data, or a network map tile
service. Both fight the offline-first goal. Dots at country centroids plus route
arcs give you the same "look how far I have been" read at a fraction of the
size, and the routes are something a choropleth cannot show at all. Swapping in
real polygons later only touches `ui/map/WorldMap.kt`; nothing else knows the
map's shape.

**Why stamps are generated, not drawn.** Shipping artwork for 200 countries is
not realistic, and a list of rows is not something anyone screenshots. Deriving
the look from a stored seed means the passport feels hand-collected while the
data stays a plain table.

## Not built yet

- Photos and journal entries attached to stamps
- "Stamp where I am now" using GPS
- Export and backup to a file, and sync across devices
- Sharing a stamp or the map as an image
