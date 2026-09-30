package io.github.graviton94.carpediem.ui.garden

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import io.github.graviton94.carpediem.core.HaruShape
import io.github.graviton94.carpediem.design.Tokens
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 하루 한 명의 모양 (번호 하나로 정해짐, core/HaruShape). 그림 파일 · WebView 없이 앱이 벡터로 그린다:
 * 단색 몸 + 먹선 윤곽 + 동그란 눈. 눈을 감을 때는 그림을 접지 않고 돌 색 눈꺼풀이 위에서 내려온다
 * (예전에는 눈 그림을 세로로 눌러 접어서, 기기마다 흰자와 눈동자가 어긋나 보였다).
 * 좌표는 box × box 칸, 땅은 y = ground (토큰 haruBox · haruGround).
 */
class HaruArt(val shape: HaruShape.Shape, val sprout: Boolean) {
    class Meta(val box: Float, val ground: Float, val stone: String, val top: Offset, val bbox: Rect)
    val meta = Meta(
        HaruShape.BOX.toFloat(), Tokens.Garden.Layout.haruGround, shape.traits.stone.id,
        Offset(shape.top.first.toFloat(), shape.top.second.toFloat()),
        Rect(shape.left.toFloat(), shape.upper.toFloat(), shape.right.toFloat(), HaruShape.GROUND.toFloat()),
    )
    /** 매끈한 몸 윤곽 (점 사이를 2차 곡선으로), 칸 좌표. */
    val body: Path = smooth(shape.body.map { Offset(it.first.toFloat(), it.second.toFloat()) })
    val bodyColor = Color(0xFF000000 or shape.bodyColor.toLong())

    companion object {
        private val cache = HashMap<Pair<Long, Boolean>, HaruArt>()
        fun of(seed: Long, sprout: Boolean): HaruArt = synchronized(cache) { cache.getOrPut(seed to sprout) { HaruArt(HaruShape.of(seed), sprout) } }

        fun smooth(pts: List<Offset>): Path = Path().apply {
            val n = pts.size
            val m0 = (pts[n - 1] + pts[0]) / 2f
            moveTo(m0.x, m0.y)
            for (i in 0 until n) { val c = pts[i]; val nx = pts[(i + 1) % n]; val mid = (c + nx) / 2f; quadraticTo(c.x, c.y, mid.x, mid.y) }
            close()
        }
    }
}

/**
 * 하루를 그린다 (앱 화면 · 위젯이 같이 씀). k = 한 칸의 크기(px).
 * lid: 0 = 눈을 뜸, 1 = 감음. look: 기본 시선에 더할 기울기. sleepy: 밤이면 아래를 봄.
 */
fun DrawScope.drawHaru(art: HaruArt, k: Float, lid: Float = 0f, look: Offset = Offset.Zero, sleepy: Boolean = false) {
    val d = Tokens.Garden.HaruDraw
    val ink = Tokens.Garden.Colors.ink
    val t = art.shape.traits
    val bb = art.meta.bbox
    // 땅 위의 옅은 그늘
    drawOval(ink.copy(alpha = d.shadow), Offset((bb.left + bb.width * 0.08f) * k, (bb.bottom - 3f) * k), Size(bb.width * 0.84f * k, 6f * k))
    // 몸은 칸 좌표 그대로 그리고 전체를 k 배로 (선 굵기도 함께 커짐)
    scale(k, k, pivot = Offset.Zero) {
        drawPath(art.body, art.bodyColor)
        clipPath(art.body) { stonePattern(art, 1f) }   // 돌의 무늬 (아주 옅게, 몸 안에서만)
        drawPath(art.body, ink, style = Stroke(d.line, join = StrokeJoin.Round))
    }
    if (art.sprout) sprout(art, k)

    art.shape.eyes.forEachIndexed { i, e ->
        val c = Offset(e.x.toFloat() * k, e.y.toFloat() * k); val r = e.r.toFloat() * k
        val eye = Path().apply { addOval(Rect(c, r)) }
        if (lid >= d.closedAt) {
            // 감은 눈: 아래로 둥근 한 줄
            val p = Path().apply { moveTo(c.x - r * 0.9f, c.y); quadraticTo(c.x, c.y + r * 0.55f, c.x + r * 0.9f, c.y) }
            drawPath(p, ink, style = Stroke(d.eyeLine * k * 1.2f, cap = StrokeCap.Round))
            return@forEachIndexed
        }
        drawPath(eye, Tokens.Garden.Colors.shine)
        clipPath(eye) {
            val pr = r * t.pupil.toFloat()
            var lx = t.lookX.toFloat() + (if (i == 0) -t.spread.toFloat() else t.spread.toFloat()) + look.x
            var ly = if (sleepy) Tokens.Garden.Motion.sleepyLook else t.lookY.toFloat() + look.y
            val len = hypot(lx, ly); if (len > 1f) { lx /= len; ly /= len }
            val lim = r - pr - r * 0.1f
            val pc = Offset(c.x + lx * lim, c.y + ly * lim + lim * 0.2f)
            drawCircle(Tokens.Garden.Colors.pupil, pr, pc)
            drawCircle(Tokens.Garden.Colors.shine, pr * 0.26f, pc + Offset(-pr * 0.36f, -pr * 0.38f))
            if (lid > 0f) {
                // 눈꺼풀: 돌 색이 위에서 내려오고, 끝에 먹선 한 줄
                val y = c.y - r + 2f * r * lid
                val cover = Path().apply {
                    moveTo(c.x - r * 1.2f, c.y - r * 1.2f); lineTo(c.x + r * 1.2f, c.y - r * 1.2f); lineTo(c.x + r * 1.2f, y)
                    quadraticTo(c.x, y + r * d.lidCurve, c.x - r * 1.2f, y); close()
                }
                drawPath(cover, art.bodyColor)
                val edge = Path().apply { moveTo(c.x - r * 1.2f, y); quadraticTo(c.x, y + r * d.lidCurve, c.x + r * 1.2f, y) }
                drawPath(edge, ink, style = Stroke(d.eyeLine * k))
            }
        }
        drawPath(eye, ink, style = Stroke(d.eyeLine * k))
    }
}

/** 돌 종류마다 다른 옅은 무늬 (번호로 정해짐). */
private fun DrawScope.stonePattern(art: HaruArt, k: Float) {
    val t = art.shape.traits
    val bb = art.meta.bbox
    val r = Crayon.Rng(t.texSeed.toInt())
    val a = Tokens.Garden.HaruDraw.pattern
    fun x() = (bb.left + bb.width * r.next()) * k
    fun y() = (bb.top + bb.height * r.next()) * k
    when (t.stone.pattern) {
        "salt", "speckle" -> repeat(if (t.stone.pattern == "speckle") 10 else 16) {
            val dark = r.next() < 0.6f
            drawCircle(if (dark) Color.Black.copy(alpha = a) else Color.White.copy(alpha = a * 1.4f), bb.width * k * (0.012f + 0.014f * r.next()), Offset(x(), y()))
        }
        "layers", "marble" -> repeat(3) {
            val yy = y()
            val p = Path().apply { moveTo(bb.left * k, yy); quadraticTo(bb.center.x * k, yy + (r.next() - 0.5f) * bb.height * 0.4f * k, bb.right * k, yy + (r.next() - 0.5f) * bb.height * 0.2f * k) }
            drawPath(p, (if (t.stone.pattern == "marble") Color.Black else Color.White).copy(alpha = a * 1.2f), style = Stroke(bb.height * 0.018f * k))
        }
        "vein", "ring" -> {
            val yy = (bb.top + bb.height * (0.4f + 0.25f * r.next())) * k
            val p = Path().apply { moveTo(bb.left * k, yy); quadraticTo(bb.center.x * k, yy + bb.height * 0.2f * k, bb.right * k, yy - bb.height * 0.06f * k) }
            drawPath(p, Color.White.copy(alpha = a * 3f), style = Stroke(bb.height * 0.07f * k))
        }
        else -> {}
    }
}

/** 봄의 새싹: 머리 꼭대기에서 줄기 하나, 잎 두 장. */
private fun DrawScope.sprout(art: HaruArt, k: Float) {
    val d = Tokens.Garden.HaruDraw
    val ink = Tokens.Garden.Colors.ink
    val top = art.meta.top * k
    val h = d.sproutHeight * k
    val stem = Path().apply { moveTo(top.x, top.y + d.line * k * 0.4f); quadraticTo(top.x + h * 0.12f, top.y - h * 0.5f, top.x - h * 0.04f, top.y - h) }
    drawPath(stem, ink, style = Stroke(d.line * k * 0.6f, cap = StrokeCap.Round))
    val tip = Offset(top.x - h * 0.04f, top.y - h)
    listOf(-1f to Tokens.Garden.Colors.chip, 1f to Tokens.Garden.Colors.button).forEach { (side, col) ->
        val c = tip + Offset(side * h * 0.42f, -h * 0.06f)
        rotate(side * -18f, c) {
            val tl = Offset(c.x - h * 0.42f, c.y - h * 0.2f); val sz = Size(h * 0.84f, h * 0.4f)
            drawOval(col, tl, sz)
            drawOval(ink, tl, sz, style = Stroke(d.line * k * 0.5f))
        }
    }
}

/** 시스템에서 ‘애니메이션 없애기’를 켰는지. 켜면 깜빡임 · 눈동자 굴림을 멈춘다. */
fun reducedMotion(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** 폰 기울기 (-1 … 1). 옆으로 기울이면 x, 뒤로 눕히면 y. */
@Composable
fun rememberTilt(enabled: Boolean): Offset {
    val ctx = LocalContext.current
    var tilt by remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(enabled) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val range = Tokens.Garden.Motion.tiltRange
        val ease = Tokens.Garden.Motion.lookEase
        // 평소에 쥐는 각도를 '정면'으로: 기울기의 느린 평균을 빼서, 폰을 움직일 때만 눈이 굴러가고 곧 제자리로 온다.
        var rest: Offset? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val raw = Offset(-e.values[0], -e.values[1])
                val base = rest?.let { it + (raw - it) * Tokens.Garden.Motion.restEase } ?: raw
                rest = base
                val d = raw - base
                val target = Offset((d.x / range).coerceIn(-1f, 1f), (d.y / range).coerceIn(-1f, 1f))
                tilt += (target - tilt) * ease
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (enabled && sensor != null) sm?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm?.unregisterListener(listener) }
    }
    return if (enabled) tilt else Offset.Zero
}

/**
 * 하루 한 명. 가끔 저절로 깜빡이고, 폰을 움직이면 눈동자가 따라 굴렀다 돌아온다.
 * 밤(sleepFrom ~ sleepTo)에는 눈꺼풀이 반쯤 내려오고 아래를 본다. 위치는 부르는 쪽에서 정한다 (크기 = box × scale).
 */
@Composable
fun HaruFigure(art: HaruArt, scale: Dp, modifier: Modifier = Modifier, sleepy: Boolean = false, blinkKick: Int = 0) {
    val ctx = LocalContext.current
    val m = Tokens.Garden.Motion
    val animate = remember { !reducedMotion(ctx) }
    val blink = remember { Animatable(0f) }
    suspend fun blinkOnce() { blink.animateTo(1f, tween((m.blinkMs / 2).roundToInt())); blink.animateTo(0f, tween((m.blinkMs / 2).roundToInt())) }
    if (animate) {
        LaunchedEffect(Unit) { while (true) { delay(Random.nextLong(m.blinkMinMs.toLong(), m.blinkMaxMs.toLong())); blinkOnce() } }
        LaunchedEffect(blinkKick) { if (blinkKick > 0) blinkOnce() }
    }
    val tilt = rememberTilt(animate && !sleepy)
    Canvas(modifier.size(scale * art.meta.box)) {
        drawHaru(art, size.width / art.meta.box, maxOf(blink.value, if (sleepy) m.sleepyLid else 0f), tilt, sleepy)
    }
}
