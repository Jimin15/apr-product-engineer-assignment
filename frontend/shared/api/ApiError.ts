// 오류 응답 계약: specs/integration/spec.md §1.5 (C3)
export type FieldError = { field: string; message: string };

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly errors?: FieldError[];

  constructor(
    status: number,
    code: string,
    message: string,
    errors?: FieldError[],
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.errors = errors;
  }
}

/** 삭제되었거나 존재하지 않는 게시글 (F-25) */
export function isPostGone(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    (error.code === "POST_DELETED" || error.code === "POST_NOT_FOUND")
  );
}

/** 삭제되었거나 존재하지 않는 댓글 */
export function isCommentGone(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    (error.code === "COMMENT_DELETED" || error.code === "COMMENT_NOT_FOUND")
  );
}
