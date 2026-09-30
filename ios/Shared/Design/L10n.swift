// 자동 생성 파일 — 직접 고치지 말고 scripts/generate.py 를 실행하세요.

import Foundation

enum L10n {
    static var tagline: String { tr("tagline") }
    static var you: String { tr("you") }
    static var birthday: String { tr("birthday") }
    static var country: String { tr("country") }
    static var sex: String { tr("sex") }
    static var sexOther: String { tr("sex.other") }
    static var sexMale: String { tr("sex.male") }
    static var sexFemale: String { tr("sex.female") }
    static var lifeExpectancy: String { tr("lifeExpectancy") }
    static var auto: String { tr("auto") }
    static var custom: String { tr("custom") }
    static var lifeExpectancyFooter: String { tr("lifeExpectancy.footer") }
    static var begin: String { tr("begin") }
    static var privacy: String { tr("privacy") }
    static var timeLeft: String { tr("timeLeft") }
    static var unitDays: String { tr("unit.days") }
    static var unitWeeks: String { tr("unit.weeks") }
    static var unitMonths: String { tr("unit.months") }
    static var unitYears: String { tr("unit.years") }
    static var words: String { tr("words") }
    static var wordsNext: String { tr("words.next") }
    static var path: String { tr("path") }
    static func pathAge(_ age: String, _ season: String) -> String { tr("path.age", age, season) }
    static func pathExpected(_ years: String) -> String { tr("path.expected", years) }
    static var flow: String { tr("flow") }
    static var flowToday: String { tr("flow.today") }
    static var flowWeek: String { tr("flow.week") }
    static var flowMonth: String { tr("flow.month") }
    static var flowYear: String { tr("flow.year") }
    static func leftHours(_ n: String) -> String { tr("left.hours", n) }
    static func leftMinutes(_ n: String) -> String { tr("left.minutes", n) }
    static func leftDays(_ n: String) -> String { tr("left.days", n) }
    static var lastDay: String { tr("lastDay") }
    static var calendar: String { tr("calendar") }
    static var calendarPerWeeks: String { tr("calendar.per.weeks") }
    static var calendarPerMonths: String { tr("calendar.per.months") }
    static var calendarPerYears: String { tr("calendar.per.years") }
    static func calendarLegend(_ season: String) -> String { tr("calendar.legend", season) }
    static var seasonSpring: String { tr("season.spring") }
    static var seasonSummer: String { tr("season.summer") }
    static var seasonAutumn: String { tr("season.autumn") }
    static var seasonWinter: String { tr("season.winter") }
    static var settings: String { tr("settings") }
    static var cancel: String { tr("cancel") }
    static var done: String { tr("done") }
    static var wordsLanguage: String { tr("words.language") }
    static var wordsKorean: String { tr("words.korean") }
    static var wordsEnglish: String { tr("words.english") }
    static var wordsBoth: String { tr("words.both") }
    static var widgets: String { tr("widgets") }
    static var widgetsHelp1: String { tr("widgets.help1") }
    static var widgetsHelp2: String { tr("widgets.help2") }
    static var widgetsHelp3: String { tr("widgets.help3") }
    static var widgetsHelp4: String { tr("widgets.help4") }
    static var erase: String { tr("erase") }
    static var countrySearch: String { tr("country.search") }
    static var countrySource: String { tr("country.source") }
    static var widgetDaysLeft: String { tr("widget.daysLeft") }
    static var widgetToday: String { tr("widget.today") }
    static func widgetTodayLeft(_ n: String) -> String { tr("widget.todayLeft", n) }
    static var widgetTodaySub: String { tr("widget.todaySub") }
    static var widgetYearsLeft: String { tr("widget.yearsLeft") }
    static func widgetMonthsLeft(_ n: String) -> String { tr("widget.monthsLeft", n) }
    static func lockInline(_ n: String) -> String { tr("lock.inline", n) }
    static func lockRectSub(_ n: String) -> String { tr("lock.rect.sub", n) }
    static var androidNotification: String { tr("android.notification") }
    static var widgetUnit: String { tr("widget.unit") }
    static var widgetUnitDesc: String { tr("widget.unit.desc") }
    static var widgetDaysLeftDesc: String { tr("widget.daysLeft.desc") }
    static var widgetTodayDesc: String { tr("widget.today.desc") }
    static var widgetCalendarDesc: String { tr("widget.calendar.desc") }
    static var widgetEmpty: String { tr("widget.empty") }
    static var eraseConfirm: String { tr("erase.confirm") }
    static var eraseAction: String { tr("erase.action") }
    static func expectancyValue(_ years: String) -> String { tr("expectancy.value", years) }
    static var countryWorld: String { tr("country.world") }

    static let allKeys: [String] = ["tagline", "you", "birthday", "country", "sex", "sex.other", "sex.male", "sex.female", "lifeExpectancy", "auto", "custom", "lifeExpectancy.footer", "begin", "privacy", "timeLeft", "unit.days", "unit.weeks", "unit.months", "unit.years", "words", "words.next", "path", "path.age", "path.expected", "flow", "flow.today", "flow.week", "flow.month", "flow.year", "left.hours", "left.minutes", "left.days", "lastDay", "calendar", "calendar.per.weeks", "calendar.per.months", "calendar.per.years", "calendar.legend", "season.spring", "season.summer", "season.autumn", "season.winter", "settings", "cancel", "done", "words.language", "words.korean", "words.english", "words.both", "widgets", "widgets.help1", "widgets.help2", "widgets.help3", "widgets.help4", "erase", "country.search", "country.source", "widget.daysLeft", "widget.today", "widget.todayLeft", "widget.todaySub", "widget.yearsLeft", "widget.monthsLeft", "lock.inline", "lock.rect.sub", "android.notification", "widget.unit", "widget.unit.desc", "widget.daysLeft.desc", "widget.today.desc", "widget.calendar.desc", "widget.empty", "erase.confirm", "erase.action", "expectancy.value", "country.world"]

    private static func tr(_ key: String, _ args: String...) -> String {
        let format = Bundle.main.localizedString(forKey: key, value: nil, table: nil)
        return args.isEmpty ? format : String(format: format, arguments: args.map { $0 as NSString })
    }
}
