package io.github.graviton94.carpediem.notify

import io.github.graviton94.carpediem.core.Lines
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.graviton94.carpediem.MainActivity
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.GardenDecor
import io.github.graviton94.carpediem.data.Design
import io.github.graviton94.carpediem.data.QuoteLanguage
import io.github.graviton94.carpediem.data.Store
import io.github.graviton94.carpediem.design.Tokens
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * 알림은 하루에 많아야 세 번: 아침 (Daily, 오늘의 문장 · 그날 도착한 것), 전날 저녁 (Tomorrow, 내일의 생일 · 특별한 날),
 * 밤 (Evening, 하루 정리). 셋 다 기본은 꺼 둔다.
 *
 * 아침 알림: 오늘의 문장. 그날 정원에 새로 놓인 것 · 돌아온 한 줄 · 피어난 정원 · 도착한 편지가 있으면 그 소식이 먼저.
 */
object Daily {
    private const val WORK = "daily-notify"
    private const val CHANNEL = "daily"
    private const val ID = 1

    fun schedule(context: Context, on: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!on) { wm.cancelUniqueWork(WORK); return }
        val now = LocalDateTime.now()
        val work = PeriodicWorkRequestBuilder<DailyWorker>(1, TimeUnit.DAYS).setInitialDelay(delayTo(Tokens.Notify.hour.toInt(), Tokens.Notify.minute.toInt()), TimeUnit.MINUTES).build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, work)
    }

    /** 지금부터 다음 hour:minute 까지 (분). */
    internal fun delayTo(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMinutes()
    }

    internal fun channel(context: Context) =
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notify_channel), NotificationManager.IMPORTANCE_LOW))
    /** 하루 정리 (조용히) · 생일과 특별한 날 (보통: 화면 위에 잠깐 · 기본 소리). 폰 설정에서 따로 끌 수 있게 채널을 나눔. */
    internal fun eveningChannel(context: Context) =
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("evening", context.getString(R.string.notify_channelEvening), NotificationManager.IMPORTANCE_LOW))
    internal fun daysChannel(context: Context) =
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("days", context.getString(R.string.notify_channelDays), NotificationManager.IMPORTANCE_DEFAULT))

    /** 한글 이름 뒤 ‘이 · 가’ (받침 있으면 이). 한글이 아니면 없음 (영어 문장은 따로). */
    internal fun subject(name: String, context: Context): String {
        if (context.resources.configuration.locales[0].language != "ko") return ""
        val c = name.lastOrNull() ?: return ""
        if (c !in '가'..'힣') return ""
        return if ((c - '가') % 28 != 0) "이" else "가"
    }

    fun allowed(context: Context) = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun post(context: Context, force: Boolean = false) {
        val store = Store(context)
        if ((!store.notify && !force) || !allowed(context)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notify_channel), NotificationManager.IMPORTANCE_LOW))
        val q = store.todaysQuote()
        val text = q?.let { if (store.quoteLanguage == QuoteLanguage.ENGLISH) it.english else it.korean } ?: return
        var title = context.getString(R.string.words)
        var letterId: String? = null
        var openAt: String? = null   // 누르면 열 곳 (MainActivity.EXTRA_OPEN)
        store.profile?.takeIf { store.design == Design.GARDEN }?.let { p ->
            // 정원에 무언가 새로 생기는 날 (날짜로 정해지는 것만): 나무가 자라는 날 · 계절이 바뀌어 새 한 장이 오는 날
            val today = LocalDate.now()
            val days = java.time.temporal.ChronoUnit.DAYS.between(store.startDate, today)
            val dc = io.github.graviton94.carpediem.design.Tokens.Garden.Decor
            val event = when {
                days in listOf(dc.stageDays1, dc.stageDays2, dc.stageDays3).map { it.toLong() } -> "stage$days" to R.string.decor_new_stage
                GardenDecor.realSeason(today, p.countryCode) != GardenDecor.realSeason(today.minusDays(1), p.countryCode) -> "card$today" to R.string.decor_new_card
                else -> null
            }
            event?.takeIf { it.first !in store.notifiedMoments }?.let { (id, res) ->
                title = context.getString(res)
                store.notifiedMoments = store.notifiedMoments + id
            }
        }
        // 몇 해 전 오늘 보낸 한 줄이 있으면 그것을 알린다 (잠금 화면에는 글을 보이지 않음)
        var body = text
        val yearAgo = Lines.yearsAgo(store.lines, LocalDate.now()).firstOrNull()
        if (yearAgo != null) { title = context.getString(R.string.recall_notify, "${yearAgo.first}"); body = context.getString(R.string.recall_notifyText); openAt = "write" }
        else if (store.keepLines && store.randomRecall() != null) { title = context.getString(R.string.recall_randomNotify); body = context.getString(R.string.recall_notifyText); openAt = "write" }
        if (store.design == Design.GARDEN) {
            val today = LocalDate.now()
            // 지난 달 · 지난 해의 정원이 핀 날 (달의 첫날 · 1월 1일, 그때 한 줄이 있었으면)
            val prev = today.minusMonths(1)
            if (store.keepLines && today.dayOfMonth == 1) {
                if (today.monthValue == 1 && store.lines.any { it.date.year == today.year - 1 }) {
                    title = context.getString(R.string.year_card, "${today.year - 1}"); body = context.getString(R.string.notify_gardenText); openAt = "year:${today.year - 1}"
                } else if (store.lines.any { it.date.year == prev.year && it.date.monthValue == prev.monthValue }) {
                    title = context.getString(R.string.record_card, java.time.Month.of(prev.monthValue).getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, context.resources.configuration.locales[0]))
                    body = context.getString(R.string.notify_gardenText); openAt = "month:${prev.year}-${prev.monthValue}"
                }
            }
            // 계절의 편지가 도착한 날 (한 번만, 잠금 화면엔 글 없이)
            io.github.graviton94.carpediem.core.Letters.due(today)?.takeIf { store.keepLines }?.let { day ->
                io.github.graviton94.carpediem.core.Letters.of(store.lines, day, io.github.graviton94.carpediem.design.Tokens.Garden.Letter.minLines.toInt())
                    ?.takeIf { it.id !in store.lettersNotified && it.id !in store.lettersOpened }?.let { l ->
                        val season = context.getString(when (l.season) { io.github.graviton94.carpediem.core.Season.SPRING -> R.string.season_spring; io.github.graviton94.carpediem.core.Season.SUMMER -> R.string.season_summer; io.github.graviton94.carpediem.core.Season.AUTUMN -> R.string.season_autumn; io.github.graviton94.carpediem.core.Season.WINTER -> R.string.season_winter })
                        title = context.getString(R.string.notify_letter, season); body = context.getString(R.string.notify_letterText); letterId = l.id; openAt = "letter"
                    }
            }
        }
        // 아침 알림을 누르면 (정원 디자인 · 켜 두었을 때) 하루를 여는 숨 1분으로
        val tap = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        // 편지 · 정원 소식이면 그곳으로, 아니면 (켜 두었을 때) 아침의 숨
        if (store.design == Design.GARDEN && openAt != null) tap.putExtra(MainActivity.EXTRA_OPEN, openAt)
        else if (store.design == Design.GARDEN && store.morningBreath) tap.putExtra(MainActivity.EXTRA_MORNING_BREATH, true)
        val open = PendingIntent.getActivity(context, 0, tap, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.mipmap.ic_launcher_monochrome).setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(open).setAutoCancel(true).build()
        if (allowed(context)) {
            NotificationManagerCompat.from(context).notify(ID, n)
            // 편지를 알린 경우에만 ‘알렸음’으로 (생일에 밀렸으면 다음 날 다시)
            letterId?.let { store.lettersNotified = store.lettersNotified + it }
        }
    }
}

class DailyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result { Daily.post(applicationContext); return Result.success() }
}

/** 하루 정리 알림: 밤에 한 번, 그날 한 줄을 아직 보내지 않았을 때만. 기본 꺼짐. */
object Evening {
    private const val WORK = "evening-notify"
    private const val ID = 2

    fun schedule(context: Context, on: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!on) { wm.cancelUniqueWork(WORK); return }
        val work = PeriodicWorkRequestBuilder<EveningWorker>(1, TimeUnit.DAYS).setInitialDelay(Daily.delayTo(Tokens.Notify.eveningHour.toInt(), 0), TimeUnit.MINUTES).build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, work)
    }

    fun post(context: Context, force: Boolean = false) {
        val store = Store(context)
        if (!force && (!store.eveningNotify || store.lines.any { it.date == LocalDate.now() })) return   // 꺼 두었거나 오늘은 이미 보냄
        if (!Daily.allowed(context)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        Daily.eveningChannel(context)
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra(MainActivity.EXTRA_OPEN, "write"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, "evening").setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.notify_evening)).setContentText(context.getString(R.string.notify_eveningText))
            .setContentIntent(open).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify(ID, n)
    }
}

class EveningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result { Evening.post(applicationContext); return Result.success() }
}

/**
 * 생일 · 특별한 날 알림: 전날 저녁 (tomorrowHour) 과 그날 아침 (dayOfHour) 에 한 번씩. 가족의 생일이거나 지난 해들의 특별한 날과 같은 날일 때만
 * (겹치면 한 번에). 기본 꺼짐.
 */
object Tomorrow {
    private const val WORK = "tomorrow-notify"
    private const val WORK_TODAY = "dayof-notify"

    fun schedule(context: Context, on: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!on) { wm.cancelUniqueWork(WORK); wm.cancelUniqueWork(WORK_TODAY); return }
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<TomorrowWorker>(1, TimeUnit.DAYS).setInitialDelay(Daily.delayTo(Tokens.Notify.tomorrowHour.toInt(), 0), TimeUnit.MINUTES).build())
        wm.enqueueUniquePeriodicWork(WORK_TODAY, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<DayOfWorker>(1, TimeUnit.DAYS).setInitialDelay(Daily.delayTo(Tokens.Notify.dayOfHour.toInt(), 0), TimeUnit.MINUTES).build())
    }

    /** ahead = 1 이면 내일의 일을 오늘 저녁에, 0 이면 오늘의 일을 오늘 아침에. sample = 시험용 (그런 날이 아니어도 첫 가족의 이름으로). */
    fun post(context: Context, ahead: Int, sample: Boolean = false) {
        val store = Store(context)
        if ((!sample && (!store.tomorrowNotify || store.design != Design.GARDEN)) || !Daily.allowed(context)) return
        val day = LocalDate.now().plusDays(ahead.toLong())
        val who = if (sample) store.people.take(1) else store.people.filter { io.github.graviton94.carpediem.core.Family.isBirthday(it.birth, day) }
        val names = who.map { it.name }
        // 내 생일 (그날 아침에만): “생일 축하해요”. 시험 버튼은 가족이 없으면 이것으로
        val mine = ahead == 0 && (store.profile?.birthDate?.let { io.github.graviton94.carpediem.core.Family.isBirthday(it, day) } == true || (sample && who.isEmpty()))
        val days = io.github.graviton94.carpediem.core.SpecialDays.anniversaries(store.specialDays, day)
            .map { (d, years) -> context.getString(if (ahead == 0) R.string.notify_todaySpecial else R.string.notify_tomorrowSpecial, d.name, "$years") }
        if (names.isEmpty() && days.isEmpty() && !mine) return
        val title: String; val body: String
        if (mine) {
            title = context.getString(R.string.bday_mineToday); body = days.firstOrNull() ?: context.getString(R.string.notify_mineBirthdayText)
        } else if (names.isNotEmpty()) {
            val who = names.joinToString(", ")
            title = if (ahead == 0) context.getString(R.string.bday_today, who) else context.getString(R.string.notify_tomorrowBirthday, who)
            body = days.firstOrNull() ?: context.getString(if (ahead == 0) R.string.notify_todayBirthdayText else R.string.notify_tomorrowBirthdayText)
        } else { title = days.first(); body = days.drop(1).firstOrNull() ?: context.getString(R.string.notify_tomorrowSpecialText) }
        Daily.daysChannel(context)
        // 생일이면 그 사람의 돌 페이지 (내 생일이면 내 돌), 특별한 날이면 흐름 (인생 달력의 꽃)
        val target = if (mine) "stone:" else who.firstOrNull()?.let { "stone:${it.id}" } ?: "flow"
        val open = PendingIntent.getActivity(context, 2 + ahead * 10, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra(MainActivity.EXTRA_OPEN, target),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, "days").setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(title).setContentText(body).setContentIntent(open).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify(3 + ahead, n)
    }
}

class TomorrowWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result { Tomorrow.post(applicationContext, 1); return Result.success() }
}

class DayOfWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result { Tomorrow.post(applicationContext, 0); return Result.success() }
}
