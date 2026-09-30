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
