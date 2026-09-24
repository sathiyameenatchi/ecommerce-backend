package com.example.ecommerce.service;

import com.example.ecommerce.entity.AdminDashboardResponse;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository) {

        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public AdminDashboardResponse getDashboardData() {

        long totalUsers = userRepository.count();

        long totalProducts = productRepository.count();

        long totalOrders = orderRepository.count();

        List<Order> orders = orderRepository.findAll();

        double totalSales = 0;

        for (Order order : orders) {

            if (order.getTotalAmount() != null) {
                totalSales += order.getTotalAmount();
            }
        }

        long createdOrders =
                orderRepository.countByStatus(OrderStatus.CREATED);

        long paidOrders =
                orderRepository.countByStatus(OrderStatus.PAID);

        long processingOrders =
                orderRepository.countByStatus(OrderStatus.PROCESSING);

        long shippedOrders =
                orderRepository.countByStatus(OrderStatus.SHIPPED);

        long deliveredOrders =
                orderRepository.countByStatus(OrderStatus.DELIVERED);

        long cancelledOrders =
                orderRepository.countByStatus(OrderStatus.CANCELLED);

        return new AdminDashboardResponse(
                totalUsers,
                totalProducts,
                totalOrders,
                totalSales,
                createdOrders,
                paidOrders,
                processingOrders,
                shippedOrders,
                deliveredOrders,
                cancelledOrders
        );
    }
}