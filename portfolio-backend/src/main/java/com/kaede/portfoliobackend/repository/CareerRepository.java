package com.kaede.portfoliobackend.repository;

import com.kaede.portfoliobackend.entity.Career;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareerRepository extends JpaRepository<Career, Long> {
    // 这里暂时不需要写任何代码，JpaRepository 已经自带了 findAll(), save() 等方法
    List<Career> findAllByOrderByStartDateDesc();
}