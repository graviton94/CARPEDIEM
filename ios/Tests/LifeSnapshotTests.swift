import XCTest
@testable import CarpeDiem

final class LifeSnapshotTests: XCTestCase {
    private let calendar: Calendar = {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "UTC")!
        c.firstWeekday = 2
        return c
    }()

    private func date(_ y: Int, _ m: Int, _ d: Int, _ h: Int = 0) -> Date {
        calendar.date(from: DateComponents(year: y, month: m, day: d, hour: h))!
    }

    private func snap(_ birth: Date, _ expectancy: Double, now: Date) -> LifeSnapshot {
        LifeSnapshot(birthDate: birth, expectancy: expectancy, now: now, calendar: calendar)
    }

    func testEndDateWholeYears() {
        XCTAssertEqual(snap(date(2000, 1, 1), 80, now: date(2020, 1, 1)).end, date(2080, 1, 1))
    }

    func testEndDateFractionalYears() {
        // 0.5년 = 182.6일 → 183일
        XCTAssertEqual(snap(date(2000, 1, 1), 80.5, now: date(2020, 1, 1)).end, date(2080, 7, 2))
    }

    func testLivedAndRemaining() {
        let s = snap(date(2000, 1, 1), 1, now: date(2000, 1, 11, 12))
        XCTAssertEqual(s.lived(.days), 10)
        XCTAssertEqual(s.lived(.weeks), 1)
        XCTAssertEqual(s.lived(.months), 0)
        XCTAssertEqual(s.total(.days), 366)
        XCTAssertEqual(s.remaining(.days), 356)
        XCTAssertEqual(s.total(.months), 12)
    }

    func testAgeTurnsOnBirthday() {
        XCTAssertEqual(snap(date(1990, 10, 1), 83.5, now: date(2026, 9, 30)).age, 35)
        XCTAssertEqual(snap(date(1990, 10, 1), 83.5, now: date(2026, 10, 1)).age, 36)
    }

    func testBeyondExpectancy() {
        let s = snap(date(1900, 1, 1), 80, now: date(2026, 1, 1))
        XCTAssertEqual(s.progress, 1)
        XCTAssertTrue(s.isBeyondExpectancy)
        XCTAssertEqual(s.remaining(.days), 0)
        XCTAssertEqual(s.season, .winter)
    }

    func testBeforeBirth() {
        let s = snap(date(2030, 1, 1), 80, now: date(2026, 1, 1))
        XCTAssertEqual(s.progress, 0)
        XCTAssertEqual(s.lived(.days), 0)
        XCTAssertEqual(s.season, .spring)
    }

    func testSeasons() {
        XCTAssertEqual(Season(progress: 0.24), .spring)
        XCTAssertEqual(Season(progress: 0.25), .summer)
        XCTAssertEqual(Season(progress: 0.6), .autumn)
        XCTAssertEqual(Season(progress: 0.99), .winter)
    }

    func testPeriodProgress() {
        let s = snap(date(1990, 1, 1), 80, now: date(2026, 7, 2, 12))
        XCTAssertEqual(s.period(.day).progress, 0.5, accuracy: 0.0001)
        XCTAssertEqual(s.period(.year).progress, 0.5, accuracy: 0.0001) // 182.5 / 365
        XCTAssertEqual(s.period(.day).hoursLeft, 12)
    }

    func testPeriodRemainingDays() {
        // 2026-09-30 (수)
        let s = snap(date(1990, 1, 1), 80, now: date(2026, 9, 30, 9))
        XCTAssertEqual(s.period(.year).remainingDaysAfterToday, 92)
        XCTAssertEqual(s.period(.month).remainingDaysAfterToday, 0)
        XCTAssertEqual(s.period(.week).remainingDaysAfterToday, 4) // 월요일 시작 → 목~일
    }
}
