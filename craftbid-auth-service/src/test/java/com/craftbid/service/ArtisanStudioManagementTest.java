package com.craftbid.service;

import com.craftbid.dto.ArtisanProfileDTO;
import com.craftbid.dto.UpdateArtisanProfileRequest;
import com.craftbid.dto.UpdateReelRequest;
import com.craftbid.entity.ArtisanProfile;
import com.craftbid.entity.Category;
import com.craftbid.entity.Craft;
import com.craftbid.entity.CraftReel;
import com.craftbid.entity.Role;
import com.craftbid.entity.User;
import com.craftbid.exception.AccessDeniedException;
import com.craftbid.repository.ArtisanProfileRepository;
import com.craftbid.repository.CategoryRepository;
import com.craftbid.repository.CraftReelRepository;
import com.craftbid.repository.CraftRepository;
import com.craftbid.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArtisanStudioManagementTest {

    private UserRepository userRepository;
    private ArtisanProfileRepository artisanProfileRepository;
    private CraftRepository craftRepository;
    private CategoryRepository categoryRepository;
    private CraftReelRepository craftReelRepository;
    private FileStorageService fileStorageService;
    private EmailService emailService;

    private ArtisanService artisanService;
    private CraftService craftService;
    private CraftReelService craftReelService;

    private User artisanUser;
    private ArtisanProfile artisanProfile;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        artisanProfileRepository = mock(ArtisanProfileRepository.class);
        craftRepository = mock(CraftRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        craftReelRepository = mock(CraftReelRepository.class);
        fileStorageService = mock(FileStorageService.class);
        emailService = mock(EmailService.class);

        artisanService = new ArtisanService(
                userRepository,
                artisanProfileRepository,
                emailService,
                fileStorageService
        );

        craftService = new CraftService(
                craftRepository,
                userRepository,
                categoryRepository,
                artisanProfileRepository,
                craftReelRepository,
                fileStorageService
        );

        craftReelService = new CraftReelService(
                craftReelRepository,
                userRepository,
                artisanProfileRepository,
                craftRepository
        );

        artisanUser = new User();
        artisanUser.setId(10L);
        artisanUser.setName("Master Weaver");
        artisanUser.setEmail("artisan@craftbid.co.in");
        artisanUser.setPhone("9876543210");
        artisanUser.setCity("Varanasi");
        artisanUser.setRole(Role.CUSTOMER);
        artisanUser.setActive(true);
        artisanUser.setSellerEnabled(true);

        artisanProfile = new ArtisanProfile();
        artisanProfile.setId(20L);
        artisanProfile.setUser(artisanUser);
        artisanProfile.setShopName("Silk Heritage Studio");
        artisanProfile.setCraftType("Handloom Silk");
        artisanProfile.setCity("Varanasi");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(artisanUser.getEmail(), null, Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // 1. ARTISAN PROFILE MANAGEMENT TESTS
    // =========================================================================

    @Test
    @DisplayName("Get Artisan Profile returns combined user and studio details")
    void testGetArtisanProfile_Success() {
        when(userRepository.findByIdentifier(artisanUser.getEmail())).thenReturn(Optional.of(artisanUser));
        when(artisanProfileRepository.findByUser(artisanUser)).thenReturn(Optional.of(artisanProfile));

        ArtisanProfileDTO dto = artisanService.getArtisanProfile(artisanUser.getEmail());

        assertNotNull(dto);
        assertEquals(artisanUser.getName(), dto.getName());
        assertEquals("Silk Heritage Studio", dto.getShopName());
        assertEquals("Handloom Silk", dto.getCraftType());
        assertEquals("Varanasi", dto.getCity());
    }

    @Test
    @DisplayName("Update Artisan Profile updates name, shop name, craft type, and city")
    void testUpdateArtisanProfile_Success() {
        when(userRepository.findByIdentifier(artisanUser.getEmail())).thenReturn(Optional.of(artisanUser));
        when(artisanProfileRepository.findByUser(artisanUser)).thenReturn(Optional.of(artisanProfile));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(artisanProfileRepository.save(any(ArtisanProfile.class))).thenAnswer(i -> i.getArgument(0));

        UpdateArtisanProfileRequest req = new UpdateArtisanProfileRequest();
        req.setName("Ramesh Sharma");
        req.setShopName("Royal Varanasi Silks");
        req.setCraftType("Pure Banarasi Sarees");
        req.setCity("Varanasi East");
        req.setPhone("9123456780");

        ArtisanProfileDTO updated = artisanService.updateArtisanProfile(artisanUser.getEmail(), req);

        assertNotNull(updated);
        assertEquals("Ramesh Sharma", updated.getName());
        assertEquals("Royal Varanasi Silks", updated.getShopName());
        assertEquals("Pure Banarasi Sarees", updated.getCraftType());
        assertEquals("Varanasi East", updated.getCity());
        assertEquals("9123456780", updated.getPhone());
    }

    @Test
    @DisplayName("Upload Profile Photo saves image file and updates user & profile avatar URLs")
    void testUploadProfilePhoto_Success() throws IOException {
        MockMultipartFile avatarFile = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "image data".getBytes()
        );

        when(userRepository.findByIdentifier(artisanUser.getEmail())).thenReturn(Optional.of(artisanUser));
        when(artisanProfileRepository.findByUser(artisanUser)).thenReturn(Optional.of(artisanProfile));
        when(fileStorageService.saveFile(avatarFile, "avatars")).thenReturn("/uploads/avatars/avatar_123.jpg");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(artisanProfileRepository.save(any(ArtisanProfile.class))).thenAnswer(i -> i.getArgument(0));

        ArtisanProfileDTO dto = artisanService.uploadProfilePhoto(artisanUser.getEmail(), avatarFile);

        assertNotNull(dto);
        assertEquals("/uploads/avatars/avatar_123.jpg", dto.getProfileImageUrl());
        assertEquals("/uploads/avatars/avatar_123.jpg", artisanUser.getProfileImageUrl());
        assertEquals("/uploads/avatars/avatar_123.jpg", artisanProfile.getProfileImageUrl());
    }

    // =========================================================================
    // 2. CRAFT & CRAFT PHOTO MANAGEMENT TESTS
    // =========================================================================

    @Test
    @DisplayName("Update Craft with replacement photo updates craft entity and image URL")
    void testUpdateCraftWithMedia_Success() throws IOException {
        Category category = new Category();
        category.setId(1L);
        category.setName("Textiles");

        Craft craft = new Craft();
        craft.setId(100L);
        craft.setTitle("Old Silk Scarf");
        craft.setDescription("Old description");
        craft.setBasePrice(new BigDecimal("1200.00"));
        craft.setImageUrl("/uploads/crafts/old.jpg");
        craft.setSeller(artisanUser);
        craft.setCategory(category);
        craft.setStatus("ACTIVE");

        MockMultipartFile newImage = new MockMultipartFile(
                "image",
                "new_craft.jpg",
                "image/jpeg",
                "new image data".getBytes()
        );

        when(craftRepository.findById(100L)).thenReturn(Optional.of(craft));
        when(userRepository.findByIdentifier(artisanUser.getEmail())).thenReturn(Optional.of(artisanUser));
        when(fileStorageService.saveFile(newImage, "crafts")).thenReturn("/uploads/crafts/new_craft_456.jpg");
        when(craftRepository.save(any(Craft.class))).thenAnswer(i -> i.getArgument(0));

        Craft updated = craftService.updateCraftWithMedia(
                100L,
                "Handcrafted Banarasi Silk Stole",
                null,
                "Pure handwoven zari banarasi silk stole",
                new BigDecimal("2500.00"),
                newImage,
                true
        );

        assertNotNull(updated);
        assertEquals("Handcrafted Banarasi Silk Stole", updated.getTitle());
        assertEquals(new BigDecimal("2500.00"), updated.getBasePrice());
        assertEquals("/uploads/crafts/new_craft_456.jpg", updated.getImageUrl());
    }

    @Test
    @DisplayName("Delete Craft deletes associated craft reels to avoid foreign key violations")
    void testDeleteCraft_CascadesAssociatedReels() {
        Craft craft = new Craft();
        craft.setId(200L);
        craft.setSeller(artisanUser);

        CraftReel associatedReel = new CraftReel();
        associatedReel.setId(300L);
        associatedReel.setCraft(craft);
        associatedReel.setArtisan(artisanProfile);

        when(craftRepository.findById(200L)).thenReturn(Optional.of(craft));
        when(userRepository.findByIdentifier(artisanUser.getEmail())).thenReturn(Optional.of(artisanUser));
        when(craftReelRepository.findByCraftId(200L)).thenReturn(List.of(associatedReel));

        craftService.deleteCraft(200L);

        verify(craftReelRepository, times(1)).deleteAll(List.of(associatedReel));
        verify(craftRepository, times(1)).delete(craft);
    }

    // =========================================================================
    // 3. CRAFT REEL MANAGEMENT TESTS
    // =========================================================================

    @Test
    @DisplayName("Update Reel updates title, description, and thumbnail")
    void testUpdateReel_Success() {
        CraftReel reel = new CraftReel();
        reel.setId(50L);
        reel.setTitle("Old Title");
        reel.setDescription("Old Desc");
        reel.setVideoUrl("/uploads/reels/video.mp4");
        reel.setThumbnailUrl("/uploads/crafts/thumb.jpg");
        reel.setArtisan(artisanProfile);

        when(userRepository.findByIdentifier(artisanUser.getEmail())).thenReturn(Optional.of(artisanUser));
        when(craftReelRepository.findById(50L)).thenReturn(Optional.of(reel));
        when(craftReelRepository.save(any(CraftReel.class))).thenAnswer(i -> i.getArgument(0));

        UpdateReelRequest req = new UpdateReelRequest();
        req.setTitle("Weaving Magic Banarasi Reel");
        req.setDescription("Watch authentic handloom craftsmanship live!");
        req.setThumbnailUrl("/uploads/crafts/new_thumb.jpg");

        CraftReel updated = craftReelService.updateReel(artisanUser.getEmail(), 50L, req);

        assertNotNull(updated);
        assertEquals("Weaving Magic Banarasi Reel", updated.getTitle());
        assertEquals("Watch authentic handloom craftsmanship live!", updated.getDescription());
        assertEquals("/uploads/crafts/new_thumb.jpg", updated.getThumbnailUrl());
    }

    @Test
    @DisplayName("Delete Reel deletes reel entity when requested by owner")
    void testDeleteReel_Success() {
        CraftReel reel = new CraftReel();
        reel.setId(50L);
        reel.setArtisan(artisanProfile);

        when(userRepository.findByIdentifier(artisanUser.getEmail())).thenReturn(Optional.of(artisanUser));
        when(craftReelRepository.findById(50L)).thenReturn(Optional.of(reel));

        craftReelService.deleteReel(artisanUser.getEmail(), 50L);

        verify(craftReelRepository, times(1)).delete(reel);
    }

    @Test
    @DisplayName("Disallow modifying or deleting other artisan's reel")
    void testModifyOtherArtisanReel_ThrowsAccessDenied() {
        User otherUser = new User();
        otherUser.setId(999L);

        ArtisanProfile otherProfile = new ArtisanProfile();
        otherProfile.setId(888L);
        otherProfile.setUser(otherUser);

        CraftReel otherReel = new CraftReel();
        otherReel.setId(50L);
        otherReel.setArtisan(otherProfile);

        when(userRepository.findByIdentifier(artisanUser.getEmail())).thenReturn(Optional.of(artisanUser));
        when(craftReelRepository.findById(50L)).thenReturn(Optional.of(otherReel));

        UpdateReelRequest req = new UpdateReelRequest();
        req.setTitle("Hacked Title");

        assertThrows(AccessDeniedException.class, () ->
                craftReelService.updateReel(artisanUser.getEmail(), 50L, req)
        );

        assertThrows(AccessDeniedException.class, () ->
                craftReelService.deleteReel(artisanUser.getEmail(), 50L)
        );
    }
}
