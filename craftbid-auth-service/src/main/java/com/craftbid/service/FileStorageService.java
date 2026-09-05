package com.craftbid.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadDir;

    public static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024; // 10 MB
    public static final long MAX_VIDEO_SIZE = 100L * 1024 * 1024; // 100 MB
    public static final int MIN_FILE_SIZE = 12; // bytes

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(
            ".mp4", ".webm", ".mov", ".mkv", ".m4v"
    );

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            ".jsp", ".jspx", ".class", ".jar", ".war", ".exe", ".dll",
            ".sh", ".bat", ".cmd", ".py", ".php", ".phtml", ".html",
            ".htm", ".svg", ".xhtml", ".js", ".ts", ".vbs", ".ps1",
            ".cgi", ".pl", ".asp", ".aspx", ".htaccess"
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
            logger.error("Could not initialize upload directories: ", e);
        }
    }

    public String saveFile(
            MultipartFile file,
            String folder) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        long fileSize = file.getSize();
        if (fileSize < MIN_FILE_SIZE) {
            throw new IllegalArgumentException("File is corrupted or too small");
        }

        // Sanitize folder to strictly alphanumeric/underscores/hyphens to avoid path traversal
        String cleanFolder = folder.replaceAll("[^a-zA-Z0-9_-]", "");
        if (cleanFolder.isBlank()) {
            cleanFolder = "crafts";
        }

        Path folderPath = uploadDir.resolve(cleanFolder).normalize();
        if (!folderPath.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid upload directory path");
        }
        Files.createDirectories(folderPath);

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }

        // Reject dangerous / executable extensions immediately
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            logger.warn("Blocked upload attempt with dangerous extension: {}", extension);
            throw new IllegalArgumentException("Executable or script file extensions are strictly prohibited");
        }

        // Read initial header bytes for magic bytes verification
        byte[] headerBytes = new byte[Math.min((int) fileSize, 512)];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(headerBytes);
            if (read < MIN_FILE_SIZE) {
                throw new IllegalArgumentException("Unable to inspect file header");
            }
        }

        // Reject files containing script / HTML / executable headers
        if (containsScriptOrExecutableSignatures(headerBytes)) {
            logger.warn("Blocked upload attempt with script or binary executable content signature");
            throw new IllegalArgumentException("File content contains forbidden executable or script signatures");
        }

        String determinedExtension;

        // Validate and determine extension based on target media type
        if (cleanFolder.contains("craft") || cleanFolder.contains("image")) {
            if (fileSize > MAX_IMAGE_SIZE) {
                throw new IllegalArgumentException("Image file size exceeds the 10 MB limit");
            }
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension) && !extension.isEmpty()) {
                throw new IllegalArgumentException("Invalid image file extension. Allowed: JPG, PNG, WEBP, GIF");
            }
            determinedExtension = validateImageMagicBytes(headerBytes, extension);
        } else if (cleanFolder.contains("reel") || cleanFolder.contains("video")) {
            if (fileSize > MAX_VIDEO_SIZE) {
                throw new IllegalArgumentException("Video file size exceeds the 100 MB limit");
            }
            if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension) && !extension.isEmpty()) {
                throw new IllegalArgumentException("Invalid video file extension. Allowed: MP4, WEBM, MOV, MKV, M4V");
            }
            determinedExtension = validateVideoMagicBytes(headerBytes, extension);
        } else {
            throw new IllegalArgumentException("Unsupported upload folder destination");
        }

        // Generate safe randomized UUID filename with determined safe extension
        String fileName = UUID.randomUUID().toString() + determinedExtension;
        Path target = folderPath.resolve(fileName).normalize();

        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid target file path");
        }

        Files.copy(
                file.getInputStream(),
                target,
                StandardCopyOption.REPLACE_EXISTING
        );

        logger.info("Successfully stored verified upload file: /uploads/{}/{}", cleanFolder, fileName);
        return "/uploads/" + cleanFolder + "/" + fileName;
    }

    private boolean containsScriptOrExecutableSignatures(byte[] bytes) {
        String headerText = new String(bytes, StandardCharsets.ISO_8859_1).toLowerCase();
        if (headerText.contains("<?php")
                || headerText.contains("<script")
                || headerText.contains("<html")
                || headerText.contains("<!doctype")
                || headerText.contains("<svg")
                || headerText.contains("eval(")
                || headerText.contains("exec(")) {
            return true;
        }
        // Check ELF binary header (\x7FELF)
        if (bytes.length >= 4 && bytes[0] == 0x7F && bytes[1] == 'E' && bytes[2] == 'L' && bytes[3] == 'F') {
            return true;
        }
        // Check DOS/Windows MZ executable header
        if (bytes.length >= 2 && bytes[0] == 'M' && bytes[1] == 'Z') {
            return true;
        }
        // Check Shell script (#!/bin/ or #!/usr/)
        if (bytes.length >= 2 && bytes[0] == '#' && bytes[1] == '!') {
            return true;
        }
        return false;
    }

    private String validateImageMagicBytes(byte[] header, String requestedExt) {
        // JPEG: FF D8 FF
        if (header.length >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return requestedExt.equals(".jpeg") ? ".jpeg" : ".jpg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (header.length >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
            return ".png";
        }
        // GIF: GIF87a or GIF89a (47 49 46 38)
        if (header.length >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8') {
            return ".gif";
        }
        // WEBP: RIFF....WEBP (52 49 46 46 .... 57 45 42 50)
        if (header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return ".webp";
        }

        throw new IllegalArgumentException("Invalid image content. The uploaded file does not match any allowed image format (JPEG, PNG, WEBP, GIF).");
    }

    private String validateVideoMagicBytes(byte[] header, String requestedExt) {
        // MP4 / M4V / MOV (QuickTime / ISO Base Media File):
        // Bytes 4-7 = 'ftyp' (66 74 79 70) or 'moov' (6D 6F 6F 76) or 'mdat' (6D 64 61 74)
        if (header.length >= 8) {
            String box = new String(Arrays.copyOfRange(header, 4, 8), StandardCharsets.ISO_8859_1);
            if ("ftyp".equals(box) || "moov".equals(box) || "mdat".equals(box) || "wide".equals(box) || "skip".equals(box)) {
                if (requestedExt.equals(".mov")) return ".mov";
                if (requestedExt.equals(".m4v")) return ".m4v";
                return ".mp4";
            }
        }
        // WEBM / MKV: Matroska / EBML header (1A 45 DF A3)
        if (header.length >= 4 && (header[0] & 0xFF) == 0x1A && (header[1] & 0xFF) == 0x45 && (header[2] & 0xFF) == 0xDF && (header[3] & 0xFF) == 0xA3) {
            if (requestedExt.equals(".mkv")) return ".mkv";
            return ".webm";
        }

        throw new IllegalArgumentException("Invalid video content. The uploaded file does not match any allowed video format (MP4, WEBM, MOV, MKV).");
    }
}