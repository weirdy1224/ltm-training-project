package com.examly.springapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.examly.springapp.dto.CreateOrderRequest;
import com.examly.springapp.dto.VerifyPaymentRequest;
import com.examly.springapp.service.PaymentService;

@RestController
@RequestMapping("/api/payment")

public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestBody
            CreateOrderRequest request)
            throws Exception {

        return ResponseEntity.ok(
                paymentService.createOrder(
                        request.getAmount()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @RequestBody
            VerifyPaymentRequest request) throws Exception {

        return ResponseEntity.ok(
                paymentService
                    .verifyPayment(request));
    }
}