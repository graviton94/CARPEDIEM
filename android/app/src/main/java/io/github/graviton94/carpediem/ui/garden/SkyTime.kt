package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * 하루의 시간에 따른 하늘빛 (폰 시스템 시각). 인생의 계절은 하늘 그림이, 오늘의 시간은 이 빛이 맡는다.
 * 새벽 → 낮(빛 없음) → 해 질 녘 → 밤. 사이 시각은 부드럽게 섞는다.
 */
object SkyTime {
    class Tint(val color: Color, val alpha: Float, val night: Float, val groundKeep: Float = G.SkyTime.groundKeep)

    /** 밤 · 새벽 (darkFrom 시 ~ 다음 날 darkUntil 시): 정원 전체가 어두운 한 벌로 (폰 테마와 상관없이). */
    fun isDark(now: LocalDateTime): Boolean { val h = now.hour + now.minute / 60f; return h >= G.Night.darkFrom || h < G.Night.darkUntil }

    /**
     * 새벽(어두움) → 아침(복숭아빛이 옅어짐) → 낮(빛 없음) → 해 질 녘(노을빛, 어두워질 때까지 머묾) → 밤(짙은 남색).
     * 밤은 테마가 바뀌는 순간 함께 짙어져, 밝은 글자 · 어두운 하늘이 늘 짝을 이룬다.
     */
    fun at(now: LocalDateTime): Tint {
        val k = G.SkyTime
        val h = now.hour + now.minute / 60f
        if (isDark(now)) return Tint(G.Night.Colors.sky, G.Night.skyAlpha, 1f, G.Night.groundAlpha / G.Night.skyAlpha)
        val dawn = Tint(G.SkyTime.Dawn.color, G.SkyTime.Dawn.alpha, 0f)
        val dusk = Tint(G.SkyTime.Dusk.color, G.SkyTime.Dusk.alpha, 0f)
        fun clear(c: Tint) = Tint(c.color, 0f, 0f)
        fun mix(a: Tint, b: Tint, t: Float) = Tint(lerp(a.color, b.color, t), a.alpha + (b.alpha - a.alpha) * t, 0f)
        fun span(from: Float, to: Float) = ((h - from) / (to - from)).coerceIn(0f, 1f)
        return when {
            h < k.dayFrom -> mix(dawn, clear(dawn), span(G.Night.darkUntil, k.dayFrom))
            h < k.duskFrom -> clear(dawn)
            h < k.duskPeak -> mix(clear(dusk), dusk, span(k.duskFrom, k.duskPeak))
            else -> dusk
        }
    }
}

/**
 * 하늘 그림 위에 덮는 시간의 빛: 위가 가장 짙고 땅(groundY)으로 갈수록 옅어진다.
 * 밤에는 [starTop, starBottom] 사이 하늘에 작은 별이 몇 개 뜬다 (글자 · 하루와 겹치지 않는 띠).
 */
@Composable
fun SkyTimeLayer(now: LocalDateTime, groundY: Dp, starTop: Dp, starBottom: Dp, modifier: Modifier = Modifier) {
    val tint = SkyTime.at(now)
    if (tint.alpha <= 0f) return
    val ctx = LocalContext.current
    val star = GardenArt.sparkle(ctx)
    val k = G.SkyTime
    Canvas(modifier) {
        val gy = groundY.toPx().coerceIn(1f, size.height)
        val top = tint.color.copy(alpha = tint.alpha); val low = tint.color.copy(alpha = tint.alpha * tint.groundKeep)
        drawRect(Brush.verticalGradient(0f to top, (gy / size.height) to low, 1f to low))
        val y0 = starTop.toPx(); val y1 = starBottom.toPx()
        if (tint.night > 0f && y1 > y0) {
            val r = Crayon.Rng(now.toLocalDate().toEpochDay().toInt())   // 밤마다 자리가 조금씩 바뀐다
            val sz = (k.starSize * size.width / G.unitWidth).roundToInt()
            repeat((if (tint.night >= 1f) G.Night.stars else k.stars).toInt()) {
                val c = Offset(size.width * (0.06f + 0.88f * r.next()), y0 + (y1 - y0) * r.next())
                val s = (sz * (0.6f + 0.6f * r.next())).roundToInt().coerceAtLeast(1)
                drawImage(star, dstOffset = IntOffset((c.x - s / 2f).roundToInt(), (c.y - s / 2f).roundToInt()), dstSize = IntSize(s, s), alpha = k.starAlpha * tint.night)
            }
        }
    }
}

/**
 * 밤 · 새벽의 정원을 은은하게: 달빛 번짐, 돌들 발치의 따뜻한 빛, 길 끝의 가로등과 그 불빛 웅덩이, 반딧불 몇 마리.
 * 낮에는 그리지 않는다. groundY = 땅, stonesFrom ~ stonesTo = 돌들이 앉은 범위, moon = 달의 가운데 (없으면 null).
 */
@Composable
fun NightLights(now: LocalDateTime, groundY: Dp, stonesFrom: Dp, stonesTo: Dp, moon: DpOffset?, modifier: Modifier = Modifier) {
    if (!SkyTime.isDark(now)) return
    val n = G.Night
    // 시계는 그리는 단계에서만 읽음 (정원이 매 프레임 다시 짜이지 않게). 끊김 없이 이어지는 초라서 한 바퀴 돌 때 튀지 않음
    val clock = rememberGardenClock(!reducedMotion(LocalContext.current))
    Canvas(modifier) {
        val u = size.width / G.unitWidth
        val gy = groundY.toPx()
        val clear = Color.Transparent
        fun glow(c: Color, a: Float, center: Offset, r: Float) = drawCircle(Brush.radialGradient(listOf(c.copy(alpha = a), clear), center, r), r, center)
        // 달빛
        moon?.let { m -> glow(G.Night.Colors.moonGlow, n.moonGlow, Offset(m.x.toPx(), m.y.toPx()), u * 70) }
        // 돌들 발치의 따뜻한 빛 (땅 위에 납작한 둥근 빛)
        val a0 = stonesFrom.toPx(); val a1 = stonesTo.toPx(); val cx = (a0 + a1) / 2; val rx = (a1 - a0) / 2 + u * 46
        scale(1f, 0.32f, pivot = Offset(cx, gy)) { glow(G.Night.Colors.lamp, n.stoneGlow, Offset(cx, gy), rx) }
        // 반딧불: 땅 위를 천천히 떠다니며, 숨 쉬듯 켜졌다 꺼짐 (잠깐 어두운 쉼도)
        val e = clock.value
        val r = Crayon.Rng(31)
        repeat(n.fireflies.toInt()) { i ->
            val bx = size.width * (0.08f + 0.84f * r.next()); val by = gy - u * (14f + 60f * r.next())
            val t1 = 6f + r.next() * 4f; val t2 = 2.6f + r.next() * 1.6f; val p1 = r.next() * 6.283f; val p2 = r.next() * 6.283f; val cyc = 2.8f + r.next() * 1.8f + i * 0.3f
            val p = Offset(bx + u * (18f * sin(6.283f * e / t1 + p1) + 4f * sin(6.283f * e / t2 + p2)), by + u * (7f * cos(6.283f * e / (t1 * 1.3f) + p2) + 2.5f * sin(6.283f * e / t2 + p1)))
            val lit = 0.1f + 0.9f * glowPulse(((e + p1) % cyc) / cyc)
            glow(G.Night.Colors.firefly, 0.55f * lit, p, u * 7)
            drawCircle(G.Night.Colors.firefly.copy(alpha = lit), u * 1.1f, p)
        }
    }
}

/**
 * 마음의 날씨: 오늘 한 줄을 보냈으면 그 마음이 홈 하늘에 잠깐 머문다 (열 때마다 한 번, seconds 초).
 * 슬픔 · 걱정 · 실망 = 몇 방울 비, 기쁨 · 희망 · 고마움 = 비스듬한 햇살 한 줄기. 평온 · 고르지 않음은 그대로. 애니메이션을 끄면 없음.
 */
@Composable
fun MoodWeather(state: io.github.graviton94.carpediem.ui.AppState, now: LocalDateTime, skyBottom: Dp) {
    val f = state.lines.lastOrNull { it.date == now.toLocalDate() }?.feeling ?: return
    val rain = f in io.github.graviton94.carpediem.core.Letters.HEAVY
    val sun = f == io.github.graviton94.carpediem.core.Feeling.JOY || f == io.github.graviton94.carpediem.core.Feeling.HOPE || f == io.github.graviton94.carpediem.core.Feeling.THANKS
    if (!rain && !sun) return
    val ctx = LocalContext.current
    if (reducedMotion(ctx)) return
    val w = G.Weather
    val t = androidx.compose.runtime.remember(f) { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(f) { t.animateTo(1f, tween((w.seconds * 1000).toInt(), easing = LinearEasing)) }
    if (t.value >= 1f) return
    val ink = io.github.graviton94.carpediem.design.Theme.gc.ink
    val warm = G.Night.Colors.lamp
    Canvas(Modifier.fillMaxSize()) {
        val env = sin(t.value * Math.PI.toFloat())   // 스며들었다 사라짐
        val bottom = skyBottom.toPx().coerceIn(1f, size.height)
        val u = size.width / G.unitWidth
        if (rain) {
            val r = Crayon.Rng(now.toLocalDate().toEpochDay().toInt())
            repeat(w.drops.toInt()) {
                // 바람을 타듯: 빠르기가 조금씩 일렁이고, 옆으로 살짝 밀림
                val x0 = size.width * r.next(); val sp = 0.7f + 0.6f * r.next(); val off = r.next(); val ph = r.next() * 6.283f
                val y = ((t.value * 6f * sp + off + 0.025f * sin(t.value * 18f + ph)) % 1f) * bottom
                val x = x0 + u * 4f * sin(t.value * 9f + ph)
                drawLine(ink.copy(alpha = w.rainAlpha * env), Offset(x, y), Offset(x - u * 2f, y + u * 9f), u * 1.1f, StrokeCap.Round)
            }
        } else {
            val a = w.sunAlpha * env
            drawRect(Brush.linearGradient(listOf(Color.Transparent, warm.copy(alpha = a), Color.Transparent), Offset(size.width * 0.95f, 0f), Offset(size.width * 0.35f, bottom)), size = androidx.compose.ui.geometry.Size(size.width, bottom))
        }
    }
}

/**
 * 밤 하늘의 별똥별: 가끔, 정해지지 않은 때에 하나가 [top, bottom] 띠를 비스듬히 스쳐 감. 알려 주지 않는 작은 선물.
 * 낮이나 움직임을 끈 기기에서는 없음.
 */
@Composable
fun ShootingStars(now: LocalDateTime, top: Dp, bottom: Dp, modifier: Modifier = Modifier) {
    if (!SkyTime.isDark(now)) return
    val ctx = LocalContext.current
    if (androidx.compose.runtime.remember { reducedMotion(ctx) }) return
    val k = G.ShootingStar
    val p = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(-1f) }
    val seed = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val r = java.util.Random()
        fun wait(a: Float, b: Float) = ((a + (b - a) * r.nextFloat()) * 1000).toLong()
        kotlinx.coroutines.delay(wait(k.firstMin, k.firstMax))
        while (true) {
            seed.intValue = r.nextInt()
            p.snapTo(0f); p.animateTo(1f, tween(k.ms.toInt(), easing = androidx.compose.animation.core.LinearOutSlowInEasing)); p.snapTo(-1f)
            kotlinx.coroutines.delay(wait(k.min, k.max))
        }
    }
    val c = G.Night.Colors.moonGlow
    Canvas(modifier) {
        val t = p.value; if (t < 0f) return@Canvas
        val y0 = top.toPx(); val y1 = bottom.toPx(); if (y1 <= y0) return@Canvas
        val r = Crayon.Rng(seed.intValue)
        val sx = size.width * (0.08f + 0.5f * r.next()); val sy = y0 + (y1 - y0) * 0.6f * r.next()
        val dx = 0.91f; val dy = 0.41f; val travel = size.width * 0.35f
        val hx = sx + dx * travel * t; val hy = sy + dy * travel * t
        val tail = size.width * k.length * (if (t < 0.3f) t / 0.3f else 1f)
        val a = if (t > 0.7f) (1f - t) / 0.3f else 1f
        val from = Offset(hx - dx * tail, hy - dy * tail); val head = Offset(hx, hy)
        drawLine(Brush.linearGradient(listOf(Color.Transparent, c.copy(alpha = a)), from, head), from, head, strokeWidth = 1.6f * density, cap = StrokeCap.Round)
        drawCircle(c.copy(alpha = a), 1.5f * density, head)
    }
}
