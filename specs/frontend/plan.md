# Frontend Plan

`specs/frontend/spec.md`를 어떻게 구현할지 (HOW).
기술 선택은 사용자가 결정한 뒤 "무엇을 골랐고, 왜, 무엇을 포기했는지"로 기록한다.

---

## 기술 결정

| # | 결정할 내용 | 선택 | 이유 | 포기한 것 |
| --- | --- | --- | --- | --- |
| F1 | 데이터 호출 구조 (F-DATA) | **브라우저 직접 호출** — 화면은 클라이언트 컴포넌트, 브라우저가 `NEXT_PUBLIC_API_BASE_URL`로 백엔드 호출 (`credentials: "include"`) | 스타터의 CORS(자격 증명 허용) · 브라우저용 API 주소 · compose 빌드 인자가 이 방식을 전제로 준비돼 있어 추가 인프라가 없다. 무한 스크롤 · 좋아요 · 댓글 · 다이얼로그 등 핵심 기능이 모두 상호작용이라 데이터 경로를 브라우저 하나로 두면 흐름이 단순하다. `/dev-user` 쿠키가 자동 전송되어 헤더 변환이 필요 없다 | 첫 화면이 빈 채로 뜬 뒤 채워짐 (로딩 표시 필요). Next 서버 기능(서버 렌더링 · Route Handler)을 쓰지 않음. 백엔드 포트를 바꾸면 프론트 재빌드 필수 (아래 "F1 확인 사항") |
| F2 | 서버 데이터 상태 (F-STATE) | **TanStack Query** | 무한 스크롤(`useInfiniteQuery` + `nextCursor`), 진행 중 중복 요청 방지(F-61), 변경 후 화면 간 갱신(`invalidateQueries`, F-09), 캐시 직접 갱신(`setQueryData`, F-21 좋아요), 뒤로 가기 시 캐시 재사용이 spec 요구와 그대로 맞는다 | 의존성 하나와 쿼리 키 · 무효화 개념이 추가된다. 좋아요의 `desired` / `confirmed` 수렴 로직은 라이브러리가 해주지 않아 직접 작성한다 |
| F3 | 스타일링 (F-STYLE) | **Tailwind CSS v4** — 피그마 색 · 타이포 · 간격을 `app/(community)/community.css`의 `@theme` 토큰으로 한 번 등록해 클래스로 사용 | 마크업에서 바로 작성해 빠르고, 버튼 활성/비활성 · 하트 켜짐/꺼짐 · 20자 도달 · 전송 버튼 표시처럼 상태가 많은 화면을 `disabled:` · `data-[…]:` 변형 문법으로 간결하게 표현한다. 토큰 스케일 안에서 쓰게 되어 값이 흩어지지 않는다 | 패키지와 PostCSS 설정이 추가된다. `className`이 길어지고 의미 있는 클래스 이름이 없다. 조건부 조합에는 `clsx` 같은 도우미가 필요할 수 있다. 플랫폼별 네이티브 모듈을 써서 Docker(alpine) 빌드를 따로 확인해야 한다 (F1 확인 사항) |
| F4 | 화면 공통 상태 (F-UI) | **React Context** — 전역은 토스트 하나뿐 (`ToastProvider`를 `(community)` 레이아웃에 두고 `useToast()`로 호출). 다이얼로그 · 바텀시트는 각 화면의 `useState` | 화면 이동 뒤에도 보여야 하는 것은 토스트뿐이라(작성 → 목록, 삭제 → 목록, 수정 → 상세) 그룹 레이아웃에 Provider 하나면 충분하다. 토스트 호출은 컴포넌트 안의 요청 성공 콜백에서 hook으로 하므로 전역 저장소 라이브러리가 필요 없다 | Provider와 hook을 직접 작성한다. 전역 대상이 늘어나면 Context가 여러 개로 나뉘거나 다시 그려지는 범위를 신경 써야 한다 |
| F5 | 요청자 · 본인 여부 (F-REQ) | **쿠키만 사용** — 프론트는 `/dev-user`가 설정한 `x-user-id` 쿠키를 `credentials: "include"`로 보낸다. 백엔드는 과제 요구사항에 따라 헤더 · 쿠키를 모두 지원한다 (B-01). 프론트는 기본적으로 같은 값을 헤더로 중복 전송하지 않는다. 단 API 호출 함수(`apiFetch`)는 선택적으로 `x-user-id` 헤더를 받을 수 있게 만들어, 필요할 때(테스트 · 이후 서버 쪽 호출 등) 헤더로도 요청할 수 있다 — 이때 쿠키와 다른 값을 넣으면 서버가 400을 준다 (B-01). 본인 여부는 브라우저에서 쿠키를 읽어 B-01과 같은 규칙(trim, 비었으면 `apr_tester`, 대소문자 구분 정확 일치)으로 판단한다 (`getRequester()` 하나) | "헤더 또는 쿠키로 판단"은 요청을 받는 서버의 규칙이고 이미 구현 · 검증됐다. 쿠키와 헤더는 출처가 같아 중복 전송으로 얻는 정보가 없고, 어긋나면 400이 되는 경로만 늘어난다. API · 백엔드 변경 없이 동작한다 | 서버의 요청자 규칙(B-01)을 프론트에 한 번 더 구현한다 — 규칙이 바뀌면 두 곳을 고쳐야 한다 |
| F6 | 라우트 (F-ROUTE) | **`/` 목록 · `/posts/[id]` 상세 · `/posts/new` 작성 · `/posts/[id]/edit` 수정**. 작성 · 수정 페이지는 얇게 두고 같은 `PostForm` 컴포넌트에 모드와 초기값만 넘긴다 | 주소가 화면 의미를 그대로 드러내고, 상세 아래에 수정이 오는 구조가 상세 → 수정 → 상세 흐름과 맞는다. "수정 = 작성과 같은 화면"(F-44)은 공유 컴포넌트로 지킨다 | 페이지 파일이 하나 더 생긴다 |
| F7 | 절대 날짜 시간대 (F-TZ) | **`Asia/Seoul` 고정** — `Intl.DateTimeFormat('ko-KR', { timeZone: 'Asia/Seoul', year: '2-digit', month: '2-digit', day: '2-digit' })`의 `formatToParts()`에서 year · month · day만 꺼내 `${year}.${month}.${day}`로 조립한다. `format()` 결과를 그대로 쓰지 않는다 — `ko-KR` 기본 형식은 `26. 08. 31.`처럼 공백 · 끝 마침표가 붙는다 (Node 24에서 확인) | 한국 서비스라 한국 시간 기준으로 맞춘다. 같은 글은 보는 위치와 관계없이 같은 날짜로 보이고, 날짜 테스트가 실행 환경 시간대(로컬 KST · 컨테이너 UTC)에 따라 달라지지 않는다. 라이브러리 없이 표준 API로 처리된다 | 해외에서 보는 사용자도 한국 날짜로 본다 (상대 시간은 시간대와 무관해 영향 없음) |
| F8 | 아이콘 출처 (F-ICON) | **피그마 벡터 추출** — `design/메디큐브톡.fig`의 경로 데이터를 SVG React 컴포넌트로 옮긴다 | 디자인 반영(F-02)에 가장 정확하다 — 라이브러리 아이콘은 모양 · 선 굵기가 달라진다. 의존성이 없고 아이콘이 8개 정도라 작업량이 작다 | 추출 작업이 필요하고, 회전 · 마스크가 걸린 아이콘은 좌표를 손으로 맞춰야 할 수 있다. 추출 결과는 화면에 그려 피그마와 대조한다 |
| F9 | lint (F-LINT) | **ESLint + `eslint-config-next`** — ESLint CLI 직접 실행 (`eslint.config.mjs`, `npm run lint`). `next lint`는 Next 15.5부터 폐기 예정이라 쓰지 않는다 | TanStack Query · 무한 스크롤 감지 · 좋아요 수렴처럼 hooks 의존성 실수가 버그로 이어지기 쉬운 코드가 많아 `react-hooks` 규칙이 필요하다. Next 공식 규칙 세트 | 개발 의존성과 설정 파일이 추가된다. 포맷팅 도구(Prettier)는 정하지 않았다 |
| F10 | 테스트 (F-TEST) | **Vitest 단위 테스트 — 순수 로직만**: 상대 시간 · 절대 날짜, 코드포인트 세기 · 자르기, 요청자 판단, 아바타 선택, 좋아요 수렴 | 경계값이 많아 화면에서 일일이 확인하기 어려운 규칙(59분 59초 · 7일 · 30일 경계, 이모지, 공백, 연타 · 실패 복구)을 빠르고 정확하게 고정한다. 화면 흐름은 Verification의 375px 수동 확인과 Integration 검증에서 실제 브라우저로 확인한다 | UI 동작(무한 스크롤 · 다이얼로그 · 폼 활성 조건)은 자동 테스트가 없다 |
| F11 | 폴더 구조 (F-DIR) | **기능별** — `app/`은 얇은 페이지(라우트)만, `features/{posts,comments,likes}`에 기능별 api · 쿼리 hooks · 컴포넌트, `shared/`에 공용(api 클라이언트 · UI · 순수 함수). FSD 같은 다층 구조는 쓰지 않는다 | spec · API · 백엔드가 모두 게시글 · 댓글 · 좋아요로 나뉘어 있어 같은 경계로 나눈다. 백엔드 기능별 패키지(B10 `post/` `comment/` `like/`)와 대응되어 양쪽 계층을 같은 기준으로 설명할 수 있다. 한 기능을 고칠 때 한 폴더만 본다 | 공용(`shared`)과 기능 전용의 경계를 판단해야 한다. 화면이 적어 폴더 수가 파일 수에 비해 많아 보일 수 있다 |

---

## 스타터에서 확인한 사실

- Next.js 15.4.6 · React 19.1 · TypeScript 5, 의존성은 이 셋뿐이다. lint · 테스트 도구 없음.
- `app/layout.tsx` · `app/globals.css`(Pretendard Variable, `font-weight 45~920`) 존재.
- `app/dev-user/page.tsx`(수정 금지)는 `document.cookie`로 `x-user-id`를 `path=/`, `SameSite` 미지정으로 쓰고, 유저를 고르면 `window.location.href = "/"`로 **페이지를 새로 불러온다**.
  - 프론트(`localhost:3000`)와 백엔드(`localhost:8080`)는 포트만 다른 **같은 사이트**라, 브라우저 요청에 `credentials: "include"`를 붙이면 이 쿠키가 백엔드로 전송된다.
  - 새로 불러오므로 유저를 바꾸면 TanStack 캐시도 초기화된다 — 이전 유저의 `isLiked` · 본인 여부가 남지 않는다.
- API 주소는 두 개: 브라우저용 `NEXT_PUBLIC_API_BASE_URL`(빌드 시 인라인), 컨테이너 내부용 `API_BASE_URL`(`http://backend:8080/api`).
- `Dockerfile`은 `node:22-alpine`에서 `npm ci` → `next build` → `next start`.

## 구조 (F11)

```text
app/
  layout.tsx                 스타터 그대로 (html · body · globals.css)
  globals.css                스타터 그대로 (Pretendard)
  dev-user/                  스타터 그대로 (수정 금지)
  (community)/               괄호 폴더 = 주소에 나타나지 않는 Route Group
    layout.tsx               서버 컴포넌트: <Providers>로 감싸고 375px 가운데 틀 (F-01) · community.css 불러오기
    providers.tsx            "use client": QueryClient를 useState로 한 번만 생성 → QueryClientProvider · ToastProvider
    community.css            @import "tailwindcss" + @theme 토큰 (F3)
    page.tsx                 목록 (/)
    posts/[id]/page.tsx      상세
    posts/new/page.tsx       작성
    posts/[id]/edit/page.tsx 수정
features/
  posts/     api.ts · queries.ts(쿼리 키 · hooks) · PostList · PostListItem · PostDetail · PostMenuSheet · PostForm
  comments/  api.ts · queries.ts · CommentSection · CommentItem · CommentInput
  likes/     api.ts · useLikeToggle.ts(desired / confirmed 수렴) · LikeButton
shared/
  api/       apiFetch(기본 URL · credentials · 선택 헤더) · ApiError(code · message · errors)
  ui/        Dialog · BottomSheet · ToastProvider / useToast · TopBar · NotFoundDialog · LoadError · icons/ (피그마 추출, F8)
  lib/       time(상대 · 절대) · text(코드포인트 세기 · 자르기) · requester(getRequester) · avatar · goBack — React 무관 순수 함수만
  hooks/     useInView(IntersectionObserver)
```

- **`/dev-user` 외형을 바꾸지 않는다.** 375px 틀 · Provider · Tailwind는 `(community)` 그룹에만 적용하고, 루트 `layout.tsx` · `globals.css`는 건드리지 않는다.
  Tailwind를 루트 CSS에 넣으면 기본 초기화(preflight)가 `/dev-user`의 제목 · 문단 여백 등을 바꾸기 때문이다. `/dev-user`는 주소로 직접 열리거나 새로 불러와지므로 그룹의 CSS가 섞이지 않는다.
- 페이지는 데이터를 직접 다루지 않고 `features` 컴포넌트를 조립만 한다.
- 기능 간 참조는 공개된 쿼리 키 · hook으로만 한다 (예: 댓글 작성 후 `posts`의 쿼리 키로 무효화).
- `shared/lib`은 React에 의존하지 않는 순수 함수만 두고 Vitest 대상으로 삼는다 (F10). hook은 `shared/hooks` · 각 기능 폴더에 둔다.
- **Provider는 클라이언트 컴포넌트로 분리한다.** `layout.tsx`는 기본이 서버 컴포넌트라 Context Provider를 직접 쓸 수 없다. `providers.tsx`(`"use client"`)에서 `const [queryClient] = useState(() => new QueryClient(기본값))`로 만들어, 렌더링마다 캐시가 새로 생기지 않게 한다.

## 상태 설계

README "상태 설계" 절의 근거.

| 상태 | 두는 곳 | 이유 |
| --- | --- | --- |
| 서버 데이터 (목록 · 상세 · 댓글) | TanStack Query 캐시 (쿼리 키별) | 화면 간 공유 · 무한 스크롤 페이지 누적 · 변경 후 무효화를 한 곳에서 처리 (F2) |
| 좋아요 진행 상태 | 게시글별 `desired` · `confirmed` · `inFlight` (`useLikeToggle` 내부), 화면 값은 캐시에 반영 | 즉시 반영과 서버 확인 값을 분리해야 연타 · 실패 복구가 가능 (F-21) |
| 폼 입력 (제목 · 내용 · 조합 중 여부 · 처음 값) | `PostForm`의 로컬 상태 | 그 화면에서만 쓰고, 이탈 확인(F-46)도 같은 컴포넌트에서 계산 |
| 댓글 입력 | `CommentInput`의 로컬 상태 | 전송 후 비우기만 하면 됨 |
| 다이얼로그 · 바텀시트 열림 | 각 화면의 로컬 상태 | 화면 이동 시 함께 사라져야 함 |
| 토스트 | `ToastProvider` Context | 화면 이동 뒤에도 보여야 하는 유일한 상태 (F4) |
| 요청자 | 저장하지 않음 — 필요할 때 쿠키에서 읽음 (`getRequester()`) | `/dev-user`가 쿠키만 바꾸므로 쿠키가 유일한 출처 (F5) |

## 구현 규칙 · 주의점

- **서버에서 한 번 그려지는 점 (F1)** — App Router는 `"use client"` 컴포넌트도 첫 HTML을 서버에서 렌더링한다. 서버에는 `document.cookie`가 없다.
  - 쿠키 읽기(`getRequester()`) · 본인 판단 · 상대 시간 계산은 브라우저에서 데이터를 받은 뒤에만 한다. 쿼리는 서버에서 실행되지 않으므로 데이터가 있는 화면은 브라우저에서만 그려진다.
- **API 주소** — 브라우저 코드는 `NEXT_PUBLIC_API_BASE_URL`만 쓴다. 주소를 코드에 직접 적지 않는다. 로컬 `next dev`용으로 `frontend/.env.development`에 `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api`를 둔다 (`next dev`에서만 읽히고 Docker 빌드에는 영향 없음). `apiFetch`는 값이 없으면 분명한 오류를 낸다.
- **제목 글자 수 (F-42 · P-42)** — 입력값 그대로의 코드포인트(`[...title].length`)로 세고 20에서 자른다. `N / 20`도 같은 값이다. 서버는 trim 후 20 이하를 보므로 입력값이 20 이하면 항상 통과한다. HTML `maxLength`는 UTF-16 단위라 쓰지 않는다.
  - 코드포인트 단위로 자르므로 UTF-16 반쪽(깨진 글자)은 생기지 않는다. 단 여러 코드포인트로 된 이모지(`🙏🏻` · ZWJ 이모지)는 20번째 경계에 걸리면 중간에서 잘려 모양이 바뀔 수 있다 — 코드포인트 기준(D2)을 지키는 대가로 받아들인다.
  - 한글 조합 중(`compositionstart` ~ `compositionend`)에는 자르지 않고, 조합이 끝난 뒤 자른다. 붙여넣기로 넘친 경우도 같은 방식으로 자른다.
  - 빈 값 판단(완료 버튼 활성)은 `trim()` 기준 (P-41).
- **상대 시간 (F-12)** — 구간표대로 일 단위 내림. 미래 시각은 `방금 전`.
- **절대 날짜 (F-20 · F-33)** — `YY.MM.DD`, `Asia/Seoul` (F7). `formatToParts()`로 조립 (예: `2026-08-30T15:30:00Z` → `26.08.31`, 한국 시간으로 날짜가 바뀌는 경계). Vitest로 이 경계와 형식(공백 · 끝 마침표 없음)을 고정한다.
- **좋아요 (F-21)** — 게시글별로 `desired`(마지막으로 누른 상태) · `confirmed`(서버가 확인한 상태) · `inFlight`를 둔다.
  1. 누르면 `desired`를 뒤집고 화면은 `desired` 기준으로 즉시 그린다.
     - 아이콘: `desired`
     - 숫자: `confirmed.likeCount + (desired === confirmed.isLiked ? 0 : desired ? 1 : -1)` — 서버가 확인한 숫자에 "확인된 상태와 원하는 상태가 다를 때만" ±1. 그 사이 다른 사용자의 좋아요는 응답의 `likeCount`로 반영된다
  2. 토글을 시작할 때 해당 상세 쿼리의 진행 중 재조회를 `cancelQueries`로 멈춘다. 진행 중에는 재조회 결과가 화면의 `desired`를 덮지 않게 한다.
  3. `inFlight`가 아니면 토글 요청. 응답으로 `confirmed` 갱신.
  4. `confirmed.isLiked !== desired`면 한 번 더 토글, 같으면 끝. 끝나면 상세 · 목록 캐시에 `confirmed`를 반영한다.
  5. 실패하면 상세를 다시 조회해 `confirmed` · `desired`를 서버 값으로 맞추고 토스트. 재조회가 404면 F-53.
- **중복 제출 방지** — 요청이 진행 중이면 `작성 완료` · `수정 완료` · 댓글 전송 · 삭제 다이얼로그 `예` 버튼을 누를 수 없다 (mutation `isPending`).
- **삭제된 글 감지 (F-25)** — 상세 조회뿐 아니라 댓글 목록 · 댓글 작성 · 댓글 삭제 · 좋아요 · 수정 저장 · 게시글 삭제에서 `POST_DELETED` · `POST_NOT_FOUND`를 받아도 같은 `NotFoundDialog`(F-53)를 띄운다 (보는 사이 다른 곳에서 삭제된 경우). 다이얼로그를 띄울 때 해당 게시글을 "없음" 상태로 표시해(상세 캐시 제거 + 화면 상태) 상세 내용 · 댓글 · 입력 바(수정 화면이면 폼)를 그리지 않고 상단 바만 남긴다 — 뒤에 삭제된 글이 비치지 않게.
- **뒤로 가기 (F-08)** — 앱 안에서 이동해 온 기록이 있으면 `router.back()`, 주소로 직접 들어와 기록이 없으면 `/`로 이동 (`goBack`).
- **남의 글 수정 화면 (F-47)** — 수정 페이지는 상세 캐시(없으면 조회)로 먼저 작성자를 확인한다. 확인 중에는 폼 대신 상단 바 + `불러오는 중…`. 본인 글이면 `PostForm`, 남의 글이면 폼을 그리지 않고 `replace('/posts/{id}')` + 토스트 `다른 사람의 글은 수정할 수 없습니다.`, 404면 `NotFoundDialog`.
- **서버 검증 오류 (F-45)** — `INVALID_INPUT`의 `errors`가 여러 개면 첫 번째 `message`를 토스트로 보여준다.
- **불러오기 실패 (F-63)** — 목록 · 상세 · 댓글 쿼리가 첫 로딩에서 실패하면(`isError` · 데이터 없음) 그 자리에 `LoadError`(`불러오지 못했습니다.` + `다시 시도`)를 그리고, `다시 시도`는 해당 쿼리의 `refetch()`. 무한 스크롤 다음 묶음 실패(`isFetchNextPageError`)는 목록 아래에 같은 컴포넌트를 그리고 `fetchNextPage()`로 다시 시도한다. 404 `POST_DELETED` · `POST_NOT_FOUND`는 이 안내 대신 `NotFoundDialog`.
- **아바타 (F-07)** — 작성자 문자열의 코드포인트 합 % 3 → `avatar-{1,2,3}.png`. 같은 작성자는 항상 같은 이미지.
- **본인 여부 (F-05)** — `x-user-id` 쿠키 값을 trim, 비었으면 `apr_tester`. `author`와 대소문자 구분 정확 일치 (F5).
- **아이콘 (F8)** — 연필 · 휴지통 · 하트 외곽선/채움 · 댓글 · 화살표(뒤로 · 아래) · 세로 점 3개 · 전송을 피그마 벡터에서 SVG로 옮긴다.
- **Vitest 경로 별칭** — `@/` 별칭은 `vitest.config`의 `resolve.alias`로 맞춘다 (추가 패키지 없음).

## 쿼리 키와 캐시 갱신 (F2)

| 쿼리 키 | 요청 | 비고 |
| --- | --- | --- |
| `['posts', 'list']` | `GET /posts?size=20` + 커서 (infinite) | `getNextPageParam` = `nextCursor` (null이면 끝, F-15) |
| `['posts', 'detail', id]` | `GET /posts/{id}` | 404 `POST_DELETED` · `POST_NOT_FOUND` → F-53 |
| `['comments', postId]` | 첫 페이지 `size=2`, 이후 `size=20` + 커서 (infinite) | 페이지 파라미터에 `size`를 함께 담는다 |

| 동작 | 캐시 처리 | 이동 · 토스트 |
| --- | --- | --- |
| 게시글 작성 | 응답 Post를 목록 첫 페이지 맨 앞에 넣고(가장 최신이라 위치가 정확함) 목록 무효화 | `push('/')` + `게시글이 등록되었습니다.` |
| 게시글 수정 | 응답으로 상세 캐시 교체, 목록 캐시의 해당 항목도 응답 값으로 교체, 목록 무효화 | `replace('/posts/{id}')` + `게시글이 수정되었습니다.` (뒤로 가기에 수정 화면이 남지 않음) |
| 게시글 삭제 | 상세 캐시 제거, 목록 캐시에서 해당 항목 즉시 제거, 목록 무효화 | `replace('/')` + `게시글이 삭제되었습니다.` |
| 좋아요 | 상세 캐시 · 목록 캐시의 해당 항목을 `setQueryData`로 갱신 (`useLikeToggle`) | 실패 시 상세 재조회 + 토스트 |
| 댓글 작성 | 상세 · 목록 캐시의 `commentCount` +1, 댓글 쿼리 무효화(새 댓글이 맨 위), 상세 · 목록 무효화 | 입력창 비우고 `blur()` (키보드 닫기) |
| 댓글 삭제 | 댓글 쿼리 캐시에서 해당 댓글 즉시 제거 · `total` −1, 상세 · 목록 캐시의 `commentCount` −1, 댓글 쿼리 · 상세 · 목록 무효화 | `댓글이 삭제되었습니다.` |

- **즉시 반영 → 무효화 순서** (F-09). 화면에 없는 쿼리는 무효화해도 바로 다시 받지 않고, 다시 그려질 때 캐시의 옛 값을 먼저 보여준 뒤 재조회한다. 그래서 변경 결과를 `setQueryData`로 캐시에 먼저 반영해 옛 값(삭제한 글 · 옛 제목 · 옛 숫자)이 잠깐 보이는 일을 막고, 무효화로 서버와 최종 정합을 맞춘다.
- 무한 목록은 무효화 시 불러온 페이지를 커서 순서대로 통째로 다시 받으므로, 맨 앞에 넣은 새 글이 중복되지 않는다.
- **작성 시 첫 페이지가 잠깐 21개가 되는 것은 의도된 상태다.** 커서는 위치(offset)가 아니라 "직전 페이지 마지막 글의 `(createdAt, id)`"라서, 맨 앞에 넣어도 이미 받은 2번째 페이지(원래 20번째 글 다음부터)와 빠지거나 겹치는 글이 없다. 페이지당 20개로 맞추려고 첫 페이지 끝을 **자르지 않는다** — 잘린 글은 2번째 페이지에도 없어 재조회 전까지 사라진다. 재조회는 1페이지부터 순차로 받고 다음 커서를 새 페이지 기준으로 다시 계산하므로(TanStack Query v5 무한 쿼리) 정상 구성으로 돌아간다.

## spec 대응

| spec | 구현 |
| --- | --- |
| F-01 375px · F-02 디자인 · F-03 글꼴 | `(community)/layout.tsx` 가운데 375px 틀. 하단 고정 요소(입력 바 · 작성 버튼 · 토스트 · 완료 버튼)도 같은 폭 안에 둔다. 피그마 값은 `community.css` `@theme` 토큰 (F3). Pretendard는 스타터 `globals.css` 그대로 |
| F-04 · F-05 요청자 · 본인 여부 | `apiFetch`가 `credentials: "include"`. `getRequester()`로 본인 판단 (F5) |
| F-06 숫자 | `toLocaleString('ko-KR')` |
| F-07 아바타 | `shared/lib/avatar` |
| F-08 뒤로 가기 | `TopBar`의 화살표 → `goBack` (작성 · 수정은 F-46 확인 후) |
| F-09 변경 반영 | 위 "캐시 갱신" 표 |
| F-10 · F-15 목록 무한 스크롤 | `['posts','list']` infinite + 목록 끝 sentinel을 `shared/hooks/useInView`(IntersectionObserver)로 감지해 `fetchNextPage` |
| F-11 · F-12 목록 항목 · 상대 시간 | `PostListItem`, `shared/lib/time` |
| F-13 · F-14 작성 버튼 · 항목 클릭 | `next/link` |
| F-16 상단 바 | `TopBar` (오른쪽 비움) |
| F-20 상세 본문 · F-22 메뉴 | `PostDetail`, 본인 글이면 메뉴 버튼 → `PostMenuSheet` (F-50) |
| F-21 좋아요 | `useLikeToggle` (위 "구현 규칙"의 수렴 절차) |
| F-23 · F-44 수정 | `/posts/[id]/edit` → `PostForm`(수정 모드, 초기값 = 상세 캐시 또는 조회) |
| F-24 삭제 | `Dialog`(F-51) → 삭제 → 위 표 |
| F-25 없는 글 | 위 "삭제된 글 감지" → `NotFoundDialog`(F-53, 바깥 클릭 무시) |
| F-30 · F-31 댓글 2개 → 더보기 → 무한 스크롤 | `['comments', postId]` 첫 페이지 `size=2`. `total >= 3`이고 아직 더보기를 누르지 않았으면 버튼, 누르면 `fetchNextPage` 후 sentinel 자동 로딩으로 전환 |
| F-32 댓글 수 | 댓글 쿼리 첫 페이지의 `total` |
| F-33 · F-35 댓글 항목 · 삭제 | `CommentItem`, 본인 댓글이면 `삭제` → `Dialog`(F-51 댓글 문구) |
| F-34 댓글 작성 | `CommentInput`: `textarea`(엔터 = 줄바꿈), trim 값이 있을 때만 전송 버튼 |
| F-36 댓글 없음 | 첫 페이지 `total === 0`이면 `아직 댓글이 없습니다.` |
| F-40 · F-41 · F-42 입력 · 완료 버튼 · 글자 수 | `PostForm`: trim 기준 활성 조건, `shared/lib/text`로 코드포인트 세기 · 조합 끝난 뒤 자르기, `N / 20` |
| F-43 작성 완료 | 위 표 |
| F-45 서버 검증 실패 | 위 "구현 규칙" |
| F-47 남의 글 수정 화면 | 위 "구현 규칙" |
| F-46 · F-52 이탈 확인 | `PostForm`이 "입력 있음(작성) / 바뀜(수정)"을 계산, 화살표에서만 `Dialog` |
| F-50 바텀시트 | `BottomSheet` (바깥 · `취소`로 닫힘) |
| F-60 토스트 | `ToastProvider` (`(community)` 레이아웃, F4) |
| F-61 로딩 | TanStack Query의 진행 중 상태로 중복 요청 방지 + 표시 |
| F-62 오류 | `ApiError.code`로 분기, 나머지는 토스트 |
| F-63 불러오기 실패 | 위 "구현 규칙"의 `LoadError` |

## 추가 의존성

구현 중 이 목록에 없는 패키지가 필요하면 멈추고 Plan으로 돌아온다 (HARNESS §5).

| 패키지 | 구분 | 근거 |
| --- | --- | --- |
| `@tanstack/react-query` | dependencies | F2 |
| `tailwindcss` · `@tailwindcss/postcss` · `postcss` | devDependencies | F3 |
| `eslint` · `eslint-config-next` · `@eslint/eslintrc` | devDependencies | F9 (flat config에서 Next 규칙 불러오기) |
| `vitest` | devDependencies | F10 (순수 함수만이라 DOM 환경 불필요) |

- `clsx` 등 클래스 조합 도우미는 넣지 않는다. 필요해지면 Plan에서 다시 정한다.

## plan에서 정한 세부값

spec이 "plan에서 정한다"고 남긴 값. 피그마 · 과제에 근거가 없어 제안한 값을 사용자가 확인해 확정했다.

| 항목 | 값 |
| --- | --- |
| 토스트 표시 시간 (F-60) | 2초. 새 토스트가 오면 이전 것을 교체 |
| 로딩 표시 (F-61) | 목록 · 댓글: 목록 아래 `불러오는 중…` (Regular 12/16 `#979ea9`, 가운데). 상세 첫 로딩: 상단 바 + 화면 가운데 같은 문구 |
| 오류 문구 (F-62) | 네트워크 오류 · 500 · 알 수 없는 오류: `잠시 후 다시 시도해 주세요.` 토스트. 403 `NOT_OWNER`: `권한이 없습니다.` |
| 댓글 입력창 높이 (F-34) | 한 줄(40px)에서 시작해 최대 4줄까지 늘어나고 그 이상은 입력창 안에서 스크롤 |
| 상대 시간 갱신 | 화면을 그릴 때 계산하고 주기적으로 다시 계산하지 않는다 |
| QueryClient 기본값 | `staleTime` 30초, 창 포커스 시 재조회 끔, 실패 재시도 1회(4xx는 재시도 안 함). 변경 반영은 명시적 무효화(F-09)로 처리 |

## Verification 추가 항목

HARNESS §6에 더해 프론트에서 확인할 것.

- `/dev-user` 화면이 스타터와 같다 (구현 전후 스크린샷 비교). `git diff`에서 `app/dev-user/` · `app/layout.tsx` · `app/globals.css` 변경 없음.
- `/dev-user`로 유저를 바꾸면 본인 글 메뉴 · 댓글 `삭제` · `isLiked`가 그 유저 기준으로 바뀐다.
- 상세에서 뒤로 가기로 목록에 돌아왔을 때 스크롤 위치가 유지된다.
- 주소로 직접 연 상세에서 뒤로 가기 → 목록.
- 작성 · 댓글 전송 버튼 연타 시 하나만 생성된다.
- 게시글 삭제 · 수정 · 작성 후 목록으로 돌아갔을 때, 그리고 댓글 작성 · 삭제 후 목록으로 돌아갔을 때 옛 값(삭제한 글 · 옛 제목 · 새 글 누락 · 옛 댓글 수)이 잠깐이라도 보이지 않는다 (네트워크 느리게 설정해 확인).
- 남의 글 `/posts/{id}/edit`를 주소로 열면 폼이 한 번도 보이지 않고 상세 + 토스트로 간다.

---

## 미결정

없음.

### F1 확인 사항 — `NEXT_PUBLIC_API_BASE_URL`은 빌드 시점 값

- `NEXT_PUBLIC_*`는 `next build` 때 브라우저 번들에 문자열로 박힌다. 실행 중 환경 변수를 바꿔도 반영되지 않는다.
- 현재 전달 구조 (2026-10-05 `BACKEND_PORT=18080 docker compose config`로 확인):
  `docker-compose.yml` frontend `build.args.NEXT_PUBLIC_API_BASE_URL = http://localhost:${BACKEND_PORT:-8080}/api` → `Dockerfile` `ARG` → `ENV` → `npm run build`.
  즉 **이미지를 새로 빌드하면** 바뀐 포트가 번들에 들어간다. `API_BASE_URL`(`http://backend:8080/api`)은 실행 시점 값이라 포트와 무관하다.
- 깨지는 경우:
  1. 기본 포트로 빌드된 이미지가 남은 상태에서 `--build` 없이 포트를 바꿔 기동 → 번들에 이전 포트가 남는다. (README `## 실행`의 `--build` 안내가 이 때문)
  2. 코드에서 백엔드 주소를 하드코딩하거나, 서버 쪽 코드(서버 컴포넌트 · Route Handler)에서 `NEXT_PUBLIC_API_BASE_URL`을 쓴다 (컨테이너 안의 `localhost`는 프론트 자신).
  3. `localhost`가 아닌 주소로 접속 (로컬 실행 범위 밖).
- 규칙: 브라우저 코드는 `NEXT_PUBLIC_API_BASE_URL`만, 서버 코드는 `API_BASE_URL`만 쓴다. 주소를 코드에 직접 적지 않는다.
- 검증 (Integration): `FRONTEND_PORT=13000 BACKEND_PORT=18080 DB_PORT=25432 docker compose up --build` 후 브라우저 개발자 도구에서 API 요청이 `localhost:18080`으로 가고 화면이 동작하는지 확인. F1에서 브라우저 직접 호출을 골랐으므로 **필수**.

### F3 확인 사항 — Tailwind v4 네이티브 모듈과 Docker 빌드

- Tailwind v4는 플랫폼별 네이티브 모듈(`@tailwindcss/oxide` · `lightningcss`)을 쓴다. Windows에서 만든 `package-lock.json`으로 `node:22-alpine`(linux-musl)에서 `npm ci`하면 리눅스용 바이너리가 빠져 빌드가 실패할 수 있다 (npm 선택적 의존성 문제).
- 검증: Tailwind를 설치한 직후 `docker compose build frontend`가 성공하는지 확인한다. 로컬 `npm run dev` 성공만으로 끝내지 않는다.
- 실패하면 lockfile에 linux-musl 바이너리가 포함되도록 조치하고, 그 방법을 이 절에 기록한다.
