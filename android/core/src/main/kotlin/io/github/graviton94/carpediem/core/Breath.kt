package io.github.graviton94.carpediem.core

import java.time.LocalDate

/** 숨의 종류: 고요한 호흡 · 마음 산책 · 잠드는 명상. */
/** 고요한 호흡 · 마음 산책 · 잠드는 명상 · 고마움 명상 (고요한 호흡의 리듬, 내쉴 때마다 고마운 것 하나). */
enum class BreathKind { CALM, BOX, SLEEP, THANKS }

/** 숨 한 단계. */
enum class BreathStep { IN, HOLD, OUT, REST }

/** 앱의 소리: 없음 · 파도 · 바람 · 빗소리 · 잔잔한 파장. */
enum class Sound { NONE, WAVES, WIND, RAIN, TONE, SEASON }

/** 하루의 때: 문장 · 숨 · 권하기가 때에 맞는 말을 고른다. 아침 5–11시, 낮 11–17시, 저녁 17–21시, 밤 21–5시. */
enum class DayPart {
    MORNING, DAY, EVENING, NIGHT;
    companion object {
        fun of(hour: Int): DayPart = when (hour) { in 5..10 -> MORNING; in 11..16 -> DAY; in 17..20 -> EVENING; else -> NIGHT }
    }
}

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
        return cycles(r, maxOf(1L, (total + r.cycleMs / 2) / r.cycleMs).toInt())
    }

    /** 숨 n 번만 (하루 닫기의 짧은 숨). */
    fun cycles(r: Rhythm, n: Int): List<Phase> {
        val cycles = maxOf(1, n)
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

    /**
     * 하루의 숨결 (손끝으로, 05): from 부터 끝까지의 떨림 모양. slice ms 마다 세기 (0 ~ 255) 하나.
     * 들이쉼은 약하게 시작해 차오르고, 머묾은 고요, 내쉼은 길게 잦아든다. 같은 세기가 이어지면 하나로 묶는다.
     */
    fun touchWave(plan: List<Phase>, from: Long, slice: Long = 100, low: Int = 18, high: Int = 190): Pair<LongArray, IntArray> {
        val end = plan.lastOrNull()?.let { it.startMs + it.lengthMs } ?: return LongArray(0) to IntArray(0)
        val times = ArrayList<Long>(); val amps = ArrayList<Int>()
        var t = from.coerceAtLeast(0)
        while (t < end) {
            val len = minOf(slice, end - t)
            val a = at(plan, t + len / 2)?.let { (ph, f) ->
                when (ph.step) {
                    BreathStep.IN -> (low + (high - low) * f).toInt()
                    BreathStep.OUT -> (high * 0.85f * (1f - f) + low * f * 0.5f).toInt()
                    else -> 0
                }
            } ?: 0
            if (amps.isNotEmpty() && amps.last() == a) times[times.size - 1] = times.last() + len else { times.add(len); amps.add(a.coerceIn(0, 255)) }
            t += len
        }
        return times.toLongArray() to amps.toIntArray()
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
