package io.github.graviton94.carpediem.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 나의 나무: 인생의 계절이 종류를 정한다 (봄 벚 · 여름 느티 · 가을 은행 · 겨울 소나무). */
enum class Tree(val key: String) { CHERRY("cherry"), ZELKOVA("zelkova"), GINKGO("ginkgo"), PINE("pine");
    companion object { fun of(life: Season) = entries[life.ordinal] }
}

/** 말뚝에 걸린 것 (숨 쉰 날이 쌓일수록 하나씩). */
enum class Hang { NONE, CHIME, BELL, LANTERN }

/** 계절 한 장: 그해 · 실제 계절 · 그때의 나무. 앨범에 해마다 네 장. */
data class SeasonCard(val year: Int, val season: Season, val tree: Tree) {
    val key: String get() = "${tree.key}_${season.name.lowercase()}"
    /** 저장용 (`2026-AUTUMN-ginkgo`). */
    val id: String get() = "$year-${season.name}-${tree.key}"
    companion object {
        fun parse(id: String): SeasonCard? = id.split('-').takeIf { it.size == 3 }?.let { p ->
            val y = p[0].toIntOrNull() ?: return null
            val s = runCatching { Season.valueOf(p[1]) }.getOrNull() ?: return null
            val t = Tree.entries.firstOrNull { it.key == p[2] } ?: return null
            SeasonCard(y, s, t)
        }
    }
}

/**
 * 정원의 자리 여섯에 지금 무엇이 있는지 (design/tokens.json garden.decor, docs/plan.md 꾸밈 새 판).
 * 모두 쌓인 수만 센다: 이어 쓰기 · 마감 · 놓침이 없어, 쉬어도 잃는 것이 없다.
 *
 * tree · stage   ① 나무: 종류 = 인생의 계절, 자람 = 함께한 날 (0 새싹 · 1 어린 나무 · 2 나무 · 3 큰 나무와 그네)
 * prevTree       인생의 계절이 바뀐 뒤 changeDays 동안은 옛 나무가 옅게 남아 천천히 바뀜 (blend 0 → 1)
 * hang · letter  ② 말뚝: 숨 쉰 날 1 · 30 · 100 에 풍경 · 작은 종 · 등불, 계절의 편지가 오면 봉투
 * ribbons        ③ 연: 한 줄 kiteLines 줄에 뜨고, 그 뒤로 ribbonLines 줄마다 리본 하나 (그 줄들에서 가장 많았던 마음, 많아야 ribbonMax)
 * buds           ④ 하루의 자리: 돌멍하기 budGazes 번마다 이끼에 봉오리 하나 (많아야 budMax)
 * card           ⑤ 나무 발치: 이번 계절의 한 장 (그 계절에 정원을 열면 받음)
 */
data class Decor(
    val season: Season, val tree: Tree, val stage: Int, val prevTree: Tree?, val blend: Float,
    val hang: Hang, val letter: Boolean, val kite: Boolean, val ribbons: List<Feeling?>, val buds: Int, val card: SeasonCard,
)

object GardenDecor {
    /** 실제 계절이 남반구와 뒤집히는 나라 (설정의 나라). */
    val SOUTH = setOf("AU", "NZ", "AR", "CL", "UY", "PY", "BO", "PE", "BR", "ZA", "NA", "BW", "ZW", "MZ", "MG", "LS", "SZ", "FJ")

    /** 실제 계절 (기상 계절: 3 · 6 · 9 · 12월 1일에 바뀜). */
    fun realSeason(date: LocalDate, country: String?): Season {
        val north = when (date.monthValue) { 3, 4, 5 -> Season.SPRING; 6, 7, 8 -> Season.SUMMER; 9, 10, 11 -> Season.AUTUMN; else -> Season.WINTER }
        return if (country?.uppercase() in SOUTH) Season.entries[(north.ordinal + 2) % 4] else north
    }

    /** 앨범의 해 줄 = 그 계절이 시작한 해 (12월에 시작한 계절의 1 · 2월은 앞 해 줄에). */
    fun albumYear(date: LocalDate): Int = if (date.monthValue <= 2) date.year - 1 else date.year

    class Rules(
        val stageDays: List<Int> = listOf(100, 365, 1095),
        val hangBreaths: List<Int> = listOf(1, 30, 100),
        val kiteLines: Int = 30, val ribbonLines: Int = 30, val ribbonMax: Int = 8,
        val budGazes: Int = 10, val budMax: Int = 5, val changeDays: Int = 7,
    )

    fun stage(start: LocalDate, today: LocalDate, days: List<Int>): Int {
        val d = ChronoUnit.DAYS.between(start, today)
        return days.count { d >= it }
    }

    fun hang(breathDays: Int, levels: List<Int>): Hang = when {
        breathDays >= levels[2] -> Hang.LANTERN
        breathDays >= levels[1] -> Hang.BELL
        breathDays >= levels[0] -> Hang.CHIME
        else -> Hang.NONE
    }

    /** 리본: 글을 남긴 한 줄을 날짜 순으로 ribbonLines 줄씩 묶어, 묶음마다 가장 많았던 마음 (같으면 Feeling 순서가 앞인 마음, 마음을 안 고른 묶음은 null). 최근 ribbonMax 개. */
    fun ribbons(lines: List<DayLine>, r: Rules): List<Feeling?> {
        val written = lines.filter { it.text.isNotBlank() }.sortedBy { it.date }
        if (written.size < r.kiteLines) return emptyList()
        return written.chunked(r.ribbonLines).filter { it.size == r.ribbonLines }.map { chunk ->
            chunk.mapNotNull { it.feeling }.groupingBy { it }.eachCount().maxWithOrNull(compareBy<Map.Entry<Feeling, Int>> { it.value }.thenByDescending { it.key.ordinal })?.key
        }.takeLast(r.ribbonMax)
    }

    /** 인생의 계절이 바뀐 날 (오늘 이전 가장 최근), 없으면 null. */
    fun lifeChange(birth: LocalDate, expectancy: Double, today: LocalDate): LocalDate? {
        val end = LifeSnapshot.endDate(birth, expectancy)
        val total = end.toEpochDay() - birth.toEpochDay()
        return (1..3).map { birth.plusDays(total * it / 4) }.lastOrNull { !it.isAfter(today) }
    }

    fun of(
        today: LocalDate, country: String?, life: Season, birth: LocalDate, expectancy: Double, start: LocalDate,
        breathDays: Int, lines: List<DayLine>, gazeDays: Int, letterDue: Boolean, r: Rules = Rules(),
    ): Decor {
        val season = realSeason(today, country)
        val tree = Tree.of(life)
        val change = lifeChange(birth, expectancy, today)
        val since = change?.let { ChronoUnit.DAYS.between(it, today) }
        val prev = if (since != null && since < r.changeDays && life.ordinal > 0) Tree.of(Season.entries[life.ordinal - 1]) else null
        val blend = if (prev == null) 1f else ((since ?: 0L) + 1).toFloat() / (r.changeDays + 1)
        val ribbons = ribbons(lines, r)
        return Decor(
            season, tree, stage(start, today, r.stageDays), prev, blend,
            hang(breathDays, r.hangBreaths), letterDue, ribbons.isNotEmpty(), ribbons,
            (gazeDays / r.budGazes).coerceAtMost(r.budMax), SeasonCard(albumYear(today), season, tree),
        )
    }
}
