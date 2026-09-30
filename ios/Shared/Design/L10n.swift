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
    static var widgetsAndroid1: String { tr("widgets.android1") }
    static var widgetsAndroid2: String { tr("widgets.android2") }
    static var widgetsAndroid3: String { tr("widgets.android3") }
    static var back: String { tr("back") }
    static var design: String { tr("design") }
    static var designGlass: String { tr("design.glass") }
    static var designGarden: String { tr("design.garden") }
    static var gardenMeetTitle: String { tr("garden.meet.title") }
    static var gardenMeetSub: String { tr("garden.meet.sub") }
    static var gardenMeetGo: String { tr("garden.meet.go") }
    static var gardenDrawing: String { tr("garden.drawing") }
    static var gardenHaru: String { tr("garden.haru") }
    static func gardenNo(_ id: String) -> String { tr("garden.no", id) }
    static var gardenAge0: String { tr("garden.age0") }
    static var gardenDown: String { tr("garden.down") }
    static var gardenClose: String { tr("garden.close") }
    static var gardenPreview: String { tr("garden.preview") }
    static func gardenItemDate(_ date: String, _ when: String) -> String { tr("garden.itemDate", date, when) }
    static var stoneBasalt: String { tr("stone.basalt") }
    static var stoneGranite: String { tr("stone.granite") }
    static var stonePinkgranite: String { tr("stone.pinkgranite") }
    static var stoneSand: String { tr("stone.sand") }
    static var stoneOchre: String { tr("stone.ochre") }
    static var stoneSpeckle: String { tr("stone.speckle") }
    static var stoneSlate: String { tr("stone.slate") }
    static var stoneGneiss: String { tr("stone.gneiss") }
    static var stoneJasper: String { tr("stone.jasper") }
    static var stoneSerpentine: String { tr("stone.serpentine") }
    static var stoneJade: String { tr("stone.jade") }
    static var stoneMarble: String { tr("stone.marble") }
    static var stoneQuartz: String { tr("stone.quartz") }
    static var stoneRing: String { tr("stone.ring") }
    static var objMoss: String { tr("obj.moss") }
    static var objMossWhen: String { tr("obj.moss.when") }
    static var objMossLine: String { tr("obj.moss.line") }
    static var objTeacup: String { tr("obj.teacup") }
    static var objTeacupWhen: String { tr("obj.teacup.when") }
    static var objTeacupLine: String { tr("obj.teacup.line") }
    static var objCairn: String { tr("obj.cairn") }
    static var objCairnWhen: String { tr("obj.cairn.when") }
    static var objCairnLine: String { tr("obj.cairn.line") }
    static var objPine: String { tr("obj.pine") }
    static var objPineWhen: String { tr("obj.pine.when") }
    static var objPineLine: String { tr("obj.pine.line") }
    static var objFlower: String { tr("obj.flower") }
    static var objFlowerWhen: String { tr("obj.flower.when") }
    static var objFlowerLine: String { tr("obj.flower.line") }
    static var objPond: String { tr("obj.pond") }
    static var objPondWhen: String { tr("obj.pond.when") }
    static var objPondLine: String { tr("obj.pond.line") }
    static var objLeaf: String { tr("obj.leaf") }
    static var objLeafWhen: String { tr("obj.leaf.when") }
    static var objLeafLine: String { tr("obj.leaf.line") }
    static var objCandle: String { tr("obj.candle") }
    static var objCandleWhen: String { tr("obj.candle.when") }
    static var objCandleLine: String { tr("obj.candle.line") }
    static var objDandelion: String { tr("obj.dandelion") }
    static var objDandelionWhen: String { tr("obj.dandelion.when") }
    static var objDandelionLine: String { tr("obj.dandelion.line") }
    static var objFeather: String { tr("obj.feather") }
    static var objFeatherWhen: String { tr("obj.feather.when") }
    static var objFeatherLine: String { tr("obj.feather.line") }
    static var objSnail: String { tr("obj.snail") }
    static var objSnailWhen: String { tr("obj.snail.when") }
    static var objSnailLine: String { tr("obj.snail.line") }
    static var objAcorn: String { tr("obj.acorn") }
    static var objAcornWhen: String { tr("obj.acorn.when") }
    static var objAcornLine: String { tr("obj.acorn.line") }
    static var notifyChannel: String { tr("notify.channel") }
    static func notifyKeepsake(_ name: String) -> String { tr("notify.keepsake", name) }
    static var notifyRow: String { tr("notify.row") }
    static var notifyFooter: String { tr("notify.footer") }
    static var devUnlocked: String { tr("dev.unlocked") }
    static func gardenHaruA11y(_ stone: String) -> String { tr("garden.haruA11y", stone) }
    static var collection: String { tr("collection") }
    static var collectionSub: String { tr("collection.sub") }
    static var support: String { tr("support") }
    static var supportTitle: String { tr("support.title") }
    static var supportBody: String { tr("support.body") }
    static var supportTier1: String { tr("support.tier1") }
    static var supportTier1Price: String { tr("support.tier1.price") }
    static var supportTier2: String { tr("support.tier2") }
    static var supportTier2Price: String { tr("support.tier2.price") }
    static var supportTier3: String { tr("support.tier3") }
    static var supportTier3Price: String { tr("support.tier3.price") }
    static var supportOnce: String { tr("support.once") }
    static var supportSoon: String { tr("support.soon") }
    static var letgoTitle: String { tr("letgo.title") }
    static var letgoSub: String { tr("letgo.sub") }
    static var letgoFeeling: String { tr("letgo.feeling") }
    static var letgoHint: String { tr("letgo.hint") }
    static var letgoSend: String { tr("letgo.send") }
    static var letgoDone: String { tr("letgo.done") }
    static var letgoPrivacy: String { tr("letgo.privacy") }
    static var feelingJoy: String { tr("feeling.joy") }
    static var feelingThanks: String { tr("feeling.thanks") }
    static var feelingCalm: String { tr("feeling.calm") }
    static var feelingSad: String { tr("feeling.sad") }
    static var feelingWorry: String { tr("feeling.worry") }
    static var feelingHope: String { tr("feeling.hope") }
    static var feelingDisappoint: String { tr("feeling.disappoint") }
    static var letgoMsgJoy1: String { tr("letgo.msg.joy.1") }
    static var letgoMsgJoy2: String { tr("letgo.msg.joy.2") }
    static var letgoMsgJoy3: String { tr("letgo.msg.joy.3") }
    static var letgoMsgHope1: String { tr("letgo.msg.hope.1") }
    static var letgoMsgHope2: String { tr("letgo.msg.hope.2") }
    static var letgoMsgHope3: String { tr("letgo.msg.hope.3") }
    static var letgoMsgCalm1: String { tr("letgo.msg.calm.1") }
    static var letgoMsgCalm2: String { tr("letgo.msg.calm.2") }
    static var letgoMsgCalm3: String { tr("letgo.msg.calm.3") }
    static var letgoMsgThanks1: String { tr("letgo.msg.thanks.1") }
    static var letgoMsgThanks2: String { tr("letgo.msg.thanks.2") }
    static var letgoMsgThanks3: String { tr("letgo.msg.thanks.3") }
    static var letgoMsgDisappoint1: String { tr("letgo.msg.disappoint.1") }
    static var letgoMsgDisappoint2: String { tr("letgo.msg.disappoint.2") }
    static var letgoMsgDisappoint3: String { tr("letgo.msg.disappoint.3") }
    static var letgoMsgSad1: String { tr("letgo.msg.sad.1") }
    static var letgoMsgSad2: String { tr("letgo.msg.sad.2") }
    static var letgoMsgSad3: String { tr("letgo.msg.sad.3") }
    static var letgoMsgWorry1: String { tr("letgo.msg.worry.1") }
    static var letgoMsgWorry2: String { tr("letgo.msg.worry.2") }
    static var letgoMsgWorry3: String { tr("letgo.msg.worry.3") }
    static var letgoMsgNone1: String { tr("letgo.msg.none.1") }
    static var letgoMsgNone2: String { tr("letgo.msg.none.2") }
    static func recallTitle(_ years: String) -> String { tr("recall.title", years) }
    static var recallOpen: String { tr("recall.open") }
    static var recallClose: String { tr("recall.close") }
    static func recallNotify(_ years: String) -> String { tr("recall.notify", years) }
    static var recallNotifyText: String { tr("recall.notifyText") }
    static var recallSample: String { tr("recall.sample") }
    static var recallAddSample: String { tr("recall.addSample") }
    static var lines: String { tr("lines") }
    static var linesKeep: String { tr("lines.keep") }
    static var linesKeepFooter: String { tr("lines.keepFooter") }
    static var linesExport: String { tr("lines.export") }
    static var linesExportEmpty: String { tr("lines.exportEmpty") }
    static var linesExportTitle: String { tr("lines.exportTitle") }
    static var linesClear: String { tr("lines.clear") }
    static var linesClearConfirm: String { tr("lines.clearConfirm") }
    static var linesClearAction: String { tr("lines.clearAction") }
    static func linesCount(_ count: String) -> String { tr("lines.count", count) }
    static var objPinwheel: String { tr("obj.pinwheel") }
    static var objPinwheelWhen: String { tr("obj.pinwheel.when") }
    static var objPinwheelLine: String { tr("obj.pinwheel.line") }
    static var objPaperboat: String { tr("obj.paperboat") }
    static var objPaperboatWhen: String { tr("obj.paperboat.when") }
    static var objPaperboatLine: String { tr("obj.paperboat.line") }
    static var objKite: String { tr("obj.kite") }
    static var objKiteWhen: String { tr("obj.kite.when") }
    static var objKiteLine: String { tr("obj.kite.line") }
    static var defaults: String { tr("defaults") }
    static var defaultsUnit: String { tr("defaults.unit") }
    static var defaultsGrid: String { tr("defaults.grid") }
    static var defaultsFooter: String { tr("defaults.footer") }
    static var widgetDaysLeftGlass: String { tr("widget.daysLeft.glass") }
    static var widgetDaysLeftGarden: String { tr("widget.daysLeft.garden") }
    static var widgetTodayGlass: String { tr("widget.today.glass") }
    static var widgetTodayGarden: String { tr("widget.today.garden") }
    static var widgetCalendarGlass: String { tr("widget.calendar.glass") }
    static var widgetCalendarGarden: String { tr("widget.calendar.garden") }
    static var letgoModalTitle: String { tr("letgo.modalTitle") }
    static var letgoOk: String { tr("letgo.ok") }
    static func recallRandom(_ date: String) -> String { tr("recall.random", date) }
    static var recallRandomNotify: String { tr("recall.randomNotify") }
    static var recallAddRandom: String { tr("recall.addRandom") }
    static var recallSampleOld: String { tr("recall.sampleOld") }

    static let allKeys: [String] = ["tagline", "you", "birthday", "country", "sex", "sex.other", "sex.male", "sex.female", "lifeExpectancy", "auto", "custom", "lifeExpectancy.footer", "begin", "privacy", "timeLeft", "unit.days", "unit.weeks", "unit.months", "unit.years", "words", "words.next", "path", "path.age", "path.expected", "flow", "flow.today", "flow.week", "flow.month", "flow.year", "left.hours", "left.minutes", "left.days", "lastDay", "calendar", "calendar.per.weeks", "calendar.per.months", "calendar.per.years", "calendar.legend", "season.spring", "season.summer", "season.autumn", "season.winter", "settings", "cancel", "done", "words.language", "words.korean", "words.english", "words.both", "widgets", "widgets.help1", "widgets.help2", "widgets.help3", "widgets.help4", "erase", "country.search", "country.source", "widget.daysLeft", "widget.today", "widget.todayLeft", "widget.todaySub", "widget.yearsLeft", "widget.monthsLeft", "lock.inline", "lock.rect.sub", "android.notification", "widget.unit", "widget.unit.desc", "widget.daysLeft.desc", "widget.today.desc", "widget.calendar.desc", "widget.empty", "erase.confirm", "erase.action", "expectancy.value", "country.world", "widgets.android1", "widgets.android2", "widgets.android3", "back", "design", "design.glass", "design.garden", "garden.meet.title", "garden.meet.sub", "garden.meet.go", "garden.drawing", "garden.haru", "garden.no", "garden.age0", "garden.down", "garden.close", "garden.preview", "garden.itemDate", "stone.basalt", "stone.granite", "stone.pinkgranite", "stone.sand", "stone.ochre", "stone.speckle", "stone.slate", "stone.gneiss", "stone.jasper", "stone.serpentine", "stone.jade", "stone.marble", "stone.quartz", "stone.ring", "obj.moss", "obj.moss.when", "obj.moss.line", "obj.teacup", "obj.teacup.when", "obj.teacup.line", "obj.cairn", "obj.cairn.when", "obj.cairn.line", "obj.pine", "obj.pine.when", "obj.pine.line", "obj.flower", "obj.flower.when", "obj.flower.line", "obj.pond", "obj.pond.when", "obj.pond.line", "obj.leaf", "obj.leaf.when", "obj.leaf.line", "obj.candle", "obj.candle.when", "obj.candle.line", "obj.dandelion", "obj.dandelion.when", "obj.dandelion.line", "obj.feather", "obj.feather.when", "obj.feather.line", "obj.snail", "obj.snail.when", "obj.snail.line", "obj.acorn", "obj.acorn.when", "obj.acorn.line", "notify.channel", "notify.keepsake", "notify.row", "notify.footer", "dev.unlocked", "garden.haruA11y", "collection", "collection.sub", "support", "support.title", "support.body", "support.tier1", "support.tier1.price", "support.tier2", "support.tier2.price", "support.tier3", "support.tier3.price", "support.once", "support.soon", "letgo.title", "letgo.sub", "letgo.feeling", "letgo.hint", "letgo.send", "letgo.done", "letgo.privacy", "feeling.joy", "feeling.thanks", "feeling.calm", "feeling.sad", "feeling.worry", "feeling.hope", "feeling.disappoint", "letgo.msg.joy.1", "letgo.msg.joy.2", "letgo.msg.joy.3", "letgo.msg.hope.1", "letgo.msg.hope.2", "letgo.msg.hope.3", "letgo.msg.calm.1", "letgo.msg.calm.2", "letgo.msg.calm.3", "letgo.msg.thanks.1", "letgo.msg.thanks.2", "letgo.msg.thanks.3", "letgo.msg.disappoint.1", "letgo.msg.disappoint.2", "letgo.msg.disappoint.3", "letgo.msg.sad.1", "letgo.msg.sad.2", "letgo.msg.sad.3", "letgo.msg.worry.1", "letgo.msg.worry.2", "letgo.msg.worry.3", "letgo.msg.none.1", "letgo.msg.none.2", "recall.title", "recall.open", "recall.close", "recall.notify", "recall.notifyText", "recall.sample", "recall.addSample", "lines", "lines.keep", "lines.keepFooter", "lines.export", "lines.exportEmpty", "lines.exportTitle", "lines.clear", "lines.clearConfirm", "lines.clearAction", "lines.count", "obj.pinwheel", "obj.pinwheel.when", "obj.pinwheel.line", "obj.paperboat", "obj.paperboat.when", "obj.paperboat.line", "obj.kite", "obj.kite.when", "obj.kite.line", "defaults", "defaults.unit", "defaults.grid", "defaults.footer", "widget.daysLeft.glass", "widget.daysLeft.garden", "widget.today.glass", "widget.today.garden", "widget.calendar.glass", "widget.calendar.garden", "letgo.modalTitle", "letgo.ok", "recall.random", "recall.randomNotify", "recall.addRandom", "recall.sampleOld"]

    private static func tr(_ key: String, _ args: String...) -> String {
        let format = Bundle.main.localizedString(forKey: key, value: nil, table: nil)
        return args.isEmpty ? format : String(format: format, arguments: args.map { $0 as NSString })
    }
}
