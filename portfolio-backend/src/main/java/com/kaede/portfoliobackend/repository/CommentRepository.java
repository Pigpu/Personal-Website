package com.kaede.portfoliobackend.repository;

import com.kaede.portfoliobackend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 💡 关键修改：
     * 1. 返回类型改为 List<Map<String, Object>>，这样能接收所有字段。
     * 2. 使用 AS "别名" 强制让 SQL 返回前端需要的驼峰字段名 (createdAt, username 等)。
     */
    @Query(value = """
        SELECT 
            c.id as "id",
            c.content as "content",
            c.article_id as "articleId",
            c.user_id as "userId",
            c.parent_id as "parentId",
            c.created_at as "createdAt",
            
            u.username as "username",
            u.role as "role",
            
            pu.username as "parentUsername"
        FROM comments c
        LEFT JOIN users u ON c.user_id = u.id
        LEFT JOIN comments pc ON c.parent_id = pc.id
        LEFT JOIN users pu ON pc.user_id = pu.id
        WHERE c.article_id = :articleId
        ORDER BY c.created_at ASC
        """, nativeQuery = true)
    List<Map<String, Object>> findByArticleIdWithUsernames(@Param("articleId") Long articleId);
}