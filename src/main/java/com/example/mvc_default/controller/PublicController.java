package com.example.mvc_default.controller;

import com.example.mvc_default.entity.Category;
import com.example.mvc_default.entity.Product;
import com.example.mvc_default.entity.Review;
import com.example.mvc_default.repository.CategoryRepository;
import com.example.mvc_default.repository.ProductRepository;
import com.example.mvc_default.repository.ReviewRepository;
import com.example.mvc_default.service.FileStorageService;
import com.example.mvc_default.service.SitePageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class PublicController {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SitePageService sitePageService;
    private final FileStorageService fileStorageService;
    private final ReviewRepository reviewRepository;

    public PublicController(CategoryRepository categoryRepository, ProductRepository productRepository, SitePageService sitePageService, FileStorageService fileStorageService, ReviewRepository reviewRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.sitePageService = sitePageService;
        this.fileStorageService = fileStorageService;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/catalog";
    }

    @GetMapping("/catalog")
    public String catalog(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("catalogHeaderImage", sitePageService.getContent("catalogHeaderImage", ""));
        model.addAttribute("catalogHeaderImageMobile", sitePageService.getContent("catalogHeaderImageMobile", ""));
        model.addAttribute("weather", sitePageService.getContent("weather", "NONE"));
        return "catalog";
    }

    @GetMapping("/catalog/{categoryId}")
    public String categoryProducts(@PathVariable Long categoryId, Model model) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("category", category);
        model.addAttribute("products", productRepository.findByCategoryId(categoryId));
        return "category-products";
    }

    @GetMapping("/product/{slug}")
    public String product(@PathVariable String slug, Model model) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("product", product);
        return "product";
    }

    @GetMapping("/product/{slug}/gift")
    public String giftPage(@PathVariable String slug, Model model) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("product", product);
        model.addAttribute("giftUnlocked", false);
        return "gift";
    }

    @PostMapping("/product/{slug}/gift")
    public String submitGiftReview(
            @PathVariable String slug,
            @RequestParam("reviewPhoto") MultipartFile reviewPhoto,
            Model model
    ) throws Exception {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (reviewPhoto != null && !reviewPhoto.isEmpty()) {
            String fileName = fileStorageService.save(reviewPhoto, "gift-reviews");
            Review review = new Review();
            review.setProduct(product);
            review.setPhotoFileName(fileName);
            reviewRepository.save(review);
        }
        model.addAttribute("product", product);
        model.addAttribute("giftUnlocked", true);
        return "gift";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("content", sitePageService.getContent("about", "Добавьте информацию о бренде в админ-панели."));
        return "about";
    }

    @GetMapping("/contacts")
    public String contacts(Model model) {
        model.addAttribute("content", sitePageService.getContent("contacts", "Добавьте контакты в админ-панели."));
        return "contacts";
    }
}
