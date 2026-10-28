package com.kaede.portfoliobackend;

import com.kaede.portfoliobackend.entity.User;
import com.kaede.portfoliobackend.repository.PremiumListenerRepository;
import com.kaede.portfoliobackend.repository.UserRepository;
import com.kaede.portfoliobackend.service.PremiumAudienceService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false"})
@Transactional
class PremiumAudiencePersistenceTests {
    @Autowired PremiumAudienceService service;
    @Autowired UserRepository users;
    @Autowired PremiumListenerRepository listeners;
    @Autowired EntityManager entityManager;

    private User user(String suffix) {
        var value = new User(); value.setUsername("audience-test-" + suffix + "-" + UUID.randomUUID());
        value.setPassword("test-only-encoded-password"); value.setRole("ROLE_USER");
        return users.saveAndFlush(value);
    }
    private UsernamePasswordAuthenticationToken authentication(User user) {
        return new UsernamePasswordAuthenticationToken(user.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
    @Test void grantingAndRevokingApplyToTheSameSignedInAccountWithoutChangingItsRoleOrPassword() {
        var user = user("access");
        var authentication = authentication(user);
        assertFalse(service.access(authentication).allowed());
        long previousCount = listeners.count();
        var first = service.grant(new PremiumAudienceService.ListenerInput(user.getId(), null));
        var again = service.grant(new PremiumAudienceService.ListenerInput(null, user.getUsername()));
        assertEquals(first.userId(), again.userId());
        assertEquals(first.grantedAt(), again.grantedAt());
        assertEquals(previousCount + 1, listeners.count());
        entityManager.flush(); entityManager.clear();
        assertTrue(service.access(authentication).allowed());
        service.requireAccess(authentication);

        service.revoke(user.getId());
        entityManager.flush(); entityManager.clear();
        assertFalse(service.access(authentication).allowed());
        assertThrows(ResponseStatusException.class, () -> service.requireAccess(authentication));
        var unchanged = users.findById(user.getId()).orElseThrow();
        assertEquals("ROLE_USER", unchanged.getRole());
        assertEquals("test-only-encoded-password", unchanged.getPassword());
        service.revoke(user.getId());
    }
    @Test void searchesAreBoundedAndIncludeCurrentGrantState() {
        var user = user("search");
        service.grant(new PremiumAudienceService.ListenerInput(user.getId(), null));
        var results = service.searchUsers(user.getUsername());
        assertEquals(1, results.size()); assertTrue(results.getFirst().authorized());
        assertEquals(user.getId(), results.getFirst().userId());
        assertTrue(service.searchUsers(" ").isEmpty());
        assertThrows(ResponseStatusException.class, () -> service.searchUsers("a".repeat(256)));
    }
    @Test void administratorsKeepAccessWithoutBeingGrantedOrRevokedAsListeners() {
        var admin = user("admin"); admin.setRole("ROLE_ADMIN"); users.saveAndFlush(admin);
        var authentication = new UsernamePasswordAuthenticationToken(admin.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(service.access(authentication).allowed());
        assertTrue(service.access(authentication).admin());
        assertThrows(ResponseStatusException.class, () -> service.grant(new PremiumAudienceService.ListenerInput(admin.getId(), null)));
        service.revoke(admin.getId());
        assertTrue(service.access(authentication).allowed());
    }
}
