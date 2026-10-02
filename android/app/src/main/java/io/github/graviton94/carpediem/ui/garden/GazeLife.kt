package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.design.Tokens
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import io.github.graviton94.carpediem.design.Tokens.Garden.Gaze as Z

/** 돌멍하기 정원의 자리 (GardenHome 이 bare 일 때 알려 줌): 땅선 · 내 하루 · 해와 달 · 낮인지 · 돌 머리 높이. */
internal data class GazeGeom(val gy: Dp, val haruX: Dp, val sunX: Dp, val sunY: Dp, val day: Boolean, val haruAbove: Dp)
internal val gazeGeom = mutableStateOf<GazeGeom?>(null)

/** 숨 한 번 (breathS 초: 4 들이쉬고 6 내쉬기) 의 찬 정도 0 … 1. 하루 · 빛 · 불빛이 모두 이 박자로. */
internal fun gazeBreath(t: Float): Float {
    val total = Z.breathIn + Z.breathOut
    val u = ((t % total) + total) % total
    return if (u < Z.breathIn) 0.5f - 0.5f * cos(PI.toFloat() * u / Z.breathIn) else 0.5f + 0.5f * cos(PI.toFloat() * (u - Z.breathIn) / Z.breathOut)
}

private const val TAU = 6.2832f
/** 같은 수면 같은 값 (0 … 1): 정해진 순서로 흩어지는 수. */
private fun hash(i: Int, k: Int): Float { val x = sin(i * 12.9898f + k * 78.233f) * 43758.547f; return x - floor(x) }
/** 부드러운 흔들림 (몇 개의 사인을 겹쳐, 되풀이가 눈에 띄지 않게). */
private fun nz(t: Float, k: Float) = 0.5f * sin(t * 1.13f + k * 1.7f) + 0.3f * sin(t * 2.31f + k * 4.1f) + 0.2f * sin(t * 3.97f + k * 2.3f)

/**
 * 돌멍하기의 움직이는 정원: 불멍 · 물멍처럼 오래 바라봐도 편한 느린 움직임만. 놀랄 일 없이, 되풀이가 보이지 않게.
 * 모든 계절: 하늘에 구름이 2–3분에 걸쳐 흐르고, 해 · 달빛이 숨처럼 일렁이고, 가끔 잎 (봄 꽃잎 · 겨울 눈) 이 내려와 눕고, 낮엔 가끔 먼 새 두 마리, 밤엔 반딧불 셋.
 * 봄 · 여름: 돌들 앞 작은 연못 (비친 빛 · 잔물결 · 잎이 닿으면 동그란 물결). 가을 · 겨울: 작은 화톳불 (불꽃 · 불티 · 돌들 얼굴의 따뜻한 빛).
 * dim (0 … dimAlpha) = 화면이 어두워진 만큼 움직임도 느려짐. 움직임을 끈 기기는 빛의 숨만.
 */
@Composable
internal fun GazeLife(season: Season, now: LocalDateTime, dim: () -> Float) {
    val ctx = LocalContext.current
    val g = gazeGeom.value ?: return
    val moving = remember { !reducedMotion(ctx) }
    val dark = SkyTime.isDark(now)
    val key = GardenArt.key(season)
    val pond = season == Season.SPRING || season == Season.SUMMER
    // 시계: 어두워질수록 천천히 흐름 (그리는 단계에서만 읽음)
    val clock = remember { mutableFloatStateOf(Z.startAt) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) withFrameNanos { n -> if (n - last >= FRAME_NS) { val dt = (n - last) / 1e9f; last = n; clock.floatValue += dt * (1f - Z.dimSlow * (dim() / Z.dimAlpha).coerceIn(0f, 1f)) } }
    }
    val clouds = remember(key, dark) { listOf(0, 1).map { GardenArt.image(ctx, "cloud_${if (dark) "night" else key}_$it.webp") } }
    val piece = remember(key) { GardenArt.image(ctx, "wind_$key.webp") }
    val pondImg = remember { if (pond) GardenArt.image(ctx, "gaze_pond.webp") else null }
    val logs = remember { if (!pond) GardenArt.image(ctx, "gaze_logs.webp") else null }
    val filter = nightFilter(dark)
    val ink = Tokens.Garden.Colors.ink
    Canvas(Modifier.fillMaxSize()) {
        val t = clock.floatValue
        val u = size.width / Tokens.Garden.unitWidth
        val gy = g.gy.toPx()
        val b = gazeBreath(t)
        // 해 · 달빛이 숨처럼
        val sun = Offset(g.sunX.toPx(), g.sunY.toPx()); val lc = if (dark) Color(0xFFF1E4BE) else Color(0xFFFAD68C)
        drawCircle(Brush.radialGradient(listOf(lc.copy(alpha = Z.lightBase + Z.lightBreath * b), Color.Transparent), sun, u * 90f), u * 90f, sun)
        if (!moving) return@Canvas
        // 구름: 나무 꼭대기보다 위 하늘에서 천천히 흐름
        val top = max(u * 40f, gy - u * 470f)
        listOf(Triple(0.15f, 2.6f, 0.95f), Triple(0.55f, 1.9f, 0.72f), Triple(0.85f, 2.2f, 0.82f)).forEachIndexed { i, (x0, sp, k) ->
            val w = u * 120f * k; val h = u * 50f * k
            val x = ((x0 * size.width + t * sp * u * Z.cloudSpeed) % (size.width + w * 1.4f)) - w * 0.7f
            val y = top + u * (24f + 46f * i) - h / 2
            drawImage(clouds[i % 2], IntOffset.Zero, IntSize(clouds[i % 2].width, clouds[i % 2].height), IntOffset(x.toInt(), y.toInt()), IntSize(w.toInt(), h.toInt()), alpha = 0.92f, colorFilter = null)
        }
        // 낮: 가끔 먼 새 두 마리 (birdEvery 초마다 한 번, 26초 동안 건넘)
        if (!dark) { val n = floor(t / Z.birdEvery).toInt(); val a = t - n * Z.birdEvery - 6f
            if (a in 0f..26f) { val q = a / 26f; val dir = if (hash(n, 1) < 0.5f) 1f else -1f
                val y0 = top + u * (10f + 30f * hash(n, 2))
                listOf(0f to 0f, 1f to 1f).forEach { (k, _) ->
                    val x = if (dir > 0) -u * 20f + (size.width + u * 40f) * q else size.width + u * 20f - (size.width + u * 40f) * q
                    val bx = x - dir * k * u * 16f; val by = y0 + k * u * 7f + u * 2f * sin(t * 0.7f + k)
                    val glide = ((a + k * 0.9f) % 4f) > 2.6f; val wing = if (glide) 0.25f else 0.25f + 0.75f * (0.5f + 0.5f * sin((a + k * 0.4f) * TAU * 1.3f))
                    val s = u * 5f
                    val path = Path().apply { moveTo(bx - s, by - s * wing); quadraticTo(bx - s * 0.4f, by - s * 0.15f * wing, bx, by + s * 0.2f); quadraticTo(bx + s * 0.4f, by - s * 0.15f * wing, bx + s, by - s * wing) }
                    drawPath(path, ink.copy(alpha = 0.5f * min(1f, min(q, 1f - q) * 12f)), style = Stroke(u * 1.1f, cap = StrokeCap.Round))
                }
            }
        }
        // 연못 (봄 · 여름)
        val pc = Offset(size.width / 2, gy + u * Z.pondY); val pw = u * Tokens.Garden.Decor.pondBoxW * Z.pondScale; val ph = u * Tokens.Garden.Decor.pondBoxH * Z.pondScale
        if (pondImg != null) {
            drawImage(pondImg, IntOffset.Zero, IntSize(pondImg.width, pondImg.height), IntOffset((pc.x - pw / 2).toInt(), (pc.y - ph / 2).toInt()), IntSize(pw.toInt(), ph.toInt()), colorFilter = filter)
            // 물에 비친 해 · 달: 물결에 부서진 빛 조각이 세로로 늘어서 일렁임
            val mx = (sun.x).coerceIn(pc.x - pw * 0.3f, pc.x + pw * 0.3f); val rc = if (dark) Color(0xFFF1E4BE) else Color(0xFFFFECBE)
            for (j in 0 until 7) { val yy = pc.y - ph * 0.28f + j * ph * 0.08f; val wv = u * (1f - j / 9f) * (14f + 5f * hash(j, 3)) * (0.6f + 0.4f * sin(t * 0.7f + j))
                val sh = u * (3f * sin(t * 0.9f + j * 1.3f) + 1.5f * sin(t * 1.7f + j)); val al = 0.5f * (0.55f + 0.45f * sin(t * 1.1f + j * 2.1f)) * (1f - j / 10f)
                drawOval(rc.copy(alpha = al), Offset(mx + sh - wv, yy - u * 1.2f), Size(wv * 2, u * 2.4f)) }
            drawCircle(Brush.radialGradient(listOf(rc.copy(alpha = (0.12f + 0.08f * b)), Color.Transparent), Offset(mx, pc.y - ph * 0.1f), u * 40f), u * 40f, Offset(mx, pc.y - ph * 0.1f))
            // 물 위 빛줄: 천천히 흘러감
            for (j in 0 until 7) { val len = u * (10f + 18f * hash(j, 5)); val span = pw * 0.7f - len
                val lx = pc.x - pw * 0.35f + ((hash(j, 4) * span + t * u * (3f + 3f * hash(j, 6))) % span); val ly = pc.y - ph * 0.3f + ph * 0.6f * hash(j, 7)
                drawLine((if (dark) Color(0xFFF1E4BE) else Color.White).copy(alpha = (if (dark) 0.18f else 0.5f) * (0.5f + 0.5f * sin(t * 0.6f + j))), Offset(lx, ly), Offset(lx + len, ly), u * 0.9f, StrokeCap.Round) }
        }
        // 화톳불 (가을 · 겨울): 돌들 왼쪽 앞
        val fire = Offset((g.haruX.toPx() - u * Z.fireFromHaru).coerceAtLeast(u * 44f), gy + u * Z.fireY)
        if (logs != null) fireBack(fire, t, dark, b, u)
        // 내려오는 잎 (겨울은 눈송이가 하늘에서): pieceEvery 초마다 한 장
        val n0 = floor(t / Z.pieceEvery).toInt()
        for (n in n0 - 2..n0) { if (n < 0) continue
            val t0 = n * Z.pieceEvery + hash(n, 9) * Z.pieceEvery * 0.5f; val a = t - t0; if (a < 0f) continue
            val fall = Z.pieceFall + 3f * hash(n, 10); val snow = season == Season.WINTER
            val x0 = if (snow) size.width * (0.1f + 0.8f * hash(n, 11)) else u * (Tokens.Garden.Decor.treeX + 10f + 90f * hash(n, 11))
            val y0 = if (snow) top else gy - u * (150f + 50f * hash(n, 12))
            val onWater = pondImg != null && hash(n, 13) < 0.5f
            val amp = u * (14f + 12f * hash(n, 14)); val w = TAU / (3.2f + 1.4f * hash(n, 15)); val ph0 = hash(n, 16) * TAU
            val drift = u * 4f
            val landX0 = x0 + amp * sin(w * fall + ph0) + fall * drift
            val ly = if (onWater) pc.y - ph * 0.3f + ph * 0.6f * hash(n, 17) else gy + u * (8f + 24f * hash(n, 17))
            val landX = if (onWater) landX0.coerceIn(pc.x - pw * 0.38f, pc.x + pw * 0.38f) else landX0
            val p = min(1f, a / fall)
            val x = if (a < fall) x0 + amp * sin(w * a + ph0) + a * drift + (landX - landX0) * p else landX
            val y = y0 + (ly - y0) * p
            val rot = if (a < fall) 0.7f * cos(w * a + ph0) else 0.15f
            val al = if (a < fall) min(1f, a / 0.8f) else (1f - (a - fall - Z.pieceRest) / 2f).coerceIn(0f, 1f)
            if (al <= 0f) continue
            if (onWater && a >= fall && a - fall < 4.5f) ripple(Offset(x, y), (a - fall) / 4.5f, u)
            val sz = u * Z.pieceSize; val flip = 0.4f + 0.6f * abs(cos(w * a * 0.7f + ph0))
            translate(x, y) { rotate(rot * 57.3f, Offset.Zero) { scale(flip, 1f, Offset.Zero) {
                drawImage(piece, IntOffset.Zero, IntSize(piece.width, piece.height), IntOffset((-sz / 2).toInt(), (-sz / 2).toInt()), IntSize(sz.toInt(), sz.toInt()), alpha = al * (if (onWater && a >= fall) 0.85f else 1f), colorFilter = filter)
            } } }
        }
        if (logs != null) fireFront(fire, logs, t, dark, b, u, filter)
        // 밤: 반딧불 셋이 천천히
        if (dark) for (f in 0 until 3) {
            val fx = size.width * (0.3f + 0.18f * f) + u * (18f * sin(t / (6f + f) * PI.toFloat() + f) + 5f * sin(t * 0.5f + f * 2f)); val fy = gy - u * (40f + 14f * f) + u * 7f * cos(t / (7f + f) * PI.toFloat() + f)
            val lit = 0.08f + 0.92f * glowPulse(((t + f * 1.3f) % (3f + f * 0.7f)) / (3f + f * 0.7f)); val col = Tokens.Garden.Night.Colors.firefly
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.5f * lit), Color.Transparent), Offset(fx, fy), u * 9f), u * 9f, Offset(fx, fy))
            drawCircle(col.copy(alpha = lit), u * 1.2f, Offset(fx, fy))
        }
    }
}

/** 물에 닿은 자리의 동그란 물결 둘 (q = 0 … 1, 4.5초). */
private fun DrawScope.ripple(c: Offset, q: Float, u: Float) {
    for (k in 0 until 2) { val p = q - k * 0.18f; if (p <= 0f) continue
        val rx = u * (4f + p * 32f); drawOval(Color.White.copy(alpha = (1f - p) * 0.55f), Offset(c.x - rx, c.y - rx * 0.28f), Size(rx * 2, rx * 0.56f), style = Stroke(u * 0.9f)) }
}

/** 불 뒤의 빛: 땅과 둘레가 숨 · 불꽃에 맞춰 따뜻하게 일렁임. */
private fun DrawScope.fireBack(f: Offset, t: Float, dark: Boolean, b: Float, u: Float) {
    val k = (if (dark) 1f else 0.35f) * (0.85f + 0.1f * nz(t * 1.6f, 3f) + 0.08f * b)
    val c = Offset(f.x, f.y - u * 14f); val warm = Color(0xFFFFAA5A)
    drawCircle(Brush.radialGradient(0f to warm.copy(alpha = 0.32f * k), 0.5f to warm.copy(alpha = 0.12f * k), 1f to Color.Transparent, center = c, radius = u * 150f), u * 150f, c)
}

/** 한지 불꽃 한 장: 끝이 느리게 일렁이는 물방울 모양 + 옅은 종이 가장자리. */
private fun DrawScope.flame(x: Float, y: Float, h: Float, w: Float, t: Float, k: Float, col: Color) {
    val tip = nz(t * 1.1f, k) * w * 0.55f; val sway = nz(t * 0.8f, k + 5f) * w * 0.25f; val hh = h * (0.88f + 0.12f * nz(t * 1.7f, k + 9f))
    val p = Path().apply { moveTo(x - w, y); cubicTo(x - w * 1.05f, y - hh * 0.45f, x - w * 0.35f + sway, y - hh * 0.7f, x + tip, y - hh); cubicTo(x + w * 0.35f + sway, y - hh * 0.7f, x + w * 1.05f, y - hh * 0.45f, x + w, y); quadraticTo(x, y + w * 0.35f, x - w, y); close() }
    drawPath(p, col); drawPath(p, Color(0xFFFFF4DC).copy(alpha = 0.5f), style = Stroke(w * 0.06f))
}

/** 장작 · 불꽃 넷 · 불티 · (낮엔) 아지랑이 · 돌들 얼굴에 일렁이는 빛. */
private fun DrawScope.fireFront(f: Offset, logs: ImageBitmap, t: Float, dark: Boolean, b: Float, u: Float, filter: androidx.compose.ui.graphics.ColorFilter?) {
    val s = u * (if (dark) 1.2f else 0.7f)
    val lw = u * Tokens.Garden.Decor.fireBoxW; val lh = u * Tokens.Garden.Decor.fireBoxH
    drawImage(logs, IntOffset.Zero, IntSize(logs.width, logs.height), IntOffset((f.x - lw / 2).toInt(), (f.y + u * 9f - lh).toInt()), IntSize(lw.toInt(), lh.toInt()), colorFilter = filter)
    val by = f.y - u * 1f
    flame(f.x, by, 46f * s, 15f * s, t, 1f, Color(0xFFE8955A)); flame(f.x - 7f * s, by, 30f * s, 9f * s, t + 1.3f, 2f, Color(0xFFC8553D))
    flame(f.x + 8f * s, by, 34f * s, 9f * s, t + 2.1f, 3f, Color(0xFFD9744E)); flame(f.x + u, by - u, 24f * s, 7f * s, t + 0.6f, 4f, Color(0xFFF6C979))
    // 불티: 하나둘 올라가다 꺼짐
    for (k in 0 until 4) { val cyc = 3.2f + k * 0.7f; val a = ((t + k * 1.1f) % cyc) / cyc
        val ex = f.x + nz(t * 0.7f, k * 3f) * u * 10f + (k - 1.5f) * u * 4f + a * u * 6f * sin(k.toFloat()); val ey = by - 30f * s - a * 120f * s
        val al = (if (dark) 1f else 0.5f) * sin(PI.toFloat() * min(1f, a * 1.3f)) * (1f - a); if (al > 0f) drawCircle(Color(0xFFFFD28C).copy(alpha = al), u * 1.1f, Offset(ex, ey)) }
    if (!dark) for (j in 0 until 3) { val a = (t * 0.3f + j / 3f) % 1f; val y0 = by - u * 50f - a * u * 70f
        val p = Path().apply { moveTo(f.x - u * 6f + j * u * 5f, y0); for (q in 1..6) lineTo(f.x - u * 6f + j * u * 5f + u * 3f * sin(q * 1.4f + t * 1.5f + j), y0 - q * u * 5f) }
        drawPath(p, Color.White.copy(alpha = 0.3f * sin(PI.toFloat() * a)), style = Stroke(u)) }
    val k2 = (if (dark) 1f else 0.3f) * (0.8f + 0.2f * nz(t * 1.9f, 7f)); val c = Offset(f.x + u * 40f, by - u * 20f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFB46E).copy(alpha = 0.22f * k2), Color.Transparent), c, u * 130f), u * 130f, c, blendMode = BlendMode.Softlight)
}
