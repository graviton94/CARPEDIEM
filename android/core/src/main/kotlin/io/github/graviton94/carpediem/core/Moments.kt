package io.github.graviton94.carpediem.core

import java.time.LocalDate
import java.time.MonthDay

/** 정원에 놓이는 것 하나와 그것이 생긴 날. */
data class Moment(val id: String, val date: LocalDate)

/**
 * 정원에 놓이는 것 16가지가 생기는 순간 (design/art/README.md).
 * 12가지는 시간이 흐르면 저절로 온다. 3가지(바람개비 · 종이배 · 연)는 오늘의 한 줄을 7 · 30 · 100일 이어 쓴 날에, 풍경은 처음 하루와 숨 쉰 날에 온다 (빠져도 벌은 없음).
 * 날짜로만 정하므로 앱을 다시 설치해도 생년월일 · 시작일만 있으면 같은 것이 돌아온다 (문장 넘김 · 돌아옴 제외).
 */
object Moments {
    val ids = listOf("moss", "teacup", "cairn", "pine", "flower", "pond", "leaf", "candle", "dandelion", "bookmark", "snail", "acorn", "pinwheel", "paperboat", "kite", "windchime")

    /** 북반구 기준 계절이 시작하는 날. */
    private val spring = MonthDay.of(3, 1)
    private val summer = MonthDay.of(6, 1)
    private val autumn = MonthDay.of(9, 1)
    private val newYear = MonthDay.of(1, 1)

    /** 최근에 생긴 것부터. */
    fun earned(start: LocalDate, birth: LocalDate, expectancy: Double, today: LocalDate, firstSkip: LocalDate? = null, returned: LocalDate? = null, streaks: Map<Int, LocalDate> = emptyMap(), firstBreath: LocalDate? = null): List<Moment> {
        val out = ArrayList<Moment>()
        fun add(id: String, d: LocalDate?) { if (d != null && !d.isBefore(start) && !d.isAfter(today)) out.add(Moment(id, d)) }
        add("moss", start)
        add("teacup", start.plusDays(7))
        add("cairn", start.plusDays(100))
        add("pine", start.plusYears(1))
        add("flower", next(spring, start))
        add("pond", next(summer, start))
        add("leaf", next(autumn, start))
        add("candle", next(MonthDay.from(birth), start))
        add("dandelion", next(newYear, start))
        add("bookmark", firstSkip)
        add("snail", returned)
        add("acorn", nextSeasonChange(birth, expectancy, start))
        add("pinwheel", streaks[7]); add("paperboat", streaks[30]); add("kite", streaks[100])
        add("windchime", firstBreath)
        return out.sortedByDescending { it.date }
    }

    /** 시험용: 모두 오늘 생긴 것으로. */
    fun all(today: LocalDate) = ids.map { Moment(it, today) }

    /** start 뒤에 처음 오는 그날 (start 당일은 치지 않음). 2월 29일생은 평년에 3월 1일. */
    private fun next(day: MonthDay, start: LocalDate): LocalDate {
        var y = start.year
        while (true) {
            val d = if (day.isValidYear(y)) day.atYear(y) else LocalDate.of(y, 3, 1)
            if (d.isAfter(start)) return d
            y++
        }
    }

    /** 살아온 비율이 1/4 · 2/4 · 3/4 을 넘는 날 가운데 start 뒤에 처음 오는 날. */
    private fun nextSeasonChange(birth: LocalDate, expectancy: Double, start: LocalDate): LocalDate? {
        val end = LifeSnapshot.endDate(birth, expectancy)
        val total = end.toEpochDay() - birth.toEpochDay()
        return (1..3).map { birth.plusDays(total * it / 4) }.firstOrNull { it.isAfter(start) }
    }
}
