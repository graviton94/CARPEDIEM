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

    // 인생 달력은 개월로 보기가 기본. 예전 기본값이 남아 있으면 한 번만 개월로 맞춤
    var grid: GridScale
        get() { if (!prefs.getBoolean("gridMonths", false)) prefs.edit().putBoolean("gridMonths", true).putString("grid", GridScale.MONTHS.name).apply()
            return runCatching { GridScale.valueOf(prefs.getString("grid", null)!!) }.getOrDefault(GridScale.MONTHS) }
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

    /** 아침 문장 · 하루 정리 알림 시각 (하루의 몇째 분). 고르지 않았으면 기본 (토큰: 7시 · 22시). */
    var morningMinute: Int
        get() = prefs.getInt("morningMinute", (io.github.graviton94.carpediem.design.Tokens.Notify.hour * 60 + io.github.graviton94.carpediem.design.Tokens.Notify.minute).toInt())
        set(v) = prefs.edit().putInt("morningMinute", v.coerceIn(0, 24 * 60 - 1)).apply()
    var eveningMinute: Int
        get() = prefs.getInt("eveningMinute", (io.github.graviton94.carpediem.design.Tokens.Notify.eveningHour * 60).toInt())
        set(v) = prefs.edit().putInt("eveningMinute", v.coerceIn(0, 24 * 60 - 1)).apply()

    /** 처음 온 사람의 안내: 첫 화면 앞 소개 몇 장을 봤는지 · 정원 둘러보기를 마쳤는지 · 처음 들어가 본 페이지 (기록 · 추억 · 흐름). */
    var introSeen: Boolean
        get() = prefs.getBoolean("introSeen", false)
        set(v) = prefs.edit().putBoolean("introSeen", v).apply()
    var guideDone: Boolean
        get() = prefs.getBoolean("guideDone", false)
        set(v) = prefs.edit().putBoolean("guideDone", v).apply()
    /** 첫 일주일 길잡이에서 이미 눌러 본 권유 (core FirstWeek 의 키). */
    var nudgesSeen: Set<String>
        get() = prefs.getStringSet("nudgesSeen", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("nudgesSeen", v).apply()
    var nudgeShown: Set<String>
        get() = prefs.getStringSet("nudgeShown", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("nudgeShown", v).apply()
    /** 마지막으로 백업 파일을 저장한 날 (epochDay, 없으면 -1). */
    var lastBackup: Long
        get() = prefs.getLong("lastBackup", -1)
        set(v) = prefs.edit().putLong("lastBackup", v).apply()
    var pageHints: Set<String>
        get() = prefs.getStringSet("pageHints", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("pageHints", v).apply()

    /** 디버그 빌드 화면 확인용으로만 하루 번호를 정한다. */
    fun overrideHaruSeed(v: Long) = prefs.edit().putLong("haruSeed", v and 0xFFFFFFFFL).apply()

    /** 하루 한 번 알림 (오늘의 문장 · 새로 놓인 것). 기본은 끔. */
    var notify: Boolean
        get() = prefs.getBoolean("notify", false)
        set(v) = prefs.edit().putBoolean("notify", v).apply()

    /** 저녁 한 줄 알림 (그날 한 줄을 아직 보내지 않았을 때만, 고른 시각에). 기본 꺼짐. */
    var notifyAsked: Boolean
        get() = prefs.getBoolean("notifyAsked", false)
        set(v) = prefs.edit().putBoolean("notifyAsked", v).apply()

    var eveningNotify: Boolean
        get() = prefs.getBoolean("eveningNotify", false)
        set(v) = prefs.edit().putBoolean("eveningNotify", v).apply()
    var tomorrowNotify: Boolean
        get() = prefs.getBoolean("tomorrowNotify", false)
        set(v) = prefs.edit().putBoolean("tomorrowNotify", v).apply()
    /** 나중에 채운 날 (다른 날의 한 줄, epochDay): 이어 쓰기 흔적은 그날 쓴 줄로만 센다. */
    var backfilled: Set<String>
        get() = prefs.getStringSet("backfilled", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("backfilled", v).apply()
    /** 오늘의 질문 받기 (기본 켬). 끄면 질문 날에도 오늘의 문장. */
    var questionsOn: Boolean
        get() = prefs.getBoolean("questionsOn", true)
        set(v) = prefs.edit().putBoolean("questionsOn", v).apply()
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

    /** 마지막으로 앱을 연 날 (알림이 오래 안 온 사람에게 늘지 않고 쉬게, core Nudges). */
    val lastOpen: LocalDate? get() = if (prefs.contains("lastOpen")) LocalDate.ofEpochDay(prefs.getLong("lastOpen", 0)) else null

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
        if (!questionsOn || prefs.getLong("questionSkip", -1) == date.toEpochDay()) return null
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

    /** 아침 씨앗 (04): 날마다 하나, 저녁에 꽃 · 쉼. */
    var seeds: List<io.github.graviton94.carpediem.core.Seed>
        get() = io.github.graviton94.carpediem.core.Seeds.decode(prefs.getString("seeds", null))
        set(v) = prefs.edit().putString("seeds", io.github.graviton94.carpediem.core.Seeds.encode(v)).apply()
    var seedsOn: Boolean
        get() = prefs.getBoolean("seedsOn", true)
        set(v) = prefs.edit().putBoolean("seedsOn", v).apply()
    /** 아침 씨앗을 ‘다음에’ 한 날 (그날은 다시 묻지 않음). */
    var seedSkipped: LocalDate?
        get() = prefs.getLong("seedSkip", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let { LocalDate.ofEpochDay(it) }
        set(v) = prefs.edit().apply { if (v == null) remove("seedSkip") else putLong("seedSkip", v.toEpochDay()) }.apply()
    /** 걱정한 밤 다음 아침의 한마디 (06) 를 보여 준 날. */
    var comfortShown: LocalDate?
        get() = prefs.getLong("comfortShown", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let { LocalDate.ofEpochDay(it) }
        set(v) = prefs.edit().apply { if (v == null) remove("comfortShown") else putLong("comfortShown", v.toEpochDay()) }.apply()
    /** 미래의 나에게 (10): 나무 밑 항아리들. */
    var capsules: List<io.github.graviton94.carpediem.core.Capsule>
        get() = io.github.graviton94.carpediem.core.Capsules.decode(prefs.getString("capsules", null))
        set(v) = prefs.edit().putString("capsules", io.github.graviton94.carpediem.core.Capsules.encode(v)).apply()
    /** 하루의 숨결을 손끝으로 (05): 숨 쉬는 동안 떨림으로. */
    var breathTouch: Boolean
        get() = prefs.getBoolean("breathTouch", false)
        set(v) = prefs.edit().putBoolean("breathTouch", v).apply()

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

    // ───── 정원 꾸밈 (core GardenDecor) ─────
    /** 돌멍하기를 한 날 (epoch day). 열 날마다 이끼에 봉오리 하나. */
    var gazeDays: Set<String>
        get() = prefs.getStringSet("gazeDays", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("gazeDays", v).apply()
    /** 받은 계절 한 장 (SeasonCard.id). 그 계절에 정원을 열면 하나. */
    var seasonCards: Set<String>
        get() = prefs.getStringSet("seasonCards", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("seasonCards", v).apply()
    /** 그림을 보낸 날 (그날 정원에 돌아오면 둘에 한 번 비눗방울). */
    var sharedOn: LocalDate?
        get() = if (prefs.contains("sharedOn")) LocalDate.ofEpochDay(prefs.getLong("sharedOn", 0)) else null
        set(v) = prefs.edit().apply { if (v == null) remove("sharedOn") else putLong("sharedOn", v.toEpochDay()) }.apply()
    /** 이미 본 우연한 순간 (같은 날 · 같은 주에 두 번 오지 않게): "snail:2026-10-02" 처럼. */
    var chancesShown: Set<String>
        get() = prefs.getStringSet("chancesShown", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("chancesShown", v).apply()
    /** 처음 만난 날 (앨범 ‘만난 순간’): "rainbow:2026-10-02" 처럼, 순간마다 하나. */
    var chancesMet: Set<String>
        get() = prefs.getStringSet("chancesMet", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("chancesMet", v).apply()
    /** 절기 한 줄을 알린 날 (그날 정원을 처음 열 때 한 번, S1). */
    var termNoted: String?
        get() = prefs.getString("termNoted", null)
        set(v) = prefs.edit().putString("termNoted", v).apply()
    /** 돌에게 건넨 이번 계절의 조각 (core Offerings, R1). */
    var offerings: List<io.github.graviton94.carpediem.core.Offering>
        get() = io.github.graviton94.carpediem.core.Offerings.decode(prefs.getString("offerings", null))
        set(v) = prefs.edit().putString("offerings", io.github.graviton94.carpediem.core.Offerings.encode(v)).apply()
    /** 기억의 주에 한 줄 알림을 켠 기억의 돌 id (기본은 모두 꺼짐, R2). */
    var memoryWeekOn: Set<String>
        get() = prefs.getStringSet("memoryWeekOn", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("memoryWeekOn", v).apply()
    /** 기억의 주 알림을 보낸 것 ("id:2026-10-20", 해마다 한 번). */
    var memoryWeekSent: Set<String>
        get() = prefs.getStringSet("memoryWeekSent", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("memoryWeekSent", v).apply()
    /** ‘올해의 정원을 한 장으로’ 를 물은 해 (해마다 한 번, S2). */
    var gardenYearAsked: Int
        get() = prefs.getInt("gardenYearAsked", 0)
        set(v) = prefs.edit().putInt("gardenYearAsked", v).apply()
    /** 달팽이 손님이 길에 나온 때 (ms). 한 시간쯤 머물며 천천히 건넘. */
    var snailAt: Long
        get() = prefs.getLong("snailAt", 0L)
        set(v) = prefs.edit().putLong("snailAt", v).apply()
    /** 마지막으로 정원에서 본 꾸밈 (새로 생긴 것을 한 번만 알리려고). 처음엔 null = 조용히 기억만. */
    var decorSeen: String?
        get() = prefs.getString("decorSeen", null)
        set(v) = prefs.edit().putString("decorSeen", v).apply()

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

    /**
     * 기록 옮기기: 앱 안의 모든 것 (설정 · 한 줄 · 가족 · 기억의 돌 · 특별한 날 …) 을 JSON 한 덩이로.
     * 새 폰에서 [restore] 로 그대로 들여온다. 파일은 사람이 고른 곳에만 저장된다 (앱은 어디로도 보내지 않음).
     */
    fun backup(): String {
        val all = org.json.JSONObject()
        prefs.all.forEach { (k, v) ->
            val e = org.json.JSONObject()
            when (v) {
                is Boolean -> e.put("t", "b").put("v", v)
                is Int -> e.put("t", "i").put("v", v)
                is Long -> e.put("t", "l").put("v", v)
                is Float -> e.put("t", "f").put("v", v.toDouble())
                is String -> e.put("t", "s").put("v", v)
                is Set<*> -> e.put("t", "ss").put("v", org.json.JSONArray(v.filterIsInstance<String>()))
                else -> return@forEach
            }
            all.put(k, e)
        }
        return org.json.JSONObject().put("app", BACKUP_APP).put("v", 1).put("prefs", all).toString()
    }

    /** [backup] 으로 만든 글을 들여온다. 이 앱의 파일이 아니거나 읽을 수 없으면 아무것도 바꾸지 않고 false. */
    fun restore(json: String): Boolean {
        val root = runCatching { org.json.JSONObject(json) }.getOrNull() ?: return false
        if (root.optString("app") != BACKUP_APP) return false
        val all = root.optJSONObject("prefs") ?: return false
        if (!all.has("birth")) return false   // 하루의 정보 (생년월일) 가 없는 파일로는 지금 기록을 지우지 않음
        val ed = prefs.edit().clear().putBoolean("gridMonths", true)   // 되살린 인생 달력 단위는 그대로
        for (k in all.keys()) {
            val e = all.optJSONObject(k) ?: continue
            when (e.optString("t")) {
                "b" -> ed.putBoolean(k, e.optBoolean("v"))
                "i" -> ed.putInt(k, e.optInt("v"))
                "l" -> ed.putLong(k, e.optLong("v"))
                "f" -> ed.putFloat(k, e.optDouble("v").toFloat())
                "s" -> ed.putString(k, e.optString("v"))
                "ss" -> ed.putStringSet(k, e.optJSONArray("v")?.let { a -> (0 until a.length()).map { a.optString(it) }.toSet() } ?: emptySet())
            }
        }
        // 기록을 들여온 사람은 처음 온 사람이 아님: 소개 · 둘러보기 · 페이지 안내 · 하루를 만나는 장면은 건너뜀
        // 알림 허락은 폰마다 다르니 새 폰에서 다시 물음
        ed.remove("notifyAsked")
        ed.putBoolean("introSeen", true).putBoolean("guideDone", true).putBoolean("meetPending", false)
            .putStringSet("pageHints", io.github.graviton94.carpediem.ui.PAGE_HINTS)
            .putStringSet("nudgesSeen", io.github.graviton94.carpediem.core.FirstWeek.STEPS.map { it.first }.toSet())
        return ed.commit()
    }

    companion object {
        /** 기록 옮기기 파일의 표 (다른 앱의 파일을 들여오지 않게). */
        private const val BACKUP_APP = "carpediem-backup"
        /** 이만큼 쉬었다 돌아오면 달팽이가 놓인다. */
        const val RETURN_AFTER_DAYS = 30

        val defaultQuoteLanguage: QuoteLanguage
            // 폰의 말이 한국어 · 일본어 · 번체 중국어면 그 말 + 영어, 그 밖 (영어 포함) 은 영어
            get() = java.util.Locale.getDefault().let { if (io.github.graviton94.carpediem.core.Langs.of(it.language, it.country, it.script) != "en") QuoteLanguage.BOTH else QuoteLanguage.ENGLISH }
    }
}
