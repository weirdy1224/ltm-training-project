package com.examly.springapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body of POST /api/resend-otp.
 *
 * @author Lokesh
 */
public class ResendOtpRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    public ResendOtpRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
