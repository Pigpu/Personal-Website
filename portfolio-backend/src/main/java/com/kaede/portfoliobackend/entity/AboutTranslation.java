package com.kaede.portfoliobackend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "about_translations")
public class AboutTranslation {
    @Id
    @Column(length = 2)
    private String language;
    @Column(nullable = false, length = 120)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String purpose1;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String purpose2;
}
