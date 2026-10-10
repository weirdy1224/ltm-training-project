package com.examly.springapp.controller;

import com.examly.springapp.dto.TicketDtos.FeedbackRequest;
import com.examly.springapp.dto.TicketDtos.TicketView;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Support tickets for customers and instructors: raise a ticket, read your tickets, rate the admin's response. */
@RestController
@RequestMapping("/api/tickets")
@Validated
@Tag(name = "Support tickets", description = "Raise and follow your own support tickets (CUSTOMER, INSTRUCTOR)")
public class TicketController {
    private final TicketService tickets;
    private final AccessService access;

    public TicketController(TicketService tickets, AccessService access) {
        this.tickets = tickets;
        this.access = access;
    }

    @Operation(summary = "Raise a ticket", description = "The userId must be your own id. The screenshot (PNG, JPG, GIF or WEBP, up to 5 MB) is optional.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketView> create(@RequestParam("userId") Long userId,
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestPart(value = "screenshot", required = false) MultipartFile screenshot) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tickets.create(access.currentUser(), userId, category, description, screenshot));
    }

    @Operation(summary = "My tickets", description = "All tickets raised by the logged-in user, newest first.")
    @GetMapping
    public ResponseEntity<List<TicketView>> mine() {
        return ResponseEntity.ok(tickets.mine(access.currentUser()));
    }

    @Operation(summary = "One of my tickets")
    @GetMapping("/{id}")
    public ResponseEntity<TicketView> get(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(tickets.getMine(access.currentUser(), id));
    }

    @Operation(summary = "Rate the admin's response", description = "Only when the ticket is RESOLVED or CLOSED, and only once.")
    @PostMapping("/{id}/feedback")
    public ResponseEntity<TicketView> feedback(@PathVariable @Min(1) Long id, @Valid @RequestBody FeedbackRequest body) {
        return ResponseEntity.ok(tickets.feedback(access.currentUser(), id, body));
    }
}