# 모듈별 API Spec

상태: `Draft v0.2 (B2C)`

이 디렉터리는 External Securities OpenAPI의 공통 계약과 업무 모듈별 Endpoint 상세 계약을 관리한다. 모든 Request/Response 예시는 구현 전 초안이며, 거래소 규정과 내부 OMS 계약이 확정되면 OpenAPI 3 문서와 함께 갱신한다.

## 문서 목록

|업무 모듈|상세 API Spec|주요 책임|
|:--:|:--|:--|
|공통|[공통 API 규약](공통-API-규약.md)|인증 Header, 오류, 멱등성, 페이지네이션, WebSocket Envelope|
|사용자|[사용자 API Spec](사용자-API.md)|가입·로그인·Token 갱신·Session|
|계좌|[계좌 API Spec](계좌-API.md)|계좌 정보·거래 권한·제한 상태|
|매매|[매매 API Spec](매매-API.md)|주문·정정·취소·체결·Private Stream|
|원장|[원장 API Spec](원장-API.md)|잔고·포지션·원장 내역|
|시장|[시장 API Spec](시장-API.md)|상품·시장 상태·시세·Market Data Stream|
|리스크·한도|[리스크·한도 API Spec](리스크-한도-API.md)|주문 사전 검증·한도·주문 가능 금액|

## 계약 소유 원칙

- 하나의 외부 Endpoint는 한 모듈의 API Spec에서만 상세 정의한다.
- 계좌의 잔고와 포지션 조회 Endpoint는 계좌가 아니라 원장 API Spec이 소유한다.
- 주문 가능 금액과 사전 주문 검증 Endpoint는 리스크·한도 API Spec이 소유한다.
- 체결 알림과 주문 상태 이벤트는 매매 Private Stream이 소유하고, 시장 시세 이벤트와 분리한다.
- 내부 운영·관리용 `/internal/v1` Endpoint는 External API와 인증·감사 정책이 다르므로 별도 설계서에서 정의한다.

## 상세 API 작성 형식

각 Endpoint는 HTTP Method와 URL, 설명·인증·성공 Status 표, Request Header와 Body, Request Validation, 성공·실패 Body 순서로 기술한다. Body가 없는 요청도 계약을 명확히 하기 위해 빈 JSON Object인 `{}`로 표시한다.
