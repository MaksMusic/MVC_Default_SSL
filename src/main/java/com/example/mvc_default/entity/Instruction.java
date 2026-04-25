package com.example.mvc_default.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "instructions")
public class Instruction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstructionType type;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String contentText;

    private String fileName;

    private String externalUrl;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InstructionType getType() { return type; }
    public void setType(InstructionType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
