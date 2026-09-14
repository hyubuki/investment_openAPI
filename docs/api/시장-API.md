# 시장 모듈 API Spec

상태: `Draft v0.2 (B2C)`

공통 오류 형식과 Header 정책은 [공통 API 규약](공통-API-규약.md)을 따른다. 가격·수량은 문자열로 반환하며 시세 응답은 거래소 sequence와 시각을 포함한다.

---

## GET : /api/v1/instruments

|항목|내용|
|:--:|:--|
|설명|시장·상품 코드·상품명으로 거래 상품을 검색한다.|
|인증|Bearer Access Token|
|성공 Status|`200 OK`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","X-Request-Id":"market-req-001"}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|market|선택 Query, 지원 시장 코드|
|query|선택, Symbol·ISIN·상품명, 최대 100자|
|status|선택, 지원 상품 상태 Enum|
|cursor/limit|서버 발급 Cursor, limit 1~100|

## Response

### success-body

```json
{"items":[{"instrumentId":"018f8f3a-7c11-73b2-b411-8e71ca121111","symbol":"005930","name":"삼성전자","market":"KRX","assetClass":"EQUITY","currency":"KRW","status":"ACTIVE"}],"nextCursor":null,"hasMore":false}
```

### fail-body

```json
{"type":"https://api.example.com/problems/validation-error","title":"Validation failed","status":400,"code":"VALIDATION_ERROR","detail":"The market parameter is invalid.","instance":"/api/v1/instruments","requestId":"market-req-001","retryable":false,"occurredAt":"2026-09-11T01:20:00Z","violations":[{"field":"market","reason":"unsupported market"}]}
```

---

## GET : /api/v1/instruments/{instrumentId}

|항목|내용|
|:--:|:--|
|설명|상품 기준정보와 주문 단위를 조회한다.|
|인증|Bearer Access Token|
|성공 Status|`200 OK`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","X-Request-Id":"market-req-002"}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|instrumentId|필수 UUID Path Variable|
|상품 상태|상장폐지 상품도 조회 가능하나 `tradable`은 false|

## Response

### success-body

```json
{"instrumentId":"018f8f3a-7c11-73b2-b411-8e71ca121111","symbol":"005930","isin":"KR7005930003","name":"삼성전자","market":"KRX","assetClass":"EQUITY","currency":"KRW","quantityScale":0,"priceScale":0,"lotSize":"1","status":"ACTIVE","tradable":true}
```

### fail-body

```json
{"type":"https://api.example.com/problems/instrument-not-found","title":"Instrument not found","status":404,"code":"INSTRUMENT_NOT_FOUND","detail":"The instrument does not exist.","instance":"/api/v1/instruments/{instrumentId}","requestId":"market-req-002","retryable":false,"occurredAt":"2026-09-11T01:20:01Z","violations":[]}
```

---

## GET : /api/v1/markets

|항목|내용|
|:--:|:--|
|설명|OpenAPI가 지원하는 시장 목록을 조회한다.|
|인증|Bearer Access Token|
|성공 Status|`200 OK`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","X-Request-Id":"market-req-003"}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|status|선택 Query, `ACTIVE` 또는 `INACTIVE`|
|Authorization|유효한 Bearer Access Token 필수|

## Response

### success-body

```json
{"items":[{"market":"KRX","name":"Korea Exchange","timezone":"Asia/Seoul","status":"ACTIVE"}]}
```

### fail-body

```json
{"type":"https://api.example.com/problems/unauthorized","title":"Unauthorized","status":401,"code":"UNAUTHORIZED","detail":"A valid access token is required.","instance":"/api/v1/markets","requestId":"market-req-003","retryable":false,"occurredAt":"2026-09-11T01:20:02Z","violations":[]}
```

---

## GET : /api/v1/markets/{market}/status

|항목|내용|
|:--:|:--|
|설명|현재 시장 세션과 주문 가능 상태를 조회한다.|
|인증|Bearer Access Token|
|성공 Status|`200 OK`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","X-Request-Id":"market-req-004"}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|market|필수 Path, 지원 시장 코드|
|상태 최신성|거래소 상태 sequence와 수신 시각이 허용 범위 이내|

## Response

### success-body

```json
{"market":"KRX","tradingDate":"2026-09-11","sessionType":"REGULAR","state":"OPEN","orderEntryAllowed":true,"asOf":"2026-09-11T01:20:03Z"}
```

### fail-body

```json
{"type":"https://api.example.com/problems/market-data-not-available","title":"Market status not available","status":503,"code":"MARKET_DATA_NOT_AVAILABLE","detail":"A current market status is not available.","instance":"/api/v1/markets/{market}/status","requestId":"market-req-004","retryable":true,"occurredAt":"2026-09-11T01:20:03Z","violations":[]}
```

---

## GET : /api/v1/markets/{market}/calendar

|항목|내용|
|:--:|:--|
|설명|시장 거래일과 예정 세션을 기간으로 조회한다.|
|인증|Bearer Access Token|
|성공 Status|`200 OK`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","X-Request-Id":"market-req-005"}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|market|필수 Path, 지원 시장 코드|
|from/to|필수 `yyyy-MM-dd`, `from <= to`|
|조회 기간|최대 1년|

## Response

### success-body

```json
{"market":"KRX","items":[{"tradingDate":"2026-09-11","businessDay":true,"holidayName":null,"sessions":[{"sessionType":"REGULAR","opensAt":"2026-09-11T00:00:00Z","closesAt":"2026-09-11T06:30:00Z"}]}]}
```

### fail-body

```json
{"type":"https://api.example.com/problems/validation-error","title":"Validation failed","status":400,"code":"VALIDATION_ERROR","detail":"The requested calendar range is invalid.","instance":"/api/v1/markets/{market}/calendar","requestId":"market-req-005","retryable":false,"occurredAt":"2026-09-11T01:20:04Z","violations":[{"field":"to","reason":"range must not exceed one year"}]}
```

---

## GET : /api/v1/market-data/quotes/{instrumentId}

|항목|내용|
|:--:|:--|
|설명|상품의 최신 체결가와 최우선 매수·매도 호가를 조회한다.|
|인증|Bearer Access Token|
|성공 Status|`200 OK`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","X-Request-Id":"market-req-006"}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|instrumentId|필수 UUID, 존재하는 상품|
|시세 최신성|시장 상태별 허용 지연 시간 이내|
|sequence|마지막 적용 거래소 sequence를 포함|

## Response

### success-body

```json
{"instrumentId":"018f8f3a-7c11-73b2-b411-8e71ca121111","lastPrice":"25000","bestBidPrice":"24950","bestBidQuantity":"1500","bestAskPrice":"25000","bestAskQuantity":"700","marketState":"OPEN","exchangeSequence":31892011,"exchangeTimestamp":"2026-09-11T01:20:05Z","receivedAt":"2026-09-11T01:20:05.000400Z"}
```

### fail-body

```json
{"type":"https://api.example.com/problems/market-data-not-available","title":"Market data not available","status":503,"code":"MARKET_DATA_NOT_AVAILABLE","detail":"The latest quote does not satisfy the freshness requirement.","instance":"/api/v1/market-data/quotes/{instrumentId}","requestId":"market-req-006","retryable":true,"occurredAt":"2026-09-11T01:20:05Z","violations":[]}
```

---

## GET : /api/v1/market-data/order-books/{instrumentId}

|항목|내용|
|:--:|:--|
|설명|상품의 지정 깊이 호가 Snapshot을 조회한다.|
|인증|Bearer Access Token|
|성공 Status|`200 OK`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","X-Request-Id":"market-req-007"}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|instrumentId|필수 UUID, 존재하는 상품|
|depth|선택 Query, 1~10, 기본값 5|
|Snapshot 완전성|sequence gap 없이 구성된 Snapshot만 반환|

## Response

### success-body

```json
{"instrumentId":"018f8f3a-7c11-73b2-b411-8e71ca121111","bids":[{"level":1,"price":"24950","quantity":"1500"}],"asks":[{"level":1,"price":"25000","quantity":"700"}],"exchangeSequence":31892012,"exchangeTimestamp":"2026-09-11T01:20:06Z","receivedAt":"2026-09-11T01:20:06.000400Z"}
```

### fail-body

```json
{"type":"https://api.example.com/problems/sequence-gap","title":"Order book sequence gap","status":503,"code":"SEQUENCE_GAP","detail":"The order book snapshot is being resynchronized.","instance":"/api/v1/market-data/order-books/{instrumentId}","requestId":"market-req-007","retryable":true,"occurredAt":"2026-09-11T01:20:06Z","violations":[]}
```

---

## GET : /api/v1/market-data/trades/{instrumentId}

|항목|내용|
|:--:|:--|
|설명|상품에서 발생한 시장 전체 체결 Tick을 조회한다. 사용자 주문 Execution과 구분한다.|
|인증|Bearer Access Token|
|성공 Status|`200 OK`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","X-Request-Id":"market-req-008"}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|instrumentId|필수 UUID, 존재하는 상품|
|cursor/limit|서버 발급 Cursor, limit 1~500|
|from/to|선택 ISO-8601 UTC, 보존 범위 이내|

## Response

### success-body

```json
{"items":[{"tradeId":"KRX-TRADE-90001","price":"25000","quantity":"40","exchangeSequence":31892013,"executedAt":"2026-09-11T01:20:07Z"}],"nextCursor":null,"hasMore":false}
```

### fail-body

```json
{"type":"https://api.example.com/problems/instrument-not-found","title":"Instrument not found","status":404,"code":"INSTRUMENT_NOT_FOUND","detail":"The instrument does not exist.","instance":"/api/v1/market-data/trades/{instrumentId}","requestId":"market-req-008","retryable":false,"occurredAt":"2026-09-11T01:20:07Z","violations":[]}
```

---

## GET (WebSocket Upgrade) : /api/v1/stream/market-data

|항목|내용|
|:--:|:--|
|설명|Quote, Order Book과 시장 체결 이벤트를 상품별로 구독한다.|
|인증|Bearer Access Token|
|성공 Status|`101 Switching Protocols`|

## Request

### header

```json
{"Authorization":"Bearer {accessToken}","Connection":"Upgrade","Upgrade":"websocket","Sec-WebSocket-Version":"13","X-Request-Id":"market-req-009"}
```

### body

연결 이후 구독 메시지:

```json
{"type":"SUBSCRIBE","requestId":"sub-market-001","channels":[{"name":"quotes","instrumentIds":["018f8f3a-7c11-73b2-b411-8e71ca121111"]},{"name":"order-books","depth":10,"instrumentIds":["018f8f3a-7c11-73b2-b411-8e71ca121111"]}]}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|Authorization|Handshake와 구독 처리 시 Access Token·Session 검증|
|channels.name|지원 채널만 허용|
|instrumentIds|존재하는 상품, Client별 최대 구독 수 제한|
|depth|Order Book 채널에서만 1~10|
|재연결|Order Book REST Snapshot 이후 새 sequence부터 적용|

## Response

### success-body

```json
{"eventId":"018f8f3a-9000-7451-9211-12d64580309","sequence":31892014,"channel":"market-data/quotes/018f8f3a-7c11-73b2-b411-8e71ca121111","type":"QUOTE_UPDATED","schemaVersion":1,"occurredAt":"2026-09-11T01:20:08Z","publishedAt":"2026-09-11T01:20:08.000500Z","data":{"lastPrice":"25000","bestBidPrice":"24950","bestAskPrice":"25000"}}
```

### fail-body

```json
{"type":"ERROR","requestId":"sub-market-001","code":"SUBSCRIPTION_LIMIT_EXCEEDED","message":"The instrument subscription limit has been exceeded.","retryable":false}
```
