package com.kaede.portfoliobackend;

import com.kaede.portfoliobackend.config.JwtAuthenticationFilter;
import com.kaede.portfoliobackend.config.SecurityConfig;
import com.kaede.portfoliobackend.controller.PremiumAudienceController;
import com.kaede.portfoliobackend.entity.PremiumAudio;
import com.kaede.portfoliobackend.entity.PremiumListener;
import com.kaede.portfoliobackend.entity.User;
import com.kaede.portfoliobackend.repository.PremiumAudioRepository;
import com.kaede.portfoliobackend.repository.PremiumListenerRepository;
import com.kaede.portfoliobackend.repository.UserRepository;
import com.kaede.portfoliobackend.service.PremiumAudienceService;
import com.kaede.portfoliobackend.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PremiumAudienceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtils.class, PremiumAudienceService.class})
class PremiumAudienceSecurityTests {
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @MockitoBean UserRepository users;
    @MockitoBean PremiumListenerRepository listeners;
    @MockitoBean PremiumAudioRepository audio;

    private String admin() { return "Bearer " + jwt.generateToken("audience-admin", "ROLE_ADMIN"); }
    private String reader() { return "Bearer " + jwt.generateToken("audience-reader", "ROLE_USER"); }
    private User user() {
        var user = new User(); user.setId(42L); user.setUsername("audience-reader");
        user.setRole("ROLE_USER"); user.setPassword("must-never-be-exposed"); return user;
    }

    @Test void anonymousVisitorsCannotSeeEntryOrReadTheEpisodeList() throws Exception {
        mvc.perform(get("/api/premium/access")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.allowed").value(false)).andExpect(jsonPath("$.admin").value(false));
        mvc.perform(get("/api/premium/list")).andExpect(status().isForbidden());
        verifyNoInteractions(users, listeners, audio);
    }
    @Test void ordinaryAccountsWithoutAnExplicitGrantCannotBypassTheHiddenEntry() throws Exception {
        when(users.findByUsername("audience-reader")).thenReturn(Optional.of(user()));
        mvc.perform(get("/api/premium/access").header("Authorization", reader())).andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false));
        mvc.perform(get("/api/premium/list").header("Authorization", reader())).andExpect(status().isForbidden());
        verifyNoInteractions(audio);
    }
    @Test void authorizedListenerCanReadEpisodeTitlesButNotAdministratorAudioData() throws Exception {
        when(users.findByUsername("audience-reader")).thenReturn(Optional.of(user()));
        when(listeners.existsById(42L)).thenReturn(true);
        var item = new PremiumAudio(); item.setId(9L); item.setTitle("Shared episode");
        item.setUuid("11111111-2222-4333-8444-555555555555"); item.setAudioUrl("https://example.test/private.mp3");
        when(audio.findAll()).thenReturn(List.of(item));
        mvc.perform(get("/api/premium/access").header("Authorization", reader())).andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true)).andExpect(jsonPath("$.admin").value(false));
        mvc.perform(get("/api/premium/list").header("Authorization", reader())).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$[0].title").value("Shared episode"))
                .andExpect(jsonPath("$[0].audioUrl").doesNotExist());
    }
    @Test void administratorsAlwaysHaveAccessWithoutAListenerRecord() throws Exception {
        mvc.perform(get("/api/premium/access").header("Authorization", admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true)).andExpect(jsonPath("$.admin").value(true));
        mvc.perform(get("/api/premium/list").header("Authorization", admin())).andExpect(status().isOk());
        verifyNoInteractions(users, listeners);
    }
    @Test void listenersCannotSearchAccountsManageListenersOrUseAdministratorEndpoints() throws Exception {
        for (String token : List.of(reader(), "Bearer invalid-token", "")) {
            mvc.perform(get("/api/premium/admin/users").param("query", "reader").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(get("/api/premium/admin/listeners").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(post("/api/premium/admin/listeners").header("Authorization", token).contentType("application/json").content("{\"userId\":42}"))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/premium/admin/listeners/42").header("Authorization", token)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(users, listeners, audio);
    }
    @Test void searchIsBoundedAndReturnsNoPasswordOrAccountRole() throws Exception {
        when(users.findByUsernameContainingIgnoreCaseOrderByUsernameAscIdAsc(eq("reader"), any())).thenReturn(List.of(user()));
        mvc.perform(get("/api/premium/admin/users").param("query", " reader ").header("Authorization", admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].username").value("audience-reader"))
                .andExpect(jsonPath("$[0].password").doesNotExist()).andExpect(jsonPath("$[0].role").doesNotExist());
        verify(users).findByUsernameContainingIgnoreCaseOrderByUsernameAscIdAsc(eq("reader"), argThat(page -> page.getPageSize() == 20));
    }
    @Test void administratorCanAddByUsernameAndRemoveWithoutEditingTheAccount() throws Exception {
        when(users.findByUsername("audience-reader")).thenReturn(Optional.of(user()));
        when(listeners.saveAndFlush(any())).thenAnswer(call -> {
            PremiumListener value = call.getArgument(0); value.setUserId(value.getUser().getId()); return value;
        });
        mvc.perform(post("/api/premium/admin/listeners").header("Authorization", admin()).contentType("application/json")
                .content("{\"username\":\"audience-reader\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.password").doesNotExist());
        var existing = new PremiumListener(); existing.setUserId(42L); existing.setUser(user());
        when(listeners.findById(42L)).thenReturn(Optional.of(existing));
        mvc.perform(delete("/api/premium/admin/listeners/42").header("Authorization", admin())).andExpect(status().isNoContent());
        verify(listeners).delete(existing);
        verify(users, never()).save(any());
    }
    @Test void nonexistentAccountsAreRejected() throws Exception {
        mvc.perform(post("/api/premium/admin/listeners").header("Authorization", admin()).contentType("application/json")
                .content("{\"userId\":999}"))
                .andExpect(status().isNotFound());
        verify(listeners, never()).saveAndFlush(any());
    }
}
