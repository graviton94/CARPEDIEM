import Foundation

struct Quote: Equatable {
    let number: Int
    let korean: String
    let english: String
}

enum QuoteLanguage: String, CaseIterable, Codable, Identifiable {
    case korean, english, both

    var id: String { rawValue }

    var label: String {
        switch self {
        case .korean: L10n.wordsKorean
        case .english: L10n.wordsEnglish
        case .both: L10n.wordsBoth
        }
    }

    static var `default`: QuoteLanguage { AppLanguage.isKorean ? .both : .english }
}

enum AppLanguage {
    static var isKorean: Bool { Bundle.main.preferredLocalizations.first?.hasPrefix("ko") ?? false }
    static var locale: Locale { Locale(identifier: Bundle.main.preferredLocalizations.first ?? "en") }
}

/// 오늘의 문장 고르기.
///
/// - 하루에 하나, 자정에 바뀐다.
/// - `seed`로 섞은 순서를 따라가므로 한 바퀴(명언 수만큼의 날) 동안 겹치지 않는다.
/// - 바퀴가 바뀔 때는 새 순서의 첫 문장이 바로 전날 문장과 같지 않게 한다.
/// - 같은 seed와 날짜면 앱과 위젯이 항상 같은 문장을 얻는다(저장 없이 계산만으로).
struct QuoteBook {
    static let shared = QuoteBook(csv: CSV.bundled("quotes"))

    let quotes: [Quote]

    init(csv: String) {
        quotes = CSV.records(csv).compactMap { r in
            guard let n = Int(r["No"] ?? ""), let ko = r["한글"], let en = r["영문"] else { return nil }
            return Quote(number: n, korean: ko, english: en)
        }
    }

    /// 하루 번호(시작일부터 며칠째)에 해당하는 명언 위치.
    func index(day: Int, seed: UInt64) -> Int {
        let n = quotes.count
        guard n > 0 else { return 0 }
        let d = max(0, day)
        return order(cycle: d / n, seed: seed)[d % n]
    }

    func quote(day: Int, seed: UInt64, offset: Int = 0) -> Quote? {
        guard !quotes.isEmpty else { return nil }
        return quotes[index(day: day + offset, seed: seed)]
    }

    func order(cycle: Int, seed: UInt64) -> [Int] {
        var result = shuffled(seed: seed &+ UInt64(cycle) &* 0x9E37_79B9_7F4A_7C15)
        if cycle > 0, let previousLast = order(cycle: cycle - 1, seed: seed).last, result.first == previousLast, result.count > 1 {
            result.swapAt(0, 1)
        }
        return result
    }

    private func shuffled(seed: UInt64) -> [Int] {
        var rng = SplitMix64(seed: seed)
        var a = Array(quotes.indices)
        if a.count > 1 {
            for i in stride(from: a.count - 1, to: 0, by: -1) {
                a.swapAt(i, Int(rng.next() % UInt64(i + 1)))
            }
        }
        return a
    }
}

struct SplitMix64: RandomNumberGenerator {
    private var state: UInt64
    init(seed: UInt64) { state = seed }
    mutating func next() -> UInt64 {
        state &+= 0x9E37_79B9_7F4A_7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return z ^ (z >> 31)
    }
}
