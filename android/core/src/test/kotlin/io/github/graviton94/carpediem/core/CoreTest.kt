package io.github.graviton94.carpediem.core

import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private fun data(name: String) = File(System.getProperty("dataDir"), name).readText()
private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
private fun t(y: Int, m: Int, day: Int, h: Int = 0) = LocalDateTime.of(y, m, day, h, 0)

/** iOS LifeSnapshotTests 와 같은 예시. 두 플랫폼의 계산이 같아야 한다. */
class LifeSnapshotTest {
    @Test fun endDateWholeYears() = assertEquals(d(2080, 1, 1), LifeSnapshot.endDate(d(2000, 1, 1), 80.0))
    @Test fun endDateFractionalYears() = assertEquals(d(2080, 7, 2), LifeSnapshot.endDate(d(2000, 1, 1), 80.5))

    @Test fun livedAndRemaining() {
        val s = LifeSnapshot(d(2000, 1, 1), 1.0, t(2000, 1, 11, 12))
        assertEquals(10, s.lived(LifeUnit.DAYS))
        assertEquals(1, s.lived(LifeUnit.WEEKS))
        assertEquals(0, s.lived(LifeUnit.MONTHS))
        assertEquals(366, s.total(LifeUnit.DAYS))
        assertEquals(356, s.remaining(LifeUnit.DAYS))
        assertEquals(12, s.total(LifeUnit.MONTHS))
    }

    @Test fun ageTurnsOnBirthday() {
        assertEquals(35, LifeSnapshot(d(1990, 10, 1), 83.5, t(2026, 9, 30)).age)
        assertEquals(36, LifeSnapshot(d(1990, 10, 1), 83.5, t(2026, 10, 1)).age)
    }

    @Test fun beyondExpectancy() {
        val s = LifeSnapshot(d(1900, 1, 1), 80.0, t(2026, 1, 1))
        assertEquals(1.0, s.progress)
        assertTrue(s.isBeyondExpectancy)
        assertEquals(0, s.remaining(LifeUnit.DAYS))
        assertEquals(Season.WINTER, s.season)
    }

    @Test fun beforeBirth() {
        val s = LifeSnapshot(d(2030, 1, 1), 80.0, t(2026, 1, 1))
        assertEquals(0.0, s.progress)
        assertEquals(0, s.lived(LifeUnit.DAYS))
        assertEquals(Season.SPRING, s.season)
    }

    @Test fun seasons() {
        assertEquals(Season.SPRING, Season.of(0.24))
        assertEquals(Season.SUMMER, Season.of(0.25))
        assertEquals(Season.AUTUMN, Season.of(0.6))
        assertEquals(Season.WINTER, Season.of(0.99))
    }

    @Test fun periodProgress() {
        val s = LifeSnapshot(d(1990, 1, 1), 80.0, t(2026, 7, 2, 12))
        assertEquals(0.5, s.period(LifePeriod.DAY).progress, 1e-4)
        assertEquals(0.5, s.period(LifePeriod.YEAR).progress, 1e-4)
        assertEquals(12, s.period(LifePeriod.DAY).hoursLeft)
    }

    @Test fun periodRemainingDays() {
        val s = LifeSnapshot(d(1990, 1, 1), 80.0, t(2026, 9, 30, 9))
        assertEquals(92, s.period(LifePeriod.YEAR).remainingDaysAfterToday)
        assertEquals(0, s.period(LifePeriod.MONTH).remainingDaysAfterToday)
        assertEquals(4, s.period(LifePeriod.WEEK).remainingDaysAfterToday)
    }
}

class DataTest {
    @Test fun csvHandlesQuotesCommasAndComments() {
        assertEquals(listOf(listOf("a", "b"), listOf("x, y", "그가 \"안녕\" 했다")), Csv.rows("# 주석\na,b\n\"x, y\",\"그가 \"\"안녕\"\" 했다\"\n"))
    }

    @Test fun quotesAreComplete() {
        val book = QuoteBook(data("quotes.csv"))
        assertEquals((1..100).toList(), book.quotes.map { it.number })
        assertTrue(book.quotes.all { it.korean.isNotBlank() && it.english.isNotBlank() })
    }

    @Test fun quotesDoNotRepeatWithinACycle() {
        val book = QuoteBook(data("quotes.csv"))
        assertEquals(100, (0 until 100).map { book.index(it, 42uL) }.toSet().size)
    }

    @Test fun newCycleDoesNotRepeatYesterday() {
        val book = QuoteBook(data("quotes.csv"))
        for (seed in 0uL until 200uL) assertNotEquals(book.index(99, seed), book.index(100, seed), "seed $seed")
    }

    @Test fun quoteOrderStableForSameSeed() {
        val book = QuoteBook(data("quotes.csv"))
        assertEquals((0 until 30).map { book.index(it, 7uL) }, (0 until 30).map { book.index(it, 7uL) })
        assertNotEquals((0 until 30).map { book.index(it, 7uL) }, (0 until 30).map { book.index(it, 8uL) })
    }

    @Test fun lifeExpectancyTable() {
        val table = LifeExpectancyTable(data("life-expectancy.csv"))
        assertTrue(table.countries.size > 200)
        assertTrue(table.countries.all { it.total in 40.0..100.0 })
        assertEquals("KR", table.defaultCountry("KR"))
        assertEquals(LifeExpectancyTable.WORLD, table.defaultCountry("ZZ"))
        assertEquals(table.country("WLD")!!.total, table.expectancy("ZZ", Sex.OTHER))
        assertEquals("대한민국", table.country("KR")!!.displayName(Locale.KOREAN))
    }

    @Test fun customExpectancyWins() {
        val table = LifeExpectancyTable(data("life-expectancy.csv"))
        val p = LifeProfile(d(1994, 6, 15), "KR", Sex.MALE, null)
        assertEquals(table.country("KR")!!.male, p.expectancy(table))
        assertEquals(95.0, p.copy(customExpectancy = 95.0).expectancy(table))
    }
}
