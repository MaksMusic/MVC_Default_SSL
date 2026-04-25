package com.example.mvc_default.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "site_pages")
public class SitePage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pageKey;

    @Column(length = 10000)
    private String content;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPageKey() { return pageKey; }
    public void setPageKey(String pageKey) { this.pageKey = pageKey; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
