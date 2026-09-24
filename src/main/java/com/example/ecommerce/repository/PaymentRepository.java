package com.example.ecommerce.repository;


import com.example.ecommerce.entity.Payment;
import com.example.ecommerce.entity.Order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder(Order order);

    Optional<Payment> findByPaymentId(String paymentId);
}