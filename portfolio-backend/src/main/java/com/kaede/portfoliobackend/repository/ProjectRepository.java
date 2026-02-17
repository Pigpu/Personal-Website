package com.kaede.portfoliobackend.repository;

import com.kaede.portfoliobackend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // 1. 按最新发布排序
    List<Project> findAllByOrderByCreatedAtDesc();

    // 2. 按最多点赞排序
    List<Project> findAllByOrderByLikeCountDesc();

    // 3. 搜索功能（匹配标题或描述，忽略大小写）
    List<Project> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String desc);
}
