package io.github.graviton94.carpediem.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 미래의 나에게 (10): 지금 쓴 한마디를 항아리에 담아 나무 밑에 묻는다. 고른 날 (다음 생일 · 1년 뒤 · 석 달 뒤) 이 오기 전에는 열 수 없다.
 * 열린 항아리는 추억에 남는다.
 */
data class Capsule(val written: LocalDate, val opens: LocalDate, val text: String, val opened: Boolean = false)

enum class CapsuleWhen { SEASON, BIRTHDAY, YEAR }

object Capsules {
    const val MAX = 60
    const val MAX_CHARS = 120

    fun encode(list: List<Capsule>): String = list.joinToString("\n") {
        "${it.written.toEpochDay()}\t${it.opens.toEpochDay()}\t${if (it.opened) 1 else 0}\t${Lines.clean(it.text, MAX_CHARS, Lines.MAX_LINES).replace('\n', ' ')}"
    }
    fun decode(s: String?): List<Capsule> = s.orEmpty().lineSequence().mapNotNull { r ->
        val p = r.split('\t', limit = 4); if (p.size < 4) return@mapNotNull null
        val w = p[0].toLongOrNull() ?: return@mapNotNull null; val o = p[1].toLongOrNull() ?: return@mapNotNull null
        p[3].replace(' ', '\n').takeIf { it.isNotBlank() }?.let { Capsule(LocalDate.ofEpochDay(w), LocalDate.ofEpochDay(o), it, p[2] == "1") }
    }.toList()

    /** 언제 열릴지: 석 달 뒤 · 다음 생일 (오늘이 생일이면 내년) · 1년 뒤. */
    fun opensOn(w: CapsuleWhen, today: LocalDate, birth: LocalDate?): LocalDate = when (w) {
        CapsuleWhen.SEASON -> today.plusMonths(3)
        CapsuleWhen.YEAR -> today.plusYears(1)
        CapsuleWhen.BIRTHDAY -> birth?.let { b -> Family.nextBirthday(b, today.plusDays(1)) } ?: today.plusYears(1)
    }

    fun bury(list: List<Capsule>, today: LocalDate, opens: LocalDate, text: String): List<Capsule> {
        val t = Lines.clean(text, MAX_CHARS, Lines.MAX_LINES); if (t.isEmpty() || !opens.isAfter(today)) return list
        return (list + Capsule(today, opens, t)).sortedBy { it.opens }.takeLast(MAX)
    }

    /** 오늘 열 수 있는 것 (가장 오래 기다린 것부터). */
    fun due(list: List<Capsule>, today: LocalDate): Capsule? = list.filter { !it.opened && !it.opens.isAfter(today) }.minByOrNull { it.opens }

    fun open(list: List<Capsule>, c: Capsule): List<Capsule> = list.map { if (it == c) it.copy(opened = true) else it }

    /** 아직 묻혀 있는 것 (정원에 작은 흙더미). */
    fun sealed(list: List<Capsule>, today: LocalDate): List<Capsule> = list.filter { !it.opened && it.opens.isAfter(today) }

    fun openedOnes(list: List<Capsule>): List<Capsule> = list.filter { it.opened }.sortedByDescending { it.opens }
}

/**
 * 생일 아침의 나이테 (07): 지난 생일부터 이번 생일 전날까지가 한 해. 열두 달마다 가장 많이 고른 마음이 고리의 한 칸.
 * 한 줄이 없는 달은 빈 칸 (탓하지 않음).
 */
data class Ring(val age: Int, val start: LocalDate, val end: LocalDate, val months: List<Feeling?>, val lines: Int, val thanks: Int, val top: Feeling?, val pick: DayLine?)

object Rings {
    /** 나이 age 가 되기 전 한 해 (age - 1 세의 해): [age-1 번째 생일, age 번째 생일). */
    fun of(birth: LocalDate, age: Int, lines: List<DayLine>, seed: Long = 0): Ring {
        val start = Family.birthdayIn(birth, birth.year + age - 1)
        val end = Family.birthdayIn(birth, birth.year + age).minusDays(1)
        val inside = lines.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        val months = (0 until 12).map { i ->
            val a = start.plusMonths(i.toLong()); val b = start.plusMonths(i + 1L)
            top(inside.filter { !it.date.isBefore(a) && it.date.isBefore(b) })
        }
        val warm = inside.filter { it.text.isNotBlank() && it.feeling in setOf(Feeling.JOY, Feeling.THANKS, Feeling.HOPE, Feeling.CALM) }
        val pick = (warm.ifEmpty { inside.filter { it.text.isNotBlank() } }).let { if (it.isEmpty()) null else it[Math.floorMod(seed + age, it.size.toLong()).toInt()] }
        return Ring(age, start, end, months, inside.size, inside.count { it.feeling == Feeling.THANKS }, top(inside), pick)
    }

    /** 가장 많이 고른 마음 (같으면 나중에 고른 것). */
    private fun top(l: List<DayLine>): Feeling? =
        l.mapNotNull { it.feeling }.withIndex().groupBy { it.value }.maxWithOrNull(compareBy({ it.value.size }, { it.value.last().index }))?.key

    /** 오늘이 생일이면 막 끝난 한 해의 나이 (이번에 되는 나이), 아니면 null. 그 해에 한 줄이 하나도 없으면 null. */
    fun newToday(birth: LocalDate?, today: LocalDate, lines: List<DayLine>): Int? {
        if (birth == null || !Family.isBirthday(birth, today)) return null
        val age = today.year - birth.year
        if (age < 1) return null
        val r = of(birth, age, lines)
        return if (r.lines > 0) age else null
    }

    /** 지금까지 다 지나간 해 가운데 한 줄이 있는 해들 (최근 것부터, 이번에 되는 나이). */
    fun done(birth: LocalDate, today: LocalDate, lines: List<DayLine>): List<Int> {
        val first = lines.minOfOrNull { it.date } ?: return emptyList()
        val nowAge = ChronoUnit.YEARS.between(birth, today).toInt()
        return (1..nowAge).filter { age -> val start = Family.birthdayIn(birth, birth.year + age - 1); val end = Family.birthdayIn(birth, birth.year + age)
            end.isAfter(first) && lines.any { !it.date.isBefore(start) && it.date.isBefore(end) } }.sortedDescending()
    }
}

/**
 * 한 해의 엔딩 크레딧 (08): 인트로 → 봄 → 여름 → 가을 → 겨울 → 아웃트로, 모두 60 ~ 80초.
 * 계절마다 그 계절 정원 위로 그때의 한 줄이 날짜와 함께 올라간다. 한 줄이 적은 계절은 짧게 (10초), 많은 계절은 길게 (15초).
 * 고르기: 고마움 · 기쁨 · 희망을 먼저, 나머지는 고르게. 무거운 마음도 넣되 연달아 나오지 않게. 계절마다 많아야 PER_SEASON 줄.
 */
enum class CreditPart { INTRO, SEASON, OUTRO }

class CreditScene(val part: CreditPart, val season: Season?, val startMs: Long, val lengthMs: Long, val lines: List<DayLine>)

object Credits {
    const val INTRO_MS = 8_000L
    const val OUTRO_MS = 12_000L
    const val MIN_SEASON_MS = 10_000L
    const val MAX_SEASON_MS = 15_000L
    const val PER_SEASON = 6
    val ORDER = listOf(Season.SPRING, Season.SUMMER, Season.AUTUMN, Season.WINTER)

    /** 그 해의 계절 (달력의 해, 겨울은 1 · 2 · 12월). */
    fun seasonLines(lines: List<DayLine>, year: Int, s: Season): List<DayLine> =
        lines.filter { it.date.year == year && it.text.isNotBlank() && Memories.seasonOf(it.date) == s }.sortedBy { it.date }

    fun pick(list: List<DayLine>, n: Int = PER_SEASON): List<DayLine> {
        if (list.size <= n) return list
        val warm = setOf(Feeling.THANKS, Feeling.JOY, Feeling.HOPE)
        val first = list.filter { it.feeling in warm }.let { w -> if (w.size <= n / 2) w else (0 until n / 2).map { w[it * w.size / (n / 2)] } }
        val rest = list.filterNot { it in first }
        val need = n - first.size
        val even = if (rest.isEmpty() || need <= 0) emptyList() else (0 until need).map { rest[it * rest.size / need] }.distinct()
        val chosen = (first + even).distinct().sortedBy { it.date }.toMutableList()
        // 무거운 마음이 연달아 나오면 뒤의 것을 뺌
        var i = 1
        while (i < chosen.size) { if (chosen[i].feeling in Letters.HEAVY && chosen[i - 1].feeling in Letters.HEAVY) chosen.removeAt(i) else i++ }
        return chosen
    }

    fun plan(lines: List<DayLine>, year: Int): List<CreditScene> {
        val out = ArrayList<CreditScene>()
        out += CreditScene(CreditPart.INTRO, null, 0, INTRO_MS, emptyList())
        var t = INTRO_MS
        ORDER.forEach { s ->
            val picked = pick(seasonLines(lines, year, s))
            val len = (MIN_SEASON_MS + picked.size * 1_000L).coerceIn(MIN_SEASON_MS, MAX_SEASON_MS)
            out += CreditScene(CreditPart.SEASON, s, t, len, picked); t += len
        }
        out += CreditScene(CreditPart.OUTRO, null, t, OUTRO_MS, emptyList())
        return out
    }

    fun total(plan: List<CreditScene>): Long = plan.last().let { it.startMs + it.lengthMs }
    fun at(plan: List<CreditScene>, ms: Long): CreditScene? = plan.lastOrNull { it.startMs <= ms }?.takeIf { ms < it.startMs + it.lengthMs }
}
