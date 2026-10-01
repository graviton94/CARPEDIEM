package io.github.graviton94.carpediem.core

import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit

enum class Kind { PERSON, PET }
enum class Species { DOG, CAT, OTHER }

/**
 * 정원에 함께 앉는 가족 · 반려동물 한 명 (기기 안에만).
 * birth 를 모르면 길 위가 아니라 내 돌 곁에 앉는다. 반려동물의 birth 는 ‘생일 또는 우리 집에 온 날’.
 */
data class Person(
    val id: String,
    val name: String,
    val kind: Kind,
    val species: Species? = null,
    val birth: LocalDate? = null,
    val sex: Sex = Sex.OTHER,
    val country: String = LifeExpectancyTable.WORLD,
    val seed: Long,
    val rerolls: Int = 0,
    val metOn: LocalDate,
    val showAhead: Boolean = false,
    /** 함께한 첫날을 직접 정했으면 그날 (null = 저절로: 두 삶이 겹친 첫날). */
    val together: LocalDate? = null,
    /** 기억의 돌: 곁을 떠난 날 (적어 준 경우만). */
    val until: LocalDate? = null,
    /** 기억의 돌: 정원 하늘에 작은 별로 둘지 (기본 켬). */
    val star: Boolean = true,
)

object Family {
    /** 돌이 많아 길(span)에 다 앉지 못하면 모두를 같은 비율로 줄인다 (1 = 그대로). gap = 돌 사이 최소 간격. */
    fun fitScale(widths: List<Double>, span: Double, gap: Double): Double {
        val need = widths.sum() + gap * (widths.size - 1).coerceAtLeast(0)
        return if (need <= span || widths.isEmpty()) 1.0 else ((span - gap * (widths.size - 1)) / widths.sum()).coerceIn(0.3, 1.0)
    }

    /** 나를 포함해 한 정원에 앉는 돌 수. */
    const val MAX = 9
    const val NAME_MAX = 8
    const val REROLLS = 3

    fun cleanName(s: String) = s.replace(Regex("[\\t\\r\\n]+"), " ").trim().let { if (it.codePointCount(0, it.length) <= NAME_MAX) it else it.substring(0, it.offsetByCodePoints(0, NAME_MAX)) }

    // ───── 저장: 한 줄에 한 명, 칸은 탭 ─────
    fun encode(list: List<Person>): String = list.joinToString("\n") { p ->
        listOf(p.id, cleanName(p.name), p.kind.name, p.species?.name ?: "-", p.birth?.toEpochDay()?.toString() ?: "-", p.sex.name, p.country,
            p.seed.toString(), p.rerolls.toString(), p.metOn.toEpochDay().toString(), if (p.showAhead) "1" else "0", p.together?.toEpochDay()?.toString() ?: "-",
            p.until?.toEpochDay()?.toString() ?: "-", if (p.star) "1" else "0").joinToString("\t")
    }

    fun decode(s: String?): List<Person> = s.orEmpty().lineSequence().mapNotNull { row ->
        val f = row.split('\t'); if (f.size < 11) return@mapNotNull null
        runCatching {
            Person(f[0], f[1], Kind.valueOf(f[2]), f[3].takeIf { it != "-" }?.let { Species.valueOf(it) }, f[4].toLongOrNull()?.let { LocalDate.ofEpochDay(it) },
                Sex.valueOf(f[5]), f[6], f[7].toLong(), f[8].toInt(), LocalDate.ofEpochDay(f[9].toLong()), f[10] == "1",
                f.getOrNull(11)?.toLongOrNull()?.let { LocalDate.ofEpochDay(it) },
                f.getOrNull(12)?.toLongOrNull()?.let { LocalDate.ofEpochDay(it) }, f.getOrNull(13) != "0")
        }.getOrNull()
    }.toList()

    // ───── 날짜 ─────
    /** 그해의 생일 (2월 29일은 평년에 2월 28일). */
    fun birthdayIn(birth: LocalDate, year: Int): LocalDate {
        val md = MonthDay.from(birth)
        return if (md.isValidYear(year)) md.atYear(year) else LocalDate.of(year, 2, 28)
    }

    fun isBirthday(birth: LocalDate?, today: LocalDate) = birth != null && birthdayIn(birth, today.year) == today

    /** 오늘을 포함해 다음에 오는 생일. */
    fun nextBirthday(birth: LocalDate, today: LocalDate): LocalDate =
        birthdayIn(birth, today.year).let { if (it.isBefore(today)) birthdayIn(birth, today.year + 1) else it }

    /** 기대수명: 사람은 나라 · 성별 평균, 반려동물은 종의 기대수명 (앱 토큰에서 받음). */
    fun expectancy(p: Person, table: LifeExpectancyTable, petYears: (Species) -> Double): Double =
        if (p.kind == Kind.PET) petYears(p.species ?: Species.OTHER) else table.expectancy(p.country, p.sex)

    fun daysUntil(to: LocalDate, today: LocalDate) = ChronoUnit.DAYS.between(today, to)

    /** 함께한 첫날: 직접 정한 날, 없으면 두 삶이 겹친 첫날 (반려동물은 생일 · 온 날), 생일도 모르면 정원에 부른 날. */
    fun togetherSince(myBirth: LocalDate, p: Person): LocalDate {
        p.together?.let { return it }
        val b = p.birth ?: return p.metOn
        return if (p.kind == Kind.PET) b else maxOf(myBirth, b)
    }

    /**
     * 돌 자리 정하기 (같은 입력이면 늘 같은 자리).
     * targets: 원래 자리 (null = 생일 모름 → me 곁), widths: 몸 폭, me: 내 돌 번호.
     * 이웃한 두 돌이 (폭 합 / 2 + gap) 보다 가까우면 반씩 밀어낸다. 길 [lo, hi] 안에 가두고, 모자라면 gap 을 minGap 까지 줄인다.
     */
    fun place(targets: List<Double?>, widths: List<Double>, me: Int, lo: Double, hi: Double, gap: Double, minGap: Double): List<Double> {
        val n = targets.size
        if (n == 0) return emptyList()
        val meX = targets[me] ?: ((lo + hi) / 2)
        var side = meX + widths[me] / 2
        val t = DoubleArray(n) { i -> targets[i] ?: run { val x = side + gap + widths[i] / 2; side = x + widths[i] / 2; x } }
        val order = (0 until n).sortedWith(compareBy({ t[it] }, { it }))
        fun solve(g: Double): DoubleArray {
            val p = DoubleArray(n) { t[it] }
            fun need(a: Int, b: Int) = (widths[a] + widths[b]) / 2 + g
            repeat(40) {
                for (k in 0 until n - 1) {
                    val a = order[k]; val b = order[k + 1]; val d = p[b] - p[a]; val nd = need(a, b)
                    if (d < nd) { val push = (nd - d) / 2; p[a] -= push; p[b] += push }
                }
                for (i in 0 until n) p[i] = p[i].coerceIn(lo + widths[i] / 2, hi - widths[i] / 2)
            }
            // 끝에서 한 번 더: 앞에서부터 밀고, 뒤에서부터 당겨 틈을 맞춤
            for (k in 1 until n) { val a = order[k - 1]; val b = order[k]; p[b] = maxOf(p[b], p[a] + need(a, b)) }
            for (k in n - 2 downTo 0) { val a = order[k]; val b = order[k + 1]; p[b] = minOf(p[b], hi - widths[b] / 2); p[a] = minOf(p[a], p[b] - need(a, b)) }
            return p
        }
        val total = widths.sum()
        val g = if (total + gap * (n - 1) <= hi - lo) gap else ((hi - lo - total) / (n - 1).coerceAtLeast(1)).coerceAtLeast(minGap)
        return solve(g).toList()
    }
}

/**
 * 기억의 돌: 곁을 떠난 가족 · 반려동물. 정원 · 위젯 · 알림에는 나오지 않고 ‘기억의 자리’에서만 (가족 9개와 따로, 넷까지).
 * 숫자(나이 · 떠난 지 며칠)는 쓰지 않고, 적어 준 날을 계절까지만 보여 준다.
 */
object Memories {
    const val MAX = 4
    /** 북반구 계절 (3–5 봄, 6–8 여름, 9–11 가을, 12–2 겨울). */
    fun seasonOf(d: LocalDate): Season = when (d.monthValue) { in 3..5 -> Season.SPRING; in 6..8 -> Season.SUMMER; in 9..11 -> Season.AUTUMN; else -> Season.WINTER }
    /** 함께한 첫날: 직접 정한 날. 반려동물은 없으면 생일 · 우리 집에 온 날 (사람의 생일은 함께한 날이 아니므로 쓰지 않음). */
    fun from(p: Person): LocalDate? = p.together ?: p.birth?.takeIf { p.kind == Kind.PET }
}
