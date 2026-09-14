# 원장 모듈 API Spec

상태: `Draft v0.2 (B2C)`

공통 오류 형식과 Header 정책은 [공통 API 규약](공통-API-규약.md)을 따른다. 외부 Client가 Posting을 직접 생성·수정·삭제하는 API는 제공하지 않는다.

---

## GET : /api/v1/accounts/{accountId}/balances

|항목|내용|
|:--:|:--|
|설명|계좌의 통화별 결제·결제예정·예약·가용 현금 잔고를 조회한다.|
|인증|Bearer Access Token, 본인 계좌|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580401"
}
```
### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|accountId|필수 UUID, 계좌 조회 권한 확인|
|currency|선택 Query, ISO 4217 통화 코드|
|Projection 최신성|`asOfSequence`와 허용 지연 기준을 충족해야 함|
|금액 표현|모든 금액을 정밀도 보존을 위한 문자열로 반환|

## Response

### success-body

```json
{
  "accountId": "018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
  "currency": "KRW",
  "settledAmount": "10000000",
  "pendingSettlementAmount": "-1000000",
  "reservedAmount": "2500000",
  "availableAmount": "6500000",
  "asOfSequence": 712003,
  "asOf": "2026-09-11T01:30:00Z"
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/balance-not-available",
  "title": "Balance not available",
  "status": 503,
  "code": "BALANCE_NOT_AVAILABLE",
  "detail": "A sufficiently recent balance is not available.",
  "instance": "/api/v1/accounts/018f8f3a-6bc1-7bc2-a7ef-8cd43c875111/balances",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580401",
  "retryable": true,
  "occurredAt": "2026-09-11T01:30:00Z",
  "violations": []
}
```

---

## GET : /api/v1/accounts/{accountId}/positions

|항목|내용|
|:--:|:--|
|설명|계좌의 상품별 결제·결제예정·예약·가용 수량과 평균 취득가를 조회한다.|
|인증|Bearer Access Token, 본인 계좌|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580402"
}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|accountId|필수 UUID, 계좌 조회 권한 확인|
|market|선택 Query, 지원 시장 코드|
|cursor|선택, 서버 발급 불투명 Cursor|
|limit|1~100, 기본값 20|
|Projection 최신성|목록 전체의 `asOf`와 개별 `asOfSequence` 확인|

## Response

### success-body

```json
{
  "items": [
    {
      "instrumentId": "018f8f3a-7c11-73b2-b411-8e71ca121111",
      "settledQuantity": "100",
      "pendingSettlementQuantity": "40",
      "reservedQuantity": "20",
      "availableQuantity": "80",
      "averageCost": "24500",
      "asOfSequence": 712003
    }
  ],
  "nextCursor": null,
  "hasMore": false,
  "asOf": "2026-09-11T01:30:01Z"
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/account-not-found",
  "title": "Account not found",
  "status": 404,
  "code": "ACCOUNT_NOT_FOUND",
  "detail": "The account was not found within the permitted scope.",
  "instance": "/api/v1/accounts/018f8f3a-6bc1-7bc2-a7ef-8cd43c875111/positions",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580402",
  "retryable": false,
  "occurredAt": "2026-09-11T01:30:01Z",
  "violations": []
}
```

---

## GET : /api/v1/accounts/{accountId}/ledger-entries

|항목|내용|
|:--:|:--|
|설명|계좌의 입출금·매매·수수료·세금·보정 내역을 업무 관점으로 조회한다.|
|인증|Bearer Access Token, 본인 계좌|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580403"
}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|accountId|필수 UUID, 계좌 조회 권한 확인|
|type|선택 Query, `TRADE`, `DEPOSIT`, `WITHDRAWAL`, `FEE`, `TAX`, `REVERSAL`|
|from/to|선택 ISO-8601 UTC, `from < to`, 최대 조회 기간 제한|
|cursor|선택, 서버 발급 Cursor|
|limit|1~100, 기본값 20|

## Response

### success-body

```json
{
  "items": [
    {
      "ledgerEntryId": "018f8f3a-9000-7e19-b520-667b62580403",
      "type": "TRADE",
      "businessEventId": "KRX-EXEC-501",
      "instrumentId": "018f8f3a-7c11-73b2-b411-8e71ca121111",
      "quantity": "40",
      "cashAmount": "-1000000",
      "currency": "KRW",
      "effectiveAt": "2026-09-11T01:29:59Z",
      "recordedAt": "2026-09-11T01:30:00Z"
    }
  ],
  "nextCursor": null,
  "hasMore": false
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/validation-error",
  "title": "Validation failed",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "detail": "One or more query parameters are invalid.",
  "instance": "/api/v1/accounts/018f8f3a-6bc1-7bc2-a7ef-8cd43c875111/ledger-entries",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580403",
  "retryable": false,
  "occurredAt": "2026-09-11T01:30:02Z",
  "violations": [
    {
      "field": "from",
      "reason": "must be earlier than to"
    }
  ]
}
```
