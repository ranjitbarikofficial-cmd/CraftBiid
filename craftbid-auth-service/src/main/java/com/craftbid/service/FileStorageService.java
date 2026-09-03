package com.craftbid.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(
            ".mp4", ".webm", ".mov", ".mkv", ".m4v"
    );

    public FileStorageService(
            @Value("${file.upload-dir:uploads}") String uploadDir) {

        this.uploadDir = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.uploadDir.resolve("crafts"));
            Files.createDirectories(this.uploadDir.resolve("reels"));
        } catch (IOException e) {
            System.err.println("Could not initialize upload directories: " + e.getMessage());
        }
    }

    public String saveFile(
            MultipartFile file,
            String folder) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Sanitize folder to avoid path traversal
        String cleanFolder = folder.replace("..", "").replace("/", "").replace("\\", "");

        Path folderPath = uploadDir.resolve(cleanFolder).normalize();
        Files.createDirectories(folderPath);

        String originalName = file.getOriginalFilename();
        String extension = "";

        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }

        // Validate extension based on folder
        if (cleanFolder.contains("craft") || cleanFolder.contains("image")) {
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension) && file.getContentType() != null && !file.getContentType().startsWith("image/")) {
                throw new IllegalArgumentException("Invalid image file type. Allowed: JPG, PNG, WEBP, GIF");
            }
            if (extension.isEmpty()) {
                extension = ".jpg";
            }
        } else if (cleanFolder.contains("reel") || cleanFolder.contains("video")) {
            if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension) && file.getContentType() != null && !file.getContentType().startsWith("video/")) {
                throw new IllegalArgumentException("Invalid video file type. Allowed: MP4, WEBM, MOV");
            }
            if (extension.isEmpty()) {
                extension = ".mp4";
            }
        }

        String fileName = UUID.randomUUID() + extension;
        Path target = folderPath.resolve(fileName).normalize();

        Files.copy(
                file.getInputStream(),
                target,
                StandardCopyOption.REPLACE_EXISTING
        );

        return "/uploads/" + cleanFolder + "/" + fileName;
    }
}