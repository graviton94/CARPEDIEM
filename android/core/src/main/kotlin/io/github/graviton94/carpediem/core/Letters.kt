package io.github.graviton94.carpediem.core

import java.time.LocalDate

/** 계절의 편지: 도착한 날(3 · 6 · 9 · 12월 1일)의 계절 이름으로, 그 앞 석 달 동안 보낸 한 줄. */
data class Letter(val arrives: LocalDate, val season: Season, val from: LocalDate, val until: LocalDate, val lines: List<DayLine>, val heavy: List<DayLine>) {
    /** "2027-03" 처럼 편지 하나를 가리키는 이름 (연 편지 기억에 씀). */
    val id: String get() = "%04d-%02d".format(arrives.year, arrives.monthValue)
}

object Letters {
    val MONTHS = listOf(3, 6, 9, 12)
    /** 무거운 마음: 기본으로 편지에 넣지 않고 ‘흘려보낸 마음 N번’만 (편지 안 스위치로 함께 보기). */
    val HEAVY = setOf(Feeling.SAD, Feeling.WORRY, Feeling.DISAPPOINT)

    private fun seasonOf(month: Int) = when (month) { 3 -> Season.SPRING; 6 -> Season.SUMMER; 9 -> Season.AUTUMN; else -> Season.WINTER }

    /** 오늘까지 도착한 가장 최근 편지 날 (그달 안에서만 봉투가 보인다). */
    fun due(today: LocalDate): LocalDate? = if (today.monthValue in MONTHS) today.withDayOfMonth(1) else null

    /** 도착한 날의 편지. 글 있는 한 줄이 minLines 개보다 적으면 오지 않는다 (null). */
    fun of(list: List<DayLine>, arrives: LocalDate, minLines: Int): Letter? {
        val from = arrives.minusMonths(3)
        val inside = list.filter { it.text.isNotBlank() && !it.date.isBefore(from) && it.date.isBefore(arrives) }.sortedBy { it.date }
        if (inside.size < minLines) return null
        val (heavy, light) = inside.partition { it.feeling in HEAVY }
        return Letter(arrives, seasonOf(arrives.monthValue), from, arrives.minusDays(1), light, heavy)
    }

    /** 지금까지 받은 편지 (최근 것부터): 처음 한 줄을 보낸 뒤 도착한 날마다. */
    fun received(list: List<DayLine>, today: LocalDate, minLines: Int): List<Letter> {
        val first = list.minOfOrNull { it.date } ?: return emptyList()
        var d = LocalDate.of(first.year, 1, 1)
        val out = mutableListOf<Letter>()
        while (!d.isAfter(today)) {
            if (d.monthValue in MONTHS && d.dayOfMonth == 1) of(list, d, minLines)?.let(out::add)
            d = d.plusMonths(1).withDayOfMonth(1)
        }
        return out.reversed()
    }
}
