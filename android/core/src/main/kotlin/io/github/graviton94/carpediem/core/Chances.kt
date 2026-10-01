package io.github.graviton94.carpediem.core

import java.time.DayOfWeek
import java.time.LocalDate

/** 정원의 우연한 순간 (몇 초 머물다 사라지고, 물건은 남지 않음). */
enum class Chance(val key: String) { BUBBLES("bubbles"), FIREFLIES("fireflies"), RAINBOW("rainbow"), BUTTERFLIES("butterflies"), SNAIL("snail"), AURORA("aurora"), WIND("wind");
    companion object { fun of(key: String) = entries.firstOrNull { it.key == key } }
}

/**
 * 언제 무엇이 오는지 (정해진 상이 아니라 쓰는 날들이 주는 것). 한 번에 하나만.
 * 한 줄을 보낸 순간 = 계절 바람 (어제 무거운 마음을 남겼으면 무지개), 숨을 끝까지 쉰 밤 = 반딧불 (겨울 밤엔 스무 번에 한 번 오로라),
 * 정원을 열 때 = 오랜만에 돌아온 날 달팽이, 그림을 보낸 날 둘에 한 번 비눗방울, 봄 · 여름 숨을 세 번 쉰 주에 한 번 나비 한 쌍.
 */
object Chances {
    fun onLine(before: List<DayLine>, today: LocalDate): Chance =
        if (before.any { it.date == today.minusDays(1) && it.feeling in Letters.HEAVY }) Chance.RAINBOW else Chance.WIND

    /** roll = 지금까지 숨 쉰 날 수 (스무 번에 한 번을 날마다 같게 정함). */
    fun onBreath(night: Boolean, winter: Boolean, roll: Int): Chance? = when {
        !night -> null
        winter && roll % 20 == 0 -> Chance.AURORA
        else -> Chance.FIREFLIES
    }

    /** 정원을 열 때. 이미 본 것 (shown 의 키) 은 다시 오지 않음. 돌려주는 키를 shown 에 더해 둔다. */
    fun onOpen(today: LocalDate, season: Season, sharedOn: LocalDate?, returned: LocalDate?, breathDays: List<LocalDate>, shown: Set<String>): Pair<Chance, String>? {
        val week = today.with(DayOfWeek.MONDAY)
        val snail = "snail:$today"; if (returned == today && snail !in shown) return Chance.SNAIL to snail
        val bubbles = "bubbles:$today"; if (sharedOn == today && bubbles !in shown && today.toEpochDay() % 2 == 0L) return Chance.BUBBLES to bubbles
        val fly = "butterflies:$week"
        if (season == Season.SPRING || season == Season.SUMMER) {
            if (fly !in shown && breathDays.distinct().count { !it.isBefore(week) && !it.isAfter(today) } >= 3) return Chance.BUTTERFLIES to fly
        }
        return null
    }
}
