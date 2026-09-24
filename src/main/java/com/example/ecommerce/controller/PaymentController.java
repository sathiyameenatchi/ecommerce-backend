package com.example.ecommerce.controller;

import com.example.ecommerce.dto.PaymentResponse;
import com.example.ecommerce.dto.PaymentVerificationRequest;
import com.example.ecommerce.service.PaymentService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(
            PaymentService paymentService) {

        this.paymentService = paymentService;
    }

    // =====================================================
    // CREATE PAYMENT
    // =====================================================

    @PostMapping("/create/{orderId}")
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable Long orderId,
            Authentication authentication)
            throws Exception {

        String email = authentication.getName();

        PaymentResponse response =
                paymentService.createPaymentOrder(
                        email,
                        orderId
                );

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // DEMO PAYMENT
    // =====================================================

    @PostMapping("/demo/{orderId}")
    public ResponseEntity<PaymentResponse> demoPayment(
            @PathVariable Long orderId,
            Authentication authentication) {

        String email = authentication.getName();

        PaymentResponse response =
                paymentService.demoPayment(
                        email,
                        orderId
                );

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // VERIFY RAZORPAY PAYMENT
    // =====================================================

    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(
            @RequestBody PaymentVerificationRequest request)
            throws Exception {

        boolean verified =
                paymentService.verifyPayment(
                        request.getRazorpayOrderId(),
                        request.getRazorpayPaymentId(),
                        request.getRazorpaySignature(),
                        request.getOrderId()
                );

        if (verified) {
            return ResponseEntity.ok(
                    "Payment verified successfully"
            );
        }

        return ResponseEntity
                .badRequest()
                .body(
                        "Payment verification failed"
                );
    }

    // =====================================================
    // PAYMENT SUCCESS
    // =====================================================

    @PutMapping("/success/{orderId}")
    public ResponseEntity<PaymentResponse> paymentSuccess(
            @PathVariable Long orderId,
            Authentication authentication) {

        String email = authentication.getName();

        PaymentResponse response =
                paymentService.paymentSuccess(
                        email,
                        orderId
                );

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // PAYMENT FAILED
    // =====================================================

    @PutMapping("/failed/{orderId}")
    public ResponseEntity<PaymentResponse> paymentFailed(
            @PathVariable Long orderId,
            Authentication authentication) {

        String email = authentication.getName();

        PaymentResponse response =
                paymentService.paymentFailed(
                        email,
                        orderId
                );

        return ResponseEntity.ok(response);
    }
}