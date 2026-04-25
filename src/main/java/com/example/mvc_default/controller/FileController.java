package com.example.mvc_default.controller;

import com.example.mvc_default.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Files;
import java.nio.file.Path;

@Controller
public class FileController {
    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/files")
    public ResponseEntity<Resource> serveFile(
            @RequestParam String path,
            @RequestParam(defaultValue = "false") boolean download
    ) throws Exception {
        Resource resource = fileStorageService.loadAsResource(path);
        String contentDisposition = download
                ? "attachment; filename=\"" + resource.getFilename() + "\""
                : "inline; filename=\"" + resource.getFilename() + "\"";
        String type = Files.probeContentType(Path.of(resource.getFile().getAbsolutePath()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(type == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(type))
                .body(resource);
    }
}
