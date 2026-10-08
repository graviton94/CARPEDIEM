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
        val warm = inside.filter { it.text.isNotBlank() && it.feeling in Feeling.BRIGHT + Feeling.CALM }
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
 * 계절마다 그 계절의 일들이 한 장씩 빠르게 지나간다 (한 장 ITEM_MS): 내가 남긴 한 줄만이 아니라 가족의 생일, ‘함께한 지 n일 · n년’,
 * 특별한 날, 만난 순간 · 손님, 핀 씨앗, 열린 항아리, 하루를 처음 만난 날.
 * 고르기: 사람의 일 (생일 · 인연 · 특별한 날) 먼저, 그다음 순간 · 씨앗 · 항아리, 남은 자리는 한 줄 (고마움 · 기쁨 · 희망 먼저, 무거운 마음은 연달아 두지 않음).
 */
enum class CreditPart { INTRO, SEASON, OUTRO }

enum class CreditKind { LINE, MY_BIRTHDAY, BIRTHDAY, TOGETHER_DAYS, TOGETHER_YEARS, SPECIAL, MOMENT, SEED, CAPSULE, FIRST }

/** 크레딧 한 장. a · b 는 이름 · 수 같은 글감 (말은 앱이 붙임). */
data class CreditItem(val date: LocalDate, val kind: CreditKind, val a: String = "", val b: String = "", val line: DayLine? = null)

/** months = 그 장의 달 (한 해를 달 순서로: 1–2월 겨울 · 봄 · 여름 · 가을 · 12월 겨울). */
class CreditScene(val part: CreditPart, val season: Season?, val startMs: Long, val lengthMs: Long, val items: List<CreditItem>, val months: IntRange = 1..12) {
    /** 계절 이름 다음, 한 장씩 보이는 때. */
    fun itemAt(ms: Long): Pair<CreditItem, Float>? {
        if (items.isEmpty()) return null
        val t = ms - startMs - Credits.HEADER_MS; if (t < 0) return null
        val each = (lengthMs - Credits.HEADER_MS) / items.size
        val i = (t / each).toInt().coerceAtMost(items.size - 1)
        return items[i] to ((t - i * each).toFloat() / each).coerceIn(0f, 1f)
    }
}

object Credits {
    const val INTRO_MS = 6_000L
    const val OUTRO_MS = 12_000L
    const val HEADER_MS = 2_000L
    const val ITEM_MS = 2_400L
    const val MIN_SEASON_MS = 8_500L
    const val MAX_SEASON_MS = 12_000L
    const val PER_SEASON = 4
    /** 겨울 두 장 (1–2월 · 12월) 은 짧게. */
    const val PER_WINTER = 3
    /** 한 해 (1월 → 12월) 를 달 순서대로: 해의 첫 겨울 · 봄 · 여름 · 가을 · 해의 끝 겨울. */
    val CHAPTERS = listOf(Season.WINTER to 1..2, Season.SPRING to 3..5, Season.SUMMER to 6..8, Season.AUTUMN to 9..11, Season.WINTER to 12..12)
    private val PEOPLE = setOf(CreditKind.MY_BIRTHDAY, CreditKind.BIRTHDAY, CreditKind.TOGETHER_DAYS, CreditKind.TOGETHER_YEARS, CreditKind.SPECIAL, CreditKind.FIRST)

    /** 인연의 날 수 가운데 크레딧에 올릴 것: 100 · 200 · 300 · 500 · 1000 · 그 뒤 1000마다. */
    fun milestone(days: Long): Boolean = days in setOf(100L, 200L, 300L, 500L) || (days >= 1000 && days % 1000 == 0L)

    /**
     * 그 해의 일들 (계절 순서와 상관없이 날짜순). met = "key:yyyy-mm-dd" (만난 순간 · 손님), start = 하루를 처음 만난 날.
     */
    fun events(year: Int, lines: List<DayLine>, birth: LocalDate?, people: List<Person>, special: List<SpecialDay>, met: List<String>,
               seeds: List<Seed>, capsules: List<Capsule>, start: LocalDate?): List<CreditItem> {
        val out = ArrayList<CreditItem>()
        lines.filter { it.date.year == year && it.text.isNotBlank() }.forEach { out += CreditItem(it.date, CreditKind.LINE, line = it) }
        birth?.let { b -> if (year > b.year) out += CreditItem(Family.birthdayIn(b, year), CreditKind.MY_BIRTHDAY, b = "${year - b.year}") }
        people.forEach { p ->
            p.birth?.let { b -> if (year >= b.year) out += CreditItem(Family.birthdayIn(b, year), CreditKind.BIRTHDAY, p.name) }
            Memories.from(p)?.let { t ->
                (1..120).forEach { n -> val d = Family.birthdayIn(t, t.year + n); if (d.year == year) out += CreditItem(d, CreditKind.TOGETHER_YEARS, p.name, "$n") }
                val from = ChronoUnit.DAYS.between(t, LocalDate.of(year, 1, 1)); val to = ChronoUnit.DAYS.between(t, LocalDate.of(year, 12, 31))
                for (n in maxOf(1L, from)..to) if (milestone(n)) out += CreditItem(t.plusDays(n), CreditKind.TOGETHER_DAYS, p.name, "$n")
            }
        }
        special.filter { it.date.year < year }.forEach { s -> out += CreditItem(Family.birthdayIn(s.date, year), CreditKind.SPECIAL, s.name, "${year - s.date.year}") }
        met.mapNotNull { r -> r.split(':', limit = 2).takeIf { it.size == 2 }?.let { (k, d) -> runCatching { LocalDate.parse(d) }.getOrNull()?.let { k to it } } }
            .filter { it.second.year == year }.forEach { (k, d) -> out += CreditItem(d, CreditKind.MOMENT, k) }
        seeds.filter { it.date.year == year && it.state == SeedState.BLOOMED }.forEach { out += CreditItem(it.date, CreditKind.SEED, it.text) }
        capsules.filter { it.opened && it.opens.year == year }.forEach { out += CreditItem(it.opens, CreditKind.CAPSULE, it.written.year.toString()) }
        start?.takeIf { it.year == year }?.let { out += CreditItem(it, CreditKind.FIRST) }
        return out.sortedBy { it.date }
    }

    fun pick(items: List<CreditItem>, n: Int = PER_SEASON): List<CreditItem> {
        val people = items.filter { it.kind in PEOPLE }.let { even(it, minOf(it.size, (n + 1) / 2)) }
        val others = items.filter { it.kind != CreditKind.LINE && it.kind !in PEOPLE }.let { even(it, minOf(it.size, maxOf(1, (n - people.size) / 2))) }
        val room = n - people.size - others.size
        val lines = pickLines(items.filter { it.kind == CreditKind.LINE }, room)
        val chosen = (people + others + lines).sortedBy { it.date }.toMutableList()
        // 남은 자리가 있으면 사람의 일 · 순간을 더 (한 줄이 적은 계절)
        if (chosen.size < n) items.filter { it !in chosen && it.kind != CreditKind.LINE }.take(n - chosen.size).let { chosen += it; chosen.sortBy { it.date } }
        return chosen
    }

    private fun <T> even(l: List<T>, k: Int): List<T> = if (k <= 0) emptyList() else if (l.size <= k) l else (0 until k).map { l[it * l.size / k] }

    private fun pickLines(list: List<CreditItem>, n: Int): List<CreditItem> {
        if (n <= 0) return emptyList()
        val warm = Feeling.BRIGHT
        val first = even(list.filter { it.line?.feeling in warm }, n / 2 + n % 2)
        val chosen = (first + even(list.filterNot { it in first }, n - first.size)).distinct().sortedBy { it.date }.toMutableList()
        var i = 1
        while (i < chosen.size) { if (chosen[i].line?.feeling in Letters.HEAVY && chosen[i - 1].line?.feeling in Letters.HEAVY) chosen.removeAt(i) else i++ }
        return chosen
    }

    fun plan(events: List<CreditItem>, year: Int): List<CreditScene> {
        val out = ArrayList<CreditScene>()
        out += CreditScene(CreditPart.INTRO, null, 0, INTRO_MS, emptyList())
        var t = INTRO_MS
        CHAPTERS.forEach { (s, months) ->
            val picked = pick(events.filter { it.date.year == year && it.date.monthValue in months }, if (s == Season.WINTER) PER_WINTER else PER_SEASON)
            val len = (HEADER_MS + picked.size * ITEM_MS).coerceIn(MIN_SEASON_MS, MAX_SEASON_MS)
            out += CreditScene(CreditPart.SEASON, s, t, len, picked, months); t += len
        }
        out += CreditScene(CreditPart.OUTRO, null, t, OUTRO_MS, emptyList())
        return out
    }

    fun total(plan: List<CreditScene>): Long = plan.last().let { it.startMs + it.lengthMs }
    fun at(plan: List<CreditScene>, ms: Long): CreditScene? = plan.lastOrNull { it.startMs <= ms }?.takeIf { ms < it.startMs + it.lengthMs }
}
