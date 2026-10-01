package com.examly.springapp.repository;

import com.examly.springapp.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartRepo extends JpaRepository<Cart, Long> {

    List<Cart> findByCustomerCustomerId(Long customerId);
}