import SwiftUI

/// 화면 위쪽에서 햇빛처럼 번지는 배경.
struct SkyBackground: View {
    var glow: Double = Tokens.Effect.glowStrength

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width, h = geo.size.height
            ZStack {
                Color.cdBase
                RadialGradient(colors: [Color.cdLight.opacity(glow * 1.2), .clear], center: UnitPoint(x: 0.22, y: -0.05), startRadius: 0, endRadius: max(w, h) * 0.6)
                RadialGradient(colors: [Color.cdOlive.opacity(0.18), .clear], center: UnitPoint(x: 0.3, y: 1.05), startRadius: 0, endRadius: max(w, h) * 0.6)
            }
        }
        .ignoresSafeArea()
    }
}

/// 반투명 유리 카드.
struct GlassCard<Content: View>: View {
    var padding: CGFloat = Tokens.Layout.cardPadding
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.Space.sp3) { content }
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background {
                RoundedRectangle(cornerRadius: Tokens.Radius.lg, style: .continuous)
                    .fill(.ultraThinMaterial)
                    .overlay(RoundedRectangle(cornerRadius: Tokens.Radius.lg, style: .continuous).fill(Color.cdGlass))
                    .overlay(RoundedRectangle(cornerRadius: Tokens.Radius.lg, style: .continuous).strokeBorder(Color.cdGlassEdge, lineWidth: Tokens.Stroke.line))
            }
    }
}

/// 진행 막대. `glowing`이면 끝이 빛 색으로 번진다.
struct ProgressBar: View {
    let value: Double
    var height: CGFloat = Tokens.Stroke.bar
    var glowing = false

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Color.cdDim)
                Capsule()
                    .fill(glowing ? AnyShapeStyle(LinearGradient(colors: [.cdOlive, .cdOlive, .cdLight], startPoint: .leading, endPoint: .trailing)) : AnyShapeStyle(Color.cdOlive))
                    .frame(width: max(height, geo.size.width * min(max(value, 0), 1)))
                    .shadow(color: glowing ? Color.cdLight.opacity(Tokens.Effect.glowStrength) : .clear, radius: height)
            }
        }
        .frame(height: height)
        .accessibilityElement()
        .accessibilityValue(Text(value.formatted(.percent.precision(.fractionLength(0)))))
    }
}

/// 엔소 · 궤도: 가늘고 온전한 원(영원) 위에 굵고 열린 원(삶)과 한 점(오늘).
struct EnsoMark: View {
    var color: Color = .cdOlive
    var dot: Color = .cdNow

    var body: some View {
        Canvas { ctx, size in
            let s = min(size.width, size.height), c = CGPoint(x: size.width / 2, y: size.height / 2), r = s * 0.38
            var orbit = Path(); orbit.addArc(center: c, radius: r, startAngle: .zero, endAngle: .degrees(360), clockwise: false)
            ctx.stroke(orbit, with: .color(color.opacity(0.45)), lineWidth: s * 0.035)
            var life = Path(); life.addArc(center: c, radius: r, startAngle: .degrees(-50), endAngle: .degrees(238), clockwise: false)
            ctx.stroke(life, with: .color(color), style: StrokeStyle(lineWidth: s * 0.11, lineCap: .round))
            let a = Angle.degrees(-86).radians
            let p = CGPoint(x: c.x + cos(a) * r, y: c.y + sin(a) * r), d = s * 0.1
            ctx.fill(Path(ellipseIn: CGRect(x: p.x - d / 2, y: p.y - d / 2, width: d, height: d)), with: .color(dot))
        }
        .accessibilityHidden(true)
    }
}
