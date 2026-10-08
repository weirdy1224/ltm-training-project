package com.examly.springapp.service;

import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.RateLimitExceededException;
import com.examly.springapp.model.EmailOtp;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.EmailOtpRepo;
import com.examly.springapp.repository.UserRepo;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OTP rules: 6 random digits from SecureRandom, stored only as a BCrypt hash, valid for a few minutes,
 * at most a few wrong guesses, one new OTP per cool-down period. The digits are never logged.
 *
 * @author Lokesh
 */
@Service
public class OtpServiceImpl implements OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    private final EmailOtpRepo otpRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final int validMinutes;
    private final int maxAttempts;
    private final int resendSeconds;
    private final SecureRandom random = new SecureRandom();

    public OtpServiceImpl(EmailOtpRepo otpRepo, UserRepo userRepo, PasswordEncoder passwordEncoder,
                          EmailService emailService,
                          @Value("${app.otp.valid-minutes:10}") int validMinutes,
                          @Value("${app.otp.max-attempts:5}") int maxAttempts,
                          @Value("${app.otp.resend-seconds:60}") int resendSeconds) {
        this.otpRepo = otpRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.validMinutes = validMinutes;
        this.maxAttempts = maxAttempts;
        this.resendSeconds = resendSeconds;
    }

    @Override
    @Transactional
    public void issueAndSend(User user) {
        log.trace("issueAndSend entered for userId={}", user.getUserId());
        otpRepo.deleteByUser_UserId(user.getUserId());   // only the newest OTP may work
        String code = String.format("%06d", random.nextInt(1_000_000));
        otpRepo.save(new EmailOtp(user, passwordEncoder.encode(code), LocalDateTime.now().plusMinutes(validMinutes)));
        // if the mail fails, the exception rolls this transaction back, so no OTP exists that the user never received
        emailService.sendOtpEmail(user.getEmail(), user.getUsername(), code, validMinutes);
        log.info("OTP issued for user {}", user.getUserId());
    }

    /** A wrong guess must still be counted, so this method does NOT roll back for InvalidRequestException. */
    @Override
    @Transactional(noRollbackFor = InvalidRequestException.class)
    public User verify(String email, String otp) {
        log.trace("verify entered");
        String normalised = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepo.findByEmail(normalised)
                .orElseThrow(() -> new InvalidRequestException("Invalid or expired OTP. Please request a new one."));
        if (user.hasVerifiedEmail()) {
            throw new InvalidRequestException("This email is already verified. Please log in.");
        }
        EmailOtp stored = otpRepo.findFirstByUser_UserIdOrderByCreatedAtDesc(user.getUserId())
                .filter(o -> !o.isUsed())
                .orElseThrow(() -> new InvalidRequestException("Invalid or expired OTP. Please request a new one."));
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("This OTP has expired. Please request a new one.");
        }
        if (stored.getAttempts() >= maxAttempts) {
            throw new InvalidRequestException("Too many wrong attempts. Please request a new OTP.");
        }
        if (!passwordEncoder.matches(otp, stored.getCodeHash())) {
            stored.setAttempts(stored.getAttempts() + 1);
            otpRepo.save(stored);
            int left = Math.max(0, maxAttempts - stored.getAttempts());
            log.warn("Wrong OTP for user {} ({} attempt(s) left)", user.getUserId(), left);
            throw new InvalidRequestException("Incorrect OTP. " + left + " attempt(s) left.");
        }
        stored.setUsed(true);
        otpRepo.save(stored);
        user.setEmailVerified(Boolean.TRUE);
        User saved = userRepo.save(user);
        log.info("Email verified for user {}", saved.getUserId());
        return saved;
    }

    @Override
    @Transactional
    public void resend(String email) {
        log.trace("resend entered");
        String normalised = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepo.findByEmail(normalised).orElse(null);
        if (user == null || user.hasVerifiedEmail()) {
            return;   // say nothing: the answer must not reveal which emails are registered
        }
        Optional<EmailOtp> latest = otpRepo.findFirstByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
        if (latest.isPresent()) {
            long waited = Duration.between(latest.get().getCreatedAt(), LocalDateTime.now()).getSeconds();
            if (waited < resendSeconds) {
                throw new RateLimitExceededException("Please wait before asking for another OTP", resendSeconds - waited);
            }
        }
        issueAndSend(user);
    }

    /** Housekeeping: expired OTPs are useless, so remove them every night. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpired() {
        long removed = otpRepo.deleteByExpiresAtBefore(LocalDateTime.now().minusDays(1));
        log.debug("Purged {} old OTP(s)", removed);
    }
}
