import SwiftUI

/// 인생 달력. 지나온 칸은 계절 색, 지금 칸은 빛, 남은 칸은 흐리게.
struct LifeGrid: View {
    let total: Int
    let filled: Int
    let columns: Int
    var glow: Double = Tokens.Effect.glowStrength

    @Environment(\.colorScheme) private var scheme

    private var rows: Int { max(1, (max(total, 1) + columns - 1) / columns) }

    var body: some View {
        Canvas { ctx, size in
            guard total > 0 else { return }
            let traits = UITraitCollection(userInterfaceStyle: scheme == .dark ? .dark : .light)
            let seasons = Tokens.Palette.seasons.map { Color(uiColor: $0.uiColor.resolvedColor(with: traits)) }
            let future = Color(uiColor: Tokens.Palette.future.uiColor.resolvedColor(with: traits))
            let now = Color(uiColor: Tokens.Palette.now.uiColor.resolvedColor(with: traits))
            let cell = min(size.width / CGFloat(columns), size.height / CGFloat(rows))
            let dot = cell * Tokens.Grid.dotRatio
            let originX = (size.width - cell * CGFloat(columns)) / 2
            var paths = Array(repeating: Path(), count: 4), ahead = Path()
            var current: CGRect?
            for i in 0..<total {
                let cx = originX + CGFloat(i % columns) * cell + cell / 2, cy = CGFloat(i / columns) * cell + cell / 2
                if i == filled {
                    let d = cell * Tokens.Grid.nowRatio
                    current = CGRect(x: cx - d / 2, y: cy - d / 2, width: d, height: d)
                    continue
                }
                let rect = CGRect(x: cx - dot / 2, y: cy - dot / 2, width: dot, height: dot)
                if i < filled { paths[min(3, i * 4 / total)].addEllipse(in: rect) } else { ahead.addEllipse(in: rect) }
            }
            for (i, p) in paths.enumerated() { ctx.fill(p, with: .color(seasons[i])) }
            ctx.fill(ahead, with: .color(future))
            if let current {
                var glowCtx = ctx
                glowCtx.addFilter(.shadow(color: now.opacity(0.9), radius: cell * (0.2 + glow)))
                glowCtx.fill(Path(ellipseIn: current), with: .color(now))
            }
        }
        .aspectRatio(CGFloat(columns) / CGFloat(rows), contentMode: .fit)
        .accessibilityElement()
        .accessibilityLabel(Text(L10n.calendar))
        .accessibilityValue(Text("\(filled) / \(total)"))
    }
}
