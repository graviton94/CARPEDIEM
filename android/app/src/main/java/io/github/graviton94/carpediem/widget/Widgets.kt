package io.github.graviton94.carpediem.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.graviton94.carpediem.MainActivity
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.LifeSnapshot
import io.github.graviton94.carpediem.core.LifeUnit
import io.github.graviton94.carpediem.core.Quote
import io.github.graviton94.carpediem.data.QuoteLanguage
import io.github.graviton94.carpediem.data.Store
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.Labels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.max

object Widgets {
    /** 앱에서 정보가 바뀌면 모든 위젯을 새로 그린다. */
    /**
     * 홈 화면에 위젯 두기 (Android 8+, 런처가 지원할 때): 런처가 ‘추가할까요?’ 를 물음. 지원하지 않으면 false (안내 글로).
     * kind: days · line · record · calendar (위젯은 정원 모양 넷만)
     */
    fun pin(context: Context, kind: String): Boolean {
        val cls = when (kind) {
            "line" -> LineGardenReceiver::class.java; "record" -> RecordReceiver::class.java
            "calendar" -> LifeCalendarGardenReceiver::class.java; else -> DaysLeftGardenReceiver::class.java
        }
        val mgr = android.appwidget.AppWidgetManager.getInstance(context)
        if (!mgr.isRequestPinAppWidgetSupported) return false
        return runCatching { mgr.requestPinAppWidget(android.content.ComponentName(context, cls), null, null) }.getOrDefault(false)
    }

    fun refresh(context: Context) {
        CoroutineScope(Dispatchers.Default).launch { updateAll(context) }
    }

    suspend fun updateAll(context: Context) {
        DaysLeftGardenWidget().updateAll(context)
        LineGardenWidget().updateAll(context)
        RecordWidget().updateAll(context)
        LifeCalendarGardenWidget().updateAll(context)
    }

    /** 하늘 빛 · 오늘의 문장이 제때 바뀌도록 한 시간마다 새로 그린다. */
    fun scheduleHourly(context: Context) {
        val work = PeriodicWorkRequestBuilder<RefreshWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("widget-refresh", ExistingPeriodicWorkPolicy.KEEP, work)
        scheduleMidnight(context, ExistingWorkPolicy.KEEP)
    }

    /** 자정 바로 뒤에 한 번 (남은 날 · ‘남겼어요’ 가 날이 바뀌자마자 맞게). 돌 때마다 다음 자정을 다시 예약. */
    fun scheduleMidnight(context: Context, policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
        val now = java.time.ZonedDateTime.now()
        val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusMinutes(1)
        val work = OneTimeWorkRequestBuilder<MidnightWorker>().setInitialDelay(java.time.Duration.between(now, next).toMinutes().coerceAtLeast(1), TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniqueWork("widget-midnight", policy, work)
    }
}

class MidnightWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Widgets.updateAll(applicationContext)
        Widgets.scheduleMidnight(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)   // 지금 도는 일이 끝난 뒤 이어서
        return Result.success()
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
    val lang: String = io.github.graviton94.carpediem.data.Words.lang(context)
    val unit: LifeUnit = store.unit
    val now: LocalDateTime = LocalDateTime.now()
    init { io.github.graviton94.carpediem.ui.garden.SkyTime.useCountry(context, profile?.countryCode); gardenNight = io.github.graviton94.carpediem.ui.garden.SkyTime.isDark(now) }
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

/** 위젯은 앱 글꼴을 못 쓴다. 한글은 기기 명조가 제각각이라 기본 글꼴로, 숫자만 명조. */
private fun style(size: androidx.compose.ui.unit.TextUnit, c: androidx.glance.unit.ColorProvider, weight: FontWeight = FontWeight.Normal, serif: Boolean = false) =
    TextStyle(color = c, fontSize = size, fontWeight = weight, fontFamily = if (serif) FontFamily.Serif else null)

private fun px(context: Context, dp: Dp) = max(1, (dp.value * context.resources.displayMetrics.density).toInt())

// ───────────────────────── 남은 날 (2×2) ─────────────────────────

class DaysLeftGardenWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData(context)
        provideContent {
            GardenSurface(context, data, GardenWidgetArt.Kind.DAYS) {
                val s = data.snapshot
                if (s == null) Text(context.getString(R.string.widget_empty), style = style(Tokens.TypeScale.caption1.size, gSub))
                else Column {
                    Text(Labels.number(s.remaining(data.unit)), style = style((Tokens.TypeScale.largeTitle.size.value * Tokens.Widget.numberScale).sp, gInk, FontWeight.Bold, serif = true), maxLines = 1)
                    Text(if (data.unit == LifeUnit.DAYS) context.getString(R.string.widget_daysLeft) else Labels.unit(context, data.unit), style = style(Tokens.TypeScale.caption1.size, gSub))
                }
            }
        }
    }
}

class DaysLeftGardenReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = DaysLeftGardenWidget() }

// ───────────────────────── 오늘의 한 줄 (3×2) ─────────────────────────

/**
 * 오늘의 한 줄 위젯: 정원 그림 위에 오늘의 문장 한 줄과 ‘오늘의 한 줄 남기기’. 누르면 기록 페이지의 쓰는 칸으로.
 * 남긴 날은 ‘남겼어요’ 한 줄만 (보낸 글은 위젯에 보이지 않음).
 */
class LineGardenWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData(context)
        val store = Store(context)
        val today = java.time.LocalDate.now()
        val words = store.todaysQuote(today)?.let { io.github.graviton94.carpediem.data.Words.main(it, store.quoteLanguage, io.github.graviton94.carpediem.data.Words.lang(context)) }.orEmpty()
        val sent = store.lines.any { it.date == today }
        val open = android.content.Intent(context, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_OPEN, "write")
        provideContent {
            val size = LocalSize.current
            val bmp = GardenWidgetArt.render(context, GardenWidgetArt.Kind.TODAY, px(context, size.width), px(context, size.height), data.snapshot, data.now)
            Box(GlanceModifier.fillMaxSize().clickable(androidx.glance.appwidget.action.actionStartActivity(open))) {
                Image(ImageProvider(bmp), null, GlanceModifier.fillMaxSize(), contentScale = androidx.glance.layout.ContentScale.FillBounds)
                Column(GlanceModifier.fillMaxSize().padding(Tokens.Layout.widgetPadding)) {
                    Text(context.getString(R.string.words), style = style(Tokens.TypeScale.caption1.size, gSub))
                    // 낮은 위젯 · 큰 글씨에서도 아래 ‘남기기’ 가 잘리지 않게 문장은 두 줄까지
                    Text(words, style = style(Tokens.TypeScale.subhead.size, gInk, FontWeight.Medium), maxLines = if (size.height < 150.dp) 2 else 3)
                    Spacer(GlanceModifier.defaultWeight())
                    Text(context.getString(if (sent) R.string.widget_lineDone else R.string.widget_lineWrite),
                        style = style(Tokens.TypeScale.footnote.size, if (sent) gSub else gInk, if (sent) FontWeight.Normal else FontWeight.Bold))
                }
            }
        }
    }
}
class LineGardenReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = LineGardenWidget() }

// ───────────────────────── 인생 달력 (4×2 · 4×4) ─────────────────────────

class LifeCalendarGardenWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData(context)
        provideContent {
            val gs = LocalSize.current
            val large = gs.height >= Tokens.Widget.largeFromHeight.dp
            GardenSurface(context, data, if (large) GardenWidgetArt.Kind.LARGE else GardenWidgetArt.Kind.CALENDAR) {
                val s = data.snapshot
                when {
                    s == null -> Text(context.getString(R.string.widget_empty), style = style(Tokens.TypeScale.caption1.size, gSub))
                    large -> Column(GlanceModifier.fillMaxSize()) {
                        Text(Labels.number(s.remaining(LifeUnit.DAYS)), style = style(Tokens.TypeScale.largeTitle.size, gInk, FontWeight.Bold, serif = true), maxLines = 1)
                        Text(context.getString(R.string.widget_daysLeft), style = style(Tokens.TypeScale.caption1.size, gSub))
                        Spacer(GlanceModifier.defaultWeight())
                        data.quote?.let { q -> Text(io.github.graviton94.carpediem.data.Words.main(q, data.language, data.lang), style = style(Tokens.TypeScale.footnote.size, gInk), maxLines = 2) }
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
        }
    }
}

class LifeCalendarGardenReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = LifeCalendarGardenWidget() }
