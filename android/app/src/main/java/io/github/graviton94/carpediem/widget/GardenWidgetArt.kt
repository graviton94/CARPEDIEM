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
import io.github.graviton94.carpediem.ui.garden.birthdayCake
import io.github.graviton94.carpediem.core.Family
import io.github.graviton94.carpediem.core.Kind
import io.github.graviton94.carpediem.ui.garden.SkyTime
import org.json.JSONObject
import java.time.LocalDateTime
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 정원 디자인 위젯의 바탕 그림 (글자는 Glance 가 얹는다).
 * 앱과 같은 그림 파일(assets/garden)과 앱과 같은 벡터 하루(drawHaru)로 그린다.
 */
object GardenWidgetArt {
    enum class Kind { DAYS, TODAY, CALENDAR, LARGE, FAMILY }

    private val L = Tokens.Garden.Layout
    private val W = Tokens.Garden.Widget

    private fun asset(context: Context, name: String): Bitmap = context.assets.open("garden/$name").use { BitmapFactory.decodeStream(it) }
    private fun key(s: Season) = when (s) { Season.SPRING -> "spring"; Season.SUMMER -> "summer"; Season.AUTUMN -> "autumn"; Season.WINTER -> "winter" }

    fun render(context: Context, kind: Kind, wPx: Int, hPx: Int, s: LifeSnapshot?, now: LocalDateTime): Bitmap {
        val w = max(1, wPx); val h = max(1, hPx)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Tokens.Garden.Colors.paper.toArgb())
        val season = s?.season ?: Season.SPRING
        val units = if (kind == Kind.DAYS || kind == Kind.TODAY) W.small else W.wide
        val u = w / units
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 하늘: 폭에 맞추고 위쪽부터
        val sky = asset(context, "sky_${key(season)}.jpg")
        c.drawBitmap(sky, null, RectF(0f, 0f, w.toFloat(), w * sky.height / sky.width.toFloat()), paint)

        val gy = h * when (kind) { Kind.LARGE -> W.largeGroundRatio; Kind.FAMILY -> W.familyGround; else -> W.groundRatio }
        // 하루의 시간에 따른 하늘빛 (앱 정원과 같은 규칙)
        SkyTime.at(now).takeIf { it.alpha > 0f }?.let { t ->
            val top = t.color.copy(alpha = t.alpha).toArgb(); val low = t.color.copy(alpha = t.alpha * Tokens.Garden.SkyTime.groundKeep).toArgb()
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, max(1f, gy), top, low, Shader.TileMode.CLAMP) })
        }
        if (kind != Kind.CALENDAR) {
            val strip = asset(context, "strip_${key(season)}.png")
            val sh = w * strip.height / strip.width.toFloat(); val lineY = sh * (L.stripLineY / L.stripHeight)
            c.drawBitmap(strip, null, RectF(0f, gy - lineY, w.toFloat(), gy - lineY + sh), paint)
        }

        // 해 · 달: 폰 시각
        if (kind == Kind.TODAY || kind == Kind.LARGE || kind == Kind.FAMILY) {
            val hour = now.hour + now.minute / 60f
            val day = hour in Tokens.Garden.Motion.sunrise..Tokens.Garden.Motion.sunset
            val t = if (day) (hour - Tokens.Garden.Motion.sunrise) / (Tokens.Garden.Motion.sunset - Tokens.Garden.Motion.sunrise)
            else ((hour - Tokens.Garden.Motion.sunset + 24f) % 24f) / (24f - (Tokens.Garden.Motion.sunset - Tokens.Garden.Motion.sunrise))
            val base = h * W.sunArcBase; val top = h * W.sunArcTop; val r = W.sunRadius * u
            val x = w * 0.14f + (w * 0.72f) * t; val y = base - (base - top) * sin(t * Math.PI).toFloat()
            if (kind == Kind.TODAY) {
                val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = u * 1.2f; color = Tokens.Garden.Colors.ink.copy(alpha = 0.35f).toArgb() }
                val path = android.graphics.Path(); for (i in 0..40) { val tt = i / 40f; val px = w * 0.14f + w * 0.72f * tt; val py = base - (base - top) * sin(tt * Math.PI).toFloat(); if (i == 0) path.moveTo(px, py) else path.lineTo(px, py) }
                c.drawPath(path, arc)
            }
            c.drawBitmap(asset(context, if (day) "sun.png" else "moon.png"), null, RectF(x - r, y - r, x + r, y + r), paint)
        }

        if (kind == Kind.CALENDAR && s != null) grid(context, c, RectF(w * W.gridLeft, W.gridInset * u, w - W.gridInset * u, h - W.gridInset * u), s, u)

        if (kind == Kind.LARGE && s != null) {
            val moss = asset(context, "obj_moss.png"); val box = L.objBox * W.mossScale * u
            val hx = w * 0.1f + w * 0.8f * s.progress.toFloat().coerceIn(0f, 1f)
            val mx = max(box * 0.3f, hx - W.haruLarge * u * 1.2f)
            c.drawBitmap(moss, null, RectF(mx - box / 2, gy - box * (L.objGround / L.objBox), mx + box / 2, gy - box * (L.objGround / L.objBox) + box), paint)
            haru(context, c, hx, gy, W.haruLarge * u, u, now)
        }
        if (kind == Kind.DAYS) haru(context, c, w * W.haruX, gy, W.haruSmall * u, u, now)
        if (kind == Kind.FAMILY) family(context, c, w, gy, u, now)

        // 위젯 모서리 둥글게
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            val rr = Tokens.Radius.lg.value * context.resources.displayMetrics.density
            drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), rr, rr, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) })
        }
        bmp.recycle()
        return out
    }

    /** 돌 하나: 몸 가운데를 bodyX 에, 땅 gy 에 (Compose 그리기를 위젯 캔버스에). */
    private fun stone(c: Canvas, art: io.github.graviton94.carpediem.ui.garden.HaruArt, bodyX: Float, gy: Float, k: Float, hat: Boolean) {
        val box = art.meta.box
        c.save(); c.translate(bodyX - art.meta.bbox.center.x * k, gy - art.meta.ground * k)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(c), androidx.compose.ui.geometry.Size(box * k, box * k),
        ) { drawHaru(art, k, hat = hat) }
        c.restore()
    }

    /** 가족의 정원: 나와 가족의 돌을 앱과 같은 규칙으로 (겹치지 않게, 생일이면 모자와 케이크). 글자 없음. */
    private fun family(context: Context, c: Canvas, w: Int, gy: Float, u: Float, now: LocalDateTime) {
        val store = Store(context)
        val profile = store.profile ?: return
        val today = now.toLocalDate()
        val me = LifeSnapshot(profile.birthDate, profile.expectancy(store.table), now)
        val sprout = me.season == Season.SPRING
        val base = W.familyHaru * u / L.haruArtWidth
        data class S(val art: io.github.graviton94.carpediem.ui.garden.HaruArt, val k: Float, val prog: Double?, val bday: Boolean)
        val slots = listOf(S(io.github.graviton94.carpediem.ui.garden.HaruArt.of(store.haruSeed, sprout), base, me.progress, Family.isBirthday(profile.birthDate, today))) +
            store.people.map { p -> S(io.github.graviton94.carpediem.ui.garden.HaruArt.of(p.seed, sprout), if (p.kind == Kind.PET) base * Tokens.Garden.Family.petScale else base,
                p.birth?.let { LifeSnapshot(it, store.expectancy(p), now).progress }, Family.isBirthday(p.birth, today)) }
        val lo = W.gridInset * u; val hi = w - W.gridInset * u
        val xs = Family.place(slots.map { sl -> sl.prog?.let { (lo + (hi - lo) * (0.06 + 0.88 * it.coerceIn(0.0, 1.0))) } }, slots.map { (it.art.meta.bbox.width * it.k).toDouble() }, 0,
            lo.toDouble(), hi.toDouble(), (Tokens.Garden.Family.gap * u).toDouble(), (Tokens.Garden.Family.minGap * u).toDouble())
        slots.forEachIndexed { i, sl ->
            stone(c, sl.art, xs[i].toFloat(), gy, sl.k, sl.bday)
            if (sl.bday) {
                val cw = Tokens.Garden.Party.cakeWidth * u; val cx = xs[i].toFloat() + sl.art.meta.bbox.width * sl.k * 0.18f
                c.save(); c.translate(cx - cw / 2, gy - cw + u * 1.5f)
                androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr,
                    androidx.compose.ui.graphics.Canvas(c), androidx.compose.ui.geometry.Size(cw, cw)) { birthdayCake() }
                c.restore()
            }
        }
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
