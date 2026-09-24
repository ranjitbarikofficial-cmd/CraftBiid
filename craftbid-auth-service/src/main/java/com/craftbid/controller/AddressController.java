package com.craftbid.controller;

import com.craftbid.dto.AddressDTO;
import com.craftbid.entity.Address;
import com.craftbid.entity.User;
import com.craftbid.repository.UserRepository;
import com.craftbid.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;
    private final UserRepository userRepository;

    public AddressController(AddressService addressService, UserRepository userRepository) {
        this.addressService = addressService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required");
        }
        return userRepository.findByIdentifier(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<AddressDTO>> getMyAddresses(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(addressService.getUserAddresses(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressDTO> getAddressById(Authentication authentication, @PathVariable Long id) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(addressService.getAddressById(id, user));
    }

    @PostMapping
    public ResponseEntity<AddressDTO> createAddress(
            Authentication authentication,
            @Valid @RequestBody AddressDTO request) {
        User user = getAuthenticatedUser(authentication);
        Address address = addressService.createAddress(user, request);
        return ResponseEntity.ok(AddressDTO.fromEntity(address));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDTO> updateAddress(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AddressDTO request) {
        User user = getAuthenticatedUser(authentication);
        Address address = addressService.updateAddress(id, user, request);
        return ResponseEntity.ok(AddressDTO.fromEntity(address));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(Authentication authentication, @PathVariable Long id) {
        User user = getAuthenticatedUser(authentication);
        addressService.deleteAddress(id, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<AddressDTO> setDefaultAddress(Authentication authentication, @PathVariable Long id) {
        User user = getAuthenticatedUser(authentication);
        Address address = addressService.setDefaultAddress(id, user);
        return ResponseEntity.ok(AddressDTO.fromEntity(address));
    }
}
