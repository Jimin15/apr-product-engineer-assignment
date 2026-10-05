import "./community.css";
import { Providers } from "./providers";

// 375px 고정 · 가운데 정렬 (F-01 · FD-9). Tailwind 와 Provider 는 이 그룹에만 적용한다.
export default function CommunityLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <Providers>
      <div className="relative mx-auto min-h-dvh w-[375px] bg-white text-ink">
        {children}
      </div>
    </Providers>
  );
}
