package io.github.graviton94.carpediem.core

import java.time.LocalDate

/**
 * 달 · 해 돌아보기 (1.1.12): 카드와 영상에 실을 날 고르기.
 * 보여 줘도 괜찮은 줄이 먼저: 특별한 날 (특별한 날 꽃 · 가족 생일) → 사진 있는 날 → 밝은 마음 → 알맞은 길이.
 * 무거운 마음 (Letters.HEAVY) 의 줄 · 빈 줄 · 오늘 것은 저절로는 고르지 않음 (사람이 직접 고르면 넣음).
 */
object Recap {
    /** 고를 수 있는 하루: special = 그날의 이름 (특별한 날 · 누구의 생일), photo = 사진이 있는지. */
    data class Pick(val line: DayLine, val special: String?, val photo: Boolean) {
        val date: LocalDate get() = line.date
        val score: Int get() = (if (special != null) 100 else 0) + (if (photo) 50 else 0) +
            (if (line.feeling in Feeling.BRIGHT) 20 else if (line.feeling == Feeling.CALM) 8 else 0) +
            (if (line.text.length in GOOD_LEN) 10 else 0)
    }

    /** 달 영상 · 카드에 싣는 날 수, 해 영상의 장면 수, 해 카드의 한 줄 수. */
    const val MONTH_MAX = 3
    const val YEAR_MAX = 8
    const val YEAR_LINES = 3
    private val GOOD_LEN = 8..60

    /** 그 기간 (from..to) 의 모든 지난 날 (사람이 직접 고르는 목록: 무거운 마음도 들어감, 빈 줄 · 오늘은 빼고), 날짜순. */
    fun days(lines: List<DayLine>, from: LocalDate, to: LocalDate, today: LocalDate, special: (LocalDate) -> String?, photo: (LocalDate) -> Boolean): List<Pick> =
        lines.filter { !it.date.isBefore(from) && !it.date.isAfter(to) && it.date.isBefore(today) && it.text.isNotBlank() }
            .sortedBy { it.date }.map { Pick(it, special(it.date), photo(it.date)) }

    /** 저절로 고를 수 있는 것만 (무거운 마음 빼고). */
    fun auto(all: List<Pick>): List<Pick> = all.filter { it.line.feeling !in Letters.HEAVY }

    /** 점수 높은 순 (같으면 나중 날). */
    private fun ranked(c: List<Pick>) = c.sortedWith(compareByDescending<Pick> { it.score }.thenByDescending { it.date })

    /** 달: 가장 좋은 max 날, 날짜순. */
    fun month(all: List<Pick>, max: Int = MONTH_MAX): List<Pick> = ranked(auto(all)).take(max).sortedBy { it.date }

    /** 해: 계절마다 가장 좋은 날 하나씩 (있는 계절만), 나머지는 점수 순으로 max 까지, 날짜순. */
    fun year(all: List<Pick>, max: Int = YEAR_MAX): List<Pick> {
        val r = ranked(auto(all))
        val first = r.groupBy { Family.seasonOf(it.date) }.values.map { it.first() }
        return (first + r.filterNot { it in first }).take(max).sortedBy { it.date }
    }

    /** 해 카드의 한 줄들: 서로 다른 계절에서 먼저, 모자라면 아무 계절에서. 날짜순. */
    fun yearLines(all: List<Pick>, n: Int = YEAR_LINES): List<Pick> = year(all, Int.MAX_VALUE).let { y ->
        val bySeason = ranked(y).distinctBy { Family.seasonOf(it.date) }
        (bySeason + ranked(y).filterNot { it in bySeason }).take(n).sortedBy { it.date }
    }

    /** 가장 많던 마음 (같으면 나중에 고른 것). 무거운 마음이 가장 많으면 null (보내는 그림에 크게 쓰지 않음). */
    fun mood(lines: List<DayLine>): Feeling? =
        lines.filter { it.feeling != null }.groupBy { it.feeling!! }
            .maxWithOrNull(compareBy<Map.Entry<Feeling, List<DayLine>>> { it.value.size }.thenBy { e -> e.value.maxOf { it.date } })?.key
            ?.takeIf { it !in Letters.HEAVY }

    /** 그날의 이름: 특별한 날 꽃 (그날이거나 해마다 같은 날) 또는 생일 (birthdays = 이름 · 태어난 날). */
    fun specialOf(d: LocalDate, specials: List<SpecialDay>, birthdays: List<Pair<String, LocalDate>>): String? =
        specials.firstOrNull { it.date == d || (it.date.year < d.year && Family.birthdayIn(it.date, d.year) == d) }?.name?.takeIf { it.isNotBlank() }
            ?: birthdays.firstOrNull { (_, b) -> b.year <= d.year && Family.birthdayIn(b, d.year) == d }?.first
}
