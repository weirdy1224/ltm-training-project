package com.examly.springapp.dto;

public class CreateOrderRequest {

    private Double amount;

    public CreateOrderRequest() {
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
