package com.kaede.portfoliobackend;

import com.kaede.portfoliobackend.repository.AboutTranslationRepository;
import com.kaede.portfoliobackend.repository.FriendLinkRepository;
import com.kaede.portfoliobackend.service.AboutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AboutValidationTests {
    private final AboutTranslationRepository translations = mock(AboutTranslationRepository.class);
    private final FriendLinkRepository links = mock(FriendLinkRepository.class);
    private final AboutService service = new AboutService(translations, links);

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "http://example.com/old", "https://example.com:8443/path?query=1#part", "https://example.com/日本語"})
    void acceptsHttpAndHttpsWebsites(String value) {
        assertTrue(AboutService.normalizeLinkUrl(value).startsWith("http"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "data:text/html,hello", "ftp://example.com/file", "//example.com", "/relative", "https:///path", "https://user:pass@example.com", "https://example.com:0", "https://example.com:65536", "https://exa mple.com", "https://"})
    void rejectsNonWebLinksAndMalformedUrls(String value) {
        var exception = assertThrows(ResponseStatusException.class, () -> AboutService.normalizeLinkUrl(value));
        assertEquals(400, exception.getStatusCode().value());
    }

    @Test void validatesTheEncodedUrlLengthBeforeDatabaseWrite() {
        assertThrows(ResponseStatusException.class, () -> service.saveLink(null, new AboutService.LinkInput("Name", "https://example.com/" + "日".repeat(400), "")));
        verifyNoInteractions(links);
    }

    @Test void requiresAllThreeLanguagesAndLimitsProfileFields() {
        assertThrows(ResponseStatusException.class, () -> service.saveProfile(null));
        assertThrows(ResponseStatusException.class, () -> service.saveProfile(new AboutService.ProfileInput(Map.of())));
        var good = new AboutService.TranslationInput("Title", "", "");
        assertThrows(ResponseStatusException.class, () -> service.saveProfile(new AboutService.ProfileInput(Map.of("zh", good, "en", good, "fr", good))));
        for (var bad : new AboutService.TranslationInput[] {
                new AboutService.TranslationInput(" ", "", ""),
                new AboutService.TranslationInput("a".repeat(121), "", ""),
                new AboutService.TranslationInput("Title", "a".repeat(10001), "") }) {
            assertThrows(ResponseStatusException.class, () -> service.saveProfile(new AboutService.ProfileInput(Map.of("zh", bad, "en", good, "ja", good))));
        }
    }

    @Test void rejectsEmptyAndOversizedLinkFields() {
        assertThrows(ResponseStatusException.class, () -> service.saveLink(null, null));
        for (var value : new AboutService.LinkInput[] {
                new AboutService.LinkInput(" ", "https://example.com", ""),
                new AboutService.LinkInput("a".repeat(121), "https://example.com", ""),
                new AboutService.LinkInput("Name", "", ""),
                new AboutService.LinkInput("Name", "https://example.com", "a".repeat(501)) }) {
            assertThrows(ResponseStatusException.class, () -> service.saveLink(null, value));
        }
        verifyNoInteractions(links);
    }

    @Test void missingLinksCannotBeEditedOrDeleted() {
        var exception = assertThrows(ResponseStatusException.class, () -> service.saveLink(999L, new AboutService.LinkInput("Name", "https://example.com", "")));
        assertEquals(404, exception.getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.deleteLink(999L)).getStatusCode().value());
    }
}
