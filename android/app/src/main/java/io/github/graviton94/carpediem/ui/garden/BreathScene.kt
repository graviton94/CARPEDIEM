package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.graviton94.carpediem.core.Breath
import io.github.graviton94.carpediem.core.BreathKind
import io.github.graviton94.carpediem.core.BreathStep
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import io.github.graviton94.carpediem.design.Tokens.Garden.BreathScene as Z

private const val TAU = 6.2832f

/**
 * 숨의 갈래마다의 그림 (아주 옅게, 종이 위 연필 정도). 하루 뒤 · 둘레에 그려지고, 값은 그리는 단계에서만 읽는다.
 * 마음 물결 (CALM): 하루가 작은 웅덩이 가에. 물에 흐리게 비치고, 내쉴 때 물결이 퍼지고, 들이쉴 때 빛이 모임.
 * 마음 산책 (BOX): 네 걸음에 들이쉬고 네 걸음에 내쉼. 발자국이 1초에 한 걸음, 가장자리 길을 같은 빠르기로 (발끝이 가는 쪽).
 * 마음 등불 (SLEEP): 화면 위 가운데에서 긴 끈으로 내려온 작은 한지 등. 숨에 맞춰 밝아졌다 낮아지고, 숨이 거듭될수록 조금씩 어두워짐.
 * 마음 꽃밭 (THANKS): 내쉬며 고마운 것 하나를 떠올릴 때마다 하루 발치에 한지 꽃 한 송이 (정원의 봄 조각).
 * groundY = 하루가 앉은 땅선, scale = 하루 그림 한 칸의 크기.
 */
@Composable
internal fun BreathScene(kind: BreathKind, art: HaruArt, scale: Dp, groundY: Dp, animate: Boolean, plan: List<Breath.Phase>, elapsed: () -> Long, full: () -> Float) {
    val ctx = LocalContext.current
    val night = Theme.gc.night
    val ink = Tokens.Garden.Colors.ink
    val flowers = remember(kind) { if (kind == BreathKind.THANKS) listOf("cherry_spring", "zelkova_spring", "ginkgo_spring", "pine_spring").map { GardenArt.card(ctx, it) } else emptyList() }
    val ins = remember(plan) { plan.filter { it.step == BreathStep.IN } }
    val outs = remember(plan) { plan.filter { it.step == BreathStep.OUT } }
    Canvas(Modifier.fillMaxSize()) {
        val u = size.width / Tokens.Garden.unitWidth
        val ms = elapsed(); val t = ms / 1000f; val f = full()
        val gy = groundY.toPx(); val cx = size.width / 2; val k = scale.toPx()
        // 숨 고리 (모든 장면): 하루 발치의 가는 원, 들숨에 위에서부터 차고 머금에 머물고 날숨에 비워짐
        run {
            val rw = k * art.meta.bbox.width * Tokens.Garden.Breath.ringWidth; val rh = rw * 0.22f
            val tl = Offset(cx - rw / 2, gy - rh / 2); val sz = Size(rw, rh); val sw = u * Tokens.Garden.Breath.ringStroke
            drawOval((if (night) Color(0xFFF4EBDA) else ink).copy(alpha = if (night) 0.16f else 0.12f), tl, sz, style = Stroke(sw))
            if (f > 0.005f) drawArc(if (night) Color(0xFFFFD796) else Color(0xFF5F7236), -90f, 360f * f.coerceIn(0f, 1f), false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
        }
        when (kind) {
            BreathKind.CALM -> {
                // 들이쉬면 하루 둘레에 빛이 모임
                glow(Offset(cx, gy - u * 20f), u * 90f, Color(0xFFFADEAA), Z.halo * f)
                // 웅덩이: 가운데가 진하고 가장자리로 스러지는 납작한 물빛
                scale(1f, 0.3f, Offset(cx, gy + u * 12f)) { drawCircle(Brush.radialGradient(0f to Color(0xFFB0C6CC).copy(alpha = 0.55f), 0.6f to Color(0xFFB0C6CC).copy(alpha = 0.3f), 1f to Color.Transparent, center = Offset(cx, gy + u * 12f), radius = u * 120f), u * 120f, Offset(cx, gy + u * 12f)) }
                // 물에 비친 하루 (위아래 뒤집어 옅게, 물결에 살짝 일렁임)
                val left = cx - k * art.meta.bbox.center.x; val top = gy - k * Tokens.Garden.Layout.haruGround
                val wob = if (animate) u * 1.6f * sin(t * 1.3f) else 0f
                drawIntoCanvas { c -> c.saveLayer(Rect(0f, gy, size.width, size.height), Paint().apply { alpha = Z.reflect }) }
                translate(left + wob, 2 * gy - top) { scale(1f, -1f, Offset.Zero) { drawHaru(art, k, lid = 1f) } }
                drawIntoCanvas { it.restore() }
                if (!animate) return@Canvas
                // 내쉴 때마다 물결 셋 (0.9초 간격), 퍼지며 옅어짐
                outs.forEach { o -> for (j in 0 until 3) { val a = (ms - o.startMs) / 1000f - j * 0.9f; if (a < 0f || a > 7.5f) continue
                    val q = a / 7.5f; val e = 1f - (1f - q).pow(2.2f); val rx = u * (36f + e * 78f); val al = (1f - q) * (1f - q) * 0.7f
                    drawOval(Color.White.copy(alpha = al), Offset(cx - rx, gy + u * 4f - rx * 0.17f), Size(rx * 2, rx * 0.34f), style = Stroke(u * (1.2f - q * 0.5f))) } }
                // 물 위 빛줄
                for (i in 0 until 5) { val span = u * 170f; val len = u * (8f + 10f * hash(i, 1)); val lx = cx - span / 2 + ((hash(i, 2) * span + t * u * (2f + 2f * hash(i, 3))) % span); val ly = gy + u * (8f + 24f * hash(i, 4))
                    drawLine(Color.White.copy(alpha = 0.3f + 0.3f * sin(t * 0.6f + i)), Offset(lx, ly), Offset(lx + len, ly), u, StrokeCap.Round) }
            }
            BreathKind.BOX -> {
                if (!animate) return@Canvas
                val m = u * 30f; val top = u * 120f; val bot = size.height - u * 96f; val pw = size.width - 2 * m; val ph = bot - top; val per = 2 * (pw + ph)
                fun edge(d0: Float): Triple<Float, Float, Float> { var d = ((d0 % per) + per) % per
                    if (d < ph) return Triple(m, bot - d, -PI.toFloat() / 2); d -= ph
                    if (d < pw) return Triple(m + d, top, 0f); d -= pw
                    if (d < ph) return Triple(size.width - m, top + d, PI.toFloat() / 2); d -= ph
                    return Triple(size.width - m - d, bot, PI.toFloat()) }
                val now = floor(t).toInt()
                for (s in now - Z.stepsFade.toInt()..now) { if (s < 0) continue; val a = t - s; if (a < 0f || a > Z.stepsFade) continue
                    val (x, y, ang) = edge(s * u * Z.stride); val sd = if (s % 2 == 0) -1f else 1f
                    val al = Z.foot * (1f - a / Z.stepsFade).pow(1.3f) * min(1f, a / 0.4f)
                    translate(x - sin(ang) * u * 4.5f * sd, y + cos(ang) * u * 4.5f * sd) { rotate(ang * 180f / PI.toFloat(), Offset.Zero) {
                        // 앞꿈치 (넓게, 가는 쪽) · 뒤꿈치 (작게)
                        drawOval(ink.copy(alpha = al), Offset(u * -0.8f, u * -2.5f), Size(u * 6.8f, u * 5f))
                        drawOval(ink.copy(alpha = al), Offset(u * -5.7f, u * -1.8f), Size(u * 4.2f, u * 3.6f))
                    } }
                }
            }
            BreathKind.SLEEP -> {
                val n = ins.count { it.startMs <= ms }; val deep = min(1f, n / 8f) * 0.25f
                if (night && animate) { for (i in 0 until 24) { val a = (0.16f + 0.26f * (0.5f + 0.5f * sin(t * 0.3f + hash(i, 5) * TAU))) * (1f - deep)
                    drawCircle(Color(0xFFFFF8E2).copy(alpha = a), u * (0.6f + 0.6f * hash(i, 6)), Offset(size.width * hash(i, 7), size.height * (0.05f + 0.5f * hash(i, 8)))) } }
                // 위 가운데에서 긴 끈으로 내려온 작은 등, 아주 천천히 흔들림
                val sway = if (animate) 0.025f * sin(t * 0.55f) + 0.01f * sin(t * 1.3f) else 0f; val len = size.height * Z.lanternAt
                val lp = Offset(cx + sin(sway) * len, cos(sway) * len)
                val lv = (0.3f + 0.7f * f) * (1f - deep)
                glow(lp, u * (50f + 70f * lv), Color(0xFFFFCE82), (if (night) 0.45f else 0.3f) * lv)
                glow(Offset(cx, gy), u * 100f, Color(0xFFFFCE82), (if (night) 0.08f else 0.05f) * lv)
                drawLine((if (night) Color(0xFFDCD2BE) else ink).copy(alpha = 0.3f), Offset(cx, 0f), Offset(lp.x, lp.y - u * 11f), u * 0.7f)
                translate(lp.x, lp.y) { rotate(-sway * 57.3f, Offset.Zero) {
                    drawOval(Color(0xFFC8AA78).copy(red = (0.78f + 0.22f * lv).coerceAtMost(1f)), Offset(-u * 8f, -u * 11f), Size(u * 16f, u * 22f))
                    drawLine(Color(0xFFA04637).copy(alpha = 0.8f), Offset(-u * 5f, -u * 11f), Offset(u * 5f, -u * 11f), u * 2f)
                    drawLine(Color(0xFFA04637).copy(alpha = 0.8f), Offset(-u * 5f, u * 11f), Offset(u * 5f, u * 11f), u * 2f)
                    drawOval(Color(0xFF785A3C).copy(alpha = 0.25f), Offset(-u * 4.4f, -u * 11f), Size(u * 8.8f, u * 22f), style = Stroke(u * 0.6f))
                } }
            }
            BreathKind.THANKS -> {
                // 내쉰 숨마다 한 송이: 양옆으로 번갈아, 바깥으로 차츰
                val doneOuts = outs.count { it.startMs <= ms }; val cur = outs.lastOrNull { it.startMs <= ms && ms < it.startMs + it.lengthMs }
                val shown = min(doneOuts, Z.flowers.toInt())
                for (i in 0 until shown) {
                    val side = if (i % 2 == 0) -1f else 1f; val d = u * (52f + (i / 2) * 30f + 10f * hash(i, 9))
                    val img = flowers[(hash(i, 10) * flowers.size).toInt().coerceIn(0, flowers.size - 1)]
                    val g = if (i == shown - 1 && cur != null && animate) min(1f, (ms - cur.startMs) / cur.lengthMs.toFloat() * 1.3f) else 1f
                    val e = g * g * (3 - 2 * g); val kk = u * (Z.flowerSize + 10f * hash(i, 11)) / img.height * (0.55f + 0.45f * e)
                    val w = img.width * kk; val h = img.height * kk; val x = cx + side * d; val y = gy + u * (2f + 6f * hash(i, 12) + (i / 2) * 3f)
                    translate(x, y) { rotate((hash(i, 13) - 0.5f) * 23f * (2f - e), Offset.Zero) {
                        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset((-w / 2).toInt(), (-h).toInt()), IntSize(w.toInt(), h.toInt()), alpha = e)
                    } }
                }
            }
        }
    }
}

/** 마음 산책: 들이쉬는 네 걸음 · 내쉬는 네 걸음을 세는 작은 점 넷 (문구 밑). */
@Composable
internal fun WalkCount(plan: List<Breath.Phase>, elapsed: () -> Long, modifier: Modifier = Modifier) {
    val ink = Tokens.Garden.Colors.ink
    Canvas(modifier) {
        val a = Breath.at(plan, elapsed()) ?: return@Canvas
        val step = floor(a.second * 4f).toInt(); val r = size.height / 2; val gap = size.width / 4
        for (i in 0 until 4) drawCircle(ink.copy(alpha = if (i <= step) 0.45f else 0.12f), r, Offset(gap * (i + 0.5f), r))
    }
}

private fun DrawScope.glow(c: Offset, r: Float, col: Color, a: Float) { if (a <= 0f) return; drawCircle(Brush.radialGradient(listOf(col.copy(alpha = a.coerceIn(0f, 1f)), Color.Transparent), c, r), r, c) }
private fun hash(i: Int, k: Int): Float { val x = sin(i * 12.9898f + k * 78.233f) * 43758.547f; return x - floor(x) }
