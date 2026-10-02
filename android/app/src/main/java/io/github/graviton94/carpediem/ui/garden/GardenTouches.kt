package io.github.graviton94.carpediem.ui.garden

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.graviton94.carpediem.core.BreathKind
import io.github.graviton94.carpediem.core.TermTouch
import io.github.graviton94.carpediem.design.Tokens
import java.time.LocalDateTime
import kotlin.math.floor
import kotlin.math.sin

private val T = Tokens.Garden.TermTouch
private const val TAU = 6.2832f
private fun h(i: Int, k: Int): Float { val x = sin(i * 12.9898f + k * 78.233f) * 43758.547f; return x - floor(x) }

/**
 * 스물넷 절기 (S1): 절기가 든 날 정원에 아주 작은 변화 하나 (그날 하루). 그림 파일 없이 옅게 그린다.
 * gy = 땅, skyTop ~ skyBottom = 글자 아래 ~ 하루 머리 위 하늘 띠, sun = 해 · 달의 가운데 (낮이면 해).
 */
@Composable
internal fun TermTouches(touch: TermTouch, now: LocalDateTime, gy: Dp, skyTop: Dp, skyBottom: Dp, sun: Offset?, day: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val moving = remember { !reducedMotion(ctx) }
    val clock = rememberGardenClock(moving && touch in setOf(TermTouch.BLOSSOM, TermTouch.DRIZZLE, TermTouch.SNOW, TermTouch.HEAT, TermTouch.DEW, TermTouch.THAW, TermTouch.LONG_NIGHT))
    val dark = SkyTime.isDark(now)
    val a0 = T.alpha * (if (dark) T.nightAlpha else 1f)
    val C = T.Colors
    Canvas(modifier.graphicsLayer()) {
        val u = size.width / Tokens.Garden.unitWidth
        val g = gy.toPx(); val y0 = skyTop.toPx(); val y1 = skyBottom.toPx().coerceAtLeast(y0 + 1f)
        val t = clock.value
        fun glint(c: Offset, r: Float, a: Float, col: Color) {
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = a * 0.5f), Color.Transparent), c, r * 3f), r * 3f, c)
            drawLine(col.copy(alpha = a), Offset(c.x - r, c.y), Offset(c.x + r, c.y), u * 0.6f, StrokeCap.Round)
            drawLine(col.copy(alpha = a), Offset(c.x, c.y - r), Offset(c.x, c.y + r), u * 0.6f, StrokeCap.Round)
        }
        when (touch) {
            // 소한 · 대한: 땅 가장자리에 얇은 얼음빛
            TermTouch.ICE -> drawRect(Brush.verticalGradient(0f to Color.Transparent, 1f to C.ice.copy(alpha = a0 * 0.55f), startY = g - u * 7f, endY = g + u * 2f), Offset(0f, g - u * 7f), Size(size.width, u * 9f))
            // 우수: 땅 위 작은 물방울 몇, 반짝임이 천천히 오감
            TermTouch.THAW -> repeat(T.glints.toInt()) { i ->
                val c = Offset(size.width * (0.06f + 0.88f * h(i, 1)), g + u * (2f + 10f * h(i, 2)))
                scale(1f, 0.45f, c) { drawCircle(C.drizzle.copy(alpha = a0 * 0.4f), u * (2.4f + 1.6f * h(i, 3)), c) }
                glint(c, u * 1.6f, a0 * (0.3f + 0.7f * (0.5f + 0.5f * sin(t * 0.8f + h(i, 4) * TAU))), C.dew)
            }
            // 입춘 · 경칩: 땅에서 올라온 꽃눈 몇
            TermTouch.BUDS -> repeat(5) { i ->
                val x = size.width * (0.1f + 0.8f * h(i, 5)); val ht = u * (6f + 6f * h(i, 6))
                drawLine(C.grass.copy(alpha = a0), Offset(x, g + u * 2f), Offset(x + u * 1.2f, g - ht), u * 0.9f, StrokeCap.Round)
                drawOval(C.bud.copy(alpha = a0 + 0.2f), Offset(x + u * 1.2f - u * 1.8f, g - ht - u * 3.4f), Size(u * 3.6f, u * 4.4f))
            }
            // 춘분 · 청명: 꽃잎 몇 장이 하늘에서 천천히 내려옴
            TermTouch.BLOSSOM -> repeat(T.petals.toInt()) { i ->
                val dur = 14f + 8f * h(i, 7); val q = ((t / dur + h(i, 8)) % 1f)
                val x = size.width * (0.05f + 0.9f * h(i, 9)) + u * 22f * sin(t * 0.4f + i) + u * 30f * q
                val y = y0 + (g - y0) * q
                val a = a0 * (if (q < 0.1f) q / 0.1f else if (q > 0.9f) (1f - q) / 0.1f else 1f)
                rotate((t * 40f + i * 60f) % 360f, Offset(x, y)) { drawOval(C.petal.copy(alpha = a), Offset(x - u * 2.6f, y - u * 1.5f), Size(u * 5.2f, u * 3f)) }
            }
            // 곡우: 보슬비 (아주 가늘고 옅게)
            TermTouch.DRIZZLE -> repeat(T.drops.toInt()) { i ->
                val sp = 0.6f + 0.5f * h(i, 10); val q = ((t * 0.35f * sp + h(i, 11)) % 1f)
                val x = size.width * h(i, 12) - u * 6f * q; val y = y0 + (g - y0) * q
                drawLine(C.drizzle.copy(alpha = a0 * 0.5f), Offset(x, y), Offset(x - u * 1.4f, y + u * 7f), u * 0.7f, StrokeCap.Round)
            }
            // 입하 · 소만 · 망종: 길가에 풀 몇 포기
            TermTouch.GREEN -> repeat(T.tufts.toInt()) { i ->
                val x = size.width * (0.05f + 0.9f * h(i, 13)); val ht = u * (5f + 5f * h(i, 14))
                for (j in -1..1) drawLine(C.grass.copy(alpha = a0), Offset(x + j * u * 1.4f, g + u * 2f), Offset(x + j * u * 3.2f, g - ht * (1f - 0.25f * kotlin.math.abs(j))), u * 0.9f, StrokeCap.Round)
            }
            // 하지: 가장 높은 해, 둘레에 따뜻한 빛
            TermTouch.HIGH_SUN -> { if (day && sun != null) drawCircle(Brush.radialGradient(listOf(C.heat.copy(alpha = a0 * 0.45f), Color.Transparent), sun, u * 70f), u * 70f, sun) }
            // 소서 · 대서: 땅 위 아지랑이 (느리게 일렁이는 옅은 띠)
            TermTouch.HEAT -> repeat(3) { i ->
                val yy = g - u * (6f + 7f * i)
                for (k in 0 until 24) {
                    val x = size.width * k / 24f; val x2 = size.width * (k + 1) / 24f
                    val w1 = u * 1.4f * sin(x / (u * 30f) + t * 0.9f + i); val w2 = u * 1.4f * sin(x2 / (u * 30f) + t * 0.9f + i)
                    drawLine(C.heat.copy(alpha = a0 * 0.22f), Offset(x, yy + w1), Offset(x2, yy + w2), u * 1.2f, StrokeCap.Round)
                }
            }
            // 입추 ~ 한로: 풀잎 끝 이슬 반짝임
            TermTouch.DEW -> repeat(T.glints.toInt()) { i ->
                val c = Offset(size.width * (0.05f + 0.9f * h(i, 15)), g - u * (1f + 5f * h(i, 16)))
                glint(c, u * 1.5f, a0 * (0.2f + 0.8f * glowPulse(((t * 0.25f + h(i, 17)) % 1f))), C.dew)
            }
            // 상강 · 입동: 땅의 첫서리 (흰 점들과 옅은 띠)
            TermTouch.FROST -> {
                drawRect(Brush.verticalGradient(0f to Color.Transparent, 1f to C.frost.copy(alpha = a0 * 0.5f), startY = g - u * 4f, endY = g + u * 3f), Offset(0f, g - u * 4f), Size(size.width, u * 7f))
                repeat(T.speckles.toInt()) { i -> drawCircle(C.frost.copy(alpha = a0 * (0.5f + 0.5f * h(i, 18))), u * (0.5f + 0.5f * h(i, 19)), Offset(size.width * h(i, 20), g - u * 2f + u * 9f * h(i, 21))) }
            }
            // 소설 · 대설: 눈송이 몇이 아주 천천히
            TermTouch.SNOW -> repeat(T.flakes.toInt()) { i ->
                val dur = 18f + 10f * h(i, 22); val q = ((t / dur + h(i, 23)) % 1f)
                val x = size.width * h(i, 24) + u * 10f * sin(t * 0.5f + i); val y = y0 + (g - y0) * q
                drawCircle(C.frost.copy(alpha = a0 * (if (q > 0.92f) (1f - q) / 0.08f else 1f)), u * (1.1f + 0.8f * h(i, 25)), Offset(x, y))
            }
            // 동지: 가장 긴 밤의 별 하나 (낮엔 아주 옅게)
            TermTouch.LONG_NIGHT -> {
                val c = Offset(size.width * 0.78f, y0 + (y1 - y0) * 0.25f)
                val a = (if (dark) 0.95f else 0.3f) * (0.75f + 0.25f * sin(t * 0.7f))
                drawCircle(Brush.radialGradient(listOf(C.star.copy(alpha = a * 0.45f), Color.Transparent), c, u * 16f), u * 16f, c)
                glint(c, u * 4f, a, C.star); drawCircle(C.star.copy(alpha = a), u * 1.6f, c)
            }
        }
    }
}

/**
 * 숨이 정원에 스미기 (E3): 오늘 마친 숨이 정원에 그날만 남는 흔적.
 * 마음 산책 = 길 시작에서 하루 쪽으로 옅은 발자국 몇 걸음, 마음 꽃밭 = 하루 발치에 한지 꽃 두어 송이.
 * (마음 등불 = 말뚝 등이 조금 더 따뜻하게 → DecorBack, 마음 물결 = 연못 물결 → GazeLife.)
 */
@Composable
internal fun BreathTraces(trace: Set<BreathKind>, now: LocalDateTime, gy: Dp, haruX: Dp, haruHalf: Dp, modifier: Modifier = Modifier) {
    if (BreathKind.BOX !in trace && BreathKind.THANKS !in trace) return
    val ctx = LocalContext.current
    val flowers = remember(BreathKind.THANKS in trace) { if (BreathKind.THANKS in trace) listOf("cherry_spring", "ginkgo_spring").map { GardenArt.card(ctx, it) } else emptyList() }
    val dark = SkyTime.isDark(now)
    val ink = if (dark) Tokens.Garden.Night.Colors.ink else Tokens.Garden.Colors.ink
    val seed = now.toLocalDate().toEpochDay().toInt()
    Canvas(modifier) {
        val u = size.width / Tokens.Garden.unitWidth
        val g = gy.toPx(); val hx = haruX.toPx(); val half = haruHalf.toPx()
        if (BreathKind.BOX in trace) {
            // 하루 왼쪽에서 하루 쪽으로 걸어온 발자국 (가까울수록 진하게)
            val n = T.steps.toInt()
            for (s in 0 until n) {
                val x = hx - half - u * 6f - (n - 1 - s) * u * T.stepGap; if (x < u * 8f) continue
                val side = if (s % 2 == 0) -1f else 1f
                val a = T.traceFoot * (0.35f + 0.65f * s / (n - 1).coerceAtLeast(1))
                footprint(Offset(x, g + u * (5f + 2.6f * side)), u, ink.copy(alpha = a))
            }
        }
        if (BreathKind.THANKS in trace && flowers.isNotEmpty()) {
            repeat(T.traceFlowers.toInt()) { i ->
                val img = flowers[i % flowers.size]
                val side = if (i % 2 == 0) 1f else -1f
                val kk = u * (T.traceFlowerSize + 4f * h(seed + i, 30)) / img.height
                val w = img.width * kk; val ht = img.height * kk
                val x = hx + side * (half + u * (8f + 6f * h(seed + i, 31))); val y = g + u * (3f + 2f * h(seed + i, 32))
                translate(x, y) { rotate((h(seed + i, 33) - 0.5f) * 18f, Offset.Zero) {
                    drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset((-w / 2).toInt(), (-ht).toInt()), IntSize(w.toInt(), ht.toInt()), alpha = if (dark) 0.75f else 0.95f)
                } }
            }
        }
    }
}

/** 오른쪽으로 걷는 발자국 하나: 앞꿈치 (넓게) · 뒤꿈치 (작게). */
private fun DrawScope.footprint(c: Offset, u: Float, col: Color) = translate(c.x, c.y) {
    drawOval(col, Offset(u * -0.8f, u * -2.5f), Size(u * 6.8f, u * 5f))
    drawOval(col, Offset(u * -5.7f, u * -1.8f), Size(u * 4.2f, u * 3.6f))
}
