package com.examly.springapp.controller;

import com.examly.springapp.model.Cart;
import com.examly.springapp.service.CartService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(
            CartService cartService) {

        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<Cart> addCart(
            @RequestBody Cart cart) {

        Cart savedCart =
                cartService.addCart(cart);

        return new ResponseEntity<>(
                savedCart,
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<List<Cart>> getAllCarts() {

        return ResponseEntity.ok(
                cartService.getAllCarts()
        );
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<Cart> getCartById(
            @PathVariable Long cartId) {

        return cartService
                .getCartById(cartId)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Cart>> getCartsByCustomerId(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                cartService.getCartsByCustomerId(customerId)
        );
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> deleteCart(
            @PathVariable Long cartId) {

        cartService.deleteCart(cartId);

        return ResponseEntity.noContent().build();
    }
}