package io.github.graviton94.carpediem.core

import java.util.Locale

/** 따옴표 칸과 칸 안의 쉼표를 지원하는 작은 CSV 읽기. `#`으로 시작하는 줄은 주석. */
object Csv {
    fun rows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        val chars = text.replace("\r\n", "\n") + "\n"
        var i = 0
        while (i < chars.length) {
            val c = chars[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < chars.length && chars[i + 1] == '"') { field.append('"'); i++ } else quoted = false
                } else field.append(c)
            } else when (c) {
                '"' -> quoted = true
                ',' -> { row.add(field.toString()); field.clear() }
                '\n' -> {
                    row.add(field.toString()); field.clear()
                    val comment = row.firstOrNull()?.startsWith("#") == true
                    val empty = row.size == 1 && row[0].isEmpty()
                    if (!comment && !empty) rows.add(row)
                    row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        return rows
    }

    fun records(text: String): List<Map<String, String>> {
        val all = rows(text)
        val header = all.firstOrNull() ?: return emptyList()
        return all.drop(1).map { values -> header.mapIndexed { i, h -> h to values.getOrElse(i) { "" } }.toMap() }
    }
}

data class CountryLife(val code: String, val sourceName: String, val total: Double, val male: Double, val female: Double) {
    fun expectancy(sex: Sex) = when (sex) {
        Sex.OTHER -> total
        Sex.MALE -> male
        Sex.FEMALE -> female
    }

    /** 표시 언어로 된 나라 이름 (세계 평균은 호출하는 쪽에서 문구로 바꾼다). */
    fun displayName(locale: Locale): String =
        Locale.Builder().setRegion(code).build().getDisplayCountry(locale).ifBlank { sourceName }
}

/** 나라별 0세 기대수명 (data/life-expectancy.csv). */
class LifeExpectancyTable(csv: String) {
    val countries: List<CountryLife> = Csv.records(csv).mapNotNull { r ->
        val code = r["code"] ?: return@mapNotNull null
        val t = r["total"]?.toDoubleOrNull() ?: return@mapNotNull null
        val m = r["male"]?.toDoubleOrNull() ?: return@mapNotNull null
        val f = r["female"]?.toDoubleOrNull() ?: return@mapNotNull null
        CountryLife(code, r["en"] ?: code, t, m, f)
    }

    fun country(code: String) = countries.firstOrNull { it.code == code }

    /** 표에 없는 나라는 세계 평균. */
    fun expectancy(code: String, sex: Sex): Double = (country(code) ?: country(WORLD))?.expectancy(sex) ?: 73.0

    fun defaultCountry(region: String?): String = if (region != null && country(region) != null) region else WORLD

    companion object { const val WORLD = "WLD" }
}

data class Quote(val number: Int, val korean: String, val english: String, val more: Map<String, String> = emptyMap()) {
    /** 그 말의 문장 (없으면 영어). */
    fun text(lang: String) = Langs.pick(lang, korean, english, more)
}

/**
 * 오늘의 문장 고르기 (iOS QuoteBook 과 같은 규칙).
 * - 하루에 하나, 자정에 바뀐다.
 * - seed 로 섞은 순서를 따라가므로 한 바퀴(명언 수만큼의 날) 동안 겹치지 않는다.
 * - 바퀴가 바뀔 때 새 순서의 첫 문장이 바로 전날 문장과 같지 않게 한다.
 */
class QuoteBook(csv: String) {
    val quotes: List<Quote> = Csv.records(csv).mapNotNull { r ->
        val n = r["No"]?.toIntOrNull() ?: return@mapNotNull null
        Quote(n, r["한글"] ?: return@mapNotNull null, r["영문"] ?: return@mapNotNull null, Langs.extra(r))
    }

    fun index(day: Int, seed: ULong): Int {
        val n = quotes.size
        if (n == 0) return 0
        val d = day.coerceAtLeast(0)
        return order(d / n, seed)[d % n]
    }

    fun quote(day: Int, seed: ULong, offset: Int = 0): Quote? = if (quotes.isEmpty()) null else quotes[index(day + offset, seed)]

    fun order(cycle: Int, seed: ULong): IntArray {
        val result = shuffled(seed + cycle.toULong() * 0x9E3779B97F4A7C15uL)
        if (cycle > 0 && result.size > 1 && result[0] == order(cycle - 1, seed).last()) {
            val t = result[0]; result[0] = result[1]; result[1] = t
        }
        return result
    }

    private fun shuffled(seed: ULong): IntArray {
        val rng = SplitMix64(seed)
        val a = IntArray(quotes.size) { it }
        for (i in a.size - 1 downTo 1) {
            val j = (rng.next() % (i + 1).toULong()).toInt()
            val t = a[i]; a[i] = a[j]; a[j] = t
        }
        return a
    }
}

class SplitMix64(private var state: ULong) {
    fun next(): ULong {
        state += 0x9E3779B97F4A7C15uL
        var z = state
        z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
        z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
        return z xor (z shr 31)
    }
}
