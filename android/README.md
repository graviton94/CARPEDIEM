# Carpe Diem — Android

Jetpack Compose 앱 + Glance 위젯. Android 8.0(API 26) 이상.

| 모듈 | 내용 |
|---|---|
| `core/` | 계산 로직 (순수 Kotlin) — 기간 계산, 명언 순서, CSV, 기대수명 표. iOS 와 같은 테스트 예시 |
| `app/` | 앱 화면(온보딩 · 홈 · 설정 · 나라 선택), 위젯(남은 날 2×2 · 오늘 2×2 · 인생 달력 4×2/4×4) |

- `app/.../design/Tokens.kt`, `res/values*/strings.xml`, `assets/*.csv` 는 `scripts/generate.py` 가 만듭니다. 직접 고치지 마세요.
- 폰트 · 아이콘: `scripts/build_fonts.py`, `scripts/build_icon.py`
- 빌드: GitHub Actions `Android` 워크플로 → APK 가 `android-latest` 릴리스에 올라감. 설치법은 [docs/android-install.md](../docs/android-install.md)
- 로컬: Android Studio 로 `android/` 폴더를 열면 됩니다. SDK 가 없으면 `core` 만 빌드됩니다.
