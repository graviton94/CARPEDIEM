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

    /** notes = (음, 시작 ms). 다른 스레드에서 만들어 틀고, 끝나면 스스로 놓음. */
    fun play(notes: List<Pair<Double, Long>>) {
        if (notes.isEmpty()) return
        Thread({
            runCatching {
                val sr = 22050; val ring = T.ringMs / 1000.0
                val total = ((notes.maxOf { it.second } / 1000.0 + ring) * sr).toInt()
                val mix = FloatArray(total)
                notes.forEach { (f, at) ->
                    val s0 = (at / 1000.0 * sr).toInt()
                    for (i in 0 until (ring * sr).toInt()) {
                        val j = s0 + i; if (j >= total) break
                        val t = i / sr.toDouble()
                        // 맑은 종: 바탕음 + 옅은 배음, 빠르게 올라 천천히 잦아듦
                        val env = (1 - exp(-t * 90)) * exp(-t * 3.2)
                        mix[j] += (env * (sin(2 * PI * f * t) + 0.25 * sin(4 * PI * f * t) + 0.08 * sin(6 * PI * f * t))).toFloat()
                    }
                }
                val peak = mix.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1f)
                val pcm = ShortArray(total) { (mix[it] / peak * T.volume * Short.MAX_VALUE).toInt().toShort() }
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
