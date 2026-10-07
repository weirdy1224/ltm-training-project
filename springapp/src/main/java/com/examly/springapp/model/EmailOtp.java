package com.examly.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One emailed one-time password for confirming a new account. Only a BCrypt hash of the 6 digits is stored,
 * never the digits themselves. An OTP stops working when it is used, expires, or has been guessed wrong too often.
 *
 * @author Lokesh
 */
@Entity
@Table(name = "email_otps")
public class EmailOtp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Wrong guesses so far. */
    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private boolean used = false;

    /** No-arg constructor required by JPA. */
    public EmailOtp() {}

    /** New OTP for a user; chains to the no-arg constructor. */
    public EmailOtp(User user, String codeHash, LocalDateTime expiresAt) {
        this();
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
