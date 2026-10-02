package io.github.graviton94.carpediem.data

import android.content.Context
import io.github.graviton94.carpediem.core.Langs
import io.github.graviton94.carpediem.core.Question
import io.github.graviton94.carpediem.core.Quote

/**
 * 오늘의 문장 · 질문을 어느 말로: 폰의 말 (한국어 · 일본어 · 번체 중국어) 문장이 먼저, 고르면 영어도 함께.
 * 영어 폰 (그 밖의 말 포함) 은 영어 문장 하나만. 설정의 ‘한글 · 영문 · 둘 다’ 는 ‘이 말 · 영어 · 둘 다’ 로 읽는다 (KOREAN = 이 말).
 */
object Words {
    fun lang(ctx: Context): String = ctx.resources.configuration.locales[0].let { Langs.of(it.language, it.country, it.script) }

    /** 고르는 칸이 필요한지 (영어 폰이면 영어 하나뿐이라 없음). */
    fun choosable(ctx: Context) = lang(ctx) != "en"

    fun main(q: Quote, l: QuoteLanguage, lang: String): String = if (l == QuoteLanguage.ENGLISH || lang == "en") q.english else q.text(lang)
    fun second(q: Quote, l: QuoteLanguage, lang: String): String? = if (l == QuoteLanguage.BOTH && lang != "en") q.english else null
    fun main(q: Question, l: QuoteLanguage, lang: String): String = if (l == QuoteLanguage.ENGLISH || lang == "en") q.english else q.text(lang)
    fun second(q: Question, l: QuoteLanguage, lang: String): String? = if (l == QuoteLanguage.BOTH && lang != "en") q.english else null
}
