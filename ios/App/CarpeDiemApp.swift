import SwiftUI
import WidgetKit

@main
struct CarpeDiemApp: App {
    @StateObject private var model = AppModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
                .tint(.cdOlive)
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active { model.refreshQuote() }
                }
        }
    }
}

@MainActor
final class AppModel: ObservableObject {
    private let store = LifeStore.shared

    @Published private(set) var profile: LifeProfile?
    @Published private(set) var quote: Quote?
    @Published var quoteLanguage: QuoteLanguage {
        didSet { store.quoteLanguage = quoteLanguage; reloadWidgets() }
    }

    init() {
        let store = LifeStore.shared
        Self.applyTestArguments(store)
        store.ensureQuoteSeed()
        quoteLanguage = store.quoteLanguage
        profile = store.profile
        quote = store.todaysQuote()
    }

    func save(_ profile: LifeProfile) {
        store.profile = profile
        self.profile = profile
        reloadWidgets()
    }

    func nextQuote() {
        store.skipQuote()
        quote = store.todaysQuote()
        reloadWidgets()
    }

    func refreshQuote() { quote = store.todaysQuote() }

    func eraseAll() {
        store.eraseAll()
        store.ensureQuoteSeed()
        profile = nil
        quoteLanguage = .default
        quote = store.todaysQuote()
        reloadWidgets()
    }

    private func reloadWidgets() { WidgetCenter.shared.reloadAllTimelines() }

    /// 화면 캡처용 실행 인자 (Debug 빌드만): -cd.reset YES, -cd.seed YES, -cd.style light|dark
    private static func applyTestArguments(_ store: LifeStore) {
        #if DEBUG
        let args = UserDefaults.standard
        if args.bool(forKey: "cd.reset") { store.eraseAll() }
        if args.bool(forKey: "cd.seed"), store.profile == nil {
            let birth = LifeCalendar.current.date(from: DateComponents(year: 1994, month: 6, day: 15)) ?? .now
            store.profile = LifeProfile(birthDate: birth, countryCode: "KR", sex: .other, customExpectancy: nil)
        }
        #endif
    }

    /// 화면 캡처용 강제 모드. 평소에는 nil(기기 설정을 따름).
    static var forcedColorScheme: ColorScheme? {
        #if DEBUG
        switch UserDefaults.standard.string(forKey: "cd.style") {
        case "dark": return .dark
        case "light": return .light
        default: return nil
        }
        #else
        return nil
        #endif
    }
}

struct RootView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        GeometryReader { geo in
            Group {
                if let profile = model.profile {
                    HomeView(profile: profile)
                } else {
                    OnboardingView()
                }
            }
            .environment(\.deviceClass, DeviceClass(width: geo.size.width))
            .preferredColorScheme(AppModel.forcedColorScheme)
        }
    }
}
