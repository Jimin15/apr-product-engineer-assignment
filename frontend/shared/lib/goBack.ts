type Router = {
  back: () => void;
  push: (href: string) => void;
  replace: (href: string) => void;
};

/*
 * 앱 안에서 몇 번째 화면인지(0 = 앱에 들어온 첫 화면)를 history state 에 적어 둔다.
 * `history.length` 는 앱에 들어오기 전 기록(새 탭 페이지 · 다른 사이트)까지 세므로
 * "앱 안에서 이동해 왔는지" 판단에 쓸 수 없다.
 */
const INDEX_KEY = "__appIndex";
let index = 0;
let lastPath: string | null = null;
let replacing = false;

function stamp(next: number) {
  index = next;
  window.history.replaceState(
    { ...window.history.state, [INDEX_KEY]: next },
    "",
  );
}

/** 화면 주소가 바뀔 때마다 부른다 (`HistoryTracker`). */
export function trackPath(path: string): void {
  if (path === lastPath) return;
  const first = lastPath === null;
  lastPath = path;

  const saved = window.history.state?.[INDEX_KEY];
  if (typeof saved === "number") {
    // 뒤로 · 앞으로 가기 또는 새로고침: 적어 둔 순번을 그대로 쓴다
    index = saved;
  } else {
    stamp(first ? 0 : replacing ? index : index + 1);
  }
  replacing = false;
}

/** 현재 화면을 바꾼다 (기록을 쌓지 않음). 순번이 늘지 않도록 표시해 둔다. */
export function replaceRoute(router: Router, href: string): void {
  replacing = true;
  router.replace(href);
}

/** 앱 안에서 이동해 온 기록이 있는지 */
export function canGoBackInApp(): boolean {
  return index > 0;
}

/**
 * 상단 왼쪽 화살표 (F-08). 앱 안에서 이동해 온 기록이 있으면 뒤로, 주소로 직접 들어와
 * 기록이 없으면 목록으로 간다.
 */
export function goBack(router: Router, canGoBack: boolean = index > 0): void {
  if (canGoBack) router.back();
  else router.push("/");
}
