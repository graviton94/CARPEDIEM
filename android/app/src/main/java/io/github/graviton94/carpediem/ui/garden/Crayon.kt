package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 크레용 파스텔 그리기 (design/art/src/concepts.js · mix.js 와 같은 방식을 앱에서 가볍게).
 * 선: 굵기가 조금씩 변하는 두 번 그은 선을 종이 결(tooth_line.png)로 거른다.
 * 채움: 선에서 살짝 어긋난 면 + 비스듬한 결을 종이 결(tooth_fill.png)로 거른다.
 * 모든 길이는 정원 단위(u, 화면 폭 / 390)를 곱해 쓴다.
 */
object Crayon {
    private val C = Tokens.Garden.Crayon

    /** mulberry32: 같은 seed 면 같은 흔들림. */
    class Rng(seed: Int) {
        private var a = seed
        fun next(): Float {
            a += 0x6D2B79F5
            var t = a
            t = (t xor (t ushr 15)) * (t or 1)
            t = t xor (t + (t xor (t ushr 7)) * (t or 61))
            return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toFloat() / 4294967296f
        }
    }

    /** 손으로 그린 듯 흔들리는 둥근 사각형 윤곽. */
    fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float, seed: Int, wobble: Float): List<Offset> {
        val rr = min(r, min(w, h) / 2)
        val pts = ArrayList<Offset>()
        val n = 10
        fun arc(cx: Float, cy: Float, a0: Double) { for (i in 0..n) { val a = a0 + i / n.toDouble() * PI / 2; pts.add(Offset(cx + (cos(a) * rr).toFloat(), cy + (sin(a) * rr).toFloat())) } }
        fun edge(a: Offset, b: Offset) { val steps = max(1, ((b - a).getDistance() / max(1f, rr)).toInt()); for (i in 1 until steps) pts.add(a + (b - a) * (i / steps.toFloat())) }
        arc(x + w - rr, y + rr, -PI / 2); edge(pts.last(), Offset(x + w, y + h - rr))
        arc(x + w - rr, y + h - rr, 0.0); edge(pts.last(), Offset(x + rr, y + h))
        arc(x + rr, y + h - rr, PI / 2); edge(pts.last(), Offset(x, y + rr))
        arc(x + rr, y + rr, PI)
        return wobble(pts, wobble, seed)
    }

    /** 가로로 흔들리며 이어지는 한 줄. */
    fun line(x0: Float, x1: Float, y: Float, step: Float, seed: Int, amp: Float): List<Offset> {
        val out = ArrayList<Offset>(); var x = x0; val ph = Rng(seed).next() * 6f
        while (x <= x1) { out.add(Offset(x, y + sin(x / step * 0.3f + ph) * amp)); x += step }
        return out
    }

    private fun wobble(pts: List<Offset>, amt: Float, seed: Int): List<Offset> {
        val r = Rng(seed); val f = FloatArray(4) { r.next() * 6f }; val n = pts.size
        return pts.mapIndexed { i, q -> val t = i / n.toFloat() * 2 * PI.toFloat(); Offset(q.x + amt * (sin(t * 3 + f[0]) * 0.5f + sin(t * 8 + f[1]) * 0.3f), q.y + amt * (sin(t * 4 + f[2]) * 0.5f + sin(t * 9 + f[3]) * 0.3f)) }
    }

    fun path(pts: List<Offset>, closed: Boolean = true) = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }; if (closed) close() }

    /** 종이 결 무늬 (반복). */
    fun tooth(img: ImageBitmap, unitPx: Float): ShaderBrush = object : ShaderBrush() {
        override fun createShader(size: Size): Shader = ImageShader(img, TileMode.Repeated, TileMode.Repeated).also { s ->
            s.setLocalMatrix(android.graphics.Matrix().apply { val k = max(0.6f, unitPx * C.textureScale); setScale(k, k) })
        }
    }

    /** block 으로 그린 것을 종이 결로 거른다. */
    fun DrawScope.textured(mask: ShaderBrush, block: DrawScope.() -> Unit) {
        drawIntoCanvas { c ->
            c.saveLayer(Rect(Offset.Zero, size), Paint())
            block()
            drawRect(mask, blendMode = BlendMode.DstIn)
            c.restore()
        }
    }

    /** 굵기가 변하는 크레용 선 (두 번). */
    fun DrawScope.stroke(pts: List<Offset>, width: Float, color: Color, seed: Int, closed: Boolean = true, passes: Int = 2) {
        if (pts.size < 2) return
        val r = Rng(seed); val ph = r.next() * 9f
        val list = if (closed) pts + pts.first() else pts
        repeat(passes) { pass ->
            val o = if (pass == 0) Offset.Zero else Offset((r.next() - 0.5f) * width * C.passOffset, (r.next() - 0.5f) * width * C.passOffset)
            for (i in 0 until list.size - 1) {
                val t = i / list.size.toFloat()
                val pr = 1f + C.vary * (sin(t * 17f + ph) * 0.6f + sin(t * 5.3f + ph * 2) * 0.4f)
                drawLine(color, list[i] + o, list[i + 1] + o, width * pr, StrokeCap.Round)
            }
        }
    }

    /** 선에서 살짝 어긋난 면 + 비스듬한 결. */
    fun DrawScope.fill(pts: List<Offset>, color: Color, unitPx: Float) {
        val off = Offset(C.fillOffsetX * unitPx, C.fillOffsetY * unitPx)
        val p = path(pts.map { it + off })
        drawPath(p, color)
        clipPath(p) {
            val gap = C.hatchGap * unitPx; val a = C.hatchAngle
            val dx = cos(a); val dy = sin(a); val diag = size.width + size.height
            var k = -diag
            val hatch = Color(red = color.red * 0.86f, green = color.green * 0.86f, blue = color.blue * 0.86f, alpha = C.hatchAlpha)
            while (k < diag) {
                val cx = size.width / 2 - dy * k; val cy = size.height / 2 + dx * k
                drawLine(hatch, Offset(cx - dx * diag, cy - dy * diag), Offset(cx + dx * diag, cy + dy * diag), gap * 0.9f)
                k += gap
            }
        }
    }
}

/** 크레용 테두리 상자 (+ 채움). 정원 디자인의 카드 · 입력칸 · 버튼 바탕. */
@Composable
fun Modifier.crayonBox(fillColor: Color? = null, radius: Float = Tokens.Garden.Radius.box, strokeWidth: Float = Tokens.Garden.Stroke.box, seed: Int = 1): Modifier {
    val ctx = LocalContext.current
    val u = with(LocalDensity.current) { Theme.unit.toPx() }
    val ink = Theme.gc.ink
    val line = Crayon.tooth(GardenArt.toothLine(ctx), u)
    val fillMask = Crayon.tooth(GardenArt.toothFill(ctx), u)
    return this.drawWithCache {
        val inset = strokeWidth * u
        val pts = Crayon.roundRect(inset, inset, size.width - inset * 2, size.height - inset * 2, radius * u, seed, u * 0.9f)
        onDrawBehind {
            with(Crayon) {
                if (fillColor != null) textured(fillMask) { fill(pts, fillColor, u) }
                textured(line) { stroke(pts, strokeWidth * u, ink, seed + 5) }
            }
        }
    }
}

/** 한 줄 구분선. */
@Composable
fun CrayonRule(modifier: Modifier = Modifier, seed: Int = 7) {
    val ctx = LocalContext.current
    val u = with(LocalDensity.current) { Theme.unit.toPx() }
    val mask = Crayon.tooth(GardenArt.toothLine(ctx), u)
    val w = Tokens.Garden.Stroke.rule
    val ink = Theme.gc.ink
    Box(modifier.fillMaxWidth().height(Theme.unit * (w * 3)).drawWithCache {
        val pts = Crayon.line(0f, size.width, size.height / 2, u * 5, seed, u * 0.6f)
        onDrawBehind { with(Crayon) { textured(mask) { stroke(pts, w * u, ink.copy(alpha = 0.7f), seed, closed = false, passes = 1) } } }
    })
}

/** 크레용 막대 (흐르는 시간 · 지나온 길). */
@Composable
fun CrayonBar(value: Float, color: Color, modifier: Modifier = Modifier, seed: Int = 11) {
    val ctx = LocalContext.current
    val u = with(LocalDensity.current) { Theme.unit.toPx() }
    val line = Crayon.tooth(GardenArt.toothLine(ctx), u)
    val fillMask = Crayon.tooth(GardenArt.toothFill(ctx), u)
    val r = Tokens.Garden.Radius.bar; val sw = Tokens.Garden.Stroke.bar
    val ink = Theme.gc.ink
    Box(modifier.fillMaxWidth().height(Theme.unit * (r * 2 + sw * 2)).drawWithCache {
        val inset = sw * u; val h = size.height - inset * 2; val w = size.width - inset * 2
        val outline = Crayon.roundRect(inset, inset, w, h, h / 2, seed, u * 0.6f)
        val filled = Crayon.roundRect(inset, inset, max(h, w * value.coerceIn(0f, 1f)), h, h / 2, seed + 1, u * 0.6f)
        onDrawBehind { with(Crayon) { textured(fillMask) { fill(filled, color, u) }; textured(line) { stroke(outline, sw * u, ink, seed + 2) } } }
    })
}

/**
 * 인생 달력: 칸마다 작은 조약돌 (손으로 그린 듯 조금씩 다른 모양 · 크기 · 기울기).
 * 지나온 돌은 계절 색으로 칠하고 먹선을 살짝, 지금은 호박색, 남은 날은 옅은 테두리만.
 * 칸이 수천 개라 모양은 크기가 정해질 때 한 번만 만들고 (색마다 경로 하나), 그리기는 그것을 칠하기만 한다.
 * sharedFrom: 이 칸부터 지금까지는 ‘함께한’ 칸 (호박빛 테두리). showAhead = false 면 남은 칸을 그리지 않는다 (가족의 달력).
 */
@Composable
fun CrayonCalendar(total: Int, filled: Int, columns: Int, modifier: Modifier = Modifier, sharedFrom: Int = Int.MAX_VALUE, showAhead: Boolean = true) {
    val ctx = LocalContext.current
    val u = with(LocalDensity.current) { Theme.unit.toPx() }
    val mask = Crayon.tooth(GardenArt.toothFill(ctx), u)
    val shown = if (showAhead) total else min(total, filled + 1)   // 남은 칸을 숨기면 그만큼 줄도 줄인다
    val rows = max(1, ceil(max(shown, 1) / columns.toFloat()).toInt())
    val L = Tokens.Garden.Layout
    val inkC = Theme.gc.ink; val futureC = Theme.gc.future
    Spacer(modifier.fillMaxWidth().aspectRatio(columns / rows.toFloat()).drawWithCache {
        val cell = min(size.width / columns, size.height / rows)
        val r = Crayon.Rng(860)
        val season = List(4) { Path() }; val now = Path(); val ahead = Path(); val shared = Path()
        for (i in 0 until max(total, 0)) {
            val cx = (i % columns + 0.5f) * cell; val cy = (i / columns + 0.5f) * cell
            val base = cell / 2 / L.calendarGap * (1f - L.pebbleJitter + L.pebbleJitter * r.next())
            val rx = base * (1f + (r.next() - 0.5f) * L.pebbleSquash); val ry = base * (1f - (r.next() - 0.5f) * L.pebbleSquash)
            val rot = r.next() * 6.283f
            val pts = List(7) { k -> val a = rot + k * 6.283f / 7; val w = 1f + (r.next() - 0.5f) * L.pebbleWobble; Offset(cx + cos(a) * rx * w, cy + sin(a) * ry * w) }
            if (i > filled && !showAhead) continue
            val target = when { i < filled -> season[min(3, i * 4 / total)]; i == filled -> now; else -> ahead }
            // 점 사이를 부드러운 곡선으로 (가운데점을 지나는 2차 곡선)
            val m0 = (pts[6] + pts[0]) / 2f
            target.moveTo(m0.x, m0.y)
            for (k in 0 until 7) { val c = pts[k]; val n = pts[(k + 1) % 7]; val mid = (c + n) / 2f; target.quadraticTo(c.x, c.y, mid.x, mid.y) }
            target.close()
            if (i in sharedFrom until filled) {
                shared.moveTo(m0.x, m0.y)
                for (k in 0 until 7) { val c = pts[k]; val n = pts[(k + 1) % 7]; val mid = (c + n) / 2f; shared.quadraticTo(c.x, c.y, mid.x, mid.y) }
                shared.close()
            }
        }
        val ink = inkC
        onDrawBehind {
            if (total <= 0) return@onDrawBehind
            with(Crayon) {
                textured(mask) {
                    season.forEachIndexed { k, path -> drawPath(path, Tokens.Garden.Colors.calendar[k]) }
                    drawPath(now, Tokens.Garden.Colors.now)
                    season.forEach { drawPath(it, ink.copy(alpha = L.pebbleLine), style = Stroke(u * 0.7f)) }
                    drawPath(shared, Tokens.Garden.Colors.now, style = Stroke(u * 1.1f))
                    drawPath(now, ink, style = Stroke(u * 0.9f))
                    drawPath(ahead, futureC, style = Stroke(u * 0.8f))
                }
            }
        }
    })
}

/** 종이 바탕 (정원 디자인의 모든 화면 뒤). */
@androidx.compose.runtime.Composable
fun Modifier.paperBackground(): Modifier = this.background(Theme.gc.base)
