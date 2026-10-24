package io.github.graviton94.carpediem.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import io.github.graviton94.carpediem.design.Tokens
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * 노래하는 돌: 하루가 한 음을 부르면 가족 돌들이 차례로 저마다의 음으로 대답한다 (오음계라 어떻게 겹쳐도 어울림).
 * 소리 파일 없이 짧은 종소리 음을 한 번에 만들어 틀고 끝. 미디어 볼륨 · 무음 모드를 따른다.
 */
object StoneSong {
    private val T = Tokens.Garden.Song
    /** 다장조 오음계 두 옥타브 (C4 ~ A5). */
    private val SCALE = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25, 587.33, 659.25, 783.99, 880.00)

    /** 돌마다 늘 같은 음 (seed 로). 내 하루는 가운데 쯤. */
    fun pitch(seed: Long, me: Boolean): Double = if (me) SCALE[4 + Math.floorMod(seed, 3L).toInt()] else SCALE[Math.floorMod(seed * 31 + 7, SCALE.size.toLong()).toInt()]

    /** 돌멍하기 노래의 음 (1.1.4): 정원 돌들의 음, 모자라면 가운데 오음계로 채워 다섯 음 이상. */
    fun gardenNotes(haruSeed: Long, seeds: List<Long>): DoubleArray {
        val set = LinkedHashSet<Double>(); set.add(pitch(haruSeed, true)); seeds.forEach { set.add(pitch(it, false)) }
        var i = 3; while (set.size < 5 && i < SCALE.size) { set.add(SCALE[i]); i++ }
        return set.toDoubleArray()
    }

    /** 별자리 연주: 마음마다 한 음 (가벼운 마음은 높고 맑게, 무거운 마음은 낮고 부드럽게). 마음이 없으면 가운데. */
    fun feelingPitch(f: io.github.graviton94.carpediem.core.Feeling?): Double = SCALE[when (f) {
        io.github.graviton94.carpediem.core.Feeling.JOY -> 9; io.github.graviton94.carpediem.core.Feeling.HOPE -> 8; io.github.graviton94.carpediem.core.Feeling.PROUD -> 8; io.github.graviton94.carpediem.core.Feeling.THANKS -> 7
        io.github.graviton94.carpediem.core.Feeling.CALM -> 5; io.github.graviton94.carpediem.core.Feeling.DISAPPOINT -> 3; io.github.graviton94.carpediem.core.Feeling.UNSETTLED -> 3
        io.github.graviton94.carpediem.core.Feeling.HAPPY -> 9; io.github.graviton94.carpediem.core.Feeling.WISTFUL -> 3; io.github.graviton94.carpediem.core.Feeling.CONCERN -> 2
        io.github.graviton94.carpediem.core.Feeling.WORRY -> 2; io.github.graviton94.carpediem.core.Feeling.SAD -> 0
        else -> 4 }]

    /**
     * 그날의 결 (1.1.6): 같은 돌의 음이라도 오늘 보낸 마음 · 때 · 계절에 따라 노래가 달라진다.
     * step = 오음계에서 옮기는 칸 (밝은 마음 위로, 무거운 마음 · 밤 아래로), tempo = 대답 사이를 늘이는 배수,
     * partials · amps = 계절의 음색 (봄 풍경 · 여름 대나무 · 가을 목탁 · 겨울 싱잉볼), decay = 잦아드는 빠르기, attack = 치는 세기, level = 크기.
     */
    class Tone(val step: Int = 0, val tempo: Float = 1f, val partials: DoubleArray = doubleArrayOf(1.0, 2.0, 3.0), val amps: DoubleArray = doubleArrayOf(1.0, 0.25, 0.08),
               val decay: Double = 3.2, val attack: Double = 90.0, val level: Float = 1f)

    /** 오늘의 결: feeling = 오늘 보낸 마음 (없으면 null), hour = 지금 시각, season = 실제 계절. */
    fun toneOf(feeling: io.github.graviton94.carpediem.core.Feeling?, hour: Int, season: io.github.graviton94.carpediem.core.Season): Tone {
        val heavy = feeling in io.github.graviton94.carpediem.core.Letters.HEAVY
        val bright = feeling in io.github.graviton94.carpediem.core.Feeling.BRIGHT
        val night = hour >= 21 || hour < 5; val morning = hour in 5..9
        val step = (if (bright) 1 else if (heavy) -2 else 0) + (if (night) -1 else 0)
        val tempo = (if (heavy) 1.35f else if (bright) 0.9f else 1f) * (if (night) 1.2f else if (morning) 0.92f else 1f)
        val (partials, amps) = when (season) {
            io.github.graviton94.carpediem.core.Season.SPRING -> doubleArrayOf(1.0, 2.76, 5.4) to doubleArrayOf(1.0, 0.4, 0.14)
            io.github.graviton94.carpediem.core.Season.SUMMER -> doubleArrayOf(1.0, 2.4, 4.6) to doubleArrayOf(1.0, 0.3, 0.1)
            io.github.graviton94.carpediem.core.Season.AUTUMN -> doubleArrayOf(1.0, 1.58, 2.9) to doubleArrayOf(1.0, 0.35, 0.12)
            io.github.graviton94.carpediem.core.Season.WINTER -> doubleArrayOf(1.0, 2.71, 5.15) to doubleArrayOf(1.0, 0.5, 0.2)
        }
        // 무거운 날은 부드럽게 치고 길게 머묾, 밝은 날은 또렷하고 짧게. 밤은 작게
        val decay = (if (heavy) 2.2 else if (bright) 3.8 else 3.2) * (if (season == io.github.graviton94.carpediem.core.Season.SUMMER) 1.4 else 1.0)
        val attack = if (heavy || night) 35.0 else 90.0
        return Tone(step, tempo, partials, amps, decay, attack, if (night) 0.7f else if (heavy) 0.85f else 1f)
    }

    /** 돌의 음을 오늘의 결만큼 오음계 안에서 옮김 (어떻게 옮겨도 어울림). */
    fun shifted(f: Double, step: Int): Double {
        val i = SCALE.indexOfFirst { kotlin.math.abs(it - f) < 0.5 }.takeIf { it >= 0 } ?: return f
        return SCALE[(i + step).coerceIn(0, SCALE.size - 1)]
    }

    /** 종 한 번을 mix 의 s0 자리부터 더함 (영상 소리도 같은 음색으로, 1.1.12). gain = 세기. */
    fun strike(mix: FloatArray, sr: Int, s0: Int, f: Double, tone: Tone, gain: Float = 1f) {
        val ring = T.ringMs / 1000.0
        for (i in 0 until (ring * sr).toInt()) {
            val j = s0 + i; if (j >= mix.size) break; if (j < 0) continue
            val t = i / sr.toDouble()
            // 종: 바탕음 + 음색의 배음, 빠르게 올라 천천히 잦아듦 (높은 배음일수록 먼저 잦아듦)
            val env = (1 - exp(-t * tone.attack))
            var v = 0.0
            for (k in tone.partials.indices) v += tone.amps[k] * exp(-t * tone.decay * (1 + 0.6 * k)) * sin(2 * PI * f * tone.partials[k] * t)
            mix[j] += (env * v * gain).toFloat()
        }
    }

    /** notes = (음, 시작 ms). 다른 스레드에서 만들어 틀고, 끝나면 스스로 놓음. tone = 음색 · 세기 (없으면 맑은 종). */
    fun play(notes: List<Pair<Double, Long>>, tone: Tone = Tone()) {
        if (notes.isEmpty()) return
        Thread({
            runCatching {
                val sr = 22050; val ring = T.ringMs / 1000.0
                val total = ((notes.maxOf { it.second } / 1000.0 + ring) * sr).toInt()
                val mix = FloatArray(total)
                notes.forEach { (f, at) -> strike(mix, sr, (at / 1000.0 * sr).toInt(), f, tone) }
                val peak = mix.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1f)
                val pcm = ShortArray(total) { (mix[it] / peak * T.volume * tone.level * Short.MAX_VALUE).toInt().toShort() }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(sr).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
                track.write(pcm, 0, pcm.size); track.play()
                Thread.sleep((total * 1000L / sr) + 200); track.release()
            }
        }, "stone-song").apply { isDaemon = true; start() }
    }
}
