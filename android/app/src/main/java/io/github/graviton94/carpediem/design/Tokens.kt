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
        object Sky {
            const val glowDark = 0.5f
            const val glowLight = 1.2f
            const val radius = 0.6f
            const val glowX = 0.22f
            const val glowY = -0.05f
            const val oliveAlpha = 0.18f
            const val oliveX = 0.3f
            const val oliveY = 1.05f
        }
        object NowHalo {
            const val alpha = 0.55f
            const val radius = 1.6f
            const val widgetAlpha = 0.5f
            const val widgetBlur = 0.6f
            const val widgetRadius = 0.8f
        }
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

    object Enso {
        const val radius = 0.38f
        const val ringAlpha = 0.45f
        const val ring = 0.05f
        const val arc = 0.13f
        const val arcStart = -50.0f
        const val arcSweep = 288.0f
        const val dotAngle = -86.0f
        const val dot = 0.07f
    }

    object Expectancy {
        const val min = 30.0f
        const val max = 120.0f
        const val step = 0.5f
    }

    object Widget {
        const val numberScale = 1.05f
        const val quoteHeight = 64.0f
        const val gridBottom = 16.0f
        const val largeFromHeight = 220.0f
        const val markSize = 12.0f
    }

    object DeviceWidth {
        const val compactBelow = 390.0f
        const val largeFrom = 430.0f
    }

    object Notify {
        const val hour = 8.0f
        const val minute = 50.0f
    }

    object Garden {
        const val unitWidth = 390.0f
        object Colors {
            val paper = Color(0xFFF6F1E6)
            val ink = Color(0xFF33281F)
            val inkSoft = Color(0xFF6B6456)
            val dim = Color(0x2433281F)
            val button = Color(0xFFB8C98E)
            val chip = Color(0xFFDCE5C4)
            val pupil = Color(0xFF1E1A17)
            val shine = Color(0xFFFFFFFF)
            val scrim = Color(0x47282014)
            val now = Color(0xFFE9A43A)
            val future = Color(0x8C8FAA6A)
            val bars = listOf(Color(0xFFF3C66A), Color(0xFFE9C98E), Color(0xFFC9B98E), Color(0xFFA9BC84))
            val calendar = listOf(Color(0xFFA9BC84), Color(0xFF8FAA6A), Color(0xFFC9A46A), Color(0xFFA8B2B6))
            val fallback = Color(0xFFA7A399)
        }
        object Layout {
            const val groundRatio = 0.76f
            const val pathStart = 26.0f
            const val pathEnd = 364.0f
            const val pathInset = 20.0f
            const val haruWidth = 26.0f
            const val meetHaruWidth = 96.0f
            const val haruBox = 200.0f
            const val haruGround = 170.0f
            const val haruArtWidth = 70.0f
            const val sunBase = 70.0f
            const val sunArc = 160.0f
            const val sunRadius = 19.0f
            const val sunStart = 40.0f
            const val sunEnd = 350.0f
            const val minSkyGap = 16.0f
            const val labelGap = 10.0f
            const val labelRow = 18.0f
            const val objBox = 200.0f
            const val objGround = 168.0f
            const val objScale = 0.36f
            const val itemFromHaru = 20.0f
            const val itemGap = 6.0f
            const val stripLineY = 40.0f
            const val stripHeight = 200.0f
            const val calendarGap = 1.3f
            const val sparkle = 9.0f
            const val devTaps = 7.0f
            const val pageSnap = 0.12f
            const val fadeTail = 0.18f
            const val parallax = 0.45f
            const val collectionCell = 104.0f
            const val supportGround = 0.825f
            const val supportHaru = 0.11f
            const val pebbleJitter = 0.18f
            const val pebbleSquash = 0.24f
            const val pebbleWobble = 0.22f
            const val pebbleLine = 0.45f
        }
        object Stroke {
            const val ground = 3.2f
            const val box = 2.2f
            const val chip = 1.5f
            const val bar = 1.8f
            const val rule = 1.2f
            const val closedEye = 1.6f
        }
        object Radius {
            const val box = 16.0f
            const val button = 28.0f
            const val chip = 13.0f
            const val bar = 8.0f
            const val cell = 1.6f
        }
        object Motion {
            const val blinkMinMs = 2400.0f
            const val blinkMaxMs = 6200.0f
            const val blinkMs = 170.0f
            const val pageMs = 520.0f
            const val letGoMs = 2200.0f
            const val modalFadeMs = 360.0f
            const val fallMs = 3600.0f
            const val cardDelayMs = 520.0f
            const val lookEase = 0.12f
            const val restEase = 0.02f
            const val tiltRange = 6.0f
            const val sunrise = 6.0f
            const val sunset = 19.0f
            const val keyboardMs = 320.0f
        }
        object Crayon {
            const val vary = 0.3f
            const val fillOffsetX = 1.8f
            const val fillOffsetY = 1.3f
            const val hatchAlpha = 0.3f
            const val hatchGap = 3.2f
            const val hatchAngle = -0.75f
            const val textureScale = 0.55f
            const val passOffset = 0.5f
        }
        object Widget {
            const val small = 160.0f
            const val wide = 340.0f
            const val groundRatio = 0.84f
            const val largeGroundRatio = 0.8f
            const val haruSmall = 32.0f
            const val haruLarge = 51.0f
            const val haruX = 0.72f
            const val sunRadius = 11.0f
            const val sunArcTop = 0.3f
            const val sunArcBase = 0.84f
            const val gridLeft = 0.4f
            const val gridInset = 14.0f
            const val cell = 1.3f
            const val mossScale = 0.34f
            const val familyHaru = 24.0f
            const val familyGround = 0.74f
        }
        object SkyTime {
            object Night {
                val color = Color(0xFF2F3B5E)
                const val alpha = 0.3f
            }
            object Dawn {
                val color = Color(0xFFF1B089)
                const val alpha = 0.26f
            }
            object Dusk {
                val color = Color(0xFFE48B6A)
                const val alpha = 0.3f
            }
            const val dawnFrom = 4.5f
            const val dawnPeak = 6.0f
            const val dayFrom = 8.0f
            const val duskFrom = 16.5f
            const val duskPeak = 18.5f
            const val nightFrom = 20.0f
            const val groundKeep = 0.35f
            const val stars = 7.0f
            const val starSize = 6.0f
            const val starAlpha = 0.75f
        }
        object LetGo {
            const val maxChars = 60.0f
            const val rise = 150.0f
            const val drift = 36.0f
            const val feather = 44.0f
            const val feathers = 9.0f
            const val sway = 34.0f
            const val randomMinDays = 5.0f
            const val randomMaxDays = 20.0f
            const val randomMinAge = 30.0f
        }
        object HaruDraw {
            const val line = 5.0f
            const val eyeLine = 2.8f
            const val shadow = 0.12f
            const val closedAt = 0.97f
            const val lidCurve = 0.22f
            const val pattern = 0.1f
            const val sproutHeight = 22.0f
        }
        object Family {
            const val max = 5.0f
            const val gap = 10.0f
            const val minGap = 6.0f
            const val nameChars = 4.0f
            const val petScale = 0.72f
            const val dogYears = 13.0f
            const val catYears = 15.0f
            const val otherYears = 10.0f
            const val tickShift = 4.0f
            const val feather = 16.0f
        }
        object Touch {
            const val petMs = 900.0f
            const val wiggleDeg = 3.0f
            const val squash = 0.03f
            const val blushMs = 1200.0f
            const val hop = 12.0f
            const val hopMs = 420.0f
            const val windowMs = 3000.0f
            const val restAfter = 5.0f
            const val restWindowMs = 10000.0f
            const val restMs = 2000.0f
            const val doubleMs = 280.0f
            const val openMs = 420.0f
            const val haptic = 1.0f
        }
        object Party {
            const val hatWidth = 22.0f
            const val hatHeight = 26.0f
            const val cakeWidth = 11.0f
            object Colors {
                val hat = Color(0xFFE9A43A)
                val stripe = Color(0xFFD98C7A)
                val pompom = Color(0xFFF6F1E6)
                val cake = Color(0xFFF4EEDF)
                val cream = Color(0xFFD98C7A)
                val flame = Color(0xFFF0A84A)
            }
        }
    }
}
