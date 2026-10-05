package com.apr.community.post;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeCountRepository extends JpaRepository<PostLikeCount, Long> {
}
