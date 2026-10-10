package com.examly.springapp.controller;

import com.examly.springapp.dto.TicketDtos.RespondRequest;
import com.examly.springapp.dto.TicketDtos.TicketView;
import com.examly.springapp.dto.TicketDtos.UserTicketSummary;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** Support tickets for admins: see who raised tickets, read a ticket, answer it (text resolves it, empty closes it). */
@RestController
@RequestMapping("/api/admin/tickets")
@Validated
@Tag(name = "Ticket management", description = "Read and answer support tickets (ADMIN only)")
public class AdminTicketController {
    private final TicketService tickets;
    private final AccessService access;

    public AdminTicketController(TicketService tickets, AccessService access) {
        this.tickets = tickets;
        this.access = access;
    }

    @Operation(summary = "Users who raised tickets", description = "One entry per user with a count of their tickets in each state.")
    @GetMapping("/users")
    public ResponseEntity<List<UserTicketSummary>> users() {
        return ResponseEntity.ok(tickets.summaries());
    }

    @Operation(summary = "Tickets of one user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TicketView>> byUser(@PathVariable @Min(1) Long userId) {
        return ResponseEntity.ok(tickets.forUser(userId));
    }

    @Operation(summary = "Open one ticket", description = "An OPEN ticket becomes PENDING while an admin is looking at it.")
    @GetMapping("/{id}")
    public ResponseEntity<TicketView> get(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(tickets.adminOpen(id));
    }

    @Operation(summary = "Answer a ticket", description = "A written response resolves the ticket; an empty response closes it. Only one response per ticket.")
    @PostMapping("/{id}/respond")
    public ResponseEntity<TicketView> respond(@PathVariable @Min(1) Long id, @Valid @RequestBody RespondRequest body) {
        return ResponseEntity.ok(tickets.respond(access.currentUser(), id, body.getResponse()));
    }
}