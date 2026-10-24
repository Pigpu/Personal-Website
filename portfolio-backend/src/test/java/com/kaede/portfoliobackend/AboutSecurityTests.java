package com.kaede.portfoliobackend;

import com.kaede.portfoliobackend.config.JwtAuthenticationFilter;
import com.kaede.portfoliobackend.config.SecurityConfig;
import com.kaede.portfoliobackend.controller.AboutController;
import com.kaede.portfoliobackend.entity.FriendLink;
import com.kaede.portfoliobackend.service.AboutService;
import com.kaede.portfoliobackend.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AboutController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtils.class})
class AboutSecurityTests {
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @MockitoBean AboutService service;
    private record Operation(HttpMethod method, String path, String body) {}
    private final List<Operation> management = List.of(
            new Operation(HttpMethod.PUT, "/api/about/admin/profile", "{\"translations\":{}}"),
            new Operation(HttpMethod.POST, "/api/about/admin/links", "{\"name\":\"Friend\",\"url\":\"https://example.test\",\"description\":\"\"}"),
            new Operation(HttpMethod.PUT, "/api/about/admin/links/1", "{\"name\":\"Friend\",\"url\":\"https://example.test\",\"description\":\"\"}"),
            new Operation(HttpMethod.DELETE, "/api/about/admin/links/1", null)
    );

    @Test void visitorsCanReadIntroductionsAndLinksWithoutCachedPersonalContent() throws Exception {
        var link = new FriendLink();
        link.setId(1L); link.setName("Friend"); link.setUrl("https://example.test"); link.setDescription("");
        when(service.getContent()).thenReturn(new AboutService.AboutContent(Map.of(), List.of(link)));
        mvc.perform(get("/api/about")).andExpect(status().isOk())
                .andExpect(jsonPath("$.translations").isMap()).andExpect(jsonPath("$.links[0].name").value("Friend"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }
    @Test void visitorsCannotModifyAboutContent() throws Exception {
        for (Operation operation : management) {
            var builder = request(operation.method(), operation.path());
            if (operation.body() != null) builder.contentType("application/json").content(operation.body());
            mvc.perform(builder).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }
    @Test void regularUsersCannotModifyAboutContent() throws Exception {
        String token = jwt.generateToken("about-test-reader", "ROLE_USER");
        for (Operation operation : management) {
            var builder = request(operation.method(), operation.path()).header("Authorization", "Bearer " + token);
            if (operation.body() != null) builder.contentType("application/json").content(operation.body());
            mvc.perform(builder).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }
    @Test void verifiedAdminCanSaveProfilesAndCreateEditDeleteLinks() throws Exception {
        String token = jwt.generateToken("about-test-admin", "ROLE_ADMIN");
        for (Operation operation : management) {
            var builder = request(operation.method(), operation.path()).header("Authorization", "Bearer " + token);
            if (operation.body() != null) builder.contentType("application/json").content(operation.body());
            mvc.perform(builder).andExpect(status().is2xxSuccessful());
        }
        verify(service).saveProfile(any());
        verify(service).saveLink(isNull(), any());
        verify(service).saveLink(eq(1L), any());
        verify(service).deleteLink(1L);
    }
    @Test void invalidTokensCannotGrantAdminAccess() throws Exception {
        mvc.perform(delete("/api/about/admin/links/1").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
