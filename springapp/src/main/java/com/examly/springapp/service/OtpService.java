package com.examly.springapp.service;

import com.examly.springapp.model.User;

/**
 * One-time passwords that prove a new user owns the email address they registered with.
 *
 * @author Lokesh
 */
public interface OtpService {
    /** Creates a fresh OTP (older ones stop working) and emails it. */
    void issueAndSend(User user);

    /** Checks the 6 digits. On success the account's email is marked verified; otherwise InvalidRequestException. */
    User verify(String email, String otp);

    /**
     * Sends a new OTP. Silent when the email is unknown or already verified (so it reveals nothing);
     * RateLimitExceededException when the previous OTP was sent less than the cool-down ago.
     */
    void resend(String email);
}
