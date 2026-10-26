package com.kaede.portfoliobackend;

import com.kaede.portfoliobackend.controller.PremiumAudioController;
import com.kaede.portfoliobackend.entity.PremiumAudio;
import com.kaede.portfoliobackend.repository.PremiumAudioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false"})
@Transactional
class PremiumAudioPersistenceTests {
    @Autowired PremiumAudioController controller;
    @Autowired PremiumAudioRepository repository;
    @Autowired EntityManager entityManager;

    @Test void publishingDoesNotChangeExistingUuidRecordsInTheOriginalTable() {
        String oldUuid = UUID.randomUUID().toString();
        var previous = new PremiumAudio();
        previous.setUuid(oldUuid); previous.setTitle("Existing episode");
        previous.setAudioUrl("https://example.test/old.wav"); previous.setCreateAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        repository.saveAndFlush(previous);
        Long oldId = previous.getId();
        long count = repository.count();

        var next = new PremiumAudio();
        next.setTitle("New private episode"); next.setAudioUrl("/uploads/media/new.flac");
        controller.savePremiumAudio(next);
        entityManager.flush();
        entityManager.clear();

        assertEquals(count + 1, repository.count());
        var existing = controller.getAudioByUuid(oldUuid).getBody();
        assertNotNull(existing);
        assertEquals(oldId, existing.getId());
        assertEquals("https://example.test/old.wav", existing.getAudioUrl());
        assertEquals(LocalDateTime.of(2026, 4, 1, 12, 0), existing.getCreateAt());
        assertNotEquals(oldUuid, next.getUuid());
        assertEquals("/uploads/media/new.flac", repository.findByUuid(next.getUuid()).orElseThrow().getAudioUrl());
    }
}
