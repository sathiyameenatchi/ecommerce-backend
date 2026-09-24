package com.example.ecommerce.controller;

import com.example.ecommerce.dto.AddressRequest;
import com.example.ecommerce.dto.AddressResponse;
import com.example.ecommerce.entity.Address;
import com.example.ecommerce.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    // GET USER ADDRESSES
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AddressResponse>> getUserAddresses(
            @PathVariable Long userId) {

        List<Address> addresses =
                addressService.getUserAddresses(userId);

        List<AddressResponse> response =
                addresses.stream()
                        .map(AddressResponse::new)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // ADD ADDRESS
    @PostMapping("/user/{userId}")
    public ResponseEntity<AddressResponse> addAddress(
            @PathVariable Long userId,
            @Valid @RequestBody AddressRequest request) {

        Address address = new Address();

        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());

        Address saved =
                addressService.addAddress(userId, address);

        return ResponseEntity.ok(
                new AddressResponse(saved)
        );
    }

    // UPDATE ADDRESS
    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long addressId,
            @RequestBody Address address) {

        Address updated =
                addressService.updateAddress(
                        addressId,
                        address
                );

        return ResponseEntity.ok(
                new AddressResponse(updated)
        );
    }

    // DELETE ADDRESS
    @DeleteMapping("/{addressId}")
    public ResponseEntity<String> deleteAddress(
            @PathVariable Long addressId) {

        addressService.deleteAddress(addressId);

        return ResponseEntity.ok(
                "Address deleted successfully"
        );
    }
}

