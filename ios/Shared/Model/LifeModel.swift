import Foundation

enum Sex: String, CaseIterable, Codable, Identifiable {
    case other, male, female

    var id: String { rawValue }

    var label: String {
        switch self {
        case .other: L10n.sexOther
        case .male: L10n.sexMale
        case .female: L10n.sexFemale
        }
    }
}

struct LifeProfile: Codable, Equatable {
    var birthDate: Date
    var countryCode: String
    var sex: Sex
    /// nil이면 나라·성별 평균(자동), 값이 있으면 사용자가 직접 넣은 기대수명.
    var customExpectancy: Double?

    func expectancy(in table: LifeExpectancyTable = .shared) -> Double {
        customExpectancy ?? table.expectancy(country: countryCode, sex: sex)
    }

    static func makeDefault(now: Date = .now, calendar: Calendar = LifeCalendar.current, locale: Locale = .current) -> LifeProfile {
        let birth = calendar.date(byAdding: .year, value: -30, to: calendar.startOfDay(for: now)) ?? now
        return LifeProfile(birthDate: birth, countryCode: LifeExpectancyTable.shared.defaultCountry(for: locale), sex: .other, customExpectancy: nil)
    }
}

enum LifeUnit: String, CaseIterable, Codable, Identifiable {
    case days, weeks, months, years

    var id: String { rawValue }

    var label: String {
        switch self {
        case .days: L10n.unitDays
        case .weeks: L10n.unitWeeks
        case .months: L10n.unitMonths
        case .years: L10n.unitYears
        }
    }
}

/// 인생 달력 한 칸의 크기.
enum GridScale: String, CaseIterable, Codable, Identifiable {
    case weeks, months, years

    var id: String { rawValue }

    var unit: LifeUnit {
        switch self {
        case .weeks: .weeks
        case .months: .months
        case .years: .years
        }
    }

    var columns: Int {
        switch self {
        case .weeks: Tokens.Grid.weeksColumns
        case .months: Tokens.Grid.monthsColumns
        case .years: Tokens.Grid.yearsColumns
        }
    }

    var label: String {
        switch self {
        case .weeks: L10n.calendarPerWeeks
        case .months: L10n.calendarPerMonths
        case .years: L10n.calendarPerYears
        }
    }
}

enum Season: Int, CaseIterable {
    case spring, summer, autumn, winter

    init(progress: Double) {
        self = Season(rawValue: min(3, max(0, Int(progress * 4)))) ?? .winter
    }

    var label: String {
        switch self {
        case .spring: L10n.seasonSpring
        case .summer: L10n.seasonSummer
        case .autumn: L10n.seasonAutumn
        case .winter: L10n.seasonWinter
        }
    }
}

enum LifeCalendar {
    /// 월요일 시작, 기기 시간대.
    static var current: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .current
        calendar.firstWeekday = 2
        return calendar
    }
}
