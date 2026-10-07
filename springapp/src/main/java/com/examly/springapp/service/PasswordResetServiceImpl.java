package com.examly.springapp.service;

import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.model.PasswordResetToken;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.PasswordResetTokenRepo;
import com.examly.springapp.repository.RefreshTokenRepo;
import com.examly.springapp.repository.UserRepo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String PASSWORD_POLICY =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,72}$";

    private static final int TOKEN_VALID_MINUTES = 15;

    private final UserRepo userRepo;
    private final PasswordResetTokenRepo tokenRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    private final String frontendUrl;
    private final String mailFrom;

    public PasswordResetServiceImpl(
            UserRepo userRepo,
            PasswordResetTokenRepo tokenRepo,
            RefreshTokenRepo refreshTokenRepo,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${spring.mail.username}") String mailFrom) {

        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.mailFrom = mailFrom;
    }

    @Override
    @Transactional
    public void requestReset(String email) {

        String normalised =
                email.trim().toLowerCase(Locale.ROOT);

        User user = userRepo.findByEmail(normalised).orElse(null);

        // Always return normally when the email is unknown.
        // This prevents revealing which email addresses are registered.
        if (user == null) {
            return;
        }

        // Invalidate any previous reset links for this user.
        tokenRepo.deleteByUser_UserId(user.getUserId());

        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        tokenHash,
                        user,
                        LocalDateTime.now()
                                .plusMinutes(TOKEN_VALID_MINUTES)
                );

        tokenRepo.save(resetToken);

        String resetLink = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/reset-password")
                .queryParam("token", rawToken)
                .build()
                .toUriString();

        sendResetEmail(user.getEmail(), resetLink);
    }

    @Override
    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword) {

        if (newPassword == null ||
                !newPassword.matches(PASSWORD_POLICY)) {

            throw new InvalidRequestException(
                    "Password must be 8 to 72 characters and include an uppercase letter, lowercase letter, number and special character, with no spaces"
            );
        }

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                tokenRepo.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new InvalidRequestException(
                                        "Invalid or expired password reset link"
                                ));

        if (resetToken.isUsed()) {
            throw new InvalidRequestException(
                    "This password reset link has already been used"
            );
        }

        if (resetToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new InvalidRequestException(
                    "This password reset link has expired"
            );
        }

        User user = resetToken.getUser();

        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        userRepo.save(user);

        // Mark this reset link as unusable.
        resetToken.setUsed(true);
        tokenRepo.save(resetToken);

        // End existing refresh-token sessions.
        refreshTokenRepo.deleteByUser_UserId(
                user.getUserId()
        );
    }

    private String generateToken() {

        byte[] bytes = new byte[32];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 is not available",
                    e
            );
        }
    }

    private void sendResetEmail(
            String email,
            String resetLink) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(mailFrom);
        message.setTo(email);

        message.setSubject(
                "Reset your E-Learning Platform password"
        );

        message.setText(
                "We received a request to reset your password.\n\n" +
                "Use the following link to create a new password:\n\n" +
                resetLink +
                "\n\nThis link expires in 15 minutes.\n\n" +
                "If you did not request a password reset, you can ignore this email."
        );

        mailSender.send(message);
    }
}