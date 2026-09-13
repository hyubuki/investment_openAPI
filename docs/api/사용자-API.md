# 사용자 모듈 API Spec

상태: `Draft v0.1`

공통 오류 형식과 Header 정책은 [공통 API 규약](공통-API-규약.md)을 따른다.

---

## POST : /api/v1/users

|항목|내용|
|:--:|:--|
|설명|일반 사용자를 등록하고 인증 Session을 생성해 Token Pair를 함께 발급한다.|
|인증|불필요|
|성공 Status|`201 Created`|

## Request

### header

```json
{
  "Content-Type": "application/json",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580111"
}
```
### body

```json
{
  "email": "trader@example.com",
  "password": "client-secret-password"
}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|email|필수, trim 후 소문자 정규화, 이메일 형식, UTF-8 기준 최대 100 byte|
|password|필수, 8~72 byte, Password 정책 충족|
|role|Client 입력을 허용하지 않고 서버가 `USER`로 지정|
|중복 이메일|정규화된 이메일 Unique Constraint로 최종 검증|
|Session 발급|Redis Session 저장 실패 시 사용자 생성 Transaction도 실패 처리|

## Response

### success-body

```json
{
  "userId": "018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
  "email": "trader@example.com",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2026-09-11T01:00:00Z",
  "tokens": {
    "tokenType": "Bearer",
    "accessToken": "access-token-value",
    "expiresIn": 1800,
    "refreshToken": "refresh-token-value",
    "refreshExpiresIn": 86400
  }
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/email-already-exists",
  "title": "Email already exists",
  "status": 409,
  "code": "EMAIL_ALREADY_EXISTS",
  "detail": "The normalized email is already registered.",
  "instance": "/api/v1/users",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580111",
  "retryable": false,
  "occurredAt": "2026-09-11T01:00:00Z",
  "violations": []
}
```

---

## POST : /api/v1/auth/login

|항목|내용|
|:--:|:--|
|설명|이메일과 Password를 검증하고 Access/Refresh Token을 발급한다.|
|인증|불필요|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Content-Type": "application/json",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580112"
}
```

### body

```json
{
  "email": "trader@example.com",
  "password": "client-secret-password"
}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|email|필수, 이메일 형식, trim 및 소문자 정규화|
|password|필수, 최대 72 byte|
|사용자 상태|`ACTIVE` 상태만 로그인 허용|
|실패 응답|미등록 이메일과 잘못된 Password를 동일한 오류로 처리|
|호출 빈도|IP·사용자 기준 인증 Rate Limit 적용|

## Response

### success-body

```json
{
  "tokenType": "Bearer",
  "accessToken": "access-token-value",
  "expiresIn": 1800,
  "refreshToken": "refresh-token-value",
  "refreshExpiresIn": 86400
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/invalid-credentials",
  "title": "Invalid credentials",
  "status": 401,
  "code": "INVALID_CREDENTIALS",
  "detail": "The supplied credentials are invalid.",
  "instance": "/api/v1/auth/login",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580112",
  "retryable": false,
  "occurredAt": "2026-09-11T01:00:01Z",
  "violations": []
}
```

---

## POST : /api/v1/auth/refresh

|항목|내용|
|:--:|:--|
|설명|Refresh Token을 회전하고 새 Token Pair를 발급한다.|
|인증|Request Body의 Refresh Token 사용|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Content-Type": "application/json",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580113"
}
```

### body

```json
{
  "refreshToken": "refresh-token-value"
}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|refreshToken|필수, 최대 4096자, 서명·issuer·audience·만료·token_use 검증|
|Session|Redis Session이 활성 상태이고 Token Hash가 일치해야 함|
|회전|성공 시 기존 Refresh Token을 즉시 폐기|
|재사용|폐기된 Token 재사용 시 관련 Session을 폐기|

## Response

### success-body

```json
{
  "tokenType": "Bearer",
  "accessToken": "new-access-token-value",
  "expiresIn": 1800,
  "refreshToken": "new-refresh-token-value",
  "refreshExpiresIn": 86400
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/refresh-token-reused",
  "title": "Refresh token reused",
  "status": 401,
  "code": "REFRESH_TOKEN_REUSED",
  "detail": "The refresh token has already been rotated.",
  "instance": "/api/v1/auth/refresh",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580113",
  "retryable": false,
  "occurredAt": "2026-09-11T01:00:02Z",
  "violations": []
}
```

---

## POST : /api/v1/auth/logout

|항목|내용|
|:--:|:--|
|설명|현재 인증 Session을 폐기한다.|
|인증|Bearer Access Token|
|성공 Status|`204 No Content`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580114"
}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|Authorization|유효한 Access Token 필수|
|Session|Token의 `sid`와 현재 Session이 일치해야 함|
|멱등성|이미 폐기된 Session의 재요청 정책은 `204` 반환으로 통일|

## Response

### success-body

```json
{}
```

실제 `204` 응답에는 Body가 없다.

### fail-body

```json
{
  "type": "https://api.example.com/problems/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "code": "UNAUTHORIZED",
  "detail": "A valid access token is required.",
  "instance": "/api/v1/auth/logout",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580114",
  "retryable": false,
  "occurredAt": "2026-09-11T01:00:03Z",
  "violations": []
}
```

---

## GET : /api/v1/users/me

|항목|내용|
|:--:|:--|
|설명|현재 인증된 사용자 정보를 조회한다.|
|인증|Bearer Access Token|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580115"
}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|Authorization|Access Token의 서명·만료·Session 상태 검증|
|사용자 상태|`ACTIVE` 상태만 조회 허용|
|사용자 식별자|Request 값이 아닌 Security Principal의 `userId` 사용|

## Response

### success-body

```json
{
  "userId": "018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
  "email": "trader@example.com",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2026-09-11T01:00:00Z"
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/user-inactive",
  "title": "User inactive",
  "status": 401,
  "code": "USER_INACTIVE",
  "detail": "The authenticated user is not active.",
  "instance": "/api/v1/users/me",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580115",
  "retryable": false,
  "occurredAt": "2026-09-11T01:00:04Z",
  "violations": []
}
```

---

## GET : /api/v1/api-clients

|항목|내용|
|:--:|:--|
|설명|현재 고객이 관리할 수 있는 API Client 목록을 조회한다.|
|인증|Bearer Access Token, 고객 관리자 권한|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580116"
}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|status|선택 Query, 허용 Enum만 사용|
|cursor|선택, 서버 발급 불투명 Cursor|
|limit|1~100, 기본값 20|
|고객 권한|현재 사용자와 유효한 관계가 있는 고객의 Client만 조회|

## Response

### success-body

```json
{
  "items": [
    {
      "apiClientId": "018f8f3a-7111-7e19-b520-667b62580111",
      "name": "institutional-algo",
      "clientType": "CONFIDENTIAL",
      "status": "ACTIVE",
      "scopes": ["orders:read", "orders:write", "market:read"]
    }
  ],
  "nextCursor": null,
  "hasMore": false
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/customer-access-denied",
  "title": "Customer access denied",
  "status": 403,
  "code": "CUSTOMER_ACCESS_DENIED",
  "detail": "The user cannot manage API clients for this customer.",
  "instance": "/api/v1/api-clients",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580116",
  "retryable": false,
  "occurredAt": "2026-09-11T01:00:05Z",
  "violations": []
}
```

---

## POST : /api/v1/api-clients

|항목|내용|
|:--:|:--|
|설명|고객 소유의 API Client를 생성한다.|
|인증|Bearer Access Token, 고객 관리자 권한|
|성공 Status|`201 Created`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "Content-Type": "application/json",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580117"
}
```

### body

```json
{
  "customerId": "018f8f3a-6111-7e19-b520-667b62580111",
  "name": "institutional-algo",
  "clientType": "CONFIDENTIAL",
  "scopes": ["orders:read", "orders:write", "market:read"],
  "accountPermissions": [
    {
      "accountId": "018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
      "permission": "TRADE"
    }
  ]
}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|customerId|필수 UUID, 현재 사용자의 관리 권한 확인|
|name|필수, 1~100자|
|clientType|허용 Enum 값|
|scopes|지원 Scope만 허용하고 요청자의 위임 가능 범위를 초과하지 않음|
|accountPermissions|해당 고객과 관계있는 계좌만 허용|

## Response

### success-body

```json
{
  "apiClientId": "018f8f3a-7111-7e19-b520-667b62580111",
  "customerId": "018f8f3a-6111-7e19-b520-667b62580111",
  "name": "institutional-algo",
  "clientType": "CONFIDENTIAL",
  "status": "ACTIVE",
  "scopes": ["orders:read", "orders:write", "market:read"],
  "createdAt": "2026-09-11T01:00:06Z"
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/scope-not-granted",
  "title": "Scope not granted",
  "status": 403,
  "code": "SCOPE_NOT_GRANTED",
  "detail": "One or more requested scopes cannot be delegated.",
  "instance": "/api/v1/api-clients",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580117",
  "retryable": false,
  "occurredAt": "2026-09-11T01:00:06Z",
  "violations": []
}
```

---

## GET : /api/v1/api-clients/{apiClientId}

|항목|내용|
|:--:|:--|
|설명|API Client의 상태, Scope와 계좌 권한을 조회한다.|
|인증|Bearer Access Token, Client 소유 고객 관리자|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580118"
}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|apiClientId|필수 UUID Path Variable|
|고객 권한|Client 소유 고객에 대한 관리자 권한 확인|

## Response

### success-body

```json
{
  "apiClientId": "018f8f3a-7111-7e19-b520-667b62580111",
  "name": "institutional-algo",
  "clientType": "CONFIDENTIAL",
  "status": "ACTIVE",
  "scopes": ["orders:read", "orders:write", "market:read"],
  "accountPermissions": [
    {
      "accountId": "018f8f3a-6bc1-7bc2-a7ef-8cd43c875111",
      "permission": "TRADE"
    }
  ]
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/api-client-not-found",
  "title": "API client not found",
  "status": 404,
  "code": "API_CLIENT_NOT_FOUND",
  "detail": "The API client was not found within the permitted scope.",
  "instance": "/api/v1/api-clients/018f8f3a-7111-7e19-b520-667b62580111",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580118",
  "retryable": false,
  "occurredAt": "2026-09-11T01:00:07Z",
  "violations": []
}
```

---

## PATCH : /api/v1/api-clients/{apiClientId}

|항목|내용|
|:--:|:--|
|설명|API Client 이름, 상태, Scope 또는 계좌 권한을 변경한다.|
|인증|Bearer Access Token, Client 소유 고객 관리자|
|성공 Status|`200 OK`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "Content-Type": "application/json",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580119"
}
```

### body

```json
{
  "name": "institutional-algo-v2",
  "status": "ACTIVE",
  "scopes": ["orders:read", "market:read"]
}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|apiClientId|필수 UUID, Client 소유 고객 권한 확인|
|변경 필드|최소 하나 이상 존재|
|status|허용된 상태 전이만 가능|
|scopes|지원 Scope이며 요청자의 위임 가능 범위 이내|
|동시 변경|`@Version` 또는 조건부 UPDATE로 유실 방지|

## Response

### success-body

```json
{
  "apiClientId": "018f8f3a-7111-7e19-b520-667b62580111",
  "name": "institutional-algo-v2",
  "status": "ACTIVE",
  "scopes": ["orders:read", "market:read"],
  "updatedAt": "2026-09-11T01:00:08Z"
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/api-client-state-conflict",
  "title": "API client state conflict",
  "status": 409,
  "code": "API_CLIENT_STATE_CONFLICT",
  "detail": "The API client cannot transition to the requested state.",
  "instance": "/api/v1/api-clients/018f8f3a-7111-7e19-b520-667b62580111",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580119",
  "retryable": false,
  "occurredAt": "2026-09-11T01:00:08Z",
  "violations": []
}
```

---

## POST : /api/v1/api-clients/{apiClientId}/credentials

|항목|내용|
|:--:|:--|
|설명|API Client Credential을 생성하고 Secret을 최초 한 번 반환한다.|
|인증|Bearer Access Token, Client 소유 고객 관리자|
|성공 Status|`201 Created`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "Content-Type": "application/json",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580120"
}
```

### body

```json
{
  "expiresAt": "2027-09-11T00:00:00Z"
}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|apiClientId|활성 API Client UUID|
|expiresAt|현재보다 미래이며 정책상 최대 유효기간 이내|
|활성 Credential 수|Client별 최대 활성 Credential 수 제한|

## Response

### success-body

```json
{
  "credentialId": "018f8f3a-8111-7e19-b520-667b62580111",
  "keyPrefix": "inv_live_ab12",
  "secret": "secret-visible-only-once",
  "expiresAt": "2027-09-11T00:00:00Z",
  "createdAt": "2026-09-11T01:00:09Z"
}
```

### fail-body

```json
{
  "type": "https://api.example.com/problems/credential-limit-exceeded",
  "title": "Credential limit exceeded",
  "status": 422,
  "code": "CREDENTIAL_LIMIT_EXCEEDED",
  "detail": "The API client has reached its active credential limit.",
  "instance": "/api/v1/api-clients/018f8f3a-7111-7e19-b520-667b62580111/credentials",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580120",
  "retryable": false,
  "occurredAt": "2026-09-11T01:00:09Z",
  "violations": []
}
```

---

## DELETE : /api/v1/api-clients/{apiClientId}/credentials/{credentialId}

|항목|내용|
|:--:|:--|
|설명|API Credential을 즉시 폐기한다.|
|인증|Bearer Access Token, Client 소유 고객 관리자|
|성공 Status|`204 No Content`|

## Request

### header

```json
{
  "Authorization": "Bearer {accessToken}",
  "X-Request-Id": "018f8f3a-6000-7e19-b520-667b62580121"
}
```

### body

```json
{}
```

### Request Validation

|검증항목|검증사항|
|:--:|:--|
|apiClientId|필수 UUID, 관리 권한 확인|
|credentialId|해당 API Client 소유 Credential UUID|
|멱등성|이미 폐기된 Credential은 `204` 반환|

## Response

### success-body

```json
{}
```

실제 `204` 응답에는 Body가 없다.

### fail-body

```json
{
  "type": "https://api.example.com/problems/api-client-access-denied",
  "title": "API client access denied",
  "status": 403,
  "code": "API_CLIENT_ACCESS_DENIED",
  "detail": "The credential cannot be managed by the authenticated user.",
  "instance": "/api/v1/api-clients/018f8f3a-7111-7e19-b520-667b62580111/credentials/018f8f3a-8111-7e19-b520-667b62580111",
  "requestId": "018f8f3a-6000-7e19-b520-667b62580121",
  "retryable": false,
  "occurredAt": "2026-09-11T01:00:10Z",
  "violations": []
}
```
