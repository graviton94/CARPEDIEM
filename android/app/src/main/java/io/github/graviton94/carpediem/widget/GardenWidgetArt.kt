package io.github.graviton94.carpediem.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
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
import io.github.graviton94.carpediem.ui.garden.HaruArtStore
import org.json.JSONObject
import java.time.LocalDateTime
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 정원 디자인 위젯의 바탕 그림 (글자는 Glance 가 얹는다).
 * 앱과 같은 그림 파일(assets/garden)과, 앱이 저장해 둔 하루 그림(files/haru)을 쓴다. 위젯은 WebView 를 띄우지 않는다.
 */
object GardenWidgetArt {
    enum class Kind { DAYS, TODAY, CALENDAR, LARGE }

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

        val gy = h * if (kind == Kind.LARGE) W.largeGroundRatio else W.groundRatio
        if (kind != Kind.CALENDAR) {
            val strip = asset(context, "strip_${key(season)}.png")
            val sh = w * strip.height / strip.width.toFloat(); val lineY = sh * (L.stripLineY / L.stripHeight)
            c.drawBitmap(strip, null, RectF(0f, gy - lineY, w.toFloat(), gy - lineY + sh), paint)
        }

        // 해 · 달: 폰 시각
        if (kind == Kind.TODAY || kind == Kind.LARGE) {
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

        // 위젯 모서리 둥글게
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            val rr = Tokens.Radius.lg.value * context.resources.displayMetrics.density
            drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), rr, rr, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) })
        }
        bmp.recycle()
        return out
    }

    /** 앱이 저장해 둔 하루 그림 + 기본 시선의 눈동자. 밤에는 졸린 눈. 그림이 없으면 그리지 않는다. */
    private fun haru(context: Context, c: Canvas, cx: Float, gy: Float, widthUnits: Float, u: Float, now: LocalDateTime) {
        val store = Store(context)
        val season = store.profile?.let { LifeSnapshot(it.birthDate, it.expectancy(store.table), now).season } ?: Season.SPRING
        val f = HaruArtStore.files(context, store.haruSeed, season == Season.SPRING) ?: return
        val m = runCatching { JSONObject(f.third.readText()) }.getOrNull() ?: return
        val body = BitmapFactory.decodeFile(f.first.path) ?: return
        val eyes = BitmapFactory.decodeFile(f.second.path) ?: return
        val box = m.getDouble("box").toFloat(); val ground = m.getDouble("ground").toFloat()
        val k = widthUnits / L.haruArtWidth   // 하루 그림 한 칸 = k px
        // 큰 돌도 위젯 밖으로 나가지 않게
        val bb = m.getJSONArray("bbox"); val edge = W.gridInset * u
        val lo = (box / 2 - bb.getDouble(0).toFloat()) * k + edge; val hi = c.width - (bb.getDouble(0).toFloat() + bb.getDouble(2).toFloat() - box / 2) * k - edge
        val x = if (lo <= hi) cx.coerceIn(lo, hi) else c.width / 2f
        val left = x - box / 2 * k; val top = gy - ground * k
        val dst = RectF(left, top, left + box * k, top + box * k)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        c.drawBitmap(body, null, dst, paint)
        val h = now.hour
        val sleepy = h >= Tokens.Garden.Motion.sleepFrom.toInt() || h < Tokens.Garden.Motion.sleepTo.toInt()
        val lid = if (sleepy) Tokens.Garden.Motion.sleepyLid else 0f
        val list = m.getJSONArray("eye")
        for (i in 0 until list.length()) {
            val e = list.getJSONObject(i); val ex = e.getDouble("x").toFloat(); val ey = e.getDouble("y").toFloat(); val er = e.getDouble("r").toFloat()
            val px = left + ex * k; val py = top + ey * k; val r = er * k * 1.35f
            c.save(); c.clipRect(px - r, py - r, px + r, py + r); c.scale(1f, 1f - lid * 0.9f, px, py); c.drawBitmap(eyes, null, dst, paint); c.restore()
            val pr = er * m.getDouble("pupil").toFloat() * k
            var lx = m.getDouble("lookX").toFloat() + (if (i == 0) -1 else 1) * m.getDouble("spread").toFloat()
            var ly = if (sleepy) Tokens.Garden.Motion.sleepyLook else m.getDouble("lookY").toFloat()
            val len = hypot(lx, ly); if (len > 1f) { lx /= len; ly /= len }
            val lim = er * k - pr - er * k * 0.1f
            val qx = px + lx * lim; val qy = py + ly * lim * (1f - lid) + lim * 0.2f
            paint.color = Tokens.Garden.Colors.pupil.toArgb(); c.drawCircle(qx, qy, pr, paint)
            paint.color = Tokens.Garden.Colors.shine.toArgb(); c.drawCircle(qx - pr * 0.35f, qy - pr * 0.38f, pr * 0.24f, paint)
        }
        body.recycle(); eyes.recycle()
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
        for (i in 0 until total) {
            val x = ox + (i % cols) * cell + pad / 2; val y = (i / cols) * cell + pad / 2; val rect = RectF(x, y, x + cell - pad, y + cell - pad)
            val rr = Tokens.Garden.Radius.cell * u
            when {
                i < lived -> { p.style = Paint.Style.FILL; p.color = Tokens.Garden.Colors.calendar[min(3, i * 4 / total)].toArgb() }
                i == lived -> { p.style = Paint.Style.FILL; p.color = Tokens.Garden.Colors.now.toArgb() }
                else -> { p.style = Paint.Style.STROKE; p.strokeWidth = u * 0.8f; p.color = Tokens.Garden.Colors.future.toArgb() }
            }
            lc.drawRoundRect(rect, rr, rr, p)
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
