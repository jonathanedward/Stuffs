package com.stampbook.app.ui.stamp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stampbook.app.core.StampShape
import com.stampbook.app.core.StampStyle
import com.stampbook.app.core.StampStyles
import com.stampbook.app.data.model.Stamp
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import android.graphics.Paint as NativePaint
import android.graphics.Path as NativePath
import android.graphics.RectF
import android.graphics.Typeface

private val STAMP_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

/** The text a stamp carries, arranged so nothing is printed twice. */
private data class StampText(
    val top: String,
    val center: String,
    val date: String,
    val bottom: String,
)

private fun Stamp.text(style: StampStyle): StampText {
    val hasCity = !city.isNullOrBlank()
    val countryName = country?.name ?: countryCode
    return StampText(
        top = if (hasCity) countryName.uppercase(Locale.ENGLISH) else style.label,
        center = headline.uppercase(Locale.ENGLISH),
        date = date.format(STAMP_DATE).uppercase(Locale.ENGLISH),
        bottom = if (hasCity) style.label else "",
    )
}

/**
 * Draws one stamp: shape, border, arced country name, place, date. Everything is
 * derived from [Stamp.seed], so the same stamp always prints the same way.
 */
@Composable
fun StampMark(
    stamp: Stamp,
    modifier: Modifier = Modifier,
    stampSize: Dp = 132.dp,
    rotate: Boolean = true,
) {
    val style = remember(stamp.seed) { StampStyles.forSeed(stamp.seed) }
    val text = remember(stamp.id, stamp.city, stamp.countryCode, stamp.date, style) { stamp.text(style) }
    val ink = Color(style.inkArgb)

    Canvas(
        modifier = modifier
            .size(stampSize)
            .graphicsLayer {
                if (rotate) rotationZ = style.rotationDegrees
                alpha = style.alpha
                // Required so the ink-wear pass below can punch holes in the stamp
                // instead of clearing whatever is painted behind it.
                compositingStrategy = CompositingStrategy.Offscreen
            },
    ) {
        drawStamp(style, ink, text)
        drawInkWear(style)
    }
}

private fun DrawScope.drawStamp(style: StampStyle, ink: Color, text: StampText) {
    val extent = min(size.width, size.height)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = extent * 0.46f
    val stroke = (extent * 0.024f).coerceAtLeast(1.5f)
    val center = Offset(cx, cy)

    when (style.shape) {
        StampShape.CIRCLE -> {
            drawCircle(ink, radius, center, style = Stroke(stroke))
            if (style.doubleBorder) {
                drawCircle(ink, radius * 0.87f, center, style = Stroke(stroke * 0.45f))
            }
        }

        StampShape.SCALLOP -> {
            drawPath(scallopPath(center, radius, waves = 22, depth = radius * 0.05f), ink, style = Stroke(stroke))
            drawCircle(ink, radius * 0.8f, center, style = Stroke(stroke * 0.45f))
        }

        StampShape.RECTANGLE -> {
            val halfW = radius * 0.99f
            val halfH = radius * 0.72f
            drawRoundedBox(center, halfW, halfH, ink, stroke)
            if (style.doubleBorder) {
                drawRoundedBox(center, halfW * 0.9f, halfH * 0.86f, ink, stroke * 0.45f)
            }
        }

        StampShape.HEXAGON -> {
            drawPath(polygonPath(center, radius, sides = 6, rotationDegrees = 90f), ink, style = Stroke(stroke))
            if (style.doubleBorder) {
                drawPath(
                    polygonPath(center, radius * 0.85f, sides = 6, rotationDegrees = 90f),
                    ink,
                    style = Stroke(stroke * 0.45f),
                )
            }
        }
    }

    val curvedHeader = style.shape == StampShape.CIRCLE || style.shape == StampShape.SCALLOP
    val ceiling = if (curvedHeader) radius * 0.74f else radius * 0.46f

    // Header: hugs the ring on round stamps, sits flat on angular ones.
    if (text.top.isNotEmpty()) {
        val paint = inkPaint(ink, extent * 0.085f, bold = true, spacing = 0.14f)
        if (curvedHeader) {
            val arcRadius = radius * 0.74f
            fitToWidth(paint, text.top, (PI * arcRadius * 0.86f).toFloat(), extent * 0.048f)
            val arc = NativePath().apply {
                addArc(RectF(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius), 180f, 180f)
            }
            drawIntoCanvas { it.nativeCanvas.drawTextOnPath(text.top, arc, 0f, 0f, paint) }
        } else {
            fitToWidth(paint, text.top, radius * 1.6f, extent * 0.048f)
            drawNativeText(text.top, cx, cy - ceiling, paint)
        }
    }

    // Place name: the loudest line on the stamp.
    val centerPaint = inkPaint(ink, extent * 0.155f, bold = true, spacing = 0.02f)
    fitToWidth(centerPaint, text.center, radius * 1.5f, extent * 0.07f)
    drawNativeText(text.center, cx, cy + extent * 0.03f, centerPaint)

    val datePaint = inkPaint(ink, extent * 0.082f, bold = false, spacing = 0.1f)
    fitToWidth(datePaint, text.date, radius * 1.4f, extent * 0.05f)
    drawNativeText(text.date, cx, cy + extent * 0.155f, datePaint)

    if (text.bottom.isNotEmpty()) {
        val bottomPaint = inkPaint(ink, extent * 0.06f, bold = false, spacing = 0.2f)
        fitToWidth(bottomPaint, text.bottom, radius * 1.2f, extent * 0.04f)
        drawNativeText(text.bottom, cx, cy + style.shape.footerOffset() * radius, bottomPaint)
    }

    // Little stars flanking the place name, like a real border stamp. They start
    // outside whatever width the name ended up needing, and are dropped rather than
    // drawn over the border once they run out of room.
    val starRadius = extent * 0.022f
    val halfName = centerPaint.measureText(text.center) / 2f
    val starLimit = style.shape.sideRoom() * radius
    repeat(style.starCount) { index ->
        val offsetX = halfName + extent * 0.055f + index * extent * 0.05f
        if (offsetX + starRadius > starLimit) return@repeat
        listOf(cx - offsetX, cx + offsetX).forEach { x ->
            drawPath(starPath(Offset(x, cy), starRadius, starRadius * 0.42f), ink)
        }
    }
}

/** Speckles of missing ink so the stamp reads as pressed, not printed. */
private fun DrawScope.drawInkWear(style: StampStyle) {
    val random = Random(style.hashCode())
    val extent = min(size.width, size.height)
    repeat(48) {
        drawCircle(
            color = Color.Transparent,
            radius = extent * random.nextDouble(0.004, 0.022).toFloat(),
            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height),
            blendMode = BlendMode.Clear,
        )
    }
}

/** How far down the footer line can sit before it meets the outline, as a fraction of the radius. */
private fun StampShape.footerOffset(): Float = when (this) {
    StampShape.CIRCLE -> 0.72f
    StampShape.SCALLOP -> 0.64f
    StampShape.RECTANGLE -> 0.52f
    StampShape.HEXAGON -> 0.58f
}

/** Half-width available on the centre line, as a fraction of the radius. */
private fun StampShape.sideRoom(): Float = when (this) {
    StampShape.CIRCLE -> 0.88f
    StampShape.SCALLOP -> 0.78f
    StampShape.RECTANGLE -> 0.86f
    StampShape.HEXAGON -> 0.74f
}

private fun DrawScope.drawRoundedBox(center: Offset, halfW: Float, halfH: Float, ink: Color, stroke: Float) {
    drawRoundRect(
        color = ink,
        topLeft = Offset(center.x - halfW, center.y - halfH),
        size = Size(halfW * 2, halfH * 2),
        cornerRadius = CornerRadius(halfW * 0.09f),
        style = Stroke(stroke),
    )
}

private fun DrawScope.drawNativeText(text: String, x: Float, y: Float, paint: NativePaint) {
    drawIntoCanvas { it.nativeCanvas.drawText(text, x, y, paint) }
}

private fun inkPaint(ink: Color, textSize: Float, bold: Boolean, spacing: Float): NativePaint =
    NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        color = ink.toArgb()
        this.textSize = textSize
        letterSpacing = spacing
        textAlign = NativePaint.Align.CENTER
        typeface = Typeface.create(Typeface.SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

/** Shrinks the paint until the text fits, so long country names never overflow the ring. */
private fun fitToWidth(paint: NativePaint, text: String, maxWidth: Float, minSize: Float) {
    while (paint.textSize > minSize && paint.measureText(text) > maxWidth) {
        paint.textSize -= paint.textSize * 0.06f
    }
}

private fun polygonPath(center: Offset, radius: Float, sides: Int, rotationDegrees: Float): Path {
    val path = Path()
    val start = rotationDegrees * PI.toFloat() / 180f
    repeat(sides) { i ->
        val angle = start + i * 2f * PI.toFloat() / sides
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun scallopPath(center: Offset, radius: Float, waves: Int, depth: Float): Path {
    val path = Path()
    val steps = waves * 8
    repeat(steps + 1) { i ->
        val t = i.toFloat() / steps
        val angle = t * 2f * PI.toFloat()
        val r = radius + depth * sin(angle * waves)
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun starPath(center: Offset, outer: Float, inner: Float, points: Int = 5): Path {
    val path = Path()
    repeat(points * 2) { i ->
        val r = if (i % 2 == 0) outer else inner
        val angle = (-PI / 2 + i * PI / points).toFloat()
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
