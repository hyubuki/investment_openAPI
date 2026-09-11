# 계좌 모듈 API Spec

상태: `Draft v0.1`

공통 오류 형식과 Header 정책은 [공통 API 규약](공통-API-규약.md)을 따른다.

---

## GET : /api/v1/accounts

|항목|내용|
|:--:|:--|
|설명|인증 주체가 접근할 수 있는 거래 계좌 목록을 조회한다.|
|인증|Bearer Token, `accounts:read`|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580201"
}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|status|선택 Query, `PENDING`, `ACTIVE`, `RESTRICTED`, `SUSPENDED`, `CLOSED` 중 하나|
|cursor|선택, 서버 발급 불투명 Cursor|
|limit|1~100, 기본값 20|
|접근 범위|Security Principal의 고객·API Client 계좌 권한으로 제한|

## Response

### success-body

```json
{
  "items": [
    {
      "accountId": "018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
      "accountNumberMasked": "123-****-7890",
      "accountType": "CASH",
      "baseCurrency": "KRW",
      "status": "ACTIVE"
    }
  ],
  "nextCursor": null,
  "hasMore": false
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/scope-not-granted",
  "title": "Scope not granted",
  "status": 403,
  "code": "SCOPE_NOT_GRANTED",
  "detail": "The accounts:read scope is required.",
  "instance": "/api/v1/accounts",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580201",
  "retryable": false,
  "occurredAt": "2026-09-11T01:10:00Z",
  "violations": []
}
```

---

## GET : /api/v1/accounts/{accountId}

|항목|내용|
|:--:|:--|
|설명|거래 계좌의 기본 정보와 현재 상태를 조회한다.|
|인증|Bearer Token, `accounts:read`, 계좌 조회 권한|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580202"
}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|accountId|필수 UUID Path Variable|
|계좌 권한|현재 사용자/API Client가 계좌를 조회할 수 있어야 함|
|민감정보|전체 계좌번호를 응답하거나 로그에 기록하지 않음|

## Response

### success-body

```json
{
  "accountId": "018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
  "accountNumberMasked": "123-****-7890",
  "accountType": "CASH",
  "baseCurrency": "KRW",
  "status": "ACTIVE",
  "openedAt": "2026-09-01T00:00:00Z",
  "restrictionsPresent": false
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
  "instance": "/api/v1/accounts/018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580202",
  "retryable": false,
  "occurredAt": "2026-09-11T01:10:01Z",
  "violations": []
}
```

---

## GET : /api/v1/accounts/{accountId}/trading-permissions

|항목|내용|
|:--:|:--|
|설명|계좌의 시장·상품·방향·주문 유형별 거래 권한을 조회한다.|
|인증|Bearer Token, `accounts:read`, 계좌 조회 권한|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580203"
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
|activeOnly|선택 Boolean, 기본값 `true`|
|유효기간|조회 시점이 `effectiveFrom` 이상, `effectiveUntil` 미만인 권한만 활성|

## Response

### success-body

```json
{
  "accountId": "018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
  "items": [
    {
      "market": "KRX",
      "assetClass": "EQUITY",
      "side": "BOTH",
      "orderType": "LIMIT",
      "status": "ACTIVE",
      "effectiveFrom": "2026-09-01T00:00:00Z",
      "effectiveUntil": null
    }
  ]
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/account-access-denied",
  "title": "Account access denied",
  "status": 403,
  "code": "ACCOUNT_ACCESS_DENIED",
  "detail": "The authenticated client cannot access this account.",
  "instance": "/api/v1/accounts/018f8f3a-6bc1-7bc2-a7ef-8cd43c875111/trading-permissions",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580203",
  "retryable": false,
  "occurredAt": "2026-09-11T01:10:02Z",
  "violations": []
}
```

---

## GET : /api/v1/accounts/{accountId}/restrictions

|항목|내용|
|:--:|:--|
|설명|계좌에 적용된 거래 및 출금 제한을 조회한다.|
|인증|Bearer Token, `accounts:read`, 계좌 조회 권한|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580204"
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
|activeOnly|선택 Boolean, 기본값 `true`|
|cursor|선택, 서버 발급 Cursor|
|limit|1~100, 기본값 20|

## Response

### success-body

```json
{
  "items": [
    {
      "restrictionId": "018f8f3a-8000-7e19-b520-667b62580204",
      "restrictionType": "BUY_BLOCKED",
      "reasonCode": "COMPLIANCE_REVIEW",
      "effectiveFrom": "2026-09-11T00:00:00Z",
      "effectiveUntil": null
    }
  ],
  "nextCursor": null,
  "hasMore": false
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/account-access-denied",
  "title": "Account access denied",
  "status": 403,
  "code": "ACCOUNT_ACCESS_DENIED",
  "detail": "The authenticated client cannot access this account.",
  "instance": "/api/v1/accounts/018f8f3a-6bc1-7bc2-a7ef-8cd43c875111/restrictions",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580204",
  "retryable": false,
  "occurredAt": "2026-09-11T01:10:03Z",
  "violations": []
}
```

## 관련 API

URI가 계좌 하위에 있더라도 잔고와 포지션 데이터는 원장 모듈이 관리한다. 상세 계약은 [원장 모듈 API Spec](원장-API.md)을 따른다.
