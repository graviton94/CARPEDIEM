# Google Play 출시 준비

## 1. 업로드 키 만들기 (한 번, 내 컴퓨터에서)

```
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 upload.jks > upload.b64        # macOS: base64 -i upload.jks -o upload.b64
```

- 암호 · 별칭은 직접 정하고, 어디에도 붙여 넣지 말 것 (채팅 · 이슈 · 커밋 모두).
- `upload.jks` 와 암호는 따로 안전한 곳에 보관. 잃어버리면 Play Console 에서 업로드 키 재설정을 요청해야 함.

## 2. GitHub Secrets 에 넣기

저장소 → Settings → Secrets and variables → Actions → New repository secret

| 이름 | 값 |
|---|---|
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | `upload.b64` 파일 내용 |
| `ANDROID_UPLOAD_STORE_PASSWORD` | 키스토어 암호 |
| `ANDROID_UPLOAD_KEY_ALIAS` | `upload` (위에서 정한 별칭) |
| `ANDROID_UPLOAD_KEY_PASSWORD` | 키 암호 |

## 3. 서명된 AAB 받기

Actions → **Android release (Play)** → Run workflow → 버전(예: 1.0.0) → 끝나면 아래 Artifacts 의 `.aab` 를 받아 Play Console 에 올림.
Play 앱 서명(Play App Signing)을 켜 두면 Google 이 배포용 키를 관리하고, 우리는 업로드 키만 씀.

## 4. 응원하기 상품 (Play Console → 수익 창출 → 인앱 상품, 소모성)

| 상품 ID | 이름 | 가격 (한국) |
|---|---|---|
| `support_tea` | 차 한 잔 | 1,500원 |
| `support_coffee` | 커피 한 잔 | 4,400원 |
| `support_cake` | 케이크 한 조각 | 11,000원 |

상품이 등록되면 앱의 응원하기에서 Play 가격이 보이고 결제 창이 열림. 등록 전에는 ‘시험판’ 안내만.
