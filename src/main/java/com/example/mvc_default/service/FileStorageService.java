package com.example.mvc_default.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path uploadDir;

    public FileStorageService(@Value("${app.storage.upload-dir:uploads}") String uploadDir) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public String save(MultipartFile file) throws IOException {
        return save(file, "");
    }

    public String save(MultipartFile file, String subDir) throws IOException {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            extension = originalName.substring(dot);
        }
        String storedName = UUID.randomUUID() + extension;
        String normalizedSubDir = (subDir == null ? "" : subDir.trim()).replace("\\", "/");
        Path targetDir = normalizedSubDir.isEmpty() ? uploadDir : uploadDir.resolve(normalizedSubDir);
        Files.createDirectories(targetDir);
        Files.copy(file.getInputStream(), targetDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        return normalizedSubDir.isEmpty() ? storedName : normalizedSubDir + "/" + storedName;
    }

    public Resource loadAsResource(String fileName) throws IOException {
        Path filePath = uploadDir.resolve(fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            throw new IOException("File not found");
        }
        return resource;
    }
}
