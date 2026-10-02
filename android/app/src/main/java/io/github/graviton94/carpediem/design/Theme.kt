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

    /**
     * 명조: 글에 한글이 있으면 Noto Serif KR, 가나 · 한자가 있으면 폰의 말에 따라 Noto Serif JP (일본어) · TC (번체 중국어) · KR (그 밖), 아니면 Lora.
     * (Compose 는 글자별 대체 서체를 지정할 수 없음)
     */
    fun style(text: String? = null, scale: Float = 1f): TextStyle {
        val family = when (this.family) {
            Family.Text -> FontFamily.Default
            Family.Serif -> when {
                text == null || text.none { isHangul(it) || isCjk(it) } -> Fonts.lora
                // 앱에 넣은 명조에 없는 글자가 하나라도 있으면 문장 전체를 기본 글꼴로 (글자마다 글꼴이 섞이지 않게)
                text.any(::isHangul) -> if (text.all { !isHangul(it) || it in Fonts.serifKrChars }) Fonts.notoSerifKr else FontFamily.Default
                Fonts.lang == "ja" -> if (text.all { !isCjk(it) || it in Fonts.serifJpChars }) Fonts.notoSerifJp else FontFamily.Default
                Fonts.lang == "zh-TW" -> if (text.all { !isCjk(it) || it in Fonts.serifTcChars }) Fonts.notoSerifTc else FontFamily.Default
                else -> FontFamily.Default
            }
        }
        return TextStyle(fontFamily = family, fontWeight = weight, fontSize = size * scale, letterSpacing = tracking.em)
    }

    fun serif() = copy(family = Family.Serif, weight = FontWeight.Medium)
}

private fun isHangul(c: Char) = c in '가'..'힣' || c in 'ㄱ'..'ㆎ'
/** 가나 · 한자 · 전각 문장부호 (U+3000 ~ U+9FFF, 전각 영숫자). */
private fun isCjk(c: Char) = c in '\u3000'..'\u9FFF' || c in '\uFF00'..'\uFFEF'

object Fonts {
    val serifKrChars: Set<Char> by lazy { SERIF_KR_CHARS.toHashSet() }
    val lora = FontFamily(Font(R.font.lora_medium, FontWeight.Medium), Font(R.font.lora_semibold, FontWeight.SemiBold), Font(R.font.lora_semibold, FontWeight.Bold))
    val notoSerifKr = FontFamily(Font(R.font.notoserifkr_medium, FontWeight.Medium), Font(R.font.notoserifkr_semibold, FontWeight.SemiBold), Font(R.font.notoserifkr_semibold, FontWeight.Bold))
    // 일본어 · 번체 중국어 명조는 한 굵기 (앱 크기를 아끼려고)
    val serifJpChars: Set<Char> by lazy { SERIF_JP_CHARS.toHashSet() }
    val serifTcChars: Set<Char> by lazy { SERIF_TC_CHARS.toHashSet() }
    val notoSerifJp = FontFamily(Font(R.font.notoserifjp_medium, FontWeight.Medium), Font(R.font.notoserifjp_medium, FontWeight.SemiBold), Font(R.font.notoserifjp_medium, FontWeight.Bold))
    val notoSerifTc = FontFamily(Font(R.font.notoseriftc_medium, FontWeight.Medium), Font(R.font.notoseriftc_medium, FontWeight.SemiBold), Font(R.font.notoseriftc_medium, FontWeight.Bold))
    /** 앱의 말 (폰 언어를 따라, 바뀌면 앱이 다시 시작됨). */
    val lang: String get() = java.util.Locale.getDefault().let { io.github.graviton94.carpediem.core.Langs.of(it.language, it.country, it.script) }
}

/**
 * 정원 디자인의 색 한 벌: 낮 · 해 질 녘은 밝은 종이, 밤 · 새벽은 폰 테마와 상관없이 어두운 남색 (Tokens.Garden.Night).
 * 정원 화면의 바탕 · 상자 · 선 · 칩은 이 값만 쓴다. 하루(돌) 그림의 먹선은 늘 같은 먹색.
 */
class GardenColors(val night: Boolean, val base: Color, val paper: Color, val ink: Color, val inkSoft: Color, val dim: Color,
                   val chip: Color, val button: Color, val future: Color, val scrim: Color) {
    companion object {
        private val d = Tokens.Garden.Colors
        val Day = GardenColors(false, d.paper, d.paper, d.ink, d.inkSoft, d.dim, d.chip, d.button, d.future, d.scrim)
        val Night = Tokens.Garden.Night.Colors.let { n -> GardenColors(true, n.base, n.paper, n.ink, n.inkSoft, n.dim, n.chip, n.button, n.future, n.scrim) }
        fun of(night: Boolean) = if (night) Night else Day
    }
}

/** 현재 모드로 풀어 둔 색. 화면 코드는 이 값만 쓴다. 정원 디자인은 시각에 따라 밝은 종이 · 어두운 남색. */
data class Palette(val dark: Boolean, val garden: Boolean = false, val night: Boolean = false) {
    private fun c(d: DynamicColor) = d.resolve(dark)
    private val g = GardenColors.of(night)
    val base = if (garden) g.base else c(Tokens.Palette.base)
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
val LocalGardenColors = staticCompositionLocalOf { GardenColors.Day }

object Theme {
    val palette: Palette @Composable @ReadOnlyComposable get() = LocalPalette.current
    val deviceClass: DeviceClass @Composable @ReadOnlyComposable get() = LocalDeviceClass.current
    val design: Design @Composable @ReadOnlyComposable get() = LocalDesign.current
    val garden: Boolean @Composable @ReadOnlyComposable get() = LocalDesign.current == Design.GARDEN
    val unit: Dp @Composable @ReadOnlyComposable get() = LocalGardenUnit.current
    /** 정원의 색 (밤이면 어두운 한 벌). */
    val gc: GardenColors @Composable @ReadOnlyComposable get() = LocalGardenColors.current
}

@Composable
fun CarpeDiemTheme(dark: Boolean = isSystemInDarkTheme(), deviceClass: DeviceClass, design: Design = Design.GLASS, screenWidth: Dp = Tokens.Garden.unitWidth.dp,
                   night: Boolean = false, content: @Composable () -> Unit) {
    val garden = design == Design.GARDEN
    val gardenNight = garden && night
    val p = Palette(dark && !garden, garden, gardenNight)
    val gc = GardenColors.of(gardenNight)
    val unit = screenWidth / Tokens.Garden.unitWidth
    val scheme = if ((dark && !garden) || gardenNight) darkColorScheme(primary = p.olive, onPrimary = p.onOlive, background = p.base, surface = if (garden) gc.paper else p.base,
        surfaceContainerHigh = if (garden) gc.paper else p.base, onSurface = p.foreground, onBackground = p.foreground)
    else lightColorScheme(primary = p.olive, onPrimary = p.onOlive, background = p.base, surface = p.base, onSurface = p.foreground, onBackground = p.foreground)
    CompositionLocalProvider(LocalPalette provides p, LocalDeviceClass provides deviceClass, LocalDesign provides design, LocalGardenUnit provides unit, LocalGardenColors provides gc) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
