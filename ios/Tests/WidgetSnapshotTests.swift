import SwiftUI
import WidgetKit
import XCTest
@testable import CarpeDiem

/// 위젯을 기기별 실제 위젯 크기로 그려 SNAPSHOT_DIR/widgets 에 저장한다.
/// 위젯 코드를 그대로 쓰되, 홈 화면이 주는 배경·모서리·안쪽 여백(16pt)은 여기서 흉내 낸다.
@MainActor
final class WidgetSnapshotTests: XCTestCase {
    private struct Device { let name: String; let small: CGFloat; let medium: CGSize; let large: CGSize }
    private let devices = [
        Device(name: "SE", small: 148, medium: CGSize(width: 321, height: 148), large: CGSize(width: 321, height: 324)),
        Device(name: "12mini", small: 155, medium: CGSize(width: 329, height: 155), large: CGSize(width: 329, height: 345)),
        Device(name: "16", small: 158, medium: CGSize(width: 338, height: 158), large: CGSize(width: 338, height: 354)),
        Device(name: "ProMax", small: 170, medium: CGSize(width: 364, height: 170), large: CGSize(width: 364, height: 382)),
    ]

    func testRenderWidgets() throws {
        guard let path = ProcessInfo.processInfo.environment["SNAPSHOT_DIR"], !path.isEmpty else { throw XCTSkip("SNAPSHOT_DIR 없음") }
        let dir = URL(fileURLWithPath: path).appendingPathComponent("widgets")
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        let entry = LifeEntry.sample

        for dark in [false, true] {
            let mode = dark ? "dark" : "light"
            for d in devices {
                let s = CGSize(width: d.small, height: d.small)
                try render(DaysLeftView(entry: entry, familyOverride: .systemSmall), s, dark, dir, "\(mode)-\(d.name)-small-daysleft")
                try render(TodayView(entry: entry, familyOverride: .systemSmall), s, dark, dir, "\(mode)-\(d.name)-small-today")
                try render(LifeCalendarView(entry: entry, familyOverride: .systemMedium), d.medium, dark, dir, "\(mode)-\(d.name)-medium-calendar")
                try render(LifeCalendarView(entry: entry, familyOverride: .systemLarge), d.large, dark, dir, "\(mode)-\(d.name)-large-calendar")
            }
        }
        try render(DaysLeftView(entry: entry, familyOverride: .accessoryCircular), CGSize(width: 76, height: 76), true, dir, "lock-circular", accessory: true)
        try render(DaysLeftView(entry: entry, familyOverride: .accessoryRectangular), CGSize(width: 172, height: 76), true, dir, "lock-rectangular", accessory: true)
        try render(DaysLeftView(entry: entry, familyOverride: .accessoryInline), CGSize(width: 234, height: 26), true, dir, "lock-inline", accessory: true)
    }

    private func render<V: View>(_ view: V, _ size: CGSize, _ dark: Bool, _ dir: URL, _ name: String, accessory: Bool = false) throws {
        let radius = 22 * size.height / 158
        let framed = Group {
            if accessory {
                view.frame(width: size.width, height: size.height).foregroundStyle(.white).background(Color(white: 0.25))
            } else {
                view
                    .padding(Tokens.Layout.widgetPadding)
                    .frame(width: size.width, height: size.height)
                    .background(WidgetSurface())
                    .clipShape(RoundedRectangle(cornerRadius: min(radius, 24), style: .continuous))
                    .padding(Tokens.Space.sp3)
                    .background(Color(white: dark ? 0.08 : 0.72))
            }
        }
        .environment(\.colorScheme, dark ? .dark : .light)

        let renderer = ImageRenderer(content: framed)
        renderer.scale = 3
        let image = try XCTUnwrap(renderer.uiImage, name)
        try XCTUnwrap(image.pngData()).write(to: dir.appendingPathComponent("\(name).png"))
    }
}
