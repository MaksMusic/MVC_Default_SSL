package com.example.mvc_default.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    public static final String PASSWORD_HASH_PAGE_KEY = "adminPasswordHash";

    private final SitePageService sitePageService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String bootstrapPlainPassword;

    private volatile String cachedBootstrapEncoded;

    public AdminUserDetailsService(SitePageService sitePageService, PasswordEncoder passwordEncoder) {
        this.sitePageService = sitePageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!adminUsername.equals(username)) {
            throw new UsernameNotFoundException(username);
        }
        return User.withUsername(adminUsername)
                .password(resolveEncodedPassword())
                .roles("ADMIN")
                .build();
    }

    private String resolveEncodedPassword() {
        String stored = sitePageService.getContent(PASSWORD_HASH_PAGE_KEY, "").trim();
        if (!stored.isEmpty()) {
            return stored;
        }
        synchronized (this) {
            if (cachedBootstrapEncoded == null) {
                cachedBootstrapEncoded = passwordEncoder.encode(bootstrapPlainPassword);
            }
            return cachedBootstrapEncoded;
        }
    }

    public boolean verifyPassword(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        String stored = sitePageService.getContent(PASSWORD_HASH_PAGE_KEY, "").trim();
        if (!stored.isEmpty()) {
            return passwordEncoder.matches(rawPassword, stored);
        }
        return bootstrapPlainPassword.equals(rawPassword);
    }

    public void savePasswordHash(String bcryptHash) {
        sitePageService.updateContent(PASSWORD_HASH_PAGE_KEY, bcryptHash);
    }
}
