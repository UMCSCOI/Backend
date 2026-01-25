# 스코이 : 편리한 스테이블 코인 결제 플랫폼

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
    |   ├── auth        # JWT
    |   ├── client      # 외부 API 호출
    |   ├── config      # 각종 설정
    |   └── util        # 유틸
    └── ScoiApplication
```
