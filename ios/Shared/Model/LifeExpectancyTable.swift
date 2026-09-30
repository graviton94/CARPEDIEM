import Foundation

struct CountryLife: Identifiable, Equatable {
    let code: String
    /// 원자료의 영문 이름. 화면에는 `name`을 쓴다.
    let sourceName: String
    let total: Double
    let male: Double
    let female: Double

    var id: String { code }

    /// 앱 언어로 된 나라 이름 (iOS가 제공). 세계 평균은 앱 문구.
    var name: String {
        if code == LifeExpectancyTable.worldCode { return L10n.countryWorld }
        return AppLanguage.locale.localizedString(forRegionCode: code) ?? sourceName
    }

    func matches(_ query: String) -> Bool {
        [name, sourceName, code].contains { $0.localizedCaseInsensitiveContains(query) }
    }

    func expectancy(for sex: Sex) -> Double {
        switch sex {
        case .other: total
        case .male: male
        case .female: female
        }
    }
}

/// 나라별 0세 기대수명 (data/life-expectancy.csv).
struct LifeExpectancyTable {
    static let worldCode = "WLD"
    static let shared = LifeExpectancyTable(csv: CSV.bundled("life-expectancy"))

    let countries: [CountryLife]

    init(csv: String) {
        countries = CSV.records(csv).compactMap { r in
            guard let code = r["code"], let t = Double(r["total"] ?? ""), let m = Double(r["male"] ?? ""), let f = Double(r["female"] ?? "") else { return nil }
            return CountryLife(code: code, sourceName: r["en"] ?? code, total: t, male: m, female: f)
        }
    }

    /// 세계 평균을 맨 위에, 나머지는 앱 언어의 이름순.
    var sortedForDisplay: [CountryLife] {
        let world = countries.filter { $0.code == Self.worldCode }
        let rest = countries.filter { $0.code != Self.worldCode }.sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
        return world + rest
    }

    func country(_ code: String) -> CountryLife? { countries.first { $0.code == code } }

    /// 표에 없는 나라는 세계 평균을 쓴다.
    func expectancy(country code: String, sex: Sex) -> Double {
        (country(code) ?? country(Self.worldCode))?.expectancy(for: sex) ?? 73.0
    }

    /// 기기 지역이 표에 있으면 그 나라, 없으면 세계 평균.
    func defaultCountry(for locale: Locale) -> String {
        guard let region = locale.region?.identifier, country(region) != nil else { return Self.worldCode }
        return region
    }
}
