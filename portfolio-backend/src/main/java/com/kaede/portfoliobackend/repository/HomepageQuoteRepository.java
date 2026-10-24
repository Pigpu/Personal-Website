package com.kaede.portfoliobackend.repository;

import com.kaede.portfoliobackend.entity.HomepageQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface HomepageQuoteRepository extends JpaRepository<HomepageQuote, Long> {
    List<HomepageQuote> findAllByOrderByIdDesc();

    @Query(value = "SELECT * FROM homepage_quotes WHERE enabled = true AND (language IS NULL OR language = :language) ORDER BY random() LIMIT 1", nativeQuery = true)
    Optional<HomepageQuote> findRandomForLanguage(@Param("language") String language);
}
