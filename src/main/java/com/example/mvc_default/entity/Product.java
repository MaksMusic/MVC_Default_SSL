package com.example.mvc_default.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 2000)
    private String shortDescription;

    private String imageFileName;

    private String videoFileName;

    private String audioFileName;

    @Column(length = 1000)
    private String wbProductUrl;

    @Column(length = 2000)
    private String instructionUrl;

    @Column(length = 2000)
    private String giftUrl;

    @Column(length = 2000)
    private String lunarCalendarUrl;

    @Column(length = 2000)
    private String giftDescription;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Instruction> instructions = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public String getImageFileName() { return imageFileName; }
    public void setImageFileName(String imageFileName) { this.imageFileName = imageFileName; }
    public String getVideoFileName() { return videoFileName; }
    public void setVideoFileName(String videoFileName) { this.videoFileName = videoFileName; }
    public String getAudioFileName() { return audioFileName; }
    public void setAudioFileName(String audioFileName) { this.audioFileName = audioFileName; }
    public String getWbProductUrl() { return wbProductUrl; }
    public void setWbProductUrl(String wbProductUrl) { this.wbProductUrl = wbProductUrl; }
    public String getInstructionUrl() { return instructionUrl; }
    public void setInstructionUrl(String instructionUrl) { this.instructionUrl = instructionUrl; }
    public String getGiftUrl() { return giftUrl; }
    public void setGiftUrl(String giftUrl) { this.giftUrl = giftUrl; }
    public String getLunarCalendarUrl() { return lunarCalendarUrl; }
    public void setLunarCalendarUrl(String lunarCalendarUrl) { this.lunarCalendarUrl = lunarCalendarUrl; }
    public String getGiftDescription() { return giftDescription; }
    public void setGiftDescription(String giftDescription) { this.giftDescription = giftDescription; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public List<Instruction> getInstructions() { return instructions; }
    public void setInstructions(List<Instruction> instructions) { this.instructions = instructions; }
}
