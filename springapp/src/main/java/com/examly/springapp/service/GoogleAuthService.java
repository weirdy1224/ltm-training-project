package com.examly.springapp.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.examly.springapp.model.Customer;
import com.examly.springapp.repository.CustomerRepo;

@Service
public class GoogleAuthService {

    @Value("${google.client-id}")
    private String clientId;

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepo customerRepo;

    public GoogleAuthService(
            UserRepo userRepo,
            CustomerRepo customerRepo,
            PasswordEncoder passwordEncoder) {

        this.userRepo = userRepo;
        this.customerRepo = customerRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public User googleLogin(String idTokenString) throws Exception {

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance())
                .setAudience(
                        Collections.singletonList(clientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);

        if (idToken == null) {
            throw new BadCredentialsException(
                    "Invalid Google token");
        }

        Payload payload = idToken.getPayload();

        String email = payload.getEmail();

        String name = (String) payload.get("name");

        User user = userRepo.findByEmail(email)
                .orElseGet(() -> {

                    User u = new User();

                    u.setEmail(email);

                    u.setUsername(
                            name != null && !name.isBlank()
                                    ? name
                                    : email.split("@")[0]);

                    u.setMobileNumber("0000000000");

                    u.setRole("CUSTOMER");

                    u.setPassword(
                            passwordEncoder.encode(
                                    "GOOGLE_LOGIN_USER"));

                    return userRepo.save(u);
                });

        customerRepo
                .findByUser_UserId(user.getUserId())
                .orElseGet(() -> {

                    Customer customer = new Customer();

                    customer.setCustomerName(
                            user.getUsername());

                    customer.setInformation(
                            "Signed in with Google");

                    customer.setUser(user);

                    return customerRepo.save(customer);
                });

        return user;
    }
}