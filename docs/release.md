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

## 5. Play Console 등록 순서 (체크리스트)

- [ ] 개발자 계정 (개인, 새 Google 계정) · 본인 확인
- [ ] 앱 만들기: 이름 ‘하루의 정원’ (한국어) / ‘Carpe Diem’ (영어), 무료, 앱
- [ ] 스토어 등록정보: 문안 `docs/store-listing.md`, 아이콘 512 × 512, 그래픽 이미지 `docs/store/feature-*.png`, 스크린샷 `docs/store/screenshot-*.png`
- [ ] 개인정보처리방침 URL: `docs/privacy.md` 를 공개 주소로 (GitHub Pages 를 켜면 `https://graviton94.github.io/CARPEDIEM/privacy`), 문의 이메일 채우기
- [ ] 데이터 보안: 수집 · 공유하는 데이터 없음, 기기 안에만 저장, 결제는 Google Play
- [ ] 콘텐츠 등급 설문 (폭력 · 도박 없음), 대상 연령 13세 이상, 광고 없음
- [ ] 인앱 상품 3개 (4번 표)
- [ ] 내부 테스트 트랙에 AAB (3번) → 폰에서 받아 확인 → 비공개 테스트 (개인 계정은 테스터 12명 · 14일 이상) → 프로덕션
- [ ] targetSdk 36 (빌드에 반영됨)
- [ ] **테스트가 끝나면, 프로덕션 올리기 전에 개발자 모드 막기**: 지금은 설정 맨 아래 버전을 누르면 출시 빌드에서도 켜짐 (시험 알림 · 시험 기록 넣기가 실제 기록에 들어감). `Screens.kt` 의 `onVersionTap` 과 `if (state.devMode)` 를 `BuildConfig.DEBUG &&` 로 감싸기
