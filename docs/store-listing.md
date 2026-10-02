# 스토어 문안

## 한국어

- **앱 이름 (30자):** 하루의 정원 · 남은 날과 오늘의 한 줄
- **짧은 설명 (80자):** 남은 날을 조용히 세고, 오늘의 마음을 한 줄로 떠나보내는 작은 정원. 모든 기록은 여기에만 머물러요.

**긴 설명**

세상에 하나뿐인 돌, 하루
처음 열면 나만의 조약돌 ‘하루’를 만나요. 하루는 인생의 길 위, 지금 내가 있는 자리에 앉아 있어요. 쓰다듬으면 웃고, 가족 · 반려동물의 돌과 나란히 앉아요.

남은 시간과 인생 달력
기대수명까지 남은 날을 일 · 주 · 개월 · 년으로. 인생 전체를 작은 조약돌 달력으로 한눈에 봐요.

한지로 접은 정원
나무 · 말뚝 · 연 · 이끼가 함께한 날만큼 천천히 자라요. 계절마다 한 장씩 앨범에 모이고, 12월 마지막 주엔 한 해를 한 장으로 돌아봐요.

진짜 하늘
고른 나라의 실제 해 뜨고 지는 시각을 따라 하늘빛이 바뀌고, 달도 그날 밤 모양 그대로 떠요. 스물넷 절기마다 정원에 첫서리 · 꽃잎 · 눈송이 같은 작은 변화가 하루 머물러요.

오늘의 한 줄
기쁨도 슬픔도 한 줄에 실어 떠나보내요. 보낸 마음은 그 달의 별자리를 따라 ‘마음의 기록’으로 놓이고 (낮엔 꽃, 밤엔 별), 계절마다 ‘계절의 편지’로 조용히 돌아와요.

숨, 쉼 · 돌멍하기
마음 물결 · 마음 산책 · 마음 등불 · 마음 꽃밭. 마친 숨은 그날 정원에 발자국 · 꽃 · 따뜻한 등빛으로 남아요. 돌멍하기에선 구름 · 연못 · 화톳불과 계절의 소리를 오래 바라봐요.

곁에 있는 사람들
가족의 생일엔 카드 한 장, 계절마다 돌 곁에 조각 하나. 곁을 떠난 가족은 기억의 돌로, 하늘의 작은 별로 남아요.

홈 화면에서도
남은 날 · 오늘 · 인생 달력 · 가족의 정원 · 마음의 기록 위젯, 누르면 바로 시작하는 숨 바로가기와 빠른 설정 타일.

조용한 앱
계정도, 광고도, 서버도 없어요. 모든 기록은 여기에만 머물러요.

## English

- **App name:** Carpe Diem · Days Left & Today’s Line
- **Short description:** Quietly count the days left, and let today’s feeling go in a single line. Everything stays right here.

**Full description**

Haru, a stone like no other
Meet your own pebble, Haru. It sits on the path of your life, right where you are now. Tap it and it smiles; the stones of your family and pets sit beside it.

Time left and a life calendar
Days, weeks, months or years left. Your whole life as a small calendar of pebbles.

A garden folded from hanji paper
A tree, a post, a kite and moss grow slowly with the days you spend together. One piece gathers in the album each season, and in the last week of December the whole year fits on one page.

The real sky
The light follows the actual sunrise and sunset of the country you choose, and the moon rises in that night's shape. On each of the 24 solar terms, something small stays in the garden for the day: first frost, petals, a few snowflakes.

Today's line
Send joy or sorrow off in a single line. What you sent settles along the month's constellation — flowers by day, stars by night — and comes back each season as a letter.

Breathe, rest, and gaze
Ripple, walk, lantern and flower-field breathing. Each finished breath leaves footprints, flowers or a warmer lantern in the garden that day. Gaze mode lets you watch clouds, a pond or a small fire with the sounds of the season.

The people beside you
A card for family birthdays, a seasonal piece left beside a stone. Those who are gone stay as memory stones and small stars in the sky.

On your home screen
Days left, today, life calendar, family garden and mood record widgets, plus a breath shortcut and a quick settings tile that start right away.

A quiet app
No account, no ads, no server. Everything stays right here.

## 스크린샷 6장 (CI 캡처에서)

만든 그림: `docs/store/` (스크린샷 1080 × 1920 여섯 장, 그래픽 이미지 1024 × 500 한국어 · 영어). 다시 만들기: `python3 scripts/build_store.py <screenshots-android 클론>`

| # | 캡처 | 위에 얹을 한 줄 |
|---|---|---|
| 1 | g03_home | 남은 날을 조용히 세는 정원 |
| 2 | g02_meet | 세상에 하나뿐인 돌, 하루 |
| 3 | g36_care | 기쁨도 슬픔도 한 줄에 실어 보내요 |
| 4 | g49_record_month | 그 달의 별자리를 따라 피는 마음 |
| 5 | g26_breath_in | 하루를 여는 숨, 하루를 마무리하는 명상 |
| 6 | g54_birthday_eve | 가족의 돌과 함께, 밤에는 별빛 아래 |

## 데이터 보안 (Play Console 설문 답)

| 질문 | 답 |
|---|---|
| 사용자 데이터를 수집하거나 공유하나요? | 아니요 (모든 기록은 기기 안에만, 서버 없음) |
| 데이터가 전송 중에 암호화되나요? | 해당 없음 (보내는 데이터 없음) |
| 사용자가 데이터 삭제를 요청할 수 있나요? | 앱 안에서 직접 지움 (설정 → 모든 기록 지우기, 앱 삭제) |
| 위치 | 쓰지 않음 (나라의 대표 도시로 해 · 달 계산) |
| 결제 | Google Play 가 처리 (개발자는 결제 정보를 받지 않음) |
| 광고 | 없음 |
| 대상 연령 | 13세 이상 |
| 개인정보처리방침 | https://graviton94.github.io/privacy/ |
