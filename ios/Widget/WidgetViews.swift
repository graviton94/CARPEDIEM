import SwiftUI
import WidgetKit

// MARK: - 공통

/// 위젯 배경. iOS 위젯은 실제로 투명할 수 없어, 빛 번짐과 윤곽 하이라이트로 유리 느낌을 낸다.
struct WidgetSurface: View {
    var body: some View {
        ZStack {
            LinearGradient(colors: [.cdWidgetTop, .cdWidgetBottom], startPoint: .topLeading, endPoint: .bottomTrailing)
            RadialGradient(colors: [Color.cdLight.opacity(Tokens.Effect.glowStrength * 0.8), .clear], center: .topLeading, startRadius: 0, endRadius: 180)
        }
    }
}

/// 위젯 크기가 기기마다 달라서, 기준 크기(iPhone 16 작은 위젯 158pt) 대비 비율로 글자를 맞춘다.
private func widgetScale(_ size: CGSize, family: WidgetFamily) -> CGFloat {
    let reference: CGFloat = 158 - Tokens.Layout.widgetPadding * 2
    let side = family == .systemSmall ? min(size.width, size.height) : size.height
    return min(1.15, max(0.85, side / reference))
}

private struct WidgetLabel: View {
    let text: String
    var mark = false
    let scale: CGFloat

    var body: some View {
        HStack(spacing: Tokens.Space.sp1) {
            if mark { EnsoMark().frame(width: Tokens.TypeScale.caption1.size * scale, height: Tokens.TypeScale.caption1.size * scale) }
            Text(text.uppercased()).textStyle(Tokens.TypeScale.caption2, scale: scale).tracking(0.6).foregroundStyle(Color.cdOlive)
        }
        .widgetAccentable()
    }
}

private struct EmptyWidget: View {
    var body: some View {
        VStack(spacing: Tokens.Space.sp2) {
            EnsoMark().frame(width: Tokens.Stroke.icon * 1.6, height: Tokens.Stroke.icon * 1.6)
            Text(L10n.widgetEmpty).textStyle(Tokens.TypeScale.caption1).foregroundStyle(Color.cdSecondary).multilineTextAlignment(.center)
        }
    }
}

private func number(_ n: Int) -> String { n.formatted(.number) }

// MARK: - 남은 날 (작게 + 잠금 화면)

struct DaysLeftView: View {
    @Environment(\.widgetFamily) private var family
    let entry: LifeEntry

    var body: some View {
        Group {
            if let s = entry.snapshot {
                switch family {
                case .accessoryCircular: circular(s)
                case .accessoryRectangular: rectangular(s)
                case .accessoryInline: Label { Text(L10n.lockInline(number(s.remaining(.days)))) } icon: { Image(systemName: "circle.dashed") }
                default: small(s)
                }
            } else {
                EmptyWidget()
            }
        }
        .containerBackground(for: .widget) {
            if family == .accessoryCircular { AccessoryWidgetBackground() } else if family.isAccessory { Color.clear } else { WidgetSurface() }
        }
    }

    private func small(_ s: LifeSnapshot) -> some View {
        GeometryReader { geo in
            let k = widgetScale(geo.size, family: family)
            VStack(alignment: .leading, spacing: 0) {
                WidgetLabel(text: entry.unit == .days ? L10n.widgetDaysLeft : entry.unit.label, mark: true, scale: k)
                Spacer(minLength: 0)
                Text(number(s.remaining(entry.unit)))
                    .textStyle(Tokens.TypeScale.largeTitle, scale: k * 1.12)
                    .monospacedDigit().lineLimit(1).minimumScaleFactor(0.6)
                    .shadow(color: Color.cdLight.opacity(Tokens.Effect.glowStrength), radius: Tokens.Space.sp3)
                Spacer(minLength: 0)
                ProgressBar(value: s.progress, height: Tokens.Stroke.barThin * k, glowing: true)
                HStack {
                    Text(s.progress.formatted(.percent.precision(.fractionLength(1))))
                    Spacer()
                    Text(s.season.label)
                }
                .textStyle(Tokens.TypeScale.caption2, scale: k).fontWeight(.regular).foregroundStyle(Color.cdSecondary)
                .padding(.top, Tokens.Space.sp1)
            }
        }
    }

    private func circular(_ s: LifeSnapshot) -> some View {
        Gauge(value: s.progress) {
            EnsoMark(color: .primary, dot: .primary)
        } currentValueLabel: {
            Text("\(Int(s.progress * 100))")
        }
        .gaugeStyle(.accessoryCircularCapacity)
    }

    private func rectangular(_ s: LifeSnapshot) -> some View {
        VStack(alignment: .leading, spacing: Tokens.Space.sp1 / 2) {
            Text(L10n.lockInline(number(s.remaining(.days)))).font(.headline).widgetAccentable()
            Text(L10n.lockRectSub(number(s.remaining(.weeks)))).font(.caption)
            Gauge(value: s.progress) { EmptyView() }.gaugeStyle(.accessoryLinearCapacity)
        }
    }
}

// MARK: - 오늘 (작게)

struct TodayView: View {
    @Environment(\.widgetFamily) private var family
    let entry: LifeEntry

    var body: some View {
        GeometryReader { geo in
            let k = widgetScale(geo.size, family: family)
            let today = LifeSnapshot(birthDate: entry.date, expectancy: 1, now: entry.date).period(.day)
            VStack(alignment: .leading, spacing: 0) {
                WidgetLabel(text: L10n.widgetToday, scale: k)
                ZStack {
                    Circle().stroke(Color.cdDim, lineWidth: Tokens.Stroke.barThick * k * 0.8)
                    Circle().trim(from: 0, to: today.progress)
                        .stroke(Color.cdOlive, style: StrokeStyle(lineWidth: Tokens.Stroke.barThick * k * 0.8, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                    VStack(spacing: 0) {
                        Text(L10n.widgetTodayLeft("\(today.hoursLeft)")).textStyle(Tokens.TypeScale.title2, scale: k).monospacedDigit()
                        Text(L10n.widgetTodaySub).textStyle(Tokens.TypeScale.caption2, scale: k).fontWeight(.regular).foregroundStyle(Color.cdSecondary)
                    }
                }
                .padding(Tokens.Space.sp2 * k)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .containerBackground(for: .widget) { WidgetSurface() }
    }
}

// MARK: - 인생 달력 (중간 · 크게)

struct LifeCalendarView: View {
    @Environment(\.widgetFamily) private var family
    let entry: LifeEntry

    var body: some View {
        Group {
            if let s = entry.snapshot {
                GeometryReader { geo in
                    let k = widgetScale(geo.size, family: family)
                    if family == .systemLarge { large(s, k) } else { medium(s, k) }
                }
            } else {
                EmptyWidget()
            }
        }
        .containerBackground(for: .widget) { WidgetSurface() }
    }

    private func medium(_ s: LifeSnapshot, _ k: CGFloat) -> some View {
        HStack(spacing: Tokens.Space.sp4 * k) {
            VStack(alignment: .leading, spacing: 0) {
                WidgetLabel(text: L10n.calendar, scale: k)
                Spacer(minLength: 0)
                Text(number(s.remaining(.years))).textStyle(Tokens.TypeScale.largeTitle, scale: k).monospacedDigit()
                    .shadow(color: Color.cdLight.opacity(Tokens.Effect.glowStrength), radius: Tokens.Space.sp3)
                Text(L10n.widgetYearsLeft).textStyle(Tokens.TypeScale.footnote, scale: k).foregroundStyle(Color.cdSecondary)
                Spacer(minLength: 0)
                Text("\(s.season.label) · \(s.progress.formatted(.percent.precision(.fractionLength(0))))")
                    .textStyle(Tokens.TypeScale.caption2, scale: k).fontWeight(.regular).foregroundStyle(Color.cdSecondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            LifeGrid(total: s.total(.years), filled: s.lived(.years), columns: Tokens.Grid.widgetMediumColumns)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .layoutPriority(1)
        }
    }

    private func large(_ s: LifeSnapshot, _ k: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: Tokens.Space.sp3 * k) {
            HStack(alignment: .firstTextBaseline) {
                WidgetLabel(text: L10n.calendar, scale: k)
                Spacer()
                Text(L10n.widgetMonthsLeft(number(s.remaining(.months))))
                    .textStyle(Tokens.TypeScale.caption1, scale: k).foregroundStyle(Color.cdSecondary)
            }
            LifeGrid(total: s.total(.months), filled: s.lived(.months), columns: Tokens.Grid.widgetLargeColumns)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            if let q = entry.quote {
                Divider().overlay(Color.cdDim)
                Text(QuoteText.primary(q, language: LifeStore.shared.quoteLanguage))
                    .textStyle(Tokens.TypeScale.footnote.serifVariant, scale: k)
                    .lineLimit(3)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

private extension WidgetFamily {
    var isAccessory: Bool { self == .accessoryCircular || self == .accessoryRectangular || self == .accessoryInline }
}
