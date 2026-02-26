package com.kaede.portfoliobackend.repository;

import com.kaede.portfoliobackend.entity.ProjectLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectLikeRepository extends JpaRepository<ProjectLike, Long> {
    // 查找某人是否对某个作品点过赞
    Optional<ProjectLike> findByProjectIdAndUsername(Long projectId, String username);

    // 获取某个作品的所有点赞记录（按时间倒序）
    List<ProjectLike> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}