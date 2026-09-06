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
    private final CategoryRepository categoryRepository;
    private final CraftRepository craftRepository;
    private final CraftReelRepository craftReelRepository;

    public DataInitializer(
            UserRepository userRepository,
            ArtisanProfileRepository artisanProfileRepository,
            PasswordEncoder passwordEncoder,
            CategoryRepository categoryRepository,
            CraftRepository craftRepository,
            CraftReelRepository craftReelRepository) {

        this.userRepository = userRepository;
        this.artisanProfileRepository = artisanProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoryRepository = categoryRepository;
        this.craftRepository = craftRepository;
        this.craftReelRepository = craftReelRepository;
    }

    @Override
    public void run(String... args) {
        try {
            seedCategories();
        } catch (Exception e) {
            System.out.println("ℹ️ Categories already initialized or seeded: " + e.getMessage());
        }

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

        try {
            seedDemoCraftsAndReels();
        } catch (Exception e) {
            System.out.println("ℹ️ Demo crafts/reels already seeded: " + e.getMessage());
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

    private void seedCategories() {
        java.util.List<String[]> defaultCategories = java.util.List.of(
                new String[]{"Pottery & Ceramics", "Handcrafted clay pots, ceramic vases, stoneware bowls, and sculpted art"},
                new String[]{"Woodworking", "Hand-carved wooden decor, custom furniture, utensils, and ornamental pieces"},
                new String[]{"Handmade Jewelry", "Artisanal necklaces, gemstone rings, handcrafted bracelets, and beadwork"},
                new String[]{"Paintings & Canvas", "Oil paintings, watercolors, acrylic artwork, and traditional folk paintings"},
                new String[]{"Textiles & Weaving", "Handwoven rugs, tapestries, embroidered shawls, and organic cotton textiles"},
                new String[]{"Leather Goods", "Handmade leather wallets, bags, belts, and bespoke leather craft"},
                new String[]{"Glass Art", "Stained glass, blown glass ornaments, and sculpted glassware"},
                new String[]{"Metalcraft & Sculptures", "Forged iron, brass decor, copper crafts, and handmade sculptures"}
        );

        for (String[] cat : defaultCategories) {
            categoryRepository.findByNameIgnoreCase(cat[0]).ifPresentOrElse(
                    existing -> {},
                    () -> {
                        Category category = new Category();
                        category.setName(cat[0]);
                        category.setDescription(cat[1]);
                        categoryRepository.save(category);
                        System.out.println("✅ Seeded Category: " + cat[0]);
                    }
            );
        }
    }

    private void seedDemoCraftsAndReels() {
        if (craftRepository.count() > 0) {
            return;
        }

        User artisanUser = userRepository.findByEmail("rb650196@gmail.com")
                .orElse(null);
        if (artisanUser == null) {
            return;
        }

        ArtisanProfile artisanProfile = artisanProfileRepository.findByUser(artisanUser)
                .orElse(null);
        if (artisanProfile == null) {
            return;
        }

        Category pottery = categoryRepository.findByNameIgnoreCase("Pottery & Ceramics").orElse(null);
        Category woodworking = categoryRepository.findByNameIgnoreCase("Woodworking").orElse(null);
        Category jewelry = categoryRepository.findByNameIgnoreCase("Handmade Jewelry").orElse(null);
        Category paintings = categoryRepository.findByNameIgnoreCase("Paintings & Canvas").orElse(null);
        Category textiles = categoryRepository.findByNameIgnoreCase("Textiles & Weaving").orElse(null);
        Category metalcraft = categoryRepository.findByNameIgnoreCase("Metalcraft & Sculptures").orElse(null);

        // 1. Royal Terracotta Vase
        if (pottery != null) {
            Craft c1 = new Craft();
            c1.setTitle("Royal Terracotta Heritage Vase");
            c1.setDescription("Hand-moulded terracotta flower vase fired in traditional wood kilns with intricate tribal engravings.");
            c1.setBasePrice(new java.math.BigDecimal("1800.00"));
            c1.setImageUrl("https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=800&q=80");
            c1.setCategory(pottery);
            c1.setSeller(artisanUser);
            c1.setStatus("ACTIVE");
            Craft savedC1 = craftRepository.save(c1);

            CraftReel r1 = new CraftReel();
            r1.setArtisan(artisanProfile);
            r1.setCraft(savedC1);
            r1.setTitle("Shaping the Terracotta Vase on Wheel");
            r1.setDescription("Watch master artisan shape wet clay into an ancient heritage terracotta vase.");
            r1.setVideoUrl("https://assets.mixkit.co/videos/preview/mixkit-potter-shaping-a-clay-pot-on-a-wheel-42866-large.mp4");
            r1.setThumbnailUrl("https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=800&q=80");
            r1.setViews(1240L);
            r1.setLikes(342L);
            r1.setStatus("ACTIVE");
            craftReelRepository.save(r1);
        }

        // 2. Hand-Carved Teakwood Temple Decor
        if (woodworking != null) {
            Craft c2 = new Craft();
            c2.setTitle("Hand-Carved Teakwood Temple Decor");
            c2.setDescription("Exquisite temple pillar replica hand-carved from seasoned Indian teakwood with rich walnut polish.");
            c2.setBasePrice(new java.math.BigDecimal("4500.00"));
            c2.setImageUrl("https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=800&q=80");
            c2.setCategory(woodworking);
            c2.setSeller(artisanUser);
            c2.setStatus("ACTIVE");
            Craft savedC2 = craftRepository.save(c2);

            CraftReel r2 = new CraftReel();
            r2.setArtisan(artisanProfile);
            r2.setCraft(savedC2);
            r2.setTitle("Teakwood Fine Chisel Detailing");
            r2.setDescription("Intricate chisel work bringing life to raw seasoned teakwood.");
            r2.setVideoUrl("https://assets.mixkit.co/videos/preview/mixkit-craftsman-carving-wood-with-a-chisel-42870-large.mp4");
            r2.setThumbnailUrl("https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=800&q=80");
            r2.setViews(980L);
            r2.setLikes(215L);
            r2.setStatus("ACTIVE");
            craftReelRepository.save(r2);
        }

        // 3. Traditional Kundan Meenakari Choker
        if (jewelry != null) {
            Craft c3 = new Craft();
            c3.setTitle("Traditional Kundan Meenakari Choker");
            c3.setDescription("24k gold-plated artisanal necklace studded with un-cut Kundan stones and vibrant red enamel Meenakari work.");
            c3.setBasePrice(new java.math.BigDecimal("3200.00"));
            c3.setImageUrl("https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?w=800&q=80");
            c3.setCategory(jewelry);
            c3.setSeller(artisanUser);
            c3.setStatus("ACTIVE");
            Craft savedC3 = craftRepository.save(c3);

            CraftReel r3 = new CraftReel();
            r3.setArtisan(artisanProfile);
            r3.setCraft(savedC3);
            r3.setTitle("Setting Uncut Stones in Kundan Choker");
            r3.setDescription("Meticulous placement of uncut gemstones in handcrafted filigree.");
            r3.setVideoUrl("https://assets.mixkit.co/videos/preview/mixkit-hands-of-a-jeweler-assembling-a-necklace-42868-large.mp4");
            r3.setThumbnailUrl("https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?w=800&q=80");
            r3.setViews(1850L);
            r3.setLikes(520L);
            r3.setStatus("ACTIVE");
            craftReelRepository.save(r3);
        }

        // 4. Pattachitra Heritage Canvas Scroll
        if (paintings != null) {
            Craft c4 = new Craft();
            c4.setTitle("Pattachitra Heritage Canvas Scroll");
            c4.setDescription("Ancient Odisha folk art hand-painted on treated tussar canvas using organic mineral and vegetable pigments.");
            c4.setBasePrice(new java.math.BigDecimal("2800.00"));
            c4.setImageUrl("https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=800&q=80");
            c4.setCategory(paintings);
            c4.setSeller(artisanUser);
            c4.setStatus("ACTIVE");
            Craft savedC4 = craftRepository.save(c4);

            CraftReel r4 = new CraftReel();
            r4.setArtisan(artisanProfile);
            r4.setCraft(savedC4);
            r4.setTitle("Fine Linework in Heritage Pattachitra");
            r4.setDescription("Hand-painting intricate traditional mythological motifs with single-hair brushes.");
            r4.setVideoUrl("https://assets.mixkit.co/videos/preview/mixkit-artist-painting-with-a-fine-brush-42872-large.mp4");
            r4.setThumbnailUrl("https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=800&q=80");
            r4.setViews(2100L);
            r4.setLikes(640L);
            r4.setStatus("ACTIVE");
            craftReelRepository.save(r4);
        }

        // 5. Handloom Sambalpuri Ikat Silk Shawl
        if (textiles != null) {
            Craft c5 = new Craft();
            c5.setTitle("Handloom Sambalpuri Ikat Silk Shawl");
            c5.setDescription("Tie-dyed pure silk shawl woven on wooden handlooms with traditional Konark chakra motifs.");
            c5.setBasePrice(new java.math.BigDecimal("3600.00"));
            c5.setImageUrl("https://images.unsplash.com/photo-1606760227091-3dd870d97f1d?w=800&q=80");
            c5.setCategory(textiles);
            c5.setSeller(artisanUser);
            c5.setStatus("ACTIVE");
            Craft savedC5 = craftRepository.save(c5);

            CraftReel r5 = new CraftReel();
            r5.setArtisan(artisanProfile);
            r5.setCraft(savedC5);
            r5.setTitle("Rhythm of the Wooden Handloom");
            r5.setDescription("Master weavers passing the wooden shuttle to create authentic tie-and-dye patterns.");
            r5.setVideoUrl("https://assets.mixkit.co/videos/preview/mixkit-weaving-fabric-on-a-loom-42874-large.mp4");
            r5.setThumbnailUrl("https://images.unsplash.com/photo-1606760227091-3dd870d97f1d?w=800&q=80");
            r5.setViews(1420L);
            r5.setLikes(410L);
            r5.setStatus("ACTIVE");
            craftReelRepository.save(r5);
        }

        // 6. Dhokra Lost-Wax Brass Tribal Figurine
        if (metalcraft != null) {
            Craft c6 = new Craft();
            c6.setTitle("Dhokra Lost-Wax Brass Tribal Figurine");
            c6.setDescription("Non-ferrous metal casting using 4000-year-old lost-wax technique by indigenous tribal metallurgists.");
            c6.setBasePrice(new java.math.BigDecimal("2200.00"));
            c6.setImageUrl("https://images.unsplash.com/photo-1569388330292-79cc1ec67270?w=800&q=80");
            c6.setCategory(metalcraft);
            c6.setSeller(artisanUser);
            c6.setStatus("ACTIVE");
            Craft savedC6 = craftRepository.save(c6);

            CraftReel r6 = new CraftReel();
            r6.setArtisan(artisanProfile);
            r6.setCraft(savedC6);
            r6.setTitle("Dhokra Molten Brass Pouring");
            r6.setDescription("Pouring glowing molten brass into ancient clay-wax moulds.");
            r6.setVideoUrl("https://assets.mixkit.co/videos/preview/mixkit-sculptor-shaping-metal-with-heat-42876-large.mp4");
            r6.setThumbnailUrl("https://images.unsplash.com/photo-1569388330292-79cc1ec67270?w=800&q=80");
            r6.setViews(1650L);
            r6.setLikes(480L);
            r6.setStatus("ACTIVE");
            craftReelRepository.save(r6);
        }

        System.out.println("✅ Seeded initial demo crafts and reels for live marketplace showcase");
    }
}
