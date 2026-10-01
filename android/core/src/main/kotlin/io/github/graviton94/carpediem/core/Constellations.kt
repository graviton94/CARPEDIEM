package io.github.graviton94.carpediem.core

import java.time.LocalDate
import kotlin.math.hypot

/** 달마다 그 달 저녁 하늘에 잘 보이는 별자리 하나 (data/constellations.csv). 별은 0..1 좌표, 선은 걷는 순서. */
data class Constellation(val month: Int, val korean: String, val english: String, val stars: List<Pair<Float, Float>>, val lines: List<Pair<Int, Int>>)

/** 하루의 자리: 0..1 좌표와 크기 배율 (1 = 보통). */
data class Spot(val x: Float, val y: Float, val size: Float)

/**
 * 마음의 기록: 그 달의 별자리는 길잡이일 뿐. 1일부터 선을 따라 가되 간격은 고르지 않고, 선에서 천천히 굽이치며 벗어나
 * 흩뿌려진다. 판 전체도 해마다 조금 돌고 줄고 옮겨져, 같은 달이라도 해마다 · 사람마다 다른 무늬가 된다.
 * 같은 seed 면 늘 같은 자리.
 */
class ConstellationBook(csv: String) {
    val all: List<Constellation> = Csv.records(csv).mapNotNull { r ->
        val m = r["월"]?.toIntOrNull() ?: return@mapNotNull null
        val stars = r["별(x:y)"].orEmpty().split(' ').filter { it.isNotBlank() }.mapNotNull { s -> s.split(':').let { p -> p.getOrNull(0)?.toFloatOrNull()?.let { x -> p.getOrNull(1)?.toFloatOrNull()?.let { y -> x to y } } } }
        val lines = r["선"].orEmpty().split(' ').filter { it.isNotBlank() }.mapNotNull { s -> s.split('-').let { p -> p.getOrNull(0)?.toIntOrNull()?.let { a -> p.getOrNull(1)?.toIntOrNull()?.let { b -> a to b } } } }
            .filter { (a, b) -> a in stars.indices && b in stars.indices }
        if (stars.isEmpty()) null else Constellation(m, r["한글"].orEmpty(), r["영문"].orEmpty(), stars, lines)
    }

    fun of(month: Int): Constellation? = all.firstOrNull { it.month == month }
}

object Constellations {
    /** 흩뿌림의 정도 (판 대비): spread = 선에서 벗어나는 폭, wander = 천천히 굽이치는 폭, tilt = 판이 도는 최대 각 (라디안). */
    class Scatter(val spread: Float = 0.085f, val wander: Float = 0.065f, val tilt: Float = 0.22f)

    /** 그 해 그 달의 seed (설치 seed 와 함께): 같은 달이어도 해마다 · 사람마다 다르게. */
    fun seed(install: Long, year: Int, month: Int): Long = install * 31 + year * 12L + month

    /** 별자리를 판 가운데로 넓혀 (길쭉한 별자리도 판을 고루 쓰게, 가로세로 비는 반쯤만 살림). */
    fun fit(c: Constellation): Constellation {
        val x0 = c.stars.minOf { it.first }; val x1 = c.stars.maxOf { it.first }; val y0 = c.stars.minOf { it.second }; val y1 = c.stars.maxOf { it.second }
        val w = (x1 - x0).coerceAtLeast(0.05f); val h = (y1 - y0).coerceAtLeast(0.05f); val m = maxOf(w, h)
        val tw = 0.8f * (0.5f * w / m + 0.5f); val th = 0.8f * (0.5f * h / m + 0.5f)
        return c.copy(stars = c.stars.map { (x, y) -> (0.5f + (x - (x0 + x1) / 2) / w * tw) to (0.5f + (y - (y0 + y1) / 2) / h * th) })
    }

    /** n 날의 자리 (1일부터). */
    fun scatter(c0: Constellation, n: Int, seed: Long, k: Scatter = Scatter()): List<Spot> {
        if (n <= 0) return emptyList()
        val c = fit(c0)
        val rng = java.util.Random(seed)
        fun gauss() = (rng.nextFloat() + rng.nextFloat() + rng.nextFloat() - 1.5f) / 1.5f   // -1..1, 가운데가 짙음
        val segs = c.lines.ifEmpty { c.stars.indices.zipWithNext() }.ifEmpty { listOf(0 to 0) }
        val len = segs.map { (a, b) -> hypot(c.stars[a].first - c.stars[b].first, c.stars[a].second - c.stars[b].second).coerceAtLeast(1e-3f) }
        val total = len.sum()
        // 고르지 않은 간격: 날마다 0.35 ~ 1.65 만큼 걸어, 전체를 선의 길이에 맞춤
        val steps = FloatArray(n) { 0.35f + 1.3f * rng.nextFloat() }
        val sum = steps.sum()
        var walked = steps[0] * 0.5f
        // 천천히 굽이치는 벗어남: 낮은 주파수 사인 둘
        val f1 = 1f + rng.nextFloat() * 2f; val f2 = 3f + rng.nextFloat() * 3f
        val p1 = rng.nextFloat() * 6.283f; val p2 = rng.nextFloat() * 6.283f
        // 판 전체: 해마다 조금 돌고 · 줄고 · 옮겨짐
        val rot = (rng.nextFloat() - 0.5f) * 2f * k.tilt; val sc = 0.86f + 0.12f * rng.nextFloat()
        val dx0 = (rng.nextFloat() - 0.5f) * 0.08f; val dy0 = (rng.nextFloat() - 0.5f) * 0.08f
        val cr = kotlin.math.cos(rot); val sr = kotlin.math.sin(rot)
        return List(n) { i ->
            val s = (walked / sum) * total
            if (i < n - 1) walked += (steps[i] + steps[i + 1]) * 0.5f
            // s 가 놓이는 선과 그 위의 점
            var acc = 0f; var e = 0
            while (e < segs.size - 1 && acc + len[e] < s) { acc += len[e]; e++ }
            val t = ((s - acc) / len[e]).coerceIn(0f, 1f)
            val (ax, ay) = c.stars[segs[e].first]; val (bx, by) = c.stars[segs[e].second]
            val ux = (bx - ax) / len[e]; val uy = (by - ay) / len[e]
            val u = s / total
            val off = k.wander * (0.6f * kotlin.math.sin(u * 6.283f * f1 + p1) + 0.4f * kotlin.math.sin(u * 6.283f * f2 + p2)) + k.spread * gauss()
            val along = k.spread * 0.4f * gauss()
            var x = ax + (bx - ax) * t - uy * off + ux * along
            var y = ay + (by - ay) * t + ux * off + uy * along
            // 판 돌리기 · 줄이기 · 옮기기 (가운데 기준)
            val rx = (x - 0.5f) * sc; val ry = (y - 0.5f) * sc
            x = 0.5f + rx * cr - ry * sr + dx0; y = 0.5f + rx * sr + ry * cr + dy0
            // 크기: 대개 보통, 가끔 작거나 조금 큼
            val r = rng.nextFloat(); val size = when { r < 0.12f -> 1.45f + 0.25f * rng.nextFloat(); r < 0.35f -> 0.65f + 0.15f * rng.nextFloat(); else -> 0.85f + 0.3f * rng.nextFloat() }
            Spot(x.coerceIn(0.03f, 0.97f), y.coerceIn(0.03f, 0.97f), size)
        }
    }

    /** 그 달의 첫날 ~ 마지막 날, 날마다 보낸 한 줄 또는 null. */
    fun monthDays(list: List<DayLine>, year: Int, month: Int): List<Pair<LocalDate, DayLine?>> {
        val byDay = list.associateBy { it.date }
        val first = LocalDate.of(year, month, 1)
        return (0 until first.lengthOfMonth()).map { k -> first.plusDays(k.toLong()).let { d -> d to byDay[d] } }
    }

    /** 한 달의 정원이 피는 때: 달이 바뀐 뒤 days 일 동안 (1월 초는 한 해의 정원이 대신). 지난 달 (연, 월) 또는 null. */
    fun monthDue(today: LocalDate, days: Int): Pair<Int, Int>? {
        if (today.dayOfMonth > days || today.monthValue == 1) return null
        val prev = today.minusMonths(1)
        return prev.year to prev.monthValue
    }
}
