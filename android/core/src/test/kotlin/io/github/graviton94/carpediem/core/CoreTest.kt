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
        assertEquals(16, Moments.all(start).size)
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
        val friend = mom.copy(together = d(2015, 3, 1))
        assertEquals(d(2015, 3, 1), Family.togetherSince(d(1996, 5, 1), friend))
        assertEquals(friend.copy(name = "엄마"), Family.decode(Family.encode(listOf(friend)))[0])
        // 1.1 기록(칸 11개)도 읽힘
        assertEquals(null, Family.decode(Family.encode(listOf(mom)).substringBeforeLast("\t"))[0].together)
        // 한 줄의 받는 돌, 예전 기록(칸 3개)도 읽힘
        val lines = Lines.decode(Lines.encode(listOf(DayLine(d(2026, 10, 1), "고마워요", Feeling.THANKS, "k3f9a2qz"))) + "\n20000\t-\t예전")
        assertEquals("k3f9a2qz", lines[0].to); assertEquals(null, lines[1].to); assertEquals("예전", lines[1].text)
    }

    @Test fun breathPlan() {
        val calm = Breath.Rhythm(4.0, 0.0, 6.0, 0.0)
        val one = Breath.plan(calm, 1)
        assertEquals(12, one.size)                     // 6번 × (들이쉼 · 내쉼)
        assertEquals(60_000L, one.last().startMs + one.last().lengthMs)
        assertTrue(one.none { it.step == BreathStep.HOLD })
        val sleep = Breath.plan(Breath.Rhythm(4.0, 7.0, 8.0, 0.0), 1)
        val end = sleep.last().startMs + sleep.last().lengthMs
        assertTrue(end in 57_000L..76_000L, "마지막 숨은 끝까지: $end")
        assertEquals(BreathStep.OUT, sleep.last().step)
        assertEquals(BreathStep.IN, Breath.at(one, 1_000)!!.first.step)
        assertEquals(BreathStep.OUT, Breath.at(one, 5_000)!!.first.step)
        assertEquals(null, Breath.at(one, 60_000))
        assertEquals(0f, Breath.fullness(BreathStep.IN, 0f), 1e-4f); assertEquals(1f, Breath.fullness(BreathStep.IN, 1f), 1e-4f)
        val days = listOf(d(2026, 10, 1) to BreathKind.BOX)
        assertEquals(days, Breath.decode(Breath.encode(days)))
        // 숨의 흔적: 처음 숨 쉰 날 풍경
        val m = Moments.earned(d(2026, 9, 1), d(1990, 1, 1), 80.0, d(2026, 10, 2), firstBreath = d(2026, 10, 1)).map { it.id }
        assertTrue("windchime" in m)
    }
}

class ReflectTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test fun questionsTwiceAWeekNoRepeatFor30Weeks() {
        val book = QuestionBook(data("questions.csv"))
        assertEquals(60, book.all.size)
        assertEquals(setOf("scene", "mood", "joy", "now"), book.all.map { it.group }.toSet())
        for (seed in listOf(0L, 1L, 6L, 13L, 2718281L, -99L)) {
            val (a, b) = Questions.days(seed)
            assertTrue(a != b && (b.value - a.value) in 3..4)
            // 3년치: 주마다 정확히 두 번, 연달아 60번 안에는 같은 질문 없음
            var day = d(2026, 1, 5)
            val asked = mutableListOf<Int>()
            repeat(52 * 3) {
                val week = (0 until 7).mapNotNull { book.of(seed, day.plusDays(it.toLong())) }
                assertEquals(2, week.size)
                asked += week.map { it.id }
                day = day.plusWeeks(1)
            }
            asked.windowed(60, 1).forEach { assertEquals(60, it.toSet().size) }
            // 갈래가 번갈아
            assertNotEquals(book.byId(asked[0])!!.group, book.byId(asked[1])!!.group)
        }
        assertEquals(book.of(7, d(2026, 10, 1)), book.of(7, d(2026, 10, 1)))
    }

    @Test fun linesKeepQuestionAndReadOldRows() {
        val list = listOf(
            DayLine(d(2026, 1, 1), "a", Feeling.JOY),
            DayLine(d(2026, 1, 2), "b", null, "p1"),
            DayLine(d(2026, 1, 3), "c", Feeling.CALM, null, 12),
            DayLine(d(2026, 1, 4), "d", null, "p2", 3),
        )
        assertEquals(list, Lines.decode(Lines.encode(list)))
        assertEquals(listOf(DayLine(d(2026, 1, 1), "x", null)), Lines.decode("${d(2026, 1, 1).toEpochDay()}\t-\tx"))
    }

    @Test fun moodSkyLastDays() {
        val list = listOf(DayLine(d(2026, 9, 30), "a", Feeling.JOY), DayLine(d(2026, 9, 1), "b", Feeling.SAD), DayLine(d(2026, 8, 31), "c", null))
        val sky = Lines.lastDays(list, d(2026, 9, 30), 30)
        assertEquals(30, sky.size)
        assertEquals(d(2026, 9, 1), sky.first().first)
        assertEquals(Feeling.SAD, sky.first().second?.feeling)
        assertEquals(Feeling.JOY, sky.last().second?.feeling)
        assertEquals(28, sky.count { it.second == null })
    }

    @Test fun seasonalLetters() {
        val list = listOf(
            DayLine(d(2023, 11, 30), "before", Feeling.JOY),
            DayLine(d(2023, 12, 1), "first", Feeling.THANKS),
            DayLine(d(2024, 1, 5), "", null),
            DayLine(d(2024, 2, 29), "leap", Feeling.SAD),
            DayLine(d(2024, 2, 10), "mid", Feeling.HOPE),
            DayLine(d(2024, 3, 1), "after", Feeling.CALM),
        )
        val l = Letters.of(list, d(2024, 3, 1), 3)!!
        assertEquals(Season.SPRING, l.season)
        assertEquals(listOf("first", "mid"), l.lines.map { it.text })
        assertEquals(listOf("leap"), l.heavy.map { it.text })
        assertEquals(d(2024, 2, 29), l.until)
        assertEquals("2024-03", l.id)
        // 세 줄보다 적으면 오지 않음
        assertEquals(null, Letters.of(list.take(3), d(2024, 3, 1), 3))
        assertEquals(d(2024, 3, 1), Letters.due(d(2024, 3, 20)))
        assertEquals(null, Letters.due(d(2024, 4, 1)))
        assertEquals(listOf("2024-03"), Letters.received(list, d(2024, 9, 2), 3).map { it.id })
    }

    @Test fun nineStonesFit() {
        assertEquals(1.0, Family.fitScale(listOf(30.0, 30.0), 300.0, 6.0))
        val w = List(9) { 40.0 }
        val k = Family.fitScale(w, 330.0, 6.0)
        assertTrue(k < 1.0 && w.sum() * k + 6.0 * 8 <= 330.0 + 1e-6)
    }

    @Test fun memoryStoneFields() {
        val m = Person("m1", "보리", Kind.PET, Species.DOG, LocalDate.of(2011, 4, 2), seed = 7, metOn = LocalDate.of(2026, 10, 1), until = LocalDate.of(2024, 12, 20), star = false)
        assertEquals(listOf(m), Family.decode(Family.encode(listOf(m))))
        // 예전 기록 (칸 12개) 은 별 켬, 떠난 날 없음
        val old = Family.decode(Family.encode(listOf(m.copy(until = null, star = true))).split('\t').take(12).joinToString("\t")).single()
        assertEquals(null, old.until); assertTrue(old.star)
        assertEquals(Season.WINTER, Memories.seasonOf(LocalDate.of(2024, 12, 20)))
        assertEquals(Season.SPRING, Memories.seasonOf(LocalDate.of(2011, 4, 2)))
        assertEquals(LocalDate.of(2011, 4, 2), Memories.from(m))
        assertEquals(null, Memories.from(m.copy(kind = Kind.PERSON, species = null)))
    }

    @Test fun dayParts() {
        assertEquals(DayPart.NIGHT, DayPart.of(4)); assertEquals(DayPart.MORNING, DayPart.of(5)); assertEquals(DayPart.MORNING, DayPart.of(10))
        assertEquals(DayPart.DAY, DayPart.of(11)); assertEquals(DayPart.EVENING, DayPart.of(17)); assertEquals(DayPart.NIGHT, DayPart.of(21)); assertEquals(DayPart.NIGHT, DayPart.of(0))
    }

    @Test fun yearAndWish() {
        val list = listOf(DayLine(LocalDate.of(2024, 2, 29), "a", Feeling.JOY), DayLine(LocalDate.of(2024, 12, 31), "b", null))
        val y = Lines.yearDays(list, 2024)
        assertEquals(366, y.size); assertEquals(Feeling.JOY, y[59].second?.feeling); assertEquals("b", y.last().second?.text)
        assertEquals(2026, Lines.yearDue(LocalDate.of(2026, 12, 31))); assertEquals(2026, Lines.yearDue(LocalDate.of(2027, 1, 7)))
        assertEquals(null, Lines.yearDue(LocalDate.of(2027, 1, 8))); assertEquals(null, Lines.yearDue(LocalDate.of(2026, 12, 30)))
        assertEquals("2026-12", Lines.wishDue(LocalDate.of(2026, 12, 14))); assertEquals(null, Lines.wishDue(LocalDate.of(2026, 12, 15))); assertEquals(null, Lines.wishDue(LocalDate.of(2026, 11, 1)))
    }
}
