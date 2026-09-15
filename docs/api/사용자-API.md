# 사용자 모듈 API Spec

상태: `Draft v0.2 (B2C)`

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
|실패 횟수|등록된 활성 사용자의 Password가 연속으로 기본 5회 실패하면 계정을 잠금|
|잠금 시간|마지막 실패 시각부터 기본 15분 동안 로그인 차단, 만료 후 다음 로그인에서 자동 해제|
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

Access Token 검증 시 Token의 `sid`가 Redis의 현재 사용자 Session과 일치해야 한다.
Redis 또는 사용자 저장소에서 인증 상태를 확인할 수 없으면 요청을 우회시키지 않고
`503 AUTHENTICATION_UNAVAILABLE`로 거부한다.

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
