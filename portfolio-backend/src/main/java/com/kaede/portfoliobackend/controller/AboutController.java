package com.kaede.portfoliobackend.controller;

import com.kaede.portfoliobackend.entity.FriendLink;
import com.kaede.portfoliobackend.service.AboutService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/about")
public class AboutController {
    private final AboutService service;
    public AboutController(AboutService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<AboutService.AboutContent> getContent() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.getContent());
    }
    @PutMapping("/admin/profile")
    public AboutService.AboutContent saveProfile(@RequestBody AboutService.ProfileInput input) {
        return service.saveProfile(input);
    }
    @PostMapping("/admin/links")
    public ResponseEntity<FriendLink> addLink(@RequestBody AboutService.LinkInput input) {
        return ResponseEntity.status(201).body(service.saveLink(null, input));
    }
    @PutMapping("/admin/links/{id}")
    public FriendLink updateLink(@PathVariable Long id, @RequestBody AboutService.LinkInput input) {
        return service.saveLink(id, input);
    }
    @DeleteMapping("/admin/links/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable Long id) {
        service.deleteLink(id);
        return ResponseEntity.noContent().build();
    }
}
