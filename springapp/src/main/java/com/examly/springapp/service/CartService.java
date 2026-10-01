package com.examly.springapp.service;

import com.examly.springapp.model.Cart;

import java.util.List;
import java.util.Optional;

public interface CartService {

    Cart addCart(Cart cart);

    List<Cart> getAllCarts();

    Optional<Cart> getCartById(Long cartId);

    List<Cart> getCartsByCustomerId(Long customerId);

    void deleteCart(Long cartId);
}