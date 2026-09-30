# 직접 설치(APK)용 서명 키

`sideload.jks`는 **내 폰에 직접 설치하는 APK 전용** 키입니다. 비밀번호가 `app/build.gradle.kts`에 공개돼 있습니다.

- 같은 키로 서명해야 새 APK로 덮어써서 업데이트할 수 있어 저장소에 넣어 두었습니다.
- **Google Play 출시에는 쓰지 않습니다.** Play에 올릴 때는 새 업로드 키를 만들어 GitHub Secrets에 넣고, Play 앱 서명을 사용합니다.
