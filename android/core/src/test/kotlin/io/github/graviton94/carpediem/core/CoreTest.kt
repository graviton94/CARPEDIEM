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
        assertTrue(m.any { it.id == "bookmark" } && m.any { it.id == "snail" })
        assertEquals(16, Moments.all(start).size)
    }

    @Test fun linesOnePerDayAndRoundTrip() {
        val a = DayLine(d(2026, 9, 30), "오늘은\t좋았다\n정말", Feeling.JOY)
        val list = Lines.add(Lines.add(emptyList(), a), a.copy(text = "두 번째"))
        assertEquals(1, list.size)
        val back = Lines.decode(Lines.encode(list + DayLine(d(2026, 10, 1), "그냥", null)))
        assertEquals("오늘은 좋았다\n정말", back[0].text)   // 줄바꿈은 남고 탭은 빈칸
        assertEquals(Feeling.JOY, back[0].feeling)
        assertEquals(null, back[1].feeling)
        assertEquals("가나다", Lines.clean("  가나다라마  ", 3))
        assertEquals(emptyList(), Lines.decode(null))
    }

    @Test fun linesMoodAndSearch() {
        val list = listOf(DayLine(d(2026, 9, 1), "산책", Feeling.CALM), DayLine(d(2026, 9, 2), "비", Feeling.SAD), DayLine(d(2026, 9, 3), "또 산책", Feeling.CALM),
            DayLine(d(2026, 9, 4), "그냥", null, "mom"), DayLine(d(2026, 10, 1), "새 달", Feeling.JOY), DayLine(d(2026, 10, 4), "오늘 산책", Feeling.JOY))
        assertEquals(Feeling.CALM, Lines.monthMood(list, 2026, 9))
        assertEquals(null, Lines.monthMood(list, 2026, 8))
        // 같은 수면 나중에 고른 마음
        assertEquals(Feeling.SAD, Lines.monthMood(list.take(2), 2026, 9))
        val names = mapOf(Feeling.CALM to "고요", Feeling.SAD to "슬픔", Feeling.JOY to "기쁨")
        val find = { q: String -> Lines.search(list, q, d(2026, 10, 4), { names[it] ?: "" }, { if (it == "mom") "엄마" else null }).map { it.date.dayOfMonth } }
        assertEquals(listOf(3, 1), find("산책"))     // 최근 것부터, 오늘 것은 빼고
        assertEquals(listOf(2), find("슬픔"))
        assertEquals(listOf(4), find("엄마"))
        assertEquals(emptyList(), find("  "))
    }

    @Test fun firstWeekNudges() {
        assertEquals(null, FirstWeek.next(0, emptySet()))            // 만난 날은 둘러보기만
        assertEquals("breath", FirstWeek.next(1, emptySet()))
        assertEquals("breath", FirstWeek.next(3, emptySet()))        // 놓친 것은 다음 날로
        assertEquals("gaze", FirstWeek.next(5, setOf("breath", "stone")))
        assertEquals(null, FirstWeek.next(4, setOf("breath", "stone")))  // 아직 오지 않은 날의 것은 기다림 (이틀에 하나)
        assertEquals(null, FirstWeek.next(2, setOf("breath")))
        assertEquals(null, FirstWeek.next(14, emptySet()))           // 둘째 주가 끝나면 그만
    }

    @Test fun linesEditAndRemove() {
        val a = DayLine(d(2026, 10, 3), "어제", Feeling.CALM, "k3f9a2qz", 4); val b = DayLine(d(2026, 10, 4), "오늘", null)
        val list = listOf(a, b)
        // 고치면 글 · 마음만 바뀌고 받는 돌 · 질문은 그대로
        assertEquals(listOf(a.copy(text = "어제는\n비", feeling = Feeling.SAD), b), Lines.edit(list, a.date, "어제는\n비", Feeling.SAD))
        assertEquals(list, Lines.edit(list, d(2026, 10, 5), "없는 날", null))
        // 지우면 그날은 빈 날 → 다시 쓸 수 있음
        assertEquals(listOf(b), Lines.remove(list, a.date))
        assertEquals(2, Lines.add(Lines.remove(list, a.date), a.copy(text = "다시")).size)
    }

    @Test fun linesKeepLineBreaks() {
        // 여러 줄: 빈 줄 여럿은 하나로, 줄 끝 빈칸 없이, 최대 MAX_LINES 줄
        assertEquals("첫 줄\n\n둘째 줄", Lines.clean("  첫 줄   \r\n\n\n\n둘째 줄\n\n", 60, Lines.MAX_LINES))
        assertEquals("1\n2\n3\n4\n5", Lines.clean("1\n2\n3\n4\n5\n6\n7", 60, Lines.MAX_LINES))
        assertEquals("가나\n다", Lines.clean("가나\n다라마", 4, Lines.MAX_LINES))   // 줄바꿈도 한 글자
        // 한 줄 (기본): 예전처럼 줄바꿈을 빈칸으로
        assertEquals("가 나", Lines.clean("가\n\u2028나", 60))
        // 저장 · 읽기: 기록 하나는 한 줄 그대로, 글 안의 줄바꿈은 되살아남. 예전 기록 (줄바꿈 없음) 도 그대로
        val l = DayLine(d(2026, 10, 4), "아침엔 비\n저녁엔 \\n 해", Feeling.CALM, "k3f9a2qz", 7)
        val enc = Lines.encode(listOf(l, DayLine(d(2026, 10, 5), "그냥", null)))
        assertEquals(2, enc.lines().size)
        assertEquals(listOf(l, DayLine(d(2026, 10, 5), "그냥", null)), Lines.decode(enc))
        assertEquals("예전 \\n 글", Lines.decode("20000\t-\t예전 \\n 글").single().text)
        assertEquals("2026-10-04 · 고요 · 아침엔 비 / 저녁엔 \\n 해", Lines.export(listOf(l)) { "고요" })
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

    @Test fun nineStonesOverlapBeforeShrinking() {
        // 아홉 돌 (폭 46) 은 길 338 에 그냥은 안 앉지만, 22% 까지 겹치면 줄이지 않고 앉음
        val w = List(9) { 46.0 }; val g = Family.overlapGap(w, 0.22)
        assertEquals(1.0, Family.fitScale(w, 338.0, g))
        val xs = Family.place(List(9) { 195.0 }, w, 0, 26.0, 364.0, 10.0, g).sorted()
        for (k in 0 until 8) assertTrue(xs[k + 1] - xs[k] >= 46.0 + g - 1e-6, "너무 겹침 $xs")
        xs.forEach { assertTrue(it - 23.0 >= 26.0 - 1e-6 && it + 23.0 <= 364.0 + 1e-6) }
        // 여섯 돌은 겹치지 않음
        val six = Family.place(List(6) { 50.0 + it * 55.0 }, List(6) { 46.0 }, 0, 26.0, 364.0, 10.0, g).sorted()
        for (k in 0 until 5) assertTrue(six[k + 1] - six[k] >= 46.0 + 10.0 - 1e-6)
        // 더 많으면 그때 줄임
        assertTrue(Family.fitScale(List(12) { 46.0 }, 338.0, Family.overlapGap(List(12) { 46.0 }, 0.22)) < 1.0)
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

    @Test fun specialDays() {
        val a = SpecialDay(LocalDate.of(2020, 3, 2), "첫 출근"); val b = SpecialDay(LocalDate.of(2018, 5, 1), "이사한 날")
        val list = SpecialDays.put(SpecialDays.put(emptyList(), a), b)
        assertEquals(listOf(b, a), list)
        assertEquals(list, SpecialDays.decode(SpecialDays.encode(list)))
        // 같은 날에 다른 이름은 둘 다 남고, 같은 이름은 한 번만
        assertEquals(listOf(b, a, a.copy(name = "결혼")), SpecialDays.put(list, a.copy(name = "결혼")))
        assertEquals(list, SpecialDays.put(list, a))
        // 그 날이 든 달력 칸: 그날까지 지나온 단위 수
        assertEquals(22, LifeSnapshot(LocalDate.of(1998, 1, 15), 80.0, LocalDate.of(2020, 3, 2).atStartOfDay()).lived(LifeUnit.YEARS))
    }

    @Test fun constellationGarden() {
        val book = ConstellationBook(data("constellations.csv"))
        assertEquals((1..12).toList(), book.all.map { it.month })
        book.all.forEach { c ->
            for (n in listOf(28, 29, 30, 31)) {
                val spots = Constellations.scatter(c, n, Constellations.seed(7, 2026, c.month))
                assertEquals(n, spots.size, c.korean)
                spots.forEach { assertTrue(it.x in 0f..1f && it.y in 0f..1f && it.size in 0.6f..1.75f, c.korean) }
            }
            val a = Constellations.scatter(c, 30, Constellations.seed(7, 2026, c.month))
            assertEquals(a, Constellations.scatter(c, 30, Constellations.seed(7, 2026, c.month)))   // 같은 seed 면 같은 자리
            assertNotEquals(a, Constellations.scatter(c, 30, Constellations.seed(7, 2027, c.month)))   // 해마다 다르게
            assertNotEquals(a, Constellations.scatter(c, 30, Constellations.seed(8, 2026, c.month)))   // 사람마다 다르게
            // 흩어져도 별자리에서 온 무늬: 가장 가까운 별까지 평균 거리가 판의 1/3 안
            val near = a.map { s -> c.stars.minOf { (x, y) -> kotlin.math.hypot(x - s.x, y - s.y) } }.average()
            assertTrue(near < 0.33, "${c.korean} $near")
        }
        val m = Constellations.monthDays(listOf(DayLine(LocalDate.of(2026, 2, 28), "a", Feeling.JOY)), 2026, 2)
        assertEquals(28, m.size); assertEquals("a", m.last().second?.text)
        assertEquals(2026 to 9, Constellations.monthDue(LocalDate.of(2026, 10, 3), 3))
        assertEquals(null, Constellations.monthDue(LocalDate.of(2026, 10, 4), 3))
        assertEquals(null, Constellations.monthDue(LocalDate.of(2027, 1, 2), 3))   // 1월 초는 한 해의 정원
    }

    @Test fun birthdayEveAndAnniversaries() {
        val birth = LocalDate.of(1964, 10, 2)
        assertEquals(null, Family.birthdaySoon(birth, LocalDate.of(2026, 10, 1).atTime(16, 59), 17))
        assertEquals(1, Family.birthdaySoon(birth, LocalDate.of(2026, 10, 1).atTime(17, 0), 17))
        assertEquals(0, Family.birthdaySoon(birth, LocalDate.of(2026, 10, 2).atTime(23, 59), 17))
        assertEquals(null, Family.birthdaySoon(birth, LocalDate.of(2026, 10, 3).atTime(0, 0), 17))
        val work = SpecialDay(LocalDate.of(2022, 4, 4), "첫 출근"); val leap = SpecialDay(LocalDate.of(2020, 2, 29), "윤일")
        assertEquals(listOf(work to 4), SpecialDays.anniversaries(listOf(work, leap), LocalDate.of(2026, 4, 4)))
        assertEquals(listOf(leap to 6), SpecialDays.anniversaries(listOf(work, leap), LocalDate.of(2026, 2, 28)))
        assertEquals(emptyList(), SpecialDays.anniversaries(listOf(work), LocalDate.of(2022, 4, 4)))   // 그해 당일은 아님
    }
}

/** 정원 꾸밈 (자리 여섯): 쌓인 수만 세고, 실제 계절은 나라의 반구를 따른다. */
class GardenDecorTest {
    private fun line(y: Int, m: Int, day: Int, f: Feeling?) = DayLine(d(y, m, day), "한 줄", f)

    @Test fun realSeasonFollowsHemisphere() {
        assertEquals(Season.AUTUMN, GardenDecor.realSeason(d(2026, 10, 1), "KR"))
        assertEquals(Season.SPRING, GardenDecor.realSeason(d(2026, 10, 1), "AU"))
        assertEquals(Season.WINTER, GardenDecor.realSeason(d(2027, 2, 28), "KR"))
        assertEquals(Season.SUMMER, GardenDecor.realSeason(d(2027, 1, 5), "nz"))
    }

    @Test fun albumYearKeepsDecemberSeasonTogether() {
        assertEquals(2026, GardenDecor.albumYear(d(2026, 12, 20)))
        assertEquals(2026, GardenDecor.albumYear(d(2027, 2, 1)))
        assertEquals(2027, GardenDecor.albumYear(d(2027, 3, 1)))
    }

    @Test fun treeGrowsWithDaysTogether() {
        val days = listOf(100, 365, 1095)
        assertEquals(0, GardenDecor.stage(d(2026, 1, 1), d(2026, 4, 10), days))
        assertEquals(1, GardenDecor.stage(d(2026, 1, 1), d(2026, 4, 11), days))
        assertEquals(2, GardenDecor.stage(d(2026, 1, 1), d(2027, 1, 1), days))
        assertEquals(3, GardenDecor.stage(d(2026, 1, 1), d(2029, 1, 1), days))
    }

    @Test fun hangsByBreathDays() {
        val lv = listOf(1, 30, 100)
        assertEquals(Hang.NONE, GardenDecor.hang(0, lv)); assertEquals(Hang.CHIME, GardenDecor.hang(1, lv))
        assertEquals(Hang.BELL, GardenDecor.hang(30, lv)); assertEquals(Hang.LANTERN, GardenDecor.hang(140, lv))
    }

    @Test fun kiteAndRibbonsFromWrittenLines() {
        val r = GardenDecor.Rules()
        val few = (1..29).map { line(2026, 1, 1, Feeling.JOY).copy(date = d(2026, 1, 1).plusDays(it.toLong())) }
        assertTrue(GardenDecor.ribbons(few, r).isEmpty())
        // 30줄 = 연 + 리본 하나 (가장 많은 마음), 빈 한 줄은 세지 않음, 마음 없는 묶음은 null
        val joy = (0 until 20).map { DayLine(d(2026, 1, 1).plusDays(it.toLong()), "글", Feeling.JOY) } + (20 until 30).map { DayLine(d(2026, 1, 1).plusDays(it.toLong()), "글", Feeling.CALM) }
        val blank = listOf(DayLine(d(2026, 3, 1), "", Feeling.SAD))
        val none = (40 until 70).map { DayLine(d(2026, 1, 1).plusDays(it.toLong()), "글", null) }
        assertEquals(listOf(Feeling.JOY), GardenDecor.ribbons(joy + blank, r))
        assertEquals(listOf(Feeling.JOY, null), GardenDecor.ribbons(joy + none, r))
        // 많아야 넷, 최근 것
        val many = (0 until 300).map { DayLine(d(2020, 1, 1).plusDays(it.toLong()), "글", if (it < 30) Feeling.SAD else Feeling.HOPE) }
        assertEquals(4, GardenDecor.ribbons(many, r).size); assertEquals(Feeling.HOPE, GardenDecor.ribbons(many, r).first())
    }

    @Test fun decorPutsTogetherTheSixSlots() {
        val birth = d(1990, 5, 5)
        val dec = GardenDecor.of(d(2026, 10, 1), "KR", Season.SUMMER, birth, 84.0, d(2025, 1, 1), breathDays = 31, lines = emptyList(), gazeDays = 57, letterDue = true)
        assertEquals(Season.AUTUMN, dec.season); assertEquals(Tree.ZELKOVA, dec.tree); assertEquals(2, dec.stage)
        assertEquals(Hang.BELL, dec.hang); assertTrue(dec.letter); assertEquals(false, dec.kite); assertEquals(5, dec.buds)
        assertEquals("zelkova_autumn", dec.card.key); assertEquals(dec.card, SeasonCard.parse(dec.card.id))
    }

    @Test fun treeChangesSlowlyAfterLifeSeasonTurns() {
        val birth = d(2000, 1, 1)
        val turn = GardenDecor.lifeChange(birth, 80.0, d(2021, 1, 1))!!   // 1/4 = 인생의 여름이 시작한 날
        val first = GardenDecor.of(turn, "KR", Season.SUMMER, birth, 80.0, d(2019, 1, 1), 0, emptyList(), 0, false)
        assertEquals(Tree.CHERRY, first.prevTree); assertTrue(first.blend < 0.2f)
        val later = GardenDecor.of(turn.plusDays(8), "KR", Season.SUMMER, birth, 80.0, d(2019, 1, 1), 0, emptyList(), 0, false)
        assertEquals(null, later.prevTree); assertEquals(1f, later.blend)
    }
}

class SeasonAlbumTest {
    @Test fun linesGroupByAlbumSeason() {
        val l = listOf(DayLine(d(2026, 12, 3), "a", Feeling.CALM), DayLine(d(2027, 1, 9), "b", Feeling.CALM), DayLine(d(2027, 2, 9), "c", Feeling.JOY), DayLine(d(2027, 3, 1), "d", null), DayLine(d(2027, 3, 2), "", Feeling.SAD))
        val m = GardenDecor.seasonLines(l, "KR")
        assertEquals(GardenDecor.SeasonLines(3, Feeling.CALM), m[2026 to Season.WINTER])
        assertEquals(GardenDecor.SeasonLines(1, null), m[2027 to Season.SPRING])
    }
}

class ChancesTest {
    @Test fun lineBringsWindOrRainbowAfterHeavyDay() {
        val today = d(2026, 10, 2)
        assertEquals(Chance.WIND, Chances.onLine(listOf(DayLine(d(2026, 10, 1), "a", Feeling.JOY)), today))
        assertEquals(Chance.RAINBOW, Chances.onLine(listOf(DayLine(d(2026, 10, 1), "a", Feeling.SAD)), today))
    }
    @Test fun nightBreathBringsFirefliesOrAurora() {
        assertEquals(null, Chances.onBreath(false, true, 20))
        assertEquals(Chance.AURORA, Chances.onBreath(true, true, 40))
        assertEquals(Chance.FIREFLIES, Chances.onBreath(true, true, 41)); assertEquals(Chance.FIREFLIES, Chances.onBreath(true, false, 40))
    }
    @Test fun openingBringsSnailBubblesButterfliesOnce() {
        val today = d(2026, 10, 2)   // 목요일, 짝수 날
        assertEquals(Chance.SNAIL to "snail:$today", Chances.onOpen(today, Season.AUTUMN, null, today, emptyList(), emptySet()))
        assertEquals(null, Chances.onOpen(today, Season.AUTUMN, null, today, emptyList(), setOf("snail:$today")))
        val even = d(2026, 10, 2).let { if (it.toEpochDay() % 2 == 0L) it else it.plusDays(1) }
        assertEquals(Chance.BUBBLES, Chances.onOpen(even, Season.AUTUMN, even, null, emptyList(), emptySet())?.first)
        val breaths = listOf(d(2026, 6, 1), d(2026, 6, 2), d(2026, 6, 3))   // 월 · 화 · 수
        assertEquals(Chance.BUTTERFLIES, Chances.onOpen(d(2026, 6, 3), Season.SUMMER, null, null, breaths, emptySet())?.first)
        assertEquals(null, Chances.onOpen(d(2026, 6, 3), Season.AUTUMN, null, null, breaths, emptySet()))
    }

    @Test fun comfortOnlyAfterHeavyYesterdayOncePerDay() {
        val today = d(2026, 10, 5)
        val heavy = listOf(DayLine(d(2026, 10, 4), "걱정", Feeling.WORRY))
        assertTrue(Comfort.due(heavy, today, null))
        assertEquals(false, Comfort.due(heavy, today, today))
        assertEquals(false, Comfort.due(heavy, today, today.minusDays(1)))   // 사흘에 한 번까지
        assertTrue(Comfort.due(heavy, today, today.minusDays(3)))
        assertEquals(false, Comfort.due(listOf(DayLine(d(2026, 10, 4), "좋아", Feeling.JOY)), today, null))
        assertEquals(false, Comfort.due(listOf(DayLine(d(2026, 10, 3), "걱정", Feeling.WORRY)), today, null))
    }
    @Test fun seedsPlantAnswerAndRoundTrip() {
        val day = d(2026, 10, 5)
        var s = Seeds.plant(emptyList(), day, "  한 번 웃기 ")
        assertEquals("한 번 웃기", Seeds.of(s, day)?.text)
        s = Seeds.plant(s, day, "하늘 보기")   // 같은 날은 바꿔 심음
        assertEquals(1, s.size); assertEquals(Seeds.toAsk(s, day)?.text, "하늘 보기")
        assertEquals(s, Seeds.plant(s, day, "   "))
        s = Seeds.answer(s, day, bloomed = true)
        assertEquals(null, Seeds.toAsk(s, day)); assertEquals(1, Seeds.bloomed(s).size)
        s = Seeds.plant(s, d(2026, 10, 6), "천천히 먹기").let { Seeds.answer(it, d(2026, 10, 6), bloomed = false) }
        assertEquals(SeedState.RESTING, Seeds.of(s, d(2026, 10, 6))?.state); assertEquals(1, Seeds.bloomed(s).size)
        assertEquals(s, Seeds.decode(Seeds.encode(s)))
        assertEquals(24, Seeds.plant(emptyList(), day, "가".repeat(40)).first().text.length)
        val c = Seeds.choices(10, day, 4); assertEquals(4, c.size); assertTrue(c.all { it in 0 until 10 })
        assertNotEquals(c, Seeds.choices(10, day.plusDays(1), 4))
    }
    @Test fun breathCyclesAndTouchWave() {
        val r = Breath.Rhythm(4.0, 7.0, 8.0, 0.0)
        val plan = Breath.cycles(r, 2)
        assertEquals(6, plan.size); assertEquals(38_000L, plan.last().let { it.startMs + it.lengthMs })
        val (times, amps) = Breath.touchWave(plan, 0)
        assertEquals(38_000L, times.sum()); assertEquals(times.size, amps.size)
        assertTrue(amps.all { it in 0..255 })
        // 들이쉼은 차오르고 (처음 < 끝), 머묾은 고요
        val first = amps.first(); assertTrue(amps.take(40).max() > first)
        assertEquals(0, Breath.touchWave(plan, 5_000).second.first())
        // 중간부터: 남은 길이만큼
        assertEquals(38_000L - 20_000L, Breath.touchWave(plan, 20_000).first.sum())
    }

    @Test fun capsulesBuryOpenOnTheDay() {
        val today = d(2026, 10, 4); val birth = d(2000, 5, 12)
        assertEquals(d(2027, 5, 12), Capsules.opensOn(CapsuleWhen.BIRTHDAY, today, birth))
        assertEquals(d(2027, 5, 12), Capsules.opensOn(CapsuleWhen.BIRTHDAY, d(2026, 5, 12), birth))   // 생일 당일이면 내년
        assertEquals(d(2027, 10, 4), Capsules.opensOn(CapsuleWhen.YEAR, today, birth))
        var l = Capsules.bury(emptyList(), today, d(2027, 5, 12), "1년 뒤의 너에게\n잘 지내?")
        assertEquals(l, Capsules.bury(l, today, today, "오늘 열림은 안 됨"))
        assertEquals(1, Capsules.sealed(l, today).size); assertEquals(null, Capsules.due(l, d(2027, 5, 11)))
        val c = Capsules.due(l, d(2027, 5, 12))!!
        assertEquals("1년 뒤의 너에게\n잘 지내?", c.text)
        l = Capsules.open(l, c)
        assertEquals(null, Capsules.due(l, d(2027, 6, 1))); assertEquals(1, Capsules.openedOnes(l).size)
        assertEquals(l, Capsules.decode(Capsules.encode(l)))
    }
    @Test fun ringsTwelveMonthsOfMood() {
        val birth = d(2000, 5, 12)
        val lines = listOf(DayLine(d(2025, 5, 20), "a", Feeling.JOY), DayLine(d(2025, 5, 21), "b", Feeling.JOY), DayLine(d(2025, 6, 1), "c", Feeling.SAD),
            DayLine(d(2026, 5, 11), "d", Feeling.THANKS), DayLine(d(2026, 5, 12), "e", Feeling.CALM))
        val r = Rings.of(birth, 26, lines)
        assertEquals(d(2025, 5, 12), r.start); assertEquals(d(2026, 5, 11), r.end)
        assertEquals(12, r.months.size); assertEquals(Feeling.JOY, r.months[0]); assertEquals(Feeling.THANKS, r.months[11])
        assertEquals(4, r.lines); assertEquals(1, r.thanks); assertEquals(Feeling.JOY, r.top); assertTrue(r.pick?.feeling != Feeling.SAD)
        assertEquals(26, Rings.newToday(birth, d(2026, 5, 12), lines))
        assertEquals(null, Rings.newToday(birth, d(2026, 5, 13), lines))
        assertEquals(listOf(27, 26), Rings.done(birth, d(2027, 6, 1), lines))
    }

    @Test fun paceKeepsPromptsRare() {
        val today = d(2026, 10, 10)
        assertTrue(Pace.seed(today, null, false))
        assertEquals(false, Pace.seed(today, today.minusDays(2), true))
        assertTrue(Pace.seed(today, today.minusDays(3), true))
        assertEquals(false, Pace.seed(today, today.minusDays(5), false))   // 심지 않고 지나갔으면 일주일 쉼
        assertTrue(Pace.seed(today, today.minusDays(7), false))
        val month = (0 until 30).count { Pace.morningBreath(today.plusDays(it.toLong())) }
        assertEquals(10, month)
        assertEquals(false, Pace.gap(today.minusDays(1), today, Pace.CARE_GAP))
    }

    @Test fun guestsComeOnRandomDaysNotForBeingAway() {
        val start = d(2026, 1, 1)
        val year = (0 until 365).map { Guests.on(start.plusDays(it.toLong()), 42L) }
        val visits = year.filterNotNull()
        assertTrue(visits.size in 35..70)                                // 평균 일주일에 한 번쯤
        assertTrue(visits.all { it in Guests.COMMON || it in Guests.RARE })
        assertTrue(visits.count { it in Guests.RARE } in 1 until visits.size / 2)
        assertEquals(year, (0 until 365).map { Guests.on(start.plusDays(it.toLong()), 42L) })   // 같은 날은 늘 같은 손님
        assertTrue(year != (0 until 365).map { Guests.on(start.plusDays(it.toLong()), 7L) })   // 사람마다 다른 날
    }

    @Test fun creditsFitSixtyToEightySeconds() {
        val empty = Credits.plan(emptyList(), 2026)
        assertEquals(6, empty.size); assertTrue(Credits.total(empty) in 60_000L..80_000L)
        val many = (0 until 365).map { DayLine(d(2026, 1, 1).plusDays(it.toLong()), "줄 $it", Feeling.entries[it % Feeling.entries.size]) }
        val mom = Person("m", "엄마", Kind.PERSON, birth = d(1964, 4, 2), seed = 1, metOn = d(2026, 1, 1))
        val pet = Person("p", "콩이", Kind.PET, Species.DOG, d(2023, 6, 1), seed = 2, metOn = d(2026, 1, 1))
        val ev = Credits.events(2026, many, d(2000, 5, 12), listOf(mom, pet), listOf(SpecialDay(d(2020, 9, 9), "첫 출근")), listOf("rainbow:2026-07-03", "guest_owl:2026-11-20", "snail:2025-01-01"),
            listOf(Seed(d(2026, 3, 3), "웃기", SeedState.BLOOMED)), emptyList(), d(2026, 2, 1))
        assertTrue(ev.any { it.kind == CreditKind.BIRTHDAY && it.a == "엄마" && it.date == d(2026, 4, 2) })
        assertTrue(ev.any { it.kind == CreditKind.MY_BIRTHDAY && it.b == "26" })
        assertTrue(ev.any { it.kind == CreditKind.TOGETHER_YEARS && it.a == "콩이" && it.b == "3" && it.date == d(2026, 6, 1) })
        assertTrue(ev.any { it.kind == CreditKind.TOGETHER_DAYS && it.a == "콩이" && it.b == "1000" })
        assertTrue(ev.any { it.kind == CreditKind.SPECIAL && it.b == "6" })
        assertEquals(2, ev.count { it.kind == CreditKind.MOMENT }); assertTrue(ev.any { it.kind == CreditKind.FIRST })
        val full = Credits.plan(ev, 2026)
        assertTrue(Credits.total(full) in 60_000L..80_000L)
        full.filter { it.part == CreditPart.SEASON }.forEach { sc ->
            assertTrue(sc.items.size <= Credits.PER_SEASON); assertTrue(sc.items.isNotEmpty())
            assertTrue(sc.items.all { Memories.seasonOf(it.date) == sc.season })
            assertEquals(sc.items.sortedBy { it.date }, sc.items)
            assertTrue(sc.items.any { it.kind == CreditKind.LINE })
            assertEquals(sc.items.first(), sc.itemAt(sc.startMs + Credits.HEADER_MS)?.first); assertEquals(null, sc.itemAt(sc.startMs))
        }
        assertTrue(full.first { it.season == Season.SPRING }.items.any { it.kind == CreditKind.BIRTHDAY })
        assertEquals(CreditPart.INTRO, Credits.at(full, 0)?.part); assertEquals(null, Credits.at(full, Credits.total(full)))
        assertTrue(Credits.milestone(1000)); assertTrue(Credits.milestone(100)); assertEquals(false, Credits.milestone(400))
    }
}
