package com.kaede.portfoliobackend.service;

import com.kaede.portfoliobackend.entity.PremiumListener;
import com.kaede.portfoliobackend.entity.User;
import com.kaede.portfoliobackend.repository.PremiumListenerRepository;
import com.kaede.portfoliobackend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PremiumAudienceService {
    public record Access(boolean allowed, boolean admin) {}
    public record ListenerView(Long userId, String username, LocalDateTime grantedAt) {}
    public record UserView(Long userId, String username, boolean authorized, boolean admin) {}
    public record ListenerInput(Long userId, String username) {}

    private final UserRepository users;
    private final PremiumListenerRepository listeners;

    public PremiumAudienceService(UserRepository users, PremiumListenerRepository listeners) {
        this.users = users;
        this.listeners = listeners;
    }

    public Access access(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return new Access(false, false);
        }
        boolean admin = authentication.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));
        if (admin) return new Access(true, true);
        boolean allowed = users.findByUsername(authentication.getName())
                .map(user -> listeners.existsById(user.getId())).orElse(false);
        return new Access(allowed, false);
    }

    public void requireAccess(Authentication authentication) {
        if (!access(authentication).allowed()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "尚未获得私密音频列表的访问权限");
    }

    public List<ListenerView> listListeners() {
        return listeners.findAllByOrderByGrantedAtDescUserIdAsc().stream().map(this::view).toList();
    }

    public List<UserView> searchUsers(String query) {
        String value = query == null ? "" : query.trim();
        if (value.isEmpty()) return List.of();
        if (value.length() > 255) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名过长");
        return users.findByUsernameContainingIgnoreCaseOrderByUsernameAscIdAsc(value, PageRequest.of(0, 20))
                .stream().map(user -> {
                    boolean admin = isAdministrator(user);
                    return new UserView(user.getId(), user.getUsername(), admin || listeners.existsById(user.getId()), admin);
                }).toList();
    }

    @Transactional
    public ListenerView grant(ListenerInput input) {
        if (input == null || (input.userId() == null && (input.username() == null || input.username().isBlank()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请指定一个已注册账号");
        }
        User user;
        if (input.userId() != null) {
            if (input.userId() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号无效");
            user = users.findById(input.userId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "账号不存在"));
        } else {
            String name = input.username().trim();
            if (name.length() > 255) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名过长");
            user = users.findByUsername(name).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "账号不存在"));
        }
        if (isAdministrator(user)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员默认拥有访问权限，无需添加");
        PremiumListener listener = listeners.findById(user.getId()).orElseGet(() -> {
            var value = new PremiumListener();
            value.setUser(user);
            return listeners.saveAndFlush(value);
        });
        return view(listener);
    }

    @Transactional
    public void revoke(Long userId) {
        // Repeated removals are harmless; the account and its role are never modified.
        listeners.findById(userId).ifPresent(listeners::delete);
        listeners.flush();
    }

    private ListenerView view(PremiumListener listener) {
        return new ListenerView(listener.getUserId(), listener.getUser().getUsername(), listener.getGrantedAt());
    }

    private boolean isAdministrator(User user) {
        return "ROLE_ADMIN".equals(user.getRole()) || "ADMIN".equals(user.getRole());
    }
}
