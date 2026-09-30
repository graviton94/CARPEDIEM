import SwiftUI
import WidgetKit

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

