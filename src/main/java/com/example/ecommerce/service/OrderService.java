package com.example.ecommerce.service;

import com.example.ecommerce.dto.CreateOrderRequest;
import com.example.ecommerce.dto.OrderItemResponse;
import com.example.ecommerce.dto.OrderResponse;
import com.example.ecommerce.entity.Address;
import com.example.ecommerce.entity.Cart;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Payment;
import com.example.ecommerce.entity.PaymentStatus;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.AddressRepository;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final AddressRepository addressRepository;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            AddressRepository addressRepository) {

        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.addressRepository = addressRepository;
    }

    // =====================================================
    // CREATE ORDER
    // =====================================================

    @Transactional
    public OrderResponse createOrder(
            String email,
            CreateOrderRequest request) {

        // =================================================
        // FIND USER
        // =================================================

        User user = getUser(email);

        // =================================================
        // VALIDATE REQUEST
        // =================================================

        if (request == null) {
            throw new RuntimeException(
                    "Order request cannot be null"
            );
        }

        if (request.getAddressId() == null) {
            throw new RuntimeException(
                    "Address ID is required"
            );
        }

        // =================================================
        // FIND CART
        // =================================================

        Cart cart =
                cartRepository
                        .findByUser(user)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cart not found"
                                )
                        );

        // =================================================
        // CHECK CART
        // =================================================

        if (cart.getItems() == null
                || cart.getItems().isEmpty()) {

            throw new RuntimeException(
                    "Cart is empty"
            );
        }

        // =================================================
        // FIND ADDRESS
        // =================================================

        Address address =
                addressRepository
                        .findById(
                                request.getAddressId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Address not found"
                                )
                        );

        // =================================================
        // CHECK ADDRESS OWNER
        // =================================================

        if (address.getUser() == null
                || address.getUser().getId() == null
                || !address.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You cannot use this address"
            );
        }

        // =================================================
        // CREATE ORDER
        // =================================================

        Order order = new Order();

        order.setUser(user);

        order.setAddress(address);

        order.setStatus(
                OrderStatus.CREATED
        );

        // =================================================
        // MAKE SURE ITEMS LIST EXISTS
        // =================================================

        if (order.getItems() == null) {

            throw new RuntimeException(
                    "Order items list is not initialized"
            );
        }

        // =================================================
        // SHIPPING
        // =================================================

        double shippingCharge = 50.0;

        double totalAmount = 0.0;

        // =================================================
        // CART ITEMS
        // =================================================

        for (CartItem cartItem :
                cart.getItems()) {

            // =================================================
            // CHECK CART ITEM
            // =================================================

            if (cartItem == null) {

                throw new RuntimeException(
                        "Invalid cart item"
                );
            }

            // =================================================
            // PRODUCT
            // =================================================

            Product product =
                    cartItem.getProduct();

            if (product == null) {

                throw new RuntimeException(
                        "Product not found in cart"
                );
            }

            // =================================================
            // QUANTITY
            // =================================================

            int quantity =
                    cartItem.getQuantity();

            if (quantity <= 0) {

                throw new RuntimeException(
                        "Invalid quantity for product: "
                                + product.getName()
                );
            }

            // =================================================
            // PRICE
            // =================================================

            double productPrice =
                    product.getPrice();

            if (productPrice < 0) {

                throw new RuntimeException(
                        "Invalid product price: "
                                + product.getName()
                );
            }

            // =================================================
            // STOCK CHECK
            //
            // IMPORTANT:
            //
            // Stock is NOT reduced here.
            //
            // Stock is reduced only after successful payment.
            // =================================================

            if (product.getStock() < quantity) {

                throw new RuntimeException(
                        "Not enough stock for product: "
                                + product.getName()
                                + ". Available stock = "
                                + product.getStock()
                );
            }

            // =================================================
            // SUBTOTAL
            // =================================================

            double subtotal =
                    productPrice * quantity;

            totalAmount += subtotal;

            // =================================================
            // CREATE ORDER ITEM
            // =================================================

            OrderItem orderItem =
                    new OrderItem();

            // IMPORTANT:
            // Set the actual order object.
            orderItem.setOrder(order);

            orderItem.setProduct(product);

            orderItem.setQuantity(quantity);

            // Save current price into order item.
            orderItem.setPrice(productPrice);

            // Add item to order.
            order.getItems().add(orderItem);
        }

        // =================================================
        // SHIPPING
        // =================================================

        totalAmount += shippingCharge;

        // =================================================
        // VALIDATE TOTAL
        // =================================================

        if (totalAmount <= 0) {

            throw new RuntimeException(
                    "Invalid order total"
            );
        }

        // =================================================
        // SET TOTAL
        // =================================================

        order.setTotalAmount(
                totalAmount
        );

        // =================================================
        // SAVE ORDER
        // =================================================

        Order savedOrder =
                orderRepository.save(order);

        // =================================================
        // CREATE PAYMENT RECORD
        //
        // Initial status:
        // PENDING
        //
        // Payment ID will be added when:
        //
        // Razorpay order is created
        // OR
        // Demo payment is selected.
        // =================================================

        Payment payment =
                new Payment();

        payment.setOrder(savedOrder);

        payment.setAmount(
                savedOrder.getTotalAmount()
        );

        payment.setStatus(
                PaymentStatus.PENDING
        );

        paymentRepository.save(payment);

        // =================================================
        // KEEP ORDER CREATED
        // =================================================

        savedOrder.setStatus(
                OrderStatus.CREATED
        );

        savedOrder =
                orderRepository.save(
                        savedOrder
                );

        // =================================================
        // IMPORTANT
        //
        // DO NOT CLEAR CART HERE.
        //
        // Cart will be cleared only after successful
        // payment.
        // =================================================

        System.out.println(
                "========================================="
        );

        System.out.println(
                "ORDER CREATED SUCCESSFULLY"
        );

        System.out.println(
                "ORDER ID = "
                        + savedOrder.getId()
        );

        System.out.println(
                "ORDER TOTAL = ₹"
                        + savedOrder.getTotalAmount()
        );

        System.out.println(
                "ORDER STATUS = "
                        + savedOrder.getStatus()
        );

        System.out.println(
                "PAYMENT STATUS = "
                        + payment.getStatus()
        );

        System.out.println(
                "========================================="
        );

        return convertToResponse(
                savedOrder
        );
    }


    // =====================================================
    // COMPLETE SUCCESSFUL PAYMENT
    //
    // Called by PaymentService after:
    //
    // 1. Razorpay signature verified
    // 2. Demo payment successful
    //
    // Performs:
    //
    // Payment SUCCESS
    // Order PAID
    // Stock reduced
    // Cart cleared
    //
    // =====================================================

    @Transactional
    public void completeSuccessfulPayment(
            Long orderId,
            String paymentId) {

        System.out.println(
                "========================================="
        );

        System.out.println(
                "COMPLETE SUCCESSFUL PAYMENT"
        );

        System.out.println(
                "ORDER ID = "
                        + orderId
        );

        System.out.println(
                "PAYMENT ID = "
                        + paymentId
        );

        System.out.println(
                "========================================="
        );

        // =================================================
        // VALIDATION
        // =================================================

        if (orderId == null) {

            throw new RuntimeException(
                    "Order ID cannot be null"
            );
        }

        if (paymentId == null
                || paymentId.isBlank()) {

            throw new RuntimeException(
                    "Payment ID cannot be empty"
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
        // IDEMPOTENCY
        //
        // If already PAID:
        //
        // DO NOT:
        // reduce stock again
        // clear cart again
        // update payment again
        // =================================================

        if (order.getStatus()
                == OrderStatus.PAID) {

            System.out.println(
                    "ORDER ALREADY PAID - NOTHING TO DO"
            );

            return;
        }

        // =================================================
        // CANCELLED CHECK
        // =================================================

        if (order.getStatus()
                == OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Cancelled order cannot be completed"
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
                                        "Payment not found for order: "
                                                + orderId
                                )
                        );

        // =================================================
        // PAYMENT ALREADY SUCCESS
        // =================================================

        if (payment.getStatus()
                == PaymentStatus.SUCCESS) {

            System.out.println(
                    "PAYMENT ALREADY SUCCESS"
            );

            // Make sure order is PAID.
            order.setPaymentId(
                    payment.getPaymentId()
            );

            order.setStatus(
                    OrderStatus.PAID
            );

            orderRepository.save(order);

            return;
        }

        // =================================================
        // ORDER ITEMS CHECK
        // =================================================

        if (order.getItems() == null
                || order.getItems().isEmpty()) {

            throw new RuntimeException(
                    "Order has no items"
            );
        }

        // =================================================
        // FIRST CHECK ALL STOCK
        //
        // IMPORTANT:
        //
        // Check ALL products first.
        //
        // Only if everything is available,
        // reduce stock.
        // =================================================

        for (OrderItem orderItem :
                order.getItems()) {

            if (orderItem == null) {

                throw new RuntimeException(
                        "Invalid order item"
                );
            }

            Product product =
                    orderItem.getProduct();

            if (product == null) {

                throw new RuntimeException(
                        "Product not found for order item"
                );
            }

            int quantity =
                    orderItem.getQuantity();

            if (quantity <= 0) {

                throw new RuntimeException(
                        "Invalid order quantity for product: "
                                + product.getName()
                );
            }

            if (product.getStock() < quantity) {

                throw new RuntimeException(
                        "Insufficient stock for product: "
                                + product.getName()
                                + ". Available = "
                                + product.getStock()
                                + ", Required = "
                                + quantity
                );
            }
        }

        // =================================================
        // REDUCE STOCK
        // =================================================

        for (OrderItem orderItem :
                order.getItems()) {

            Product product =
                    orderItem.getProduct();

            int quantity =
                    orderItem.getQuantity();

            int oldStock =
                    product.getStock();

            int newStock =
                    oldStock - quantity;

            product.setStock(
                    newStock
            );

            System.out.println(
                    "STOCK UPDATED"
            );

            System.out.println(
                    "PRODUCT = "
                            + product.getName()
            );

            System.out.println(
                    "OLD STOCK = "
                            + oldStock
            );

            System.out.println(
                    "SOLD = "
                            + quantity
            );

            System.out.println(
                    "NEW STOCK = "
                            + newStock
            );
        }

        // =================================================
        // UPDATE PAYMENT
        // =================================================

        payment.setPaymentId(
                paymentId
        );

        payment.setAmount(
                order.getTotalAmount()
        );

        payment.setStatus(
                PaymentStatus.SUCCESS
        );

        paymentRepository.save(
                payment
        );

        // =================================================
        // UPDATE ORDER
        // =================================================

        order.setPaymentId(
                paymentId
        );

        order.setStatus(
                OrderStatus.PAID
        );

        orderRepository.save(
                order
        );

        // =================================================
        // CLEAR CART
        // =================================================

        Cart cart =
                cartRepository
                        .findByUser(order.getUser())
                        .orElse(null);

        if (cart != null) {

            if (cart.getItems() != null) {

                cart.getItems().clear();
            }

            cartRepository.save(
                    cart
            );

            System.out.println(
                    "CART CLEARED"
            );
        }

        // =================================================
        // SUCCESS
        // =================================================

        System.out.println(
                "========================================="
        );

        System.out.println(
                "PAYMENT COMPLETED SUCCESSFULLY"
        );

        System.out.println(
                "ORDER ID = "
                        + order.getId()
        );

        System.out.println(
                "PAYMENT ID = "
                        + paymentId
        );

        System.out.println(
                "PAYMENT STATUS = "
                        + payment.getStatus()
        );

        System.out.println(
                "ORDER STATUS = "
                        + order.getStatus()
        );

        System.out.println(
                "STOCK REDUCED = YES"
        );

        System.out.println(
                "CART CLEARED = YES"
        );

        System.out.println(
                "========================================="
        );
    }


    // =====================================================
    // GET MY ORDERS
    // =====================================================

    public List<OrderResponse> getMyOrders(
            String email) {

        User user =
                getUser(email);

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(
                        user.getId()
                )
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    // =====================================================
    // GET MY ORDERS BY USER ID
    // =====================================================

    public List<Order> getMyOrders(
            Long userId) {

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(
                        userId
                );
    }


    // =====================================================
    // GET ALL ORDERS - ADMIN
    // =====================================================

    public List<OrderResponse> getAllOrders() {

        return orderRepository
                .findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    // =====================================================
    // GET ORDER BY ID
    // =====================================================

    public OrderResponse getOrderById(
            String email,
            Long orderId) {

        User user =
                getUser(email);

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                )
                        );

        // =================================================
        // OWNER CHECK
        // =================================================

        if (order.getUser() == null
                || !order.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You cannot access this order"
            );
        }

        return convertToResponse(
                order
        );
    }


    // =====================================================
    // GET ORDER BY ID - INTERNAL
    // =====================================================

    public Order getOrderById(
            Long orderId) {

        return orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found"
                        )
                );
    }


    // =====================================================
    // UPDATE ORDER STATUS - ADMIN
    // =====================================================

    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            OrderStatus status) {

        if (status == null) {

            throw new RuntimeException(
                    "Order status cannot be null"
            );
        }

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                )
                        );

        // =================================================
        // PAID ORDER PROTECTION
        // =================================================

        if (order.getStatus()
                == OrderStatus.PAID
                &&
                (status == OrderStatus.CREATED
                        || status
                        == OrderStatus.PAYMENT_PENDING)) {

            throw new RuntimeException(
                    "Paid order cannot be moved back to payment state"
            );
        }

        order.setStatus(
                status
        );

        Order savedOrder =
                orderRepository.save(
                        order
                );

        return convertToResponse(
                savedOrder
        );
    }


    // =====================================================
    // CANCEL ORDER
    // =====================================================

    @Transactional
    public OrderResponse cancelOrder(
            String email,
            Long orderId) {

        User user =
                getUser(email);

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                )
                        );

        // =================================================
        // OWNER CHECK
        // =================================================

        if (order.getUser() == null
                || !order.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You cannot cancel this order"
            );
        }

        // =================================================
        // ALREADY CANCELLED
        // =================================================

        if (order.getStatus()
                == OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "Order is already cancelled"
            );
        }

        // =================================================
        // PAID ORDER
        // =================================================

        if (order.getStatus()
                == OrderStatus.PAID) {

            throw new RuntimeException(
                    "Paid order cannot be cancelled here"
            );
        }

        // =================================================
        // CANCEL ORDER
        // =================================================

        order.setStatus(
                OrderStatus.CANCELLED
        );

        // =================================================
        // OPTIONAL:
        // Mark pending payment as failed.
        // =================================================

        Payment payment =
                paymentRepository
                        .findByOrder(order)
                        .orElse(null);

        if (payment != null
                && payment.getStatus()
                == PaymentStatus.PENDING) {

            payment.setStatus(
                    PaymentStatus.FAILED
            );

            paymentRepository.save(
                    payment
            );
        }

        Order savedOrder =
                orderRepository.save(
                        order
                );

        return convertToResponse(
                savedOrder
        );
    }


    // =====================================================
    // GET USER BY EMAIL
    // =====================================================

    private User getUser(
            String email) {

        if (email == null
                || email.isBlank()) {

            throw new RuntimeException(
                    "Email cannot be empty"
            );
        }

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );
    }


    // =====================================================
    // CONVERT ORDER TO RESPONSE
    // =====================================================

    private OrderResponse convertToResponse(
            Order order) {

        List<OrderItemResponse> itemResponses =
                order.getItems()
                        .stream()
                        .map(
                                this::convertItemToResponse
                        )
                        .toList();

        return new OrderResponse(
                order.getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getPaymentId(),
                order.getCreatedAt(),
                itemResponses
        );
    }


    // =====================================================
    // CONVERT ORDER ITEM TO RESPONSE
    // =====================================================

    private OrderItemResponse convertItemToResponse(
            OrderItem item) {

        Product product =
                item.getProduct();

        if (product == null) {

            throw new RuntimeException(
                    "Product not found for order item"
            );
        }

        double subtotal =
                item.getPrice()
                        * item.getQuantity();

        return new OrderItemResponse(
                product.getId(),
                product.getName(),
                item.getPrice(),
                item.getQuantity(),
                subtotal
        );
    }
}