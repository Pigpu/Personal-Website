package com.kaede.portfoliobackend;

import com.kaede.portfoliobackend.config.JwtAuthenticationFilter;
import com.kaede.portfoliobackend.config.SecurityConfig;
import com.kaede.portfoliobackend.controller.HomepageController;
import com.kaede.portfoliobackend.entity.HomepageCountdown;
import com.kaede.portfoliobackend.service.HomepageService;
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
import java.util.Optional;
import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomepageController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtils.class})
class HomepageSecurityTests {
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @MockitoBean HomepageService service;

    private record Operation(HttpMethod method, String path, String body) {}
    private final List<Operation> management = List.of(
            new Operation(HttpMethod.PUT, "/api/home/admin/profile", "{\"translations\":{}}"),
            new Operation(HttpMethod.GET, "/api/home/admin/quotes", null),
            new Operation(HttpMethod.POST, "/api/home/admin/quotes", "{\"content\":\"hello\",\"enabled\":true}"),
            new Operation(HttpMethod.PUT, "/api/home/admin/quotes/1", "{\"content\":\"hello\",\"enabled\":false}"),
            new Operation(HttpMethod.GET, "/api/home/admin/listening", null),
            new Operation(HttpMethod.PUT, "/api/home/admin/listening", "{\"enabled\":false}"),
            new Operation(HttpMethod.PUT, "/api/home/admin/countdown", "{\"event\":\"修士入学考\",\"targetDate\":\"2028-02-29\"}")
    );

    @Test void visitorsCanReadContentAndEmptyQuoteWithoutLoggingIn() throws Exception {
        when(service.getContent()).thenReturn(new HomepageService.HomepageContent(Map.of(), null));
        when(service.randomQuote("zh")).thenReturn(Optional.empty());
        mvc.perform(get("/api/home")).andExpect(status().isOk()).andExpect(jsonPath("$.translations").isMap())
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/home/quote")).andExpect(status().isNoContent());
    }

    @Test void visitorsReadTheSharedCountdownWithNoCacheOrBrowserSpecificFallback() throws Exception {
        var value = new HomepageCountdown();
        value.setEvent("修士入学考");
        value.setTargetDate(LocalDate.of(2028, 2, 29));
        when(service.getCountdown()).thenReturn(Optional.of(value));
        mvc.perform(get("/api/home/countdown")).andExpect(status().isOk())
                .andExpect(jsonPath("$.event").value("修士入学考"))
                .andExpect(jsonPath("$.targetDate").value("2028-02-29"))
                .andExpect(header().string("Cache-Control", "no-store"));
        when(service.getCountdown()).thenReturn(Optional.empty());
        mvc.perform(get("/api/home/countdown")).andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test void visitorsCannotAccessAnyManagementEndpoint() throws Exception {
        for (Operation operation : management) {
            var builder = request(operation.method(), operation.path());
            if (operation.body() != null) builder.contentType("application/json").content(operation.body());
            mvc.perform(builder).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test void signedInRegularUsersCannotManageHomepage() throws Exception {
        String token = jwt.generateToken("homepage-test-reader", "ROLE_USER");
        for (Operation operation : management) {
            var builder = request(operation.method(), operation.path()).header("Authorization", "Bearer " + token);
            if (operation.body() != null) builder.contentType("application/json").content(operation.body());
            mvc.perform(builder).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test void verifiedAdminCanUseAllManagementEndpoints() throws Exception {
        String token = jwt.generateToken("homepage-test-admin", "ROLE_ADMIN");
        for (Operation operation : management) {
            var builder = request(operation.method(), operation.path()).header("Authorization", "Bearer " + token);
            if (operation.body() != null) builder.contentType("application/json").content(operation.body());
            mvc.perform(builder).andExpect(status().is2xxSuccessful());
        }
        verify(service).getQuotes();
        verify(service).getListening();
        verify(service).saveProfile(any());
        verify(service).saveListening(any());
        verify(service).saveCountdown(any());
        verify(service).saveQuote(isNull(), any());
        verify(service).saveQuote(eq(1L), any());
    }

    @Test void forgedRoleInInvalidTokenDoesNotGrantAdminAccess() throws Exception {
        mvc.perform(get("/api/home/admin/quotes").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
