package com.example.ecommerce.dto;

import java.util.List;

public class SalesReportResponse {

    private Double totalSales;
    private Long totalOrders;

    private List<MonthlySales> monthlySales;
    private List<StatusSales> orderStatus;

    public SalesReportResponse() {
    }

    public SalesReportResponse(
            Double totalSales,
            Long totalOrders,
            List<MonthlySales> monthlySales,
            List<StatusSales> orderStatus) {

        this.totalSales = totalSales;
        this.totalOrders = totalOrders;
        this.monthlySales = monthlySales;
        this.orderStatus = orderStatus;
    }

    public Double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(Double totalSales) {
        this.totalSales = totalSales;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public List<MonthlySales> getMonthlySales() {
        return monthlySales;
    }

    public void setMonthlySales(List<MonthlySales> monthlySales) {
        this.monthlySales = monthlySales;
    }

    public List<StatusSales> getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(List<StatusSales> orderStatus) {
        this.orderStatus = orderStatus;
    }


    public static class MonthlySales {

        private String month;
        private Double sales;

        public MonthlySales() {
        }

        public MonthlySales(String month, Double sales) {
            this.month = month;
            this.sales = sales;
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public Double getSales() {
            return sales;
        }

        public void setSales(Double sales) {
            this.sales = sales;
        }
    }


    public static class StatusSales {

        private String status;
        private Long count;

        public StatusSales() {
        }

        public StatusSales(String status, Long count) {
            this.status = status;
            this.count = count;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
}