# Integration Spec

Frontend ↔ Backend 연결과 실행 환경에 대한 요구사항 (WHAT).
범위: API 계약 · Docker · 환경 변수.
상위 문서: `specs/product-spec.md` (`P-xx`), `specs/backend/spec.md` (`B-xx`). 원본: `ASSIGNMENT.md` §2–3, §5, §7, `README.md` `## 실행`.

---

## 1. API 계약

> `ASSIGNMENT.md` §5의 경로와 응답 예시 형태를 그대로 지킨다.

### 1.1 공통

- **I-01 기본 주소** — `http://localhost:{BACKEND_PORT}/api` (기본 `8080`). `GET /health`만 `/api` 밖에 있다.
- **I-02 형식** — JSON 본문이 있는 요청과 응답은 `Content-Type: application/json`을 사용한다.
- **I-03 요청자** — `x-user-id` 헤더 또는 `x-user-id` 쿠키. 규칙은 `backend/spec.md` B-01.
- **I-04 시각** — UTC ISO-8601, `Z` 접미사, **초 단위** (소수점 이하 없음). 예: `2026-08-30T13:55:54Z`
  - 새로 작성된 데이터도 같은 형식이다. 응답에 보이는 시각과 저장된 시각의 정밀도가 같다.
  - 같은 시각의 항목 순서는 `backend/spec.md` B-23을 따른다.

### 1.2 객체

**Post**

```json
{
  "id": 1,
  "title": "부스터 프로 써보신 분?",
  "content": "…",
  "likeCount": 1796,
  "commentCount": 2,
  "isLiked": false,
  "author": "apr_tester",
  "createdAt": "2026-08-30T13:55:54Z"
}
```

**Comment**

```json
{ "id": 2, "postId": 1, "content": "…", "author": "cos_holic", "createdAt": "2026-08-30T14:02:33Z" }
```

**목록 응답 (Page)**

```json
{
  "items": [ … ],
  "total": 39,
  "nextCursor": { "createdAt": "2026-08-29T10:12:03Z", "id": 21 }
}
```

- `items`와 `total`은 필수다. `total`은 조건에 맞는 전체 개수(삭제 제외)다.
- `nextCursor`는 다음 묶음의 시작 기준이다. 이번 응답의 마지막 항목의 `createdAt` · `id`와 같다.
  다음 요청에 `cursorCreatedAt = nextCursor.createdAt`, `cursorId = nextCursor.id`로 보낸다.
- 더 가져올 항목이 없으면 `"nextCursor": null`이다.

### 1.3 목록 쿼리 파라미터 (`GET /posts`, `GET /posts/{postId}/comments`)

| 이름 | 필수 | 규칙 |
| --- | --- | --- |
| `size` | 아니오 | 정수 1~100, 생략 시 20. 범위 밖 · 숫자 아님 → 오류 |
| `cursorCreatedAt` | 아니오 | 직전 응답의 마지막 항목 `createdAt` (I-04 형식) |
| `cursorId` | 아니오 | 직전 응답의 마지막 항목 `id` (양의 정수) |

- 두 커서 파라미터는 **둘 다 있거나 둘 다 없어야** 한다. 하나만 있거나 형식이 잘못되면 400 `INVALID_QUERY`.
- 둘 다 없으면 처음(가장 최신)부터, 있으면 `(createdAt, id)`가 커서보다 **뒤**(더 오래된)인 항목부터 준다.

### 1.4 엔드포인트

| 메서드 · 경로 | 요청 본문 | 성공 응답 본문 | 성공 상태 |
| --- | --- | --- | --- |
| `GET /posts` | — | Page\<Post\> | 200 |
| `POST /posts` | `{ "title", "content" }` | 생성된 Post | 201 |
| `GET /posts/{postId}` | — | Post | 200 |
| `PATCH /posts/{postId}` | `{ "title", "content" }` | 수정된 Post | 200 |
| `DELETE /posts/{postId}` | — | 없음 | 204 |
| `POST /posts/{postId}/likes/toggle` | — | `{ "isLiked": true, "likeCount": 1797 }` | 200 |
| `GET /posts/{postId}/comments` | — | Page\<Comment\> | 200 |
| `POST /posts/{postId}/comments` | `{ "content" }` | 생성된 Comment | 201 |
| `DELETE /comments/{commentId}` | — | 없음 | 204 |

### 1.5 오류

**오류 응답 본문**

```json
{ "code": "POST_DELETED", "message": "삭제된 게시글입니다." }
```

- `code`: 원인을 나타내는 고정 문자열. 클라이언트는 `code`로 분기한다.
- `message`: 사람이 읽는 설명. 문구는 바뀔 수 있으며 분기에 쓰지 않는다.
- 입력 검증 오류(`INVALID_INPUT`)는 잘못된 필드를 모두 담은 `errors` 목록을 함께 준다. 다른 오류에는 `errors`가 없다.

```json
{
  "code": "INVALID_INPUT",
  "message": "입력값이 올바르지 않습니다.",
  "errors": [
    { "field": "title", "message": "제목을 입력해 주세요." },
    { "field": "content", "message": "내용을 입력해 주세요." }
  ]
}
```

**오류 목록**

| 경우 | 상태 | `code` |
| --- | --- | --- |
| 본문 검증 실패 (필수 누락 · 공백만 · 제목 20자 초과 · JSON 형식 오류) · 경로 변수 형식 오류(`/posts/abc`) · 디코딩할 수 없는 `x-user-id` 쿠키 | 400 | `INVALID_INPUT` |
| `size` · `cursorCreatedAt` · `cursorId` 값이 잘못됨 | 400 | `INVALID_QUERY` |
| 요청자 헤더 · 쿠키 불일치 | 400 | `USER_ID_MISMATCH` |
| 남의 게시글 · 댓글 수정 · 삭제 | 403 | `NOT_OWNER` |
| 존재하지 않는 게시글 | 404 | `POST_NOT_FOUND` |
| 삭제된 게시글 | 404 | `POST_DELETED` |
| 존재하지 않는 댓글 | 404 | `COMMENT_NOT_FOUND` |
| 삭제된 댓글 | 404 | `COMMENT_DELETED` |
| 동시 요청 충돌로 처리 실패 (좋아요 토글 재시도 소진) | 409 | `CONCURRENT_UPDATE` |
| 예상하지 못한 서버 오류 | 500 | `INTERNAL_ERROR` |

- 삭제된 게시글과 존재하지 않는 게시글은 상태 코드가 같고 `code`로 구분된다. (P-53) 댓글도 같은 규칙이다. (C5)
- 댓글이 속한 게시글이 삭제된 경우는 `POST_DELETED`로 응답한다. (B-33)
- **검사 순서**: 대상의 존재 · 삭제 여부를 먼저 확인하고, 그다음 권한을 확인한다.
  삭제된 남의 게시글을 수정 · 삭제하려 하면 권한 오류(403)가 아니라 삭제된 게시글 오류로 응답한다.

### 1.6 결정 기록

| # | 항목 | 결정 |
| --- | --- | --- |
| C1 | 페이지네이션 계약 | 커서 방식 (Backend Plan B9). 요청 `cursorCreatedAt` + `cursorId` (쌍 필수), 응답 `nextCursor: { createdAt, id } \| null`. 응답 객체로 묶어 끝을 `null` 하나로 표현하고 두 값이 짝임을 구조로 드러냄. `items` · `total`은 예시 그대로 유지하고 필드만 추가 (`ASSIGNMENT.md` §5 허용) |
| C2-a | 남의 게시글 · 댓글 수정 · 삭제 | 403 |
| C2-b | 삭제된 게시글 | 404, 구분은 오류 본문으로 (410 미사용) |
| C2-c | 검사 순서 | 존재 · 삭제 여부 먼저, 권한은 그다음 |
| C2-d | 생성 성공 | 201 + 생성된 객체 |
| C2-e | 삭제 성공 | 204, 본문 없음 |
| C3 | 오류 응답 본문 | `{ code, message }` 커스텀 코드 방식. 입력 검증 오류는 필드별 `errors` 목록 포함 |
| C4 | 시각 정밀도 | 초 단위로 통일, 저장 값과 응답 값 정밀도 동일. 근거: 시드 · 과제 응답 예시 · 화면(상대 시간) 모두 초 단위라 밀리초는 요구에서 나온 정보가 아님. 기존 데이터 형식에 새 데이터를 맞춤 |
| C5 | 삭제된 댓글 | 404 `COMMENT_DELETED`, 존재하지 않는 댓글(`COMMENT_NOT_FOUND`)과 구분. 게시글(P-53)과 같은 규칙 |

## 2. Docker

- **I-10** `docker compose up` 한 번으로 Frontend · Backend · DB가 함께 기동되고, Backend healthcheck(`GET /health` 200)를 통과한다.

## 3. 환경 변수 · 연결

- **I-20** `BACKEND_PORT` · `FRONTEND_PORT` · `DB_PORT`를 바꿔 실행해도 동작한다. (기존 B-72에 `FRONTEND_PORT` 추가)
- **I-21** 브라우저의 Frontend 요청에서 `x-user-id` 쿠키가 Backend까지 전달되어 요청자 식별에 사용될 수 있어야 한다. 기존 CORS 설정은 이를 지원해야 한다.

---

## 미결

없음. (결정 내용은 §1.6)
