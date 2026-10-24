package com.kaede.portfoliobackend;

import com.kaede.portfoliobackend.service.AboutService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false"})
@Transactional
class AboutPersistenceTests {
    @Autowired AboutService service;
    @Autowired EntityManager entityManager;

    @Test void threeLanguageProfilesPersistAndUpdatesReplaceRatherThanAddRows() {
        var zh = new AboutService.TranslationInput("关于我", "第一段\n换行", "<b>按纯文本显示</b>");
        var en = new AboutService.TranslationInput("About me", "English text", "");
        var ja = new AboutService.TranslationInput("自己紹介", "日本語の紹介", "");
        service.saveProfile(new AboutService.ProfileInput(Map.of("zh", zh, "en", en, "ja", ja)));
        entityManager.flush();
        entityManager.clear();
        var result = service.getContent().translations();
        assertEquals(zh.purpose1(), result.get("zh").getPurpose1());
        assertEquals(zh.purpose2(), result.get("zh").getPurpose2());
        assertEquals("", result.get("en").getPurpose2());
        assertEquals(ja.title(), result.get("ja").getTitle());
        var updated = new AboutService.TranslationInput("新的标题", "新的介绍", "");
        service.saveProfile(new AboutService.ProfileInput(Map.of("zh", updated, "en", en, "ja", ja)));
        entityManager.flush();
        entityManager.clear();
        assertEquals(3, service.getContent().translations().size());
        assertEquals("新的标题", service.getContent().translations().get("zh").getTitle());
        assertEquals(en.purpose1(), service.getContent().translations().get("en").getPurpose1());
        assertEquals(ja.purpose1(), service.getContent().translations().get("ja").getPurpose1());
    }

    @Test void friendsCanBeCreatedEditedAndDeletedWithoutLosingOtherLinks() {
        var first = service.saveLink(null, new AboutService.LinkInput(" Test friend A ", "https://example.test/友人", "Line one\nLine two"));
        var second = service.saveLink(null, new AboutService.LinkInput("Test friend B", "http://example.test/blog", null));
        entityManager.flush();
        entityManager.clear();
        var before = service.getContent().links();
        assertTrue(before.stream().anyMatch(link -> link.getId().equals(first.getId()) && link.getName().equals("Test friend A")));
        int count = before.size();
        var edited = service.saveLink(first.getId(), new AboutService.LinkInput("Updated friend", "https://example.test/new", "<script>literal text</script>"));
        assertEquals(first.getId(), edited.getId());
        entityManager.flush();
        entityManager.clear();
        assertEquals(count, service.getContent().links().size());
        var saved = service.getContent().links().stream().filter(link -> link.getId().equals(first.getId())).findFirst().orElseThrow();
        assertEquals("Updated friend", saved.getName());
        assertEquals("<script>literal text</script>", saved.getDescription());
        service.deleteLink(first.getId());
        entityManager.flush();
        entityManager.clear();
        assertEquals(count - 1, service.getContent().links().size());
        assertTrue(service.getContent().links().stream().anyMatch(link -> link.getId().equals(second.getId()) && link.getDescription().isEmpty()));
        assertThrows(ResponseStatusException.class, () -> service.deleteLink(first.getId()));
    }
}
