package com.examly.springapp.service;

import com.examly.springapp.model.Customer;

import java.util.Optional;

public interface CustomerService {

    Customer addCustomer(Customer customer);

    Optional<Customer> getCustomerByUserId(Long userId);
}