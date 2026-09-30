// 자동 생성 파일 — 직접 고치지 말고 scripts/generate.py 를 실행하세요.
package io.github.graviton94.carpediem.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Tokens {
    object Space {
        val sp1 = 4.dp
        val sp2 = 8.dp
        val sp3 = 12.dp
        val sp4 = 16.dp
        val sp5 = 20.dp
        val sp6 = 24.dp
        val sp8 = 32.dp
        val sp10 = 40.dp
    }

    object Layout {
        val pageMarginCompact = Space.sp4
        val pageMarginRegular = Space.sp4
        val pageMarginLarge = Space.sp5
        val cardPadding = Space.sp5
        val widgetPadding = Space.sp4
        val tapTarget = 48.dp
    }

    object TypeScale {
        val largeTitle = TypeToken(34.sp, TypeToken.Family.Serif, FontWeight.SemiBold, 0f)
        val title2 = TypeToken(22.sp, TypeToken.Family.Text, FontWeight.SemiBold, 0f)
        val title3 = TypeToken(20.sp, TypeToken.Family.Serif, FontWeight.SemiBold, 0f)
        val headline = TypeToken(17.sp, TypeToken.Family.Text, FontWeight.SemiBold, 0f)
        val body = TypeToken(17.sp, TypeToken.Family.Text, FontWeight.Normal, 0f)
        val callout = TypeToken(16.sp, TypeToken.Family.Text, FontWeight.Normal, 0f)
        val subhead = TypeToken(15.sp, TypeToken.Family.Text, FontWeight.Medium, 0f)
        val footnote = TypeToken(13.sp, TypeToken.Family.Text, FontWeight.Normal, 0f)
        val caption1 = TypeToken(12.sp, TypeToken.Family.Text, FontWeight.Normal, 0f)
        val caption2 = TypeToken(11.sp, TypeToken.Family.Text, FontWeight.Bold, 0f)
        fun display(c: DeviceClass) = TypeToken(when (c) { DeviceClass.Compact -> 54; DeviceClass.Large -> 70; else -> 64 }.sp, TypeToken.Family.Serif, FontWeight.SemiBold, -0.02f)
    }

    object Palette {
        val base = DynamicColor(Color(0xFFE7E6DB), Color(0xFF11130D))
        val foreground = DynamicColor(Color(0xFF23251C), Color(0xFFEEEBDD))
        val secondary = DynamicColor(Color(0xFF5E604B), Color(0xFFA8A690))
        val dim = DynamicColor(Color(0x1F23251C), Color(0x21EEEBDD))
        val olive = DynamicColor(Color(0xFF5F7236), Color(0xFFA4B86A))
        val onOlive = DynamicColor(Color(0xFFFFFFFF), Color(0xFF11130D))
        val light = DynamicColor(Color(0xFFF2B35A), Color(0xFFF5B45C))
        val now = DynamicColor(Color(0xFFE89A32), Color(0xFFF5B45C))
        val danger = DynamicColor(Color(0xFFC9372A), Color(0xFFFF6B5E))
        val future = DynamicColor(Color(0x1F23251C), Color(0x1FEEEBDD))
        val glass = DynamicColor(Color(0x6BFFFFFF), Color(0x14FFFFFF))
        val glassEdge = DynamicColor(Color(0xB8FFFFFF), Color(0x24FFFFFF))
        val widgetTop = DynamicColor(Color(0xFFF4F3EC), Color(0xFF1E2117))
        val widgetBottom = DynamicColor(Color(0xFFE3E5D2), Color(0xFF15170F))
        val seasons = listOf(DynamicColor(Color(0xFFA9B67A), Color(0xFFC3D18E)), DynamicColor(Color(0xFF5F7236), Color(0xFF9DB060)), DynamicColor(Color(0xFFB5651D), Color(0xFFDB8B4E)), DynamicColor(Color(0xFF8C8A74), Color(0xFFA8A690)))
    }

    object Radius {
        val sm = 10.dp
        val md = 16.dp
        val lg = 28.dp
        val pill = 999.dp
    }

    object Stroke {
        val hair = 0.5.dp
        val line = 1.dp
        val barThin = 4.dp
        val bar = 6.dp
        val barThick = 10.dp
        val icon = 22.dp
    }

    object Effect {
        const val glowStrength = 0.45f
        const val glassOpacity = 0.5f
    }

    object Grid {
        const val weeksColumns = 52
        const val monthsColumns = 36
        const val yearsColumns = 10
        const val dotRatio = 0.68f
        const val nowRatio = 1.0f
        const val widgetMediumColumns = 14
        const val widgetMediumTextRatio = 0.34f
        const val widgetLargeColumns = 36
    }
}
