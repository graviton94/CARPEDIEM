package io.github.graviton94.carpediem.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.DayLine
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.core.Lines
import io.github.graviton94.carpediem.data.Store
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.widget.Widgets
import java.time.LocalDate

/**
 * 하루 정리 알림에서 바로 한 줄 (01): 알림을 내려 쓰고 보내면 앱을 열지 않아도 오늘의 한 줄로 남는다.
 * 보낸 뒤 알림은 ‘정원에 두었어요’로 바뀌고, 마음 셋 (기쁨 · 평온 · 걱정) 가운데 하나를 고를 수 있다. 다른 마음은 앱에서.
 * 잠금 화면에는 글을 보이지 않는다.
 */
object LineReply {
    private const val KEY = "line"
    private const val ACTION_REPLY = "io.github.graviton94.carpediem.LINE_REPLY"
    private const val ACTION_FEEL = "io.github.graviton94.carpediem.LINE_FEEL"
    private const val EXTRA_FEELING = "feeling"
    /** 알림에서 고를 수 있는 마음 (자리가 셋뿐이라). */
    val FEELINGS = listOf(Feeling.JOY, Feeling.CALM, Feeling.WORRY)

    private fun mutable() = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0

    /** 알림의 ‘한 줄 쓰기’ 칸. */
    fun replyAction(context: Context): NotificationCompat.Action {
        val input = RemoteInput.Builder(KEY).setLabel(context.getString(R.string.reply_hint)).build()
        val pi = PendingIntent.getBroadcast(context, 20, Intent(context, LineReplyReceiver::class.java).setAction(ACTION_REPLY),
            PendingIntent.FLAG_UPDATE_CURRENT or mutable())
        return NotificationCompat.Action.Builder(0, context.getString(R.string.reply_action), pi).addRemoteInput(input)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY).setAllowGeneratedReplies(false).build()
    }

    private fun feelAction(context: Context, f: Feeling, i: Int): NotificationCompat.Action {
        val pi = PendingIntent.getBroadcast(context, 21 + i, Intent(context, LineReplyReceiver::class.java).setAction(ACTION_FEEL).putExtra(EXTRA_FEELING, f.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Action.Builder(0, io.github.graviton94.carpediem.ui.Labels.feeling(context, f), pi).build()
    }

    /** 오늘의 한 줄로 남김. 이미 오늘 한 줄이 있으면 그 아래 줄로 덧붙임 (줄 수 · 글자 수 안에서). 남겼으면 true. */
    fun save(context: Context, text: String, today: LocalDate = LocalDate.now()): Boolean {
        val store = Store(context)
        val max = Tokens.Garden.LetGo.maxChars.toInt()
        val t = Lines.clean(text, max, Lines.MAX_LINES); if (t.isEmpty()) return false
        val lines = store.lines
        val old = lines.firstOrNull { it.date == today }
        val next = when {
            old == null -> Lines.add(lines, if (store.keepLines) DayLine(today, t, null) else DayLine(today, "", null))
            store.keepLines -> Lines.edit(lines, today, Lines.clean(old.text + "\n" + t, max, Lines.MAX_LINES), old.feeling)
            else -> lines
        }
        store.lines = next
        // 이어 쓰기 흔적 (그날 쓴 줄만 셈, 나중에 채운 날은 빼고)
        val b = store.backfilled
        val s = Lines.streaks(next.filter { it.date.toEpochDay().toString() !in b }, store.streaks)
        if (s != store.streaks) store.streaks = s
        Widgets.refresh(context)
        return true
    }

    /** 오늘 한 줄의 마음만 바꿈 (기록 남기기를 끈 사람은 마음을 저장하지 않음). */
    fun feel(context: Context, f: Feeling, today: LocalDate = LocalDate.now()) {
        val store = Store(context)
        if (!store.keepLines) return
        val old = store.lines.firstOrNull { it.date == today } ?: return
        store.lines = Lines.edit(store.lines, today, old.text, f)
        Widgets.refresh(context)
    }

    /** 보낸 뒤의 알림: 글 없이 ‘정원에 두었어요’ + 마음 고르기. */
    internal fun posted(context: Context, id: Int) {
        if (!Daily.allowed(context)) return
        Daily.eveningChannel(context)
        val b = NotificationCompat.Builder(context, "evening").setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.reply_done)).setContentText(context.getString(R.string.reply_feel))
            .setOnlyAlertOnce(true).setAutoCancel(true).setTimeoutAfter(10 * 60_000L)
        if (Store(context).keepLines) FEELINGS.forEachIndexed { i, f -> b.addAction(feelAction(context, f, i)) }
        NotificationManagerCompat.from(context).notify(id, b.build())
    }

    internal fun felt(context: Context, id: Int, f: Feeling) {
        if (!Daily.allowed(context)) return
        val n = NotificationCompat.Builder(context, "evening").setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.reply_felt, io.github.graviton94.carpediem.ui.Labels.feeling(context, f)))
            .setContentText(context.getString(R.string.reply_night)).setOnlyAlertOnce(true).setAutoCancel(true).setTimeoutAfter(6_000L).build()
        NotificationManagerCompat.from(context).notify(id, n)
    }

    internal fun handle(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REPLY -> {
                val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY)?.toString().orEmpty()
                if (save(context, text)) posted(context, Evening.ID)
                else NotificationManagerCompat.from(context).cancel(Evening.ID)
            }
            ACTION_FEEL -> {
                val f = Feeling.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_FEELING) } ?: return
                feel(context, f); felt(context, Evening.ID, f)
            }
        }
    }
}

class LineReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = LineReply.handle(context, intent)
}
