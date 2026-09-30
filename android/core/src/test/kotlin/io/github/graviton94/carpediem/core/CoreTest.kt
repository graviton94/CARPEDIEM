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

    @Test fun momentsComeWithTime() {
        val start = d(2026, 9, 30)
        assertEquals(listOf("moss"), Moments.earned(start, d(2000, 5, 12), 86.4, start).map { it.id })
        val week = Moments.earned(start, d(2000, 5, 12), 86.4, start.plusDays(7)).map { it.id }
        assertEquals(listOf("teacup", "moss"), week)
        val later = Moments.earned(start, d(2000, 5, 12), 86.4, d(2027, 6, 1)).associate { it.id to it.date }
        assertEquals(d(2027, 1, 1), later["dandelion"])
        assertEquals(d(2027, 3, 1), later["flower"])
        assertEquals(d(2027, 5, 12), later["candle"])
        assertEquals(d(2027, 6, 1), later["pond"])
        assertEquals(start.plusDays(100), later["cairn"])
        assertTrue("pine" !in later && "leaf" !in later)
    }

    @Test fun momentsOrderAndOptional() {
        val start = d(2026, 1, 10)
        val m = Moments.earned(start, d(1992, 2, 29), 80.0, d(2026, 3, 5), firstSkip = d(2026, 1, 11), returned = d(2026, 3, 4))
        assertEquals(m.map { it.date }, m.map { it.date }.sortedDescending())
        assertEquals(d(2026, 3, 1), m.first { it.id == "candle" }.date)
        assertTrue(m.any { it.id == "feather" } && m.any { it.id == "snail" })
        assertEquals(15, Moments.all(start).size)
    }

    @Test fun linesOnePerDayAndRoundTrip() {
        val a = DayLine(d(2026, 9, 30), "오늘은\t좋았다\n정말", Feeling.JOY)
        val list = Lines.add(Lines.add(emptyList(), a), a.copy(text = "두 번째"))
        assertEquals(1, list.size)
        val back = Lines.decode(Lines.encode(list + DayLine(d(2026, 10, 1), "그냥", null)))
        assertEquals("오늘은 좋았다 정말", back[0].text)
        assertEquals(Feeling.JOY, back[0].feeling)
        assertEquals(null, back[1].feeling)
        assertEquals("가나다", Lines.clean("  가나다라마  ", 3))
        assertEquals(emptyList(), Lines.decode(null))
    }

    @Test fun linesStreaksAndYearsAgo() {
        val days = (0L until 8L).map { d(2026, 1, 1).plusDays(it) } + (0L until 30L).map { d(2026, 3, 1).plusDays(it) }
        val list = days.map { DayLine(it, "줄", null) }
        assertEquals(d(2026, 1, 7), Lines.streakReached(days, 7))
        assertEquals(d(2026, 3, 30), Lines.streakReached(days, 30))
        assertEquals(null, Lines.streakReached(days, 100))
        // 기록을 지워도 이미 얻은 흔적은 남는다
        val kept = Lines.streaks(list, emptyMap())
        assertEquals(kept, Lines.streaks(emptyList(), kept))
        val m = Moments.earned(d(2025, 12, 1), d(1990, 5, 1), 80.0, d(2026, 4, 1), streaks = kept).map { it.id }
        assertTrue("pinwheel" in m && "paperboat" in m && "kite" !in m)
        // 1년 뒤 오늘 (날짜만 남긴 줄은 빼고), 2월 29일은 평년에 2월 28일
        val past = listOf(DayLine(d(2025, 9, 30), "작년", Feeling.HOPE), DayLine(d(2024, 9, 30), "재작년", null), DayLine(d(2025, 9, 30).minusYears(3), "", null), DayLine(d(2024, 2, 29), "윤날", null))
        assertEquals(listOf(1 to "작년", 2 to "재작년"), Lines.yearsAgo(past, d(2026, 9, 30)).map { it.first to it.second.text })
        assertEquals("윤날", Lines.yearsAgo(past, d(2025, 2, 28)).single().second.text)
        assertEquals("2025-09-30 · 희망 · 작년", Lines.export(past.take(1)) { "희망" })
    }

    @Test fun linesRandomPick() {
        val today = d(2026, 9, 30)
        val list = listOf(DayLine(today.minusDays(10), "최근", null), DayLine(today.minusDays(40), "한 달 전", Feeling.CALM), DayLine(today.minusYears(1), "작년 오늘", null), DayLine(today.minusDays(60), "", null))
        assertEquals("한 달 전", Lines.randomPick(list, today, 7L, 30)?.text)
        assertEquals(Lines.randomPick(list, today, 7L, 30), Lines.randomPick(list, today, 7L, 30))
        assertEquals(null, Lines.randomPick(list.take(1), today, 7L, 30))
        val next = Lines.nextRandomDay(today, 7L, 5, 20)
        assertTrue(next in today.plusDays(5)..today.plusDays(20))
    }

    /** 앱의 벡터 하루가 예전 그림(JS)과 같은 돌 · 같은 눈 자리인지. 값은 design/art/src 로 뽑은 것. */
    @Test fun haruShapeMatchesArtEngine() {
        data class Ref(val seed: Long, val stone: String, val eyes: List<Triple<Double, Double, Double>>, val top: Pair<Double, Double>)
        val refs = listOf(
            Ref(2718281, "gneiss", listOf(Triple(66.767, 124.584, 12.319), Triple(98.058, 128.612, 9.211)), 69.85 to 91.68),
            Ref(12345, "ring", listOf(Triple(99.255, 118.776, 12.178), Triple(130.110, 119.459, 11.291)), 105.17 to 67.21),
            Ref(99, "sand", listOf(Triple(74.173, 120.511, 14.629), Triple(109.754, 128.423, 12.250)), 83.63 to 86.05),
            Ref(4254103021, "basalt", listOf(Triple(92.363, 133.511, 15.185), Triple(130.663, 130.171, 14.803)), 108.14 to 90.48),
        )
        for (r in refs) {
            val s = HaruShape.of(r.seed, points = 180)
            assertEquals(r.stone, s.traits.stone.id)
            s.eyes.zip(r.eyes).forEach { (e, x) ->
                assertEquals(x.first, e.x, 0.01); assertEquals(x.second, e.y, 0.01); assertEquals(x.third, e.r, 0.01)
            }
            assertEquals(r.top.first, s.top.first, 0.05); assertEquals(r.top.second, s.top.second, 0.05)
        }
    }

    @Test fun familyPlacementNeverOverlaps() {
        val r = kotlin.random.Random(42)
        repeat(200) {
            val n = 1 + r.nextInt(5)
            val widths = List(n) { 36.0 + r.nextDouble() * 22 }
            val targets = List(n) { if (r.nextInt(8) == 0 && it != 0) null else 46.0 + r.nextDouble() * 298 }
            val xs = Family.place(targets, widths, 0, 26.0, 364.0, 10.0, 6.0)
            val sorted = xs.indices.sortedBy { xs[it] }
            for (k in 0 until n - 1) { val a = sorted[k]; val b = sorted[k + 1]; assertTrue(xs[b] - xs[a] >= (widths[a] + widths[b]) / 2 + 6.0 - 1e-6, "겹침 $targets") }
            xs.forEachIndexed { i, x -> assertTrue(x - widths[i] / 2 >= 26.0 - 1e-6 && x + widths[i] / 2 <= 364.0 + 1e-6, "길 밖 $targets") }
        }
        // 떨어져 있으면 제자리
        assertEquals(listOf(100.0, 250.0), Family.place(listOf(100.0, 250.0), listOf(52.0, 52.0), 0, 26.0, 364.0, 10.0, 6.0))
    }

    @Test fun familyDatesAndStorage() {
        assertEquals(d(2027, 2, 28), Family.nextBirthday(d(2000, 2, 29), d(2026, 3, 1)))
        assertTrue(Family.isBirthday(d(1964, 3, 2), d(2026, 3, 2)))
        val mom = Person("k3f9a2qz", "엄마\t", Kind.PERSON, birth = d(1964, 3, 2), sex = Sex.FEMALE, country = "KR", seed = 123L, metOn = d(2026, 10, 1))
        val dog = Person("p0p0p0p0", "콩이", Kind.PET, Species.DOG, d(2018, 5, 5), seed = 4254103021L, metOn = d(2026, 10, 1))
        val back = Family.decode(Family.encode(listOf(mom, dog)))
        assertEquals("엄마", back[0].name); assertEquals(mom.copy(name = "엄마"), back[0]); assertEquals(dog, back[1])
        assertEquals(d(1996, 5, 1), Family.togetherSince(d(1996, 5, 1), mom))
        assertEquals(d(2018, 5, 5), Family.togetherSince(d(1996, 5, 1), dog))
        // 한 줄의 받는 돌, 예전 기록(칸 3개)도 읽힘
        val lines = Lines.decode(Lines.encode(listOf(DayLine(d(2026, 10, 1), "고마워요", Feeling.THANKS, "k3f9a2qz"))) + "\n20000\t-\t예전")
        assertEquals("k3f9a2qz", lines[0].to); assertEquals(null, lines[1].to); assertEquals("예전", lines[1].text)
    }
}
