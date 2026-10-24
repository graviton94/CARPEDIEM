package io.github.graviton94.carpediem.share

import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.DayLine
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.core.Person
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.garden.Crayon
import io.github.graviton94.carpediem.ui.garden.HaruArt
import io.github.graviton94.carpediem.ui.garden.birthdayCake
import io.github.graviton94.carpediem.ui.garden.drawHaru
import io.github.graviton94.carpediem.ui.garden.moodColor
import io.github.graviton94.carpediem.ui.garden.MONTH_SIZES
import io.github.graviton94.carpediem.ui.garden.StarGarden
import io.github.graviton94.carpediem.ui.garden.TILE_SIZES
import io.github.graviton94.carpediem.ui.garden.meadow
import io.github.graviton94.carpediem.ui.garden.monthIn
import io.github.graviton94.carpediem.ui.garden.nightSky
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.cos
import kotlin.math.sin

/**
 * 그림 보내기: 한 줄 카드 · 한 해의 정원을 그림 한 장으로 만들어 폰의 ‘보내기’ 창으로 (서버 없음, 앱 밖으로 나가는 것은 그 그림 한 장뿐).
 * 낮의 종이 · 먹선 · 내 하루 · 명조 글자. 크기는 토큰 garden.share.
 */
object ShareCards {
    private val S = Tokens.Garden.Share
    private val paper = Tokens.Garden.Colors.paper.toArgb()
    private val ink = Tokens.Garden.Colors.ink.toArgb()
    private val inkSoft = Tokens.Garden.Colors.inkSoft.toArgb()

    private fun serif(ctx: Context) = ResourcesCompat.getFont(ctx, when (io.github.graviton94.carpediem.data.Words.lang(ctx)) { "ja" -> R.font.notoserifjp_medium; "zh-TW" -> R.font.notoseriftc_medium; else -> R.font.notoserifkr_medium })
    private fun paint(ctx: Context, size: Float, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serif(ctx); textSize = size; this.color = color }

    private fun base(w: Int, h: Int): Pair<Bitmap, Canvas> {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(b)
        c.drawColor(paper)
        // 손으로 그은 듯한 테두리 한 줄
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = ink; strokeCap = Paint.Cap.ROUND }
        val m = S.pad * 0.45f
        c.drawRoundRect(RectF(m, m, w - m, h - m), 48f, 48f, p)
        return b to c
    }

    /** 글 줄 나눔: 안드로이드 13 부터는 낱말 (어절) 단위로 (한국어가 ‘색이 / 라’ 처럼 낱말 가운데서 끊기지 않게). */
    private fun layout(s: String, tp: TextPaint, width: Int, align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER): StaticLayout {
        val b = StaticLayout.Builder.obtain(s, 0, s.length, tp, width).setAlignment(align).setLineSpacing(0f, 1.25f)
        if (android.os.Build.VERSION.SDK_INT >= 33) b.setLineBreakConfig(android.graphics.text.LineBreakConfig.Builder().setLineBreakWordStyle(android.graphics.text.LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE).build())
        return b.build()
    }

    private fun text(c: Canvas, s: String, tp: TextPaint, x: Float, y: Float, width: Int, align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER): Float {
        val l = layout(s, tp, width, align)
        c.save(); c.translate(x, y); l.draw(c); c.restore()
        return l.height.toFloat()
    }

    private fun measure(s: String, tp: TextPaint, width: Int): Float = layout(s, tp, width).height.toFloat()

    private fun haru(c: Canvas, seed: Long, cx: Float, groundY: Float, widthPx: Float, line: Boolean = true) {
        val art = HaruArt.of(seed, false)
        val k = widthPx / art.meta.bbox.width
        c.save(); c.translate(cx - art.meta.bbox.center.x * k, groundY - art.meta.ground * k)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(c),
            androidx.compose.ui.geometry.Size(art.meta.box * k, art.meta.box * k),
        ) { drawHaru(art, k, smile = 1f) }
        c.restore()
        if (!line) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 5f; color = ink; strokeCap = Paint.Cap.ROUND }
        c.drawLine(cx - widthPx * 1.4f, groundY, cx + widthPx * 1.4f, groundY, p)
    }

    /** 오늘의 한 줄 카드: 날짜 · 마음, 한 줄, 그 아래 웃는 하루. */
    fun line(ctx: Context, l: DayLine, feelingName: String?, seed: Long): Bitmap {
        val w = S.lineW.toInt(); val h = S.lineH.toInt(); val pad = S.pad
        val (b, c) = base(w, h)
        val date = l.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        text(c, listOfNotNull(date, feelingName).joinToString(" · "), paint(ctx, S.small, inkSoft), pad, pad * 1.4f, (w - pad * 2).toInt())
        val tp = paint(ctx, S.text, ink)
        val bodyW = (w - pad * 2.4f).toInt()
        val hBody = measure(l.text, tp, bodyW)
        text(c, l.text, tp, pad * 1.2f, h * 0.42f - hBody / 2f, bodyW)
        haru(c, seed, w / 2f, h * 0.8f, w * 0.2f)
        text(c, ctx.getString(R.string.share_footer), paint(ctx, S.small * 0.8f, inkSoft), pad, h - pad * 1.25f, (w - pad * 2).toInt())
        return b
    }

    /**
     * 지난 하루 한 장 (1.1.11, 달의 정원에서 고른 날, 세로 1080 × 1920 · 이야기 크기):
     * 위엔 그 달의 정원 (낮엔 꽃밭 · 밤엔 별자리, 그날이 빛남), 가운데엔 내 하루와 그날 한 줄을 보낸 사람의 돌,
     * 아래엔 날짜 · 마음 · (그날의 질문) · 시간이 묻은 사진 · 한 줄.
     */
    fun day(ctx: Context, book: io.github.graviton94.carpediem.core.ConstellationBook, days: List<Pair<LocalDate, DayLine?>>, install: Long, night: Boolean, today: LocalDate,
            title: String, sub: String, l: DayLine, feelingName: String?, question: String?, photo: Bitmap?, mySeed: Long, to: Person?): Bitmap {
        val w = DAY_W; val h = DAY_H; val pad = S.pad
        val (b, c) = board(w, h, night)
        val fg = if (night) Y.plain.toArgb() else ink; val soft = if (night) Y.plain.copy(alpha = 0.7f).toArgb() else inkSoft
        val inner = (w - pad * 2).toInt()
        // ① 그 달의 정원 (그날을 고른 채로)
        var y = pad * 1.1f
        y += text(c, title, paint(ctx, S.text * 0.8f, fg), pad, y, inner) + pad * 0.1f
        y += text(c, sub, paint(ctx, S.small * 0.9f, soft), pad, y, inner) + pad * 0.25f
        // 아래 (돌 · 날짜 · 질문 · 사진 · 한 줄) 에 쓸 높이를 먼저 재고, 정원 판은 남는 만큼만 (사진이 빠지지 않게, 1.1.11)
        val date0 = l.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        // 사진이 있으면 돌을 조금 작게 (정원 판 · 사진 · 한 줄이 함께 들어가게)
        val stoneW = w * (if (photo != null) DAY_STONE_PHOTO else DAY_STONE)
        val below = stoneW * 0.95f + pad * 0.6f +
            (if (to != null) measure(ctx.getString(R.string.share_dayTo, to.name), paint(ctx, S.small * 0.85f, soft), inner) else 0f) +
            measure(listOfNotNull(date0, feelingName).joinToString(" · "), paint(ctx, S.small, soft), inner) + pad * 0.1f +
            (if (!question.isNullOrBlank()) measure(question, paint(ctx, S.small * 0.88f, soft), inner) + pad * 0.1f else 0f) +
            (if (photo != null) w * DAY_PHOTO_W * photo.height / photo.width + pad * 0.7f else 0f) +
            measure(l.text, paint(ctx, S.text * 0.78f, fg), (w - pad * 2.4f).toInt()) + pad * 2.5f
        val gw = (w - pad * 2f); val gh = minOf(gw / Tokens.Garden.Year.monthAspect, h * 0.3f, (h - y - below).coerceAtLeast(h * 0.12f)); val gwFit = gh * Tokens.Garden.Year.monthAspect
        val dots = io.github.graviton94.carpediem.ui.garden.StarGarden.month(book, days, install)
        drawCompose(c, (w - gwFit) / 2f, y, gwFit, gh) {
            if (night) nightSky() else meadow()
            monthIn(androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset.Zero, size), dots, night, today, MONTH_SIZES, picked = l.date)
        }
        y += gh + pad * 0.5f
        // ② 내 하루와, 그날 한 줄을 보낸 사람의 돌
        val sw = stoneW
        val gy = y + sw * 0.95f
        if (to == null) stoneAt(c, HaruArt.of(mySeed, false), w / 2f, gy, sw)
        else {
            val gap = sw * 0.85f
            stoneAt(c, HaruArt.of(mySeed, false), w / 2f - gap, gy, sw)
            stoneAt(c, HaruArt.of(to.seed, false), w / 2f + gap, gy, sw * (if (to.kind == io.github.graviton94.carpediem.core.Kind.PET) Tokens.Garden.Family.petScale else 1f))
        }
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 5f; color = fg; strokeCap = Paint.Cap.ROUND; alpha = 150 }
        c.drawLine(w / 2f - sw * 2.4f, gy, w / 2f + sw * 2.4f, gy, lp)
        y = gy + pad * 0.15f
        if (to != null) y += text(c, ctx.getString(R.string.share_dayTo, to.name), paint(ctx, S.small * 0.85f, soft), pad, y, inner)
        y += pad * 0.45f
        // ③ 그날: 날짜 · 마음, 질문, 사진, 한 줄
        val date = l.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        y += text(c, listOfNotNull(date, feelingName).joinToString(" · "), paint(ctx, S.small, soft), pad, y, inner) + pad * 0.1f
        if (!question.isNullOrBlank()) y += text(c, question, paint(ctx, S.small * 0.88f, soft), pad, y, inner) + pad * 0.1f
        val foot = h - pad * 1.6f
        val tp = paint(ctx, S.text * 0.78f, fg)
        val bodyW = (w - pad * 2.4f).toInt()
        val hBody = measure(l.text, tp, bodyW)
        // 사진은 남은 자리만큼만 (한 줄이 액자 밖으로 밀려나지 않게, 1.1.11). 자리가 너무 작으면 사진 없이 한 줄만
        val room = foot - y - hBody - pad * 0.9f
        if (photo != null && room >= w * DAY_PHOTO_MIN) {
            val pw = minOf(w * 0.5f, room * photo.width / photo.height, w * DAY_PHOTO_W * 1.3f)
            val ph = pw * photo.height / photo.width
            val tilt = (Math.floorMod(l.date.toEpochDay() * 7919, 70L) - 35) / 10f
            val top = y + pad * 0.2f
            c.save(); c.rotate(tilt, w / 2f, top + ph / 2f)
            c.drawBitmap(photo, null, RectF((w - pw) / 2f, top, (w + pw) / 2f, top + ph), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            c.restore()
            y = top + ph + pad * 0.5f
            text(c, l.text, tp, pad * 1.2f, y, bodyW)
        } else text(c, l.text, tp, pad * 1.2f, (y + foot) / 2f - hBody / 2f, bodyW)
        text(c, ctx.getString(R.string.share_footer), paint(ctx, S.small * 0.8f, soft), pad, h - pad * 1.25f, inner)
        return b
    }
    /** 하루 카드 크기 (이야기 · 상태 공유에 맞는 세로). */
    private const val DAY_W = 1080
    private const val DAY_H = 1920
    /** 하루 카드의 사진이 이보다 작아지면 (폭 비율) 사진을 빼고 한 줄만. */
    private const val DAY_PHOTO_MIN = 0.16f
    /** 하루 카드의 사진 폭 (자리를 잡을 때의 기준, 폭 비율). */
    private const val DAY_PHOTO_W = 0.28f
    /** 하루 카드의 돌 폭 (사진 없을 때 · 있을 때). */
    private const val DAY_STONE = 0.15f
    private const val DAY_STONE_PHOTO = 0.11f

    private fun stoneAt(c: Canvas, a: HaruArt, x: Float, gy: Float, width: Float) {
        val k = width / a.meta.bbox.width
        c.save(); c.translate(x - a.meta.bbox.center.x * k, gy - a.meta.ground * k)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr,
            androidx.compose.ui.graphics.Canvas(c), androidx.compose.ui.geometry.Size(a.meta.box * k, a.meta.box * k)) { drawHaru(a, k, smile = 1f) }
        c.restore()
    }

    /** Compose 그리기를 그림 위 (left, top) 의 w × h 판에 (앱 화면과 같은 그리기). */
    private fun drawCompose(c: Canvas, left: Float, top: Float, w: Float, h: Float, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
        c.save(); c.translate(left, top)
        c.clipPath(android.graphics.Path().apply { addRoundRect(RectF(0f, 0f, w, h), w * 0.04f, w * 0.04f, android.graphics.Path.Direction.CW) })
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr,
            androidx.compose.ui.graphics.Canvas(c), androidx.compose.ui.geometry.Size(w, h), block)
        c.restore()
    }

    /** 밤이면 남색 바탕 · 옅은 크림 글자. */
    private fun board(w: Int, h: Int, night: Boolean): Pair<Bitmap, Canvas> {
        val (b, c) = base(w, h)
        if (night) {
            c.drawColor(Y.skyBottom.toArgb())
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = Y.plain.toArgb(); strokeCap = Paint.Cap.ROUND }
            val m = S.pad * 0.45f; c.drawRoundRect(RectF(m, m, w - m, h - m), 48f, 48f, p)
        }
        return b to c
    }
    private val Y = Tokens.Garden.Year.Colors
    /** 생일 카드의 돌 크기 (예전 대비). */
    private const val CARD_STONE = 0.58f

    /** 한 달의 정원: 그 달의 별자리 (보내는 때가 밤이면 별, 낮이면 꽃), 별자리 이름 · 한 줄 수. */
    fun month(ctx: Context, book: io.github.graviton94.carpediem.core.ConstellationBook, days: List<Pair<LocalDate, DayLine?>>, install: Long, night: Boolean, today: LocalDate, title: String, sub: String): Bitmap {
        val w = S.yearW.toInt(); val h = S.yearH.toInt(); val pad = S.pad
        val (b, c) = board(w, h, night)
        val fg = if (night) Y.plain.toArgb() else ink; val soft = if (night) Y.plain.copy(alpha = 0.7f).toArgb() else inkSoft
        var y = pad * 1.2f
        y += text(c, title, paint(ctx, S.text * 0.9f, fg), pad, y, (w - pad * 2).toInt()) + pad * 0.2f
        y += text(c, sub, paint(ctx, S.small, soft), pad, y, (w - pad * 2).toInt()) + pad * 0.4f
        // 판은 아래 글 · 이름과 겹치지 않는 만큼만 (넘치면 가운데로 줄임)
        val avail = h - pad * 1.8f - y
        val gh = minOf((w - pad * 2) / Tokens.Garden.Year.monthAspect, avail); val gw = gh * Tokens.Garden.Year.monthAspect
        val gx = (w - gw) / 2f
        val dots = StarGarden.month(book, days, install)
        drawCompose(c, gx, y, gw, gh) {
            if (night) nightSky() else meadow()
            monthIn(androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset.Zero, size), dots, night, today, MONTH_SIZES)
        }
        text(c, ctx.getString(R.string.share_footer), paint(ctx, S.small * 0.8f, soft), pad, h - pad * 1.25f, (w - pad * 2).toInt())
        return b
    }

    /** 한 해의 정원: 열두 달의 별자리를 4 × 3 칸에, 한 줄 · 고마움 수, 고마움 한 줄 몇 개. */
    fun year(ctx: Context, book: io.github.graviton94.carpediem.core.ConstellationBook, days: List<Pair<LocalDate, DayLine?>>, install: Long, night: Boolean, today: LocalDate, title: String, count: String): Bitmap {
        val w = S.yearW.toInt(); val h = S.yearH.toInt(); val pad = S.pad
        val (b, c) = board(w, h, night)
        val fg = if (night) Y.plain.toArgb() else ink; val soft = if (night) Y.plain.copy(alpha = 0.7f).toArgb() else inkSoft
        val tw = (w - pad * 2).toInt()
        val titleP = paint(ctx, S.text * 0.9f, fg); val countP = paint(ctx, S.small, soft); val lp = paint(ctx, S.small * 0.75f, soft)
        // 아래 (한 줄 수 · 그해 마음의 빛깔 띠) 는 먼저 재어 두고, 열두 칸은 남은 자리 한가운데에 (글과 그림이 겹치지 않게).
        // 받는 사람이 보는 그림이라 한 줄의 글은 싣지 않음 (1.1.6)
        val footY = h - pad * 1.25f
        val bandH = pad * 0.22f
        val feelings = days.mapNotNull { it.second }.groupingBy { it.feeling }.eachCount().entries.sortedBy { it.key?.ordinal ?: Int.MAX_VALUE }
        val below = measure(count, countP, tw) + (if (feelings.isEmpty()) 0f else pad * 0.3f + bandH)
        var y = pad * 1.2f
        y += text(c, title, titleP, pad, y, tw) + pad * 0.5f
        val gridTop = y; val gridBottom = footY - pad * 0.4f - below - pad * 0.5f
        val gap = pad * 0.18f; val label = lp.textSize * 1.6f
        val cw = minOf((w - pad * 2 - gap * 3) / 4f, ((gridBottom - gridTop - gap * 2) / 3f - label) * Tokens.Garden.Year.monthAspect).coerceAtLeast(1f)
        val ch = cw / Tokens.Garden.Year.monthAspect
        val gw = cw * 4 + gap * 3; val gh = (ch + label) * 3 + gap * 2
        val gx = (w - gw) / 2f; val gy = gridTop + ((gridBottom - gridTop) - gh).coerceAtLeast(0f) / 2f
        val months = days.groupBy { it.first.monthValue }
        val loc = ctx.resources.configuration.locales[0]
        (1..12).forEach { m ->
            val x = gx + ((m - 1) % 4) * (cw + gap); val ty = gy + ((m - 1) / 4) * (ch + label + gap)
            // 한 칸씩 둥근 판: 그 달의 정원 (낮) · 은하수 (밤)
            drawCompose(c, x, ty, cw, ch) {
                if (night) nightSky() else meadow()
                months[m]?.let { ds -> monthIn(androidx.compose.ui.geometry.Rect(0f, 0f, cw, ch), StarGarden.month(book, ds, install), night, today, TILE_SIZES) }
            }
            text(c, java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, loc), lp, x, ty + ch + label * 0.15f, cw.toInt())
        }
        y = footY - pad * 0.4f - below
        y += text(c, count, countP, pad, y, tw) + pad * 0.3f
        // 마음의 빛깔 띠: 그해 보낸 마음의 몫만큼 (가벼운 마음부터 무거운 마음 · 마음 없음 순), 둥근 띠 하나
        val total = feelings.sumOf { it.value }
        if (total > 0) {
            val bw = tw * 0.72f; val bx = (w - bw) / 2f
            c.save(); c.clipPath(android.graphics.Path().apply { addRoundRect(RectF(bx, y, bx + bw, y + bandH), bandH / 2, bandH / 2, android.graphics.Path.Direction.CW) })
            var x = bx; val bp = Paint(Paint.ANTI_ALIAS_FLAG)
            feelings.forEach { (f, n) -> val segW = bw * n / total; bp.color = io.github.graviton94.carpediem.ui.garden.feelingColor(f).toArgb(); c.drawRect(x, y, x + segW + 1f, y + bandH, bp); x += segW }
            c.restore()
        }
        text(c, ctx.getString(R.string.share_footer), paint(ctx, S.small * 0.8f, soft), pad, h - pad * 1.25f, (w - pad * 2).toInt())
        return b
    }

    /**
     * 생일 카드: 나와 그 사람의 돌이 작게 나란히, 사이에 케이크와 작은 하트. 그 사람만 고깔, 둘 다 웃는 눈 · 발그레한 볼.
     * 낮엔 종이에 깃발 줄, 밤 (보내는 때가 어두우면) 엔 남색에 작은 전구 줄과 별, 촛불 빛.
     */
    /** sent = (올해 그 사람에게 보낸 한 줄 수, 고른 한 줄): 있으면 인사말 대신 ‘올해 보낸 마음’ 과 그 한 줄. */
    @Suppress("UNUSED_PARAMETER")
    fun birthday(ctx: Context, name: String, seed: Long, pet: Boolean, mySeed: Long, dark: Boolean, sent: Pair<Int, String>? = null): Bitmap {
        // 생일 카드는 늘 ‘밤의 촛불’: 별 뜬 밤하늘 · 전구 줄 · 촛불 빛 안의 작은 두 돌과 케이크
        val night = true
        val w = S.lineW.toInt(); val h = S.lineH.toInt(); val pad = S.pad
        val (b, c) = board(w, h, night)
        drawCompose(c, 0f, 0f, w.toFloat(), h.toFloat()) {
            drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Y.skyTop, Y.skyBottom)), size = size)
        }
        val fg = if (night) Y.plain.toArgb() else ink; val soft = if (night) Y.plain.copy(alpha = 0.7f).toArgb() else inkSoft
        val P = Tokens.Garden.Party
        // 깃발 줄 · 전구 줄 (위), 밤엔 별 몇 개
        drawCompose(c, 0f, 0f, w.toFloat(), h.toFloat()) {
            val y0 = h * 0.08f; val sag = h * 0.05f; val x1 = w * 0.1f; val x2 = w * 0.9f
            val string = androidx.compose.ui.graphics.Path().apply { moveTo(x1, y0); quadraticTo(w / 2f, y0 + sag * 2, x2, y0) }
            drawPath(string, if (night) Y.plain.copy(alpha = 0.55f) else Tokens.Garden.Colors.ink.copy(alpha = 0.6f), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
            val cols = listOf(Tokens.Garden.Party.Colors.stripe, Tokens.Garden.Party.Colors.flame, Tokens.Garden.Mood.Colors.calm, Tokens.Garden.Mood.Colors.hope, Tokens.Garden.Party.Colors.heart)
            for (k in 1 until 9) {
                val t = k / 9f
                val px = (1 - t) * (1 - t) * x1 + 2 * (1 - t) * t * (w / 2f) + t * t * x2
                val py = (1 - t) * (1 - t) * y0 + 2 * (1 - t) * t * (y0 + sag * 2) + t * t * y0
                if (night) {
                    val o = androidx.compose.ui.geometry.Offset(px, py + w * 0.012f)
                    drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Tokens.Garden.Party.Colors.glow.copy(alpha = 0.45f), androidx.compose.ui.graphics.Color.Transparent), o, w * 0.03f), w * 0.03f, o)
                    drawOval(Tokens.Garden.Party.Colors.flameCore, androidx.compose.ui.geometry.Offset(px - w * 0.007f, py + w * 0.003f), androidx.compose.ui.geometry.Size(w * 0.014f, w * 0.02f))
                } else {
                    val flag = androidx.compose.ui.graphics.Path().apply { moveTo(px - w * 0.022f, py); lineTo(px + w * 0.022f, py); lineTo(px, py + w * 0.045f); close() }
                    drawPath(flag, cols[k % cols.size])
                }
            }
            if (night) { val r = Crayon.Rng(5); repeat(60) { drawCircle(Y.core, 1.2f + 2.2f * r.next(), androidx.compose.ui.geometry.Offset(w * (0.05f + 0.9f * r.next()), h * (0.05f + 0.6f * r.next())), 0.2f + 0.5f * r.next()) } }
        }
        val tp = paint(ctx, S.text, fg)
        text(c, ctx.getString(R.string.bday_cardTitle, name), tp, pad, h * 0.17f, (w - pad * 2).toInt())
        if (sent == null) text(c, ctx.getString(R.string.bday_cardSub), paint(ctx, S.small, soft), pad, h * 0.17f + S.text * 1.8f, (w - pad * 2).toInt())
        else {
            val y1 = h * 0.17f + S.text * 1.8f
            val sh = text(c, ctx.getString(R.string.bday_cardSent, name, "${sent.first}"), paint(ctx, S.small * 0.9f, soft), pad, y1, (w - pad * 2).toInt())
            text(c, "“${sent.second.replace('\n', ' ')}”", paint(ctx, S.small * 1.15f, fg), pad, y1 + sh + S.small * 0.5f, (w - pad * 2).toInt())
        }
        val gy = h * 0.78f; val cx = w / 2f
        val myArt = HaruArt.of(mySeed, false); val art = HaruArt.of(seed, false)
        val sw = w * P.card * CARD_STONE   // 돌은 작게: 글과 밤하늘이 주인공
        fun stone(a: HaruArt, x: Float, width: Float, hat: Boolean) {
            val k = width / a.meta.bbox.width
            c.save(); c.translate(x - a.meta.bbox.center.x * k, gy - a.meta.ground * k)
            androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr,
                androidx.compose.ui.graphics.Canvas(c), androidx.compose.ui.geometry.Size(a.meta.box * k, a.meta.box * k)) { drawHaru(a, k, smile = 1f, blush = 0.8f, hat = hat) }
            c.restore()
        }
        // 촛불 빛: 케이크에서 퍼져 두 돌을 비춤 · 아래는 어두운 땅
        drawCompose(c, 0f, 0f, w.toFloat(), h.toFloat()) {
            val o = androidx.compose.ui.geometry.Offset(cx, gy - sw * 0.4f)
            drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Tokens.Garden.Party.Colors.glow.copy(alpha = 0.34f), androidx.compose.ui.graphics.Color.Transparent), o, sw * 3.6f), sw * 3.6f, o)
            drawRect(Y.skyBottom.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(0f, gy), androidx.compose.ui.geometry.Size(size.width, size.height - gy))
        }
        val gap = sw * 1.2f
        stone(myArt, cx - gap, sw, false)
        stone(art, cx + gap, sw * (if (pet) 0.78f else 1f), true)
        val cw = sw * 0.62f
        c.save(); c.translate(cx - cw / 2, gy - cw + 4f)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr,
            androidx.compose.ui.graphics.Canvas(c), androidx.compose.ui.geometry.Size(cw, cw)) { birthdayCake() }
        c.restore()
        // 작은 하트
        val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Tokens.Garden.Party.Colors.heart.toArgb() }
        val hx = cx; val hy = gy - cw * 1.35f; val hs = sw * 0.07f
        c.drawPath(android.graphics.Path().apply { moveTo(hx, hy + hs); cubicTo(hx - hs * 2f, hy - hs * 0.6f, hx - hs, hy - hs * 2.2f, hx, hy - hs); cubicTo(hx + hs, hy - hs * 2.2f, hx + hs * 2f, hy - hs * 0.6f, hx, hy + hs); close() }, hp)
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 5f; color = fg; strokeCap = Paint.Cap.ROUND }
        lp.alpha = 110; c.drawLine(pad, gy, w - pad, gy, lp)
        text(c, ctx.getString(R.string.share_footer), paint(ctx, S.small * 0.8f, soft), pad, h - pad * 1.25f, (w - pad * 2).toInt())
        return b
    }

    private fun art(ctx: Context, name: String): Bitmap? = runCatching { ctx.assets.open("garden/$name").use { android.graphics.BitmapFactory.decodeStream(it) } }.getOrNull()

    /** 그림 하나를 (cx, bottom) 에 높이 hPx 로 (비율 그대로). */
    private fun put(c: Canvas, b: Bitmap?, cx: Float, bottom: Float, hPx: Float, alpha: Int = 255) {
        b ?: return
        val w = hPx * b.width / b.height
        c.drawBitmap(b, null, RectF(cx - w / 2, bottom - hPx, cx + w / 2, bottom), Paint(Paint.FILTER_BITMAP_FLAG).apply { this.alpha = alpha })
    }

    /**
     * 정원의 한 해 (S2): 테두리 없는 한 장의 포스터. 그 계절 하늘이 화면 가득, 아래쪽에 땅 · 나무 · 말뚝 · 연 · 하루,
     * 그해 받은 계절 조각은 하루 곁 땅 위에 놓임 (받지 못한 계절은 그리지 않음). 위에 큰 해 숫자와 ‘하루의 정원’. 숫자 통계는 쓰지 않음.
     */
    fun gardenYear(ctx: Context, year: Int, cards: List<io.github.graviton94.carpediem.core.SeasonCard>, hang: io.github.graviton94.carpediem.core.Hang,
                   ribbons: List<Feeling?>, season: io.github.graviton94.carpediem.core.Season, seed: Long,
                   tree: io.github.graviton94.carpediem.core.Tree? = null, stage: Int = 2, kite: Boolean = false): Bitmap {
        val w = S.yearW.toInt(); val h = S.yearH.toInt(); val pad = S.pad
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(b)
        c.drawColor(paper)
        val key = season.name.lowercase()
        val f = Paint(Paint.FILTER_BITMAP_FLAG)
        // 하늘: 화면 가득 (위를 맞춰 채움)
        art(ctx, "sky_$key.jpg")?.let { sky -> val sh = w.toFloat() * sky.height / sky.width; c.drawBitmap(sky, null, RectF(0f, 0f, w.toFloat(), maxOf(sh, h.toFloat())), f) }
        // 땅: 앱과 같은 띠, 땅선은 아래 30% 위에. 띠 아래는 끝까지 늘여 채움
        val gy = h * 0.70f
        art(ctx, "strip_$key.webp")?.let { st ->
            val L = Tokens.Garden.Layout; val sh = w * L.stripHeight / Tokens.Garden.unitWidth; val top = gy - sh * L.stripLineY / L.stripHeight
            c.drawBitmap(st, null, RectF(0f, top, w.toFloat(), maxOf(top + sh, h.toFloat())), f)
        }
        // 나무 (왼쪽) · 말뚝과 걸린 것 (오른쪽) · 연과 리본
        val foot = h * 0.012f
        tree?.let { t -> put(c, art(ctx, "tree_${t.key}_${key}_${stage.coerceIn(0, 3)}.webp"), w * 0.15f, gy + foot, h * 0.36f) }
        val postX = w * 0.87f; val postH = h * 0.22f
        put(c, art(ctx, "post_$key.webp"), postX, gy + foot, postH)
        val hangName = when (hang) { io.github.graviton94.carpediem.core.Hang.CHIME -> "post_chime.webp"; io.github.graviton94.carpediem.core.Hang.BELL -> "post_bell.webp"; io.github.graviton94.carpediem.core.Hang.LANTERN -> "post_lantern.webp"; else -> null }
        hangName?.let { put(c, art(ctx, it), postX, gy + foot, postH) }
        if (kite) {
            val kx = w * 0.66f; val ky = h * 0.47f; val kh = h * 0.08f
            val string = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = inkSoft; alpha = 120; strokeWidth = 2.5f; style = Paint.Style.STROKE }
            c.drawPath(android.graphics.Path().apply { moveTo(kx, ky); quadTo(kx + kh * 0.8f, ky + kh * 1.8f, postX - pad * 0.1f, gy - postH * 0.85f) }, string)
            put(c, art(ctx, "kite.webp"), kx, ky, kh)
            val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 6f; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
            ribbons.take(8).forEachIndexed { i, fe ->
                rp.color = io.github.graviton94.carpediem.ui.garden.feelingColor(fe).toArgb()
                val sx = kx + (i - (ribbons.size.coerceAtMost(8) - 1) / 2f) * 7f; val len = kh * (0.6f + 0.08f * (i % 3))
                c.drawPath(android.graphics.Path().apply { moveTo(sx, ky - kh * 0.05f); quadTo(sx - len * 0.35f, ky + len * 0.45f, sx - len * 0.15f + (i % 2) * 8f, ky + len) }, rp)
            }
        }
        // 하루 (가운데) 와 그해 받은 계절 조각 (하루 양옆 땅 위, 계절 순서대로)
        haru(c, seed, w * 0.5f, gy, w * 0.17f, line = false)
        val got = io.github.graviton94.carpediem.core.Season.entries.mapNotNull { s -> cards.firstOrNull { it.season == s } }
        val spots = listOf(0.33f, 0.68f, 0.26f, 0.75f)
        got.take(4).forEachIndexed { i, card -> put(c, art(ctx, "card_${card.key}.webp"), w * spots[i], gy + foot, h * 0.075f) }
        // 글자: 위에 큰 해 숫자 · 이름, 아래 땅 위에 작은 꼬리말 (아래를 살짝 어둡게 해 읽히게)
        val yh = text(c, "$year", paint(ctx, S.text * 2.6f, ink), pad, pad * 1.0f, w - (pad * 2).toInt())
        text(c, ctx.getString(R.string.title_name), paint(ctx, S.small * 1.1f, inkSoft), pad, pad * 1.0f + yh + S.small * 0.2f, w - (pad * 2).toInt())
        c.drawRect(0f, h * 0.84f, w.toFloat(), h.toFloat(), Paint().apply { shader = android.graphics.LinearGradient(0f, h * 0.84f, 0f, h.toFloat(), 0x00000000, 0x33000000, android.graphics.Shader.TileMode.CLAMP) })
        text(c, ctx.getString(R.string.share_footer), paint(ctx, S.small * 0.8f, 0xF2FBF4E6.toInt()), pad, h - pad * 0.95f, w - (pad * 2).toInt())
        return b
    }

    /** 그림을 화면 스레드 밖에서 그리고 저장한 뒤 ‘보내기’ 창을 연다 (느린 폰에서 누른 순간 멈추지 않게). */
    fun send(ctx: Context, name: String, draw: () -> Bitmap) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val f = runCatching { save(ctx, draw(), name) }.getOrNull() ?: return@launch
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { open(ctx, f) }
        }
    }

    /** 이미 그린 그림을 보냄 (저장도 화면 스레드 밖에서). */
    fun send(ctx: Context, bmp: Bitmap, name: String) = send(ctx, name) { bmp }

    private fun save(ctx: Context, bmp: Bitmap, name: String): File {
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "$name.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f
    }

    private fun open(ctx: Context, f: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.share", f)
            val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ctx.startActivity(Intent.createChooser(send, ctx.getString(R.string.share_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            io.github.graviton94.carpediem.data.Store(ctx).sharedOn = java.time.LocalDate.now()   // 그날 정원에 돌아오면 가끔 비눗방울
        }
    }

    fun feelingName(ctx: Context, f: Feeling?) = f?.let { io.github.graviton94.carpediem.ui.Labels.feeling(ctx, it) }
}
