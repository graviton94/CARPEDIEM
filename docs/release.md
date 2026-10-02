# Google Play 출시 준비

## 1. 업로드 키 (PC 없이, 한 번)

1. 저장소 → Settings → Secrets and variables → Actions 에 `ANDROID_UPLOAD_PASSPHRASE` (16자 이상, 직접 설치용 암호와 다르게) 를 넣는다. 암호는 채팅 · 이슈 · 커밋 어디에도 붙여 넣지 않고 따로 보관.
2. Actions → **Android signing key (one time)** → kind = `upload` 로 실행. GitHub 안에서 키를 만들어 암호로 잠근 `android/keystore/upload.jks.gpg` 만 저장소에 올린다 (로그에는 인증서 지문만).
3. 암호를 잃어버리면 Play Console 에서 업로드 키 재설정을 요청해야 한다.

## 2. 서명된 AAB 받기

Actions → **Android release (Play)** → Run workflow → 버전(예: 1.0.0) → 끝나면 Artifacts 의 `.aab` 를 받아 Play Console 에 올림.
Play 앱 서명(Play App Signing)을 켜 두면 Google 이 배포용 키를 관리하고, 우리는 업로드 키만 씀. 이 빌드에서는 개발자 도구가 꺼진다.

## 3. 버전 번호

- versionCode = 그 워크플로의 실행 번호 (Play 용 AAB 는 `Android release` 실행 번호라 1, 2, 3 … 으로 늘어남)
- versionName = 실행할 때 적는 버전 (1.0.0 …)

## 4. 응원하기 상품 (Play Console → 수익 창출 → 인앱 상품, 소모성)

| 상품 ID | 이름 | 가격 (한국) |
|---|---|---|
| `support_tea` | 차 한 잔 | 1,500원 |
| `support_coffee` | 커피 한 잔 | 4,400원 |
| `support_cake` | 케이크 한 조각 | 11,000원 |

상품이 등록되면 앱의 응원하기에서 Play 가격이 보이고 결제 창이 열림. 등록 전에는 ‘시험판’ 안내만.

## 5. Play Console 등록 순서 (체크리스트)

- [ ] 개발자 계정 (개인, 새 Google 계정) · 본인 확인
- [ ] 앱 만들기: 기본 언어 영어 (en-US) ‘Carpe Diem’, 무료, 앱. 번역: 한국어 ‘하루의 정원’ · 일본어 ‘ハルの庭’ · 중국어(번체) ‘小日的庭院’
- [ ] 스토어 등록정보: 문안 `docs/store-listing.md`, 아이콘 512 × 512, 말마다 그래픽 이미지 · 스크린샷 `docs/store/<ko|en|ja|zh-TW>/` (영어는 기본 등록정보, 나머지는 ‘번역 추가’ 로 ko-KR · ja-JP · zh-TW, 홍콩용 zh-HK 는 zh-TW 그대로)
- [x] 개인정보처리방침 URL: https://graviton94.github.io/privacy/ (공개 저장소 graviton94.github.io). 문의는 Play 의 개발자 연락처로
- [ ] 데이터 보안: 수집 · 공유하는 데이터 없음, 기기 안에만 저장, 결제는 Google Play
- [ ] 콘텐츠 등급 설문 (폭력 · 도박 없음), 대상 연령 13세 이상, 광고 없음
- [ ] 인앱 상품 3개 (4번 표)
- [ ] 내부 테스트 트랙에 AAB (3번) → 폰에서 받아 확인 → 비공개 테스트 (개인 계정은 테스터 12명 · 14일 이상) → 프로덕션
- [x] targetSdk 36 (빌드에 반영됨)
- [x] 말 네 가지: 설정 › 앱 › 언어 에서 고를 수 있고 (locales_config), AAB 는 말을 나누지 않아 바꿔도 글자가 빠지지 않음
- [x] 개발자 모드: Play 업로드 키로 만든 빌드 (`BuildConfig.DEV_TOOLS = false`) 에서는 켜지지 않음. 직접 설치 APK 에서는 그대로
