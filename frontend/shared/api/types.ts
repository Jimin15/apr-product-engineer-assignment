// API 계약: specs/integration/spec.md §1.2
export type Cursor = { createdAt: string; id: number };

export type Page<T> = {
  items: T[];
  total: number;
  nextCursor: Cursor | null;
};

export type Post = {
  id: number;
  title: string;
  content: string;
  likeCount: number;
  commentCount: number;
  isLiked: boolean;
  author: string;
  createdAt: string;
};

export type Comment = {
  id: number;
  postId: number;
  content: string;
  author: string;
  createdAt: string;
};

export type LikeResult = { isLiked: boolean; likeCount: number };
