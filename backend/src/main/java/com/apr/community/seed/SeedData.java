package com.apr.community.seed;

import java.time.Instant;
import java.util.List;

/** seed/seed.json 의 구조. */
public record SeedData(List<SeedPost> posts,
                       List<SeedComment> comments,
                       List<Long> deletedPostIds,
                       List<Long> deletedCommentIds) {

    public record SeedPost(Long id, String title, String content, int likeCount, String author, Instant createdAt) {}

    public record SeedComment(Long id, Long postId, String content, String author, Instant createdAt) {}
}
