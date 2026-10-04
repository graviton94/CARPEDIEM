package io.github.graviton94.carpediem.core

import java.time.LocalDate

/** 오늘의 한 줄에 실어 보내는 마음 (고르지 않아도 된다). */
enum class Feeling { JOY, HOPE, CALM, THANKS, DISAPPOINT, SAD, WORRY }

/** 하루에 한 줄. 떠나보낸 뒤에는 화면에 다시 보이지 않고, 기기 안에만 남는다. text 가 비었으면 ‘기록 남기지 않기’로 날짜만 남긴 것. */
data class DayLine(val date: LocalDate, val text: String, val feeling: Feeling?, val to: String? = null, val question: Int? = null)

object Lines {
    /** 오늘의 한 줄은 이름은 ‘한 줄’이지만 이만큼 줄을 나눠 쓸 수 있다 (빈 줄 포함). */
    const val MAX_LINES = 5
    /** 저장할 때 글 안의 줄바꿈 자리 (기록 하나가 한 줄이라 줄바꿈 대신 U+2028 줄 구분 문자로 둔다). */
    private const val LS = '\u2028'

    /**
     * 다듬기: 앞뒤 빈칸 없이, 최대 max 글자. lines = 1 이면 줄바꿈 · 탭을 빈칸으로 (한 줄).
     * lines > 1 이면 줄바꿈은 남기되 탭은 빈칸, 줄 끝 빈칸은 지우고, 빈 줄이 여럿 이어지면 하나로, 최대 lines 줄.
     */
    fun clean(text: String, max: Int, lines: Int = 1): String {
        val t = if (lines <= 1) text.replace(Regex("[\\t\\r\\n$LS]+"), " ").trim()
        else text.replace("\r\n", "\n").replace('\r', '\n').replace(LS, '\n').replace('\t', ' ')
            .split('\n').joinToString("\n") { it.trimEnd() }.replace(Regex("\n{3,}"), "\n\n").trim()
            .split('\n').take(lines).joinToString("\n").trimEnd()
        return if (t.codePointCount(0, t.length) <= max) t else t.substring(0, t.offsetByCodePoints(0, max)).trimEnd()
    }

    /**
     * 저장 형식: 한 줄에 하나, `epochDay<TAB>FEELING(없으면 -)<TAB>글[<TAB>받는 돌 id(없으면 빈칸)[<TAB>질문 번호]]`. 예전 기록(칸 3 · 4개)도 그대로 읽힌다.
     * 글 안의 줄바꿈은 U+2028 로 바꿔 둔다 (예전 기록에는 줄바꿈이 없어 그대로 읽힘).
     */
    fun encode(list: List<DayLine>): String =
        list.joinToString("\n") {
            "${it.date.toEpochDay()}\t${it.feeling?.name ?: "-"}\t${clean(it.text, Int.MAX_VALUE, Int.MAX_VALUE).replace('\n', LS)}" +
                (if (it.to != null || it.question != null) "\t${it.to.orEmpty()}" else "") + (it.question?.let { q -> "\t$q" } ?: "")
        }

    fun decode(s: String?): List<DayLine> =
        s.orEmpty().lineSequence().mapNotNull { row ->
            val p = row.split('\t', limit = 5)
            if (p.size < 3) return@mapNotNull null
            val day = p[0].toLongOrNull() ?: return@mapNotNull null
            DayLine(LocalDate.ofEpochDay(day), p[2].replace(LS, '\n'), Feeling.entries.firstOrNull { it.name == p[1] }, p.getOrNull(3)?.takeIf { it.isNotBlank() }, p.getOrNull(4)?.toIntOrNull())
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
        list.filter { it.text.isNotBlank() }.joinToString("\n") { l -> listOfNotNull(l.date.toString(), l.feeling?.let(feelingName), l.text.replace("\n", " / ")).joinToString(" · ") }

    /** 마음의 하늘: 오늘까지 n일 (오래된 날부터), 날마다 보낸 한 줄 또는 null (쉰 날). */
    fun lastDays(list: List<DayLine>, today: LocalDate, n: Int): List<Pair<LocalDate, DayLine?>> {
        val byDay = list.associateBy { it.date }
        return (n - 1 downTo 0).map { k -> today.minusDays(k.toLong()).let { d -> d to byDay[d] } }
    }

    /** 한 해의 정원: 그해의 모든 날 (1월 1일부터), 날마다 보낸 한 줄 또는 null. */
    fun yearDays(list: List<DayLine>, year: Int): List<Pair<LocalDate, DayLine?>> {
        val byDay = list.associateBy { it.date }
        val first = LocalDate.of(year, 1, 1)
        return (0 until first.lengthOfYear()).map { k -> first.plusDays(k.toLong()).let { d -> d to byDay[d] } }
    }

    /** 한 해의 정원이 피는 때: 12월 31일 ~ 다음 해 1월 7일. 그 해 (없으면 null). */
    fun yearDue(today: LocalDate): Int? = when {
        today.monthValue == 12 && today.dayOfMonth == 31 -> today.year
        today.monthValue == 1 && today.dayOfMonth <= 7 -> today.year - 1
        else -> null
    }

    /** 계절 첫날의 바람: 3 · 6 · 9 · 12월 1 ~ 14일 (그 계절 이름의 편지 id, 예 "2026-12"). */
    fun wishDue(today: LocalDate): String? =
        if (today.monthValue in listOf(3, 6, 9, 12) && today.dayOfMonth <= 14) "%04d-%02d".format(today.year, today.monthValue) else null

    /** 마음의 날씨: 그 달에 가장 많이 고른 마음 (같으면 나중에 고른 것). 고른 마음이 없으면 null. */
    fun monthMood(list: List<DayLine>, year: Int, month: Int): Feeling? =
        list.filter { it.date.year == year && it.date.monthValue == month && it.feeling != null }
            .groupBy { it.feeling!! }.maxWithOrNull(compareBy<Map.Entry<Feeling, List<DayLine>>> { it.value.size }.thenBy { e -> e.value.maxOf { it.date } })?.key

    /** 기록 찾기: 글 · 마음 이름 · 받는 사람 이름에 낱말이 든 줄 (최근 것부터, 오늘 것은 빼고). */
    fun search(list: List<DayLine>, query: String, today: LocalDate, feelingName: (Feeling) -> String, personName: (String) -> String?): List<DayLine> {
        val q = query.trim(); if (q.isEmpty()) return emptyList()
        return list.filter { l ->
            l.date != today && (l.text.contains(q, ignoreCase = true) || l.feeling?.let { feelingName(it).contains(q, ignoreCase = true) } == true ||
                l.to?.let(personName)?.contains(q, ignoreCase = true) == true)
        }.sortedByDescending { it.date }
    }

    /** 그날의 한 줄을 고침: 글 · 마음만 바꾸고 받는 돌 · 질문은 그대로. 그날 줄이 없으면 그대로. */
    fun edit(list: List<DayLine>, day: LocalDate, text: String, feeling: Feeling?): List<DayLine> =
        list.map { if (it.date == day) it.copy(text = text, feeling = feeling) else it }

    /** 그날의 한 줄을 지움 (그날은 다시 쓸 수 있는 빈 날이 됨). */
    fun remove(list: List<DayLine>, day: LocalDate): List<DayLine> = list.filterNot { it.date == day }

    /** 같은 날에 이미 보냈으면 그대로 (하루에 한 줄). 날짜순. */
    fun add(list: List<DayLine>, line: DayLine): List<DayLine> =
        if (list.any { it.date == line.date }) list else (list + line).sortedBy { it.date }
}

/** 특별한 날 꽃: 인생 달력의 한 칸에 놓는 작은 꽃 (이름만). 해마다 그 전날 저녁에 한 번 알림. */
data class SpecialDay(val date: LocalDate, val name: String)

object SpecialDays {
    const val MAX = 30
    const val NAME_MAX = 12
    fun encode(list: List<SpecialDay>): String = list.joinToString("\n") { "${it.date.toEpochDay()}\t${Lines.clean(it.name, NAME_MAX)}" }
    fun decode(s: String?): List<SpecialDay> = s.orEmpty().lineSequence().mapNotNull { r ->
        val p = r.split('\t', limit = 2); val d = p.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        SpecialDay(LocalDate.ofEpochDay(d), p.getOrNull(1).orEmpty())
    }.toList()
    /** day 가 지난 해들의 특별한 날과 같은 날짜이면 (그 날, 몇 년 전). 2월 29일은 평년엔 2월 28일. */
    fun anniversaries(list: List<SpecialDay>, day: LocalDate): List<Pair<SpecialDay, Int>> =
        list.filter { it.date.year < day.year && Family.birthdayIn(it.date, day.year) == day }.map { it to day.year - it.date.year }

    /** 같은 날은 하나만 (이름을 바꿈), 날짜순, 최대 MAX. */
    /** 같은 날에도 여럿 (같은 날 · 같은 이름은 한 번만). 날짜순, 최대 MAX. */
    fun put(list: List<SpecialDay>, day: SpecialDay): List<SpecialDay> = (list.filterNot { it == day } + day).sortedBy { it.date }.takeLast(MAX)
}

/**
 * 첫 일주일 길잡이: 둘러보기 다음 날부터 하루에 하나씩, 정원 아래 조용한 권유 한 줄.
 * 이미 해 본 것 (done) 은 건너뛰고, 놓친 것은 다음 날로 넘어감. 둘째 주가 끝나면 더 권하지 않음. 점수 · 체크 표시는 없음.
 */
object FirstWeek {
    /** 권하는 차례: 키와 처음 권하는 날 (만난 날 = 0). */
    val STEPS = listOf("breath" to 1, "stone" to 2, "gaze" to 3, "special" to 4, "widget" to 5, "backup" to 6)
    const val LAST_DAY = 13

    fun next(daysSinceMet: Long, done: Set<String>): String? =
        if (daysSinceMet !in 1..LAST_DAY) null else STEPS.firstOrNull { (k, d) -> d <= daysSinceMet && k !in done }?.first
}
