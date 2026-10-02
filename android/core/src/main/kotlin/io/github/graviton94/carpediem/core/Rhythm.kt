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
