package com.kaede.portfoliobackend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "homepage_quotes")
public class HomepageQuote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 500)
    private String content;
    @Column(nullable = false, length = 120)
    private String source = "";
    // null means suitable for all three site languages.
    @Column(length = 2)
    private String language;
    @Column(nullable = false)
    private boolean enabled = true;
}
