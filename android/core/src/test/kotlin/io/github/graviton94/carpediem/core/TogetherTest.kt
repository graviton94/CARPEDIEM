package io.github.graviton94.carpediem.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 함께한 날의 기념일 (1.1.6): 한 해 돌아보기와 같은 규칙. */
class TogetherTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private val dog = Person("p", "콩이", Kind.PET, Species.DOG, birth = d(2020, 5, 5), seed = 1, metOn = d(2026, 10, 1))
    private val mom = Person("m", "엄마", Kind.PERSON, birth = d(1964, 3, 2), seed = 2, metOn = d(2026, 10, 1))

    @Test fun petCountsFromBirth() {
        assertEquals(Family.Together(1, true), Family.togetherToday(dog, d(2021, 5, 5)))
        assertEquals(Family.Together(100, false), Family.togetherToday(dog, d(2020, 5, 5).plusDays(100)))
        assertEquals(Family.Together(1000, false), Family.togetherToday(dog, d(2020, 5, 5).plusDays(1000)))
        assertNull(Family.togetherToday(dog, d(2020, 5, 5).plusDays(101)))
        assertNull(Family.togetherToday(dog, d(2020, 5, 5)))       // 첫날은 기념일이 아님
        assertNull(Family.togetherToday(dog, d(2019, 1, 1)))       // 오기 전
    }

    /** 사람은 직접 정한 첫날이 있을 때만 (생일은 함께한 날이 아님). */
    @Test fun personNeedsChosenDay() {
        assertNull(Family.togetherToday(mom, d(2026, 3, 2)))
        val chosen = mom.copy(together = d(2016, 10, 13))
        assertEquals(Family.Together(10, true), Family.togetherToday(chosen, d(2026, 10, 13)))
    }

    @Test fun leapDayFallsOnFeb28() {
        val leap = dog.copy(birth = d(2020, 2, 29))
        assertEquals(Family.Together(1, true), Family.togetherToday(leap, d(2021, 2, 28)))
        assertEquals(Family.Together(4, true), Family.togetherToday(leap, d(2024, 2, 29)))
    }
}
