import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "./ApiError";
import { apiFetch } from "./apiFetch";
import { GENERIC_ERROR_MESSAGE, errorMessage } from "./errorMessage";

const BASE = "http://localhost:8080/api";

function mockFetch(status: number, body?: unknown) {
  const fetchMock = vi.fn(async () => ({
    ok: status >= 200 && status < 300,
    status,
    json: async () => {
      if (body === undefined) throw new Error("no body");
      return body;
    },
  }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("apiFetch (plan F1 · F5 · C3)", () => {
  beforeEach(() => vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", BASE));
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
  });

  it("기본 주소 + 경로, credentials: include, 기본은 헤더 없음", async () => {
    const fetchMock = mockFetch(200, { items: [] });
    await apiFetch("/posts?size=20");
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe(`${BASE}/posts?size=20`);
    expect(init.credentials).toBe("include");
    expect(init.method).toBe("GET");
    expect(init.headers).toEqual({});
    expect(init.body).toBeUndefined();
  });

  it("JSON 본문과 선택적 x-user-id 헤더", async () => {
    const fetchMock = mockFetch(201, { id: 43 });
    await apiFetch("/posts", {
      method: "POST",
      body: { title: "t", content: "c" },
      userId: "Jelin",
    });
    const [, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(init.headers).toEqual({
      "Content-Type": "application/json",
      "x-user-id": "Jelin",
    });
    expect(init.body).toBe(JSON.stringify({ title: "t", content: "c" }));
  });

  it("4xx 본문 → ApiError(status · code · message · errors)", async () => {
    mockFetch(400, {
      code: "INVALID_INPUT",
      message: "입력값이 올바르지 않습니다.",
      errors: [{ field: "title", message: "제목을 입력해 주세요." }],
    });
    const error = await apiFetch("/posts", { method: "POST", body: {} }).catch(
      (e: unknown) => e,
    );
    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(400);
    expect(apiError.code).toBe("INVALID_INPUT");
    expect(apiError.errors?.[0]?.field).toBe("title");
  });

  it("본문 없는 오류도 ApiError", async () => {
    mockFetch(502);
    const error = await apiFetch("/posts").catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).code).toBe("UNKNOWN");
  });

  it("204 는 본문 없이 끝난다", async () => {
    mockFetch(204);
    await expect(apiFetch("/posts/1", { method: "DELETE" })).resolves.toBeUndefined();
  });

  it("기본 주소가 없으면 분명한 오류", async () => {
    vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "");
    await expect(apiFetch("/posts")).rejects.toThrow(/NEXT_PUBLIC_API_BASE_URL/);
  });
});

describe("errorMessage (F-45 · plan 세부값)", () => {
  it("INVALID_INPUT 은 첫 번째 필드 문구", () => {
    const error = new ApiError(400, "INVALID_INPUT", "입력값이 올바르지 않습니다.", [
      { field: "content", message: "내용을 입력해 주세요." },
      { field: "title", message: "제목을 입력해 주세요." },
    ]);
    expect(errorMessage(error)).toBe("내용을 입력해 주세요.");
  });

  it("NOT_OWNER 는 권한 문구", () => {
    expect(errorMessage(new ApiError(403, "NOT_OWNER", "x"))).toBe("권한이 없습니다.");
  });

  it("네트워크 · 500 · 알 수 없는 오류는 공통 문구", () => {
    expect(errorMessage(new TypeError("Failed to fetch"))).toBe(GENERIC_ERROR_MESSAGE);
    expect(errorMessage(new ApiError(500, "INTERNAL_ERROR", "x"))).toBe(
      GENERIC_ERROR_MESSAGE,
    );
    expect(errorMessage(undefined)).toBe(GENERIC_ERROR_MESSAGE);
  });
});
