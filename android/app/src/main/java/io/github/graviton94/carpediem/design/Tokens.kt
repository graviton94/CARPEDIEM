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
        const val hour = 7.0f
        const val minute = 0.0f
        const val eveningHour = 22.0f
        const val tomorrowHour = 19.0f
        const val birthdayFrom = 17.0f
        const val dayOfHour = 9.0f
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
            val moss = Color(0xFFA9BC7A)
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
            const val pineScale = 1.6f
            const val pinwheelMs = 7000.0f
            const val chimeMs = 3200.0f
            const val chimeSwing = 4.0f
            const val itemFromHaru = 20.0f
            const val itemGap = 6.0f
            const val stripLineY = 110.0f
            const val hillTop = 108.0f
            const val bigFont = 1.3f
            const val stripHeight = 300.0f
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
            const val tabMark = 14.0f
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
            const val blinkMinMs = 3000.0f
            const val blinkMaxMs = 7000.0f
            const val blinkTwice = 0.2f
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
            const val noteMs = 4000.0f
            const val noteMaxMs = 6000.0f
            const val noteBaseMs = 2500.0f
            const val noteCharMs = 70.0f
            const val noteAt = 0.44f
            const val turnTilt = 3.0f
            const val turnLift = 8.0f
            const val turnShade = 0.5f
            const val turnEdge = 18.0f
            const val turnSnap = 0.2f
            const val breathDrift = 26.0f
            const val typeMs = 90.0f
            const val typePause = 4.0f
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
            const val stars = 4.0f
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
            const val fiber = 0.55f
            const val fiberScale = 0.5f
            const val light = 0.16f
            const val shade = 0.1f
        }
        object Family {
            const val max = 9.0f
            const val oneRow = 5.0f
            const val gap = 10.0f
            const val minGap = 6.0f
            const val overlap = 0.22f
            const val nameChars = 4.0f
            const val petScale = 0.72f
            const val dogYears = 13.0f
            const val catYears = 15.0f
            const val otherYears = 10.0f
            const val feather = 16.0f
            const val offerSize = 20.0f
            const val pageStone = 0.45f
        }
        object Touch {
            const val petMs = 900.0f
            const val wiggleDeg = 3.0f
            const val squash = 0.03f
            const val blushMs = 1200.0f
            const val hop = 12.0f
            const val hopMs = 560.0f
            const val touchPad = 8.0f
            const val windowMs = 3000.0f
            const val restAfter = 5.0f
            const val restWindowMs = 10000.0f
            const val restMs = 2000.0f
            const val doubleMs = 280.0f
            const val openMs = 420.0f
            const val haptic = 1.0f
            const val holdMs = 600.0f
        }
        object TermTouch {
            object Colors {
                val bud = Color(0xFFE59AA6)
                val petal = Color(0xFFF2C4CC)
                val grass = Color(0xFF7E9A5A)
                val frost = Color(0xFFF4F7FA)
                val drizzle = Color(0xFF8A9AA8)
                val dew = Color(0xFFFFFFFF)
                val star = Color(0xFFFFF4D6)
                val heat = Color(0xFFF4C37A)
                val ice = Color(0xFFDDE8F0)
            }
            const val alpha = 0.38f
            const val nightAlpha = 0.7f
            const val petals = 6.0f
            const val drops = 26.0f
            const val flakes = 14.0f
            const val tufts = 7.0f
            const val glints = 10.0f
            const val speckles = 46.0f
            const val steps = 7.0f
            const val stepGap = 15.0f
            const val traceFoot = 0.16f
            const val traceFlowers = 2.0f
            const val traceFlowerSize = 22.0f
            const val warmLamp = 1.35f
        }
        object Party {
            const val hatWidth = 22.0f
            const val hatHeight = 26.0f
            const val cakeWidth = 11.0f
            object Colors {
                val hat = Color(0xFFF7E9CF)
                val stripe = Color(0xFFEFA27C)
                val dot = Color(0xFFFFFFFF)
                val pompom = Color(0xFFFFF8EC)
                val cake = Color(0xFFF1C9B8)
                val cream = Color(0xFFFFF6EA)
                val berry = Color(0xFFE2655F)
                val plate = Color(0xFFFBF8F0)
                val candle = Color(0xFFFBF8F0)
                val candleStripe = Color(0xFF7FA6C4)
                val flame = Color(0xFFF0A84A)
                val flameCore = Color(0xFFFFE7A8)
                val glow = Color(0xFFF4C37A)
                val heart = Color(0xFFE98F86)
            }
            const val tilt = 12.0f
            const val card = 0.2f
        }
        object Breath {
            const val calmIn = 4.0f
            const val calmOut = 6.0f
            const val boxIn = 4.0f
            const val boxHold = 0.0f
            const val boxOut = 4.0f
            const val boxRest = 0.0f
            const val sleepIn = 4.0f
            const val sleepHold = 7.0f
            const val sleepOut = 8.0f
            const val swell = 0.03f
            const val rise = 6.0f
            const val eyesAfter = 3.0f
            const val cueSeconds = 60.0f
            const val haruWidth = 38.0f
            const val ruleInset = 0.3f
            const val haruAt = 0.73f
            const val nightFrom = 22.0f
            const val line = 2.0f
            const val sleepFadeAfter = 4000.0f
            const val sleepFadeMs = 4000.0f
        }
        object Gaze {
            const val dimAfter = 300.0f
            const val dimMs = 30000.0f
            const val dimAlpha = 0.72f
            const val releaseAfter = 600.0f
            const val breathIn = 4.0f
            const val breathOut = 6.0f
            const val swell = 0.015f
            const val lightBase = 0.08f
            const val lightBreath = 0.12f
            const val dimSlow = 0.5f
            const val startAt = 6.0f
            const val cloudSpeed = 1.0f
            const val birdEvery = 80.0f
            const val pieceEvery = 10.0f
            const val pieceFall = 9.0f
            const val pieceRest = 3.0f
            const val pieceSize = 13.0f
            const val pondY = 72.0f
            const val pondScale = 1.0f
            const val fireY = 50.0f
            const val fireFromHaru = 70.0f
        }
        object BreathScene {
            const val halo = 0.2f
            const val reflect = 0.15f
            const val stride = 24.0f
            const val stepsFade = 12.0f
            const val foot = 0.6f
            const val lanternAt = 0.3f
            const val flowers = 10.0f
            const val flowerSize = 26.0f
        }
        object Sound {
            const val sampleRate = 22050.0f
            const val volume = 0.35f
            const val chime = 0.5f
            const val chimeHz = 528.0f
            const val toneLow = 174.0f
            const val toneHigh = 177.0f
            const val fadeInMs = 2500.0f
            const val fadeOutMs = 1800.0f
            const val waveSeconds = 9.0f
            const val bowl = 0.55f
            const val bowlInHz = 220.0f
            const val bowlOutHz = 164.8f
            const val bowlRing = 8.0f
            const val bowlAttackMs = 35.0f
            const val bowlBeat = 0.7f
            const val introMs = 6000.0f
            const val layer = 0.4f
            const val layerMin = 18.0f
            const val layerMax = 40.0f
        }
        object Night {
            const val darkFrom = 20.0f
            const val darkUntil = 6.5f
            object Colors {
                val base = Color(0xFF151A28)
                val paper = Color(0xFF212838)
                val ink = Color(0xFFECE4CF)
                val inkSoft = Color(0xFFB3AC9C)
                val dim = Color(0xFF394157)
                val chip = Color(0xFF3E4B38)
                val button = Color(0xFF57683F)
                val future = Color(0xFF4E5873)
                val scrim = Color(0xA6000000)
                val sky = Color(0xFF10162A)
                val lamp = Color(0xFFF4C37A)
                val firefly = Color(0xFFF3E38E)
                val moonGlow = Color(0xFFF1E9CF)
            }
            const val skyAlpha = 0.84f
            const val groundAlpha = 0.78f
            const val stars = 9.0f
            const val twinkleMs = 3400.0f
            const val fireflies = 2.0f
            const val fireflyMs = 5200.0f
            const val stoneGlow = 0.1f
            const val moonGlow = 0.16f
            const val moonDark = 0.16f
        }
        object Question {
            const val perWeek = 2.0f
        }
        object Mood {
            const val days = 30.0f
            const val columns = 15.0f
            const val gap = 1.28f
            const val wobble = 0.12f
            const val line = 0.8f
            const val nightDarken = 0.18f
            object Colors {
                val joy = Color(0xFFF2C04E)
                val hope = Color(0xFFF1B089)
                val calm = Color(0xFFA9C6D4)
                val thanks = Color(0xFFB8C98E)
                val disappoint = Color(0xFFB9B4A8)
                val sad = Color(0xFF8C9AB0)
                val worry = Color(0xFF9C8FA6)
                val none = Color(0xFFE6DFCF)
            }
        }
        object Letter {
            const val minLines = 3.0f
            const val envelope = 34.0f
        }
        object Care {
            const val lookSeconds = 30.0f
            const val lookSize = 120.0f
            const val lookDown = 0.6f
        }
        object Weather {
            const val seconds = 10.0f
            const val drops = 28.0f
            const val rainAlpha = 0.35f
            const val sunAlpha = 0.22f
        }
        object SleepGarden {
            const val dim = 0.18f
        }
        object Year {
            const val thanks = 3.0f
            const val monthAspect = 0.86f
            const val twinkleMs = 4200.0f
            const val monthDue = 3.0f
            const val monthFlower = 0.026f
            const val monthStar = 0.009f
            const val tileFlower = 0.05f
            const val hazeAlpha = 0.07f
            const val field = 120.0f
            object Colors {
                val skyTop = Color(0xFF101426)
                val skyBottom = Color(0xFF1E253C)
                val haze = Color(0xFFC9C3E8)
                val core = Color(0xFFF3E6C8)
                val plain = Color(0xFFECE4CF)
                val meadowTop = Color(0xFFF6F1E3)
                val meadowBottom = Color(0xFFE7EDDA)
                val meadowHaze = Color(0xFFC9D8A8)
                val heart = Color(0xFFFFF6DE)
                val rest = Color(0xFF82965F)
            }
            const val tileStar = 0.018f
        }
        object Share {
            const val lineW = 1080.0f
            const val lineH = 1350.0f
            const val yearW = 1080.0f
            const val yearH = 1350.0f
            const val text = 58.0f
            const val small = 34.0f
            const val pad = 96.0f
        }
        object Song {
            const val gapMs = 320.0f
            const val firstMs = 520.0f
            const val ringMs = 1800.0f
            const val ringSize = 64.0f
            const val volume = 0.22f
            const val hop = 3.0f
        }
        object ShootingStar {
            const val firstMin = 6.0f
            const val firstMax = 18.0f
            const val min = 25.0f
            const val max = 80.0f
            const val ms = 1100.0f
            const val length = 0.2f
        }
        object Special {
            const val size = 1.25f
            object Colors {
                val petal = Color(0xFFE9A3B4)
                val heart = Color(0xFFF2C04E)
            }
        }
        object Decor {
            const val treeBoxW = 300.0f
            const val treeBoxH = 290.0f
            const val treeAtX = 140.0f
            const val treeAtY = 270.0f
            const val treeScale = 1.0f
            const val treeX = 30.0f
            const val treePx = 2.0f
            const val stageDays1 = 100.0f
            const val stageDays2 = 365.0f
            const val stageDays3 = 1095.0f
            const val postBoxW = 86.0f
            const val postBoxH = 134.0f
            const val postAtX = 50.0f
            const val postAtY = 125.0f
            const val postScale = 0.9f
            const val postPx = 2.0f
            const val chimeX = 25.0f
            const val chimeY = 30.0f
            const val bellX = 56.0f
            const val bellY = 30.5f
            const val lanternX = 38.0f
            const val lanternY = 30.5f
            const val tieX = 17.0f
            const val tieY = 28.5f
            const val chimeBreaths = 1.0f
            const val bellBreaths = 30.0f
            const val lanternBreaths = 100.0f
            const val swayMs = 3200.0f
            const val swayDeg = 4.0f
            const val mossBoxW = 90.0f
            const val mossBoxH = 28.0f
            const val mossAtX = 45.0f
            const val mossAtY = 18.0f
            const val mossWidth = 1.42f
            const val mossPx = 3.0f
            const val budGazes = 10.0f
            const val budMax = 5.0f
            const val pondBoxW = 200.0f
            const val pondBoxH = 64.0f
            const val fireBoxW = 64.0f
            const val fireBoxH = 22.0f
            const val kiteBoxW = 50.0f
            const val kiteBoxH = 140.0f
            const val kiteAtX = 25.0f
            const val kiteAtY = 25.0f
            const val kiteScale = 0.85f
            const val kitePx = 3.0f
            const val kiteX = 292.0f
            const val kiteHigh = 0.4f
            const val kiteLines = 30.0f
            const val ribbonLines = 30.0f
            const val ribbonMax = 4.0f
            const val ribbonMute = 0.45f
            const val kiteMs = 12000.0f
            const val kiteDrift = 2.0f
            const val kiteTilt = 3.0f
            const val cardBoxW = 60.0f
            const val cardBoxH = 64.0f
            const val cardAtY = 56.0f
            const val cardPx = 5.0f
            const val cardMini = 0.5f
            const val cardFromTree = 30.0f
            const val cardTilt = 0.0f
            const val gazeCountMs = 30000.0f
            const val glow = 34.0f
            object Colors {
                val glow = Color(0xFFFFCE82)
                val tail = Color(0xFFF4EEDF)
            }
            const val nightKeep = 0.82f
        }
        object Chance {
            const val bubblesMs = 8000.0f
            const val firefliesMs = 10000.0f
            const val fireflies = 5.0f
            const val rainbowMs = 8000.0f
            const val butterfliesMs = 11000.0f
            const val auroraMs = 10000.0f
            const val windMs = 7000.0f
            const val flapHz = 3.5f
            const val glideMs = 500.0f
            const val glideEvery = 2600.0f
            const val flyBox = 9.0f
            const val snailMinutes = 60.0f
            const val snailWalk = 26.0f
            const val snailScale = 0.5f
            const val rainbowScale = 1.0f
            const val auroraScale = 1.3f
            const val windPieces = 5.0f
            const val windSize = 9.0f
        }
    }
}
