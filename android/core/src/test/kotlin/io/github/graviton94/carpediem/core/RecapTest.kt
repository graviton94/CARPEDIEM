package io.github.graviton94.carpediem.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 달 · 해 돌아보기 (1.1.12): 날 고르기. */
class RecapTest {
    private val today = LocalDate.of(2026, 10, 24)
    private fun l(m: Int, d: Int, f: Feeling?, t: String = "오늘은 조용히 좋은 날이었다") = DayLine(LocalDate.of(2026, m, d), t, f)

    @Test fun heavyLinesAreNeverPickedAutomatically() {
        val lines = listOf(l(9, 1, Feeling.SAD), l(9, 2, Feeling.WORRY), l(9, 3, Feeling.CALM))
        val all = Recap.days(lines, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), today, { null }, { true })
        assertEquals(3, all.size)                                    // 직접 고르는 목록엔 다 있음
        assertEquals(listOf(LocalDate.of(2026, 9, 3)), Recap.month(all).map { it.date })
    }

    @Test fun specialThenPhotoThenBright() {
        val lines = listOf(l(9, 1, Feeling.JOY), l(9, 2, Feeling.CALM), l(9, 3, Feeling.MEH), l(9, 4, Feeling.JOY))
        val all = Recap.days(lines, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), today,
            { if (it.dayOfMonth == 3) "엄마 생신" else null }, { it.dayOfMonth == 2 })
        assertEquals(listOf(2, 3, 4), Recap.month(all).map { it.date.dayOfMonth })   // 생신 · 사진 · 나중의 기쁨
    }

    @Test fun todayAndBlankAreLeftOut() {
        val lines = listOf(DayLine(today, "오늘", Feeling.JOY), l(10, 1, Feeling.JOY, ""))
        assertTrue(Recap.days(lines, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), today, { null }, { false }).isEmpty())
    }

    @Test fun yearTakesEverySeasonFirst() {
        val lines = (1..9).map { l(4, it, Feeling.JOY) } + l(7, 1, Feeling.MEH) + l(10, 1, Feeling.MEH)
        val all = Recap.days(lines, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), today, { null }, { false })
        val y = Recap.year(all, 3)
        assertEquals(setOf(Season.SPRING, Season.SUMMER, Season.AUTUMN), y.map { Memories.seasonOf(it.date) }.toSet())
        assertEquals(y.sortedBy { it.date }, y)
        assertEquals(3, Recap.yearLines(all).map { Memories.seasonOf(it.date) }.toSet().size)
    }

    @Test fun moodSkipsHeavy() {
        assertNull(Recap.mood(listOf(l(9, 1, Feeling.SAD), l(9, 2, Feeling.SAD), l(9, 3, Feeling.JOY))))
        assertEquals(Feeling.THANKS, Recap.mood(listOf(l(9, 1, Feeling.THANKS), l(9, 2, Feeling.THANKS), l(9, 3, Feeling.JOY))))
    }

    @Test fun specialNames() {
        val sp = listOf(SpecialDay(LocalDate.of(2022, 5, 7), "첫 출근"))
        assertEquals("첫 출근", Recap.specialOf(LocalDate.of(2026, 5, 7), sp, emptyList()))
        assertEquals("엄마", Recap.specialOf(LocalDate.of(2026, 10, 3), sp, listOf("엄마" to LocalDate.of(1960, 10, 3))))
        assertNull(Recap.specialOf(LocalDate.of(2026, 5, 8), sp, emptyList()))
    }
}
