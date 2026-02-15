package com.kaede.portfoliobackend.repository;

import com.kaede.portfoliobackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 👈 声明这个方法，Spring Data JPA 会自动帮你实现 SQL 查询
    Optional<User> findByUsername(String username);
}