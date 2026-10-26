package com.kaede.portfoliobackend;

import com.kaede.portfoliobackend.config.JwtAuthenticationFilter;
import com.kaede.portfoliobackend.config.SecurityConfig;
import com.kaede.portfoliobackend.controller.PremiumAudioController;
import com.kaede.portfoliobackend.entity.PremiumAudio;
import com.kaede.portfoliobackend.repository.PremiumAudioRepository;
import com.kaede.portfoliobackend.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PremiumAudioController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtils.class})
class PremiumAudioSecurityTests {
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @MockitoBean PremiumAudioRepository repository;
    private static final String UUID_VALUE = "11111111-2222-4333-8444-555555555555";

    private PremiumAudio episode() {
        var audio = new PremiumAudio();
        audio.setId(17L); audio.setUuid(UUID_VALUE); audio.setTitle("Existing private audio");
        audio.setAudioUrl("https://example.test/voice.wav?signature=keep-this");
        audio.setCreateAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        return audio;
    }

    @Test void existingUuidPlaysWithoutLoginAndKeepsTheSavedAudioAddress() throws Exception {
        when(repository.findByUuid(UUID_VALUE)).thenReturn(Optional.of(episode()));
        mvc.perform(get("/api/premium/play/" + UUID_VALUE)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.uuid").value(UUID_VALUE))
                .andExpect(jsonPath("$.title").value("Existing private audio"))
                .andExpect(jsonPath("$.audioUrl").value("https://example.test/voice.wav?signature=keep-this"));
        verify(repository, never()).save(any());
    }
    @Test void missingUuidReturnsNotFoundRatherThanAnAuthenticationFailure() throws Exception {
        when(repository.findByUuid(UUID_VALUE)).thenReturn(Optional.empty());
        mvc.perform(get("/api/premium/play/" + UUID_VALUE)).andExpect(status().isNotFound());
    }
    @Test void visitorsAndRegularUsersCannotListOrPublishPrivateAudio() throws Exception {
        for (String token : List.of("", jwt.generateToken("premium-reader", "ROLE_USER"), "invalid-token")) {
            mvc.perform(get("/api/premium/admin/list").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/premium/admin/save").header("Authorization", "Bearer " + token)
                    .contentType("application/json").content("{\"title\":\"Episode\",\"audioUrl\":\"/uploads/media/voice.wav\"}"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(repository);
    }
    @Test void verifiedAdminCanListEpisodes() throws Exception {
        when(repository.findAll()).thenReturn(List.of(episode()));
        mvc.perform(get("/api/premium/admin/list").header("Authorization", "Bearer " + jwt.generateToken("premium-admin", "ROLE_ADMIN")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$[0].uuid").value(UUID_VALUE));
    }
    @Test void publishingCreatesANewUuidAndTimestamp() throws Exception {
        mvc.perform(post("/api/premium/admin/save")
                .header("Authorization", "Bearer " + jwt.generateToken("premium-admin", "ROLE_ADMIN"))
                .contentType("application/json")
                .content("{\"title\":\"New episode\",\"audioUrl\":\"/uploads/media/new.flac\",\"uuid\":\"client-value\"}"))
                .andExpect(status().isOk()).andExpect(content().string("发布成功"));
        var captor = ArgumentCaptor.forClass(PremiumAudio.class);
        verify(repository).save(captor.capture());
        var saved = captor.getValue();
        assertNull(saved.getId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getUuid()));
        assertNotEquals(UUID_VALUE, saved.getUuid());
        assertNotNull(saved.getCreateAt());
        assertEquals("/uploads/media/new.flac", saved.getAudioUrl());
    }
}
