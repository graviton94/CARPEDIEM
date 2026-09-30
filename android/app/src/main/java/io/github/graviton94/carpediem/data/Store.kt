package io.github.graviton94.carpediem.data

import android.content.Context
import io.github.graviton94.carpediem.core.GridScale
import io.github.graviton94.carpediem.core.LifeExpectancyTable
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.LifeUnit
import io.github.graviton94.carpediem.core.Quote
import io.github.graviton94.carpediem.core.QuoteBook
import io.github.graviton94.carpediem.core.Sex
import java.time.LocalDate
import kotlin.random.Random

enum class QuoteLanguage { KOREAN, ENGLISH, BOTH }

/** 앱과 위젯이 함께 읽는 저장소 (같은 앱 프로세스의 SharedPreferences). */
class Store(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("carpediem", Context.MODE_PRIVATE)
    private val assets = context.applicationContext.assets

    val table: LifeExpectancyTable by lazy { LifeExpectancyTable(assets.open("life-expectancy.csv").bufferedReader().readText()) }
    val book: QuoteBook by lazy { QuoteBook(assets.open("quotes.csv").bufferedReader().readText()) }

    var profile: LifeProfile?
        get() {
            if (!prefs.contains("birth")) return null
            return LifeProfile(
                birthDate = LocalDate.ofEpochDay(prefs.getLong("birth", 0)),
                countryCode = prefs.getString("country", LifeExpectancyTable.WORLD) ?: LifeExpectancyTable.WORLD,
                sex = runCatching { Sex.valueOf(prefs.getString("sex", "OTHER")!!) }.getOrDefault(Sex.OTHER),
                customExpectancy = if (prefs.contains("custom")) prefs.getFloat("custom", 0f).toDouble() else null,
            )
        }
        set(p) = prefs.edit().apply {
            if (p == null) {
                remove("birth"); remove("country"); remove("sex"); remove("custom")
            } else {
                putLong("birth", p.birthDate.toEpochDay()); putString("country", p.countryCode); putString("sex", p.sex.name)
                if (p.customExpectancy != null) putFloat("custom", p.customExpectancy!!.toFloat()) else remove("custom")
            }
        }.apply()

    var quoteLanguage: QuoteLanguage
        get() = runCatching { QuoteLanguage.valueOf(prefs.getString("quoteLanguage", null)!!) }.getOrDefault(defaultQuoteLanguage)
        set(v) = prefs.edit().putString("quoteLanguage", v.name).apply()

    var unit: LifeUnit
        get() = runCatching { LifeUnit.valueOf(prefs.getString("unit", null)!!) }.getOrDefault(LifeUnit.DAYS)
        set(v) = prefs.edit().putString("unit", v.name).apply()

    var grid: GridScale
        get() = runCatching { GridScale.valueOf(prefs.getString("grid", null)!!) }.getOrDefault(GridScale.MONTHS)
        set(v) = prefs.edit().putString("grid", v.name).apply()

    /** 처음 부를 때 무작위 seed 와 시작일을 만든다. */
    fun ensureQuoteSeed(today: LocalDate = LocalDate.now()) {
        if (prefs.contains("quoteSeed")) return
        prefs.edit().putString("quoteSeed", Random.nextLong().toULong().toString()).putLong("quoteStart", today.toEpochDay()).apply()
    }

    /** 오늘의 문장. 앱에서 ‘다음 문장’을 누른 횟수는 그날에만 적용된다. */
    fun todaysQuote(date: LocalDate = LocalDate.now()): Quote? {
        val seed = prefs.getString("quoteSeed", null)?.toULongOrNull() ?: 0x5EEDuL
        val today = date.toEpochDay()
        val start = prefs.getLong("quoteStart", today)
        val skip = if (prefs.getLong("quoteSkipDay", -1) == today) prefs.getInt("quoteSkip", 0) else 0
        return book.quote((today - start).toInt(), seed, skip)
    }

    fun skipQuote(date: LocalDate = LocalDate.now()) {
        val today = date.toEpochDay()
        val current = if (prefs.getLong("quoteSkipDay", -1) == today) prefs.getInt("quoteSkip", 0) else 0
        prefs.edit().putLong("quoteSkipDay", today).putInt("quoteSkip", current + 1).apply()
    }

    fun eraseAll() = prefs.edit().clear().apply()

    companion object {
        val defaultQuoteLanguage: QuoteLanguage
            get() = if (java.util.Locale.getDefault().language == "ko") QuoteLanguage.BOTH else QuoteLanguage.ENGLISH
    }
}
