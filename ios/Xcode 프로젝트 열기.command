#!/bin/bash
# 더블클릭하면 CarpeDiem.xcodeproj 를 만들고 Xcode 로 연다.
cd "$(dirname "$0")" || exit 1
if ! xcodebuild -version >/dev/null 2>&1; then
  echo "Xcode 가 필요합니다. Mac App Store 에서 설치한 뒤 한 번 실행해 주세요."
  open "macappstore://apps.apple.com/app/xcode/id497799835"; read -n 1 -s -r -p "아무 키나 누르면 닫힙니다."; exit 1
fi
XCODEGEN="$(command -v xcodegen || find .tools -type f -name xcodegen -perm -u+x 2>/dev/null | head -n 1)"
if [ -z "$XCODEGEN" ]; then
  echo "XcodeGen 을 내려받는 중... (최초 1회)"
  mkdir -p .tools && curl -fsSL -o .tools/xcodegen.zip https://github.com/yonaskolb/XcodeGen/releases/latest/download/xcodegen.zip \
    && unzip -q -o .tools/xcodegen.zip -d .tools && rm -f .tools/xcodegen.zip
  XCODEGEN="$(find .tools -type f -name xcodegen -perm -u+x 2>/dev/null | head -n 1)"
fi
[ -n "$XCODEGEN" ] || { echo "XcodeGen 설치 실패. 터미널에서 'brew install xcodegen' 후 다시 실행해 주세요."; read -n 1 -s -r; exit 1; }
"$XCODEGEN" generate && open CarpeDiem.xcodeproj
