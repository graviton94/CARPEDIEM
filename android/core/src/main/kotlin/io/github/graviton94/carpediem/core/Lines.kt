package io.github.graviton94.carpediem.core

import java.time.LocalDate

/** 오늘의 한 줄에 실어 보내는 마음 (고르지 않아도 된다). */
enum class Feeling { JOY, THANKS, CALM, SAD, WORRY, ANGRY }

/** 하루에 한 줄. 떠나보낸 뒤에는 화면에 다시 보이지 않고, 기기 안에만 남는다. */
data class DayLine(val date: LocalDate, val text: String, val feeling: Feeling?)

object Lines {
    /** 한 줄로 다듬기: 줄바꿈 · 탭을 빈칸으로, 앞뒤 빈칸 없이, 최대 max 글자. */
    fun clean(text: String, max: Int): String =
        text.replace(Regex("[\\t\\r\\n]+"), " ").trim().let { if (it.codePointCount(0, it.length) <= max) it else it.substring(0, it.offsetByCodePoints(0, max)) }

    /** 저장 형식: 한 줄에 하나, `epochDay<TAB>FEELING(없으면 -)<TAB>글`. */
    fun encode(list: List<DayLine>): String =
        list.joinToString("\n") { "${it.date.toEpochDay()}\t${it.feeling?.name ?: "-"}\t${clean(it.text, Int.MAX_VALUE)}" }

    fun decode(s: String?): List<DayLine> =
        s.orEmpty().lineSequence().mapNotNull { row ->
            val p = row.split('\t', limit = 3)
            if (p.size < 3) return@mapNotNull null
            val day = p[0].toLongOrNull() ?: return@mapNotNull null
            DayLine(LocalDate.ofEpochDay(day), p[2], Feeling.entries.firstOrNull { it.name == p[1] })
        }.toList()

    /** 같은 날에 이미 보냈으면 그대로 (하루에 한 줄). 날짜순. */
    fun add(list: List<DayLine>, line: DayLine): List<DayLine> =
        if (list.any { it.date == line.date }) list else (list + line).sortedBy { it.date }
}
