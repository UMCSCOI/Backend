# 스코이 : 편리한 스테이블 코인 결제 플랫폼

## 💡 Project Overview

스코이는 스테이블코인을 일상적인 결제 수단으로 사용할 수 있도록 설계된 금융 플랫폼입니다.
결제부터 투자까지, 누구나 쉽게 사용할 수 있는 편리한 스테이블 코인 금융 환경을 제공하는 것을 목표로 합니다.

<img height="512" alt="image" src="https://github.com/user-attachments/assets/531acd4d-1c28-4912-b1a9-90adac60df05" />

## 🎯 주요 기능

### 🔐 **SMS 본인인증 & 간편 로그인**
- CoolSMS 기반 휴대폰 번호 인증으로 간편하게 회원가입                                                                                                                 
- 6자리 간편 비밀번호 + JWT 토큰 방식으로 빠르고 안전한 로그인
- 5회 실패 시 계정 잠금, SMS 재인증으로 해제

### 🏦 **멀티 거래소 API 연동**
- 업비트·빗썸 API 키를 등록해 두 거래소를 하나의 앱에서 통합 관리
- 거래소별 연동 상태 확인 및 API 키 등록·수정·삭제

### 💰 **원화 충전 & 자산 조회**
- 원화(KRW) 충전 요청 및 USDT/USDC 입금 주소 생성·조회
- 보유 자산(KRW, BTC, ETH 등) 전체 조회
- 주문 체결 시 FCM 푸시 알림 및 실시간 웹소켓 연동

### 📋 **내 지갑 & 거래 내역**
- 입출금·충전 거래 내역을 기간·유형별로 통합 조회
- 원화 출금 (카카오·네이버·하나 2차 인증 지원)
- 거래 UUID 기반 상세 내역 조회

  
## 👥 Contributors

|                                                           **마크/김주헌**                                                            |                                                         **호/원종호**                                                         | **띵/장명준** | **드로코드/김민규** | **희동/서희정** |
|:-------------------------------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------------------------------:| :---: | :---: | :---: |
| [<img src="https://avatars.githubusercontent.com/u/75869755?v=4" width="150"><br/>rlawngjs0313](https://github.com/rlawngjs0313) | [<img src="https://avatars.githubusercontent.com/u/133846600?v=4" width="150"><br/>yee2know](https://github.com/yee2know) | [<img src="https://avatars.githubusercontent.com/u/103755402?v=4" width="150"><br/>komascode](https://github.com/komascode) | [<img src="https://avatars.githubusercontent.com/u/90828383?v=4" width="150"><br/>kingmingyu](https://github.com/kingmingyu) | [<img src="https://avatars.githubusercontent.com/u/180945392?v=4" width="150"><br/>seohyunk09](https://github.com/seohyunk09) |

### ⚙️ 기술 스택
- Java 21
- Spring Boot 4.0.1
- MySQL
- Swagger 2.7.0
- Feign Client

### 📋 Commit Message Convention
|     Gitmoji     | Description |
|:---------------:| - |
|   `✨ feat: `   | 새로운 기능 추가 |
|   `🐛 fix: `    | 버그 수정 |
|   `📝 docs: `   | 문서 추가, 수정, 삭제 |
|   `✅ test: `   | 테스트 코드 추가, 수정, 삭제 |
|  `💄 style: `   | 코드 형식 변경 |
| `♻️ refactor: ` | 코드 리팩토링 |
|   `⚡️ perf: `   | 성능 개선 |
|    `💚 ci: `    | CI 관련 설정 수정 |
|  `🚀 chore: `   | 기타 변경사항 |
|  `🔥 remove:`️   | 코드 및 파일 제거 |

### 아키텍처 구조
도메인 아키텍쳐 (DDD)
```
└── java/com/example/scoi/
    ├── domain
    |   ├── auth        # 인증 관련
    |   ├── charge      # 충전 관련
    |   ├── invest      # 투자 관련
    |   ├── member      # 마이페이지 관련
    |   ├── myWallet    # 내지갑 관련
    |   └── transfer    # 이체 관련
    ├── global
    |   ├── apiPayload  # 응답 통일
    |   ├── security    # 시큐리티
    |   ├── client      # 외부 API 호출
    |   ├── config      # 각종 설정
    |   ├── util        # 유틸
    |   └── redis       # Redis
    └── ScoiApplication
```
### 서버 아키텍처

<img width="1159" height="729" alt="서버 아키텍처" src="https://github.com/user-attachments/assets/527e386d-9f2c-47b7-a27c-3c8ae5e4ec3f" />
