# 정원 에셋 (시안 1)

코드로 그린 과슈풍 그림입니다. 이미지 생성 AI 없이 만들었고, 마음에 들지 않는 그림은 같은 이름의 파일로 바꾸면 됩니다.

- 원본 코드: `src/` (`paint.js` 붓 도구와 소품, `scenes.js` 배경과 장면, `engine.js` 하루 조약돌)
- 다시 굽기: `node scripts/build_art.js` (Playwright 필요)
- 빛은 모두 왼쪽 위. 하루(조약돌)는 파일로 굽지 않고 앱이 번호로 직접 그립니다. `jar.png` 와 `donation.jpg` 는 하루 자리를 비워 둡니다.

| 파일 | 크기 | 쓰임 |
|---|---|---|
| `prop_*.png` (20종) | 512 × 512, 투명 | 정원 소품. 매달리는 것(lantern · star · moon · bell)은 위쪽 가장자리에 줄 |
| `jar.png` | 600 × 750, 투명 | 유리병 정원 (하루 자리 비움) |
| `season_*.jpg` (4종) | 1170 × 2532 | 인생의 계절 배경 |
| `theme_*.jpg` (6종) | 1170 × 2532 | 테마 배경 |
| `donation.jpg` | 1800 × 1200 | 응원하기 화면 위쪽 그림 |
| `store_bg.jpg` | 1290 × 2796 | 스토어 스크린샷 배경 |

소품 목록: pot 새싹 화분 · lantern 종이 등불 · books 오래된 책 · teacup 찻잔 · chair 나무 의자 · star 종이 별 · mushroom 빨간 버섯 · globe 스노볼 · moon 초승달 · boat 종이배 · bell 놋쇠 방울 · acorn 도토리 · lavender 라벤더 · candle 초 · watch 회중시계 · pinecone 솔방울 · can 물뿌리개 · basket 사과 바구니 · shell 조개껍데기 · hourglass 모래시계
