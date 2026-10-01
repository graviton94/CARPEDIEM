package io.github.graviton94.carpediem.data

import io.github.graviton94.carpediem.core.Breath
import io.github.graviton94.carpediem.core.BreathKind
import io.github.graviton94.carpediem.core.Sound
import io.github.graviton94.carpediem.core.Family
import io.github.graviton94.carpediem.core.Person
import io.github.graviton94.carpediem.core.Species
import io.github.graviton94.carpediem.core.DayLine
import io.github.graviton94.carpediem.core.Lines
import android.content.Context
import io.github.graviton94.carpediem.core.GridScale
import io.github.graviton94.carpediem.core.LifeExpectancyTable
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.LifeUnit
import io.github.graviton94.carpediem.core.Quote
import io.github.graviton94.carpediem.core.QuoteBook
import io.github.graviton94.carpediem.core.Question
import io.github.graviton94.carpediem.core.QuestionBook
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
    val questions: QuestionBook by lazy { QuestionBook(assets.open("questions.csv").bufferedReader().readText()) }
    val constellations: io.github.graviton94.carpediem.core.ConstellationBook by lazy { io.github.graviton94.carpediem.core.ConstellationBook(assets.open("constellations.csv").bufferedReader().readText()) }

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

    /** 저녁 한 줄 알림 (그날 한 줄을 아직 보내지 않았을 때만, 고른 시각에). 기본 꺼짐. */
    var eveningNotify: Boolean
        get() = prefs.getBoolean("eveningNotify", false)
        set(v) = prefs.edit().putBoolean("eveningNotify", v).apply()
    var eveningHour: Int
        get() = prefs.getInt("eveningHour", io.github.graviton94.carpediem.design.Tokens.Notify.eveningHour.toInt())
        set(v) = prefs.edit().putInt("eveningHour", v).apply()
    /** 아침 알림을 누르면 숨, 쉼 1분 (하루를 여는 숨). 기본 켬. */
    var morningBreath: Boolean
        get() = prefs.getBoolean("morningBreath", true)
        set(v) = prefs.edit().putBoolean("morningBreath", v).apply()

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

    // ───── 오늘의 질문 · 계절의 편지 ─────
    /** 오늘의 질문 (질문 날이 아니거나 ‘오늘은 문장으로’를 고른 날은 null). 순서는 문장과 같은 seed 로. */
    fun todaysQuestion(date: LocalDate = LocalDate.now()): Question? {
        if (prefs.getLong("questionSkip", -1) == date.toEpochDay()) return null
        val seed = prefs.getString("quoteSeed", null)?.toULongOrNull()?.toLong() ?: 0x5EEDL
        return questions.of(seed, date)
    }
    fun skipQuestion(date: LocalDate = LocalDate.now()) = prefs.edit().putLong("questionSkip", date.toEpochDay()).apply()

    /** 펼쳐 본 편지 ("2027-03"). 연 편지는 봉투가 다시 뜨지 않고 ‘받은 편지’에만. */
    var lettersOpened: Set<String>
        get() = prefs.getStringSet("lettersOpened", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("lettersOpened", v).apply()
    /** 알림으로 이미 알린 편지. */
    var lettersNotified: Set<String>
        get() = prefs.getStringSet("lettersNotified", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("lettersNotified", v).apply()

    /** 돌봄 권하기 (마음을 보낸 뒤 작은 한 가지). 기본 켬. */
    var care: Boolean
        get() = prefs.getBoolean("care", true)
        set(v) = prefs.edit().putBoolean("care", v).apply()

    /** 오늘의 한 줄 (기기 안에만). 떠나보낸 글은 화면에 다시 보이지 않는다. */
    var lines: List<DayLine>
        get() = Lines.decode(prefs.getString("lines", null))
        set(v) = prefs.edit().putString("lines", Lines.encode(v)).apply()

    /** 한 줄 기록만 지우기 (이미 정원에 놓인 이어 쓰기 흔적은 남음). */
    fun clearLines() = prefs.edit().remove("lines").remove("memoryLines").apply()

    /** 기록 남기지 않기를 켜면 보낸 날짜만 남기고 글 · 마음은 저장하지 않는다. */
    var keepLines: Boolean
        get() = prefs.getBoolean("keepLines", true)
        set(v) = prefs.edit().putBoolean("keepLines", v).apply()

    /** 이어 쓰기 흔적을 얻은 날 (7 · 30 · 100 → 날짜). 기록을 지워도 남는다. 저장 형식 `7:epochDay,30:epochDay`. */
    var streaks: Map<Int, LocalDate>
        get() = prefs.getString("streaks", null).orEmpty().split(',').mapNotNull { e ->
            val p = e.split(':'); val n = p.getOrNull(0)?.toIntOrNull(); val d = p.getOrNull(1)?.toLongOrNull()
            if (n != null && d != null) n to LocalDate.ofEpochDay(d) else null
        }.toMap()
        set(v) = prefs.edit().putString("streaks", v.entries.joinToString(",") { "${it.key}:${it.value.toEpochDay()}" }).apply()

    /**
     * 문득 찾아오는 한 줄 (정한 주기 없이, 5 ~ 20일에 한 번쯤 아무 날). 도착한 날은 하루 종일 같은 줄.
     * 앱을 열 때와 아침 알림 때 부른다. 없으면 null.
     */
    fun randomRecall(today: LocalDate = LocalDate.now()): DayLine? {
        val t = io.github.graviton94.carpediem.design.Tokens.Garden.LetGo
        val lines = this.lines
        val seed = prefs.getString("quoteSeed", null)?.toULongOrNull()?.toLong() ?: 0x5EEDL
        if (prefs.getLong("randomOn", -1) == today.toEpochDay()) {
            val d = prefs.getLong("randomPick", -1); return lines.firstOrNull { it.date.toEpochDay() == d }
        }
        val next = if (prefs.contains("randomNext")) LocalDate.ofEpochDay(prefs.getLong("randomNext", 0)) else null
        if (next == null) {
            if (lines.any { it.text.isNotBlank() }) prefs.edit().putLong("randomNext", Lines.nextRandomDay(today, seed, t.randomMinDays.toInt(), t.randomMaxDays.toInt()).toEpochDay()).apply()
            return null
        }
        if (today.isBefore(next)) return null
        val pick = Lines.randomPick(lines, today, seed, t.randomMinAge.toInt()) ?: return null
        prefs.edit().putLong("randomOn", today.toEpochDay()).putLong("randomPick", pick.date.toEpochDay())
            .putLong("randomNext", Lines.nextRandomDay(today, seed, t.randomMinDays.toInt(), t.randomMaxDays.toInt()).toEpochDay()).apply()
        return pick
    }

    /** 시험용: 다음 문득 찾아올 날을 오늘로. */
    fun randomRecallNow(today: LocalDate = LocalDate.now()) = prefs.edit().putLong("randomNext", today.toEpochDay()).remove("randomOn").apply()

    /** 가족의 정원: 함께 앉은 가족 · 반려동물 (나는 빼고, 최대 8). */
    var people: List<Person>
        get() = Family.decode(prefs.getString("people", null))
        set(v) = prefs.edit().putString("people", Family.encode(v)).apply()

    /** 특별한 날 꽃 (인생 달력 위). */
    var specialDays: List<io.github.graviton94.carpediem.core.SpecialDay>
        get() = io.github.graviton94.carpediem.core.SpecialDays.decode(prefs.getString("specialDays", null))
        set(v) = prefs.edit().putString("specialDays", io.github.graviton94.carpediem.core.SpecialDays.encode(v)).apply()

    /** 계절 첫날의 바람: 편지 id ("2026-12") → 한 줄. 석 달 뒤 편지 첫 장에 돌아온다. 저장 형식 `id<TAB>글` 한 줄에 하나. */
    var wishes: Map<String, String>
        get() = prefs.getString("wishes", null).orEmpty().lineSequence().mapNotNull { r -> r.split('\t', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
        set(v) = prefs.edit().putString("wishes", v.entries.joinToString("\n") { "${it.key}\t${it.value.replace('\n', ' ').replace('\t', ' ')}" }).apply()
    /** ‘다음에’를 누른 계절 (그 계절엔 다시 묻지 않음). */
    var wishSkipped: Set<String>
        get() = prefs.getStringSet("wishSkipped", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("wishSkipped", v).apply()
    /** 펼쳐 본 한 해의 정원 (해). */
    var monthsOpened: Set<String>
        get() = prefs.getStringSet("monthsOpened", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("monthsOpened", v).apply()
    var yearsOpened: Set<String>
        get() = prefs.getStringSet("yearsOpened", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("yearsOpened", v).apply()

    /** 기억의 돌 (곁을 떠난 가족 · 반려동물, 넷까지). 정원 · 위젯 · 알림에는 나오지 않는다. */
    var memories: List<Person>
        get() = Family.decode(prefs.getString("memories", null))
        set(v) = prefs.edit().putString("memories", Family.encode(v)).apply()
    /** 기억의 자리에서 보낸 한 줄 (오늘의 한 줄 · 회상 · 편지와 섞이지 않게 따로). */
    var memoryLines: List<DayLine>
        get() = Lines.decode(prefs.getString("memoryLines", null))
        set(v) = prefs.edit().putString("memoryLines", Lines.encode(v)).apply()

    /** 그 사람의 기대수명 (반려동물은 종의 기대수명). */
    fun expectancy(p: Person): Double = Family.expectancy(p, table) {
        val f = io.github.graviton94.carpediem.design.Tokens.Garden.Family
        when (it) { Species.DOG -> f.dogYears; Species.CAT -> f.catYears; Species.OTHER -> f.otherYears }.toDouble()
    }

    // ───── 숨 ─────
    /** 숨 쉰 날과 종류 (시간 · 횟수는 세지 않음). */
    var breaths: List<Pair<LocalDate, BreathKind>>
        get() = Breath.decode(prefs.getString("breaths", null))
        set(v) = prefs.edit().putString("breaths", Breath.encode(v)).apply()
    /** 마지막에 고른 숨 · 분 · 소리 (다음에 그대로). */
    var breathKind: BreathKind
        get() = runCatching { BreathKind.valueOf(prefs.getString("breathKind", null)!!) }.getOrDefault(BreathKind.CALM)
        set(v) = prefs.edit().putString("breathKind", v.name).apply()
    var breathMinutes: Int
        get() = prefs.getInt("breathMinutes", 1)
        set(v) = prefs.edit().putInt("breathMinutes", v).apply()
    var sound: Sound
        get() = runCatching { Sound.valueOf(prefs.getString("sound", null)!!) }.getOrDefault(Sound.WAVES)
        set(v) = prefs.edit().putString("sound", v.name).apply()

    fun eraseAll() = prefs.edit().clear().apply()

    companion object {
        /** 이만큼 쉬었다 돌아오면 달팽이가 놓인다. */
        const val RETURN_AFTER_DAYS = 30

        val defaultQuoteLanguage: QuoteLanguage
            get() = if (java.util.Locale.getDefault().language == "ko") QuoteLanguage.BOTH else QuoteLanguage.ENGLISH
    }
}
