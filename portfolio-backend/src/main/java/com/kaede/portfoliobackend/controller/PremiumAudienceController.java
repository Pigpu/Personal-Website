package com.kaede.portfoliobackend.controller;

import com.kaede.portfoliobackend.repository.PremiumAudioRepository;
import com.kaede.portfoliobackend.service.PremiumAudienceService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/premium")
public class PremiumAudienceController {
    public record Episode(Long id, String uuid, String title, LocalDateTime createAt) {}
    private final PremiumAudienceService audience;
    private final PremiumAudioRepository audio;

    public PremiumAudienceController(PremiumAudienceService audience, PremiumAudioRepository audio) {
        this.audience = audience;
        this.audio = audio;
    }

    @GetMapping("/access")
    public ResponseEntity<PremiumAudienceService.Access> access(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(audience.access(authentication));
    }

    @GetMapping("/list")
    public ResponseEntity<List<Episode>> list(Authentication authentication) {
        audience.requireAccess(authentication);
        var episodes = audio.findAll().stream().map(value -> new Episode(value.getId(), value.getUuid(), value.getTitle(), value.getCreateAt())).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(episodes);
    }

    @GetMapping("/admin/listeners")
    public ResponseEntity<List<PremiumAudienceService.ListenerView>> listeners() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(audience.listListeners());
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<PremiumAudienceService.UserView>> users(@RequestParam(defaultValue = "") String query) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(audience.searchUsers(query));
    }

    @PostMapping("/admin/listeners")
    public PremiumAudienceService.ListenerView grant(@RequestBody PremiumAudienceService.ListenerInput input) {
        return audience.grant(input);
    }

    @DeleteMapping("/admin/listeners/{userId}")
    public ResponseEntity<Void> revoke(@PathVariable Long userId) {
        audience.revoke(userId);
        return ResponseEntity.noContent().build();
    }
}
