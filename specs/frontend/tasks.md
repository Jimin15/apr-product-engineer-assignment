# Frontend Tasks

`specs/frontend/plan.md`를 실행 순서대로 나눈 작업 목록.
형식과 상태 규칙은 `docs/harness/HARNESS.md` §4를 따른다. **검증 근거가 비어 있으면 DONE으로 바꾸지 않는다.**

- 상태: `TODO` → `IN PROGRESS` → `DONE` (막히면 `BLOCKED` + 사유)
- 참조: spec `F-xx` · 결정 `FD-xx` (`specs/frontend/spec.md`) · plan `Fx` (`specs/frontend/plan.md`) · product `P-xx` · integration `I-xx` / `Cx`
- 작업 브랜치: `main`이 아닌 별도 브랜치 (CLAUDE.md)
- 화면 확인은 브라우저 375px(개발자 도구 모바일 뷰)에서 한다. 백엔드는 `docker compose up -d db backend`로 띄운다.

---

## 0. 준비

- [ ] T-00 작업 환경 확인 — `TODO`
  - 대응 spec: — (전제 조건)
  - 작업: 작업 브랜치 생성, 스타터가 그대로 빌드되는지 확인, **`/dev-user` 현재 화면 스크린샷 저장** (T-61 비교 기준)
  - 검증 방법: `cd frontend && npm ci && npm run build` 성공. `/dev-user` 375px 스크린샷 파일 경로 기록
  - 검증 근거:

- [ ] T-01 의존성 · 설정 추가 — `TODO`
  - 대응 spec: — (plan F2 · F3 · F9 · F10 · "추가 의존성" · F3 확인 사항)
  - 작업:
    - 설치: `@tanstack/react-query` / dev: `tailwindcss` · `@tailwindcss/postcss` · `postcss` · `eslint` · `eslint-config-next` · `@eslint/eslintrc` · `vitest` (목록 밖 패키지 금지)
    - 설정 파일: `postcss.config.mjs`, `eslint.config.mjs`(flat config, Next 규칙), `vitest.config.ts`(`@/` 별칭은 `resolve.alias`)
    - `package.json` scripts: `lint`(`eslint .`), `test`(`vitest run`)
    - `frontend/.env.development`: `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api`
  - 검증 방법:
    - `npm run build` · `npm run lint` 통과
    - `npm test`는 여기서 확인하지 않는다 — `vitest run`은 테스트 파일이 없으면 실패 코드로 끝난다. Vitest 실행은 첫 테스트가 생기는 T-11에서 확인
    - **`docker compose build frontend` 성공** (Tailwind 네이티브 모듈 · alpine, plan F3 확인 사항). 실패하면 조치와 방법을 plan F3 확인 사항에 기록
  - 검증 근거:

## 1. 기반

- [ ] T-10 `(community)` 그룹 레이아웃 · 디자인 토큰 — `TODO`
  - 대응 spec: F-01 · F-02 · F-03 / plan F3 · F11 "구조"
  - 작업:
    - `app/(community)/providers.tsx`(`"use client"`): `useState`로 만든 QueryClient(plan 세부값의 기본값) → QueryClientProvider · ToastProvider 자리
    - `app/(community)/layout.tsx`(서버 컴포넌트): `Providers`로 감싸고 375px 가운데 틀
    - `app/(community)/community.css`: `@import "tailwindcss"` + `@theme` 토큰 (색 `#292828` · `#979ea9` · `#5e5e5e` · `#eaecee` · `#ececec` · `#e3e3e3` · `#f3f4f8` · `#f4f6f9` · `#dadee3` · `#c2102f` · `#ff8da1` 등, 타이포 Bold 20/28 · 18/26 · 16/24 · 15/22 · 14/22 · Medium 16/24 · 14/24 · Regular 15/22 · 14/18 · 13/20 · 12/16 · SemiBold 14/22)
    - 기존 `app/page.tsx`를 `app/(community)/page.tsx`로 옮겨 목록 자리를 만든다 (같은 `/` 경로 충돌 방지)
    - 루트 `app/layout.tsx` · `app/globals.css` · `app/dev-user/`는 건드리지 않는다
  - 검증 방법:
    - `/`가 375px 가운데 틀 안에 그려진다 (넓은 창에서도 375px)
    - 글꼴이 Pretendard로 렌더링된다 (개발자 도구 Computed `font-family`). `@theme`의 `--font-sans`도 Pretendard로 맞춘다
    - `/dev-user`가 T-00 스크린샷과 같다 (Tailwind preflight가 적용되지 않음)
    - `git diff --stat main -- frontend/app/layout.tsx frontend/app/globals.css frontend/app/dev-user` 출력 없음
  - 검증 근거:

- [ ] T-11 API 클라이언트 — `TODO`
  - 대응 spec: F-04 · F-62 / plan F1 · F5 · "구현 규칙 · API 주소" / integration C3
  - 작업: `shared/api/apiFetch` — `NEXT_PUBLIC_API_BASE_URL` + 경로, `credentials: "include"`, JSON 본문, 선택적 `x-user-id` 헤더 옵션, 값이 없으면 분명한 오류. 실패 응답은 `ApiError(status, code, message, errors)`로 던진다. 204는 본문 없이 처리. 토스트에 쓸 문구를 고르는 `errorMessage(error)`(INVALID_INPUT이면 첫 번째 `errors[].message`, 403 · 네트워크 · 500 등은 plan 세부값 문구)를 순수 함수로 둔다
  - 검증 방법: Vitest(`vi.stubGlobal('fetch')`) — 요청 URL · `credentials` · 헤더 옵션 유무, 4xx 본문 → `ApiError` 필드, 204 처리, 기본 URL 없을 때 오류, `errorMessage`가 경우별 문구를 고르는지 (INVALID_INPUT 여러 개 → 첫 번째)
  - 검증 근거:

- [ ] T-12 순수 함수 · 단위 테스트 — `TODO`
  - 대응 spec: F-05 · F-07 · F-12 · F-20 · F-33 · F-42 / plan F5 · F7 · F10 · "구현 규칙"
  - 작업: `shared/lib`
    - `time`: 상대 시간(구간표, 미래 → `방금 전`), 절대 날짜(`formatToParts` 조립, `Asia/Seoul`), 댓글 시각(7일 미만 상대 · 이상 절대)
    - `text`: 코드포인트 세기 · 20 코드포인트 자르기, trim 빈 값 판단
    - `requester`: 쿠키 문자열 → 요청자(trim, 비었으면 `apr_tester`), 본인 판단(대소문자 구분 정확 일치)
    - `avatar`: 작성자 → `avatar-{1,2,3}.png` (코드포인트 합 % 3)
  - 검증 방법: `npm test` 통과. 포함할 경우:
    - 상대 시간 경계: 59초 · 60초 · 59분 59초 · 60분 · 23시간 · 24시간 · 6일 · 7일 · 29일 · 30일 · 364일 · 365일, 미래 시각
    - 절대 날짜: `2026-08-30T15:30:00Z` → `26.08.31` (KST 날짜 경계), 결과에 공백 · 끝 마침표 없음
    - 코드포인트: 시드 6번 제목(🙏🏻 포함) = 19, 21자 → 20자로 잘림, 자른 결과에 UTF-16 반쪽(서로게이트)이 남지 않음(`�` 없음 — 예: 19자 + `😌` + 1자 → 19자 + `😌`)
    - 알려진 한계(테스트로 고정): 여러 코드포인트 이모지는 경계에서 잘릴 수 있다 — 19자 + `🙏🏻` → 19자 + `🙏` (피부색만 빠짐)
    - 요청자: 쿠키 없음 · 빈 값 · 공백뿐 → `apr_tester`, 앞뒤 공백 제거, `Jelin` ≠ `jelin`
    - 아바타: 같은 입력 → 같은 결과, 결과는 1~3
  - 검증 근거:

- [ ] T-13 공통 UI 컴포넌트 — `TODO`
  - 대응 spec: F-08 · F-16 · F-50 · F-51 · F-52 · F-53 · F-60 · F-61 · F-63 / plan F4 · "구현 규칙 · 뒤로 가기"
  - 작업: `shared/ui` — `TopBar`(뒤로 가기 · 오른쪽 슬롯), `Dialog`(F-51 틀, 버튼 1~2개, 바깥 클릭 허용 여부 옵션), `BottomSheet`(F-50), `ToastProvider` / `useToast`(2초, 교체, 높이 40 · 가로 문구에 맞춤), `LoadError`(F-63), `NotFoundDialog`(F-53), `goBack`(기록 없으면 `/`), `shared/hooks/useInView`
  - 검증 방법: 이후 화면 task(T-20~T-52)에서 각 컴포넌트가 spec 수치대로 표시되는지 확인 — 여기서는 `npm run lint` · `npm run build` 통과
  - 검증 근거:

- [ ] T-14 아이콘 추출 — `TODO`
  - 대응 spec: F-02 / plan F8
  - 작업: `design/메디큐브톡.fig` 벡터 경로 → `shared/ui/icons/` SVG 컴포넌트. 연필(작성 버튼, 45°) · 휴지통 · 하트 외곽선/채움 · 댓글 · 뒤로 화살표 · 아래 화살표(더보기) · 세로 점 3개 · 전송 · 수정(바텀시트)
  - 검증 방법: 아이콘을 모아 그린 화면 스크린샷을 사용자에게 보여 피그마와 모양 · 방향 · 색을 대조받는다 (확인 후 임시 화면은 삭제)
  - 검증 근거:

## 2. 목록

- [ ] T-20 게시글 목록 · 무한 스크롤 — `TODO`
  - 대응 spec: F-06 · F-07 · F-10 · F-11 · F-12 · F-15 · F-16 · F-61 · F-63 / plan F2 "쿼리 키"
  - 작업: `features/posts` api · `['posts','list']` infinite(size 20, `nextCursor`) · `PostList` · `PostListItem`(아바타 18 · 작성자 · 제목 한 줄 말줄임 · 미리보기 한 줄 · 좋아요/댓글 아이콘 + 수 · 상대 시간 · 구분선) · 끝 sentinel · `불러오는 중…` · `LoadError`
  - 검증 방법:
    - 첫 요청 `size=20`, 끝까지 내리면 커서로 다음 19개, `nextCursor: null` 이후 요청 없음 (개발자 도구 Network) — 합계 39개, 삭제 글(9 · 30 · 31) 없음
    - 최신순(`createdAt` 내림차순, 같으면 `id` 내림차순)으로 표시된다
    - 항목 값이 API 응답과 같고 숫자에 천 단위 구분, 상대 시간 표기
    - 첫 로딩 · 다음 묶음을 받는 동안 목록 아래 `불러오는 중…`
    - 백엔드를 멈춘 상태에서 첫 로딩 → `불러오지 못했습니다.` + `다시 시도`, 백엔드 재기동 후 `다시 시도` → 목록 표시
    - 첫 묶음을 받은 뒤 백엔드를 멈추고 끝까지 내림 → 목록 아래 같은 안내, 재기동 후 `다시 시도` → 다음 묶음 이어서 표시
    - 피그마 목록 화면과 수치(높이 118 · 글꼴 · 색 · 상단 바 테두리) 대조
  - 검증 근거:

- [ ] T-21 작성 버튼 · 항목 이동 — `TODO`
  - 대응 spec: F-13 · F-14
  - 작업: 오른쪽 아래 고정 작성 버튼(지름 50, `#c2102f`, 연필) → `/posts/new`, 항목 클릭 → `/posts/{id}`
  - 검증 방법: 넓은 창에서도 버튼이 375px 틀 오른쪽 아래에 있다. 각 이동 확인
  - 검증 근거:

## 3. 상세

- [ ] T-30 게시글 상세 · 없는 글 — `TODO`
  - 대응 spec: F-08 · F-20 · F-25 · F-53 · F-63 / plan "구현 규칙 · 삭제된 글 감지 · 뒤로 가기"
  - 작업: `['posts','detail',id]` · `PostDetail`(제목 · 아바타 36 · 작성자 · `YY.MM.DD` · 내용 줄바꿈 · 좋아요 자리 · 8px 띠) · 첫 로딩 표시 · `LoadError` · 404 → `NotFoundDialog`
  - 검증 방법:
    - 1번 글 값이 API와 같다, 내용 줄바꿈 유지
    - `/posts/9`(삭제) · `/posts/99999`(없음) → 상단 바만 남은 빈 화면 위에 다이얼로그, 바깥 클릭으로 안 닫힘, `목록으로` → `/`
    - 목록에서 들어와 뒤로 가기 → 목록, 주소로 직접 연 상세에서 뒤로 가기 → `/`
    - 백엔드를 멈춘 상태에서 상세 열기 → `불러오지 못했습니다.` + `다시 시도`, 재기동 후 `다시 시도` → 상세 표시
  - 검증 근거:

- [ ] T-31 좋아요 — `TODO`
  - 대응 spec: F-21 · FD-4 / plan "구현 규칙 · 좋아요" · F10
  - 작업: `features/likes` — 수렴 로직(순수 함수로 분리) · `useLikeToggle` · `LikeButton`(외곽선 `#979ea9` / 채움 `#ff8da1`, `좋아요 N`). 상세 · 목록 캐시 반영, 시작 시 `cancelQueries`
  - 검증 방법:
    - Vitest: 한 번 · 두 번 · 세 번 연타 시 요청 수와 최종 상태, 진행 중 추가 클릭, 실패 시 서버 값으로 복구, 표시 숫자 식(`confirmed.likeCount + (desired === confirmed.isLiked ? 0 : desired ? 1 : -1)`)의 네 경우
    - 브라우저: 누르는 즉시 아이콘 · 숫자 변경, 빠른 3회 연타 후 요청 2회 이하 · 최종 상태 = 마지막 누른 상태, 목록으로 돌아가도 같은 숫자
    - 요청 실패(백엔드 중지) → 서버 상태로 되돌림 + 토스트 `좋아요 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.` (긴 문구도 토스트 안에 다 보이고 375px를 넘지 않음)
  - 검증 근거:

- [ ] T-32 본인 글 메뉴 · 게시글 삭제 — `TODO`
  - 대응 spec: F-05 · F-22 · F-24 · F-50 · F-51 · F-09 · P-25 / plan "캐시 갱신"
  - 작업: 본인 글이면 세로 점 3개 버튼 → `PostMenuSheet`(수정하기 · 삭제하기 · 취소) → 삭제 `Dialog` → 삭제, 상세 캐시 제거 · 목록 캐시에서 즉시 제거 · 무효화 → `replace('/')` + `게시글이 삭제되었습니다.`
  - 검증 방법:
    - `/dev-user`로 고른 유저의 글에만 메뉴가 보이고 남의 글엔 없다
    - 삭제 후 목록에 그 글이 **잠깐도** 보이지 않는다 (네트워크 느리게), 토스트 표시, DB 행은 남는다(백엔드 검증과 동일 — `psql`로 `deleted_at` 확인)
    - `취소` · 바깥 클릭 → 닫힘, `예` 연타 시 요청 1회
    - 본인 글 상세를 연 채 다른 탭에서 `/dev-user`로 유저를 바꾼 뒤 삭제 → 서버 403 → 토스트 `권한이 없습니다.`
  - 검증 근거:

- [ ] T-33 보는 사이 삭제된 글 — `TODO`
  - 대응 spec: F-25 · F-53 / plan "구현 규칙 · 삭제된 글 감지"
  - 작업: 댓글 목록 · 댓글 작성 · 댓글 삭제 · 좋아요 · 수정 저장 · 게시글 삭제의 `POST_DELETED` · `POST_NOT_FOUND` 오류를 `NotFoundDialog`로 연결
  - 검증 방법: 본인 글 상세(또는 수정 화면)를 연 상태에서 `curl -X DELETE -H "x-user-id: <작성자>" …/api/posts/{id}`로 지운 뒤, 같은 화면에서 좋아요 · 댓글 전송 · 댓글 더보기 · 댓글 삭제 · 수정 저장 · 메뉴 삭제를 각각 시도 → 모두 `NotFoundDialog` → `목록으로`. **다이얼로그 뒤에 삭제된 글의 제목 · 내용 · 댓글 · 입력 바(수정 화면이면 폼)가 보이지 않는다** (상단 바만 남음)
  - 검증 근거:

## 4. 댓글

- [ ] T-40 댓글 목록 · 더보기 · 무한 스크롤 — `TODO`
  - 대응 spec: F-30 · F-31 · F-32 · F-33 · F-36 · F-63 / plan "쿼리 키"
  - 작업: `features/comments` api · `['comments', postId]` infinite(첫 `size=2`, 이후 20) · `CommentSection`(헤더 `댓글 (N)` · 구분선) · `CommentItem`(아바타 30 · 작성자 · 시각 · 내용 · 본인이면 내용 아래 `삭제`) · 더보기 버튼 · sentinel · 빈 상태 · `LoadError`
  - 검증 방법:
    - 2번 글(댓글 1,411): 처음 2개 + `댓글 더보기`, 누르면 다음 20개, 이후 스크롤로 자동 로딩, 버튼 다시 안 보임, 헤더 `(1,411)`
    - 댓글 2개 이하인 글: 버튼 없음. 0개인 글: `아직 댓글이 없습니다.`
    - 최신순, 7일 미만 상대 · 이상 `YY.MM.DD`
    - 댓글 첫 로딩 실패 · 다음 묶음 실패(백엔드 중지) → 댓글 영역에 `불러오지 못했습니다.` + `다시 시도`, 재기동 후 복구
  - 검증 근거:

- [ ] T-41 댓글 작성 — `TODO`
  - 대응 spec: F-34 · FD-1 · F-09 / plan "캐시 갱신" · 세부값(입력창 4줄)
  - 작업: `CommentInput` — 하단 고정 바, `textarea`(엔터 줄바꿈, 최대 4줄), trim 값이 있을 때만 전송 버튼(입력창 안쪽 오른쪽 28px), 전송 → 캐시 `commentCount` +1 · 무효화 → 입력창 비우고 `blur()`
  - 검증 방법:
    - 빈 값 · 공백뿐 → 버튼 없음, 글자 입력 → 버튼 나타남(레이아웃 밀림 없음)
    - 전송 후 새 댓글이 맨 위, 헤더 수 +1, 목록으로 돌아가면 `commentCount` +1 (옛 숫자가 잠깐도 안 보임)
    - 전송 버튼 연타 → 댓글 1개
    - 5줄 이상 입력 → 입력창은 4줄 높이에서 멈추고 안에서 스크롤
    - 전송 실패(백엔드 중지) → 토스트 `잠시 후 다시 시도해 주세요.`, 입력 내용 유지
  - 검증 근거:

- [ ] T-42 댓글 삭제 — `TODO`
  - 대응 spec: F-35 · F-51 · P-36 · FD-11 / plan "캐시 갱신"
  - 작업: 본인 댓글 `삭제` → `Dialog`(댓글 문구) → 삭제, 댓글 캐시에서 즉시 제거 · `total` −1 · `commentCount` −1 · 무효화 · 토스트 `댓글이 삭제되었습니다.`, 상세 유지
  - 검증 방법:
    - 남의 댓글엔 `삭제` 없음. 삭제 후 목록에서 사라지고 헤더 −1, 목록 화면 `commentCount` −1, 토스트, 상세에 머문다
    - `예` 연타 시 요청 1회
    - 마지막 댓글을 지우면 `아직 댓글이 없습니다.`가 다시 보인다
    - 처음 2개만 보이는 상태에서 하나를 지우면, 재조회가 끝난 뒤 다음 댓글로 채워진다 (그 사이 잠깐 1개만 보일 수 있음 — 즉시 보충은 요구사항이 아님)
    - 전체 3개 → 2개가 되면 `댓글 더보기`가 바로 사라진다 (`total` 즉시 −1)
  - 검증 근거:

## 5. 작성 · 수정

- [ ] T-50 게시글 작성 — `TODO`
  - 대응 spec: F-40 · F-41 · F-42 · F-43 · F-45 · P-47 · FD-12 / plan "구현 규칙 · 제목 글자 수 · 중복 제출"
  - 작업: `/posts/new` · `PostForm`(작성 모드) — `글작성` 라벨, 제목(placeholder · 밑줄 색 · `N / 20` · 20에서 `#c2102f`) · 내용(placeholder), 완료 버튼 활성 조건, 조합 끝난 뒤 자르기, 저장 → 목록 캐시 맨 앞에 넣기 · 무효화 → `push('/')` + `게시글이 등록되었습니다.`
  - 검증 방법:
    - 제목 · 내용 중 하나라도 비었거나 공백뿐이면 버튼 비활성
    - 한글로 20자 입력 시 21번째가 입력되지 않고 조합 중 글자가 깨지지 않는다, 붙여넣기로 넘쳐도 20자, `20 / 20`이 빨간색, 🙏🏻는 2로 센다
    - 저장 후 목록 맨 위에 새 글이 바로 보이고 토스트, 완료 버튼 연타 → 글 1개
    - 2페이지 이상 불러온 상태에서 작성 → 목록을 끝까지 내려 빠지거나 겹치는 글이 없다 (총 개수 = 이전 + 1, 같은 id 중복 없음)
    - 서버 검증 오류는 화면이 같은 규칙으로 먼저 막아 정상 조작으로 만들 수 없다 → Chrome 개발자 도구 응답 덮어쓰기(Override content)로 `POST /posts`가 400 `INVALID_INPUT`(errors 2개)을 주게 하고, 첫 번째 문구가 토스트로 뜨는지 확인 (문구 선택 자체는 T-11 Vitest)
  - 검증 근거:

- [ ] T-51 게시글 수정 · 남의 글 진입 — `TODO`
  - 대응 spec: F-23 · F-44 · F-47 · F-09 · FD-8 · FD-14 / plan "구현 규칙 · 남의 글 수정 화면"
  - 작업: `/posts/[id]/edit` — 작성자 확인(확인 중 로딩), 본인 글이면 `PostForm`(수정 모드: `글수정` · `수정 완료` · 기존 값), 남의 글이면 폼 없이 `replace('/posts/{id}')` + `다른 사람의 글은 수정할 수 없습니다.`, 404면 `NotFoundDialog`. 저장 → 상세 캐시 교체 · 목록 항목 교체 · 무효화 → `replace('/posts/{id}')` + `게시글이 수정되었습니다.`
  - 검증 방법:
    - 메뉴 → 수정하기 → 기존 제목 · 내용이 채워져 있다, 저장 후 상세에 반영 · 토스트, 뒤로 가기에 수정 화면이 나오지 않음, 목록에 옛 제목이 잠깐도 안 보임
    - 남의 글 수정 주소를 직접 열면 폼이 한 번도 보이지 않는다
    - 수정에서도 작성과 같은 제약: 제목 · 내용을 비우면 `수정 완료` 비활성, 제목 20자 제한 · `N / 20` (과제: 제약은 작성과 같음)
    - `수정 완료` 연타 → 요청 1회
  - 검증 근거:

- [ ] T-52 작성 · 수정 중 이탈 확인 — `TODO`
  - 대응 spec: F-46 · F-52 · P-46 · FD-10
  - 작업: `PostForm`이 "입력 있음(작성) / 처음 값과 다름(수정)" 계산, 뒤로 가기 화살표에서만 `Dialog`(`나가기` · `계속 작성` / `나가기`)
  - 검증 방법:
    - 작성: 빈 상태 → 바로 나감, 공백만 → 바로 나감, 글자 입력 → 다이얼로그, `계속 작성` → 머묾(입력 유지), `나가기` → 이전 화면
    - 수정: 그대로 → 바로 나감, 고친 뒤 → 다이얼로그
    - 저장 성공 이동 시 다이얼로그 없음
  - 검증 근거:

## 6. 통합 검증

- [ ] T-60 lint · 테스트 · 빌드 — `TODO`
  - 대응 spec: — (HARNESS §6, plan F9 · F10)
  - 검증 방법: `npm run lint` · `npm test` · `npm run build` 모두 통과, 경고 목록 기록
  - 검증 근거:

- [ ] T-61 plan "Verification 추가 항목" — `TODO`
  - 대응 spec: F-05 · F-09 · plan "Verification 추가 항목"
  - 검증 방법:
    - `/dev-user` 화면이 T-00 스크린샷과 같다
    - `/dev-user`로 유저를 바꾸면 메뉴 · 댓글 `삭제` · `isLiked`가 그 유저 기준으로 바뀐다 (쿠키 전송 포함)
    - 상세 → 뒤로 가기 시 목록 스크롤 위치 유지
    - 모든 화면에서 브라우저 콘솔에 hydration 오류 · React 경고가 없다 (plan "구현 규칙 · 서버에서 한 번 그려지는 점")
    - 네트워크를 느리게 한 상태에서 작성 · 수정 · 삭제 · 댓글 작성 · 삭제 후 목록에 옛 값이 잠깐도 보이지 않는다
  - 검증 근거:

- [ ] T-62 디자인 대조 — `TODO`
  - 대응 spec: F-02 및 각 화면 수치
  - 검증 방법: 375px에서 목록 · 작성 완료 토스트 · 상세(본인 · 남 · 좋아요 누름 · 댓글 1개 · 0개) · 메뉴 바텀시트 · 글 삭제 · 댓글 삭제 다이얼로그 · 작성(빈 · 입력) 화면을 피그마 프레임과 나란히 비교, 차이 목록 기록
  - 검증 근거:

- [ ] T-63 Docker Compose 기동 (기본 포트) — `TODO`
  - 대응 spec: I-10 / plan F1 확인 사항
  - 검증 방법: `docker compose up --build` → frontend `healthy`, `http://localhost:3000`에서 목록 · 상세 · 좋아요 · 댓글 · 작성이 동작, API 요청이 `localhost:8080`으로 감
  - 검증 근거: (포트 변경 기동은 Integration 단계에서 확인 — plan F1 확인 사항)

- [ ] T-64 보존 대상 확인 — `TODO`
  - 대응 spec: CLAUDE.md "반드시 유지"
  - 검증 방법: `git diff --stat main -- frontend/app/dev-user frontend/app/layout.tsx frontend/app/globals.css frontend/Dockerfile docker-compose.yml` 출력 없음. README는 `## 실행` 절만 `main`과 같은지 확인 (설명 절은 채워야 하므로 비교 대상 아님)
  - 검증 근거:

## 7. 다음 단계로 넘긴 것

프론트 task에서 다루지 않고 Integration 단계에서 확인한다. **`specs/integration/tasks.md`가 아직 없으므로**, Integration tasks를 쓸 때 아래를 빠뜨리지 않는다.

- 포트를 바꿔 기동: `FRONTEND_PORT=13000 BACKEND_PORT=18080 DB_PORT=25432 docker compose up --build` → 브라우저 API 요청이 `localhost:18080`으로 가고 화면 동작 (plan F1 확인 사항, I-20)
- 빈 DB 첫 기동(`docker compose down -v` 후 `up --build`)에서 프론트까지 동작
- README 작성: 기술 선택 · 상태 설계 · 구조 · 데이터 적재 · 테스트 · 개선한 점 (plan 기술 결정 · "상태 설계" · "구조", spec FD-1 · FD-5 · FD-6 · FD-10~15, product D8~D11)
