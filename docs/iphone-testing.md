# 내 아이폰에서 테스트하기

두 가지 방법이 있습니다. **Mac이 없다면 1번(TestFlight)** 을 쓰세요.

| | 1. TestFlight (추천) | 2. Mac + 케이블 |
|---|---|---|
| Mac | 필요 없음 | 필요 (Xcode) |
| 비용 | Apple Developer Program 연 $99 | 무료 계정 가능하나 위젯이 데이터를 못 읽음* → 사실상 $99 필요 |
| 설치 | 아이폰 TestFlight 앱에서 설치 | Xcode에서 ▶︎ |
| 유효 기간 | 빌드당 90일 | 유료 1년 · 무료 7일 |
| 친구에게 배포 | 가능 (최대 1만 명) | 불가 |

\* 무료(Personal Team) 계정은 App Group을 쓸 수 없어, 위젯이 앱의 생년월일을 읽지 못합니다.

---

## 1. TestFlight — Mac 없이

### ① Apple Developer Program 가입 (1회, 승인 1~2일)
1. https://developer.apple.com/programs/enroll/ → Apple ID로 로그인 → 개인(Individual)으로 가입, 연 $99 결제
2. 승인 메일이 오면 https://developer.apple.com/account → **Membership details** 에서 **Team ID**(10자리) 확인

### ② 번들 ID 정하기
전 세계에서 유일해야 합니다. 예: `com.junyeong.carpediem` (영문 소문자, 점으로 구분)

### ③ App Store Connect에 앱 만들기
1. https://appstoreconnect.apple.com → **앱** → **＋ → 신규 앱**
2. 플랫폼 iOS · 이름 `Carpe Diem`(이미 있으면 `Carpe Diem - 오늘을 소중히` 등) · 기본 언어 한국어
3. 번들 ID 목록에 ②가 없으면 **Certificates, Identifiers & Profiles → Identifiers → ＋** 에서 먼저 등록
   - 등록할 때 **App Groups** 기능에 체크하고, `group.<번들 ID>` 그룹을 만들어 연결
   - 위젯용 `<번들 ID>.widget` 도 같은 방법으로 등록하고 같은 App Group에 연결
4. SKU는 아무 문자열 (예: `carpediem-001`)

### ④ App Store Connect API 키 만들기
1. App Store Connect → **사용자 및 액세스 → 통합 → App Store Connect API → 팀 키 → ＋**
2. 이름 `GitHub CI`, 액세스 **관리(Admin)** — 서명 인증서를 자동으로 만들려면 관리 권한이 필요합니다
3. 생성 후 표시되는 **Issuer ID**, **키 ID** 를 적어 두고 **API 키 다운로드**(`AuthKey_XXXX.p8`) — 한 번만 받을 수 있습니다

### ⑤ GitHub 저장소에 설정 넣기
https://github.com/graviton94/CARPEDIEM → **Settings → Secrets and variables → Actions**

| 탭 | 이름 | 값 |
|---|---|---|
| Secrets | `APPLE_TEAM_ID` | ①의 Team ID |
| Secrets | `ASC_KEY_ID` | ④의 키 ID |
| Secrets | `ASC_ISSUER_ID` | ④의 Issuer ID |
| Secrets | `ASC_KEY_P8` | `.p8` 파일을 메모장으로 열어 `-----BEGIN PRIVATE KEY-----` 부터 끝까지 전부 붙여넣기 |
| Variables | `APP_BUNDLE_ID` | ②의 번들 ID |

> 키와 ID는 채팅에 붙여넣지 말고 위 화면에만 넣어 주세요.

### ⑥ 업로드
1. 저장소 → **Actions → TestFlight → Run workflow**
2. 15~20분 뒤 App Store Connect → 앱 → **TestFlight** 탭에 빌드가 나타납니다 (처리에 10~30분 추가)
3. 처음 한 번: 빌드의 ‘수출 규정 준수’ 질문은 앱에 이미 “암호화 사용 안 함”으로 넣어 두어 자동 처리됩니다

### ⑦ 아이폰에 설치
1. App Store Connect → TestFlight → **내부 테스트 → ＋** 그룹 만들기 → 본인 Apple ID 추가
2. 아이폰에 **TestFlight** 앱 설치 (App Store 무료)
3. 초대 메일의 링크를 누르거나 TestFlight 앱에서 **Carpe Diem → 설치**
4. 위젯: 홈 화면을 길게 눌러 ‘편집’ → ‘위젯 추가’ → Carpe Diem

수정할 때마다 ⑥만 다시 하면 TestFlight 앱에 새 빌드가 뜹니다.

---

## 2. Mac + 케이블

1. Mac에 Xcode 설치 → Xcode 설정 → **Accounts** 에 Apple ID 추가
2. `ios/Config.xcconfig` 에 `APP_BUNDLE_ID`, `DEVELOPMENT_TEAM` 입력
3. `ios/Xcode 프로젝트 열기.command` 더블클릭
4. 아이폰: 설정 → 개인정보 보호 및 보안 → **개발자 모드** 켜기 (재시동)
5. 케이블로 연결 → Xcode 위쪽에서 내 아이폰 선택 → ▶︎
6. 처음 실행 시 아이폰 설정 → 일반 → VPN 및 기기 관리 → 개발자 앱 **신뢰**
