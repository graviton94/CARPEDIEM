package io.github.graviton94.carpediem.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.graviton94.carpediem.MainActivity
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Constellations
import io.github.graviton94.carpediem.data.Store
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.garden.MONTH_SIZES
import io.github.graviton94.carpediem.ui.garden.SkyTime
import io.github.graviton94.carpediem.ui.garden.StarGarden
import io.github.graviton94.carpediem.ui.garden.meadow
import io.github.graviton94.carpediem.ui.garden.monthIn
import io.github.graviton94.carpediem.ui.garden.nightSky
import java.time.LocalDateTime
import java.time.format.TextStyle as MonthStyle

/**
 * 마음의 기록 위젯: 이번 달의 무늬 (앱의 추억 페이지와 같은 그리기), 낮엔 꽃 · 밤엔 별. 위에 ‘9월의 정원’ 과 별자리 이름.
 * 누르면 추억의 이번 달로. 기록 남기기를 꺼 두면 안내만.
 */
class RecordWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = Store(context)
        val now = LocalDateTime.now(); val today = now.toLocalDate()
        SkyTime.useCountry(context, store.profile?.countryCode)
        val night = SkyTime.isDark(now)
        val days = Constellations.monthDays(store.lines, today.year, today.monthValue)
        val locale = context.resources.configuration.locales[0]
        val month = today.month.getDisplayName(MonthStyle.FULL_STANDALONE, locale)
        val cons = store.constellations.of(today.monthValue)?.name(io.github.graviton94.carpediem.data.Words.lang(context)).orEmpty()
        val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_OPEN, "record:${today.year}-${today.monthValue}")
        val ink = if (night) Tokens.Garden.Year.Colors.plain else Tokens.Garden.Colors.ink
        provideContent {
            val size = LocalSize.current
            val bmp = recordBitmap(context, size.width, size.height, if (store.keepLines) StarGarden.month(store.constellations, days, store.haruSeed) else emptyList(), night, today)
            Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity(open))) {
                Image(ImageProvider(bmp), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                Column(GlanceModifier.padding(Tokens.Layout.widgetPadding)) {
                    Text(context.getString(R.string.record_monthTitle, month), style = TextStyle(color = ColorProvider(ink, ink), fontSize = Tokens.TypeScale.subhead.size, fontWeight = FontWeight.Medium))
                    Text(if (store.keepLines) cons else context.getString(R.string.record_off), style = TextStyle(color = ColorProvider(ink.copy(alpha = 0.7f), ink.copy(alpha = 0.7f)), fontSize = Tokens.TypeScale.caption2.size))
                }
            }
        }
    }
}

class RecordReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = RecordWidget() }

/** 둥근 판 하나에 이번 달의 무늬 (그림 보내기 · 앱과 같은 그리기). */
private fun recordBitmap(context: Context, w: Dp, h: Dp, dots: List<StarGarden.Dot>, night: Boolean, today: java.time.LocalDate): Bitmap {
    val d = context.resources.displayMetrics.density
    // 너무 크면 가로세로를 같은 비율로 줄임 (그림이 늘어나지 않게)
    val k = minOf(1f, 1200f / maxOf(w.value * d, h.value * d, 1f))
    val bw = (w.value * d * k).toInt().coerceAtLeast(1); val bh = (h.value * d * k).toInt().coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val r = Tokens.Radius.lg.value * d
    c.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, bw.toFloat(), bh.toFloat()), r, r, Path.Direction.CW) })
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(Density(d), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(c), Size(bw.toFloat(), bh.toFloat())) {
        if (night) nightSky() else meadow()
        // 무늬는 위 글자 아래, 가로세로 짧은 쪽에 맞춘 판
        val top = bh * 0.22f
        val boardW = minOf(bw.toFloat(), (bh - top) * Tokens.Garden.Year.monthAspect); val boardH = boardW / Tokens.Garden.Year.monthAspect
        val left = (bw - boardW) / 2f
        monthIn(Rect(Offset(left, top), Size(boardW, boardH)), dots, night, today, MONTH_SIZES)
    }
    return bmp
}
