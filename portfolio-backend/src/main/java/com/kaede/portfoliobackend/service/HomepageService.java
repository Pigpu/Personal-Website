package com.kaede.portfoliobackend.service;

import com.kaede.portfoliobackend.entity.*;
import com.kaede.portfoliobackend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class HomepageService {
    public record TranslationInput(String desc1, String desc2, String contactPublic, String contactProf) {}
    public record ProfileInput(Map<String, TranslationInput> translations) {}
    public record QuoteInput(String content, String source, String language, boolean enabled) {}
    public record ListeningInput(String artist, String title, String url, boolean enabled) {}
    public record CountdownInput(String event, String targetDate) {}
    public record HomepageContent(Map<String, HomepageTranslation> translations, HomepageListening listening) {}

    private static final Set<String> LANGUAGES = Set.of("zh", "en", "ja");
    private static final Pattern SONG_ID = Pattern.compile("(?:^|&)id=([1-9][0-9]*)(?:&|$)");
    private final HomepageTranslationRepository translations;
    private final HomepageQuoteRepository quotes;
    private final HomepageListeningRepository listening;
    private final HomepageCountdownRepository countdown;

    public HomepageService(HomepageTranslationRepository translations, HomepageQuoteRepository quotes, HomepageListeningRepository listening, HomepageCountdownRepository countdown) {
        this.translations = translations;
        this.quotes = quotes;
        this.listening = listening;
        this.countdown = countdown;
    }

    public HomepageContent getContent() {
        Map<String, HomepageTranslation> result = new LinkedHashMap<>();
        translations.findAll().forEach(item -> result.put(item.getLanguage(), item));
        return new HomepageContent(result, listening.findById(1L).filter(HomepageListening::isEnabled).orElse(null));
    }

    @Transactional
    public HomepageContent saveProfile(ProfileInput input) {
        if (input == null || input.translations() == null || !input.translations().keySet().equals(LANGUAGES)) {
            throw badRequest("请同时提交中、英、日三套简介与联系方式");
        }
        for (String language : List.of("zh", "en", "ja")) {
            TranslationInput value = input.translations().get(language);
            if (value == null) throw badRequest("语言内容不能为空");
            HomepageTranslation item = translations.findById(language).orElseGet(HomepageTranslation::new);
            item.setLanguage(language);
            item.setDesc1(text(value.desc1(), 5000));
            item.setDesc2(text(value.desc2(), 5000));
            item.setContactPublic(text(value.contactPublic(), 5000));
            item.setContactProf(text(value.contactProf(), 5000));
            translations.save(item);
        }
        return getContent();
    }

    public Optional<HomepageQuote> randomQuote(String language) {
        requireLanguage(language);
        return quotes.findRandomForLanguage(language);
    }

    public List<HomepageQuote> getQuotes() { return quotes.findAllByOrderByIdDesc(); }

    @Transactional
    public HomepageQuote saveQuote(Long id, QuoteInput input) {
        if (input == null) throw badRequest("请填写语录内容");
        String content = text(input.content(), 500);
        if (content.isBlank()) throw badRequest("语录内容不能为空");
        String language = input.language();
        if (language != null) requireLanguage(language);
        HomepageQuote item = id == null ? new HomepageQuote() : quotes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "语录不存在"));
        item.setContent(content);
        item.setSource(text(input.source(), 120));
        item.setLanguage(language);
        item.setEnabled(input.enabled());
        return quotes.save(item);
    }

    public HomepageListening getListening() { return listening.findById(1L).orElseGet(HomepageListening::new); }

    public Optional<HomepageCountdown> getCountdown() { return countdown.findById(1L); }

    @Transactional
    public HomepageCountdown saveCountdown(CountdownInput input) {
        if (input == null) throw badRequest("请填写倒计时事件和目标日期");
        String event = text(input.event(), 120);
        if (event.isBlank()) throw badRequest("事件名称不能为空");
        String date = input.targetDate();
        if (date == null || !date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw badRequest("请填写有效的目标日期（YYYY-MM-DD）");
        }
        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(date);
            if (targetDate.getYear() < 1) throw new DateTimeParseException("Invalid year", date, 0);
        } catch (DateTimeParseException exception) {
            throw badRequest("请填写有效的目标日期（YYYY-MM-DD）");
        }
        countdown.saveCurrent(event, targetDate);
        return getCountdown().orElseThrow();
    }

    @Transactional
    public HomepageListening saveListening(ListeningInput input) {
        if (input == null) throw badRequest("请填写歌曲信息");
        String artist = text(input.artist(), 120);
        String title = text(input.title(), 200);
        String url = text(input.url(), 2000);
        if (input.enabled() && (artist.isBlank() || title.isBlank() || url.isBlank())) {
            throw badRequest("显示最近在听时，请填写歌手、歌名和网易云歌曲链接");
        }
        if (!url.isBlank()) url = normalizeSongUrl(url);
        listening.saveCurrent(artist, title, url, input.enabled());
        return getListening();
    }

    public static String normalizeSongUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || !("music.163.com".equalsIgnoreCase(uri.getHost()) || "y.music.163.com".equalsIgnoreCase(uri.getHost()))) {
                throw new IllegalArgumentException();
            }
            URI song = uri;
            if (uri.getRawFragment() != null && uri.getRawFragment().startsWith("/song?")) {
                if (!(uri.getPath().isEmpty() || uri.getPath().equals("/"))) throw new IllegalArgumentException();
                song = URI.create("https://music.163.com" + uri.getRawFragment());
            }
            if (!Set.of("/song", "/song/", "/m/song").contains(song.getPath())) throw new IllegalArgumentException();
            var match = SONG_ID.matcher(Objects.requireNonNullElse(song.getRawQuery(), ""));
            if (!match.find()) throw new IllegalArgumentException();
            return "https://music.163.com/#/song?id=" + match.group(1);
        } catch (IllegalArgumentException exception) {
            throw badRequest("请填写有效的 HTTPS 网易云歌曲链接（包含歌曲 id）");
        }
    }

    private static String text(String value, int maxLength) {
        String result = Objects.requireNonNullElse(value, "").trim();
        if (result.length() > maxLength) throw badRequest("内容超过长度限制：" + maxLength);
        return result;
    }
    private static void requireLanguage(String language) {
        if (language == null || !LANGUAGES.contains(language)) throw badRequest("仅支持 zh、en、ja");
    }
    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
