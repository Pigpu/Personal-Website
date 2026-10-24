package com.kaede.portfoliobackend.repository;

import com.kaede.portfoliobackend.entity.HomepageListening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomepageListeningRepository extends JpaRepository<HomepageListening, Long> {
    // Fixed primary key plus an atomic upsert: repeated or concurrent saves never add history rows.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO homepage_listening (id, artist, title, url, enabled)
            VALUES (1, :artist, :title, :url, :enabled)
            ON CONFLICT (id) DO UPDATE SET artist = EXCLUDED.artist, title = EXCLUDED.title,
                url = EXCLUDED.url, enabled = EXCLUDED.enabled
            """, nativeQuery = true)
    void saveCurrent(@Param("artist") String artist, @Param("title") String title,
                     @Param("url") String url, @Param("enabled") boolean enabled);
}
