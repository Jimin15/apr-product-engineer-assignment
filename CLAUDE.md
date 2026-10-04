# CLAUDE.md

메디큐브톡 커뮤니티 과제 — 에이피알 Product Engineer

`ASSIGNMENT.md`가 최우선 요구사항이다. 문서나 구현과 충돌하면 임의로 판단하지 말고 사용자에게 알린다.

## 작업 문서

작업 전 `docs/harness/HARNESS.md`를 확인하고, 현재 영역의 문서를 읽는다.

- 제품: `specs/product-spec.md`
- Backend: `specs/backend/{spec,plan,tasks}.md`
- Frontend: `specs/frontend/{spec,plan,tasks}.md`
- Integration: `specs/integration/{spec,tasks}.md`

## 규칙

- Spec은 WHAT, Plan은 HOW로 구분한다.
- 중요한 기술 선택은 임의로 결정하지 않는다.
- 과제 · 디자인에 없거나 불명확해서 임의로 정해야 하는 것은 선택지를 제시하고 사용자가 정한다.
- 구현 전 관련 코드와 문서를 먼저 확인한다.
- 과제 규모를 넘는 불필요한 추상화나 인프라를 추가하지 않는다.
- 검증되지 않은 작업은 완료로 처리하지 않는다.
- Backend → Frontend → Integration 순서로 진행한다.

## 반드시 유지

- `frontend/app/dev-user/` 수정 금지
- `GET /health` 경로와 200 응답 유지
- 기존 CORS 설정 유지
- `seed/seed.json`의 기존 ID 유지
- `BACKEND_PORT`, `FRONTEND_PORT`, `DB_PORT` 동작 유지
- `README.md`의 `## 실행` 절 유지 (실행 방법이 바뀐 경우에만 수정)

## Git
- `main`에 직접 커밋하지 않는다.
- 별도 브랜치 → PR → `main` 병합
- commit, push, merge는 사용자가 요청할 때만 수행한다.