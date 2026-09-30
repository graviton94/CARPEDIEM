package io.github.graviton94.carpediem.data

import io.github.graviton94.carpediem.core.DayLine
import io.github.graviton94.carpediem.core.Lines
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

/** 화면 디자인: 처음 만든 유리 버전과 손그림 정원 버전. 설정에서 바꿀 수 있다. */
enum class Design { GLASS, GARDEN }

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

    var design: Design
        get() = runCatching { Design.valueOf(prefs.getString("design", null)!!) }.getOrDefault(Design.GARDEN)
        set(v) = prefs.edit().putString("design", v.name).apply()

    /** 하루(조약돌)의 번호. 처음 부를 때 한 번 정해지고 바뀌지 않는다 (32비트, design/pebble.md). */
    val haruSeed: Long
        get() {
            if (!prefs.contains("haruSeed")) prefs.edit().putLong("haruSeed", Random.nextLong(0, 1L shl 32)).apply()
            return prefs.getLong("haruSeed", 0)
        }

    /** 온보딩을 마친 뒤 ‘하루를 만났습니다’ 화면을 아직 보지 않았는지. */
    var meetPending: Boolean
        get() = prefs.getBoolean("meetPending", false)
        set(v) = prefs.edit().putBoolean("meetPending", v).apply()

    /** 디버그 빌드 화면 확인용으로만 하루 번호를 정한다. */
    fun overrideHaruSeed(v: Long) = prefs.edit().putLong("haruSeed", v and 0xFFFFFFFFL).apply()

    /** 하루 한 번 알림 (오늘의 문장 · 새로 놓인 것). 기본은 끔. */
    var notify: Boolean
        get() = prefs.getBoolean("notify", false)
        set(v) = prefs.edit().putBoolean("notify", v).apply()

    /** 설정의 버전 글자를 여러 번 누르면 켜지는 개발자 모드. 시험용 항목만 보인다. */
    var devMode: Boolean
        get() = prefs.getBoolean("devMode", false)
        set(v) = prefs.edit().putBoolean("devMode", v).apply()

    /** 알림으로 이미 알린 놓인 것 (같은 것을 두 번 알리지 않음). */
    var notifiedMoments: Set<String>
        get() = prefs.getStringSet("notifiedMoments", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("notifiedMoments", v).apply()

    /** 시험용: 놓이는 것을 날짜와 상관없이 모두 보여 준다. */
    var previewAll: Boolean
        get() = prefs.getBoolean("previewAll", false)
        set(v) = prefs.edit().putBoolean("previewAll", v).apply()

    /** 함께 시작한 날 (오늘의 문장 시작일과 같다). */
    val startDate: LocalDate get() = LocalDate.ofEpochDay(prefs.getLong("quoteStart", LocalDate.now().toEpochDay()))
    val firstSkip: LocalDate? get() = if (prefs.contains("firstSkip")) LocalDate.ofEpochDay(prefs.getLong("firstSkip", 0)) else null
    val returned: LocalDate? get() = if (prefs.contains("returned")) LocalDate.ofEpochDay(prefs.getLong("returned", 0)) else null

    /** 앱을 열 때마다 부른다. 마지막으로 연 날에서 오래 지났으면 ‘돌아온 날’로 남긴다. */
    fun markOpened(today: LocalDate = LocalDate.now()) {
        val last = if (prefs.contains("lastOpen")) prefs.getLong("lastOpen", 0) else null
        val e = prefs.edit().putLong("lastOpen", today.toEpochDay())
        if (last != null && today.toEpochDay() - last >= RETURN_AFTER_DAYS && !prefs.contains("returned")) e.putLong("returned", today.toEpochDay())
        e.apply()
    }

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
        val e = prefs.edit().putLong("quoteSkipDay", today).putInt("quoteSkip", current + 1)
        if (!prefs.contains("firstSkip")) e.putLong("firstSkip", today)
        e.apply()
    }

    /** 오늘의 한 줄 (기기 안에만). 떠나보낸 글은 화면에 다시 보이지 않는다. */
    var lines: List<DayLine>
        get() = Lines.decode(prefs.getString("lines", null))
        set(v) = prefs.edit().putString("lines", Lines.encode(v)).apply()

    fun eraseAll() = prefs.edit().clear().apply()

    companion object {
        /** 이만큼 쉬었다 돌아오면 달팽이가 놓인다. */
        const val RETURN_AFTER_DAYS = 30

        val defaultQuoteLanguage: QuoteLanguage
            get() = if (java.util.Locale.getDefault().language == "ko") QuoteLanguage.BOTH else QuoteLanguage.ENGLISH
    }
}
