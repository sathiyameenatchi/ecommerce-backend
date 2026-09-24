package com.example.ecommerce.dto;


import java.util.List;

public class CartResponse {

    private Long cartId;
    private List<CartItemResponse> items;
    private Double totalAmount;

    public CartResponse() {
    }

    public CartResponse(
            Long cartId,
            List<CartItemResponse> items,
            Double totalAmount) {

        this.cartId = cartId;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}