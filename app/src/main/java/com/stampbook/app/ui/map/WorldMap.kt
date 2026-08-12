package com.stampbook.app.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.stampbook.app.core.Projection
import com.stampbook.app.data.country.Countries
import com.stampbook.app.data.country.Country
import kotlin.math.abs
import kotlin.math.hypot

/** An ordered run of places from one trip, drawn as a route across the map. */
data class MapRoute(val stops: List<Country>)

/**
 * A constellation map: every country in the catalogue is a dot, the ones you have
 * stamped burn brighter, and each trip is drawn as an arc between its stops.
 * Deliberately not a choropleth — no country polygons ship with the app.
 */
@Composable
fun WorldConstellationMap(
    visitedCodes: Set<String>,
    routes: List<MapRoute>,
    modifier: Modifier = Modifier,
    dotColor: Color = Color.Gray,
    visitedColor: Color = Color(0xFFB08D3F),
    routeColor: Color = Color(0xFF1B3A6B),
    graticuleColor: Color = Color.Gray,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    // Keep the map from being dragged entirely off screen.
                    val maxX = size.width * (scale - 1f) / 2f
                    val maxY = size.height * (scale - 1f) / 2f
                    offset = Offset(
                        (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                        (offset.y + pan.y * scale).coerceIn(-maxY, maxY),
                    )
                }
            },
    ) {
        drawGraticule(graticuleColor)
        drawRoutes(routes, routeColor)
        drawCountryDots(visitedCodes, dotColor, visitedColor)
    }
}

private fun DrawScope.pointFor(country: Country): Offset = Offset(
    Projection.xFraction(country.longitude) * size.width,
    Projection.yFraction(country.latitude) * size.height,
)

private fun DrawScope.drawGraticule(color: Color) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(3f, 9f))
    val faint = color.copy(alpha = 0.22f)
    // Meridians every 30 degrees, parallels every 30 degrees, plus a solid equator.
    for (lon in -150..150 step 30) {
        val x = Projection.xFraction(lon.toDouble()) * size.width
        drawLine(faint, Offset(x, 0f), Offset(x, size.height), 1f, pathEffect = dash)
    }
    for (lat in -60..60 step 30) {
        val y = Projection.yFraction(lat.toDouble()) * size.height
        val isEquator = lat == 0
        drawLine(
            if (isEquator) color.copy(alpha = 0.4f) else faint,
            Offset(0f, y),
            Offset(size.width, y),
            if (isEquator) 1.2f else 1f,
            pathEffect = if (isEquator) null else dash,
        )
    }
}

private fun DrawScope.drawCountryDots(visitedCodes: Set<String>, dotColor: Color, visitedColor: Color) {
    val base = size.minDimension * 0.006f
    Countries.all.forEach { country ->
        val point = pointFor(country)
        if (country.code in visitedCodes) {
            drawCircle(visitedColor.copy(alpha = 0.22f), base * 3.2f, point)
            drawCircle(visitedColor, base * 1.5f, point)
        } else {
            drawCircle(dotColor.copy(alpha = 0.35f), base * 0.75f, point)
        }
    }
}

private fun DrawScope.drawRoutes(routes: List<MapRoute>, routeColor: Color) {
    val stroke = Stroke(width = size.minDimension * 0.004f)
    routes.forEach { route ->
        route.stops.zipWithNext { from, to ->
            if (from.code == to.code) return@zipWithNext
            val start = pointFor(from)
            val deltaX = (Projection.shortestLongitudeDelta(from.longitude, to.longitude) / 360.0).toFloat() * size.width
            val end = Offset(start.x + deltaX, pointFor(to).y)
            val path = arcBetween(start, end)
            drawPath(path, routeColor.copy(alpha = 0.55f), style = stroke)
            // The shorter way round can leave the canvas; redraw it entering from the far edge.
            if (end.x < 0f) {
                translate(left = size.width) { drawPath(path, routeColor.copy(alpha = 0.55f), style = stroke) }
            } else if (end.x > size.width) {
                translate(left = -size.width) { drawPath(path, routeColor.copy(alpha = 0.55f), style = stroke) }
            }
        }
    }
}

/** A gentle bow, so overlapping routes stay tellable apart. */
private fun arcBetween(start: Offset, end: Offset): Path {
    val midX = (start.x + end.x) / 2f
    val midY = (start.y + end.y) / 2f
    val distance = hypot(end.x - start.x, end.y - start.y)
    val lift = (distance * 0.16f).coerceAtMost(abs(distance) + 1f)
    return Path().apply {
        moveTo(start.x, start.y)
        quadraticTo(midX, midY - lift, end.x, end.y)
    }
}
