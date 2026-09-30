package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.Canvas
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
    class Tint(val color: Color, val alpha: Float, val night: Float)

    fun at(now: LocalDateTime): Tint {
        val k = G.SkyTime
        val h = now.hour + now.minute / 60f
        val night = Tint(k.Night.color, k.Night.alpha, 1f)
        val dawn = Tint(k.Dawn.color, k.Dawn.alpha, 0f)
        val dusk = Tint(k.Dusk.color, k.Dusk.alpha, 0f)
        fun clear(c: Tint) = Tint(c.color, 0f, 0f)
        fun mix(a: Tint, b: Tint, t: Float) = Tint(lerp(a.color, b.color, t), a.alpha + (b.alpha - a.alpha) * t, a.night + (b.night - a.night) * t)
        fun span(from: Float, to: Float) = ((h - from) / (to - from)).coerceIn(0f, 1f)
        return when {
            h < k.dawnFrom || h >= k.nightFrom -> night
            h < k.dawnPeak -> mix(night, dawn, span(k.dawnFrom, k.dawnPeak))
            h < k.dayFrom -> mix(dawn, clear(dawn), span(k.dawnPeak, k.dayFrom))
            h < k.duskFrom -> clear(dawn)
            h < k.duskPeak -> mix(clear(dusk), dusk, span(k.duskFrom, k.duskPeak))
            else -> mix(dusk, night, span(k.duskPeak, k.nightFrom))
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
        val top = tint.color.copy(alpha = tint.alpha); val low = tint.color.copy(alpha = tint.alpha * k.groundKeep)
        drawRect(Brush.verticalGradient(0f to top, (gy / size.height) to low, 1f to low))
        val y0 = starTop.toPx(); val y1 = starBottom.toPx()
        if (tint.night > 0f && y1 > y0) {
            val r = Crayon.Rng(now.toLocalDate().toEpochDay().toInt())   // 밤마다 자리가 조금씩 바뀐다
            val sz = (k.starSize * size.width / G.unitWidth).roundToInt()
            repeat(k.stars.toInt()) {
                val c = Offset(size.width * (0.06f + 0.88f * r.next()), y0 + (y1 - y0) * r.next())
                val s = (sz * (0.6f + 0.6f * r.next())).roundToInt().coerceAtLeast(1)
                drawImage(star, dstOffset = IntOffset((c.x - s / 2f).roundToInt(), (c.y - s / 2f).roundToInt()), dstSize = IntSize(s, s), alpha = k.starAlpha * tint.night)
            }
        }
    }
}
