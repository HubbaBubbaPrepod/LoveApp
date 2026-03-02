package com.example.loveapp.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.asin

private const val WIDGET_SERVER_BASE = "http://195.2.71.218:3005"
private const val ICON_PX = 80

/**
 * Pre-renders activity icons to circular PNG files in the cache directory so that
 * Glance widgets can display them as proper bitmaps via ImageProvider.
 *
 * For Material icon keys: renders the ImageVector path tree to an Android Bitmap.
 * For /uploads/... paths: downloads the image via OkHttp and crops to a circle.
 * Returns a comma-separated list of absolute file paths (or emoji fallback strings).
 */
object WidgetIconPreparer {

    private val httpClient by lazy { OkHttpClient() }

    /**
     * @param iconValues  list of raw icon values (Material icon key / /uploads/path / emoji)
     * @param iconVectors map of icon key → ImageVector (pass CUSTOM_ICON_MAP)
     * @param prefix      "my" or "pt" — file naming prefix
     */
    suspend fun prepareIcons(
        context: Context,
        iconValues: List<String>,
        iconVectors: Map<String, ImageVector>,
        prefix: String
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "widget_icons").also { it.mkdirs() }
        iconValues.take(4).mapIndexed { idx, iconValue ->
            val file = File(dir, "${prefix}_$idx.png")
            val bmp: Bitmap? = when {
                iconValue.startsWith("/uploads/") || iconValue.startsWith("http") -> {
                    val url = if (iconValue.startsWith("/")) "$WIDGET_SERVER_BASE$iconValue" else iconValue
                    downloadBitmap(url)
                }
                iconVectors.containsKey(iconValue) -> {
                    iconVectors[iconValue]!!.toAndroidBitmap(ICON_PX,
                        android.graphics.Color.parseColor("#1E90FF"))
                }
                else -> null
            }
            if (bmp != null) {
                runCatching {
                    val circular = makeCircle(bmp)
                    file.outputStream().use { out -> circular.compress(Bitmap.CompressFormat.PNG, 95, out) }
                    file.absolutePath
                }.getOrElse { WidgetActivityIcons.toEmoji(iconValue) }
            } else {
                WidgetActivityIcons.toEmoji(iconValue)
            }
        }.joinToString(",")
    }

    private fun downloadBitmap(url: String): Bitmap? = runCatching {
        val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
        val bytes = response.body?.bytes() ?: return null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()

    private fun makeCircle(src: Bitmap): Bitmap {
        val s = ICON_PX
        val out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Fully opaque white mask circle — SRC_IN will clip the source at full 100% opacity
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(s / 2f, s / 2f, s / 2f, paint)
        // Clip-in the scaled source
        paint.reset()
        paint.isAntiAlias = true
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val scaled = if (src.width != s || src.height != s)
            Bitmap.createScaledBitmap(src, s, s, true) else src
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return out
    }
}

// ── ImageVector → Android Bitmap ─────────────────────────────────────────────

fun ImageVector.toAndroidBitmap(sizePx: Int, @ColorInt tint: Int): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    // Scale from viewport coordinates to pixel size
    canvas.scale(sizePx / viewportWidth, sizePx / viewportHeight)
    drawVectorGroup(canvas, root, tint)
    return bmp
}

private fun drawVectorGroup(canvas: Canvas, group: VectorGroup, tint: Int) {
    canvas.save()
    if (group.rotation != 0f)
        canvas.rotate(group.rotation, group.pivotX, group.pivotY)
    if (group.scaleX != 1f || group.scaleY != 1f)
        canvas.scale(group.scaleX, group.scaleY, group.pivotX, group.pivotY)
    if (group.translationX != 0f || group.translationY != 0f)
        canvas.translate(group.translationX, group.translationY)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    for (node in group) {
        when (node) {
            is VectorGroup -> drawVectorGroup(canvas, node, tint)
            is VectorPath  -> {
                val path = node.pathData.toAndroidPath()
                path.fillType = when (node.pathFillType) {
                    PathFillType.EvenOdd -> Path.FillType.EVEN_ODD
                    else                 -> Path.FillType.WINDING
                }
                if (node.fill != null && node.fillAlpha > 0f) {
                    paint.reset()
                    paint.isAntiAlias = true
                    paint.style = Paint.Style.FILL
                    paint.color = brushToColor(node.fill!!, tint)
                    paint.alpha = (node.fillAlpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawPath(path, paint)
                }
                if (node.stroke != null && node.strokeLineWidth > 0f && node.strokeAlpha > 0f) {
                    paint.reset()
                    paint.isAntiAlias = true
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = node.strokeLineWidth
                    paint.color = brushToColor(node.stroke!!, tint)
                    paint.alpha = (node.strokeAlpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawPath(path, paint)
                }
            }
        }
    }
    canvas.restore()
}

private fun brushToColor(brush: androidx.compose.ui.graphics.Brush, fallback: Int): Int {
    if (brush !is SolidColor) return fallback
    val c = brush.value
    // Black or fully transparent → use tint color
    if (c.red == 0f && c.green == 0f && c.blue == 0f) return fallback
    return android.graphics.Color.argb(
        (c.alpha * 255).toInt(), (c.red * 255).toInt(),
        (c.green * 255).toInt(), (c.blue * 255).toInt()
    )
}

// ── PathNode list → android.graphics.Path ────────────────────────────────────

fun List<PathNode>.toAndroidPath(): Path {
    val p = Path()
    var cx = 0f; var cy = 0f
    var cx2 = 0f; var cy2 = 0f   // last cubic second control pt
    var qx1 = 0f; var qy1 = 0f   // last quad control pt
    var lastWasCubic = false
    var lastWasQuad = false

    for (n in this) {
        when (n) {
            is PathNode.MoveTo -> {
                p.moveTo(n.x, n.y); cx = n.x; cy = n.y
                lastWasCubic = false; lastWasQuad = false
            }
            is PathNode.RelativeMoveTo -> {
                p.rMoveTo(n.dx, n.dy); cx += n.dx; cy += n.dy
                lastWasCubic = false; lastWasQuad = false
            }
            is PathNode.LineTo -> {
                p.lineTo(n.x, n.y); cx = n.x; cy = n.y
                lastWasCubic = false; lastWasQuad = false
            }
            is PathNode.RelativeLineTo -> {
                p.rLineTo(n.dx, n.dy); cx += n.dx; cy += n.dy
                lastWasCubic = false; lastWasQuad = false
            }
            is PathNode.HorizontalTo -> {
                p.lineTo(n.x, cy); cx = n.x
                lastWasCubic = false; lastWasQuad = false
            }
            is PathNode.RelativeHorizontalTo -> {
                p.rLineTo(n.dx, 0f); cx += n.dx
                lastWasCubic = false; lastWasQuad = false
            }
            is PathNode.VerticalTo -> {
                p.lineTo(cx, n.y); cy = n.y
                lastWasCubic = false; lastWasQuad = false
            }
            is PathNode.RelativeVerticalTo -> {
                p.rLineTo(0f, n.dy); cy += n.dy
                lastWasCubic = false; lastWasQuad = false
            }
            is PathNode.CurveTo -> {
                p.cubicTo(n.x1, n.y1, n.x2, n.y2, n.x3, n.y3)
                cx2 = n.x2; cy2 = n.y2; cx = n.x3; cy = n.y3
                lastWasCubic = true; lastWasQuad = false
            }
            is PathNode.RelativeCurveTo -> {
                p.rCubicTo(n.dx1, n.dy1, n.dx2, n.dy2, n.dx3, n.dy3)
                cx2 = cx + n.dx2; cy2 = cy + n.dy2; cx += n.dx3; cy += n.dy3
                lastWasCubic = true; lastWasQuad = false
            }
            is PathNode.ReflectiveCurveTo -> {
                // x1,y1 = explicit 2nd control pt; x2,y2 = end pt
                val rx1 = if (lastWasCubic) 2 * cx - cx2 else cx
                val ry1 = if (lastWasCubic) 2 * cy - cy2 else cy
                p.cubicTo(rx1, ry1, n.x1, n.y1, n.x2, n.y2)
                cx2 = n.x1; cy2 = n.y1; cx = n.x2; cy = n.y2
                lastWasCubic = true; lastWasQuad = false
            }
            is PathNode.RelativeReflectiveCurveTo -> {
                // dx1,dy1 = explicit 2nd control pt (relative); dx2,dy2 = end pt (relative)
                val rx1 = if (lastWasCubic) 2 * cx - cx2 else cx
                val ry1 = if (lastWasCubic) 2 * cy - cy2 else cy
                p.cubicTo(rx1, ry1, cx + n.dx1, cy + n.dy1, cx + n.dx2, cy + n.dy2)
                cx2 = cx + n.dx1; cy2 = cy + n.dy1; cx += n.dx2; cy += n.dy2
                lastWasCubic = true; lastWasQuad = false
            }
            is PathNode.QuadTo -> {
                p.quadTo(n.x1, n.y1, n.x2, n.y2)
                qx1 = n.x1; qy1 = n.y1; cx = n.x2; cy = n.y2
                lastWasCubic = false; lastWasQuad = true
            }
            is PathNode.RelativeQuadTo -> {
                p.rQuadTo(n.dx1, n.dy1, n.dx2, n.dy2)
                qx1 = cx + n.dx1; qy1 = cy + n.dy1; cx += n.dx2; cy += n.dy2
                lastWasCubic = false; lastWasQuad = true
            }
            is PathNode.ReflectiveQuadTo -> {
                val rqx = if (lastWasQuad) 2 * cx - qx1 else cx
                val rqy = if (lastWasQuad) 2 * cy - qy1 else cy
                p.quadTo(rqx, rqy, n.x, n.y)
                qx1 = rqx; qy1 = rqy; cx = n.x; cy = n.y
                lastWasCubic = false; lastWasQuad = true
            }
            is PathNode.RelativeReflectiveQuadTo -> {
                val rqx = if (lastWasQuad) 2 * cx - qx1 else cx
                val rqy = if (lastWasQuad) 2 * cy - qy1 else cy
                p.quadTo(rqx, rqy, cx + n.dx, cy + n.dy)
                qx1 = rqx; qy1 = rqy; cx += n.dx; cy += n.dy
                lastWasCubic = false; lastWasQuad = true
            }
            is PathNode.ArcTo -> {
                svgArcToPath(p, cx, cy,
                    n.horizontalEllipseRadius, n.verticalEllipseRadius,
                    n.theta, n.isMoreThanHalf, n.isPositiveArc,
                    n.arcStartX, n.arcStartY)
                cx = n.arcStartX; cy = n.arcStartY
                lastWasCubic = false; lastWasQuad = false
            }
            is PathNode.RelativeArcTo -> {
                val ex = cx + n.arcStartDx; val ey = cy + n.arcStartDy
                svgArcToPath(p, cx, cy,
                    n.horizontalEllipseRadius, n.verticalEllipseRadius,
                    n.theta, n.isMoreThanHalf, n.isPositiveArc, ex, ey)
                cx = ex; cy = ey
                lastWasCubic = false; lastWasQuad = false
            }
            PathNode.Close -> {
                p.close(); lastWasCubic = false; lastWasQuad = false
            }
        }
    }
    return p
}

/**
 * Tries to load a pre-rendered icon bitmap from its cached file path.
 * Returns null if the value is an emoji string (not a file path).
 */
fun loadWidgetIconBitmap(iconStr: String): Bitmap? =
    if (iconStr.startsWith("/")) runCatching { BitmapFactory.decodeFile(iconStr) }.getOrNull()
    else null

/**
 * Converts an SVG arc (endpoint parameterization) to [android.graphics.Path.arcTo].
 * Implements SVG specification section F.6.
 */
private fun svgArcToPath(
    path: Path, x0: Float, y0: Float,
    rxIn: Float, ryIn: Float, xRotDeg: Float,
    largeArc: Boolean, sweep: Boolean,
    x: Float, y: Float
) {
    if (x0 == x && y0 == y) return
    val phi = Math.toRadians(xRotDeg.toDouble())
    val cosPhi = cos(phi); val sinPhi = sin(phi)
    val dx = (x0 - x) / 2.0; val dy = (y0 - y) / 2.0
    val x1p =  cosPhi * dx + sinPhi * dy
    val y1p = -sinPhi * dx + cosPhi * dy

    var rx = abs(rxIn).toDouble(); var ry = abs(ryIn).toDouble()
    val x1p2 = x1p * x1p; val y1p2 = y1p * y1p
    // Ensure radii are large enough (SVG spec step 3)
    val lambda = x1p2 / (rx * rx) + y1p2 / (ry * ry)
    if (lambda > 1.0) { val scale = sqrt(lambda); rx *= scale; ry *= scale }

    val rxx = rx * rx; val ryy = ry * ry
    val num = max(0.0, rxx * ryy - rxx * y1p2 - ryy * x1p2)
    val den = rxx * y1p2 + ryy * x1p2
    val sq = sqrt(num / den) * (if (largeArc == sweep) -1.0 else 1.0)
    val cxp =  sq * rx * y1p / ry
    val cyp = -sq * ry * x1p / rx

    val cxd = cosPhi * cxp - sinPhi * cyp + (x0 + x) / 2.0
    val cyd = sinPhi * cxp + cosPhi * cyp + (y0 + y) / 2.0

    fun angle(ux: Double, uy: Double, vx: Double, vy: Double): Double {
        val sign = if (ux * vy - uy * vx < 0) -1.0 else 1.0
        return sign * acos(max(-1.0, min(1.0,
            (ux * vx + uy * vy) / (sqrt(ux * ux + uy * uy) * sqrt(vx * vx + vy * vy))
        )))
    }

    val ux = (x1p - cxp) / rx;  val uy = (y1p - cyp) / ry
    val vx = (-x1p - cxp) / rx; val vy = (-y1p - cyp) / ry
    val startAngle = Math.toDegrees(angle(1.0, 0.0, ux, uy))
    var sweepAngle  = Math.toDegrees(angle(ux, uy, vx, vy)) % 360.0
    if (!sweep && sweepAngle > 0) sweepAngle -= 360.0
    if ( sweep && sweepAngle < 0) sweepAngle += 360.0

    path.arcTo(
        RectF((cxd - rx).toFloat(), (cyd - ry).toFloat(),
              (cxd + rx).toFloat(), (cyd + ry).toFloat()),
        startAngle.toFloat(), sweepAngle.toFloat()
    )
}
