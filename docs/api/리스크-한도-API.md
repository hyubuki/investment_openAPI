# 리스크·한도 모듈 API Spec

상태: `Draft v0.1`

공통 오류 형식과 Header 정책은 [공통 API 규약](공통-API-규약.md)을 따른다. Preview 결과는 실제 주문 승인이나 자원 예약을 보장하지 않는다.

---

## POST : /api/v1/order-previews

|항목|내용|
|:--:|:--|
|설명|현재 시세·잔고·정책을 기준으로 주문 가능 여부와 예상 필요 금액을 조회한다.|
|인증|Bearer Token, `orders:read`, 계좌 조회 권한|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "Content-Type": "application/json",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580501"
}
```

### body

```json
{
  "accountId": "018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
  "instrumentId": "018f8f3a-7c11-73b2-b411-8e71ca121111",
  "market": "KRX",
  "side": "BUY",
  "orderType": "LIMIT",
  "quantity": "100",
  "limitPrice": "25000"
}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|accountId|필수 UUID, 계좌 조회 권한과 상태 확인|
|instrumentId/market|필수, 상품과 시장의 관계 및 거래 가능 상태 확인|
|side|`BUY`, `SELL` 중 하나|
|orderType|지원 주문 유형|
|quantity|문자열 Decimal, 0보다 크고 상품 거래 단위 충족|
|limitPrice|지정가 주문은 필수, 호가 단위와 가격제한 충족|
|데이터 최신성|시세와 원장 Projection이 허용 지연 기준 이내|

## Response

### success-body

```json
{
  "allowed": true,
  "estimatedOrderAmount": "2500000",
  "estimatedFees": "375",
  "requiredBuyingPower": "2500375",
  "availableBuyingPower": "6500000",
  "policyVersion": 12,
  "priceAsOf": "2026-09-11T01:40:00Z",
  "expiresAt": "2026-09-11T01:40:02Z",
  "warnings": []
}
```

업무 규칙상 주문이 불가능한 경우에도 평가가 정상 수행되었다면 `200 OK`와 `allowed: false`를 반환하고 사유 코드를 포함한다.

### fail-body

```json
{
  "type": "https://api.example.com/problems/risk-check-unavailable",
  "title": "Risk check unavailable",
  "status": 503,
  "code": "RISK_CHECK_UNAVAILABLE",
  "detail": "Required market or ledger data is not sufficiently current.",
  "instance": "/api/v1/order-previews",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580501",
  "retryable": true,
  "occurredAt": "2026-09-11T01:40:00Z",
  "violations": []
}
```

---

## GET : /api/v1/accounts/{accountId}/trading-limits

|항목|내용|
|:--:|:--|
|설명|계좌에 적용되는 외부 공개 가능 거래 한도를 조회한다.|
|인증|Bearer Token, `accounts:read`, 계좌 조회 권한|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580502"
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
|instrumentId|선택 UUID, market과의 관계 확인|
|공개 범위|내부 탐지 Rule과 비공개 Threshold를 응답에서 제외|

## Response

### success-body

```json
{
  "accountId": "018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
  "policyVersion": 12,
  "items": [
    {
      "limitType": "MAX_ORDER_AMOUNT",
      "scopeType": "MARKET",
      "scopeId": "KRX",
      "value": "100000000",
      "unit": "AMOUNT",
      "currency": "KRW",
      "action": "REJECT"
    }
  ],
  "asOf": "2026-09-11T01:40:01Z"
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/account-access-denied",
  "title": "Account access denied",
  "status": 403,
  "code": "ACCOUNT_ACCESS_DENIED",
  "detail": "The authenticated client cannot access limits for this account.",
  "instance": "/api/v1/accounts/018f8f3a-6bc1-7bc2-a7ef-8cd43c875111/trading-limits",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580502",
  "retryable": false,
  "occurredAt": "2026-09-11T01:40:01Z",
  "violations": []
}
```

---

## GET : /api/v1/accounts/{accountId}/buying-power

|항목|내용|
|:--:|:--|
|설명|계좌의 현재 주문 가능 금액 또는 매도 가능 수량을 조회한다.|
|인증|Bearer Token, `balances:read`, 계좌 조회 권한|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580503"
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
|instrumentId|필수 UUID|
|side|선택 Query, 기본값 `BUY`|
|limitPrice|선택 문자열 Decimal, 지정 시 호가 단위 검증|
|원장 최신성|가용 잔고와 활성 Hold의 기준 sequence 확인|

## Response

### success-body

```json
{
  "accountId": "018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
  "instrumentId": "018f8f3a-7c11-73b2-b411-8e71ca121111",
  "side": "BUY",
  "currency": "KRW",
  "availableBuyingPower": "6500000",
  "availableQuantity": null,
  "ledgerSequence": 712003,
  "policyVersion": 12,
  "asOf": "2026-09-11T01:40:02Z"
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/balance-not-available",
  "title": "Balance not available",
  "status": 503,
  "code": "BALANCE_NOT_AVAILABLE",
  "detail": "Buying power cannot be calculated from the current ledger state.",
  "instance": "/api/v1/accounts/018f8f3a-6bc1-7bc2-a7ef-8cd43c875111/buying-power",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580503",
  "retryable": true,
  "occurredAt": "2026-09-11T01:40:02Z",
  "violations": []
}
```
