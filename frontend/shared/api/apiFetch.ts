import { ApiError, type FieldError } from "./ApiError";

type Options = {
  method?: "GET" | "POST" | "PATCH" | "DELETE";
  body?: unknown;
  /** 기본은 쿠키만 보낸다 (plan F5). 필요할 때만 헤더로도 보낼 수 있다. */
  userId?: string;
  signal?: AbortSignal;
};

type ErrorBody = { code?: string; message?: string; errors?: FieldError[] };

/**
 * 브라우저에서 백엔드를 직접 호출한다 (plan F1).
 * `credentials: "include"` 로 `/dev-user` 가 설정한 `x-user-id` 쿠키를 함께 보낸다.
 */
export async function apiFetch<T>(
  path: string,
  options: Options = {},
): Promise<T> {
  const base = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (!base) {
    throw new Error(
      "NEXT_PUBLIC_API_BASE_URL 이 설정되지 않았습니다. (.env.development 또는 빌드 인자)",
    );
  }

  const headers: Record<string, string> = {};
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  if (options.userId !== undefined) headers["x-user-id"] = options.userId;

  const res = await fetch(`${base}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    credentials: "include",
    signal: options.signal,
  });

  if (!res.ok) {
    let payload: ErrorBody = {};
    try {
      payload = (await res.json()) as ErrorBody;
    } catch {
      // 본문이 JSON 이 아니면 상태 코드만으로 처리한다.
    }
    throw new ApiError(
      res.status,
      payload.code ?? "UNKNOWN",
      payload.message ?? `요청에 실패했습니다. (${res.status})`,
      payload.errors,
    );
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}
