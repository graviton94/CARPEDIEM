# 직접 설치(APK)용 서명 키

직접 설치하는 APK 전용 키는 **GitHub Secrets 에만** 있습니다 (저장소에는 없음).

| Secret | 내용 |
|---|---|
| `SIDELOAD_KEYSTORE_BASE64` | 키스토어(.jks) 파일을 base64 로 바꾼 글 |
| `SIDELOAD_STORE_PASSWORD` | 키스토어 암호 |
| `SIDELOAD_KEY_PASSWORD` | 키 암호 (별칭은 `sideload`) |

- `android.yml` 이 키를 임시 파일로 풀어 서명합니다. 같은 키로 서명해야 새 APK 로 덮어써 업데이트할 수 있습니다.
- Secrets 가 없으면 debug 키로 서명된 시험용 APK 만 나오고, 릴리즈(android-latest)는 올리지 않습니다.
- **Google Play 출시에는 쓰지 않습니다.** Play 는 `ANDROID_UPLOAD_*` (android-release.yml) 을 씁니다.
