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
        store.profile?.takeIf { store.design == Design.GARDEN }?.let { p ->
            val today = LocalDate.now()
            val fresh = Moments.earned(store.startDate, p.birthDate, p.expectancy(store.table), today, store.firstSkip, store.returned, store.streaks).filter { it.date == today && it.id !in store.notifiedMoments }
            fresh.firstOrNull()?.let { m ->
                val id = context.resources.getIdentifier("obj_${m.id}", "string", context.packageName)
                if (id != 0) title = context.getString(R.string.notify_keepsake, context.getString(id))
                store.notifiedMoments = store.notifiedMoments + fresh.map { it.id }
            }
        }
        // 몇 해 전 오늘 보낸 한 줄이 있으면 그것을 알린다 (잠금 화면에는 글을 보이지 않음)
        var body = text
        Lines.yearsAgo(store.lines, LocalDate.now()).firstOrNull()?.let { (years, _) ->
            title = context.getString(R.string.recall_notify, "$years"); body = context.getString(R.string.recall_notifyText)
        }
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.mipmap.ic_launcher_monochrome).setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(open).setAutoCancel(true).build()
        if (allowed(context)) NotificationManagerCompat.from(context).notify(ID, n)
    }
}

class DailyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result { Daily.post(applicationContext); return Result.success() }
}
