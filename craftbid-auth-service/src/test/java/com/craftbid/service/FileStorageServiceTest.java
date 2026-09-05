package com.craftbid.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileStorageService Upload Security Tests")
public class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("Should accept and store valid JPEG image with verified magic bytes")
    void shouldAcceptValidJpeg() throws IOException {
        // Valid JPEG header: FF D8 FF E0 00 10 4A 46 49 46 00 01 (12+ bytes)
        byte[] validJpeg = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                0x01, 0x01, 0x00, 0x60
        };

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "handcrafted_vase.jpg",
                "image/jpeg",
                validJpeg
        );

        String savedPath = fileStorageService.saveFile(file, "crafts");

        assertNotNull(savedPath);
        assertTrue(savedPath.startsWith("/uploads/crafts/"));
        assertTrue(savedPath.endsWith(".jpg"));
        assertFalse(savedPath.contains("handcrafted_vase")); // sanitized to random UUID
    }

    @Test
    @DisplayName("Should accept and store valid PNG image with verified magic bytes")
    void shouldAcceptValidPng() throws IOException {
        // Valid PNG header: 89 50 4E 47 0D 0A 1A 0A + chunk data
        byte[] validPng = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "painting.png",
                "image/png",
                validPng
        );

        String savedPath = fileStorageService.saveFile(file, "crafts");

        assertNotNull(savedPath);
        assertTrue(savedPath.startsWith("/uploads/crafts/"));
        assertTrue(savedPath.endsWith(".png"));
    }

    @Test
    @DisplayName("Should accept and store valid MP4 video with ftyp box")
    void shouldAcceptValidMp4() throws IOException {
        // Valid MP4 header: 4-byte box size + 'ftyp' (66 74 79 70) + 'isom'
        byte[] validMp4 = new byte[]{
                0x00, 0x00, 0x00, 0x18,
                'f', 't', 'y', 'p',
                'i', 's', 'o', 'm',
                0x00, 0x00, 0x02, 0x00
        };

        MockMultipartFile file = new MockMultipartFile(
                "video",
                "pottery_making.mp4",
                "video/mp4",
                validMp4
        );

        String savedPath = fileStorageService.saveFile(file, "reels");

        assertNotNull(savedPath);
        assertTrue(savedPath.startsWith("/uploads/reels/"));
        assertTrue(savedPath.endsWith(".mp4"));
    }

    @Test
    @DisplayName("Should reject dangerous executable file extensions (PHP, JSP, EXE, SH)")
    void shouldRejectDangerousExtensions() {
        byte[] dummyData = "This is a dummy test script".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile phpFile = new MockMultipartFile("file", "shell.php", "application/x-php", dummyData);
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.saveFile(phpFile, "crafts"));

        MockMultipartFile jspFile = new MockMultipartFile("file", "exploit.jsp", "text/plain", dummyData);
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.saveFile(jspFile, "crafts"));

        MockMultipartFile exeFile = new MockMultipartFile("file", "trojan.exe", "application/octet-stream", dummyData);
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.saveFile(exeFile, "crafts"));

        MockMultipartFile svgFile = new MockMultipartFile("file", "xss.svg", "image/svg+xml", dummyData);
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.saveFile(svgFile, "crafts"));
    }

    @Test
    @DisplayName("Should reject spoofed extension with embedded PHP/HTML/Script content")
    void shouldRejectSpoofedScriptContent() {
        byte[] phpPayload = "<?php system($_GET['cmd']); ?>".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "innocent_photo.jpg",
                "image/jpeg",
                phpPayload
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveFile(file, "crafts")
        );

        assertTrue(ex.getMessage().contains("forbidden executable or script") || ex.getMessage().contains("does not match any allowed image format"));
    }

    @Test
    @DisplayName("Should reject spoofed extension with ELF binary header")
    void shouldRejectElfBinaryHeader() {
        byte[] elfPayload = new byte[]{
                0x7F, 'E', 'L', 'F',
                0x02, 0x01, 0x01, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
        };

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "photo.png",
                "image/png",
                elfPayload
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveFile(file, "crafts")
        );
    }

    @Test
    @DisplayName("Should reject empty or truncated files below 12 bytes")
    void shouldRejectTruncatedFiles() {
        byte[] tiny = new byte[]{0x01, 0x02, 0x03};

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "tiny.jpg",
                "image/jpeg",
                tiny
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveFile(file, "crafts")
        );
    }

    @Test
    @DisplayName("Should reject image uploads exceeding the 10 MB limit")
    void shouldRejectOversizedImage() {
        // Create an 11 MB fake JPEG stream
        byte[] largeData = new byte[11 * 1024 * 1024];
        largeData[0] = (byte) 0xFF;
        largeData[1] = (byte) 0xD8;
        largeData[2] = (byte) 0xFF;
        largeData[3] = (byte) 0xE0;

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "large.jpg",
                "image/jpeg",
                largeData
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveFile(file, "crafts")
        );

        assertTrue(ex.getMessage().contains("10 MB"));
    }

    @Test
    @DisplayName("Should sanitize path traversal in folder name and prevent escaping upload root")
    void shouldSanitizePathTraversal() throws IOException {
        byte[] validJpeg = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                0x01, 0x01, 0x00, 0x60
        };

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                validJpeg
        );

        // Attempt path traversal like "../../etc"
        String savedPath = fileStorageService.saveFile(file, "../../crafts/sub");

        assertNotNull(savedPath);
        assertTrue(savedPath.startsWith("/uploads/craftssub/"));
        assertFalse(savedPath.contains(".."));
    }
}
