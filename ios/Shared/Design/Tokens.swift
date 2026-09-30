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
}
