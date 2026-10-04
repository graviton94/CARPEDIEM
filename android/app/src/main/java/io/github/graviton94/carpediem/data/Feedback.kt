package io.github.graviton94.carpediem.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import io.github.graviton94.carpediem.R
import java.io.File

/**
 * 의견 보내기와 오류 기록. 서버로 보내는 것은 없다: 앱이 멈추면 그 자리를 폰 안 파일 하나에만 적어 두고,
 * 사용자가 ‘보내기’를 누를 때만 메일에 담긴다. 기록 (한 줄 · 가족 · 날짜) 은 어떤 경우에도 담지 않는다.
 */
object Feedback {
    private const val CRASH_FILE = "last_crash.txt"
    private const val MAX = 6000

    /** 앱이 멈출 때 그 자리를 적어 두고, 원래 처리 (앱 닫기) 로 넘긴다. Application.onCreate 에서 한 번. */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                val text = "${java.time.LocalDateTime.now()} · ${t.name}\n" + e.stackTraceToString()
                File(app.filesDir, CRASH_FILE).writeText(text.take(MAX))
            }
            previous?.uncaughtException(t, e)
        }
    }

    /** 지난번에 멈춘 기록 (없으면 null). */
    fun lastCrash(context: Context): String? = runCatching { File(context.filesDir, CRASH_FILE).takeIf { it.exists() }?.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
    fun clearCrash(context: Context) { runCatching { File(context.filesDir, CRASH_FILE).delete() } }

    /** 기기 · 앱 정보 몇 줄 (기록은 담지 않음). */
    fun deviceInfo(context: Context): String {
        val pkg = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        @Suppress("DEPRECATION")
        val code = pkg?.let { if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong() }
        val locale = context.resources.configuration.locales[0]
        return listOf(
            "App ${pkg?.versionName ?: "?"} ($code)",
            "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            "${Build.MANUFACTURER} ${Build.MODEL}",
            "${locale.toLanguageTag()} · font ${context.resources.configuration.fontScale}",
        ).joinToString("\n")
    }

    /** 메일 앱으로 의견 보내기: 제목 · 기기 정보 (· 멈춘 기록) 를 미리 채움. 받는 곳이 없으면 false. */
    fun send(context: Context, crash: String? = null): Boolean {
        val to = context.getString(R.string.contact_email).ifBlank { return false }
        val body = "\n\n\n---\n" + deviceInfo(context) + (crash?.let { "\n\n--- crash ---\n" + it.take(MAX) } ?: "")
        val subject = context.getString(if (crash != null) R.string.feedback_crashSubject else R.string.feedback_subject)
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            .putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, body).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
