package io.github.graviton94.carpediem

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.toArgb
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.Sex
import io.github.graviton94.carpediem.core.Species
import io.github.graviton94.carpediem.core.Kind
import io.github.graviton94.carpediem.core.Person
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.garden.WidgetPreviewScreen
import io.github.graviton94.carpediem.ui.garden.CollectionScreen
import io.github.graviton94.carpediem.ui.garden.SkyTime
import io.github.graviton94.carpediem.core.Sound
import io.github.graviton94.carpediem.core.BreathKind
import io.github.graviton94.carpediem.ui.garden.GazeScreen
import io.github.graviton94.carpediem.ui.garden.LookScreen
import io.github.graviton94.carpediem.ui.garden.MemoryScreen
import io.github.graviton94.carpediem.ui.garden.BreathScreen
import androidx.compose.animation.togetherWith
import io.github.graviton94.carpediem.ui.garden.AddPersonScreen
import io.github.graviton94.carpediem.ui.garden.StoneScreen
import io.github.graviton94.carpediem.ui.garden.SupportScreen
import java.time.LocalDate
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.graviton94.carpediem.design.CarpeDiemTheme
import io.github.graviton94.carpediem.design.DeviceClass
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.CountryScreen
import io.github.graviton94.carpediem.ui.HomeScreen
import io.github.graviton94.carpediem.ui.OnboardingScreen
import io.github.graviton94.carpediem.ui.SettingsScreen
import io.github.graviton94.carpediem.data.Design
import io.github.graviton94.carpediem.ui.garden.GardenHome
import io.github.graviton94.carpediem.ui.garden.MeetScreen
import io.github.graviton94.carpediem.widget.Widgets
import kotlinx.coroutines.delay
import java.time.LocalDateTime

class CarpeDiemApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Widgets.scheduleHourly(this)
    }
}

private sealed interface Screen {
    data object Main : Screen
    data object Settings : Screen
    data object WidgetPreview : Screen
    data class Collection(val back: Screen) : Screen
    data class Support(val back: Screen) : Screen
    /** 돌의 페이지 (id = null 이면 내 하루). */
    data class Stone(val id: String?, val back: Screen) : Screen
    /** 가족의 돌 더하기 (editId 가 있으면 고치기). */
    data class AddPerson(val editId: String?, val back: Screen, val memory: Boolean = false) : Screen
    data class Memory(val back: Screen) : Screen
    /** 하루와 숨 쉬기. */
    data class Breathe(val kind: BreathKind, val minutes: Int, val sound: Sound, val back: Screen) : Screen
    /** 멍하니 보는 정원. */
    data class Gaze(val back: Screen) : Screen
    data class Look(val back: Screen) : Screen
    data class Country(val back: Screen) : Screen
}

class MainActivity : ComponentActivity() {
    companion object { const val EXTRA_MORNING_BREATH = "carpediem.morningBreath" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val state = AppState(applicationContext)
        val start = (if (BuildConfig.DEBUG) debugSetup(state) else Screen.Main).let { s ->
            // 아침 알림에서 왔으면 하루를 여는 숨 1분
            if (intent?.getBooleanExtra(EXTRA_MORNING_BREATH, false) == true && state.profile != null) Screen.Breathe(BreathKind.CALM, 1, state.sound, Screen.Main) else s
        }
        setContent {
            BoxWithConstraints {
                val screenW = maxWidth
                fun clock() = state.fixedNow ?: LocalDateTime.now()
                var now by remember { mutableStateOf(clock()) }
                LaunchedEffect(Unit) { while (true) { delay(60_000); now = clock() } }
                // 정원은 시각을 따라: 밤 · 새벽은 어두운 한 벌, 낮 · 해 질 녘은 밝은 종이 (폰 테마와 상관없이)
                val night = SkyTime.isDark(now)
                CarpeDiemTheme(deviceClass = DeviceClass.of(screenW), design = state.design, screenWidth = screenW, night = night) {
                    var screen by remember { mutableStateOf(start) }
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { now = clock(); state.refreshQuote(); state.opened(); state.refreshQuestion() }
                    // 앱을 다시 열 때(화면에 다시 나올 때) 남은 시간 · 인생 달력 단위를 기본값으로
                    LifecycleEventEffect(Lifecycle.Event.ON_START) { state.resetViewToDefaults() }
                    // 정원은 늘 밝은 종이라 상태바 · 내비게이션 바 글자를 어둡게 둔다
                    val sysDark = isSystemInDarkTheme()
                    LaunchedEffect(state.design, sysDark, night) {
                        val paper = Tokens.Garden.Colors.paper.toArgb()
                        if (state.design == Design.GARDEN && night) enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
                        else if (state.design == Design.GARDEN || !sysDark) enableEdgeToEdge(SystemBarStyle.light(android.graphics.Color.TRANSPARENT, paper), SystemBarStyle.light(android.graphics.Color.TRANSPARENT, paper))
                        else enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
                    }

                    // 돌의 페이지로는 돌이 다가오듯 부드럽게 (옅어지며 조금 커짐)
                    androidx.compose.animation.AnimatedContent(
                        targetState = screen, label = "screen",
                        transitionSpec = {
                            val ms = Tokens.Garden.Touch.openMs.toInt()
                            if (targetState is Screen.Stone || initialState is Screen.Stone)
                                (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(ms)) + androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(ms), initialScale = 0.94f)) togetherWith
                                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(ms / 2))
                            else androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                        },
                    ) { target ->
                    when (val s = target) {
                        Screen.WidgetPreview -> WidgetPreviewScreen(state, now)
                        is Screen.Country -> {
                            val d = state.draft
                            if (d == null) screen = s.back
                            else CountryScreen(state, d.countryCode, d.sex, { code -> state.draft = d.copy(countryCode = code) }) { screen = s.back }
                        }
                        Screen.Settings -> state.profile?.let {
                            SettingsScreen(state, it, { screen = Screen.Main }, { screen = Screen.Country(Screen.Settings) },
                                onCollection = { screen = Screen.Collection(Screen.Settings) }, onSupport = { screen = Screen.Support(Screen.Settings) },
                                onStone = { id -> screen = Screen.Stone(id, Screen.Settings) }, onAddPerson = { screen = Screen.AddPerson(null, Screen.Settings) })
                        } ?: run { screen = Screen.Main }
                        is Screen.Collection -> state.profile?.let { CollectionScreen(state, it, now, onMemory = { screen = Screen.Memory(s) }) { screen = s.back } } ?: run { screen = Screen.Main }
                        is Screen.Memory -> MemoryScreen(state, now, onAdd = { screen = Screen.AddPerson(null, s, memory = true) }) { screen = s.back }
                        // 응원하기는 하루의 정원 그림이라 유리 버전에서도 정원 모습으로 연다
                        is Screen.Support -> CarpeDiemTheme(deviceClass = DeviceClass.of(screenW), design = Design.GARDEN, screenWidth = screenW, night = night) { SupportScreen(state, now) { screen = s.back } }
                        is Screen.Stone -> state.profile?.let { StoneScreen(state, it, now, s.id, { screen = s.back }, { id -> screen = Screen.AddPerson(id, s) }, onBreath = { k, m, snd -> screen = Screen.Breathe(k, m, snd, s) }) } ?: run { screen = Screen.Main }
                        is Screen.Breathe -> state.profile?.let { BreathScreen(state, it, now, s.kind, s.minutes, s.sound) { screen = s.back } } ?: run { screen = Screen.Main }
                        is Screen.Gaze -> state.profile?.let { GazeScreen(state, it, now) { screen = s.back } } ?: run { screen = Screen.Main }
                        is Screen.Look -> LookScreen { screen = s.back }
                        is Screen.AddPerson -> state.profile?.let {
                            AddPersonScreen(state, it, s.editId, onDone = { id -> screen = if (s.memory) Screen.Memory(Screen.Collection(Screen.Main)) else if (s.editId != null && id != null) (s.back as? Screen.Stone)?.copy() ?: Screen.Main else Screen.Main },
                                onBack = { screen = s.back }, memory = s.memory, onAddMemory = { screen = Screen.AddPerson(null, s, memory = true) },
                                onMovedToMemory = { screen = Screen.Memory(Screen.Collection(Screen.Main)) })
                        } ?: run { screen = Screen.Main }
                        Screen.Main -> {
                            val profile = state.profile
                            val garden = state.design == Design.GARDEN
                            when {
                                profile == null -> OnboardingScreen(state) { screen = Screen.Country(Screen.Main) }
                                garden && state.meetPending -> MeetScreen(state) { state.finishMeet() }
                                garden -> GardenHome(state, profile, now, onSettings = { screen = Screen.Settings },
                                    onCollection = { screen = Screen.Collection(Screen.Main) }, onSupport = { screen = Screen.Support(Screen.Main) },
                                    onStone = { id -> screen = Screen.Stone(id, Screen.Main) }, onAddPerson = { screen = Screen.AddPerson(null, Screen.Main) },
                                    onBreath = { k, m, snd -> screen = Screen.Breathe(k, m, snd, Screen.Main) }, onGaze = { screen = Screen.Gaze(Screen.Main) },
                                    onLook = { screen = Screen.Look(Screen.Main) })
                                else -> HomeScreen(state, profile, now, onSettings = { screen = Screen.Settings }, onSupport = { screen = Screen.Support(Screen.Main) })
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

/**
 * 디버그 빌드 전용: 화면 캡처(CI)에서 상태를 정해 여는 실행 옵션. 출시(release) 빌드에서는 읽지 않는다.
 *   adb shell am start -n io.github.graviton94.carpediem/.MainActivity --ez cd.reset true --es cd.birth 2000-05-12 --es cd.design garden
 *   --el cd.seed 2718281 --es cd.now 2026-09-30T15:00 --ez cd.preview true --ez cd.meet true --es cd.screen settings|widgets
 */
private fun MainActivity.debugSetup(state: AppState): Screen {
    val x = intent ?: return Screen.Main
    if (x.getBooleanExtra("cd.reset", false)) state.eraseAll()
    x.getStringExtra("cd.design")?.let { state.changeDesign(if (it == "glass") Design.GLASS else Design.GARDEN) }
    if (x.hasExtra("cd.seed")) state.store.overrideHaruSeed(x.getLongExtra("cd.seed", 0))
    x.getStringExtra("cd.birth")?.let { b ->
        val sex = when (x.getStringExtra("cd.sex")) { "male" -> Sex.MALE; "female" -> Sex.FEMALE; else -> Sex.OTHER }
        state.save(LifeProfile(LocalDate.parse(b), x.getStringExtra("cd.country") ?: "KR", sex, null))
    }
    if (x.hasExtra("cd.meet")) { if (x.getBooleanExtra("cd.meet", false)) state.begin(state.profile ?: state.defaultProfile()) else state.finishMeet() }
    if (x.hasExtra("cd.preview")) state.changePreviewAll(x.getBooleanExtra("cd.preview", false))
    x.getStringExtra("cd.now")?.let { state.fixedNow = LocalDateTime.parse(it) }
    if (x.getBooleanExtra("cd.recall", false)) { state.addSampleYearAgo(); state.addSampleRandom() }
    if (x.getBooleanExtra("cd.letter", false)) state.addSampleLetter()
    if (x.getBooleanExtra("cd.moods", false)) state.addSampleMoods()
    if (x.getBooleanExtra("cd.memory", false)) state.addSampleMemory()
    x.getStringExtra("cd.today")?.let { f -> runCatching { io.github.graviton94.carpediem.core.Feeling.valueOf(f) }.getOrNull()?.let { state.addSampleToday(it) } }
    // 캡처용: 한 줄을 보낸 뒤 한마디 창 + 돌봄 권하기 (예: cd.care CALM_BREATH)
    x.getStringExtra("cd.care")?.let { c -> state.toast = io.github.graviton94.carpediem.ui.Labels.letGoMessage(this, io.github.graviton94.carpediem.core.Feeling.SAD); state.care = runCatching { io.github.graviton94.carpediem.ui.Care.valueOf(c) }.getOrNull() }
    state.debugOpenLetter = x.getBooleanExtra("cd.openLetter", false)
    if (x.getBooleanExtra("cd.year", false)) state.addSampleYear()
    if (x.getBooleanExtra("cd.months", false)) state.addSampleMonths()
    // 캡처용: 특별한 날 꽃 둘 (태어난 지 18년 · 22년쯤)
    if (x.getBooleanExtra("cd.special", false)) state.profile?.birthDate?.let { b ->
        state.putSpecialDay(io.github.graviton94.carpediem.core.SpecialDay(b.plusYears(18).plusDays(40), "입학"))
        state.putSpecialDay(io.github.graviton94.carpediem.core.SpecialDay(b.plusYears(22).plusDays(100), "첫 출근"))
    }
    // 캡처용: 이번 편지가 돌려줄 지난 계절의 바람
    if (x.getBooleanExtra("cd.wish", false)) io.github.graviton94.carpediem.core.Letters.due((state.fixedNow ?: LocalDateTime.now()).toLocalDate())?.minusMonths(3)?.let { d -> state.saveWish("%04d-%02d".format(d.year, d.monthValue), getString(R.string.wish_sample)) }
    state.debugOpenYear = x.getBooleanExtra("cd.openYear", false)
    state.debugOpenMonth = x.getBooleanExtra("cd.openMonth", false)
    state.refreshQuestion()
    if (x.getBooleanExtra("cd.question", false)) state.previewQuestion()
    // 가족의 정원 시험: 동생 · 콩이(강아지) · 엄마(오늘 생일) · 아빠
    if (x.getBooleanExtra("cd.family", false)) {
        val today = (state.fixedNow ?: LocalDateTime.now()).toLocalDate()
        state.people.forEach { state.removePerson(it.id) }
        listOf(
            Person("sib00001", "동생", Kind.PERSON, birth = LocalDate.of(1999, 4, 2), sex = Sex.FEMALE, country = "KR", seed = 12345, metOn = today),
            Person("pet00001", "콩이", Kind.PET, Species.DOG, LocalDate.of(2018, 5, 5), seed = 99, metOn = today),
            Person("mom00001", "엄마", Kind.PERSON, birth = today.withYear(1964), sex = Sex.FEMALE, country = "KR", seed = 4254103021, metOn = today),
            Person("dad00001", "아빠", Kind.PERSON, birth = LocalDate.of(1961, 8, 20), sex = Sex.MALE, country = "KR", seed = 31337, metOn = today),
        ).forEach { state.savePerson(it) }
        // cd.full: 정원을 가득 (나 + 8)
        if (x.getBooleanExtra("cd.full", false)) listOf(
            Person("gma00001", "할머니", Kind.PERSON, birth = LocalDate.of(1938, 2, 11), sex = Sex.FEMALE, country = "KR", seed = 777, metOn = today),
            Person("bro00001", "형", Kind.PERSON, birth = LocalDate.of(1996, 11, 3), sex = Sex.MALE, country = "KR", seed = 2024, metOn = today),
            Person("cat00001", "나비", Kind.PET, Species.CAT, LocalDate.of(2021, 3, 1), seed = 5150, metOn = today),
            Person("frd00001", "지우", Kind.PERSON, birth = LocalDate.of(2001, 7, 9), sex = Sex.OTHER, country = "KR", seed = 8080, metOn = today),
        ).forEach { state.savePerson(it) }
    }
    return when (x.getStringExtra("cd.screen")) { "settings" -> Screen.Settings; "widgets" -> Screen.WidgetPreview; "collection" -> Screen.Collection(Screen.Main); "support" -> Screen.Support(Screen.Main); "stone" -> Screen.Stone(x.getStringExtra("cd.stoneId") ?: state.people.firstOrNull()?.id, Screen.Main); "add" -> Screen.AddPerson(null, Screen.Main); "breath" -> Screen.Breathe(BreathKind.CALM, 1, Sound.WAVES, Screen.Main); "gaze" -> Screen.Gaze(Screen.Main); "look" -> Screen.Look(Screen.Main); "thanks" -> Screen.Breathe(BreathKind.THANKS, 1, Sound.SEASON, Screen.Main); "memory" -> Screen.Memory(Screen.Collection(Screen.Main)); else -> Screen.Main }
}
