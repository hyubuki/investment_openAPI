# 업무 모듈 및 API 설계 기준

## 1. 목적

이 디렉터리는 External Securities OpenAPI의 업무 모듈, JPA Entity, Service, Repository, 외부 API 및 내부 메시지 계약을 정의한다.
현재 문서는 구현 전 검토를 위한 `Draft v0.1`이다. 거래소 규정, 내부 OMS 계약 및 고객 요구사항이 확인되면 ADR과 OpenAPI 문서로 확정한다

## 2. 권장 업무 모듈 구성
제안된 사용자, 계좌, 매매, 원장, 시장의 다섯 축은 적절하다. 단, 증권 주문에서 리스크 검사는 선택 기능이 아니므로 `리스크·한도`를 별도 업무 모듈로 둔다.

| 업무 모듈 | 핵심 책임 |
|---|---|
| 사용자 | 인증 주체, 투자 고객, API Client 및 접근 권한 관리 |
| 계좌 | 거래 계좌, 상품별 거래 권한 및 계좌 상태 관리 |
| 매매 | 주문, 정정, 취소, 체결 및 주문 상태 머신 관리 |
| 원장 | 현금·증권의 불변 거래 기록, 잔고와 포지션 산출 |
| 시장 | 상품 기준정보, 거래일·세션 및 실시간 시장 데이터 제공 |
| 리스크·한도 | 주문 전 리스크 검사, 한도 및 주문 가능 금액·수량 관리 |

업무별 패키지, Service, Repository 및 테이블 책임을 분리한 Modular Monolith를 기본안으로 한다. 독립 배포, 장애 격리 또는 확장 요구가 확인된 영역만 추후 분리한다.
현재 문서의 `Entity`는 JPA 영속성 Entity를 의미한다. 현 단계의 업무 규칙과 트랜잭션 조정 책임은 Service에 둔다.

## 3. 지원 영역
다음 영역은 중요하지만 업무 데이터를 직접 관리하기보다 외부 연동 또는 운영 지원 역할을 담당한다.

| 지원 영역 | 책임 |
|---|---|
| Exchange Connectivity | 거래소 세션, 전문 변환, sequence, heartbeat, 재전송 및 연결 복구 |
| Streaming | WebSocket 연결, 인증, 구독, Backpressure 및 Replay 전달 |
| Reconciliation | 거래소·OMS·원장 간 불일치 탐지와 복구 Workflow |
| Clearing & Settlement | 체결 이후 결제 의무, 결제 예정 건 및 결제 실패를 관리. Post-trade 범위가 커지면 독립 업무 모듈로 분리 |
| Notification | Push, 이메일 등 부가 알림. 체결의 원장이 아님 |
| Audit & Compliance | 감사 이벤트 보관, 조회 및 규제 대응 |

## 4. 모듈 관계

```text
사용자 ───────┐
              ▼
계좌 ──────→ 리스크·한도 ──────┐
  │                            ▼
  │                          매매 ─────→ Exchange Connectivity ─────→ 거래소
  │                            │
  │                            ├─ Order/Execution Event ─────→ Streaming
  │                            ▼
  └────────────────────────── 원장
                               │
                               └─ Balance/Position Projection

시장 ── Instrument/Session/Price Reference ──→ 리스크·한도, 매매
```

### 데이터 변경 책임

- 사용자 모듈은 신원과 인증 상태를 변경하며 계좌 잔고는 변경하지 않는다.
- 계좌 모듈은 거래 가능 상태와 상품 권한을 변경하며 체결 내역은 변경하지 않는다.
- 매매 모듈은 주문과 체결을 변경하며 현금·증권 최종 잔고를 직접 수정하지 않는다.
- 원장 모듈은 Posting, 현금·증권 예약, 잔고와 포지션을 변경한다.
- 시장 모듈은 상품과 시장 상태를 변경하며 고객 주문 상태를 변경하지 않는다.
- 리스크·한도 모듈은 주문 가능 여부, 정책 한도 및 한도 점유를 변경하며 현금·증권 잔고와 주문 Lifecycle은 변경하지 않는다.

모듈 간 JPA 연관관계는 편의만으로 추가하지 않는다. 강한 트랜잭션 일관성과 탐색이 함께 필요한 관계만 연관관계를 사용하고, 그 외에는 식별자와 Service 호출 또는 내부 메시지를 사용한다. 선택 근거는 쿼리 패턴, 잠금 범위와 결합도를 기준으로 기록한다.

주문 전 승인 과정은 매매 모듈의 `OrderProcessingService`가 조정한다. 리스크 한도 점유와 원장 Asset Hold가 모두 성공한 이후에만 거래소 전송을 허용하며, 중간 단계 실패 시 이미 성공한 예약을 멱등하게 보상한다.

## 5. 공통 JPA 및 Service 원칙

- API Request/Response DTO와 JPA Entity를 같은 클래스로 사용하지 않는다.
- `EnumType.STRING`을 사용하고 Enum 변경의 데이터 호환성을 검토한다.
- DB 시간 컬럼은 PostgreSQL `timestamptz`, Java 타입은 `Instant`를 기본으로 한다.
- 금액·가격·소수 수량의 `precision`과 `scale`을 컬럼마다 명시한다.
- 변경 경쟁이 있는 Entity에는 `@Version` 또는 명시적인 Lock 전략을 적용한다.
- 컬렉션 연관관계는 기본 LAZY로 조회하고 목록 API에는 전용 Projection Query를 사용한다.
- 주문, 체결 및 원장은 Cascade Delete와 `orphanRemoval`로 삭제하지 않는다.
- Schema 변경은 Flyway로 수행하고 운영 Profile은 `ddl-auto=validate`를 사용한다.
- 쓰기 Service는 `@Transactional`, 조회 Service는 `@Transactional(readOnly = true)`를 기본으로 한다.
- 거래소와 외부 시스템 호출을 DB 트랜잭션 안에서 장시간 대기하지 않는다.
- Controller는 Repository를 직접 호출하지 않고 입력 검증과 Service 호출, DTO 변환만 담당한다.
- Service와 Repository는 생성자 주입을 사용한다.

## 6. 문서 목록

- [공통 API 규약](../api/공통-API-규약.md)
- [모듈별 API Spec](../api/README.md)
- [사용자 모듈](사용자.md)
- [계좌 모듈](계좌.md)
- [매매 모듈](매매.md)
- [원장 모듈](원장.md)
- [시장 모듈](시장.md)
- [리스크·한도 모듈](리스크-한도.md)
- [용어사전](../용어사전.md)

## 7. 구현 순서

모듈별 진행 체크리스트와 이슈 생성 순서는 [GitHub 이슈 초안](../../.github/issue-drafts/README.md)에서 관리한다.

1. 공통 ID, 시간, 금액, 수량 및 오류 계약을 확정한다.
2. 사용자 인증과 API Client 권한 경계를 확정한다.
3. 계좌 및 상품별 거래 권한을 정의한다.
4. 시장의 Instrument와 Trading Session 계약을 정의한다.
5. 리스크 사전 검사와 원장의 자금·수량 예약 계약을 정의한다.
6. 주문 상태 머신, 체결 멱등성 및 거래소 연동 인터페이스를 정의한다.
7. 체결로부터 원장 Posting과 잔고 Projection을 생성한다.
8. WebSocket 이벤트와 Replay API를 구현한다.
9. 거래소·OMS·원장 Reconciliation과 운영 도구를 구현한다.

각 단계는 정상 흐름뿐 아니라 중복, 역순, 타임아웃, 연결 단절, 부분 체결 및 동시 취소 시나리오를 포함해야 한다.

## 8. 패키지 경계 초안

```text
dev.hyuki.investment_openapi
├── identity
├── account
├── trading
├── ledger
├── market
├── risk
├── exchange
├── streaming
├── reconciliation
└── support
```

각 업무 모듈은 기본적으로 `entity`, `repository`, `service`, `presentation`, `dto` 패키지를 사용한다. 외부 연동 구현이 있는 모듈은 `integration`을 추가한다. 모든 Entity를 하나의 공통 `domain` 패키지에 모으지 않는다.

```text
trading
├── entity
├── repository
├── service
├── presentation
├── dto
└── integration
```
