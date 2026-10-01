package io.github.graviton94.carpediem.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.graviton94.carpediem.MainActivity
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.LifePeriod
import io.github.graviton94.carpediem.core.LifeSnapshot
import io.github.graviton94.carpediem.core.LifeUnit
import io.github.graviton94.carpediem.core.Quote
import io.github.graviton94.carpediem.data.QuoteLanguage
import io.github.graviton94.carpediem.data.Store
import io.github.graviton94.carpediem.design.Palette
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.Labels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object Widgets {
    /** 앱에서 정보가 바뀌면 모든 위젯을 새로 그린다. */
    fun refresh(context: Context) {
        CoroutineScope(Dispatchers.Default).launch { updateAll(context) }
    }

    suspend fun updateAll(context: Context) {
        DaysLeftWidget().updateAll(context); DaysLeftGardenWidget().updateAll(context)
        TodayWidget().updateAll(context); TodayGardenWidget().updateAll(context)
        LifeCalendarWidget().updateAll(context); LifeCalendarGardenWidget().updateAll(context)
        FamilyGardenWidget().updateAll(context)
        RecordWidget().updateAll(context)
    }

    /** ‘오늘’ 위젯이 한 시간마다, 문장이 자정 무렵 바뀌도록 한 시간마다 새로 그린다. */
    fun scheduleHourly(context: Context) {
        val work = PeriodicWorkRequestBuilder<RefreshWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("widget-refresh", ExistingPeriodicWorkPolicy.KEEP, work)
    }
}

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Widgets.updateAll(applicationContext)
        return Result.success()
    }
}

/** 위젯 한 번 그릴 때 필요한 값. */
private class WidgetData(context: Context) {
    val store = Store(context)
    val profile = store.profile
    val snapshot: LifeSnapshot? = profile?.let { LifeSnapshot(it.birthDate, it.expectancy(store.table), LocalDateTime.now()) }
    val quote: Quote? = store.todaysQuote()
    val language: QuoteLanguage = store.quoteLanguage
    val unit: LifeUnit = store.unit
    val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val palette = Palette(dark)
    val now: LocalDateTime = LocalDateTime.now()
    init { gardenNight = io.github.graviton94.carpediem.ui.garden.SkyTime.isDark(now) }
}

/** 정원 위젯 글자색: 밤 · 새벽이면 그림도 어두워지므로 밝은 글자 (앱과 같은 규칙). */
@Volatile private var gardenNight = false

// ───────────────────────── 정원 디자인 ─────────────────────────

private val gInk get() = (if (gardenNight) Tokens.Garden.Night.Colors.ink else Tokens.Garden.Colors.ink).let { color(it, it) }
private val gSub get() = (if (gardenNight) Tokens.Garden.Night.Colors.inkSoft else Tokens.Garden.Colors.inkSoft).let { color(it, it) }

/** 정원 그림 바탕 + 글자. 종이 그림이라 다크 모드에서도 밝게. */
@Composable
private fun GardenSurface(context: Context, data: WidgetData, kind: GardenWidgetArt.Kind, content: @Composable () -> Unit) {
    val size = LocalSize.current
    val bmp = GardenWidgetArt.render(context, kind, px(context, size.width), px(context, size.height), data.snapshot, data.now)
    Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>())) {
        Image(ImageProvider(bmp), null, GlanceModifier.fillMaxSize(), contentScale = androidx.glance.layout.ContentScale.FillBounds)
        Box(GlanceModifier.fillMaxSize().padding(Tokens.Layout.widgetPadding)) { content() }
    }
}

// ───────────────────────── 공통 ─────────────────────────

private fun color(day: Color, night: Color) = ColorProvider(day, night)
private val fg get() = color(Tokens.Palette.foreground.light, Tokens.Palette.foreground.dark)
private val sub get() = color(Tokens.Palette.secondary.light, Tokens.Palette.secondary.dark)
private val olive get() = color(Tokens.Palette.olive.light, Tokens.Palette.olive.dark)

/** 위젯은 앱 글꼴을 못 쓴다. 한글은 기기 명조가 제각각이라 기본 글꼴로, 숫자만 명조. */
private fun style(size: androidx.compose.ui.unit.TextUnit, c: androidx.glance.unit.ColorProvider, weight: FontWeight = FontWeight.Normal, serif: Boolean = false) =
    TextStyle(color = c, fontSize = size, fontWeight = weight, fontFamily = if (serif) FontFamily.Serif else null)

@Composable
private fun Surface(content: @Composable () -> Unit) {
    Box(
        GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_background)).cornerRadius(Tokens.Radius.lg)
            .padding(Tokens.Layout.widgetPadding).clickable(actionStartActivity<MainActivity>()),
    ) { content() }
}

@Composable
private fun Label(context: Context, text: String, data: WidgetData, mark: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (mark) {
            Image(ImageProvider(ensoBitmap(context, data.palette, Tokens.Widget.markSize.dp)), null, GlanceModifier.size(Tokens.Widget.markSize.dp))
            Spacer(GlanceModifier.width(Tokens.Space.sp1))
        }
        Text(text.uppercase(), style = style(Tokens.TypeScale.caption2.size, olive, FontWeight.Bold))
    }
}

@Composable
private fun Empty(context: Context) {
    Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(context.getString(R.string.widget_empty), style = style(Tokens.TypeScale.caption1.size, sub))
    }
}

private fun px(context: Context, dp: Dp) = max(1, (dp.value * context.resources.displayMetrics.density).toInt())

// ───────────────────────── 남은 날 (2×2) ─────────────────────────

/** 위젯 목록에 유리 · 정원 두 모양이 따로 있다 (앱 디자인 설정과 상관없이 고를 수 있게). */
abstract class DaysLeftBase(private val garden: Boolean) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData(context)
        provideContent {
            if (garden) GardenSurface(context, data, GardenWidgetArt.Kind.DAYS) {
                val s = data.snapshot
                if (s == null) Text(context.getString(R.string.widget_empty), style = style(Tokens.TypeScale.caption1.size, gSub))
                else Column {
                    Text(Labels.number(s.remaining(data.unit)), style = style((Tokens.TypeScale.largeTitle.size.value * Tokens.Widget.numberScale).sp, gInk, FontWeight.Bold, serif = true), maxLines = 1)
                    Text(if (data.unit == LifeUnit.DAYS) context.getString(R.string.widget_daysLeft) else Labels.unit(context, data.unit), style = style(Tokens.TypeScale.caption1.size, gSub))
                }
            } else Surface {
                val s = data.snapshot
                if (s == null) Empty(context) else Column(GlanceModifier.fillMaxSize()) {
                    val label = if (data.unit == LifeUnit.DAYS) context.getString(R.string.widget_daysLeft) else Labels.unit(context, data.unit)
                    Label(context, label, data, mark = true)
                    Spacer(GlanceModifier.defaultWeight())
                    Text(Labels.number(s.remaining(data.unit)), style = style((Tokens.TypeScale.largeTitle.size.value * Tokens.Widget.numberScale).sp, fg, FontWeight.Bold, serif = true), maxLines = 1)
                    Spacer(GlanceModifier.defaultWeight())
                    LinearProgressIndicator(s.progress.toFloat(), GlanceModifier.fillMaxWidth().height(Tokens.Stroke.barThin), color = olive,
                        backgroundColor = color(Tokens.Palette.dim.light, Tokens.Palette.dim.dark))
                    Spacer(GlanceModifier.height(Tokens.Space.sp1))
                    Row(GlanceModifier.fillMaxWidth()) {
                        Text(Labels.percent(s.progress), style = style(Tokens.TypeScale.caption2.size, sub))
                        Spacer(GlanceModifier.defaultWeight())
                        Text(Labels.season(context, s.season), style = style(Tokens.TypeScale.caption2.size, sub))
                    }
                }
            }
        }
    }
}

class DaysLeftWidget : DaysLeftBase(garden = false)
class DaysLeftGardenWidget : DaysLeftBase(garden = true)
class DaysLeftReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = DaysLeftWidget() }
class DaysLeftGardenReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = DaysLeftGardenWidget() }

// ───────────────────────── 오늘 (2×2) ─────────────────────────

abstract class TodayBase(private val garden: Boolean) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData(context)
        val today = LifeSnapshot(java.time.LocalDate.now(), 1.0, LocalDateTime.now()).period(LifePeriod.DAY)
        provideContent {
            if (garden) GardenSurface(context, data, GardenWidgetArt.Kind.TODAY) {
                Column {
                    Text(context.getString(R.string.widget_todayLeft, "${today.hoursLeft}"), style = style(Tokens.TypeScale.title2.size, gInk, FontWeight.Bold, serif = true))
                    Text(context.getString(R.string.widget_todaySub), style = style(Tokens.TypeScale.caption1.size, gSub))
                }
            } else Surface {
                Column(GlanceModifier.fillMaxSize()) {
                    Label(context, context.getString(R.string.widget_today), data)
                    val side = min(LocalSize.current.width.value, LocalSize.current.height.value).dp - Tokens.Layout.widgetPadding * 2 - Tokens.Space.sp5
                    Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                        Image(ImageProvider(ringBitmap(context, data.palette, side, today.progress.toFloat())), null, GlanceModifier.size(side))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(context.getString(R.string.widget_todayLeft, "${today.hoursLeft}"), style = style(Tokens.TypeScale.title2.size, fg, FontWeight.Bold, serif = true))
                            Text(context.getString(R.string.widget_todaySub), style = style(Tokens.TypeScale.caption2.size, sub))
                        }
                    }
                }
            }
        }
    }
}

class TodayWidget : TodayBase(garden = false)
class TodayGardenWidget : TodayBase(garden = true)
class TodayReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TodayWidget() }
class TodayGardenReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TodayGardenWidget() }

// ───────────────────────── 인생 달력 (4×2 · 4×4) ─────────────────────────

abstract class LifeCalendarBase(private val garden: Boolean) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData(context)
        provideContent {
            val gs = LocalSize.current
            if (garden) {
                val large = gs.height >= Tokens.Widget.largeFromHeight.dp
                GardenSurface(context, data, if (large) GardenWidgetArt.Kind.LARGE else GardenWidgetArt.Kind.CALENDAR) {
                    val s = data.snapshot
                    when {
                        s == null -> Text(context.getString(R.string.widget_empty), style = style(Tokens.TypeScale.caption1.size, gSub))
                        large -> Column(GlanceModifier.fillMaxSize()) {
                            Text(Labels.number(s.remaining(LifeUnit.DAYS)), style = style(Tokens.TypeScale.largeTitle.size, gInk, FontWeight.Bold, serif = true), maxLines = 1)
                            Text(context.getString(R.string.widget_daysLeft), style = style(Tokens.TypeScale.caption1.size, gSub))
                            Spacer(GlanceModifier.defaultWeight())
                            data.quote?.let { q -> Text(if (data.language == QuoteLanguage.ENGLISH) q.english else q.korean, style = style(Tokens.TypeScale.footnote.size, gInk), maxLines = 2) }
                        }
                        else -> Column(GlanceModifier.fillMaxHeight().width((gs.width - Tokens.Layout.widgetPadding * 2) * Tokens.Garden.Widget.gridLeft)) {
                            Text(context.getString(R.string.calendar), style = style(Tokens.TypeScale.footnote.size, gInk, FontWeight.Bold))
                            Spacer(GlanceModifier.defaultWeight())
                            Text("${s.remaining(LifeUnit.YEARS)}", style = style(Tokens.TypeScale.largeTitle.size, gInk, FontWeight.Bold, serif = true))
                            Text(context.getString(R.string.widget_yearsLeft), style = style(Tokens.TypeScale.caption1.size, gSub))
                            Spacer(GlanceModifier.defaultWeight())
                            Text("${Labels.season(context, s.season)} · ${Labels.percent(s.progress, 0)}", style = style(Tokens.TypeScale.caption2.size, gSub))
                        }
                    }
                }
            } else Surface {
                val s = data.snapshot
                val size = LocalSize.current
                val inner = androidx.compose.ui.unit.DpSize(size.width - Tokens.Layout.widgetPadding * 2, size.height - Tokens.Layout.widgetPadding * 2)
                when {
                    s == null -> Empty(context)
                    size.height < Tokens.Widget.largeFromHeight.dp -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        val textW = inner.width * Tokens.Grid.widgetMediumTextRatio
                        Column(GlanceModifier.width(textW).fillMaxHeight()) {
                            Label(context, context.getString(R.string.calendar), data)
                            Spacer(GlanceModifier.defaultWeight())
                            Text("${s.remaining(LifeUnit.YEARS)}", style = style(Tokens.TypeScale.largeTitle.size, fg, FontWeight.Bold, serif = true))
                            Text(context.getString(R.string.widget_yearsLeft), style = style(Tokens.TypeScale.footnote.size, sub))
                            Spacer(GlanceModifier.defaultWeight())
                            Text("${Labels.season(context, s.season)} · ${Labels.percent(s.progress, 0)}", style = style(Tokens.TypeScale.caption2.size, sub))
                        }
                        Spacer(GlanceModifier.width(Tokens.Space.sp4))
                        val gw = inner.width - textW - Tokens.Space.sp4
                        Image(ImageProvider(gridBitmap(context, data.palette, gw, inner.height, s.total(LifeUnit.YEARS), s.lived(LifeUnit.YEARS), Tokens.Grid.widgetMediumColumns)), null,
                            GlanceModifier.width(gw).height(inner.height))
                    }
                    else -> Column(GlanceModifier.fillMaxSize()) {
                        Row(GlanceModifier.fillMaxWidth()) {
                            Label(context, context.getString(R.string.calendar), data)
                            Spacer(GlanceModifier.defaultWeight())
                            Text(context.getString(R.string.widget_monthsLeft, Labels.number(s.remaining(LifeUnit.MONTHS))), style = style(Tokens.TypeScale.caption1.size, sub))
                        }
                        Spacer(GlanceModifier.height(Tokens.Space.sp3))
                        val quoteH = if (data.quote != null) Tokens.Widget.quoteHeight.dp else 0.dp
                        val gh = inner.height - Tokens.Space.sp3 * 2 - quoteH - Tokens.Widget.gridBottom.dp
                        Image(ImageProvider(gridBitmap(context, data.palette, inner.width, gh, s.total(LifeUnit.MONTHS), s.lived(LifeUnit.MONTHS), Tokens.Grid.widgetLargeColumns)), null,
                            GlanceModifier.fillMaxWidth().height(gh))
                        data.quote?.let { q ->
                            Spacer(GlanceModifier.height(Tokens.Space.sp3))
                            Text(if (data.language == QuoteLanguage.ENGLISH) q.english else q.korean, style = style(Tokens.TypeScale.footnote.size, fg), maxLines = 3)
                        }
                    }
                }
            }
        }
    }
}

class LifeCalendarWidget : LifeCalendarBase(garden = false)
class LifeCalendarGardenWidget : LifeCalendarBase(garden = true)
class LifeCalendarReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = LifeCalendarWidget() }
class LifeCalendarGardenReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = LifeCalendarGardenWidget() }

// ───────────────────────── 가족의 정원 (4×2, 정원 모양만) ─────────────────────────

class FamilyGardenWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData(context)
        provideContent {
            GardenSurface(context, data, GardenWidgetArt.Kind.FAMILY) {
                if (data.snapshot == null) Text(context.getString(R.string.widget_empty), style = style(Tokens.TypeScale.caption1.size, gSub))
            }
        }
    }
}

class FamilyGardenReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = FamilyGardenWidget() }

// ───────────────────────── 비트맵 그리기 ─────────────────────────

private fun gridBitmap(context: Context, p: Palette, w: Dp, h: Dp, total: Int, filled: Int, columns: Int): Bitmap {
    val bw = px(context, w); val bh = px(context, h)
    val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
    if (total <= 0) return bmp
    val c = Canvas(bmp)
    val rows = max(1, (total + columns - 1) / columns)
    val cell = min(bw.toFloat() / columns, bh.toFloat() / rows)
    val dot = cell * Tokens.Grid.dotRatio
    val ox = (bw - cell * columns) / 2f
    val oy = (bh - cell * rows) / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    for (i in 0 until total) {
        val cx = ox + (i % columns) * cell + cell / 2
        val cy = oy + (i / columns) * cell + cell / 2
        when {
            i == filled -> {
                paint.color = p.now.copy(alpha = Tokens.Effect.NowHalo.widgetAlpha).toArgb(); paint.maskFilter = BlurMaskFilter(cell * Tokens.Effect.NowHalo.widgetBlur, BlurMaskFilter.Blur.NORMAL)
                c.drawCircle(cx, cy, cell * Tokens.Effect.NowHalo.widgetRadius, paint); paint.maskFilter = null
                paint.color = p.now.toArgb(); c.drawCircle(cx, cy, cell * Tokens.Grid.nowRatio / 2, paint)
            }
            i < filled -> { paint.color = p.seasons[min(3, i * 4 / total)].toArgb(); c.drawCircle(cx, cy, dot / 2, paint) }
            else -> { paint.color = p.future.toArgb(); c.drawCircle(cx, cy, dot / 2, paint) }
        }
    }
    return bmp
}

private fun ringBitmap(context: Context, p: Palette, side: Dp, progress: Float): Bitmap {
    val s = px(context, side)
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val stroke = px(context, Tokens.Stroke.barThick).toFloat()
    val r = RectF(stroke, stroke, s - stroke, s - stroke)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND }
    paint.color = p.dim.toArgb(); c.drawArc(r, 0f, 360f, false, paint)
    paint.color = p.olive.toArgb(); c.drawArc(r, -90f, 360f * progress.coerceIn(0f, 1f), false, paint)
    return bmp
}

private fun ensoBitmap(context: Context, p: Palette, side: Dp): Bitmap {
    val e = Tokens.Enso
    val s = px(context, side)
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val r = s * e.radius; val cx = s / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    paint.color = p.olive.copy(alpha = e.ringAlpha).toArgb(); paint.strokeWidth = s * e.ring; c.drawCircle(cx, cx, r, paint)
    paint.color = p.olive.toArgb(); paint.strokeWidth = s * e.arc; paint.strokeCap = Paint.Cap.ROUND
    c.drawArc(RectF(cx - r, cx - r, cx + r, cx + r), e.arcStart, e.arcSweep, false, paint)
    val a = Math.toRadians(e.dotAngle.toDouble())
    paint.style = Paint.Style.FILL; paint.color = p.now.toArgb()
    c.drawCircle(cx + (cos(a) * r).toFloat(), cx + (sin(a) * r).toFloat(), s * e.dot, paint)
    return bmp
}
