package com.kaede.portfoliobackend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password; // 存储加密后的密码
    private String role;     // 比如 "ROLE_ADMIN" 或 "ROLE_USER"
    private LocalDateTime createdAt = LocalDateTime.now();
}