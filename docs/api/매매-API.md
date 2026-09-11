# 매매 모듈 API Spec

상태: `Draft v0.1`

공통 오류 형식과 Header 정책은 [공통 API 규약](공통-API-규약.md)을 따른다. `202 Accepted`는 거래소 접수나 체결 성공을 의미하지 않는다.

---

## POST : /api/v1/orders

|항목|내용|
|:--:|:--|
|설명|신규 주문을 접수하고 내부 주문 식별자를 반환한다.|
|인증|Bearer Token, `orders:write`, 계좌 거래 권한|
|성공 Status|`202 Accepted`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","Content-Type":"application/json","Idempotency-Key":"order-20260911-000001","X-Request-Id":"trading-req-001"}
```

### body

```json
{"clientOrderId":"algo-a-20260911-000001","accountId":"018f8f3a-6bc1-7bc2-a7ef-8cd43c875111","instrumentId":"018f8f3a-7c11-73b2-b411-8e71ca121111","market":"KRX","side":"BUY","orderType":"LIMIT","timeInForce":"DAY","quantity":"100","limitPrice":"25000"}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|Idempotency-Key|필수, Client 범위에서 유일, 동일 Key의 Payload Hash 일치|
|clientOrderId|필수, API Client 범위에서 유일, 최대 64자|
|accountId|필수 UUID, 계좌 거래 권한과 활성 상태 확인|
|instrumentId/market|필수, 상품·시장 일치 및 거래 가능 상태|
|side/orderType/timeInForce|지원 Enum 조합|
|quantity|문자열 Decimal, 양수, 상품 거래 단위 충족|
|limitPrice|지정가는 필수, 시장가는 미입력, 호가 단위·가격제한 충족|
|리스크·원장|Limit Reservation과 Asset Hold 확보 후 거래소 송신|

## Response

### success-body

```json
{"orderId":"018f8f3a-8111-76a8-87a1-c320f47b1111","clientOrderId":"algo-a-20260911-000001","status":"RECEIVED","receivedAt":"2026-09-11T01:50:00Z"}
```

### fail-body

```json
{"type":"https://api.example.com/problems/insufficient-buying-power","title":"Insufficient buying power","status":422,"code":"INSUFFICIENT_BUYING_POWER","detail":"The account does not have enough available cash.","instance":"/api/v1/orders","requestId":"trading-req-001","retryable":false,"occurredAt":"2026-09-11T01:50:00Z","violations":[]}
```

---

## GET : /api/v1/orders/{orderId}

|항목|내용|
|:--:|:--|
|설명|주문의 최신 상태와 누적 체결 정보를 조회한다.|
|인증|Bearer Token, `orders:read`, 계좌 조회 권한|
|성공 Status|`200 OK`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","X-Request-Id":"trading-req-002"}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|orderId|필수 UUID Path Variable|
|계좌 권한|주문 계좌 조회 권한 확인|
|정보 은닉|권한 밖 주문도 존재 여부를 노출하지 않고 `404` 처리|

## Response

### success-body

```json
{"orderId":"018f8f3a-8111-76a8-87a1-c320f47b1111","clientOrderId":"algo-a-20260911-000001","accountId":"018f8f3a-6bc1-7bc2-a7ef-8cd43c875111","instrumentId":"018f8f3a-7c11-73b2-b411-8e71ca121111","market":"KRX","side":"BUY","orderType":"LIMIT","timeInForce":"DAY","quantity":"100","limitPrice":"25000","cumulativeQuantity":"40","leavesQuantity":"60","canceledQuantity":"0","averagePrice":"24990","status":"PARTIALLY_FILLED","exchangeOrderId":"KRX-20260911-123456","receivedAt":"2026-09-11T01:50:00Z","acceptedAt":"2026-09-11T01:50:00.001500Z","updatedAt":"2026-09-11T01:50:01Z"}
```

### fail-body

```json
{"type":"https://api.example.com/problems/order-not-found","title":"Order not found","status":404,"code":"ORDER_NOT_FOUND","detail":"The order was not found within the permitted scope.","instance":"/api/v1/orders/{orderId}","requestId":"trading-req-002","retryable":false,"occurredAt":"2026-09-11T01:50:01Z","violations":[]}
```

---

## GET : /api/v1/orders

|항목|내용|
|:--:|:--|
|설명|접근 가능한 계좌의 주문을 Cursor 기반으로 조회한다.|
|인증|Bearer Token, `orders:read`|
|성공 Status|`200 OK`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","X-Request-Id":"trading-req-003"}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|accountId|선택 UUID, 지정 시 계좌 조회 권한 확인|
|status|선택, 복수 Enum은 쉼표로 구분|
|from/to|선택 ISO-8601 UTC, 최대 조회 기간 제한|
|cursor/limit|서버 발급 Cursor, limit 1~100|

## Response

### success-body

```json
{"items":[{"orderId":"018f8f3a-8111-76a8-87a1-c320f47b1111","clientOrderId":"algo-a-20260911-000001","accountId":"018f8f3a-6bc1-7bc2-a7ef-8cd43c875111","instrumentId":"018f8f3a-7c11-73b2-b411-8e71ca121111","side":"BUY","quantity":"100","cumulativeQuantity":"40","leavesQuantity":"60","status":"PARTIALLY_FILLED","receivedAt":"2026-09-11T01:50:00Z"}],"nextCursor":null,"hasMore":false}
```

### fail-body

```json
{"type":"https://api.example.com/problems/validation-error","title":"Validation failed","status":400,"code":"VALIDATION_ERROR","detail":"One or more query parameters are invalid.","instance":"/api/v1/orders","requestId":"trading-req-003","retryable":false,"occurredAt":"2026-09-11T01:50:02Z","violations":[{"field":"from","reason":"must be earlier than to"}]}
```

---

## POST : /api/v1/orders/{orderId}/cancellations

|항목|내용|
|:--:|:--|
|설명|주문의 전체 또는 일부 미체결 잔량 취소를 요청한다.|
|인증|Bearer Token, `orders:write`, 계좌 거래 권한|
|성공 Status|`202 Accepted`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","Content-Type":"application/json","Idempotency-Key":"cancel-20260911-000001","X-Request-Id":"trading-req-004"}
```

### body

```json
{"clientInstructionId":"cancel-a-000001","quantity":"60"}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|orderId|필수 UUID, 주문 계좌 거래 권한 확인|
|Idempotency-Key|필수, 동일 Key Payload 일치|
|clientInstructionId|필수, 주문 범위에서 유일|
|quantity|선택, 생략 시 전 잔량, 입력 시 0보다 크고 현재 잔량 이하|
|주문 상태|`NEW`, `PARTIALLY_FILLED` 등 취소 가능한 상태|

## Response

### success-body

```json
{"instructionId":"018f8f3a-9000-76a8-87a1-c320f47b1111","orderId":"018f8f3a-8111-76a8-87a1-c320f47b1111","type":"CANCEL","status":"RECEIVED","requestedAt":"2026-09-11T01:50:03Z"}
```

### fail-body

```json
{"type":"https://api.example.com/problems/order-state-conflict","title":"Order state conflict","status":409,"code":"ORDER_STATE_CONFLICT","detail":"The order cannot be canceled in its current state.","instance":"/api/v1/orders/{orderId}/cancellations","requestId":"trading-req-004","retryable":false,"occurredAt":"2026-09-11T01:50:03Z","violations":[]}
```

---

## POST : /api/v1/orders/{orderId}/replacements

|항목|내용|
|:--:|:--|
|설명|주문의 미체결 수량 또는 지정 가격 정정을 요청한다.|
|인증|Bearer Token, `orders:write`, 계좌 거래 권한|
|성공 Status|`202 Accepted`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","Content-Type":"application/json","Idempotency-Key":"replace-20260911-000001","X-Request-Id":"trading-req-005"}
```

### body

```json
{"clientInstructionId":"replace-a-000001","newLeavesQuantity":"60","newLimitPrice":"25100"}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|orderId|필수 UUID, 주문 계좌 거래 권한 확인|
|Idempotency-Key/clientInstructionId|필수, 각각 정의된 Scope에서 유일|
|newLeavesQuantity|선택, 0보다 크고 시장 정정 규칙 충족|
|newLimitPrice|선택, 호가 단위와 가격제한 충족|
|변경 필드|수량 또는 가격 중 하나 이상 입력|
|예약 증가|노출 증가분의 한도와 Asset Hold를 먼저 확보|

## Response

### success-body

```json
{"instructionId":"018f8f3a-9001-76a8-87a1-c320f47b1111","orderId":"018f8f3a-8111-76a8-87a1-c320f47b1111","type":"REPLACE","status":"RECEIVED","requestedAt":"2026-09-11T01:50:04Z"}
```

### fail-body

```json
{"type":"https://api.example.com/problems/invalid-order-price","title":"Invalid order price","status":422,"code":"INVALID_ORDER_PRICE","detail":"The replacement price does not match the tick-size rule.","instance":"/api/v1/orders/{orderId}/replacements","requestId":"trading-req-005","retryable":false,"occurredAt":"2026-09-11T01:50:04Z","violations":[{"field":"newLimitPrice","reason":"invalid tick size"}]}
```

---

## GET : /api/v1/executions/{executionId}

|항목|내용|
|:--:|:--|
|설명|고객 주문에서 발생한 개별 체결을 조회한다.|
|인증|Bearer Token, `executions:read`, 계좌 조회 권한|
|성공 Status|`200 OK`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","X-Request-Id":"trading-req-006"}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|executionId|필수 문자열 Path, 최대 길이 제한|
|계좌 권한|연결된 주문 계좌 조회 권한 확인|

## Response

### success-body

```json
{"executionId":"KRX-EXEC-501","orderId":"018f8f3a-8111-76a8-87a1-c320f47b1111","exchangeOrderId":"KRX-20260911-123456","executionPrice":"25000","executionQuantity":"40","liquidityRole":null,"executedAt":"2026-09-11T01:50:01Z","receivedAt":"2026-09-11T01:50:01.000500Z"}
```

### fail-body

```json
{"type":"https://api.example.com/problems/execution-not-found","title":"Execution not found","status":404,"code":"EXECUTION_NOT_FOUND","detail":"The execution was not found within the permitted scope.","instance":"/api/v1/executions/{executionId}","requestId":"trading-req-006","retryable":false,"occurredAt":"2026-09-11T01:50:05Z","violations":[]}
```

---

## GET : /api/v1/executions

|항목|내용|
|:--:|:--|
|설명|접근 가능한 계좌의 개별 체결 목록을 조회한다.|
|인증|Bearer Token, `executions:read`|
|성공 Status|`200 OK`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","X-Request-Id":"trading-req-007"}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|accountId/orderId|선택, 지정 Resource에 대한 계좌 권한 확인|
|from/to|선택 ISO-8601 UTC, 최대 조회 기간 제한|
|cursor/limit|서버 발급 Cursor, limit 1~100|

## Response

### success-body

```json
{"items":[{"executionId":"KRX-EXEC-501","orderId":"018f8f3a-8111-76a8-87a1-c320f47b1111","executionPrice":"25000","executionQuantity":"40","executedAt":"2026-09-11T01:50:01Z"}],"nextCursor":null,"hasMore":false}
```

### fail-body

```json
{"type":"https://api.example.com/problems/account-access-denied","title":"Account access denied","status":403,"code":"ACCOUNT_ACCESS_DENIED","detail":"The authenticated client cannot access executions for this account.","instance":"/api/v1/executions","requestId":"trading-req-007","retryable":false,"occurredAt":"2026-09-11T01:50:06Z","violations":[]}
```

---

## GET (WebSocket Upgrade) : /api/v1/stream/private

|항목|내용|
|:--:|:--|
|설명|계좌별 주문 상태와 체결 이벤트를 실시간 구독하고 sequence 이후 Replay를 요청한다.|
|인증|Bearer Token, `orders:read` 또는 `executions:read`, 계좌 권한|
|성공 Status|`101 Switching Protocols`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","Connection":"Upgrade","Upgrade":"websocket","Sec-WebSocket-Version":"13","X-Request-Id":"trading-req-008"}
```

### body

연결 이후 구독 메시지:

```json
{"type":"SUBSCRIBE","requestId":"sub-private-001","channels":[{"name":"orders","accountId":"018f8f3a-6bc1-7bc2-a7ef-8cd43c875111"},{"name":"executions","accountId":"018f8f3a-6bc1-7bc2-a7ef-8cd43c875111"}],"resumeAfterSequence":981273}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|Authorization|Handshake와 구독 시 Token·Session 상태 검증|
|channels|지원 채널과 계좌별 Scope 확인|
|resumeAfterSequence|0 이상의 sequence, Replay 보존 범위 이내|
|Backpressure|세션별 bounded queue 초과 시 연결 종료 후 Replay 요구|

## Response

### success-body

```json
{"eventId":"018f8f3a-9000-7451-9211-12d64580111","sequence":981274,"channel":"accounts/018f8f3a-6bc1-7bc2-a7ef-8cd43c875111/executions","type":"EXECUTION_CREATED","schemaVersion":1,"occurredAt":"2026-09-11T01:50:01Z","publishedAt":"2026-09-11T01:50:01.000700Z","data":{"orderId":"018f8f3a-8111-76a8-87a1-c320f47b1111","executionId":"KRX-EXEC-501","lastPrice":"25000","lastQuantity":"40","cumulativeQuantity":"40","leavesQuantity":"60","orderStatus":"PARTIALLY_FILLED"}}
```

### fail-body

```json
{"type":"ERROR","requestId":"sub-private-001","code":"REPLAY_SEQUENCE_EXPIRED","message":"The requested sequence is outside the replay retention window.","retryable":false,"recovery":"QUERY_CURRENT_STATE"}
```
