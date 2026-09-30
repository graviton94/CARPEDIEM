package io.github.graviton94.carpediem.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 하루(조약돌)의 모양: 번호(seed) 하나로 돌 종류 · 색 · 윤곽 · 눈 자리가 정해진다.
 * design/art/src/engine.js 의 traitsOf · outline 과 concepts.js 의 haruShape 를 그대로 옮겼다
 * (같은 번호면 예전과 같은 돌 · 같은 모양). 그리기는 앱이 벡터로 한다 (그림 파일 · WebView 없음).
 *
 * 좌표는 200 × 200 칸, 땅은 y = 171 (앱 토큰 haruBox · haruGround 와 같은 칸).
 */
object HaruShape {
    data class Stone(val id: String, val base: List<Int>, val pattern: String)

    /** 돌 14가지 (engine.js STONES 와 같은 순서 · 같은 색). */
    val stones = listOf(
        Stone("basalt", listOf(0x2E2E30, 0x3A3A3C, 0x27282B), "fine"),
        Stone("granite", listOf(0x8E8A82, 0x9A968D, 0x7F7B74), "salt"),
        Stone("pinkgranite", listOf(0xB39A8E, 0xA88E82, 0xBCA497), "salt"),
        Stone("sand", listOf(0xC8B9A0, 0xBFAE93, 0xD2C4AC), "grain"),
        Stone("ochre", listOf(0xB98E5A, 0xAE8352, 0xC49A66), "grain"),
        Stone("speckle", listOf(0xA7A399, 0x9C988E), "speckle"),
        Stone("slate", listOf(0x5C6670, 0x65707A, 0x56606A), "layers"),
        Stone("gneiss", listOf(0x7C776E, 0x86817A, 0x6F6A62), "layers"),
        Stone("jasper", listOf(0x8B3E2C, 0x7E3727, 0x96472F), "jasper"),
        Stone("serpentine", listOf(0x4E6A55, 0x57735D, 0x465F4C), "mottle"),
        Stone("jade", listOf(0x8FA890, 0x9BB39B, 0x859E86), "mottle"),
        Stone("marble", listOf(0xE4E1D8, 0xDCD8CD, 0xEAE7DF), "marble"),
        Stone("quartz", listOf(0x3B3B3E, 0x7F7B74, 0x5C6670), "vein"),
        Stone("ring", listOf(0x2E2E30, 0x5C6670, 0x8B3E2C), "ring"),
    )

    class Harm(val k: Int, val a: Double, val p: Double)
    class Eye(val x: Double, val y: Double, val r: Double)
    class Traits(
        val stone: Stone, val base: Int, val size: Double, val aspect: Double, val rot: Double, val harm: List<Harm>, val flat: Double,
        val eyeSize: Double, val eyeRatio: Double, val eyeX: Double, val eyeY: Double, val gap: Double, val tilt: Double,
        val spread: Double, val lookX: Double, val lookY: Double, val pupil: Double, val texSeed: Long,
    )

    /** 모양 한 벌 (200 × 200 칸). body 는 매끈한 곡선을 이을 점들, top 은 머리 꼭대기(새싹 · 모자 자리). */
    class Shape(val traits: Traits, val body: List<Pair<Double, Double>>, val eyes: List<Eye>, val top: Pair<Double, Double>,
                val left: Double, val right: Double, val upper: Double, val bodyColor: Int, val halfWidth: Double)

    const val BOX = 200.0
    const val GROUND = 171.0
    /** 앱 그림의 기준 폭 (design tokens garden.layout.haruArtWidth 와 같음). */
    const val ART_WIDTH = 70.0

    /** mulberry32 (engine.js rng 과 비트 단위로 같음). */
    class Rng(seed: Int) {
        private var a = seed
        fun next(): Double {
            a += 0x6D2B79F5
            var t = a
            t = (t xor (t ushr 15)) * (t or 1)
            t = t xor (t + (t xor (t ushr 7)) * (t or 61))
            return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
        }
    }

    private fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t

    private fun shade(c: Int, k: Double): Int {
        fun ch(v: Int) = (if (k > 0) v + (255 - v) * k else v * (1 + k)).roundToInt().coerceIn(0, 255)
        return (ch(c shr 16 and 255) shl 16) or (ch(c shr 8 and 255) shl 8) or ch(c and 255)
    }

    private fun mixWhite(c: Int, t: Double): Int {
        fun ch(v: Int) = (v + (255 - v) * t).roundToInt()
        return (ch(c shr 16 and 255) shl 16) or (ch(c shr 8 and 255) shl 8) or ch(c and 255)
    }

    fun traits(seed: Long): Traits {
        val r = Rng(seed.toInt())
        val stone = stones[floor(r.next() * stones.size).toInt()]
        var base = stone.base[floor(r.next() * stone.base.size).toInt()]
        base = shade(base, (r.next() - 0.5) * 0.12)
        val size = lerp(0.64, 1.06, r.next())
        val aspect = lerp(0.6, 0.92, r.next()); val rot = (r.next() - 0.5) * 0.9
        val harm = (2..6).map { k -> val a = (r.next() * 0.07) / (k - 1) * 1.6; Harm(k, a, r.next() * PI * 2) }
        val flat = lerp(0.1, 0.3, r.next())
        val eyeSize = lerp(0.15, 0.23, r.next()); val ratio = lerp(0.68, 1.0, r.next())
        val ex = lerp(-0.3, 0.3, r.next())
        val ey = lerp(-0.22, 0.02, r.next()); val gap = lerp(0.9, 1.35, r.next()); val tilt = (r.next() - 0.5) * 0.35
        val spread = lerp(0.0, 0.6, r.next()); val lookX = (r.next() - 0.5) * 1.2; val lookY = (r.next() - 0.5) * 0.8
        val pupil = lerp(0.5, 0.64, r.next()); val tex = floor(r.next() * 4294967296.0).toLong()
        return Traits(stone, base, size, aspect, rot, harm, flat, eyeSize, ratio, ex, ey, gap, tilt, spread, lookX, lookY, pupil, tex)
    }

    /** 200 칸 상자 가운데(x = 100), 땅 y = 171 에 앉힌 모양. points = 윤곽 점 수 (그리기용이라 72면 충분). */
    fun of(seed: Long, points: Int = 72): Shape {
        val t = traits(seed)
        val width = ART_WIDTH / t.size.pow(0.85)
        val w = width * t.size; val h = w * t.aspect
        val c = cos(t.rot); val s = sin(t.rot)
        val raw = (0 until points).map { i ->
            val th = i.toDouble() / points * PI * 2
            var rr = 1.0; t.harm.forEach { rr += it.a * cos(it.k * th + it.p) }
            val x = cos(th) * w * rr; var y = sin(th) * h * rr
            if (y > h * 0.35) y = h * 0.35 + (y - h * 0.35) * (1 - t.flat)
            (x * c - y * s) to (x * s + y * c)
        }
        val cx = BOX / 2
        val dy = GROUND - raw.maxOf { it.second }
        val body = raw.map { (x, y) -> (x + cx) to (y + dy) }
        val er = w * t.eyeSize
        val eyes = listOf(-1, 1).mapIndexed { idx, sd ->
            val lx = w * t.eyeX + sd * er * t.gap; val ly = h * t.eyeY + sd * er * t.tilt * 2
            Eye(cx + lx * c - ly * s, dy + lx * s + ly * c, er * (if (idx == 0) 1.0 else t.eyeRatio))
        }
        val top = body.minBy { it.second }
        return Shape(t, body, eyes, top, body.minOf { it.first }, body.maxOf { it.first }, top.second, mixWhite(t.base, 0.24), w)
    }
}
