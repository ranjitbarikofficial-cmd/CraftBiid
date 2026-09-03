package com.craftbid.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/profile")
    public String profile(Authentication authentication) {

        String email = authentication.getName();

        return "Welcome " + email + "! JWT authentication is working.";
    }
}