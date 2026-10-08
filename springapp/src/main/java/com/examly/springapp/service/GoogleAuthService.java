package com.examly.springapp.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.examly.springapp.exception.AccountNotActiveException;
import com.examly.springapp.model.Customer;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.CustomerRepo;
import com.examly.springapp.repository.UserRepo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

/**
 * "Sign in with Google": checks the ID token with Google, then finds or creates the matching account.
 * Safety rules: Google must say the email is verified; a new account gets a long RANDOM password
 * (nobody knows it, so it cannot be used at /api/login); an instructor who is still waiting for admin
 * approval cannot get in this way either.
 *
 * @author Lokesh
 */
@Service
public class GoogleAuthService {
    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    @Value("${google.client-id}")
    private String clientId;

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepo customerRepo;
    private final SecureRandom random = new SecureRandom();

    public GoogleAuthService(
            UserRepo userRepo,
            CustomerRepo customerRepo,
            PasswordEncoder passwordEncoder) {

        this.userRepo = userRepo;
        this.customerRepo = customerRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public User googleLogin(String idTokenString) throws Exception {
        log.trace("googleLogin entered");
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new BadCredentialsException("Invalid Google token");
        }

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

        if (payload.getEmail() == null || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            log.warn("Google login refused: the Google email is missing or not verified");
            throw new BadCredentialsException("Google email is not verified");
        }

        String email = payload.getEmail().trim().toLowerCase(Locale.ROOT);

        String name = (String) payload.get("name");

        User user = userRepo.findByEmail(email)
                .orElseGet(() -> {

                    User u = new User();

                    u.setEmail(email);

                    String username = name != null && !name.isBlank()
                            ? name
                            : email.split("@")[0];
                    u.setUsername(username.length() > 50 ? username.substring(0, 50) : username);

                    u.setMobileNumber("0000000000");

                    u.setRole("CUSTOMER");

                    u.setPassword(passwordEncoder.encode(randomPassword()));

                    u.setEmailVerified(Boolean.TRUE);
                    u.setApproved(Boolean.TRUE);

                    return userRepo.save(u);
                });

        if (!user.hasBeenApproved()) {
            throw new AccountNotActiveException(AccountNotActiveException.APPROVAL_PENDING,
                    "Your instructor account is waiting for admin approval");
        }
        if (!user.hasVerifiedEmail()) {
            // Google just proved the owner controls this address, so the OTP step is no longer needed
            user.setEmailVerified(Boolean.TRUE);
            user = userRepo.save(user);
        }

        if ("CUSTOMER".equals(user.getRole())) {
            final User owner = user;
            customerRepo
                    .findByUser_UserId(owner.getUserId())
                    .orElseGet(() -> {

                        Customer customer = new Customer();

                        customer.setCustomerName(
                                owner.getUsername());

                        customer.setInformation(
                                "Signed in with Google");

                        customer.setUser(owner);

                        return customerRepo.save(customer);
                    });
        }

        return user;
    }

    /** 32 random bytes as text: far longer than anybody could guess, and different for every account. */
    private String randomPassword() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
