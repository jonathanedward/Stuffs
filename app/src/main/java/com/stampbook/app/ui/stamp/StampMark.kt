package com.stampbook.app.ui.stamp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stampbook.app.core.BorderStyle
import com.stampbook.app.core.StampDesign
import com.stampbook.app.core.StampDevice
import com.stampbook.app.core.StampImpression
import com.stampbook.app.core.StampLayout
import com.stampbook.app.core.StampShape
import com.stampbook.app.core.StampStyles
import com.stampbook.app.data.model.Stamp
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import android.graphics.Paint as NativePaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface

private val STAMP_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

/** The words this stamp carries, arranged so nothing is printed twice. */
private class Copy(
    val country: String,
    val place: String,
    val date: String,
    val label: String,
    val serial: String,
    val hasCity: Boolean,
)

private fun Stamp.copy(design: StampDesign, impression: StampImpression): Copy {
    val hasCity = !city.isNullOrBlank()
    val countryName = (country?.name ?: countryCode).uppercase(Locale.ENGLISH)
    return Copy(
        // With no city the place line already says the country, so the slot that
        // would repeat it carries the authority's wording instead.
        country = if (hasCity) countryName else design.label,
        place = headline.uppercase(Locale.ENGLISH),
        date = date.format(STAMP_DATE).uppercase(Locale.ENGLISH),
        label = if (hasCity) design.label else "",
        serial = "No ${impression.serial}",
        hasCity = hasCity,
    )
}

/**
 * Draws one stamp. The design comes from the country, so every Japanese stamp is
 * the same stamp; the seed decides only how this particular pressing came out.
 */
@Composable
fun StampMark(
    stamp: Stamp,
    modifier: Modifier = Modifier,
    stampSize: Dp = 132.dp,
    rotate: Boolean = true,
) {
    val design = remember(stamp.countryCode) { StampStyles.forCountry(stamp.countryCode) }
    val impression = remember(stamp.seed) { StampStyles.forSeed(stamp.seed) }
    val copy = remember(stamp.id, stamp.city, stamp.countryCode, stamp.date, design, impression) {
        stamp.copy(design, impression)
    }
    val ink = Color(design.inkArgb)

    Canvas(
        modifier = modifier
            .size(stampSize)
            .graphicsLayer {
                if (rotate) rotationZ = impression.rotationDegrees
                alpha = impression.alpha
                // Required so the knocked-out text and the ink-wear pass punch
                // through the stamp instead of clearing what is behind it.
                compositingStrategy = CompositingStrategy.Offscreen
            },
    ) {
        drawStamp(design, impression, ink, copy)
    }
}

private fun DrawScope.drawStamp(
    design: StampDesign,
    impression: StampImpression,
    ink: Color,
    copy: Copy,
) {
    val extent = min(size.width, size.height)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = extent * 0.46f
    val stroke = (extent * 0.024f).coerceAtLeast(1.5f)
    val outline = design.shape.path(cx, cy, radius)

    drawBorder(design, outline, ink, stroke, cx, cy, radius)

    val frame = Frame(design, ink, cx, cy, radius, extent, outline)
    when (design.layout) {
        StampLayout.ARCH -> frame.arch(this, copy)
        StampLayout.DATE_ARCH -> frame.dateArch(this, copy)
        StampLayout.STACKED -> frame.stacked(this, copy)
        StampLayout.BAND -> frame.band(this, copy)
        StampLayout.SPLIT -> frame.split(this, copy)
        StampLayout.FORM -> frame.form(this, copy)
    }

    if (design.cornerTicks) {
        drawCornerTicks(design.shape, design.border.contentInset, ink, cx, cy, radius, extent, stroke)
    }
    drawInkWear(impression.wearSeed)
}

// ---------------------------------------------------------------- outlines

private fun StampShape.path(cx: Float, cy: Float, r: Float): Path = when (this) {
    StampShape.CIRCLE -> Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }
    StampShape.OVAL -> Path().apply {
        addOval(Rect(cx - r, cy - r * OVAL_RATIO, cx + r, cy + r * OVAL_RATIO))
    }
    StampShape.SCALLOP -> scallopPath(cx, cy, r, waves = 22, depth = r * 0.05f)
    StampShape.OCTAGON -> polygonPath(cx, cy, r, sides = 8, rotationDegrees = 22.5f)
    StampShape.HEXAGON -> polygonPath(cx, cy, r, sides = 6, rotationDegrees = 0f)
    StampShape.RECTANGLE -> Path().apply {
        addRect(Rect(cx - r * 0.99f, cy - r * 0.70f, cx + r * 0.99f, cy + r * 0.70f))
    }
    StampShape.ROUNDED_RECT -> Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                Rect(cx - r * 0.96f, cy - r * 0.72f, cx + r * 0.96f, cy + r * 0.72f),
                CornerRadius(r * 0.26f),
            ),
        )
    }
    StampShape.SHIELD -> shieldPath(cx, cy, r)
}

private const val OVAL_RATIO = 0.70f

/** Half the room available on a horizontal line, used to fit every line of text. */
private fun StampShape.halfWidthAt(r: Float, y: Float): Float {
    val dy = abs(y)
    return when (this) {
        StampShape.CIRCLE -> if (dy >= r) 0f else sqrt(r * r - dy * dy)
        StampShape.SCALLOP -> if (dy >= r) 0f else sqrt(r * r - dy * dy) * 0.94f
        StampShape.OCTAGON -> if (dy >= r) 0f else sqrt(r * r - dy * dy) * 0.92f
        StampShape.OVAL -> {
            val ry = r * OVAL_RATIO
            if (dy >= ry) 0f else r * sqrt(1f - (dy / ry) * (dy / ry))
        }
        // Flat top and bottom, points left and right: the room falls off linearly.
        StampShape.HEXAGON -> (r - dy / 0.866f * 0.5f).coerceAtLeast(0f)
        StampShape.RECTANGLE -> if (dy > r * 0.70f) 0f else r * 0.99f
        StampShape.ROUNDED_RECT -> if (dy > r * 0.72f) 0f else r * 0.96f
        StampShape.SHIELD -> when {
            y <= r * 0.20f -> r * 0.82f
            y >= r * 0.98f -> 0f
            else -> r * 0.82f * (1f - (y - r * 0.20f) / (r * 0.78f))
        }
    }
}

private fun polygonPath(cx: Float, cy: Float, r: Float, sides: Int, rotationDegrees: Float) =
    Path().apply {
        val start = rotationDegrees * PI.toFloat() / 180f
        repeat(sides) { i ->
            val angle = start + i * 2f * PI.toFloat() / sides
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

private fun scallopPath(cx: Float, cy: Float, r: Float, waves: Int, depth: Float) = Path().apply {
    val steps = waves * 8
    repeat(steps + 1) { i ->
        val t = i.toFloat() / steps
        val angle = t * 2f * PI.toFloat()
        val rr = r + depth * sin(angle * waves)
        val x = cx + rr * cos(angle)
        val y = cy + rr * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun shieldPath(cx: Float, cy: Float, r: Float) = Path().apply {
    val halfW = r * 0.86f
    val top = cy - r * 0.74f
    val shoulder = cy + r * 0.20f
    val point = cy + r * 0.98f
    val corner = r * 0.18f
    moveTo(cx - halfW + corner, top)
    lineTo(cx + halfW - corner, top)
    quadraticTo(cx + halfW, top, cx + halfW, top + corner)
    lineTo(cx + halfW, shoulder)
    quadraticTo(cx + halfW * 0.86f, point, cx, point)
    quadraticTo(cx - halfW * 0.86f, point, cx - halfW, shoulder)
    lineTo(cx - halfW, top + corner)
    quadraticTo(cx - halfW, top, cx - halfW + corner, top)
    close()
}

// ---------------------------------------------------------------- borders

private fun DrawScope.drawBorder(
    design: StampDesign,
    outline: Path,
    ink: Color,
    stroke: Float,
    cx: Float,
    cy: Float,
    radius: Float,
) {
    when (design.border) {
        BorderStyle.SINGLE -> drawPath(outline, ink, style = Stroke(stroke))

        BorderStyle.DOUBLE -> {
            drawPath(outline, ink, style = Stroke(stroke))
            drawPath(design.shape.path(cx, cy, radius * 0.88f), ink, style = Stroke(stroke * 0.5f))
        }

        BorderStyle.DASHED -> drawPath(
            outline,
            ink,
            style = Stroke(
                width = stroke,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(radius * 0.14f, radius * 0.07f),
                ),
            ),
        )

        // A zero-length dash with a round cap prints as a bead.
        BorderStyle.BEADED -> {
            drawPath(outline, ink, style = Stroke(stroke * 0.4f))
            drawPath(
                design.shape.path(cx, cy, radius * 0.9f),
                ink,
                style = Stroke(
                    width = stroke * 1.5f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(0.01f, radius * 0.115f),
                    ),
                ),
            )
        }

        BorderStyle.HAIRLINE_PAIR -> {
            drawPath(outline, ink, style = Stroke(stroke * 1.35f))
            drawPath(design.shape.path(cx, cy, radius * 0.93f), ink, style = Stroke(stroke * 0.28f))
        }
    }
}

private fun DrawScope.drawCornerTicks(
    shape: StampShape,
    borderInset: Float,
    ink: Color,
    cx: Float,
    cy: Float,
    radius: Float,
    extent: Float,
    stroke: Float,
) {
    val y = radius * 0.5f
    val arm = extent * 0.055f
    listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f).forEach { (sx, sy) ->
        // Measured per corner: a shield is far narrower at the foot than the head.
        val px = cx + sx * shape.halfWidthAt(radius * borderInset, sy * y) * 0.84f
        val py = cy + sy * y
        drawLine(ink, Offset(px, py), Offset(px - sx * arm, py), stroke * 0.5f)
        drawLine(ink, Offset(px, py), Offset(px, py - sy * arm), stroke * 0.5f)
    }
}

// ---------------------------------------------------------------- layouts

/**
 * Everything a layout needs to place a line of text and know how much room it
 * has there. Sizes are fractions of the stamp so they hold at any dimension.
 */
private class Frame(
    val design: StampDesign,
    val ink: Color,
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val extent: Float,
    val outline: Path,
) {
    private val argb = ink.toArgb()

    private fun paint(scale: Float, bold: Boolean, spacing: Float) =
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = argb
            textSize = extent * scale
            letterSpacing = spacing
            textAlign = NativePaint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    /** The radius text has to live inside: the innermost line of the border. */
    private val inner = radius * design.border.contentInset

    private fun roomAt(y: Float, inset: Float = 0.82f) =
        design.shape.halfWidthAt(inner, y) * 2f * inset

    /** Shrinks the paint until the text fits the room the outline leaves there. */
    private fun fit(paint: NativePaint, text: String, maxWidth: Float, minimumSize: Float) {
        while (paint.textSize > minimumSize && paint.measureText(text) > maxWidth) {
            paint.textSize -= paint.textSize * 0.06f
        }
    }

    /** Draws a centred line at [dy] from the middle, fitted to the room there. */
    private fun DrawScope.line(
        text: String,
        dy: Float,
        scale: Float,
        bold: Boolean = false,
        spacing: Float = 0.1f,
        floor: Float = 0.042f,
        x: Float = cx,
        width: Float = roomAt(dy + if (dy < 0) -extent * scale * 0.7f else 0f),
    ): NativePaint? {
        if (text.isEmpty()) return null
        val paint = paint(scale, bold, spacing)
        fit(paint, text, width, extent * floor)
        drawIntoCanvas { it.nativeCanvas.drawText(text, x, cy + dy, paint) }
        return paint
    }

    /**
     * The biggest arc whose text still clears the top of the outline. A circle can
     * carry it near the rim; an oval is only 0.7 as tall, so its arc has to come in.
     */
    private fun arcRadius(scale: Float): Float {
        val verticalHalf = when (design.shape) {
            StampShape.OVAL -> inner * OVAL_RATIO
            StampShape.OCTAGON -> inner * 0.92f
            StampShape.SCALLOP -> inner * 0.95f
            else -> inner
        }
        return (verticalHalf - extent * scale * 1.15f).coerceIn(radius * 0.42f, radius * 0.80f)
    }

    /** Where the right-hand panel of a split layout sits, and how wide it can be. */
    private fun panel(dy: Float): Pair<Float, Float> {
        val right = design.shape.halfWidthAt(inner, dy) * 0.90f
        val left = -radius * 0.24f
        return (cx + (left + right) / 2f) to ((right - left) * 0.92f)
    }

    private fun DrawScope.arc(text: String, arcRadius: Float, scale: Float, bottom: Boolean) {
        if (text.isEmpty()) return
        val paint = paint(scale, bold = true, spacing = 0.14f)
        // The arc itself is the width limit here, not the outline.
        while (paint.textSize > extent * 0.042f &&
            paint.measureText(text) / arcRadius > PI.toFloat() * 0.86f
        ) {
            paint.textSize -= paint.textSize * 0.06f
        }
        val widths = text.map { paint.measureText(it.toString()) }
        val span = widths.sum() / arcRadius
        val direction = if (bottom) -1f else 1f
        var angle = (if (bottom) PI.toFloat() / 2f + span / 2f else -PI.toFloat() / 2f - span / 2f)
        drawIntoCanvas { canvas ->
            text.forEachIndexed { index, char ->
                val step = widths[index] / arcRadius * direction
                val mid = angle + step / 2f
                canvas.nativeCanvas.apply {
                    save()
                    translate(cx + arcRadius * cos(mid), cy + arcRadius * sin(mid))
                    rotate((mid * 180f / PI.toFloat()) + if (bottom) -90f else 90f)
                    drawText(char.toString(), 0f, 0f, paint)
                    restore()
                }
                angle += step
            }
        }
    }

    private fun DrawScope.rule(dy: Float, width: Float = roomAt(dy, 0.62f)) {
        drawLine(
            ink,
            Offset(cx - width / 2f, cy + dy),
            Offset(cx + width / 2f, cy + dy),
            extent * 0.007f,
        )
    }

    private fun DrawScope.starsAround(placePaint: NativePaint?, place: String, dy: Float) {
        if (design.stars == 0 || placePaint == null) return
        val starRadius = extent * 0.022f
        val half = placePaint.measureText(place) / 2f
        val limit = design.shape.halfWidthAt(inner, dy) * 0.9f
        repeat(design.stars) { index ->
            val offset = half + extent * 0.055f + index * extent * 0.05f
            if (offset + starRadius > limit) return
            listOf(cx - offset, cx + offset).forEach { x ->
                drawPath(starPath(Offset(x, cy + dy - extent * 0.045f), starRadius, starRadius * 0.42f), ink)
            }
        }
    }

    fun arch(scope: DrawScope, copy: Copy) = with(scope) {
        arc(copy.country, arcRadius(0.078f), 0.078f, bottom = false)
        if (design.device != StampDevice.NONE) {
            drawDevice(design.device, ink, Offset(cx, cy - extent * 0.185f), extent * 0.088f)
        }
        val place = line(copy.place, extent * 0.05f, 0.150f, bold = true, spacing = 0.02f, floor = 0.062f)
        starsAround(place, copy.place, extent * 0.05f)
        line(copy.date, extent * 0.165f, 0.078f)
        line(copy.label, extent * 0.275f, 0.055f, spacing = 0.2f)
    }

    fun dateArch(scope: DrawScope, copy: Copy) = with(scope) {
        arc(copy.date, arcRadius(0.072f), 0.072f, bottom = false)
        val place = line(copy.place, extent * 0.03f, 0.150f, bold = true, spacing = 0.02f, floor = 0.062f)
        starsAround(place, copy.place, extent * 0.03f)
        line(copy.label, extent * 0.145f, 0.055f, spacing = 0.2f)
        arc(if (copy.hasCity) copy.country else copy.place, arcRadius(0.068f), 0.068f, bottom = true)
    }

    fun stacked(scope: DrawScope, copy: Copy) = with(scope) {
        line(copy.country, -extent * 0.165f, 0.072f, bold = true, spacing = 0.14f)
        rule(-extent * 0.115f)
        val place = line(copy.place, extent * 0.045f, 0.150f, bold = true, spacing = 0.02f, floor = 0.062f)
        starsAround(place, copy.place, extent * 0.045f)
        line(copy.date, extent * 0.155f, 0.076f)
        line(copy.label, extent * 0.245f, 0.052f, spacing = 0.2f)
    }

    fun band(scope: DrawScope, copy: Copy) = with(scope) {
        val half = extent * 0.105f
        clipPath(outline) {
            drawRect(
                ink,
                topLeft = Offset(cx - radius * 1.1f, cy - half),
                size = Size(radius * 2.2f, half * 2f),
            )
        }
        // Knocked out of the band, so the paper shows through the letters.
        val knockout = paint(0.125f, bold = true, spacing = 0.03f).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        fit(knockout, copy.place, roomAt(0f, 0.78f), extent * 0.055f)
        drawIntoCanvas {
            it.nativeCanvas.drawText(copy.place, cx, cy + extent * 0.043f, knockout)
        }
        line(copy.country, -extent * 0.155f, 0.068f, bold = true, spacing = 0.14f)
        line(copy.date, extent * 0.20f, 0.072f)
        line(copy.label, extent * 0.285f, 0.05f, spacing = 0.2f)
    }

    fun split(scope: DrawScope, copy: Copy) = with(scope) {
        val divider = cx - radius * 0.24f
        val reach = design.shape.halfWidthAt(inner, 0f) * 0.55f
        drawLine(ink, Offset(divider, cy - reach), Offset(divider, cy + reach), extent * 0.008f)
        drawDevice(
            design.device,
            ink,
            Offset((divider + cx - design.shape.halfWidthAt(inner, 0f) * 0.9f) / 2f, cy),
            extent * 0.105f,
        )
        listOf(
            Triple(copy.country, -extent * 0.105f, 0.058f),
            Triple(copy.place, extent * 0.035f, 0.105f),
            Triple(copy.date, extent * 0.135f, 0.058f),
            Triple(copy.serial, extent * 0.215f, 0.046f),
        ).forEachIndexed { index, (text, dy, scale) ->
            val (x, width) = panel(dy)
            line(
                text = text,
                dy = dy,
                scale = scale,
                bold = index <= 1,
                spacing = if (index == 1) 0.02f else if (index == 0) 0.12f else 0.08f,
                floor = 0.042f,
                x = x,
                width = width,
            )
        }
    }

    fun form(scope: DrawScope, copy: Copy) = with(scope) {
        line(copy.country, -extent * 0.185f, 0.068f, bold = true, spacing = 0.14f)
        rule(-extent * 0.14f)
        line(copy.place, extent * 0.015f, 0.140f, bold = true, spacing = 0.02f, floor = 0.058f)
        rule(extent * 0.075f)
        line(copy.date, extent * 0.16f, 0.070f)
        line(copy.serial, extent * 0.245f, 0.048f, spacing = 0.08f)
    }
}

// ---------------------------------------------------------------- devices

/**
 * Travel devices, drawn as filled silhouettes. At the size these sit on a stamp,
 * an outline of thin strokes collapses into a scribble.
 */
private fun DrawScope.drawDevice(device: StampDevice, ink: Color, at: Offset, size: Float) {
    fun shape(points: List<Pair<Float, Float>>) = Path().apply {
        points.forEachIndexed { index, (px, py) ->
            val x = at.x + px * size
            val y = at.y + py * size
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    fun wheel(px: Float, py: Float, r: Float) =
        drawCircle(ink, size * r, Offset(at.x + px * size, at.y + py * size))

    when (device) {
        StampDevice.NONE -> return

        // An airliner seen from above, nose to the top.
        StampDevice.PLANE -> {
            val right = listOf(
                0f to -1.00f, 0.14f to -0.42f, 0.95f to 0.18f, 0.95f to 0.40f,
                0.16f to 0.10f, 0.16f to 0.62f, 0.40f to 0.86f, 0.40f to 1.00f, 0f to 0.82f,
            )
            drawPath(shape(right + right.reversed().drop(1).map { -it.first to it.second }), ink)
        }

        StampDevice.SHIP -> {
            drawPath(shape(listOf(-0.85f to 0.35f, 0.85f to 0.35f, 0.55f to 0.78f, -0.55f to 0.78f)), ink)
            drawPath(shape(listOf(-0.04f to -0.80f, 0.04f to -0.80f, 0.04f to 0.32f, -0.04f to 0.32f)), ink)
            drawPath(shape(listOf(0.10f to -0.72f, 0.68f to -0.26f, 0.10f to 0.04f)), ink)
        }

        StampDevice.TRAIN -> {
            drawPath(shape(listOf(-0.85f to -0.50f, 0.75f to -0.50f, 0.75f to 0.30f, -0.85f to 0.30f)), ink)
            drawPath(shape(listOf(-0.80f to -0.78f, -0.52f to -0.78f, -0.52f to -0.50f, -0.80f to -0.50f)), ink)
            wheel(-0.50f, 0.46f, 0.20f)
            wheel(0.38f, 0.46f, 0.20f)
        }

        StampDevice.CAR -> {
            drawPath(
                shape(
                    listOf(
                        -0.95f to 0.28f, -0.95f to -0.02f, -0.52f to -0.05f, -0.28f to -0.48f,
                        0.30f to -0.48f, 0.55f to -0.05f, 0.95f to -0.02f, 0.95f to 0.28f,
                    ),
                ),
                ink,
            )
            wheel(-0.50f, 0.34f, 0.22f)
            wheel(0.50f, 0.34f, 0.22f)
        }

        StampDevice.GLOBE -> {
            val stroke = Stroke(width = max(size * 0.11f, 1f))
            drawCircle(ink, size * 0.72f, at, style = stroke)
            drawPath(
                Path().apply {
                    moveTo(at.x - size * 0.72f, at.y)
                    lineTo(at.x + size * 0.72f, at.y)
                    moveTo(at.x, at.y - size * 0.72f)
                    quadraticTo(at.x + size * 0.42f, at.y, at.x, at.y + size * 0.72f)
                    moveTo(at.x, at.y - size * 0.72f)
                    quadraticTo(at.x - size * 0.42f, at.y, at.x, at.y + size * 0.72f)
                },
                ink,
                style = stroke,
            )
        }
    }
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

/** Speckles of missing ink, so the stamp reads as pressed rather than printed. */
private fun DrawScope.drawInkWear(seed: Int) {
    val random = Random(seed)
    val extent = min(size.width, size.height)
    repeat(48) {
        drawCircle(
            color = Color.Transparent,
            radius = extent * random.nextDouble(0.004, 0.020).toFloat(),
            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height),
            blendMode = BlendMode.Clear,
        )
    }
}
