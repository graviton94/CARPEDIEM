package io.github.graviton94.carpediem.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 오늘의 질문 하나 (data/questions.csv). group = scene · mood · joy · now. */
data class Question(val id: Int, val group: String, val korean: String, val english: String)

/**
 * 일주일에 두 번, 문장 자리에 가벼운 질문. 요일은 설치마다 정해진 두 요일 (매주 같음, 3 · 4일 간격).
 * 질문은 갈래가 번갈아 오고, 60개를 다 쓰기 전에는 같은 질문이 다시 오지 않는다. 같은 seed · 같은 날이면 같은 질문.
 */
class QuestionBook(csv: String) {
    val all: List<Question> = Csv.records(csv).mapNotNull { r ->
        val id = r["No"]?.toIntOrNull() ?: return@mapNotNull null
        Question(id, r["갈래"].orEmpty(), r["한글"].orEmpty(), r["영문"].orEmpty())
    }

    fun byId(id: Int): Question? = all.firstOrNull { it.id == id }

    /** 이 설치의 질문 순서: 갈래마다 섞은 뒤 한 갈래씩 번갈아. */
    fun order(seed: Long): List<Question> {
        val groups = all.groupBy { it.group }.values.map { it.shuffled(kotlin.random.Random(seed * 7 + it.first().id)) }
        val n = groups.maxOfOrNull { it.size } ?: 0
        return (0 until n).flatMap { k -> groups.mapNotNull { it.getOrNull(k) } }
    }

    /** 오늘이 질문 날이면 그 질문, 아니면 null. */
    fun of(seed: Long, date: LocalDate): Question? {
        val n = Questions.count(seed, date) ?: return null
        val list = order(seed)
        return if (list.isEmpty()) null else list[(n % list.size).toInt()]
    }
}

object Questions {
    /** 이 설치의 두 요일 (월 = 1 … 일 = 7). 사이가 3일이거나 4일. */
    fun days(seed: Long): Pair<DayOfWeek, DayOfWeek> {
        val a = Math.floorMod(seed, 7L).toInt()
        val gap = 3 + Math.floorMod(seed / 7, 2L).toInt()
        val b = (a + gap) % 7
        return DayOfWeek.of(minOf(a, b) + 1) to DayOfWeek.of(maxOf(a, b) + 1)
    }

    fun isDay(seed: Long, date: LocalDate): Boolean = days(seed).let { date.dayOfWeek == it.first || date.dayOfWeek == it.second }

    /** 몇 번째 질문 날인지 (1970-01-05 월요일부터 센다). 질문 날이 아니면 null. */
    fun count(seed: Long, date: LocalDate): Long? {
        if (!isDay(seed, date)) return null
        val week = ChronoUnit.WEEKS.between(LocalDate.of(1970, 1, 5), date.with(DayOfWeek.MONDAY))
        return week * 2 + if (date.dayOfWeek == days(seed).first) 0 else 1
    }
}
