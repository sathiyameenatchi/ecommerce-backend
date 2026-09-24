package com.example.ecommerce.service;

import com.example.ecommerce.dto.PaymentResponse;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Payment;
import com.example.ecommerce.entity.PaymentStatus;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.UserRepository;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RazorpayClient razorpayClient;
    private final OrderService orderService;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            RazorpayClient razorpayClient,
            OrderService orderService) {

        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.razorpayClient = razorpayClient;
        this.orderService = orderService;
    }

    // =====================================================
    // SAFE RAZORPAY VALUE CONVERSION
    // =====================================================

    private long getLongValue(
            Object value,
            String fieldName) {

        if (value == null) {
            throw new RuntimeException(
                    "Razorpay " + fieldName + " is missing"
            );
        }

        try {

            if (value instanceof Number) {
                return ((Number) value).longValue();
            }

            return Long.parseLong(
                    String.valueOf(value)
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Invalid Razorpay " + fieldName + ": " + value,
                    e
            );
        }
    }

    private String getStringValue(
            Object value,
            String fieldName) {

        if (value == null) {
            throw new RuntimeException(
                    "Razorpay " + fieldName + " is missing"
            );
        }

        return String.valueOf(value);
    }

    // =====================================================
    // CREATE RAZORPAY PAYMENT ORDER
    // =====================================================

    @Transactional
    public PaymentResponse createPaymentOrder(
            String email,
            Long orderId) throws Exception {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found: " + email
                                )
                        );

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found: " + orderId
                                )
                        );

        // =================================================
        // OWNER CHECK
        // =================================================

        if (order.getUser() == null
                || order.getUser().getId() == null
                || !order.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You cannot pay for this order"
            );
        }

        // =================================================
        // ORDER STATUS CHECK
        // =================================================

        if (order.getStatus()
                == OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Cancelled order cannot be paid"
            );
        }

        if (order.getStatus()
                == OrderStatus.PAID) {

            throw new RuntimeException(
                    "Order is already paid"
            );
        }

        // =================================================
        // AMOUNT VALIDATION
        // =================================================

        if (order.getTotalAmount() <= 0) {

            throw new RuntimeException(
                    "Invalid order amount: ₹"
                            + order.getTotalAmount()
            );
        }

        BigDecimal amountRupees =
                BigDecimal
                        .valueOf(order.getTotalAmount())
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        long amountInPaise =
                amountRupees
                        .multiply(
                                BigDecimal.valueOf(100)
                        )
                        .longValueExact();

        // =================================================
        // CREATE RAZORPAY ORDER
        // =================================================

        JSONObject options = new JSONObject();

        options.put(
                "amount",
                amountInPaise
        );

        options.put(
                "currency",
                "INR"
        );

        options.put(
                "receipt",
                "order_" + orderId
        );

        com.razorpay.Order razorpayOrder;

        try {

            razorpayOrder =
                    razorpayClient.orders.create(
                            options
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to create Razorpay order: "
                            + e.getMessage(),
                    e
            );
        }

        // =================================================
        // RAZORPAY ORDER ID
        // =================================================

        String razorpayOrderId =
                getStringValue(
                        razorpayOrder.get("id"),
                        "order ID"
                );

        // =================================================
        // RAZORPAY AMOUNT CHECK
        // =================================================

        long razorpayAmount =
                getLongValue(
                        razorpayOrder.get("amount"),
                        "amount"
                );

        if (razorpayAmount != amountInPaise) {

            throw new RuntimeException(
                    "Razorpay amount mismatch. Expected: "
                            + amountInPaise
                            + ", Received: "
                            + razorpayAmount
            );
        }

        // =================================================
        // RAZORPAY CURRENCY CHECK
        // =================================================

        String razorpayCurrency =
                getStringValue(
                        razorpayOrder.get("currency"),
                        "currency"
                );

        if (!"INR".equalsIgnoreCase(
                razorpayCurrency
        )) {

            throw new RuntimeException(
                    "Invalid Razorpay currency: "
                            + razorpayCurrency
            );
        }

        // =================================================
        // SAVE PAYMENT
        // =================================================

        Payment payment =
                paymentRepository
                        .findByOrder(order)
                        .orElse(null);

        if (payment == null) {

            payment = new Payment();

            payment.setOrder(order);

            payment.setCreatedAt(
                    LocalDateTime.now()
            );
        }

        payment.setAmount(
                order.getTotalAmount()
        );

        payment.setPaymentId(
                razorpayOrderId
        );

        payment.setStatus(
                PaymentStatus.PENDING
        );

        paymentRepository.save(payment);

        // =================================================
        // UPDATE ORDER
        // =================================================

        order.setPaymentId(
                razorpayOrderId
        );

        order.setStatus(
                OrderStatus.PAYMENT_PENDING
        );

        orderRepository.save(order);

        // =================================================
        // RESPONSE
        // =================================================

        return new PaymentResponse(
                orderId.toString(),
                razorpayOrderId,
                keyId,
                amountInPaise,
                "INR",
                false
        );
    }

    // =====================================================
    // VERIFY RAZORPAY PAYMENT
    // =====================================================

    @Transactional
    public boolean verifyPayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature,
            Long orderId)
            throws Exception {

        // =================================================
        // BASIC VALIDATION
        // =================================================

        if (razorpayOrderId == null
                || razorpayOrderId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay order ID is missing"
            );
        }

        if (razorpayPaymentId == null
                || razorpayPaymentId.isBlank()) {

            throw new RuntimeException(
                    "Razorpay payment ID is missing"
            );
        }

        if (razorpaySignature == null
                || razorpaySignature.isBlank()) {

            throw new RuntimeException(
                    "Razorpay signature is missing"
            );
        }

        if (orderId == null) {

            throw new RuntimeException(
                    "Order ID is missing"
            );
        }

        // =================================================
        // FIND ORDER
        // =================================================

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found: "
                                                + orderId
                                )
                        );

        // =================================================
        // ORDER STATUS
        // =================================================

        if (order.getStatus()
                == OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Cancelled order cannot be paid"
            );
        }

        // =================================================
        // IDEMPOTENCY
        // =================================================

        if (order.getStatus()
                == OrderStatus.PAID) {

            return true;
        }

        // =================================================
        // CHECK RAZORPAY ORDER ID
        // =================================================

        if (order.getPaymentId() == null
                || !order.getPaymentId()
                .equals(razorpayOrderId)) {

            throw new RuntimeException(
                    "Invalid Razorpay order ID"
            );
        }

        // =================================================
        // FIND PAYMENT
        // =================================================

        Payment payment =
                paymentRepository
                        .findByOrder(order)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment record not found"
                                )
                        );

        if (payment.getPaymentId() == null
                || !payment.getPaymentId()
                .equals(razorpayOrderId)) {

            throw new RuntimeException(
                    "Payment record does not match Razorpay order"
            );
        }

        // =================================================
        // EXPECTED AMOUNT
        // =================================================

        BigDecimal expectedAmount =
                BigDecimal
                        .valueOf(order.getTotalAmount())
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        long expectedPaise =
                expectedAmount
                        .multiply(
                                BigDecimal.valueOf(100)
                        )
                        .longValueExact();

        // =================================================
        // FETCH RAZORPAY ORDER
        // =================================================

        com.razorpay.Order razorpayOrder;

        try {

            razorpayOrder =
                    razorpayClient.orders.fetch(
                            razorpayOrderId
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to verify Razorpay order",
                    e
            );
        }

        // =================================================
        // CHECK RAZORPAY ORDER AMOUNT
        // =================================================

        long razorpayOrderAmount =
                getLongValue(
                        razorpayOrder.get("amount"),
                        "order amount"
                );

        if (razorpayOrderAmount
                != expectedPaise) {

            payment.setStatus(
                    PaymentStatus.FAILED
            );

            paymentRepository.save(payment);

            throw new RuntimeException(
                    "Payment amount mismatch. Expected: "
                            + expectedPaise
                            + ", Received: "
                            + razorpayOrderAmount
            );
        }

        // =================================================
        // CHECK RAZORPAY ORDER CURRENCY
        // =================================================

        String razorpayCurrency =
                getStringValue(
                        razorpayOrder.get("currency"),
                        "order currency"
                );

        if (!"INR".equalsIgnoreCase(
                razorpayCurrency
        )) {

            payment.setStatus(
                    PaymentStatus.FAILED
            );

            paymentRepository.save(payment);

            throw new RuntimeException(
                    "Invalid payment currency: "
                            + razorpayCurrency
            );
        }

        // =================================================
        // VERIFY SIGNATURE
        // =================================================

        String payload =
                razorpayOrderId
                        + "|"
                        + razorpayPaymentId;

        boolean verified =
                Utils.verifySignature(
                        payload,
                        razorpaySignature,
                        keySecret
                );

        if (!verified) {

            payment.setStatus(
                    PaymentStatus.FAILED
            );

            paymentRepository.save(payment);

            throw new RuntimeException(
                    "Razorpay signature verification failed"
            );
        }

        // =================================================
        // FETCH ACTUAL PAYMENT
        // =================================================

        com.razorpay.Payment razorpayPayment;

        try {

            razorpayPayment =
                    razorpayClient.payments.fetch(
                            razorpayPaymentId
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to fetch Razorpay payment",
                    e
            );
        }

        // =================================================
        // PAYMENT AMOUNT
        // =================================================

        long actualPaymentAmount =
                getLongValue(
                        razorpayPayment.get("amount"),
                        "payment amount"
                );

        if (actualPaymentAmount
                != expectedPaise) {

            payment.setStatus(
                    PaymentStatus.FAILED
            );

            paymentRepository.save(payment);

            throw new RuntimeException(
                    "Actual Razorpay payment amount mismatch"
            );
        }

        // =================================================
        // PAYMENT CURRENCY
        // =================================================

        String actualPaymentCurrency =
                getStringValue(
                        razorpayPayment.get("currency"),
                        "payment currency"
                );

        if (!"INR".equalsIgnoreCase(
                actualPaymentCurrency
        )) {

            payment.setStatus(
                    PaymentStatus.FAILED
            );

            paymentRepository.save(payment);

            throw new RuntimeException(
                    "Actual Razorpay payment currency mismatch"
            );
        }

        // =================================================
        // PAYMENT ORDER ID
        // =================================================

        String actualPaymentOrderId =
                getStringValue(
                        razorpayPayment.get("order_id"),
                        "payment order ID"
                );

        if (!razorpayOrderId.equals(
                actualPaymentOrderId
        )) {

            payment.setStatus(
                    PaymentStatus.FAILED
            );

            paymentRepository.save(payment);

            throw new RuntimeException(
                    "Razorpay payment/order mismatch"
            );
        }

        // =================================================
        // PAYMENT STATUS
        // =================================================

        String razorpayPaymentStatus =
                getStringValue(
                        razorpayPayment.get("status"),
                        "payment status"
                );

        if (!"captured".equalsIgnoreCase(
                razorpayPaymentStatus
        )) {

            payment.setStatus(
                    PaymentStatus.PENDING
            );

            paymentRepository.save(payment);

            throw new RuntimeException(
                    "Razorpay payment is not captured. Current status: "
                            + razorpayPaymentStatus
            );
        }

        // =================================================
        // COMPLETE PAYMENT
        // =================================================

        orderService.completeSuccessfulPayment(
                orderId,
                razorpayPaymentId
        );

        // =================================================
        // UPDATE PAYMENT
        // =================================================

        payment =
                paymentRepository
                        .findByOrder(order)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment record not found"
                                )
                        );

        payment.setPaymentId(
                razorpayPaymentId
        );

        payment.setAmount(
                order.getTotalAmount()
        );

        payment.setStatus(
                PaymentStatus.SUCCESS
        );

        paymentRepository.save(payment);

        // =================================================
        // UPDATE ORDER
        // =================================================

        order.setPaymentId(
                razorpayPaymentId
        );

        order.setStatus(
                OrderStatus.PAID
        );

        orderRepository.save(order);

        return true;
    }

    // =====================================================
    // PAYMENT SUCCESS
    // =====================================================

    @Transactional
    public PaymentResponse paymentSuccess(
            String email,
            Long orderId) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found: "
                                                + orderId
                                )
                        );

        // =================================================
        // OWNER CHECK
        // =================================================

        if (order.getUser() == null
                || order.getUser().getId() == null
                || !order.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You cannot update this payment"
            );
        }

        if (order.getStatus()
                == OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Cancelled order cannot be paid"
            );
        }

        // =================================================
        // ALREADY PAID
        // =================================================

        if (order.getStatus()
                == OrderStatus.PAID) {

            Payment payment =
                    paymentRepository
                            .findByOrder(order)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Payment record not found"
                                    )
                            );

            return new PaymentResponse(
                    orderId.toString(),
                    payment.getPaymentId(),
                    keyId,
                    getAmountInPaise(
                            payment.getAmount()
                    ),
                    "INR",
                    false
            );
        }

        // =================================================
        // FIND PAYMENT
        // =================================================

        Payment payment =
                paymentRepository
                        .findByOrder(order)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found"
                                )
                        );

        String paymentId =
                payment.getPaymentId();

        if (paymentId == null
                || paymentId.isBlank()) {

            throw new RuntimeException(
                    "Payment ID not found"
            );
        }

        /*
         * Payment verification should normally
         * happen through verifyPayment().
         */
        throw new RuntimeException(
                "Payment must be verified through Razorpay verification"
        );
    }

    // =====================================================
    // PAYMENT FAILED
    // =====================================================

    @Transactional
    public PaymentResponse paymentFailed(
            String email,
            Long orderId) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found: "
                                                + orderId
                                )
                        );

        // =================================================
        // OWNER CHECK
        // =================================================

        if (order.getUser() == null
                || order.getUser().getId() == null
                || !order.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You cannot update this payment"
            );
        }

        // =================================================
        // PAID CHECK
        // =================================================

        if (order.getStatus()
                == OrderStatus.PAID) {

            throw new RuntimeException(
                    "Paid order cannot be marked as failed"
            );
        }

        // =================================================
        // FIND PAYMENT
        // =================================================

        Payment payment =
                paymentRepository
                        .findByOrder(order)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found"
                                )
                        );

        payment.setStatus(
                PaymentStatus.FAILED
        );

        paymentRepository.save(payment);

        // =================================================
        // RESET ORDER
        // =================================================

        order.setStatus(
                OrderStatus.CREATED
        );

        orderRepository.save(order);

        return new PaymentResponse(
                orderId.toString(),
                order.getPaymentId(),
                keyId,
                getAmountInPaise(
                        payment.getAmount()
                ),
                "INR",
                false
        );
    }

    // =====================================================
    // DEMO PAYMENT
    // =====================================================

    @Transactional
    public PaymentResponse demoPayment(
            String email,
            Long orderId) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found: " + email
                                )
                        );

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found: " + orderId
                                )
                        );

        // =================================================
        // OWNER CHECK
        // =================================================

        if (order.getUser() == null
                || order.getUser().getId() == null
                || !order.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You cannot pay for this order"
            );
        }

        // =================================================
        // CANCELLED CHECK
        // =================================================

        if (order.getStatus()
                == OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Cancelled order cannot be paid"
            );
        }

        // =================================================
        // AMOUNT VALIDATION
        // =================================================

        if (order.getTotalAmount() <= 0) {

            throw new RuntimeException(
                    "Invalid order amount: ₹"
                            + order.getTotalAmount()
            );
        }

        // =================================================
        // ALREADY PAID
        // =================================================

        if (order.getStatus()
                == OrderStatus.PAID) {

            Payment payment =
                    paymentRepository
                            .findByOrder(order)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Payment record not found"
                                    )
                            );

            return new PaymentResponse(
                    orderId.toString(),
                    payment.getPaymentId(),
                    keyId,
                    getAmountInPaise(
                            payment.getAmount()
                    ),
                    "INR",
                    true
            );
        }

        // =================================================
        // FIND / CREATE PAYMENT
        // =================================================

        Payment payment =
                paymentRepository
                        .findByOrder(order)
                        .orElse(null);

        if (payment == null) {

            payment = new Payment();

            payment.setOrder(order);

            payment.setCreatedAt(
                    LocalDateTime.now()
            );
        }

        // =================================================
        // DEMO PAYMENT ID
        // =================================================

        String demoPaymentId =
                "DEMO_"
                        + orderId
                        + "_"
                        + System.currentTimeMillis();

        payment.setPaymentId(
                demoPaymentId
        );

        payment.setAmount(
                order.getTotalAmount()
        );

        payment.setStatus(
                PaymentStatus.SUCCESS
        );

        paymentRepository.save(payment);

        // =================================================
        // UPDATE ORDER
        // =================================================

        order.setPaymentId(
                demoPaymentId
        );

        order.setStatus(
                OrderStatus.PAID
        );

        orderRepository.save(order);

        // =================================================
        // RESPONSE
        // =================================================

        return new PaymentResponse(
                orderId.toString(),
                demoPaymentId,
                keyId,
                getAmountInPaise(
                        order.getTotalAmount()
                ),
                "INR",
                true
        );
    }

    // =====================================================
    // AMOUNT CONVERSION
    // =====================================================

    private long getAmountInPaise(
            double amount) {

        return BigDecimal
                .valueOf(amount)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .multiply(
                        BigDecimal.valueOf(100)
                )
                .longValueExact();
    }
}