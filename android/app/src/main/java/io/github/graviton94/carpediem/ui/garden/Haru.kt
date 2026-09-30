package io.github.graviton94.carpediem.ui.garden

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.graviton94.carpediem.design.Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

/** 하루의 그림. 몸(눈 없음) · 눈 흰자 두 장과, 앱이 직접 그릴 눈동자 정보. 좌표는 box × box 칸, 땅은 y = ground. */
class HaruArt(val body: ImageBitmap, val eyes: ImageBitmap, val meta: Meta) {
    class Eye(val x: Float, val y: Float, val r: Float)
    class Meta(val box: Float, val ground: Float, val stone: String, val eyes: List<Eye>, val pupil: Float, val spread: Float, val lookX: Float, val lookY: Float, val top: Offset, val bbox: Rect)
}

/**
 * 하루는 번호마다 달라서 미리 구울 수 없다. 앱에 든 assets/garden/haru.html(보기용 시안과 같은 JS)로
 * 처음 한 번 그려 files/haru 에 저장하고, 다음부터는 저장한 것을 쓴다.
 */
object HaruArtStore {
    private const val VERSION = 2
    private const val PX = 1024
    private const val TIMEOUT_MS = 15_000L
    private val memory = HashMap<String, HaruArt>()

    suspend fun get(context: Context, seed: Long, sprout: Boolean): HaruArt? {
        val key = "v${VERSION}_${seed}_${if (sprout) 1 else 0}"
        synchronized(memory) { memory[key] }?.let { return it }
        val dir = File(context.filesDir, "haru").apply { mkdirs() }
        val metaFile = File(dir, "$key.json"); val bodyFile = File(dir, "$key.body.png"); val eyesFile = File(dir, "$key.eyes.png")
        if (!metaFile.exists() || !bodyFile.exists() || !eyesFile.exists()) {
            val json = withTimeoutOrNull(TIMEOUT_MS) { render(context.applicationContext, seed, sprout) } ?: return null
            withContext(Dispatchers.IO) {
                bodyFile.writeBytes(dataUrl(json.getString("body"))); eyesFile.writeBytes(dataUrl(json.getString("eyes")))
                json.remove("body"); json.remove("eyes"); metaFile.writeText(json.toString())
            }
            io.github.graviton94.carpediem.widget.Widgets.refresh(context)
        }
        val art = withContext(Dispatchers.IO) {
            runCatching {
                val m = JSONObject(metaFile.readText())
                val given = m.getJSONArray("eye").let { a -> (0 until a.length()).map { i -> a.getJSONObject(i).let { HaruArt.Eye(it.f("x"), it.f("y"), it.f("r")) } } }
                val top = m.getJSONArray("top"); val bb = m.getJSONArray("bbox")
                val eyesBmp = BitmapFactory.decodeFile(eyesFile.path)
                val eyes = if (m.optBoolean("fitted")) given else fitEyes(eyesBmp, given, m.f("box")).also { fit ->
                    // 위젯도 같은 자리를 쓰도록 저장해 둔다
                    m.put("eye", org.json.JSONArray(fit.map { JSONObject().put("x", it.x.toDouble()).put("y", it.y.toDouble()).put("r", it.r.toDouble()) }))
                    m.put("fitted", true); metaFile.writeText(m.toString())
                }
                HaruArt(
                    BitmapFactory.decodeFile(bodyFile.path).asImageBitmap(), eyesBmp.asImageBitmap(),
                    HaruArt.Meta(m.f("box"), m.f("ground"), m.getString("stone"), eyes, m.f("pupil"), m.f("spread"), m.f("lookX"), m.f("lookY"),
                        Offset(top.getDouble(0).toFloat(), top.getDouble(1).toFloat()),
                        Rect(bb.getDouble(0).toFloat(), bb.getDouble(1).toFloat(), (bb.getDouble(0) + bb.getDouble(2)).toFloat(), (bb.getDouble(1) + bb.getDouble(3)).toFloat())),
                )
            }.getOrNull()
        } ?: return null
        synchronized(memory) { memory[key] = art }
        return art
    }

    /** 위젯이 쓰는 저장된 그림 (없으면 null). 위젯은 WebView 를 띄우지 않는다. */
    fun files(context: Context, seed: Long, sprout: Boolean): Triple<File, File, File>? {
        val key = "v${VERSION}_${seed}_${if (sprout) 1 else 0}"
        val dir = File(context.filesDir, "haru")
        val t = Triple(File(dir, "$key.body.png"), File(dir, "$key.eyes.png"), File(dir, "$key.json"))
        return t.takeIf { it.first.exists() && it.second.exists() && it.third.exists() }
    }

    private fun JSONObject.f(k: String) = getDouble(k).toFloat()

    /**
     * 눈 흰자 그림에서 실제 눈의 가운데를 다시 잰다. 기기 WebView 마다 그림이 조금 어긋날 수 있어서,
     * 눈동자 · 깜빡임 자리를 적어 둔 좌표가 아니라 그려진 흰자에 맞춘다 (크기는 적어 둔 값).
     */
    private fun fitEyes(bmp: Bitmap, given: List<HaruArt.Eye>, box: Float): List<HaruArt.Eye> {
        val w = bmp.width; val h = bmp.height; val k = w / box
        val px = IntArray(w * h).also { bmp.getPixels(it, 0, w, 0, 0, w, h) }
        return given.mapIndexed { i, e ->
            val cx = e.x * k; val cy = e.y * k; val reach = e.r * k * 2.2f
            var x0 = Int.MAX_VALUE; var y0 = Int.MAX_VALUE; var x1 = -1; var y1 = -1
            for (y in (cy - reach).toInt().coerceAtLeast(0) until (cy + reach).toInt().coerceAtMost(h)) for (x in (cx - reach).toInt().coerceAtLeast(0) until (cx + reach).toInt().coerceAtMost(w)) {
                if ((px[y * w + x] ushr 24) < 128) continue
                // 두 눈이 붙어 있어도 가까운 쪽 눈에만 센다
                val mine = hypot(x - cx, y - cy); if (given.withIndex().any { (j, o) -> j != i && hypot(x - o.x * k, y - o.y * k) < mine }) continue
                if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y
            }
            if (x1 < 0) e else HaruArt.Eye((x0 + x1) / 2f / k, (y0 + y1) / 2f / k, e.r)
        }
    }
    private fun dataUrl(s: String): ByteArray = Base64.decode(s.substringAfter(","), Base64.DEFAULT)

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun render(context: Context, seed: Long, sprout: Boolean): JSONObject? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val web = WebView(context)
            web.settings.javaScriptEnabled = true
            web.settings.allowFileAccess = true
            var done = false
            fun finish(v: JSONObject?) { if (!done) { done = true; web.destroy(); if (cont.isActive) cont.resume(v) } }
            web.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    view.evaluateJavascript("exportHaru($seed, $PX, $sprout)") { raw ->
                        finish(runCatching { (JSONTokener(raw).nextValue() as? String)?.let { JSONObject(it) } }.getOrNull())
                    }
                }
            }
            cont.invokeOnCancellation { if (!done) { done = true; web.post { web.destroy() } } }
            web.loadUrl("file:///android_asset/garden/haru.html")
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
 * 하루 한 명. 몸 → 눈 흰자(깜빡일 때 세로로 접힘) → 눈동자(기울기를 따라 굴러감).
 * 밤(sleepFrom ~ sleepTo)에는 눈을 반쯤 감고 아래를 본다. 위치는 부르는 쪽에서 정한다 (크기 = box × scale).
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
    val mt = art.meta
    Canvas(modifier.size(scale * mt.box)) {
        val k = size.width / mt.box
        val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
        drawImage(art.body, dstSize = dst)
        val lid = maxOf(blink.value, if (sleepy) m.sleepyLid else 0f)
        mt.eyes.forEachIndexed { i, e ->
            val c = Offset(e.x * k, e.y * k); val r = e.r * k * 1.35f
            clipRect(c.x - r, c.y - r, c.x + r, c.y + r) {
                scale(1f, 1f - lid * 0.9f, pivot = c) { drawImage(art.eyes, dstOffset = IntOffset.Zero, dstSize = dst) }
            }
            if (lid < 0.6f) {
                val pr = e.r * mt.pupil * k
                var lx = mt.lookX + (if (i == 0) -mt.spread else mt.spread) + tilt.x
                var ly = if (sleepy) m.sleepyLook else mt.lookY + tilt.y
                val len = hypot(lx, ly); if (len > 1f) { lx /= len; ly /= len }
                val lim = e.r * k - pr - e.r * k * 0.1f
                val p = Offset(c.x + lx * lim, c.y + ly * lim * (1f - lid) + lim * 0.2f)
                drawCircle(Tokens.Garden.Colors.pupil, pr, p)
                drawCircle(Tokens.Garden.Colors.shine, pr * 0.24f, p + Offset(-pr * 0.35f, -pr * 0.38f))
            } else if (lid > 0.8f) {
                val w = e.r * k
                drawLine(Tokens.Garden.Colors.ink, Offset(c.x - w, c.y), Offset(c.x + w, c.y), Tokens.Garden.Stroke.closedEye * k, StrokeCap.Round)
            }
        }
    }
}

/**
 * 하루를 그리지 못했을 때(아주 드묾: WebView 가 없거나 막힌 기기) 잠시 보여 주는 단순한 돌.
 * 같은 box 좌표를 써서 자리가 어긋나지 않는다. 다음에 앱을 열면 다시 그려 본다.
 */
@Composable
fun HaruFallback(scale: Dp, modifier: Modifier = Modifier) {
    val box = Tokens.Garden.Layout.haruBox; val g = Tokens.Garden.Layout.haruGround
    Canvas(modifier.size(scale * box)) {
        val k = size.width / box
        val w = Tokens.Garden.Layout.haruArtWidth * k; val h = w * 0.62f
        val c = Offset(size.width / 2, g * k - h)
        drawOval(Tokens.Garden.Colors.fallback, Offset(c.x - w, c.y - h), androidx.compose.ui.geometry.Size(w * 2, h * 2))
        drawOval(Tokens.Garden.Colors.ink, Offset(c.x - w, c.y - h), androidx.compose.ui.geometry.Size(w * 2, h * 2), style = androidx.compose.ui.graphics.drawscope.Stroke(Tokens.Garden.Stroke.box * k))
        listOf(-0.3f, 0.12f).forEach { dx ->
            val e = Offset(c.x + dx * w, c.y - h * 0.15f); val r = w * 0.17f
            drawCircle(Tokens.Garden.Colors.shine, r, e)
            drawCircle(Tokens.Garden.Colors.ink, r, e, style = androidx.compose.ui.graphics.drawscope.Stroke(Tokens.Garden.Stroke.closedEye * k))
            drawCircle(Tokens.Garden.Colors.pupil, r * 0.52f, e + Offset(0f, r * 0.15f))
        }
    }
}
