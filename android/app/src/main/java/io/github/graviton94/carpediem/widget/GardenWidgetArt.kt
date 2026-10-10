package io.github.graviton94.carpediem.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import io.github.graviton94.carpediem.core.LifeSnapshot
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.data.Store
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.garden.drawHaru
import io.github.graviton94.carpediem.core.Kind as PersonKind
import io.github.graviton94.carpediem.ui.garden.SkyTime
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.sin

/**
 * 정원 디자인 위젯의 바탕 그림 (글자는 Glance 가 얹는다).
 * 앱과 같은 그림 파일(assets/garden)과 앱과 같은 벡터 하루(drawHaru)로 그린다.
 */
object GardenWidgetArt {
    enum class Kind { DAYS, TODAY }

    private val L = Tokens.Garden.Layout
    private val W = Tokens.Garden.Widget

    private fun asset(context: Context, name: String): Bitmap = context.assets.open("garden/$name").use { BitmapFactory.decodeStream(it) }
    private fun key(s: Season) = when (s) { Season.SPRING -> "spring"; Season.SUMMER -> "summer"; Season.AUTUMN -> "autumn"; Season.WINTER -> "winter" }

    /** 달을 실제 모양으로 (앱의 MoonShape 와 같은 규칙): 어두운 쪽은 옅게, 밝은 쪽은 타원 경계로. */
    private fun moon(c: Canvas, img: Bitmap, r: RectF, phase: Double, paint: Paint) {
        val faint = Paint(paint).apply { alpha = (255 * Tokens.Garden.Night.moonDark).toInt() }
        c.drawBitmap(img, null, r, faint)
        val waxing = (phase < 0.5) != SkyTime.south
        val k = kotlin.math.cos(2 * Math.PI * phase).toFloat()
        val e = r.width() / 2f * Tokens.Garden.Night.moonDisk * kotlin.math.abs(k)
        val circle = android.graphics.Path().apply { addOval(r, android.graphics.Path.Direction.CW) }
        val half = android.graphics.Path().apply { addRect(if (waxing) RectF(r.centerX(), r.top, r.right, r.bottom) else RectF(r.left, r.top, r.centerX(), r.bottom), android.graphics.Path.Direction.CW) }
        val lit = android.graphics.Path(); lit.op(circle, half, android.graphics.Path.Op.INTERSECT)
        val ell = android.graphics.Path().apply { addOval(RectF(r.centerX() - e, r.top, r.centerX() + e, r.bottom), android.graphics.Path.Direction.CW) }
        lit.op(ell, if (k > 0f) android.graphics.Path.Op.DIFFERENCE else android.graphics.Path.Op.UNION)
        c.save(); c.clipPath(lit); c.drawBitmap(img, null, r, paint); c.restore()
    }

    fun render(context: Context, kind: Kind, wPx: Int, hPx: Int, s: LifeSnapshot?, now: LocalDateTime): Bitmap {
        val w = max(1, wPx); val h = max(1, hPx)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        SkyTime.useCountry(context, io.github.graviton94.carpediem.data.Store(context).profile?.countryCode)
        c.drawColor((if (SkyTime.isDark(now)) Tokens.Garden.Night.Colors.base else Tokens.Garden.Colors.paper).toArgb())
        // 하늘 · 땅 · 이끼는 앱 정원처럼 실제 계절 (나라의 반구를 따라)
        val season = io.github.graviton94.carpediem.core.GardenDecor.realSeason(now.toLocalDate(), io.github.graviton94.carpediem.data.Store(context).profile?.countryCode)
        val units = if (kind == Kind.DAYS) W.small else W.wide
        val u = w / units
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 하늘: 폭에 맞추고 위쪽부터
        val sky = asset(context, "sky_${key(season)}.jpg")
        c.drawBitmap(sky, null, RectF(0f, 0f, w.toFloat(), w * sky.height / sky.width.toFloat()), paint)

        val gy = h * W.groundRatio
        run {
            val strip = asset(context, "strip_${key(season)}.webp")
            val sh = w * strip.height / strip.width.toFloat(); val lineY = sh * (L.stripLineY / L.stripHeight)
            c.drawBitmap(strip, null, RectF(0f, gy - lineY, w.toFloat(), gy - lineY + sh), paint)
        }

        // 하루의 시간에 따른 하늘빛 (앱 정원과 같은 규칙, 땅 그림까지 덮음)
        SkyTime.at(now).takeIf { it.alpha > 0f }?.let { t ->
            val top = t.color.copy(alpha = t.alpha).toArgb(); val low = t.color.copy(alpha = t.alpha * t.groundKeep).toArgb()
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, max(1f, gy), top, low, Shader.TileMode.CLAMP) })
        }
        // 해 · 달: 폰 시각
        if (kind == Kind.TODAY) {
            val (day, t) = SkyTime.sunPath(now)   // 그날 실제 해 뜨고 지는 시각 (앱 정원과 같음)
            // 해 · 달은 늘 땅 위, 가장자리에서도 반쪽이 잘리지 않게 (가로 끝은 반지름만큼 안쪽)
            val r = W.sunRadius * u
            val base = minOf(h * W.sunArcBase, gy - r * 1.4f); val top = minOf(h * W.sunArcTop, base - r * 2f).coerceAtLeast(r * 1.2f)
            val x0 = maxOf(w * 0.14f, r * 1.6f); val span = (w - 2 * x0).coerceAtLeast(0f)
            val x = x0 + span * t; val y = base - (base - top) * sin(t * Math.PI).toFloat()
            if (kind == Kind.TODAY) {
                val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = u * 1.2f; color = Tokens.Garden.Colors.ink.copy(alpha = 0.35f).toArgb() }
                val path = android.graphics.Path(); for (i in 0..40) { val tt = i / 40f; val px = x0 + span * tt; val py = base - (base - top) * sin(tt * Math.PI).toFloat(); if (i == 0) path.moveTo(px, py) else path.lineTo(px, py) }
                c.drawPath(path, arc)
            }
            if (day) c.drawBitmap(asset(context, "sun.png"), null, RectF(x - r, y - r, x + r, y + r), paint)
            else moon(c, asset(context, "moon_full.png"), RectF(x - r, y - r, x + r, y + r), SkyTime.moonPhase(now), paint)
        }


        if (kind == Kind.DAYS) haru(context, c, w * W.haruX, gy, W.haruSmall * u, u, now)

        // 위젯 모서리 둥글게
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            val rr = Tokens.Radius.lg.value * context.resources.displayMetrics.density
            drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), rr, rr, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) })
        }
        bmp.recycle()
        return out
    }

    /** 하루를 앱과 같은 벡터로 (Compose 그리기를 위젯 캔버스에). */
    private fun haru(context: Context, c: Canvas, cx: Float, gy: Float, widthUnits: Float, u: Float, now: LocalDateTime) {
        val store = Store(context)
        val season = store.profile?.let { LifeSnapshot(it.birthDate, it.expectancy(store.table), now).season } ?: Season.SPRING
        val art = io.github.graviton94.carpediem.ui.garden.HaruArt.of(store.haruSeed, season == Season.SPRING)
        val box = art.meta.box; val ground = art.meta.ground; val bb = art.meta.bbox
        val k = widthUnits / L.haruArtWidth   // 하루 그림 한 칸 = k px
        // 큰 돌도 위젯 밖으로 나가지 않게
        val edge = W.gridInset * u
        val lo = (box / 2 - bb.left) * k + edge; val hi = c.width - (bb.right - box / 2) * k - edge
        val x = if (lo <= hi) cx.coerceIn(lo, hi) else c.width / 2f
        val left = x - box / 2 * k; val top = gy - ground * k
        c.save(); c.translate(left, top)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(c),
            androidx.compose.ui.geometry.Size(box * k, box * k),
        ) { drawHaru(art, k) }
        c.restore()
    }
}
