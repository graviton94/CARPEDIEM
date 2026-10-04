package io.github.graviton94.carpediem.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 알림이 귀찮지 않게 (C2): 문장은 날마다 돌아가며, 할 일을 이미 한 날은 쉬고, 오래 오지 않으면 늘리지 않고 오히려 쉰다.
 * 돌아오면 정원의 달팽이 손님이 맞는다 (Chances.onOpen).
 */
object Nudges {
    /** 이만큼 (일) 정원을 열지 않았으면 아침 · 저녁 알림을 쉰다 (생일 · 특별한 날은 그대로). */
    const val QUIET_AFTER = 3

    fun quiet(lastOpen: LocalDate?, today: LocalDate): Boolean =
        lastOpen != null && ChronoUnit.DAYS.between(lastOpen, today) >= QUIET_AFTER

    /** 저녁 (하루 정리) 알림을 쉬는 날: 오늘 한 줄을 남겼거나 오늘 숨을 쉬었으면. */
    fun eveningRests(today: LocalDate, lines: List<DayLine>, breaths: List<Pair<LocalDate, BreathKind>>): Boolean =
        lines.any { it.date == today } || breaths.any { it.first == today }

    /** n 개 문장 가운데 오늘의 것 (날마다 다음 것으로, 이웃한 날은 겹치지 않음). salt 로 알림마다 시작을 다르게. */
    fun pick(day: LocalDate, n: Int, salt: Int = 0): Int = if (n <= 0) 0 else Math.floorMod(day.toEpochDay() + salt, n.toLong()).toInt()
}

/** 기억의 날 (R2): 기억의 돌에 적은 날이 해마다 돌아오는 주 (그날 앞뒤 사흘). 그 주엔 그 별이 조금 더 밝다. */
object MemoryWeek {
    const val HALF = 3

    /** 오늘이 그 주이면 그 해의 그날, 아니면 null. 떠난 해 그 자체는 치지 않음. */
    fun of(p: Person, today: LocalDate): LocalDate? {
        val u = p.until ?: return null
        return (today.year - 1..today.year + 1).map { Family.birthdayIn(u, it) }
            .firstOrNull { it.isAfter(u) && kotlin.math.abs(ChronoUnit.DAYS.between(it, today)) <= HALF }
    }

    /** 그 주의 첫날 (한 줄 알림은 이날 한 번). */
    fun startsToday(p: Person, today: LocalDate): Boolean = of(p, today)?.minusDays(HALF.toLong()) == today
}

/** 숨이 정원에 스미기 (E3): 오늘 마친 숨의 종류 (그날만, 세지도 쌓지도 않음). */
object BreathTrace {
    fun today(breaths: List<Pair<LocalDate, BreathKind>>, today: LocalDate): Set<BreathKind> =
        breaths.filter { it.first == today }.map { it.second }.toSet()
}

/** 돌에게 건네는 한 조각 (R1): 가족 돌 곁에 놓은 이번 계절의 조각. 한 사람에게 계절마다 하나, 그 계절이 끝날 때까지 정원에. */
data class Offering(val personId: String, val card: SeasonCard, val date: LocalDate)

object Offerings {
    /** 같은 해 · 같은 계절이면 같은 때 (나무는 그사이 바뀌어도 됨). */
    private fun same(a: SeasonCard, b: SeasonCard) = a.year == b.year && a.season == b.season

    fun encode(list: List<Offering>) = list.joinToString("\n") { "${it.personId}\t${it.card.id}\t${it.date.toEpochDay()}" }

    fun decode(s: String?): List<Offering> = s.orEmpty().lineSequence().mapNotNull { r ->
        val f = r.split('\t'); if (f.size < 3) return@mapNotNull null
        val c = SeasonCard.parse(f[1]) ?: return@mapNotNull null
        val d = f[2].toLongOrNull() ?: return@mapNotNull null
        Offering(f[0], c, LocalDate.ofEpochDay(d))
    }.toList()

    fun canOffer(list: List<Offering>, personId: String, now: SeasonCard) = list.none { it.personId == personId && same(it.card, now) }

    /** 놓기: 이미 이번 계절에 놓았으면 그대로. 오래된 것 (지난 계절) 은 지운다 (앨범에 남지 않는 조용한 마음). */
    fun put(list: List<Offering>, o: Offering): List<Offering> =
        if (!canOffer(list, o.personId, o.card)) list else list.filter { same(it.card, o.card) } + o

    /** 지금 정원에 놓여 있는 것 (사람 id → 조각). */
    fun shown(list: List<Offering>, now: SeasonCard): Map<String, Offering> =
        list.filter { same(it.card, now) }.associateBy { it.personId }
}

/** 정원의 한 해 (S2): 12월 마지막 주 (25 ~ 31일) 에 그해의 정원을 한 장으로 볼까 묻는다. */
object YearCard {
    const val FROM_DAY = 25
    fun due(today: LocalDate): Int? = if (today.monthValue == 12 && today.dayOfMonth >= FROM_DAY) today.year else null
}

/**
 * 걱정한 밤 다음 아침 (06): 어제 남긴 한 줄이 무거운 마음 (걱정 · 슬픔 · 실망) 이었으면, 오늘 처음 정원을 열 때 하루가 위로 한마디만.
 * 묻지도 세지도 않는다. shown = 이미 보여 준 날 (하루에 한 번).
 */
object Comfort {
    fun due(lines: List<DayLine>, today: LocalDate, shown: LocalDate?): Boolean =
        Pace.gap(shown, today, Pace.COMFORT_GAP) && lines.any { it.date == today.minusDays(1) && it.feeling in Letters.HEAVY }
}

/**
 * 고요한 빈도: 정원이 먼저 건네는 말 (권유 · 한마디) 이 날마다 되풀이되거나 부담이 되지 않게.
 * 기능은 그대로 두고, 얼마나 자주 말을 거는지만 여기서 정한다. 사람이 먼저 하는 일 (쓰기 · 숨 · 돌멍) 은 언제든.
 */
object Pace {
    /** 걱정한 밤 다음 아침의 한마디: 사흘에 한 번까지. */
    const val COMFORT_GAP = 3
    /** 한 줄 뒤 권유 (숨 · 바라보기 · 보내기): 사흘에 한 번까지. */
    const val CARE_GAP = 3
    /** 아침 씨앗: 권한 날로부터 사흘 뒤에 다시, 그때 심지 않았으면 일주일 쉼. */
    const val SEED_GAP = 3
    const val SEED_REST = 7
    /** 아침 숨 한 줄 권유: 사흘에 하루. */
    const val BREATH_EVERY = 3

    fun gap(last: LocalDate?, today: LocalDate, days: Int): Boolean = last == null || ChronoUnit.DAYS.between(last, today) >= days
    fun seed(today: LocalDate, lastOffered: LocalDate?, plantedThen: Boolean): Boolean = gap(lastOffered, today, if (plantedThen) SEED_GAP else SEED_REST)
    fun morningBreath(today: LocalDate): Boolean = Math.floorMod(today.toEpochDay(), BREATH_EVERY.toLong()) == 0L
}

/** 아침 씨앗 (04): 오늘 마음에 심는 작은 다짐 하나. 저녁에 ‘싹이 텄나요?’ — 텄으면 꽃, 아니면 흙 속에서 쉼 (실패로 남지 않음). */
enum class SeedState { PLANTED, BLOOMED, RESTING }

data class Seed(val date: LocalDate, val text: String, val state: SeedState = SeedState.PLANTED)

object Seeds {
    const val MAX_CHARS = 24
    /** 오래된 것부터 버리는 개수 (꽃은 추억에 남기려 넉넉히). */
    const val KEEP = 400

    fun encode(list: List<Seed>): String = list.joinToString("\n") { "${it.date.toEpochDay()}\t${it.state.name}\t${Lines.clean(it.text, MAX_CHARS)}" }
    fun decode(s: String?): List<Seed> = s.orEmpty().lineSequence().mapNotNull { r ->
        val p = r.split('\t', limit = 3); if (p.size < 3) return@mapNotNull null
        val d = p[0].toLongOrNull() ?: return@mapNotNull null
        val st = SeedState.entries.firstOrNull { it.name == p[1] } ?: return@mapNotNull null
        p[2].takeIf { it.isNotBlank() }?.let { Seed(LocalDate.ofEpochDay(d), it, st) }
    }.toList()

    fun of(list: List<Seed>, day: LocalDate): Seed? = list.lastOrNull { it.date == day }

    /** 하루에 하나: 같은 날이면 바꿔 심음 (글만, 상태는 처음으로). */
    fun plant(list: List<Seed>, day: LocalDate, text: String): List<Seed> {
        val t = Lines.clean(text, MAX_CHARS); if (t.isEmpty()) return list
        return (list.filterNot { it.date == day } + Seed(day, t)).sortedBy { it.date }.takeLast(KEEP)
    }

    fun answer(list: List<Seed>, day: LocalDate, bloomed: Boolean): List<Seed> =
        list.map { if (it.date == day && it.state == SeedState.PLANTED) it.copy(state = if (bloomed) SeedState.BLOOMED else SeedState.RESTING) else it }

    /** 저녁에 물어볼 씨앗: 오늘 심었고 아직 답하지 않은 것. */
    fun toAsk(list: List<Seed>, today: LocalDate): Seed? = of(list, today)?.takeIf { it.state == SeedState.PLANTED }

    fun bloomed(list: List<Seed>): List<Seed> = list.filter { it.state == SeedState.BLOOMED }.sortedByDescending { it.date }

    /** 오늘 보여 줄 고르기 몇 개 (n 개 중 날마다 돌아가며 k 개). */
    fun choices(n: Int, today: LocalDate, k: Int): List<Int> = if (n <= 0) emptyList() else (0 until minOf(k, n)).map { Math.floorMod(today.toEpochDay() * k + it, n.toLong()).toInt() }.distinct()
}

/**
 * 정원 손님: 앱을 열든 안 열든 날마다 정해지는 우연 (평균 일주일에 한 번, 사람마다 다른 날).
 * 쉬었다고 더 오거나 매일 열었다고 덜 오지 않는다. 다섯 중 하나쯤은 드문 손님 (토끼 · 부엉이).
 */
object Guests {
    val COMMON = listOf("tit", "squirrel", "hedgehog")
    val RARE = listOf("rabbit", "owl")
    /** 평균 며칠에 한 번. */
    const val EVERY = 7

    /** 오늘 손님 (없으면 null). seed = 사람마다 다른 수 (하루 번호). */
    fun on(today: LocalDate, seed: Long): String? {
        val h = mix(seed * 1_000_003L + today.toEpochDay())
        if (Math.floorMod(h, EVERY.toLong()) != 0L) return null
        val k = Math.floorMod(h ushr 16, 10L).toInt()
        return if (k < RARE.size) RARE[k] else COMMON[k % COMMON.size]
    }

    private fun mix(x: Long): Long {
        var z = x + -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }
}
