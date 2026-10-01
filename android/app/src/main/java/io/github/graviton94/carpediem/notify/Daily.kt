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
import io.github.graviton94.carpediem.core.Moments
import io.github.graviton94.carpediem.data.Design
import io.github.graviton94.carpediem.data.QuoteLanguage
import io.github.graviton94.carpediem.data.Store
import io.github.graviton94.carpediem.design.Tokens
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** 하루 한 번 조용한 알림: 오늘의 문장, 그리고 그날 정원에 새로 놓인 것이 있으면 그것. 기본은 꺼 둔다. */
object Daily {
    private const val WORK = "daily-notify"
    private const val CHANNEL = "daily"
    private const val ID = 1

    fun schedule(context: Context, on: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!on) { wm.cancelUniqueWork(WORK); return }
        val now = LocalDateTime.now()
        val at = LocalTime.of(Tokens.Notify.hour.toInt(), Tokens.Notify.minute.toInt())
        var next = now.toLocalDate().atTime(at)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val work = PeriodicWorkRequestBuilder<DailyWorker>(1, TimeUnit.DAYS).setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES).build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, work)
    }

    fun allowed(context: Context) = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun post(context: Context) {
        val store = Store(context)
        if (!store.notify || !allowed(context)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notify_channel), NotificationManager.IMPORTANCE_LOW))
        val q = store.todaysQuote()
        val text = q?.let { if (store.quoteLanguage == QuoteLanguage.ENGLISH) it.english else it.korean } ?: return
        var title = context.getString(R.string.words)
        var letterId: String? = null
        store.profile?.takeIf { store.design == Design.GARDEN }?.let { p ->
            val today = LocalDate.now()
            val fresh = Moments.earned(store.startDate, p.birthDate, p.expectancy(store.table), today, store.firstSkip, store.returned, store.streaks, store.breaths.minOfOrNull { it.first }).filter { it.date == today && it.id !in store.notifiedMoments }
            fresh.firstOrNull()?.let { m ->
                val id = context.resources.getIdentifier("obj_${m.id}", "string", context.packageName)
                if (id != 0) title = context.getString(R.string.notify_keepsake, context.getString(id))
                store.notifiedMoments = store.notifiedMoments + fresh.map { it.id }
            }
        }
        // 몇 해 전 오늘 보낸 한 줄이 있으면 그것을 알린다 (잠금 화면에는 글을 보이지 않음)
        var body = text
        val yearAgo = Lines.yearsAgo(store.lines, LocalDate.now()).firstOrNull()
        if (yearAgo != null) { title = context.getString(R.string.recall_notify, "${yearAgo.first}"); body = context.getString(R.string.recall_notifyText) }
        else if (store.keepLines && store.randomRecall() != null) { title = context.getString(R.string.recall_randomNotify); body = context.getString(R.string.recall_notifyText) }
        if (store.design == Design.GARDEN) {
            val today = LocalDate.now()
            // 질문 날: 문장 대신 오늘의 질문
            store.todaysQuestion(today)?.takeIf { yearAgo == null }?.let { qq ->
                title = context.getString(R.string.question_label); body = if (store.quoteLanguage == QuoteLanguage.ENGLISH) qq.english else qq.korean
            }
            // 계절의 편지가 도착한 날 (한 번만, 잠금 화면엔 글 없이)
            io.github.graviton94.carpediem.core.Letters.due(today)?.takeIf { store.keepLines }?.let { day ->
                io.github.graviton94.carpediem.core.Letters.of(store.lines, day, io.github.graviton94.carpediem.design.Tokens.Garden.Letter.minLines.toInt())
                    ?.takeIf { it.id !in store.lettersNotified && it.id !in store.lettersOpened }?.let { l ->
                        val season = context.getString(when (l.season) { io.github.graviton94.carpediem.core.Season.SPRING -> R.string.season_spring; io.github.graviton94.carpediem.core.Season.SUMMER -> R.string.season_summer; io.github.graviton94.carpediem.core.Season.AUTUMN -> R.string.season_autumn; io.github.graviton94.carpediem.core.Season.WINTER -> R.string.season_winter })
                        title = context.getString(R.string.notify_letter, season); body = context.getString(R.string.notify_letterText); letterId = l.id
                    }
            }
        }
        // 가족 생일이 가장 먼저 (정원 디자인일 때)
        if (store.design == Design.GARDEN) {
            val names = store.people.filter { io.github.graviton94.carpediem.core.Family.isBirthday(it.birth, LocalDate.now()) }.map { it.name }
            if (names.isNotEmpty()) { title = context.getString(R.string.notify_birthday, names.joinToString(", ")); body = text; letterId = null }
        }
        // 아침 알림을 누르면 (정원 디자인 · 켜 두었을 때) 하루를 여는 숨 1분으로
        val tap = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (store.design == Design.GARDEN && store.morningBreath) tap.putExtra(MainActivity.EXTRA_MORNING_BREATH, true)
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

/** 저녁 한 줄 알림: 고른 시각에 하루 한 번, 그날 한 줄을 아직 보내지 않았을 때만. 기본 꺼짐. */
object Evening {
    private const val WORK = "evening-notify"
    private const val ID = 2

    fun schedule(context: Context, on: Boolean, hour: Int) {
        val wm = WorkManager.getInstance(context)
        if (!on) { wm.cancelUniqueWork(WORK); return }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(hour.coerceIn(0, 23), 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val work = PeriodicWorkRequestBuilder<EveningWorker>(1, TimeUnit.DAYS).setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES).build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, work)
    }

    fun post(context: Context) {
        val store = Store(context)
        if (!store.eveningNotify || !Daily.allowed(context)) return
        if (store.lines.any { it.date == LocalDate.now() }) return   // 오늘은 이미 보냄
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("daily", context.getString(R.string.notify_channel), NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, "daily").setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.notify_evening)).setContentText(context.getString(R.string.notify_eveningText))
            .setContentIntent(open).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify(ID, n)
    }
}

class EveningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result { Evening.post(applicationContext); return Result.success() }
}
