package com.example.ecommerce.dto;

public class PaymentResponse {

    private String orderId;
    private String razorpayOrderId;
    private String keyId;
    private Long amount;
    private String currency;

    // NEW
    private boolean demoPayment;

    public PaymentResponse() {
    }

    public PaymentResponse(
            String orderId,
            String razorpayOrderId,
            String keyId,
            Long amount,
            String currency,
            boolean demoPayment) {

        this.orderId = orderId;
        this.razorpayOrderId = razorpayOrderId;
        this.keyId = keyId;
        this.amount = amount;
        this.currency = currency;
        this.demoPayment = demoPayment;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public String getKeyId() {
        return keyId;
    }

    public Long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    // NEW
    public boolean isDemoPayment() {
        return demoPayment;
    }
}