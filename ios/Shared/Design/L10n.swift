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
    static var timeLeftDays: String { tr("timeLeft.days") }
    static var timeLeftWeeks: String { tr("timeLeft.weeks") }
    static var timeLeftMonths: String { tr("timeLeft.months") }
    static var timeLeftYears: String { tr("timeLeft.years") }
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
    static var widgetsList: String { tr("widgets.list") }
    static var widgetsNameDaysLeft: String { tr("widgets.name.daysLeft") }
    static var widgetsNameToday: String { tr("widgets.name.today") }
    static var widgetsNameCalendar: String { tr("widgets.name.calendar") }
    static var widgetsNameFamily: String { tr("widgets.name.family") }
    static var widgetsNameRecord: String { tr("widgets.name.record") }
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
    static var gardenAge0: String { tr("garden.age0") }
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
    static var objBookmark: String { tr("obj.bookmark") }
    static var objBookmarkWhen: String { tr("obj.bookmark.when") }
    static var objBookmarkLine: String { tr("obj.bookmark.line") }
    static var objSnail: String { tr("obj.snail") }
    static var objSnailWhen: String { tr("obj.snail.when") }
    static var objSnailLine: String { tr("obj.snail.line") }
    static var objAcorn: String { tr("obj.acorn") }
    static var objAcornWhen: String { tr("obj.acorn.when") }
    static var objAcornLine: String { tr("obj.acorn.line") }
    static var notifyChannel: String { tr("notify.channel") }
    static var notifyChannelEvening: String { tr("notify.channelEvening") }
    static var notifyChannelDays: String { tr("notify.channelDays") }
    static var notifyMineBirthdayText: String { tr("notify.mineBirthdayText") }
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
    static func gardenMetOn(_ date: String, _ stone: String) -> String { tr("garden.metOn", date, stone) }
    static var family: String { tr("family") }
    static var familyAdd: String { tr("family.add") }
    static var familyFull: String { tr("family.full") }
    static var familyGlassNote: String { tr("family.glassNote") }
    static var familyMe: String { tr("family.me") }
    static var addKind: String { tr("add.kind") }
    static var kindPerson: String { tr("kind.person") }
    static var kindPet: String { tr("kind.pet") }
    static var speciesDog: String { tr("species.dog") }
    static var speciesCat: String { tr("species.cat") }
    static var speciesOther: String { tr("species.other") }
    static var addName: String { tr("add.name") }
    static var addNameHint: String { tr("add.nameHint") }
    static var addBirth: String { tr("add.birth") }
    static var addBirthPet: String { tr("add.birthPet") }
    static var addBirthUnknown: String { tr("add.birthUnknown") }
    static var addBirthPick: String { tr("add.birthPick") }
    static var addNext: String { tr("add.next") }
    static func addMeet(_ name: String) -> String { tr("add.meet", name) }
    static func addReroll(_ n: String) -> String { tr("add.reroll", n) }
    static var stoneTogether: String { tr("stone.together") }
    static var stoneSinceMet: String { tr("stone.sinceMet") }
    static func stoneDays(_ n: String) -> String { tr("stone.days", n) }
    static var stoneNextBirthday: String { tr("stone.nextBirthday") }
    static var stoneBirthdayToday: String { tr("stone.birthdayToday") }
    static func stoneCalendar(_ name: String) -> String { tr("stone.calendar", name) }
    static var stoneCalendarLegend: String { tr("stone.calendarLegend") }
    static var stoneShowAhead: String { tr("stone.showAhead") }
    static var stoneLines: String { tr("stone.lines") }
    static func stoneLinesCount(_ n: String) -> String { tr("stone.linesCount", n) }
    static var stoneEdit: String { tr("stone.edit") }
    static var stoneSave: String { tr("stone.save") }
    static var stoneRemove: String { tr("stone.remove") }
    static func stoneRemoveConfirm(_ name: String) -> String { tr("stone.removeConfirm", name) }
    static var stoneRemoveAction: String { tr("stone.removeAction") }
    static func stoneAge(_ age: String) -> String { tr("stone.age", age) }
    static func stonePetAge(_ age: String) -> String { tr("stone.petAge", age) }
    static var letgoTo: String { tr("letgo.to") }
    static func letgoModalTo(_ name: String) -> String { tr("letgo.modalTo", name) }
    static func gardenStoneA11y(_ name: String, _ stone: String) -> String { tr("garden.stoneA11y", name, stone) }
    static var gardenPet: String { tr("garden.pet") }
    static var widgetFamilyGarden: String { tr("widget.family.garden") }
    static var widgetFamilyDesc: String { tr("widget.family.desc") }
    static var breath: String { tr("breath") }
    static var breathKindCalm: String { tr("breath.kind.calm") }
    static var breathKindBox: String { tr("breath.kind.box") }
    static var breathKindSleep: String { tr("breath.kind.sleep") }
    static var breathKindDescCalm: String { tr("breath.kindDesc.calm") }
    static var breathKindDescBox: String { tr("breath.kindDesc.box") }
    static var breathKindDescSleep: String { tr("breath.kindDesc.sleep") }
    static func breathMinutes(_ n: String) -> String { tr("breath.minutes", n) }
    static var breathSound: String { tr("breath.sound") }
    static var soundNone: String { tr("sound.none") }
    static var soundWaves: String { tr("sound.waves") }
    static var soundWind: String { tr("sound.wind") }
    static var soundRain: String { tr("sound.rain") }
    static var soundTone: String { tr("sound.tone") }
    static var breathStart: String { tr("breath.start") }
    static var breathIn: String { tr("breath.in") }
    static var breathHold: String { tr("breath.hold") }
    static var breathOut: String { tr("breath.out") }
    static var breathRest: String { tr("breath.rest") }
    static var breathWalkIn: String { tr("breath.walk.in") }
    static var breathWalkOut: String { tr("breath.walk.out") }
    static var breathPause: String { tr("breath.pause") }
    static var breathKeep: String { tr("breath.keep") }
    static var breathStop: String { tr("breath.stop") }
    static func breathStartA11y(_ kind: String, _ n: String) -> String { tr("breath.startA11y", kind, n) }
    static var breathHome: String { tr("breath.home") }
    static var breathDoneCalm1: String { tr("breath.done.calm.1") }
    static var breathDoneCalm2: String { tr("breath.done.calm.2") }
    static var breathDoneCalm3: String { tr("breath.done.calm.3") }
    static var breathDoneBox1: String { tr("breath.done.box.1") }
    static var breathDoneBox2: String { tr("breath.done.box.2") }
    static var breathDoneBox3: String { tr("breath.done.box.3") }
    static var breathDoneSleep1: String { tr("breath.done.sleep.1") }
    static var breathDoneSleep2: String { tr("breath.done.sleep.2") }
    static var breathDoneSleep3: String { tr("breath.done.sleep.3") }
    static var breathNight: String { tr("breath.night") }
    static func breathAsk(_ name: String) -> String { tr("breath.ask", name) }
    static var breathAskHelp: String { tr("breath.askHelp") }
    static var breathAskGo: String { tr("breath.askGo") }
    static var breathAskStay: String { tr("breath.askStay") }
    static var breathBells: String { tr("breath.bells") }
    static var gaze: String { tr("gaze") }
    static var gazeSoundOn: String { tr("gaze.soundOn") }
    static var gazeSoundOff: String { tr("gaze.soundOff") }
    static var gazeExit: String { tr("gaze.exit") }
    static var objWindchime: String { tr("obj.windchime") }
    static var objWindchimeWhen: String { tr("obj.windchime.when") }
    static var objWindchimeLine: String { tr("obj.windchime.line") }
    static var addTogether: String { tr("add.together") }
    static var addTogetherAuto: String { tr("add.togetherAuto") }
    static var addTogetherHelp: String { tr("add.togetherHelp") }
    static var questionLabel: String { tr("question.label") }
    static var questionAnswer: String { tr("question.answer") }
    static var questionSkip: String { tr("question.skip") }
    static var questionAnswered: String { tr("question.answered") }
    static var moodTitle: String { tr("mood.title") }
    static func moodSub(_ month: String, _ name: String) -> String { tr("mood.sub", month, name) }
    static func moodA11y(_ month: String, _ n: String) -> String { tr("mood.a11y", month, n) }
    static func letterArrived(_ season: String) -> String { tr("letter.arrived", season) }
    static func letterTitle(_ year: String, _ season: String) -> String { tr("letter.title", year, season) }
    static func letterRange(_ from: String, _ until: String) -> String { tr("letter.range", from, until) }
    static func letterHeavy(_ n: String) -> String { tr("letter.heavy", n) }
    static var letterShowHeavy: String { tr("letter.showHeavy") }
    static var letterEnd: String { tr("letter.end") }
    static var letterFold: String { tr("letter.fold") }
    static var letters: String { tr("letters") }
    static var lettersSub: String { tr("letters.sub") }
    static func notifyLetter(_ season: String) -> String { tr("notify.letter", season) }
    static var notifyLetterText: String { tr("notify.letterText") }
    static var letterSample1: String { tr("letter.sample1") }
    static var letterSample2: String { tr("letter.sample2") }
    static var letterSample3: String { tr("letter.sample3") }
    static var letterSample4: String { tr("letter.sample4") }
    static var letterSample5: String { tr("letter.sample5") }
    static var devLetter: String { tr("dev.letter") }
    static var licensesBody: String { tr("licenses.body") }
    static var footerAbout: String { tr("footer.about") }
    static var footerContact: String { tr("footer.contact") }
    static var footerNotices: String { tr("footer.notices") }
    static var aboutBody: String { tr("about.body") }
    static var contactBody: String { tr("contact.body") }
    static var contactSoon: String { tr("contact.soon") }
    static var contactSend: String { tr("contact.send") }
    static var contactEmail: String { tr("contact.email") }
    static var noticesPrivacy: String { tr("notices.privacy") }
    static var careCalm: String { tr("care.calm") }
    static var careBox: String { tr("care.box") }
    static var careLook: String { tr("care.look") }
    static var careSendTo: String { tr("care.sendTo") }
    static var careSetting: String { tr("care.setting") }
    static var careFooter: String { tr("care.footer") }
    static var lookCue: String { tr("look.cue") }
    static var lookDone: String { tr("look.done") }
    static var thanksBook: String { tr("thanks.book") }
    static var thanksSub: String { tr("thanks.sub") }
    static var thanksEnd: String { tr("thanks.end") }
    static func thanksPage(_ n: String, _ total: String) -> String { tr("thanks.page", n, total) }
    static func letgoCount(_ n: String) -> String { tr("letgo.count", n) }
    static var memory: String { tr("memory") }
    static var memorySub: String { tr("memory.sub") }
    static func memorySeason(_ year: String, _ season: String) -> String { tr("memory.season", year, season) }
    static func memoryRange(_ from: String, _ until: String) -> String { tr("memory.range", from, until) }
    static func memoryFrom(_ from: String) -> String { tr("memory.from", from) }
    static func memoryUntil(_ until: String) -> String { tr("memory.until", until) }
    static var memoryStar: String { tr("memory.star") }
    static var memoryStarHelp: String { tr("memory.starHelp") }
    static func memoryStarNote(_ name: String) -> String { tr("memory.starNote", name) }
    static func memorySend(_ name: String) -> String { tr("memory.send", name) }
    static var memorySendHint: String { tr("memory.sendHint") }
    static func memorySent(_ name: String) -> String { tr("memory.sent", name) }
    static var memorySentToday: String { tr("memory.sentToday") }
    static func memoryCount(_ n: String) -> String { tr("memory.count", n) }
    static var memoryOpen: String { tr("memory.open") }
    static var memoryBack: String { tr("memory.back") }
    static var memoryBackFull: String { tr("memory.backFull") }
    static func memoryRemoveConfirm(_ name: String) -> String { tr("memory.removeConfirm", name) }
    static var memoryAdd: String { tr("memory.add") }
    static var memoryAddMore: String { tr("memory.addMore") }
    static var memoryFull: String { tr("memory.full") }
    static var memoryToMemory: String { tr("memory.toMemory") }
    static func memoryToMemoryConfirm(_ name: String) -> String { tr("memory.toMemoryConfirm", name) }
    static var memoryToMemoryAction: String { tr("memory.toMemoryAction") }
    static var memoryAddUntil: String { tr("memory.addUntil") }
    static func memoryMeet(_ name: String) -> String { tr("memory.meet", name) }
    static func memoryA11y(_ name: String) -> String { tr("memory.a11y", name) }
    static var memorySampleName: String { tr("memory.sampleName") }
    static var soundSeason: String { tr("sound.season") }
    static var breathPartMorning: String { tr("breath.part.morning") }
    static var breathPartDay: String { tr("breath.part.day") }
    static var breathPartEvening: String { tr("breath.part.evening") }
    static var breathPartNight: String { tr("breath.part.night") }
    static var breathDoneMorning1: String { tr("breath.done.morning.1") }
    static var breathDoneMorning2: String { tr("breath.done.morning.2") }
    static var breathDoneMorning3: String { tr("breath.done.morning.3") }
    static var breathDoneEvening1: String { tr("breath.done.evening.1") }
    static var breathDoneEvening2: String { tr("breath.done.evening.2") }
    static var breathDoneEvening3: String { tr("breath.done.evening.3") }
    static var breathDoneNight1: String { tr("breath.done.night.1") }
    static var breathDoneNight2: String { tr("breath.done.night.2") }
    static var breathDoneNight3: String { tr("breath.done.night.3") }
    static var breathMorning: String { tr("breath.morning") }
    static var letgoSubMorning: String { tr("letgo.sub.morning") }
    static var letgoSubEvening: String { tr("letgo.sub.evening") }
    static var letgoSubNight: String { tr("letgo.sub.night") }
    static var letgoMsgNoneMorning1: String { tr("letgo.msg.none.morning.1") }
    static var letgoMsgNoneEvening1: String { tr("letgo.msg.none.evening.1") }
    static var letgoMsgNoneNight1: String { tr("letgo.msg.none.night.1") }
    static var careSleep: String { tr("care.sleep") }
    static var careMorning: String { tr("care.morning") }
    static var lookDoneMorning: String { tr("look.done.morning") }
    static var lookDoneNight: String { tr("look.done.night") }
    static var notifyEvening: String { tr("notify.evening") }
    static var notifyEveningText: String { tr("notify.eveningText") }
    static var notifyEveningRow: String { tr("notify.eveningRow") }
    static var notifyEveningFooter: String { tr("notify.eveningFooter") }
    static var notifyMorningBreath: String { tr("notify.morningBreath") }
    static var haruStateRest: String { tr("haru.state.rest") }
    static var haruStateSmile: String { tr("haru.state.smile") }
    static var haruStateDown: String { tr("haru.state.down") }
    static var haruStateHat: String { tr("haru.state.hat") }
    static var haruStateCalm: String { tr("haru.state.calm") }
    static var breathKindThanks: String { tr("breath.kind.thanks") }
    static var breathKindDescThanks: String { tr("breath.kindDesc.thanks") }
    static var breathOutThanks: String { tr("breath.out.thanks") }
    static var breathDoneThanks1: String { tr("breath.done.thanks.1") }
    static var breathDoneThanks2: String { tr("breath.done.thanks.2") }
    static var breathDoneThanks3: String { tr("breath.done.thanks.3") }
    static var breathThanksAsk: String { tr("breath.thanksAsk") }
    static var breathThanksHint: String { tr("breath.thanksHint") }
    static var breathThanksKeep: String { tr("breath.thanksKeep") }
    static var breathThanksKept: String { tr("breath.thanksKept") }
    static func wishTitle(_ season: String) -> String { tr("wish.title", season) }
    static var wishSub: String { tr("wish.sub") }
    static var wishHint: String { tr("wish.hint") }
    static var wishKeep: String { tr("wish.keep") }
    static var wishLater: String { tr("wish.later") }
    static var wishKept: String { tr("wish.kept") }
    static func wishBack(_ season: String) -> String { tr("wish.back", season) }
    static func yearCard(_ year: String) -> String { tr("year.card", year) }
    static func yearTitle(_ year: String) -> String { tr("year.title", year) }
    static func yearCount(_ n: String, _ t: String) -> String { tr("year.count", n, t) }
    static var yearEnd: String { tr("year.end") }
    static var yearList: String { tr("year.list") }
    static var shareImage: String { tr("share.image") }
    static var shareChooser: String { tr("share.chooser") }
    static var shareFooter: String { tr("share.footer") }
    static var devYear: String { tr("dev.year") }
    static var wishSample: String { tr("wish.sample") }
    static var supportThanks: String { tr("support.thanks") }
    static var bdayCard: String { tr("bday.card") }
    static func bdayCardTitle(_ name: String) -> String { tr("bday.cardTitle", name) }
    static var bdayCardSub: String { tr("bday.cardSub") }
    static var specialAdd: String { tr("special.add") }
    static var specialTitle: String { tr("special.title") }
    static var specialName: String { tr("special.name") }
    static var specialNameHint: String { tr("special.nameHint") }
    static var specialSave: String { tr("special.save") }
    static var specialRemove: String { tr("special.remove") }
    static func specialRemoveConfirm(_ name: String) -> String { tr("special.removeConfirm", name) }
    static var specialFull: String { tr("special.full") }
    static func yearCardNight(_ year: String) -> String { tr("year.cardNight", year) }
    static var recordMonth: String { tr("record.month") }
    static var recordYear: String { tr("record.year") }
    static func recordMonthTitle(_ month: String) -> String { tr("record.monthTitle", month) }
    static var recordPrev: String { tr("record.prev") }
    static var recordNext: String { tr("record.next") }
    static var recordRest: String { tr("record.rest") }
    static var recordNoText: String { tr("record.noText") }
    static var recordHint: String { tr("record.hint") }
    static func recordCard(_ month: String) -> String { tr("record.card", month) }
    static func recordCardNight(_ month: String) -> String { tr("record.cardNight", month) }
    static var recordDayPattern: String { tr("record.dayPattern") }
    static var notifyTomorrowRow: String { tr("notify.tomorrowRow") }
    static var notifyTomorrowFooter: String { tr("notify.tomorrowFooter") }
    static func notifyTomorrowBirthday(_ names: String) -> String { tr("notify.tomorrowBirthday", names) }
    static var notifyTomorrowBirthdayText: String { tr("notify.tomorrowBirthdayText") }
    static func notifyTomorrowSpecial(_ name: String, _ years: String) -> String { tr("notify.tomorrowSpecial", name, years) }
    static var notifyTomorrowSpecialText: String { tr("notify.tomorrowSpecialText") }
    static var notifyGardenText: String { tr("notify.gardenText") }
    static func bdayTomorrow(_ name: String) -> String { tr("bday.tomorrow", name) }
    static func bdayToday(_ name: String) -> String { tr("bday.today", name) }
    static var bdayMineTomorrow: String { tr("bday.mineTomorrow") }
    static var bdayMineToday: String { tr("bday.mineToday") }
    static var stoneBirthdayTomorrow: String { tr("stone.birthdayTomorrow") }
    static var notifyTodayBirthdayText: String { tr("notify.todayBirthdayText") }
    static func notifyTodaySpecial(_ name: String, _ years: String) -> String { tr("notify.todaySpecial", name, years) }
    static var devNotify: String { tr("dev.notify") }
    static var tabGarden: String { tr("tab.garden") }
    static var tabWrite: String { tr("tab.write") }
    static var tabMemories: String { tr("tab.memories") }
    static var tabFlow: String { tr("tab.flow") }
    static var familyAddShort: String { tr("family.addShort") }
    static var stoneRemoveHelp: String { tr("stone.removeHelp") }
    static var stoneTidy: String { tr("stone.tidy") }
    static var memoryToMemoryHelp: String { tr("memory.toMemoryHelp") }
    static var recordOff: String { tr("record.off") }
    static var recordYearHint: String { tr("record.yearHint") }
    static var gazeAsk: String { tr("gaze.ask") }
    static var gazeBack: String { tr("gaze.back") }
    static var gazeStay: String { tr("gaze.stay") }
    static var widgetRecordGarden: String { tr("widget.record.garden") }
    static var widgetRecordDesc: String { tr("widget.record.desc") }
    static var backupExport: String { tr("backup.export") }
    static var backupImport: String { tr("backup.import") }
    static var backupImportConfirm: String { tr("backup.importConfirm") }
    static var backupImportAction: String { tr("backup.importAction") }
    static var backupSaved: String { tr("backup.saved") }
    static var backupDone: String { tr("backup.done") }
    static var backupFail: String { tr("backup.fail") }
    static var decorTreeCherry: String { tr("decor.tree.cherry") }
    static var decorTreeZelkova: String { tr("decor.tree.zelkova") }
    static var decorTreeGinkgo: String { tr("decor.tree.ginkgo") }
    static var decorTreePine: String { tr("decor.tree.pine") }
    static var decorStage0: String { tr("decor.stage.0") }
    static var decorStage1: String { tr("decor.stage.1") }
    static var decorStage2: String { tr("decor.stage.2") }
    static var decorStage3: String { tr("decor.stage.3") }
    static func decorTreeLine(_ days: String, _ stage: String) -> String { tr("decor.tree.line", days, stage) }
    static func decorTreeNext(_ days: String) -> String { tr("decor.tree.next", days) }
    static var decorTreeHelp: String { tr("decor.tree.help") }
    static var decorPost: String { tr("decor.post") }
    static var decorHangChime: String { tr("decor.hang.chime") }
    static var decorHangBell: String { tr("decor.hang.bell") }
    static var decorHangLantern: String { tr("decor.hang.lantern") }
    static var decorPostNone: String { tr("decor.post.none") }
    static func decorPostLine(_ n: String) -> String { tr("decor.post.line", n) }
    static func decorPostNext(_ n: String) -> String { tr("decor.post.next", n) }
    static var decorPostHelp: String { tr("decor.post.help") }
    static var decorPostLetter: String { tr("decor.post.letter") }
    static var decorKite: String { tr("decor.kite") }
    static func decorKiteLine(_ n: String, _ k: String) -> String { tr("decor.kite.line", n, k) }
    static func decorKiteNone(_ n: String) -> String { tr("decor.kite.none", n) }
    static var decorKiteHelp: String { tr("decor.kite.help") }
    static func decorMossLine(_ n: String, _ k: String) -> String { tr("decor.moss.line", n, k) }
    static var decorMossHelp: String { tr("decor.moss.help") }
    static var decorCard: String { tr("decor.card") }
    static func decorCardLine(_ year: String, _ season: String, _ name: String) -> String { tr("decor.card.line", year, season, name) }
    static var decorCardHelp: String { tr("decor.card.help") }
    static var decorCardCherrySpring: String { tr("decor.card.cherry_spring") }
    static var decorCardCherrySummer: String { tr("decor.card.cherry_summer") }
    static var decorCardCherryAutumn: String { tr("decor.card.cherry_autumn") }
    static var decorCardCherryWinter: String { tr("decor.card.cherry_winter") }
    static var decorCardZelkovaSpring: String { tr("decor.card.zelkova_spring") }
    static var decorCardZelkovaSummer: String { tr("decor.card.zelkova_summer") }
    static var decorCardZelkovaAutumn: String { tr("decor.card.zelkova_autumn") }
    static var decorCardZelkovaWinter: String { tr("decor.card.zelkova_winter") }
    static var decorCardGinkgoSpring: String { tr("decor.card.ginkgo_spring") }
    static var decorCardGinkgoSummer: String { tr("decor.card.ginkgo_summer") }
    static var decorCardGinkgoAutumn: String { tr("decor.card.ginkgo_autumn") }
    static var decorCardGinkgoWinter: String { tr("decor.card.ginkgo_winter") }
    static var decorCardPineSpring: String { tr("decor.card.pine_spring") }
    static var decorCardPineSummer: String { tr("decor.card.pine_summer") }
    static var decorCardPineAutumn: String { tr("decor.card.pine_autumn") }
    static var decorCardPineWinter: String { tr("decor.card.pine_winter") }
    static var decorNewTree: String { tr("decor.new.tree") }
    static var decorNewStage: String { tr("decor.new.stage") }
    static var decorNewChime: String { tr("decor.new.chime") }
    static var decorNewBell: String { tr("decor.new.bell") }
    static var decorNewLantern: String { tr("decor.new.lantern") }
    static var decorNewKite: String { tr("decor.new.kite") }
    static var decorNewRibbon: String { tr("decor.new.ribbon") }
    static var decorNewBud: String { tr("decor.new.bud") }
    static var decorNewCard: String { tr("decor.new.card") }
    static var decorNewLetter: String { tr("decor.new.letter") }
    static var collectionGrown: String { tr("collection.grown") }
    static var collectionGrownSub: String { tr("collection.grownSub") }
    static var decorHint: String { tr("decor.hint") }
    static var album: String { tr("album") }
    static func albumLines(_ n: String) -> String { tr("album.lines", n) }
    static func albumWhen(_ year: String, _ season: String, _ tree: String) -> String { tr("album.when", year, season, tree) }
    static func albumCell(_ year: String, _ season: String) -> String { tr("album.cell", year, season) }
    static var albumFirst: String { tr("album.first") }
    static var albumFirstSub: String { tr("album.firstSub") }
    static var chanceBubbles: String { tr("chance.bubbles") }
    static var chanceFireflies: String { tr("chance.fireflies") }
    static var chanceRainbow: String { tr("chance.rainbow") }
    static var chanceButterflies: String { tr("chance.butterflies") }
    static var chanceSnail: String { tr("chance.snail") }
    static var chanceAurora: String { tr("chance.aurora") }
    static var chanceWind: String { tr("chance.wind") }
    static var albumMet: String { tr("album.met") }
    static var albumMetSub: String { tr("album.metSub") }
    static var termSohan: String { tr("term.sohan") }
    static var termDaehan: String { tr("term.daehan") }
    static var termIpchun: String { tr("term.ipchun") }
    static var termUsu: String { tr("term.usu") }
    static var termGyeongchip: String { tr("term.gyeongchip") }
    static var termChunbun: String { tr("term.chunbun") }
    static var termCheongmyeong: String { tr("term.cheongmyeong") }
    static var termGogu: String { tr("term.gogu") }
    static var termIpha: String { tr("term.ipha") }
    static var termSoman: String { tr("term.soman") }
    static var termMangjong: String { tr("term.mangjong") }
    static var termHaji: String { tr("term.haji") }
    static var termSoseo: String { tr("term.soseo") }
    static var termDaeseo: String { tr("term.daeseo") }
    static var termIpchu: String { tr("term.ipchu") }
    static var termCheoseo: String { tr("term.cheoseo") }
    static var termBaengno: String { tr("term.baengno") }
    static var termChubun: String { tr("term.chubun") }
    static var termHallo: String { tr("term.hallo") }
    static var termSanggang: String { tr("term.sanggang") }
    static var termIpdong: String { tr("term.ipdong") }
    static var termSoseol: String { tr("term.soseol") }
    static var termDaeseol: String { tr("term.daeseol") }
    static var termDongji: String { tr("term.dongji") }
    static var offerButton: String { tr("offer.button") }
    static var offerAsk: String { tr("offer.ask") }
    static func offerAction(_ piece: String) -> String { tr("offer.action", piece) }
    static func offerLabel(_ season: String, _ name: String, _ piece: String) -> String { tr("offer.label", season, name, piece) }
    static func offerDone(_ name: String) -> String { tr("offer.done", name) }
    static var offerGiven: String { tr("offer.given") }
    static func memoryWeekNotify(_ name: String, _ subject: String) -> String { tr("memory.weekNotify", name, subject) }
    static var memoryWeekRow: String { tr("memory.weekRow") }
    static var memoryWeekHelp: String { tr("memory.weekHelp") }
    static var notifyMorning0: String { tr("notify.morning.0") }
    static var notifyMorning1: String { tr("notify.morning.1") }
    static var notifyMorning2: String { tr("notify.morning.2") }
    static var notifyMorning3: String { tr("notify.morning.3") }
    static var notifyMorning4: String { tr("notify.morning.4") }
    static var notifyEvening0: String { tr("notify.evening.0") }
    static var notifyEvening1: String { tr("notify.evening.1") }
    static var notifyEvening2: String { tr("notify.evening.2") }
    static var notifyEvening3: String { tr("notify.evening.3") }
    static var notifyEvening4: String { tr("notify.evening.4") }
    static var breathTileLabel: String { tr("breathTile.label") }
    static var breathTileSub: String { tr("breathTile.sub") }
    static var widgetsNameBreath: String { tr("widgets.name.breath") }
    static var widgetBreathGarden: String { tr("widget.breath.garden") }
    static var widgetBreathDesc: String { tr("widget.breath.desc") }
    static func widgetBreathMinute(_ name: String) -> String { tr("widget.breath.minute", name) }
    static var gardenYearAsk: String { tr("gardenYear.ask") }
    static var gardenYearOpen: String { tr("gardenYear.open") }
    static func gardenYearTitle(_ year: String) -> String { tr("gardenYear.title", year) }
    static var gardenYearAlbum: String { tr("gardenYear.album") }
    static var gardenYearMet: String { tr("gardenYear.met") }
    static var gardenYearEmpty: String { tr("gardenYear.empty") }
    static var questionOn: String { tr("question.on") }
    static var questionOnFooter: String { tr("question.onFooter") }
    static var letgoToday: String { tr("letgo.today") }
    static var letgoOther: String { tr("letgo.other") }
    static func letgoDayTitle(_ date: String) -> String { tr("letgo.dayTitle", date) }
    static var letgoDayHint: String { tr("letgo.dayHint") }
    static var letgoDaySend: String { tr("letgo.daySend") }
    static var letgoBackToday: String { tr("letgo.backToday") }
    static func letgoDayDone(_ date: String) -> String { tr("letgo.dayDone", date) }
    static var letgoDayTaken: String { tr("letgo.dayTaken") }
    static var recordWriteDay: String { tr("record.writeDay") }
    static var introTitle1: String { tr("intro.title1") }
    static var introBody1: String { tr("intro.body1") }
    static var introTitle2: String { tr("intro.title2") }
    static var introBody2: String { tr("intro.body2") }
    static var introTitle3: String { tr("intro.title3") }
    static var introBody3: String { tr("intro.body3") }
    static var introStart: String { tr("intro.start") }
    static var guideSkip: String { tr("guide.skip") }
    static var guideNext: String { tr("guide.next") }
    static var guideOk: String { tr("guide.ok") }
    static var guideAgain: String { tr("guide.again") }
    static var guideWelcomeTitle: String { tr("guide.welcomeTitle") }
    static var guideWelcome: String { tr("guide.welcome") }
    static var guideNumberTitle: String { tr("guide.numberTitle") }
    static var guideNumber: String { tr("guide.number") }
    static var guideWordsTitle: String { tr("guide.wordsTitle") }
    static var guideWords: String { tr("guide.words") }
    static var guideHaruTitle: String { tr("guide.haruTitle") }
    static var guideHaru: String { tr("guide.haru") }
    static var guidePathTitle: String { tr("guide.pathTitle") }
    static var guidePath: String { tr("guide.path") }
    static var guideActionsTitle: String { tr("guide.actionsTitle") }
    static var guideActions: String { tr("guide.actions") }
    static var guideTabsTitle: String { tr("guide.tabsTitle") }
    static var guideTabs: String { tr("guide.tabs") }
    static var guideSettingsTitle: String { tr("guide.settingsTitle") }
    static var guideSettings: String { tr("guide.settings") }
    static var guideEndTitle: String { tr("guide.endTitle") }
    static var guideEnd: String { tr("guide.end") }
    static var guideWrite: String { tr("guide.write") }
    static var guideLook: String { tr("guide.look") }
    static var notifyAskTitle: String { tr("notify.askTitle") }
    static var notifyAskBody: String { tr("notify.askBody") }
    static var notifyAskYes: String { tr("notify.askYes") }
    static var notifyAskLater: String { tr("notify.askLater") }
    static var notifyBlockedTitle: String { tr("notify.blockedTitle") }
    static var notifyBlocked: String { tr("notify.blocked") }
    static var notifyOpenSettings: String { tr("notify.openSettings") }
    static var recordTodayHidden: String { tr("record.todayHidden") }
    static func letgoTotal(_ n: String) -> String { tr("letgo.total", n) }
    static var letgoSeeBelow: String { tr("letgo.seeBelow") }
    static var letgoYesterday: String { tr("letgo.yesterday") }
    static var backupTitle: String { tr("backup.title") }
    static var backupFooter: String { tr("backup.footer") }
    static var backupSaveFail: String { tr("backup.saveFail") }
    static var backupBefore: String { tr("backup.before") }
    static var backupFirst: String { tr("backup.first") }
    static var settingsSaveAsk: String { tr("settings.saveAsk") }
    static var settingsSave: String { tr("settings.save") }
    static var settingsDiscard: String { tr("settings.discard") }
    static var onboardNeedBirth: String { tr("onboard.needBirth") }
    static var onboardPickBirth: String { tr("onboard.pickBirth") }
    static var breathSleepBack: String { tr("breath.sleepBack") }
    static var addNeedName: String { tr("add.needName") }
    static var tourWriteBoxTitle: String { tr("tour.writeBoxTitle") }
    static var tourWriteBox: String { tr("tour.writeBox") }
    static var tourWriteFeelingTitle: String { tr("tour.writeFeelingTitle") }
    static var tourWriteFeeling: String { tr("tour.writeFeeling") }
    static var tourWriteDaysTitle: String { tr("tour.writeDaysTitle") }
    static var tourWriteDays: String { tr("tour.writeDays") }
    static var tourWriteRecordTitle: String { tr("tour.writeRecordTitle") }
    static var tourWriteRecord: String { tr("tour.writeRecord") }
    static var tourMemAlbumTitle: String { tr("tour.memAlbumTitle") }
    static var tourMemAlbum: String { tr("tour.memAlbum") }
    static var tourMemYearTitle: String { tr("tour.memYearTitle") }
    static var tourMemYear: String { tr("tour.memYear") }
    static var tourMemMoreTitle: String { tr("tour.memMoreTitle") }
    static var tourMemMore: String { tr("tour.memMore") }
    static var tourFlowBarsTitle: String { tr("tour.flowBarsTitle") }
    static var tourFlowBars: String { tr("tour.flowBars") }
    static var tourFlowCalendarTitle: String { tr("tour.flowCalendarTitle") }
    static var tourFlowCalendar: String { tr("tour.flowCalendar") }
    static var tourFlowSpecialTitle: String { tr("tour.flowSpecialTitle") }
    static var tourFlowSpecial: String { tr("tour.flowSpecial") }
    static var tourStoneTitle: String { tr("tour.stoneTitle") }
    static var tourStone: String { tr("tour.stone") }
    static var tourStoneInfoTitle: String { tr("tour.stoneInfoTitle") }
    static var tourStoneInfo: String { tr("tour.stoneInfo") }
    static var tourStoneCalendarTitle: String { tr("tour.stoneCalendarTitle") }
    static var tourStoneCalendar: String { tr("tour.stoneCalendar") }
    static var tourStoneActionTitle: String { tr("tour.stoneActionTitle") }
    static var tourStoneAction: String { tr("tour.stoneAction") }
    static var onboardRestore: String { tr("onboard.restore") }
    static var lockTitle: String { tr("lock.title") }
    static var lockBody: String { tr("lock.body") }
    static var editAction: String { tr("edit.action") }
    static var editSave: String { tr("edit.save") }
    static var editDone: String { tr("edit.done") }
    static var editDelete: String { tr("edit.delete") }
    static var editDeleted: String { tr("edit.deleted") }
    static var editDeleteAsk: String { tr("edit.deleteAsk") }
    static var editDeleteHelp: String { tr("edit.deleteHelp") }
    static var editDeleteDay: String { tr("edit.deleteDay") }
    static func editDeleteDayAsk(_ day: String) -> String { tr("edit.deleteDayAsk", day) }
    static var feedbackRow: String { tr("feedback.row") }
    static var feedbackFooter: String { tr("feedback.footer") }
    static var feedbackSubject: String { tr("feedback.subject") }
    static var feedbackCrashSubject: String { tr("feedback.crashSubject") }
    static var feedbackCrashTitle: String { tr("feedback.crashTitle") }
    static var feedbackCrashBody: String { tr("feedback.crashBody") }
    static var feedbackCrashSend: String { tr("feedback.crashSend") }
    static var feedbackCrashSkip: String { tr("feedback.crashSkip") }
    static var notifyTime: String { tr("notify.time") }
    static var widgetLineGarden: String { tr("widget.line.garden") }
    static var widgetLineDesc: String { tr("widget.line.desc") }
    static var widgetsNameLine: String { tr("widgets.name.line") }
    static var widgetLineWrite: String { tr("widget.lineWrite") }
    static var widgetLineDone: String { tr("widget.lineDone") }
    static var nudgeBreath: String { tr("nudge.breath") }
    static var nudgeStone: String { tr("nudge.stone") }
    static var nudgeGaze: String { tr("nudge.gaze") }
    static var nudgeSpecial: String { tr("nudge.special") }
    static var nudgeWidget: String { tr("nudge.widget") }
    static var nudgeWidgetHow: String { tr("nudge.widgetHow") }
    static var nudgeBackup: String { tr("nudge.backup") }
    static var searchHint: String { tr("search.hint") }
    static var searchNone: String { tr("search.none") }
    static func searchCount(_ n: String) -> String { tr("search.count", n) }
    static func moodWeatherA11y(_ name: String) -> String { tr("mood.weatherA11y", name) }
    static func gardenPathA11y(_ age: String, _ years: String) -> String { tr("garden.pathA11y", age, years) }
    static var editLate: String { tr("edit.late") }
    static var editRestored: String { tr("edit.restored") }
    static var editUndo: String { tr("edit.undo") }
    static var editUntil: String { tr("edit.until") }
    static var letgoPrivacyOff: String { tr("letgo.privacyOff") }
    static var letgoPrivacyPlain: String { tr("letgo.privacyPlain") }
    static var recordOffHow: String { tr("record.offHow") }
    static func backupLast(_ date: String) -> String { tr("backup.last", date) }
    static var widgetPin: String { tr("widget.pin") }
    static var feedbackCopied: String { tr("feedback.copied") }
    static var replyAction: String { tr("reply.action") }
    static var replyHint: String { tr("reply.hint") }
    static var replyDone: String { tr("reply.done") }
    static var replyFeel: String { tr("reply.feel") }
    static func replyFelt(_ feeling: String) -> String { tr("reply.felt", feeling) }
    static var replyNight: String { tr("reply.night") }
    static var shortcutWrite: String { tr("shortcut.write") }
    static var shortcutBreath: String { tr("shortcut.breath") }
    static var shortcutGaze: String { tr("shortcut.gaze") }
    static var shortcutClose: String { tr("shortcut.close") }
    static var closeDayEntry: String { tr("closeDay.entry") }
    static var closeDayEntrySub: String { tr("closeDay.entrySub") }
    static var closeDayEntrySent: String { tr("closeDay.entrySent") }
    static var closeDayStop: String { tr("closeDay.stop") }
    static var closeDayEnough: String { tr("closeDay.enough") }
    static var closeDayKeep: String { tr("closeDay.keep") }
    static var closeDayLineTitle: String { tr("closeDay.lineTitle") }
    static var closeDayLineDone: String { tr("closeDay.lineDone") }
    static var closeDayLineDoneSub: String { tr("closeDay.lineDoneSub") }
    static var closeDayNext: String { tr("closeDay.next") }
    static var closeDaySkip: String { tr("closeDay.skip") }
    static var closeDayThanksTitle: String { tr("closeDay.thanksTitle") }
    static var closeDayLanternTitle: String { tr("closeDay.lanternTitle") }
    static var closeDayEnd: String { tr("closeDay.end") }
    static var closeDayEndSub: String { tr("closeDay.endSub") }
    static var closeDayOff: String { tr("closeDay.off") }
    static var closeDayGoodnight: String { tr("closeDay.goodnight") }
    static var seedTitle: String { tr("seed.title") }
    static var seedOwn: String { tr("seed.own") }
    static var seedHint: String { tr("seed.hint") }
    static var seedPlant: String { tr("seed.plant") }
    static var seedLater: String { tr("seed.later") }
    static var seedPlanted: String { tr("seed.planted") }
    static func seedAskLabel(_ seed: String) -> String { tr("seed.askLabel", seed) }
    static var seedAsk: String { tr("seed.ask") }
    static var seedYes: String { tr("seed.yes") }
    static var seedNo: String { tr("seed.no") }
    static var seedBloomed: String { tr("seed.bloomed") }
    static var seedRest: String { tr("seed.rest") }
    static var seedGarden: String { tr("seed.garden") }
    static var seedGardenSub: String { tr("seed.gardenSub") }
    static var seedSetting: String { tr("seed.setting") }
    static var seedSettingFooter: String { tr("seed.settingFooter") }
    static var seedChoice0: String { tr("seed.choice.0") }
    static var seedChoice1: String { tr("seed.choice.1") }
    static var seedChoice2: String { tr("seed.choice.2") }
    static var seedChoice3: String { tr("seed.choice.3") }
    static var seedChoice4: String { tr("seed.choice.4") }
    static var seedChoice5: String { tr("seed.choice.5") }
    static var seedChoice6: String { tr("seed.choice.6") }
    static var seedChoice7: String { tr("seed.choice.7") }
    static var seedChoice8: String { tr("seed.choice.8") }
    static var seedChoice9: String { tr("seed.choice.9") }
    static var seedChoice10: String { tr("seed.choice.10") }
    static var seedChoice11: String { tr("seed.choice.11") }
    static var breathWay: String { tr("breath.way") }
    static var breathWayEyes: String { tr("breath.wayEyes") }
    static var breathWayTouch: String { tr("breath.wayTouch") }
    static var breathWayTouchHelp: String { tr("breath.wayTouchHelp") }
    static var breathTouchTitle: String { tr("breath.touchTitle") }
    static var breathTouchSub: String { tr("breath.touchSub") }
    static var breathTouchHold: String { tr("breath.touchHold") }
    static var breathTouchDone: String { tr("breath.touchDone") }
    static var comfortFrom: String { tr("comfort.from") }
    static var comfort0: String { tr("comfort.0") }
    static var comfort1: String { tr("comfort.1") }
    static var comfort2: String { tr("comfort.2") }
    static var comfort3: String { tr("comfort.3") }
    static var comfort4: String { tr("comfort.4") }
    static var capsuleWhen: String { tr("capsule.when") }
    static var capsuleSeason: String { tr("capsule.season") }
    static var capsuleSeasonPlain: String { tr("capsule.seasonPlain") }
    static var capsuleBirthday: String { tr("capsule.birthday") }
    static var capsuleYear: String { tr("capsule.year") }
    static var capsuleTitle: String { tr("capsule.title") }
    static var capsuleSub: String { tr("capsule.sub") }
    static var capsuleHint: String { tr("capsule.hint") }
    static func capsuleOpensOn(_ date: String) -> String { tr("capsule.opensOn", date) }
    static var capsuleBury: String { tr("capsule.bury") }
    static func capsuleBuried(_ date: String) -> String { tr("capsule.buried", date) }
    static func capsuleFrom(_ date: String) -> String { tr("capsule.from", date) }
    static var capsuleReceived: String { tr("capsule.received") }
    static func capsuleMound(_ date: String) -> String { tr("capsule.mound", date) }
    static var capsuleOpened: String { tr("capsule.opened") }
    static var capsuleNotifyText: String { tr("capsule.notifyText") }
    static var capsuleSection: String { tr("capsule.section") }
    static var capsuleSectionSub: String { tr("capsule.sectionSub") }
    static func capsuleSealed(_ n: String, _ date: String) -> String { tr("capsule.sealed", n, date) }
    static var capsuleWrite: String { tr("capsule.write") }
    static func capsuleOpenedRow(_ written: String, _ opened: String) -> String { tr("capsule.openedRow", written, opened) }
    static var capsuleSample: String { tr("capsule.sample") }
    static func ringNew(_ n: String) -> String { tr("ring.new", n) }
    static func ringTitle(_ n: String) -> String { tr("ring.title", n) }
    static func ringCount(_ lines: String, _ thanks: String) -> String { tr("ring.count", lines, thanks) }
    static func ringTop(_ feeling: String) -> String { tr("ring.top", feeling) }
    static var ringWalk: String { tr("ring.walk") }
    static var ringWalkClose: String { tr("ring.walkClose") }
    static var ringLetter: String { tr("ring.letter") }
    static var ringSection: String { tr("ring.section") }
    static var ringSectionSub: String { tr("ring.sectionSub") }
    static func ringChip(_ n: String) -> String { tr("ring.chip", n) }
    static var recallKeepCard: String { tr("recall.keepCard") }
    static var recallContinue: String { tr("recall.continue") }
    static func recallContinueLabel(_ date: String) -> String { tr("recall.continueLabel", date) }
    static var returnTitle: String { tr("return.title") }
    static var guestTitStory: String { tr("guest.tit.story") }
    static var guestSquirrelStory: String { tr("guest.squirrel.story") }
    static var guestHedgehogStory: String { tr("guest.hedgehog.story") }
    static var guestRabbitStory: String { tr("guest.rabbit.story") }
    static var guestOwlStory: String { tr("guest.owl.story") }
    static var chanceGuestTit: String { tr("chance.guest_tit") }
    static var chanceGuestSquirrel: String { tr("chance.guest_squirrel") }
    static var chanceGuestHedgehog: String { tr("chance.guest_hedgehog") }
    static var chanceGuestRabbit: String { tr("chance.guest_rabbit") }
    static var chanceGuestOwl: String { tr("chance.guest_owl") }
    static var photoAdd: String { tr("photo.add") }
    static var photoAddToday: String { tr("photo.addToday") }
    static var photoRemove: String { tr("photo.remove") }
    static var photoFail: String { tr("photo.fail") }
    static var creditsSection: String { tr("credits.section") }
    static var creditsSectionSub: String { tr("credits.sectionSub") }
    static func creditsChip(_ year: String) -> String { tr("credits.chip", year) }
    static var creditsReady: String { tr("credits.ready") }
    static var creditsSkip: String { tr("credits.skip") }
    static var creditsPaused: String { tr("credits.paused") }
    static var creditsGarden: String { tr("credits.garden") }
    static var creditsIntro: String { tr("credits.intro") }
    static var creditsSpring: String { tr("credits.spring") }
    static var creditsSummer: String { tr("credits.summer") }
    static var creditsAutumn: String { tr("credits.autumn") }
    static var creditsWinter: String { tr("credits.winter") }
    static var creditsQuiet: String { tr("credits.quiet") }
    static var creditsStarring: String { tr("credits.starring") }
    static var creditsStone: String { tr("credits.stone") }
    static var creditsHaru: String { tr("credits.haru") }
    static var creditsPeople: String { tr("credits.people") }
    static var creditsKept: String { tr("credits.kept") }
    static var creditsMood: String { tr("credits.mood") }
    static func creditsNext(_ year: String) -> String { tr("credits.next", year) }
    static var seedNote: String { tr("seed.note") }
    static var creditsCountLines: String { tr("credits.countLines") }
    static var creditsCountThanks: String { tr("credits.countThanks") }
    static var creditsCountBreaths: String { tr("credits.countBreaths") }
    static func creditsMyBirthday(_ n: String) -> String { tr("credits.myBirthday", n) }
    static func creditsBirthday(_ name: String) -> String { tr("credits.birthday", name) }
    static func creditsTogetherDays(_ name: String, _ n: String) -> String { tr("credits.togetherDays", name, n) }
    static func creditsTogetherYears(_ name: String, _ n: String) -> String { tr("credits.togetherYears", name, n) }
    static func creditsSpecial(_ name: String, _ n: String) -> String { tr("credits.special", name, n) }
    static func creditsMoment(_ name: String) -> String { tr("credits.moment", name) }
    static func creditsSeed(_ text: String) -> String { tr("credits.seed", text) }
    static func creditsCapsule(_ year: String) -> String { tr("credits.capsule", year) }
    static var creditsFirst: String { tr("credits.first") }
    static func creditsPersonDays(_ name: String, _ n: String) -> String { tr("credits.personDays", name, n) }
    static var creditsGuests: String { tr("credits.guests") }
    static func creditsBloomed(_ n: String) -> String { tr("credits.bloomed", n) }
    static var creditsMade: String { tr("credits.made") }
    static var creditsMadeBy: String { tr("credits.madeBy") }
    static func creditsSLines(_ n: String) -> String { tr("credits.sLines", n) }
    static func creditsSThanks(_ n: String) -> String { tr("credits.sThanks", n) }
    static func creditsSBreaths(_ n: String) -> String { tr("credits.sBreaths", n) }
    static var titleName: String { tr("title.name") }
    static var titleEnter: String { tr("title.enter") }
    static var titleHello: String { tr("title.hello") }
    static var titleHelloMorning: String { tr("title.hello.morning") }
    static var titleHelloEvening: String { tr("title.hello.evening") }
    static var titleHelloNight: String { tr("title.hello.night") }
    static var titleSetting: String { tr("title.setting") }
    static var titleHelloFirst: String { tr("title.helloFirst") }
    static var moodReadSub: String { tr("mood.readSub") }

    static let allKeys: [String] = ["tagline", "you", "birthday", "country", "sex", "sex.other", "sex.male", "sex.female", "lifeExpectancy", "auto", "custom", "lifeExpectancy.footer", "begin", "privacy", "timeLeft", "timeLeft.days", "timeLeft.weeks", "timeLeft.months", "timeLeft.years", "unit.days", "unit.weeks", "unit.months", "unit.years", "words", "words.next", "path", "path.age", "path.expected", "flow", "flow.today", "flow.week", "flow.month", "flow.year", "left.hours", "left.minutes", "left.days", "lastDay", "calendar", "calendar.per.weeks", "calendar.per.months", "calendar.per.years", "calendar.legend", "season.spring", "season.summer", "season.autumn", "season.winter", "settings", "cancel", "done", "words.language", "words.korean", "words.english", "words.both", "widgets", "widgets.list", "widgets.name.daysLeft", "widgets.name.today", "widgets.name.calendar", "widgets.name.family", "widgets.name.record", "widgets.help1", "widgets.help2", "widgets.help3", "widgets.help4", "erase", "country.search", "country.source", "widget.daysLeft", "widget.today", "widget.todayLeft", "widget.todaySub", "widget.yearsLeft", "widget.monthsLeft", "lock.inline", "lock.rect.sub", "android.notification", "widget.unit", "widget.unit.desc", "widget.daysLeft.desc", "widget.today.desc", "widget.calendar.desc", "widget.empty", "erase.confirm", "erase.action", "expectancy.value", "country.world", "widgets.android1", "widgets.android2", "widgets.android3", "back", "design", "design.glass", "design.garden", "garden.meet.title", "garden.meet.sub", "garden.meet.go", "garden.drawing", "garden.haru", "garden.age0", "garden.close", "garden.preview", "garden.itemDate", "stone.basalt", "stone.granite", "stone.pinkgranite", "stone.sand", "stone.ochre", "stone.speckle", "stone.slate", "stone.gneiss", "stone.jasper", "stone.serpentine", "stone.jade", "stone.marble", "stone.quartz", "stone.ring", "obj.moss", "obj.moss.when", "obj.moss.line", "obj.teacup", "obj.teacup.when", "obj.teacup.line", "obj.cairn", "obj.cairn.when", "obj.cairn.line", "obj.pine", "obj.pine.when", "obj.pine.line", "obj.flower", "obj.flower.when", "obj.flower.line", "obj.pond", "obj.pond.when", "obj.pond.line", "obj.leaf", "obj.leaf.when", "obj.leaf.line", "obj.candle", "obj.candle.when", "obj.candle.line", "obj.dandelion", "obj.dandelion.when", "obj.dandelion.line", "obj.bookmark", "obj.bookmark.when", "obj.bookmark.line", "obj.snail", "obj.snail.when", "obj.snail.line", "obj.acorn", "obj.acorn.when", "obj.acorn.line", "notify.channel", "notify.channelEvening", "notify.channelDays", "notify.mineBirthdayText", "notify.keepsake", "notify.row", "notify.footer", "dev.unlocked", "garden.haruA11y", "collection", "collection.sub", "support", "support.title", "support.body", "support.tier1", "support.tier1.price", "support.tier2", "support.tier2.price", "support.tier3", "support.tier3.price", "support.once", "support.soon", "letgo.title", "letgo.sub", "letgo.feeling", "letgo.hint", "letgo.send", "letgo.done", "letgo.privacy", "feeling.joy", "feeling.thanks", "feeling.calm", "feeling.sad", "feeling.worry", "feeling.hope", "feeling.disappoint", "letgo.msg.joy.1", "letgo.msg.joy.2", "letgo.msg.joy.3", "letgo.msg.hope.1", "letgo.msg.hope.2", "letgo.msg.hope.3", "letgo.msg.calm.1", "letgo.msg.calm.2", "letgo.msg.calm.3", "letgo.msg.thanks.1", "letgo.msg.thanks.2", "letgo.msg.thanks.3", "letgo.msg.disappoint.1", "letgo.msg.disappoint.2", "letgo.msg.disappoint.3", "letgo.msg.sad.1", "letgo.msg.sad.2", "letgo.msg.sad.3", "letgo.msg.worry.1", "letgo.msg.worry.2", "letgo.msg.worry.3", "letgo.msg.none.1", "letgo.msg.none.2", "recall.title", "recall.open", "recall.close", "recall.notify", "recall.notifyText", "recall.sample", "recall.addSample", "lines", "lines.keep", "lines.keepFooter", "lines.export", "lines.exportEmpty", "lines.exportTitle", "lines.clear", "lines.clearConfirm", "lines.clearAction", "lines.count", "obj.pinwheel", "obj.pinwheel.when", "obj.pinwheel.line", "obj.paperboat", "obj.paperboat.when", "obj.paperboat.line", "obj.kite", "obj.kite.when", "obj.kite.line", "defaults", "defaults.unit", "defaults.grid", "defaults.footer", "widget.daysLeft.glass", "widget.daysLeft.garden", "widget.today.glass", "widget.today.garden", "widget.calendar.glass", "widget.calendar.garden", "letgo.modalTitle", "letgo.ok", "recall.random", "recall.randomNotify", "recall.addRandom", "recall.sampleOld", "garden.metOn", "family", "family.add", "family.full", "family.glassNote", "family.me", "add.kind", "kind.person", "kind.pet", "species.dog", "species.cat", "species.other", "add.name", "add.nameHint", "add.birth", "add.birthPet", "add.birthUnknown", "add.birthPick", "add.next", "add.meet", "add.reroll", "stone.together", "stone.sinceMet", "stone.days", "stone.nextBirthday", "stone.birthdayToday", "stone.calendar", "stone.calendarLegend", "stone.showAhead", "stone.lines", "stone.linesCount", "stone.edit", "stone.save", "stone.remove", "stone.removeConfirm", "stone.removeAction", "stone.age", "stone.petAge", "letgo.to", "letgo.modalTo", "garden.stoneA11y", "garden.pet", "widget.family.garden", "widget.family.desc", "breath", "breath.kind.calm", "breath.kind.box", "breath.kind.sleep", "breath.kindDesc.calm", "breath.kindDesc.box", "breath.kindDesc.sleep", "breath.minutes", "breath.sound", "sound.none", "sound.waves", "sound.wind", "sound.rain", "sound.tone", "breath.start", "breath.in", "breath.hold", "breath.out", "breath.rest", "breath.walk.in", "breath.walk.out", "breath.pause", "breath.keep", "breath.stop", "breath.startA11y", "breath.home", "breath.done.calm.1", "breath.done.calm.2", "breath.done.calm.3", "breath.done.box.1", "breath.done.box.2", "breath.done.box.3", "breath.done.sleep.1", "breath.done.sleep.2", "breath.done.sleep.3", "breath.night", "breath.ask", "breath.askHelp", "breath.askGo", "breath.askStay", "breath.bells", "gaze", "gaze.soundOn", "gaze.soundOff", "gaze.exit", "obj.windchime", "obj.windchime.when", "obj.windchime.line", "add.together", "add.togetherAuto", "add.togetherHelp", "question.label", "question.answer", "question.skip", "question.answered", "mood.title", "mood.sub", "mood.a11y", "letter.arrived", "letter.title", "letter.range", "letter.heavy", "letter.showHeavy", "letter.end", "letter.fold", "letters", "letters.sub", "notify.letter", "notify.letterText", "letter.sample1", "letter.sample2", "letter.sample3", "letter.sample4", "letter.sample5", "dev.letter", "licenses.body", "footer.about", "footer.contact", "footer.notices", "about.body", "contact.body", "contact.soon", "contact.send", "contact.email", "notices.privacy", "care.calm", "care.box", "care.look", "care.sendTo", "care.setting", "care.footer", "look.cue", "look.done", "thanks.book", "thanks.sub", "thanks.end", "thanks.page", "letgo.count", "memory", "memory.sub", "memory.season", "memory.range", "memory.from", "memory.until", "memory.star", "memory.starHelp", "memory.starNote", "memory.send", "memory.sendHint", "memory.sent", "memory.sentToday", "memory.count", "memory.open", "memory.back", "memory.backFull", "memory.removeConfirm", "memory.add", "memory.addMore", "memory.full", "memory.toMemory", "memory.toMemoryConfirm", "memory.toMemoryAction", "memory.addUntil", "memory.meet", "memory.a11y", "memory.sampleName", "sound.season", "breath.part.morning", "breath.part.day", "breath.part.evening", "breath.part.night", "breath.done.morning.1", "breath.done.morning.2", "breath.done.morning.3", "breath.done.evening.1", "breath.done.evening.2", "breath.done.evening.3", "breath.done.night.1", "breath.done.night.2", "breath.done.night.3", "breath.morning", "letgo.sub.morning", "letgo.sub.evening", "letgo.sub.night", "letgo.msg.none.morning.1", "letgo.msg.none.evening.1", "letgo.msg.none.night.1", "care.sleep", "care.morning", "look.done.morning", "look.done.night", "notify.evening", "notify.eveningText", "notify.eveningRow", "notify.eveningFooter", "notify.morningBreath", "haru.state.rest", "haru.state.smile", "haru.state.down", "haru.state.hat", "haru.state.calm", "breath.kind.thanks", "breath.kindDesc.thanks", "breath.out.thanks", "breath.done.thanks.1", "breath.done.thanks.2", "breath.done.thanks.3", "breath.thanksAsk", "breath.thanksHint", "breath.thanksKeep", "breath.thanksKept", "wish.title", "wish.sub", "wish.hint", "wish.keep", "wish.later", "wish.kept", "wish.back", "year.card", "year.title", "year.count", "year.end", "year.list", "share.image", "share.chooser", "share.footer", "dev.year", "wish.sample", "support.thanks", "bday.card", "bday.cardTitle", "bday.cardSub", "special.add", "special.title", "special.name", "special.nameHint", "special.save", "special.remove", "special.removeConfirm", "special.full", "year.cardNight", "record.month", "record.year", "record.monthTitle", "record.prev", "record.next", "record.rest", "record.noText", "record.hint", "record.card", "record.cardNight", "record.dayPattern", "notify.tomorrowRow", "notify.tomorrowFooter", "notify.tomorrowBirthday", "notify.tomorrowBirthdayText", "notify.tomorrowSpecial", "notify.tomorrowSpecialText", "notify.gardenText", "bday.tomorrow", "bday.today", "bday.mineTomorrow", "bday.mineToday", "stone.birthdayTomorrow", "notify.todayBirthdayText", "notify.todaySpecial", "dev.notify", "tab.garden", "tab.write", "tab.memories", "tab.flow", "family.addShort", "stone.removeHelp", "stone.tidy", "memory.toMemoryHelp", "record.off", "record.yearHint", "gaze.ask", "gaze.back", "gaze.stay", "widget.record.garden", "widget.record.desc", "backup.export", "backup.import", "backup.importConfirm", "backup.importAction", "backup.saved", "backup.done", "backup.fail", "decor.tree.cherry", "decor.tree.zelkova", "decor.tree.ginkgo", "decor.tree.pine", "decor.stage.0", "decor.stage.1", "decor.stage.2", "decor.stage.3", "decor.tree.line", "decor.tree.next", "decor.tree.help", "decor.post", "decor.hang.chime", "decor.hang.bell", "decor.hang.lantern", "decor.post.none", "decor.post.line", "decor.post.next", "decor.post.help", "decor.post.letter", "decor.kite", "decor.kite.line", "decor.kite.none", "decor.kite.help", "decor.moss.line", "decor.moss.help", "decor.card", "decor.card.line", "decor.card.help", "decor.card.cherry_spring", "decor.card.cherry_summer", "decor.card.cherry_autumn", "decor.card.cherry_winter", "decor.card.zelkova_spring", "decor.card.zelkova_summer", "decor.card.zelkova_autumn", "decor.card.zelkova_winter", "decor.card.ginkgo_spring", "decor.card.ginkgo_summer", "decor.card.ginkgo_autumn", "decor.card.ginkgo_winter", "decor.card.pine_spring", "decor.card.pine_summer", "decor.card.pine_autumn", "decor.card.pine_winter", "decor.new.tree", "decor.new.stage", "decor.new.chime", "decor.new.bell", "decor.new.lantern", "decor.new.kite", "decor.new.ribbon", "decor.new.bud", "decor.new.card", "decor.new.letter", "collection.grown", "collection.grownSub", "decor.hint", "album", "album.lines", "album.when", "album.cell", "album.first", "album.firstSub", "chance.bubbles", "chance.fireflies", "chance.rainbow", "chance.butterflies", "chance.snail", "chance.aurora", "chance.wind", "album.met", "album.metSub", "term.sohan", "term.daehan", "term.ipchun", "term.usu", "term.gyeongchip", "term.chunbun", "term.cheongmyeong", "term.gogu", "term.ipha", "term.soman", "term.mangjong", "term.haji", "term.soseo", "term.daeseo", "term.ipchu", "term.cheoseo", "term.baengno", "term.chubun", "term.hallo", "term.sanggang", "term.ipdong", "term.soseol", "term.daeseol", "term.dongji", "offer.button", "offer.ask", "offer.action", "offer.label", "offer.done", "offer.given", "memory.weekNotify", "memory.weekRow", "memory.weekHelp", "notify.morning.0", "notify.morning.1", "notify.morning.2", "notify.morning.3", "notify.morning.4", "notify.evening.0", "notify.evening.1", "notify.evening.2", "notify.evening.3", "notify.evening.4", "breathTile.label", "breathTile.sub", "widgets.name.breath", "widget.breath.garden", "widget.breath.desc", "widget.breath.minute", "gardenYear.ask", "gardenYear.open", "gardenYear.title", "gardenYear.album", "gardenYear.met", "gardenYear.empty", "question.on", "question.onFooter", "letgo.today", "letgo.other", "letgo.dayTitle", "letgo.dayHint", "letgo.daySend", "letgo.backToday", "letgo.dayDone", "letgo.dayTaken", "record.writeDay", "intro.title1", "intro.body1", "intro.title2", "intro.body2", "intro.title3", "intro.body3", "intro.start", "guide.skip", "guide.next", "guide.ok", "guide.again", "guide.welcomeTitle", "guide.welcome", "guide.numberTitle", "guide.number", "guide.wordsTitle", "guide.words", "guide.haruTitle", "guide.haru", "guide.pathTitle", "guide.path", "guide.actionsTitle", "guide.actions", "guide.tabsTitle", "guide.tabs", "guide.settingsTitle", "guide.settings", "guide.endTitle", "guide.end", "guide.write", "guide.look", "notify.askTitle", "notify.askBody", "notify.askYes", "notify.askLater", "notify.blockedTitle", "notify.blocked", "notify.openSettings", "record.todayHidden", "letgo.total", "letgo.seeBelow", "letgo.yesterday", "backup.title", "backup.footer", "backup.saveFail", "backup.before", "backup.first", "settings.saveAsk", "settings.save", "settings.discard", "onboard.needBirth", "onboard.pickBirth", "breath.sleepBack", "add.needName", "tour.writeBoxTitle", "tour.writeBox", "tour.writeFeelingTitle", "tour.writeFeeling", "tour.writeDaysTitle", "tour.writeDays", "tour.writeRecordTitle", "tour.writeRecord", "tour.memAlbumTitle", "tour.memAlbum", "tour.memYearTitle", "tour.memYear", "tour.memMoreTitle", "tour.memMore", "tour.flowBarsTitle", "tour.flowBars", "tour.flowCalendarTitle", "tour.flowCalendar", "tour.flowSpecialTitle", "tour.flowSpecial", "tour.stoneTitle", "tour.stone", "tour.stoneInfoTitle", "tour.stoneInfo", "tour.stoneCalendarTitle", "tour.stoneCalendar", "tour.stoneActionTitle", "tour.stoneAction", "onboard.restore", "lock.title", "lock.body", "edit.action", "edit.save", "edit.done", "edit.delete", "edit.deleted", "edit.deleteAsk", "edit.deleteHelp", "edit.deleteDay", "edit.deleteDayAsk", "feedback.row", "feedback.footer", "feedback.subject", "feedback.crashSubject", "feedback.crashTitle", "feedback.crashBody", "feedback.crashSend", "feedback.crashSkip", "notify.time", "widget.line.garden", "widget.line.desc", "widgets.name.line", "widget.lineWrite", "widget.lineDone", "nudge.breath", "nudge.stone", "nudge.gaze", "nudge.special", "nudge.widget", "nudge.widgetHow", "nudge.backup", "search.hint", "search.none", "search.count", "mood.weatherA11y", "garden.pathA11y", "edit.late", "edit.restored", "edit.undo", "edit.until", "letgo.privacyOff", "letgo.privacyPlain", "record.offHow", "backup.last", "widget.pin", "feedback.copied", "reply.action", "reply.hint", "reply.done", "reply.feel", "reply.felt", "reply.night", "shortcut.write", "shortcut.breath", "shortcut.gaze", "shortcut.close", "closeDay.entry", "closeDay.entrySub", "closeDay.entrySent", "closeDay.stop", "closeDay.enough", "closeDay.keep", "closeDay.lineTitle", "closeDay.lineDone", "closeDay.lineDoneSub", "closeDay.next", "closeDay.skip", "closeDay.thanksTitle", "closeDay.lanternTitle", "closeDay.end", "closeDay.endSub", "closeDay.off", "closeDay.goodnight", "seed.title", "seed.own", "seed.hint", "seed.plant", "seed.later", "seed.planted", "seed.askLabel", "seed.ask", "seed.yes", "seed.no", "seed.bloomed", "seed.rest", "seed.garden", "seed.gardenSub", "seed.setting", "seed.settingFooter", "seed.choice.0", "seed.choice.1", "seed.choice.2", "seed.choice.3", "seed.choice.4", "seed.choice.5", "seed.choice.6", "seed.choice.7", "seed.choice.8", "seed.choice.9", "seed.choice.10", "seed.choice.11", "breath.way", "breath.wayEyes", "breath.wayTouch", "breath.wayTouchHelp", "breath.touchTitle", "breath.touchSub", "breath.touchHold", "breath.touchDone", "comfort.from", "comfort.0", "comfort.1", "comfort.2", "comfort.3", "comfort.4", "capsule.when", "capsule.season", "capsule.seasonPlain", "capsule.birthday", "capsule.year", "capsule.title", "capsule.sub", "capsule.hint", "capsule.opensOn", "capsule.bury", "capsule.buried", "capsule.from", "capsule.received", "capsule.mound", "capsule.opened", "capsule.notifyText", "capsule.section", "capsule.sectionSub", "capsule.sealed", "capsule.write", "capsule.openedRow", "capsule.sample", "ring.new", "ring.title", "ring.count", "ring.top", "ring.walk", "ring.walkClose", "ring.letter", "ring.section", "ring.sectionSub", "ring.chip", "recall.keepCard", "recall.continue", "recall.continueLabel", "return.title", "guest.tit.story", "guest.squirrel.story", "guest.hedgehog.story", "guest.rabbit.story", "guest.owl.story", "chance.guest_tit", "chance.guest_squirrel", "chance.guest_hedgehog", "chance.guest_rabbit", "chance.guest_owl", "photo.add", "photo.addToday", "photo.remove", "photo.fail", "credits.section", "credits.sectionSub", "credits.chip", "credits.ready", "credits.skip", "credits.paused", "credits.garden", "credits.intro", "credits.spring", "credits.summer", "credits.autumn", "credits.winter", "credits.quiet", "credits.starring", "credits.stone", "credits.haru", "credits.people", "credits.kept", "credits.mood", "credits.next", "seed.note", "credits.countLines", "credits.countThanks", "credits.countBreaths", "credits.myBirthday", "credits.birthday", "credits.togetherDays", "credits.togetherYears", "credits.special", "credits.moment", "credits.seed", "credits.capsule", "credits.first", "credits.personDays", "credits.guests", "credits.bloomed", "credits.made", "credits.madeBy", "credits.sLines", "credits.sThanks", "credits.sBreaths", "title.name", "title.enter", "title.hello", "title.hello.morning", "title.hello.evening", "title.hello.night", "title.setting", "title.helloFirst", "mood.readSub"]

    private static func tr(_ key: String, _ args: String...) -> String {
        let format = Bundle.main.localizedString(forKey: key, value: nil, table: nil)
        return args.isEmpty ? format : String(format: format, arguments: args.map { $0 as NSString })
    }
}
