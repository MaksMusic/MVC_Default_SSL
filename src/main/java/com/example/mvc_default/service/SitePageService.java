package com.example.mvc_default.service;

import com.example.mvc_default.entity.SitePage;
import com.example.mvc_default.repository.SitePageRepository;
import org.springframework.stereotype.Service;

@Service
public class SitePageService {
    private final SitePageRepository sitePageRepository;

    public SitePageService(SitePageRepository sitePageRepository) {
        this.sitePageRepository = sitePageRepository;
    }

    public String getContent(String key, String fallback) {
        return sitePageRepository.findByPageKey(key).map(SitePage::getContent).orElse(fallback);
    }

    public void updateContent(String key, String content) {
        SitePage page = sitePageRepository.findByPageKey(key).orElseGet(SitePage::new);
        page.setPageKey(key);
        page.setContent(content);
        sitePageRepository.save(page);
    }
}
