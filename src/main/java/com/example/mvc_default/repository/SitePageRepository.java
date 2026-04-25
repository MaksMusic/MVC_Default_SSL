package com.example.mvc_default.repository;

import com.example.mvc_default.entity.SitePage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SitePageRepository extends JpaRepository<SitePage, Long> {
    Optional<SitePage> findByPageKey(String pageKey);
}
