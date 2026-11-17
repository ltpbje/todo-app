package com.todo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    /** 仅允许图片——该接口位于 JWT 过滤器之后，按用户 token 鉴权 */
    private static final Set<String> ALLOWED = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final long MAX_SIZE = 10 * 1024 * 1024;

    private final Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads");

    @PostMapping
    public Map<String, String> upload(@RequestPart("file") MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = original == null ? "" : original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only png/jpg/jpeg/gif/webp allowed");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large (max 10MB)");
        }
        try {
            Files.createDirectories(uploadDir);
            String name = UUID.randomUUID() + "." + ext;
            file.transferTo(uploadDir.resolve(name).toAbsolutePath());
            return Map.of("url", "/uploads/" + name);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Save failed");
        }
    }
}
