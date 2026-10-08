package com.examly.springapp.service;

import com.examly.springapp.dto.ApplicationStatusResponse;
import com.examly.springapp.dto.InstructorApplicationDTO;
import com.examly.springapp.model.LoginDTO;
import com.examly.springapp.model.User;
import java.util.List;

/**
 * Business operations on user accounts: registration and credential checks.
 *
 * @author Suriya
 */
public interface UserService {
    /**
     * Public sign-up. Creates a CUSTOMER or an INSTRUCTOR (asking for ADMIN is refused). The account stays inactive until
     * the emailed OTP is verified; an instructor also needs an admin's approval. Sends the OTP.
     */
    User register(User user);
    /** Admin-only: creates another admin account. */
    User createAdmin(User user);
    /** Admin-only: lists the admin accounts. */
    List<User> listAdmins();
    /** Admin-only: removes an admin (never yourself, never the last one). */
    void deleteAdmin(Long userId, String currentEmail);
    /** Checks email and password, then that the email is verified and (for instructors) approved. */
    User authenticate(LoginDTO login);
    /** Admin-only: lists the instructor accounts with the details they entered when registering, newest first. */
    List<InstructorApplicationDTO> listInstructors();
    /** Admin-only: lets a waiting instructor log in. */
    User approveInstructor(Long userId);
    /** Admin-only: removes an instructor who has not been approved yet. */
    void rejectInstructor(Long userId);
    /** Public: where is the instructor application of this email (EMAIL_NOT_VERIFIED, UNDER_REVIEW, APPROVED or NOT_FOUND)? */
    ApplicationStatusResponse getApplicationStatus(String email);
    User getByEmail(String email);
}