package com.examly.springapp.controller;

import com.examly.springapp.dto.InstructorApplicationDTO;
import com.examly.springapp.model.User;
import com.examly.springapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Instructor approval for admins: an instructor who registered and verified the email OTP cannot log in until an admin
 * approves the account here. Every admin may do this.
 *
 * @author Lokesh
 */
@RestController
@RequestMapping("/api/admin/instructors")
@Validated
@Tag(name = "Instructor management", description = "List, approve and reject instructor accounts (ADMIN only)")
public class InstructorAdminController {
    private static final Logger log = LoggerFactory.getLogger(InstructorAdminController.class);

    private final UserService userService;

    @Autowired
    public InstructorAdminController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List the instructor accounts", description = "ADMIN only. Newest first; each row has the account state (approved, emailVerified) and the experience, skills and about-you details the instructor entered while registering.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instructors returned"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @GetMapping
    public ResponseEntity<List<InstructorApplicationDTO>> list() {
        log.trace("list instructors called");
        return ResponseEntity.status(HttpStatus.OK).body(userService.listInstructors());
    }

    @Operation(summary = "Approve an instructor", description = "ADMIN only. The instructor can log in afterwards and is told by email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instructor approved"),
            @ApiResponse(responseCode = "400", description = "Invalid id or not an instructor"),
            @ApiResponse(responseCode = "403", description = "Not an admin"),
            @ApiResponse(responseCode = "404", description = "Instructor not found")
    })
    @PutMapping("/{userId}/approve")
    public ResponseEntity<User> approve(@PathVariable @Min(value = 1, message = "must be a positive number") Long userId) {
        log.trace("approve instructor called for userId={}", userId);
        return ResponseEntity.status(HttpStatus.OK).body(userService.approveInstructor(userId));
    }

    @Operation(summary = "Reject an instructor", description = "ADMIN only. Removes an instructor account that has not been approved yet.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instructor removed"),
            @ApiResponse(responseCode = "400", description = "Already approved, invalid id or not an instructor"),
            @ApiResponse(responseCode = "403", description = "Not an admin"),
            @ApiResponse(responseCode = "404", description = "Instructor not found")
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> reject(@PathVariable @Min(value = 1, message = "must be a positive number") Long userId) {
        log.trace("reject instructor called for userId={}", userId);
        userService.rejectInstructor(userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
