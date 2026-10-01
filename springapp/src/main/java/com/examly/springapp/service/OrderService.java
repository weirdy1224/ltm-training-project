package com.examly.springapp.service;

import com.examly.springapp.model.Orders;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    Orders addOrder(Orders order);

    List<Orders> getAllOrders();

    Optional<Orders> getOrderById(Long orderId);

    List<Orders> getOrdersByCustomerId(Long customerId);
}