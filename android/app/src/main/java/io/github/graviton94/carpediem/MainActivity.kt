package io.github.graviton94.carpediem

import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
        // 앱이 멈추면 그 자리를 폰 안에만 적어 둠 (다음에 열 때 보낼지 물음)
        io.github.graviton94.carpediem.data.Feedback.install(this)
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
    /** 하루 닫기 (03): 한 줄 → 고마움 → 등불. */
    data class CloseDay(val back: Screen) : Screen
    /** 한 해의 엔딩 크레딧 (08). */
    data class Credits(val year: Int, val back: Screen) : Screen
    data class Country(val back: Screen) : Screen
}

class MainActivity : ComponentActivity() {
    /** 앱이 떠 있을 때 알림 · 위젯을 누르면 (singleTop): 하던 일을 지우지 않고 그 자리로 옮겨 감. */
    private var newOpen by mutableStateOf<android.content.Intent?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent); newOpen = intent
    }

    companion object {
        const val EXTRA_MORNING_BREATH = "carpediem.morningBreath"
        /** 알림 · 바로 가기를 누르면 열 곳: letter · write · flow · month:2026-9 · year:2026 · stone:<id> · memory · breath · gaze · close */
        const val EXTRA_OPEN = "carpediem.open"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 오늘의 하늘 · 땅 그림을 화면 밖에서 먼저 읽기 시작
        runCatching { val p = io.github.graviton94.carpediem.data.Store(applicationContext).profile; io.github.graviton94.carpediem.ui.garden.GardenArt.warm(applicationContext, io.github.graviton94.carpediem.core.GardenDecor.realSeason(java.time.LocalDate.now(), p?.countryCode ?: "KR")) }
        val state = AppState(applicationContext)
        if (savedInstanceState == null) state.resumeSchedules()
        // 알림 · 위젯 · 바로 가기로 왔는지 (그러면 첫 화면 없이 바로 그곳으로)
        val linked = savedInstanceState == null && (intent?.hasExtra(EXTRA_OPEN) == true || intent?.getBooleanExtra(EXTRA_MORNING_BREATH, false) == true)
        val start = (if (BuildConfig.DEBUG) debugSetup(state) else Screen.Main).let { s ->
            // 아침 알림에서 왔으면 하루를 여는 숨 1분
            if (savedInstanceState == null && intent?.getBooleanExtra(EXTRA_MORNING_BREATH, false) == true && state.profile != null) { state.homePage = 0; state.goAsk = "morning"; Screen.Main } else s
        }.let { s ->
            // 알림 · 위젯에서 왔으면 그곳으로. 처음 켤 때 한 번만 (돌리거나 다시 그릴 때 또 가지 않게), 쓴 표는 지운다
            val open = if (savedInstanceState == null) intent?.getStringExtra(EXTRA_OPEN) else null
            intent?.removeExtra(EXTRA_OPEN); intent?.removeExtra(EXTRA_MORNING_BREATH)
            openFrom(open, state) ?: s
        }
        // 캡처 스크립트가 연 실행 (cd.* 표) 에서는 처음 알림 허락을 묻지 않음
        val scripted = BuildConfig.DEBUG && intent?.extras?.keySet()?.any { it.startsWith("cd.") } == true
        // 앱 첫 화면 (타이틀): 아이콘으로 켤 때마다 (알림 · 위젯 · 바로 가기로 온 때는 바로 그곳으로)
        val firstTitle = (start == Screen.Main && !linked && savedInstanceState == null && !scripted && state.titleDue()) ||
            (BuildConfig.DEBUG && intent?.getBooleanExtra("cd.title", false) == true)
        setContent {
            BoxWithConstraints {
                val screenW = maxWidth
                fun clock() = state.fixedNow ?: LocalDateTime.now()
                var now by remember { mutableStateOf(clock()) }
                // 분이 바뀔 때마다 (분 경계에 맞춰), 앱이 보일 때만. 돌아오면 바로 지금 시각으로
                val clockOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                LaunchedEffect(clockOwner) {
                    clockOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                        while (true) { now = clock(); delay(60_000L - System.currentTimeMillis() % 60_000L) }
                    }
                }
                // 정원은 시각을 따라: 밤 · 새벽은 어두운 한 벌, 낮 · 해 질 녘은 밝은 종이 (폰 테마와 상관없이)
                val night = SkyTime.isDark(now)
                CarpeDiemTheme(deviceClass = DeviceClass.of(screenW), design = state.design, screenWidth = screenW, night = night) {
                    var screen by remember { mutableStateOf(start) }
                    // 정원을 처음부터 다시 그리는 번호 (알림에서 다른 페이지 · 판으로 갈 때)
                    var homeEpoch by remember { mutableStateOf(0) }
                    var title by remember { mutableStateOf(firstTitle) }
                    androidx.compose.runtime.SideEffect { if (state.titleUp != title) state.titleUp = title; io.github.graviton94.carpediem.ui.garden.GardenPause.on = title }
                    LaunchedEffect(newOpen) {
                        val x = newOpen ?: return@LaunchedEffect
                        newOpen = null
                        val morning = x.getBooleanExtra(EXTRA_MORNING_BREATH, false) && state.profile != null
                        val open = x.getStringExtra(EXTRA_OPEN)
                        x.removeExtra(EXTRA_OPEN); x.removeExtra(EXTRA_MORNING_BREATH)
                        if (state.profile == null) return@LaunchedEffect
                        title = false
                        if (morning) { state.homePage = 0; state.goAsk = "morning" }
                        screen = if (morning) Screen.Main else openFrom(open, state) ?: Screen.Main
                        homeEpoch++
                    }
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { now = clock(); state.refreshQuote(); state.opened(); state.refreshQuestion(); state.recheckNotify(); state.syncFromStore() }
                    // 앱을 다시 열 때(화면에 다시 나올 때) 남은 시간 · 인생 달력 단위를 기본값으로
                    LifecycleEventEffect(Lifecycle.Event.ON_START) {
                        // 30분 넘게 떠났다 돌아오면 다시 첫 화면부터 (resetViewIfAway 보다 먼저 물어야 함)
                        if (!title && screen == Screen.Main && state.fixedNow == null && state.awayLong() && state.titleDue()) title = true
                        state.resetViewIfAway()
                    }
                    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { state.stopped() }
                    // 정원은 늘 밝은 종이라 상태바 · 내비게이션 바 글자를 어둡게 둔다
                    val sysDark = isSystemInDarkTheme()
                    LaunchedEffect(state.design, sysDark, night) {
                        val paper = Tokens.Garden.Colors.paper.toArgb()
                        if (state.design == Design.GARDEN && night) enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
                        else if (state.design == Design.GARDEN || !sysDark) enableEdgeToEdge(SystemBarStyle.light(android.graphics.Color.TRANSPARENT, paper), SystemBarStyle.light(android.graphics.Color.TRANSPARENT, paper))
                        else enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
                    }

                    // 정원에 처음 들어온 날 한 번 (둘러보기를 마친 뒤): 무엇을 받는지 먼저 말하고, ‘받을게요’면 알림 허락을 물음.
                    // 허락하면 아침 · 저녁 · 전날 알림을 켬 (캡처용 시각을 정한 실행에서는 묻지 않음)
                    val askNotify = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok -> state.notifyAsked(ok) }
                    var notifyNote by remember { mutableStateOf(false) }
                    // 둘러보기가 끝나고, 첫 한 줄을 남겼거나 하루가 지난 뒤에 (처음 온 날 묻는 것이 줄줄이 이어지지 않게)
                    val guided = (state.guideDone || state.design != Design.GARDEN) && !state.touring &&
                        (state.lines.isNotEmpty() || state.store.startDate.isBefore((state.fixedNow ?: LocalDateTime.now()).toLocalDate()))
                    LaunchedEffect(screen == Screen.Main, state.profile != null, state.meetPending, guided, title) {
                        if (screen == Screen.Main && state.profile != null && !state.meetPending && guided && !title && !scripted && state.fixedNow == null && !state.store.notifyAsked) {
                            if (android.os.Build.VERSION.SDK_INT >= 33 && !io.github.graviton94.carpediem.notify.Daily.allowed(this@MainActivity)) { delay(Tokens.Garden.Motion.pageMs.toLong()); notifyNote = true }
                            else state.notifyAsked(true)
                        }
                    }
                    // 지난번에 앱이 멈췄으면 한 번만 조용히: 알려 줄지 (메일에 그 자리와 기기 정보만, 기록은 담지 않음)
                    var crash by remember { mutableStateOf(if (scripted) null else io.github.graviton94.carpediem.data.Feedback.lastCrash(this@MainActivity)) }
                    crash?.let { c -> if (screen == Screen.Main && state.profile != null && !state.touring && !notifyNote && !title) io.github.graviton94.carpediem.ui.GardenAlert(
                        onDismissRequest = { crash = null; io.github.graviton94.carpediem.data.Feedback.clearCrash(this@MainActivity) },
                        title = { androidx.compose.material3.Text(getString(R.string.feedback_crashTitle)) },
                        text = { androidx.compose.material3.Text(getString(R.string.feedback_crashBody)) },
                        confirmButton = { androidx.compose.material3.TextButton(onClick = {
                            crash = null
                            // 메일 앱이 열렸을 때만 지움 (없으면 클립보드에 복사했다고 알림)
                            if (io.github.graviton94.carpediem.data.Feedback.send(this@MainActivity, c)) io.github.graviton94.carpediem.data.Feedback.clearCrash(this@MainActivity)
                            else state.say(getString(R.string.feedback_copied))
                        }) { androidx.compose.material3.Text(getString(R.string.feedback_crashSend)) } },
                        dismissButton = { androidx.compose.material3.TextButton(onClick = { crash = null; io.github.graviton94.carpediem.data.Feedback.clearCrash(this@MainActivity) }) { androidx.compose.material3.Text(getString(R.string.feedback_crashSkip), color = io.github.graviton94.carpediem.design.Theme.palette.secondary) } },
                    ) }
                    if (notifyNote) io.github.graviton94.carpediem.ui.GardenAlert(
                        onDismissRequest = { notifyNote = false; state.notifyAsked(false) },
                        title = { androidx.compose.material3.Text(getString(R.string.notify_askTitle)) },
                        text = { androidx.compose.material3.Text(getString(R.string.notify_askBody)) },
                        confirmButton = { androidx.compose.material3.TextButton(onClick = { notifyNote = false; askNotify.launch(android.Manifest.permission.POST_NOTIFICATIONS) }) { androidx.compose.material3.Text(getString(R.string.notify_askYes)) } },
                        dismissButton = { androidx.compose.material3.TextButton(onClick = { notifyNote = false; state.notifyAsked(false) }) { androidx.compose.material3.Text(getString(R.string.notify_askLater), color = io.github.graviton94.carpediem.design.Theme.palette.secondary) } },
                    )

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
                        is Screen.Credits -> state.profile?.let { io.github.graviton94.carpediem.ui.garden.CreditsScreen(state, it, s.year) { screen = s.back } } ?: run { screen = Screen.Main }
                        is Screen.CloseDay -> state.profile?.let { io.github.graviton94.carpediem.ui.garden.CloseDayScreen(state, it, now) { screen = s.back } } ?: run { screen = Screen.Main }
                        is Screen.AddPerson -> state.profile?.let {
                            AddPersonScreen(state, it, s.editId, onDone = { id -> screen = if (s.memory) Screen.Memory(Screen.Main) else if (s.editId != null && id != null) (s.back as? Screen.Stone)?.copy() ?: Screen.Main else if (s.back == Screen.Settings) Screen.Settings else Screen.Main },
                                onBack = { screen = s.back }, memory = s.memory, onAddMemory = { screen = Screen.AddPerson(null, s, memory = true) },
                                onMovedToMemory = { screen = Screen.Memory(Screen.Main) })
                        } ?: run { screen = Screen.Main }
                        Screen.Main -> {
                            val profile = state.profile
                            val garden = state.design == Design.GARDEN
                            when {
                                // 처음 온 사람: 소개 석 장 (정원 디자인) → 생일 · 나라 → 하루를 만남 → 정원 둘러보기
                                profile == null && garden && !state.introSeen -> io.github.graviton94.carpediem.ui.garden.IntroScreen(state) { state.finishIntro() }
                                profile == null -> OnboardingScreen(state) { screen = Screen.Country(Screen.Main) }
                                garden && state.meetPending -> MeetScreen(state) { state.finishMeet() }
                                garden -> androidx.compose.runtime.key(homeEpoch) { GardenHome(state, profile, now, onSettings = { screen = Screen.Settings },
                                    onCollection = { screen = Screen.Collection(Screen.Main) }, onSupport = { screen = Screen.Support(Screen.Main) },
                                    onStone = { id -> screen = Screen.Stone(id, Screen.Main) }, onAddPerson = { screen = Screen.AddPerson(null, Screen.Main) },
                                    onBreath = { k, m, snd -> screen = Screen.Breathe(k, m, snd, Screen.Main) }, onGaze = { screen = Screen.Gaze(Screen.Main) },
                                    onLook = { screen = Screen.Look(Screen.Main) }, onMemory = { screen = Screen.Memory(Screen.Main) }, onCloseDay = { screen = Screen.CloseDay(Screen.Main) }, onCredits = { y -> screen = Screen.Credits(y, Screen.Main) }) }
                                else -> HomeScreen(state, profile, now, onSettings = { screen = Screen.Settings }, onSupport = { screen = Screen.Support(Screen.Main) })
                            }
                        }
                    }
                    }
                    // 하루의 첫 화면 (정원 위에 덮어 두었다가, 누르면 하루 자리에서 동그랗게 열림)
                    if (title && screen == Screen.Main) io.github.graviton94.carpediem.ui.garden.TitleScreen(state, now) {
                        title = false; state.titleSeen(clock().toLocalDate())
                    }
                    // 짧은 알림 한마디: 어느 화면에서든 같은 자리 · 같은 움직임
                    io.github.graviton94.carpediem.ui.NoteHost(state.note.takeIf { !io.github.graviton94.carpediem.ui.garden.chanceOnScreen.value && !state.touring && !title }) { state.noteDone() }   // 우연한 순간이 끝날 때까지 기다림
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
    // 캡처 스크립트의 실행: 처음 온 사람의 안내는 건너뜀 (cd.guide true 면 안내를 처음부터 보여 줌)
    if (x.extras?.keySet()?.any { it.startsWith("cd.") } == true) state.debugGuides(x.getBooleanExtra("cd.guide", false))
    x.getStringExtra("cd.design")?.let { state.changeDesign(if (it == "glass") Design.GLASS else Design.GARDEN) }
    if (x.hasExtra("cd.seed")) state.store.overrideHaruSeed(x.getLongExtra("cd.seed", 0))
    x.getStringExtra("cd.birth")?.let { b ->
        val sex = when (x.getStringExtra("cd.sex")) { "male" -> Sex.MALE; "female" -> Sex.FEMALE; else -> Sex.OTHER }
        state.save(LifeProfile(LocalDate.parse(b), x.getStringExtra("cd.country") ?: "KR", sex, null))
    }
    if (x.hasExtra("cd.meet")) { if (x.getBooleanExtra("cd.meet", false)) state.begin(state.profile ?: state.defaultProfile()) else state.finishMeet() }
    if (x.hasExtra("cd.preview")) state.changePreviewAll(x.getBooleanExtra("cd.preview", false))
    x.getStringExtra("cd.now")?.let { state.fixedNow = LocalDateTime.parse(it) }
    // 시험용: 그날 절기 (S1) 를 정해 정원의 작은 변화를 봄 (예: sanggang · dongji)
    // 시험용: 가족 돌 둘에게 이번 계절의 조각을 놓아 둠 (R1)
    if (x.getBooleanExtra("cd.offer", false)) state.profile?.let { p ->
        val day = (state.fixedNow ?: LocalDateTime.now()).toLocalDate()
        val card = state.decor(p, io.github.graviton94.carpediem.core.LifeSnapshot(p.birthDate, p.expectancy(state.store.table), day.atTime(12, 0)), day).card
        state.people.filter { it.id == "mom00001" || it.id == "pet00001" }.forEach { state.offer(it.id, card, day) }
    }
    x.getStringExtra("cd.term")?.let { k -> state.termOverride = io.github.graviton94.carpediem.core.SolarTerm.entries.firstOrNull { it.key == k } }
    // 캡처용: 우연한 순간 하나를 바로 (bubbles · fireflies · rainbow · butterflies · snail · aurora · wind)
    x.getStringExtra("cd.chance")?.let { k -> io.github.graviton94.carpediem.core.Chance.of(k)?.let { state.showChance(it, (state.fixedNow ?: LocalDateTime.now()).toLocalDate()) } }
    if (x.getBooleanExtra("cd.recall", false)) { state.addSampleYearAgo(); state.addSampleRandom() }
    // 캡처용: 걱정한 밤 다음 아침 (06) · 아침 씨앗을 심은 날 (04, 저녁에 ‘싹이 텄나요?’)
    if (x.getBooleanExtra("cd.comfort", false)) state.addSampleWorryYesterday()
    if (x.getBooleanExtra("cd.morningSeed", false)) state.addSampleSeed()
    if (x.hasExtra("cd.touch")) state.changeBreathTouch(x.getBooleanExtra("cd.touch", false))
    // 캡처용: 오늘 열리는 항아리 (10) · 지난 한 해의 한 줄들 (07 나이테, 생일 아침에)
    if (x.getBooleanExtra("cd.capsule", false)) state.addSampleCapsule()
    if (x.getBooleanExtra("cd.ringYear", false)) state.addSampleRingYear()
    // 캡처용: 며칠 만에 돌아온 날 (09, 손님 · 인사)
    x.getStringExtra("cd.guest")?.let { state.pretendGuest(it) }
    if (x.getBooleanExtra("cd.photos", false)) state.addSamplePhotos()
    state.debugGardenYear = x.getBooleanExtra("cd.gardenYear", false)
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
    state.debugTyped = x.getBooleanExtra("cd.typed", false)
    state.homePage = if (state.debugOpenYear || state.debugOpenMonth) 1 else x.getIntExtra("cd.page", 0)
    state.refreshQuestion()
    if (x.getBooleanExtra("cd.question", false)) state.previewQuestion()
    // 가족의 정원 시험: 동생 · 콩이(강아지) · 엄마(오늘 생일) · 아빠
    if (x.getBooleanExtra("cd.family", false)) {
        val today = (state.fixedNow ?: LocalDateTime.now()).toLocalDate()
        state.people.forEach { state.removePerson(it.id) }
        // 시험용 이름도 그 나라 말로 (스토어 캡처에 한글 이름이 섞이지 않게)
        val n = when (io.github.graviton94.carpediem.data.Words.lang(this)) {
            "en" -> listOf("Sis", "Bean", "Mom", "Dad", "Grandma", "Bro", "Mittens", "Jamie")
            "ja" -> listOf("妹", "まめ", "ママ", "パパ", "おばあちゃん", "兄", "ミケ", "ゆう")
            "zh-TW" -> listOf("妹妹", "豆豆", "媽媽", "爸爸", "奶奶", "哥哥", "咪咪", "小宇")
            else -> listOf("동생", "콩이", "엄마", "아빠", "할머니", "형", "나비", "지우")
        }
        listOf(
            Person("sib00001", n[0], Kind.PERSON, birth = LocalDate.of(1999, 4, 2), sex = Sex.FEMALE, country = "KR", seed = 12345, metOn = today),
            Person("pet00001", n[1], Kind.PET, Species.DOG, LocalDate.of(2018, 5, 5), seed = 99, metOn = today),
            Person("mom00001", n[2], Kind.PERSON, birth = today.withYear(1964), sex = Sex.FEMALE, country = "KR", seed = 4254103021, metOn = today),
            Person("dad00001", n[3], Kind.PERSON, birth = LocalDate.of(1961, 8, 20), sex = Sex.MALE, country = "KR", seed = 31337, metOn = today),
        ).forEach { state.savePerson(it) }
        // cd.full: 정원을 가득 (나 + 8)
        if (x.getBooleanExtra("cd.full", false)) listOf(
            Person("gma00001", n[4], Kind.PERSON, birth = LocalDate.of(1938, 2, 11), sex = Sex.FEMALE, country = "KR", seed = 777, metOn = today),
            Person("bro00001", n[5], Kind.PERSON, birth = LocalDate.of(1996, 11, 3), sex = Sex.MALE, country = "KR", seed = 2024, metOn = today),
            Person("cat00001", n[6], Kind.PET, Species.CAT, LocalDate.of(2021, 3, 1), seed = 5150, metOn = today),
            Person("frd00001", n[7], Kind.PERSON, birth = LocalDate.of(2001, 7, 9), sex = Sex.OTHER, country = "KR", seed = 8080, metOn = today),
        ).forEach { state.savePerson(it) }
    }
    // 캡처용: 위젯 고르는 화면의 미리보기 그림 (실제 위젯을 그대로 그려 파일로)
    if (x.getBooleanExtra("cd.widgetShots", false)) lifecycleScope.launch {
        kotlinx.coroutines.delay(1500); io.github.graviton94.carpediem.widget.WidgetShots.save(this@debugSetup)
    }
    return when (x.getStringExtra("cd.screen")) { "settings" -> Screen.Settings; "widgets" -> Screen.WidgetPreview; "collection" -> Screen.Collection(Screen.Main); "support" -> Screen.Support(Screen.Main); "stone" -> Screen.Stone(x.getStringExtra("cd.stoneId") ?: state.people.firstOrNull()?.id, Screen.Main); "add" -> Screen.AddPerson(null, Screen.Main); "breath" -> Screen.Breathe(BreathKind.CALM, 1, Sound.WAVES, Screen.Main); "gaze" -> Screen.Gaze(Screen.Main); "look" -> Screen.Look(Screen.Main); "thanks" -> Screen.Breathe(BreathKind.THANKS, 1, Sound.SEASON, Screen.Main); "walk" -> Screen.Breathe(BreathKind.BOX, 1, Sound.NONE, Screen.Main); "lantern" -> Screen.Breathe(BreathKind.SLEEP, 1, Sound.NONE, Screen.Main); "ripple" -> Screen.Breathe(BreathKind.CALM, 1, Sound.NONE, Screen.Main); "memory" -> Screen.Memory(Screen.Main); "close" -> Screen.CloseDay(Screen.Main); "credits" -> Screen.Credits(x.getIntExtra("cd.creditsYear", (state.fixedNow ?: LocalDateTime.now()).year), Screen.Main); else -> Screen.Main }
}

/** 알림에서 왔을 때 열 곳. 홈 안의 페이지 · 판은 state 에 적어 두고 (홈이 처음 그릴 때 씀), 돌 페이지는 그 화면으로. */
private fun openFrom(open: String?, state: AppState): Screen? {
    if (open == null || state.profile == null) return null
    val (kind, arg) = open.split(':', limit = 2).let { it[0] to it.getOrNull(1) }
    when (kind) {
        "letter" -> { state.homePage = 0; state.debugOpenLetter = true }
        "write" -> { state.homePage = 1; state.focusWrite = !state.sentOn((state.fixedNow ?: java.time.LocalDateTime.now()).toLocalDate()) }
        "flow" -> state.homePage = 3
        "garden" -> state.homePage = 0
        // month = 알림 (지난 달의 정원이 피었다는 소식, 펼친 것으로 남김) · record = 위젯 (이번 달을 보기만)
        "month", "record" -> arg?.split('-')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 && it[1] in 1..12 && it[0] in 1900..java.time.LocalDate.now().year }?.let { (y, m) ->
            state.homePage = 1; state.pendingRecord = io.github.graviton94.carpediem.ui.garden.RecordView(y, m); if (kind == "month") state.openMonth(y, m)
        }
        "year" -> arg?.toIntOrNull()?.takeIf { it in 1900..java.time.LocalDate.now().year }?.let { y -> state.homePage = 1; state.pendingRecord = io.github.graviton94.carpediem.ui.garden.RecordView(y, null); state.openYear(y) }
        "memory" -> return Screen.Memory(Screen.Main)
        // 숨 바로가기 (빠른 설정 타일 · 바로 가기, C1) · 돌멍하기 (02): 바로 시작하지 않고 정원에서 “… 하러 갈까요?” 한 번
        "breath" -> { state.homePage = 0; state.goAsk = "breath" }
        "gaze" -> if (state.design == Design.GARDEN) { state.homePage = 0; state.goAsk = "gaze" }
        // 하루 닫기 (정원 디자인에서만)
        "close" -> return if (state.design == Design.GARDEN) Screen.CloseDay(Screen.Main) else { state.focusWrite = !state.sentOn((state.fixedNow ?: java.time.LocalDateTime.now()).toLocalDate()); null }
        "stone" -> return if (arg.isNullOrEmpty()) Screen.Stone(null, Screen.Main) else arg.takeIf { id -> state.people.any { it.id == id } }?.let { Screen.Stone(it, Screen.Main) }   // 비면 내 돌
    }
    return null
}
