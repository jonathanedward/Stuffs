package com.stampbook.app.ui.map

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.stampbook.app.core.Projection
import com.stampbook.app.data.country.Countries
import com.stampbook.app.data.country.Country
import com.stampbook.app.data.world.CountryShape
import com.stampbook.app.data.world.WorldShapes
import kotlin.math.hypot

/** An ordered run of places from one trip, drawn as a route across the map. */
data class MapRoute(val stops: List<Country>)

private const val MAX_ZOOM = 8f

/**
 * The world with its borders drawn, countries you have stamped filled in, and each
 * trip traced through its stops. Outlines come from the offline world.sbw asset, so
 * the map works with no network and no tile server.
 */
@Composable
fun WorldChoroplethMap(
    visitedCodes: Set<String>,
    routes: List<MapRoute>,
    ocean: Color,
    land: Color,
    visited: Color,
    border: Color,
    route: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shapes by produceState(initialValue = emptyList<CountryShape>(), context) {
        value = WorldShapes.load(context)
    }

    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    zoom = (zoom * zoomChange).coerceIn(1f, MAX_ZOOM)
                    val limitX = size.width * (zoom - 1f) / 2f
                    val limitY = size.height * (zoom - 1f) / 2f
                    pan = Offset(
                        (pan.x + panChange.x * zoom).coerceIn(-limitX, limitX),
                        (pan.y + panChange.y * zoom).coerceIn(-limitY, limitY),
                    )
                }
            }
            // Outlines are rebuilt only when the map is resized or the data changes,
            // never on a pan or a pinch.
            .drawWithCache {
                val outlines = shapes.map { shape ->
                    Outline(
                        path = shape.toPath(size),
                        wraps = shape.wraps,
                        visited = shape.code in visitedCodes,
                    )
                }
                // Countries too small to have an outline at this resolution still
                // deserve to light up when you stamp them.
                val outlined = shapes.mapTo(HashSet()) { it.code }
                val dots = visitedCodes
                    .filter { it !in outlined }
                    .mapNotNull { Countries[it] }

                onDrawBehind {
                    drawRect(ocean)
                    withTransform({
                        translate(pan.x, pan.y)
                        scale(zoom, zoom)
                    }) {
                        val hairline = (size.minDimension * 0.0018f / zoom).coerceAtLeast(0.35f)
                        outlines.forEach { outline ->
                            outline.eachCopy(size.width) { shift ->
                                translate(left = shift) {
                                    drawPath(outline.path, if (outline.visited) visited else land)
                                    drawPath(outline.path, border, style = Stroke(hairline))
                                }
                            }
                        }
                        drawRoutes(routes, route, zoom)
                        dots.forEach { country ->
                            val point = Offset(
                                Projection.xFraction(country.longitude) * size.width,
                                Projection.yFraction(country.latitude) * size.height,
                            )
                            drawCircle(visited, size.minDimension * 0.007f / zoom, point)
                        }
                    }
                }
            },
    )
}

private class Outline(val path: Path, val wraps: Boolean, val visited: Boolean) {
    /** Draws once normally, and again a map-width over when the outline runs off an edge. */
    inline fun eachCopy(width: Float, block: (Float) -> Unit) {
        block(0f)
        if (wraps) {
            block(width)
            block(-width)
        }
    }
}

private fun CountryShape.toPath(size: Size): Path = Path().apply {
    // Even-odd so a ring inside another ring reads as a hole, which is how
    // Lesotho stays visible inside South Africa.
    fillType = PathFillType.EvenOdd
    rings.forEach { ring ->
        moveTo(ring[0] * size.width, ring[1] * size.height)
        for (i in 1 until ring.size / 2) {
            lineTo(ring[i * 2] * size.width, ring[i * 2 + 1] * size.height)
        }
        close()
    }
}

private fun DrawScope.drawRoutes(routes: List<MapRoute>, routeColor: Color, zoom: Float) {
    val stroke = Stroke(width = (size.minDimension * 0.005f / zoom).coerceAtLeast(0.6f))
    val ink = routeColor.copy(alpha = 0.75f)
    routes.forEach { mapRoute ->
        mapRoute.stops.zipWithNext { from, to ->
            if (from.code == to.code) return@zipWithNext
            val start = Offset(
                Projection.xFraction(from.longitude) * size.width,
                Projection.yFraction(from.latitude) * size.height,
            )
            val deltaX =
                (Projection.shortestLongitudeDelta(from.longitude, to.longitude) / 360.0).toFloat() * size.width
            val end = Offset(start.x + deltaX, Projection.yFraction(to.latitude) * size.height)
            val path = arcBetween(start, end)
            drawPath(path, ink, style = stroke)
            // The shorter way round can leave the canvas; redraw it entering from the far edge.
            if (end.x < 0f) {
                translate(left = size.width) { drawPath(path, ink, style = stroke) }
            } else if (end.x > size.width) {
                translate(left = -size.width) { drawPath(path, ink, style = stroke) }
            }
        }
    }
}

/** A gentle bow, so overlapping routes stay tellable apart. */
private fun arcBetween(start: Offset, end: Offset): Path {
    val lift = hypot(end.x - start.x, end.y - start.y) * 0.16f
    return Path().apply {
        moveTo(start.x, start.y)
        quadraticTo((start.x + end.x) / 2f, (start.y + end.y) / 2f - lift, end.x, end.y)
    }
}
