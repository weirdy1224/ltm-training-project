package com.examly.springapp.service;

import com.examly.springapp.model.Customer;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.CustomerRepo;
import com.examly.springapp.repository.UserRepo;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepo customerRepo;
    private final UserRepo userRepo;

    public CustomerServiceImpl(
            CustomerRepo customerRepo,
            UserRepo userRepo) {

        this.customerRepo = customerRepo;
        this.userRepo = userRepo;
    }

    @Override
    public Customer addCustomer(Customer customer) {

        if (customer.getUser() != null
                && customer.getUser().getUserId() != null) {

            Long userId = customer.getUser().getUserId();

            User user = userRepo.findById(userId)
                    .orElseThrow(
                            () -> new RuntimeException("User not found")
                    );

            customer.setUser(user);
        }

        return customerRepo.save(customer);
    }

    @Override
    public Optional<Customer> getCustomerByUserId(Long userId) {
        return customerRepo.findByUserUserId(userId);
    }
}