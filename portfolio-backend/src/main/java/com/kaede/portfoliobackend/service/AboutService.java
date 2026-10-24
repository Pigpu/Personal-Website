package com.kaede.portfoliobackend.service;

import com.kaede.portfoliobackend.entity.AboutTranslation;
import com.kaede.portfoliobackend.entity.FriendLink;
import com.kaede.portfoliobackend.repository.AboutTranslationRepository;
import com.kaede.portfoliobackend.repository.FriendLinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AboutService {
    public record TranslationInput(String title, String purpose1, String purpose2) {}
    public record ProfileInput(Map<String, TranslationInput> translations) {}
    public record LinkInput(String name, String url, String description) {}
    public record AboutContent(Map<String, AboutTranslation> translations, List<FriendLink> links) {}
    private static final Set<String> LANGUAGES = Set.of("zh", "en", "ja");
    private final AboutTranslationRepository translations;
    private final FriendLinkRepository links;

    public AboutService(AboutTranslationRepository translations, FriendLinkRepository links) {
        this.translations = translations;
        this.links = links;
    }

    public AboutContent getContent() {
        Map<String, AboutTranslation> result = new LinkedHashMap<>();
        translations.findAll().forEach(value -> result.put(value.getLanguage(), value));
        return new AboutContent(result, links.findAllByOrderByIdAsc());
    }

    @Transactional
    public AboutContent saveProfile(ProfileInput input) {
        if (input == null || input.translations() == null || !input.translations().keySet().equals(LANGUAGES)) {
            throw badRequest("请同时提交中、英、日三套介绍");
        }
        for (String language : List.of("zh", "en", "ja")) {
            var value = input.translations().get(language);
            if (value == null) throw badRequest("语言内容不能为空");
            var item = new AboutTranslation();
            item.setLanguage(language);
            item.setTitle(required(value.title(), 120, "标题不能为空"));
            item.setPurpose1(text(value.purpose1(), 10000));
            item.setPurpose2(text(value.purpose2(), 10000));
            translations.save(item);
        }
        return getContent();
    }

    @Transactional
    public FriendLink saveLink(Long id, LinkInput input) {
        if (input == null) throw badRequest("请填写友情链接");
        String name = required(input.name(), 120, "网站名称不能为空");
        String url = normalizeLinkUrl(required(input.url(), 2000, "网站链接不能为空"));
        String description = text(input.description(), 500);
        FriendLink item = id == null ? new FriendLink() : links.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "友情链接不存在"));
        item.setName(name);
        item.setUrl(url);
        item.setDescription(description);
        return links.save(item);
    }

    @Transactional
    public void deleteLink(Long id) {
        FriendLink item = links.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "友情链接不存在"));
        links.delete(item);
    }

    public static String normalizeLinkUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = Objects.requireNonNullElse(uri.getScheme(), "").toLowerCase(Locale.ROOT);
            if (!Set.of("http", "https").contains(scheme) || uri.getHost() == null || uri.getHost().isBlank()
                    || uri.getUserInfo() != null || uri.getPort() < -1 || uri.getPort() == 0 || uri.getPort() > 65535) {
                throw new IllegalArgumentException();
            }
            return text(uri.toASCIIString(), 2000);
        } catch (IllegalArgumentException exception) {
            throw badRequest("请填写有效的 HTTP 或 HTTPS 网站链接");
        }
    }

    private static String required(String value, int max, String message) {
        String result = text(value, max);
        if (result.isBlank()) throw badRequest(message);
        return result;
    }
    private static String text(String value, int max) {
        String result = Objects.requireNonNullElse(value, "").trim();
        if (result.length() > max) throw badRequest("内容超过长度限制：" + max);
        return result;
    }
    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
