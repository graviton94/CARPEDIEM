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
open --ez cd.reset true --es cd.design garden --el cd.seed 2718281 --es cd.birth 2000-05-12 --es cd.sex female --ez cd.meet false --es cd.now $NOW; shot q00_decor_first 8
open --ez cd.preview true --es cd.now $NOW; shot q01_decor_autumn 6
open --ez cd.preview true --es cd.now 2026-09-30T22:40; shot q02_decor_night 6
open --ez cd.preview true --es cd.now 2026-04-15T15:00; shot q03_decor_spring 6
open --ez cd.preview true --es cd.now 2026-07-20T15:00; shot q04_decor_summer 6
open --ez cd.preview true --es cd.now 2027-01-12T14:00; shot q05_decor_winter 6
open --ez cd.preview true --ez cd.family true --es cd.now 2027-01-12T14:00; shot q06_decor_family_winter 6
adb shell input tap 90 1650; shot q07_decor_sheet 3
open --ez cd.preview true --es cd.birth 1962-03-02 --es cd.now $NOW; shot q08_decor_ginkgo 6
open --ez cd.preview true --es cd.screen collection --es cd.now $NOW; shot q09_album 5
# 우연한 순간 (캡처용으로 바로 띄움): 몇 초 뒤 모습
open --ez cd.preview false --es cd.birth 2000-05-12 --es cd.chance butterflies --es cd.now 2026-05-20T15:00; shot q10_chance_butterflies 7
open --es cd.chance rainbow --es cd.now $NOW; shot q11_chance_rainbow 5
open --es cd.chance wind --es cd.now $NOW; shot q12_chance_wind 3
open --es cd.chance bubbles --es cd.now $NOW; shot q13_chance_bubbles 4
open --es cd.chance fireflies --es cd.now 2026-09-30T22:40; shot q14_chance_fireflies 5
open --es cd.chance aurora --es cd.now 2027-01-12T22:40; shot q15_chance_aurora 5
open --es cd.chance snail --es cd.now $NOW; shot q16_chance_snail 5
open --es cd.screen gaze --es cd.now 2026-10-02T15:00; shot q17_gaze_autumn 8
open --es cd.screen gaze --es cd.now 2026-10-02T22:40; shot q18_gaze_autumn_night 8
open --es cd.screen gaze --es cd.now 2026-04-15T15:00; shot q19_gaze_spring 8
open --es cd.screen gaze --es cd.now 2026-07-20T22:40; shot q20_gaze_summer_night 8
open --es cd.screen breath --es cd.now 2026-10-03T07:30; shot q21_breath_intro 2
open --es cd.screen ripple --es cd.now 2026-10-03T15:00; shot q22_breath_ripple 9
open --es cd.screen walk --es cd.now 2026-10-03T15:00; shot q23_breath_walk 10
open --es cd.screen lantern --es cd.now 2026-10-02T22:40; shot q24_breath_lantern 9
open --es cd.screen thanks --es cd.now 2026-10-03T15:00; shot q25_breath_flowers 40
# 절기의 작은 변화 (S1, cd.term) · 실제 해 시각 (S3: 12월 오후 5시 반은 어스름) · 달 모양 · 한 해 한 장 (S2, 12월 마지막 주)
open --ez cd.preview false --es cd.term sanggang --es cd.now 2026-10-23T15:00; shot q26_term_sanggang 6
open --es cd.term dongji --es cd.now 2026-12-22T19:30; shot q27_term_dongji 6
open --es cd.term chunbun --es cd.now 2026-04-15T15:00; shot q28_term_blossom 8
open --es cd.term soseol --es cd.now 2027-01-12T14:00; shot q29_term_snow 8
open --es cd.now 2026-12-10T17:30; shot q30_sky_december_1730 5
open --es cd.now 2026-10-14T21:00; shot q31_moon_crescent 5
open --es cd.now 2026-10-26T22:00; shot q32_moon_full 5
open --es cd.now 2026-12-27T15:00; shot q33_year_note 5
# 돌에게 건넨 조각 (R1) · 기억의 주에 밝아진 별 (R2: 보리가 떠난 날 12월 20일 앞뒤 사흘)
open --ez cd.family true --es cd.now 2026-10-05T15:00; sleep 5
open --ez cd.offer true --es cd.now 2026-10-05T15:00; shot q34_offer 6
open --es cd.screen stone --es cd.stoneId mom00001 --es cd.now 2026-10-05T15:00; sleep 3; swipe_up; shot q35_offer_stone 3
open --ez cd.memory true --es cd.now 2026-12-20T21:30; shot q36_memory_week 6
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

# 정원: 처음 켜기 → 하루를 만남 → 홈
open --ez cd.reset true --ez cd.guide true --es cd.design garden --es cd.now $NOW;                    shot g00_intro 4
open --ez cd.reset true --es cd.design garden --es cd.now $NOW;                                       shot g01_onboarding 5
open --ez cd.reset true --es cd.design garden --el cd.seed 2718281 --es cd.birth 2000-05-12 --es cd.sex female --ez cd.meet true --es cd.now $NOW; shot g02_meet 14
open --ez cd.meet false --es cd.now $NOW;                                                              shot g03_home 6
# 처음 온 사람의 둘러보기: 첫 장 → 한 번 눌러 둘째 장 (남은 시간을 비춤)
open --ez cd.guide true --es cd.now $NOW;                                                              shot g03b_guide 6
adb shell input tap 540 250; shot g03c_guide_number 3
open --ez cd.preview true --es cd.now $NOW;                                                            shot g04_home_all 5
open --ei cd.page 1 --es cd.now $NOW;                                                             shot g05_write 3
open --ei cd.page 2 --es cd.now $NOW;                                                             shot g06_memories 3
open --ei cd.page 3 --es cd.now $NOW;                                                             shot g06_flow 3
open --ez cd.preview false --es cd.now 2026-09-30T07:10;                                               shot g07_morning 5
open --es cd.now 2026-09-30T23:30;                                                                     shot g08_night 5; shot g08_night_late 8
open --es cd.screen settings --es cd.now $NOW;                                                         shot g09_settings 4
swipe_up; shot g10_settings_more 3
open --es cd.screen widgets --es cd.now $NOW;                                                          shot g11_widgets 6
open --ez cd.preview true --es cd.screen collection --es cd.now $NOW;                                  shot g16_collection 5
open --ez cd.preview false --es cd.screen support --es cd.now $NOW;                                    shot g17_support 6
adb shell input tap 540 1700; shot g18_support_tap 2
open --es cd.now $NOW --ez cd.recall true --ei cd.page 1;                                           shot g21_letgo_recall 4
# 가족의 정원 (엄마는 오늘 생일): 홈 · 돌의 페이지 · 돌 더하기
open --ez cd.family true --es cd.now $NOW;                                                             shot g22_family 5
open --es cd.screen stone --es cd.now $NOW;                                                            shot g23_stone 4
open --es cd.screen add --es cd.now $NOW;                                                              shot g24_add 3
open --ez cd.family true --ez cd.full true --es cd.now $NOW;                                         shot g35_family9 5; shot g35_family9_late 10
# 그 순간 앱의 모든 스레드 스택 (멈춤 원인 찾기): kill -3 → /data/anr
adb shell kill -3 "$(adb shell pidof $P | tr -d '\r')" 2>/dev/null; sleep 3
open --es cd.now 2026-09-30T23:10;                                                                     shot g25_family_night 5
open --es cd.screen breath --es cd.now $NOW;                                                           shot g26_breath_in 3; shot g27_breath_later 5
open --es cd.screen gaze --es cd.now 2026-09-30T23:10;                                                 shot g28_gaze_night 5
open --es cd.now 2026-09-30T18:20;                                                                     shot g19_dusk 5
open --es cd.now 2026-09-30T05:20;                                                                     shot g20_dawn 5
# 1.3 깨닫기: 오늘의 질문 · 마음의 하늘 · 계절의 편지 (12월 2일)
open --ez cd.question true --es cd.now $NOW;                                                          shot g29_question 5
open --ez cd.moods true --ei cd.page 1 --es cd.now $NOW;                                            shot g30_mood 5
open --ez cd.letter true --es cd.now 2026-12-02T10:00;                                             shot g31_letter 5
open --ez cd.wish true --ez cd.openLetter true --es cd.now 2026-12-02T10:00;                            shot g32_letter_open 4
open --ez cd.letter true --ez cd.openLetter true --es cd.now 2026-12-02T21:30;                         shot g33_letter_night 4
open --es cd.screen collection --es cd.now 2026-12-02T10:00; swipe_up; swipe_up;                       shot g34_letters 3
# 1.4 돌봄: 한마디 창 아래 권유 · 잠깐 바라보기 · 고마움 책
open --es cd.care CALM_BREATH --es cd.now $NOW;                                                       shot g36_care 14
open --es cd.screen look --es cd.now $NOW;                                                             shot g37_look 12
# 1.4 기억의 돌: 기억의 자리 (모은 것 안) · 하늘의 별 (밤)
open --ez cd.memory true --es cd.screen memory --es cd.now $NOW;                                      shot g38_memory 6
open --ez cd.memory true --es cd.now 2026-09-30T22:40;                                                shot g39_memory_star 8
# 1.5 감각: 마음의 날씨 (오늘 슬픔 → 비 · 다음 날 기쁨 → 햇살) · 아침의 숨 · 잠들기 전 정원
open --es cd.today SAD --es cd.now 2026-10-01T15:00;                                                  shot g40_weather_rain 4
open --es cd.today JOY --es cd.now 2026-10-02T15:00;                                                  shot g41_weather_sun 4
open --es cd.screen breath --es cd.now 2026-10-03T07:30;                                              shot g42_breath_morning 4
open --es cd.now 2026-10-03T23:20;                                                                     shot g43_sleepy 8
# 1.6 작은 의식: 고마움 숨 (내쉴 때) · 계절 첫날의 바람 · 한 해의 정원
open --es cd.screen thanks --es cd.now 2026-10-03T15:00;                                              shot g44_thanks_breath 7
open --ei cd.page 1 --es cd.now 2026-12-03T10:00;                                                 shot g45_wish 4
open --ez cd.year true --ez cd.openYear true --es cd.now 2026-12-31T15:00;                            shot g46_year 6
# 1.7: 특별한 날 꽃 (인생 달력) · 엄마 생일의 돌 페이지 (생일 카드 보내기)
open --ez cd.special true --ei cd.page 3 --es cd.now $NOW;                                         shot g47_special 5
open --es cd.screen stone --es cd.stoneId mom00001 --es cd.now $NOW;                                  shot g48_birthday_card 5
# 생일 전날 밤: 엄마의 돌이 먼저 고깔을 쓰고 “내일은 엄마 생일이에요”
open --es cd.now 2026-09-29T21:00;                                                                     shot g54_birthday_eve 5
open --es cd.screen stone --es cd.stoneId mom00001 --es cd.now 2026-09-29T21:00;                      shot g55_birthday_eve_stone 4
# 하루를 열고 닫는 작은 의식: 아침 씨앗 (04) · 걱정한 밤 다음 아침 (06) · 하루 닫기 입구와 첫 걸음 (03) · 저녁의 ‘싹이 텄나요?’ · 손끝 숨 (05)
open --es cd.now 2026-10-03T07:40;                                                                     shot g56_seed_card 5
open --ez cd.comfort true --es cd.now 2026-10-03T08:10;                                                shot g57_comfort 5
open --es cd.now 2026-10-03T21:10;                                                                     shot g58_closeday_entry 5
open --es cd.screen close --es cd.now 2026-10-03T21:10;                                                shot g59_closeday_line 4
open --ez cd.morningSeed true --es cd.today CALM --ei cd.page 1 --es cd.now 2026-10-03T20:30;           shot g60_seed_ask 5
open --ez cd.touch true --es cd.screen breath --es cd.now 2026-10-03T15:00;                            shot g61_breath_touch 4
open --ez cd.touch false --es cd.now $NOW; sleep 2
# 미래의 나에게 (10): 오늘 열린 항아리 · 생일 아침의 나이테 (07, 2000-05-12 생 → 2026-05-12 아침) · 추억의 항아리 · 나이테
open --ez cd.capsule true --es cd.now 2026-10-04T09:00;                                                shot g62_capsule_open 5
open --ez cd.ringYear true --es cd.now 2026-05-12T09:00;                                               shot g63_ring_note 6
open --ei cd.page 2 --es cd.now 2026-05-20T15:00; swipe_up; swipe_up; swipe_up; swipe_up;               shot g64_keepsakes 3
# 돌아온 날의 손님 (09): 닷새 · 스무 날 만에
open --ei cd.away 5 --es cd.now 2026-10-04T10:00;                                                      shot g65_return_guest 5
open --ei cd.away 20 --es cd.now 2026-10-05T10:00;                                                     shot g66_return_rare 5
# 한 줄에 사진 한 장 (11): 오늘 · 1년 전 오늘 (돌아온 한 줄, 그만큼 바랜 사진) · 지난 날 기록
open --ez cd.photos true --ei cd.page 1 --es cd.now 2026-10-06T21:00;                                  shot g67_photo_today 5
swipe_up; shot g68_photo_more 3
# 한 해의 엔딩 크레딧 (08): 인트로 · 봄 · 끝 (아웃트로)
open --ez cd.ringYear true --es cd.screen credits --ei cd.creditsYear 2026 --es cd.now 2026-12-28T20:00;  shot g69_credits_intro 4; shot g70_credits_spring 12; shot g71_credits_end 70
# 별자리 정원: 마음의 기록 (이번 달 · 낮/밤) · 지난 달의 정원 카드 · 한 해의 띠 (밤) · 지난 정원
open --ez cd.months true --ez cd.openMonth true --es cd.now 2026-09-28T15:00;                       shot g49_record_month 6
open --ez cd.openMonth true --es cd.now 2026-09-28T22:30;                                             shot g50_record_month_night 6
open --es cd.now 2026-10-02T15:00;                                                                  shot g51_month_card 5
open --ez cd.year true --ez cd.openYear true --es cd.now 2026-12-31T22:00;                            shot g52_year_night 8
open --es cd.screen collection --es cd.now 2026-10-02T15:00; swipe_up; swipe_up; swipe_up;            shot g53_past_gardens 3
# 인생의 계절 (다른 생년월일 · 다른 하루)
open --el cd.seed 12345 --es cd.birth 2016-03-01 --es cd.now $NOW;                                     shot g12_spring 14
open --el cd.seed 99 --es cd.birth 1968-08-20 --ez cd.preview true --es cd.now $NOW;                   shot g13_autumn 14
open --el cd.seed 31337 --es cd.birth 1944-01-05 --ez cd.preview false --es cd.now $NOW;               shot g14_winter 14
open --el cd.seed 2718281 --es cd.birth 1930-01-01 --es cd.now $NOW;                                   shot g15_beyond 8
# 유리 버전
open --es cd.design glass --es cd.birth 2000-05-12 --es cd.now $NOW;                                   shot v01_glass_home 5
open --es cd.screen settings;                                                                          shot v02_glass_settings 4
# 다크 모드 · 영어 · 큰 글자 · 작은 화면 (정원)
open --es cd.design garden --el cd.seed 2718281 --es cd.birth 2000-05-12 --es cd.now $NOW
adb shell cmd uimode night yes; sleep 2; open --es cd.now $NOW;                                         shot x01_dark 6
adb shell cmd uimode night no
adb shell cmd locale set-app-locales $P --locales en-US 2>/dev/null; open --es cd.now $NOW;           shot x02_english 6
open --es cd.screen settings;                                                                          shot x03_english_settings 4
adb shell cmd locale set-app-locales $P --locales ko-KR 2>/dev/null
adb shell settings put system font_scale 1.3; open --es cd.now $NOW;                                   shot x04_bigfont 6
adb shell settings put system font_scale 1.0
adb shell wm size 720x1280; adb shell wm density 320; open --es cd.now $NOW;                          shot x05_small 6
adb shell wm size reset; adb shell wm density reset
quick_scenes
finish
