package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.github.graviton94.carpediem.core.DayLine
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.sin

/**
 * 한 해의 정원을 은하수처럼: 1월에서 12월로 흐르는 빛의 띠를 따라 날마다 별 하나.
 * 한 줄을 보낸 날은 그 마음의 색으로 빛나는 별 (고마움은 조금 더 밝게), 쉰 날은 아주 작은 별 가루.
 * 자리는 날짜로 정해져 늘 같다. 앱 화면 (Compose) 과 그림 보내기 (android Canvas) 가 같은 자리를 쓴다.
 */
object Galaxy {
    /** 별 하나: 0..1 좌표 (x, y), 크기 (판 너비 대비), 색, 밝기 0..1. */
    class Star(val x: Float, val y: Float, val r: Float, val color: Color, val glow: Float)

    /** 띠의 가운데 줄 (0..1): 왼쪽 아래에서 오른쪽 위로, 부드럽게 한 번 휘어짐. */
    fun spine(t: Float): Pair<Float, Float> = (0.06f + 0.88f * t) to (0.72f - 0.44f * t + 0.12f * sin(t * 2f * PI.toFloat()))

    fun stars(days: List<Pair<LocalDate, DayLine?>>): List<Star> = days.mapIndexed { i, (d, l) ->
        val rng = Crayon.Rng(d.toEpochDay().toInt() * 31 + 7)
        val t = i / (days.size - 1).coerceAtLeast(1).toFloat()
        val (sx, sy) = spine(t)
        // 띠 가운데로 모이고 가장자리로 갈수록 드문 흩어짐 (두 난수의 평균 ≈ 가운데가 짙음)
        val spread = (rng.next() + rng.next() + rng.next() - 1.5f) * G.Year.band * 0.8f
        val along = (rng.next() - 0.5f) * 0.014f
        val x = (sx + along).coerceIn(0.02f, 0.98f); val y = (sy + spread).coerceIn(0.04f, 0.96f)
        if (l == null) Star(x, y, G.Year.dust * (0.6f + 0.8f * rng.next()), G.Year.Colors.plain, 0.25f + 0.2f * rng.next())
        else {
            val bright = l.feeling == Feeling.THANKS
            Star(x, y, G.Year.star * (if (bright) 1.4f else 1f) * (0.7f + 0.6f * rng.next()), if (l.feeling == null) G.Year.Colors.plain else moodColor(l.feeling), if (bright) 1f else 0.85f)
        }
    }

    /** 띠의 은은한 빛 (가운데 줄을 따라 부드러운 번짐). */
    fun haze(): List<Pair<Float, Float>> = List(40) { k -> spine(k / 39f) }

    /** 판 전체의 작은 배경 별 (해마다 같은 자리): x, y, 크기(판 너비 비율), 밝기. */
    fun field(year: Int): List<Star> { val r = Crayon.Rng(year); return List(G.Year.field.toInt()) { Star(r.next(), r.next(), 0.0006f + 0.0014f * r.next(), Color.White, 0.08f + 0.3f * r.next()) } }
}

/** 앱 화면의 은하수 판. 낮 · 밤 상관없이 밤하늘 한 장. */
@Composable
internal fun YearGalaxy(days: List<Pair<LocalDate, DayLine?>>, modifier: Modifier) {
    val stars = remember(days) { Galaxy.stars(days) }
    val haze = remember { Galaxy.haze() }
    val field = remember(days) { Galaxy.field(days.firstOrNull()?.first?.year ?: 0) }
    Canvas(modifier.aspectRatio(G.Year.aspect)) {
        val w = size.width; val h = size.height
        drawRoundRect(Brush.verticalGradient(listOf(G.Year.Colors.skyTop, G.Year.Colors.skyBottom)), cornerRadius = CornerRadius(w * 0.04f))
        field.forEach { s -> drawCircle(Color.White.copy(alpha = s.glow), s.r * w, Offset(s.x * w, s.y * h)) }
        haze.forEach { (x, y) ->
            val c = Offset(x * w, y * h); val r = w * 0.13f; val r2 = w * 0.06f
            drawCircle(Brush.radialGradient(listOf(G.Year.Colors.haze.copy(alpha = G.Year.hazeAlpha), Color.Transparent), c, r), r, c)
            drawCircle(Brush.radialGradient(listOf(G.Year.Colors.core.copy(alpha = G.Year.hazeAlpha * 0.8f), Color.Transparent), c, r2), r2, c)
        }
        stars.forEach { s ->
            val c = Offset(s.x * w, s.y * h); val r = s.r * w
            if (s.glow >= 0.5f) drawCircle(Brush.radialGradient(listOf(s.color.copy(alpha = 0.5f * s.glow), Color.Transparent), c, r * 3.6f), r * 3.6f, c)
            drawCircle(s.color.copy(alpha = s.glow.coerceAtMost(1f)), r, c)
            if (s.glow >= 1f) drawCircle(Color.White.copy(alpha = 0.7f), r * 0.4f, c)   // 고마움: 가운데 작은 빛
        }
    }
}

/** 그림 보내기용: 같은 은하수를 android Canvas 에. */
internal fun drawGalaxy(c: android.graphics.Canvas, left: Float, top: Float, w: Float, days: List<Pair<LocalDate, DayLine?>>) {
    val h = w / G.Year.aspect
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    fun argb(col: Color, a: Float) = android.graphics.Color.argb((a * 255).toInt().coerceIn(0, 255), (col.red * 255).toInt(), (col.green * 255).toInt(), (col.blue * 255).toInt())
    p.shader = android.graphics.LinearGradient(0f, top, 0f, top + h, argb(G.Year.Colors.skyTop, 1f), argb(G.Year.Colors.skyBottom, 1f), android.graphics.Shader.TileMode.CLAMP)
    c.drawRoundRect(android.graphics.RectF(left, top, left + w, top + h), w * 0.04f, w * 0.04f, p)
    p.shader = null
    Galaxy.field(days.firstOrNull()?.first?.year ?: 0).forEach { s -> p.color = argb(Color.White, s.glow); c.drawCircle(left + s.x * w, top + s.y * h, s.r * w, p) }
    Galaxy.haze().forEach { (x, y) ->
        val cx = left + x * w; val cy = top + y * h; val r = w * 0.13f; val r2 = w * 0.06f
        p.shader = android.graphics.RadialGradient(cx, cy, r, argb(G.Year.Colors.haze, G.Year.hazeAlpha), 0, android.graphics.Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
        p.shader = android.graphics.RadialGradient(cx, cy, r2, argb(G.Year.Colors.core, G.Year.hazeAlpha * 0.8f), 0, android.graphics.Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r2, p)
    }
    p.shader = null
    Galaxy.stars(days).forEach { s ->
        val cx = left + s.x * w; val cy = top + s.y * h; val r = s.r * w
        if (s.glow >= 0.5f) {
            p.shader = android.graphics.RadialGradient(cx, cy, r * 3.6f, argb(s.color, 0.5f * s.glow), 0, android.graphics.Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r * 3.6f, p); p.shader = null
        }
        p.color = argb(s.color, s.glow.coerceAtMost(1f)); c.drawCircle(cx, cy, r, p)
        if (s.glow >= 1f) { p.color = argb(Color.White, 0.7f); c.drawCircle(cx, cy, r * 0.4f, p) }
    }
}

