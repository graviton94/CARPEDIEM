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
import androidx.glance.layout.Row
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
    private const val REFRESH_MINUTES = 30L
    /** 앱에서 정보가 바뀌면 모든 위젯을 새로 그린다. */
    /**
     * 홈 화면에 위젯 두기 (Android 8+, 런처가 지원할 때): 런처가 ‘추가할까요?’ 를 물음. 지원하지 않으면 false (안내 글로).
     * kind: line (남은 날 · 오늘의 한 줄) · record (마음의 기록). 위젯은 이 둘뿐
     */
    fun pin(context: Context, kind: String): Boolean {
        val cls = if (kind == "record") RecordReceiver::class.java else LineGardenReceiver::class.java
        val mgr = android.appwidget.AppWidgetManager.getInstance(context)
        if (!mgr.isRequestPinAppWidgetSupported) return false
        return runCatching { mgr.requestPinAppWidget(android.content.ComponentName(context, cls), null, null) }.getOrDefault(false)
    }

    fun refresh(context: Context) {
        CoroutineScope(Dispatchers.Default).launch { updateAll(context) }
    }

    suspend fun updateAll(context: Context) {
        LineGardenWidget().updateAll(context)
        RecordWidget().updateAll(context)
    }

    /** 하늘빛 · 해와 달 · 하루의 잠이 때를 따라 바뀌도록 30분마다 새로 그린다 (1.1.7, 예전엔 한 시간). */
    fun scheduleHourly(context: Context) {
        val work = PeriodicWorkRequestBuilder<RefreshWorker>(REFRESH_MINUTES, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("widget-refresh-30", ExistingPeriodicWorkPolicy.KEEP, work)
        WorkManager.getInstance(context).cancelUniqueWork("widget-refresh")   // 예전 한 시간짜리
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

// ───────────────────────── 공통 ─────────────────────────

private fun color(day: Color, night: Color) = ColorProvider(day, night)

/** 위젯은 앱 글꼴을 못 쓴다. 한글은 기기 명조가 제각각이라 기본 글꼴로, 숫자만 명조. */
private fun style(size: androidx.compose.ui.unit.TextUnit, c: androidx.glance.unit.ColorProvider, weight: FontWeight = FontWeight.Normal, serif: Boolean = false) =
    TextStyle(color = c, fontSize = size, fontWeight = weight, fontFamily = if (serif) FontFamily.Serif else null)

private fun px(context: Context, dp: Dp) = max(1, (dp.value * context.resources.displayMetrics.density).toInt())

// ───────────────────────── 남은 날 · 오늘의 한 줄 (4×2) ─────────────────────────

/**
 * 정원 위젯 (4×2, 1.1.7): 내 정원 한 장 (나무 · 말뚝 · 하루와 가족, 하늘 · 해와 달 · 별이 때를 따라). 글자는 아주 조금만:
 * 위에 남은 시간 하나와 오늘의 문장 한 줄. 하루의 표정 (잠 · 웃음) 이 오늘을 말함. 누르면 정원으로.
 * (클래스 이름은 예전 ‘남은 날 · 오늘의 한 줄’ 위젯 그대로: 이미 둔 위젯이 사라지지 않게)
 */
class LineGardenWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 다시 그릴 때마다 새로 읽음 (위젯이 떠 있는 동안 고쳐 그려도 예전 값이 남지 않게)
        provideContent {
            val data = WidgetData(context)
            val store = Store(context)
            val words = store.todaysQuote(data.now.toLocalDate())?.let { io.github.graviton94.carpediem.data.Words.main(it, store.quoteLanguage, data.lang) }.orEmpty()
            val size = LocalSize.current
            val bmp = GardenWidgetArt.garden(context, px(context, size.width), px(context, size.height), data.now)
            Box(GlanceModifier.fillMaxSize().clickable(androidx.glance.appwidget.action.actionStartActivity(openGarden(context)))) {
                Image(ImageProvider(bmp), null, GlanceModifier.fillMaxSize(), contentScale = androidx.glance.layout.ContentScale.FillBounds)
                Column(GlanceModifier.fillMaxSize().padding(Tokens.Layout.widgetPadding)) {
                    data.snapshot?.let { s ->
                        Row(verticalAlignment = androidx.glance.layout.Alignment.Bottom) {
                            Text(Labels.number(s.remaining(data.unit)), style = style(Tokens.TypeScale.headline.size, gInk, FontWeight.Bold, serif = true), maxLines = 1)
                            Spacer(GlanceModifier.width(Tokens.Space.sp1))
                            Text(if (data.unit == LifeUnit.DAYS) context.getString(R.string.widget_daysLeft) else Labels.unit(context, data.unit), style = style(Tokens.TypeScale.caption2.size, gSub), maxLines = 1)
                        }
                    }
                    if (words.isNotEmpty()) Text(words, style = style(Tokens.TypeScale.footnote.size, gInk), maxLines = 1)
                }
            }
        }
    }
}

/** 위젯을 누르면: 정원으로 (앱을 그대로 염). */
internal fun openGarden(context: Context) = android.content.Intent(context, MainActivity::class.java)
    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)

class LineGardenReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = LineGardenWidget() }
