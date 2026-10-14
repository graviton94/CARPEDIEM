package io.github.graviton94.carpediem.core

import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 모은 것 (1.1.6): 또렷해진 별자리 · 만난 절기. */
class CollectTest {
    private fun line(y: Int, m: Int, d: Int) = DayLine(LocalDate.of(y, m, d), "a", null)

    @Test fun constellationNeedsEnoughDaysInOneMonth() {
        val oct = (1..9).map { line(2026, 10, it) }
        assertNull(Constellations.clearMonths(oct, 10)[10])                       // 아흐레는 아직
        val more = oct + line(2026, 10, 20) + line(2026, 10, 20)                   // 같은 날 두 줄은 하루
        assertEquals(LocalDate.of(2026, 10, 20), Constellations.clearMonths(more, 10)[10])
        // 두 달에 나뉜 열흘은 모이지 않음
        val split = (1..5).map { line(2026, 9, it) } + (1..5).map { line(2026, 10, it) }
        assertEquals(emptyMap(), Constellations.clearMonths(split, 10))
    }

    @Test fun sameConstellationKeepsFirstYear() {
        val a = (1..10).map { line(2026, 3, it) }; val b = (1..10).map { line(2027, 3, it) }
        assertEquals(LocalDate.of(2026, 3, 10), Constellations.clearMonths(b + a, 10)[3])
    }

    @Test fun termsMetOnTheirDays() {
        val seoul = ZoneId.of("Asia/Seoul")
        val met = Sky.termsMet(listOf(LocalDate.of(2027, 10, 23), LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 24), LocalDate.of(2026, 3, 20)), seoul)
        assertEquals(LocalDate.of(2026, 10, 23), met[SolarTerm.SANGGANG])      // 처음 만난 날
        assertEquals(LocalDate.of(2026, 3, 20), met[SolarTerm.CHUNBUN])
        assertEquals(2, met.size)                                              // 절기가 아닌 날은 없음
    }
}
