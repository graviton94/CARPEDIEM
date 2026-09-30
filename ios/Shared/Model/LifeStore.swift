import Foundation

/// 앱과 위젯이 함께 읽는 저장소 (App Group UserDefaults).
struct LifeStore {
    static let appGroupID = (Bundle.main.object(forInfoDictionaryKey: "AppGroupID") as? String) ?? ""
    static let shared = LifeStore(defaults: {
        guard !appGroupID.isEmpty, let d = UserDefaults(suiteName: appGroupID) else { return .standard }
        return d
    }())

    private enum Key {
        static let profile = "profile.v1"
        static let quoteSeed = "quote.seed"
        static let quoteStart = "quote.startDay"
        static let quoteSkipDay = "quote.skipDay"
        static let quoteSkip = "quote.skip"
        static let quoteLanguage = "quote.language"
    }

    let defaults: UserDefaults

    var profile: LifeProfile? {
        get { defaults.data(forKey: Key.profile).flatMap { try? JSONDecoder().decode(LifeProfile.self, from: $0) } }
        nonmutating set {
            if let newValue, let data = try? JSONEncoder().encode(newValue) { defaults.set(data, forKey: Key.profile) }
            else { defaults.removeObject(forKey: Key.profile) }
        }
    }

    var quoteLanguage: QuoteLanguage {
        get { defaults.string(forKey: Key.quoteLanguage).flatMap(QuoteLanguage.init(rawValue:)) ?? .default }
        nonmutating set { defaults.set(newValue.rawValue, forKey: Key.quoteLanguage) }
    }

    /// 처음 부를 때 무작위 seed와 시작일을 만든다. 위젯에서는 만들지 않고 읽기만 한다.
    func ensureQuoteSeed(today: Date = .now) {
        guard defaults.object(forKey: Key.quoteSeed) == nil else { return }
        defaults.set(String(UInt64.random(in: .min ... .max)), forKey: Key.quoteSeed)
        defaults.set(Self.dayNumber(today), forKey: Key.quoteStart)
    }

    /// 오늘의 문장. 앱에서 ‘다음 문장’을 누른 횟수는 그날에만 적용된다.
    func todaysQuote(on date: Date = .now, book: QuoteBook = .shared) -> Quote? {
        let seed = UInt64(defaults.string(forKey: Key.quoteSeed) ?? "") ?? 0x5EED
        let start = defaults.object(forKey: Key.quoteStart) as? Int ?? Self.dayNumber(date)
        let today = Self.dayNumber(date)
        let skip = defaults.integer(forKey: Key.quoteSkipDay) == today ? defaults.integer(forKey: Key.quoteSkip) : 0
        return book.quote(day: today - start, seed: seed, offset: skip)
    }

    func skipQuote(on date: Date = .now) {
        let today = Self.dayNumber(date)
        let current = defaults.integer(forKey: Key.quoteSkipDay) == today ? defaults.integer(forKey: Key.quoteSkip) : 0
        defaults.set(today, forKey: Key.quoteSkipDay)
        defaults.set(current + 1, forKey: Key.quoteSkip)
    }

    func eraseAll() {
        [Key.profile, Key.quoteSeed, Key.quoteStart, Key.quoteSkipDay, Key.quoteSkip, Key.quoteLanguage].forEach(defaults.removeObject(forKey:))
    }

    /// 기기 시간대 기준 날짜 번호 (자정에 바뀜).
    static func dayNumber(_ date: Date, calendar: Calendar = LifeCalendar.current) -> Int {
        let start = calendar.startOfDay(for: date)
        return calendar.dateComponents([.day], from: Date(timeIntervalSinceReferenceDate: 0), to: start).day ?? 0
    }
}
