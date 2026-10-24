package io.github.graviton94.carpediem.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.ConstellationBook
import io.github.graviton94.carpediem.core.DayLine
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.core.Recap
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.garden.HaruArt
import io.github.graviton94.carpediem.ui.garden.MONTH_SIZES
import io.github.graviton94.carpediem.ui.garden.StarGarden
import io.github.graviton94.carpediem.ui.garden.TILE_SIZES
import io.github.graviton94.carpediem.ui.garden.meadow
import io.github.graviton94.carpediem.ui.garden.monthIn
import io.github.graviton94.carpediem.ui.garden.nightSky
import java.time.LocalDate

/**
 * 달 · 해 카드 (1.1.12, 시안 2차): 세로 1080 × 1920, 하루 카드와 같은 틀.
 * 달 = 사진 (없으면 대표 한 줄) → 제목 · 별자리 · 수 → 그 달의 정원 → 가장 많던 마음 → 하루와 인사.
 * 해 = 제목 · 수 → 열두 달 칸 (특별한 날이 있는 달은 작은 동그라미와 이름) → 한 줄 셋 → 하루와 인사.
 * 꽃 · 별은 테두리 안쪽 판에만 (판 가장자리에서 8% 안쪽에 놓여 큰 꽃도 테두리에 닿지 않음). 영상 (RecapVideo) 도 같은 조각을 씀.
 */
object RecapCards {
    const val W = 1080
    const val H = 1920
    private val S = Tokens.Garden.Share
    private val Y = Tokens.Garden.Year.Colors
    private val ink = Tokens.Garden.Colors.ink.toArgb()
    private val SOFT = android.graphics.Color.rgb(80, 70, 58)
    private val ACCENT = android.graphics.Color.rgb(160, 100, 75)
    private val ACCENT_NIGHT = android.graphics.Color.rgb(240, 200, 160)
    /** 한 줄이 이보다 길면 줄여서 (카드 · 영상의 큰 글). */
    private const val LINE_MAX = 90
    private const val YEAR_LINE_MAX = 46
    /** 카드의 꽃 · 별 크기 배율 (판이 앱보다 크게 보이지 않아, 조금 키움. 판 가장자리 8% 안쪽이라 테두리에 닿지 않음). */
    private const val DOT = 1.35f

    /**
     * 카드 · 영상 한 벌의 재료. month = null 이면 한 해. days = 그 달 (또는 그해) 의 모든 날.
     * picks = 실을 날 (달: 사진 · 대표 한 줄 · 영상 장면, 해: 한 줄 셋 또는 영상 장면), photos = 그중 사진이 있는 날의 그림.
     * specials = 해 카드에서 달마다 이름 하나 (특별한 날 · 생일).
     */
    class Art(
        val ctx: Context, val year: Int, val month: Int?, val period: String, val title: String, val sub: String,
        val days: List<Pair<LocalDate, DayLine?>>, val book: ConstellationBook, val install: Long, val seed: Long,
        val night: Boolean, val today: LocalDate, val mood: Feeling?, val moodName: String?,
        val picks: List<Recap.Pick>, val photos: Map<LocalDate, Bitmap>, val specials: Map<Int, String>,
        val dayLabel: (Recap.Pick) -> String,
    ) {
        val fg get() = if (night) Y.plain.toArgb() else ink
        val soft get() = if (night) Y.plain.copy(alpha = 0.82f).toArgb() else SOFT
        val accent get() = if (night) ACCENT_NIGHT else ACCENT
        /** 달마다 그 달의 날 (해 카드 · 영상의 칸). */
        val byMonth: Map<Int, List<Pair<LocalDate, DayLine?>>> by lazy { days.groupBy { it.first.monthValue } }
        val dots: List<StarGarden.Dot> by lazy { if (month != null) StarGarden.month(book, days, install) else emptyList() }
    }

    fun pad() = S.pad

    /** 바탕: 종이 위에 그 달 · 해의 정원 (낮 꽃밭 · 밤 별하늘) 을 가득. */
    fun background(c: Canvas, a: Art) {
        c.drawColor(ShareCards.paper)
        ShareCards.drawCompose(c, 0f, 0f, W.toFloat(), H.toFloat()) { if (a.night) nightSky() else meadow() }
    }

    /** 손으로 그은 듯한 테두리 한 줄. */
    fun frame(c: Canvas, a: Art) {
        val m = S.pad * 0.45f
        c.drawRoundRect(RectF(m, m, W - m, H - m), 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = a.fg; strokeCap = Paint.Cap.ROUND })
    }

    fun short(s: String, max: Int = LINE_MAX): String { val t = s.replace(' ', ' ').replace('\n', ' ').trim(); return if (t.length > max) t.take(max - 1).trimEnd() + "…" else t }

    /** 사진 몇 장을 살짝 기울여 겹쳐 (위 top 부터). 아래 끝 y. */
    fun photos(c: Canvas, list: List<Bitmap>, top: Float, cx: Float = W / 2f, scale: Float = 1f): Float {
        if (list.isEmpty()) return top
        val n = list.size.coerceAtMost(3)
        val pw0 = W * scale * when (n) { 1 -> 0.5f; 2 -> 0.38f; else -> 0.3f }
        val maxH = H * scale * (if (n == 1) 0.26f else 0.2f)
        var bottom = top
        val tilts = floatArrayOf(-5f, 3f, -2f)
        list.take(n).forEachIndexed { i, b ->
            var pw = pw0; var ph = pw * b.height / b.width
            if (ph > maxH) { ph = maxH; pw = ph * b.width / b.height }
            val x = cx + (i - (n - 1) / 2f) * pw0 * (if (n == 2) 1.04f else 0.98f)
            val r = RectF(x - pw / 2f, top, x + pw / 2f, top + ph)
            c.save(); c.rotate(if (n == 1) -3f else tilts[i], r.centerX(), r.centerY())
            c.drawBitmap(b, null, r, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            c.restore()
            bottom = maxOf(bottom, top + ph)
        }
        return bottom
    }

    /** 그 달의 정원을 판 r 안에 (판 비율 그대로, 가운데). upto = 이만큼의 날만 (영상에서 날마다 피어남). */
    fun garden(c: Canvas, a: Art, r: RectF, upto: Int = Int.MAX_VALUE) {
        val aspect = Tokens.Garden.Year.monthAspect
        val gw = minOf(r.width(), r.height() * aspect); val gh = gw / aspect
        val gx = r.centerX() - gw / 2f; val gy = r.top + (r.height() - gh) / 2f
        val dots = a.dots.take(upto.coerceAtLeast(0))
        ShareCards.drawCompose(c, gx, gy, gw, gh) { monthIn(androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset.Zero, size), dots, a.night, a.today, floatArrayOf(MONTH_SIZES[0] * DOT, MONTH_SIZES[1] * DOT)) }
    }

    /** 맨 아래: 하루 (가장 많던 마음의 표정) 와 ‘함께해 줘서 고마워요 · 하루’. */
    fun bottom(c: Canvas, a: Art) {
        val gy = H - S.pad * 1.75f
        ShareCards.stoneAt(c, HaruArt.of(a.seed, false), W / 2f, gy, W * 0.13f, a.mood)
        ShareCards.text(c, a.ctx.getString(R.string.recap_thanks), ShareCards.paint(a.ctx, S.small * 0.8f, a.soft), S.pad, gy + S.pad * 0.22f, (W - S.pad * 2).toInt())
    }

    /** 대표 한 줄 (사진이 없을 때 위에 크게): “한 줄” 과 그 아래 날짜 · 마음 (특별한 날이면 그 이름도). 아래 끝 y. */
    fun quote(c: Canvas, a: Art, p: Recap.Pick, top: Float, size: Float = S.text * 0.8f): Float {
        var y = top
        y += ShareCards.text(c, "“${short(p.line.text)}”", ShareCards.paint(a.ctx, size, a.fg), S.pad * 1.2f, y, (W - S.pad * 2.4f).toInt()) + S.pad * 0.25f
        y += ShareCards.text(c, a.dayLabel(p), ShareCards.paint(a.ctx, S.small * 0.9f, if (p.special != null) a.accent else a.soft), S.pad, y, (W - S.pad * 2).toInt())
        return y
    }

    fun month(a: Art): Bitmap {
        val b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val c = Canvas(b)
        val pad = S.pad; val inner = (W - pad * 2).toInt()
        background(c, a)
        var y = pad * 1.1f
        val shots = a.picks.filter { it.photo }.mapNotNull { a.photos[it.date] }.take(3)
        val first = a.picks.firstOrNull()
        y = when {
            shots.isNotEmpty() -> photos(c, shots, y) + pad * 0.7f
            first != null -> quote(c, a, first, pad * 1.5f) + pad * 0.8f
            else -> H * 0.16f
        }
        y += ShareCards.text(c, a.title, ShareCards.paint(a.ctx, S.text * 1.05f, a.fg), pad, y, inner) + pad * 0.1f
        y += ShareCards.text(c, a.sub, ShareCards.paint(a.ctx, S.small, a.soft), pad, y, inner) + pad * 0.3f
        garden(c, a, RectF(pad * 0.6f, y, W - pad * 0.6f, H - pad * 4.1f))
        a.moodName?.let { ShareCards.text(c, a.ctx.getString(R.string.recap_mood, it), ShareCards.paint(a.ctx, S.small * 1.05f, a.soft), pad, H - pad * 3.75f, inner) }
        bottom(c, a); frame(c, a)
        return b
    }

    /** 해 카드의 열두 칸 중 하나 (영상에서도 한 칸씩). */
    fun tile(c: Canvas, a: Art, m: Int, x: Float, y: Float, cw: Float, ch: Float, label: Boolean = true) {
        val ds = a.byMonth[m]
        ShareCards.drawCompose(c, x, y, cw, ch) {
            if (a.night) nightSky() else meadow()
            if (ds != null) monthIn(androidx.compose.ui.geometry.Rect(0f, 0f, cw, ch), StarGarden.month(a.book, ds, a.install), a.night, a.today, TILE_SIZES)
        }
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f; color = a.soft; alpha = 110 }
        c.drawRoundRect(RectF(x, y, x + cw, y + ch), cw * 0.04f, cw * 0.04f, edge)
        val special = a.specials[m]
        if (special != null) c.drawCircle(x + cw - cw * 0.08f, y + cw * 0.08f, cw * 0.035f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = a.accent })
        if (label) {
            val name = java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, a.ctx.resources.configuration.locales[0])
            // 칸 아래 이름: 특별한 날이 있으면 ‘5월 · 첫 출근’, 칸보다 길면 달 이름만 (색으로 표시)
            val lp = ShareCards.paint(a.ctx, S.small * 0.72f, if (special != null) a.accent else a.soft)
            val full = if (special != null) "$name · $special" else name
            ShareCards.text(c, if (lp.measureText(full) <= cw) full else name, lp, x, y + ch + S.small * 0.12f, cw.toInt())
        }
    }

    /** 해 카드의 열두 칸 자리: (x, y, 칸 너비, 칸 높이) 를 달마다. top..bottom 안에 3 × 4. */
    fun grid(top: Float, bottom: Float): List<FloatArray> {
        val pad = S.pad; val gap = pad * 0.28f; val label = S.small * 0.72f * 1.7f
        val aspect = Tokens.Garden.Year.monthAspect
        val cw = minOf((W - pad * 1.6f - gap * 2) / 3f, ((bottom - top - gap * 3) / 4f - label) * aspect).coerceAtLeast(1f)
        val ch = cw / aspect
        val gw = cw * 3 + gap * 2; val gh = (ch + label) * 4 + gap * 3
        val gx = (W - gw) / 2f; val gy = top + ((bottom - top) - gh).coerceAtLeast(0f) / 2f
        return (1..12).map { m -> floatArrayOf(gx + ((m - 1) % 3) * (cw + gap), gy + ((m - 1) / 3) * (ch + label + gap), cw, ch) }
    }

    fun year(a: Art): Bitmap {
        val b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val c = Canvas(b)
        val pad = S.pad; val inner = (W - pad * 2).toInt()
        background(c, a)
        var y = pad * 0.95f
        y += ShareCards.text(c, a.title, ShareCards.paint(a.ctx, S.text * 1.05f, a.fg), pad, y, inner) + pad * 0.1f
        y += ShareCards.text(c, a.sub, ShareCards.paint(a.ctx, S.small * 0.95f, a.soft), pad, y, inner) + pad * 0.35f
        // 한 줄 셋은 먼저 재어 두고 (하루 위), 열두 칸은 남은 자리에
        val lp = ShareCards.paint(a.ctx, S.small * 0.95f, a.fg)
        val lines = a.picks.take(Recap.YEAR_LINES).map { "“${short(it.line.text, YEAR_LINE_MAX)}”" }
        val gapL = pad * 0.12f
        val linesH = lines.sumOf { (ShareCards.measure(it, lp, inner) + gapL).toDouble() }.toFloat()
        val linesTop = H - pad * 2.75f - linesH
        grid(y, linesTop - pad * 0.3f).forEachIndexed { i, r -> tile(c, a, i + 1, r[0], r[1], r[2], r[3]) }
        var ly = linesTop
        lines.forEach { ly += ShareCards.text(c, it, lp, pad, ly, inner) + gapL }
        bottom(c, a); frame(c, a)
        return b
    }
}
