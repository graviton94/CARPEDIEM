package io.github.graviton94.carpediem.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.data.Design

/** 화면 폭에 따른 크기 등급. 큰 숫자 크기와 화면 좌우 여백만 이 등급을 따른다 (iOS 와 같은 기준). */
enum class DeviceClass {
    Compact, Regular, Large;

    val pageMargin: Dp
        get() = when (this) {
            Compact -> Tokens.Layout.pageMarginCompact
            Regular -> Tokens.Layout.pageMarginRegular
            Large -> Tokens.Layout.pageMarginLarge
        }

    companion object {
        fun of(widthDp: Dp): DeviceClass = when {
            widthDp < Tokens.DeviceWidth.compactBelow.dp -> Compact
            widthDp >= Tokens.DeviceWidth.largeFrom.dp -> Large
            else -> Regular
        }
    }
}

/** 라이트 · 다크에 따라 바뀌는 색. */
data class DynamicColor(val light: Color, val dark: Color) {
    fun resolve(dark: Boolean) = if (dark) this.dark else light
}

/** 글자 토큰. 크기 단위가 sp 라서 사용자의 글꼴 크기 설정에 따라 커진다. */
data class TypeToken(val size: TextUnit, val family: Family, val weight: FontWeight, val tracking: Float) {
    enum class Family { Serif, Text }

    /** 명조는 글에 한글이 있으면 Noto Serif KR, 아니면 Lora. (Compose 는 글자별 대체 서체를 지정할 수 없음) */
    fun style(text: String? = null, scale: Float = 1f): TextStyle {
        val family = when (this.family) {
            Family.Text -> FontFamily.Default
            Family.Serif -> if (text != null && text.any { it in '가'..'힣' || it in 'ㄱ'..'ㆎ' }) Fonts.notoSerifKr else Fonts.lora
        }
        return TextStyle(fontFamily = family, fontWeight = weight, fontSize = size * scale, letterSpacing = tracking.em)
    }

    fun serif() = copy(family = Family.Serif, weight = FontWeight.Medium)
}

object Fonts {
    val lora = FontFamily(Font(R.font.lora_medium, FontWeight.Medium), Font(R.font.lora_semibold, FontWeight.SemiBold), Font(R.font.lora_semibold, FontWeight.Bold))
    val notoSerifKr = FontFamily(Font(R.font.notoserifkr_medium, FontWeight.Medium), Font(R.font.notoserifkr_semibold, FontWeight.SemiBold), Font(R.font.notoserifkr_semibold, FontWeight.Bold))
}

/** 현재 모드로 풀어 둔 색. 화면 코드는 이 값만 쓴다. 정원 디자인은 종이 그림이라 늘 밝은 종이 · 잉크 색. */
data class Palette(val dark: Boolean, val garden: Boolean = false) {
    private fun c(d: DynamicColor) = d.resolve(dark)
    private val g = Tokens.Garden.Colors
    val base = if (garden) g.paper else c(Tokens.Palette.base)
    val foreground = if (garden) g.ink else c(Tokens.Palette.foreground)
    val secondary = if (garden) g.inkSoft else c(Tokens.Palette.secondary)
    val dim = if (garden) g.dim else c(Tokens.Palette.dim)
    val olive = c(Tokens.Palette.olive)
    val onOlive = c(Tokens.Palette.onOlive)
    val light = c(Tokens.Palette.light)
    val now = c(Tokens.Palette.now)
    val danger = c(Tokens.Palette.danger)
    val future = c(Tokens.Palette.future)
    val glass = c(Tokens.Palette.glass)
    val glassEdge = c(Tokens.Palette.glassEdge)
    val widgetTop = c(Tokens.Palette.widgetTop)
    val widgetBottom = c(Tokens.Palette.widgetBottom)
    val seasons = Tokens.Palette.seasons.map { c(it) }
}

val LocalPalette = staticCompositionLocalOf { Palette(false) }
val LocalDeviceClass = staticCompositionLocalOf { DeviceClass.Regular }
val LocalDesign = staticCompositionLocalOf { Design.GLASS }
/** 정원 단위 한 칸의 크기 (화면 폭 / Tokens.Garden.unitWidth). */
val LocalGardenUnit = staticCompositionLocalOf { 1.dp }

object Theme {
    val palette: Palette @Composable @ReadOnlyComposable get() = LocalPalette.current
    val deviceClass: DeviceClass @Composable @ReadOnlyComposable get() = LocalDeviceClass.current
    val design: Design @Composable @ReadOnlyComposable get() = LocalDesign.current
    val garden: Boolean @Composable @ReadOnlyComposable get() = LocalDesign.current == Design.GARDEN
    val unit: Dp @Composable @ReadOnlyComposable get() = LocalGardenUnit.current
}

@Composable
fun CarpeDiemTheme(dark: Boolean = isSystemInDarkTheme(), deviceClass: DeviceClass, design: Design = Design.GLASS, screenWidth: Dp = Tokens.Garden.unitWidth.dp, content: @Composable () -> Unit) {
    val garden = design == Design.GARDEN
    val p = Palette(dark && !garden, garden)
    val unit = screenWidth / Tokens.Garden.unitWidth
    val scheme = if (dark && !garden) darkColorScheme(primary = p.olive, onPrimary = p.onOlive, background = p.base, surface = p.base, onSurface = p.foreground, onBackground = p.foreground)
    else lightColorScheme(primary = p.olive, onPrimary = p.onOlive, background = p.base, surface = p.base, onSurface = p.foreground, onBackground = p.foreground)
    CompositionLocalProvider(LocalPalette provides p, LocalDeviceClass provides deviceClass, LocalDesign provides design, LocalGardenUnit provides unit) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
