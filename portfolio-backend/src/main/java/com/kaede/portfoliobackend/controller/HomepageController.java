package com.kaede.portfoliobackend.controller;

import com.kaede.portfoliobackend.entity.HomepageQuote;
import com.kaede.portfoliobackend.entity.HomepageListening;
import com.kaede.portfoliobackend.entity.HomepageCountdown;
import com.kaede.portfoliobackend.service.HomepageService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/home")
public class HomepageController {
    private final HomepageService service;
    public HomepageController(HomepageService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<HomepageService.HomepageContent> getContent() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.getContent());
    }

    @GetMapping("/quote")
    public ResponseEntity<HomepageQuote> randomQuote(@RequestParam(defaultValue = "zh") String lang) {
        return service.randomQuote(lang)
                .map(value -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value))
                .orElseGet(() -> ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build());
    }

    @PutMapping("/admin/profile")
    public HomepageService.HomepageContent saveProfile(@RequestBody HomepageService.ProfileInput input) {
        return service.saveProfile(input);
    }

    @GetMapping("/admin/quotes")
    public ResponseEntity<List<HomepageQuote>> getQuotes() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.getQuotes());
    }

    @PostMapping("/admin/quotes")
    public ResponseEntity<HomepageQuote> addQuote(@RequestBody HomepageService.QuoteInput input) {
        return ResponseEntity.status(201).body(service.saveQuote(null, input));
    }

    @PutMapping("/admin/quotes/{id}")
    public HomepageQuote updateQuote(@PathVariable Long id, @RequestBody HomepageService.QuoteInput input) {
        return service.saveQuote(id, input);
    }

    @GetMapping("/admin/listening")
    public ResponseEntity<HomepageListening> getListening() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.getListening());
    }

    @PutMapping("/admin/listening")
    public HomepageListening saveListening(@RequestBody HomepageService.ListeningInput input) {
        return service.saveListening(input);
    }

    @GetMapping("/countdown")
    public ResponseEntity<HomepageCountdown> getCountdown() {
        return service.getCountdown()
                .map(value -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value))
                .orElseGet(() -> ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build());
    }

    @PutMapping("/admin/countdown")
    public HomepageCountdown saveCountdown(@RequestBody HomepageService.CountdownInput input) {
        return service.saveCountdown(input);
    }
}
