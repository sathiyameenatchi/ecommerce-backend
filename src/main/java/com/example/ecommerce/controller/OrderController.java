package com.example.ecommerce.controller;

import com.example.ecommerce.dto.OrderResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.service.OrderService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.ecommerce.dto.CreateOrderRequest;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // =========================
    // CREATE ORDER
    // =========================
    @PostMapping("/create")
    public ResponseEntity<OrderResponse> createOrder(
            Authentication authentication,
            @RequestBody CreateOrderRequest request) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                orderService.createOrder(email, request)
        );
    }
    // =========================
    // GET ALL ORDERS - ADMIN
    // =========================
    @GetMapping("/admin/all")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {

        return ResponseEntity.ok(
                orderService.getAllOrders()
        );
    }

    // =========================
    // GET MY ORDERS
    // =========================
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                orderService.getMyOrders(email)
        );
    }
    // =========================
    // GET SINGLE ORDER
    // =========================
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId,
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                orderService.getOrderById(
                        email,
                        orderId
                )
        );
    }

    // =========================
    // UPDATE ORDER STATUS - ADMIN
    // =========================
    @PutMapping("/admin/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {

        return ResponseEntity.ok(
                orderService.updateOrderStatus(
                        orderId,
                        status
                )
        );
    }

    // =========================
    // CANCEL ORDER
    // =========================
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                orderService.cancelOrder(
                        email,
                        orderId
                )
        );
    }
}


