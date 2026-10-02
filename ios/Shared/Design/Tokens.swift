// 자동 생성 파일 — 직접 고치지 말고 scripts/generate.py 를 실행하세요.

import SwiftUI

enum Tokens {
    enum Space {
        static let sp1: CGFloat = 4
        static let sp2: CGFloat = 8
        static let sp3: CGFloat = 12
        static let sp4: CGFloat = 16
        static let sp5: CGFloat = 20
        static let sp6: CGFloat = 24
        static let sp8: CGFloat = 32
        static let sp10: CGFloat = 40
    }

    enum Layout {
        static let pageMarginCompact: CGFloat = Space.sp4
        static let pageMarginRegular: CGFloat = Space.sp4
        static let pageMarginLarge: CGFloat = Space.sp5
        static let cardPadding: CGFloat = Space.sp5
        static let widgetPadding: CGFloat = Space.sp4
        static let tapTarget: CGFloat = 44
    }

    enum TypeScale {
        static let largeTitle = TypeToken(size: 34, style: .largeTitle, family: .serif, weight: .semibold, tracking: 0)
        static let title2 = TypeToken(size: 22, style: .title2, family: .text, weight: .semibold, tracking: 0)
        static let title3 = TypeToken(size: 20, style: .title3, family: .serif, weight: .semibold, tracking: 0)
        static let headline = TypeToken(size: 17, style: .headline, family: .text, weight: .semibold, tracking: 0)
        static let body = TypeToken(size: 17, style: .body, family: .text, weight: .regular, tracking: 0)
        static let callout = TypeToken(size: 16, style: .callout, family: .text, weight: .regular, tracking: 0)
        static let subhead = TypeToken(size: 15, style: .subheadline, family: .text, weight: .medium, tracking: 0)
        static let footnote = TypeToken(size: 13, style: .footnote, family: .text, weight: .regular, tracking: 0)
        static let caption1 = TypeToken(size: 12, style: .caption1, family: .text, weight: .regular, tracking: 0)
        static let caption2 = TypeToken(size: 11, style: .caption2, family: .text, weight: .bold, tracking: 0)
        static func display(_ c: DeviceClass) -> TypeToken {
            let size: CGFloat = c == .compact ? 54 : c == .large ? 70 : 64
            return TypeToken(size: size, style: .largeTitle, family: .serif, weight: .semibold, tracking: -0.02)
        }
    }

    enum Palette {
        static let base = DynamicColor(light: 0xE7E6DBFF, dark: 0x11130DFF)
        static let foreground = DynamicColor(light: 0x23251CFF, dark: 0xEEEBDDFF)
        static let secondary = DynamicColor(light: 0x5E604BFF, dark: 0xA8A690FF)
        static let dim = DynamicColor(light: 0x23251C1F, dark: 0xEEEBDD21)
        static let olive = DynamicColor(light: 0x5F7236FF, dark: 0xA4B86AFF)
        static let onOlive = DynamicColor(light: 0xFFFFFFFF, dark: 0x11130DFF)
        static let light = DynamicColor(light: 0xF2B35AFF, dark: 0xF5B45CFF)
        static let now = DynamicColor(light: 0xE89A32FF, dark: 0xF5B45CFF)
        static let danger = DynamicColor(light: 0xC9372AFF, dark: 0xFF6B5EFF)
        static let future = DynamicColor(light: 0x23251C1F, dark: 0xEEEBDD1F)
        static let glass = DynamicColor(light: 0xFFFFFF6B, dark: 0xFFFFFF14)
        static let glassEdge = DynamicColor(light: 0xFFFFFFB8, dark: 0xFFFFFF24)
        static let widgetTop = DynamicColor(light: 0xF4F3ECFF, dark: 0x1E2117FF)
        static let widgetBottom = DynamicColor(light: 0xE3E5D2FF, dark: 0x15170FFF)
        static let seasons: [DynamicColor] = [
            DynamicColor(light: 0xA9B67AFF, dark: 0xC3D18EFF),
            DynamicColor(light: 0x5F7236FF, dark: 0x9DB060FF),
            DynamicColor(light: 0xB5651DFF, dark: 0xDB8B4EFF),
            DynamicColor(light: 0x8C8A74FF, dark: 0xA8A690FF),
        ]
    }

    enum Radius {
        static let sm: CGFloat = 10
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
        static let pill: CGFloat = 999
    }

    enum Stroke {
        static let hair: CGFloat = 0.5
        static let line: CGFloat = 1
        static let barThin: CGFloat = 4
        static let bar: CGFloat = 6
        static let barThick: CGFloat = 10
        static let icon: CGFloat = 22
    }

    enum Effect {
        static let glowStrength: Double = 0.45
        static let glassOpacity: Double = 0.5
        static let glassBlur: CGFloat = 26
        enum Sky {
            static let glowDark: CGFloat = 0.5
            static let glowLight: CGFloat = 1.2
            static let radius: CGFloat = 0.6
            static let glowX: CGFloat = 0.22
            static let glowY: CGFloat = -0.05
            static let oliveAlpha: CGFloat = 0.18
            static let oliveX: CGFloat = 0.3
            static let oliveY: CGFloat = 1.05
        }
        enum NowHalo {
            static let alpha: CGFloat = 0.55
            static let radius: CGFloat = 1.6
            static let widgetAlpha: CGFloat = 0.5
            static let widgetBlur: CGFloat = 0.6
            static let widgetRadius: CGFloat = 0.8
        }
    }

    enum Grid {
        static let weeksColumns = 52
        static let monthsColumns = 36
        static let yearsColumns = 10
        static let dotRatio: CGFloat = 0.68
        static let nowRatio: CGFloat = 1.0
        static let widgetMediumColumns = 14
        static let widgetMediumTextRatio: CGFloat = 0.34
        static let widgetLargeColumns = 36
    }

    enum Enso {
        static let radius: CGFloat = 0.38
        static let ringAlpha: CGFloat = 0.45
        static let ring: CGFloat = 0.05
        static let arc: CGFloat = 0.13
        static let arcStart: CGFloat = -50.0
        static let arcSweep: CGFloat = 288.0
        static let dotAngle: CGFloat = -86.0
        static let dot: CGFloat = 0.07
    }

    enum Expectancy {
        static let min: CGFloat = 30.0
        static let max: CGFloat = 120.0
        static let step: CGFloat = 0.5
    }

    enum Widget {
        static let numberScale: CGFloat = 1.05
        static let quoteHeight: CGFloat = 64.0
        static let gridBottom: CGFloat = 16.0
        static let largeFromHeight: CGFloat = 220.0
        static let markSize: CGFloat = 12.0
    }

    enum DeviceWidth {
        static let compactBelow: CGFloat = 390.0
        static let largeFrom: CGFloat = 430.0
    }

    enum Notify {
        static let hour: CGFloat = 7.0
        static let minute: CGFloat = 0.0
        static let eveningHour: CGFloat = 22.0
        static let tomorrowHour: CGFloat = 19.0
        static let birthdayFrom: CGFloat = 17.0
        static let dayOfHour: CGFloat = 9.0
    }

    enum Garden {
        static let unitWidth: CGFloat = 390.0
        enum Colors {
            static let paper: UInt32 = 0xF6F1E6FF
            static let ink: UInt32 = 0x33281FFF
            static let inkSoft: UInt32 = 0x6B6456FF
            static let dim: UInt32 = 0x33281F24
            static let button: UInt32 = 0xB8C98EFF
            static let chip: UInt32 = 0xDCE5C4FF
            static let pupil: UInt32 = 0x1E1A17FF
            static let shine: UInt32 = 0xFFFFFFFF
            static let scrim: UInt32 = 0x28201447
            static let now: UInt32 = 0xE9A43AFF
            static let future: UInt32 = 0x8FAA6A8C
            static let bars: [UInt32] = [0xF3C66AFF, 0xE9C98EFF, 0xC9B98EFF, 0xA9BC84FF]
            static let calendar: [UInt32] = [0xA9BC84FF, 0x8FAA6AFF, 0xC9A46AFF, 0xA8B2B6FF]
            static let fallback: UInt32 = 0xA7A399FF
            static let moss: UInt32 = 0xA9BC7AFF
        }
        enum Layout {
            static let groundRatio: CGFloat = 0.76
            static let pathStart: CGFloat = 26.0
            static let pathEnd: CGFloat = 364.0
            static let pathInset: CGFloat = 20.0
            static let haruWidth: CGFloat = 26.0
            static let meetHaruWidth: CGFloat = 96.0
            static let haruBox: CGFloat = 200.0
            static let haruGround: CGFloat = 170.0
            static let haruArtWidth: CGFloat = 70.0
            static let sunBase: CGFloat = 70.0
            static let sunArc: CGFloat = 160.0
            static let sunRadius: CGFloat = 19.0
            static let sunStart: CGFloat = 40.0
            static let sunEnd: CGFloat = 350.0
            static let minSkyGap: CGFloat = 16.0
            static let labelGap: CGFloat = 10.0
            static let labelRow: CGFloat = 18.0
            static let objBox: CGFloat = 200.0
            static let objGround: CGFloat = 168.0
            static let objScale: CGFloat = 0.36
            static let pineScale: CGFloat = 1.6
            static let pinwheelMs: CGFloat = 7000.0
            static let chimeMs: CGFloat = 3200.0
            static let chimeSwing: CGFloat = 4.0
            static let itemFromHaru: CGFloat = 20.0
            static let itemGap: CGFloat = 6.0
            static let stripLineY: CGFloat = 110.0
            static let hillTop: CGFloat = 108.0
            static let bigFont: CGFloat = 1.3
            static let stripHeight: CGFloat = 300.0
            static let calendarGap: CGFloat = 1.3
            static let sparkle: CGFloat = 9.0
            static let devTaps: CGFloat = 7.0
            static let pageSnap: CGFloat = 0.12
            static let fadeTail: CGFloat = 0.18
            static let parallax: CGFloat = 0.45
            static let collectionCell: CGFloat = 104.0
            static let supportGround: CGFloat = 0.825
            static let supportHaru: CGFloat = 0.11
            static let pebbleJitter: CGFloat = 0.18
            static let pebbleSquash: CGFloat = 0.24
            static let pebbleWobble: CGFloat = 0.22
            static let pebbleLine: CGFloat = 0.45
            static let tabMark: CGFloat = 14.0
        }
        enum Stroke {
            static let ground: CGFloat = 3.2
            static let box: CGFloat = 2.2
            static let chip: CGFloat = 1.5
            static let bar: CGFloat = 1.8
            static let rule: CGFloat = 1.2
            static let closedEye: CGFloat = 1.6
        }
        enum Radius {
            static let box: CGFloat = 16.0
            static let button: CGFloat = 28.0
            static let chip: CGFloat = 13.0
            static let bar: CGFloat = 8.0
            static let cell: CGFloat = 1.6
        }
        enum Motion {
            static let blinkMinMs: CGFloat = 3000.0
            static let blinkMaxMs: CGFloat = 7000.0
            static let blinkTwice: CGFloat = 0.2
            static let blinkMs: CGFloat = 170.0
            static let pageMs: CGFloat = 520.0
            static let letGoMs: CGFloat = 2200.0
            static let modalFadeMs: CGFloat = 360.0
            static let fallMs: CGFloat = 3600.0
            static let cardDelayMs: CGFloat = 520.0
            static let lookEase: CGFloat = 0.12
            static let restEase: CGFloat = 0.02
            static let tiltRange: CGFloat = 6.0
            static let sunrise: CGFloat = 6.0
            static let sunset: CGFloat = 19.0
            static let keyboardMs: CGFloat = 320.0
            static let noteMs: CGFloat = 4000.0
            static let noteMaxMs: CGFloat = 6000.0
            static let noteBaseMs: CGFloat = 2500.0
            static let noteCharMs: CGFloat = 70.0
            static let noteAt: CGFloat = 0.44
            static let turnTilt: CGFloat = 3.0
            static let turnLift: CGFloat = 8.0
            static let turnShade: CGFloat = 0.5
            static let turnEdge: CGFloat = 18.0
            static let turnSnap: CGFloat = 0.2
            static let breathDrift: CGFloat = 26.0
            static let typeMs: CGFloat = 90.0
            static let typePause: CGFloat = 4.0
        }
        enum Crayon {
            static let vary: CGFloat = 0.3
            static let fillOffsetX: CGFloat = 1.8
            static let fillOffsetY: CGFloat = 1.3
            static let hatchAlpha: CGFloat = 0.3
            static let hatchGap: CGFloat = 3.2
            static let hatchAngle: CGFloat = -0.75
            static let textureScale: CGFloat = 0.55
            static let passOffset: CGFloat = 0.5
        }
        enum Widget {
            static let small: CGFloat = 160.0
            static let wide: CGFloat = 340.0
            static let groundRatio: CGFloat = 0.84
            static let largeGroundRatio: CGFloat = 0.8
            static let haruSmall: CGFloat = 32.0
            static let haruLarge: CGFloat = 51.0
            static let haruX: CGFloat = 0.72
            static let sunRadius: CGFloat = 11.0
            static let sunArcTop: CGFloat = 0.3
            static let sunArcBase: CGFloat = 0.84
            static let gridLeft: CGFloat = 0.4
            static let gridInset: CGFloat = 14.0
            static let cell: CGFloat = 1.3
            static let mossScale: CGFloat = 0.34
            static let familyHaru: CGFloat = 24.0
            static let familyGround: CGFloat = 0.74
        }
        enum SkyTime {
            enum Night {
                static let color: UInt32 = 0x2F3B5EFF
                static let alpha: CGFloat = 0.3
            }
            enum Dawn {
                static let color: UInt32 = 0xF1B089FF
                static let alpha: CGFloat = 0.26
            }
            enum Dusk {
                static let color: UInt32 = 0xE48B6AFF
                static let alpha: CGFloat = 0.3
            }
            static let dawnFrom: CGFloat = 4.5
            static let dawnPeak: CGFloat = 6.0
            static let dayFrom: CGFloat = 8.0
            static let duskFrom: CGFloat = 16.5
            static let duskPeak: CGFloat = 18.5
            static let nightFrom: CGFloat = 20.0
            static let groundKeep: CGFloat = 0.35
            static let stars: CGFloat = 4.0
            static let starSize: CGFloat = 6.0
            static let starAlpha: CGFloat = 0.75
        }
        enum LetGo {
            static let maxChars: CGFloat = 60.0
            static let rise: CGFloat = 150.0
            static let drift: CGFloat = 36.0
            static let feather: CGFloat = 44.0
            static let feathers: CGFloat = 9.0
            static let sway: CGFloat = 34.0
            static let randomMinDays: CGFloat = 5.0
            static let randomMaxDays: CGFloat = 20.0
            static let randomMinAge: CGFloat = 30.0
        }
        enum HaruDraw {
            static let line: CGFloat = 5.0
            static let eyeLine: CGFloat = 2.8
            static let shadow: CGFloat = 0.12
            static let closedAt: CGFloat = 0.97
            static let lidCurve: CGFloat = 0.22
            static let pattern: CGFloat = 0.1
            static let sproutHeight: CGFloat = 22.0
            static let fiber: CGFloat = 0.55
            static let fiberScale: CGFloat = 0.5
            static let light: CGFloat = 0.16
            static let shade: CGFloat = 0.1
        }
        enum Family {
            static let max: CGFloat = 9.0
            static let oneRow: CGFloat = 5.0
            static let gap: CGFloat = 10.0
            static let minGap: CGFloat = 6.0
            static let overlap: CGFloat = 0.22
            static let nameChars: CGFloat = 4.0
            static let petScale: CGFloat = 0.72
            static let dogYears: CGFloat = 13.0
            static let catYears: CGFloat = 15.0
            static let otherYears: CGFloat = 10.0
            static let feather: CGFloat = 16.0
            static let offerSize: CGFloat = 20.0
            static let pageStone: CGFloat = 0.45
        }
        enum Touch {
            static let petMs: CGFloat = 900.0
            static let wiggleDeg: CGFloat = 3.0
            static let squash: CGFloat = 0.03
            static let blushMs: CGFloat = 1200.0
            static let hop: CGFloat = 12.0
            static let hopMs: CGFloat = 560.0
            static let touchPad: CGFloat = 8.0
            static let windowMs: CGFloat = 3000.0
            static let restAfter: CGFloat = 5.0
            static let restWindowMs: CGFloat = 10000.0
            static let restMs: CGFloat = 2000.0
            static let doubleMs: CGFloat = 280.0
            static let openMs: CGFloat = 420.0
            static let haptic: CGFloat = 1.0
            static let holdMs: CGFloat = 600.0
        }
        enum TermTouch {
            enum Colors {
                static let bud: UInt32 = 0xE59AA6FF
                static let petal: UInt32 = 0xF2C4CCFF
                static let grass: UInt32 = 0x7E9A5AFF
                static let frost: UInt32 = 0xF4F7FAFF
                static let drizzle: UInt32 = 0x8A9AA8FF
                static let dew: UInt32 = 0xFFFFFFFF
                static let star: UInt32 = 0xFFF4D6FF
                static let heat: UInt32 = 0xF4C37AFF
                static let ice: UInt32 = 0xDDE8F0FF
            }
            static let alpha: CGFloat = 0.38
            static let nightAlpha: CGFloat = 0.7
            static let petals: CGFloat = 6.0
            static let drops: CGFloat = 26.0
            static let flakes: CGFloat = 14.0
            static let tufts: CGFloat = 7.0
            static let glints: CGFloat = 10.0
            static let speckles: CGFloat = 46.0
            static let steps: CGFloat = 7.0
            static let stepGap: CGFloat = 15.0
            static let traceFoot: CGFloat = 0.16
            static let traceFlowers: CGFloat = 2.0
            static let traceFlowerSize: CGFloat = 22.0
            static let warmLamp: CGFloat = 1.35
        }
        enum Party {
            static let hatWidth: CGFloat = 22.0
            static let hatHeight: CGFloat = 26.0
            static let cakeWidth: CGFloat = 11.0
            enum Colors {
                static let hat: UInt32 = 0xF7E9CFFF
                static let stripe: UInt32 = 0xEFA27CFF
                static let dot: UInt32 = 0xFFFFFFFF
                static let pompom: UInt32 = 0xFFF8ECFF
                static let cake: UInt32 = 0xF1C9B8FF
                static let cream: UInt32 = 0xFFF6EAFF
                static let berry: UInt32 = 0xE2655FFF
                static let plate: UInt32 = 0xFBF8F0FF
                static let candle: UInt32 = 0xFBF8F0FF
                static let candleStripe: UInt32 = 0x7FA6C4FF
                static let flame: UInt32 = 0xF0A84AFF
                static let flameCore: UInt32 = 0xFFE7A8FF
                static let glow: UInt32 = 0xF4C37AFF
                static let heart: UInt32 = 0xE98F86FF
            }
            static let tilt: CGFloat = 12.0
            static let card: CGFloat = 0.2
        }
        enum Breath {
            static let calmIn: CGFloat = 4.0
            static let calmOut: CGFloat = 6.0
            static let boxIn: CGFloat = 4.0
            static let boxHold: CGFloat = 0.0
            static let boxOut: CGFloat = 4.0
            static let boxRest: CGFloat = 0.0
            static let sleepIn: CGFloat = 4.0
            static let sleepHold: CGFloat = 7.0
            static let sleepOut: CGFloat = 8.0
            static let swell: CGFloat = 0.03
            static let rise: CGFloat = 6.0
            static let eyesAfter: CGFloat = 3.0
            static let cueSeconds: CGFloat = 60.0
            static let haruWidth: CGFloat = 38.0
            static let ruleInset: CGFloat = 0.3
            static let haruAt: CGFloat = 0.73
            static let nightFrom: CGFloat = 22.0
            static let line: CGFloat = 2.0
            static let sleepFadeAfter: CGFloat = 4000.0
            static let sleepFadeMs: CGFloat = 4000.0
        }
        enum Gaze {
            static let dimAfter: CGFloat = 300.0
            static let dimMs: CGFloat = 30000.0
            static let dimAlpha: CGFloat = 0.72
            static let releaseAfter: CGFloat = 600.0
            static let breathIn: CGFloat = 4.0
            static let breathOut: CGFloat = 6.0
            static let swell: CGFloat = 0.015
            static let lightBase: CGFloat = 0.08
            static let lightBreath: CGFloat = 0.12
            static let dimSlow: CGFloat = 0.5
            static let startAt: CGFloat = 6.0
            static let cloudSpeed: CGFloat = 1.0
            static let birdEvery: CGFloat = 80.0
            static let pieceEvery: CGFloat = 10.0
            static let pieceFall: CGFloat = 9.0
            static let pieceRest: CGFloat = 3.0
            static let pieceSize: CGFloat = 13.0
            static let pondY: CGFloat = 72.0
            static let pondScale: CGFloat = 1.0
            static let fireY: CGFloat = 50.0
            static let fireFromHaru: CGFloat = 70.0
        }
        enum BreathScene {
            static let halo: CGFloat = 0.2
            static let reflect: CGFloat = 0.15
            static let stride: CGFloat = 24.0
            static let stepsFade: CGFloat = 12.0
            static let foot: CGFloat = 0.6
            static let lanternAt: CGFloat = 0.3
            static let flowers: CGFloat = 10.0
            static let flowerSize: CGFloat = 26.0
        }
        enum Sound {
            static let sampleRate: CGFloat = 22050.0
            static let volume: CGFloat = 0.35
            static let chime: CGFloat = 0.5
            static let chimeHz: CGFloat = 528.0
            static let toneLow: CGFloat = 174.0
            static let toneHigh: CGFloat = 177.0
            static let fadeInMs: CGFloat = 2500.0
            static let fadeOutMs: CGFloat = 1800.0
            static let waveSeconds: CGFloat = 9.0
            static let bowl: CGFloat = 0.55
            static let bowlInHz: CGFloat = 220.0
            static let bowlOutHz: CGFloat = 164.8
            static let bowlRing: CGFloat = 8.0
            static let bowlAttackMs: CGFloat = 35.0
            static let bowlBeat: CGFloat = 0.7
            static let introMs: CGFloat = 6000.0
            static let layer: CGFloat = 0.4
            static let layerMin: CGFloat = 18.0
            static let layerMax: CGFloat = 40.0
        }
        enum Night {
            static let darkFrom: CGFloat = 20.0
            static let darkUntil: CGFloat = 6.5
            enum Colors {
                static let base: UInt32 = 0x151A28FF
                static let paper: UInt32 = 0x212838FF
                static let ink: UInt32 = 0xECE4CFFF
                static let inkSoft: UInt32 = 0xB3AC9CFF
                static let dim: UInt32 = 0x394157FF
                static let chip: UInt32 = 0x3E4B38FF
                static let button: UInt32 = 0x57683FFF
                static let future: UInt32 = 0x4E5873FF
                static let scrim: UInt32 = 0x000000A6
                static let sky: UInt32 = 0x10162AFF
                static let lamp: UInt32 = 0xF4C37AFF
                static let firefly: UInt32 = 0xF3E38EFF
                static let moonGlow: UInt32 = 0xF1E9CFFF
            }
            static let skyAlpha: CGFloat = 0.84
            static let groundAlpha: CGFloat = 0.78
            static let stars: CGFloat = 9.0
            static let twinkleMs: CGFloat = 3400.0
            static let fireflies: CGFloat = 2.0
            static let fireflyMs: CGFloat = 5200.0
            static let stoneGlow: CGFloat = 0.1
            static let moonGlow: CGFloat = 0.16
            static let moonDark: CGFloat = 0.16
        }
        enum Question {
            static let perWeek: CGFloat = 2.0
        }
        enum Mood {
            static let days: CGFloat = 30.0
            static let columns: CGFloat = 15.0
            static let gap: CGFloat = 1.28
            static let wobble: CGFloat = 0.12
            static let line: CGFloat = 0.8
            static let nightDarken: CGFloat = 0.18
            enum Colors {
                static let joy: UInt32 = 0xF2C04EFF
                static let hope: UInt32 = 0xF1B089FF
                static let calm: UInt32 = 0xA9C6D4FF
                static let thanks: UInt32 = 0xB8C98EFF
                static let disappoint: UInt32 = 0xB9B4A8FF
                static let sad: UInt32 = 0x8C9AB0FF
                static let worry: UInt32 = 0x9C8FA6FF
                static let none: UInt32 = 0xE6DFCFFF
            }
        }
        enum Letter {
            static let minLines: CGFloat = 3.0
            static let envelope: CGFloat = 34.0
        }
        enum Care {
            static let lookSeconds: CGFloat = 30.0
            static let lookSize: CGFloat = 120.0
            static let lookDown: CGFloat = 0.6
        }
        enum Weather {
            static let seconds: CGFloat = 10.0
            static let drops: CGFloat = 28.0
            static let rainAlpha: CGFloat = 0.35
            static let sunAlpha: CGFloat = 0.22
        }
        enum SleepGarden {
            static let dim: CGFloat = 0.18
        }
        enum Year {
            static let thanks: CGFloat = 3.0
            static let monthAspect: CGFloat = 0.86
            static let twinkleMs: CGFloat = 4200.0
            static let monthDue: CGFloat = 3.0
            static let monthFlower: CGFloat = 0.026
            static let monthStar: CGFloat = 0.009
            static let tileFlower: CGFloat = 0.05
            static let hazeAlpha: CGFloat = 0.07
            static let field: CGFloat = 120.0
            enum Colors {
                static let skyTop: UInt32 = 0x101426FF
                static let skyBottom: UInt32 = 0x1E253CFF
                static let haze: UInt32 = 0xC9C3E8FF
                static let core: UInt32 = 0xF3E6C8FF
                static let plain: UInt32 = 0xECE4CFFF
                static let meadowTop: UInt32 = 0xF6F1E3FF
                static let meadowBottom: UInt32 = 0xE7EDDAFF
                static let meadowHaze: UInt32 = 0xC9D8A8FF
                static let heart: UInt32 = 0xFFF6DEFF
                static let rest: UInt32 = 0x82965FFF
            }
            static let tileStar: CGFloat = 0.018
        }
        enum Share {
            static let lineW: CGFloat = 1080.0
            static let lineH: CGFloat = 1350.0
            static let yearW: CGFloat = 1080.0
            static let yearH: CGFloat = 1350.0
            static let text: CGFloat = 58.0
            static let small: CGFloat = 34.0
            static let pad: CGFloat = 96.0
        }
        enum Song {
            static let gapMs: CGFloat = 320.0
            static let firstMs: CGFloat = 520.0
            static let ringMs: CGFloat = 1800.0
            static let ringSize: CGFloat = 64.0
            static let volume: CGFloat = 0.22
            static let hop: CGFloat = 3.0
        }
        enum ShootingStar {
            static let firstMin: CGFloat = 6.0
            static let firstMax: CGFloat = 18.0
            static let min: CGFloat = 25.0
            static let max: CGFloat = 80.0
            static let ms: CGFloat = 1100.0
            static let length: CGFloat = 0.2
        }
        enum Special {
            static let size: CGFloat = 1.25
            enum Colors {
                static let petal: UInt32 = 0xE9A3B4FF
                static let heart: UInt32 = 0xF2C04EFF
            }
        }
        enum Decor {
            static let treeBoxW: CGFloat = 300.0
            static let treeBoxH: CGFloat = 290.0
            static let treeAtX: CGFloat = 140.0
            static let treeAtY: CGFloat = 270.0
            static let treeScale: CGFloat = 1.0
            static let treeX: CGFloat = 30.0
            static let treePx: CGFloat = 2.0
            static let stageDays1: CGFloat = 100.0
            static let stageDays2: CGFloat = 365.0
            static let stageDays3: CGFloat = 1095.0
            static let postBoxW: CGFloat = 86.0
            static let postBoxH: CGFloat = 134.0
            static let postAtX: CGFloat = 50.0
            static let postAtY: CGFloat = 125.0
            static let postScale: CGFloat = 0.9
            static let postPx: CGFloat = 2.0
            static let chimeX: CGFloat = 25.0
            static let chimeY: CGFloat = 30.0
            static let bellX: CGFloat = 56.0
            static let bellY: CGFloat = 30.5
            static let lanternX: CGFloat = 38.0
            static let lanternY: CGFloat = 30.5
            static let tieX: CGFloat = 17.0
            static let tieY: CGFloat = 28.5
            static let chimeBreaths: CGFloat = 1.0
            static let bellBreaths: CGFloat = 30.0
            static let lanternBreaths: CGFloat = 100.0
            static let swayMs: CGFloat = 3200.0
            static let swayDeg: CGFloat = 4.0
            static let mossBoxW: CGFloat = 90.0
            static let mossBoxH: CGFloat = 28.0
            static let mossAtX: CGFloat = 45.0
            static let mossAtY: CGFloat = 18.0
            static let mossWidth: CGFloat = 1.42
            static let mossPx: CGFloat = 3.0
            static let budGazes: CGFloat = 10.0
            static let budMax: CGFloat = 5.0
            static let pondBoxW: CGFloat = 200.0
            static let pondBoxH: CGFloat = 64.0
            static let fireBoxW: CGFloat = 64.0
            static let fireBoxH: CGFloat = 22.0
            static let kiteBoxW: CGFloat = 50.0
            static let kiteBoxH: CGFloat = 140.0
            static let kiteAtX: CGFloat = 25.0
            static let kiteAtY: CGFloat = 25.0
            static let kiteScale: CGFloat = 0.85
            static let kitePx: CGFloat = 3.0
            static let kiteX: CGFloat = 292.0
            static let kiteHigh: CGFloat = 0.4
            static let kiteLines: CGFloat = 30.0
            static let ribbonLines: CGFloat = 30.0
            static let ribbonMax: CGFloat = 4.0
            static let ribbonMute: CGFloat = 0.45
            static let kiteMs: CGFloat = 12000.0
            static let kiteDrift: CGFloat = 2.0
            static let kiteTilt: CGFloat = 3.0
            static let cardBoxW: CGFloat = 60.0
            static let cardBoxH: CGFloat = 64.0
            static let cardAtY: CGFloat = 56.0
            static let cardPx: CGFloat = 5.0
            static let cardMini: CGFloat = 0.5
            static let cardFromTree: CGFloat = 30.0
            static let cardTilt: CGFloat = 0.0
            static let gazeCountMs: CGFloat = 30000.0
            static let glow: CGFloat = 34.0
            enum Colors {
                static let glow: UInt32 = 0xFFCE82FF
                static let tail: UInt32 = 0xF4EEDFFF
            }
            static let nightKeep: CGFloat = 0.82
        }
        enum Chance {
            static let bubblesMs: CGFloat = 8000.0
            static let firefliesMs: CGFloat = 10000.0
            static let fireflies: CGFloat = 5.0
            static let rainbowMs: CGFloat = 8000.0
            static let butterfliesMs: CGFloat = 11000.0
            static let auroraMs: CGFloat = 10000.0
            static let windMs: CGFloat = 7000.0
            static let flapHz: CGFloat = 3.5
            static let glideMs: CGFloat = 500.0
            static let glideEvery: CGFloat = 2600.0
            static let flyBox: CGFloat = 9.0
            static let snailMinutes: CGFloat = 60.0
            static let snailWalk: CGFloat = 26.0
            static let snailScale: CGFloat = 0.5
            static let rainbowScale: CGFloat = 1.0
            static let auroraScale: CGFloat = 1.3
            static let windPieces: CGFloat = 5.0
            static let windSize: CGFloat = 9.0
        }
    }
}
