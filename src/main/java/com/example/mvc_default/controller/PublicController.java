package com.example.mvc_default.controller;

import com.example.mvc_default.entity.Category;
import com.example.mvc_default.entity.Product;
import com.example.mvc_default.repository.CategoryRepository;
import com.example.mvc_default.repository.ProductRepository;
import com.example.mvc_default.service.SitePageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class PublicController {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SitePageService sitePageService;

    public PublicController(CategoryRepository categoryRepository, ProductRepository productRepository, SitePageService sitePageService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.sitePageService = sitePageService;
    }

    @GetMapping("/")
    public String home(Model model) {
        String weather = sitePageService.getContent("weather", "NONE");
        model.addAttribute("weather", weather == null ? "NONE" : weather);
        return "index";
    }

    @GetMapping("/catalog")
    public String catalog(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
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
