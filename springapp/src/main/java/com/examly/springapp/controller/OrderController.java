package com.examly.springapp.controller;

import com.examly.springapp.model.Orders;
import com.examly.springapp.service.OrderService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(
            OrderService orderService) {

        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Orders> addOrder(
            @RequestBody Orders order) {

        Orders savedOrder =
                orderService.addOrder(order);

        return new ResponseEntity<>(
                savedOrder,
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<List<Orders>> getAllOrders() {

        return ResponseEntity.ok(
                orderService.getAllOrders()
        );
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Orders>> getOrdersByCustomerId(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                orderService.getOrdersByCustomerId(customerId)
        );
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Orders> getOrderById(
            @PathVariable Long orderId) {

        return orderService
                .getOrderById(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }
}