package io.github.graviton94.carpediem.ui.garden

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import io.github.graviton94.carpediem.core.Constellation
import io.github.graviton94.carpediem.core.ConstellationBook
import io.github.graviton94.carpediem.core.Constellations
import io.github.graviton94.carpediem.core.DayLine
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * 마음의 기록 · 한 해의 정원: 날마다 그 달의 별자리 선을 따라 하나씩 (1일부터, 꼭짓점은 조금 크게).
 * 낮에는 마음 색의 작은 꽃 (줄기 없이), 밤에는 같은 자리의 별. 쉰 날은 옅은 점 하나, 오지 않은 날은 더 옅게.
 * 앱 화면 (Compose) 과 그림 보내기 (CanvasDrawScope) 가 같은 그리기를 쓴다.
 */
object StarGarden {
    /** 하루의 자리: 별자리 판 안의 0..1 좌표. */
    class Dot(val date: LocalDate, val line: DayLine?, val x: Float, val y: Float, val vertex: Boolean) {
        val month get() = date.monthValue
        val seed get() = date.toEpochDay().toInt()
    }

    fun month(book: ConstellationBook, days: List<Pair<LocalDate, DayLine?>>): List<Dot> {
        val c = days.firstOrNull()?.first?.monthValue?.let(book::of) ?: return emptyList()
        return Constellations.layout(c, days.size).zip(days) { s, (d, l) -> Dot(d, l, s.x, s.y, s.vertex) }
    }

    fun year(book: ConstellationBook, days: List<Pair<LocalDate, DayLine?>>): List<Dot> =
        days.groupBy { it.first.monthValue }.toSortedMap().values.flatMap { month(book, it) }

    /** 한 해 띠의 가운데 줄 (판 높이 비율). 달의 가운데 (0..1, 한 해 길이 비율) 에서. */
    fun spine(t: Float): Float = 0.5f + 0.16f * sin(t * 2f * PI.toFloat() * 1.5f)
    fun monthCenter(month: Int): Float = (month - 0.5f) / 12f

    fun color(l: DayLine?, night: Boolean): Color =
        if (l?.feeling == null) (if (night) G.Year.Colors.plain else lerp(G.Year.Colors.plain, G.Year.Colors.rest, 0.3f)) else moodColor(l.feeling)
}

private val C = G.Year.Colors

internal fun DrawScope.meadow(r: Rect = Rect(Offset.Zero, size)) = drawRect(Brush.verticalGradient(listOf(C.meadowTop, C.meadowBottom), r.top, r.bottom), r.topLeft, r.size)
internal fun DrawScope.nightSky(r: Rect = Rect(Offset.Zero, size)) = drawRect(Brush.verticalGradient(listOf(C.skyTop, C.skyBottom), r.top, r.bottom), r.topLeft, r.size)

private fun DrawScope.glow(c: Offset, r: Float, col: Color, a: Float) {
    if (r <= 0f || a <= 0f) return
    drawCircle(Brush.radialGradient(listOf(col.copy(alpha = a.coerceAtMost(1f)), Color.Transparent), c, r), r, c)
}

/** 띠의 은은한 빛: 밤엔 보랏빛, 낮엔 풀빛. */
private fun DrawScope.haze(c: Offset, r: Float, night: Boolean, k: Float = 1f) {
    if (night) { glow(c, r, C.haze, G.Year.hazeAlpha * k); glow(c, r * 0.4f, C.core, G.Year.hazeAlpha * 0.6f * k) }
    else { glow(c, r, C.meadowHaze, G.Year.hazeAlpha * 1.8f * k); glow(c, r * 0.4f, C.heart, G.Year.hazeAlpha * 1.6f * k) }
}

/** 다섯 잎 꽃 하나 (줄기 없이). rot = 도. */
internal fun DrawScope.smallFlower(c: Offset, r: Float, col: Color, rot: Float, alpha: Float = 1f) {
    for (k in 0 until 5) rotate(rot + k * 72f, c) { drawOval(col, Offset(c.x + r * 0.12f, c.y - r * 0.36f), Size(r, r * 0.72f), alpha) }
    drawCircle(C.heart, r * 0.3f, c, alpha)
}

/** 하루 하나: e = 지금 살랑 · 반짝이는 정도 (0..1). big / small = 꼭짓점 · 선 위의 크기 (px). */
private fun DrawScope.day(d: StarGarden.Dot, c: Offset, night: Boolean, big: Float, small: Float, today: LocalDate, e: Float, picked: Boolean) {
    val r0 = if (d.vertex) big else small
    when {
        d.date.isAfter(today) -> drawCircle(if (night) C.plain else C.rest, small * 0.22f, c, if (night) 0.07f else 0.12f)
        d.line == null -> drawCircle(if (night) C.plain else C.rest, small * 0.26f, c, if (night) 0.14f else 0.35f)
        night -> {
            val col = StarGarden.color(d.line, true); val r = r0 * (1f + 0.7f * e)
            glow(c, r * (3.2f + 4f * e), col, (if (d.line.feeling == Feeling.THANKS) 0.45f else 0.3f) + 0.4f * e)
            drawCircle(col, r, c, (if (d.vertex) 0.9f else 0.75f) + 0.1f * e)
            if (d.line.feeling == Feeling.THANKS) drawCircle(Color.White, r * 0.4f, c, 0.7f)
        }
        else -> {
            val rot = (d.seed * 47 % 360).toFloat() + e * 24f * sin(e * 12f)
            smallFlower(c, r0 * (1f + 0.25f * e), StarGarden.color(d.line, false), rot, if (d.vertex) 0.95f else 0.85f)
        }
    }
    if (picked) drawCircle(if (night) C.plain else C.rest, big * 1.6f, c, 0.7f, style = Stroke(big * 0.18f))
}

/** 별자리 선 (밤에만, 아주 옅게). */
private fun DrawScope.lines(cons: Constellation, at: (Pair<Float, Float>) -> Offset, width: Float) =
    cons.lines.forEach { (a, b) -> drawLine(C.plain.copy(alpha = G.Year.lineAlpha), at(cons.stars[a]), at(cons.stars[b]), width) }

/** 한 달 판: rect 안에 그 달의 별자리 하나 (배경은 부르는 쪽이). flower / star = 판 너비 대비 크기 짝. */
internal fun DrawScope.monthIn(
    r: Rect, dots: List<StarGarden.Dot>, cons: Constellation?, night: Boolean, today: LocalDate,
    sizes: FloatArray, pick: Int = -1, e: Float = 0f, picked: LocalDate? = null,
) {
    val bx = r.left + r.width * 0.08f; val bw = r.width * 0.84f; val by = r.top + r.height * 0.08f; val bh = r.height * 0.84f
    fun at(p: Pair<Float, Float>) = Offset(bx + p.first * bw, by + p.second * bh)
    haze(r.center, r.width * 0.5f, night)
    if (night && cons != null) lines(cons, ::at, r.width * 0.003f)
    val big = r.width * (if (night) sizes[2] else sizes[0]); val small = r.width * (if (night) sizes[3] else sizes[1])
    dots.forEachIndexed { i, d -> day(d, at(d.x to d.y), night, big, small, today, if (i == pick) e else 0f, d.date == picked) }
}

internal val MONTH_SIZES get() = floatArrayOf(G.Year.monthFlower, G.Year.monthFlowerSmall, G.Year.monthStar, G.Year.monthStarSmall)
internal val TILE_SIZES get() = floatArrayOf(G.Year.tileFlower, G.Year.tileFlowerSmall, G.Year.tileFlower * 0.4f, G.Year.tileFlowerSmall * 0.35f)

/** 밤하늘의 작은 배경 별 (같은 seed 면 같은 자리). x 는 0..span. */
private fun field(seed: Int, span: Float): List<FloatArray> {
    val r = Crayon.Rng(seed); return List(G.Year.field.toInt()) { floatArrayOf(r.next() * span, r.next(), 0.0006f + 0.0012f * r.next(), 0.06f + 0.22f * r.next()) }
}

/** 한 해 띠: 열두 달의 별자리가 띠를 따라 차례로. off = 왼쪽 끝에서 흘러간 거리 (px). */
internal fun DrawScope.yearBand(dots: List<StarGarden.Dot>, book: ConstellationBook, night: Boolean, today: LocalDate, off: Float, pick: Int, e: Float, sky: List<FloatArray>) {
    val w = size.width; val h = size.height; val len = G.Year.span * w
    if (night) nightSky() else meadow()
    if (night) sky.forEach { s -> val x = s[0] * w - off * 0.6f; if (x > -2f && x < w + 2f) drawCircle(Color.White, s[2] * w, Offset(x, s[1] * h), s[3]) }
    for (k in 0..60) { val t = k / 60f; val x = t * len - off; if (x > -w * 0.4f && x < w * 1.4f) haze(Offset(x, StarGarden.spine(t) * h), w * 0.3f, night) }
    fun at(month: Int, p: Pair<Float, Float>): Offset {
        val t = StarGarden.monthCenter(month)
        return Offset(t * len - off + (p.first - 0.5f) * G.Year.boxW * w, StarGarden.spine(t) * h + (p.second - 0.5f) * G.Year.boxH * w)
    }
    if (night) book.all.forEach { c -> val mid = at(c.month, 0.5f to 0.5f).x; if (mid > -w * 0.3f && mid < w * 1.3f) lines(c, { at(c.month, it) }, w * 0.0018f) }
    val big = w * (if (night) G.Year.yearStar else G.Year.yearFlower); val small = w * (if (night) G.Year.yearStarSmall else G.Year.yearFlowerSmall)
    dots.forEachIndexed { i, d ->
        val c = at(d.month, d.x to d.y)
        if (c.x > -big * 2 && c.x < w + big * 2) day(d, c, night, big, small, today, if (i == pick) e else 0f, false)
    }
}

/** 몇 초에 하나씩 살랑 · 반짝: (고른 순번, 남은 정도 0..1). 움직임을 끄면 멈춘 그림. */
@Composable
internal fun rememberTwinkle(dots: List<StarGarden.Dot>, animate: Boolean): Pair<State<Int>, State<Float>> {
    val pick = remember { mutableIntStateOf(-1) }
    val e = remember { Animatable(0f) }
    val still = reducedMotion(LocalContext.current)
    LaunchedEffect(dots, animate, still) {
        val alive = dots.indices.filter { dots[it].line != null }
        if (!animate || still || alive.isEmpty()) return@LaunchedEffect
        val rng = kotlin.random.Random(dots.size)
        while (true) {
            delay((G.Year.twinkleMs - G.Year.twinkleMs * 0.4f).toLong())
            pick.intValue = alive[rng.nextInt(alive.size)]
            e.snapTo(1f); e.animateTo(0f, tween((G.Year.twinkleMs * 0.4f).toInt(), easing = LinearEasing))
        }
    }
    return pick to e.asState()
}

/** 한 달의 정원 판 (마음의 기록 · 지난 정원). 누르면 그날을 고른다. */
@Composable
internal fun MonthGarden(
    book: ConstellationBook, days: List<Pair<LocalDate, DayLine?>>, night: Boolean, today: LocalDate, modifier: Modifier,
    animate: Boolean = true, picked: LocalDate? = null, sizes: FloatArray = MONTH_SIZES, onPick: ((StarGarden.Dot) -> Unit)? = null,
) {
    val dots = remember(book, days) { StarGarden.month(book, days) }
    val cons = remember(book, days) { days.firstOrNull()?.first?.monthValue?.let(book::of) }
    val (pick, e) = rememberTwinkle(dots, animate)
    val tap = if (onPick == null) Modifier else Modifier.pointerInput(dots) {
        detectTapGestures { pos ->
            val w = size.width.toFloat(); val h = size.height.toFloat()
            dots.filter { !it.date.isAfter(today) }.minByOrNull { hypot(w * (0.08f + 0.84f * it.x) - pos.x, h * (0.08f + 0.84f * it.y) - pos.y) }
                ?.takeIf { hypot(w * (0.08f + 0.84f * it.x) - pos.x, h * (0.08f + 0.84f * it.y) - pos.y) < w * 0.09f }?.let { onPick?.invoke(it) }
        }
    }
    val sky = remember(days) { field(days.firstOrNull()?.first?.toEpochDay()?.toInt() ?: 0, 1f) }
    Canvas(modifier.aspectRatio(G.Year.monthAspect).then(tap)) {
        if (night) { nightSky(); sky.forEach { s -> drawCircle(Color.White, s[2] * size.width, Offset(s[0] * size.width, s[1] * size.height), s[3] * 0.7f) } } else meadow()
        monthIn(Rect(Offset.Zero, size), dots, cons, night, today, sizes, pick.value, e.value, picked)
    }
}

/** 한 해의 정원 띠: 아주 천천히 흐르고 (끝에 닿으면 되돌아), 옆으로 밀면 멈추고 따라온다. month = 지금 가운데 있는 달. */
@Composable
internal fun YearFlow(book: ConstellationBook, days: List<Pair<LocalDate, DayLine?>>, night: Boolean, today: LocalDate, modifier: Modifier, onMonth: (Int) -> Unit = {}) {
    val dots = remember(book, days) { StarGarden.year(book, days) }
    val sky = remember(days) { field(days.firstOrNull()?.first?.year ?: 0, G.Year.span) }
    val (pick, e) = rememberTwinkle(dots, true)
    val still = reducedMotion(LocalContext.current)
    val flow = rememberInfiniteTransition(label = "year")
    val auto = flow.animateFloat(0f, 1f, infiniteRepeatable(tween(G.Year.flowMs.toInt(), easing = LinearEasing), RepeatMode.Reverse), label = "flow")
    var manual by remember { mutableStateOf<Float?>(if (still) 0f else null) }
    val frac = remember { derivedStateOf { manual ?: auto.value } }
    val month by remember { derivedStateOf { (1 + ((frac.value * (G.Year.span - 1f) + 0.5f) / G.Year.span * 12f).toInt()).coerceIn(1, 12) } }
    LaunchedEffect(month) { onMonth(month) }
    Canvas(modifier.aspectRatio(G.Year.flowAspect).pointerInput(Unit) {
        detectHorizontalDragGestures { _, dx -> val travel = (G.Year.span - 1f) * size.width; manual = ((manual ?: auto.value) - dx / travel).coerceIn(0f, 1f) }
    }) {
        yearBand(dots, book, night, today, frac.value * (G.Year.span - 1f) * size.width, pick.value, e.value, sky)
    }
}

/** 홈 카드의 작은 그림: 낮엔 꽃 세 송이, 밤엔 별 세 개. */
@Composable
internal fun TinyGarden(night: Boolean, modifier: Modifier) {
    val still = reducedMotion(LocalContext.current)
    val t = rememberInfiniteTransition(label = "tiny").animateFloat(0f, 1f, infiniteRepeatable(tween(G.Year.twinkleMs.toInt(), easing = LinearEasing)), label = "t")
    val spots = listOf(Triple(0.25f, 0.5f, Feeling.JOY), Triple(0.5f, 0.68f, Feeling.HOPE), Triple(0.76f, 0.38f, Feeling.THANKS))
    Canvas(modifier) {
        val ph = if (still) 0.2f else t.value
        if (night) nightSky() else meadow()
        spots.forEachIndexed { i, (x, y, f) ->
            val k = sin((ph + i / 3f) * 2f * PI.toFloat()); val c = Offset(x * size.width, y * size.height)
            if (night) { val a = 0.55f + 0.4f * k.coerceAtLeast(0f); glow(c, size.width * 0.12f, C.core, a * 0.5f); drawCircle(C.core, size.width * 0.03f, c, a) }
            else smallFlower(c + Offset(0f, k * size.height * 0.03f), size.width * 0.11f, moodColor(f), k * 10f + i * 30f)
        }
    }
}
