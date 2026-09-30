# Carpe Diem — iOS

SwiftUI 앱 + WidgetKit 위젯. iOS 17 이상, 아이폰 세로.

## 실행 (Mac)

1. Xcode 설치 후 한 번 실행
2. `Xcode 프로젝트 열기.command` 더블클릭 → `CarpeDiem.xcodeproj` 생성 후 Xcode가 열림
3. `CarpeDiem` 스킴 + 시뮬레이터 선택 → ▶︎
4. 실기기: `Config.xcconfig`의 `APP_BUNDLE_ID`, `DEVELOPMENT_TEAM`을 채운 뒤 2번 다시

## 구조

| 폴더 | 내용 |
|---|---|
| `App/` | 앱 화면 — 온보딩, 홈, 설정, 나라 선택 |
| `Widget/` | 위젯 — 남은 날(작게 · 잠금 화면 3종), 오늘(작게), 인생 달력(중간 · 크게) |
| `Shared/Design/` | `Tokens.swift`, `L10n.swift`(자동 생성), `Theme.swift` |
| `Shared/Model/` | 계산(`LifeSnapshot`), 저장(`LifeStore`), 명언(`QuoteBook`), 기대수명 표 |
| `Shared/Components/` | 배경 빛, 유리 카드, 진행 막대, 인생 달력, 엔소 표시 |
| `Shared/Resources/` | 폰트, 명언·기대수명 CSV, 한/영 문구 (일부 자동 생성) |
| `Tests/` | 계산 · 명언 순서 · 데이터 · 문구 · 폰트 테스트 |

## 원본을 바꿨을 때

| 바꾼 것 | 실행 |
|---|---|
| `design/tokens.json`, `design/strings.json`, `data/*.csv` | `python3 scripts/generate.py` |
| 문구나 명언 (한글 글자가 늘었을 수 있음) | `python3 scripts/build_fonts.py` |
| 아이콘 | `python3 scripts/build_icon.py` |
| `project.yml`, `Config.xcconfig` | `Xcode 프로젝트 열기.command` 다시 실행 |

CI(`.github/workflows/ios.yml`)는 생성 파일이 최신인지 확인하고 시뮬레이터에서 빌드·테스트합니다.
