# 메디큐브톡 커뮤니티

메디큐브톡 커뮤니티 화면(목록, 상세, 작성, 수정)과 API를 구현한 과제입니다.
과제 내용은 [`ASSIGNMENT.md`](./ASSIGNMENT.md)에 있습니다.

---

## 실행

```bash
docker compose up
```

| | 주소 |
| --- | --- |
| 프론트엔드 | http://localhost:3000 |
| 백엔드 | http://localhost:8080 |
| 기동 확인 | http://localhost:8080/health · `GET /health` 는 유지해주세요 |

포트가 이미 사용 중이면 환경 변수로 바꿀 수 있습니다.

```bash
FRONTEND_PORT=13000 BACKEND_PORT=18080 DB_PORT=25432 docker compose up --build
```

`BACKEND_PORT`를 바꾸면 프론트엔드를 다시 빌드해야 해서 `--build`가 필요합니다.
`NEXT_PUBLIC_API_BASE_URL`이 빌드 시점에 클라이언트 번들로 인라인되기 때문입니다.

<details>
<summary>참고 — 제공된 스타터의 구성</summary>

```
seed/seed.json        초기 데이터
docker-compose.yml    DB(PostgreSQL 16) + 백엔드 + 프론트엔드
backend/              Spring Boot 3.5 / Java 17. DB 연결 · GET /health · CORS 설정만 있습니다.
frontend/             Next.js 15 App Router. 기본 설정과 app/dev-user/ 만 있습니다.
```

- **DB 호스트 포트 기본값은 `15432`** 입니다. `5432`는 이미 쓰고 있는 경우가 많아 피했습니다.
- `seed/`는 백엔드 컨테이너의 `/seed`에 읽기 전용으로 마운트됩니다. (`APP_SEED_PATH`)
- DB 데이터는 named volume(`dbdata`)에 남습니다. `docker compose down`을 해도 유지되고,
  **초기화하려면 `docker compose down -v`** 를 쓰세요.
- 브라우저에서 호출할 때와 컨테이너 안에서 호출할 때 주소가 달라, API 주소를 두 개 줍니다.
  - `NEXT_PUBLIC_API_BASE_URL` — 브라우저에서 (클라이언트 컴포넌트)
  - `API_BASE_URL` — 컨테이너 안에서 (서버 컴포넌트 · 라우트 핸들러)
- **`/dev-user`** 에서 요청자를 바꿀 수 있습니다. 고르면 `x-user-id` 쿠키가 설정됩니다.
  쿠키가 없으면 기본 요청자는 `apr_tester`입니다.
- 백엔드 의존성에는 `spring-boot-starter-jdbc`만 들어 있습니다. ORM은 직접 고르세요.
- 테스트 환경(`spring-boot-starter-test` · JUnit · Mockito)은 준비되어 있습니다.
  테스트를 작성하면 그대로 동작합니다.
- 폰트는 `frontend/public/fonts/`의 **Pretendard**를 쓰면 됩니다. `globals.css`에 적용되어 있습니다.
- **CORS는 설정되어 있습니다** (`CorsConfig.java`). `http://localhost:*`(및 `127.0.0.1:*`) 오리진을
  자격 증명과 함께 허용하므로 포트를 바꿔도 동작합니다. 브라우저에서 직접 호출할 때는 요청에
  `credentials: "include"`를 붙여야 `x-user-id` 쿠키가 전송됩니다.

로컬 개발은 compose 없이 하셔도 됩니다. DB만 띄우고 각각 실행하는 쪽이 빠릅니다.

```bash
docker compose up db
cd backend  && ./gradlew bootRun
cd frontend && npm install && npm run dev
```

</details>

### 함께 보면 좋은 것

| | 위치 |
| --- | --- |
| API 문서 (Swagger UI) | http://localhost:8080/swagger-ui.html — 각 API를 바로 호출해 볼 수 있습니다. `x-user-id` 헤더 칸에 요청자를 넣습니다 |
| 요구사항, 설계 문서 | [`specs/`](./specs) — 아래 "진행 방식" 참고 |
| 백엔드 테스트 | `cd backend && ./gradlew test` (Testcontainers로 PostgreSQL을 직접 띄우므로 Docker가 필요합니다) |

---

## 진행 방식

구현 전에 **무엇을 만들지(WHAT)와 어떻게 만들지(HOW)를 문서로 분리**하고, 과제에 없거나 모호한 부분은 선택지를 비교해 결정한 뒤 이유와 포기한 것을 함께 남겼습니다.

```text
docs/harness/HARNESS.md     작업 절차 — Inspect → Spec → Plan → Tasks → Implementation → Verification → Review
specs/product-spec.md       구현과 무관한 제품 요구사항 (P-xx), 결정 기록 D1~D11
specs/backend/              spec (B-xx), plan (기술 결정 B1~B17), tasks (T-00~T-80, 검증 근거 포함)
specs/frontend/             spec (F-xx, 피그마 수치 반영), plan (기술 결정 F1~F11), tasks
specs/integration/spec.md   API 계약 (C1~C5), Docker, 환경 변수
```

- 아래 README의 각 결정 옆 기호(`B8`, `F2`, `D2` 등)는 위 문서의 결정 번호입니다. 자세한 비교와 근거는 해당 문서에 있습니다.
- 각 task는 "검증 근거"가 채워져야 완료로 바꿨습니다. 테스트 결과뿐 아니라 실제 `docker compose` 기동, curl, psql 확인 결과를 근거로 적었습니다.

### AI 활용 — 하네스 엔지니어링

AI 코딩 에이전트(Claude Code)와 함께 작업했습니다. AI가 빠르게 만들되 **판단은 사람이 하고, 결과는 실행으로 검증**하도록 작업 환경(하네스)을 먼저 설계했습니다.

| 장치 | 역할 |
| --- | --- |
| [`CLAUDE.md`](./CLAUDE.md) | 매 작업마다 읽는 규칙. 과제 문서가 최우선, 중요한 기술 선택은 임의로 정하지 않기, 보존 대상(`/dev-user`, `/health`, CORS, 시드 id, 포트) 목록, `main` 직접 커밋 금지 |
| [`HARNESS.md`](./docs/harness/HARNESS.md) | 작업 절차와 단계별 완료 조건. "검증 근거가 비어 있으면 완료로 바꾸지 않는다" |
| spec, plan, tasks 문서 | AI가 맥락을 잃지 않도록 결정을 문서로 남기고, 다음 작업은 문서를 읽고 시작 |

- **결정은 사람이:** AI는 선택지와 장점, 포기하는 것을 비교해 제시하고, 고르는 것은 직접 했습니다. 이 README의 결정 번호(B, F, D, FD)가 그 기록입니다.
- **검증은 실행으로:** "테스트 통과"에서 멈추지 않고 브랜치마다 그 커밋만 따로 꺼내 빌드, 테스트하고, 빈 DB로 실제 기동해 curl로 API를 확인했습니다.
- **AI 결과도 다시 검토:** 다른 AI 리뷰를 받아 문서와 대조했고, 리뷰가 예전 결정을 기준으로 한 경우는 걸러냈습니다. 요청하지 않은 코드, 테스트가 생기면 되돌렸습니다.
- **반복 작업을 AI에게:** 피그마 파일(`.fig`)을 디코딩해 화면별 색, 글꼴, 간격, 문구와 아이콘 벡터를 뽑아 spec에 옮기는 일처럼, 사람이 손으로 하면 오래 걸리는 작업을 맡겼습니다.

---

## 기술 선택

과제가 정한 스택(Spring Boot, Next.js, PostgreSQL) 밖의 선택은 후보를 비교해 정했습니다. 항목 옆 번호는 `specs/` 문서의 결정 번호이고, 더 자세한 비교는 그 문서에 있습니다.

### 백엔드

**DB 접근: Spring Data JPA** (B1)
- 후보: Spring Data JPA, JdbcTemplate(스타터에 이미 있는 것), MyBatis
- 고른 이유: 생성, 수정, 단건 조회처럼 단순한 작업을 적은 코드로 처리하고, 엔티티 값을 바꾸면 자동으로 UPDATE되는 변경 감지를 쓸 수 있습니다. JdbcTemplate는 모든 쿼리와 결과 매핑을 손으로 써야 하고, MyBatis는 SQL을 XML이나 애너테이션으로 따로 관리해야 해서 이 규모에서는 손이 더 많이 갑니다. 목록과 집계처럼 복잡한 조회는 JPA 안에서 JPQL(`@Query`)로 직접 써서 보완했습니다.
- 포기한 것: N+1 문제처럼 JPA 특유의 동작을 늘 신경 써야 합니다.

**스키마 관리: `schema.sql` + `ddl-auto: validate`** (B2)
- 후보: `schema.sql` 파일, Flyway 같은 마이그레이션 도구, JPA가 테이블을 자동 생성(`ddl-auto: create`/`update`)
- 고른 이유: 필요한 건 제출 시점의 스키마 하나라 변경 이력을 관리하는 Flyway는 과합니다. 자동 생성은 `create`면 재기동할 때마다 데이터가 지워지고, `update`는 제약이나 인덱스를 원하는 대로 만들지 못합니다. `schema.sql`은 테이블과 제약이 파일에 그대로 보이고, `validate`가 엔티티와 테이블이 어긋나면 서버가 뜰 때 바로 실패시켜 줍니다.
- 포기한 것: 스키마 변경 이력이 남지 않습니다.

**목록 나눠 받기: 커서 페이지네이션** (B9, C1)
- 후보: 페이지 번호 방식(`page`, `size`), 커서 방식
- 고른 이유: 페이지 번호 방식은 목록을 보는 사이 새 글이 생기면 항목이 한 칸씩 밀려, 무한 스크롤에서 같은 글이 또 나오거나 빠집니다. 커서 방식은 "마지막으로 본 글의 `(createdAt, id)` 다음부터" 가져오므로 그런 일이 없습니다. 시드에 같은 초에 쓴 댓글이 있어서 `id`까지 넣어 순서를 확정했습니다. 커서 값은 인코딩한 문자열 대신 `cursorCreatedAt`, `cursorId` 두 파라미터로 그대로 보내, 주소만 봐도 어디서부터 가져오는지 보이게 했습니다.
- 포기한 것: 특정 페이지로 바로 건너뛸 수 없습니다. 두 커서 값이 항상 짝으로 와야 해서 그 검증이 필요합니다.

**좋아요 동시성: 낙관적 락 + 재시도** (B8)
- 후보: 비관적 락(`SELECT ... FOR UPDATE`), 낙관적 락(`@Version`) + 재시도, DB에서 바로 더하기(`like_count = like_count + 1`), Redis
- 고른 이유: 좋아요는 읽기가 대부분이고, 같은 글에 여러 사람이 정확히 동시에 누르는 일은 드뭅니다. 비관적 락은 충돌이 없는 대부분의 요청에도 매번 잠금을 걸고, Redis는 서버를 하나 더 운영해야 해서 지금 규모에는 과합니다. 그래서 평소에는 잠금 없이 처리하고 드물게 충돌할 때만 다시 시도하는 낙관적 락을 골랐습니다. 좋아요 수는 게시글과 분리한 `post_like_counts` 테이블에 둬서, 글 수정과 좋아요가 서로를 막지 않습니다.
- 포기한 것: 같은 글에 요청이 몰리면 3번 재시도 안에 처리 못 한 요청은 409를 받습니다. 사용자가 늘어 충돌이 잦아지면 DB에서 바로 더하는 방식, 그다음 Redis로 옮기는 게 맞다고 봅니다. (아래 "남은 과제")

**삭제: `deleted_at` 소프트 삭제, 쿼리마다 조건 직접 작성** (B6)
- 후보: 삭제 표시는 `is_deleted` 같은 참/거짓 컬럼, `deleted_at` 시각 컬럼. 삭제된 행을 거르는 방법은 `@SQLRestriction`으로 자동 숨김, 쿼리마다 직접 작성
- 고른 이유: `deleted_at`은 삭제 여부와 삭제 시각이 함께 남습니다. `@SQLRestriction`으로 자동으로 숨기면 삭제된 글을 아예 조회할 수 없어서, "삭제된 글(`POST_DELETED`)"과 "원래 없는 글(`POST_NOT_FOUND`)"을 구분할 수 없습니다. 그래서 쿼리마다 `deleted_at IS NULL`을 직접 적었습니다.
- 포기한 것: 조건을 한 곳이라도 빠뜨리면 삭제된 글이 보입니다. 모든 조회 경로를 검사하는 테스트(`SoftDeleteExposureTest`)로 막았습니다.

**요청자 식별: `@CurrentUser` + ArgumentResolver** (B11)
- 후보: 컨트롤러마다 `@RequestHeader`, `@CookieValue`로 직접 읽기, 필터나 인터셉터에서 읽어 ThreadLocal에 저장, ArgumentResolver로 파라미터에 넣기
- 고른 이유: 컨트롤러마다 직접 읽으면 "헤더와 쿠키 중 무엇을 쓰고, 공백이면 어떻게 하고, 둘이 다르면 400" 규칙이 API마다 반복됩니다. 필터와 ThreadLocal은 어느 API가 요청자를 쓰는지 코드에 드러나지 않습니다. ArgumentResolver는 요청자가 필요한 API의 파라미터에 `@CurrentUser`가 붙어 바로 보이고, 규칙은 `RequesterIdResolver` 한 곳에 모입니다.
- 포기한 것: 애너테이션, Resolver, 등록 설정 코드가 늘어납니다.

**제목 20자 세는 방법: 유니코드 코드포인트** (D2, B14)
- 후보: Java 기본 방식(UTF-16, `@Size`), 유니코드 코드포인트, 눈에 보이는 글자 단위
- 고른 이유: Java 기본 방식으로 세면 시드 6번 제목("탈모에 좋은 제품 공유해주세요 🙏🏻")이 21자가 되어, 원래 있던 글이 수정할 때 거절됩니다. 눈에 보이는 글자 단위는 PostgreSQL `VARCHAR(20)`과 세는 방식이 달라 DB 제약과 어긋납니다. 코드포인트는 시드 제목을 통과시키고 DB와도 같은 방식이라, 직접 만든 `@CodePointLength`로 검사합니다.
- 포기한 것: 피부색 이모지처럼 눈에는 한 글자여도 2자 이상으로 셉니다.

**테스트 DB: Testcontainers** (B16-a)
- 후보: H2 같은 메모리 DB, 로컬에 띄운 PostgreSQL, Testcontainers
- 고른 이유: 시드 개수, 새 글 id, 중복 적재 방지, 동시 요청은 실제 PostgreSQL에서 돌려야 의미가 있습니다. H2는 시퀀스와 락 동작이 달라서, 실제로는 깨지는데 테스트는 통과하는 일이 생길 수 있습니다. 로컬 DB는 미리 띄워 둬야 하고 개발 데이터와 섞입니다. Testcontainers는 `./gradlew test` 한 줄로 테스트용 PostgreSQL을 새로 띄웁니다.
- 포기한 것: 첫 실행이 느리고 Docker가 있어야 합니다.

**Lombok** (B17-a)
- 후보: Lombok 사용, 사용하지 않고 직접 작성
- 고른 이유: getter와 생성자 같은 반복 코드를 줄입니다. 대신 엔티티에는 `@Getter`와 protected 기본 생성자만 쓰고 `@Setter`, `@Data`는 쓰지 않아, 값은 `update(...)`, `delete(...)` 같은 메서드로만 바뀌게 했습니다. 요청과 응답 객체는 Java `record`로 썼습니다.
- 포기한 것: 생성되는 코드가 소스에 보이지 않습니다.

**API 문서: Swagger (springdoc-openapi), 문서 인터페이스 분리**
- 후보: README에 표로 정리, Swagger 애너테이션을 컨트롤러에 직접 작성, 문서용 인터페이스를 따로 두고 컨트롤러가 구현
- 고른 이유: Swagger는 문서를 보면서 바로 호출해 볼 수 있습니다. 애너테이션을 컨트롤러에 직접 쓰면 컨트롤러가 문서 코드로 길어지므로, 문서 내용은 기능별 `PostApiDocs`, `CommentApiDocs`, `LikeApiDocs`에 모으고 컨트롤러는 `implements`만 합니다. 오류 응답 예시는 `ErrorCode`에서 자동으로 만들어 실제 응답과 어긋나지 않게 했습니다.
- 포기한 것: 의존성이 하나 늘고, 컨트롤러와 문서 인터페이스의 메서드 모양을 맞춰야 합니다.

### 프론트엔드

**데이터 호출 위치: 브라우저에서 백엔드 직접 호출** (F1)
- 후보: 브라우저에서 직접 호출, Next 서버를 거쳐 호출(Route Handler), 첫 화면만 서버에서 그리고 이후는 브라우저에서 호출
- 고른 이유: 스타터의 CORS 설정, 브라우저용 API 주소, compose 빌드 설정이 모두 직접 호출을 전제로 준비돼 있습니다. Next 서버를 거치면 API마다 중계 코드와 쿠키 전달 처리가 생기고, 첫 화면만 서버에서 그리면 데이터 경로가 둘이 되고 서버와 브라우저의 시각 차이로 "3분 전" 같은 상대 시간이 어긋날 수 있습니다. 무한 스크롤, 좋아요, 댓글이 전부 화면에서 하는 동작이라 브라우저 하나로 두는 게 단순합니다.
- 포기한 것: 첫 화면이 잠깐 비었다가 채워집니다. 백엔드 포트를 바꾸면 프론트를 다시 빌드해야 합니다.

**서버 데이터 관리: TanStack Query** (F2)
- 후보: TanStack Query, SWR, RTK Query, 라이브러리 없이 직접 구현
- 고른 이유: 무한 스크롤(`useInfiniteQuery` + `nextCursor`), 같은 요청 중복 방지, 변경 후 다른 화면 갱신, 캐시 직접 수정, 뒤로 가기 때 캐시 재사용이 요구사항과 그대로 맞습니다. SWR은 가볍지만 변경 후 여러 화면을 갱신하는 도구가 적고, RTK Query는 이 규모에 비해 설정이 무겁습니다. 직접 구현하면 위 기능을 전부 손으로 만들어야 해서 버그가 생길 곳이 늘어납니다.
- 포기한 것: 쿼리 키와 무효화 개념을 익혀야 합니다. 좋아요 연타 처리는 라이브러리가 해 주지 않아 직접 만들었습니다.

**스타일: Tailwind CSS v4** (F3)
- 후보: CSS Modules, Tailwind CSS, styled-components 같은 CSS-in-JS
- 고른 이유: 피그마의 색과 글꼴 크기를 `@theme` 토큰으로 한 번 등록해 씁니다. 버튼 활성/비활성, 하트, 20자 도달처럼 상태가 많은 화면을 짧게 쓸 수 있습니다. CSS Modules는 상태별 클래스 조합을 직접 처리해야 하고, CSS-in-JS는 App Router에서 별도 설정과 실행 비용이 듭니다.
- 포기한 것: `className`이 길어집니다. 운영체제별 모듈을 써서 Docker 빌드가 되는지 따로 확인해야 했습니다.

**화면 공통 상태: React Context (토스트만 전역)** (F4)
- 후보: React Context, Zustand, 전역 없이 화면을 옮길 때 주소(`?toast=`)로 전달
- 고른 이유: 화면을 옮긴 뒤에도 보여야 하는 건 토스트 하나뿐이라, 이것 하나를 위해 Zustand를 들일 필요가 없습니다. 주소로 넘기면 URL이 지저분해지고 처리가 화면마다 흩어집니다. 다이얼로그와 바텀시트는 각 화면 안의 상태로 충분합니다.
- 포기한 것: 전역 상태가 늘어나면 Context를 나눠야 합니다.

**요청자 전달: 화면은 쿠키로** (F5)
- 후보: 쿠키만 보내기, 쿠키와 같은 값을 헤더로도 보내기, 서버에 "내가 누구인지" API 추가
- 고른 이유: 백엔드는 과제 요구대로 `x-user-id` 헤더와 쿠키를 모두 받습니다. 헤더는 Swagger나 curl처럼 화면을 거치지 않는 호출에서 쓰고, 화면은 `/dev-user`가 바꾸는 값이 쿠키라서 쿠키를 보냅니다. 헤더로도 보내면 둘이 어긋날 때 400이 나는 경우만 늘고, 새 API를 만들면 화면마다 요청이 하나 더 생깁니다. 필요하면 `apiFetch`에서 헤더를 붙일 수 있게 열어 두었습니다.
- 포기한 것: "본인 글인지" 판단 규칙(쿠키 값 앞뒤 공백 제거, 없으면 `apr_tester`)을 프론트에도 한 번 더 구현합니다.

**아이콘: 피그마에서 직접 뽑은 SVG** (F8)
- 후보: 아이콘 라이브러리(lucide 등), 피그마에서 손으로 하나씩 내보내기, 피그마 파일에서 벡터 경로 추출
- 고른 이유: 라이브러리 아이콘은 모양과 굵기가 피그마와 다릅니다. 피그마 파일을 풀어 아이콘 경로 데이터를 그대로 옮기면 디자인과 정확히 같고, 손으로 내보내는 수작업도 필요 없습니다.
- 포기한 것: 추출하는 수고가 듭니다.

**코드 검사와 테스트: ESLint + Vitest(순수 로직만)** (F9, F10)
- 후보: lint는 ESLint, Biome, 타입 검사(`tsc`)만. 테스트는 없음, 순수 로직 단위 테스트, 컴포넌트 테스트, 브라우저 E2E(Playwright)
- 고른 이유: React hooks 실수는 Next 공식 규칙이 있는 ESLint가 가장 잘 잡습니다. 테스트는 경계값이 많아 눈으로 확인하기 어려운 규칙(상대 시간, 글자 수 자르기, 좋아요 연타 처리)을 단위 테스트로 고정했습니다. 컴포넌트 테스트와 E2E는 설정과 실행 비용에 비해 이 규모에서 얻는 게 적다고 봤습니다.
- 포기한 것: 화면 흐름은 자동 테스트가 없고 실제 브라우저로 확인합니다.

---

## 상태 설계

### 프론트엔드 — 서버에서 받은 데이터를 어디에 두었나

| 상태 | 두는 곳 | 이유 |
| --- | --- | --- |
| 서버 데이터 (목록, 상세, 댓글) | TanStack Query 캐시. 쿼리 키 `['posts','list']`, `['posts','detail', id]`, `['comments', postId]` | 화면 간 공유, 무한 스크롤 페이지 누적, 변경 후 무효화를 한 곳에서 처리 |
| 좋아요 진행 상태 | 게시글별 `desired`(마지막으로 누른 상태), `confirmed`(서버가 확인한 상태), `inFlight` (`useLikeToggle`) | 누르는 즉시 화면을 바꾸면서, API가 "설정"이 아니라 "토글"이라 연타 시 요청을 하나씩 보내 마지막 상태로 수렴시켜야 한다 |
| 폼 입력, 다이얼로그, 바텀시트 | 각 화면의 로컬 상태 | 그 화면에서만 쓰고 이동하면 사라져야 한다 |
| 토스트 | `ToastProvider` Context | 작성 → 목록, 삭제 → 목록처럼 화면 이동 뒤에도 보여야 하는 유일한 상태 |
| 요청자 | 저장하지 않음 — 필요할 때 쿠키에서 읽음 | 화면에서 요청자를 바꾸는 방법은 `/dev-user`뿐이고, `/dev-user`는 쿠키를 바꾼 뒤 페이지를 새로 불러오므로 따로 저장해 둘 필요가 없다 |

**변경 후 캐시를 맞추는 방식: 즉시 반영 → 무효화**
화면에 없는 쿼리는 무효화해도 다시 그려질 때 캐시의 옛 값을 먼저 보여준다. 그래서 변경 결과를 캐시에 먼저 반영해 삭제한 글, 옛 제목, 옛 숫자가 잠깐 보이는 일을 막고, 무효화로 서버와 최종 정합을 맞춘다.

| 동작 | 캐시 처리 |
| --- | --- |
| 게시글 작성 | 응답 게시글을 목록 첫 페이지 맨 앞에 넣고 무효화 (잠깐 21개가 되어도 커서가 "마지막 글 기준"이라 빠지거나 겹치는 글이 없다 — 20개로 맞추려고 끝을 자르면 그 글이 재조회 전까지 사라진다) |
| 게시글 수정, 삭제 | 상세 캐시 교체, 제거, 목록 캐시의 해당 항목 교체, 제거 후 무효화 |
| 좋아요 | 상세, 목록 캐시의 해당 항목을 `confirmed` 값으로 갱신 |
| 댓글 작성, 삭제 | 상세, 목록의 `commentCount` ±1, 댓글 캐시에서 삭제 댓글 제거 후 무효화 |

### 백엔드 — 데이터를 어떻게 저장했나

| 데이터 | 저장 방식 | 이유 |
| --- | --- | --- |
| 삭제 | 행을 지우지 않고 `deleted_at`만 기록 | 과제의 삭제 정책. 삭제 여부와 시각이 함께 남는다 |
| 삭제된 글의 댓글 | 댓글 행은 그대로 두고, 부모 글 상태로 숨김 (B6-c) | 시드가 이미 이 모델이다 — 삭제된 글 9, 30, 31의 댓글 69개가 `deletedCommentIds`에 없다 |
| 댓글 수 (`commentCount`) | 저장하지 않고 조회할 때 계산 (B7) | 원본(댓글)에서 직접 세므로 어긋날 수 없다. 목록 20개의 댓글 수를 `GROUP BY` 한 번으로 센다 |
| 좋아요 수 | 별도 카운터 테이블 `post_like_counts` (B8-a) | 시드는 좋아요 **기록 없이** 숫자만 준다(1번 글 1796). 그래서 기록 개수로 셀 수 없고, 시드 숫자에서 시작하는 카운터를 둔다. 이후 토글은 기록과 카운터를 한 트랜잭션에서 함께 바꾼다 |

### 좋아요 동시성 — 프론트와 백엔드에서 두 번 막는다

좋아요 API는 "좋아요로 설정"이 아니라 **토글**이라, 같은 요청이 겹치면 순서에 따라 결과가 뒤집힌다. 그래서 두 계층에서 각자 다른 문제를 막았다.

| 계층 | 막는 상황 | 방법 |
| --- | --- | --- |
| **프론트** (`useLikeToggle`) | 한 화면에서 하트를 연타할 때 요청이 여러 개 동시에 나가 응답 순서가 엇갈리는 것 | 누르는 즉시 화면만 바꾸고(`desired`), 서버 요청은 **게시글당 한 번에 하나만** 보낸다. 응답(`confirmed`)이 오면 마지막으로 누른 상태와 비교해 다르면 한 번 더 토글한다. 3번 연타해도 요청은 2번 이하로 나가고, 최종 상태는 마지막으로 누른 상태다. 실패하면 서버 값으로 되돌린다 |
| **백엔드** (`LikeFacade`, `LikeService`) | 프론트가 막을 수 없는 경우 — 다른 탭, 다른 기기의 같은 사용자, 서로 다른 사용자의 동시 요청, 화면을 거치지 않은 API 직접 호출 | `post_likes(post_id, user_id)` 유니크 제약으로 중복 좋아요를 DB에서 막고, 카운터는 `@Version` 낙관적 락으로 충돌을 잡아 트랜잭션 밖에서 최대 3회 재시도한다. `like_count >= 0` CHECK 제약이 마지막 방어선이다 |

- **역할이 다르다.** 프론트는 사용자 경험(즉시 반응)과 불필요한 요청을 줄이는 쪽이고, **정합성은 백엔드가 보장**한다. 클라이언트를 믿지 않아도 숫자와 기록이 항상 일치해야 하기 때문이다.
- **확인한 결과:** 같은 사용자가 동시에 2번씩 20회 눌러도 매번 "좋아요 → 취소"로 원래 상태에 돌아왔고, 서로 다른 사용자 6명이 동시에 눌렀을 때 숫자는 정확히 성공한 수만큼만 늘었다. 어느 경우에도 좋아요 수 = 기록 수였다.

---

## 구조

### 백엔드 — 기능별 패키지 (B10)

```text
com.apr.community
├── CommunityApplication, CorsConfig, HealthController   스타터 그대로
├── common/
│   ├── error/        ErrorCode, ApiException, GlobalExceptionHandler, ErrorResponse — 오류 응답 { code, message, errors? }
│   ├── user/         @CurrentUser, CurrentUserArgumentResolver, RequesterIdResolver — 요청자 식별
│   ├── paging/       CursorPageRequest, PageResponse, Cursor — 커서 파라미터 검증, 목록 응답
│   ├── validation/   @CodePointLength, CodePointLengthValidator
│   ├── config/       ClockConfig, WebConfig, OpenApiConfig
│   └── docs/         ApiErrorCodes, SchemaDoc (Swagger 문서용)
├── post/             Post, PostLikeCount (엔티티), PostRepository, 프로젝션(PostCommentCount, PostLikeCountView), PostService, PostController, PostApiDocs, dto/
├── comment/          Comment, CommentRepository, CommentService, CommentController, CommentApiDocs, dto/
├── like/             PostLike, PostLikeRepository, LikeService, LikeFacade, LikeController, LikeApiDocs, dto/
└── seed/             SeedLoader, SeedData
```

- **나눈 기준:** spec, API, 테이블이 모두 게시글, 댓글, 좋아요로 나뉘어 있어 같은 경계로 나눴다. 기능 하나를 고칠 때 한 폴더만 본다. 각 기능 안은 Controller → Service → Repository 계층만 두고 추가 추상화는 하지 않았다.
- **기능 간 의존은 한 방향** (B10-b): 댓글, 좋아요 → 게시글. "삭제됐거나 없는 글" 규칙은 `PostService.getActivePost` 한 곳에만 있고 댓글(`CommentService`), 좋아요(`LikeService`)가 재사용한다.
- **예외: 화면 응답용 집계** (B10-c): 게시글 응답의 `commentCount`, `likeCount`, `isLiked`는 `PostRepository`의 JPQL이 다른 기능의 테이블을 직접 읽어 묶음 단위로 센다(`countActiveComments`, `findLikeCounts`, `findLikedPostIds`). 서비스 간 역방향 의존 없이 N+1을 피하기 위해서다.
- **좋아요의 트랜잭션 분리:** 재시도 루프(`LikeFacade.toggle`)와 트랜잭션 메서드(`LikeService.toggle`)를 **별도 빈**으로 나눴다. 같은 클래스 안에서 호출하면 Spring 프록시를 거치지 않아 재시도마다 새 트랜잭션이 열리지 않기 때문이다.
- **오류 응답이 한 곳에 모인다:** 서비스는 `ApiException(ErrorCode)`만 던지고 `GlobalExceptionHandler`가 상태 코드, 본문을 만든다. 클라이언트는 `code`로 분기한다(삭제된 글 `POST_DELETED`와 없는 글 `POST_NOT_FOUND`는 둘 다 404지만 코드로 구분, D5).

### 프론트엔드 — 라우트는 얇게, 기능별 폴더 (F11)

```text
app/
  layout.tsx, globals.css, dev-user/   스타터 그대로
  (community)/                           주소에 나타나지 않는 Route Group
    layout.tsx, providers.tsx           QueryClient, ToastProvider, 375px 가운데 틀
    page.tsx                             목록 (/)
    posts/[id]/page.tsx, posts/new/page.tsx, posts/[id]/edit/page.tsx
features/   posts/, comments/, likes/  기능별 api, 쿼리 hooks, 컴포넌트
shared/     api/(apiFetch, ApiError), ui/(Dialog, BottomSheet, Toast, 아이콘), lib/(시간, 글자 수, 요청자, 아바타 — 순수 함수), hooks/
```

- **`/dev-user` 화면을 바꾸지 않으려고** 375px 틀, Provider, Tailwind를 `(community)` 그룹에만 적용했다. Tailwind를 루트 CSS에 넣으면 기본 초기화(preflight)가 `/dev-user`의 여백까지 바꾸기 때문이다. 루트 `layout.tsx`, `globals.css`는 스타터 그대로다.
- 백엔드와 같은 경계(게시글, 댓글, 좋아요)로 나눠 양쪽 계층을 같은 기준으로 설명할 수 있게 했다.

---

## 데이터 적재

`seed/seed.json`(게시글 42, 댓글 2,076)을 백엔드가 기동할 때 직접 읽어 적재합니다 — `backend/.../seed/SeedLoader.java`.

| 무엇을 | 어떻게 | 이유 |
| --- | --- | --- |
| **언제** | `SmartInitializingSingleton.afterSingletonsInstantiated()` — 모든 빈이 준비된 직후, **웹 서버가 포트를 열기 전** (B3) | 적재가 끝나야 포트가 열리므로 적재 중에 `/health`, API 요청이 들어오지 않는다. 제공된 compose 마운트(`/seed`, `APP_SEED_PATH`)를 그대로 쓴다 |
| **중복 방지** | `posts`가 비어 있을 때만 적재 (B4) | 행을 지우지 않는 설계라 한 번 적재되면 다시 비지 않는다. 별도 "적재 완료" 테이블이 필요 없다 |
| **원자성** | 전체를 `TransactionTemplate` 하나로 묶음 | 실패하면 롤백되고 기동이 실패한다. 반쯤 적재된 상태가 남지 않는다 |
| **id 유지** | 시드 id를 그대로 INSERT하고, 같은 트랜잭션에서 `setval`로 시퀀스를 최대 id에 맞춤 (`resetSequence`) (B5) | id를 지정해 넣으면 시퀀스가 따라오지 않아 첫 작성에서 PK가 충돌한다. 새 글은 43, 새 댓글은 2077부터 이어진다 |
| **삭제 표시** | `deletedPostIds`, `deletedCommentIds`에 있는 행만 `deleted_at` = 적재 시각 | 시드에 삭제 시각이 없어 적재 시각으로 표시했다. 삭제된 글의 댓글은 건드리지 않는다 (B6-c) |
| **좋아요** | 게시글마다 `post_like_counts`를 시드의 `likeCount`로 생성, 좋아요 기록은 없음 | 과제: "좋아요 기록은 없고 `likeCount`만 주어진 값에서 시작" |
| **대량 INSERT** | JPA가 아니라 `JdbcTemplate.batchUpdate` | 2천여 행을 엔티티로 하나씩 저장할 이유가 없고, id를 지정한 INSERT가 SQL로 더 명확하다 |

**스키마** — `backend/src/main/resources/schema.sql` (기동마다 실행되며 모든 문장이 `IF NOT EXISTS`)

```text
posts             id, title VARCHAR(20), content TEXT, author, created_at, deleted_at
comments          id, post_id(FK), content, author, created_at, deleted_at
post_likes        id, post_id(FK), user_id, UNIQUE(post_id, user_id)        ← 같은 사람은 한 글에 한 번만
post_like_counts  post_id(PK = FK), like_count CHECK(>= 0), version          ← 게시글당 1행, 낙관적 락
```

**검증한 결과** — 빈 DB 첫 기동: 게시글 42(삭제 3), 댓글 2,076(삭제 81) 저장, API 활성 데이터 게시글 39, 댓글 1,926. 재기동, `docker compose down` 후 재기동: `Seed skipped`, 행 수 그대로.
저장(소프트 삭제 포함)과 노출(필터링)을 따로 확인한 이유는, 삭제 데이터도 행으로 남기는 설계라 활성 개수만 보면 삭제 행이 실제로 저장됐는지 놓치기 때문이다.

---

## 테스트

### 백엔드 — 75개 (`cd backend && ./gradlew test`)

| 종류 | 대상 | 왜 |
| --- | --- | --- |
| 단위 (DB 없음) | `RequesterIdResolverTest`, `CodePointLengthValidatorTest`, `LikeFacadeRetryTest` | 규칙 자체를 빠르게 고정: 요청자 판단(공백, 빈 값, 불일치), 코드포인트 세기(시드 6번 제목 19자), 재시도 대상 판별(버전 충돌, 유니크 위반은 재시도, CHECK 위반은 그대로 올림, 3회 후 409) |
| 웹 계층 | `CommonWebLayerTest` | 오류 응답 형식, 쿠키 URL 디코딩, 경로 변수, 쿼리 파라미터 형식 오류 구분 |
| 통합 (Testcontainers PostgreSQL) | `SeedLoaderTest`, `Post*ApiTest`, `CommentApiTest`, `LikeToggleApiTest`, `SoftDeleteExposureTest` | DB에서만 의미 있는 동작: 시드 개수, 멱등성, 시퀀스, 커서 중복, 누락, 소프트 삭제 노출, 좋아요 동시 요청 |

- **`SoftDeleteExposureTest`**: 소프트 삭제를 쿼리마다 명시하는 설계라 조건 하나만 빠져도 노출된다. 목록, `total`, 상세, 댓글 목록, 댓글 `total`, `commentCount` 전 경로를 한 곳에서 검사한다.
- **좋아요 동시성**: "서로 다른 사용자 N명이 동시에 누르면 정확히 +N"은 낙관적 락 + 3회 재시도로는 보장되지 않는다는 것을 측정으로 확인했다. 그래서 테스트는 확률적인 "성공 수"가 아니라 불변식(숫자 = 기록 수 = 성공 수)을 단언한다.
- 테스트 외에도 실제 `docker compose`로 띄워 API 9개를 curl로 81개 항목 확인했다 (응답 형태, 권한, 삭제 정책, 한글, 이모지 본문, 커서로 끝까지 받기, 포트 변경 기동).

### 프론트엔드 — Vitest 순수 로직 (F10)

상대 시간 구간 경계(59분 59초, 7일, 30일, 365일), 절대 날짜 형식(`26.08.31`, 한국 시간 날짜 경계), 코드포인트 세기, 자르기, 요청자 판단, 좋아요 수렴(연타, 실패 복구)을 단위 테스트로 고정한다. 화면 흐름은 375px 실제 브라우저와 compose 기동으로 확인한다.

---

## 개선한 점, 남은 과제

### 개선한 점

피그마와 과제에 없지만, 직접 써 보면 불편할 것 같은 지점을 채웠습니다.

**글을 쓰다 뒤로 가면 경고 팝업을 띄웁니다** (D8)
- 불편: 긴 글을 쓰다가 실수로 뒤로 가기를 누르면 쓴 내용이 전부 사라집니다.
- 개선: 내용을 입력한 상태에서 뒤로 가기를 누르면 "작성 중인 내용이 저장되지 않습니다. 정말 나가시겠습니까?" 경고 팝업이 떠서, 실수로 나가 쓴 글을 잃는 것을 방지했습니다. `계속 작성`을 누르면 입력한 내용 그대로 돌아옵니다. 수정 화면은 실제로 고친 내용이 있을 때만 띄워서, 아무것도 안 고쳤는데 팝업이 뜨는 번거로움은 없앴습니다.

**제목 옆에 글자 수를 보여 줍니다** (D10)
- 불편: "(최대 20자)" 안내는 입력을 시작하면 사라져서, 21번째 글자가 왜 안 써지는지 알 수 없습니다.
- 개선: 제목 오른쪽에 `N / 20`을 항상 보여 주고, 20자가 되면 숫자가 빨간색으로 바뀝니다.

**게시글을 지우면 삭제됐다고 알려 줍니다** (D11)
- 불편: 삭제하면 바로 목록으로 넘어가서, 지워진 건지 오류로 튕긴 건지 헷갈립니다.
- 개선: 목록에 "게시글이 삭제되었습니다." 토스트를 띄웁니다.

**댓글을 지워도 삭제됐다고 알려 줍니다** (D9)
- 불편: 댓글이 그냥 사라지기만 하면 제대로 처리됐는지 확신하기 어렵습니다.
- 개선: "댓글이 삭제되었습니다." 토스트를 띄웁니다. 댓글 작성은 새 댓글이 바로 맨 위에 보여서 따로 알리지 않습니다.

**댓글 전송 버튼을 넣었습니다** (FD-1)
- 불편: 피그마 화면에는 댓글을 보내는 버튼이 없습니다.
- 개선: 피그마 컴포넌트에 있던 빨간 전송 버튼을 썼습니다. 글자를 입력해야 버튼이 나타나서 빈 댓글은 보낼 수 없습니다. 엔터는 줄바꿈이라 여러 줄로 쓸 수 있습니다.

**없는 글에 들어가면 안내창을 띄웁니다** (FD-5)
- 불편: 주소를 직접 입력했거나 보는 사이 글이 지워졌을 때, 빈 화면만 보이면 무슨 일인지 모릅니다.
- 개선: "삭제되었거나 존재하지 않는 게시글입니다." 안내창과 목록으로 가는 버튼을 띄웁니다. 안내창 뒤에는 지워진 글이 비치지 않습니다.

**불러오기에 실패하면 다시 시도할 수 있습니다** (FD-15)
- 불편: 실패를 토스트로만 알리면, 토스트가 사라진 뒤 빈 화면에 갇힙니다.
- 개선: 그 자리에 "불러오지 못했습니다."와 다시 시도 버튼을 둡니다.

**댓글이 없으면 문구로 알려 줍니다** (FD-6)
- 불편: 빈 공간만 있으면 불러오는 중인지, 오류인지, 정말 댓글이 없는지 구분이 안 됩니다.
- 개선: "아직 댓글이 없습니다."를 보여 줍니다.

**남의 글 수정 화면은 들어오자마자 막습니다** (FD-14)
- 불편: 메뉴가 없어도 주소로 수정 화면에 들어올 수 있습니다. 저장할 때 거절하면 입력한 내용을 잃습니다.
- 개선: 들어오는 순간 상세 화면으로 돌려보내고 이유를 알려 줍니다.

### 남은 과제

**인기 글에 좋아요가 몰리면 일부 요청이 실패할 수 있습니다**
- 여러 사람이 정확히 동시에 누르면, 재시도 3번 안에 처리 못 한 요청은 409 응답을 받습니다. 8명이 동시에 누르면 3~5명이 성공했습니다.
- 숫자와 기록은 어긋나지 않습니다. 좋아요가 한 글에 동시에 몰리는 일은 지금 규모에서 드물어서 낙관적 락으로 충분하다고 판단했습니다.
- 사용자가 늘어 충돌이 잦아지면 두 단계로 바꿀 수 있습니다.
  - 먼저 DB 안에서: 카운터를 `like_count = like_count + 1`처럼 DB가 한 번에 더하는 방식으로 바꾸면, 서로 다른 사용자끼리는 충돌 없이 모두 성공합니다. (plan B8)
  - 그래도 DB 쓰기가 부담되면 Redis로: 누른 사람 목록은 Redis Set(`SADD` / `SREM`), 숫자는 `INCR` / `DECR`로 처리하면 Redis가 한 번에 하나씩 처리하므로 잠금과 재시도가 필요 없습니다. DB에는 주기적으로 모아서 반영합니다. 대신 Redis 서버를 추가로 운영해야 하고, DB 반영 전에 Redis가 죽으면 그 사이 좋아요가 사라지지 않게 하는 처리가 필요합니다.

**게시글, 댓글 작성 "따닥" 중복은 화면에서만 막았습니다**
- 버튼을 빠르게 두 번 누르면 같은 글이나 댓글이 두 번 저장될 수 있습니다. 화면에서는 요청이 끝날 때까지 같은 버튼을 다시 실행하지 않게 막았습니다(`useSingleFlight`). 버튼 비활성화는 화면이 다시 그려진 뒤에 적용돼서, 그 사이 들어오는 연타까지 막으려고 따로 잠금을 걸었습니다.
- 서버는 같은 요청이 두 번 오면 그대로 두 번 저장합니다. 화면을 거치지 않고 API를 직접 호출하거나, 응답을 받지 못한 사용자가 다시 보내면 중복이 생길 수 있습니다.
- 서버에서도 막으려면 요청마다 고유한 키(`Idempotency-Key` 헤더)를 받아, 같은 키로 다시 온 요청은 저장하지 않고 처음 결과를 돌려주는 방식이 필요합니다.

**댓글이 많아지면 목록이 느려질 수 있습니다**
- 목록의 댓글 수는 볼 때마다 셉니다. 지금 인덱스는 기본 키와 유니크 제약뿐입니다.
- 현재 데이터에서는 차이가 없어 미리 추가하지 않았습니다. 데이터가 늘면 `comments(post_id)` 인덱스를 추가할 계획입니다. (backend tasks T-80)

**브라우저 뒤로 가기와 새로고침은 묻지 않습니다**
- 화면의 뒤로 가기 화살표를 누를 때만 경고 팝업을 띄웁니다.
- Next.js App Router에는 브라우저 이동을 막는 공식 방법이 없어, 억지로 막으면 다른 이동이 깨질 수 있다고 판단했습니다.

**이모지가 20자 경계에서 잘릴 수 있습니다**
- 피부색 이모지(🙏🏻)처럼 눈에는 한 글자지만 내부적으로는 두 글자 이상인 이모지가 있습니다. 제목이 19자일 때 이런 이모지를 넣으면 20자를 넘는 부분이 잘려 🙏 만 남습니다.
- 화면과 서버가 글자 수를 같은 방식으로 세게 해서, 화면에서 입력되는 제목은 저장할 때 항상 통과하도록 했습니다. 눈에 보이는 개수로 세면 화면에서는 20자인데 서버에서는 21자로 거절되는 일이 생기기 때문입니다.
