import XCTest

/// 실제 앱 화면을 찍어 SNAPSHOT_DIR 에 저장한다. (.github/workflows/screenshots.yml)
/// 평소 테스트에서는 건너뛴다.
final class ScreenshotTests: XCTestCase {
    private var dir: URL!

    override func setUpWithError() throws {
        guard let path = ProcessInfo.processInfo.environment["SNAPSHOT_DIR"], !path.isEmpty else { throw XCTSkip("SNAPSHOT_DIR 없음") }
        dir = URL(fileURLWithPath: path)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        continueAfterFailure = true
    }

    func testCaptureScreens() {
        let full = ProcessInfo.processInfo.environment["SNAPSHOT_FULL"] == "1"
        let configs: [(lang: String, region: String, style: String)] = full
            ? [("ko", "ko_KR", "light"), ("ko", "ko_KR", "dark"), ("en", "en_US", "light"), ("en", "en_US", "dark")]
            : [("ko", "ko_KR", "light")]
        for c in configs {
            let prefix = "\(c.lang)-\(c.style)"
            let base = ["-AppleLanguages", "(\(c.lang))", "-AppleLocale", c.region, "-cd.style", c.style]
            let app = XCUIApplication()

            app.launchArguments = base + ["-cd.reset", "YES"]
            app.launch()
            settle()
            save("\(prefix)-1-onboarding")
            app.terminate()

            app.launchArguments = base + ["-cd.seed", "YES"]
            app.launch()
            settle()
            save("\(prefix)-2-home")
            app.swipeUp(velocity: .slow)
            settle()
            save("\(prefix)-3-home-scrolled")
            app.swipeUp(velocity: .fast)
            app.swipeUp(velocity: .fast)
            settle()
            save("\(prefix)-4-home-bottom")

            let settings = app.buttons["settings"]
            if settings.waitForExistence(timeout: 3) {
                settings.tap()
                settle()
                save("\(prefix)-5-settings")
                let country = app.buttons["country"]
                if country.waitForExistence(timeout: 3) {
                    country.tap()
                    settle()
                    save("\(prefix)-6-country")
                }
            } else {
                XCTFail("설정 버튼을 찾지 못함 (\(prefix))")
            }
            app.terminate()
        }
    }

    private func settle() { Thread.sleep(forTimeInterval: 1.2) }

    private func save(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        try? shot.pngRepresentation.write(to: dir.appendingPathComponent("\(name).png"))
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
