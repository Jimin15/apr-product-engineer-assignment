# HARNESS — 작업 절차

작업은 다음 순서로 진행한다.

Inspect → Spec → Plan → Tasks → Implementation → Verification → Review

영역은 Backend → Frontend → Integration 순서로 진행한다.
한 영역의 Review가 끝난 뒤 다음 영역의 Plan으로 넘어간다.

## 1. Inspect

현재 저장소와 과제 요구사항을 확인한다.

- 확인 대상: `ASSIGNMENT.md`, `README.md`, 관련 코드, `seed/seed.json`, 필요한 경우 디자인
- 산출물: 요구사항 요약, 현재 구현 상태, 보존 대상, 구현에 영향을 주는 제약사항
- 완료 조건:
  - 이후 작업에 필요한 내용이 실제 파일과 코드를 근거로 확인되어 있다.
  - 확인하지 않은 내용을 가정하지 않는다.

## 2. Spec (WHAT)

구현해야 할 동작을 정의한다.

- 산출물: `specs/product-spec.md`, `specs/{backend,frontend,integration}/spec.md`
- 규칙:
  - WHAT만 작성한다.
  - 구현 방법과 기술 선택은 작성하지 않는다.
  - 과제에 없거나 모호한 동작은 선택지를 제시해 사용자가 결정하고, 결정 내용을 spec의 결정 기록에 남긴다.
- 완료 조건:
  - 과제 요구사항이 빠짐없이 반영되어 있다.
  - 모든 요구사항이 검증 가능한 문장이다.
  - 미결 사항이 명시되어 있다.

## 3. Plan (HOW)

Spec을 어떻게 구현할지 결정한다.

- 산출물: `specs/{backend,frontend}/plan.md`
- 중요한 기술 선택은 다음을 비교한 뒤 사용자 결정 후 확정한다.

  ```text
  결정할 내용 / 선택지 / 각 선택지의 장점 / 포기하는 것 / 이 과제에서 고려할 점
  ```

- 확정된 선택은 plan.md에 "무엇을 골랐고, 왜, 무엇을 포기했는지"로 기록한다.
- 완료 조건:
  - 모든 Spec 요구사항에 구현 방법이 대응된다.
  - 미확정 기술 선택이 없다.

## 4. Tasks

Plan을 실행 가능한 단위로 나눈다.

- 산출물: `specs/{backend,frontend,integration}/tasks.md`
- 항목 형식:

  ```markdown
  - [ ] T-01 작업 이름
    - 대응 spec: (요구사항 번호)
    - 검증 방법: (실행할 명령 또는 확인 절차)
    - 검증 근거: (비어 있으면 DONE 불가)
  ```

- 상태: `TODO` → `IN PROGRESS` → `DONE` (막히면 `BLOCKED` + 사유)
- 완료 조건: 각 task가 하나 이상의 spec 요구사항에 연결되고 검증 방법이 있다.

## 5. Implementation

- task 하나씩 진행한다. 시작 전에 관련 코드를 다시 읽는다.
- plan에 없는 기술 · 라이브러리를 추가해야 하면 멈추고 Plan 단계로 돌아간다.
- 보존 대상(`CLAUDE.md` 참고)을 건드리지 않는다.

## 6. Verification

실제 실행 결과로 검증한다.

- 확인 대상:
  - Backend test / build
  - Frontend lint / build (lint 도구는 Frontend Plan에서 결정)
  - 주요 API
  - `docker compose up --build`
  - `GET /health` 200
  - 포트 환경 변수 (`BACKEND_PORT` · `FRONTEND_PORT` · `DB_PORT`)
    - 포트를 바꿨을 때 화면에서 백엔드 호출이 실제로 동작하는지
  - 재기동 시 시드 중복 여부
  - 빈 DB에서 첫 기동 (`docker compose down -v` 후 `up`)
  - 시드 적재 결과 — DB 저장 행(게시글 42 · 댓글 2,076, 삭제 표시 3 · 81)과 API 활성 데이터(게시글 39 · 댓글 1,926)를 구분해 확인, `commentCount`
  - 삭제 후 DB 행이 남아 있는지 (API 응답이 아니라 DB에서 직접 확인)
  - 화면 동작 (375px, 디자인 반영, 무한 스크롤 · 더보기 · 좋아요 토글 · 작성 토스트 · 본인 글 메뉴)
  - `/dev-user`로 유저를 바꿨을 때 요청자와 본인 여부가 따라 바뀌는지 (쿠키 전송 포함)
  - 보존 대상이 바뀌지 않았는지 (`git diff`)
- 실행한 명령과 결과 요약을 task의 "검증 근거"에 적는다.
- 실패하면 실패 그대로 기록한다. 검증하지 못한 항목은 DONE으로 바꾸지 않는다.

## 7. Review

- spec 요구사항 하나하나를 검증 근거와 대조한다.
- 과도한 추상화 · 불필요한 코드 · 보존 대상 변경 여부를 확인한다.
- README에 쓸 내용(기술 선택 · 상태 설계 · 구조 · 데이터 적재 · 테스트)이 plan에 근거와 함께 있는지 확인한다.
- 완료 조건: 미충족 요구사항이 없거나, 남은 것이 명시적으로 기록되어 있다.
