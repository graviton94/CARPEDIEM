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
    /** 숨 한 단계. lo · hi = 이 단계가 채우는 숨의 크기 (한숨 호흡의 두 번째 들이쉼은 0.75 → 1). */
    class Phase(val step: BreathStep, val startMs: Long, val lengthMs: Long, val lo: Float = 0f, val hi: Float = 1f)

    /** 숨 한 번의 박자 (초): 들이쉼 · 머묾 · 내쉼 · 머묾. topS = 한 번 더 들이쉬는 숨 (한숨 호흡, 0 이면 없음). 앱 토큰에서 받는다. */
    class Rhythm(val inS: Double, val holdS: Double, val outS: Double, val restS: Double, val topS: Double = 0.0) {
        val cycleMs get() = ((inS + topS + holdS + outS + restS) * 1000).toLong()
    }
    /** 한숨 호흡에서 첫 들이쉼이 채우는 몫. */
    const val FIRST_SIP = 0.75f

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
            val top = r.topS > 0
            listOf(BreathStep.IN to r.inS, BreathStep.IN to r.topS, BreathStep.HOLD to r.holdS, BreathStep.OUT to r.outS, BreathStep.REST to r.restS).forEachIndexed { i, (s, sec) ->
                val ms = (sec * 1000).toLong()
                // 한숨 호흡: 첫 들이쉼은 0 → 0.75, 한 번 더는 0.75 → 1
                val (lo, hi) = when { !top -> 0f to 1f; i == 0 -> 0f to FIRST_SIP; i == 1 -> FIRST_SIP to 1f; else -> 0f to 1f }
                if (ms > 0) { out.add(Phase(s, t, ms, lo, hi)); t += ms }
            }
        }
        return out
    }

    /**
     * 하루의 숨결 (손끝으로, 05): from 부터 끝까지의 떨림 모양. slice ms 마다 세기 (0 ~ 255) 하나.
     * 들이쉼은 약하게 시작해 차오르고, 머묾은 고요, 내쉼은 길게 잦아든다. 같은 세기가 이어지면 하나로 묶는다.
     */
    /**
     * 하루의 숨결 (손끝으로, 05): from 부터 끝까지의 떨림 (길이 ms, 세기 0 ~ 255; 세기 0 = 쉼).
     * 빠르게 떨지 않고 아주 느리고 여리게 톡톡 (1.1.4): 들이쉼엔 [inEvery] ms 마다 조금씩 또렷하게, 내쉼엔 [outEvery] ms 마다 잦아들게, 머묾은 고요.
     */
    fun touchWave(plan: List<Phase>, from: Long, pulse: Long = 40, inEvery: Long = 1500, outEvery: Long = 2500, low: Int = 18, high: Int = 80): Pair<LongArray, IntArray> {
        val times = ArrayList<Long>(); val amps = ArrayList<Int>()
        fun add(len: Long, a: Int) { if (len <= 0) return; if (amps.isNotEmpty() && amps.last() == a) times[times.size - 1] = times.last() + len else { times.add(len); amps.add(a.coerceIn(0, 255)) } }
        var t = from.coerceAtLeast(0)
        plan.forEach { ph ->
            val end = ph.startMs + ph.lengthMs
            if (end <= t) return@forEach
            val every = when (ph.step) { BreathStep.IN -> inEvery; BreathStep.OUT -> outEvery; else -> 0L }
            if (every == 0L) { add(end - t, 0); t = end; return@forEach }
            // 이 단계 안의 톡: 단계 시작에서 every 마다
            var k = ph.startMs
            while (k < end) {
                val pEnd = minOf(k + pulse, end)
                if (pEnd > t) {
                    if (k > t) add(k - t, 0)
                    val f = ((k - ph.startMs).toFloat() / ph.lengthMs).coerceIn(0f, 1f)
                    val a = if (ph.step == BreathStep.IN) (low + (high - low) * (ph.lo + (ph.hi - ph.lo) * f)).toInt() else (high - (high - low) * f).toInt()
                    add(pEnd - maxOf(k, t), a); t = pEnd
                }
                val next = minOf(k + every, end)
                if (next > t) { add(next - t, 0); t = next }
                k += every
            }
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
    fun fullness(p: Phase, f: Float): Float = if (p.step == BreathStep.IN) p.lo + (p.hi - p.lo) * ease(f) else fullness(p.step, f)
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
