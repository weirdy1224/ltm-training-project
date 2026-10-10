package com.examly.springapp.model;

/** Life cycle of a support ticket: OPEN -> PENDING (admin opened it) -> RESOLVED (answered) or CLOSED (no answer). */
public enum TicketStatus {
    OPEN,
    PENDING,
    CLOSED,
    RESOLVED
}