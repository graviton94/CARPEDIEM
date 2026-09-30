import SwiftUI

struct HomeView: View {
    let profile: LifeProfile
    @EnvironmentObject private var model: AppModel
    @Environment(\.deviceClass) private var deviceClass
    @AppStorage("home.unit") private var unit: LifeUnit = .days
    @AppStorage("home.grid") private var grid: GridScale = .months
    @State private var showSettings = false

    var body: some View {
        NavigationStack {
            ZStack {
                SkyBackground()
                TimelineView(.periodic(from: .now, by: 60)) { context in
                    let s = LifeSnapshot(profile: profile, now: context.date)
                    ScrollView {
                        VStack(alignment: .leading, spacing: Tokens.Space.sp3) {
                            Text(verbatim: "Carpe Diem").textStyle(Tokens.TypeScale.largeTitle)
                                .padding(.horizontal, Tokens.Space.sp1)
                                .padding(.bottom, Tokens.Space.sp1)
                            timeLeft(s)
                            if let q = model.quote { words(q) }
                            path(s)
                            flow(s)
                            calendar(s)
                        }
                        .padding(.horizontal, deviceClass.pageMargin)
                        .padding(.bottom, Tokens.Space.sp10)
                    }
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showSettings = true } label: { Image(systemName: "gearshape") }
                        .accessibilityLabel(L10n.settings)
                        .accessibilityIdentifier("settings")
                        .tint(.cdForeground)
                }
            }
            .sheet(isPresented: $showSettings) { SettingsView(profile: profile) }
        }
    }

    private func timeLeft(_ s: LifeSnapshot) -> some View {
        GlassCard {
            Text(L10n.timeLeft).textStyle(Tokens.TypeScale.subhead).foregroundStyle(Color.cdSecondary)
            HStack(alignment: .firstTextBaseline, spacing: Tokens.Space.sp2) {
                Text(s.remaining(unit).formatted(.number))
                    .textStyle(Tokens.TypeScale.display(deviceClass))
                    .monospacedDigit()
                    .contentTransition(.numericText())
                    .lineLimit(1).minimumScaleFactor(0.5)
                    .shadow(color: Color.cdLight.opacity(Tokens.Effect.glowStrength), radius: Tokens.Space.sp4)
                Text(unit.label).textStyle(Tokens.TypeScale.title2).foregroundStyle(Color.cdSecondary)
            }
            .animation(.snappy, value: unit)
            UnitSegments(selection: $unit)
        }
        .background(alignment: .topLeading) {
            Circle().fill(RadialGradient(colors: [Color.cdLight.opacity(Tokens.Effect.glowStrength * 0.9), .clear], center: .center, startRadius: 0, endRadius: Tokens.Space.sp10 * 3.5))
                .frame(width: Tokens.Space.sp10 * 7, height: Tokens.Space.sp10 * 7)
                .offset(x: -Tokens.Space.sp10 * 2, y: -Tokens.Space.sp10 * 3.5)
                .allowsHitTesting(false)
        }
        .clipShape(RoundedRectangle(cornerRadius: Tokens.Radius.lg, style: .continuous))
    }

    private func words(_ q: Quote) -> some View {
        Button { withAnimation(.easeInOut) { model.nextQuote() } } label: {
            GlassCard {
                Text(L10n.words.uppercased()).textStyle(Tokens.TypeScale.caption1).fontWeight(.bold).tracking(1).foregroundStyle(Color.cdOlive)
                Text(QuoteText.primary(q, language: model.quoteLanguage))
                    .textStyle(Tokens.TypeScale.headline.serifVariant)
                    .lineSpacing(Tokens.Space.sp1)
                    .fixedSize(horizontal: false, vertical: true)
                if let second = QuoteText.secondary(q, language: model.quoteLanguage) {
                    Text(second).textStyle(Tokens.TypeScale.footnote.serifVariant).italic().foregroundStyle(Color.cdSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Text(L10n.wordsNext).textStyle(Tokens.TypeScale.caption2).fontWeight(.regular).foregroundStyle(Color.cdSecondary)
            }
        }
        .buttonStyle(.plain)
        .id(q.number)
        .transition(.opacity)
    }

    private func path(_ s: LifeSnapshot) -> some View {
        GlassCard {
            HStack(alignment: .firstTextBaseline) {
                Text(L10n.path).textStyle(Tokens.TypeScale.title3)
                Spacer()
                Text(s.progress.formatted(.percent.precision(.fractionLength(1)))).textStyle(Tokens.TypeScale.title3).foregroundStyle(Color.cdOlive).monospacedDigit()
            }
            ProgressBar(value: s.progress, height: Tokens.Stroke.barThick, glowing: true)
            HStack {
                Text(L10n.pathAge("\(s.age)", s.season.label))
                Spacer()
                Text(L10n.pathExpected(s.expectancy.formatted(.number.precision(.fractionLength(1)))))
            }
            .textStyle(Tokens.TypeScale.caption1).foregroundStyle(Color.cdSecondary)
        }
    }

    private func flow(_ s: LifeSnapshot) -> some View {
        GlassCard {
            Text(L10n.flow).textStyle(Tokens.TypeScale.title3)
            ForEach(LifePeriod.allCases) { period in
                let p = s.period(period)
                VStack(alignment: .leading, spacing: Tokens.Space.sp2) {
                    HStack {
                        Text(period.label).textStyle(Tokens.TypeScale.subhead)
                        Spacer()
                        Text("\(p.progress.formatted(.percent.precision(.fractionLength(0)))) · \(p.remainingText)")
                            .textStyle(Tokens.TypeScale.caption1).foregroundStyle(Color.cdSecondary).monospacedDigit()
                    }
                    ProgressBar(value: p.progress)
                }
                .padding(.top, Tokens.Space.sp1)
            }
        }
    }

    private func calendar(_ s: LifeSnapshot) -> some View {
        GlassCard {
            HStack {
                Text(L10n.calendar).textStyle(Tokens.TypeScale.title3)
                Spacer()
                Menu {
                    Picker(L10n.calendar, selection: $grid) {
                        ForEach(GridScale.allCases) { Text($0.label).tag($0) }
                    }
                } label: {
                    HStack(spacing: Tokens.Space.sp1) {
                        Text(grid.label)
                        Image(systemName: "chevron.down").imageScale(.small)
                    }
                    .textStyle(Tokens.TypeScale.subhead).fontWeight(.semibold).foregroundStyle(Color.cdOlive)
                    .frame(minHeight: Tokens.Layout.tapTarget)
                }
            }
            LifeGrid(total: s.total(grid.unit), filled: s.lived(grid.unit), columns: grid.columns)
            Text(L10n.calendarLegend(s.season.label)).textStyle(Tokens.TypeScale.caption1).foregroundStyle(Color.cdSecondary)
        }
    }
}

struct UnitSegments: View {
    @Binding var selection: LifeUnit

    var body: some View {
        HStack(spacing: 0) {
            ForEach(LifeUnit.allCases) { unit in
                let on = unit == selection
                Button { selection = unit } label: {
                    Text(unit.label)
                        .textStyle(Tokens.TypeScale.subhead)
                        .fontWeight(on ? .bold : .medium)
                        .foregroundStyle(on ? Color.cdOnOlive : Color.cdForeground)
                        .frame(maxWidth: .infinity, minHeight: Tokens.Layout.tapTarget - Tokens.Space.sp2)
                        .background(Capsule().fill(on ? Color.cdOlive : .clear))
                        .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(on ? .isSelected : [])
            }
        }
        .padding(Tokens.Space.sp1 / 2)
        .background(Capsule().fill(Color.cdDim))
        .padding(.top, Tokens.Space.sp1)
    }
}
