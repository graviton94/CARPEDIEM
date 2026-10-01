package io.github.graviton94.carpediem.ui.garden

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.graviton94.carpediem.core.Chance
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import io.github.graviton94.carpediem.design.Tokens.Garden.Chance as C

/** 몇 초 동안의 밝기: 처음 fadeIn 동안 떠오르고, 끝 fadeOut 동안 스러짐 (t = 0 … 1). */
private fun envelope(t: Float, fadeIn: Float = 0.18f, fadeOut: Float = 0.25f) = min(1f, t / fadeIn).coerceAtLeast(0f) * min(1f, (1f - t) / fadeOut).coerceAtLeast(0f)

/**
 * 우연한 순간 한 번 (core Chances 가 정함). 끝나면 onDone. 움직임을 끈 폰에서는 멈춘 한 장면으로 잠깐 보였다 사라지고, 나비는 나오지 않는다.
 * 자리: haruX = 내 하루, treeX · postX = 길의 양 끝 (나무 · 말뚝), skyTop = 글자 아래 하늘.
 */
@Composable
internal fun ChanceLayer(chance: Chance, now: LocalDateTime, season: Season, gy: Dp, haruX: Dp, treeX: Dp, postX: Dp, skyTop: Dp, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val u = Theme.unit
    val moving = remember { !reducedMotion(ctx) }
    val ms = when (chance) {
        Chance.BUBBLES -> C.bubblesMs; Chance.FIREFLIES -> C.firefliesMs; Chance.RAINBOW -> C.rainbowMs; Chance.BUTTERFLIES -> C.butterfliesMs
        Chance.AURORA -> C.auroraMs; Chance.WIND -> C.windMs; Chance.SNAIL -> 0f
    }
    val t = remember(chance) { Animatable(0f) }
    LaunchedEffect(chance) {
        if (chance == Chance.SNAIL || (!moving && chance == Chance.BUTTERFLIES)) { onDone(); return@LaunchedEffect }
        if (moving) t.animateTo(1f, tween(ms.toInt(), easing = LinearEasing))
        else { t.snapTo(0.5f); kotlinx.coroutines.delay(3000) }
        onDone()
    }
    val dark = SkyTime.isDark(now)
    when (chance) {
        Chance.RAINBOW -> if (!dark) {
            val img = GardenArt.image(ctx, "moment_rainbow.webp"); val k = u * C.rainbowScale
            Image(img, null, Modifier.offset(u * 210f - k * 70f, gy - u * 30f - k * 66f).size(k * 140f, k * 70f).graphicsLayer { alpha = envelope(t.value) })
        }
        Chance.AURORA -> {
            val img = GardenArt.image(ctx, "moment_aurora.webp"); val k = u * C.auroraScale
            Image(img, null, Modifier.offset(u * 195f - k * 130f, skyTop + u * 30f).size(k * 260f, k * 60f).graphicsLayer { alpha = envelope(t.value, 0.25f, 0.3f); translationX = sin(t.value * PI.toFloat() * 2f) * 4f * density })
        }
        Chance.BUBBLES -> Canvas(Modifier.fillMaxSize()) {
            val e = t.value; val s = u.toPx()
            for (i in 0 until 6) {
                val st = i * 0.09f; val a = ((e - st) / 0.62f); if (a !in 0f..1f) continue
                val r = s * (3f + (i % 3) * 1.1f)
                val c = Offset(haruX.toPx() + s * (-14f + i * 6f) + sin(a * 6f + i) * s * 4f, (gy - u * 18f).toPx() - a * s * 90f)
                val al = min(1f, a / 0.15f) * min(1f, (1f - a) / 0.3f)
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.06f * al), Color(0xFFC8E1EB).copy(alpha = 0.2f * al), Color.White.copy(alpha = 0.7f * al)), c, r), r, c)
                drawCircle(Color(0xFFA0BED2).copy(alpha = 0.6f * al), r, c, style = Stroke(s * 0.5f))
                drawOval(Color.White.copy(alpha = 0.9f * al), Offset(c.x - r * 0.62f, c.y - r * 0.54f), Size(r * 0.44f, r * 0.24f))
            }
        }
        Chance.FIREFLIES -> Canvas(Modifier.fillMaxSize()) {
            val e = t.value; val s = u.toPx(); val env = envelope(e, 0.15f, 0.2f); val col = Tokens.Garden.Night.Colors.firefly
            val r = Crayon.Rng(31)
            repeat(10) { i ->
                val bx = haruX.toPx() + (r.next() - 0.5f) * s * 80f; val by = (gy - u * 8f).toPx() - r.next() * s * 44f; val ph = r.next() * 6.283f; val sp = 0.6f + 0.8f * r.next()
                val p = Offset(bx + sin(e * 6.283f * 2f * sp + ph) * s * 10f, by + cos(e * 6.283f * 2.6f * sp + ph) * s * 5f)
                val lit = (0.25f + 0.75f * max(0f, sin(e * 6.283f * (3f + i % 3) + ph))) * env
                drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.55f * lit), Color.Transparent), p, s * 7f), s * 7f, p)
                drawCircle(col.copy(alpha = lit), s * 1.1f, p)
            }
        }
        Chance.WIND -> {
            val img = GardenArt.image(ctx, "wind_${GardenArt.key(season)}.webp")
            Canvas(Modifier.fillMaxSize()) {
                val e = t.value; val s = u.toPx(); val r = Crayon.Rng(4401); val sz = C.windSize * s
                repeat(C.windPieces.toInt()) { i ->
                    val delay = r.next() * 0.35f; val speed = 0.8f + r.next() * 0.5f; val y0 = (gy - u * 160f).toPx() + r.next() * s * 110f; val spin = (r.next() - 0.5f) * 720f
                    val a = ((e - delay) / (1f - 0.35f)).coerceIn(0f, 1f); if (a <= 0f || a >= 1f) return@repeat
                    val x = -sz * 2 + (size.width + sz * 4) * a * speed; val y = y0 + a * s * 40f + sin(a * 9f + i) * s * 6f
                    val al = min(1f, a / 0.1f) * min(1f, (1f - a) / 0.15f)
                    translate(x, y) { rotate(spin * a, Offset.Zero) { drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset((-sz / 2).toInt(), (-sz / 2).toInt()), IntSize(sz.toInt(), sz.toInt()), alpha = al) } }
                }
            }
        }
        Chance.BUTTERFLIES -> {
            val wl = GardenArt.image(ctx, "fly_wing_l.webp"); val wr = GardenArt.image(ctx, "fly_wing_r.webp"); val body = GardenArt.image(ctx, "fly_body.webp")
            val heads = remember { floatArrayOf(Float.NaN, Float.NaN) }
            Canvas(Modifier.fillMaxSize()) {
                val s = u.toPx(); val g = gy.toPx(); val total = C.butterfliesMs
                // 길: 나무 잎 옆 → 위로 떠올라 → 말뚝 위 (좌우로 조금씩 흔들림)
                val p0 = Offset(treeX.toPx() + s * 40f, g - s * 84f); val p1 = Offset(treeX.toPx() + s * 110f, g - s * 150f)
                val p2 = Offset(postX.toPx() - s * 90f, g - s * 118f); val p3 = Offset(postX.toPx(), g - s * 86f)
                fun bez(q: Float): Offset { val a = 1 - q; return p0 * (a * a * a) + p1 * (3 * a * a * q) + p2 * (3 * a * q * q) + p3 * (q * q * q) }
                listOf(0f to 1f, 700f to 0.8f).forEachIndexed { k, (delay, sz) ->
                    val em = t.value * total - delay; if (em < 0f || em > total - 700f) return@forEachIndexed
                    val u0 = em / (total - 700f); val q = if (u0 < 0.5f) 2 * u0 * u0 else 1 - (-2 * u0 + 2).let { it * it } / 2
                    val p = bez(q); val nx = bez(min(1f, q + 0.01f)) - p; val len = hypot(nx.x, nx.y).coerceAtLeast(0.001f); val dir = Offset(nx.x / len, nx.y / len)
                    val wob = sin(em / 1000f * 2f * PI.toFloat() * 0.55f + k * 2.1f) * s * 7f
                    val glide = ((em + k * 900f) % C.glideEvery) > C.glideEvery - C.glideMs
                    val ph = em / 1000f * 2f * PI.toFloat() * C.flapHz
                    val ang = if (glide) 0.18f else ((40f + 40f * sin(ph)) * PI.toFloat() / 180f)
                    val bob = if (glide) 0f else -1.4f * s * sin(ph + 0.6f)
                    val pos = Offset(p.x - dir.y * wob, p.y + dir.x * wob + bob)
                    // 머리 (그림의 위) 를 나는 쪽으로, 휙 돌지 않게 천천히 따라감
                    val target = atan2(dir.y, dir.x) + PI.toFloat() / 2
                    var h = heads[k]; if (h.isNaN()) h = target
                    var d = target - h; while (d > PI) d -= 2 * PI.toFloat(); while (d < -PI) d += 2 * PI.toFloat(); h += d * 0.08f; heads[k] = h
                    val box = C.flyBox * s * sz; val w = max(0.12f, cos(ang)); val al = min(1f, u0 / 0.06f) * min(1f, (1f - u0) / 0.08f)
                    translate(pos.x, pos.y) { rotate(h * 180f / PI.toFloat(), Offset.Zero) {
                        val dst = IntSize(box.toInt(), box.toInt()); val at = IntOffset((-box / 2).toInt(), (-box / 2).toInt())
                        scale(w, 1f, Offset.Zero) { drawImage(wl, IntOffset.Zero, IntSize(wl.width, wl.height), at, dst, alpha = al); drawImage(wr, IntOffset.Zero, IntSize(wr.width, wr.height), at, dst, alpha = al) }
                        drawImage(body, IntOffset.Zero, IntSize(body.width, body.height), at, dst, alpha = al)
                    } }
                }
            }
        }
        Chance.SNAIL -> {}
    }
}

/** 달팽이 손님: 오랜만에 돌아온 날, 나무 발치 옆 길을 한 시간쯤 아주 천천히 건넘 (그동안 정원을 열 때마다 그 자리에). */
@Composable
internal fun SnailGuest(snailAt: Long, now: LocalDateTime, gy: Dp, treeX: Dp) {
    val ctx = LocalContext.current
    val u = Theme.unit
    val wall = System.currentTimeMillis()
    val span = C.snailMinutes * 60_000f
    val a = (wall - snailAt) / span
    if (snailAt <= 0L || a !in 0f..1f) return
    val img = GardenArt.image(ctx, "moment_snail.webp"); val k = u * C.snailScale
    val x = treeX + u * 56f + u * C.snailWalk * a
    Image(img, null, Modifier.offset(x - k * 20f, gy + u * 1f - k * 20f).size(k * 40f, k * 24f), colorFilter = nightFilter(SkyTime.isDark(now)))
}
