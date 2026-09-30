import UIKit
import XCTest
@testable import CarpeDiem

final class DataTests: XCTestCase {
    func testCSVHandlesQuotesCommasAndComments() {
        let rows = CSV.rows("# 주석\na,b\n\"x, y\",\"그가 \"\"안녕\"\" 했다\"\n")
        XCTAssertEqual(rows, [["a", "b"], ["x, y", "그가 \"안녕\" 했다"]])
    }

    func testBundledQuotesAreComplete() {
        let book = QuoteBook.shared
        XCTAssertEqual(book.quotes.count, 100)
        XCTAssertEqual(book.quotes.map(\.number), Array(1...100))
        XCTAssertTrue(book.quotes.allSatisfy { !$0.korean.isEmpty && !$0.english.isEmpty })
    }

    func testQuotesDoNotRepeatWithinACycle() {
        let book = QuoteBook.shared
        let seen = Set((0..<100).map { book.index(day: $0, seed: 42) })
        XCTAssertEqual(seen.count, 100)
    }

    func testQuoteOrderIsStableForSameSeed() {
        let book = QuoteBook.shared
        XCTAssertEqual((0..<30).map { book.index(day: $0, seed: 7) }, (0..<30).map { book.index(day: $0, seed: 7) })
        XCTAssertNotEqual((0..<30).map { book.index(day: $0, seed: 7) }, (0..<30).map { book.index(day: $0, seed: 8) })
    }

    func testNewCycleDoesNotRepeatYesterday() {
        let book = QuoteBook.shared
        for seed in UInt64(0)..<200 {
            XCTAssertNotEqual(book.index(day: 99, seed: seed), book.index(day: 100, seed: seed), "seed \(seed)")
        }
    }

    func testSkipShowsTheNextQuoteOnlyToday() {
        let defaults = UserDefaults(suiteName: "test.\(UUID().uuidString)")!
        let store = LifeStore(defaults: defaults)
        let today = Date(timeIntervalSince1970: 1_800_000_000)
        store.ensureQuoteSeed(today: today)
        let first = store.todaysQuote(on: today)
        store.skipQuote(on: today)
        XCTAssertNotEqual(store.todaysQuote(on: today), first)
        let tomorrow = today.addingTimeInterval(86_400)
        XCTAssertEqual(store.todaysQuote(on: tomorrow), QuoteBook.shared.quote(day: 1, seed: UInt64(defaults.string(forKey: "quote.seed")!)!))
    }

    func testLifeExpectancyTable() {
        let table = LifeExpectancyTable.shared
        XCTAssertNotNil(table.country("KR"))
        XCTAssertNotNil(table.country(LifeExpectancyTable.worldCode))
        XCTAssertGreaterThan(table.countries.count, 10)
        XCTAssertTrue(table.countries.allSatisfy { (40...100).contains($0.total) && $0.female >= $0.male - 5 })
        XCTAssertEqual(table.sortedForDisplay.first?.code, LifeExpectancyTable.worldCode)
        XCTAssertFalse(table.country("KR")!.name.isEmpty)
        XCTAssertEqual(table.expectancy(country: "ZZ", sex: .other), table.country("WLD")!.total)
        XCTAssertEqual(table.defaultCountry(for: Locale(identifier: "ko_KR")), "KR")
        XCTAssertEqual(table.defaultCountry(for: Locale(identifier: "xx_ZZ")), "WLD")
    }

    func testCustomExpectancyWins() {
        var p = LifeProfile(birthDate: .now, countryCode: "KR", sex: .male, customExpectancy: nil)
        XCTAssertEqual(p.expectancy(), LifeExpectancyTable.shared.country("KR")!.male)
        p.customExpectancy = 95
        XCTAssertEqual(p.expectancy(), 95)
    }

    func testEveryStringIsLocalized() {
        for lang in ["ko", "en"] {
            let path = Bundle.main.path(forResource: lang, ofType: "lproj")!
            let bundle = Bundle(path: path)!
            for key in L10n.allKeys {
                XCTAssertNotEqual(bundle.localizedString(forKey: key, value: "∅", table: nil), "∅", "\(lang): \(key)")
            }
        }
    }

    func testFontsAreRegistered() {
        for name in ["Lora-Medium", "Lora-SemiBold", "NotoSerifKR-Medium", "NotoSerifKR-SemiBold"] {
            XCTAssertNotNil(UIFont(name: name, size: 17), name)
        }
    }
}
