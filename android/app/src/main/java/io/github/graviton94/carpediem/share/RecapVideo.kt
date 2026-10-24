package io.github.graviton94.carpediem.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Memories
import io.github.graviton94.carpediem.core.Recap
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.sound.StoneSong
import io.github.graviton94.carpediem.ui.garden.HaruArt
import kotlinx.coroutines.ensureActive
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * 달 · 해 영상 (1.1.12): 폰 안에서 만든다 (서버 없음). H.264 1080 × 1920 · 30fps (안 되는 폰은 720 × 1280) + AAC 소리, MP4.
 * 소리는 앱이 만드는 돌 노래: 꽃 (별) 이 필 때마다 그날 마음의 음을 그 계절 음색으로, 밑에 아주 낮은 바탕.
 * 장면: 질문 → (달) 날마다 피는 정원 · (해) 열두 달이 차오름 → 고른 날들 → 카드 → 하루의 인사. 장면 사이는 바탕으로 천천히 스러졌다 떠오름.
 */
object RecapVideo {
    enum class Kind { INTRO, BLOOM, MONTHS, DAY, CARD, OUTRO }
    class Scene(val kind: Kind, val sec: Float, val pick: Recap.Pick? = null)

    private const val FPS = 30
    private const val FADE = 0.7f
    private const val DAY_SEC = 4.5f
    private const val SR = 44100

    fun scenes(month: Boolean, picks: List<Recap.Pick>): List<Scene> =
        if (month) listOf(Scene(Kind.INTRO, 3f), Scene(Kind.BLOOM, 7f)) + picks.map { Scene(Kind.DAY, DAY_SEC, it) } + listOf(Scene(Kind.CARD, 4f), Scene(Kind.OUTRO, 2.5f))
        else listOf(Scene(Kind.INTRO, 4f), Scene(Kind.MONTHS, 8f)) + picks.map { Scene(Kind.DAY, DAY_SEC, it) } + listOf(Scene(Kind.CARD, 6f), Scene(Kind.OUTRO, 4f))

    fun seconds(month: Boolean, n: Int): Int = Math.round(scenes(month, emptyList()).sumOf { it.sec.toDouble() } + n * DAY_SEC).toInt()

    /** 장면 하나의 정지된 그림 (투명 바탕 위, 글 · 사진 · 하루). 움직이는 장면 (BLOOM · MONTHS) 은 null. */
    private fun still(a: RecapCards.Art, s: Scene, card: Bitmap): Bitmap? {
        val W = RecapCards.W; val H = RecapCards.H; val pad = RecapCards.pad(); val inner = (W - pad * 2).toInt()
        val S = Tokens.Garden.Share
        if (s.kind == Kind.CARD) return card
        if (s.kind == Kind.BLOOM || s.kind == Kind.MONTHS) return null
        val b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val c = Canvas(b)
        when (s.kind) {
            Kind.INTRO -> {
                val t = a.ctx.getString(R.string.recap_intro, a.period); val p = ShareCards.paint(a.ctx, S.text * 1.15f, a.fg)
                ShareCards.text(c, t, p, pad, H * 0.42f - ShareCards.measure(t, p, inner) / 2f, inner)
            }
            Kind.OUTRO -> {
                val t = a.ctx.getString(R.string.recap_outro, a.period); val p = ShareCards.paint(a.ctx, S.text, a.fg)
                ShareCards.text(c, t, p, pad, H * 0.38f - ShareCards.measure(t, p, inner) / 2f, inner)
                ShareCards.stoneAt(c, HaruArt.of(a.seed, false), W / 2f, H * 0.66f, W * 0.2f, io.github.graviton94.carpediem.core.Feeling.JOY)
            }
            else -> {
                val p = s.pick ?: return b
                var y = pad * 1.4f
                a.photos[p.date]?.let { y = RecapCards.photos(c, listOf(it), y, scale = 1.25f) + pad * 0.8f } ?: run { y = H * 0.3f }
                RecapCards.quote(c, a, p, y, S.text * 0.85f)
            }
        }
        return b
    }

    private fun fade(t: Float, len: Float, inOnly: Boolean = false): Float =
        minOf(1f, t / FADE, if (inOnly) 1f else (len - t) / FADE).coerceIn(0f, 1f)

    /** 만들기. progress = 0..1. 만든 MP4 파일 (캐시의 share 폴더). 취소하면 지우고 CancellationException. */
    suspend fun make(a: RecapCards.Art, card: Bitmap, scenes: List<Scene>, name: String, progress: (Float) -> Unit): File {
        val dir = File(a.ctx.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.filter { it.name.endsWith(".mp4") }?.forEach { it.delete() }
        val out = File(dir, "$name.mp4")
        val (ew, eh) = size()
        val total = scenes.sumOf { (it.sec * FPS).toInt() }
        // 소리 먼저 (짧아서 한 번에): AAC 조각들
        val audio = encodeAudio(sound(a, scenes))
        coroutineContext.ensureActive()
        val fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, ew, eh).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, if (ew >= 1080) 4_000_000 else 2_500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var ok = false
        try {
            codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); codec.start()
            val info = MediaCodec.BufferInfo()
            var vTrack = -1; var started = false; var outDone = false
            fun drain(wait: Long) {
                while (!outDone) {
                    val o = codec.dequeueOutputBuffer(info, wait)
                    when {
                        o == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                        o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            vTrack = muxer.addTrack(codec.outputFormat)
                            val aTrack = audio?.let { muxer.addTrack(it.first) }
                            muxer.start(); started = true
                            if (aTrack != null && audio != null) audio.second.forEach { (buf, bi) -> muxer.writeSampleData(aTrack, buf, bi) }
                        }
                        o >= 0 -> {
                            val buf = codec.getOutputBuffer(o)!!
                            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                            if (info.size > 0 && started) { buf.position(info.offset); buf.limit(info.offset + info.size); muxer.writeSampleData(vTrack, buf, info) }
                            codec.releaseOutputBuffer(o, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outDone = true
                        }
                    }
                }
            }
            // 장면마다 정지 그림 · 바탕 (한 번만 그림)
            val bg = Bitmap.createBitmap(RecapCards.W, RecapCards.H, Bitmap.Config.ARGB_8888).also { RecapCards.background(Canvas(it), a); RecapCards.frame(Canvas(it), a) }
            val stills = HashMap<Int, Bitmap?>()
            val frame = Bitmap.createBitmap(RecapCards.W, RecapCards.H, Bitmap.Config.ARGB_8888); val fc = Canvas(frame)
            val small = if (ew != RecapCards.W) Bitmap.createBitmap(ew, eh, Bitmap.Config.ARGB_8888) else null
            val yuv = Yuv(ew, eh)
            var lastKey = ""
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            // 해: 열두 칸 자리와 칸 그림 (MONTHS 장면)
            val pad = RecapCards.pad()
            val cells by lazy { RecapCards.grid(pad * 2.6f, RecapCards.H - pad * 2.2f) }
            val tiles by lazy { cells.mapIndexed { i, r -> Bitmap.createBitmap(RecapCards.W, RecapCards.H, Bitmap.Config.ARGB_8888).also { RecapCards.tile(Canvas(it), a, i + 1, r[0], r[1], r[2], r[3]) } } }
            val head by lazy {
                Bitmap.createBitmap(RecapCards.W, RecapCards.H, Bitmap.Config.ARGB_8888).also {
                    ShareCards.text(Canvas(it), a.title, ShareCards.paint(a.ctx, Tokens.Garden.Share.text * 1.05f, a.fg), pad, pad * 1.2f, (RecapCards.W - pad * 2).toInt())
                }
            }
            var n = 0
            scenes.forEachIndexed { si, s ->
                val frames = (s.sec * FPS).toInt()
                for (f in 0 until frames) {
                    coroutineContext.ensureActive()
                    val t = f / FPS.toFloat()
                    val al = fade(t, s.sec, inOnly = s.kind == Kind.OUTRO)
                    val q = (al * 40).toInt()
                    val key: String
                    when (s.kind) {
                        Kind.BLOOM -> {
                            val k = ((t / (s.sec - FADE * 1.5f)).coerceIn(0f, 1f) * a.dots.size).toInt()
                            key = "$si:$q:$k"
                            if (key != lastKey) {
                                fc.drawBitmap(bg, 0f, 0f, null)
                                val lay = fc.saveLayerAlpha(0f, 0f, RecapCards.W.toFloat(), RecapCards.H.toFloat(), (al * 255).toInt())
                                fc.drawBitmap(head, 0f, 0f, null)
                                RecapCards.garden(fc, a, RectF(pad * 0.6f, pad * 2.6f, RecapCards.W - pad * 0.6f, RecapCards.H - pad * 2.2f), k)
                                fc.restoreToCount(lay)
                            }
                        }
                        Kind.MONTHS -> {
                            val shown = ((t / (s.sec - FADE * 1.5f)).coerceIn(0f, 1f) * 12f)
                            key = "$si:$q:${(shown * 8).toInt()}"
                            if (key != lastKey) {
                                fc.drawBitmap(bg, 0f, 0f, null)
                                val lay = fc.saveLayerAlpha(0f, 0f, RecapCards.W.toFloat(), RecapCards.H.toFloat(), (al * 255).toInt())
                                fc.drawBitmap(head, 0f, 0f, null)
                                tiles.forEachIndexed { i, tb -> val e = (shown - i).coerceIn(0f, 1f); if (e > 0f) fc.drawBitmap(tb, 0f, 0f, Paint().apply { alpha = (e * 255).toInt() }) }
                                fc.restoreToCount(lay)
                            }
                        }
                        else -> {
                            key = "$si:$q"
                            if (key != lastKey) {
                                val st = stills.getOrPut(si) { still(a, s, card) }
                                fc.drawBitmap(bg, 0f, 0f, null)
                                st?.let { fc.drawBitmap(it, 0f, 0f, Paint().apply { alpha = (al * 255).toInt() }) }
                            }
                        }
                    }
                    if (key != lastKey) {
                        val src = small?.also { Canvas(it).drawBitmap(frame, null, RectF(0f, 0f, ew.toFloat(), eh.toFloat()), paint) } ?: frame
                        yuv.from(src); lastKey = key
                    }
                    // 넣기 (자리가 날 때까지 나온 것을 빼며)
                    var idx: Int
                    while (true) { idx = codec.dequeueInputBuffer(10_000); if (idx >= 0) break; drain(0) }
                    val img = codec.getInputImage(idx)!!
                    yuv.into(img)
                    codec.queueInputBuffer(idx, 0, ew * eh * 3 / 2, n * 1_000_000L / FPS, 0)
                    n++; drain(0)
                    if (n % 6 == 0) progress(n / total.toFloat())
                }
                if (s.kind != Kind.CARD) stills.remove(si)?.recycle()
            }
            var idx: Int
            while (true) { idx = codec.dequeueInputBuffer(10_000); if (idx >= 0) break; drain(0) }
            codec.queueInputBuffer(idx, 0, 0, n * 1_000_000L / FPS, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            while (!outDone) { coroutineContext.ensureActive(); drain(10_000) }
            progress(1f)
            ok = true
        } finally {
            runCatching { codec.stop() }; runCatching { codec.release() }
            runCatching { muxer.stop() }; runCatching { muxer.release() }
            if (!ok) out.delete()
        }
        return out
    }

    /** 이 폰의 H.264 인코더가 세로 1080 × 1920 을 받으면 그대로, 아니면 720 × 1280. */
    private fun size(): Pair<Int, Int> {
        val ok = runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { ci ->
                ci.isEncoder && ci.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, true) } &&
                    ci.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).videoCapabilities?.isSizeSupported(1080, 1920) == true
            }
        }.getOrDefault(false)
        return if (ok) 1080 to 1920 else 720 to 1280
    }

    /** 그림 → YUV 4:2:0 (BT.601, 좁은 범위). 같은 그림이면 다시 바꾸지 않고 넣기만. */
    private class Yuv(val w: Int, val h: Int) {
        val px = IntArray(w * h); val y = ByteArray(w * h); val u = ByteArray(w * h / 4); val v = ByteArray(w * h / 4)
        fun from(b: Bitmap) {
            b.getPixels(px, 0, w, 0, 0, w, h)
            val cw = w / 2
            for (j in 0 until h) {
                val row = j * w
                for (i in 0 until w) {
                    val c = px[row + i]; val r = (c shr 16) and 255; val g = (c shr 8) and 255; val bl = c and 255
                    y[row + i] = (((66 * r + 129 * g + 25 * bl + 128) shr 8) + 16).toByte()
                    if (j and 1 == 0 && i and 1 == 0) {
                        val k = (j / 2) * cw + i / 2
                        u[k] = (((-38 * r - 74 * g + 112 * bl + 128) shr 8) + 128).toByte()
                        v[k] = (((112 * r - 94 * g - 18 * bl + 128) shr 8) + 128).toByte()
                    }
                }
            }
        }
        fun into(img: android.media.Image) {
            val pl = img.planes
            put(pl[0], y, w, h)
            put(pl[1], u, w / 2, h / 2)
            put(pl[2], v, w / 2, h / 2)
        }
        private fun put(p: android.media.Image.Plane, src: ByteArray, pw: Int, ph: Int) {
            val buf: ByteBuffer = p.buffer; val rs = p.rowStride; val ps = p.pixelStride
            if (ps == 1) {
                if (rs == pw) { buf.position(0); buf.put(src, 0, pw * ph) }
                else for (j in 0 until ph) { buf.position(j * rs); buf.put(src, j * pw, pw) }
            } else for (j in 0 until ph) { val base = j * rs; val s0 = j * pw; for (i in 0 until pw) buf.put(base + i * ps, src[s0 + i]) }
        }
    }

    /** 소리: 장면에 맞춘 돌 노래 음과 낮은 바탕. 모노 PCM (−1..1). */
    private fun sound(a: RecapCards.Art, scenes: List<Scene>): FloatArray {
        val totalSec = scenes.sumOf { it.sec.toDouble() } + 0.2
        val mix = FloatArray((totalSec * SR).toInt())
        val season = Memories.seasonOf(java.time.LocalDate.of(a.year, a.month ?: a.today.monthValue.takeIf { a.year == a.today.year } ?: 12, 1))
        fun tone(s: io.github.graviton94.carpediem.core.Season) = StoneSong.toneOf(null, 12, s)
        fun hit(sec: Float, hz: Double, gain: Float, s: io.github.graviton94.carpediem.core.Season = season) = StoneSong.strike(mix, SR, (sec * SR).toInt(), hz, tone(s), gain)
        val me = StoneSong.pitch(a.seed, true)
        var t0 = 0f
        scenes.forEach { s ->
            when (s.kind) {
                Kind.INTRO -> { hit(t0 + 0.4f, me, 0.6f); hit(t0 + 1.3f, StoneSong.shifted(me, 2), 0.45f) }
                Kind.BLOOM -> {
                    val grow = s.sec - FADE * 1.5f
                    a.dots.forEachIndexed { i, d -> d.line?.let { l -> hit(t0 + grow * (i + 1) / a.dots.size.coerceAtLeast(1), StoneSong.feelingPitch(l.feeling), 0.32f) } }
                }
                Kind.MONTHS -> {
                    val grow = s.sec - FADE * 1.5f
                    (1..12).forEach { m ->
                        val ls = a.byMonth[m]?.mapNotNull { it.second }.orEmpty()
                        if (ls.isNotEmpty()) hit(t0 + grow * (m - 0.5f) / 12f, StoneSong.feelingPitch(Recap.mood(ls)), 0.5f, Memories.seasonOf(java.time.LocalDate.of(a.year, m, 1)))
                    }
                }
                Kind.DAY -> s.pick?.let { p ->
                    val ps = Memories.seasonOf(p.date); val f = StoneSong.feelingPitch(p.line.feeling)
                    hit(t0 + 0.4f, f, 0.6f, ps)
                    if (p.special != null) hit(t0 + 0.75f, StoneSong.shifted(f, 2), 0.4f, ps)
                }
                Kind.CARD -> listOf(4, 6, 7).forEachIndexed { i, k -> hit(t0 + 0.3f + i * 0.18f, StoneSong.shifted(523.25, k - 5), 0.4f) }
                Kind.OUTRO -> { hit(t0 + 0.4f, me, 0.55f); hit(t0 + 1.2f, StoneSong.shifted(me, -1), 0.45f) }
            }
            t0 += s.sec
        }
        // 종소리는 한데 모아 크기를 맞추고, 그 밑에 아주 낮은 바탕 (두 음의 느린 울림 + 바람 같은 잡음)
        val peak = mix.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-3f)
        val k = 0.62f / peak
        var lp = 0f; var seed = 0x2545F491
        val root = 130.81
        for (i in mix.indices) {
            val t = i / SR.toDouble()
            val swell = 0.5 + 0.5 * sin(2 * PI * t / 9.0)
            val drone = (sin(2 * PI * root * t) * 0.6 + sin(2 * PI * root * 1.5 * t) * 0.4) * (0.035 + 0.02 * swell)
            seed = seed xor (seed shl 13); seed = seed xor (seed ushr 17); seed = seed xor (seed shl 5)
            lp += ((seed % 10000) / 10000f - lp) * 0.02f
            val edge = minOf(1.0, t / 1.5, (mix.size / SR.toDouble() - t) / 1.5).coerceAtLeast(0.0)
            mix[i] = ((mix[i] * k + drone + lp * 0.05f) * edge).toFloat().coerceIn(-1f, 1f)
        }
        return mix
    }

    /** PCM → AAC 조각들 (형식, [조각, 정보]). 소리 인코더가 없으면 null (영상만). */
    private fun encodeAudio(pcm: FloatArray): Pair<MediaFormat, List<Pair<ByteBuffer, MediaCodec.BufferInfo>>>? = runCatching {
        val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SR, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 96_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val chunks = ArrayList<Pair<ByteBuffer, MediaCodec.BufferInfo>>()
        var outFmt: MediaFormat? = null
        try {
            codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); codec.start()
            val info = MediaCodec.BufferInfo()
            var pos = 0; var inDone = false; var outDone = false
            while (!outDone) {
                if (!inDone) {
                    val i = codec.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val buf = codec.getInputBuffer(i)!!; buf.clear()
                        val n = minOf((buf.capacity() / 2).coerceAtMost(4096), pcm.size - pos)
                        if (n <= 0) { codec.queueInputBuffer(i, 0, 0, pos * 1_000_000L / SR, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inDone = true }
                        else {
                            val sb = buf.order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            for (k in 0 until n) sb.put((pcm[pos + k] * Short.MAX_VALUE).toInt().toShort())
                            codec.queueInputBuffer(i, 0, n * 2, pos * 1_000_000L / SR, 0); pos += n
                        }
                    }
                }
                val o = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outFmt = codec.outputFormat
                    o >= 0 -> {
                        val buf = codec.getOutputBuffer(o)!!
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                            val copy = ByteBuffer.allocateDirect(info.size); buf.position(info.offset); buf.limit(info.offset + info.size); copy.put(buf); copy.flip()
                            chunks += copy to MediaCodec.BufferInfo().apply { set(0, info.size, info.presentationTimeUs, info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM.inv()) }
                        }
                        codec.releaseOutputBuffer(o, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outDone = true
                    }
                }
            }
        } finally { runCatching { codec.stop() }; codec.release() }
        outFmt?.let { it to chunks }
    }.getOrNull()
}
