package com.kaede.portfoliobackend.repository;

import com.kaede.portfoliobackend.entity.ArticleLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {
    // 查找某人是否对某篇文章点过赞
    Optional<ArticleLike> findByArticleIdAndUsername(Long articleId, String username);

    // 获取某篇文章的所有点赞记录（按时间倒序）
    List<ArticleLike> findByArticleIdOrderByCreatedAtDesc(Long articleId);
}