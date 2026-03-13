package com.kaede.portfoliobackend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "paper")
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDate writeDate;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String pdfUrl;

    // --- Getters and Setters ---
    // (如果你用了 Lombok，可以直接在类上面加 @Data 注解，不用写下面这些)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getWriteDate() { return writeDate; }
    public void setWriteDate(LocalDate writeDate) { this.writeDate = writeDate; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
}