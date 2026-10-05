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
    /** gaze = 돌멍하기: 바탕 소리 위에 계절 한 겹 (가끔 새 · 풀벌레 · 마른 잎 · 눈 밟기, 늘 연못 물소리 또는 장작 소리) 을 아주 작게 (E2). */
    class Player(private val sound: Sound, private val season: Season = Season.SPRING, private val gaze: Boolean = false) {
        @Volatile var breath: Float = -1f
        @Volatile private var stopping = false
        private var thread: Thread? = null

        /** 껐다 다시 켤 수 있음 (앱을 떠났다 돌아와 이어 할 때). 앞의 소리는 세대가 바뀌면 스르르 꺼짐. */
        @Volatile private var gen = 0
        fun start() {
            if (sound == Sound.NONE) return
            if (thread?.isAlive == true && !stopping) return
            val my = ++gen; stopping = false
            thread = Thread({ runCatching { play(my) } }, "soundscape").apply { isDaemon = true; start() }
        }

        fun stop() { stopping = true }

        private fun play(my: Int) {
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
            // 돌멍하기의 계절 한 겹 (E2): 가끔 오는 계절 소리 하나 + 연못 (봄 · 여름) 또는 화톳불 (가을 · 겨울)
            val pond = season == Season.SPRING || season == Season.SUMMER
            fun r01() = (white() + 1f) / 2f
            var evLeft = 0; var evLen = 1; var nextEv = (sr * 8).toLong(); var evPh = 0.0
            var lp2 = 0f; var tr = 0f; var plop = 0; var plopLen = 1; var plopPh = 0.0; var nextPlop = (sr * 4).toLong(); var crack2 = 0f; var hpC2 = 0f
            fun layer(t: Double, w: Float, pink: Float): Float {
                var out = 0f
                if (evLeft <= 0 && n >= nextEv) {
                    evLen = (sr * when (season) { Season.SPRING -> 1.6; Season.SUMMER -> 3.2; Season.AUTUMN -> 1.8; Season.WINTER -> 2.2 }).toInt(); evLeft = evLen; evPh = 0.0
                    nextEv = n + (sr * (T.layerMin + (T.layerMax - T.layerMin) * r01())).toLong()
                }
                if (evLeft > 0) {
                    val k = 1.0 - evLeft.toDouble() / evLen; val env = sin(PI * k).toFloat()
                    out += when (season) {
                        // 봄: 먼 새가 세 음
                        Season.SPRING -> { val note = (k * 3).toInt(); val kk = k * 3 - note
                            evPh += two * (3000.0 + 500.0 * note + 600.0 * kk) / sr
                            if (kk < 0.55) (sin(evPh) * sin(PI * kk / 0.55)).toFloat() * 0.12f else 0f }
                        // 여름: 풀벌레 한 번 낮게 이어짐
                        Season.SUMMER -> { evPh += two * 4600 / sr; val trill = if (sin(two * t * 30) > 0) 1f else 0f; sin(evPh).toFloat() * trill * env * 0.03f }
                        // 가을: 바람이 지날 때 마른 잎 바스락
                        Season.AUTUMN -> { lp2 += (w - lp2) * 0.25f; (w - lp2) * env * env * 0.16f * (0.6f + 0.4f * sin(two * t * 11).toFloat()) }
                        // 겨울: 아주 멀리 천천히 눈 밟는 네 걸음
                        Season.WINTER -> { val step = (k * 4) % 1.0; val e = if (step < 0.25) sin(PI * step / 0.25).toFloat() else 0f; lp2 += (w - lp2) * 0.12f; lp2 * e * 0.5f }
                    }
                    evLeft--
                }
                if (pond) {
                    // 연못: 졸졸 흐르는 물빛 잡음 + 가끔 퐁
                    tr += (pink - tr) * 0.2f; out += (pink - tr) * 0.18f * (0.6f + 0.4f * sin(two * t * 0.21).toFloat())
                    if (plop <= 0 && n >= nextPlop) { plopLen = (sr * 0.09).toInt(); plop = plopLen; plopPh = 0.0; nextPlop = n + (sr * (4 + 7 * r01())).toLong() }
                    if (plop > 0) { val k = 1.0 - plop.toDouble() / plopLen; plopPh += two * (900 - 450 * k) / sr; out += (sin(plopPh) * (1 - k) * (1 - k)).toFloat() * 0.09f; plop-- }
                } else {
                    // 화톳불: 낮게 웅웅 + 가끔 탁
                    if (white() > 0.9996f) crack2 = 0.4f + 0.2f * r01()
                    crack2 *= 0.982f
                    val hp = (w - hpC2) * 0.5f; hpC2 = w
                    out += brown * 1.0f + hp * crack2 * 0.9f
                }
                return out * T.layer
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
                    } + (if (gaze) layer(t, w, pink) else 0f)
                    gain = if (stopping || my != gen) (gain - fadeOut).coerceAtLeast(0f) else (gain + fadeIn).coerceAtMost(1f)
                    buf[i] = (v * gain * T.volume * Short.MAX_VALUE).coerceIn(-32767f, 32767f).toInt().toShort()
                }
                track.write(buf, 0, buf.size)
                if ((stopping || my != gen) && gain <= 0f) break
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
