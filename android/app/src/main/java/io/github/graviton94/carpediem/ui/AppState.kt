package io.github.graviton94.carpediem.ui

import io.github.graviton94.carpediem.core.BreathKind
import io.github.graviton94.carpediem.core.Decor
import io.github.graviton94.carpediem.core.Chance
import io.github.graviton94.carpediem.core.Chances
import io.github.graviton94.carpediem.core.GardenDecor
import io.github.graviton94.carpediem.core.Hang
import io.github.graviton94.carpediem.core.LifeSnapshot
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

/** 한 줄을 보낸 뒤 한마디 창 아래 권하는 작은 한 가지 (docs: 1.4 돌봄). */
enum class Care { CALM_BREATH, BOX_BREATH, LOOK, SEND_TO, SLEEP_BREATH, MORNING_BREATH }

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

    init {
        store.ensureQuoteSeed(); quote = store.todaysQuote()
        // 알림 시각이 바뀌어도 켜 둔 알림은 새 시각으로 (예전에 맞춘 시각에 머물지 않게)
        if (store.notify) io.github.graviton94.carpediem.notify.Daily.schedule(context, true)
        if (store.eveningNotify) io.github.graviton94.carpediem.notify.Evening.schedule(context, true)
        if (store.tomorrowNotify) io.github.graviton94.carpediem.notify.Tomorrow.schedule(context, true)
    }
    // question 은 fixedNow 를 정한 뒤 (MainActivity) · 날이 바뀔 때 refreshQuestion 으로 채운다

    fun save(p: LifeProfile) { store.profile = p; profile = p; Widgets.refresh(context) }
    /** 온보딩을 마칠 때. 정원 디자인이면 하루를 만나는 화면을 먼저 보여 준다. */
    fun begin(p: LifeProfile) { save(p); if (design == Design.GARDEN) { store.meetPending = true; meetPending = true } }
    fun finishMeet() { store.meetPending = false; meetPending = false }
    fun changeDesign(v: Design) { store.design = v; design = v; Widgets.refresh(context) }
    fun changePreviewAll(v: Boolean) { store.previewAll = v; previewAll = v }
    fun opened() { store.markOpened(); checkRandomRecall() }
    var eveningNotify by mutableStateOf(store.eveningNotify)
        private set
    var tomorrowNotify by mutableStateOf(store.tomorrowNotify)
        private set
    var morningBreath by mutableStateOf(store.morningBreath)
        private set
    fun changeEvening(on: Boolean) {
        store.eveningNotify = on; eveningNotify = on
        io.github.graviton94.carpediem.notify.Evening.schedule(context, on)
    }
    fun changeTomorrow(on: Boolean) {
        store.tomorrowNotify = on; tomorrowNotify = on
        io.github.graviton94.carpediem.notify.Tomorrow.schedule(context, on)
    }
    fun changeMorningBreath(v: Boolean) { store.morningBreath = v; morningBreath = v }
    fun changeNotify(v: Boolean) { store.notify = v; notify = v; io.github.graviton94.carpediem.notify.Daily.schedule(context, v) }
    fun unlockDev() { store.devMode = true; devMode = true }
    fun nextQuote() { store.skipQuote(); quote = store.todaysQuote(); blinkKick++; Widgets.refresh(context) }
    fun refreshQuote() { quote = store.todaysQuote() }
    /** 타자기처럼 한 글자씩 다 쳐 본 문장 (같은 문장은 정원에 다시 와도 한 번에). */
    var typedQuote: String? = null
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
    fun sentOn(day: LocalDate): Boolean = lines.any { it.date == day }
    var keepLines by mutableStateOf(store.keepLines)
        private set
    /** 이어 쓰기 흔적 (7 · 30 · 100일 → 얻은 날). */
    var streaks by mutableStateOf(store.streaks)
        private set
    /** 보낸 뒤 잠깐 떠오르는 한마디 (마음에 맞춰). 보이고 나면 null. */
    var toast by mutableStateOf<String?>(null)

    /** 보낸 뒤 창의 제목 (기쁨 · 고마움 · 희망을 누군가에게 보냈을 때만 “엄마에게 보냈어요”). */
    var toastTitle by mutableStateOf<String?>(null)

    /** 화면 아래 잠깐 떠오르는 짧은 알림 (모든 화면이 같은 움직임으로). 글과 때 (같은 글을 다시 띄울 때도 새로). */
    var note by mutableStateOf<Pair<String, Long>?>(null)
    fun say(text: String) { note = text to System.nanoTime() }

    fun letGo(text: String, feeling: Feeling?, to: String? = null, today: LocalDate = (fixedNow ?: LocalDateTime.now()).toLocalDate()) {
        val t = Lines.clean(text, Tokens.Garden.LetGo.maxChars.toInt()); if (t.isEmpty()) return
        // 질문에 답한 한 줄이면 질문 번호도 함께
        val q = answering?.id; answering = null
        // 기록 남기지 않기: 날짜만 (이어 쓰기 흔적은 이어 간다)
        val line = if (keepLines) DayLine(today, t, feeling, to, q) else DayLine(today, "", null, to)
        val person = people.firstOrNull { it.id == to }
        toastTitle = if (person != null && feeling in setOf(Feeling.JOY, Feeling.THANKS, Feeling.HOPE)) context.getString(R.string.letgo_modalTo, person.name) else null
        val moment = Chances.onLine(lines, today)
        val next = Lines.add(lines, line)
        store.lines = next; lines = next
        showChance(moment, today)   // 정원에 돌아가면 계절 바람 (어제 무거웠으면 무지개)
        val s = Lines.streaks(next, streaks); if (s != streaks) { store.streaks = s; streaks = s }
        val part = Labels.part(fixedNow ?: LocalDateTime.now())
        toast = Labels.letGoMessage(context, feeling, part)
        care = careFor(feeling, line.to, today, part)
        Widgets.refresh(context)   // 마음의 기록 위젯에 오늘의 꽃 · 별
    }
    /** 돌봄 권하기 (켜 두었을 때). 하루 한 줄이라 하루 한 번까지. 오늘 이미 숨 쉬었으면 숨은 권하지 않음. */
    var careOn by mutableStateOf(store.care)
        private set
    fun changeCare(v: Boolean) { store.care = v; careOn = v }
    var care by mutableStateOf<Care?>(null)
    private fun careFor(f: Feeling?, to: String?, today: LocalDate, part: io.github.graviton94.carpediem.core.DayPart): Care? {
        val night = part == io.github.graviton94.carpediem.core.DayPart.NIGHT; val morning = part == io.github.graviton94.carpediem.core.DayPart.MORNING
        if (!careOn || design != Design.GARDEN) return null
        val breathed = breaths.any { it.first == today }
        return when (f) {
            // 밤에는 잠드는 명상, 아침의 슬픔엔 맑은 숨으로
            Feeling.SAD -> if (breathed) null else if (night) Care.SLEEP_BREATH else if (morning) Care.MORNING_BREATH else Care.CALM_BREATH
            Feeling.WORRY -> if (breathed) null else if (night) Care.SLEEP_BREATH else Care.BOX_BREATH
            Feeling.DISAPPOINT -> Care.LOOK
            Feeling.JOY, Feeling.THANKS -> if (people.isNotEmpty() && to == null && keepLines) Care.SEND_TO else null
            else -> null
        }
    }
    /** ‘이 마음, 누구에게’: 오늘 보낸 한 줄을 그 돌에게 (곁에 깃털). */
    fun sendTodayTo(id: String, today: LocalDate = (fixedNow ?: LocalDateTime.now()).toLocalDate()) {
        val next = lines.map { if (it.date == today) it.copy(to = id) else it }; store.lines = next; lines = next
    }
    fun changeKeepLines(v: Boolean) {
        store.keepLines = v; keepLines = v
        // 끄는 순간 지금까지의 글도 지운다 (날짜는 남겨 흔적을 잇는다)
        if (!v) {
            val dates = lines.map { DayLine(it.date, "", null) }; store.lines = dates; lines = dates
            val m = memoryLines.map { it.copy(text = "") }; store.memoryLines = m; memoryLines = m
        }
    }
    fun clearLines() { store.clearLines(); lines = emptyList(); randomLine = null; memoryLines = emptyList() }
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
    fun refreshQuestion() {
        if (!previewQ) question = store.todaysQuestion(nowDate())
        // 날이 바뀌었으면 어제 질문에 답하던 것은 놓아 둔다
        if (answering != null && answering?.id != question?.id) answering = null
    }
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
        // 한 줄이 모자라도 지난 계절의 바람이 있으면 그 바람만 담긴 편지가 온다
        val l = io.github.graviton94.carpediem.core.Letters.of(lines, day, Tokens.Garden.Letter.minLines.toInt())
            ?: io.github.graviton94.carpediem.core.Letter(day, io.github.graviton94.carpediem.core.Memories.seasonOf(day), day.minusMonths(3), day.minusDays(1), emptyList(), emptyList())
                .takeIf { wishFor(it) != null }
        return l?.takeIf { it.id !in lettersOpened }
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
        val id = io.github.graviton94.carpediem.core.Letters.of(next, day, 1)?.id ?: return
        val v = lettersOpened - id; store.lettersOpened = v; lettersOpened = v
    }

    /** 시험용 (캡처): 지난 30일에 여러 마음을 넣어 마음의 하늘을 채운다 (글 없이 마음만이라 편지에는 들어가지 않음). */
    fun addSampleMoods(today: LocalDate = nowDate()) {
        val fs = listOf(Feeling.JOY, Feeling.CALM, null, Feeling.THANKS, Feeling.HOPE, Feeling.CALM, Feeling.WORRY, Feeling.JOY, Feeling.SAD, Feeling.CALM, Feeling.THANKS, Feeling.JOY, Feeling.DISAPPOINT, Feeling.HOPE, Feeling.CALM, Feeling.JOY)
        var next = lines
        fs.forEachIndexed { i, f -> next = Lines.add(next, DayLine(today.minusDays(1L + i * 29L / fs.size + (i % 3)), "", f)) }
        store.lines = next; lines = next
    }
    /** 캡처용: 오늘 한 줄을 이미 보낸 것으로 (마음의 날씨 · 하루의 표정 확인). */
    fun addSampleToday(f: Feeling, today: LocalDate = nowDate()) {
        val next = Lines.add(lines, DayLine(today, context.getString(R.string.recall_sample), f)); store.lines = next; lines = next
    }
    /** 캡처용: 홈을 열면 이번 달 편지를 바로 펼친다. */
    var debugOpenLetter = false
    var debugOpenYear = false
    var debugOpenMonth = false
    /** 홈에서 보던 페이지 (0 정원 · 1 기록 · 2 추억 · 3 흐름). 돌 페이지 등에서 돌아오면 그 페이지로. 캡처용으로 처음 페이지를 정하기도. */
    var homePage = 0
    /** 알림에서 열 마음의 기록 판 (홈이 처음 그릴 때 한 번 씀). */
    var pendingRecord: io.github.graviton94.carpediem.ui.garden.RecordView? = null
    /** 시험용 (개발자 모드 · 캡처): 오늘이 12월 31일 ~ 1월 7일이면 그 해에 여러 마음을 흩어 놓아 한 해의 정원이 피게. */
    fun addSampleYear(today: LocalDate = nowDate()) {
        val y = Lines.yearDue(today) ?: return
        val fs = Feeling.entries + listOf<Feeling?>(null, Feeling.CALM, Feeling.JOY, Feeling.THANKS)
        var next = lines; val r = kotlin.random.Random(y)
        for (k in 0 until 220) { val d = LocalDate.of(y, 1, 1).plusDays(r.nextLong(0, 365)); next = Lines.add(next, DayLine(d, if (r.nextInt(5) == 0) context.getString(R.string.letter_sample1) else "", fs[r.nextInt(fs.size)])) }
        store.lines = next; lines = next; val v = yearsOpened - "$y"; store.yearsOpened = v; yearsOpened = v
    }
    /** 시험용 (캡처): 지난 달과 이번 달에 여러 마음을 흩어 놓아 마음의 기록 · 지난 달의 정원이 보이게. */
    fun addSampleMonths(today: LocalDate = nowDate()) {
        val fs = Feeling.entries + listOf<Feeling?>(null, Feeling.CALM, Feeling.JOY)
        var next = lines; val r = kotlin.random.Random(today.monthValue)
        val from = today.withDayOfMonth(1).minusMonths(1)
        var d = from
        while (!d.isAfter(today)) { if (r.nextInt(5) < 3) next = Lines.add(next, DayLine(d, if (r.nextInt(4) == 0) context.getString(R.string.letter_sample1) else "", fs[r.nextInt(fs.size)])); d = d.plusDays(1) }
        store.lines = next; lines = next; val v = monthsOpened - "${from.year}-${from.monthValue}"; store.monthsOpened = v; monthsOpened = v
    }

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
        // 밤에 숨을 끝까지 쉬었으면 정원에 반딧불 (겨울 밤엔 스무 번에 한 번 오로라)
        val night = io.github.graviton94.carpediem.ui.garden.SkyTime.isDark(fixedNow ?: LocalDateTime.now())
        val winter = profile?.let { GardenDecor.realSeason(today, it.countryCode) } == Season.WINTER
        Chances.onBreath(night, winter, next.map { it.first }.distinct().size)?.let { showChance(it, today) }
    }
    val firstBreath: LocalDate? get() = breaths.minOfOrNull { it.first }

    // ───── 우연한 순간 (core Chances) ─────
    /** 정원에 다음에 보일 순간 (보이고 나면 null). 한 번에 하나. */
    var chance by mutableStateOf<Chance?>(null)
        private set
    var chancesMet by mutableStateOf(store.chancesMet)
        private set
    fun showChance(c: Chance, today: LocalDate) {
        chance = c
        if (chancesMet.none { it.startsWith(c.key + ":") }) { val n = chancesMet + "${c.key}:$today"; store.chancesMet = n; chancesMet = n }
        if (c == Chance.SNAIL) store.snailAt = System.currentTimeMillis()
    }
    fun chanceDone() { chance = null }
    /** 정원을 열 때: 오랜만에 돌아온 날 달팽이, 그림을 보낸 날 비눗방울, 숨을 세 번 쉰 봄 · 여름 주에 나비 한 쌍 (한 번씩). */
    fun openChance(today: LocalDate, season: Season) {
        if (chance != null || previewAll) return
        Chances.onOpen(today, season, store.sharedOn, store.returned, breaths.map { it.first }, store.chancesShown)?.let { (c, key) -> store.chancesShown = store.chancesShown + key; showChance(c, today) }
    }

    // ───── 정원 꾸밈 (자리 여섯, core GardenDecor) ─────
    var gazeDays by mutableStateOf(store.gazeDays)
        private set
    /** 돌멍하기를 연 날 (하루에 한 번만 셈). */
    fun recordGaze(today: LocalDate = (fixedNow ?: LocalDateTime.now()).toLocalDate()) {
        val k = today.toEpochDay().toString(); if (k in gazeDays) return
        val next = gazeDays + k; store.gazeDays = next; gazeDays = next
    }
    var seasonCards by mutableStateOf(store.seasonCards)
        private set
    /** 지금 정원의 꾸밈. 시험용 미리 보기(previewAll)면 모두 다 자라고 걸린 모습. */
    fun decor(profile: LifeProfile, s: LifeSnapshot, today: LocalDate): Decor {
        val D = Tokens.Garden.Decor
        val rules = GardenDecor.Rules(listOf(D.stageDays1.toInt(), D.stageDays2.toInt(), D.stageDays3.toInt()), listOf(D.chimeBreaths.toInt(), D.bellBreaths.toInt(), D.lanternBreaths.toInt()),
            D.kiteLines.toInt(), D.ribbonLines.toInt(), D.ribbonMax.toInt(), D.budGazes.toInt(), D.budMax.toInt())
        val d = GardenDecor.of(today, profile.countryCode, s.season, profile.birthDate, s.expectancy, store.startDate, breaths.map { it.first }.distinct().size, lines, gazeDays.size, letterDue(today) != null, rules)
        return if (!previewAll) d else d.copy(stage = 3, hang = Hang.LANTERN, kite = true, ribbons = Feeling.entries.toList() + null, buds = rules.budMax)
    }
    /** 정원을 열 때: 이번 계절의 한 장을 받고, 지난번에 본 것보다 새로 생긴 것이 있으면 한 줄로 알림 (처음엔 조용히 기억만). */
    fun noticeDecor(d: Decor) {
        if (previewAll) return
        if (d.card.id !in seasonCards) { val next = seasonCards + d.card.id; store.seasonCards = next; seasonCards = next }
        val now = listOf(d.stage, d.tree.ordinal, d.hang.ordinal, if (d.kite) 1 else 0, d.ribbons.size, d.buds).joinToString(",") + "," + d.card.id + "," + (if (d.letter) 1 else 0)
        val before = store.decorSeen; store.decorSeen = now
        if (before == null || before == now) return
        val b = before.split(","); fun n(i: Int) = b.getOrNull(i)?.toIntOrNull() ?: 0
        val msg = when {
            d.tree.ordinal != n(1) -> R.string.decor_new_tree
            d.stage > n(0) -> R.string.decor_new_stage
            d.hang.ordinal > n(2) -> when (d.hang) { Hang.LANTERN -> R.string.decor_new_lantern; Hang.BELL -> R.string.decor_new_bell; else -> R.string.decor_new_chime }
            d.kite && n(3) == 0 -> R.string.decor_new_kite
            d.ribbons.size > n(4) -> R.string.decor_new_ribbon
            d.buds > n(5) -> R.string.decor_new_bud
            d.letter && n(7) == 0 -> R.string.decor_new_letter
            d.card.id != b.getOrNull(6) -> R.string.decor_new_card
            else -> null
        }
        msg?.let { say(context.getString(it)) }
    }

    // ───── 가족의 정원 ─────
    var people by mutableStateOf(store.people)
        private set
    fun newSeed(): Long = kotlin.random.Random.nextLong(0, 1L shl 32)
    fun savePerson(p: Person) {
        val next = if (people.any { it.id == p.id }) people.map { if (it.id == p.id) p else it } else (people + p).take(Tokens.Garden.Family.max.toInt() - 1)
        store.people = next; people = next; Widgets.refresh(context)
    }
    fun removePerson(id: String) { val next = people.filterNot { it.id == id }; store.people = next; people = next; Widgets.refresh(context) }
    // ───── 특별한 날 꽃 ─────
    var specialDays by mutableStateOf(store.specialDays)
        private set
    fun putSpecialDay(d: io.github.graviton94.carpediem.core.SpecialDay) { val v = io.github.graviton94.carpediem.core.SpecialDays.put(specialDays, d); store.specialDays = v; specialDays = v }
    fun removeSpecialDay(date: LocalDate) { val v = specialDays.filterNot { it.date == date }; store.specialDays = v; specialDays = v }

    // ───── 계절 첫날의 바람 · 한 해의 정원 ─────
    var wishes by mutableStateOf(store.wishes)
        private set
    var wishSkipped by mutableStateOf(store.wishSkipped)
        private set
    var yearsOpened by mutableStateOf(store.yearsOpened)
        private set
    /** 이번 계절 첫 두 주에 묻는 바람 (이미 답했거나 ‘다음에’를 눌렀으면 null). 기록 남기기를 끄면 묻지 않음. */
    fun wishDue(today: LocalDate = nowDate()): String? =
        Lines.wishDue(today)?.takeIf { keepLines && it !in wishes && it !in wishSkipped }
    fun saveWish(id: String, text: String) { val t = Lines.clean(text, Tokens.Garden.LetGo.maxChars.toInt()); if (t.isEmpty()) return; val v = wishes + (id to t); store.wishes = v; wishes = v }
    fun skipWish(id: String) { val v = wishSkipped + id; store.wishSkipped = v; wishSkipped = v }
    /** 이 편지가 돌려줄 지난 계절의 바람 (석 달 전 계절 id). */
    fun wishFor(letter: io.github.graviton94.carpediem.core.Letter): String? = wishes["%04d-%02d".format(letter.arrives.minusMonths(3).year, letter.arrives.minusMonths(3).monthValue)]
    /** 한 해의 정원이 핀 해 (12월 31일 ~ 1월 7일, 한 줄이 하나라도 있고 아직 펼치지 않았을 때). */
    fun yearDue(today: LocalDate = nowDate()): Int? =
        Lines.yearDue(today)?.takeIf { y -> keepLines && "$y" !in yearsOpened && lines.any { it.date.year == y } }
    fun openYear(y: Int) { val v = yearsOpened + "$y"; store.yearsOpened = v; yearsOpened = v }
    /** 지난 정원에 놓일 해들 (한 줄이 있는 해, 올해부터 거꾸로). */
    fun gardenYears(): List<Int> = if (!keepLines) emptyList() else lines.map { it.date.year }.distinct().sortedDescending()
    var monthsOpened by mutableStateOf(store.monthsOpened)
        private set
    /** 지난 달의 정원이 핀 때 (달이 바뀐 뒤 사흘, 그 달에 한 줄이 하나라도 있고 아직 펼치지 않았을 때). (연, 월). */
    fun monthDue(today: LocalDate = nowDate()): Pair<Int, Int>? =
        io.github.graviton94.carpediem.core.Constellations.monthDue(today, Tokens.Garden.Year.monthDue.toInt())
            ?.takeIf { (y, m) -> keepLines && "$y-$m" !in monthsOpened && lines.any { it.date.year == y && it.date.monthValue == m } }
    fun openMonth(y: Int, m: Int) { val v = monthsOpened + "$y-$m"; store.monthsOpened = v; monthsOpened = v }

    // ───── 기억의 돌 ─────
    var memories by mutableStateOf(store.memories)
        private set
    var memoryLines by mutableStateOf(store.memoryLines)
        private set
    private fun putMemories(v: List<Person>) { store.memories = v; memories = v }
    fun saveMemory(p: Person) = putMemories(if (memories.any { it.id == p.id }) memories.map { if (it.id == p.id) p else it } else (memories + p).take(io.github.graviton94.carpediem.core.Memories.MAX))
    fun setStar(id: String, on: Boolean) = putMemories(memories.map { if (it.id == id) it.copy(star = on) else it })
    /** 가족의 돌을 기억의 자리로 (넷이 차 있으면 false). */
    fun toMemory(id: String, until: LocalDate? = null): Boolean {
        val p = people.firstOrNull { it.id == id } ?: return false
        if (memories.size >= io.github.graviton94.carpediem.core.Memories.MAX) return false
        saveMemory(p.copy(star = true, until = until)); removePerson(id); return true
    }
    /** 기억의 자리에서 다시 정원으로 (정원이 가득이면 false). */
    fun backToGarden(id: String): Boolean {
        val p = memories.firstOrNull { it.id == id } ?: return false
        if (people.size >= Tokens.Garden.Family.max.toInt() - 1) return false
        savePerson(p.copy(until = null)); putMemories(memories.filterNot { it.id == id }); return true
    }
    fun removeMemory(id: String) {
        putMemories(memories.filterNot { it.id == id })
        val l = memoryLines.filterNot { it.to == id }; store.memoryLines = l; memoryLines = l
    }
    /** 기억의 돌에게 한 줄 (그 돌마다 하루 한 번). 기록 남기기를 끄면 날짜만. */
    fun sendToMemory(id: String, text: String, today: LocalDate = (fixedNow ?: LocalDateTime.now()).toLocalDate()): Boolean {
        val t = Lines.clean(text, Tokens.Garden.LetGo.maxChars.toInt()); if (t.isEmpty()) return false
        if (memoryLines.any { it.to == id && it.date == today }) return false
        val l = (memoryLines + DayLine(today, if (keepLines) t else "", null, id)).sortedBy { it.date }; store.memoryLines = l; memoryLines = l
        return true
    }
    /** 캡처용: 기억의 돌 하나 (반려견, 2011년 봄 ~ 2024년 겨울). */
    fun addSampleMemory() {
        if (memories.isNotEmpty()) return
        saveMemory(Person("mem00001", context.getString(R.string.memory_sampleName), io.github.graviton94.carpediem.core.Kind.PET, io.github.graviton94.carpediem.core.Species.DOG,
            LocalDate.of(2011, 4, 2), seed = 6060, metOn = LocalDate.of(2026, 9, 1), until = LocalDate.of(2024, 12, 20)))
    }

    fun newPersonId(): String = (1..8).map { "abcdefghijkmnpqrstuvwxyz23456789".random() }.joinToString("")

    fun eraseAll() {
        previewQ = false
        store.eraseAll(); store.ensureQuoteSeed(); unit = store.unit; grid = store.grid; defaultUnit = unit; defaultGrid = grid; lines = emptyList(); streaks = emptyMap(); keepLines = true; care = null; careOn = true; question = null; answering = null; lettersOpened = emptySet(); toast = null; randomLine = null; people = emptyList(); memories = emptyList(); memoryLines = emptyList(); wishes = emptyMap(); wishSkipped = emptySet(); specialDays = emptyList(); yearsOpened = emptySet(); breaths = emptyList(); gazeDays = emptySet(); seasonCards = emptySet(); chancesMet = emptySet(); chance = null; breathKind = store.breathKind; breathMinutes = store.breathMinutes; sound = store.sound
        profile = null; quoteLanguage = store.quoteLanguage; quote = store.todaysQuote(); design = store.design; meetPending = false; previewAll = false; notify = false; devMode = false; io.github.graviton94.carpediem.notify.Daily.schedule(context, false); io.github.graviton94.carpediem.notify.Evening.schedule(context, false); eveningNotify = false; io.github.graviton94.carpediem.notify.Tomorrow.schedule(context, false); tomorrowNotify = false; morningBreath = true; Widgets.refresh(context)
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
    /** 때의 말: base_때_번호 가운데 하나 (없으면 null). 낮은 따로 두지 않고 원래 말을 쓴다. */
    fun timed(c: Context, base: String, part: io.github.graviton94.carpediem.core.DayPart): String? {
        if (part == io.github.graviton94.carpediem.core.DayPart.DAY) return null
        val p = part.name.lowercase()
        val single = c.resources.getIdentifier("${base}_$p", "string", c.packageName)
        val ids = generateSequence(1) { it + 1 }.map { c.resources.getIdentifier("${base}_${p}_$it", "string", c.packageName) }.takeWhile { it != 0 }.toList()
        return when { ids.isNotEmpty() -> c.getString(ids.random()); single != 0 -> c.getString(single); else -> null }
    }
    fun part(now: java.time.LocalDateTime) = io.github.graviton94.carpediem.core.DayPart.of(now.hour)
    fun letGoMessage(c: Context, f: Feeling?, part: io.github.graviton94.carpediem.core.DayPart = io.github.graviton94.carpediem.core.DayPart.DAY): String {
        if (f == null) timed(c, "letgo_msg_none", part)?.let { return it }
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
