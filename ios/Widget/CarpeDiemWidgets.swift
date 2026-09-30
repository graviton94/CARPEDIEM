import AppIntents
import SwiftUI
import WidgetKit

@main
struct CarpeDiemWidgets: WidgetBundle {
    var body: some Widget {
        DaysLeftWidget()
        TodayWidget()
        LifeCalendarWidget()
    }
}

// MARK: - 타임라인

struct LifeEntry: TimelineEntry {
    let date: Date
    let profile: LifeProfile?
    let quote: Quote?
    var unit: LifeUnit = .days

    var snapshot: LifeSnapshot? { profile.map { LifeSnapshot(profile: $0, now: date) } }

    static var sample: LifeEntry {
        let birth = LifeCalendar.current.date(from: DateComponents(year: 1994, month: 6, day: 15)) ?? .now
        return LifeEntry(date: .now, profile: LifeProfile(birthDate: birth, countryCode: "KR", sex: .other, customExpectancy: nil),
                         quote: QuoteBook.shared.quotes.first)
    }
}

enum LifeTimeline {
    /// 지금부터 매 정시마다 24시간 분량. ‘오늘’ 위젯이 한 시간마다 바뀌고, 자정에 문장이 바뀐다.
    static func make(unit: LifeUnit, now: Date = .now) -> Timeline<LifeEntry> {
        let store = LifeStore.shared, profile = store.profile, calendar = LifeCalendar.current
        let hour = calendar.dateInterval(of: .hour, for: now)?.start ?? now
        var entries = [LifeEntry(date: now, profile: profile, quote: store.todaysQuote(on: now), unit: unit)]
        for h in 1...24 {
            guard let d = calendar.date(byAdding: .hour, value: h, to: hour) else { continue }
            entries.append(LifeEntry(date: d, profile: profile, quote: store.todaysQuote(on: d), unit: unit))
        }
        return Timeline(entries: entries, policy: .atEnd)
    }

    static func current(unit: LifeUnit = .days) -> LifeEntry {
        let store = LifeStore.shared
        guard store.profile != nil else { return .sample }
        return LifeEntry(date: .now, profile: store.profile, quote: store.todaysQuote(), unit: unit)
    }
}

struct StaticProvider: TimelineProvider {
    func placeholder(in context: Context) -> LifeEntry { .sample }
    func getSnapshot(in context: Context, completion: @escaping (LifeEntry) -> Void) { completion(LifeTimeline.current()) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<LifeEntry>) -> Void) { completion(LifeTimeline.make(unit: .days)) }
}

// MARK: - 단위 설정

extension LifeUnit: AppEnum {
    static var typeDisplayRepresentation: TypeDisplayRepresentation { TypeDisplayRepresentation(name: LocalizedStringResource("widget.unit")) }
    static var caseDisplayRepresentations: [LifeUnit: DisplayRepresentation] {
        [.days: DisplayRepresentation(title: LocalizedStringResource("unit.days")),
         .weeks: DisplayRepresentation(title: LocalizedStringResource("unit.weeks")),
         .months: DisplayRepresentation(title: LocalizedStringResource("unit.months")),
         .years: DisplayRepresentation(title: LocalizedStringResource("unit.years"))]
    }
}

struct UnitIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource { LocalizedStringResource("widget.unit") }
    static var description: IntentDescription { IntentDescription(LocalizedStringResource("widget.unit.desc")) }

    @Parameter(title: LocalizedStringResource("widget.unit"), default: .days)
    var unit: LifeUnit
}

struct DaysLeftProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> LifeEntry { .sample }
    func snapshot(for configuration: UnitIntent, in context: Context) async -> LifeEntry { LifeTimeline.current(unit: configuration.unit) }
    func timeline(for configuration: UnitIntent, in context: Context) async -> Timeline<LifeEntry> { LifeTimeline.make(unit: configuration.unit) }
}

// MARK: - 위젯 정의

struct DaysLeftWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "DaysLeft", intent: UnitIntent.self, provider: DaysLeftProvider()) { entry in
            DaysLeftView(entry: entry)
        }
        .configurationDisplayName(L10n.widgetDaysLeft)
        .description(L10n.widgetDaysLeftDesc)
        .supportedFamilies([.systemSmall, .accessoryCircular, .accessoryRectangular, .accessoryInline])
    }
}

struct TodayWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "Today", provider: StaticProvider()) { entry in
            TodayView(entry: entry)
        }
        .configurationDisplayName(L10n.widgetToday)
        .description(L10n.widgetTodayDesc)
        .supportedFamilies([.systemSmall])
    }
}

struct LifeCalendarWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "LifeCalendar", provider: StaticProvider()) { entry in
            LifeCalendarView(entry: entry)
        }
        .configurationDisplayName(L10n.calendar)
        .description(L10n.widgetCalendarDesc)
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}
