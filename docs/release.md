# Google Play 출시 준비

## 1. 업로드 키 (PC 없이, 한 번)

1. 저장소 → Settings → Secrets and variables → Actions 에 `ANDROID_UPLOAD_PASSPHRASE` (16자 이상, 직접 설치용 암호와 다르게) 를 넣는다. 암호는 채팅 · 이슈 · 커밋 어디에도 붙여 넣지 않고 따로 보관.
2. Actions → **Android signing key (one time)** → kind = `upload` 로 실행. GitHub 안에서 키를 만들어 암호로 잠근 `android/keystore/upload.jks.gpg` 만 저장소에 올린다 (로그에는 인증서 지문만).
3. 암호를 잃어버리면 Play Console 에서 업로드 키 재설정을 요청해야 한다.

## 2. 서명된 AAB 받기

Actions → **Android release (Play)** → Run workflow → 버전(예: 1.0.0) → 끝나면 Artifacts 의 `.aab` 를 받아 Play Console 에 올림.
Play 앱 서명(Play App Signing)을 켜 두면 Google 이 배포용 키를 관리하고, 우리는 업로드 키만 씀. 이 빌드에서는 개발자 도구가 꺼진다.

## 3. 버전 번호

- versionCode = 그 워크플로의 실행 번호 × 10 + 시도 횟수 (같은 실행을 다시 돌려도 겹치지 않음). 필요하면 `CD_VERSION_CODE` 로 직접
- 업로드 키 없이 `bundleRelease` 를 돌리면 빌드가 멈춤 (debug 키 · 개발자 도구가 켜진 AAB 가 만들어지지 않게)
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
- [x] 개인정보처리방침 URL: https://graviton94.github.io/privacy/ (공개 저장소 graviton94.github.io, 2026-10-05 판: 문의 ruahn49@gmail.com · 사진 · 의견 메일 반영)
- [ ] 데이터 보안: `docs/store-listing.md` 의 표대로 — 앱 정보 및 성능 › 비정상 종료 로그 · 진단만 ‘수집’ (선택 사항 · 사용자가 직접 메일로 보낼 때만 · 공유 안 함 · 앱 기능/오류 수정). 그 밖은 기기 안에만, 결제는 Google Play
- [ ] 콘텐츠 등급 설문 (폭력 · 도박 없음), 대상 연령 13세 이상, 광고 없음
- [ ] 인앱 상품 3개 (4번 표)
- [ ] 내부 테스트 트랙에 AAB (3번) → 폰에서 받아 확인 → 비공개 테스트 (개인 계정은 테스터 12명 · 14일 이상) → 프로덕션
- [x] targetSdk 36 (빌드에 반영됨)
- [x] 말 네 가지: 설정 › 앱 › 언어 에서 고를 수 있고 (locales_config), AAB 는 말을 나누지 않아 바꿔도 글자가 빠지지 않음
- [x] 개발자 모드: Play 업로드 키로 만든 빌드 (`BuildConfig.DEV_TOOLS = false`) 에서는 켜지지 않음. 직접 설치 APK 에서는 그대로

## 6. 직접 해야 하는 것만 (코드 · 문서 · 방침 페이지는 끝남)

1. Play Console 개발자 계정 · 앱 만들기 · 스토어 등록정보 올리기 (5번 체크리스트)
2. 데이터 보안 설문 · 콘텐츠 등급 · 대상 연령 답하기 (5번 표 그대로)
3. 인앱 상품 3개 만들기 (4번 표)
4. GitHub Secrets 의 `ANDROID_UPLOAD_PASSPHRASE` 확인 → Actions › **Android release (Play)** 실행 → `.aab` 를 내부 테스트 트랙에 올림 → 실기기 확인 → 비공개 테스트 (테스터 12명 · 14일)
5. Play 앱 서명 켜기 (업로드 키를 잃어도 재설정할 수 있게)

## 7. 새 버전 낼 때 (새로워진 점 · 출시 노트는 한 번만 씀)

1. `design/changelog.json` 맨 위에 새 버전과 4개 언어 줄을 더함 (언어마다 줄 수 같게, Play 는 언어마다 500자까지)
2. `python3 scripts/generate.py` → 앱의 ‘새로워진 점’ (업데이트 뒤 한 번 · 설정 › 도움 · 응원) 과 `docs/release-notes/<버전>.txt` 가 같이 만들어짐
3. Actions › **Android release (Play)** 를 그 버전으로 실행 → 실행 요약에 4개 언어 출시 노트가 나옴 → Play Console 출시 노트 칸에 그대로 붙여넣기
