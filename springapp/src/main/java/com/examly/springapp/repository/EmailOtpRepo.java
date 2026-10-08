package com.examly.springapp.repository;

import com.examly.springapp.model.EmailOtp;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for emailed OTPs.
 *
 * @author Lokesh
 */
@Repository
public interface EmailOtpRepo extends JpaRepository<EmailOtp, Long> {
    /** The newest OTP of a user (used or not), so the resend cool-down can look at its creation time. */
    Optional<EmailOtp> findFirstByUser_UserIdOrderByCreatedAtDesc(Long userId);

    void deleteByUser_UserId(Long userId);

    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
