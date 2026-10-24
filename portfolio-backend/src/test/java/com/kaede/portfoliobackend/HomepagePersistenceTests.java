package com.kaede.portfoliobackend;

import com.kaede.portfoliobackend.service.HomepageService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

// Uses the configured local PostgreSQL; every test rolls back its changes.
@SpringBootTest(properties = {"spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false"})
@Transactional
class HomepagePersistenceTests {
    @Autowired HomepageService service;
    @Autowired EntityManager entityManager;

    @Test void countdownIsSharedAndRepeatedUpdatesKeepOnlyTheCurrentRow() {
        for (int i = 0; i < 20; i++) {
            var saved = service.saveCountdown(new HomepageService.CountdownInput("  修士入学考 " + i + "  ", "2028-02-29"));
            assertEquals(1L, saved.getId());
            entityManager.flush();
            entityManager.clear();
        }
        Number count = (Number) entityManager.createNativeQuery("SELECT count(*) FROM homepage_countdown").getSingleResult();
        assertEquals(1L, count.longValue());
        var latest = service.getCountdown().orElseThrow();
        assertEquals("修士入学考 19", latest.getEvent());
        assertEquals(LocalDate.of(2028, 2, 29), latest.getTargetDate());
        // Past dates are still valid configurations; the browser displays zero remaining days.
        assertEquals(LocalDate.of(2020, 1, 1), service.saveCountdown(new HomepageService.CountdownInput("Past event", "2020-01-01")).getTargetDate());
    }

    @Test void anUnconfiguredCountdownHasNoInventedDefault() {
        entityManager.createNativeQuery("DELETE FROM homepage_countdown").executeUpdate();
        entityManager.clear();
        assertTrue(service.getCountdown().isEmpty());
    }

    @Test void databaseConstraintRejectsAnAdditionalCountdownIdentity() {
        assertThrows(PersistenceException.class, () -> entityManager.createNativeQuery("""
                INSERT INTO homepage_countdown (id, event, target_date)
                VALUES (2, 'Test event', DATE '2028-02-29')
                """).executeUpdate());
    }

    @Test void allThreeTranslationsAndLiteralCharactersSurviveDatabaseRoundTrip() {
        var zh = new HomepageService.TranslationInput("中文简介\n第二行", "", "test@example.test", "<b>plain text</b>");
        var en = new HomepageService.TranslationInput("English intro", "Paragraph 2", "contact@example.test", "Additional note");
        var ja = new HomepageService.TranslationInput("日本語の紹介", "", "test@example.test", "連絡先");
        service.saveProfile(new HomepageService.ProfileInput(Map.of("zh", zh, "en", en, "ja", ja)));
        entityManager.flush();
        entityManager.clear();
        var result = service.getContent().translations();
        assertEquals(3, result.size());
        assertEquals(zh.desc1(), result.get("zh").getDesc1());
        assertEquals("", result.get("zh").getDesc2());
        assertEquals("test@example.test", result.get("zh").getContactPublic());
        assertEquals("<b>plain text</b>", result.get("zh").getContactProf());
        assertEquals(en.desc1(), result.get("en").getDesc1());
        assertEquals(ja.desc1(), result.get("ja").getDesc1());
    }

    @Test void quoteSelectionHonorsLanguageAndEnabledState() {
        service.saveQuote(null, new HomepageService.QuoteInput("test common quote", "", null, true));
        service.saveQuote(null, new HomepageService.QuoteInput("test Chinese quote", "", "zh", true));
        var disabled = service.saveQuote(null, new HomepageService.QuoteInput("test disabled quote", "", null, false));
        entityManager.flush();
        entityManager.clear();
        for (int i = 0; i < 20; i++) {
            var selected = service.randomQuote("en").orElseThrow();
            assertTrue(selected.isEnabled());
            assertTrue(selected.getLanguage() == null || selected.getLanguage().equals("en"));
            assertNotEquals(disabled.getId(), selected.getId());
        }
    }

    @Test void repeatedSongUpdatesOnlyKeepTheLatestSingleRow() {
        for (int i = 0; i < 20; i++) {
            var saved = service.saveListening(new HomepageService.ListeningInput(
                    "Test artist " + i, "Test song " + i, "https://music.163.com/song?id=" + (10000 + i), true));
            assertEquals(1L, saved.getId());
            entityManager.flush();
            entityManager.clear();
        }
        Number count = (Number) entityManager.createNativeQuery("SELECT count(*) FROM homepage_listening").getSingleResult();
        assertEquals(1L, count.longValue());
        var latest = service.getContent().listening();
        assertEquals("Test artist 19", latest.getArtist());
        assertEquals("Test song 19", latest.getTitle());
        assertEquals("https://music.163.com/#/song?id=10019", latest.getUrl());
    }

    @Test void hidingTheSongKeepsTheSameRowAndItsInformation() {
        service.saveListening(new HomepageService.ListeningInput("Test artist", "Test song", "https://y.music.163.com/m/song?id=12345", true));
        service.saveListening(new HomepageService.ListeningInput("Test artist", "Test song", "https://music.163.com/song?id=12345", false));
        entityManager.flush();
        entityManager.clear();
        assertNull(service.getContent().listening());
        assertEquals("Test song", service.getListening().getTitle());
        assertEquals(1L, service.getListening().getId());
        Number count = (Number) entityManager.createNativeQuery("SELECT count(*) FROM homepage_listening").getSingleResult();
        assertEquals(1L, count.longValue());
    }

    @Test void databaseConstraintRejectsAnAdditionalSongIdentity() {
        assertThrows(PersistenceException.class, () -> entityManager.createNativeQuery("""
                INSERT INTO homepage_listening (id, artist, title, url, enabled)
                VALUES (2, 'Test artist', 'Test song', '', false)
                """).executeUpdate());
    }
}
