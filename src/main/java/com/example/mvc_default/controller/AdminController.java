package com.example.mvc_default.controller;

import com.example.mvc_default.entity.*;
import com.example.mvc_default.repository.CategoryRepository;
import com.example.mvc_default.repository.InstructionRepository;
import com.example.mvc_default.repository.ProductRepository;
import com.example.mvc_default.service.FileStorageService;
import com.example.mvc_default.service.QRCodeService;
import com.example.mvc_default.service.SitePageService;
import com.example.mvc_default.service.SlugService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InstructionRepository instructionRepository;
    private final FileStorageService fileStorageService;
    private final SitePageService sitePageService;
    private final QRCodeService qrCodeService;

    public AdminController(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            InstructionRepository instructionRepository,
            FileStorageService fileStorageService,
            SitePageService sitePageService,
            QRCodeService qrCodeService
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.instructionRepository = instructionRepository;
        this.fileStorageService = fileStorageService;
        this.sitePageService = sitePageService;
        this.qrCodeService = qrCodeService;
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

    @PostMapping("/catalogs")
    public String createCatalog(@RequestParam String name) {
        Category category = new Category();
        category.setName(name);
        categoryRepository.save(category);
        return "redirect:/admin/catalogs";
    }

    @PostMapping("/catalogs/{id}/rename")
    public String renameCatalog(@PathVariable Long id, @RequestParam String name) {
        Category category = categoryRepository.findById(id).orElseThrow();
        category.setName(name);
        categoryRepository.save(category);
        return "redirect:/admin/catalogs";
    }

    @PostMapping("/catalogs/{id}/delete")
    public String deleteCatalog(@PathVariable Long id) {
        categoryRepository.deleteById(id);
        return "redirect:/admin/catalogs";
    }

    @GetMapping("/products")
    public String products(Model model, HttpServletRequest request) {
        model.addAttribute("activeMenu", "products");
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("baseUrl", request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort());
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
        Product product = productRepository.findById(id).orElseThrow();
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
            @RequestParam Long categoryId,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) MultipartFile videoFile,
            @RequestParam(required = false) MultipartFile audioFile,
            @RequestParam(required = false) String instructionTitle,
            @RequestParam(required = false) MultipartFile instructionFile
    ) throws Exception {
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        Product product = new Product();
        product.setName(name);
        product.setShortDescription(shortDescription);
        product.setCategory(category);
        product.setSlug(uniqueSlug(name));
        applyMediaFiles(product, imageFile, videoFile, audioFile);
        productRepository.save(product);

        createInstructionIfProvided(product, instructionTitle, instructionFile);
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/update")
    public String updateProduct(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String shortDescription,
            @RequestParam Long categoryId,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) MultipartFile videoFile,
            @RequestParam(required = false) MultipartFile audioFile,
            @RequestParam(required = false) String instructionTitle,
            @RequestParam(required = false) MultipartFile instructionFile
    ) throws Exception {
        Product product = productRepository.findById(id).orElseThrow();
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        product.setName(name);
        product.setShortDescription(shortDescription);
        product.setCategory(category);
        applyMediaFiles(product, imageFile, videoFile, audioFile);
        productRepository.save(product);

        createInstructionIfProvided(product, instructionTitle, instructionFile);
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
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

    @ModelAttribute("qrService")
    public QRCodeService qrService() {
        return qrCodeService;
    }

    private void applyMediaFiles(Product product, MultipartFile imageFile, MultipartFile videoFile, MultipartFile audioFile) throws Exception {
        if (imageFile != null && !imageFile.isEmpty()) {
            product.setImageFileName(fileStorageService.save(imageFile, "images"));
        }
        if (videoFile != null && !videoFile.isEmpty()) {
            product.setVideoFileName(fileStorageService.save(videoFile, "videos"));
        }
        if (audioFile != null && !audioFile.isEmpty()) {
            product.setAudioFileName(fileStorageService.save(audioFile, "audio"));
        }
    }

    private void createInstructionIfProvided(Product product, String instructionTitle, MultipartFile instructionFile) throws Exception {
        if (instructionFile == null || instructionFile.isEmpty()) {
            return;
        }
        Instruction instruction = new Instruction();
        instruction.setProduct(product);
        instruction.setType(InstructionType.PDF);
        instruction.setTitle((instructionTitle == null || instructionTitle.isBlank()) ? "Инструкция" : instructionTitle);
        instruction.setFileName(fileStorageService.save(instructionFile, "instructions"));
        instructionRepository.save(instruction);
    }

    private String uniqueSlug(String name) {
        String base = SlugService.toSlug(name);
        if (productRepository.findBySlug(base).isEmpty()) {
            return base;
        }
        return base + "-" + System.currentTimeMillis();
    }
}
