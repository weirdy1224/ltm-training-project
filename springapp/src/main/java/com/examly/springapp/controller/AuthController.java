package com.examly.springapp.controller;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.config.LoginAttemptService;
import com.examly.springapp.dto.GoogleLoginRequest;
import com.examly.springapp.model.LoginDTO;
import com.examly.springapp.model.User;
import com.examly.springapp.service.GoogleAuthService;
import com.examly.springapp.service.RefreshTokenService;
import com.examly.springapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import com.examly.springapp.dto.ApplicationStatusRequest;
import com.examly.springapp.dto.ApplicationStatusResponse;
import com.examly.springapp.dto.ForgotPasswordRequest;
import com.examly.springapp.dto.ResendOtpRequest;
import com.examly.springapp.dto.ResetPasswordRequest;
import com.examly.springapp.dto.VerifyOtpRequest;
import com.examly.springapp.service.OtpService;
import com.examly.springapp.service.PasswordResetService;

/**
 * Registration (with email OTP), login, token refresh and logout. The access token travels in
 * the response body,
 * the refresh token only in an HttpOnly cookie. Passwords and tokens are never
 * logged.
 *
 * @author Suriya
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Authentication", description = "Register, verify the email OTP, log in, refresh the access token and log out")
public class AuthController {
        private static final Logger log = LoggerFactory.getLogger(AuthController.class);
        private static final String REFRESH_COOKIE = "refresh_token";

        private final UserService userService;
        private final JwtUtil jwtUtil;
        private final RefreshTokenService refreshTokenService;
        private final LoginAttemptService loginAttempts;
        private final boolean secureCookie;
        private final long refreshDays;
        private final GoogleAuthService authService;
        private final PasswordResetService passwordResetService;
        private final OtpService otpService;

        @Autowired
        public AuthController(
                        UserService userService,
                        JwtUtil jwtUtil,
                        RefreshTokenService refreshTokenService,
                        LoginAttemptService loginAttempts,
                        @Value("${app.cookie.secure:false}") boolean secureCookie,
                        @Value("${jwt.refresh-expiration-days:7}") long refreshDays,
                        GoogleAuthService googleAuthService,
                        PasswordResetService passwordResetService,
                        OtpService otpService) {

                this.userService = userService;
                this.jwtUtil = jwtUtil;
                this.refreshTokenService = refreshTokenService;
                this.loginAttempts = loginAttempts;
                this.secureCookie = secureCookie;
                this.refreshDays = refreshDays;
                this.authService = googleAuthService;
                this.passwordResetService = passwordResetService;
                this.otpService = otpService;
        }

        @Operation(summary = "Sign in with Google", description = "Public. Verifies the Google ID token and returns the same answer as /api/login. A new Google user becomes a CUSTOMER with a verified email.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Logged in"),
                        @ApiResponse(responseCode = "401", description = "Invalid Google token or the Google email is not verified"),
                        @ApiResponse(responseCode = "403", description = "Instructor account still waiting for admin approval")
        })
        @PostMapping("/google-login")
        public ResponseEntity<Map<String, Object>> googleLogin(
                        @RequestBody GoogleLoginRequest request)
                        throws Exception {

                log.trace("googleLogin called");

                User user = authService.googleLogin(
                                request.getCredential());

                return session(user);
        }

        @Operation(summary = "Register a new customer or instructor", description = "Public. Role CUSTOMER (default) or INSTRUCTOR; ADMIN is refused. The account is created inactive and a 6-digit OTP is emailed; call /api/verify-otp next. Instructors also wait for admin approval. Admin accounts are created by the main admin through /api/admin/users.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "User created, OTP emailed"),
                        @ApiResponse(responseCode = "400", description = "Validation failed (email, mobile number, password rules)"),
                        @ApiResponse(responseCode = "403", description = "The ADMIN role cannot be requested here"),
                        @ApiResponse(responseCode = "409", description = "A user with this email already exists"),
                        @ApiResponse(responseCode = "502", description = "The account was saved but the OTP email could not be sent; use /api/resend-otp")
        })
        @PostMapping("/register")
        public ResponseEntity<User> register(@Valid @RequestBody User user) {
                log.trace("register called");
                User saved = userService.register(user);
                log.info("Registration completed for user {}", saved.getUserId());
                return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }

        @Operation(summary = "Verify the email OTP", description = "Public. Activates the account when the 6 digits match. Returns {message, role, approved}; an instructor still needs admin approval before logging in.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Email verified"),
                        @ApiResponse(responseCode = "400", description = "Wrong, expired or over-used OTP"),
                        @ApiResponse(responseCode = "429", description = "Too many requests")
        })
        @PostMapping("/verify-otp")
        public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
                log.trace("verifyOtp called");
                User user = otpService.verify(request.getEmail(), request.getOtp());
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("message", user.hasBeenApproved()
                                ? "Email verified. You can log in now."
                                : "Email verified. An admin must approve your instructor account before you can log in.");
                body.put("role", user.getRole());
                body.put("approved", user.hasBeenApproved());
                return ResponseEntity.status(HttpStatus.OK).body(body);
        }

        @Operation(summary = "Send a new OTP", description = "Public. Always answers 200 so it reveals nothing about registered emails; 429 while the cool-down of the previous OTP is running.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "OTP sent if the account exists and is not verified"),
                        @ApiResponse(responseCode = "429", description = "Please wait before asking again (see Retry-After)"),
                        @ApiResponse(responseCode = "502", description = "The email could not be sent")
        })
        @PostMapping("/resend-otp")
        public ResponseEntity<Map<String, String>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
                log.trace("resendOtp called");
                otpService.resend(request.getEmail());
                return ResponseEntity.status(HttpStatus.OK)
                                .body(Map.of("message", "If this email is waiting for verification, a new OTP has been sent."));
        }

        @Operation(summary = "Track an instructor application", description = "Public. Send the email used to apply; the answer is only the step (EMAIL_NOT_VERIFIED, UNDER_REVIEW, APPROVED or NOT_FOUND), never personal data.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Status returned (NOT_FOUND is also a 200, so the page reveals nothing about other users)"),
                        @ApiResponse(responseCode = "400", description = "Email missing or invalid"),
                        @ApiResponse(responseCode = "429", description = "Too many requests")
        })
        @PostMapping("/instructor-application/status")
        public ResponseEntity<ApplicationStatusResponse> applicationStatus(@Valid @RequestBody ApplicationStatusRequest request) {
                log.trace("applicationStatus called");
                return ResponseEntity.status(HttpStatus.OK).body(userService.getApplicationStatus(request.getEmail()));
        }

        @Operation(summary = "Log in", description = "Returns {token, expiresIn, userId, role, username, email} and sets the refresh cookie.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Logged in"),
                        @ApiResponse(responseCode = "400", description = "Email or password missing"),
                        @ApiResponse(responseCode = "401", description = "Wrong email or password"),
                        @ApiResponse(responseCode = "403", description = "Correct password but the email is not verified (code EMAIL_NOT_VERIFIED) or the instructor is not approved yet (code APPROVAL_PENDING)"),
                        @ApiResponse(responseCode = "429", description = "Too many attempts, see the Retry-After header")
        })
        @PostMapping("/login")
        public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginDTO login,
                        HttpServletRequest request) {
                log.trace("login called");
                // Business rule: failed logins are counted per (email, IP) pair and lock that
                // pair for a while.
                String key = login.getEmail().trim().toLowerCase(Locale.ROOT) + "|" + request.getRemoteAddr();
                loginAttempts.checkNotLocked(key);
                User user;
                try {
                        user = userService.authenticate(login);
                } catch (BadCredentialsException e) {
                        loginAttempts.recordFailure(key);
                        throw e;
                }
                loginAttempts.recordSuccess(key);
                log.info("Login successful for user {} with role {}", user.getUserId(), user.getRole());
                return session(user);
        }

        /**
         * Swaps the refresh cookie for a new access token (and a new refresh cookie).
         */
        @Operation(summary = "Refresh the access token", description = "Uses the HttpOnly refresh cookie; the old refresh token stops working (rotation).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "New access token issued"),
                        @ApiResponse(responseCode = "401", description = "Refresh cookie missing, invalid, expired or already used"),
                        @ApiResponse(responseCode = "429", description = "Too many requests")
        })
        @PostMapping("/refresh")
        public ResponseEntity<Map<String, Object>> refresh(
                        @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
                log.trace("refresh called");
                User user = refreshTokenService.consume(refreshToken);
                return session(user);
        }

        @Operation(summary = "Log out", description = "Revokes the refresh token and clears the cookie.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Logged out (also when there was no session)")
        })
        @PostMapping("/logout")
        public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
                log.trace("logout called");
                refreshTokenService.revoke(refreshToken);
                log.info("Logout completed");
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                                .build();
        }

        @PostMapping("/forgot-password")
        public ResponseEntity<Map<String, String>> forgotPassword(
                        @Valid @RequestBody ForgotPasswordRequest request) {

                passwordResetService.requestReset(request.getEmail());

                return ResponseEntity.ok(
                                Map.of(
                                                "message",
                                                "If an account exists for that email, a password reset link has been sent."));
        }

        @PostMapping("/reset-password")
        public ResponseEntity<Map<String, String>> resetPassword(
                        @Valid @RequestBody ResetPasswordRequest request) {

                passwordResetService.resetPassword(
                                request.getToken(),
                                request.getPassword());

                return ResponseEntity.ok(
                                Map.of(
                                                "message",
                                                "Password reset successfully"));
        }

        /**
         * Builds the login/refresh answer: the access token (with userId and username
         * claims) plus the refresh cookie.
         */
        private ResponseEntity<Map<String, Object>> session(User user) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("token", jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getUserId(),
                                user.getUsername()));
                body.put("expiresIn", jwtUtil.getAccessExpirationMs() / 1000);
                body.put("userId", user.getUserId());
                body.put("role", user.getRole());
                body.put("username", user.getUsername());
                body.put("email", user.getEmail());
                String refresh = refreshTokenService.issue(user);
                log.debug("Session issued for user {}", user.getUserId());
                return ResponseEntity.status(HttpStatus.OK)
                                .header(HttpHeaders.SET_COOKIE,
                                                cookie(refresh, Duration.ofDays(refreshDays)).toString())
                                .body(body);
        }

        private ResponseCookie cookie(String value, Duration maxAge) {
                return ResponseCookie.from(REFRESH_COOKIE, value)
                                .httpOnly(true)
                                .secure(secureCookie)
                                .sameSite("Strict")
                                .path("/api")
                                .maxAge(maxAge)
                                .build();
        }

}