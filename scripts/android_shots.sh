#!/usr/bin/env bash
# 에뮬레이터에서 앱을 실행해 일반 사용자가 겪을 장면을 캡처한다 (.github/workflows/android-screens.yml).
# 디버그 빌드만 cd.* 실행 옵션을 읽는다 (MainActivity.debugSetup).
set -u
P=io.github.graviton94.carpediem
OUT=${1:-shots}
mkdir -p "$OUT"
NOW=2026-09-30T15:00
# ONLY="q26 q31" 이면 그 장면만 찍음 (나머지는 상태만 맞추고 기다리지 않음)
# ONLY 의 장면을 다 찍었으면 나머지 장면 (상태 맞추기 포함) 은 건너뛰고 바로 마무리 (느린 에뮬레이터가 멈추기 전에)
LEFT=" ${ONLY:-} "
shot() { if [ -n "${ONLY:-}" ]; then case " $ONLY " in *" ${1%%_*} "*) ;; *) return 0;; esac; fi; sleep "$2"; adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"
  if [ -n "${ONLY:-}" ]; then LEFT="${LEFT/ ${1%%_*} / }"; [ -z "${LEFT// /}" ] && { finish; exit 0; }; fi; }
open() { adb shell am force-stop $P; adb shell am start -W -n $P/.MainActivity "$@" >/dev/null; }
# 찍을 장면인지 (ONLY 가 비면 전부) · 여럿 중 하나라도
wantany() { local n; for n in "$@"; do want "$n" && return 0; done; return 1; }
want() { [ -z "${ONLY:-}" ] && return 0; case " $ONLY " in *" ${1%%_*} "*) return 0;; esac; return 1; }
# 보기만 바꾸는 옵션 (폰에 남는 상태가 없음): ONLY 로 고른 장면이 아니면 이런 장면은 열지도 않고 건너뜀 (앱을 껐다 켜는 시간 · 느린 에뮬레이터 멈춤을 아낌)
VIEW=" cd.now cd.screen cd.page cd.stoneId cd.chance cd.term cd.title cd.openMonth cd.openYear cd.openLetter cd.adornSheet cd.care cd.guest cd.news cd.update cd.creditsYear cd.slip cd.sleepSheet cd.letterNote "
# s 장면 기다릴초 [열기 옵션…]: 고른 장면이면 열고 찍음. 아니면 상태를 바꾸는 옵션이 있을 때만 열어 두고 (다음 장면이 기대니까), 보기만 하는 장면은 건너뜀
s() { local n=$1 w=$2 a; shift 2
  if want "$n"; then open "$@"; shot "$n" "$w"; return; fi
  for a in "$@"; do case "$a" in cd.*) case "$VIEW" in *" $a "*) ;; *) open "$@"; return;; esac;; esac; done; }
swipe_up() { adb shell input swipe 540 1900 540 500 500; }

adb shell settings put global window_animation_scale 1; adb shell settings put global animator_duration_scale 1
# 느린 에뮬레이터의 'System UI 응답 없음' 창이 화면을 가리지 않게. 부팅 직후 잠시 쉰다.
adb shell settings put global hide_error_dialogs 1
sleep 45; adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null
# 기본 언어는 한국어 (영어는 아래에서 따로)
adb shell cmd locale set-app-locales $P --locales ${LOCALES:-ko-KR} 2>/dev/null   # [locale ja-JP] 처럼 고르면 그 말로

quick_scenes() {
# 부팅 직후 느린 에뮬레이터: 한 번 열어 데워 둠 (첫 화면이 ‘응답 없음’으로 닫히지 않게)
open --es cd.now $NOW; sleep 25
# 한지 정원의 자리 여섯: 처음 (새싹 · 빈 말뚝) → 모두 자란 모습 (preview) 을 실제 계절 넷 · 밤 · 가족 · 인생의 가을로
s q00_decor_first 8 --ez cd.reset true --es cd.design garden --el cd.seed 2718281 --es cd.birth 2000-05-12 --es cd.sex female --ez cd.meet false --es cd.now $NOW
s q01_decor_autumn 6 --ez cd.preview true --es cd.now $NOW
s q02_decor_night 6 --ez cd.preview true --es cd.now 2026-09-30T22:40
s q03_decor_spring 6 --ez cd.preview true --es cd.now 2026-04-15T15:00
s q04_decor_summer 6 --ez cd.preview true --es cd.now 2026-07-20T15:00
s q05_decor_winter 6 --ez cd.preview true --es cd.now 2027-01-12T14:00
s q06_decor_family_winter 6 --ez cd.preview true --ez cd.family true --es cd.now 2027-01-12T14:00
adb shell input tap 90 1650; shot q07_decor_sheet 3
s q08_decor_ginkgo 6 --ez cd.preview true --es cd.birth 1962-03-02 --es cd.now $NOW
s q09_album 5 --ez cd.preview true --es cd.screen collection --es cd.now $NOW
# 우연한 순간 (캡처용으로 바로 띄움): 몇 초 뒤 모습
s q10_chance_butterflies 7 --ez cd.preview false --es cd.birth 2000-05-12 --es cd.chance butterflies --es cd.now 2026-05-20T15:00
s q11_chance_rainbow 5 --es cd.chance rainbow --es cd.now $NOW
s q12_chance_wind 3 --es cd.chance wind --es cd.now $NOW
s q13_chance_bubbles 4 --es cd.chance bubbles --es cd.now $NOW
s q14_chance_fireflies 5 --es cd.chance fireflies --es cd.now 2026-09-30T22:40
s q15_chance_aurora 5 --es cd.chance aurora --es cd.now 2027-01-12T22:40
s q16_chance_snail 5 --es cd.chance snail --es cd.now $NOW
s q17_gaze_autumn 8 --es cd.screen gaze --es cd.now 2026-10-02T15:00
s q18_gaze_autumn_night 8 --es cd.screen gaze --es cd.now 2026-10-02T22:40
s q19_gaze_spring 8 --es cd.screen gaze --es cd.now 2026-04-15T15:00
s q20_gaze_summer_night 8 --es cd.screen gaze --es cd.now 2026-07-20T22:40
# 잠들기 (1.1.5): 분 바퀴 시트
s q38_gaze_sleep 8 --es cd.screen gaze --ez cd.sleepSheet true --es cd.now 2026-10-02T22:40
s q21_breath_intro 2 --es cd.screen breath --es cd.now 2026-10-03T07:30
s q22_breath_ripple 9 --es cd.screen ripple --es cd.now 2026-10-03T15:00
s q23_breath_walk 10 --es cd.screen walk --es cd.now 2026-10-03T15:00
s q24_breath_lantern 9 --es cd.screen lantern --es cd.now 2026-10-02T22:40
s q25_breath_flowers 40 --es cd.screen thanks --es cd.now 2026-10-03T15:00
# 절기의 작은 변화 (S1, cd.term) · 실제 해 시각 (S3: 12월 오후 5시 반은 어스름) · 달 모양 · 한 해 한 장 (S2, 12월 마지막 주)
s q26_term_sanggang 6 --ez cd.preview false --es cd.term sanggang --es cd.now 2026-10-23T15:00
s q27_term_dongji 6 --es cd.term dongji --es cd.now 2026-12-22T19:30
s q28_term_blossom 8 --es cd.term chunbun --es cd.now 2026-04-15T15:00
s q29_term_snow 8 --es cd.term soseol --es cd.now 2027-01-12T14:00
s q30_sky_december_1730 5 --es cd.now 2026-12-10T17:30
s q31_moon_crescent 5 --es cd.now 2026-10-14T21:00
s q32_moon_full 5 --es cd.now 2026-10-26T22:00
s q33_year_note 5 --es cd.now 2026-12-27T15:00
# 돌에게 건넨 조각 (R1) · 기억의 주에 밝아진 별 (R2: 보리가 떠난 날 12월 20일 앞뒤 사흘)
open --ez cd.family true --es cd.now 2026-10-05T15:00; sleep 5
s q34_offer 6 --ez cd.offer true --es cd.supported support_coffee --es cd.now 2026-10-05T15:00
s q35_offer_stone 3 --es cd.screen stone --es cd.stoneId mom00001 --es cd.now 2026-10-05T15:00; sleep 3; swipe_up
s q37_adorn_sheet 5 --es cd.screen stone --es cd.stoneId mom00001 --ez cd.adornSheet true --es cd.now 2026-10-05T15:00
s q36_memory_week 6 --ez cd.memory true --es cd.now 2026-12-20T21:30
# 위젯 미리보기 그림 (위젯 고르는 화면용): 가족 · 이번 달 기록이 있는 정원으로 실제 위젯을 그려 꺼냄
open --ez cd.reset true --es cd.design garden --el cd.seed 2718281 --es cd.birth 2000-05-12 --es cd.sex female --ez cd.meet false --ez cd.family true --ez cd.months true --ez cd.widgetShots true
W=/sdcard/Android/data/$P/files/widgets
for i in $(seq 1 90); do adb shell ls $W/done >/dev/null 2>&1 && break; sleep 2; done
mkdir -p "$OUT/widgets"; adb pull $W/. "$OUT/widgets/" >/dev/null 2>&1; rm -f "$OUT/widgets/done"; ls "$OUT/widgets"
}
finish() {
# 오류 확인
adb logcat -d -s AndroidRuntime:E chromium:E > "$OUT/logcat.txt" || true
# 멈춤 · 느린 첫 화면 살피기: ANR · 앱 쪽 경고 이상
adb logcat -d ActivityManager:W ActivityTaskManager:W Choreographer:I OpenGLRenderer:W "*:S" > "$OUT/logcat_app.txt" || true
adb logcat -d | grep -iE "carpediem|ANR in" | tail -300 >> "$OUT/logcat_app.txt" || true
# 앱이 멈춘 횟수 (0 이어야 함): 느린 첫 화면 · 무거운 그리기를 잡는다
echo "app ANR: $(grep -c "ANR in $P" "$OUT/logcat_app.txt")" > "$OUT/anr.txt"; cat "$OUT/anr.txt"
# 멈춘 순간 메인 스레드가 어디 있었는지 (ANR 기록). 루트가 되는 에뮬레이터 이미지에서만
adb root >/dev/null 2>&1; sleep 3
mkdir -p "$OUT/anr"; adb shell ls /data/anr 2>/dev/null | tr -d '\r' | while read -r f; do adb pull "/data/anr/$f" "$OUT/anr/" >/dev/null 2>&1; done
# 우리 앱 것만 남김
for f in "$OUT"/anr/*; do grep -q "Cmd line: $P" "$f" 2>/dev/null || rm -f "$f"; done
ls "$OUT/anr" 2>/dev/null | head
ls -la "$OUT"
}

# STORE=1: 스토어 그림용 장면만, 네 말로 한 번에 → $OUT/store/<말>/ (scripts/build_store.py)
store_scenes() {
for loc in ko-KR en-US ja-JP zh-TW; do
  adb shell cmd locale set-app-locales $P --locales $loc 2>/dev/null
  local base=$OUT; OUT=$base/store/$loc; mkdir -p "$OUT"
  open --es cd.now $NOW; sleep 20
  open --ez cd.reset true --es cd.design garden --el cd.seed 2718281 --es cd.birth 2000-05-12 --es cd.sex female --ez cd.meet true --es cd.now $NOW; shot g02_meet 14
  open --ez cd.meet false --ez cd.typed true --es cd.now $NOW;                      shot g03_home 10
  open --ez cd.preview true --ez cd.typed true --es cd.now $NOW;                   shot g04_home_all 8
  open --ez cd.preview false --es cd.now $NOW; sleep 3
  open --es cd.screen breath --es cd.now $NOW;                                      shot g26_breath_in 8
  open --es cd.care CALM_BREATH --es cd.now $NOW;                                  shot g36_care 14
  open --ez cd.family true --es cd.now $NOW; sleep 4
  open --ez cd.typed true --es cd.now 2026-09-29T21:00;                             shot g54_birthday_eve 10
  open --ez cd.months true --ez cd.openMonth true --es cd.now 2026-09-28T15:00;  shot g49_record_month 6
  open --ez cd.openMonth true --es cd.now 2026-09-28T22:30;                        shot g50_record_month_night 6
  OUT=$base
done
}
if [ -n "${STORE:-}" ]; then store_scenes; finish; exit 0; fi

# QUICK=1 ([quick-shots] 커밋): 타자기 · 위젯 미리보기만 (5분 남짓)
if [ -n "${QUICK:-}" ]; then quick_scenes; finish; exit 0; fi
# ONLY 가 q 장면뿐이면 정원 흐름은 통째로 건너뛰고 q 장면 (스스로 상태를 맞춤) 만
if [ -n "${ONLY:-}" ] && ! echo " $ONLY " | grep -qE ' [^q ][^ ]*'; then quick_scenes; finish; exit 0; fi

# 정원: 처음 켜기 → 하루를 만남 → 홈
s g00_intro 4 --ez cd.reset true --ez cd.guide true --es cd.design garden --es cd.now $NOW
# 처음 시작하기 → 소개 장 (오른쪽 위 ‘건너뛰기’ 종이 칩)
if want g00b; then open --ez cd.reset true --ez cd.guide true --es cd.design garden --es cd.now $NOW; sleep 5; adb shell input tap 540 1950; shot g00b_intro_skip 4; fi
s g01_onboarding 5 --ez cd.reset true --es cd.design garden --es cd.now $NOW
s g02_meet 14 --ez cd.reset true --es cd.design garden --el cd.seed 2718281 --es cd.birth 2000-05-12 --es cd.sex female --ez cd.meet true --es cd.now $NOW
s g03_home 6 --ez cd.meet false --es cd.now $NOW
# 돌 별 꾸밈 (1.1.3): 가족 넷 · 엄마 곁 계절 조각 · 콩이 곁 조약돌 · 하루 곁 커피 → 정원 · 엄마 돌 페이지 · 고르기
open --ez cd.family true --es cd.now 2026-10-05T15:00; sleep 4
s a01_adorn_garden 6 --ez cd.offer true --es cd.supported support_coffee --es cd.now 2026-10-05T15:00
s a02_adorn_stone 3 --es cd.screen stone --es cd.stoneId mom00001 --es cd.now 2026-10-05T15:00; sleep 3; swipe_up
s a03_adorn_sheet 5 --es cd.screen stone --es cd.stoneId mom00001 --ez cd.adornSheet true --es cd.now 2026-10-05T15:00
open --ez cd.recall true --es cd.now 2026-10-04T10:00; sleep 5
s a04_slip_letter 8 --ez cd.recall true --ez cd.slip true --es cd.now 2026-10-04T10:00
s a05_support 5 --es cd.screen support --es cd.now 2026-10-05T15:00
# 1.1.5 응원: 같은 응원 두 번 → 커피 두 잔 (엄마 곁 고르기에 따로) · 응원 페이지 미리 보기 (이미 커피를 응원한 사람)
s a07_adorn_two 6 --ez cd.family true --es cd.supported support_coffee,support_coffee --es cd.screen stone --es cd.stoneId mom00001 --ez cd.adornSheet true --es cd.now 2026-10-05T15:00
s a08_support_preview 5 --es cd.screen support --es cd.now 2026-10-05T15:00
# 기념일 편지 (백 번째 한 줄 · 만난 지 1년)
s a09_anniv_100 6 --es cd.letterNote 100 --es cd.now 2026-10-05T10:00
s a10_anniv_year 6 --es cd.letterNote year --es cd.now 2026-10-05T10:00
# 화면이 짧은 폰 (세 버튼 내비게이션 등): 땅이 탭 위까지 이어지는지 · 권유 쪽지의 ×
if wantany a06_short_ground; then adb shell wm size 1080x2100; open --ez cd.reset true --es cd.design garden --el cd.seed 2718281 --es cd.birth 2000-05-12 --es cd.sex female --ez cd.meet false --es cd.update available --es cd.now 2026-10-05T15:00; shot a06_short_ground 8; adb shell wm size reset; fi
open --ez cd.reset true --es cd.design garden --el cd.seed 2718281 --es cd.birth 2000-05-12 --es cd.sex female --ez cd.meet false --es cd.now $NOW; sleep 6
# 처음 온 사람의 둘러보기: 첫 장 → 한 번 눌러 둘째 장 (남은 시간을 비춤)
s g03b_guide 6 --ez cd.guide true --es cd.now $NOW
adb shell input tap 540 250; shot g03c_guide_number 3
s g04_home_all 5 --ez cd.preview true --es cd.now $NOW
s g05_write 3 --ei cd.page 1 --es cd.now $NOW
s g06_memories 3 --ei cd.page 2 --es cd.now $NOW
s g06_flow 3 --ei cd.page 3 --es cd.now $NOW
s g07_morning 5 --ez cd.preview false --es cd.now 2026-09-30T07:10
s g08_night_late 8 --es cd.now 2026-09-30T23:30;                                                                     shot g08_night 5
s g09_settings 4 --es cd.screen settings --es cd.now $NOW
swipe_up; shot g10_settings_more 3
s g11_widgets 6 --es cd.screen widgets --es cd.now $NOW
s g16_collection 5 --ez cd.preview true --es cd.screen collection --es cd.now $NOW
s g17_support 6 --ez cd.preview false --es cd.screen support --es cd.now $NOW
adb shell input tap 540 1700; shot g18_support_tap 2
s g21_letgo_recall 4 --es cd.now $NOW --ez cd.recall true --ei cd.page 1
# 가족의 정원 (엄마는 오늘 생일): 홈 · 돌의 페이지 · 돌 더하기
s g22_family 5 --ez cd.family true --es cd.now $NOW
s g23_stone 4 --es cd.screen stone --es cd.now $NOW
s g24_add 3 --es cd.screen add --es cd.now $NOW
s g35_family9_late 10 --ez cd.family true --ez cd.full true --es cd.now $NOW;                                         shot g35_family9 5
# 그 순간 앱의 모든 스레드 스택 (멈춤 원인 찾기): kill -3 → /data/anr
adb shell kill -3 "$(adb shell pidof $P | tr -d '\r')" 2>/dev/null; sleep 3
s g25_family_night 5 --es cd.now 2026-09-30T23:10
s g27_breath_later 5 --es cd.screen breath --es cd.now $NOW;                                                           shot g26_breath_in 3
s g28_gaze_night 5 --es cd.screen gaze --es cd.now 2026-09-30T23:10
s g19_dusk 5 --es cd.now 2026-09-30T18:20
s g20_dawn 5 --es cd.now 2026-09-30T05:20
# 1.3 깨닫기: 오늘의 질문 · 마음의 하늘 · 계절의 편지 (12월 2일)
s g29_question 5 --ez cd.question true --es cd.now $NOW
s g30_mood 5 --ez cd.moods true --ei cd.page 1 --es cd.now $NOW
s g31_letter 5 --ez cd.letter true --es cd.now 2026-12-02T10:00
s g32_letter_open 4 --ez cd.wish true --ez cd.openLetter true --es cd.now 2026-12-02T10:00
s g33_letter_night 4 --ez cd.letter true --ez cd.openLetter true --es cd.now 2026-12-02T21:30
s g34_letters 3 --es cd.screen collection --es cd.now 2026-12-02T10:00; swipe_up; swipe_up
# 1.4 돌봄: 한마디 창 아래 권유 · 잠깐 바라보기 · 고마움 책
s g36_care 14 --es cd.care CALM_BREATH --es cd.now $NOW
s g37_look 12 --es cd.screen look --es cd.now $NOW
# 1.4 기억의 돌: 기억의 자리 (모은 것 안) · 하늘의 별 (밤)
s g38_memory 6 --ez cd.memory true --es cd.screen memory --es cd.now $NOW
s g39_memory_star 8 --ez cd.memory true --es cd.now 2026-09-30T22:40
# 1.5 감각: 마음의 날씨 (오늘 슬픔 → 비 · 다음 날 기쁨 → 햇살) · 아침의 숨 · 잠들기 전 정원
s g40_weather_rain 4 --es cd.today SAD --es cd.now 2026-10-01T15:00
s g41_weather_sun 4 --es cd.today JOY --es cd.now 2026-10-02T15:00
s g42_breath_morning 4 --es cd.screen breath --es cd.now 2026-10-03T07:30
s g43_sleepy 8 --es cd.now 2026-10-03T23:20
# 1.6 작은 의식: 고마움 숨 (내쉴 때) · 계절 첫날의 바람 · 한 해의 정원
s g44_thanks_breath 7 --es cd.screen thanks --es cd.now 2026-10-03T15:00
s g45_wish 4 --ei cd.page 1 --es cd.now 2026-12-03T10:00
s g46_year 6 --ez cd.year true --ez cd.openYear true --es cd.now 2026-12-31T15:00
# 1.7: 특별한 날 꽃 (인생 달력) · 엄마 생일의 돌 페이지 (생일 카드 보내기)
s g47_special 5 --ez cd.special true --ei cd.page 3 --es cd.now $NOW
s g48_birthday_card 5 --es cd.screen stone --es cd.stoneId mom00001 --es cd.now $NOW
# 생일 전날 밤: 엄마의 돌이 먼저 고깔을 쓰고 “내일은 엄마 생일이에요”
s g54_birthday_eve 5 --es cd.now 2026-09-29T21:00
s g55_birthday_eve_stone 4 --es cd.screen stone --es cd.stoneId mom00001 --es cd.now 2026-09-29T21:00
# 하루를 열고 닫는 작은 의식: 아침 씨앗 (04) · 걱정한 밤 다음 아침 (06) · 하루 닫기 입구와 첫 걸음 (03) · 저녁의 ‘싹이 텄나요?’ · 손끝 숨 (05)
s g56_seed_card 5 --es cd.now 2026-10-03T07:40
s g57_comfort 5 --ez cd.comfort true --es cd.now 2026-10-03T08:10
s g58_closeday_entry 5 --es cd.now 2026-10-03T21:10
s g59_closeday_line 4 --es cd.screen close --es cd.now 2026-10-03T21:10
# 하루 닫기에서 한 줄을 쓰다 뒤로: ‘쓰던 한 줄을 지우고 나갈까요?’ (키보드 내리기 → 한 번 더 뒤로)
if want g59b; then open --es cd.screen close --es cd.now 2026-10-03T21:10; sleep 5; adb shell input tap 540 870; sleep 1; adb shell input text "good"; sleep 1; adb shell input keyevent 4; sleep 1; adb shell input keyevent 4; shot g59b_closeday_leave 2; fi
s g60_seed_ask 5 --ez cd.morningSeed true --es cd.today CALM --ei cd.page 1 --es cd.now 2026-10-03T20:30
s g61_breath_touch 4 --ez cd.touch true --es cd.screen breath --es cd.now 2026-10-03T15:00
open --ez cd.touch false --es cd.now $NOW; sleep 2
# 미래의 나에게 (10): 오늘 열린 항아리 · 생일 아침의 나이테 (07, 2000-05-12 생 → 2026-05-12 아침) · 추억의 항아리 · 나이테
s g62_capsule_open 5 --ez cd.capsule true --es cd.now 2026-10-04T09:00
s g63_ring_note 6 --ez cd.ringYear true --es cd.now 2026-05-12T09:00
s g64_keepsakes 3 --ei cd.page 2 --es cd.now 2026-05-20T15:00; for i in 1 2 3 4 5 6 7 8 9; do swipe_up; done
# 돌아온 날의 손님 (09): 닷새 · 스무 날 만에
s g65_guest 5 --es cd.guest tit --es cd.now 2026-10-04T10:00
# 손님이 물고 온 한 줄 · 펼친 쪽지 · 하루가 준 조약돌
s g75_slip_guest 5 --ez cd.recall true --es cd.now 2026-10-04T10:00
s g76_slip_open 6 --ez cd.recall true --ez cd.slip true --es cd.now 2026-10-04T10:00
s g77_pebble 5 --ez cd.pebble true --es cd.now 2026-10-04T10:00
s g78_bday_card 5 --ez cd.family true --es cd.screen card --es cd.now 2026-10-04T10:00
s g79_news 5 --ez cd.news true --es cd.now 2026-10-04T10:00
s g80_update 5 --es cd.update available --es cd.now 2026-10-04T15:00
s g81_support_mark 5 --es cd.supported support_coffee,support_cake --es cd.now 2026-10-04T15:00
s g66_guest_rare 5 --es cd.guest owl --es cd.now 2026-10-05T10:00
# 한 줄에 사진 한 장 (11): 오늘 · 1년 전 오늘 (돌아온 한 줄, 그만큼 바랜 사진) · 지난 날 기록
s g67_photo_today 5 --ez cd.photos true --ei cd.page 1 --es cd.now 2026-10-06T21:00
# 어젯밤 쓰던 한 줄이 남은 아침: 어제의 한 줄로 이어 쓰기 (칸 위 한 줄 안내)
s g82_carried_draft 5 --es cd.draftYesterday walk --ei cd.page 1 --es cd.now 2026-10-08T08:00
# 미니 하루 (1.1.5): 지난 7일 마음 셋이 하루 발치에 · 가족과 옹기종기
s g83_mini_haru 6 --ez cd.family true --ez cd.months true --es cd.now 2026-09-28T15:00
swipe_up; shot g68_photo_more 3
# 한 해의 엔딩 크레딧 (08): 인트로 · 봄 · 끝 (아웃트로)
open --ez cd.family true --ez cd.special true --es cd.now 2026-12-28T20:00; sleep 3
s g71_credits_end 45 --ez cd.ringYear true --es cd.screen credits --ei cd.creditsYear 2026 --es cd.now 2026-12-28T20:00;  shot g69_credits_intro 4; shot g70_credits_winter 6; shot g70b_credits_spring 9; shot g70c_credits_summer 11
# 하루의 첫 화면 (그날 처음 열 때): 아침 · 밤
# 첫 화면은 하늘 → 땅이 오름 (4초 남짓) → 이름 · 인사 → ‘눌러서 정원으로’ 가 마지막에 (느린 에뮬레이터는 더 늦음)
if wantany g73a g73b g73 g73c; then open --ez cd.title true --es cd.now 2026-10-03T07:40; shot g73a_title_sky 1; shot g73b_title_pan 2; shot g73_title_morning 12; shot g73c_title_hint 8; fi
s g74_title_night 18 --ez cd.title true --es cd.now 2026-10-03T22:30
# 정원의 한 해 한 장 (S2): 나무 · 말뚝 · 연이 있는 작은 정원
s g72_garden_year 8 --ez cd.gardenYear true --es cd.now 2026-10-20T15:00
# 별자리 정원: 마음의 기록 (이번 달 · 낮/밤) · 지난 달의 정원 카드 · 한 해의 띠 (밤) · 지난 정원
s g49_record_month 6 --ez cd.months true --ez cd.openMonth true --es cd.now 2026-09-28T15:00
s g50_record_month_night 6 --ez cd.openMonth true --es cd.now 2026-09-28T22:30
s g51_month_card 5 --es cd.now 2026-10-02T15:00
s g52_year_night 8 --ez cd.year true --ez cd.openYear true --es cd.now 2026-12-31T22:00
s g53_past_gardens 3 --es cd.screen collection --es cd.now 2026-10-02T15:00; swipe_up; swipe_up; swipe_up
# 인생의 계절 (다른 생년월일 · 다른 하루)
s g12_spring 14 --el cd.seed 12345 --es cd.birth 2016-03-01 --es cd.now $NOW
s g13_autumn 14 --el cd.seed 99 --es cd.birth 1968-08-20 --ez cd.preview true --es cd.now $NOW
s g14_winter 14 --el cd.seed 31337 --es cd.birth 1944-01-05 --ez cd.preview false --es cd.now $NOW
s g15_beyond 8 --el cd.seed 2718281 --es cd.birth 1930-01-01 --es cd.now $NOW
# 유리 버전
s v01_glass_home 5 --es cd.design glass --es cd.birth 2000-05-12 --es cd.now $NOW
s v02_glass_settings 4 --es cd.screen settings
# 다크 모드 · 영어 · 큰 글자 · 작은 화면 (정원)
open --es cd.design garden --el cd.seed 2718281 --es cd.birth 2000-05-12 --es cd.now $NOW
adb shell cmd uimode night yes; sleep 2; open --es cd.now $NOW;                                         shot x01_dark 6
adb shell cmd uimode night no
adb shell cmd locale set-app-locales $P --locales en-US 2>/dev/null; open --es cd.now $NOW;           shot x02_english 6
s x03_english_settings 4 --es cd.screen settings
adb shell cmd locale set-app-locales $P --locales ko-KR 2>/dev/null
adb shell settings put system font_scale 1.3; open --es cd.now $NOW;                                   shot x04_bigfont 6
adb shell settings put system font_scale 1.0
adb shell wm size 720x1280; adb shell wm density 320; open --es cd.now $NOW;                          shot x05_small 6
adb shell wm size reset; adb shell wm density reset
quick_scenes
finish
