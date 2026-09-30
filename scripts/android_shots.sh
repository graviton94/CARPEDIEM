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
open --es cd.now 2026-09-30T23:30;                                                                     shot g08_night 5
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
open --es cd.now 2026-09-30T18:20;                                                                     shot g19_dusk 5
open --es cd.now 2026-09-30T05:20;                                                                     shot g20_dawn 5
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
ls -la "$OUT"
