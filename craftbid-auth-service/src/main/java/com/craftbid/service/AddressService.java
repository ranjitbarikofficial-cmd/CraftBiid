package com.craftbid.service;

import com.craftbid.dto.AddressDTO;
import com.craftbid.entity.Address;
import com.craftbid.entity.User;
import com.craftbid.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<AddressDTO> getUserAddresses(User user) {
        return addressRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(AddressDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<Address> getAddressEntity(Long id, User user) {
        return addressRepository.findByIdAndUser(id, user);
    }

    public AddressDTO getAddressById(Long id, User user) {
        Address address = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Address not found or unauthorized"));
        return AddressDTO.fromEntity(address);
    }

    @Transactional
    public Address createAddress(User user, AddressDTO dto) {
        if (dto.isDefault()) {
            addressRepository.findByUserAndIsDefaultTrue(user).ifPresent(oldDefault -> {
                oldDefault.setDefault(false);
                addressRepository.save(oldDefault);
            });
        } else {
            // If it's the user's first address, make it default automatically
            List<Address> existing = addressRepository.findByUserOrderByCreatedAtDesc(user);
            if (existing.isEmpty()) {
                dto.setDefault(true);
            }
        }

        Address address = new Address();
        address.setUser(user);
        address.setFullName(dto.getFullName());
        address.setPhone(dto.getPhone());
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPincode(dto.getPincode());
        address.setLandmark(dto.getLandmark());
        address.setDefault(dto.isDefault());

        return addressRepository.save(address);
    }

    @Transactional
    public Address updateAddress(Long id, User user, AddressDTO dto) {
        Address address = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Address not found or unauthorized"));

        if (dto.isDefault() && !address.isDefault()) {
            addressRepository.findByUserAndIsDefaultTrue(user).ifPresent(oldDefault -> {
                oldDefault.setDefault(false);
                addressRepository.save(oldDefault);
            });
        }

        address.setFullName(dto.getFullName());
        address.setPhone(dto.getPhone());
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPincode(dto.getPincode());
        address.setLandmark(dto.getLandmark());
        address.setDefault(dto.isDefault());

        return addressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(Long id, User user) {
        Address address = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Address not found or unauthorized"));
        addressRepository.delete(address);
    }

    @Transactional
    public Address setDefaultAddress(Long id, User user) {
        Address address = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Address not found or unauthorized"));

        addressRepository.findByUserAndIsDefaultTrue(user).ifPresent(oldDefault -> {
            oldDefault.setDefault(false);
            addressRepository.save(oldDefault);
        });

        address.setDefault(true);
        return addressRepository.save(address);
    }
}
