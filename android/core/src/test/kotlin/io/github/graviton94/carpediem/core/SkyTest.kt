package io.github.graviton94.carpediem.core

import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d)
private val SEOUL = ZoneId.of("Asia/Seoul")

class SkyTest {
    private val places = PlaceTable(File(System.getProperty("dataDir"), "places.csv").readText())

    @Test fun termsOfKnownDays() {
        assertEquals(SolarTerm.IPCHUN, Sky.termOn(day(2026, 2, 4), SEOUL))
        assertEquals(SolarTerm.CHUNBUN, Sky.termOn(day(2026, 3, 20), SEOUL))
        assertEquals(SolarTerm.HAJI, Sky.termOn(day(2026, 6, 21), SEOUL))
        assertEquals(SolarTerm.CHUBUN, Sky.termOn(day(2026, 9, 23), SEOUL))
        assertEquals(SolarTerm.SANGGANG, Sky.termOn(day(2026, 10, 23), SEOUL))
        assertEquals(SolarTerm.DONGJI, Sky.termOn(day(2026, 12, 22), SEOUL))
        assertNull(Sky.termOn(day(2026, 6, 20), SEOUL))
        assertEquals(SolarTerm.HAJI, Sky.termOf(day(2026, 7, 1), SEOUL))
    }

    @Test fun everyTermOncePerYear() {
        val y = 2027
        val found = (0 until LocalDate.of(y, 1, 1).lengthOfYear()).mapNotNull { Sky.termOn(LocalDate.of(y, 1, 1).plusDays(it.toLong()), SEOUL) }
        assertEquals(SolarTerm.entries.toList(), found)
    }

    @Test fun sunTimesSeoul() {
        val p = places.of("KR", SEOUL, Instant.EPOCH)
        val s = assertNotNull(Sky.sun(day(2026, 6, 21), p, SEOUL))
        assertTrue(abs(s.rise.hour * 60 + s.rise.minute - (5 * 60 + 11)) <= 6, "${s.rise}")
        assertTrue(abs(s.set.hour * 60 + s.set.minute - (19 * 60 + 57)) <= 6, "${s.set}")
        val w = assertNotNull(Sky.sun(day(2026, 12, 22), p, SEOUL))
        assertTrue(abs(w.rise.hour * 60 + w.rise.minute - (7 * 60 + 43)) <= 6, "${w.rise}")
        assertTrue(abs(w.set.hour * 60 + w.set.minute - (17 * 60 + 17)) <= 6, "${w.set}")
        // 12월 오후 5시 반은 밤
        assertTrue(!Sky.isDay(day(2026, 12, 22).atTime(17, 30).atZone(SEOUL), p))
        assertTrue(Sky.isDay(day(2026, 6, 21).atTime(19, 30).atZone(SEOUL), p))
    }

    @Test fun polarDayAndNight() {
        val north = Place("XX", 78.0, 15.0)
        val z = ZoneId.of("Europe/Oslo")
        assertNull(Sky.sun(day(2026, 6, 21), north, z))
        assertTrue(Sky.isDay(day(2026, 6, 21).atTime(1, 0).atZone(z), north))
        assertTrue(!Sky.isDay(day(2026, 12, 21).atTime(12, 0).atZone(z), north))
    }

    @Test fun moon() {
        val full = Sky.moonPhase(Instant.parse("2026-01-03T10:03:00Z"))
        assertTrue(abs(full - 0.5) < 0.04, "$full")
        assertEquals(Sky.MoonShape.FULL, Sky.moonShape(full))
        val new = Sky.moonPhase(Instant.parse("2026-01-18T19:52:00Z"))
        assertTrue(new < 0.04 || new > 0.96, "$new")
        assertTrue(Sky.moonLit(new) < 0.02)
    }

    @Test fun placesCoverEveryCountry() {
        val life = LifeExpectancyTable(File(System.getProperty("dataDir"), "life-expectancy.csv").readText())
        assertEquals(emptyList(), life.countries.map { it.code }.filter { it !in places.places })
        // 표에 없으면 시간대로 짐작
        assertEquals(135.0, places.of("??", ZoneId.of("Asia/Tokyo"), Instant.EPOCH).lon)
        // 세계 평균 · 넓은 나라는 폰의 시간대를 따름
        assertEquals(135.0, places.of("WLD", ZoneId.of("Asia/Seoul"), Instant.EPOCH).lon)
        assertEquals(-120.0, places.of("US", ZoneId.of("America/Los_Angeles"), Instant.EPOCH).lon)
        assertEquals(-74.01, places.of("US", ZoneId.of("America/New_York"), Instant.EPOCH).lon)
        assertEquals(126.98, places.of("KR", ZoneId.of("Asia/Seoul"), Instant.EPOCH).lon)
    }
}

class RhythmTest {
    private fun me(until: LocalDate?) = Person("a", "보리", Kind.PET, Species.DOG, seed = 1, metOn = day(2020, 1, 1), until = until)

    @Test fun nudges() {
        assertTrue(Nudges.quiet(day(2026, 10, 1), day(2026, 10, 4)))
        assertTrue(!Nudges.quiet(day(2026, 10, 2), day(2026, 10, 4)))
        assertTrue(!Nudges.quiet(null, day(2026, 10, 4)))
        val t = day(2026, 10, 4)
        assertTrue(Nudges.eveningRests(t, listOf(DayLine(t, "a", null)), emptyList()))
        assertTrue(Nudges.eveningRests(t, emptyList(), listOf(t to BreathKind.SLEEP)))
        assertTrue(!Nudges.eveningRests(t, listOf(DayLine(t.minusDays(1), "a", null)), emptyList()))
        assertTrue(Nudges.pick(t, 5) != Nudges.pick(t.plusDays(1), 5))
        assertTrue((0..40).all { Nudges.pick(t.plusDays(it.toLong()), 5, 3) in 0 until 5 })
    }

    @Test fun memoryWeek() {
        val p = me(day(2024, 10, 20))
        assertEquals(day(2026, 10, 20), MemoryWeek.of(p, day(2026, 10, 17)))
        assertEquals(day(2026, 10, 20), MemoryWeek.of(p, day(2026, 10, 23)))
        assertNull(MemoryWeek.of(p, day(2026, 10, 24)))
        assertNull(MemoryWeek.of(p, day(2024, 10, 21)))   // 떠난 해 그 주는 아님
        assertTrue(MemoryWeek.startsToday(p, day(2026, 10, 17)))
        assertNull(MemoryWeek.of(me(null), day(2026, 10, 20)))
        // 해를 넘는 주
        assertEquals(day(2026, 1, 1), MemoryWeek.of(me(day(2023, 1, 1)), day(2025, 12, 30)))
    }

    @Test fun breathTrace() {
        val t = day(2026, 10, 4)
        assertEquals(setOf(BreathKind.BOX, BreathKind.SLEEP), BreathTrace.today(listOf(t to BreathKind.BOX, t.minusDays(1) to BreathKind.CALM, t to BreathKind.SLEEP), t))
    }

    @Test fun offerings() {
        val now = SeasonCard(2026, Season.AUTUMN, Tree.ZELKOVA)
        val o = Offering("a", now, day(2026, 10, 4))
        val old = Offering("b", SeasonCard(2026, Season.SUMMER, Tree.ZELKOVA), day(2026, 7, 1))
        val list = Offerings.put(listOf(old), o)
        assertEquals(listOf(o), list)
        assertEquals(list, Offerings.put(list, o.copy(date = day(2026, 10, 5))))
        assertTrue(!Offerings.canOffer(list, "a", now.copy(tree = Tree.GINKGO)))
        assertEquals(list, Offerings.decode(Offerings.encode(list)))
        assertEquals(setOf("a"), Offerings.shown(list, now).keys)
        assertTrue(Offerings.shown(list, SeasonCard(2026, Season.WINTER, Tree.ZELKOVA)).isEmpty())
    }

    @Test fun yearCard() {
        assertEquals(2026, YearCard.due(day(2026, 12, 25)))
        assertNull(YearCard.due(day(2026, 12, 24)))
    }
}

class LangsTest {
    @Test fun phoneLanguage() {
        assertEquals("ko", Langs.of("ko", "KR"))
        assertEquals("ja", Langs.of("ja", "JP"))
        assertEquals("zh-TW", Langs.of("zh", "TW"))
        assertEquals("zh-TW", Langs.of("zh", "HK"))
        assertEquals("zh-TW", Langs.of("zh", "", "Hant"))
        assertEquals("en", Langs.of("zh", "CN"))
        assertEquals("en", Langs.of("de", "DE"))
    }

    @Test fun pickWithFallback() {
        val csv = "No,한글,영문,日本語,繁體中文\n1,가,A,あ,\n"
        val q = QuoteBook(csv).quotes.single()
        assertEquals("가", q.text("ko")); assertEquals("あ", q.text("ja")); assertEquals("A", q.text("zh-TW")); assertEquals("A", q.text("en"))
    }
}
