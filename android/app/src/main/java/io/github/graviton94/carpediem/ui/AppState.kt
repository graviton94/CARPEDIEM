package io.github.graviton94.carpediem.ui

import io.github.graviton94.carpediem.core.BreathKind
import io.github.graviton94.carpediem.core.Person
import java.time.LocalDateTime
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.core.Lines
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.core.DayLine
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.CountryLife
import io.github.graviton94.carpediem.core.GridScale
import io.github.graviton94.carpediem.core.LifeExpectancyTable
import io.github.graviton94.carpediem.core.LifePeriod
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.LifeUnit
import io.github.graviton94.carpediem.core.PeriodProgress
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.core.Sex
import io.github.graviton94.carpediem.data.Design
import io.github.graviton94.carpediem.data.QuoteLanguage
import io.github.graviton94.carpediem.data.Store
import io.github.graviton94.carpediem.widget.Widgets
import java.time.LocalDate
import java.util.Locale

/** 화면이 보는 상태. 바뀌면 저장하고 위젯을 새로 그린다. */
class AppState(private val context: Context) {
    val store = Store(context)
    var profile by mutableStateOf(store.profile)
        private set
    var quote by mutableStateOf(store.todaysQuote())
        private set
    var quoteLanguage by mutableStateOf(store.quoteLanguage)
        private set
    var unit by mutableStateOf(store.unit)
        private set
    var grid by mutableStateOf(store.grid)
        private set
    var design by mutableStateOf(store.design)
        private set
    var meetPending by mutableStateOf(store.meetPending)
        private set
    var previewAll by mutableStateOf(store.previewAll)
        private set
    var notify by mutableStateOf(store.notify)
        private set
    var devMode by mutableStateOf(store.devMode)
        private set
    /** 디버그 빌드에서 화면 확인용으로 시각을 고정할 때만 쓴다. 평소에는 null (폰 시각). */
    var fixedNow by mutableStateOf<java.time.LocalDateTime?>(null)
    /** 문장을 넘기면 하루가 한 번 깜빡인다. */
    var blinkKick by mutableStateOf(0)
        private set
    /** 온보딩 · 설정에서 고치는 중인 정보 (나라 선택 화면을 다녀와도 유지). */
    var draft by mutableStateOf<LifeProfile?>(null)

    init { store.ensureQuoteSeed(); quote = store.todaysQuote() }
    // question 은 fixedNow 를 정한 뒤 (MainActivity) · 날이 바뀔 때 refreshQuestion 으로 채운다

    fun save(p: LifeProfile) { store.profile = p; profile = p; Widgets.refresh(context) }
    /** 온보딩을 마칠 때. 정원 디자인이면 하루를 만나는 화면을 먼저 보여 준다. */
    fun begin(p: LifeProfile) { save(p); if (design == Design.GARDEN) { store.meetPending = true; meetPending = true } }
    fun finishMeet() { store.meetPending = false; meetPending = false }
    fun changeDesign(v: Design) { store.design = v; design = v; Widgets.refresh(context) }
    fun changePreviewAll(v: Boolean) { store.previewAll = v; previewAll = v }
    fun opened() { store.markOpened(); checkRandomRecall() }
    fun changeNotify(v: Boolean) { store.notify = v; notify = v; io.github.graviton94.carpediem.notify.Daily.schedule(context, v) }
    fun unlockDev() { store.devMode = true; devMode = true }
    fun nextQuote() { store.skipQuote(); quote = store.todaysQuote(); blinkKick++; Widgets.refresh(context) }
    fun refreshQuote() { quote = store.todaysQuote() }
    fun changeQuoteLanguage(v: QuoteLanguage) { store.quoteLanguage = v; quoteLanguage = v; Widgets.refresh(context) }
    /** 홈에서 칩으로 바꾸면 이번에만 (다음에 열면 기본 단위로). 기본은 설정에서 고정한다. 위젯도 기본 단위를 쓴다. */
    fun changeUnit(v: LifeUnit) { unit = v }
    fun changeGrid(v: GridScale) { grid = v }
    var defaultUnit by mutableStateOf(store.unit)
        private set
    var defaultGrid by mutableStateOf(store.grid)
        private set
    fun changeDefaultUnit(v: LifeUnit) { store.unit = v; defaultUnit = v; unit = v; Widgets.refresh(context) }
    fun changeDefaultGrid(v: GridScale) { store.grid = v; defaultGrid = v; grid = v }
    /** 앱을 다시 열면 기본 단위로 돌아간다. */
    fun resetViewToDefaults() { unit = defaultUnit; grid = defaultGrid }
    // ───── 오늘의 한 줄 ─────
    /** 보낸 한 줄들 (기기 안에만). 화면에는 ‘몇 해 전 오늘’로만 드물게 돌아온다. */
    var lines by mutableStateOf(store.lines)
        private set
    /** 오늘 이미 한 줄을 떠나보냈는지 (날이 바뀌면 다시 쓸 수 있다). */
    val sentOn: LocalDate? get() = lines.lastOrNull()?.date
    var keepLines by mutableStateOf(store.keepLines)
        private set
    /** 이어 쓰기 흔적 (7 · 30 · 100일 → 얻은 날). */
    var streaks by mutableStateOf(store.streaks)
        private set
    /** 보낸 뒤 잠깐 떠오르는 한마디 (마음에 맞춰). 보이고 나면 null. */
    var toast by mutableStateOf<String?>(null)

    /** 보낸 뒤 창의 제목 (기쁨 · 고마움 · 희망을 누군가에게 보냈을 때만 “엄마에게 보냈어요”). */
    var toastTitle by mutableStateOf<String?>(null)

    fun letGo(text: String, feeling: Feeling?, to: String? = null, today: LocalDate = (fixedNow ?: LocalDateTime.now()).toLocalDate()) {
        val t = Lines.clean(text, Tokens.Garden.LetGo.maxChars.toInt()); if (t.isEmpty()) return
        // 질문에 답한 한 줄이면 질문 번호도 함께
        val q = answering?.id; answering = null
        // 기록 남기지 않기: 날짜만 (이어 쓰기 흔적은 이어 간다)
        val line = if (keepLines) DayLine(today, t, feeling, to, q) else DayLine(today, "", null, to)
        val person = people.firstOrNull { it.id == to }
        toastTitle = if (person != null && feeling in setOf(Feeling.JOY, Feeling.THANKS, Feeling.HOPE)) context.getString(R.string.letgo_modalTo, person.name) else null
        val next = Lines.add(lines, line)
        store.lines = next; lines = next
        val s = Lines.streaks(next, streaks); if (s != streaks) { store.streaks = s; streaks = s }
        toast = Labels.letGoMessage(context, feeling)
    }
    fun changeKeepLines(v: Boolean) {
        store.keepLines = v; keepLines = v
        // 끄는 순간 지금까지의 글도 지운다 (날짜는 남겨 흔적을 잇는다)
        if (!v) { val dates = lines.map { DayLine(it.date, "", null) }; store.lines = dates; lines = dates }
    }
    fun clearLines() { store.clearLines(); lines = emptyList(); randomLine = null }
    /** 문득 다시 찾아온 지난 한 줄 (있는 날만). */
    var randomLine by mutableStateOf<DayLine?>(null)
        private set
    fun checkRandomRecall(today: LocalDate = (fixedNow ?: LocalDateTime.now()).toLocalDate()) { randomLine = if (keepLines) store.randomRecall(today) else null }
    /** 시험용 (개발자 모드): 45일 전 한 줄을 넣고 오늘 문득 찾아오게. */
    fun addSampleRandom(today: LocalDate = (fixedNow ?: LocalDateTime.now()).toLocalDate()) {
        val next = Lines.add(lines, DayLine(today.minusDays(45), context.getString(R.string.recall_sampleOld), Feeling.CALM)); store.lines = next; lines = next
        store.randomRecallNow(today); checkRandomRecall(today)
    }
    // ───── 오늘의 질문 ─────
    private fun nowDate(): LocalDate = (fixedNow ?: LocalDateTime.now()).toLocalDate()
    /** 오늘의 질문 (질문 날에만). 부를 때마다 폰 시각으로 다시 본다. */
    var question by mutableStateOf<io.github.graviton94.carpediem.core.Question?>(null)
        private set
    fun refreshQuestion() { if (!previewQ) question = store.todaysQuestion(nowDate()) }
    private var previewQ = false
    /** ‘한 줄로 답하기’를 눌러 지금 답하는 질문 (보내면 null). */
    var answering by mutableStateOf<io.github.graviton94.carpediem.core.Question?>(null)
    fun answer() { answering = question }
    /** 시험용 (캡처): 질문 날이 아니어도 오늘 질문 하나를 띄운다. */
    fun previewQuestion() { previewQ = true; question = store.questions.order(store.haruSeed).first() }
    fun skipQuestion() { store.skipQuestion(nowDate()); previewQ = false; question = null; answering = null }

    // ───── 계절의 편지 ─────
    var lettersOpened by mutableStateOf(store.lettersOpened)
        private set
    /** 이번 달에 도착해 아직 펼치지 않은 편지 (기록 남기기를 끄면 오지 않음). */
    fun letterDue(today: LocalDate = nowDate()): io.github.graviton94.carpediem.core.Letter? {
        if (!keepLines) return null
        val day = io.github.graviton94.carpediem.core.Letters.due(today) ?: return null
        return io.github.graviton94.carpediem.core.Letters.of(lines, day, Tokens.Garden.Letter.minLines.toInt())?.takeIf { it.id !in lettersOpened }
    }
    fun received(today: LocalDate = nowDate()): List<io.github.graviton94.carpediem.core.Letter> =
        if (!keepLines) emptyList() else io.github.graviton94.carpediem.core.Letters.received(lines, today, Tokens.Garden.Letter.minLines.toInt())
    fun openLetter(id: String) { val v = lettersOpened + id; store.lettersOpened = v; lettersOpened = v }
    /** 시험용 (개발자 모드 · 캡처): 지난 석 달에 한 줄 몇 개를 넣어 이번 달 편지가 오게. 오늘이 편지 달이 아니면 다음 편지 달로 시각을 옮기지는 않는다. */
    fun addSampleLetter(today: LocalDate = nowDate()) {
        val day = io.github.graviton94.carpediem.core.Letters.due(today) ?: return
        val samples = listOf(
            Triple(85L, R.string.letter_sample1, Feeling.THANKS), Triple(60L, R.string.letter_sample2, Feeling.HOPE),
            Triple(41L, R.string.letter_sample3, Feeling.WORRY), Triple(23L, R.string.letter_sample4, Feeling.JOY), Triple(9L, R.string.letter_sample5, Feeling.CALM),
        )
        var next = lines
        samples.forEach { (ago, res, f) -> next = Lines.add(next, DayLine(day.minusDays(ago), context.getString(res), f)) }
        store.lines = next; lines = next
        val v = lettersOpened - io.github.graviton94.carpediem.core.Letters.of(next, day, 1)!!.id; store.lettersOpened = v; lettersOpened = v
    }

    /** 시험용 (캡처): 지난 30일에 여러 마음의 한 줄을 넣어 마음의 하늘을 채운다. */
    fun addSampleMoods(today: LocalDate = nowDate()) {
        val fs = listOf(Feeling.JOY, Feeling.CALM, null, Feeling.THANKS, Feeling.HOPE, Feeling.CALM, Feeling.WORRY, Feeling.JOY, Feeling.SAD, Feeling.CALM, Feeling.THANKS, Feeling.JOY, Feeling.DISAPPOINT, Feeling.HOPE, Feeling.CALM, Feeling.JOY)
        var next = lines
        fs.forEachIndexed { i, f -> next = Lines.add(next, DayLine(today.minusDays(1L + i * 29L / fs.size + (i % 3)), context.getString(R.string.recall_sample), f)) }
        store.lines = next; lines = next
    }
    /** 캡처용: 홈을 열면 이번 달 편지를 바로 펼친다. */
    var debugOpenLetter = false

    fun exportLines(): String = Lines.export(lines) { Labels.feeling(context, it) }
    /** 시험용 (개발자 모드): 1년 전 오늘 보낸 한 줄을 하나 넣어 ‘1년 뒤 오늘’을 확인한다. */
    fun addSampleYearAgo(today: LocalDate = (fixedNow ?: LocalDateTime.now()).toLocalDate()) {
        val next = Lines.add(lines, DayLine(today.minusYears(1), context.getString(R.string.recall_sample), Feeling.HOPE)); store.lines = next; lines = next
    }

    // ───── 숨 ─────
    var breaths by mutableStateOf(store.breaths)
        private set
    var breathKind by mutableStateOf(store.breathKind)
        private set
    var breathMinutes by mutableStateOf(store.breathMinutes)
        private set
    var sound by mutableStateOf(store.sound)
        private set
    fun chooseBreath(kind: BreathKind, minutes: Int, s: io.github.graviton94.carpediem.core.Sound) {
        store.breathKind = kind; store.breathMinutes = minutes; store.sound = s; breathKind = kind; breathMinutes = minutes; sound = s
    }
    fun changeSound(s: io.github.graviton94.carpediem.core.Sound) { store.sound = s; sound = s }
    /** 숨을 끝까지 쉰 날 (하루에 여러 번이어도 한 줄). */
    fun recordBreath(kind: BreathKind, today: LocalDate = (fixedNow ?: LocalDateTime.now()).toLocalDate()) {
        if (breaths.any { it.first == today && it.second == kind }) return
        val next = breaths + (today to kind); store.breaths = next; breaths = next
    }
    val firstBreath: LocalDate? get() = breaths.minOfOrNull { it.first }

    // ───── 가족의 정원 ─────
    var people by mutableStateOf(store.people)
        private set
    fun newSeed(): Long = kotlin.random.Random.nextLong(0, 1L shl 32)
    fun savePerson(p: Person) {
        val next = if (people.any { it.id == p.id }) people.map { if (it.id == p.id) p else it } else (people + p).take(Tokens.Garden.Family.max.toInt() - 1)
        store.people = next; people = next; Widgets.refresh(context)
    }
    fun removePerson(id: String) { val next = people.filterNot { it.id == id }; store.people = next; people = next; Widgets.refresh(context) }
    fun newPersonId(): String = (1..8).map { "abcdefghijkmnpqrstuvwxyz23456789".random() }.joinToString("")

    fun eraseAll() {
        store.eraseAll(); store.ensureQuoteSeed(); unit = store.unit; grid = store.grid; defaultUnit = unit; defaultGrid = grid; lines = emptyList(); streaks = emptyMap(); keepLines = true; question = null; answering = null; lettersOpened = emptySet(); toast = null; randomLine = null; people = emptyList(); breaths = emptyList(); breathKind = store.breathKind; breathMinutes = store.breathMinutes; sound = store.sound
        profile = null; quoteLanguage = store.quoteLanguage; quote = store.todaysQuote(); design = store.design; meetPending = false; previewAll = false; notify = false; devMode = false; io.github.graviton94.carpediem.notify.Daily.schedule(context, false); Widgets.refresh(context)
    }

    fun defaultProfile(): LifeProfile {
        val region = context.resources.configuration.locales[0].country.ifBlank { null }
        return LifeProfile(LocalDate.now().minusYears(30), store.table.defaultCountry(region), Sex.OTHER, null)
    }
}

/** 화면 문구 (strings.xml). 위젯과 앱이 함께 쓴다. */
object Labels {
    fun unit(c: Context, u: LifeUnit) = c.getString(when (u) { LifeUnit.DAYS -> R.string.unit_days; LifeUnit.WEEKS -> R.string.unit_weeks; LifeUnit.MONTHS -> R.string.unit_months; LifeUnit.YEARS -> R.string.unit_years })
    fun stone(c: Context, id: String): String = c.resources.getIdentifier("stone_$id", "string", c.packageName).let { if (it == 0) id else c.getString(it) }
    fun feeling(c: Context, f: Feeling): String = c.getString(c.resources.getIdentifier("feeling_${f.name.lowercase()}", "string", c.packageName))
    /** 보낸 뒤의 한마디: 마음마다 몇 가지 가운데 하나 (strings.json letgo.msg.<마음>.<번호>). 마음을 안 골랐으면 none. */
    fun letGoMessage(c: Context, f: Feeling?): String {
        val key = f?.name?.lowercase() ?: "none"
        val ids = generateSequence(1) { it + 1 }.map { c.resources.getIdentifier("letgo_msg_${key}_$it", "string", c.packageName) }.takeWhile { it != 0 }.toList()
        return if (ids.isEmpty()) c.getString(R.string.letgo_done) else c.getString(ids.random())
    }
    fun design(c: Context, d: Design) = c.getString(when (d) { Design.GLASS -> R.string.design_glass; Design.GARDEN -> R.string.design_garden })
    fun season(c: Context, s: Season) = c.getString(when (s) { Season.SPRING -> R.string.season_spring; Season.SUMMER -> R.string.season_summer; Season.AUTUMN -> R.string.season_autumn; Season.WINTER -> R.string.season_winter })
    fun sex(c: Context, s: Sex) = c.getString(when (s) { Sex.OTHER -> R.string.sex_other; Sex.MALE -> R.string.sex_male; Sex.FEMALE -> R.string.sex_female })
    fun period(c: Context, p: LifePeriod) = c.getString(when (p) { LifePeriod.DAY -> R.string.flow_today; LifePeriod.WEEK -> R.string.flow_week; LifePeriod.MONTH -> R.string.flow_month; LifePeriod.YEAR -> R.string.flow_year })
    fun gridShort(c: Context, g: GridScale) = c.getString(when (g) { GridScale.WEEKS -> R.string.unit_weeks; GridScale.MONTHS -> R.string.unit_months; GridScale.YEARS -> R.string.unit_years })
    fun grid(c: Context, g: GridScale) = c.getString(when (g) { GridScale.WEEKS -> R.string.calendar_per_weeks; GridScale.MONTHS -> R.string.calendar_per_months; GridScale.YEARS -> R.string.calendar_per_years })
    fun quoteLanguage(c: Context, q: QuoteLanguage) = c.getString(when (q) { QuoteLanguage.KOREAN -> R.string.words_korean; QuoteLanguage.ENGLISH -> R.string.words_english; QuoteLanguage.BOTH -> R.string.words_both })
    fun remaining(c: Context, p: PeriodProgress) = when (p.period) {
        LifePeriod.DAY -> if (p.hoursLeft >= 1) c.getString(R.string.left_hours, "${p.hoursLeft}") else c.getString(R.string.left_minutes, "${p.minutesLeft}")
        else -> if (p.remainingDaysAfterToday == 0) c.getString(R.string.lastDay) else c.getString(R.string.left_days, "${p.remainingDaysAfterToday}")
    }
    fun country(c: Context, country: CountryLife?, code: String): String = when {
        code == LifeExpectancyTable.WORLD -> c.getString(R.string.country_world)
        country != null -> country.displayName(c.resources.configuration.locales[0])
        else -> code
    }
    fun years(v: Double): String = String.format(Locale.getDefault(), "%.1f", v)
    fun number(n: Int): String = String.format(Locale.getDefault(), "%,d", n)
    fun percent(v: Double, digits: Int = 1): String = String.format(Locale.getDefault(), "%.${digits}f%%", v * 100)
}
