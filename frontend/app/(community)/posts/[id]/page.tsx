import { PostDetailScreen } from "@/features/posts/PostDetail";

export default async function PostPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <PostDetailScreen id={Number(id)} />;
}
