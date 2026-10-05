import Image from "next/image";
import { avatarSrc } from "@/shared/lib/avatar";

/** 작성자별 고정 아바타 (F-07) */
export function Avatar({ author, size }: { author: string; size: number }) {
  return (
    <Image
      src={avatarSrc(author)}
      alt=""
      width={size}
      height={size}
      className="shrink-0 rounded-full"
      style={{ width: size, height: size }}
    />
  );
}
