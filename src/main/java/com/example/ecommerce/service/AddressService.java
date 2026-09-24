package com.example.ecommerce.service;

import com.example.ecommerce.entity.Address;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.AddressRepository;
import com.example.ecommerce.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(
            AddressRepository addressRepository,
            UserRepository userRepository) {

        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    public Address addAddress(
            Long userId,
            Address address) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        address.setUser(user);

        return addressRepository.save(address);
    }

    public List<Address> getUserAddresses(Long userId) {

        return addressRepository.findByUserId(userId);
    }

    public Address updateAddress(
            Long addressId,
            Address updatedAddress) {

        Address existing =
                addressRepository.findById(addressId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Address not found"));

        existing.setFullName(
                updatedAddress.getFullName());

        existing.setPhone(
                updatedAddress.getPhone());

        existing.setAddressLine(
                updatedAddress.getAddressLine());

        existing.setCity(
                updatedAddress.getCity());

        existing.setState(
                updatedAddress.getState());

        existing.setPincode(
                updatedAddress.getPincode());

        return addressRepository.save(existing);
    }

    public void deleteAddress(Long addressId) {

        Address address =
                addressRepository.findById(addressId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Address not found"));

        addressRepository.delete(address);
    }
}