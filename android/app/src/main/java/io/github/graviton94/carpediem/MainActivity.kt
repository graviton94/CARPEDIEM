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
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.garden.WidgetPreviewScreen
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
    data class Country(val back: Screen) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val state = AppState(applicationContext)
        val start = if (BuildConfig.DEBUG) debugSetup(state) else Screen.Main
        setContent {
            BoxWithConstraints {
                CarpeDiemTheme(deviceClass = DeviceClass.of(maxWidth), design = state.design, screenWidth = maxWidth) {
                    var screen by remember { mutableStateOf(start) }
                    fun clock() = state.fixedNow ?: LocalDateTime.now()
                    var now by remember { mutableStateOf(clock()) }
                    LaunchedEffect(Unit) { while (true) { delay(60_000); now = clock() } }
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { now = clock(); state.refreshQuote(); state.opened() }
                    // 정원은 늘 밝은 종이라 상태바 · 내비게이션 바 글자를 어둡게 둔다
                    val sysDark = isSystemInDarkTheme()
                    LaunchedEffect(state.design, sysDark) {
                        val paper = Tokens.Garden.Colors.paper.toArgb()
                        if (state.design == Design.GARDEN || !sysDark) enableEdgeToEdge(SystemBarStyle.light(android.graphics.Color.TRANSPARENT, paper), SystemBarStyle.light(android.graphics.Color.TRANSPARENT, paper))
                        else enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
                    }

                    when (val s = screen) {
                        Screen.WidgetPreview -> WidgetPreviewScreen(state, now)
                        is Screen.Country -> {
                            val d = state.draft
                            if (d == null) screen = s.back
                            else CountryScreen(state, d.countryCode, d.sex, { code -> state.draft = d.copy(countryCode = code) }) { screen = s.back }
                        }
                        Screen.Settings -> state.profile?.let { SettingsScreen(state, it, { screen = Screen.Main }) { screen = Screen.Country(Screen.Settings) } } ?: run { screen = Screen.Main }
                        Screen.Main -> {
                            val profile = state.profile
                            val garden = state.design == Design.GARDEN
                            when {
                                profile == null -> OnboardingScreen(state) { screen = Screen.Country(Screen.Main) }
                                garden && state.meetPending -> MeetScreen(state) { state.finishMeet() }
                                garden -> GardenHome(state, profile, now) { screen = Screen.Settings }
                                else -> HomeScreen(state, profile, now) { screen = Screen.Settings }
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
    return when (x.getStringExtra("cd.screen")) { "settings" -> Screen.Settings; "widgets" -> Screen.WidgetPreview; else -> Screen.Main }
}
