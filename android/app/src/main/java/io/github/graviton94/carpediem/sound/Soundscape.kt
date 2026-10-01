package io.github.graviton94.carpediem.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.core.Sound
import io.github.graviton94.carpediem.design.Tokens
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

/**
 * 숨 · 멍하니 보는 정원의 소리. 소리 파일 없이 앱이 그때그때 만든다:
 * 파도(갈색 잡음이 숨을 따라 밀려왔다 빠짐) · 바람(분홍 잡음이 천천히 불었다 잦아듦) · 빗소리(흰 잡음 + 드문 빗방울) ·
 * 잔잔한 파장(가까운 두 음이 천천히 어긋나며 울림) · 계절의 소리(봄 새소리, 여름 풀벌레, 가을 바람과 귀뚜라미, 겨울 장작 불). 시작 · 끝에는 작은 종소리.
 * 미디어 소리로 나가서 폰의 미디어 볼륨 · 무음 모드를 따른다.
 */
object Soundscape {
    private val T = Tokens.Garden.Sound

    private fun attributes() = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private fun format(sr: Int) = AudioFormat.Builder().setSampleRate(sr).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()

    /** 바탕 소리 하나. start() 로 켜고 stop() 으로 스르르 끈다. breath (0 ~ 1) 를 주면 파도 · 파장이 숨을 따른다. */
    class Player(private val sound: Sound, private val season: Season = Season.SPRING) {
        @Volatile var breath: Float = -1f
        @Volatile private var stopping = false
        private var thread: Thread? = null

        fun start() {
            if (sound == Sound.NONE || thread != null) return
            thread = Thread({ runCatching { play() } }, "soundscape").apply { isDaemon = true; start() }
        }

        fun stop() { stopping = true }

        private fun play() {
            val sr = T.sampleRate.toInt()
            val min = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder().setAudioAttributes(attributes()).setAudioFormat(format(sr))
                .setBufferSizeInBytes(max(min, sr / 4 * 2)).setTransferMode(AudioTrack.MODE_STREAM).build()
            track.play()
            val buf = ShortArray(sr / 20)
            val fadeIn = 1f / (T.fadeInMs / 1000f * sr); val fadeOut = 1f / (T.fadeOutMs / 1000f * sr)
            var gain = 0f
            // 잡음 만드는 상태 (xorshift, 분홍 · 갈색 필터, 바람 필터, 빗방울)
            var seed = 0x2545F491
            fun white(): Float { seed = seed xor (seed shl 13); seed = seed xor (seed ushr 17); seed = seed xor (seed shl 5); return (seed % 10000) / 10000f }
            var b0 = 0f; var b1 = 0f; var b2 = 0f; var brown = 0f; var lp = 0f; var hpPrev = 0f; var drop = 0f; var smooth = 0.5f
            var n = 0L
            val two = 2.0 * PI
            // 계절의 소리: 새 한 마리의 짧은 지저귐 (음 몇 개), 풀벌레 울음, 장작 튀는 소리
            var chirpLeft = 0; var chirpLen = 1; var chirpF0 = 3000.0; var chirpF1 = 4000.0; var chirpPh = 0.0; var notes = 0; var noteGap = 0
            var cricketPh = 0.0; var crackle = 0f; var hpC = 0f
            fun seasonal(t: Double, w: Float, pink: Float): Float {
                lp += (pink - lp) * 0.03f
                val air = lp
                return when (season) {
                    Season.SPRING -> {
                        // 새: 가끔 2–4음 지저귐, 음마다 높이가 미끄러짐
                        if (chirpLeft <= 0) {
                            if (notes > 0) {
                                if (noteGap > 0) noteGap-- else {
                                    notes--; chirpLen = (sr * (0.06 + 0.04 * (white() + 1))).toInt(); chirpLeft = chirpLen
                                    chirpF0 = 2600.0 + 700.0 * (white() + 1); chirpF1 = chirpF0 + 700.0 * white(); noteGap = (sr * 0.05).toInt()
                                }
                            } else if (white() > 0.99996f) { notes = 2 + ((white() + 1) * 1.5f).toInt(); noteGap = 0 }
                        }
                        var bird = 0f
                        if (chirpLeft > 0) {
                            val k = 1.0 - chirpLeft.toDouble() / chirpLen
                            chirpPh += two * (chirpF0 + (chirpF1 - chirpF0) * k) / sr
                            bird = (sin(chirpPh) * sin(PI * k)).toFloat() * 0.22f; chirpLeft--
                        }
                        air * 1.1f + bird
                    }
                    Season.SUMMER -> {
                        // 풀벌레: 높은 음이 빠르게 떨며, 잠깐 울고 잠깐 쉼
                        cricketPh += two * 4400 / sr
                        val trill = if (sin(two * t * 28) > 0) 1f else 0f
                        val bout = if (0.5 + 0.5 * sin(two * t * 0.55) > 0.35) 1f else 0f
                        air * 0.6f + sin(cricketPh).toFloat() * trill * bout * 0.05f
                    }
                    Season.AUTUMN -> {
                        // 가을 바람 + 드문 귀뚜라미
                        val gust = 0.5f + 0.5f * sin(two * t * 0.05 + sin(two * t * 0.011) * 2).toFloat()
                        cricketPh += two * 3900 / sr
                        val chirp = if ((t % 2.6) < 0.36 && sin(two * t * 9) > 0.2) 1f else 0f
                        air * 2.0f * gust + sin(cricketPh).toFloat() * chirp * 0.035f
                    }
                    Season.WINTER -> {
                        // 장작 불: 낮게 웅웅 + 가끔 탁 튀는 소리
                        if (white() > 0.9997f) crackle = 0.5f + 0.2f * (white() + 1)
                        crackle *= 0.985f
                        val hp = (w - hpC) * 0.5f; hpC = w
                        brown * 2.2f + hp * crackle * 1.4f
                    }
                }
            }
            while (true) {
                for (i in buf.indices) {
                    val t = n.toDouble() / sr; n++
                    val w = white()
                    b0 = 0.99765f * b0 + w * 0.0990460f; b1 = 0.96300f * b1 + w * 0.2965164f; b2 = 0.57000f * b2 + w * 1.0526913f
                    val pink = (b0 + b1 + b2 + w * 0.1848f) * 0.16f
                    brown = (brown + 0.02f * w) / 1.02f
                    // 숨을 따르는 크기 (없으면 저절로 천천히)
                    val free = (0.5 - 0.5 * kotlin.math.cos(two * t / T.waveSeconds)).toFloat()
                    val target = if (breath >= 0f) breath else free
                    smooth += (target - smooth) * 0.0004f
                    val v = when (sound) {
                        Sound.WAVES -> brown * 3.2f * (0.25f + 0.75f * smooth) + pink * 0.25f * smooth * smooth
                        Sound.WIND -> {
                            val gust = 0.55f + 0.45f * sin(two * t * 0.07 + sin(two * t * 0.013) * 2).toFloat()
                            lp += (pink - lp) * (0.02f + 0.06f * gust); lp * 2.4f * gust
                        }
                        Sound.RAIN -> {
                            val hp = (w - hpPrev) * 0.5f; hpPrev = w
                            if (white() > 0.9993f) drop = 0.35f + 0.3f * white()
                            drop *= 0.994f
                            hp * 0.22f + pink * 0.35f + drop * w
                        }
                        Sound.TONE -> {
                            val swell = 0.75f + 0.25f * smooth
                            val tone = sin(two * T.toneLow * t) * 0.28 + sin(two * T.toneHigh * t) * 0.28 + sin(two * T.toneLow / 2 * t) * 0.18
                            (tone * swell * (0.85 + 0.15 * sin(two * t * 0.1))).toFloat()
                        }
                        Sound.SEASON -> seasonal(t, w, pink)
                        Sound.NONE -> 0f
                    }
                    gain = if (stopping) (gain - fadeOut).coerceAtLeast(0f) else (gain + fadeIn).coerceAtMost(1f)
                    buf[i] = (v * gain * T.volume * Short.MAX_VALUE).coerceIn(-32767f, 32767f).toInt().toShort()
                }
                track.write(buf, 0, buf.size)
                if (stopping && gain <= 0f) break
            }
            runCatching { track.stop() }; track.release()
        }
    }

/**
     * 명상 종 (싱잉볼) 한 번: 낮은 바탕음에 실제 주발처럼 어긋난 배음 (1 · 2.71 · 5.15 · 8.1 배),
     * 배음마다 아주 가까운 두 음이 맞물려 천천히 일렁이며 (beat), 부드러운 채로 쳐서 오래 잦아듦. hz = 바탕음, strikes = 치는 횟수.
     */
    fun bowl(hz: Double, strikes: Int = 1) {
        Thread({
            runCatching {
                val sr = T.sampleRate.toInt()
                val ring = T.bowlRing.toDouble(); val gapS = 1.6
                val lenS = ring + gapS * (strikes - 1)
                val mix = FloatArray((sr * lenS).toInt())
                val partials = listOf(1.0 to 1.0, 2.71 to 0.42, 5.15 to 0.16, 8.1 to 0.06)
                val decay = listOf(0.42, 0.75, 1.4, 2.6)
                val attack = T.bowlAttackMs / 1000.0; val beat = T.bowlBeat.toDouble()
                for (k in 0 until strikes) {
                    val start = (k * gapS * sr).toInt(); val f0 = hz * (if (k == 0) 1.0 else 0.89)
                    for (i in start until mix.size) {
                        val t = (i - start).toDouble() / sr
                        val a = if (t < attack) sin(PI / 2 * t / attack) else 1.0
                        var v = 0.0
                        partials.forEachIndexed { j, (r, amp) ->
                            val f = f0 * r; val d = beat * (j + 1) * 0.5
                            v += amp * exp(-decay[j] * t) * 0.5 * (sin(2 * PI * (f - d / 2) * t) + sin(2 * PI * (f + d / 2) * t + j))
                        }
                        mix[i] += (v * a).toFloat()
                    }
                }
                val peak = mix.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1f)
                val buf = ShortArray(mix.size) { (mix[it] / peak * T.bowl * 0.8 * Short.MAX_VALUE).toInt().toShort() }
                val track = AudioTrack.Builder().setAudioAttributes(attributes()).setAudioFormat(format(sr))
                    .setBufferSizeInBytes(buf.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
                track.write(buf, 0, buf.size); track.play()
                Thread.sleep((lenS * 1000).toLong() + 200)
                runCatching { track.stop() }; track.release()
            }
        }, "bowl").apply { isDaemon = true; start() }
    }

    /** 작은 종소리 (시작 한 번, 끝 두 번). strikes = 치는 횟수. */
    fun chime(strikes: Int = 1) {
        Thread({
            runCatching {
                val sr = T.sampleRate.toInt()
                val gapS = 0.7; val lenS = 2.6 + gapS * (strikes - 1)
                val buf = ShortArray((sr * lenS).toInt())
                val partials = listOf(1.0 to 1.0, 2.0 to 0.45, 2.76 to 0.32, 5.4 to 0.12)
                val decay = listOf(1.1, 1.7, 2.4, 4.0)
                for (k in 0 until strikes) {
                    val start = (k * gapS * sr).toInt(); val f0 = T.chimeHz * (if (k == 0) 1.0 else 0.75)
                    for (i in start until buf.size) {
                        val t = (i - start).toDouble() / sr
                        val attack = minOf(1.0, t / 0.006)
                        var v = 0.0
                        partials.forEachIndexed { j, (r, a) -> v += a * sin(2 * PI * f0 * r * t) * exp(-decay[j] * t) }
                        val s = buf[i] + (v * attack * T.chime * 0.45 * Short.MAX_VALUE)
                        buf[i] = s.coerceIn(-32767.0, 32767.0).toInt().toShort()
                    }
                }
                val track = AudioTrack.Builder().setAudioAttributes(attributes()).setAudioFormat(format(sr))
                    .setBufferSizeInBytes(buf.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
                track.write(buf, 0, buf.size); track.play()
                Thread.sleep((lenS * 1000).toLong() + 200)
                runCatching { track.stop() }; track.release()
            }
        }, "chime").apply { isDaemon = true; start() }
    }
}
