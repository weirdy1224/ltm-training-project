package com.examly.springapp.dto;

import com.examly.springapp.model.Ticket;
import com.examly.springapp.model.TicketCategory;
import com.examly.springapp.model.TicketStatus;
import com.examly.springapp.model.User;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Request and response shapes of the ticket system. */
public final class TicketDtos {
    private TicketDtos() {
    }

    /** The admin's reply. An empty or missing response closes the ticket; a text resolves it. */
    public static class RespondRequest {
        @Size(max = 2000, message = "The response is too long (2000 characters at most)")
        private String response;

        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
    }

    /** The user's rating of the admin's response. */
    public static class FeedbackRequest {
        @NotNull(message = "Please rate the response")
        @Min(value = 1, message = "Rating must be 1 to 5")
        @Max(value = 5, message = "Rating must be 1 to 5")
        private Integer rating;

        @NotNull(message = "Please say how useful the response was")
        @Min(value = 1, message = "Usefulness must be 1 to 5")
        @Max(value = 5, message = "Usefulness must be 1 to 5")
        private Integer usefulness;

        @Size(max = 1000, message = "The comment is too long (1000 characters at most)")
        private String comment;

        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public Integer getUsefulness() { return usefulness; }
        public void setUsefulness(Integer usefulness) { this.usefulness = usefulness; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    /** A ticket as the client sees it (the owner's name and email are included for the admin screens). */
    public record TicketView(Long id, String ticketCode, Long userId, String username, String email, String role,
            TicketCategory category, String description, String screenshotUrl, TicketStatus status,
            String adminResponse, LocalDateTime createdAt, LocalDateTime respondedAt, Integer feedbackRating,
            Integer feedbackUsefulness, String feedbackComment) {

        public static TicketView of(Ticket t, User owner) {
            return new TicketView(t.getId(), t.getTicketCode(), t.getUserId(),
                    owner == null ? null : owner.getUsername(), owner == null ? null : owner.getEmail(),
                    owner == null ? null : owner.getRole(), t.getCategory(), t.getDescription(),
                    t.getScreenshotUrl(), t.getStatus(), t.getAdminResponse(), t.getCreatedAt(), t.getRespondedAt(),
                    t.getFeedbackRating(), t.getFeedbackUsefulness(), t.getFeedbackComment());
        }
    }

    /** One container on the admin's ticket list: a user and how many tickets they have in each state. */
    public record UserTicketSummary(Long userId, String username, String email, String role, long total, long open,
            long pending, long resolved, long closed, LocalDateTime lastTicketAt) {
    }
}