package com.example.ecommerce.service;

import com.example.ecommerce.dto.AddToCartRequest;
import com.example.ecommerce.dto.CartItemResponse;
import com.example.ecommerce.dto.CartResponse;
import com.example.ecommerce.entity.Cart;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {

        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // =========================
    // ADD PRODUCT TO CART
    // =========================

    @Transactional
    public CartResponse addToCart(
            String email,
            AddToCartRequest request) {

        if (request.getQuantity() == null ||
                request.getQuantity() <= 0) {

            throw new RuntimeException(
                    "Quantity must be greater than zero"
            );
        }

        User user = getUser(email);

        Product product = productRepository
                .findById(request.getProductId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found"
                        )
                );

        if (product.getStock() < request.getQuantity()) {

            throw new RuntimeException(
                    "Not enough stock available"
            );
        }

        Cart cart = cartRepository
                .findByUser(user)
                .orElseGet(() -> {

                    Cart newCart = new Cart();
                    newCart.setUser(user);

                    return cartRepository.save(newCart);
                });

        CartItem cartItem = cartItemRepository
                .findByCartAndProduct(cart, product)
                .orElse(null);

        if (cartItem != null) {

            // Product already exists in cart
            int newQuantity =
                    cartItem.getQuantity()
                            + request.getQuantity();

            if (newQuantity > product.getStock()) {

                throw new RuntimeException(
                        "Not enough stock available"
                );
            }

            cartItem.setQuantity(newQuantity);

        } else {

            // New product in cart
            cartItem = new CartItem();

            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());

            // Keep cart's in-memory collection in sync
            cart.getItems().add(cartItem);
        }

        cartItemRepository.save(cartItem);

        return getCart(email);
    }

    // =========================
    // GET CART
    // =========================

    public CartResponse getCart(String email) {

        User user = getUser(email);

        Cart cart = cartRepository
                .findByUser(user)
                .orElseGet(() -> {

                    Cart newCart = new Cart();
                    newCart.setUser(user);

                    return cartRepository.save(newCart);
                });

        List<CartItemResponse> items =
                cart.getItems()
                        .stream()
                        .map(this::convertToResponse)
                        .toList();

        double total = items.stream()
                .mapToDouble(CartItemResponse::getSubtotal)
                .sum();

        return new CartResponse(
                cart.getId(),
                items,
                total
        );
    }

    // =========================
    // UPDATE QUANTITY
    // =========================

    public CartResponse updateQuantity(
            String email,
            Long itemId,
            Integer quantity) {

        if (quantity == null || quantity <= 0) {

            throw new RuntimeException(
                    "Quantity must be greater than zero"
            );
        }

        User user = getUser(email);

        Cart cart = cartRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cart not found"
                        )
                );

        CartItem item = cartItemRepository
                .findById(itemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cart item not found"
                        )
                );

        if (!item.getCart().getId()
                .equals(cart.getId())) {

            throw new RuntimeException(
                    "This item does not belong to your cart"
            );
        }

        Product product = item.getProduct();

        if (quantity > product.getStock()) {

            throw new RuntimeException(
                    "Not enough stock available"
            );
        }

        item.setQuantity(quantity);

        cartItemRepository.save(item);

        return getCart(email);
    }

    // =========================
    // REMOVE FROM CART
    // =========================

    @Transactional
    public CartResponse removeFromCart(
            String email,
            Long itemId) {

        User user = getUser(email);

        Cart cart = cartRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cart not found"
                        )
                );

        CartItem item = cartItemRepository
                .findById(itemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cart item not found"
                        )
                );

        if (!item.getCart().getId()
                .equals(cart.getId())) {

            throw new RuntimeException(
                    "This item does not belong to your cart"
            );
        }

        // Remove from in-memory collection
        cart.getItems().removeIf(
                cartItem ->
                        cartItem.getId().equals(itemId)
        );

        // Delete from database
        cartItemRepository.delete(item);
        cartItemRepository.flush();

        return getCart(email);
    }

    // =========================
    // GET USER
    // =========================

    private User getUser(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );
    }

    // =========================
    // CONVERT TO RESPONSE
    // =========================

    private CartItemResponse convertToResponse(
            CartItem item) {

        Product product = item.getProduct();

        double subtotal =
                product.getPrice()
                        * item.getQuantity();

        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getPrice(),
                item.getQuantity(),
                subtotal
        );
    }
}