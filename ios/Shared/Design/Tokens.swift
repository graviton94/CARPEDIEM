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
        static let hour: CGFloat = 8.0
        static let minute: CGFloat = 50.0
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
            static let objScale: CGFloat = 0.5
            static let itemFromHaru: CGFloat = 20.0
            static let itemGap: CGFloat = 6.0
            static let stripLineY: CGFloat = 40.0
            static let stripHeight: CGFloat = 200.0
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
            static let blinkMinMs: CGFloat = 2400.0
            static let blinkMaxMs: CGFloat = 6200.0
            static let blinkMs: CGFloat = 170.0
            static let pageMs: CGFloat = 520.0
            static let letGoMs: CGFloat = 2200.0
            static let modalFadeMs: CGFloat = 360.0
            static let fallMs: CGFloat = 3600.0
            static let cardDelayMs: CGFloat = 520.0
            static let lookEase: CGFloat = 0.12
            static let restEase: CGFloat = 0.02
            static let tiltRange: CGFloat = 6.0
            static let sleepFrom: CGFloat = 22.0
            static let sleepTo: CGFloat = 6.0
            static let sunrise: CGFloat = 6.0
            static let sunset: CGFloat = 19.0
            static let sleepyLid: CGFloat = 0.55
            static let sleepyLook: CGFloat = 0.6
            static let keyboardMs: CGFloat = 320.0
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
            static let stars: CGFloat = 7.0
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
        }
    }
}
