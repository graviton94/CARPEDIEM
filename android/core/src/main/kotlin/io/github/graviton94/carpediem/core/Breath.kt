package io.github.graviton94.carpediem.core

import java.time.LocalDate

/** 숨의 종류: 고요한 숨 · 네모 숨 · 잠드는 숨. */
enum class BreathKind { CALM, BOX, SLEEP }

/** 숨 한 단계. */
enum class BreathStep { IN, HOLD, OUT, REST }

/** 앱의 소리: 없음 · 파도 · 바람 · 빗소리 · 잔잔한 파장. */
enum class Sound { NONE, WAVES, WIND, RAIN, TONE }

object Breath {
    class Phase(val step: BreathStep, val startMs: Long, val lengthMs: Long)

    /** 숨 한 번의 박자 (초): 들이쉼 · 머묾 · 내쉼 · 머묾. 앱 토큰에서 받는다. */
    class Rhythm(val inS: Double, val holdS: Double, val outS: Double, val restS: Double) {
        val cycleMs get() = ((inS + holdS + outS + restS) * 1000).toLong()
    }

    /**
     * 정해진 분 동안의 숨 단계 목록. 마지막 숨은 끝까지 쉬고 끝낸다 (시간이 조금 넘어도 중간에 끊지 않음).
     * 길이가 0인 단계는 빼고, 적어도 한 번은 숨 쉰다.
     */
    fun plan(r: Rhythm, minutes: Int): List<Phase> {
        val total = minutes * 60_000L
        val cycles = maxOf(1L, (total + r.cycleMs / 2) / r.cycleMs)
        val out = ArrayList<Phase>()
        var t = 0L
        repeat(cycles.toInt()) {
            listOf(BreathStep.IN to r.inS, BreathStep.HOLD to r.holdS, BreathStep.OUT to r.outS, BreathStep.REST to r.restS).forEach { (s, sec) ->
                val ms = (sec * 1000).toLong()
                if (ms > 0) { out.add(Phase(s, t, ms)); t += ms }
            }
        }
        return out
    }

    /** 지금 몇 번째 단계, 그 단계에서 얼마나 지났는지 (0 ~ 1). 끝났으면 null. */
    fun at(plan: List<Phase>, elapsedMs: Long): Pair<Phase, Float>? {
        val p = plan.lastOrNull { it.startMs <= elapsedMs } ?: return null
        if (elapsedMs >= p.startMs + p.lengthMs) return null
        return p to ((elapsedMs - p.startMs).toFloat() / p.lengthMs)
    }

    /** 숨의 크기 (0 = 다 내쉼, 1 = 다 들이쉼): 들이쉼에 차오르고, 머묾에 그대로, 내쉼에 비워짐. 부드럽게 (사인). */
    fun fullness(step: BreathStep, f: Float): Float = when (step) {
        BreathStep.IN -> ease(f)
        BreathStep.HOLD -> 1f
        BreathStep.OUT -> 1f - ease(f)
        BreathStep.REST -> 0f
    }

    private fun ease(f: Float) = (0.5 - 0.5 * kotlin.math.cos(Math.PI * f.coerceIn(0f, 1f))).toFloat()

    // ───── 숨 쉰 날 (날짜와 종류만) ─────
    fun encode(days: List<Pair<LocalDate, BreathKind>>) = days.joinToString("\n") { "${it.first.toEpochDay()}\t${it.second.name}" }
    fun decode(s: String?): List<Pair<LocalDate, BreathKind>> = s.orEmpty().lineSequence().mapNotNull { row ->
        val f = row.split('\t'); val d = f.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        LocalDate.ofEpochDay(d) to (BreathKind.entries.firstOrNull { it.name == f.getOrNull(1) } ?: BreathKind.CALM)
    }.toList()
}
