package com.example.ecommerce.controller;

import com.example.ecommerce.dto.AddToCartRequest;
import com.example.ecommerce.dto.CartResponse;
import com.example.ecommerce.service.CartService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @RequestBody AddToCartRequest request,
            Authentication authentication) {

        System.out.println("AUTH = " + authentication);
        System.out.println("USER = " + authentication.getName());

        String email = authentication.getName();

        return ResponseEntity.ok(
                cartService.addToCart(email, request)
        );
    }
    // View cart
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                cartService.getCart(email)
        );
    }

    // Update quantity
    @PutMapping("/item/{itemId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @PathVariable Long itemId,
            @RequestParam Integer quantity,
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                cartService.updateQuantity(
                        email,
                        itemId,
                        quantity
                )
        );
    }

    // Remove item
    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<CartResponse> removeFromCart(
            @PathVariable Long itemId,
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                cartService.removeFromCart(
                        email,
                        itemId
                )
        );
    }
}
