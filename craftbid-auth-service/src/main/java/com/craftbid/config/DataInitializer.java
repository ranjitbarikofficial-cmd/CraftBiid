package com.craftbid.config;

import com.craftbid.entity.*;
import com.craftbid.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ArtisanProfileRepository artisanProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            ArtisanProfileRepository artisanProfileRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.artisanProfileRepository = artisanProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            seedAdmin();
        } catch (Exception e) {
            System.out.println("ℹ️ Admin already exists or skipping admin seeding: " + e.getMessage());
        }

        try {
            seedArtisan();
        } catch (Exception e) {
            System.out.println("ℹ️ Artisan already exists or skipping artisan seeding: " + e.getMessage());
        }

        try {
            seedBuyer();
        } catch (Exception e) {
            System.out.println("ℹ️ Buyer already exists or skipping buyer seeding: " + e.getMessage());
        }
    }

    private void seedAdmin() {
        if (userRepository.findByEmail("admin@craftbid.in").isEmpty() &&
            userRepository.findByPhone("9999999999").isEmpty()) {

            User admin = new User();
            admin.setName("CraftBid Administrator");
            admin.setEmail("admin@craftbid.in");
            admin.setPhone("9999999999");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            admin.setSellerEnabled(true);
            userRepository.save(admin);
            System.out.println("✅ Seeded Admin: admin@craftbid.in / admin123");
        }
    }

    private void seedArtisan() {
        if (userRepository.findByEmail("artisan@craftbid.in").isEmpty() &&
            userRepository.findByPhone("9876543210").isEmpty()) {

            User artisanUser = new User();
            artisanUser.setName("Rajesh Sharma");
            artisanUser.setEmail("artisan@craftbid.in");
            artisanUser.setPhone("9876543210");
            artisanUser.setPassword(passwordEncoder.encode("artisan123"));
            artisanUser.setRole(Role.CUSTOMER);
            artisanUser.setActive(true);
            artisanUser.setSellerEnabled(true);
            User savedArtisan = userRepository.save(artisanUser);

            ArtisanProfile profile = new ArtisanProfile();
            profile.setUser(savedArtisan);
            profile.setShopName("Heritage Pottery Studio");
            profile.setCraftType("Traditional Terracotta & Pottery");
            profile.setCity("Jaipur");
            artisanProfileRepository.save(profile);
            System.out.println("✅ Seeded Artisan: artisan@craftbid.in / artisan123");
        }
    }

    private void seedBuyer() {
        if (userRepository.findByEmail("buyer@craftbid.in").isEmpty() &&
            userRepository.findByPhone("9123456780").isEmpty()) {

            User buyerUser = new User();
            buyerUser.setName("Ananya Roy");
            buyerUser.setEmail("buyer@craftbid.in");
            buyerUser.setPhone("9123456780");
            buyerUser.setPassword(passwordEncoder.encode("buyer123"));
            buyerUser.setRole(Role.CUSTOMER);
            buyerUser.setActive(true);
            buyerUser.setSellerEnabled(false);
            userRepository.save(buyerUser);
            System.out.println("✅ Seeded Buyer: buyer@craftbid.in / buyer123");
        }
    }
}
