package com.examly.springapp.dto;

import java.time.LocalDateTime;

/**
 * Progress of an instructor application, as shown on the public "track my application" page. It deliberately holds no
 * personal data: only the step, a message and the dates.
 *
 * @author Lokesh
 */
public class ApplicationStatusResponse {
    /** The application has not been found (never submitted, or it was rejected and removed). */
    public static final String NOT_FOUND = "NOT_FOUND";
    /** Submitted, but the email OTP has not been entered yet. */
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    /** Email verified; waiting for an admin to read the application. */
    public static final String UNDER_REVIEW = "UNDER_REVIEW";
    /** An admin approved it: the instructor can log in. */
    public static final String APPROVED = "APPROVED";

    private String status;
    private String message;
    private LocalDateTime submittedAt;

    /** No-arg constructor required by Jackson. */
    public ApplicationStatusResponse() {}

    /** All-fields constructor; chains to the no-arg constructor. */
    public ApplicationStatusResponse(String status, String message, LocalDateTime submittedAt) {
        this();
        this.status = status;
        this.message = message;
        this.submittedAt = submittedAt;
    }

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
