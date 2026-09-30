import Foundation

/// 특정 시각 기준으로 계산한 인생의 흐름.
struct LifeSnapshot {
    let now: Date
    let calendar: Calendar
    let birth: Date
    let end: Date
    let expectancy: Double

    init(profile: LifeProfile, now: Date = .now, calendar: Calendar = LifeCalendar.current, table: LifeExpectancyTable = .shared) {
        self.init(birthDate: profile.birthDate, expectancy: profile.expectancy(in: table), now: now, calendar: calendar)
    }

    init(birthDate: Date, expectancy: Double, now: Date = .now, calendar: Calendar = LifeCalendar.current) {
        self.now = now
        self.calendar = calendar
        self.expectancy = expectancy
        self.birth = calendar.startOfDay(for: birthDate)
        self.end = LifeSnapshot.endDate(birth: birth, expectancy: expectancy, calendar: calendar)
    }

    /// 출생일 + 기대수명. 소수 부분은 평균 태양년(365.2425일)으로 환산한다.
    static func endDate(birth: Date, expectancy: Double, calendar: Calendar) -> Date {
        let years = max(0, expectancy)
        let whole = Int(years.rounded(.down))
        let extraDays = Int(((years - Double(whole)) * 365.2425).rounded())
        let base = calendar.date(byAdding: .year, value: whole, to: birth) ?? birth
        return calendar.date(byAdding: .day, value: extraDays, to: base) ?? base
    }

    /// 지나온 비율 0...1.
    var progress: Double {
        let total = end.timeIntervalSince(birth)
        guard total > 0 else { return 1 }
        return min(max(now.timeIntervalSince(birth) / total, 0), 1)
    }

    var season: Season { Season(progress: progress) }
    var isBeyondExpectancy: Bool { now >= end }
    var age: Int { lived(.years) }

    func lived(_ unit: LifeUnit) -> Int { count(unit, from: birth, to: now) }
    func remaining(_ unit: LifeUnit) -> Int { count(unit, from: now, to: end) }
    func total(_ unit: LifeUnit) -> Int { count(unit, from: birth, to: end) }

    /// 두 시각 사이에 온전히 지나간 단위 수 (날짜 경계 기준).
    func count(_ unit: LifeUnit, from: Date, to: Date) -> Int {
        guard to > from else { return 0 }
        let start = calendar.startOfDay(for: from)
        let finish = calendar.startOfDay(for: to)
        let value: Int?
        switch unit {
        case .days: value = calendar.dateComponents([.day], from: start, to: finish).day
        case .weeks: value = calendar.dateComponents([.day], from: start, to: finish).day.map { $0 / 7 }
        case .months: value = calendar.dateComponents([.month], from: start, to: finish).month
        case .years: value = calendar.dateComponents([.year], from: start, to: finish).year
        }
        return max(0, value ?? 0)
    }

    func period(_ period: LifePeriod) -> PeriodProgress {
        let interval = calendar.dateInterval(of: period.component, for: now)
            ?? DateInterval(start: calendar.startOfDay(for: now), duration: 86_400)
        return PeriodProgress(period: period, interval: interval, now: now, calendar: calendar)
    }
}

/// 오늘 · 이번 주 · 이번 달 · 올해.
enum LifePeriod: String, CaseIterable, Identifiable {
    case day, week, month, year

    var id: String { rawValue }

    var label: String {
        switch self {
        case .day: L10n.flowToday
        case .week: L10n.flowWeek
        case .month: L10n.flowMonth
        case .year: L10n.flowYear
        }
    }

    var component: Calendar.Component {
        switch self {
        case .day: .day
        case .week: .weekOfYear
        case .month: .month
        case .year: .year
        }
    }
}

struct PeriodProgress {
    let period: LifePeriod
    let interval: DateInterval
    let now: Date
    let calendar: Calendar

    var progress: Double {
        guard interval.duration > 0 else { return 0 }
        return min(max(now.timeIntervalSince(interval.start) / interval.duration, 0), 1)
    }

    var secondsLeft: TimeInterval { max(0, interval.end.timeIntervalSince(now)) }
    var hoursLeft: Int { Int(secondsLeft / 3600) }

    /// 오늘 이후로 이 기간에 남은 날 수 (오늘 제외).
    var remainingDaysAfterToday: Int {
        let today = calendar.startOfDay(for: now)
        let days = calendar.dateComponents([.day], from: today, to: interval.end).day ?? 0
        return max(0, days - 1)
    }

    var remainingText: String {
        switch period {
        case .day:
            return hoursLeft >= 1 ? L10n.leftHours("\(hoursLeft)") : L10n.leftMinutes("\(Int(secondsLeft / 60))")
        default:
            let days = remainingDaysAfterToday
            return days == 0 ? L10n.lastDay : L10n.leftDays("\(days)")
        }
    }
}
