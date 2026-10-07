package com.examly.springapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The email an instructor types on the "track my application" page.
 *
 * @author Lokesh
 */
public class ApplicationStatusRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 120, message = "Email is too long")
    private String email;

    /** No-arg constructor required by Jackson. */
    public ApplicationStatusRequest() {}

    /** Convenience constructor; chains to the no-arg constructor. */
    public ApplicationStatusRequest(String email) {
        this();
        this.email = email;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
