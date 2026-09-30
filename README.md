# Carpe Diem

생년월일과 평균 기대수명으로 남은 시간을 보여주는 iOS 위젯 중심 앱.

## 진행 상황

| 단계 | 상태 |
|---|---|
| 디자인 | 확정 — 올리브 빛, 아이콘 엔소 · 궤도 |
| 오늘의 명언 데이터 | 주제에 맞게 26개 유지, 74개 교체 — 검토 대기 |
| iOS 앱 · 위젯 | 개발 중 — `ios/` |
| Android 앱 · 위젯 | iOS 출시 후 |
| 앱스토어 등록 | 개발 후 |

## 폴더

- `data/quotes.csv` — 오늘의 명언 100개 (번호, 한글, 영문)
- `docs/quotes.md` — 명언을 고르고 보여주는 규칙
- `docs/design.md` — 디자인 결정 사항
- `design/tokens.json` — 디자인 토큰 (크기·글자·색·모서리의 유일한 원본)
- `design/strings.json` — 화면 문구 (한국어·영어)
- `design/mockup.html` — 화면 시안 (브라우저로 열기)
- `ios/` — iOS 앱과 위젯 ([ios/README.md](ios/README.md))
- `scripts/` — 토큰·문구 코드 생성, 폰트, 아이콘
