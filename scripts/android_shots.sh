#!/usr/bin/env bash
# 에뮬레이터에서 앱을 실행해 일반 사용자가 겪을 장면을 캡처한다 (.github/workflows/android-screens.yml).
# 디버그 빌드만 cd.* 실행 옵션을 읽는다 (MainActivity.debugSetup).
set -u
P=io.github.graviton94.carpediem
OUT=${1:-shots}
mkdir -p "$OUT"
NOW=2026-09-30T15:00
shot() { sleep "$2"; adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }
open() { adb shell am force-stop $P; adb shell am start -W -n $P/.MainActivity "$@" >/dev/null; }
swipe_up() { adb shell input swipe 540 1900 540 500 500; }

adb shell settings put global window_animation_scale 1; adb shell settings put global animator_duration_scale 1
# 느린 에뮬레이터의 'System UI 응답 없음' 창이 화면을 가리지 않게. 부팅 직후 잠시 쉰다.
adb shell settings put global hide_error_dialogs 1
sleep 45; adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null
# 기본 언어는 한국어 (영어는 아래에서 따로)
adb shell cmd locale set-app-locales $P --locales ko-KR 2>/dev/null

# 정원: 처음 켜기 → 하루를 만남 → 홈
open --ez cd.reset true --es cd.design garden --es cd.now $NOW;                                       shot g01_onboarding 5
open --ez cd.reset true --es cd.design garden --el cd.seed 2718281 --es cd.birth 2000-05-12 --es cd.sex female --ez cd.meet true --es cd.now $NOW; shot g02_meet 14
open --ez cd.meet false --es cd.now $NOW;                                                              shot g03_home 6
open --ez cd.preview true --es cd.now $NOW;                                                            shot g04_home_all 5
swipe_up; shot g05_below 3; swipe_up; shot g06_calendar 3
open --ez cd.preview false --es cd.now 2026-09-30T07:10;                                               shot g07_morning 5
open --es cd.now 2026-09-30T23:30;                                                                     shot g08_night 5; shot g08_night_late 8
open --es cd.screen settings --es cd.now $NOW;                                                         shot g09_settings 4
swipe_up; shot g10_settings_more 3
open --es cd.screen widgets --es cd.now $NOW;                                                          shot g11_widgets 6
open --ez cd.preview true --es cd.screen collection --es cd.now $NOW;                                  shot g16_collection 5
open --ez cd.preview false --es cd.screen support --es cd.now $NOW;                                    shot g17_support 6
adb shell input tap 540 1700; shot g18_support_tap 2
open --es cd.now $NOW --ez cd.recall true; swipe_up; sleep 2; swipe_up; swipe_up; shot g21_letgo_recall 3
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
open --ez cd.moods true --es cd.now $NOW; sleep 5; swipe_up; sleep 3; swipe_up;                        shot g30_mood 3
open --ez cd.letter true --es cd.now 2026-12-02T10:00; sleep 5; swipe_up;                              shot g31_letter 3
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
open --es cd.now 2026-12-03T10:00; sleep 5; swipe_up;                                                  shot g45_wish 3
open --ez cd.year true --ez cd.openYear true --es cd.now 2026-12-31T15:00;                            shot g46_year 6
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
