package io.github.graviton94.carpediem.core

/**
 * 앱의 말: ko · en · ja · zh-TW (번체). 데이터 CSV 는 한글 · 영문 칸에 더해 日本語 · 繁體中文 칸을 둔다.
 * 그 말의 칸이 비었으면 영어로.
 */
object Langs {
    val ALL = listOf("ko", "en", "ja", "zh-TW")
    /** 한글 · 영문 말고 더 있는 말과 그 CSV 칸 이름. */
    val COLUMNS = mapOf("ja" to "日本語", "zh-TW" to "繁體中文")

    fun extra(r: Map<String, String>): Map<String, String> =
        COLUMNS.mapNotNull { (lang, col) -> r[col]?.takeIf { it.isNotBlank() }?.let { lang to it } }.toMap()

    fun pick(lang: String, korean: String, english: String, extra: Map<String, String>): String = when (lang) {
        "ko" -> korean
        "en" -> english
        else -> extra[lang] ?: english
    }

    /** 폰의 말 (언어 · 지역 · 글자) 을 앱의 말로: 한국어 · 일본어 · 번체 중국어 (대만 · 홍콩 · 마카오, 또는 Hant), 나머지는 영어. */
    fun of(language: String, country: String = "", script: String = ""): String = when (language.lowercase()) {
        "ko" -> "ko"
        "ja" -> "ja"
        "zh" -> if (script.equals("Hant", true) || country.uppercase() in setOf("TW", "HK", "MO")) "zh-TW" else "en"
        else -> "en"
    }
}
