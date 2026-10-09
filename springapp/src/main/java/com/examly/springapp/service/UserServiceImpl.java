package com.examly.springapp.service;

import com.examly.springapp.dto.ApplicationStatusResponse;
import com.examly.springapp.dto.InstructorApplicationDTO;
import com.examly.springapp.dto.InstructorProfileRequest;
import com.examly.springapp.exception.AccountNotActiveException;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.InstructorProfile;
import com.examly.springapp.model.LoginDTO;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.EmailOtpRepo;
import com.examly.springapp.repository.InstructorProfileRepo;
import com.examly.springapp.repository.RefreshTokenRepo;
import com.examly.springapp.repository.UserRepo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.examly.springapp.model.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login rules: unique email, password policy, public sign-up for customers and instructors only (email OTP,
 * instructors also need admin approval), admin management and timing-safe credential checks.
 * Passwords and tokens are never written to the logs.
 *
 * @author Suriya
 */
@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String PASSWORD_POLICY =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,72}$";

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepo refreshTokenRepo;
    private final EmailOtpRepo emailOtpRepo;
    private final InstructorProfileRepo profileRepo;
    private final OtpService otpService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    /** The main admin from application.properties; this account can never be removed. */
    private final String mainAdminEmail;
    /** Compared against when the email is unknown so response time does not reveal which emails exist. */
    private final String dummyHash;

    public UserServiceImpl(UserRepo userRepo, PasswordEncoder passwordEncoder, RefreshTokenRepo refreshTokenRepo,
                           EmailOtpRepo emailOtpRepo, InstructorProfileRepo profileRepo, OtpService otpService, EmailService emailService,
                           NotificationService notificationService,
                           @Value("${app.admin.email}") String mainAdminEmail) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepo = refreshTokenRepo;
        this.emailOtpRepo = emailOtpRepo;
        this.profileRepo = profileRepo;
        this.otpService = otpService;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.mainAdminEmail = mainAdminEmail.trim().toLowerCase(Locale.ROOT);
        this.dummyHash = passwordEncoder.encode("not-a-real-password-1");
    }

    @Override
    public User register(User user) {
        log.trace("register entered");
        String requested = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        // Business rule: nobody can sign themselves up as an admin. Admins are created by the seeder or by another admin.
        if (requested.equals("ADMIN")) {
            log.warn("Public registration refused: ADMIN role requested");
            throw new AccessDeniedException("Admin accounts can only be created by an administrator");
        }
        boolean instructor = requested.equals("INSTRUCTOR");
        InstructorProfileRequest profile = user.getInstructorProfile();
        // Business rule: an admin decides about an instructor from these details, so they are mandatory for instructors.
        if (instructor && profile == null) {
            throw new InvalidRequestException("Please fill in your instructor profile: experience, skills and about you");
        }
        // customers are approved at once; an instructor waits for an admin. Nobody is verified before typing the OTP.
        User saved = create(user, instructor ? "INSTRUCTOR" : "CUSTOMER", false, !instructor, true);
        if (instructor) {
            saveProfile(saved, profile);
        }
        otpService.issueAndSend(saved);
        return saved;
    }

    /** Stores the instructor's details; a replaced unverified sign-up keeps one row and just gets new values. */
    private void saveProfile(User saved, InstructorProfileRequest request) {
        DatabaseOperationException.guard("saving the instructor profile", () -> {
            InstructorProfile row = profileRepo.findByUser_UserId(saved.getUserId()).orElse(null);
            if (row == null) {
                row = new InstructorProfile(saved, request);
            } else {
                row.applyFrom(request);
            }
            return profileRepo.save(row);
        });
        log.info("Instructor profile saved for user {}", saved.getUserId());
    }

    @Override
    public User createAdmin(User user) {
        log.trace("createAdmin entered");
        // an admin is added by another admin who vouches for the address, so there is no OTP step
        return create(user, "ADMIN", true, true, false);
    }

    @Override
    public List<User> listAdmins() {
        log.trace("listAdmins entered");
        return DatabaseOperationException.guard("listing admins", () -> userRepo.findByRole("ADMIN"));
    }

    @Override
    @Transactional
    public void deleteAdmin(Long userId, String currentEmail) {
        log.trace("deleteAdmin entered");
        User target = DatabaseOperationException.guard("loading a user", () -> userRepo.findById(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (!"ADMIN".equals(target.getRole())) {
            throw new InvalidRequestException("Only admin accounts can be removed here");
        }
        // Business rules: the main admin is permanent, you cannot lock yourself out, and one admin always remains.
        if (target.getEmail().equalsIgnoreCase(mainAdminEmail)) {
            throw new InvalidRequestException("The main admin cannot be removed");
        }
        if (target.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new InvalidRequestException("You cannot delete your own account");
        }
        if (userRepo.countByRole("ADMIN") <= 1) {
            throw new InvalidRequestException("At least one admin must remain");
        }
        try {
            refreshTokenRepo.deleteByUser_UserId(userId);   // end the removed admin's sessions first
            userRepo.delete(target);
            log.info("Admin {} removed", userId);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Database error while removing an admin", e);
        }
    }

    /**
     * Shared by sign-up and admin creation: password policy, unique email, BCrypt, save.
     *
     * @param replaceUnverified true for public sign-up: when the email belongs to an account that never verified it,
     *                          that half-finished sign-up is overwritten (otherwise a stranger who typed somebody's
     *                          address first would lock the real owner out).
     */
    private User create(User user, String role, boolean emailVerified, boolean approved, boolean replaceUnverified) {
        if (user.getPassword() == null || !user.getPassword().matches(PASSWORD_POLICY)) {
            throw new InvalidRequestException(
                "Password must be 8 to 72 characters and include an uppercase letter, lowercase letter, number and special character, with no spaces"
            );
        }
        String email = user.getEmail().trim().toLowerCase(Locale.ROOT);
        Optional<User> existing = DatabaseOperationException.guard("checking the email", () -> userRepo.findByEmail(email));
        Long reuseId = null;
        if (existing.isPresent()) {
            boolean abandoned = replaceUnverified && !existing.get().hasVerifiedEmail()
                    && !"ADMIN".equals(existing.get().getRole());
            if (!abandoned) {
                throw new DuplicateResourceException("A user with this email already exists");
            }
            reuseId = existing.get().getUserId();
            log.info("Unverified sign-up of user {} is being replaced", reuseId);
        }
        user.setUserId(reuseId);
        user.setEmail(email);
        user.setRole(role);
        user.setEmailVerified(emailVerified);   // never taken from the request body (the JSON field is read-only)
        user.setApproved(approved);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        try {
            User saved = userRepo.save(user);
            log.info("User {} created with role {}", saved.getUserId(), role);
            return saved;
        } catch (DataIntegrityViolationException e) {
            // two requests used the same email at the same moment (unique constraint)
            throw new DuplicateResourceException("A user with this email already exists");
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Database error while creating a user", e);
        }
    }

    @Override
    public User authenticate(LoginDTO login) {
        log.trace("authenticate entered");
        String email = login.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = DatabaseOperationException.guard("loading a user", () -> userRepo.findByEmail(email)).orElse(null);
        boolean ok = passwordEncoder.matches(login.getPassword(), user == null ? dummyHash : user.getPassword());
        if (user == null || !ok) {
            log.warn("Login failed: wrong email or password");   // never say which of the two was wrong
            throw new BadCredentialsException("Invalid credentials");
        }
        // the password was right, so it is safe to explain why the account cannot be used yet
        if (!user.hasVerifiedEmail()) {
            log.info("Login refused for user {}: email not verified", user.getUserId());
            throw new AccountNotActiveException(AccountNotActiveException.EMAIL_NOT_VERIFIED,
                    "Please verify your email with the OTP we sent you");
        }
        if (!user.hasBeenApproved()) {
            log.info("Login refused for user {}: waiting for admin approval", user.getUserId());
            throw new AccountNotActiveException(AccountNotActiveException.APPROVAL_PENDING,
                    "Your instructor account is waiting for admin approval");
        }
        log.info("User {} logged in", user.getUserId());
        return user;
    }

    @Override
    public List<InstructorApplicationDTO> listInstructors() {
        log.trace("listInstructors entered");
        List<User> list = DatabaseOperationException.guard("listing instructors", () -> userRepo.findByRole("INSTRUCTOR"));
        List<User> sorted = new java.util.ArrayList<>(list);
        sorted.sort((a, b) -> Long.compare(b.getUserId(), a.getUserId()));   // newest first
        Map<Long, InstructorProfile> profiles = new HashMap<>();
        for (InstructorProfile p : DatabaseOperationException.guard("listing instructor profiles", () -> profileRepo.findAll())) {
            profiles.put(p.getUser().getUserId(), p);
        }
        return sorted.stream().map(u -> new InstructorApplicationDTO(u, profiles.get(u.getUserId()))).toList();
    }

    @Override
    public User approveInstructor(Long userId) {
        log.trace("approveInstructor entered");
        User target = loadInstructor(userId);
        target.setApproved(Boolean.TRUE);
        User saved = DatabaseOperationException.guard("approving an instructor", () -> userRepo.save(target));
        log.info("Instructor {} approved", userId);
        emailService.sendInstructorApprovedEmail(saved.getEmail(), saved.getUsername());

        // Notify the approved instructor
        try {
            notificationService.createNotification(
                    saved.getUserId(),
                    "Instructor Application Approved",
                    "Congratulations " + saved.getUsername() + "! Your instructor application has been approved. You can now access your instructor dashboard and create courses.",
                    NotificationType.INSTRUCTOR_APPROVAL,
                    "/instructor/dashboard"
            );
        } catch (Exception ex) {
            log.warn("Failed to notify instructor of approval: {}", ex.getMessage());
        }

        // Notify admins
        try {
            List<User> admins = userRepo.findByRole("ADMIN");
            for (User admin : admins) {
                notificationService.createNotification(
                        admin.getUserId(),
                        "Instructor Application Approved",
                        "Instructor account for " + saved.getUsername() + " (" + saved.getEmail() + ") was approved.",
                        NotificationType.INSTRUCTOR_APPROVAL,
                        "/manage-instructors"
                );
            }
        } catch (Exception ex) {
            log.warn("Failed to notify admins of instructor approval: {}", ex.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional
    public void rejectInstructor(Long userId) {
        log.trace("rejectInstructor entered");
        User target = loadInstructor(userId);
        if (target.hasBeenApproved()) {
            throw new InvalidRequestException("Only instructors who are waiting for approval can be rejected");
        }
        try {
            // Notify admins of the rejection
            try {
                List<User> admins = userRepo.findByRole("ADMIN");
                for (User admin : admins) {
                    notificationService.createNotification(
                            admin.getUserId(),
                            "Instructor Application Rejected",
                            "Instructor application for " + target.getUsername() + " (" + target.getEmail() + ") was rejected.",
                            NotificationType.INSTRUCTOR_REJECTION,
                            "/manage-instructors"
                    );
                }
            } catch (Exception notifEx) {
                log.warn("Failed to notify admins of rejection: {}", notifEx.getMessage());
            }

            // Clean up any notifications for this user before deleting the user entity
            notificationService.deleteAllNotifications(userId);

            emailOtpRepo.deleteByUser_UserId(userId);
            profileRepo.deleteByUser_UserId(userId);
            refreshTokenRepo.deleteByUser_UserId(userId);
            userRepo.delete(target);
            log.info("Instructor {} rejected and removed", userId);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Database error while rejecting an instructor", e);
        }
    }

    private User loadInstructor(Long userId) {
        User target = DatabaseOperationException.guard("loading a user", () -> userRepo.findById(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
        if (!"INSTRUCTOR".equals(target.getRole())) {
            throw new InvalidRequestException("This account is not an instructor");
        }
        return target;
    }

    @Override
    public ApplicationStatusResponse getApplicationStatus(String email) {
        log.trace("getApplicationStatus entered");
        String normalised = email.trim().toLowerCase(Locale.ROOT);
        User user = DatabaseOperationException.guard("loading a user", () -> userRepo.findByEmail(normalised)).orElse(null);
        if (user == null || !"INSTRUCTOR".equals(user.getRole())) {
            // the same answer for "never applied" and "somebody else's account", so this page reveals nothing about other users
            return new ApplicationStatusResponse(ApplicationStatusResponse.NOT_FOUND,
                    "We could not find an instructor application for this email. If an admin rejected it, you can apply again.", null);
        }
        java.time.LocalDateTime submitted = DatabaseOperationException
                .guard("loading the instructor profile", () -> profileRepo.findByUser_UserId(user.getUserId()))
                .map(InstructorProfile::getSubmittedAt).orElse(null);
        if (!user.hasVerifiedEmail()) {
            return new ApplicationStatusResponse(ApplicationStatusResponse.EMAIL_NOT_VERIFIED,
                    "Your application is saved, but your email is not verified yet. Enter the OTP we emailed you.", submitted);
        }
        if (!user.hasBeenApproved()) {
            return new ApplicationStatusResponse(ApplicationStatusResponse.UNDER_REVIEW,
                    "Your application has reached the admin and is under review. You will get an email when it is approved.", submitted);
        }
        return new ApplicationStatusResponse(ApplicationStatusResponse.APPROVED,
                "Your application is approved. You can log in as an instructor now.", submitted);
    }

    @Override
    public User getByEmail(String email) {
        log.trace("getByEmail entered");
        String normalised = email.trim().toLowerCase(Locale.ROOT);
        return DatabaseOperationException.guard("loading a user", () -> userRepo.findByEmail(normalised))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}