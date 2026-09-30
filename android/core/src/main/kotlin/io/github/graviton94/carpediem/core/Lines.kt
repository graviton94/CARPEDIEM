package io.github.graviton94.carpediem.core

import java.time.LocalDate

/** 오늘의 한 줄에 실어 보내는 마음 (고르지 않아도 된다). */
enum class Feeling { JOY, HOPE, CALM, THANKS, DISAPPOINT, SAD, WORRY }

/** 하루에 한 줄. 떠나보낸 뒤에는 화면에 다시 보이지 않고, 기기 안에만 남는다. text 가 비었으면 ‘기록 남기지 않기’로 날짜만 남긴 것. */
data class DayLine(val date: LocalDate, val text: String, val feeling: Feeling?, val to: String? = null, val question: Int? = null)

object Lines {
    /** 한 줄로 다듬기: 줄바꿈 · 탭을 빈칸으로, 앞뒤 빈칸 없이, 최대 max 글자. */
    fun clean(text: String, max: Int): String =
        text.replace(Regex("[\\t\\r\\n]+"), " ").trim().let { if (it.codePointCount(0, it.length) <= max) it else it.substring(0, it.offsetByCodePoints(0, max)) }

    /** 저장 형식: 한 줄에 하나, `epochDay<TAB>FEELING(없으면 -)<TAB>글[<TAB>받는 돌 id(없으면 빈칸)[<TAB>질문 번호]]`. 예전 기록(칸 3 · 4개)도 그대로 읽힌다. */
    fun encode(list: List<DayLine>): String =
        list.joinToString("\n") {
            "${it.date.toEpochDay()}\t${it.feeling?.name ?: "-"}\t${clean(it.text, Int.MAX_VALUE)}" +
                (if (it.to != null || it.question != null) "\t${it.to.orEmpty()}" else "") + (it.question?.let { q -> "\t$q" } ?: "")
        }

    fun decode(s: String?): List<DayLine> =
        s.orEmpty().lineSequence().mapNotNull { row ->
            val p = row.split('\t', limit = 5)
            if (p.size < 3) return@mapNotNull null
            val day = p[0].toLongOrNull() ?: return@mapNotNull null
            DayLine(LocalDate.ofEpochDay(day), p[2], Feeling.entries.firstOrNull { it.name == p[1] }, p.getOrNull(3)?.takeIf { it.isNotBlank() }, p.getOrNull(4)?.toIntOrNull())
        }.toList()

    /** 이어 쓰기 흔적: 7 · 30 · 100일 (정원에 바람개비 · 종이배 · 연). */
    val STREAKS = listOf(7, 30, 100)

    /** 보낸 날들에서 n일을 처음으로 이어 쓴 날 (없으면 null). 빠진 날이 있어도 벌은 없고 다시 세기 시작할 뿐. */
    fun streakReached(dates: List<LocalDate>, n: Int): LocalDate? {
        var run = 0; var prev: LocalDate? = null
        for (d in dates.distinct().sorted()) {
            run = if (prev != null && d == prev.plusDays(1)) run + 1 else 1
            if (run >= n) return d
            prev = d
        }
        return null
    }

    /** 이미 얻은 흔적은 기록을 지워도 남는다: 예전 것과 새로 센 것 가운데 이른 날. */
    fun streaks(list: List<DayLine>, kept: Map<Int, LocalDate>): Map<Int, LocalDate> =
        STREAKS.mapNotNull { n -> listOfNotNull(kept[n], streakReached(list.map { it.date }, n)).minOrNull()?.let { n to it } }.toMap()

    /**
     * 몇 해 전 오늘 보낸 한 줄 (가까운 해부터). 글 없이 날짜만 남긴 날은 빼고,
     * 2월 29일에 보낸 줄은 평년엔 2월 28일에 돌아온다.
     */
    fun yearsAgo(list: List<DayLine>, today: LocalDate): List<Pair<Int, DayLine>> =
        list.filter { it.text.isNotBlank() && it.date.year < today.year }.mapNotNull { l ->
            val md = if (l.date.monthValue == 2 && l.date.dayOfMonth == 29 && !today.isLeapYear) java.time.MonthDay.of(2, 28) else java.time.MonthDay.from(l.date)
            if (md == java.time.MonthDay.from(today)) (today.year - l.date.year) to l else null
        }.sortedBy { it.first }

    /**
     * 문득 찾아오는 한 줄: minAge 일보다 오래된 줄 가운데 하나를 고른다 (글 없는 날, ‘몇 해 전 오늘’과 겹치는 날은 빼고).
     * 같은 seed · 같은 날이면 같은 줄.
     */
    fun randomPick(list: List<DayLine>, today: LocalDate, seed: Long, minAge: Int): DayLine? {
        val md = java.time.MonthDay.from(today)
        val pool = list.filter { it.text.isNotBlank() && !it.date.isAfter(today.minusDays(minAge.toLong())) && java.time.MonthDay.from(it.date) != md }
        if (pool.isEmpty()) return null
        return pool[kotlin.random.Random(seed xor today.toEpochDay()).nextInt(pool.size)]
    }

    /** 다음에 문득 찾아올 날: 오늘부터 min ~ max 일 뒤 가운데 하나. */
    fun nextRandomDay(today: LocalDate, seed: Long, min: Int, max: Int): LocalDate =
        today.plusDays(kotlin.random.Random(seed * 31 + today.toEpochDay()).nextInt(min, max + 1).toLong())

    /** 내보내기용 글 (한 줄에 하나: 날짜 · 마음 · 글). 마음 이름은 부르는 쪽이 정한다. */
    fun export(list: List<DayLine>, feelingName: (Feeling) -> String): String =
        list.filter { it.text.isNotBlank() }.joinToString("\n") { l -> listOfNotNull(l.date.toString(), l.feeling?.let(feelingName), l.text).joinToString(" · ") }

    /** 마음의 하늘: 오늘까지 n일 (오래된 날부터), 날마다 보낸 한 줄 또는 null (쉰 날). */
    fun lastDays(list: List<DayLine>, today: LocalDate, n: Int): List<Pair<LocalDate, DayLine?>> {
        val byDay = list.associateBy { it.date }
        return (n - 1 downTo 0).map { k -> today.minusDays(k.toLong()).let { d -> d to byDay[d] } }
    }

    /** 같은 날에 이미 보냈으면 그대로 (하루에 한 줄). 날짜순. */
    fun add(list: List<DayLine>, line: DayLine): List<DayLine> =
        if (list.any { it.date == line.date }) list else (list + line).sortedBy { it.date }
}
