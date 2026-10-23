package io.github.graviton94.carpediem.core

/**
 * 기록 옮기기 파일의 규칙. 읽고 쓰는 일 (JSON) 은 앱의 Store 가, 무엇을 받아들이고 어떻게 되살리는지는 여기서 정한다 (시험할 수 있게).
 *
 * 파일 모양: {"app": APP, "v": VERSION, "prefs": {키: {"t": 종류, "v": 값}}, "photos": {이름: base64}}
 * 종류: b (참 · 거짓) · i · l (정수) · f (소수) · s (글) · ss (글 묶음)
 */
object Backup {
    const val APP = "carpediem-backup"
    const val VERSION = 1

    /** 이 앱의 파일이고, 하루의 정보 (생년월일) 가 있을 때만 받아들인다. 아니면 지금 기록을 지우지 않는다. */
    fun accepts(appTag: String?, keys: Set<String>): Boolean = appTag == APP && "birth" in keys

    /** 저장된 값의 종류 표시. 모르는 값이면 null (그 칸은 쓰지 않음). */
    fun typeOf(v: Any?): String? = when (v) {
        is Boolean -> "b"; is Int -> "i"; is Long -> "l"; is Float -> "f"; is String -> "s"
        is Set<*> -> "ss".takeIf { v.all { it is String } }
        else -> null
    }

    /**
     * 파일에서 읽은 값 (참 · 거짓, 글, 글 묶음, 숫자는 글로) 을 종류에 맞게. 맞지 않으면 null.
     * 정수는 소수를 거치지 않고 읽는다 (2^53 보다 큰 Long 도 그대로).
     */
    fun value(type: String?, raw: Any?): Any? {
        if (raw == null) return null
        val text = raw.toString()
        return when (type) {
            "b" -> raw as? Boolean ?: text.toBooleanStrictOrNull()
            "i" -> text.toIntOrNull() ?: text.toDoubleOrNull()?.takeIf { it.isFinite() }?.toInt()
            "l" -> text.toLongOrNull() ?: text.toDoubleOrNull()?.takeIf { it.isFinite() }?.toLong()
            "f" -> text.toFloatOrNull()?.takeIf { it.isFinite() }
            "s" -> raw as? String
            "ss" -> (raw as? Set<*>)?.takeIf { s -> s.all { it is String } }
            else -> null
        }
    }

    /** 사진은 날짜 번호 이름 (예: 20123.jpg) 만. 경로를 품은 이름 (../ 등) 은 버린다. */
    fun isPhotoName(name: String): Boolean = name.endsWith(".jpg") && name.removeSuffix(".jpg").toLongOrNull() != null

    /**
     * 클라우드 백업에 넣을 사진 (1.1.10): 한도 (budget 바이트) 안에서 최근 날부터. 넘치면 오래된 사진만 빠지고 기록은 늘 들어감
     * (Android 자동 백업은 앱당 한도를 넘으면 그 앱 백업 전체가 멈추므로). files = (이름, 크기).
     */
    fun photosWithin(files: List<Pair<String, Long>>, budget: Long): List<String> {
        var used = 0L
        val out = ArrayList<String>()
        for ((name, size) in files.filter { isPhotoName(it.first) }.sortedByDescending { it.first.removeSuffix(".jpg").toLong() }) {
            if (used + size > budget) break
            used += size; out += name
        }
        return out
    }

    /** 폰마다 다른 것: 새 폰에서 다시 묻는다 (알림 허락). */
    val PER_DEVICE = setOf("notifyAsked")
}
