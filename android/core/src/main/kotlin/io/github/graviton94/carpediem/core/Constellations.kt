package io.github.graviton94.carpediem.core

import java.time.LocalDate
import kotlin.math.hypot

/** 달마다 그 달 저녁 하늘에 잘 보이는 별자리 하나 (data/constellations.csv). 별은 0..1 좌표, 선은 걷는 순서. */
data class Constellation(val month: Int, val korean: String, val english: String, val stars: List<Pair<Float, Float>>, val lines: List<Pair<Int, Int>>)

/** 별자리 위의 한 자리: 0..1 좌표, 꼭짓점(별자리의 별)인지. */
data class Spot(val x: Float, val y: Float, val vertex: Boolean)

/**
 * 마음의 기록: 그 달의 날들을 그 달 별자리의 선을 따라 1일부터 차례로 놓는다.
 * 별자리의 별(꼭짓점)에 먼저 하루씩, 남은 날은 선의 길이만큼 나눠 선 위에. 한 달이 다 가면 별자리가 된다.
 * 자리는 별자리와 날 수로만 정해져 늘 같다.
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
    /** n 날의 자리 (1일부터). 선 위의 날은 선에서 아주 조금 비켜 앉는다 (jitter = 판 대비). */
    fun layout(c: Constellation, n: Int, jitter: Float = 0.035f): List<Spot> {
        if (n <= 0) return emptyList()
        if (c.lines.isEmpty()) return List(n) { i -> c.stars[i % c.stars.size].let { Spot(it.first, it.second, i < c.stars.size) } }
        val len = c.lines.map { (a, b) -> hypot(c.stars[a].first - c.stars[b].first, c.stars[a].second - c.stars[b].second) }
        val vertices = c.lines.flatMap { listOf(it.first, it.second) }.distinct().size
        val rest = (n - vertices).coerceAtLeast(0)
        val total = len.sum().takeIf { it > 0f } ?: 1f
        val raw = len.map { it / total * rest }
        val k = raw.map { it.toInt() }.toMutableList()
        // 남은 몫은 소수점이 큰 선부터
        raw.indices.sortedByDescending { raw[it] - k[it] }.take(rest - k.sum()).forEach { k[it]++ }
        val rng = java.util.Random(c.month * 7L + 1)
        val out = ArrayList<Spot>(n); val seen = HashSet<Int>()
        c.lines.forEachIndexed { e, (a, b) ->
            val (ax, ay) = c.stars[a]; val (bx, by) = c.stars[b]
            if (seen.add(a)) out += Spot(ax, ay, true)
            val dx = bx - ax; val dy = by - ay; val l = hypot(dx, dy).takeIf { it > 0f } ?: 1f
            for (j in 1..k[e]) {
                val t = j / (k[e] + 1f); val o = (rng.nextFloat() - 0.5f) * jitter
                out += Spot(ax + dx * t - dy / l * o, ay + dy * t + dx / l * o, false)
            }
            if (seen.add(b)) out += Spot(bx, by, true)
        }
        return out.take(n)
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
