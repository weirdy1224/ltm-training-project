package com.examly.springapp.model;

import com.examly.springapp.dto.InstructorProfileRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * An account (ADMIN, INSTRUCTOR or CUSTOMER). The password is stored as a BCrypt hash and never serialised to JSON.
 *
 * @author Suriya
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 120, message = "Email is too long")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username is too long")
    private String username;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @NotBlank(message = "Role is required")
    private String role;

    /**
     * True once the owner typed the OTP that was emailed to them. READ_ONLY: a client can never set it in a request body.
     * Nullable on purpose: accounts created before this column existed have null and count as verified.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean emailVerified;

    /**
     * Instructors must be approved by an admin before they can log in (customers and admins are approved at once).
     * READ_ONLY and nullable for the same reasons as emailVerified.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean approved;

    /**
     * Only used while registering as an INSTRUCTOR (experience, skills, about). It is not a column of this table:
     * the service saves it as an InstructorProfile. WRITE_ONLY so it is never sent back in a response.
     */
    @Transient
    @Valid
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private InstructorProfileRequest instructorProfile;

    /** No-arg constructor required by JPA and Jackson. */
    public User() {}

    /** Convenience constructor for a new account (no id yet); chains to the all-fields constructor. */
    public User(String email, String password, String username, String mobileNumber, String role) {
        this(null, email, password, username, mobileNumber, role);
    }

    /** All-fields constructor; chains to the no-arg constructor. */
    public User(Long userId, String email, String password, String username, String mobileNumber, String role) {
        this();
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.username = username;
        this.mobileNumber = mobileNumber;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public InstructorProfileRequest getInstructorProfile() { return instructorProfile; }
    public void setInstructorProfile(InstructorProfileRequest instructorProfile) { this.instructorProfile = instructorProfile; }

    /** Old accounts (null) count as verified, so adding the column never locks anybody out. */
    public boolean hasVerifiedEmail() { return emailVerified == null || emailVerified; }

    /** Old accounts (null) count as approved. */
    public boolean hasBeenApproved() { return approved == null || approved; }
}