package io.github.graviton94.carpediem.share

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

    private fun serif(ctx: Context) = ResourcesCompat.getFont(ctx, R.font.notoserifkr_medium)
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

    private fun text(c: Canvas, s: String, tp: TextPaint, x: Float, y: Float, width: Int, align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER): Float {
        val l = StaticLayout.Builder.obtain(s, 0, s.length, tp, width).setAlignment(align).setLineSpacing(0f, 1.25f).build()
        c.save(); c.translate(x, y); l.draw(c); c.restore()
        return l.height.toFloat()
    }

    private fun haru(c: Canvas, seed: Long, cx: Float, groundY: Float, widthPx: Float) {
        val art = HaruArt.of(seed, false)
        val k = widthPx / art.meta.bbox.width
        c.save(); c.translate(cx - art.meta.bbox.center.x * k, groundY - art.meta.ground * k)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(c),
            androidx.compose.ui.geometry.Size(art.meta.box * k, art.meta.box * k),
        ) { drawHaru(art, k, smile = 1f) }
        c.restore()
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
        val hBody = StaticLayout.Builder.obtain(l.text, 0, l.text.length, tp, bodyW).setLineSpacing(0f, 1.25f).build().height
        text(c, l.text, tp, pad * 1.2f, h * 0.42f - hBody / 2f, bodyW)
        haru(c, seed, w / 2f, h * 0.8f, w * 0.2f)
        text(c, ctx.getString(R.string.share_footer), paint(ctx, S.small * 0.8f, inkSoft), pad, h - pad * 1.25f, (w - pad * 2).toInt())
        return b
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

    /** 한 달의 정원: 그 달의 별자리 (보내는 때가 밤이면 별, 낮이면 꽃), 별자리 이름 · 한 줄 수. */
    fun month(ctx: Context, book: io.github.graviton94.carpediem.core.ConstellationBook, days: List<Pair<LocalDate, DayLine?>>, night: Boolean, today: LocalDate, title: String, sub: String): Bitmap {
        val w = S.yearW.toInt(); val h = S.yearH.toInt(); val pad = S.pad
        val (b, c) = board(w, h, night)
        val fg = if (night) Y.plain.toArgb() else ink; val soft = if (night) Y.plain.copy(alpha = 0.7f).toArgb() else inkSoft
        var y = pad * 1.2f
        y += text(c, title, paint(ctx, S.text * 0.9f, fg), pad, y, (w - pad * 2).toInt()) + pad * 0.2f
        y += text(c, sub, paint(ctx, S.small, soft), pad, y, (w - pad * 2).toInt()) + pad * 0.4f
        val gw = w - pad * 2; val gh = gw / Tokens.Garden.Year.monthAspect
        val dots = StarGarden.month(book, days)
        val cons = days.firstOrNull()?.first?.monthValue?.let(book::of)
        drawCompose(c, pad, y, gw, gh) {
            if (night) nightSky() else meadow()
            monthIn(androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset.Zero, size), dots, cons, night, today, MONTH_SIZES)
        }
        text(c, ctx.getString(R.string.share_footer), paint(ctx, S.small * 0.8f, soft), pad, h - pad * 1.25f, (w - pad * 2).toInt())
        return b
    }

    /** 한 해의 정원: 열두 달의 별자리를 4 × 3 칸에, 한 줄 · 고마움 수, 고마움 한 줄 몇 개. */
    fun year(ctx: Context, book: io.github.graviton94.carpediem.core.ConstellationBook, days: List<Pair<LocalDate, DayLine?>>, night: Boolean, today: LocalDate, title: String, count: String, thanks: List<String>): Bitmap {
        val w = S.yearW.toInt(); val h = S.yearH.toInt(); val pad = S.pad
        val (b, c) = board(w, h, night)
        val fg = if (night) Y.plain.toArgb() else ink; val soft = if (night) Y.plain.copy(alpha = 0.7f).toArgb() else inkSoft
        var y = pad * 1.2f
        y += text(c, title, paint(ctx, S.text * 0.9f, fg), pad, y, (w - pad * 2).toInt()) + pad * 0.4f
        val gw = w - pad * 2; val cw = gw / 4f; val ch = cw / Tokens.Garden.Year.monthAspect; val label = S.small * 1.3f
        val months = days.groupBy { it.first.monthValue }
        drawCompose(c, pad, y, gw, (ch + label) * 3) {
            if (night) nightSky() else meadow()
            months.forEach { (m, ds) ->
                val r = androidx.compose.ui.geometry.Rect(((m - 1) % 4) * cw, ((m - 1) / 4) * (ch + label), ((m - 1) % 4 + 1) * cw, ((m - 1) / 4) * (ch + label) + ch)
                monthIn(r, StarGarden.month(book, ds), book.of(m), night, today, TILE_SIZES)
            }
        }
        val lp = paint(ctx, S.small * 0.75f, soft)
        (1..12).forEach { m -> text(c, java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, ctx.resources.configuration.locales[0]), lp, pad + ((m - 1) % 4) * cw, y + ((m - 1) / 4) * (ch + label) + ch, cw.toInt()) }
        y += (ch + label) * 3 + pad * 0.4f
        y += text(c, count, paint(ctx, S.small, soft), pad, y, (w - pad * 2).toInt()) + pad * 0.3f
        thanks.forEach { t -> y += text(c, "“$t”", paint(ctx, S.small * 1.05f, fg), pad, y, (w - pad * 2).toInt()) + pad * 0.15f }
        text(c, ctx.getString(R.string.share_footer), paint(ctx, S.small * 0.8f, soft), pad, h - pad * 1.25f, (w - pad * 2).toInt())
        return b
    }

    /**
     * 생일 카드: 나와 그 사람의 돌이 작게 나란히, 사이에 케이크와 작은 하트. 그 사람만 고깔, 둘 다 웃는 눈 · 발그레한 볼.
     * 낮엔 종이에 깃발 줄, 밤 (보내는 때가 어두우면) 엔 남색에 작은 전구 줄과 별, 촛불 빛.
     */
    fun birthday(ctx: Context, name: String, seed: Long, pet: Boolean, mySeed: Long, night: Boolean): Bitmap {
        val w = S.lineW.toInt(); val h = S.lineH.toInt(); val pad = S.pad
        val (b, c) = board(w, h, night)
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
            if (night) { val r = Crayon.Rng(5); repeat(18) { drawCircle(Y.core, 1.5f + 2f * r.next(), androidx.compose.ui.geometry.Offset(w * (0.08f + 0.84f * r.next()), h * (0.32f + 0.25f * r.next())), 0.2f + 0.4f * r.next()) } }
        }
        val tp = paint(ctx, S.text, fg)
        text(c, ctx.getString(R.string.bday_cardTitle, name), tp, pad, h * 0.17f, (w - pad * 2).toInt())
        text(c, ctx.getString(R.string.bday_cardSub), paint(ctx, S.small, soft), pad, h * 0.17f + S.text * 1.8f, (w - pad * 2).toInt())
        val gy = h * 0.72f; val cx = w / 2f
        val myArt = HaruArt.of(mySeed, false); val art = HaruArt.of(seed, false)
        val sw = w * P.card
        fun stone(a: HaruArt, x: Float, width: Float, hat: Boolean) {
            val k = width / a.meta.bbox.width
            c.save(); c.translate(x - a.meta.bbox.center.x * k, gy - a.meta.ground * k)
            androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr,
                androidx.compose.ui.graphics.Canvas(c), androidx.compose.ui.geometry.Size(a.meta.box * k, a.meta.box * k)) { drawHaru(a, k, smile = 1f, blush = 0.8f, hat = hat) }
            c.restore()
        }
        // 촛불 빛 (밤): 두 돌을 비춤
        if (night) drawCompose(c, 0f, 0f, w.toFloat(), h.toFloat()) {
            val o = androidx.compose.ui.geometry.Offset(cx, gy - sw * 0.5f)
            drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Tokens.Garden.Party.Colors.glow.copy(alpha = 0.22f), androidx.compose.ui.graphics.Color.Transparent), o, sw * 1.6f), sw * 1.6f, o)
        }
        val gap = sw * 0.95f
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
        c.drawLine(cx - sw * 2.1f, gy, cx + sw * 2.1f, gy, lp)
        text(c, ctx.getString(R.string.share_footer), paint(ctx, S.small * 0.8f, soft), pad, h - pad * 1.25f, (w - pad * 2).toInt())
        return b
    }

    /** 그림을 캐시에 두고 폰의 ‘보내기’ 창을 연다. */
    fun send(ctx: Context, bmp: Bitmap, name: String) {
        runCatching {
            val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
            val f = File(dir, "$name.png")
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.share", f)
            val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ctx.startActivity(Intent.createChooser(send, ctx.getString(R.string.share_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun feelingName(ctx: Context, f: Feeling?) = f?.let { io.github.graviton94.carpediem.ui.Labels.feeling(ctx, it) }
}
