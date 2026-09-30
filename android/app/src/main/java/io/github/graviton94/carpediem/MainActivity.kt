package io.github.graviton94.carpediem

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    data class Country(val back: Screen) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val state = AppState(applicationContext)
        setContent {
            BoxWithConstraints {
                CarpeDiemTheme(deviceClass = DeviceClass.of(maxWidth)) {
                    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
                    var now by remember { mutableStateOf(LocalDateTime.now()) }
                    LaunchedEffect(Unit) { while (true) { delay(60_000); now = LocalDateTime.now() } }
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { now = LocalDateTime.now(); state.refreshQuote() }

                    when (val s = screen) {
                        is Screen.Country -> {
                            val d = state.draft
                            if (d == null) screen = s.back
                            else CountryScreen(state, d.countryCode, d.sex, { code -> state.draft = d.copy(countryCode = code) }) { screen = s.back }
                        }
                        Screen.Settings -> state.profile?.let { SettingsScreen(state, it, { screen = Screen.Main }) { screen = Screen.Country(Screen.Settings) } } ?: run { screen = Screen.Main }
                        Screen.Main -> {
                            val profile = state.profile
                            if (profile == null) OnboardingScreen(state) { screen = Screen.Country(Screen.Main) }
                            else HomeScreen(state, profile, now) { screen = Screen.Settings }
                        }
                    }
                }
            }
        }
    }
}
