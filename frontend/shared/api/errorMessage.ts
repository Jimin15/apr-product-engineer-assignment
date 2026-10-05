import { ApiError } from "./ApiError";

export const GENERIC_ERROR_MESSAGE = "잠시 후 다시 시도해 주세요.";

/** 토스트에 보여줄 문구를 고른다 (plan "세부값 · 오류 문구", F-45). */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === "INVALID_INPUT") {
      return error.errors?.[0]?.message ?? error.message;
    }
    if (error.code === "NOT_OWNER") return "권한이 없습니다.";
  }
  return GENERIC_ERROR_MESSAGE;
}
