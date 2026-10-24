package com.kaede.portfoliobackend;

import com.kaede.portfoliobackend.repository.*;
import com.kaede.portfoliobackend.service.HomepageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HomepageValidationTests {
    private final HomepageService service = new HomepageService(mock(HomepageTranslationRepository.class), mock(HomepageQuoteRepository.class), mock(HomepageListeningRepository.class), mock(HomepageCountdownRepository.class));

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-29", "2026-04-31", "2026-13-01", "2026-00-10", "2026-01-00", "26-01-01", "2026-1-01", "2026-01-01T00:00:00Z", "0000-01-01", "", "not a date"})
    void rejectsInvalidCountdownDates(String value) {
        assertThrows(ResponseStatusException.class, () -> service.saveCountdown(new HomepageService.CountdownInput("入学考", value)));
    }

    @Test void requiresCountdownNameAndDateWithoutWritingInvalidValues() {
        assertThrows(ResponseStatusException.class, () -> service.saveCountdown(null));
        assertThrows(ResponseStatusException.class, () -> service.saveCountdown(new HomepageService.CountdownInput(" \n ", "2026-11-01")));
        assertThrows(ResponseStatusException.class, () -> service.saveCountdown(new HomepageService.CountdownInput("a".repeat(121), "2026-11-01")));
        assertThrows(ResponseStatusException.class, () -> service.saveCountdown(new HomepageService.CountdownInput("入学考", null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://music.163.com/#/song?id=12345", "https://music.163.com/song?id=12345&user=42", "https://y.music.163.com/m/song?userid=42&id=12345"})
    void acceptsDesktopAndMobileSongLinks(String value) {
        assertEquals("https://music.163.com/#/song?id=12345", HomepageService.normalizeSongUrl(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "https://music.163.com.evil.test/song?id=123", "https://music.163.com@evil.test/song?id=123", "https://user@music.163.com/song?id=123", "http://music.163.com/song?id=123", "https://music.163.com/artist?id=123", "https://music.163.com/song?id=abc", "https://music.163.com/song", "https://music.163.com:9999/song?id=123", "https://evil.test/#/song?id=123"})
    void rejectsUnsafeOrNonSongLinks(String value) {
        assertThrows(ResponseStatusException.class, () -> HomepageService.normalizeSongUrl(value));
    }

    @Test void visibleSongRequiresArtistTitleAndLink() {
        assertThrows(ResponseStatusException.class, () -> service.saveListening(new HomepageService.ListeningInput("", "", "", true)));
    }

    @Test void requiresAllThreeLanguagesForProfileSave() {
        assertThrows(ResponseStatusException.class, () -> service.saveProfile(new HomepageService.ProfileInput(Map.of())));
    }

    @Test void rejectsBlankOrOversizedQuotesAndUnknownLanguages() {
        assertThrows(ResponseStatusException.class, () -> service.saveQuote(null, new HomepageService.QuoteInput("  ", "", null, true)));
        assertThrows(ResponseStatusException.class, () -> service.saveQuote(null, new HomepageService.QuoteInput("a".repeat(501), "", null, true)));
        assertThrows(ResponseStatusException.class, () -> service.saveQuote(null, new HomepageService.QuoteInput("hello", "", "fr", true)));
    }

}
