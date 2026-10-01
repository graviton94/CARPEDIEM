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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    /** 하루의 자리: 판 안의 0..1 좌표, 크기 배율. 모양 (반지름 1 의 꽃 · 조약돌 빛) 은 날짜로 정해져 한 번만 만든다. */
    class Dot(val date: LocalDate, val line: DayLine?, val x: Float, val y: Float, val size: Float) {
        val month get() = date.monthValue
        val seed get() = date.toEpochDay().toInt()
        val blob: Path by lazy { blobPath(seed, 8, 0.3f) }
        val flower: Path by lazy { flowerPath(seed) }
        val heart: Path by lazy { blobPath(seed + 3, 7, 0.2f) }
        val rot: Float = (seed * 47 % 360).toFloat()
    }

    fun month(book: ConstellationBook, days: List<Pair<LocalDate, DayLine?>>, install: Long): List<Dot> {
        val first = days.firstOrNull()?.first ?: return emptyList()
        val c = book.of(first.monthValue) ?: return emptyList()
        return Constellations.scatter(c, days.size, Constellations.seed(install, first.year, first.monthValue)).zip(days) { s, (d, l) -> Dot(d, l, s.x, s.y, s.size) }
    }

    fun year(book: ConstellationBook, days: List<Pair<LocalDate, DayLine?>>, install: Long): List<Dot> =
        days.groupBy { it.first.monthValue }.toSortedMap().values.flatMap { month(book, it, install) }

    /** 조약돌처럼 울퉁불퉁한 동그라미 (반지름 ≈ 1). */
    fun blobPath(seed: Int, points: Int, wobble: Float): Path {
        val r = Crayon.Rng(seed)
        val p = List(points) { i -> val a = i * 2f * PI.toFloat() / points + (r.next() - 0.5f) * 0.4f; val rr = 1f + (r.next() - 0.5f) * 2f * wobble; Offset(kotlin.math.cos(a) * rr, sin(a) * rr) }
        fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
        return Path().apply {
            val s0 = mid(p.last(), p[0]); moveTo(s0.x, s0.y)
            p.indices.forEach { i -> val n = mid(p[i], p[(i + 1) % points]); quadraticTo(p[i].x, p[i].y, n.x, n.y) }
            close()
        }
    }

    /** 잎이 넷 · 다섯 · 여섯, 잎마다 크기가 조금씩 다른 꽃 (반지름 ≈ 1). */
    fun flowerPath(seed: Int): Path {
        val r = Crayon.Rng(seed * 7 + 1)
        val k = when { r.next() < 0.2f -> 4; r.next() < 0.25f -> 6; else -> 5 }
        val amp = FloatArray(k) { 0.7f + 0.5f * r.next() }; val ph = r.next() * 2f * PI.toFloat()
        val tau = 2f * PI.toFloat()
        return Path().apply {
            for (i in 0..48) {
                val th = i / 48f * tau
                val j = ((((th - ph) % tau + tau) % tau) / (tau / k) + 0.5f).toInt() % k
                val loc = kotlin.math.abs(kotlin.math.cos(k * (th - ph) / 2f))
                val rr = (0.42f + 0.58f * amp[j] * loc) * (1f + (r.next() - 0.5f) * 0.12f)
                val x = kotlin.math.cos(th) * rr; val y = sin(th) * rr
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }

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

/** 다섯 잎 꽃 하나 (줄기 없이, 홈 카드의 작은 그림). rot = 도. */
internal fun DrawScope.smallFlower(c: Offset, r: Float, col: Color, rot: Float, alpha: Float = 1f) {
    for (k in 0 until 5) rotate(rot + k * 72f, c) { drawOval(col, Offset(c.x + r * 0.12f, c.y - r * 0.36f), Size(r, r * 0.72f), alpha) }
    drawCircle(C.heart, r * 0.3f, c, alpha)
}

/** 반지름 1 의 모양을 c 에 r 크기로. */
private fun DrawScope.shape(path: Path, c: Offset, r: Float, rot: Float, col: Color, alpha: Float) =
    withTransform({ translate(c.x, c.y); rotate(rot, Offset.Zero); scale(r, r, Offset.Zero) }) { drawPath(path, col, alpha.coerceIn(0f, 1f)) }

/** 하루 하나: e = 지금 살랑 · 반짝이는 정도 (0..1). flower / star = 보통 크기 (px). */
private fun DrawScope.day(d: StarGarden.Dot, c: Offset, night: Boolean, flower: Float, star: Float, today: LocalDate, e: Float, picked: Boolean) {
    val base = if (night) star else flower * 0.28f
    when {
        d.date.isAfter(today) -> shape(d.blob, c, base * 0.3f, d.rot, if (night) C.plain else C.rest, if (night) 0.07f else 0.12f)
        d.line == null -> shape(d.blob, c, base * 0.42f * d.size, d.rot, if (night) C.plain else C.rest, if (night) 0.16f else 0.38f)
        night -> {
            val col = StarGarden.color(d.line, true); val r = star * d.size * (1f + 0.7f * e)
            glow(c, r * (3.4f + 4f * e), col, (if (d.line.feeling == Feeling.THANKS) 0.45f else 0.32f) + 0.4f * e)
            shape(d.blob, c, r, d.rot, col, 0.82f + 0.15f * e)
            if (d.line.feeling == Feeling.THANKS) drawCircle(Color.White, r * 0.35f, c, 0.7f)
        }
        else -> {
            val r = flower * d.size * (1f + 0.25f * e)
            shape(d.flower, c, r, d.rot + e * 24f * sin(e * 12f), StarGarden.color(d.line, false), 0.9f)
            shape(d.heart, c, r * 0.28f, d.rot, C.heart, 0.95f)
        }
    }
    if (picked) drawCircle(if (night) C.plain else C.rest, if (night) star * 4f else flower * 1.5f, c, 0.7f, style = Stroke((if (night) star else flower * 0.2f) * 0.5f))
}

/** 한 달 판: rect 안에 그 달의 무늬 (배경은 부르는 쪽이). sizes = 판 너비 대비 꽃 · 별 크기. */
internal fun DrawScope.monthIn(
    r: Rect, dots: List<StarGarden.Dot>, night: Boolean, today: LocalDate,
    sizes: FloatArray, pick: Int = -1, e: Float = 0f, picked: LocalDate? = null,
) {
    val bx = r.left + r.width * 0.08f; val bw = r.width * 0.84f; val by = r.top + r.height * 0.08f; val bh = r.height * 0.84f
    haze(r.center, r.width * 0.5f, night)
    dots.forEachIndexed { i, d -> day(d, Offset(bx + d.x * bw, by + d.y * bh), night, r.width * sizes[0], r.width * sizes[1], today, if (i == pick) e else 0f, d.date == picked) }
}

internal val MONTH_SIZES get() = floatArrayOf(G.Year.monthFlower, G.Year.monthStar)
internal val TILE_SIZES get() = floatArrayOf(G.Year.tileFlower, G.Year.tileStar)

/** 밤하늘의 작은 배경 별 (같은 seed 면 같은 자리). x 는 0..span. */
private fun field(seed: Int, span: Float): List<FloatArray> {
    val r = Crayon.Rng(seed); return List(G.Year.field.toInt()) { floatArrayOf(r.next() * span, r.next(), 0.0006f + 0.0012f * r.next(), 0.06f + 0.22f * r.next()) }
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
    book: ConstellationBook, days: List<Pair<LocalDate, DayLine?>>, install: Long, night: Boolean, today: LocalDate, modifier: Modifier,
    animate: Boolean = true, picked: LocalDate? = null, sizes: FloatArray = MONTH_SIZES, onPick: ((StarGarden.Dot) -> Unit)? = null,
) {
    val dots = remember(book, days, install) { StarGarden.month(book, days, install) }
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
        monthIn(Rect(Offset.Zero, size), dots, night, today, sizes, pick.value, e.value, picked)
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
