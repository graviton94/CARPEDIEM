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
        }
    }
}
