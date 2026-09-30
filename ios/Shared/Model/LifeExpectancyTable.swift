import Foundation

struct CountryLife: Identifiable, Equatable {
    let code: String
    let nameKo: String
    let nameEn: String
    let total: Double
    let male: Double
    let female: Double

    var id: String { code }

    /// 앱이 한국어로 실행 중이면 한글 이름.
    var name: String { AppLanguage.isKorean ? nameKo : nameEn }

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
            return CountryLife(code: code, nameKo: r["ko"] ?? code, nameEn: r["en"] ?? code, total: t, male: m, female: f)
        }
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
