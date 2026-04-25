package com.example.mvc_default.service;

import java.util.Locale;

public final class SlugService {
    private SlugService() {}

    public static String toSlug(String source) {
        if (source == null || source.isBlank()) {
            return "product";
        }
        return source.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9а-яё\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }
}
