package com.example.ecommerce.service;

import com.example.ecommerce.dto.SalesReportResponse;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SalesReportService {

    private final OrderRepository orderRepository;

    public SalesReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }


    public SalesReportResponse getSalesReport() {

        List<Order> orders =
                orderRepository.findAllByOrderByCreatedAtDesc();


        // -------------------------
        // TOTAL SALES
        // -------------------------

        double totalSales = orders.stream()
                .filter(order ->
                        order.getStatus() == OrderStatus.PAID ||
                                order.getStatus() == OrderStatus.PROCESSING ||
                                order.getStatus() == OrderStatus.SHIPPED ||
                                order.getStatus() == OrderStatus.DELIVERED
                )
                .mapToDouble(order ->
                        order.getTotalAmount()
                )
                .sum();


        // -------------------------
        // TOTAL ORDERS
        // -------------------------

        long totalOrders = orders.size();


        // -------------------------
        // MONTHLY SALES
        // -------------------------

        Map<String, Double> monthlyMap =
                new LinkedHashMap<>();


        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM yyyy");


        for (Order order : orders) {

            if (order.getCreatedAt() == null) {
                continue;
            }


            if (order.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }


            String month =
                    order.getCreatedAt().format(formatter);


            monthlyMap.put(
                    month,
                    monthlyMap.getOrDefault(month, 0.0)
                            + order.getTotalAmount()
            );
        }


        List<SalesReportResponse.MonthlySales>
                monthlySales = new ArrayList<>();


        for (Map.Entry<String, Double> entry
                : monthlyMap.entrySet()) {

            monthlySales.add(
                    new SalesReportResponse.MonthlySales(
                            entry.getKey(),
                            entry.getValue()
                    )
            );
        }


        // -------------------------
        // ORDER STATUS
        // -------------------------

        List<SalesReportResponse.StatusSales>
                statusSales = new ArrayList<>();


        for (OrderStatus status : OrderStatus.values()) {

            long count =
                    orders.stream()
                            .filter(order ->
                                    order.getStatus() == status)
                            .count();


            statusSales.add(
                    new SalesReportResponse.StatusSales(
                            status.name(),
                            count
                    )
            );
        }


        return new SalesReportResponse(
                totalSales,
                totalOrders,
                monthlySales,
                statusSales
        );
    }
}

