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
            seedOfficialAdmin();
        } catch (Exception e) {
            System.out.println("ℹ️ Admin already exists or initialized: " + e.getMessage());
        }

        try {
            seedArtisan();
        } catch (Exception e) {
            System.out.println("ℹ️ Artisan already exists: " + e.getMessage());
        }

        try {
            seedBuyer();
        } catch (Exception e) {
            System.out.println("ℹ️ Buyer already exists: " + e.getMessage());
        }
    }

    private void seedOfficialAdmin() {
        String adminEmail = "craftbid.official@gmail.com";
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                admin -> {
                    if (admin.getRole() != Role.ADMIN || !admin.isActive()) {
                        admin.setRole(Role.ADMIN);
                        admin.setActive(true);
                        userRepository.save(admin);
                    }
                },
                () -> {
                    User admin = new User();
                    admin.setName("CraftBid Official Admin");
                    admin.setEmail(adminEmail);
                    admin.setPhone("9040408690");
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setRole(Role.ADMIN);
                    admin.setActive(true);
                    admin.setSellerEnabled(true);
                    userRepository.save(admin);
                    System.out.println("✅ Seeded Official Admin: " + adminEmail);
                }
        );
    }

    private void seedArtisan() {
        String artisanEmail = "rb650196@gmail.com";
        userRepository.findByEmail(artisanEmail).ifPresentOrElse(
                user -> {
                    if (!user.isActive()) {
                        user.setActive(true);
                        userRepository.save(user);
                    }
                },
                () -> {
                    User artisanUser = new User();
                    artisanUser.setName("Ranjit Artisan");
                    artisanUser.setEmail(artisanEmail);
                    artisanUser.setPhone("7077476718");
                    artisanUser.setPassword(passwordEncoder.encode("buyer123"));
                    artisanUser.setRole(Role.CUSTOMER);
                    artisanUser.setActive(true);
                    artisanUser.setSellerEnabled(true);
                    User saved = userRepository.save(artisanUser);

                    ArtisanProfile profile = new ArtisanProfile();
                    profile.setUser(saved);
                    profile.setShopName("Royal Heritage Studio");
                    profile.setCraftType("Pottery & Ceramics");
                    profile.setCity("Bhubaneswar");
                    artisanProfileRepository.save(profile);
                }
        );
    }

    private void seedBuyer() {
        String buyerEmail = "ranjitbarik466@gmail.com";
        userRepository.findByEmail(buyerEmail).ifPresentOrElse(
                user -> {
                    if (!user.isActive()) {
                        user.setActive(true);
                        userRepository.save(user);
                    }
                },
                () -> {
                    User buyer = new User();
                    buyer.setName("Ranjit Collector");
                    buyer.setEmail(buyerEmail);
                    buyer.setPhone("9040408690");
                    buyer.setPassword(passwordEncoder.encode("buyer123"));
                    buyer.setRole(Role.CUSTOMER);
                    buyer.setActive(true);
                    buyer.setSellerEnabled(false);
                    userRepository.save(buyer);
                }
        );
    }
}
