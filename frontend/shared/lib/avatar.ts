/** 작성자 이름으로 아바타 3종 중 하나를 고정한다 (F-07 · FD-7). */
export function avatarIndex(author: string): 1 | 2 | 3 {
  let sum = 0;
  for (const ch of author) sum += ch.codePointAt(0) ?? 0;
  return ((sum % 3) + 1) as 1 | 2 | 3;
}

export function avatarSrc(author: string): string {
  return `/avatar-${avatarIndex(author)}.png`;
}
