# Carpe Diem — Android

Jetpack Compose 앱 + Glance 위젯. Android 8.0(API 26) 이상.

| 모듈 | 내용 |
|---|---|
| `core/` | 계산 로직 (순수 Kotlin) — 기간 · 문장 순서 · CSV · 기대수명, 가족 돌 자리, 꾸밈 · 우연한 순간, 절기 · 해와 달 (`Sky.kt`), 알림 규칙 · 기억의 주 · 조각 (`Rhythm.kt`). 단위 시험 `core/src/test` |
| `app/` | 한지 정원 (`ui/garden/`), 숨 · 돌멍하기 소리 (`sound/`), 알림 (`notify/`), 위젯 · 빠른 설정 타일 (`widget/`), 그림 보내기 (`share/`) |

- `app/.../design/Tokens.kt`, `res/values*/strings.xml`, `assets/*.csv` 는 `scripts/generate.py` 가 만듭니다. 직접 고치지 마세요.
- 폰트 · 아이콘 · 한지 그림: `scripts/build_fonts.py`, `scripts/build_icon.py`, `scripts/build_art.js`
- 빌드: `main` 에 푸시하면 GitHub Actions `Android` 워크플로가 시험 + APK 빌드 (`CarpeDiem-apk` 아티팩트). 설치법은 [docs/android-install.md](../docs/android-install.md)
- 캡처: 커밋 메시지에 `[quick-shots]` (빠른 장면) · `[full-shots]` (전체), `[only q26 q31]` 로 장면 고르기. 장면 목록은 `scripts/android_shots.sh`
- 시험용 장면 열기 (debug): `adb shell am start -n io.github.graviton94.carpediem/.MainActivity --es cd.now 2026-12-22T19:30 --es cd.term dongji` 처럼 `cd.*` 값으로
- 로컬: Android Studio 로 `android/` 폴더를 열면 됩니다. SDK 가 없으면 `core` 만 빌드됩니다.
