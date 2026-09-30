package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.Canvas
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
    val inf = rememberInfiniteTransition(label = "night")
    val tw by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(n.twinkleMs.toInt(), easing = LinearEasing)), label = "twinkle")
    val ff by inf.animateFloat(0f, 1f, infiniteRepeatable(tween((n.fireflyMs * 4).toInt(), easing = LinearEasing)), label = "firefly")
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
        // 가로등: 길 끝에 기둥 · 팔 · 등, 등 둘레와 땅에 불빛
        val lx = n.lampX * u; val top = gy - n.lampHeight * u
        val head = Offset(lx - u * 12, top + u * 8)
        scale(1f, 0.3f, pivot = Offset(head.x, gy)) { glow(G.Night.Colors.lamp, n.lampPool, Offset(head.x, gy), u * 70) }
        glow(G.Night.Colors.lamp, n.lampGlow, head, u * 48)
        val post = G.Night.Colors.post
        drawLine(post, Offset(lx, gy + u * 2), Offset(lx, top), u * 3.2f, StrokeCap.Round)
        drawLine(post, Offset(lx, top + u * 2), Offset(head.x, top + u * 2), u * 2.4f, StrokeCap.Round)
        drawLine(post, Offset(head.x, top + u * 2), Offset(head.x, head.y - u * 4), u * 1.6f, StrokeCap.Round)
        drawCircle(G.Night.Colors.lamp, u * 4.2f, head)
        drawCircle(Color.White.copy(alpha = 0.7f), u * 1.8f, head)
        // 반딧불: 땅 위를 천천히 맴돌며 깜빡
        val r = Crayon.Rng(31)
        repeat(n.fireflies.toInt()) { i ->
            val bx = size.width * (0.08f + 0.84f * r.next()); val by = gy - u * (14f + 60f * r.next())
            val ph = r.next() * 6.283f; val sp = 0.6f + 0.8f * r.next()
            val p = Offset(bx + sin(ff * 6.283f * sp + ph) * u * 18, by + cos(ff * 6.283f * sp * 1.3f + ph) * u * 7)
            val lit = (0.2f + 0.8f * maxOf(0f, sin(tw * 6.283f * (0.5f + i % 3 * 0.25f) + ph))).coerceIn(0f, 1f)
            glow(G.Night.Colors.firefly, 0.55f * lit, p, u * 7)
            drawCircle(G.Night.Colors.firefly.copy(alpha = lit), u * 1.1f, p)
        }
    }
}

