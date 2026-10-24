# 하루의 정원 (Carpe Diem)

생년월일과 나라의 평균 기대수명으로 **남은 시간**을 조용히 보여 주는 앱이에요.
화면에는 한지로 그린 정원이 있고, 내 돌 **하루**와 가족 · 반려동물의 돌이 인생의 길 위에 앉아 있어요.
숫자로 다그치지 않고, 하루에 한 줄 · 숨 한 번 · 바라보기로 오늘을 살게 하는 것이 목표예요.

> **1.1.9 로 Google Play 정식 출시** (2026-10). 다음 패치는 1.1.10 ([docs/plan-1.1.10.md](docs/plan-1.1.10.md)). 스토어 문안 · 그림 · 소개 영상: [docs/store-listing.md](docs/store-listing.md).
> iOS 는 같은 계산 · 문구를 쓰지만 지금은 쉬고 있어요 (빌드는 직접 실행할 때만).

## 정원에서 할 수 있는 것

| 갈래 | 내용 |
|---|---|
| 남은 시간 | 남은 날 · 주 · 달 · 해, 인생 달력 (주 · 개월 · 년), 흐르는 시간 |
| 오늘의 문장 | 하루에 한 문장 (한글 · 영어), 누르면 다음 문장 |
| 오늘의 한 줄 | 열세 가지 마음 하나와 한 줄 (다섯 줄까지, 사진 한 장), 오늘의 질문, 마음의 기록 (낮엔 꽃 · 밤엔 별), 마음별로 다시 보기, 몇 해 전 오늘, 계절의 편지, 미래의 나에게, 하루 닫기 (한 줄 · 고마움 · 등불) |
| 처음 온 사람 | 소개 석 장, 첫 정원 둘러보기 (하나씩 비춤), 페이지마다 처음 한 번 안내, 설정에서 다시 보기 |
| 숨, 쉼 | 마음 물결 · 마음 산책 · 마음 등불 · 마음 꽃밭, 마친 숨은 그날 정원에 흔적으로 남음 |
| 돌멍하기 | 오래 바라보는 정원: 구름 · 새 · 반딧불, 봄 · 여름 연못 · 가을 · 겨울 화톳불, 계절의 소리, 잠들기 타이머 |
| 가족의 정원 | 가족 · 반려동물 돌, 생일 카드, 이번 계절의 조각 건네기, 기억의 돌과 하늘의 별 · 기억의 주 |
| 꾸밈 · 모은 것 | 자리 여섯 (나무 · 발치의 한 장 · 말뚝 · 연 · 이끼 · 봉오리) 이 쌓이는 날만큼 자람, 우연한 순간. 계절 조각 · 손님과 순간 · 별자리 12 · 절기 24 를 모아 돌 곁에 둠 |
| 하늘 | 나라 · 날짜로 실제 해 뜨고 지는 시각 (노을 · 어스름 · 밤 · 새벽), 실제 달 모양, 스물넷 절기의 작은 변화 |
| 추억 | 계절 앨범, 한 해를 한 장으로, 받은 편지 · 고마움 책, 그림으로 보내기 |
| 위젯 · 바로가기 | 위젯 둘 (‘정원’ 4×2 · ‘하루’ 2×2, 때를 따라 바뀜), 아이콘 길게 누르기 (한 줄 · 숨 · 돌멍하기 · 하루 닫기), 빠른 설정 타일 ‘숨, 쉼’ · ‘돌멍하기’ |
| 기록 옮기기 | 파일로 내보내기 · 불러오기 (사진 포함), 폰 백업 · 기기 간 이전 (클라우드는 한도 안에서 최근 사진부터, 1.1.10) |
| 알림 | 아침 문장 · 하루 정리 · 생일과 특별한 날 · 기억의 주. 할 일을 한 날은 쉬고, 오래 오지 않으면 늘지 않고 쉼 |

모든 기록은 폰 안에만 있어요. 서버가 없고, 밖으로 나가는 것은 직접 보낸 그림 한 장과 직접 고른 곳에 저장한 기록 파일뿐이에요 ([docs/privacy.md](docs/privacy.md)).

## 구조

| 폴더 | 내용 |
|---|---|
| `android/core/` | 계산만 하는 순수 Kotlin (기간 · 절기 · 해와 달 · 꾸밈 · 알림 규칙 …) 과 단위 시험 |
| `android/app/` | Jetpack Compose 앱 + Glance 위젯 ([android/README.md](android/README.md)) |
| `ios/` | SwiftUI 앱 · 위젯 (쉬는 중, [ios/README.md](ios/README.md)) |
| `design/tokens.json` | 크기 · 색 · 시간 · 움직임의 유일한 원본 |
| `design/strings.json` | 화면 문구 (한국어 · 영어, 해요체) |
| `design/art/` | 한지 그림 엔진 (코드로 그린 그림을 `scripts/build_art.js` 가 webp 로 구움) |
| `data/` | 문장 · 기대수명 (UN WPP 2024) · 질문 · 별자리 · 나라마다 대표 도시 (해 · 달 계산용) |
| `docs/` | 계획 ([plan.md](docs/plan.md)) · 디자인 · 흐름 · 시험 · 설치 · 출시 안내 |
| `scripts/` | 토큰 · 문구 코드 생성, 글꼴, 아이콘, 그림 굽기, 에뮬레이터 캡처 |

토큰 · 문구 · 데이터를 바꾸면 `python3 scripts/generate.py` (문구를 바꿨으면 `python3 scripts/build_fonts.py` 도).
생성 파일은 직접 고치지 않아요. CI 가 `generate.py --check` 로 확인해요.

## 빌드와 시험

| 워크플로 | 언제 | 하는 일 |
|---|---|---|
| `Android` | `main` 에 안드로이드 · 디자인 · 데이터가 바뀌어 푸시될 때 | core 시험 + APK 빌드 (`CarpeDiem-apk` 아티팩트) |
| `Android screenshots` | 직접 실행, 또는 커밋 메시지에 `[quick-shots]` · `[full-shots]` | 에뮬레이터 장면별 캡처 → `screenshots-android` (main) · `screenshots-android-dev` (다른 가지). 직접 실행 입력: `only` (장면만) · `quick` · `store` (스토어 그림, `scripts/build_store.py`) · `video` (소개 영상, `scripts/build_video.py`) · `backup` (백업 → 지우기 → 되살리기 확인) |
| `iOS` · `TestFlight` · `Screenshots` | 직접 실행할 때만 | iOS 빌드 · 배포 · 캡처 |
| `Android release` · `Life expectancy` | 직접 실행할 때만 | 스토어용 AAB (업로드 키가 Secrets 에 있을 때) · 기대수명 다시 받기 |
| `Android` (직접 실행) | 릴리즈할 때만 | APK 를 `android-latest` 릴리즈에 올림 (직접 설치용 키가 있을 때만) |
| `Android sideload key` | 한 번만 | 직접 설치용 새 서명 키를 만들어 잠근 채 저장소에 |

- core 만 내 컴퓨터에서: `cd android && ./gradlew :core:test`
- 직접 설치용 서명 키는 암호로 잠근 파일만 저장소에, 그 암호는 GitHub Secrets 에만 있어요 ([android/keystore/README.md](android/keystore/README.md)).
- APK 설치: [docs/android-install.md](docs/android-install.md) · 시험 순서: [docs/test-guide.md](docs/test-guide.md)

## 진행 상황

| 단계 | 상태 |
|---|---|
| 1.1.4 ~ 1.1.8 | Play 비공개 테스트 트랙 (노트: `docs/plan-1.1.*.md`, `design/changelog.json`) |
| 1.1.9 | Google Play 정식 출시 (2026-10, 출시 노트 `docs/release-notes/production-1.1.9.txt`) |
| 1.1.10 | 준비 중: 클라우드 백업에 사진, 사진 사본 480 ([docs/plan-1.1.10.md](docs/plan-1.1.10.md)) |
| 응원하기 결제 | 판매자 프로필 연결 뒤 |
| iOS | 쉬는 중 |
