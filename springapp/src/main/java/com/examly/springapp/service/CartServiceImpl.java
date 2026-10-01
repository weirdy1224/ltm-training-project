package com.examly.springapp.service;

import com.examly.springapp.model.Cart;
import com.examly.springapp.model.Course;
import com.examly.springapp.model.Customer;

import com.examly.springapp.repository.CartRepo;
import com.examly.springapp.repository.CourseRepo;
import com.examly.springapp.repository.CustomerRepo;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepo cartRepo;
    private final CustomerRepo customerRepo;
    private final CourseRepo courseRepo;

    public CartServiceImpl(
            CartRepo cartRepo,
            CustomerRepo customerRepo,
            CourseRepo courseRepo) {

        this.cartRepo = cartRepo;
        this.customerRepo = customerRepo;
        this.courseRepo = courseRepo;
    }

    @Override
    public Cart addCart(Cart cart) {

        if (cart.getCustomer() != null
                && cart.getCustomer().getCustomerId() != null) {

            Customer customer = customerRepo
                    .findById(cart.getCustomer().getCustomerId())
                    .orElseThrow(
                            () -> new RuntimeException("Customer not found")
                    );

            cart.setCustomer(customer);
        }

        if (cart.getCourse() != null
                && cart.getCourse().getCourseId() != null) {

            Course course = courseRepo
                    .findById(cart.getCourse().getCourseId())
                    .orElseThrow(
                            () -> new RuntimeException("Course not found")
                    );

            cart.setCourse(course);
        }

        return cartRepo.save(cart);
    }

    @Override
    public List<Cart> getAllCarts() {
        return cartRepo.findAll();
    }

    @Override
    public Optional<Cart> getCartById(Long cartId) {
        return cartRepo.findById(cartId);
    }

    @Override
    public List<Cart> getCartsByCustomerId(Long customerId) {
        return cartRepo.findByCustomerCustomerId(customerId);
    }

    @Override
    public void deleteCart(Long cartId) {
        cartRepo.deleteById(cartId);
    }
}