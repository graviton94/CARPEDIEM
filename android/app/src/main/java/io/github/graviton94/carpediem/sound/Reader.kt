package io.github.graviton94.carpediem.sound

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * 오늘의 문장, 소리 내어 (E4): 폰의 읽기 기능으로 낮고 느리게 한 번. 한글 문장 뒤에 영어 문장이 있으면 이어서 영어로.
 * 새 권한 없음. 끝나면 (또는 stop) 읽기 기능을 놓아 준다.
 */
object Reader {
    private var tts: TextToSpeech? = null

    /** parts = (문장, 언어) 차례로. onDone(true) = 끝까지 읽음, false = 읽을 수 없음 (읽기 기능 · 목소리 없음). */
    fun read(context: Context, parts: List<Pair<String, Locale>>, done: (Boolean) -> Unit) {
        stop()
        // 읽기 기능의 알림은 다른 스레드에서 오므로, 화면 쪽 일은 화면 스레드에서 한 번만
        val main = android.os.Handler(android.os.Looper.getMainLooper()); var told = false
        val onDone: (Boolean) -> Unit = { ok -> main.post { if (!told) { told = true; done(ok) } } }
        val todo = parts.filter { it.first.isNotBlank() }
        if (todo.isEmpty()) { onDone(false); return }
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            val t = engine ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) { onDone(false); release(t); return@TextToSpeech }
            t.setSpeechRate(0.85f); t.setPitch(0.95f)
            t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) { if (id == "last") { onDone(true); release(t) } }
                @Deprecated("Deprecated in Java") override fun onError(id: String?) { onDone(false); release(t) }
                override fun onError(id: String?, errorCode: Int) { onDone(false); release(t) }
            })
            var any = false
            todo.forEachIndexed { i, (text, locale) ->
                val ok = t.setLanguage(locale).let { it != TextToSpeech.LANG_MISSING_DATA && it != TextToSpeech.LANG_NOT_SUPPORTED }
                if (!ok) return@forEachIndexed
                any = true
                t.speak(text, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, if (i == todo.lastIndex) "last" else "part$i")
                if (i < todo.lastIndex) t.playSilentUtterance(700, TextToSpeech.QUEUE_ADD, "gap$i")
            }
            if (!any) { onDone(false); release(t) }
        }
        tts = engine
    }

    fun stop() { tts?.let { release(it) } }

    private fun release(t: TextToSpeech) { runCatching { t.stop(); t.shutdown() }; if (tts === t) tts = null }
}
