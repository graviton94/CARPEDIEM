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
import androidx.compose.runtime.getValue
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
private fun envelope(t: Float, fadeIn: Float = 0.18f, fadeOut: Float = 0.25f): Float {
    fun smooth(x: Float) = x.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }   // 시작과 끝이 툭 끊기지 않게
    return smooth(t / fadeIn) * smooth((1f - t) / fadeOut)
}

/**
 * 우연한 순간 한 번 (core Chances 가 정함). 끝나면 onDone. 움직임을 끈 폰에서는 멈춘 한 장면으로 잠깐 보였다 사라지고, 나비는 나오지 않는다.
 * 자리: haruX = 내 하루, treeX · postX = 길의 양 끝 (나무 · 말뚝), skyTop = 글자 아래 하늘. back = 하늘 겹 (땅 그림 앞에 한 번, 돌들 뒤) 인지.
 */
@Composable
internal fun ChanceLayer(chance: Chance, now: LocalDateTime, season: Season, gy: Dp, haruX: Dp, treeX: Dp, postX: Dp, skyTop: Dp, back: Boolean, onDone: (seen: Boolean) -> Unit) {
    // 무지개 · 오로라는 하늘에 (먼 산 · 나무 · 돌들 뒤), 나머지는 돌들 앞
    if ((chance == Chance.RAINBOW || chance == Chance.AURORA) != back) return
    val ctx = LocalContext.current
    val u = Theme.unit
    val moving = remember { !reducedMotion(ctx) }
    val ms = when (chance) {
        Chance.BUBBLES -> C.bubblesMs; Chance.FIREFLIES -> C.firefliesMs; Chance.RAINBOW -> C.rainbowMs; Chance.BUTTERFLIES -> C.butterfliesMs
        Chance.AURORA -> C.auroraMs; Chance.WIND -> C.windMs; Chance.SNAIL -> 0f
    }
    val t = remember(chance) { Animatable(0f) }
    LaunchedEffect(chance) {
        if (chance == Chance.SNAIL) { onDone(true); return@LaunchedEffect }
        if (!moving && chance == Chance.BUTTERFLIES) { onDone(false); return@LaunchedEffect }
        if (moving) t.animateTo(1f, tween(ms.toInt(), easing = LinearEasing))
        else { t.snapTo(0.5f); kotlinx.coroutines.delay(3000) }
        // 밤엔 무지개가 그려지지 않으니 만난 것이 아님
        onDone(!(chance == Chance.RAINBOW && SkyTime.isDark(now)))
    }
    // 끝나기 전에 정원을 떠나면 (다른 페이지 · 다른 화면) 만나지 못한 것으로 정리
    val finish by androidx.compose.runtime.rememberUpdatedState(onDone)
    androidx.compose.runtime.DisposableEffect(chance) { chanceOnScreen.value = true; onDispose { chanceOnScreen.value = false; finish(false) } }
    val dark = SkyTime.isDark(now)
    when (chance) {
        Chance.RAINBOW -> if (!dark) {
            val img = GardenArt.image(ctx, "moment_rainbow.webp"); val k = u * C.rainbowScale
            Image(img, null, Modifier.offset(u * 210f - k * 80f, gy - u * 34f - k * 84f).size(k * 160f, k * 90f).graphicsLayer { alpha = envelope(t.value) })
        }
        Chance.AURORA -> {
            val img = GardenArt.image(ctx, "moment_aurora.webp"); val k = u * C.auroraScale
            Image(img, null, Modifier.offset(u * 195f - k * 130f, skyTop + u * 30f).size(k * 260f, k * 60f).graphicsLayer { alpha = envelope(t.value, 0.25f, 0.3f); translationX = sin(t.value * PI.toFloat() * 2f) * 4f * density })
        }
        Chance.BUBBLES -> {
            // 하루가 두세 번 나눠 붊: 방울마다 크기 · 처음 힘 · 떠오르는 빠르기 · 흔들림이 다름.
            // 처음엔 빨리 나가다 공기에 느려지고 (끌림 k), 천천히 떠오르며 (부력, kb 로 서서히) 바람에 밀리다 하나씩 톡 터짐
            val bubs = remember(chance) { bubbleSet(now.toLocalDate().toEpochDay().toInt()) }
            Canvas(Modifier.fillMaxSize().graphicsLayer()) {
                val e = t.value * C.bubblesMs / 1000f; val s = u.toPx()
                val ox = haruX.toPx() + bubs.side * s * 10f; val oy = (gy - u * 18f).toPx()
                bubs.list.forEach { b ->
                    val a = e - b.t0; if (a < 0f || a > b.life + POP) return@forEach
                    val ex = (1f - kotlin.math.exp(-b.k * a)) / b.k; val eb = a - (1f - kotlin.math.exp(-b.kb * a)) / b.kb
                    val c = Offset(ox + s * (bubs.side * (b.vx * ex + b.drift * a) + b.amp * sin(6.2832f * b.f * a + b.ph)),
                        oy + s * (b.vy * ex - b.rise * eb + 1.2f * sin(6.2832f * b.f * 0.7f * a + b.ph)))
                    var r = s * b.r * (0.35f + 0.65f * min(1f, a / 0.14f))
                    if (a > b.life) { val q = (a - b.life) / POP; r *= 1f + q * 0.35f
                        drawCircle(Color(0xFFC8E1EB).copy(alpha = 0.7f * (1f - q)), r * 1.2f, c, style = Stroke(s * 0.5f)); return@forEach }
                    val sq = 0.05f * sin(6.2832f * 1.6f * a + b.ph)
                    scale(1f + sq, 1f - sq, c) {
                        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.06f), Color(0xFFC8E1EB).copy(alpha = 0.22f), Color.White.copy(alpha = 0.7f)), c, r), r, c)
                        drawCircle(Color(0xFFA0BED2).copy(alpha = 0.6f), r, c, style = Stroke(s * 0.5f))
                        drawOval(Color.White.copy(alpha = 0.9f), Offset(c.x - r * 0.62f, c.y - r * 0.54f), Size(r * 0.44f, r * 0.24f))
                    }
                }
            }
        }
        Chance.FIREFLIES -> Canvas(Modifier.fillMaxSize().graphicsLayer()) {
            // 다섯 마리가 천천히 떠다님. 불빛은 숨 쉬듯 켜졌다 꺼지고, 잠깐 어두운 쉼도 있음
            val e = t.value * C.firefliesMs / 1000f; val s = u.toPx(); val env = envelope(t.value, 0.12f, 0.15f); val col = Tokens.Garden.Night.Colors.firefly
            val r = Crayon.Rng(31)
            repeat(C.fireflies.toInt()) { i ->
                val bx = haruX.toPx() + (r.next() - 0.5f) * s * 120f; val by = (gy - u * 14f).toPx() - r.next() * s * 40f
                val t1 = 5f + r.next() * 4f; val t2 = 2.5f + r.next() * 1.5f; val p1 = r.next() * 6.2832f; val p2 = r.next() * 6.2832f; val cyc = 2.6f + r.next() * 1.6f; val off = r.next() * 3f
                val p = Offset(bx + s * (16f * sin(6.2832f * e / t1 + p1) + 4f * sin(6.2832f * e / t2 + p2)), by + s * (6f * cos(6.2832f * e / (t1 * 1.3f) + p2) + 2.5f * sin(6.2832f * e / t2 + p1)))
                val lit = (0.08f + 0.92f * glowPulse(((e + off) % cyc) / cyc)) * env
                drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.55f * lit), Color.Transparent), p, s * 7f), s * 7f, p)
                drawCircle(col.copy(alpha = lit), s * 1.1f, p)
            }
        }
        Chance.WIND -> {
            // 바람이 두 번 붊: 휙 불었다 잦아들고, 조각은 떠올랐다 내려앉으며 나뭇잎처럼 뒤척임
            val img = GardenArt.image(ctx, "wind_${GardenArt.key(season)}.webp")
            val gusts = remember(chance) { gustSet() }
            LaunchedEffect(chance) { androidx.compose.runtime.snapshotFlow { t.value }.collect { v -> windGust.floatValue = gustStrength(v * C.windMs / 1000f) } }
            androidx.compose.runtime.DisposableEffect(chance) { onDispose { windGust.floatValue = 0f } }
            Canvas(Modifier.fillMaxSize().graphicsLayer()) {
                val e = t.value * C.windMs / 1000f; val s = u.toPx(); val sz = C.windSize * s
                gusts.forEach { b ->
                    val a = e - b.t0; if (a < 0f || a > b.dur) return@forEach
                    val q = a / b.dur; val g = 1f - (1f - q).let { it * it * kotlin.math.sqrt(it) } * (1f - 0.15f * q)
                    val x = -sz * 2 + (size.width + sz * 4) * g
                    val y = (gy - u * 160f).toPx() + s * (b.y0 - b.lift * sin(PI.toFloat() * q) + 3f * sin(6.2832f * 1.2f * a + b.ph) + q * 14f)
                    val rot = (b.spin * g * 2.4f + 0.5f * sin(6.2832f * 0.9f * a + b.ph)) * 180f / PI.toFloat()
                    val flip = 0.25f + 0.75f * abs(cos(6.2832f * 0.7f * a + b.ph))
                    val al = min(1f, q / 0.06f) * min(1f, (1f - q) / 0.08f)
                    translate(x, y) { rotate(rot, Offset.Zero) { scale(flip, 1f, Offset.Zero) { drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset((-sz / 2).toInt(), (-sz / 2).toInt()), IntSize(sz.toInt(), sz.toInt()), alpha = al) } } }
                }
            }
        }
        Chance.BUTTERFLIES -> {
            val wl = GardenArt.image(ctx, "fly_wing_l.webp"); val wr = GardenArt.image(ctx, "fly_wing_r.webp"); val body = GardenArt.image(ctx, "fly_body.webp")
            val heads = remember { floatArrayOf(Float.NaN, Float.NaN) }
            Canvas(Modifier.fillMaxSize().graphicsLayer()) {
                val s = u.toPx(); val g = gy.toPx(); val total = C.butterfliesMs
                // 앞 나비가 길을 냄: 나무 잎 옆 → 위로 떠올라 → 말뚝 위 (작게 굽이침). 뒤 나비는 조금 뒤에서 그 곁을 빙글 돌며 따라감
                val p0 = Offset(treeX.toPx() + s * 40f, g - s * 84f); val p1 = Offset(treeX.toPx() + s * 90f, g - s * 160f)
                val p2 = Offset(postX.toPx() - s * 120f, g - s * 150f); val p3 = Offset(postX.toPx(), g - s * 86f)
                fun bez(q: Float): Offset { val a = 1 - q; return p0 * (a * a * a) + p1 * (3 * a * a * q) + p2 * (3 * a * q * q) + p3 * (q * q * q) }
                fun lead(q: Float): Offset = bez(q) + Offset(9f * s * sin(q * 13f), 6f * s * sin(q * 9f + 1f))
                val em = t.value * total; val u0 = em / total
                val q = if (u0 < 0.5f) 2 * u0 * u0 else 1 - (-2 * u0 + 2).let { it * it } / 2
                fun follower(qq: Float, ms: Float): Offset { val o = ms / 1000f * 6.2832f / 2.3f; val rr = s * (12f + 4f * sin(ms / 1000f * 0.9f)); return lead(max(0f, qq - 0.07f)) + Offset(rr * cos(o), rr * 0.6f * sin(o)) }
                val al = min(1f, u0 / 0.05f) * min(1f, (1f - u0) / 0.07f)
                val qn = min(1f, q + 0.008f)
                listOf(Triple(lead(q), lead(qn), 0), Triple(follower(q, em), follower(qn, em + 30f), 1)).forEach { (pos0, nxt, k) ->
                    val hz = if (k == 0) C.flapHz else C.flapHz * 1.12f; val sz = if (k == 0) 1f else 0.82f; val em2 = em + k * 1300f
                    val glide = (em2 % C.glideEvery) > C.glideEvery - C.glideMs
                    val ph = em2 / 1000f * 2f * PI.toFloat() * hz
                    val ang = if (glide) 0.18f else ((40f + 40f * sin(ph)) * PI.toFloat() / 180f)
                    val bob = if (glide) 0f else -1.4f * s * sin(ph + 0.6f)
                    val d = nxt - pos0; val len = hypot(d.x, d.y).coerceAtLeast(0.001f)
                    // 머리 (그림의 위) 를 나는 쪽으로, 휙 돌지 않게 천천히 따라감
                    val target = atan2(d.y / len, d.x / len) + PI.toFloat() / 2
                    var h = heads[k]; if (h.isNaN()) h = target
                    var dd = target - h; while (dd > PI) dd -= 2 * PI.toFloat(); while (dd < -PI) dd += 2 * PI.toFloat(); h += dd * 0.08f; heads[k] = h
                    val box = C.flyBox * s * sz; val w = max(0.12f, cos(ang))
                    translate(pos0.x, pos0.y + bob) { rotate(h * 180f / PI.toFloat(), Offset.Zero) {
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

private const val POP = 0.16f

/** 우연한 순간이 지금 화면에 떠 있는지: 그동안 짧은 알림 한마디는 기다림 (한 번에 하나만). */
internal val chanceOnScreen = androidx.compose.runtime.mutableStateOf(false)

private class Bubble(val t0: Float, val vx: Float, val vy: Float, val k: Float, val rise: Float, val kb: Float, val drift: Float, val amp: Float, val f: Float, val ph: Float, val r: Float, val life: Float)
private class Bubbles(val side: Float, val list: List<Bubble>)

/** 비눗방울 한 번: 세 번 나눠 불기 (3 · 2 · 4 방울). 그날마다 부는 쪽과 방울이 조금씩 다름. */
private fun bubbleSet(seed: Int): Bubbles {
    val r = Crayon.Rng(seed * 31 + 77); val side = if (r.next() < 0.5f) 1f else -1f
    val list = ArrayList<Bubble>()
    listOf(0.3f to 3, 1.9f to 2, 3.6f to 4).forEach { (pt, n) ->
        repeat(n) { j -> list.add(Bubble(pt + j * 0.11f + r.next() * 0.05f, 26f + r.next() * 34f, -(4f + r.next() * 18f), 1.5f + r.next() * 0.8f, 7f + r.next() * 8f, 0.6f + r.next() * 0.5f,
            3f + r.next() * 4f, 1.5f + r.next() * 2.5f, 0.45f + r.next() * 0.55f, r.next() * 6.2832f, 3.4f + r.next() * 3.6f, 2.4f + r.next() * 1.4f)) }
    }
    return Bubbles(side, list)
}

/** 반딧불 불빛 한 숨 (u = 0 … 1): 천천히 켜지고 잠깐 머물다 스러진 뒤 어둡게 쉼. */
internal fun glowPulse(u: Float): Float = when {
    u < 0.22f -> (u / 0.22f).let { it * it * (3 - 2 * it) }
    u < 0.36f -> 1f
    u < 0.66f -> 1f - ((u - 0.36f) / 0.3f).let { it * it * (3 - 2 * it) }
    else -> 0f
}

private class GustPiece(val t0: Float, val dur: Float, val y0: Float, val lift: Float, val spin: Float, val ph: Float)
private val GUST_AT = listOf(0.3f to 3, 3.6f to 2)

/** 계절 바람: 두 번 불어 조각 다섯 (같은 바람의 조각은 조금씩 늦게). */
private fun gustSet(): List<GustPiece> {
    val r = Crayon.Rng(9); val out = ArrayList<GustPiece>()
    GUST_AT.forEach { (g, n) -> repeat(n) { j -> out.add(GustPiece(g + j * (0.25f + r.next() * 0.3f), 2.5f + r.next() * 0.9f, 20f + r.next() * 70f, 10f + r.next() * 16f, (if (r.next() < 0.5f) -1f else 1f) * (1.4f + r.next() * 2.2f), r.next() * 6.2832f)) } }
    return out
}

/** 바람의 세기 (초 → 0 … 1): 불 때마다 부드럽게 일었다 잦아듦. */
private fun gustStrength(e: Float): Float = GUST_AT.maxOf { (g, _) -> val a = (e - g) / 2.6f; if (a in 0f..1f) sin(PI.toFloat() * a) else 0f }

/** 달팽이 손님: 오랜만에 돌아온 날, 돌들 앞 길을 한 시간쯤 아주 천천히 건넘 (그동안 정원을 열 때마다 그 자리에, 돌보다 앞 · 조금 아래). */
@Composable
internal fun SnailGuest(snailAt: Long, now: LocalDateTime, gy: Dp, treeX: Dp) {
    val ctx = LocalContext.current
    val u = Theme.unit
    val wall = System.currentTimeMillis()
    val span = C.snailMinutes * 60_000f
    val a = (wall - snailAt) / span
    if (snailAt <= 0L || a !in 0f..1f) return
    val img = GardenArt.image(ctx, "moment_snail.webp"); val k = u * C.snailScale
    val x = treeX + u * 44f + u * C.snailWalk * a
    // 몸을 늘였다 당기며 조금씩 (머리 쪽을 축으로 늘고, 꼬리를 당겨 옴)
    val clock = rememberGardenClock(!reducedMotion(ctx))
    Image(img, null, Modifier.offset(x - k * 20f, gy + u * 5f - k * 20f).size(k * 40f, k * 26f).graphicsLayer {
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.85f, 0.8f); scaleX = 1f + 0.07f * max(0f, sin(clock.value * 6.2832f / 3.2f))
    }, colorFilter = nightFilter(SkyTime.isDark(now)))
}
