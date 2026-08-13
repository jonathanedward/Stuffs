#!/usr/bin/env python3
"""Turn Natural Earth 110m boundaries into the compact asset the app ships.

Input is the TopoJSON from the `world-atlas` package (Natural Earth 1:50m,
public domain) and the ISO code table from `world-countries`:

    npm pack world-atlas@2.0.2 world-countries@5.0.0

Output is app/src/main/assets/world.sbw — see FORMAT below. Regenerating is
only needed if the source data or the quantisation changes.

FORMAT (little-endian):
    "SBW1"              magic
    u16                 country count
    per country:
        2 bytes         ISO 3166-1 alpha-2, or "??" when the boundary has no
                        entry in the app's catalogue (drawn, never highlighted)
        u16             ring count
        per ring:
            u16         point count
            varint      zig-zag first x, first y
            varint*     zig-zag delta x, delta y for each later point

    x = round((longitude + 180) * 100), nominally 0..36000
    y = round((90 - latitude) * 100),   0..18000

Rings that cross the antimeridian (Russia, Fiji, Antarctica) are unwrapped
rather than split, so their x runs past 0 or 36000 and stays continuous. The
renderer draws such a country a second time, shifted by a map width, which is
what makes eastern Russia appear on the left-hand edge.
"""

import json
import struct
import sys
from pathlib import Path

SCALE = 100      # hundredths of a degree, about 1.1 km at the equator
TOLERANCE = 0.06  # Douglas-Peucker tolerance in degrees, under a pixel on a phone
MIN_RING = 0.09   # drop islets smaller than this bounding box, in degrees


def varint(value):
    out = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out.append(byte | 0x80)
        else:
            out.append(byte)
            return bytes(out)


def zigzag(value):
    return (value << 1) ^ (value >> 31)


def unwrap(points):
    """Keep longitudes continuous across the antimeridian instead of snapping
    back across the whole map, which would draw a stripe through every latitude."""
    out = [points[0]]
    for lon, lat in points[1:]:
        previous = out[-1][0]
        while lon - previous > 180:
            lon -= 360
        while lon - previous < -180:
            lon += 360
        out.append((lon, lat))
    return out


def area(points):
    """Twice the signed shoelace area; used only to drop degenerate rings."""
    total = 0.0
    for i in range(len(points)):
        x0, y0 = points[i]
        x1, y1 = points[(i + 1) % len(points)]
        total += x0 * y1 - x1 * y0
    return abs(total)


def simplify(points, tolerance):
    """Douglas-Peucker. The 50m source carries far more detail than a phone-width
    map can show, and every dropped point is a point not walked each frame."""
    if len(points) < 3:
        return points
    keep = [False] * len(points)
    keep[0] = keep[-1] = True
    stack = [(0, len(points) - 1)]
    while stack:
        first, last = stack.pop()
        if last <= first + 1:
            continue
        ax, ay = points[first]
        bx, by = points[last]
        dx, dy = bx - ax, by - ay
        span = dx * dx + dy * dy
        worst, at = -1.0, first
        for i in range(first + 1, last):
            px, py = points[i]
            if span == 0:
                d = (px - ax) ** 2 + (py - ay) ** 2
            else:
                t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / span))
                d = (px - ax - t * dx) ** 2 + (py - ay - t * dy) ** 2
            if d > worst:
                worst, at = d, i
        if worst > tolerance * tolerance:
            keep[at] = True
            stack.append((first, at))
            stack.append((at, last))
    return [p for p, k in zip(points, keep) if k]


def ring_extent(points):
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    return max(max(xs) - min(xs), max(ys) - min(ys))


def decode_arcs(topology):
    """TopoJSON arcs are quantised and delta-encoded; expand them to lon/lat."""
    sx, sy = topology["transform"]["scale"]
    tx, ty = topology["transform"]["translate"]
    arcs = []
    for arc in topology["arcs"]:
        x = y = 0
        points = []
        for dx, dy in arc:
            x += dx
            y += dy
            points.append((x * sx + tx, y * sy + ty))
        arcs.append(points)
    return arcs


def ring_points(arcs, indices):
    """Stitch arc indices into one closed ring; a negative index means reversed."""
    points = []
    for index in indices:
        arc = arcs[~index][::-1] if index < 0 else arcs[index]
        points.extend(arc[1:] if points else arc)
    return points


def quantise(points):
    seen = []
    for lon, lat in points:
        x = round((lon + 180) * SCALE)
        y = min(max(round((90 - lat) * SCALE), 0), 180 * SCALE)
        if not seen or seen[-1] != (x, y):
            seen.append((x, y))
    return seen


def encode_ring(points):
    out = bytearray()
    out += struct.pack("<H", len(points))
    px, py = points[0]
    out += varint(zigzag(px)) + varint(zigzag(py))
    for x, y in points[1:]:
        out += varint(zigzag(x - px)) + varint(zigzag(y - py))
        px, py = x, y
    return bytes(out)


def main():
    here = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
    topology = json.loads((here / "package/countries-50m.json").read_text())
    catalogue = json.loads((here / "package/world-countries.json").read_text()) \
        if (here / "package/world-countries.json").exists() \
        else json.loads((here / "world-countries/countries.json").read_text())

    by_numeric = {c["ccn3"]: c["cca2"] for c in catalogue if c.get("ccn3")}
    by_name = {}
    for c in catalogue:
        by_name[c["name"]["common"].lower()] = c["cca2"]
        by_name[c["name"]["official"].lower()] = c["cca2"]
        for alt in c.get("altSpellings", []):
            by_name.setdefault(alt.lower(), c["cca2"])

    arcs = decode_arcs(topology)
    shapes = []
    unmatched = []
    for geometry in topology["objects"]["countries"]["geometries"]:
        name = geometry["properties"]["name"]
        code = by_numeric.get(str(geometry.get("id"))) or by_name.get(name.lower())
        if not code:
            unmatched.append(name)
            code = "??"

        polygons = geometry["arcs"] if geometry["type"] == "MultiPolygon" else [geometry["arcs"]]
        candidates = []
        for polygon in polygons:
            for ring in polygon:
                raw = unwrap(ring_points(arcs, ring))
                # Antarctica carries a ring pinned along latitude -90 that encloses
                # nothing; drawn, it is just a rule across the foot of the map.
                if area(raw) < 1e-6:
                    continue
                candidates.append((ring_extent(raw), raw))
        # Always keep a country's largest ring, however small the country is, so
        # Monaco and Singapore still light up when stamped.
        candidates.sort(key=lambda c: c[0], reverse=True)
        rings = []
        for index, (extent, raw) in enumerate(candidates):
            if index > 0 and extent < MIN_RING:
                continue
            points = quantise(simplify(raw, TOLERANCE))
            if len(points) >= 3:
                rings.append(points)
        if rings:
            shapes.append((code, rings))

    blob = bytearray(b"SBW1")
    blob += struct.pack("<H", len(shapes))
    for code, rings in shapes:
        blob += code.encode("ascii")[:2].ljust(2, b"?")
        blob += struct.pack("<H", len(rings))
        for ring in rings:
            blob += encode_ring(ring)

    out = Path("app/src/main/assets/world.sbw")
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(blob)

    points = sum(len(r) for _, rings in shapes for r in rings)
    print(f"{len(shapes)} boundaries, {points} points, {len(blob)} bytes -> {out}")
    if unmatched:
        print("no ISO code (drawn but never highlighted):", ", ".join(sorted(unmatched)))


if __name__ == "__main__":
    main()
