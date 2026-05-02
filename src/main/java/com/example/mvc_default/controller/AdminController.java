package com.example.mvc_default.controller;

import com.example.mvc_default.entity.*;
import com.example.mvc_default.repository.CategoryRepository;
import com.example.mvc_default.repository.InstructionRepository;
import com.example.mvc_default.repository.ProductRepository;
import com.example.mvc_default.repository.ReviewRepository;
import com.example.mvc_default.service.AdminUserDetailsService;
import com.example.mvc_default.service.FileStorageService;
import com.example.mvc_default.service.QRCodeService;
import com.example.mvc_default.service.SitePageService;
import com.example.mvc_default.service.SlugService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InstructionRepository instructionRepository;
    private final ReviewRepository reviewRepository;
    private final FileStorageService fileStorageService;
    private final SitePageService sitePageService;
    private final QRCodeService qrCodeService;
    private final AdminUserDetailsService adminUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            InstructionRepository instructionRepository,
            ReviewRepository reviewRepository,
            FileStorageService fileStorageService,
            SitePageService sitePageService,
            QRCodeService qrCodeService,
            AdminUserDetailsService adminUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.instructionRepository = instructionRepository;
        this.reviewRepository = reviewRepository;
        this.fileStorageService = fileStorageService;
        this.sitePageService = sitePageService;
        this.qrCodeService = qrCodeService;
        this.adminUserDetailsService = adminUserDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String adminRoot() {
        return "redirect:/admin/catalogs";
    }

    @GetMapping("/catalogs")
    public String catalogs(Model model) {
        model.addAttribute("activeMenu", "catalogs");
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/catalogs";
    }

    @GetMapping("/catalog-header")
    public String catalogHeader(Model model) {
        model.addAttribute("activeMenu", "catalog-header");
        model.addAttribute("catalogHeaderImage", sitePageService.getContent("catalogHeaderImage", ""));
        model.addAttribute("catalogHeaderImageMobile", sitePageService.getContent("catalogHeaderImageMobile", ""));
        return "admin/catalog-header";
    }

    @PostMapping("/catalogs")
    public String createCatalog(
            @RequestParam String name,
            @RequestParam(required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes
    ) throws Exception {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Введите название каталога");
            return "redirect:/admin/catalogs";
        }
        if (categoryRepository.existsByNameIgnoreCase(normalized)) {
            redirectAttributes.addFlashAttribute("error", "Такой каталог уже есть");
            return "redirect:/admin/catalogs";
        }
        Category category = new Category();
        category.setName(normalized);
        if (imageFile != null && !imageFile.isEmpty()) {
            category.setImageFileName(fileStorageService.save(imageFile, "categories"));
        }
        try {
            categoryRepository.save(category);
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "Такой каталог уже есть");
            return "redirect:/admin/catalogs";
        }
        return "redirect:/admin/catalogs";
    }

    @PostMapping("/catalogs/{id}/rename")
    public String renameCatalog(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes
    ) throws Exception {
        Category category = categoryRepository.findById(id).orElseThrow();
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Введите название каталога");
            return "redirect:/admin/catalogs";
        }
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(normalized, id)) {
            redirectAttributes.addFlashAttribute("error", "Такой каталог уже есть");
            return "redirect:/admin/catalogs";
        }
        category.setName(normalized);
        if (imageFile != null && !imageFile.isEmpty()) {
            fileStorageService.deleteIfExists(category.getImageFileName());
            category.setImageFileName(fileStorageService.save(imageFile, "categories"));
        }
        try {
            categoryRepository.save(category);
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "Такой каталог уже есть");
            return "redirect:/admin/catalogs";
        }
        return "redirect:/admin/catalogs";
    }

    @PostMapping("/catalogs/{id}/image/delete")
    public String deleteCatalogImage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Каталог не найден");
            return "redirect:/admin/catalogs";
        }
        fileStorageService.deleteIfExists(category.getImageFileName());
        category.setImageFileName(null);
        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("success", "Картинка каталога удалена");
        return "redirect:/admin/catalogs";
    }

    @PostMapping("/catalogs/{id}/delete")
    public String deleteCatalog(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Каталог не найден");
            return "redirect:/admin/catalogs";
        }
        fileStorageService.deleteIfExists(category.getImageFileName());
        for (Product product : category.getProducts()) {
            fileStorageService.deleteIfExists(product.getImageFileName());
            fileStorageService.deleteIfExists(product.getVideoFileName());
            fileStorageService.deleteIfExists(product.getAudioFileName());
            reviewRepository.deleteByProductId(product.getId());
            for (Instruction instruction : product.getInstructions()) {
                fileStorageService.deleteIfExists(instruction.getFileName());
            }
        }
        categoryRepository.delete(category);
        return "redirect:/admin/catalogs";
    }

    @PostMapping("/catalog-header")
    public String updateCatalogHeader(
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) MultipartFile imageFileMobile,
            @RequestParam(required = false, defaultValue = "false") boolean removeCatalogHeaderImage,
            @RequestParam(required = false, defaultValue = "false") boolean removeCatalogHeaderImageMobile
    ) throws Exception {
        String current = sitePageService.getContent("catalogHeaderImage", "");
        String currentMobile = sitePageService.getContent("catalogHeaderImageMobile", "");
        if (removeCatalogHeaderImage) {
            fileStorageService.deleteIfExists(current);
            sitePageService.updateContent("catalogHeaderImage", "");
        }
        if (removeCatalogHeaderImageMobile) {
            fileStorageService.deleteIfExists(currentMobile);
            sitePageService.updateContent("catalogHeaderImageMobile", "");
        }
        if (imageFile != null && !imageFile.isEmpty()) {
            fileStorageService.deleteIfExists(current);
            sitePageService.updateContent("catalogHeaderImage", fileStorageService.save(imageFile, "catalog-header"));
        }
        if (imageFileMobile != null && !imageFileMobile.isEmpty()) {
            fileStorageService.deleteIfExists(currentMobile);
            sitePageService.updateContent("catalogHeaderImageMobile", fileStorageService.save(imageFileMobile, "catalog-header"));
        }
        return "redirect:/admin/catalog-header";
    }

    @PostMapping("/catalog-header/delete")
    public String deleteCatalogHeaderImage(@RequestParam(defaultValue = "web") String target) {
        if ("mobile".equalsIgnoreCase(target)) {
            String currentMobile = sitePageService.getContent("catalogHeaderImageMobile", "");
            fileStorageService.deleteIfExists(currentMobile);
            sitePageService.updateContent("catalogHeaderImageMobile", "");
        } else {
            String current = sitePageService.getContent("catalogHeaderImage", "");
            fileStorageService.deleteIfExists(current);
            sitePageService.updateContent("catalogHeaderImage", "");
        }
        return "redirect:/admin/catalog-header";
    }

    @GetMapping("/products")
    public String products(Model model, HttpServletRequest request) {
        model.addAttribute("activeMenu", "products");
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("baseUrl", resolvePublicBaseUrl(request));
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        model.addAttribute("activeMenu", "products");
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("instructionTypes", Arrays.asList(InstructionType.values()));
        model.addAttribute("product", new Product());
        model.addAttribute("editMode", false);
        return "admin/product-form";
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return "redirect:/admin/products?error=Товар+не+найден";
        }
        model.addAttribute("activeMenu", "products");
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("instructionTypes", Arrays.asList(InstructionType.values()));
        model.addAttribute("product", product);
        model.addAttribute("editMode", true);
        return "admin/product-form";
    }

    @PostMapping("/products")
    public String createProduct(
            @RequestParam String name,
            @RequestParam String shortDescription,
            @RequestParam(required = false) String wbProductUrl,
            @RequestParam(required = false) String instructionUrl,
            @RequestParam(required = false) String giftDescription,
            @RequestParam(required = false) String giftUrl,
            @RequestParam Long categoryId,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) MultipartFile videoFile,
            @RequestParam(required = false) MultipartFile audioFile
    ) throws Exception {
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        Product product = new Product();
        product.setName(name);
        product.setShortDescription(shortDescription);
        product.setWbProductUrl(wbProductUrl);
        product.setInstructionUrl(instructionUrl);
        product.setGiftDescription(giftDescription);
        product.setGiftUrl(giftUrl);
        product.setCategory(category);
        product.setSlug(uniqueSlug(name));
        applyMediaFiles(product, imageFile, videoFile, audioFile);
        productRepository.save(product);
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/update")
    public String updateProduct(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String shortDescription,
            @RequestParam(required = false) String wbProductUrl,
            @RequestParam(required = false) String instructionUrl,
            @RequestParam(required = false) String giftDescription,
            @RequestParam(required = false) String giftUrl,
            @RequestParam Long categoryId,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) MultipartFile videoFile,
            @RequestParam(required = false) MultipartFile audioFile,
            @RequestParam(required = false, defaultValue = "false") boolean removeImage,
            @RequestParam(required = false, defaultValue = "false") boolean removeVideo,
            @RequestParam(required = false, defaultValue = "false") boolean removeAudio,
            @RequestParam(required = false, defaultValue = "false") boolean removeInstructionFiles
    ) throws Exception {
        Product product = productRepository.findById(id).orElseThrow();
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        product.setName(name);
        product.setShortDescription(shortDescription);
        product.setWbProductUrl(wbProductUrl);
        product.setInstructionUrl(instructionUrl);
        product.setGiftDescription(giftDescription);
        product.setGiftUrl(giftUrl);
        product.setCategory(category);

        if (removeImage) {
            fileStorageService.deleteIfExists(product.getImageFileName());
            product.setImageFileName(null);
        }
        if (removeVideo) {
            fileStorageService.deleteIfExists(product.getVideoFileName());
            product.setVideoFileName(null);
        }
        if (removeAudio) {
            fileStorageService.deleteIfExists(product.getAudioFileName());
            product.setAudioFileName(null);
        }
        if (removeInstructionFiles) {
            product.getInstructions().removeIf(instruction -> {
                if (instruction.getFileName() != null && !instruction.getFileName().isBlank()) {
                    fileStorageService.deleteIfExists(instruction.getFileName());
                    return true;
                }
                return false;
            });
        }

        applyMediaFiles(product, imageFile, videoFile, audioFile);
        productRepository.save(product);
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/image/delete")
    public String deleteProductImage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Товар не найден");
            return "redirect:/admin/products";
        }
        fileStorageService.deleteIfExists(product.getImageFileName());
        product.setImageFileName(null);
        productRepository.save(product);
        redirectAttributes.addFlashAttribute("success", "Картинка товара удалена");
        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/products/{id}/delete")
    @Transactional
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Товар не найден");
            return "redirect:/admin/products";
        }

        // Remove media files first so file storage remains clean.
        fileStorageService.deleteIfExists(product.getImageFileName());
        fileStorageService.deleteIfExists(product.getVideoFileName());
        fileStorageService.deleteIfExists(product.getAudioFileName());
        for (Instruction instruction : product.getInstructions()) {
            fileStorageService.deleteIfExists(instruction.getFileName());
        }

        // Delete child instructions explicitly, then hard-delete product by id.
        reviewRepository.deleteByProductId(id);
        instructionRepository.deleteByProductId(id);
        int deletedProducts = productRepository.deleteHardById(id);
        if (deletedProducts > 0) {
            redirectAttributes.addFlashAttribute("success", "Товар удален");
        } else {
            redirectAttributes.addFlashAttribute("error", "Не удалось удалить товар");
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/instructions")
    public String addInstruction(
            @PathVariable Long id,
            @RequestParam InstructionType type,
            @RequestParam String title,
            @RequestParam(required = false) String contentText,
            @RequestParam(required = false) String externalUrl,
            @RequestParam(required = false) MultipartFile file
    ) throws Exception {
        Product product = productRepository.findById(id).orElseThrow();
        Instruction instruction = new Instruction();
        instruction.setProduct(product);
        instruction.setType(type);
        instruction.setTitle(title);
        instruction.setContentText(contentText);
        instruction.setExternalUrl(externalUrl);
        if (file != null && !file.isEmpty()) {
            instruction.setFileName(fileStorageService.save(file, "instructions"));
        }
        instructionRepository.save(instruction);
        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/instructions/{id}/delete")
    public String deleteInstruction(@PathVariable Long id) {
        Instruction instruction = instructionRepository.findById(id).orElseThrow();
        Long productId = instruction.getProduct().getId();
        instructionRepository.deleteById(id);
        return "redirect:/admin/products/" + productId + "/edit";
    }

    @GetMapping("/reviews")
    public String reviews(Model model) {
        model.addAttribute("activeMenu", "reviews");
        model.addAttribute("reviews", reviewRepository.findAllByOrderByCreatedAtDesc());
        return "admin/reviews";
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            redirectAttributes.addFlashAttribute("error", "Отзыв не найден");
            return "redirect:/admin/reviews";
        }
        fileStorageService.deleteIfExists(review.getPhotoFileName());
        reviewRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Отзыв удален");
        return "redirect:/admin/reviews";
    }

    @GetMapping("/contacts")
    public String contacts(Model model) {
        model.addAttribute("activeMenu", "contacts");
        model.addAttribute("content", sitePageService.getContent("contacts", ""));
        return "admin/contacts";
    }

    @PostMapping("/contacts")
    public String updateContacts(@RequestParam String content) {
        sitePageService.updateContent("contacts", content);
        return "redirect:/admin/contacts";
    }

    @GetMapping("/brand")
    public String brand(Model model) {
        model.addAttribute("activeMenu", "brand");
        model.addAttribute("content", sitePageService.getContent("about", ""));
        return "admin/brand";
    }

    @PostMapping("/brand")
    public String updateBrand(@RequestParam String content) {
        sitePageService.updateContent("about", content);
        return "redirect:/admin/brand";
    }

    @GetMapping("/password")
    public String passwordForm(Model model) {
        model.addAttribute("activeMenu", "password");
        return "admin/password";
    }

    @PostMapping("/password")
    public String changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {
        if (oldPassword == null || oldPassword.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Введите текущий пароль");
            return "redirect:/admin/password";
        }
        if (newPassword == null || newPassword.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Введите новый пароль");
            return "redirect:/admin/password";
        }
        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Новый пароль — не короче 8 символов");
            return "redirect:/admin/password";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Новый пароль и подтверждение не совпадают");
            return "redirect:/admin/password";
        }
        if (newPassword.equals(oldPassword)) {
            redirectAttributes.addFlashAttribute("error", "Новый пароль должен отличаться от текущего");
            return "redirect:/admin/password";
        }
        if (!adminUserDetailsService.verifyPassword(oldPassword)) {
            redirectAttributes.addFlashAttribute("error", "Неверный текущий пароль");
            return "redirect:/admin/password";
        }
        adminUserDetailsService.savePasswordHash(passwordEncoder.encode(newPassword));
        redirectAttributes.addFlashAttribute("success", "Пароль изменён. При следующем входе используйте новый пароль.");
        return "redirect:/admin/password";
    }

    @GetMapping("/weather")
    public String weather(Model model) {
        model.addAttribute("activeMenu", "weather");
        model.addAttribute("weather", normalizeWeather(sitePageService.getContent("weather", "NONE")));
        return "admin/weather";
    }

    @PostMapping("/weather")
    public String updateWeather(@RequestParam String weather) {
        sitePageService.updateContent("weather", normalizeWeather(weather));
        return "redirect:/admin/weather";
    }

    @ModelAttribute("qrService")
    public QRCodeService qrService() {
        return qrCodeService;
    }

    private void applyMediaFiles(Product product, MultipartFile imageFile, MultipartFile videoFile, MultipartFile audioFile) throws Exception {
        if (imageFile != null && !imageFile.isEmpty()) {
            fileStorageService.deleteIfExists(product.getImageFileName());
            product.setImageFileName(fileStorageService.save(imageFile, "images"));
        }
        if (videoFile != null && !videoFile.isEmpty()) {
            fileStorageService.deleteIfExists(product.getVideoFileName());
            product.setVideoFileName(fileStorageService.save(videoFile, "videos"));
        }
        if (audioFile != null && !audioFile.isEmpty()) {
            fileStorageService.deleteIfExists(product.getAudioFileName());
            product.setAudioFileName(fileStorageService.save(audioFile, "audio"));
        }
    }

    private String uniqueSlug(String name) {
        String base = SlugService.toSlug(name);
        if (productRepository.findBySlug(base).isEmpty()) {
            return base;
        }
        return base + "-" + System.currentTimeMillis();
    }

    private String normalizeWeather(String weather) {
        if (weather == null) {
            return "NONE";
        }
        String value = weather.trim().toUpperCase();
        return switch (value) {
            case "WINTER", "AUTUMN", "SUMMER", "NONE" -> value;
            default -> "NONE";
        };
    }

    private String resolvePublicBaseUrl(HttpServletRequest request) {
        String forwardedProto = firstHeaderValue(request.getHeader("X-Forwarded-Proto"));
        String forwardedHost = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
        String hostHeader = firstHeaderValue(request.getHeader("Host"));

        String scheme = (forwardedProto == null || forwardedProto.isBlank()) ? "https" : forwardedProto;
        String host = (forwardedHost == null || forwardedHost.isBlank()) ? hostHeader : forwardedHost;
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        return scheme + "://" + host;
    }

    private String firstHeaderValue(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        return headerValue.split(",")[0].trim();
    }
}
