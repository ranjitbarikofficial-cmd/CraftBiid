package com.craftbid.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "craftbid-auth-service",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/api/test/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "pong",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/api/test/secure")
    public String secureTest() {
        return "JWT authentication is working!";
    }
}