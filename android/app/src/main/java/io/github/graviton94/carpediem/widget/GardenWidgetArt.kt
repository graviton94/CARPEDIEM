package io.github.graviton94.carpediem.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import io.github.graviton94.carpediem.core.LifeSnapshot
import io.github.graviton94.carpediem.core.LifeUnit
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.data.Store
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.garden.drawHaru
import io.github.graviton94.carpediem.core.Kind as PersonKind
import io.github.graviton94.carpediem.ui.garden.SkyTime
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 정원 디자인 위젯의 바탕 그림 (글자는 Glance 가 얹는다).
 * 앱과 같은 그림 파일(assets/garden)과 앱과 같은 벡터 하루(drawHaru)로 그린다.
 */
object GardenWidgetArt {
    enum class Kind { DAYS, TODAY, CALENDAR, LARGE }

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
        val units = if (kind == Kind.DAYS || kind == Kind.TODAY) W.small else W.wide
        val u = w / units
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 하늘: 폭에 맞추고 위쪽부터
        val sky = asset(context, "sky_${key(season)}.jpg")
        c.drawBitmap(sky, null, RectF(0f, 0f, w.toFloat(), w * sky.height / sky.width.toFloat()), paint)

        val gy = h * when (kind) { Kind.LARGE -> W.largeGroundRatio; else -> W.groundRatio }
        if (kind != Kind.CALENDAR) {
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
        if (kind == Kind.TODAY || kind == Kind.LARGE) {
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

        if (kind == Kind.CALENDAR && s != null) grid(context, c, RectF(w * W.gridLeft, W.gridInset * u, w - W.gridInset * u, h - W.gridInset * u), s, u)

        if (kind == Kind.LARGE && s != null) {
            val D = Tokens.Garden.Decor
            val moss = asset(context, "moss_${key(season)}_0.webp")
            val hx = w * 0.1f + w * 0.8f * s.progress.toFloat().coerceIn(0f, 1f)
            // 이끼 방석은 하루 밑에 (앱 정원과 같은 한지 그림 · 비율: 상자 90 단위 가운데 68 단위가 방석)
            val mw = W.haruLarge * u * D.mossWidth * D.mossBoxW / 68f; val mh = mw * D.mossBoxH / D.mossBoxW
            val mtop = gy + u - mh * (D.mossAtY / D.mossBoxH)
            c.drawBitmap(moss, null, RectF(hx - mw / 2, mtop, hx + mw / 2, mtop + mh), paint)
            haru(context, c, hx, gy, W.haruLarge * u, u, now)
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

    /** 인생 달력 (1칸 = 1년): 손으로 칠한 칸을 종이 결로 거른다. */
    private fun grid(context: Context, c: Canvas, area: RectF, s: LifeSnapshot, u: Float) {
        val total = s.total(LifeUnit.YEARS); val lived = s.lived(LifeUnit.YEARS); if (total <= 0) return
        val cols = Tokens.Grid.widgetMediumColumns; val rows = max(1, (total + cols - 1) / cols)
        val cell = min(area.width() / cols, area.height() / rows); val pad = cell * (1 - 1 / W.cell)
        val layer = Bitmap.createBitmap(max(1, area.width().toInt()), max(1, area.height().toInt()), Bitmap.Config.ARGB_8888)
        val lc = Canvas(layer)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val ox = (area.width() - cell * cols) / 2f
        // 앱의 인생 달력처럼 칸마다 작은 조약돌
        val r = io.github.graviton94.carpediem.ui.garden.Crayon.Rng(860)
        for (i in 0 until total) {
            val cx = ox + (i % cols + 0.5f) * cell; val cy = (i / cols + 0.5f) * cell
            val base = (cell - pad) / 2 * (1f - L.pebbleJitter + L.pebbleJitter * r.next())
            val rx = base * (1f + (r.next() - 0.5f) * L.pebbleSquash); val ry = base * (1f - (r.next() - 0.5f) * L.pebbleSquash)
            val rot = r.next() * 6.283f
            val pts = List(7) { k -> val a = rot + k * 6.283f / 7; val w = 1f + (r.next() - 0.5f) * L.pebbleWobble; floatArrayOf(cx + kotlin.math.cos(a) * rx * w, cy + kotlin.math.sin(a) * ry * w) }
            val path = android.graphics.Path().apply {
                moveTo((pts[6][0] + pts[0][0]) / 2, (pts[6][1] + pts[0][1]) / 2)
                for (k in 0 until 7) { val c = pts[k]; val n = pts[(k + 1) % 7]; quadTo(c[0], c[1], (c[0] + n[0]) / 2, (c[1] + n[1]) / 2) }
                close()
            }
            when {
                i < lived -> { p.style = Paint.Style.FILL; p.color = Tokens.Garden.Colors.calendar[min(3, i * 4 / total)].toArgb() }
                i == lived -> { p.style = Paint.Style.FILL; p.color = Tokens.Garden.Colors.now.toArgb() }
                else -> { p.style = Paint.Style.STROKE; p.strokeWidth = u * 0.8f; p.color = Tokens.Garden.Colors.future.toArgb() }
            }
            lc.drawPath(path, p)
            if (i <= lived) { val o = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = u * 0.6f; color = Tokens.Garden.Colors.ink.copy(alpha = if (i == lived) 1f else L.pebbleLine).toArgb() }; lc.drawPath(path, o) }
        }
        val tooth = asset(context, "tooth_fill.png")
        val mask = Paint().apply {
            shader = BitmapShader(tooth, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply { setLocalMatrix(Matrix().apply { val k = max(0.6f, u * Tokens.Garden.Crayon.textureScale); setScale(k, k) }) }
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        lc.drawRect(Rect(0, 0, layer.width, layer.height), mask)
        c.drawBitmap(layer, area.left, area.top, null)
        layer.recycle()
    }
}
