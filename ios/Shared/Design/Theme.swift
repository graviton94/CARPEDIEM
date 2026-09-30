import SwiftUI
import UIKit

/// 화면 폭에 따른 크기 등급. 큰 숫자 크기와 화면 좌우 여백만 이 등급을 따른다.
enum DeviceClass {
    case compact, regular, large

    init(width: CGFloat) {
        switch width {
        case ..<390: self = .compact
        case 430...: self = .large
        default: self = .regular
        }
    }

    var pageMargin: CGFloat {
        switch self {
        case .compact: Tokens.Layout.pageMarginCompact
        case .regular: Tokens.Layout.pageMarginRegular
        case .large: Tokens.Layout.pageMarginLarge
        }
    }
}

private struct DeviceClassKey: EnvironmentKey {
    static let defaultValue = DeviceClass.regular
}

extension EnvironmentValues {
    var deviceClass: DeviceClass {
        get { self[DeviceClassKey.self] }
        set { self[DeviceClassKey.self] = newValue }
    }
}

/// 라이트·다크에 따라 바뀌는 색. 값은 0xRRGGBBAA.
struct DynamicColor {
    let light: UInt32
    let dark: UInt32

    var color: Color { Color(uiColor: uiColor) }

    var uiColor: UIColor {
        UIColor { $0.userInterfaceStyle == .dark ? Self.make(dark) : Self.make(light) }
    }

    private static func make(_ v: UInt32) -> UIColor {
        UIColor(red: CGFloat((v >> 24) & 0xFF) / 255, green: CGFloat((v >> 16) & 0xFF) / 255,
                blue: CGFloat((v >> 8) & 0xFF) / 255, alpha: CGFloat(v & 0xFF) / 255)
    }
}

/// 글자 토큰. 크기는 iOS Dynamic Type 기본값이며 사용자의 글자 크기 설정에 맞춰 커진다.
struct TypeToken {
    enum Family { case serif, text }
    enum Weight { case regular, medium, semibold, bold }

    let size: CGFloat
    let style: UIFont.TextStyle
    let family: Family
    let weight: Weight
    let tracking: CGFloat

    /// 사용자의 글자 크기 설정을 반영한 크기.
    func scaledSize(_ scale: CGFloat? = nil) -> CGFloat {
        UIFontMetrics(forTextStyle: style).scaledValue(for: size * (scale ?? 1))
    }

    /// `scale`은 위젯처럼 크기가 기기마다 다른 틀 안에서 비율을 맞출 때만 쓴다.
    func font(scale: CGFloat = 1) -> Font {
        let pointSize = scaledSize(scale)
        switch family {
        case .text:
            return .system(size: pointSize, weight: systemWeight)
        case .serif:
            return Font(SerifFont.make(size: pointSize, semibold: weight == .semibold || weight == .bold) as CTFont)
        }
    }

    private var systemWeight: Font.Weight {
        switch weight {
        case .regular: .regular
        case .medium: .medium
        case .semibold: .semibold
        case .bold: .bold
        }
    }
}

/// 라틴 글자는 Lora, 한글은 Noto Serif KR로 이어 쓰는 명조 서체.
enum SerifFont {
    static func make(size: CGFloat, semibold: Bool) -> UIFont {
        let latin = semibold ? "Lora-SemiBold" : "Lora-Medium"
        let korean = semibold ? "NotoSerifKR-SemiBold" : "NotoSerifKR-Medium"
        let cascade = UIFontDescriptor(fontAttributes: [.name: korean])
        let descriptor = UIFontDescriptor(fontAttributes: [.name: latin]).addingAttributes([.cascadeList: [cascade]])
        return UIFont(descriptor: descriptor, size: size)
    }
}

extension View {
    /// 글자 토큰을 적용한다. 자간은 토큰의 비율(em)로 준다.
    func textStyle(_ token: TypeToken, scale: CGFloat = 1) -> some View {
        font(token.font(scale: scale)).tracking(token.tracking * token.scaledSize(scale))
    }
}

extension Color {
    static let cdBase = Tokens.Palette.base.color
    static let cdForeground = Tokens.Palette.foreground.color
    static let cdSecondary = Tokens.Palette.secondary.color
    static let cdDim = Tokens.Palette.dim.color
    static let cdOlive = Tokens.Palette.olive.color
    static let cdOnOlive = Tokens.Palette.onOlive.color
    static let cdLight = Tokens.Palette.light.color
    static let cdNow = Tokens.Palette.now.color
    static let cdDanger = Tokens.Palette.danger.color
    static let cdFuture = Tokens.Palette.future.color
    static let cdGlass = Tokens.Palette.glass.color
    static let cdGlassEdge = Tokens.Palette.glassEdge.color
    static let cdWidgetTop = Tokens.Palette.widgetTop.color
    static let cdWidgetBottom = Tokens.Palette.widgetBottom.color
}
