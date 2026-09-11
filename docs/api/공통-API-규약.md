# 공통 API 규약

상태: `Draft v0.1`

## 1. 기본 규칙

| 항목 | 기준 |
|---|---|
| Base Path | `/api/v1` |
| Protocol | HTTPS REST, 실시간 이벤트는 WSS |
| Content Type | 성공 `application/json`, 오류 `application/problem+json` |
| 인증 | `Authorization: Bearer {accessToken}` |
| 시간 | ISO-8601 UTC. 예: `2026-09-11T01:01:03.123456Z` |
| 날짜 | ISO-8601. 예: `2026-09-11` |
| 가격·금액·수량 | 정밀도 손실 방지를 위해 JSON 문자열 사용 |
| ID | 외부에 노출되는 식별자는 추측하기 어려운 UUID/UUIDv7 계열 사용 |
| 페이지네이션 | 변경이 잦은 주문·체결 데이터는 cursor 기반 사용 |
| API 버전 | URL Major Version과 Schema의 하위 호환 변경 정책 병행 |

현재 인증 테스트는 `/users`, `/auth/login` 등 Version이 없는 Path를 사용하고 있다. 이 문서의 `/api/v1`은 목표 계약이므로 구현에 반영하기 전에 기존 Endpoint를 유지할지, 호환 Path를 제공할지, 일괄 전환할지를 결정해야 한다.

JPA Entity와 Service에서는 가격과 금액을 `BigDecimal`로 표현한다. 수량은 상품 규칙에 따라 `long` 또는 `BigDecimal`을 사용하며 컬럼 precision과 scale을 명시한다. API DTO와 JPA Entity를 같은 클래스로 사용하지 않는다.

## 2. 공통 Header

### 요청

| Header | 필수 | 설명 |
|---|---:|---|
| `Authorization` | 예 | Bearer Access Token |
| `X-Request-Id` | 권장 | Client 요청 추적 ID. 없으면 서버가 발급 |
| `Idempotency-Key` | 주문 명령 시 예 | 중복 주문 방지 키 |
| `Accept-Language` | 아니오 | 오류 메시지 언어. 오류 판단은 `code` 사용 |

### 응답

| Header | 설명 |
|---|---|
| `X-Request-Id` | 요청 추적 ID |
| `Location` | 생성 또는 비동기 접수된 Resource 위치 |
| `Retry-After` | Rate Limit 또는 일시 장애 시 권장 재시도 시간 |

## 3. 성공 응답

동기 생성이 완료된 Resource는 `201 Created`, 비동기 처리를 접수한 주문 명령은 `202 Accepted`, 조회는 `200 OK`, 응답 Body가 없는 성공은 `204 No Content`를 사용한다.

주문 접수 예시:

```http
HTTP/1.1 202 Accepted
Location: /api/v1/orders/018f8f3a-6bc1-7bc2-a7ef-8cd43c875111
X-Request-Id: 018f8f3a-6000-7e19-b520-667b62580111
```

```json
{
  "orderId": "018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
  "clientOrderId": "algo-a-20260911-000001",
  "status": "RECEIVED",
  "receivedAt": "2026-09-11T01:01:03.123456Z"
}
```

## 4. 오류 응답

오류는 RFC 9457 Problem Details 형태를 사용한다.

```json
{
  "type": "https://api.example.com/problems/insufficient-buying-power",
  "title": "Insufficient buying power",
  "status": 422,
  "code": "INSUFFICIENT_BUYING_POWER",
  "detail": "The account does not have enough available cash.",
  "instance": "/api/v1/orders",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580111",
  "retryable": false,
  "occurredAt": "2026-09-11T01:01:03.123456Z",
  "violations": []
}
```

| HTTP Status | 사용 기준 |
|---:|---|
| `400` | JSON 형식, 필수 필드 또는 기본 형식 오류 |
| `401` | 인증되지 않음 또는 Token 무효 |
| `403` | 계좌·상품·행위 권한 없음 |
| `404` | 권한 범위 안에서 Resource를 찾을 수 없음 |
| `409` | Idempotency 충돌 또는 현재 상태와 명령 충돌 |
| `422` | 형식은 맞지만 주문 가능 금액 등 업무 규칙 위반 |
| `429` | Rate Limit 또는 동시 주문 제한 초과 |
| `503` | 거래소·Redis·DB 등 필수 Dependency 장애 또는 주문 접수 불가 |

`detail` 문자열로 분기하지 않는다. Client는 안정적으로 관리되는 `code`와 `retryable`을 사용한다.

## 5. Idempotency

- 주문 생성·정정·취소 요청은 `Idempotency-Key`를 필수로 받는다.
- 동일 사용자 또는 API Client 범위에서 같은 키와 같은 Payload가 재전송되면 최초 결과를 반환한다.
- 같은 키에 다른 Payload가 들어오면 `409 IDEMPOTENCY_KEY_REUSED`를 반환한다.
- 키의 Scope, Payload Hash, 최초 응답 및 만료 시간을 저장한다.
- Idempotency 보존 기간은 거래일과 Client 재시도 정책을 반영해 ADR에서 확정한다.

## 6. 목록 조회

```json
{
  "items": [],
  "nextCursor": "opaque-cursor",
  "hasMore": true
}
```

- `cursor`는 불투명 문자열이며 Client가 내용을 해석하지 않는다.
- 정렬 기준은 API별로 고정하고 문서화한다.
- Offset pagination은 주문·체결처럼 데이터가 계속 추가되는 목록에서 사용하지 않는다.

## 7. WebSocket 이벤트 규약

Endpoint 초안:

```text
WSS /api/v1/stream/private
WSS /api/v1/stream/market-data
```

공통 Envelope:

```json
{
  "eventId": "018f8f3a-8ea2-75b2-8ef6-f50dd4520111",
  "sequence": 981274,
  "channel": "accounts/ACC-001/orders",
  "type": "ORDER_PARTIALLY_FILLED",
  "schemaVersion": 1,
  "occurredAt": "2026-09-11T01:01:03.123456Z",
  "publishedAt": "2026-09-11T01:01:03.125000Z",
  "data": {}
}
```

원칙:

- Private Stream은 인증 및 계좌 권한 검증 후 구독한다.
- `sequence`의 Scope를 채널 또는 계좌 단위로 명시한다.
- Heartbeat와 Client timeout을 정의한다.
- 전달은 `at-least-once`를 기본으로 하고 Client는 `eventId`/`executionId`로 중복 제거한다.
- 느린 Client의 송신 큐를 무제한 확장하지 않는다.
- 연결 종료 후 `afterSequence` 기반 Replay 또는 REST 조회로 복구한다.

## 8. 호환성

하위 호환 변경:

- 선택 필드 추가
- 새로운 이벤트 유형 또는 Enum 값 추가
- 새로운 Endpoint 추가

호환되지 않는 변경:

- 기존 필드 제거 또는 의미 변경
- 문자열을 숫자로 바꾸는 등 타입 변경
- 기존 필수 필드 추가
- 동일 이벤트 이름의 의미 변경

Client는 모르는 JSON 필드와 Enum 값을 안전하게 처리해야 한다. 서버는 호환되지 않는 변경을 새로운 Major Version에서 제공한다.

## 9. Spring Web 구현 계약

- Controller는 `@RestController`와 모듈별 Base Path를 사용한다.
- 변경 API의 Request DTO는 Java `record`와 Bean Validation을 사용하고 `@Valid`를 반드시 적용한다.
- JPA Entity를 Request Body로 받거나 Response Body로 직접 반환하지 않는다.
- 인증된 `userId`, `apiClientId` 및 권한 범위는 Client가 Body로 지정한 값을 신뢰하지 않고 Spring Security Principal에서 얻는다.
- Service가 업무 오류 코드를 가진 예외를 발생시키고 `@RestControllerAdvice`가 `application/problem+json`으로 변환한다.
- Controller에는 `@Transactional`을 두지 않고 쓰기·읽기 트랜잭션은 Service에서 선언한다.
- 목록 응답은 Entity 전체를 로딩하지 않고 Projection 또는 Query DTO를 사용한다.
- 외부 응답에는 Stack Trace, SQL, 내부 테이블명 및 거래소 원문 오류를 노출하지 않는다.

모듈별 Controller와 DTO 초안:

| 모듈 | Controller/Handler | 주요 DTO |
|---|---|---|
| 사용자 | `UserController`, `AuthController`, `ApiClientController` | `RegisterUserRequest`, `LoginRequest`, `TokenResponse`, `ApiClientResponse` |
| 계좌 | `AccountController` | `AccountSummaryResponse`, `AccountDetailResponse`, `TradingPermissionResponse` |
| 매매 | `OrderController`, `ExecutionController`, `PrivateStreamHandler` | `CreateOrderRequest`, `OrderResponse`, `CancelOrderRequest`, `ReplaceOrderRequest`, `ExecutionResponse` |
| 원장 | `BalanceController`, `PositionController`, `LedgerEntryController` | `BalanceResponse`, `PositionResponse`, `LedgerEntryResponse` |
| 시장 | `InstrumentController`, `MarketDataController`, `MarketDataStreamHandler` | `InstrumentResponse`, `QuoteResponse`, `OrderBookResponse` |
| 리스크·한도 | `OrderPreviewController`, `TradingLimitController` | `OrderPreviewRequest`, `OrderPreviewResponse`, `BuyingPowerResponse` |

DTO 이름은 구현 단계에서 최종 확정하되 하나의 DTO를 서로 의미가 다른 Endpoint에서 재사용하지 않는다.
