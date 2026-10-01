package com.examly.springapp.service;

import com.examly.springapp.model.User;

import java.util.Optional;

public interface UserService {

    User registerUser(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long userId);
}